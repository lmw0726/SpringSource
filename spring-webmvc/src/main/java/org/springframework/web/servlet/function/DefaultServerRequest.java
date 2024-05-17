/*
 * Copyright 2002-2021 the original author or authors.
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
import org.springframework.http.HttpRange;
import org.springframework.http.MediaType;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.ServletRequestPathUtils;
import org.springframework.web.util.UriBuilder;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.Charset;
import java.security.Principal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 基于 {@link HttpServletRequest} 的 {@code ServerRequest} 实现。
 *
 * @author Arjen Poutsma
 * @since 5.2
 */
class DefaultServerRequest implements ServerRequest {
	/**
	 * Servlet服务端Http请求
	 */
	private final ServletServerHttpRequest serverHttpRequest;

	/**
	 * 请求路径
	 */
	private final RequestPath requestPath;

	/**
	 * 请求头
	 */
	private final Headers headers;

	/**
	 * Http消息转换器列表
	 */
	private final List<HttpMessageConverter<?>> messageConverters;

	/**
	 * 参数值
	 */
	private final MultiValueMap<String, String> params;

	/**
	 * 属性值
	 */
	private final Map<String, Object> attributes;

	/**
	 * 文件名与文件部分映射
	 */
	@Nullable
	private MultiValueMap<String, Part> parts;


	public DefaultServerRequest(HttpServletRequest servletRequest, List<HttpMessageConverter<?>> messageConverters) {
		// 创建 ServletServerHttpRequest 实例，用于封装 Servlet 请求
		this.serverHttpRequest = new ServletServerHttpRequest(servletRequest);
		// 使用给定的消息转换器列表创建不可变的消息转换器列表
		this.messageConverters = Collections.unmodifiableList(new ArrayList<>(messageConverters));

		// 创建 DefaultRequestHeaders 实例，封装了 Servlet 请求的头部信息
		this.headers = new DefaultRequestHeaders(this.serverHttpRequest.getHeaders());
		// 创建 MultiValueMap 实例，封装了 Servlet 请求的参数信息
		this.params = CollectionUtils.toMultiValueMap(new ServletParametersMap(servletRequest));
		// 创建 ServletAttributesMap 实例，封装了 Servlet 请求的属性信息
		this.attributes = new ServletAttributesMap(servletRequest);

		// DispatcherServlet 解析了路径，但是其他情况下（例如测试），我们可能需要解析路径
		// 检查 Servlet 请求是否已经解析了请求路径
		this.requestPath = (ServletRequestPathUtils.hasParsedRequestPath(servletRequest) ?
				// 如果已经解析了请求路径，则获取已解析的请求路径
				ServletRequestPathUtils.getParsedRequestPath(servletRequest) :
				// 否则解析并缓存请求路径
				ServletRequestPathUtils.parseAndCache(servletRequest));
	}


	@Override
	public String methodName() {
		return servletRequest().getMethod();
	}

	@Override
	public URI uri() {
		return this.serverHttpRequest.getURI();
	}

	@Override
	public UriBuilder uriBuilder() {
		return ServletUriComponentsBuilder.fromRequest(servletRequest());
	}

	@Override
	public RequestPath requestPath() {
		return this.requestPath;
	}

	@Override
	public Headers headers() {
		return this.headers;
	}

	@Override
	public MultiValueMap<String, Cookie> cookies() {
		// 获取 Servlet 请求中的所有 Cookie
		Cookie[] cookies = servletRequest().getCookies();
		// 如果获取到的 Cookie 数组为 null，则将其设为空数组
		if (cookies == null) {
			cookies = new Cookie[0];
		}
		// 创建 MultiValueMap 实例，用于封装 Cookie 信息
		MultiValueMap<String, Cookie> result = new LinkedMultiValueMap<>(cookies.length);
		// 遍历 Cookie 数组，将每个 Cookie名称 和 Cookie 添加到 MultiValueMap 中
		for (Cookie cookie : cookies) {
			result.add(cookie.getName(), cookie);
		}
		return result;
	}

	@Override
	public HttpServletRequest servletRequest() {
		return this.serverHttpRequest.getServletRequest();
	}

	@Override
	public Optional<InetSocketAddress> remoteAddress() {
		return Optional.of(this.serverHttpRequest.getRemoteAddress());
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
		Type type = bodyType.getType();
		return bodyInternal(type, bodyClass(type));
	}

	static Class<?> bodyClass(Type type) {
		// 如果类型是 Class 类型，则直接返回
		if (type instanceof Class) {
			return (Class<?>) type;
		}
		// 如果类型是 ParameterizedType 类型，则尝试获取其原始类型并返回
		if (type instanceof ParameterizedType) {
			ParameterizedType parameterizedType = (ParameterizedType) type;
			if (parameterizedType.getRawType() instanceof Class) {
				// 获取原始类型并返回
				return (Class<?>) parameterizedType.getRawType();
			}
		}
		// 默认返回 Object 类型
		return Object.class;
	}

	@SuppressWarnings("unchecked")
	private <T> T bodyInternal(Type bodyType, Class<?> bodyClass) throws ServletException, IOException {
		// 获取请求的 Content-Type，如果不存在则默认为 APPLICATION_OCTET_STREAM
		MediaType contentType = this.headers.contentType().orElse(MediaType.APPLICATION_OCTET_STREAM);

		// 遍历消息转换器列表
		for (HttpMessageConverter<?> messageConverter : this.messageConverters) {
			// 如果当前消息转换器是泛型消息转换器
			if (messageConverter instanceof GenericHttpMessageConverter) {
				// 将当前消息转换器转换为泛型消息转换器
				GenericHttpMessageConverter<T> genericMessageConverter =
						(GenericHttpMessageConverter<T>) messageConverter;
				// 如果泛型消息转换器支持读取当前请求的消息
				if (genericMessageConverter.canRead(bodyType, bodyClass, contentType)) {
					// 则使用泛型消息转换器读取消息并返回结果
					return genericMessageConverter.read(bodyType, bodyClass, this.serverHttpRequest);
				}
			}
			// 如果当前消息转换器支持读取当前请求的消息
			if (messageConverter.canRead(bodyClass, contentType)) {
				// 则将当前消息转换器转换为指定类型，并使用该转换器读取消息并返回结果
				HttpMessageConverter<T> theConverter =
						(HttpMessageConverter<T>) messageConverter;
				Class<? extends T> clazz = (Class<? extends T>) bodyClass;
				return theConverter.read(clazz, this.serverHttpRequest);
			}
		}
		// 如果没有合适的消息转换器，则抛出 HttpMediaTypeNotSupportedException 异常
		throw new HttpMediaTypeNotSupportedException(contentType, getSupportedMediaTypes(bodyClass));
	}

	private List<MediaType> getSupportedMediaTypes(Class<?> bodyClass) {
		return this.messageConverters.stream()
				// 将每个消息转换器支持的媒体类型扁平化为流
				.flatMap(converter -> converter.getSupportedMediaTypes(bodyClass).stream())
				// 根据媒体类型的特异性进行排序
				.sorted(MediaType.SPECIFICITY_COMPARATOR)
				// 将排序后的媒体类型收集到列表中并返回
				.collect(Collectors.toList());
	}

	@Override
	public Optional<Object> attribute(String name) {
		return Optional.ofNullable(servletRequest().getAttribute(name));
	}

	@Override
	public Map<String, Object> attributes() {
		return this.attributes;
	}

	@Override
	public Optional<String> param(String name) {
		return Optional.ofNullable(servletRequest().getParameter(name));
	}

	@Override
	public MultiValueMap<String, String> params() {
		return this.params;
	}

	@Override
	public MultiValueMap<String, Part> multipartData() throws IOException, ServletException {
		MultiValueMap<String, Part> result = this.parts;
		if (result == null) {
			// 如果parts为空，则将ServletRequest的所有Part按名称分组，并收集到MultiValueMap中
			result = servletRequest().getParts().stream()
					.collect(Collectors.groupingBy(Part::getName,
							LinkedMultiValueMap::new,
							Collectors.toList()));
			// 将收集到的MultiValueMap保存到parts属性中
			this.parts = result;
		}
		return result;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Map<String, String> pathVariables() {
		// 获取Servlet请求中的URI模板变量
		Map<String, String> pathVariables = (Map<String, String>)
				servletRequest().getAttribute(RouterFunctions.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
		if (pathVariables != null) {
			// 如果 URI模板变量 不为空，则返回该映射
			return pathVariables;
		} else {
			// 否则返回一个空的映射
			return Collections.emptyMap();
		}
	}

	@Override
	public HttpSession session() {
		return servletRequest().getSession(true);
	}

	@Override
	public Optional<Principal> principal() {
		return Optional.ofNullable(this.serverHttpRequest.getPrincipal());
	}

	@Override
	public String toString() {
		return String.format("HTTP %s %s", method(), path());
	}

	static Optional<ServerResponse> checkNotModified(
			HttpServletRequest servletRequest, @Nullable Instant lastModified, @Nullable String etag) {
		// 最后更改时间戳
		long lastModifiedTimestamp = -1;
		if (lastModified != null && lastModified.isAfter(Instant.EPOCH)) {
			// 如果最后更改时间存在，且在Epoch之后，则转换为时间戳
			lastModifiedTimestamp = lastModified.toEpochMilli();
		}

		// 创建一个CheckNotModifiedResponse对象
		CheckNotModifiedResponse response = new CheckNotModifiedResponse();
		// 创建一个ServletWebRequest对象
		WebRequest webRequest = new ServletWebRequest(servletRequest, response);
		// 检查请求是否未被修改
		if (webRequest.checkNotModified(etag, lastModifiedTimestamp)) {
			// 如果请求不需要更新，则构建一个包含响应状态和头信息的ServerResponse对象
			return Optional.of(ServerResponse.status(response.status)
					.headers(headers -> headers.addAll(response.headers))
					.build());
		} else {
			// 如果请求需要更新，则返回空Optional对象
			return Optional.empty();
		}
	}


	/**
	 * {@link Headers}的默认实现
	 */
	static class DefaultRequestHeaders implements Headers {
		/**
		 * Http请求头
		 */
		private final HttpHeaders httpHeaders;

		public DefaultRequestHeaders(HttpHeaders httpHeaders) {
			this.httpHeaders = HttpHeaders.readOnlyHttpHeaders(httpHeaders);
		}

		@Override
		public List<MediaType> accept() {
			return this.httpHeaders.getAccept();
		}

		@Override
		public List<Charset> acceptCharset() {
			return this.httpHeaders.getAcceptCharset();
		}

		@Override
		public List<Locale.LanguageRange> acceptLanguage() {
			return this.httpHeaders.getAcceptLanguage();
		}

		@Override
		public OptionalLong contentLength() {
			// 返回Http请求头中Content-Length的值
			long value = this.httpHeaders.getContentLength();
			return (value != -1 ? OptionalLong.of(value) : OptionalLong.empty());
		}

		@Override
		public Optional<MediaType> contentType() {
			return Optional.ofNullable(this.httpHeaders.getContentType());
		}

		@Override
		public InetSocketAddress host() {
			return this.httpHeaders.getHost();
		}

		@Override
		public List<HttpRange> range() {
			return this.httpHeaders.getRange();
		}

		@Override
		public List<String> header(String headerName) {
			// 返回Http请求头中给定名称的值
			List<String> headerValues = this.httpHeaders.get(headerName);
			// 如果头部值为空，则返回空列表；否则返回头部值
			return (headerValues != null ? headerValues : Collections.emptyList());
		}

		@Override
		public HttpHeaders asHttpHeaders() {
			return this.httpHeaders;
		}

		@Override
		public String toString() {
			return this.httpHeaders.toString();
		}
	}


	private static final class ServletParametersMap extends AbstractMap<String, List<String>> {
		/**
		 * HttpServlet 请求
		 */
		private final HttpServletRequest servletRequest;

		private ServletParametersMap(HttpServletRequest servletRequest) {
			this.servletRequest = servletRequest;
		}

		@Override
		public Set<Entry<String, List<String>>> entrySet() {
			// 获取参数值对
			return this.servletRequest.getParameterMap().entrySet().stream()
					.map(entry -> {
						// 将参数值转换为不可变的List
						List<String> value = Arrays.asList(entry.getValue());
						// 构建包含参数名和不可变值列表的键值对
						return new SimpleImmutableEntry<>(entry.getKey(), value);
					})
					.collect(Collectors.toSet());
		}

		@Override
		public int size() {
			return this.servletRequest.getParameterMap().size();
		}

		@Override
		public List<String> get(Object key) {
			// 从键中获取参数名
			String name = (String) key;
			// 根据参数名从Servlet请求中获取参数值数组
			String[] parameterValues = this.servletRequest.getParameterValues(name);
			if (!ObjectUtils.isEmpty(parameterValues)) {
				// 如果参数值数组不为空，则将其转换为List并返回
				return Arrays.asList(parameterValues);
			} else {
				// 如果参数值数组为空，则返回空列表
				return Collections.emptyList();
			}
		}

		@Override
		public List<String> put(String key, List<String> value) {
			throw new UnsupportedOperationException();
		}

		@Override
		public List<String> remove(Object key) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void clear() {
			throw new UnsupportedOperationException();
		}
	}


	private static final class ServletAttributesMap extends AbstractMap<String, Object> {
		/**
		 * HttpServlet请求
		 */
		private final HttpServletRequest servletRequest;

		private ServletAttributesMap(HttpServletRequest servletRequest) {
			this.servletRequest = servletRequest;
		}

		@Override
		public boolean containsKey(Object key) {
			String name = (String) key;
			return this.servletRequest.getAttribute(name) != null;
		}

		@Override
		public void clear() {
			// 获取属性名称列表
			List<String> attributeNames = Collections.list(this.servletRequest.getAttributeNames());
			// 循环删除属性名称
			attributeNames.forEach(this.servletRequest::removeAttribute);
		}

		@Override
		public Set<Entry<String, Object>> entrySet() {
			// 获取请求中所有属性名的枚举集合，并转换为流
			return Collections.list(this.servletRequest.getAttributeNames()).stream()
					// 遍历属性名，并将每个属性名和对应的属性值封装为一个不可变的简单条目（键值对）
					.map(name -> {
						Object value = this.servletRequest.getAttribute(name);
						return new SimpleImmutableEntry<>(name, value);
					})
					// 收集封装好的键值对，组成一个不可变的集合
					.collect(Collectors.toSet());
		}

		@Override
		public Object get(Object key) {
			String name = (String) key;
			return this.servletRequest.getAttribute(name);
		}

		@Override
		public Object put(String key, Object value) {
			// 获取旧的属性值
			Object oldValue = this.servletRequest.getAttribute(key);
			// 设置新的属性值
			this.servletRequest.setAttribute(key, value);
			// 返回旧的属性值
			return oldValue;
		}

		@Override
		public Object remove(Object key) {
			String name = (String) key;
			// 获取属性值
			Object value = this.servletRequest.getAttribute(name);
			// 从Servlet请求移除属性名称
			this.servletRequest.removeAttribute(name);
			// 返回属性值
			return value;
		}
	}


	/**
	 * {@link HttpServletResponse} 的简单实现，由 {@link #checkNotModified(HttpServletRequest, Instant, String)} 使用，
	 * 用于记录 {@link ServletWebRequest#checkNotModified(String, long)} 设置的状态和头信息。对于其他方法抛出 {@code UnsupportedOperationException}。
	 */
	private static final class CheckNotModifiedResponse implements HttpServletResponse {
		/**
		 * Http请求头
		 */
		private final HttpHeaders headers = new HttpHeaders();

		/**
		 * 200 状态值
		 */
		private int status = 200;

		@Override
		public boolean containsHeader(String name) {
			return this.headers.containsKey(name);
		}

		@Override
		public void setDateHeader(String name, long date) {
			this.headers.setDate(name, date);
		}

		@Override
		public void setHeader(String name, String value) {
			this.headers.set(name, value);
		}

		@Override
		public void addHeader(String name, String value) {
			this.headers.add(name, value);
		}

		@Override
		public void setStatus(int sc) {
			this.status = sc;
		}

		@Override
		@Deprecated
		public void setStatus(int sc, String sm) {
			this.status = sc;
		}

		@Override
		public int getStatus() {
			return this.status;
		}

		@Override
		@Nullable
		public String getHeader(String name) {
			return this.headers.getFirst(name);
		}

		@Override
		public Collection<String> getHeaders(String name) {
			List<String> result = this.headers.get(name);
			return (result != null ? result : Collections.emptyList());
		}

		@Override
		public Collection<String> getHeaderNames() {
			return this.headers.keySet();
		}


		// 不支持

		@Override
		public void addCookie(Cookie cookie) {
			throw new UnsupportedOperationException();
		}

		@Override
		public String encodeURL(String url) {
			throw new UnsupportedOperationException();
		}

		@Override
		public String encodeRedirectURL(String url) {
			throw new UnsupportedOperationException();
		}

		@Override
		@Deprecated
		public String encodeUrl(String url) {
			throw new UnsupportedOperationException();
		}

		@Override
		@Deprecated
		public String encodeRedirectUrl(String url) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void sendError(int sc, String msg) throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public void sendError(int sc) throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public void sendRedirect(String location) throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public void addDateHeader(String name, long date) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setIntHeader(String name, int value) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void addIntHeader(String name, int value) {
			throw new UnsupportedOperationException();
		}


		@Override
		public String getCharacterEncoding() {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getContentType() {
			throw new UnsupportedOperationException();
		}

		@Override
		public ServletOutputStream getOutputStream() throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public PrintWriter getWriter() throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setCharacterEncoding(String charset) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setContentLength(int len) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setContentLengthLong(long len) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setContentType(String type) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setBufferSize(int size) {
			throw new UnsupportedOperationException();
		}

		@Override
		public int getBufferSize() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void flushBuffer() throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public void resetBuffer() {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean isCommitted() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void reset() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setLocale(Locale loc) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Locale getLocale() {
			throw new UnsupportedOperationException();
		}
	}

}
