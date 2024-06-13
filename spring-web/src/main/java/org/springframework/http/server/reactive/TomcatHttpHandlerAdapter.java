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

package org.springframework.http.server.reactive;

import org.apache.catalina.connector.CoyoteInputStream;
import org.apache.catalina.connector.CoyoteOutputStream;
import org.apache.catalina.connector.RequestFacade;
import org.apache.catalina.connector.ResponseFacade;
import org.apache.coyote.Request;
import org.apache.coyote.Response;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ReflectionUtils;

import javax.servlet.AsyncContext;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * {@link ServletHttpHandlerAdapter} extension that uses Tomcat APIs for reading
 * from the request and writing to the response with {@link ByteBuffer}.
 *
 * @author Violeta Georgieva
 * @author Brian Clozel
 * @see org.springframework.web.server.adapter.AbstractReactiveWebInitializer
 * @since 5.0
 */
public class TomcatHttpHandlerAdapter extends ServletHttpHandlerAdapter {


	public TomcatHttpHandlerAdapter(HttpHandler httpHandler) {
		super(httpHandler);
	}


	@Override
	protected ServletServerHttpRequest createRequest(HttpServletRequest request, AsyncContext asyncContext)
			throws IOException, URISyntaxException {

		Assert.notNull(getServletPath(), "Servlet path is not initialized");
		return new TomcatServerHttpRequest(
				request, asyncContext, getServletPath(), getDataBufferFactory(), getBufferSize());
	}

	@Override
	protected ServletServerHttpResponse createResponse(HttpServletResponse response,
													   AsyncContext asyncContext, ServletServerHttpRequest request) throws IOException {

		return new TomcatServerHttpResponse(
				response, asyncContext, getDataBufferFactory(), getBufferSize(), request);
	}


	private static final class TomcatServerHttpRequest extends ServletServerHttpRequest {

		/**
		 * Coyote 请求字段。
		 */
		private static final Field COYOTE_REQUEST_FIELD;

		/**
		 * 缓冲区大小。
		 */
		private final int bufferSize;

		/**
		 * 数据缓冲区工厂。
		 */
		private final DataBufferFactory factory;

		static {
			// 查找RequestFacade类中名为"request"的字段
			Field field = ReflectionUtils.findField(RequestFacade.class, "request");

			Assert.state(field != null, "Incompatible Tomcat implementation");

			// 设置字段可访问
			ReflectionUtils.makeAccessible(field);

			// 将找到的字段赋值给COYOTE_REQUEST_FIELD静态变量
			COYOTE_REQUEST_FIELD = field;
		}

		TomcatServerHttpRequest(HttpServletRequest request, AsyncContext context,
								String servletPath, DataBufferFactory factory, int bufferSize)
				throws IOException, URISyntaxException {

			super(createTomcatHttpHeaders(request), request, context, servletPath, factory, bufferSize);
			this.factory = factory;
			this.bufferSize = bufferSize;
		}

		private static MultiValueMap<String, String> createTomcatHttpHeaders(HttpServletRequest request) {
			// 获取RequestFacade对象，用于包装HTTP请求
			RequestFacade requestFacade = getRequestFacade(request);

			// 使用反射获取Tomcat的ConnectorRequest对象
			org.apache.catalina.connector.Request connectorRequest = (org.apache.catalina.connector.Request)
					ReflectionUtils.getField(COYOTE_REQUEST_FIELD, requestFacade);

			Assert.state(connectorRequest != null, "No Tomcat connector request");

			// 获取Tomcat的CoyoteRequest对象
			Request tomcatRequest = connectorRequest.getCoyoteRequest();

			// 返回一个新的TomcatHeadersAdapter对象，该对象适配了Tomcat的MimeHeaders头部信息
			return new TomcatHeadersAdapter(tomcatRequest.getMimeHeaders());
		}

		private static RequestFacade getRequestFacade(HttpServletRequest request) {
			// 如果request是RequestFacade的实例
			if (request instanceof RequestFacade) {
				// 直接将request转换为RequestFacade类型并返回
				return (RequestFacade) request;
			} else if (request instanceof HttpServletRequestWrapper) {
				// 如果request是HttpServletRequestWrapper的实例
				// 将request转换为HttpServletRequestWrapper类型
				HttpServletRequestWrapper wrapper = (HttpServletRequestWrapper) request;
				// 获取HttpServletRequestWrapper中包装的HttpServletRequest
				HttpServletRequest wrappedRequest = (HttpServletRequest) wrapper.getRequest();
				// 递归调用getRequestFacade方法，处理被包装的HttpServletRequest
				return getRequestFacade(wrappedRequest);
			} else {
				// 如果request既不是RequestFacade也不是HttpServletRequestWrapper
				// 抛出IllegalArgumentException异常
				throw new IllegalArgumentException("Cannot convert [" + request.getClass() +
						"] to org.apache.catalina.connector.RequestFacade");
			}
		}

		@Override
		protected DataBuffer readFromInputStream() throws IOException {
			// 获取Servlet输入流
			ServletInputStream inputStream = ((ServletRequest) getNativeRequest()).getInputStream();
			// 如果输入流不是CoyoteInputStream的实例，可能被包装，无法使用CoyoteInputStream
			if (!(inputStream instanceof CoyoteInputStream)) {
				return super.readFromInputStream();
			}
			boolean release = true;
			int capacity = this.bufferSize;
			// 分配数据缓冲区
			DataBuffer dataBuffer = this.factory.allocateBuffer(capacity);
			try {
				ByteBuffer byteBuffer = dataBuffer.asByteBuffer(0, capacity);
				// 从CoyoteInputStream中读取数据到ByteBuffer中
				int read = ((CoyoteInputStream) inputStream).read(byteBuffer);
				// 记录读取的字节数
				logBytesRead(read);
				if (read > 0) {
					// 设置数据缓冲区的写入位置
					dataBuffer.writePosition(read);
					release = false;
					return dataBuffer;
				} else if (read == -1) {
					// 如果读取到末尾，返回EOF_BUFFER
					return EOF_BUFFER;
				} else {
					// 否则返回空缓冲区
					return AbstractListenerReadPublisher.EMPTY_BUFFER;
				}
			} finally {
				// 如果需要释放数据缓冲区，则释放
				if (release) {
					DataBufferUtils.release(dataBuffer);
				}
			}
		}
	}


	private static final class TomcatServerHttpResponse extends ServletServerHttpResponse {
		/**
		 * coyote 响应字段
		 */
		private static final Field COYOTE_RESPONSE_FIELD;

		static {
			// 使用反射工具类在 ResponseFacade 类中查找名为 "response" 的字段
			Field field = ReflectionUtils.findField(ResponseFacade.class, "response");

			// 断言字段不为 null，如果为 null 则抛出异常
			Assert.state(field != null, "Incompatible Tomcat implementation");

			// 设置字段可访问
			ReflectionUtils.makeAccessible(field);

			// 将找到的字段赋值给 COYOTE_RESPONSE_FIELD 静态字段
			COYOTE_RESPONSE_FIELD = field;
		}

		TomcatServerHttpResponse(HttpServletResponse response, AsyncContext context,
								 DataBufferFactory factory, int bufferSize, ServletServerHttpRequest request) throws IOException {

			super(createTomcatHttpHeaders(response), response, context, factory, bufferSize, request);
		}

		private static HttpHeaders createTomcatHttpHeaders(HttpServletResponse response) {
			// 获取 ResponseFacade 对象
			ResponseFacade responseFacade = getResponseFacade(response);

			// 获取 ResponseFacade 对应的 ConnectorResponse 对象
			org.apache.catalina.connector.Response connectorResponse = (org.apache.catalina.connector.Response)
					ReflectionUtils.getField(COYOTE_RESPONSE_FIELD, responseFacade);

			// 断言 ConnectorResponse 对象不为 null，如果为 null 则抛出异常
			Assert.state(connectorResponse != null, "No Tomcat connector response");

			// 获取 Tomcat Response 对象
			Response tomcatResponse = connectorResponse.getCoyoteResponse();

			// 将 Tomcat Response 对象的 MimeHeaders 封装为 HttpHeaders 对象，并返回
			TomcatHeadersAdapter headers = new TomcatHeadersAdapter(tomcatResponse.getMimeHeaders());
			return new HttpHeaders(headers);
		}

		private static ResponseFacade getResponseFacade(HttpServletResponse response) {
			// 如果响应是 ResponseFacade 类型，则直接返回
			if (response instanceof ResponseFacade) {
				return (ResponseFacade) response;
			} else if (response instanceof HttpServletResponseWrapper) {
				// 如果响应是 HttpServletResponseWrapper 类型，则递归获取原始响应，并再次调用该方法
				HttpServletResponseWrapper wrapper = (HttpServletResponseWrapper) response;
				HttpServletResponse wrappedResponse = (HttpServletResponse) wrapper.getResponse();
				return getResponseFacade(wrappedResponse);
			} else {
				// 如果响应类型不是上述两种类型，则抛出异常
				throw new IllegalArgumentException("Cannot convert [" + response.getClass() +
						"] to org.apache.catalina.connector.ResponseFacade");
			}
		}

		@Override
		protected void applyHeaders() {
			// 获取原生响应对象
			HttpServletResponse response = getNativeResponse();

			// 尝试获取响应头的内容类型
			MediaType contentType = null;
			try {
				contentType = getHeaders().getContentType();
			} catch (Exception ex) {
				// 如果出现异常，尝试直接获取响应头的原始内容类型并设置到响应中
				String rawContentType = getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
				response.setContentType(rawContentType);
			}

			// 如果响应中的内容类型为 null 且从头信息中解析出的内容类型不为 null，则设置响应中的内容类型
			if (response.getContentType() == null && contentType != null) {
				response.setContentType(contentType.toString());
			}

			// 移除响应头中的 CONTENT_TYPE
			getHeaders().remove(HttpHeaders.CONTENT_TYPE);

			// 获取内容类型的字符集
			Charset charset = (contentType != null ? contentType.getCharset() : null);

			// 如果响应中的字符编码为 null ，且字符集不为 null，则设置响应中的字符编码
			if (response.getCharacterEncoding() == null && charset != null) {
				response.setCharacterEncoding(charset.name());
			}

			// 获取响应头的内容长度
			long contentLength = getHeaders().getContentLength();

			// 如果内容长度不为 -1，则设置响应的内容长度
			if (contentLength != -1) {
				response.setContentLengthLong(contentLength);
			}

			// 移除响应头中的 CONTENT_LENGTH
			getHeaders().remove(HttpHeaders.CONTENT_LENGTH);
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
			((CoyoteOutputStream) response.getOutputStream()).write(input);

			// 返回写入的字节数
			return len;
		}
	}

}
