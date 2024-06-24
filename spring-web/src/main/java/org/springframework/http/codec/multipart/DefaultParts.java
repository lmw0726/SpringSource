/*
 * Copyright 2002-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.http.codec.multipart;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.Callable;

/**
 * {@link Part} 及其子类型的默认实现。
 *
 * <p>该类提供了创建 {@link FormFieldPart} 和 {@link Part} 的静态方法。
 *
 * <p>注意：返回的 {@link Part} 或 {@link FilePart} 取决于 {@code Content-Disposition} 头部是否包含文件名。
 *
 * @author Arjen Poutsma
 * @since 5.3
 */
abstract class DefaultParts {

	/**
	 * 使用给定的参数创建一个新的 {@link FormFieldPart}。
	 *
	 * @param headers 头部信息
	 * @param value   表单字段的值
	 * @return 创建的表单字段部分
	 */
	public static FormFieldPart formFieldPart(HttpHeaders headers, String value) {
		Assert.notNull(headers, "Headers must not be null");
		Assert.notNull(value, "Value must not be null");

		return new DefaultFormFieldPart(headers, value);
	}

	/**
	 * 基于数据缓冲区流创建一个新的 {@link Part} 或 {@link FilePart}。
	 * 如果 {@code Content-Disposition} 头部包含文件名，则返回 {@link FilePart}；否则返回普通的 {@link Part}。
	 *
	 * @param headers     头部信息
	 * @param dataBuffers 部分内容的数据缓冲区流
	 * @return {@link Part} 或 {@link FilePart}
	 */
	public static Part part(HttpHeaders headers, Flux<DataBuffer> dataBuffers) {
		Assert.notNull(headers, "Headers must not be null");
		Assert.notNull(dataBuffers, "DataBuffers must not be null");

		return partInternal(headers, new FluxContent(dataBuffers));
	}

	/**
	 * 基于给定的文件创建一个新的 {@link Part} 或 {@link FilePart}。
	 * 如果 {@code Content-Disposition} 头部包含文件名，则返回 {@link FilePart}；否则返回普通的 {@link Part}。
	 *
	 * @param headers   头部信息
	 * @param file      文件
	 * @param scheduler 用于读取文件的调度器
	 * @return {@link Part} 或 {@link FilePart}
	 */
	public static Part part(HttpHeaders headers, Path file, Scheduler scheduler) {
		Assert.notNull(headers, "Headers must not be null");
		Assert.notNull(file, "File must not be null");
		Assert.notNull(scheduler, "Scheduler must not be null");

		return partInternal(headers, new FileContent(file, scheduler));
	}


	private static Part partInternal(HttpHeaders headers, Content content) {
		// 从 Content-Disposition 获取文件名
		String filename = headers.getContentDisposition().getFilename();
		if (filename != null) {
			// 如果Content-Disposition中包含文件名，则创建一个DefaultFilePart对象
			return new DefaultFilePart(headers, content);
		} else {
			// 否则创建一个DefaultPart对象
			return new DefaultPart(headers, content);
		}
	}


	/**
	 * {@link Part} 实现的抽象基类。
	 */
	private static abstract class AbstractPart implements Part {
		/**
		 * Http头部信息
		 */
		private final HttpHeaders headers;

		protected AbstractPart(HttpHeaders headers) {
			Assert.notNull(headers, "HttpHeaders is required");
			this.headers = headers;
		}

		@Override
		public String name() {
			// 获取名称
			String name = headers().getContentDisposition().getName();
			// 断言名称不为空
			Assert.state(name != null, "No name available");
			return name;
		}

		@Override
		public HttpHeaders headers() {
			return this.headers;
		}
	}


	/**
	 * {@link FormFieldPart}的默认实现
	 */
	private static class DefaultFormFieldPart extends AbstractPart implements FormFieldPart {
		/**
		 * 字段值
		 */
		private final String value;

		public DefaultFormFieldPart(HttpHeaders headers, String value) {
			super(headers);
			this.value = value;
		}

		@Override
		public Flux<DataBuffer> content() {
			return Flux.defer(() -> {
				// 将字符串转换为字节数组，并使用指定的字符集进行编码
				byte[] bytes = this.value.getBytes(MultipartUtils.charset(headers()));
				// 将字节数组包装成DataBuffer并生成Flux
				return Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(bytes));
			});
		}

		@Override
		public String value() {
			return this.value;
		}

		@Override
		public String toString() {
			String name = headers().getContentDisposition().getName();
			if (name != null) {
				return "DefaultFormFieldPart{" + name() + "}";
			} else {
				return "DefaultFormFieldPart";
			}
		}
	}


	/**
	 * {@link Part}的默认实现
	 */
	private static class DefaultPart extends AbstractPart {
		/**
		 * 内容
		 */
		protected final Content content;

		public DefaultPart(HttpHeaders headers, Content content) {
			super(headers);
			this.content = content;
		}

		@Override
		public Flux<DataBuffer> content() {
			return this.content.content();
		}

		@Override
		public Mono<Void> delete() {
			return this.content.delete();
		}

		@Override
		public String toString() {
			String name = headers().getContentDisposition().getName();
			if (name != null) {
				return "DefaultPart{" + name + "}";
			} else {
				return "DefaultPart";
			}
		}
	}


	/**
	 * {@link FilePart}的默认实现。
	 */
	private static final class DefaultFilePart extends DefaultPart implements FilePart {

		public DefaultFilePart(HttpHeaders headers, Content content) {
			super(headers, content);
		}

		@Override
		public String filename() {
			// 获取文件名
			String filename = headers().getContentDisposition().getFilename();
			// 断言文件名不为空
			Assert.state(filename != null, "No filename found");
			return filename;
		}

		@Override
		public Mono<Void> transferTo(Path dest) {
			return this.content.transferTo(dest);
		}

		@Override
		public String toString() {
			ContentDisposition contentDisposition = headers().getContentDisposition();
			String name = contentDisposition.getName();
			String filename = contentDisposition.getFilename();
			if (name != null) {
				return "DefaultFilePart{" + name + " (" + filename + ")}";
			} else {
				return "DefaultFilePart{(" + filename + ")}";
			}
		}
	}


	/**
	 * 部分内容的抽象表示。
	 */
	private interface Content {

		/**
		 * 返回此内容的数据流。
		 *
		 * @return 数据流
		 */
		Flux<DataBuffer> content();

		/**
		 * 将内容传输到指定的目标路径。
		 *
		 * @param dest 目标路径
		 * @return 表示传输完成或错误的 {@code Mono}
		 */
		Mono<Void> transferTo(Path dest);

		/**
		 * 删除此内容的底层存储。
		 *
		 * @return 表示删除操作完成或错误的 {@code Mono}
		 */
		Mono<Void> delete();
	}


	/**
	 * 基于数据缓冲区流的 {@code Content} 实现。
	 */
	private static final class FluxContent implements Content {
		/**
		 * 存放 内容 的数据缓冲区
		 */
		private final Flux<DataBuffer> content;

		public FluxContent(Flux<DataBuffer> content) {
			this.content = content;
		}

		/**
		 * 返回数据缓冲区流。
		 *
		 * @return 数据缓冲区流
		 */
		@Override
		public Flux<DataBuffer> content() {
			return this.content;
		}

		/**
		 * 将内容传输到指定的目标路径。
		 *
		 * @param dest 目标路径
		 * @return 表示传输完成或错误的 {@code Mono}
		 */
		@Override
		public Mono<Void> transferTo(Path dest) {
			return DataBufferUtils.write(this.content, dest);
		}

		/**
		 * 删除此内容的底层存储。由于基于流的内容通常不需要删除操作，此方法返回一个空的 {@code Mono}。
		 *
		 * @return 表示删除操作完成或错误的 {@code Mono}
		 */
		@Override
		public Mono<Void> delete() {
			return Mono.empty();
		}
	}


	/**
	 * 基于文件的 {@code Content} 实现。
	 */
	private static final class FileContent implements Content {
		/**
		 * 文件路径
		 */
		private final Path file;

		/**
		 * 调度器
		 */
		private final Scheduler scheduler;

		public FileContent(Path file, Scheduler scheduler) {
			this.file = file;
			this.scheduler = scheduler;
		}

		/**
		 * 返回文件的内容作为数据缓冲区流。
		 *
		 * @return 文件内容的数据缓冲区流
		 */
		@Override
		public Flux<DataBuffer> content() {
			return DataBufferUtils.readByteChannel(
							// 打开文件的字节通道，并指定为读取模式
							() -> Files.newByteChannel(this.file, StandardOpenOption.READ),
							// 使用默认的DataBuffer工厂来创建DataBuffer
							// 指定每次读取的字节数
							DefaultDataBufferFactory.sharedInstance, 1024)
					// 在指定的调度器上执行读取操作
					.subscribeOn(this.scheduler);
		}

		/**
		 * 将文件内容传输到指定的目标路径。
		 *
		 * @param dest 目标路径
		 * @return 表示传输完成或错误的 {@code Mono}
		 */
		@Override
		public Mono<Void> transferTo(Path dest) {
			return blockingOperation(() -> Files.copy(this.file, dest, StandardCopyOption.REPLACE_EXISTING));
		}

		/**
		 * 删除文件的底层存储。
		 *
		 * @return 表示删除操作完成或错误的 {@code Mono}
		 */
		@Override
		public Mono<Void> delete() {
			return blockingOperation(() -> {
				// 删除文件
				Files.delete(this.file);
				return null;
			});
		}

		/**
		 * 执行阻塞操作的辅助方法，将其包装为 {@code Mono<Void>}。
		 *
		 * @param callable 执行的可调用对象
		 * @return 表示操作完成或错误的 {@code Mono}
		 */
		private Mono<Void> blockingOperation(Callable<?> callable) {
			return Mono.<Void>create(sink -> {
						try {
							// 执行回调程序
							callable.call();
							// 通知操作完成
							sink.success();
						} catch (Exception ex) {
							// 通知操作失败
							sink.error(ex);
						}
					})
					// 在指定的调度器上执行回调操作
					.subscribeOn(this.scheduler);
		}
	}

}
