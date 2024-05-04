/*
 * Copyright 2002-2018 the original author or authors.
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

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.List;

/**
 * 一个 {@code ResourceResolver}，委托给链来定位一个资源，然后尝试找到一个具有“.gz”扩展名的变体。
 *
 * <p>只有当“Accept-Encoding”请求头包含值“gzip”时，指示客户端接受gzip响应时，解析器才会介入。
 *
 * @author Jeremy Grelle
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 4.1
 * @deprecated 自5.1起，建议使用 {@link EncodedResourceResolver}
 */
@Deprecated
public class GzipResourceResolver extends AbstractResourceResolver {


	@Override
	protected Resource resolveResourceInternal(@Nullable HttpServletRequest request, String requestPath,
											   List<? extends Resource> locations, ResourceResolverChain chain) {

		// 解析资源
		Resource resource = chain.resolveResource(request, requestPath, locations);

		// 如果资源为null，或者请求不支持gzip压缩，则直接返回资源
		if (resource == null || (request != null && !isGzipAccepted(request))) {
			return resource;
		}

		try {
			// 尝试获取gzip压缩后的资源
			Resource gzipped = new GzippedResource(resource);
			// 如果gzip压缩后的资源存在，则返回gzip压缩后的资源
			if (gzipped.exists()) {
				return gzipped;
			}
		} catch (IOException ex) {
			// 记录日志，但不影响流程
			logger.trace("No gzip resource for [" + resource.getFilename() + "]", ex);
		}

		// 返回原始资源
		return resource;
	}

	private boolean isGzipAccepted(HttpServletRequest request) {
		// 获取请求头中的Accept-Encoding字段的值
		String value = request.getHeader("Accept-Encoding");

		// 检查值是否不为null，且包含"gzip"（不区分大小写）
		return (value != null && value.toLowerCase().contains("gzip"));
	}

	@Override
	protected String resolveUrlPathInternal(String resourceUrlPath,
											List<? extends Resource> locations, ResourceResolverChain chain) {

		return chain.resolveUrlPath(resourceUrlPath, locations);
	}


	/**
	 * 一个经过gzip压缩的 {@link HttpResource}。
	 */
	static final class GzippedResource extends AbstractResource implements HttpResource {

		/**
		 * 原始资源
		 */
		private final Resource original;

		/**
		 * 经过Gzip压缩后的资源
		 */
		private final Resource gzipped;

		/**
		 * 构造一个新的GzippedResource实例。
		 *
		 * @param original 原始资源
		 * @throws IOException 如果创建资源失败
		 */
		public GzippedResource(Resource original) throws IOException {
			this.original = original;
			this.gzipped = original.createRelative(original.getFilename() + ".gz");
		}

		@Override
		public InputStream getInputStream() throws IOException {
			return this.gzipped.getInputStream();
		}

		@Override
		public boolean exists() {
			return this.gzipped.exists();
		}

		@Override
		public boolean isReadable() {
			return this.gzipped.isReadable();
		}

		@Override
		public boolean isOpen() {
			return this.gzipped.isOpen();
		}

		@Override
		public boolean isFile() {
			return this.gzipped.isFile();
		}

		@Override
		public URL getURL() throws IOException {
			return this.gzipped.getURL();
		}

		@Override
		public URI getURI() throws IOException {
			return this.gzipped.getURI();
		}

		@Override
		public File getFile() throws IOException {
			return this.gzipped.getFile();
		}

		@Override
		public long contentLength() throws IOException {
			return this.gzipped.contentLength();
		}

		@Override
		public long lastModified() throws IOException {
			return this.gzipped.lastModified();
		}

		@Override
		public Resource createRelative(String relativePath) throws IOException {
			return this.gzipped.createRelative(relativePath);
		}

		@Override
		@Nullable
		public String getFilename() {
			return this.original.getFilename();
		}

		@Override
		public String getDescription() {
			return this.gzipped.getDescription();
		}

		@Override
		public HttpHeaders getResponseHeaders() {
			// 创建HttpHeaders对象
			HttpHeaders headers = (this.original instanceof HttpResource ?
					// 如果原始对象是HttpResource，则获取其响应头部；否则创建一个空的HttpHeaders对象
					((HttpResource) this.original).getResponseHeaders() : new HttpHeaders());
			// 添加Content-Encoding头部，值为gzip
			headers.add(HttpHeaders.CONTENT_ENCODING, "gzip");
			// 添加Vary头部，值为Accept-Encoding
			headers.add(HttpHeaders.VARY, HttpHeaders.ACCEPT_ENCODING);

			// 返回构建好的HttpHeaders对象
			return headers;
		}
	}

}
