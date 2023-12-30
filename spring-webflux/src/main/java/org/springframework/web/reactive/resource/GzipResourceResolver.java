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

package org.springframework.web.reactive.resource;

import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.List;

/**
 * 一个 {@code ResourceResolver}，它委托给链路定位资源，然后尝试查找带有“.gz”扩展名的变体。
 *
 * <p>仅当“Accept-Encoding”请求头包含值“gzip”时，客户端接受gzip响应时，解析器才会介入。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 * @deprecated 自 5.1 起，建议使用 {@link EncodedResourceResolver}
 */
@Deprecated
public class GzipResourceResolver extends AbstractResourceResolver {

	/**
	 * 解析资源内部方法。
	 *
	 * @param exchange     服务器网络交换，可以为 null
	 * @param requestPath  请求路径
	 * @param locations    资源位置列表
	 * @param chain        资源解析链
	 * @return 解析的资源
	 */
	@Override
	protected Mono<Resource> resolveResourceInternal(@Nullable ServerWebExchange exchange,
													 String requestPath, List<? extends Resource> locations, ResourceResolverChain chain) {

		// 返回解析后的资源（可能是链式解析的结果）
		return chain.resolveResource(exchange, requestPath, locations)
				.map(resource -> {
					// 如果 exchange 为空或者接受 Gzip 压缩
					if (exchange == null || isGzipAccepted(exchange)) {
						try {
							// 尝试使用 GzippedResource 对象包装资源
							Resource gzipped = new GzippedResource(resource);
							// 如果存在 Gzip 版本的资源，则使用它替换原始资源
							if (gzipped.exists()) {
								resource = gzipped;
							}
						} catch (IOException ex) {
							// 捕获可能的 IOException，并记录日志
							String logPrefix = exchange != null ? exchange.getLogPrefix() : "";
							logger.trace(logPrefix + "No gzip resource for [" + resource.getFilename() + "]", ex);
						}
					}
					// 返回经过处理的资源（可能是原始资源或者 Gzip 资源）
					return resource;
				});
	}

	/**
	 * 检查是否接受Gzip编码。
	 *
	 * @param exchange 服务器网络交换
	 * @return 是否接受Gzip编码
	 */
	private boolean isGzipAccepted(ServerWebExchange exchange) {
		String value = exchange.getRequest().getHeaders().getFirst("Accept-Encoding");
		return (value != null && value.toLowerCase().contains("gzip"));
	}

	/**
	 * 解析URL路径。
	 *
	 * @param resourceUrlPath 资源URL路径
	 * @param locations       资源位置列表
	 * @param chain           资源解析链
	 * @return 解析的URL路径
	 */
	@Override
	protected Mono<String> resolveUrlPathInternal(String resourceUrlPath,
												  List<? extends Resource> locations, ResourceResolverChain chain) {

		return chain.resolveUrlPath(resourceUrlPath, locations);
	}


	/**
	 * 一种gzip压缩后的 {@link HttpResource}.
	 */
	static final class GzippedResource extends AbstractResource implements HttpResource {
		/**
		 * 原始资源
		 */
		private final Resource original;

		/**
		 * 经过gzip压缩后的资源
		 */
		private final Resource gzipped;

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
			HttpHeaders headers = (this.original instanceof HttpResource ?
					// 如果原始对象是 HttpResource，则获取响应头，否则创建一个新的 HttpHeaders 对象
					((HttpResource) this.original).getResponseHeaders() : new HttpHeaders());
			// 添加内容编码和可变头信息
			headers.add(HttpHeaders.CONTENT_ENCODING, "gzip");
			headers.add(HttpHeaders.VARY, HttpHeaders.ACCEPT_ENCODING);
			return headers;
		}
	}

}
