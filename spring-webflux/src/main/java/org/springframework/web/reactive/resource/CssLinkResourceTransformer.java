/*
 * Copyright 2002-2019 the original author or authors.
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
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.StringWriter;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * {@code CssLinkResourceTransformer} 是一个 {@link ResourceTransformer} 实现，
 * 用于修改 CSS 文件中的链接，使其与应向客户端公开的公共 URL 路径匹配（例如，在 URL 中插入基于 MD5 内容的哈希）。
 *
 * <p>该实现在 CSS 的 {@code @import} 语句和 CSS {@code url()} 函数中查找链接。
 * 然后将所有链接通过 {@link ResourceResolverChain} 解析，并相对于包含 CSS 文件的位置进行解析。
 * 如果成功解析，链接将被修改，否则保留原始链接。
 * </p>
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class CssLinkResourceTransformer extends ResourceTransformerSupport {

	/**
	 * 默认字符集为 UTF-8
	 */
	private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

	/**
	 * 日志记录器
	 */
	private static final Log logger = LogFactory.getLog(CssLinkResourceTransformer.class);

	/**
	 * LinkParser 对象的列表，初始大小为 2
	 */
	private final List<LinkParser> linkParsers = new ArrayList<>(2);


	public CssLinkResourceTransformer() {
		this.linkParsers.add(new ImportLinkParser());
		this.linkParsers.add(new UrlFunctionLinkParser());
	}

	/**
	 * 对资源进行转换，特别是针对 CSS 文件进行处理，修改其中的链接。
	 *
	 * @param exchange           当前的服务器交换对象
	 * @param inputResource      输入的资源对象
	 * @param transformerChain   资源转换器链
	 * @return 经过转换后的资源 Mono 对象
	 */
	@Override
	@SuppressWarnings("deprecation")
	public Mono<Resource> transform(ServerWebExchange exchange, Resource inputResource,
									ResourceTransformerChain transformerChain) {

		// 进行资源转换操作
		return transformerChain.transform(exchange, inputResource)
				.flatMap(outputResource -> {
					// 获取输出资源的文件名
					String filename = outputResource.getFilename();
					// 如果不是 CSS 文件，或者是已编码资源或已压缩资源，则直接返回输出资源
					if (!"css".equals(StringUtils.getFilenameExtension(filename)) ||
							inputResource instanceof EncodedResourceResolver.EncodedResource ||
							inputResource instanceof GzipResourceResolver.GzippedResource) {
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
								// 将字符缓冲区转换为 CSS 内容，并进行进一步的转换操作
								String cssContent = charBuffer.toString();
								return transformContent(cssContent, outputResource, transformerChain, exchange);
							});
				});
	}

	/**
	 * 转换 CSS 内容中的链接。
	 *
	 * @param cssContent   CSS 内容
	 * @param resource     原始资源
	 * @param chain        资源转换器链
	 * @param exchange     当前服务器交换对象
	 * @return 转换后的资源 Mono 对象
	 */
	private Mono<? extends Resource> transformContent(String cssContent, Resource resource,
													  ResourceTransformerChain chain, ServerWebExchange exchange) {

		// 解析 CSS 内容生成内容块信息列表
		List<ContentChunkInfo> contentChunkInfos = parseContent(cssContent);
		// 如果内容块信息列表为空，直接返回原始资源的 Mono
		if (contentChunkInfos.isEmpty()) {
			return Mono.just(resource);
		}

		// 对内容块信息列表进行处理
		return Flux.fromIterable(contentChunkInfos)
				.concatMap(contentChunkInfo -> {
					// 获取内容块信息对应的内容
					String contentChunk = contentChunkInfo.getContent(cssContent);
					// 如果是链接并且没有指定协议，将链接转换为绝对路径
					if (contentChunkInfo.isLink() && !hasScheme(contentChunk)) {
						String link = toAbsolutePath(contentChunk, exchange);
						return resolveUrlPath(link, exchange, resource, chain).defaultIfEmpty(contentChunk);
					} else {
						// 否则直接返回内容块
						return Mono.just(contentChunk);
					}
				})
				// 将处理后的内容块组合为一个字符串
				.reduce(new StringWriter(), (writer, chunk) -> {
					writer.write(chunk);
					return writer;
				})
				// 将组合后的字符串转换为字节数组，并构建 TransformedResource 对象
				.map(writer -> {
					byte[] newContent = writer.toString().getBytes(DEFAULT_CHARSET);
					return new TransformedResource(resource, newContent);
				});
	}


	/**
	 * 解析 CSS 内容，提取内容块信息。
	 *
	 * @param cssContent CSS 内容
	 * @return 解析出的内容块信息列表
	 */
	private List<ContentChunkInfo> parseContent(String cssContent) {
		// 创建一个按顺序排列的内容块信息的集合
		SortedSet<ContentChunkInfo> links = new TreeSet<>();
		// 使用每个链接解析器解析 CSS 内容并将结果添加到 links 集合中
		this.linkParsers.forEach(parser -> parser.parse(cssContent, links));

		// 如果 links 集合为空，返回空列表
		if (links.isEmpty()) {
			return Collections.emptyList();
		}

		// 处理 links 集合中的内容块信息，生成一个新的列表
		int index = 0;
		List<ContentChunkInfo> result = new ArrayList<>();
		for (ContentChunkInfo link : links) {
			// 将内容块信息按顺序添加到结果列表中
			result.add(new ContentChunkInfo(index, link.getStart(), false));
			result.add(link);
			index = link.getEnd();
		}

		// 如果最后一个内容块的结束位置小于 CSS 内容的长度，添加剩余的内容块信息到结果列表中
		if (index < cssContent.length()) {
			result.add(new ContentChunkInfo(index, cssContent.length(), false));
		}

		// 返回处理后的内容块信息列表
		return result;

	}

	/**
	 * 检查链接是否包含 URL 方案（Scheme）。
	 *
	 * @param link 要检查的链接
	 * @return 如果链接包含 URL 方案，则返回 {@code true}，否则返回 {@code false}
	 */
	private boolean hasScheme(String link) {
		int schemeIndex = link.indexOf(':');
		return (schemeIndex > 0 && !link.substring(0, schemeIndex).contains("/")) || link.indexOf("//") == 0;
	}


	/**
	 * {@code LinkParser} 接口用于提取代表链接的内容块。
	 */
	@FunctionalInterface
	protected interface LinkParser {

		/**
		 * 解析 CSS 内容，提取内容块信息。
		 *
		 * @param cssContent CSS 内容
		 * @param result     解析结果集合
		 */
		void parse(String cssContent, SortedSet<ContentChunkInfo> result);

	}


	/**
	 * {@code AbstractLinkParser} 是 {@link LinkParser} 实现的抽象基类。
	 */
	protected abstract static class AbstractLinkParser implements LinkParser {

		/**
		 * 获取用于搜索链接的关键字，例如 "@import"、"url("
		 *
		 * @return 用于搜索链接的关键字
		 */
		protected abstract String getKeyword();

		/**
		 * 解析内容中的链接。
		 *
		 * @param content 待解析的内容
		 * @param result  解析结果集合
		 */
		@Override
		public void parse(String content, SortedSet<ContentChunkInfo> result) {
			int position = 0;
			while (true) {
				position = content.indexOf(getKeyword(), position);
				if (position == -1) {
					return;
				}
				position += getKeyword().length();
				while (Character.isWhitespace(content.charAt(position))) {
					position++;
				}
				if (content.charAt(position) == '\'') {
					position = extractLink(position, '\'', content, result);
				} else if (content.charAt(position) == '"') {
					position = extractLink(position, '"', content, result);
				} else {
					position = extractUnquotedLink(position, content, result);
				}
			}
		}

		/**
		 * 提取链接信息。
		 *
		 * @param index   当前索引
		 * @param endChar 结束字符
		 * @param content 待处理的内容
		 * @param result  结果集合
		 * @return 下一个索引
		 */
		protected int extractLink(int index, char endChar, String content, Set<ContentChunkInfo> result) {
			int start = index + 1;
			int end = content.indexOf(endChar, start);
			result.add(new ContentChunkInfo(start, end, true));
			return end + 1;
		}

		/**
		 * 在关键字匹配后、移除空白字符后，并且下一个字符既不是单引号也不是双引号时调用。
		 *
		 * @param position   当前位置
		 * @param content    待处理的内容
		 * @param linksToAdd 待添加的链接集合
		 * @return 下一个位置
		 */
		protected abstract int extractUnquotedLink(int position, String content,
												   Set<ContentChunkInfo> linksToAdd);
	}

	/**
	 * {@code ImportLinkParser} 是一个私有静态内部类，继承自 {@link AbstractLinkParser}。
	 * 用于解析包含 "@import" 关键字的链接。
	 */
	private static class ImportLinkParser extends AbstractLinkParser {

		/**
		 * 获取链接关键字。
		 *
		 * @return 关键字 "@import"
		 */
		@Override
		protected String getKeyword() {
			return "@import";
		}

		/**
		 * 从内容中提取非引号链接。
		 *
		 * @param position 当前位置
		 * @param content  待处理的内容
		 * @param result   结果集合
		 * @return 当前位置
		 */
		@Override
		protected int extractUnquotedLink(int position, String content, Set<ContentChunkInfo> result) {
			if (content.startsWith("url(", position)) {
				// 忽略：由 UrlFunctionLinkParser 处理
			} else if (logger.isTraceEnabled()) {
				logger.trace("Unexpected syntax for @import link at index " + position);
			}
			return position;
		}
	}


	/**
	 * {@code UrlFunctionLinkParser} 是一个私有静态内部类，继承自 {@link AbstractLinkParser}。
	 * 用于解析包含 "url(" 关键字的链接。
	 */
	private static class UrlFunctionLinkParser extends AbstractLinkParser {

		/**
		 * 获取链接关键字。
		 *
		 * @return 关键字 "url("
		 */
		@Override
		protected String getKeyword() {
			return "url(";
		}

		/**
		 * 从内容中提取非引号链接。
		 *
		 * @param position 当前位置
		 * @param content  待处理的内容
		 * @param result   结果集合
		 * @return 下一个位置
		 */
		@Override
		protected int extractUnquotedLink(int position, String content, Set<ContentChunkInfo> result) {
			// 处理不带引号的 url() 函数
			return extractLink(position - 1, ')', content, result);
		}
	}


	/**
	 * {@code ContentChunkInfo} 是一个实现了 {@link Comparable} 接口的私有静态内部类。
	 * 用于表示内容块的信息，包括起始位置、结束位置以及是否是链接。
	 */
	private static class ContentChunkInfo implements Comparable<ContentChunkInfo> {

		/**
		 * 内容块起始位置
		 */
		private final int start;
		/**
		 * 内容块结束位置
		 */
		private final int end;
		/**
		 * 是否是链接
		 */
		private final boolean isLink;

		/**
		 * 创建一个 ContentChunkInfo 实例。
		 *
		 * @param start  内容块起始位置
		 * @param end    内容块结束位置
		 * @param isLink 内容块是否是链接
		 */
		ContentChunkInfo(int start, int end, boolean isLink) {
			this.start = start;
			this.end = end;
			this.isLink = isLink;
		}

		/**
		 * 获取内容块起始位置。
		 *
		 * @return 内容块起始位置
		 */
		public int getStart() {
			return this.start;
		}

		/**
		 * 获取内容块结束位置。
		 *
		 * @return 内容块结束位置
		 */
		public int getEnd() {
			return this.end;
		}

		/**
		 * 检查内容块是否是链接。
		 *
		 * @return 是否是链接
		 */
		public boolean isLink() {
			return this.isLink;
		}

		/**
		 * 根据传入的完整内容获取内容块的内容。
		 *
		 * @param fullContent 完整的内容
		 * @return 内容块的内容
		 */
		public String getContent(String fullContent) {
			return fullContent.substring(this.start, this.end);
		}

		/**
		 * 实现了 Comparable 接口的比较方法，比较两个内容块的起始位置。
		 *
		 * @param other 另一个 ContentChunkInfo 对象
		 * @return 比较结果
		 */
		@Override
		public int compareTo(ContentChunkInfo other) {
			return Integer.compare(this.start, other.start);
		}

		/**
		 * 实现了 equals 方法，用于判断两个内容块是否相等。
		 *
		 * @param other 要比较的对象
		 * @return 是否相等
		 */
		@Override
		public boolean equals(@Nullable Object other) {
			if (this == other) {
				return true;
			}
			if (!(other instanceof ContentChunkInfo)) {
				return false;
			}
			ContentChunkInfo otherCci = (ContentChunkInfo) other;
			return (this.start == otherCci.start && this.end == otherCci.end);
		}

		/**
		 * 实现了 hashCode 方法，返回内容块的哈希值。
		 *
		 * @return 哈希值
		 */
		@Override
		public int hashCode() {
			return this.start * 31 + this.end;
		}
	}

}
