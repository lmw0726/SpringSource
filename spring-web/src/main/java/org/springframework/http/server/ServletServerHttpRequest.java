/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.http.server;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.LinkedCaseInsensitiveMap;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.*;

/**
 * 基于 {@link HttpServletRequest} 的 {@link ServerHttpRequest} 实现。
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 3.0
 */
public class ServletServerHttpRequest implements ServerHttpRequest {

	/**
	 * 定义了表单内容类型为 "application/x-www-form-urlencoded"。
	 */
	protected static final String FORM_CONTENT_TYPE = "application/x-www-form-urlencoded";

	/**
	 * 表单字符集为 UTF-8。
	 */
	protected static final Charset FORM_CHARSET = StandardCharsets.UTF_8;

	/**
	 * HTTP Servlet 请求对象。
	 */
	private final HttpServletRequest servletRequest;

	/**
	 * 请求的URI。
	 */
	@Nullable
	private URI uri;

	/**
	 * 请求的头部信息。
	 */
	@Nullable
	private HttpHeaders headers;

	/**
	 * 用于异步服务器HTTP请求控制的对象。
	 */
	@Nullable
	private ServerHttpAsyncRequestControl asyncRequestControl;


	/**
	 * 根据给定的 {@link HttpServletRequest} 构造一个 {@code ServletServerHttpRequest} 实例。
	 *
	 * @param servletRequest Servlet 请求对象
	 */
	public ServletServerHttpRequest(HttpServletRequest servletRequest) {
		Assert.notNull(servletRequest, "HttpServletRequest must not be null");
		this.servletRequest = servletRequest;
	}


	/**
	 * 返回该对象基于的 {@code HttpServletRequest}。
	 */
	public HttpServletRequest getServletRequest() {
		return this.servletRequest;
	}

	@Override
	@Nullable
	public HttpMethod getMethod() {
		return HttpMethod.resolve(this.servletRequest.getMethod());
	}

	@Override
	public String getMethodValue() {
		return this.servletRequest.getMethod();
	}

	@Override
	public URI getURI() {
		// 如果URI为空
		if (this.uri == null) {
			String urlString = null;
			boolean hasQuery = false;
			try {
				// 获取请求的URL
				StringBuffer url = this.servletRequest.getRequestURL();
				// 获取请求的查询参数
				String query = this.servletRequest.getQueryString();
				// 检查是否有查询参数
				hasQuery = StringUtils.hasText(query);
				// 如果有查询参数，则将其附加到URL末尾
				if (hasQuery) {
					url.append('?').append(query);
				}
				// 将URL转换为字符串
				urlString = url.toString();
				// 创建URI对象
				this.uri = new URI(urlString);
			} catch (URISyntaxException ex) {
				// 如果捕获到URISyntaxException异常
				if (!hasQuery) {
					// 如果没有查询参数，则抛出IllegalStateException异常
					throw new IllegalStateException(
							"Could not resolve HttpServletRequest as URI: " + urlString, ex);
				}
				// 可能是查询字符串格式不正确... 尝试使用纯请求URL
				try {
					// 获取纯请求URL字符串
					urlString = this.servletRequest.getRequestURL().toString();
					// 创建URI对象
					this.uri = new URI(urlString);
				} catch (URISyntaxException ex2) {
					// 如果仍然捕获到URISyntaxException异常，则抛出IllegalStateException异常
					throw new IllegalStateException(
							"Could not resolve HttpServletRequest as URI: " + urlString, ex2);
				}
			}
		}
		// 返回已解析的URI对象
		return this.uri;
	}

	@Override
	public HttpHeaders getHeaders() {
		// 如果headers对象为空
		if (this.headers == null) {
			// 创建一个新的HttpHeaders对象
			this.headers = new HttpHeaders();

			// 遍历所有请求头
			for (Enumeration<?> names = this.servletRequest.getHeaderNames(); names.hasMoreElements(); ) {
				// 获取请求头的名称
				String headerName = (String) names.nextElement();
				// 获取请求头的所有值
				for (Enumeration<?> headerValues = this.servletRequest.getHeaders(headerName);
					 headerValues.hasMoreElements(); ) {
					// 获取请求头的单个值
					String headerValue = (String) headerValues.nextElement();
					// 将请求头及其值添加到HttpHeaders对象中
					this.headers.add(headerName, headerValue);
				}
			}

			// HttpServletRequest公开某些头部作为属性：
			// 如果尚未包含这些属性，则应包括它们
			try {
				// 获取请求的Content-Type
				MediaType contentType = this.headers.getContentType();
				if (contentType == null) {
					String requestContentType = this.servletRequest.getContentType();
					// 如果请求的Content-Type不为空
					if (StringUtils.hasLength(requestContentType)) {
						// 解析请求的Content-Type
						contentType = MediaType.parseMediaType(requestContentType);
						// 如果Content-Type是具体的
						if (contentType.isConcrete()) {
							// 设置HttpHeaders的Content-Type
							this.headers.setContentType(contentType);
						}
					}
				}
				// 如果Content-Type存在且字符集未设置
				if (contentType != null && contentType.getCharset() == null) {
					// 获取请求的字符编码
					String requestEncoding = this.servletRequest.getCharacterEncoding();
					// 如果请求的字符编码不为空
					if (StringUtils.hasLength(requestEncoding)) {
						// 解析字符编码为Charset对象
						Charset charSet = Charset.forName(requestEncoding);
						// 复制Content-Type的参数并添加字符集参数
						Map<String, String> params = new LinkedCaseInsensitiveMap<>();
						params.putAll(contentType.getParameters());
						params.put("charset", charSet.toString());
						// 创建新的MediaType对象
						MediaType mediaType = new MediaType(contentType.getType(), contentType.getSubtype(), params);
						// 设置HttpHeaders的Content-Type
						this.headers.setContentType(mediaType);
					}
				}
			} catch (InvalidMediaTypeException ex) {
				// 忽略异常：在HttpHeaders中简单地不暴露无效的Content-Type...
			}

			// 如果HttpHeaders的Content-Length小于0
			if (this.headers.getContentLength() < 0) {
				// 获取请求的内容长度
				int requestContentLength = this.servletRequest.getContentLength();
				// 如果请求的内容长度不为-1
				if (requestContentLength != -1) {
					// 设置HttpHeaders的Content-Length
					this.headers.setContentLength(requestContentLength);
				}
			}
		}

		// 返回已处理的HttpHeaders对象
		return this.headers;
	}

	@Override
	public Principal getPrincipal() {
		return this.servletRequest.getUserPrincipal();
	}

	@Override
	public InetSocketAddress getLocalAddress() {
		return new InetSocketAddress(this.servletRequest.getLocalAddr(), this.servletRequest.getLocalPort());
	}

	@Override
	public InetSocketAddress getRemoteAddress() {
		return new InetSocketAddress(this.servletRequest.getRemoteHost(), this.servletRequest.getRemotePort());
	}

	@Override
	public InputStream getBody() throws IOException {
		// 如果请求是表单提交
		if (isFormPost(this.servletRequest)) {
			// 从ServletRequest参数中获取请求体
			return getBodyFromServletRequestParameters(this.servletRequest);
		} else {
			// 否则，返回ServletRequest的输入流
			return this.servletRequest.getInputStream();
		}
	}

	@Override
	public ServerHttpAsyncRequestControl getAsyncRequestControl(ServerHttpResponse response) {
		// 如果异步请求控制对象为空
		if (this.asyncRequestControl == null) {
			// 如果响应不是ServletServerHttpResponse的实例
			if (!(response instanceof ServletServerHttpResponse)) {
				// 抛出IllegalArgumentException异常
				throw new IllegalArgumentException(
						"Response must be a ServletServerHttpResponse: " + response.getClass());
			}
			// 将响应转换为ServletServerHttpResponse
			ServletServerHttpResponse servletServerResponse = (ServletServerHttpResponse) response;
			// 创建新的ServletServerHttpAsyncRequestControl对象
			this.asyncRequestControl = new ServletServerHttpAsyncRequestControl(this, servletServerResponse);
		}
		// 返回异步请求控制对象
		return this.asyncRequestControl;
	}


	private static boolean isFormPost(HttpServletRequest request) {
		// 获取请求的Content-Type
		String contentType = request.getContentType();
		// 返回是否请求的Content-Type不为空，且包含FORM_CONTENT_TYPE，并且请求方法为POST
		return (contentType != null && contentType.contains(FORM_CONTENT_TYPE) &&
				HttpMethod.POST.matches(request.getMethod()));
	}

	/**
	 * 使用 {@link javax.servlet.ServletRequest#getParameterMap()} 重构表单 'POST' 的主体，
	 * 提供可预测的结果，而不是从主体中读取，如果任何其他代码已使用 ServletRequest 访问参数，
	 * 则会导致输入流 "已消耗"。
	 */
	private static InputStream getBodyFromServletRequestParameters(HttpServletRequest request) throws IOException {
		// 创建一个初始容量为1024的字节数组输出流
		ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);
		// 使用FORM_CHARSET字符集创建一个输出流写入器
		Writer writer = new OutputStreamWriter(bos, FORM_CHARSET);

		// 获取请求的参数映射
		Map<String, String[]> form = request.getParameterMap();
		// 遍历参数映射的条目
		for (Iterator<Map.Entry<String, String[]>> entryIterator = form.entrySet().iterator(); entryIterator.hasNext(); ) {
			// 获取映射的条目
			Map.Entry<String, String[]> entry = entryIterator.next();
			// 获取参数名
			String name = entry.getKey();
			// 获取参数值列表
			List<String> values = Arrays.asList(entry.getValue());
			// 遍历参数值列表
			for (Iterator<String> valueIterator = values.iterator(); valueIterator.hasNext(); ) {
				// 获取参数值
				String value = valueIterator.next();
				// 将参数名按照FORM_CHARSET字符集编码写入writer
				writer.write(URLEncoder.encode(name, FORM_CHARSET.name()));
				if (value != null) {
					// 如果参数值不为空，写入'='和经过FORM_CHARSET字符集编码的参数值
					writer.write('=');
					writer.write(URLEncoder.encode(value, FORM_CHARSET.name()));
					if (valueIterator.hasNext()) {
						// 如果还有下一个参数值，写入'&'
						writer.write('&');
					}
				}
			}
			if (entryIterator.hasNext()) {
				// 如果还有下一个条目，写入'&'
				writer.append('&');
			}
		}
		// 刷新writer
		writer.flush();

		// 返回包含bos字节数组的新的ByteArrayInputStream
		return new ByteArrayInputStream(bos.toByteArray());
	}

}
