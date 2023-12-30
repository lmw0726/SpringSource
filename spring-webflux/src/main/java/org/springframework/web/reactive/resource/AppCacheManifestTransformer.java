/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.web.reactive.resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.DigestUtils;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SynchronousSink;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Scanner;
import java.util.function.Consumer;

/**
 * {@code AppCacheManifestTransformer} 是一个 {@link ResourceTransformer}，用于处理 HTML5 AppCache manifest。
 *
 * <p>此转换器：
 * <ul>
 * <li>通过配置的 {@code ResourceResolver} 策略修改链接，以匹配应向客户端公开的公共 URL 路径
 * <li>在清单中追加注释，包含哈希值（例如 "# Hash: 9de0f09ed7caf84e885f1f0f11c7e326"），
 * 从而改变清单内容以触发浏览器中的 appcache 重新加载。
 * </ul>
 *
 * <p>所有扩展名为 ".appcache"（或构造函数中给定的扩展名）的文件都将由此类进行转换。
 * 使用 appcache 清单内容计算哈希值，因此清单内容的更改应使浏览器缓存失效。
 * 对链接进行版本化的资源内容更改也应该有效。
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @see <a href="https://html.spec.whatwg.org/multipage/browsers.html#offline">HTML5 离线应用规范</a>
 * @since 5.0
 * @deprecated 自 5.3 起不再受浏览器支持，已弃用
 */
@Deprecated
public class AppCacheManifestTransformer extends ResourceTransformerSupport {

	/**
	 * 清单文件的头部标识
	 */
	private static final String MANIFEST_HEADER = "CACHE MANIFEST";

	/**
	 * 缓存部分的标识
	 */
	private static final String CACHE_HEADER = "CACHE:";

	/**
	 * 清单文件中各个部分的标识集合
	 */
	private static final Collection<String> MANIFEST_SECTION_HEADERS =
			Arrays.asList(MANIFEST_HEADER, "NETWORK:", "FALLBACK:", CACHE_HEADER);

	/**
	 * 默认字符集
	 */
	private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

	/**
	 * 日志记录器
	 */
	private static final Log logger = LogFactory.getLog(AppCacheManifestTransformer.class);

	/**
	 * 文件扩展名
	 */
	private final String fileExtension;


	/**
	 * 创建一个转换 ".appcache" 扩展名文件的 AppCacheResourceTransformer。
	 */
	public AppCacheManifestTransformer() {
		this("appcache");
	}

	/**
	 * 创建一个转换指定扩展名文件的 AppCacheResourceTransformer。
	 */
	public AppCacheManifestTransformer(String fileExtension) {
		this.fileExtension = fileExtension;
	}

	/**
	 * 转换资源。
	 *
	 * @param exchange      当前的交换对象
	 * @param inputResource 输入资源
	 * @param chain         资源转换器链
	 * @return 转换后的资源
	 */
	@Override
	public Mono<Resource> transform(ServerWebExchange exchange, Resource inputResource,
									ResourceTransformerChain chain) {

		// 进行资源转换操作
		return chain.transform(exchange, inputResource)
				.flatMap(outputResource -> {
					// 获取输出资源的文件名
					String name = outputResource.getFilename();
					// 如果输出资源的文件扩展名与指定的文件扩展名不匹配
					if (!this.fileExtension.equals(StringUtils.getFilenameExtension(name))) {
						// 直接返回输出资源
						return Mono.just(outputResource);
					}
					// 获取响应的数据缓冲区工厂
					DataBufferFactory bufferFactory = exchange.getResponse().bufferFactory();
					// 读取输出资源并创建数据流 Flux
					Flux<DataBuffer> flux = DataBufferUtils
							.read(outputResource, bufferFactory, StreamUtils.BUFFER_SIZE);
					// 合并数据流中的数据缓冲区，并对其进行处理
					return DataBufferUtils.join(flux)
							.flatMap(dataBuffer -> {
								// 将数据缓冲区解码为字符缓冲区
								CharBuffer charBuffer = DEFAULT_CHARSET.decode(dataBuffer.asByteBuffer());
								// 释放数据缓冲区
								DataBufferUtils.release(dataBuffer);
								// 将字符缓冲区转换为字符串内容，并进行进一步的转换操作
								String content = charBuffer.toString();
								return transform(content, outputResource, chain, exchange);
							});
				});
	}

	/**
	 * 对传入的内容进行转换，生成带有哈希标识的清单内容，并返回转换后的资源。
	 *
	 * @param content       要转换的内容
	 * @param resource      当前正在处理的资源
	 * @param chain         资源转换器链
	 * @param exchange      当前的交换对象
	 * @return 转换后带有哈希标识的资源
	 */
	private Mono<? extends Resource> transform(String content, Resource resource,
											   ResourceTransformerChain chain, ServerWebExchange exchange) {
		// 如果内容不以 MANIFEST_HEADER 开头，则不做处理，直接返回原始资源
		if (!content.startsWith(MANIFEST_HEADER)) {
			if (logger.isTraceEnabled()) {
				logger.trace(exchange.getLogPrefix() +
						"Skipping " + resource + ": Manifest does not start with 'CACHE MANIFEST'");
			}
			return Mono.just(resource);
		}

		// 生成行信息，并进行行处理和拼接处理后的结果
		return Flux.generate(new LineInfoGenerator(content))
				.concatMap(info -> processLine(info, exchange, resource, chain))
				.reduce(new ByteArrayOutputStream(), (out, line) -> {
					writeToByteArrayOutputStream(out, line + "\n");
					return out;
				})
				// 计算哈希并添加到输出流中，最终生成转换后的资源
				.map(out -> {
					String hash = DigestUtils.md5DigestAsHex(out.toByteArray());
					writeToByteArrayOutputStream(out, "\n" + "# Hash: " + hash);
					return new TransformedResource(resource, out.toByteArray());
				});
	}

	private static void writeToByteArrayOutputStream(ByteArrayOutputStream out, String toWrite) {
		try {
			byte[] bytes = toWrite.getBytes(DEFAULT_CHARSET);
			out.write(bytes);
		} catch (IOException ex) {
			throw Exceptions.propagate(ex);
		}
	}

	/**
	 * 处理行信息并返回处理后的结果。
	 *
	 * @param info          行信息对象，包含当前行的内容和标志信息
	 * @param exchange      当前的交换对象
	 * @param resource      当前正在处理的资源
	 * @param chain         资源转换器链
	 * @return 处理后的行内容，如果不是链接行则直接返回当前行内容
	 */
	private Mono<String> processLine(LineInfo info, ServerWebExchange exchange,
									 Resource resource, ResourceTransformerChain chain) {
		// 如果不是链接行，则直接返回当前行内容
		if (!info.isLink()) {
			return Mono.just(info.getLine());
		}

		// 将相对路径转换为绝对路径，并解析为公共 URL 路径
		String link = toAbsolutePath(info.getLine(), exchange);
		return resolveUrlPath(link, exchange, resource, chain);
	}


	/**
	 * {@code LineInfoGenerator} 是一个消费者，用于生成 {@code LineInfo} 实例并提供给 {@code SynchronousSink}。
	 */
	private static class LineInfoGenerator implements Consumer<SynchronousSink<LineInfo>> {

		/**
		 * 用于扫描的 Scanner 对象
		 */
		private final Scanner scanner;

		/**
		 * 前一行 行信息 对象
		 */
		@Nullable
		private LineInfo previous;


		/**
		 * 构造一个 LineInfoGenerator 实例。
		 *
		 * @param content 要扫描的内容
		 */
		LineInfoGenerator(String content) {
			this.scanner = new Scanner(content);
		}

		/**
		 * 接受 {@code SynchronousSink} 并生成 LineInfo 实例。
		 *
		 * @param sink SynchronousSink 用于传递 LineInfo 实例
		 */
		@Override
		public void accept(SynchronousSink<LineInfo> sink) {
			// 如果扫描器有下一行内容
			if (this.scanner.hasNext()) {
				// 读取下一行并创建 LineInfo 对象
				String line = this.scanner.nextLine();
				LineInfo current = new LineInfo(line, this.previous);
				// 将当前 LineInfo 对象传递给下游
				sink.next(current);
				// 更新 previous 为当前 LineInfo 对象
				this.previous = current;
			} else {
				// 如果没有下一行内容，通知下游处理完成
				sink.complete();
			}
		}
	}


	/**
	 * {@code LineInfo} 是一个包含行信息的私有静态内部类。
	 */
	private static class LineInfo {

		/**
		 * 表示一行文本
		 */
		private final String line;

		/**
		 * 指示是否缓存该部分
		 */
		private final boolean cacheSection;

		/**
		 * 指示是否进行链接操作
		 */
		private final boolean link;


		/**
		 * 构造一个 LineInfo 实例。
		 *
		 * @param line         当前行的内容
		 * @param previousLine 前一行的 LineInfo，可为 null
		 */
		LineInfo(String line, @Nullable LineInfo previousLine) {
			this.line = line;
			this.cacheSection = initCacheSectionFlag(line, previousLine);
			this.link = iniLinkFlag(line, this.cacheSection);
		}

		/**
		 * 初始化缓存部分标志。
		 *
		 * @param line         当前行的内容
		 * @param previousLine 前一行的 LineInfo，可为 null
		 * @return 是否处于缓存部分
		 * @throws IllegalStateException 如果清单不以 MANIFEST_HEADER 开头
		 */
		private static boolean initCacheSectionFlag(String line, @Nullable LineInfo previousLine) {
			// 去除首尾空白后的文本行
			String trimmedLine = line.trim();

			// 如果清理后的行是 MANIFEST_SECTION_HEADERS 中的一个
			if (MANIFEST_SECTION_HEADERS.contains(trimmedLine)) {
				// 返回该行是否等于 CACHE_HEADER
				return trimmedLine.equals(CACHE_HEADER);
			} else if (previousLine != null) {
				// 如果前一行不为空，则返回前一行是否为缓存部分
				return previousLine.isCacheSection();
			}
			// 若前面的条件均不满足，则抛出异常，表明清单文件没有以 MANIFEST_HEADER 开始
			throw new IllegalStateException(
					"Manifest does not start with " + MANIFEST_HEADER + ": " + line);
		}

		/**
		 * 初始化链接标志。
		 *
		 * @param line           当前行的内容
		 * @param isCacheSection 是否处于缓存部分
		 * @return 是否是链接行
		 */
		private static boolean iniLinkFlag(String line, boolean isCacheSection) {
			return (isCacheSection && StringUtils.hasText(line) && !line.startsWith("#")
					&& !line.startsWith("//") && !hasScheme(line));
		}

		/**
		 * 检查字符串是否包含方案。
		 *
		 * @param line 要检查的字符串
		 * @return 字符串是否包含方案
		 */
		private static boolean hasScheme(String line) {
			int index = line.indexOf(':');
			return (line.startsWith("//") || (index > 0 && !line.substring(0, index).contains("/")));
		}

		/**
		 * 获取当前行的内容。
		 *
		 * @return 当前行的内容
		 */
		public String getLine() {
			return this.line;
		}

		/**
		 * 检查是否处于缓存部分。
		 *
		 * @return 是否处于缓存部分
		 */
		public boolean isCacheSection() {
			return this.cacheSection;
		}

		/**
		 * 检查是否是链接行。
		 *
		 * @return 是否是链接行
		 */
		public boolean isLink() {
			return this.link;
		}
	}

}
