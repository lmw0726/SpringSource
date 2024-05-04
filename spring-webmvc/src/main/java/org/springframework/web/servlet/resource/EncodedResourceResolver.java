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

import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.*;

/**
 * 解析器委托给链，如果找到资源，然后尝试查找可接受的编码（例如 gzip、brotli）变体，
 * 这是基于 "Accept-Encoding" 请求头进行的。
 *
 * <p>可以配置支持的 {@link #setContentCodings(List) contentCodings} 列表，按优先顺序排列，
 * 每个编码必须与 {@link #setExtensions(Map) extensions} 相关联。
 *
 * <p>请注意，此解析器必须在基于内容的版本策略的 {@link VersionResourceResolver} 之前有序，
 * 以确保版本计算不受编码的影响。
 *
 * @author Rossen Stoyanchev
 * @since 5.1
 */
public class EncodedResourceResolver extends AbstractResourceResolver {

	/**
	 * 默认的内容编码列表。
	 */
	public static final List<String> DEFAULT_CODINGS = Arrays.asList("br", "gzip");

	/**
	 * 内容编码列表
	 */
	private final List<String> contentCodings = new ArrayList<>(DEFAULT_CODINGS);

	/**
	 * 扩展名 和 带有.的扩展名映射
	 */
	private final Map<String, String> extensions = new LinkedHashMap<>();


	public EncodedResourceResolver() {
		this.extensions.put("gzip", ".gz");
		this.extensions.put("br", ".br");
	}


	/**
	 * 配置支持的内容编码列表，按优先顺序排列。对于给定的请求，{@literal "Accept-Encoding"} 头中存在的
	 * 第一个编码，且具有与关联扩展名的文件存在的编码，将被使用。
	 * <p><strong>注意：</strong>每个编码必须通过 {@link #registerExtension} 或 {@link #setExtensions}
	 * 关联到文件扩展名。此处对编码列表的自定义应与 {@link CachingResourceResolver} 中相同列表的自定义匹配，
	 * 以确保资源的编码变体被缓存在不同的键下。
	 * <p>默认情况下，此属性设置为 {@literal ["br", "gzip"]}。
	 *
	 * @param codings 支持的一个或多个内容编码
	 */
	public void setContentCodings(List<String> codings) {
		Assert.notEmpty(codings, "At least one content coding expected");
		this.contentCodings.clear();
		this.contentCodings.addAll(codings);
	}

	/**
	 * 返回支持的内容编码的只读列表。
	 */
	public List<String> getContentCodings() {
		return Collections.unmodifiableList(this.contentCodings);
	}

	/**
	 * 配置内容编码到文件扩展名的映射。如果扩展名不存在，则会在扩展名值前面添加一个点 "."。
	 * <p>默认情况下，这是配置的 {@literal ["br" -> ".br"]} 和 {@literal ["gzip" -> ".gz"]}。
	 *
	 * @param extensions 要使用的扩展名
	 * @see #registerExtension(String, String)
	 */
	public void setExtensions(Map<String, String> extensions) {
		extensions.forEach(this::registerExtension);
	}

	/**
	 * 返回编码到扩展名的只读映射。
	 */
	public Map<String, String> getExtensions() {
		return Collections.unmodifiableMap(this.extensions);
	}

	/**
	 * {@link #setExtensions(Map)} 的 Java 配置友好的替代方法。
	 *
	 * @param coding    内容编码
	 * @param extension 关联的文件扩展名
	 */
	public void registerExtension(String coding, String extension) {
		this.extensions.put(coding, (extension.startsWith(".") ? extension : "." + extension));
	}


	@Override
	protected Resource resolveResourceInternal(@Nullable HttpServletRequest request, String requestPath,
											   List<? extends Resource> locations, ResourceResolverChain chain) {

		// 解析资源
		Resource resource = chain.resolveResource(request, requestPath, locations);
		// 如果资源为空或请求为空，则直接返回资源
		if (resource == null || request == null) {
			return resource;
		}

		// 获取请求中的 Accept-Encoding 头部信息
		String acceptEncoding = getAcceptEncoding(request);
		// 如果 Accept-Encoding 为空，则直接返回资源
		if (acceptEncoding == null) {
			return resource;
		}

		// 遍历所有的编码方式
		for (String coding : this.contentCodings) {
			// 如果请求中包含当前编码方式
			if (acceptEncoding.contains(coding)) {
				try {
					// 获取编码方式对应的文件扩展名
					String extension = getExtension(coding);
					// 创建编码后的资源
					Resource encoded = new EncodedResource(resource, coding, extension);
					// 如果编码后的资源存在，则返回该资源
					if (encoded.exists()) {
						return encoded;
					}
				} catch (IOException ex) {
					// 如果发生异常，则记录日志，继续遍历下一个编码方式
					if (logger.isTraceEnabled()) {
						logger.trace("No " + coding + " resource for [" + resource.getFilename() + "]", ex);
					}
				}
			}
		}

		// 如果未找到符合条件的编码资源，则直接返回原始资源
		return resource;
	}

	@Nullable
	private String getAcceptEncoding(HttpServletRequest request) {
		// 获取请求中的 Accept-Encoding 头部信息
		String header = request.getHeader(HttpHeaders.ACCEPT_ENCODING);
		// 转换为小写字母形式返回
		return (header != null ? header.toLowerCase() : null);
	}

	private String getExtension(String coding) {
		String extension = this.extensions.get(coding);
		if (extension == null) {
			throw new IllegalStateException("No file extension associated with content coding " + coding);
		}
		return extension;
	}

	@Override
	protected String resolveUrlPathInternal(String resourceUrlPath,
											List<? extends Resource> locations, ResourceResolverChain chain) {

		return chain.resolveUrlPath(resourceUrlPath, locations);
	}


	/**
	 * 编码过的 {@link HttpResource}。
	 */
	static final class EncodedResource extends AbstractResource implements HttpResource {

		/**
		 * 原始资源
		 */
		private final Resource original;

		/**
		 * 编码方式
		 */
		private final String coding;

		/**
		 * 经过编码后的资源
		 */
		private final Resource encoded;

		/**
		 * 构造一个新的EncodedResource实例。
		 *
		 * @param original  原始资源
		 * @param coding    编码方式
		 * @param extension 扩展名
		 * @throws IOException 如果创建资源失败
		 */
		EncodedResource(Resource original, String coding, String extension) throws IOException {
			this.original = original;
			this.coding = coding;
			this.encoded = original.createRelative(original.getFilename() + extension);
		}

		@Override
		public InputStream getInputStream() throws IOException {
			return this.encoded.getInputStream();
		}

		@Override
		public boolean exists() {
			return this.encoded.exists();
		}

		@Override
		public boolean isReadable() {
			return this.encoded.isReadable();
		}

		@Override
		public boolean isOpen() {
			return this.encoded.isOpen();
		}

		@Override
		public boolean isFile() {
			return this.encoded.isFile();
		}

		@Override
		public URL getURL() throws IOException {
			return this.encoded.getURL();
		}

		@Override
		public URI getURI() throws IOException {
			return this.encoded.getURI();
		}

		@Override
		public File getFile() throws IOException {
			return this.encoded.getFile();
		}

		@Override
		public long contentLength() throws IOException {
			return this.encoded.contentLength();
		}

		@Override
		public long lastModified() throws IOException {
			return this.encoded.lastModified();
		}

		@Override
		public Resource createRelative(String relativePath) throws IOException {
			return this.encoded.createRelative(relativePath);
		}

		@Override
		@Nullable
		public String getFilename() {
			return this.original.getFilename();
		}

		@Override
		public String getDescription() {
			return this.encoded.getDescription();
		}

		@Override
		public HttpHeaders getResponseHeaders() {
			// 创建HttpHeaders对象
			HttpHeaders headers;
			if (this.original instanceof HttpResource) {
				// 如果原始对象是HttpResource，则获取其响应头部
				headers = ((HttpResource) this.original).getResponseHeaders();
			} else {
				// 否则创建一个空的HttpHeaders对象
				headers = new HttpHeaders();
			}
			// 添加Content-Encoding头部，值为当前的编码方式
			headers.add(HttpHeaders.CONTENT_ENCODING, this.coding);
			// 添加Vary头部，值为 Accept-Encoding
			headers.add(HttpHeaders.VARY, HttpHeaders.ACCEPT_ENCODING);

			// 返回构建好的HttpHeaders对象
			return headers;
		}
	}

}
