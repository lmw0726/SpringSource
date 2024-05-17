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

package org.springframework.web.servlet.function;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.ReadListener;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 默认的{@link ServerRequest.Builder}实现
 *
 * @author Arjen Poutsma
 * @since 5.2
 */
class DefaultServerRequestBuilder implements ServerRequest.Builder {
	/**
	 * Servlet请求
	 */
	private final HttpServletRequest servletRequest;

	/**
	 * 消息转换器列表
	 */
	private final List<HttpMessageConverter<?>> messageConverters;
	/**
	 * 方法名
	 */
	private String methodName;

	/**
	 * 请求URI
	 */
	private URI uri;

	/**
	 * 请求头
	 */
	private final HttpHeaders headers = new HttpHeaders();

	/**
	 * Cookie
	 */
	private final MultiValueMap<String, Cookie> cookies = new LinkedMultiValueMap<>();

	/**
	 * 属性值
	 */
	private final Map<String, Object> attributes = new LinkedHashMap<>();

	/**
	 * 参数值
	 */
	private final MultiValueMap<String, String> params = new LinkedMultiValueMap<>();

	/**
	 * 远程地址
	 */
	@Nullable
	private InetSocketAddress remoteAddress;

	/**
	 * 请求体
	 */
	private byte[] body = new byte[0];


	public DefaultServerRequestBuilder(ServerRequest other) {
		Assert.notNull(other, "ServerRequest must not be null");
		this.servletRequest = other.servletRequest();
		this.messageConverters = new ArrayList<>(other.messageConverters());
		this.methodName = other.methodName();
		this.uri = other.uri();
		headers(headers -> headers.addAll(other.headers().asHttpHeaders()));
		cookies(cookies -> cookies.addAll(other.cookies()));
		attributes(attributes -> attributes.putAll(other.attributes()));
		params(params -> params.addAll(other.params()));
		this.remoteAddress = other.remoteAddress().orElse(null);
	}

	@Override
	public ServerRequest.Builder method(HttpMethod method) {
		Assert.notNull(method, "HttpMethod must not be null");
		this.methodName = method.name();
		return this;
	}

	@Override
	public ServerRequest.Builder uri(URI uri) {
		Assert.notNull(uri, "URI must not be null");
		this.uri = uri;
		return this;
	}

	@Override
	public ServerRequest.Builder header(String headerName, String... headerValues) {
		for (String headerValue : headerValues) {
			this.headers.add(headerName, headerValue);
		}
		return this;
	}

	@Override
	public ServerRequest.Builder headers(Consumer<HttpHeaders> headersConsumer) {
		headersConsumer.accept(this.headers);
		return this;
	}

	@Override
	public ServerRequest.Builder cookie(String name, String... values) {
		for (String value : values) {
			this.cookies.add(name, new Cookie(name, value));
		}
		return this;
	}

	@Override
	public ServerRequest.Builder cookies(Consumer<MultiValueMap<String, Cookie>> cookiesConsumer) {
		cookiesConsumer.accept(this.cookies);
		return this;
	}

	@Override
	public ServerRequest.Builder body(byte[] body) {
		this.body = body;
		return this;
	}

	@Override
	public ServerRequest.Builder body(String body) {
		return body(body.getBytes(StandardCharsets.UTF_8));
	}

	@Override
	public ServerRequest.Builder attribute(String name, Object value) {
		Assert.notNull(name, "'name' must not be null");
		this.attributes.put(name, value);
		return this;
	}

	@Override
	public ServerRequest.Builder attributes(Consumer<Map<String, Object>> attributesConsumer) {
		attributesConsumer.accept(this.attributes);
		return this;
	}

	@Override
	public ServerRequest.Builder param(String name, String... values) {
		for (String value : values) {
			this.params.add(name, value);
		}
		return this;
	}

	@Override
	public ServerRequest.Builder params(Consumer<MultiValueMap<String, String>> paramsConsumer) {
		paramsConsumer.accept(this.params);
		return this;
	}

	@Override
	public ServerRequest.Builder remoteAddress(InetSocketAddress remoteAddress) {
		this.remoteAddress = remoteAddress;
		return this;
	}


	@Override
	public ServerRequest build() {
		return new BuiltServerRequest(this.servletRequest, this.methodName, this.uri, this.headers, this.cookies,
				this.attributes, this.params, this.remoteAddress, this.body, this.messageConverters);
	}


	private static class BuiltServerRequest implements ServerRequest {
		/**
		 * 方法名称
		 */
		private final String methodName;

		/**
		 * 请求URI
		 */
		private final URI uri;

		/**
		 * 请求头
		 */
		private final HttpHeaders headers;

		/**
		 * Servlet请求
		 */
		private final HttpServletRequest servletRequest;

		/**
		 * Cookie值
		 */
		private final MultiValueMap<String, Cookie> cookies;

		/**
		 * 属性值
		 */
		private final Map<String, Object> attributes;

		/**
		 * 请求体
		 */
		private final byte[] body;

		/**
		 * 消息转换器列表
		 */
		private final List<HttpMessageConverter<?>> messageConverters;

		/**
		 * 参数值
		 */
		private final MultiValueMap<String, String> params;

		/**
		 * 远程地址
		 */
		@Nullable
		private final InetSocketAddress remoteAddress;

		public BuiltServerRequest(HttpServletRequest servletRequest, String methodName, URI uri,
								  HttpHeaders headers, MultiValueMap<String, Cookie> cookies,
								  Map<String, Object> attributes, MultiValueMap<String, String> params,
								  @Nullable InetSocketAddress remoteAddress, byte[] body, List<HttpMessageConverter<?>> messageConverters) {

			this.servletRequest = servletRequest;
			this.methodName = methodName;
			this.uri = uri;
			this.headers = new HttpHeaders(headers);
			this.cookies = new LinkedMultiValueMap<>(cookies);
			this.attributes = new LinkedHashMap<>(attributes);
			this.params = new LinkedMultiValueMap<>(params);
			this.remoteAddress = remoteAddress;
			this.body = body;
			this.messageConverters = messageConverters;
		}

		@Override
		public String methodName() {
			return this.methodName;
		}

		@Override
		public MultiValueMap<String, Part> multipartData() throws IOException, ServletException {
			// 使用流对请求的所有Part进行分组，以Part的名称为键，值为相应的Part列表
			return servletRequest().getParts().stream()
					.collect(Collectors.groupingBy(
							// 以Part的名称为键
							Part::getName,
							// 使用LinkedMultiValueMap收集值，保持顺序
							LinkedMultiValueMap::new,
							// 将相同名称的Part收集到列表中
							Collectors.toList()));

		}

		@Override
		public URI uri() {
			return this.uri;
		}

		@Override
		public UriBuilder uriBuilder() {
			return UriComponentsBuilder.fromUri(this.uri);
		}

		@Override
		public Headers headers() {
			return new DefaultServerRequest.DefaultRequestHeaders(this.headers);
		}

		@Override
		public MultiValueMap<String, Cookie> cookies() {
			return this.cookies;
		}

		@Override
		public Optional<InetSocketAddress> remoteAddress() {
			return Optional.ofNullable(this.remoteAddress);
		}

		@Override
		public List<HttpMessageConverter<?>> messageConverters() {
			return this.messageConverters;
		}

		@Override
		public <T> T body(Class<T> bodyType) throws IOException, ServletException {
			return bodyInternal(bodyType, bodyType);
		}

		@Override
		public <T> T body(ParameterizedTypeReference<T> bodyType) throws IOException, ServletException {
			// 获取请求体类型
			Type type = bodyType.getType();
			// 根据请求体类型获取请求体类
			return bodyInternal(type, DefaultServerRequest.bodyClass(type));
		}

		@SuppressWarnings("unchecked")
		private <T> T bodyInternal(Type bodyType, Class<?> bodyClass) throws ServletException, IOException {
			// 创建一个内置的HttpInputMessage
			HttpInputMessage inputMessage = new BuiltInputMessage();

			// 获取请求的Content-Type，默认为APPLICATION_OCTET_STREAM
			MediaType contentType = headers().contentType().orElse(MediaType.APPLICATION_OCTET_STREAM);

			// 遍历消息转换器列表，尝试使用每个消息转换器读取请求体
			for (HttpMessageConverter<?> messageConverter : this.messageConverters) {
				// 如果消息转换器是泛型消息转换器
				if (messageConverter instanceof GenericHttpMessageConverter) {
					GenericHttpMessageConverter<T> genericMessageConverter =
							(GenericHttpMessageConverter<T>) messageConverter;
					// 如果泛型消息转换器支持读取给定的bodyType、bodyClass和contentType
					if (genericMessageConverter.canRead(bodyType, bodyClass, contentType)) {
						// 使用泛型消息转换器读取请求体并返回
						return genericMessageConverter.read(bodyType, bodyClass, inputMessage);
					}
				}
				// 如果消息转换器支持读取给定的bodyClass和contentType
				if (messageConverter.canRead(bodyClass, contentType)) {
					// 使用消息转换器读取请求体并返回
					HttpMessageConverter<T> theConverter =
							(HttpMessageConverter<T>) messageConverter;
					Class<? extends T> clazz = (Class<? extends T>) bodyClass;
					return theConverter.read(clazz, inputMessage);
				}
			}

			// 如果没有匹配的消息转换器，则抛出HttpMediaTypeNotSupportedException异常
			throw new HttpMediaTypeNotSupportedException(contentType, Collections.emptyList());
		}

		@Override
		public Map<String, Object> attributes() {
			return this.attributes;
		}

		@Override
		public MultiValueMap<String, String> params() {
			return this.params;
		}

		@Override
		public Map<String, String> pathVariables() {
			@SuppressWarnings("unchecked")
			Map<String, String> pathVariables = (Map<String, String>) attributes()
					.get(RouterFunctions.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
			// 如果路径变量不为null，则返回路径变量；
			if (pathVariables != null) {
				return pathVariables;
			} else {
				// 否则返回空的Map
				return Collections.emptyMap();
			}
		}

		@Override
		public HttpSession session() {
			return this.servletRequest.getSession();
		}

		@Override
		public Optional<Principal> principal() {
			return Optional.ofNullable(this.servletRequest.getUserPrincipal());
		}

		@Override
		public HttpServletRequest servletRequest() {
			return this.servletRequest;
		}


		private class BuiltInputMessage implements HttpInputMessage {

			@Override
			public InputStream getBody() throws IOException {
				return new BodyInputStream(body);
			}

			@Override
			public HttpHeaders getHeaders() {
				return headers;
			}
		}
	}


	private static class BodyInputStream extends ServletInputStream {
		/**
		 * 输入流
		 */
		private final InputStream delegate;

		public BodyInputStream(byte[] body) {
			this.delegate = new ByteArrayInputStream(body);
		}

		@Override
		public boolean isFinished() {
			return false;
		}

		@Override
		public boolean isReady() {
			return true;
		}

		@Override
		public void setReadListener(ReadListener readListener) {
			throw new UnsupportedOperationException();
		}

		@Override
		public int read() throws IOException {
			return this.delegate.read();
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			return this.delegate.read(b, off, len);
		}

		@Override
		public int read(byte[] b) throws IOException {
			return this.delegate.read(b);
		}

		@Override
		public long skip(long n) throws IOException {
			return this.delegate.skip(n);
		}

		@Override
		public int available() throws IOException {
			return this.delegate.available();
		}

		@Override
		public void close() throws IOException {
			this.delegate.close();
		}

		@Override
		public synchronized void mark(int readlimit) {
			this.delegate.mark(readlimit);
		}

		@Override
		public synchronized void reset() throws IOException {
			this.delegate.reset();
		}

		@Override
		public boolean markSupported() {
			return this.delegate.markSupported();
		}
	}

}
