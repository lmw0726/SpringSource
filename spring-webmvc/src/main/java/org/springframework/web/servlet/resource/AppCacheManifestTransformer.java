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

package org.springframework.web.servlet.resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.util.DigestUtils;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Scanner;

/**
 * 一个ResourceTransformer实现，用于处理HTML5离线应用程序中的AppCache清单。
 * <p>
 * 这个转换器主要完成以下任务：
 * <ul>
 * <li>修改链接以匹配应该暴露给客户端的公共URL路径，使用配置好的{@code ResourceResolver}策略</li>
 * <li>在清单中添加一条注释，包含一个哈希（例如："# Hash: 9de0f09ed7caf84e885f1f0f11c7e326"），从而改变清单的内容，以便触发浏览器的appcache重新加载。</li>
 * </ul>
 * <p>
 * 所有扩展名为".appcache"的文件，或者在构造函数中给出的扩展名，都将被这个类转换。
 * 这个哈希是根据appcache清单的内容和链接资源的内容计算的；因此，更改链接的资源或清单本身都应该使浏览器缓存失效。
 * <p>
 * 为了正确提供manifest文件，需要使用{@code contentNegotiationConfigurer.mediaType("appcache", MediaType.valueOf("text/manifest"))}在{@code WebMvcConfigurer}中进行配置。
 * <p>
 *
 * @author Brian Clozel
 * @see <a href="https://html.spec.whatwg.org/multipage/browsers.html#offline">HTML5离线应用程序规范</a>
 * @since 4.1
 * @deprecated as of 5.3 since browser support is going away
 */
@Deprecated
public class AppCacheManifestTransformer extends ResourceTransformerSupport {

	/**
	 * 清单标头
	 */
	private static final String MANIFEST_HEADER = "CACHE MANIFEST";

	/**
	 * 缓存标头
	 */
	private static final String CACHE_HEADER = "CACHE:";

	/**
	 * 清单节标头
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
	 * 创建一个AppCacheResourceTransformer，用于转换扩展名为".appcache"的文件。
	 */
	public AppCacheManifestTransformer() {
		this("appcache");
	}

	/**
	 * 创建一个AppCacheResourceTransformer，用于转换给定参数作为扩展名的文件。
	 *
	 * @param fileExtension 文件扩展名
	 */
	public AppCacheManifestTransformer(String fileExtension) {
		this.fileExtension = fileExtension;
	}


	@Override
	public Resource transform(HttpServletRequest request, Resource resource,
							  ResourceTransformerChain chain) throws IOException {

		// 将请求和资源进行转换，得到新的资源对象
		resource = chain.transform(request, resource);

		// 检查资源文件的扩展名是否与预期的文件扩展名相同
		if (!this.fileExtension.equals(StringUtils.getFilenameExtension(resource.getFilename()))) {
			// 如果不同，则直接返回原始资源对象
			return resource;
		}

		// 将资源对象的输入流复制到字节数组中
		byte[] bytes = FileCopyUtils.copyToByteArray(resource.getInputStream());

		// 将字节数组转换为字符串，使用默认字符集
		String content = new String(bytes, DEFAULT_CHARSET);

		// 检查内容是否以预期的头部开始
		if (!content.startsWith(MANIFEST_HEADER)) {
			// 如果不是，则记录日志并返回原始资源对象
			if (logger.isTraceEnabled()) {
				logger.trace("Skipping " + resource + ": Manifest does not start with 'CACHE MANIFEST'");
			}
			return resource;
		}

		// 创建扫描器对象，用于逐行读取内容
		@SuppressWarnings("resource")
		Scanner scanner = new Scanner(content);

		// 初始化前一行信息为空
		LineInfo previous = null;

		// 创建行聚合器对象，用于处理每一行的内容
		LineAggregator aggregator = new LineAggregator(resource, content);

		// 循环遍历每一行内容
		while (scanner.hasNext()) {
			// 读取当前行内容
			String line = scanner.nextLine();

			// 创建当前行信息对象，包含当前行内容和前一行信息
			LineInfo current = new LineInfo(line, previous);

			// 处理当前行内容，得到行输出对象
			LineOutput lineOutput = processLine(current, request, resource, chain);

			// 将行输出对象添加到聚合器中
			aggregator.add(lineOutput);

			// 更新前一行信息为当前行信息
			previous = current;
		}

		// 使用聚合器创建新的资源对象并返回
		return aggregator.createResource();
	}

	private static byte[] getResourceBytes(Resource resource) throws IOException {
		return FileCopyUtils.copyToByteArray(resource.getInputStream());
	}

	private LineOutput processLine(LineInfo info, HttpServletRequest request,
								   Resource resource, ResourceTransformerChain transformerChain) {

		// 判断info是否为链接
		if (!info.isLink()) {
			// 如果不是链接，直接返回一个新的LineOutput对象，其中包含info的行信息和null资源
			return new LineOutput(info.getLine(), null);
		}

		// 使用转换器的解析器链来解析info行对应的资源
		Resource appCacheResource = transformerChain.getResolverChain()
				.resolveResource(null, info.getLine(), Collections.singletonList(resource));

		// 获取info的行信息作为路径
		String path = info.getLine();
		// 将路径转换为绝对路径
		String absolutePath = toAbsolutePath(path, request);
		// 解析URL路径，获取新的路径
		String newPath = resolveUrlPath(absolutePath, request, resource, transformerChain);

		// 返回一个新的LineOutput对象，其中包含新路径（如果存在）或原始路径，以及解析出的应用缓存资源
		return new LineOutput((newPath != null ? newPath : path), appCacheResource);
	}


	private static class LineInfo {

		/**
		 * 一行数据
		 */
		private final String line;

		/**
		 * 是否是缓存部分
		 */
		private final boolean cacheSection;

		/**
		 * 是否是链接
		 */
		private final boolean link;

		public LineInfo(String line, @Nullable LineInfo previous) {
			this.line = line;
			this.cacheSection = initCacheSectionFlag(line, previous);
			this.link = iniLinkFlag(line, this.cacheSection);
		}

		private static boolean initCacheSectionFlag(String line, @Nullable LineInfo previousLine) {
			// 去除行首和行尾的空白字符
			String trimmedLine = line.trim();

			// 判断当前行是否为清单文件的头部
			if (MANIFEST_SECTION_HEADERS.contains(trimmedLine)) {
				// 如果当前行为缓存头部，则返回true
				return trimmedLine.equals(CACHE_HEADER);
			} else if (previousLine != null) {
				// 如果存在前一行，则判断前一行是否为缓存部分
				return previousLine.isCacheSection();
			}

			// 如果以上条件都不满足，抛出异常，表示清单文件不以指定的头部开始
			throw new IllegalStateException(
					"Manifest does not start with " + MANIFEST_HEADER + ": " + line);
		}

		private static boolean iniLinkFlag(String line, boolean isCacheSection) {
			return (isCacheSection && StringUtils.hasText(line) && !line.startsWith("#")
					&& !line.startsWith("//") && !hasScheme(line));
		}

		private static boolean hasScheme(String line) {
			int index = line.indexOf(':');
			return (line.startsWith("//") || (index > 0 && !line.substring(0, index).contains("/")));
		}

		public String getLine() {
			return this.line;
		}

		public boolean isCacheSection() {
			return this.cacheSection;
		}

		public boolean isLink() {
			return this.link;
		}
	}


	private static class LineOutput {

		/**
		 * 一行数据
		 */
		private final String line;

		/**
		 * 资源
		 */
		@Nullable
		private final Resource resource;

		public LineOutput(String line, @Nullable Resource resource) {
			this.line = line;
			this.resource = resource;
		}

		public String getLine() {
			return this.line;
		}

		@Nullable
		public Resource getResource() {
			return this.resource;
		}
	}


	private static class LineAggregator {

		/**
		 * 写入器
		 */
		private final StringWriter writer = new StringWriter();

		/**
		 * 字节数组输出流
		 */
		private final ByteArrayOutputStream baos;

		/**
		 * 资源
		 */
		private final Resource resource;

		public LineAggregator(Resource resource, String content) {
			this.resource = resource;
			this.baos = new ByteArrayOutputStream(content.length());
		}

		public void add(LineOutput lineOutput) throws IOException {
			// 将lineOutput的行内容写入输出流
			this.writer.write(lineOutput.getLine() + "\n");

			// 如果lineOutput的资源不为空，则计算资源的MD5摘要；否则，将行内容转换为字节数组
			byte[] bytes = (lineOutput.getResource() != null ?
					DigestUtils.md5Digest(getResourceBytes(lineOutput.getResource())) :
					lineOutput.getLine().getBytes(DEFAULT_CHARSET));

			// 将字节数组写入缓冲区
			this.baos.write(bytes);
		}

		public TransformedResource createResource() {
			// 使用DigestUtils的md5DigestAsHex方法，将字节数组转换为MD5哈希值，并将结果存储在字符串变量hash中
			String hash = DigestUtils.md5DigestAsHex(this.baos.toByteArray());

			// 将字符串"# Hash: "和哈希值拼接起来，然后写入writer对象
			this.writer.write("\n" + "# Hash: " + hash);

			// 将writer对象转换为字符串，然后将该字符串转换为字节数组
			byte[] bytes = this.writer.toString().getBytes(DEFAULT_CHARSET);

			// 创建一个新的TransformedResource对象，其中包含原始资源和转换后的字节数组，并返回这个新创建的对象
			return new TransformedResource(this.resource, bytes);
		}
	}

}
