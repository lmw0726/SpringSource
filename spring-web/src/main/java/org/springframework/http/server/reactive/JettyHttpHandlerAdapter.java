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

package org.springframework.http.server.reactive;

import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.server.HttpOutput;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.MultiValueMap;

import javax.servlet.AsyncContext;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * {@link ServletHttpHandlerAdapter} 的扩展，使用 Jetty API 以 {@link ByteBuffer} 形式写入响应。
 *
 * @author Violeta Georgieva
 * @author Brian Clozel
 * @see org.springframework.web.server.adapter.AbstractReactiveWebInitializer
 * @since 5.0
 */
public class JettyHttpHandlerAdapter extends ServletHttpHandlerAdapter {
	/**
	 * Jetty 10 是否存在
	 */
	private static final boolean jetty10Present = ClassUtils.isPresent(
			"org.eclipse.jetty.http.CookieCutter", JettyHttpHandlerAdapter.class.getClassLoader());


	public JettyHttpHandlerAdapter(HttpHandler httpHandler) {
		super(httpHandler);
	}


	@Override
	protected ServletServerHttpRequest createRequest(HttpServletRequest request, AsyncContext context)
			throws IOException, URISyntaxException {

		// TODO: 需要根据Jetty 10编译以使用HttpFields（从类->接口）
		if (jetty10Present) {
			// 如果Jetty 10存在，调用父类的方法来创建请求对象
			return super.createRequest(request, context);
		}

		// 否则，确保Servlet路径已初始化
		Assert.notNull(getServletPath(), "Servlet path is not initialized");
		// 返回基于JettyServerHttpRequest的请求对象
		return new JettyServerHttpRequest(
				request, context, getServletPath(), getDataBufferFactory(), getBufferSize());
	}

	@Override
	protected ServletServerHttpResponse createResponse(HttpServletResponse response,
													   AsyncContext context, ServletServerHttpRequest request) throws IOException {

		// TODO: 需要根据Jetty 10编译以使用HttpFields（从类->接口）
		if (jetty10Present) {
			// 如果Jetty 10存在，返回基于BaseJettyServerHttpResponse的响应对象
			return new BaseJettyServerHttpResponse(
					response, context, getDataBufferFactory(), getBufferSize(), request);
		} else {
			// 否则，返回基于JettyServerHttpResponse的响应对象
			return new JettyServerHttpResponse(
					response, context, getDataBufferFactory(), getBufferSize(), request);
		}
	}


	private static final class JettyServerHttpRequest extends ServletServerHttpRequest {

		JettyServerHttpRequest(HttpServletRequest request, AsyncContext asyncContext,
							   String servletPath, DataBufferFactory bufferFactory, int bufferSize)
				throws IOException, URISyntaxException {

			super(createHeaders(request), request, asyncContext, servletPath, bufferFactory, bufferSize);
		}

		private static MultiValueMap<String, String> createHeaders(HttpServletRequest servletRequest) {
			// 获取Request对象，通常通过getRequest方法获取
			Request request = getRequest(servletRequest);

			// 获取请求对象的元数据，并从中获取HTTP字段
			HttpFields fields = request.getMetaData().getFields();

			// 创建并返回一个JettyHeadersAdapter对象，将获取到的HTTP字段传入适配器中
			return new JettyHeadersAdapter(fields);
		}

		private static Request getRequest(HttpServletRequest request) {
			// 如果request对象本身就是org.eclipse.jetty.server.Request类型，则直接返回
			if (request instanceof Request) {
				return (Request) request;
			} else if (request instanceof HttpServletRequestWrapper) {
				// 如果request是HttpServletRequestWrapper类型（通常用于请求包装器）
				// 将request转换为HttpServletRequestWrapper类型
				HttpServletRequestWrapper wrapper = (HttpServletRequestWrapper) request;
				// 获取包装后的原始HttpServletRequest对象
				HttpServletRequest wrappedRequest = (HttpServletRequest) wrapper.getRequest();
				// 递归调用getRequest方法，处理包装后的HttpServletRequest对象
				return getRequest(wrappedRequest);
			} else {
				// 如果request既不是Request类型，也不是HttpServletRequestWrapper类型，则抛出异常
				throw new IllegalArgumentException("Cannot convert [" + request.getClass() +
						"] to org.eclipse.jetty.server.Request");
			}
		}


	}


	private static class BaseJettyServerHttpResponse extends ServletServerHttpResponse {

		BaseJettyServerHttpResponse(HttpServletResponse response, AsyncContext asyncContext,
									DataBufferFactory bufferFactory, int bufferSize, ServletServerHttpRequest request)
				throws IOException {

			super(response, asyncContext, bufferFactory, bufferSize, request);
		}

		BaseJettyServerHttpResponse(HttpHeaders headers, HttpServletResponse response, AsyncContext asyncContext,
									DataBufferFactory bufferFactory, int bufferSize, ServletServerHttpRequest request)
				throws IOException {

			super(headers, response, asyncContext, bufferFactory, bufferSize, request);
		}

		@Override
		protected int writeToOutputStream(DataBuffer dataBuffer) throws IOException {
			// 将 DataBuffer 转换为 ByteBuffer
			ByteBuffer input = dataBuffer.asByteBuffer();

			// 获取 ByteBuffer 中剩余的字节数
			int len = input.remaining();

			// 获取原生响应对象
			ServletResponse response = getNativeResponse();

			// 将 ByteBuffer 写入响应输出流
			((HttpOutput) response.getOutputStream()).write(input);

			// 返回写入的字节数
			return len;
		}
	}


	private static final class JettyServerHttpResponse extends BaseJettyServerHttpResponse {

		JettyServerHttpResponse(HttpServletResponse response, AsyncContext asyncContext,
								DataBufferFactory bufferFactory, int bufferSize, ServletServerHttpRequest request)
				throws IOException {

			super(createHeaders(response), response, asyncContext, bufferFactory, bufferSize, request);
		}

		private static HttpHeaders createHeaders(HttpServletResponse servletResponse) {
			// 获取响应对象
			Response response = getResponse(servletResponse);

			// 获取响应对象中的 HTTP 字段
			HttpFields fields = response.getHttpFields();

			// 将 Jetty 的 HTTP 头适配器封装到 HttpHeaders 中，并返回
			return new HttpHeaders(new JettyHeadersAdapter(fields));
		}

		private static Response getResponse(HttpServletResponse response) {
			// 如果响应对象是 Response 类型，则直接返回
			if (response instanceof Response) {
				return (Response) response;
			} else if (response instanceof HttpServletResponseWrapper) {
				// 如果响应对象是 HttpServletResponseWrapper 类型，则递归获取原始响应，并再次调用该方法
				HttpServletResponseWrapper wrapper = (HttpServletResponseWrapper) response;
				HttpServletResponse wrappedResponse = (HttpServletResponse) wrapper.getResponse();
				return getResponse(wrappedResponse);
			} else {
				// 如果响应对象类型不是上述两种类型，则抛出异常
				throw new IllegalArgumentException("Cannot convert [" + response.getClass() +
						"] to org.eclipse.jetty.server.Response");
			}
		}

		@Override
		protected void applyHeaders() {
			// 获取本地响应对象
			HttpServletResponse response = getNativeResponse();

			// 尝试获取响应头中的内容类型
			MediaType contentType = null;
			try {
				contentType = getHeaders().getContentType();
			} catch (Exception ex) {
				// 如果出现异常，尝试直接获取响应头中的原始内容类型，并设置到响应对象中
				String rawContentType = getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
				response.setContentType(rawContentType);
			}

			// 如果响应对象中的内容类型为 null，且从响应头中解析出的内容类型不为 null，则设置响应对象中的内容类型
			if (response.getContentType() == null && contentType != null) {
				response.setContentType(contentType.toString());
			}

			// 获取内容类型的字符集
			Charset charset = (contentType != null ? contentType.getCharset() : null);

			// 如果响应对象中的字符编码为 null，且字符集不为 null，则设置响应对象中的字符编码
			if (response.getCharacterEncoding() == null && charset != null) {
				response.setCharacterEncoding(charset.name());
			}

			// 获取响应头中的内容长度
			long contentLength = getHeaders().getContentLength();

			// 如果内容长度不为 -1，则设置响应对象中的内容长度
			if (contentLength != -1) {
				response.setContentLengthLong(contentLength);
			}
		}
	}

}
