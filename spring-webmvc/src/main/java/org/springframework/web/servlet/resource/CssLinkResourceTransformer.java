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

package org.springframework.web.servlet.resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * 一个实现了ResourceTransformer接口的类，用于修改CSS文件中的链接，
 * 使其匹配应该公开给客户端的公共URL路径（例如，带有MD5内容基于哈希的URL插入）。
 * <p>
 * 该实现查找CSS {@code @import}语句和CSS {@code url()}函数中的链接。
 * 所有链接都通过ResourceResolverChain进行解析，并相对于包含CSS文件的位置进行解析。如果成功解析，链接将被修改，否则原始链接将被保留。
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public class CssLinkResourceTransformer extends ResourceTransformerSupport {

	/**
	 * 默认字符集
	 */
	private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

	/**
	 * 日志记录器
	 */
	private static final Log logger = LogFactory.getLog(CssLinkResourceTransformer.class);

	/**
	 * 链接解析器列表
	 */
	private final List<LinkParser> linkParsers = new ArrayList<>(2);


	public CssLinkResourceTransformer() {
		this.linkParsers.add(new ImportStatementLinkParser());
		this.linkParsers.add(new UrlFunctionLinkParser());
	}


	@SuppressWarnings("deprecation")
	@Override
	public Resource transform(HttpServletRequest request, Resource resource, ResourceTransformerChain transformerChain)
			throws IOException {

		// 使用 转换器链 对请求和资源进行转换，并更新资源
		resource = transformerChain.transform(request, resource);

		// 获取资源的文件名
		String filename = resource.getFilename();
		// 判断文件是否为CSS文件，或者资源是否为EncodedResourceResolver.EncodedResource或GzipResourceResolver.GzippedResource类型
		if (!"css".equals(StringUtils.getFilenameExtension(filename)) ||
				resource instanceof EncodedResourceResolver.EncodedResource ||
				resource instanceof GzipResourceResolver.GzippedResource) {
			// 如果不是CSS文件或特定类型的资源，则直接返回原始资源
			return resource;
		}

		// 将资源的内容复制到字节数组中
		byte[] bytes = FileCopyUtils.copyToByteArray(resource.getInputStream());
		// 将字节数组转换为字符串，使用默认字符集
		String content = new String(bytes, DEFAULT_CHARSET);

		// 创建一个有序集合，用于存储解析出的内容块信息
		SortedSet<ContentChunkInfo> links = new TreeSet<>();
		// 遍历所有的链接解析器
		for (LinkParser parser : this.linkParsers) {
			// 使用解析器解析内容，并将解析结果添加到links集合中
			parser.parse(content, links);
		}

		// 如果 解析出的内容块信息 为空，则直接返回原始资源
		if (links.isEmpty()) {
			return resource;
		}

		// 初始化索引值为0
		int index = 0;
		// 创建一个StringWriter对象，用于写入处理后的内容
		StringWriter writer = new StringWriter();
		// 遍历 解析出的内容块信息集合 中的每个内容块信息
		for (ContentChunkInfo linkContentChunkInfo : links) {
			// 将未处理的内容写入writer
			writer.write(content.substring(index, linkContentChunkInfo.getStart()));
			// 获取当前内容块的链接部分
			String link = content.substring(linkContentChunkInfo.getStart(), linkContentChunkInfo.getEnd());
			// 初始化新链接为null
			String newLink = null;
			// 判断链接是否包含协议（如http://）
			if (!hasScheme(link)) {
				// 将相对路径转换为绝对路径
				String absolutePath = toAbsolutePath(link, request);
				// 解析URL路径，获取新的链接
				newLink = resolveUrlPath(absolutePath, request, resource, transformerChain);
			}
			// 将新链接写入writer，如果新链接为null，则写入原始链接
			writer.write(newLink != null ? newLink : link);
			// 更新索引值
			index = linkContentChunkInfo.getEnd();
		}
		// 将剩余未处理的内容写入writer
		writer.write(content.substring(index));

		// 返回一个新的TransformedResource对象，其中包含处理后的内容
		return new TransformedResource(resource, writer.toString().getBytes(DEFAULT_CHARSET));
	}

	/**
	 * hasScheme方法，判断链接是否有协议
	 */
	private boolean hasScheme(String link) {
		// 获取链接中冒号的位置
		int schemeIndex = link.indexOf(':');

		// 判断链接是否以协议名开头，且协议名前面没有斜杠，或者链接以双斜杠开头
		return ((schemeIndex > 0 && !link.substring(0, schemeIndex).contains("/")) || link.indexOf("//") == 0);
	}


	/**
	 * 提取表示链接的内容块。
	 */
	@FunctionalInterface
	protected interface LinkParser {

		void parse(String content, SortedSet<ContentChunkInfo> result);

	}


	/**
	 * AbstractLinkParser抽象类，实现LinkParser接口，提供关键字搜索和链接提取功能
	 */
	protected abstract static class AbstractLinkParser implements LinkParser {

		/**
		 * 返回用于搜索链接的关键字，例如 "@import"、"url("
		 */
		protected abstract String getKeyword();

		@Override
		public void parse(String content, SortedSet<ContentChunkInfo> result) {
			// 初始化位置变量为0
			int position = 0;
			// 无限循环，直到找到关键字或返回
			while (true) {
				// 查找关键字的位置，从当前位置开始
				position = content.indexOf(getKeyword(), position);
				// 如果找不到关键字，结束函数
				if (position == -1) {
					return;
				}
				// 将位置移动到关键字之后
				position += getKeyword().length();
				// 跳过空白字符
				while (Character.isWhitespace(content.charAt(position))) {
					position++;
				}
				// 如果遇到单引号，提取链接并更新位置
				if (content.charAt(position) == '\'') {
					position = extractLink(position, "'", content, result);
				} else if (content.charAt(position) == '"') {
					// 如果遇到双引号，提取链接并更新位置
					position = extractLink(position, "\"", content, result);
				} else {
					// 否则，提取链接并更新位置
					position = extractLink(position, content, result);
				}
			}
		}

		protected int extractLink(int index, String endKey, String content, SortedSet<ContentChunkInfo> linksToAdd) {
			// 定义一个变量start，其值为index加1
			int start = index + 1;

			// 定义一个变量end，其值为从start位置开始查找endKey在content中的位置
			int end = content.indexOf(endKey, start);

			// 将一个新的ContentChunkInfo对象添加到linksToAdd列表中，该对象的起始位置为start，结束位置为end
			linksToAdd.add(new ContentChunkInfo(start, end));

			// 返回end加上endKey的长度
			return end + endKey.length();
		}

		/**
		 * 在关键字匹配后，在删除空白字符后，以及下一个字符既不是单引号也不是双引号时调用。
		 */
		protected abstract int extractLink(int index, String content, SortedSet<ContentChunkInfo> linksToAdd);
	}

	/**
	 * ImportStatementLinkParser类，继承自AbstractLinkParser，处理@import语句中的链接
	 */
	private static class ImportStatementLinkParser extends AbstractLinkParser {

		@Override
		protected String getKeyword() {
			return "@import";
		}

		@Override
		protected int extractLink(int index, String content, SortedSet<ContentChunkInfo> linksToAdd) {
			if (content.startsWith("url(", index)) {
				// 如果从索引index开始的内容以"url("开头，则忽略此部分，因为UrlFunctionLinkParser会处理它
			} else if (logger.isTraceEnabled()) {
				// 如果日志记录器的跟踪级别日志已启用，则记录一条关于在索引index处遇到@import链接的意外语法的跟踪日志
				logger.trace("Unexpected syntax for @import link at index " + index);
			}
			// 返回索引index
			return index;
		}
	}

	/**
	 * UrlFunctionLinkParser类，继承自AbstractLinkParser，处理url()函数中的链接
	 */
	private static class UrlFunctionLinkParser extends AbstractLinkParser {

		@Override
		protected String getKeyword() {
			return "url(";
		}

		@Override
		protected int extractLink(int index, String content, SortedSet<ContentChunkInfo> linksToAdd) {
			//不带引号的url() 函数
			return extractLink(index - 1, ")", content, linksToAdd);
		}
	}

	/**
	 * ContentChunkInfo类，表示内容块信息
	 */
	private static class ContentChunkInfo implements Comparable<ContentChunkInfo> {

		/**
		 * 开始位置
		 */
		private final int start;

		/**
		 * 结束位置
		 */
		private final int end;

		ContentChunkInfo(int start, int end) {
			this.start = start;
			this.end = end;
		}

		public int getStart() {
			return this.start;
		}

		public int getEnd() {
			return this.end;
		}

		@Override
		public int compareTo(ContentChunkInfo other) {
			return Integer.compare(this.start, other.start);
		}

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

		@Override
		public int hashCode() {
			return this.start * 31 + this.end;
		}
	}

}
