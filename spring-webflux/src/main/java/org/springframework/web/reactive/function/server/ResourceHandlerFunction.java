/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.reactive.function.server;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.EnumSet;
import java.util.Set;

/**
 * 基于资源的 {@link HandlerFunction} 实现。
 *
 * @author Arjen Poutsma
 * @since 5.0
 */
class ResourceHandlerFunction implements HandlerFunction<ServerResponse> {

	/**
	 * 支持的 HTTP 方法集合
	 */
	private static final Set<HttpMethod> SUPPORTED_METHODS =
			EnumSet.of(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.OPTIONS);

	// 资源对象
	private final Resource resource;

	public ResourceHandlerFunction(Resource resource) {
		this.resource = resource;
	}

	@Override
	public Mono<ServerResponse> handle(ServerRequest request) {
		// 获取请求的 HTTP 方法
		HttpMethod method = request.method();

		if (method != null) {
			switch (method) {
				case GET:
					// 处理 GET 方法，返回资源实体的响应
					return EntityResponse.fromObject(this.resource).build()
							.map(response -> response);
				case HEAD:
					// 处理 HEAD 方法，返回资源的头部信息
					Resource headResource = new HeadMethodResource(this.resource);
					return EntityResponse.fromObject(headResource).build()
							.map(response -> response);
				case OPTIONS:
					// 处理 OPTIONS 方法，返回支持的方法列表
					return ServerResponse.ok()
							.allow(SUPPORTED_METHODS)
							.body(BodyInserters.empty());
			}
		}

		// 如果请求方法不支持，默认返回 METHOD_NOT_ALLOWED 状态
		return ServerResponse.status(HttpStatus.METHOD_NOT_ALLOWED)
				.allow(SUPPORTED_METHODS)
				.body(BodyInserters.empty());
	}


	/**
	 * 表示处理 HEAD 方法的资源。
	 */
	private static class HeadMethodResource implements Resource {

		/**
		 * 定义一个长度为 0 的字节数组常量
		 */
		private static final byte[] EMPTY = new byte[0];

		/**
		 * 声明一个 Resource 类型的委托字段
		 */
		private final Resource delegate;

		public HeadMethodResource(Resource delegate) {
			this.delegate = delegate;
		}

		@Override
		public InputStream getInputStream() throws IOException {
			return new ByteArrayInputStream(EMPTY);
		}

		// 委托方法

		@Override
		public boolean exists() {
			return this.delegate.exists();
		}

		@Override
		public URL getURL() throws IOException {
			return this.delegate.getURL();
		}

		@Override
		public URI getURI() throws IOException {
			return this.delegate.getURI();
		}

		@Override
		public File getFile() throws IOException {
			return this.delegate.getFile();
		}

		@Override
		public long contentLength() throws IOException {
			return this.delegate.contentLength();
		}

		@Override
		public long lastModified() throws IOException {
			return this.delegate.lastModified();
		}

		@Override
		public Resource createRelative(String relativePath) throws IOException {
			return this.delegate.createRelative(relativePath);
		}

		@Override
		@Nullable
		public String getFilename() {
			return this.delegate.getFilename();
		}

		@Override
		public String getDescription() {
			return this.delegate.getDescription();
		}
	}

}
