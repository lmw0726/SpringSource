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

import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.*;

/**
 * 该解析器委托给链，在找到资源后，尝试找到一个经过编码（例如gzip、brotli）的变体，根据请求的“Accept-Encoding”头部确定是否接受。
 *
 * <p>可以配置支持的{@link #setContentCodings(List) contentCodings}列表，按优先级排列，并且每种编码必须与{@link #setExtensions(Map) extensions}关联。
 *
 * <p>请注意，为了确保版本计算不受编码影响，此解析器必须在具有基于内容的版本策略的{@link VersionResourceResolver}之前排序。
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
	 * 默认编码列表
	 */
	private final List<String> contentCodings = new ArrayList<>(DEFAULT_CODINGS);

	/**
	 * 扩展名映射
	 */
	private final Map<String, String> extensions = new LinkedHashMap<>();


	public EncodedResourceResolver() {
		this.extensions.put("gzip", ".gz");
		this.extensions.put("br", ".br");
	}


	/**
	 * 配置支持的内容编码列表，按优先级排列。对于给定请求的“Accept-Encoding”头部中存在的第一个编码，且具有与其关联的文件扩展名的情况下，将使用此编码。
	 *
	 * <p><strong>注意：</strong> 每种编码都必须通过 {@link #registerExtension} 或 {@link #setExtensions} 关联到文件扩展名。
	 * 此处对编码列表的自定义应与 {@link CachingResourceResolver} 中的相同列表的自定义相匹配，以确保资源的编码变体被缓存在单独的键下。
	 *
	 * <p>默认情况下，此属性设置为 {@literal ["br", "gzip"]}。
	 *
	 * @param codings 一个或多个支持的内容编码
	 */
	public void setContentCodings(List<String> codings) {
		Assert.notEmpty(codings, "At least one content coding expected");
		this.contentCodings.clear();
		this.contentCodings.addAll(codings);
	}

	/**
	 * 返回只读的支持的内容编码列表。
	 */
	public List<String> getContentCodings() {
		return Collections.unmodifiableList(this.contentCodings);
	}

	/**
	 * 配置内容编码到文件扩展名的映射。如果未指定扩展名，则点“.”将会在扩展名值之前添加。
	 *
	 * <p>默认情况下，配置为 {@literal ["br" -> ".br"]} 和 {@literal ["gzip" -> ".gz"]}。
	 *
	 * @param extensions 要使用的扩展名
	 * @see #registerExtension(String, String)
	 */
	public void setExtensions(Map<String, String> extensions) {
		extensions.forEach(this::registerExtension);
	}

	/**
	 * 返回只读的编码到扩展名的映射。
	 */
	public Map<String, String> getExtensions() {
		return Collections.unmodifiableMap(this.extensions);
	}

	/**
	 * 与 {@link #setExtensions(Map)} 相似，适用于 Java 配置。
	 *
	 * @param coding    内容编码
	 * @param extension 关联的文件扩展名
	 */
	public void registerExtension(String coding, String extension) {
		this.extensions.put(coding, (extension.startsWith(".") ? extension : "." + extension));
	}

	/**
	 * 解析资源路径。
	 *
	 * @param exchange    可能的服务器网络交换
	 * @param requestPath 请求的路径
	 * @param locations   资源的位置列表
	 * @param chain       资源解析链
	 * @return 包含解析的资源的{@link Mono}
	 */
	@Override
	protected Mono<Resource> resolveResourceInternal(@Nullable ServerWebExchange exchange,
													 String requestPath, List<? extends Resource> locations, ResourceResolverChain chain) {

		return chain.resolveResource(exchange, requestPath, locations).map(resource -> {

			// 如果exchange为null，则直接返回原始资源
			if (exchange == null) {
				return resource;
			}

			// 获取Accept-Encoding头信息
			String acceptEncoding = getAcceptEncoding(exchange);

			// 如果Accept-Encoding为null，则直接返回原始资源
			if (acceptEncoding == null) {
				return resource;
			}

			// 遍历支持的编码类型
			for (String coding : this.contentCodings) {
				// 如果Accept-Encoding包含当前编码类型
				if (acceptEncoding.contains(coding)) {
					try {
						// 获取编码类型对应的文件扩展名
						String extension = getExtension(coding);

						// 将资源编码为指定的编码类型
						Resource encoded = new EncodedResource(resource, coding, extension);

						// 如果编码后的资源存在，则返回编码后的资源
						if (encoded.exists()) {
							return encoded;
						}
					} catch (IOException ex) {
						// 记录异常信息，但不影响程序继续执行
						logger.trace(exchange.getLogPrefix() +
								"No " + coding + " resource for [" + resource.getFilename() + "]", ex);
					}
				}
			}

			// 如果没有找到合适的编码资源，则返回原始资源
			return resource;
		});
	}

	/**
	 * 获取“Accept-Encoding”头部的编码。
	 *
	 * @param exchange 服务器网络交换
	 * @return 编码字符串或 null
	 */
	@Nullable
	private String getAcceptEncoding(ServerWebExchange exchange) {
		ServerHttpRequest request = exchange.getRequest();
		String header = request.getHeaders().getFirst(HttpHeaders.ACCEPT_ENCODING);
		return (header != null ? header.toLowerCase() : null);
	}

	/**
	 * 获取编码对应的文件扩展名。
	 *
	 * @param coding 编码名称
	 * @return 对应的文件扩展名
	 */
	private String getExtension(String coding) {
		String extension = this.extensions.get(coding);
		if (extension == null) {
			throw new IllegalStateException("No file extension associated with content coding " + coding);
		}
		return extension;
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
	 * 一种编码后的{@link HttpResource}.
	 */
	static final class EncodedResource extends AbstractResource implements HttpResource {
		/**
		 * 原始的资源
		 */
		private final Resource original;

		/**
		 * 编码类型
		 */
		private final String coding;

		/**
		 * 编码后的资源
		 */
		private final Resource encoded;

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
			HttpHeaders headers;
			if (this.original instanceof HttpResource) {
				// 如果原始对象是 HttpResource，则获取响应头
				headers = ((HttpResource) this.original).getResponseHeaders();
			} else {
				// 否则，创建一个新的 HttpHeaders 对象
				headers = new HttpHeaders();
			}
			// 添加内容编码和可变头信息
			headers.add(HttpHeaders.CONTENT_ENCODING, this.coding);
			headers.add(HttpHeaders.VARY, HttpHeaders.ACCEPT_ENCODING);
			return headers;
		}
	}

}
