/*
 * Copyright 2002-2021 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

/**
 * 表示用于存储大于 {@link DefaultPartHttpMessageReader#setMaxInMemorySize(int)} 的部分的目录。
 *
 * @author Arjen Poutsma
 * @since 5.3.7
 */
abstract class FileStorage {
	/**
	 * 日志记录器
	 */
	private static final Log logger = LogFactory.getLog(FileStorage.class);


	protected FileStorage() {
	}

	/**
	 * 获取用于存储文件的目录的 mono。
	 */
	public abstract Mono<Path> directory();


	/**
	 * 从用户指定的路径创建一个新的 {@code FileStorage}。如果路径不存在，则创建该路径。
	 */
	public static FileStorage fromPath(Path path) throws IOException {
		// 如果指定路径不存在
		if (!Files.exists(path)) {
			// 创建目录
			Files.createDirectory(path);
		}
		// 返回一个新的 PathFileStorage 对象
		return new PathFileStorage(path);
	}

	/**
	 * 基于临时目录创建一个新的 {@code FileStorage}。
	 *
	 * @param scheduler 用于阻塞操作的调度器
	 */
	public static FileStorage tempDirectory(Supplier<Scheduler> scheduler) {
		return new TempFileStorage(scheduler);
	}


	private static final class PathFileStorage extends FileStorage {
		/**
		 * 目录
		 */
		private final Mono<Path> directory;

		public PathFileStorage(Path directory) {
			this.directory = Mono.just(directory);
		}

		@Override
		public Mono<Path> directory() {
			return this.directory;
		}
	}


	private static final class TempFileStorage extends FileStorage {

		/**
		 * 标识符
		 */
		private static final String IDENTIFIER = "spring-multipart-";

		/**
		 * 调度器提供者
		 */
		private final Supplier<Scheduler> scheduler;

		/**
		 * 目录
		 */
		private volatile Mono<Path> directory = tempDirectory();


		public TempFileStorage(Supplier<Scheduler> scheduler) {
			this.scheduler = scheduler;
		}

		@Override
		public Mono<Path> directory() {
			return this.directory
					// 处理目录被删除后重新创建目录的逻辑
					.flatMap(this::createNewDirectoryIfDeleted)
					// 在指定的调度器上订阅该 Flux
					.subscribeOn(this.scheduler.get());
		}

		private Mono<Path> createNewDirectoryIfDeleted(Path directory) {
			if (!Files.exists(directory)) {
				// 如果目录不存在
				// 生成一个新的临时目录路径的 Mono
				Mono<Path> newDirectory = tempDirectory();
				// 将 当前目录 设置为新生成的临时目录路径
				this.directory = newDirectory;
				// 返回新生成的临时目录路径的 Mono
				return newDirectory;
			} else {
				// 如果目录存在，则返回当前目录路径的 Mono
				return Mono.just(directory);
			}
		}

		private static Mono<Path> tempDirectory() {
			return Mono.fromCallable(() -> {
				// 创建临时目录
				Path directory = Files.createTempDirectory(IDENTIFIER);
				// 如果日志级别是调试，则记录创建的临时目录路径
				if (logger.isDebugEnabled()) {
					logger.debug("Created temporary storage directory: " + directory);
				}
				// 返回创建的临时目录路径
				return directory;
			}).cache();
		}
	}

}
