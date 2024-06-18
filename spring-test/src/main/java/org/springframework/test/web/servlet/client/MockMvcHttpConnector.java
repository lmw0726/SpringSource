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

package org.springframework.test.web.servlet.client;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.*;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.http.client.reactive.ClientHttpResponse;
import org.springframework.http.codec.multipart.DefaultPartHttpMessageReader;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.Part;
import org.springframework.lang.Nullable;
import org.springframework.mock.http.client.reactive.MockClientHttpRequest;
import org.springframework.mock.http.client.reactive.MockClientHttpResponse;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockPart;
import org.springframework.test.web.reactive.server.MockServerClientHttpResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import reactor.core.publisher.Mono;

import javax.servlet.http.Cookie;
import java.io.StringWriter;
import java.net.URI;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;

/**
 * 通过调用 {@link MockMvc} 处理请求的连接器，而不是通过 HTTP 进行实际请求。
 *
 * @author Rossen Stoyanchev
 * @since 5.3
 */
public class MockMvcHttpConnector implements ClientHttpConnector {

	/**
	 * 多部分读取器
	 */
	private static final DefaultPartHttpMessageReader MULTIPART_READER = new DefaultPartHttpMessageReader();

	/**
	 * 超时时间
	 */
	private static final Duration TIMEOUT = Duration.ofSeconds(5);

	/**
	 * 模拟MVC
	 */
	private final MockMvc mockMvc;


	public MockMvcHttpConnector(MockMvc mockMvc) {
		this.mockMvc = mockMvc;
	}


	@Override
	public Mono<ClientHttpResponse> connect(
			HttpMethod method, URI uri, Function<? super ClientHttpRequest, Mono<Void>> requestCallback) {

		// 创建请求构建器
		RequestBuilder requestBuilder = adaptRequest(method, uri, requestCallback);

		try {
			// 使用 MockMvc 执行请求，并返回结果
			MvcResult mvcResult = this.mockMvc.perform(requestBuilder).andReturn();

			// 如果请求是异步启动的
			if (mvcResult.getRequest().isAsyncStarted()) {
				// 等待异步结果
				mvcResult.getAsyncResult();
				// 重新执行请求
				mvcResult = this.mockMvc.perform(asyncDispatch(mvcResult)).andReturn();
			}

			// 将结果适配为响应并返回 Mono
			return Mono.just(adaptResponse(mvcResult));
		} catch (Exception ex) {
			// 如果出现异常，返回包含错误的 Mono
			return Mono.error(ex);
		}
	}

	private RequestBuilder adaptRequest(
			HttpMethod httpMethod, URI uri, Function<? super ClientHttpRequest, Mono<Void>> requestCallback) {

		// 使用 HTTP 方法和 URI，创建一个 MockClientHttpRequest 对象
		MockClientHttpRequest httpRequest = new MockClientHttpRequest(httpMethod, uri);

		// 使用原子引用存储请求体的内容
		AtomicReference<byte[]> contentRef = new AtomicReference<>();
		httpRequest.setWriteHandler(dataBuffers ->
				// 将多个 DataBuffer 合并为一个，并处理其内容
				DataBufferUtils.join(dataBuffers)
						.doOnNext(buffer -> {
							// 读取 DataBuffer 中的字节数据并存储到 字节数组 中
							byte[] bytes = new byte[buffer.readableByteCount()];
							// 读取字节数组
							buffer.read(bytes);
							// 释放 DataBuffer
							DataBufferUtils.release(buffer);
							// 将读取的字节数据存入 内容引用
							contentRef.set(bytes);
						})
						.then());

		// 初始化客户端请求，应用请求回调函数并等待完成
		requestCallback.apply(httpRequest).block(TIMEOUT);

		// 初始化 MockHttpServletRequestBuilder 对象
		MockHttpServletRequestBuilder requestBuilder =
				initRequestBuilder(httpMethod, uri, httpRequest, contentRef.get());

		// 将 MockClientHttpRequest 的头部信息设置到 请求构建器 中
		requestBuilder.headers(httpRequest.getHeaders());

		// 遍历每一个请求的HttpCookie列表
		for (List<HttpCookie> cookies : httpRequest.getCookies().values()) {
			// 遍历 Cookie 列表
			for (HttpCookie cookie : cookies) {
				// 将 Cookie 添加到请求构建器
				requestBuilder.cookie(new Cookie(cookie.getName(), cookie.getValue()));
			}
		}

		// 返回构建好的 MockHttpServletRequestBuilder 对象
		return requestBuilder;
	}

	private MockHttpServletRequestBuilder initRequestBuilder(
			HttpMethod httpMethod, URI uri, MockClientHttpRequest httpRequest, @Nullable byte[] bytes) {

		// 获取请求头中的 Content-Type
		String contentType = httpRequest.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);

		// 如果 Content-Type 不是以 "multipart/" 开头
		if (!StringUtils.startsWithIgnoreCase(contentType, "multipart/")) {
			// 创建普通的 MockHttpServletRequestBuilder 对象
			MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.request(httpMethod, uri);

			// 如果 字节数组 不为空
			if (!ObjectUtils.isEmpty(bytes)) {
				// 设置请求体内容
				requestBuilder.content(bytes);
			}
			// 返回普通的 MockHttpServletRequestBuilder 对象
			return requestBuilder;
		}

		// 如果 Content-Type 是 "multipart/" 开头，解析多部分请求以适应 Servlet 部分
		MockMultipartHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.multipart(httpMethod, uri);

		// 断言 bytes 不为空，因为这是一个多部分请求
		Assert.notNull(bytes, "No multipart content");

		// 构建 ReactiveHttpInputMessage 对象，用于读取请求体
		ReactiveHttpInputMessage inputMessage = MockServerHttpRequest.post(uri.toString())
				.headers(httpRequest.getHeaders())
				.body(Mono.just(DefaultDataBufferFactory.sharedInstance.wrap(bytes)));

		// 使用 多部分读取器 读取多部分请求
		MULTIPART_READER.read(ResolvableType.forClass(Part.class), inputMessage, Collections.emptyMap())
				.flatMap(part ->
						DataBufferUtils.join(part.content())
								.doOnNext(buffer -> {
									// 创建 部分字节数组
									byte[] partBytes = new byte[buffer.readableByteCount()];
									// 读取 DataBuffer 中的字节数据
									buffer.read(partBytes);
									// 释放 DataBuffer
									DataBufferUtils.release(buffer);

									// 适应 javax.servlet.http.Part...
									MockPart mockPart = (part instanceof FilePart ?
											new MockPart(part.name(), ((FilePart) part).filename(), partBytes) :
											new MockPart(part.name(), partBytes));
									// 设置 MockPart 的头部信息
									mockPart.getHeaders().putAll(part.headers());
									// 添加 MockPart 到 请求构建器
									requestBuilder.part(mockPart);
								}))
				// 阻塞直到读取完成
				.blockLast(TIMEOUT);

		// 返回构建好的 MockMultipartHttpServletRequestBuilder 对象
		return requestBuilder;
	}

	private MockClientHttpResponse adaptResponse(MvcResult mvcResult) {
		// 创建 MockClientHttpResponse 对象
		MockClientHttpResponse clientResponse = new MockMvcServerClientHttpResponse(mvcResult);

		// 获取 MockHttpServletResponse 对象
		MockHttpServletResponse servletResponse = mvcResult.getResponse();

		// 遍历响应头的名称
		for (String header : servletResponse.getHeaderNames()) {
			// 遍历每个响应头的所有值
			for (String value : servletResponse.getHeaders(header)) {
				// 将响应头和值添加到 客户端响应 中
				clientResponse.getHeaders().add(header, value);
			}
		}

		// 检查是否有转发的 URL
		if (servletResponse.getForwardedUrl() != null) {
			// 如果有转发的 URL，将其添加到响应头中
			clientResponse.getHeaders().add("Forwarded-Url", servletResponse.getForwardedUrl());
		}

		// 遍历所有的 Cookie
		for (Cookie cookie : servletResponse.getCookies()) {
			// 使用 Servlet响应 中的 Cookie，创建 响应Cookie 对象
			ResponseCookie httpCookie =
					ResponseCookie.fromClientResponse(cookie.getName(), cookie.getValue())
							.maxAge(Duration.ofSeconds(cookie.getMaxAge()))
							.domain(cookie.getDomain())
							.path(cookie.getPath())
							.secure(cookie.getSecure())
							.httpOnly(cookie.isHttpOnly())
							.build();
			// 将 响应Cookie 添加到 客户端响应 中
			clientResponse.getCookies().add(httpCookie.getName(), httpCookie);
		}

		// 获取响应内容的字节数组
		byte[] bytes = servletResponse.getContentAsByteArray();

		// 使用 默认数据缓冲区工厂 将字节数组包装为 数据缓冲区
		DefaultDataBuffer dataBuffer = DefaultDataBufferFactory.sharedInstance.wrap(bytes);

		// 设置 客户端响应 的响应体
		clientResponse.setBody(Mono.just(dataBuffer));

		// 返回构建好的 客户端响应 对象
		return clientResponse;
	}


	private static class MockMvcServerClientHttpResponse
			extends MockClientHttpResponse implements MockServerClientHttpResponse {
		/**
		 * MVC结果
		 */
		private final MvcResult mvcResult;


		public MockMvcServerClientHttpResponse(MvcResult result) {
			super(result.getResponse().getStatus());
			this.mvcResult = new PrintingMvcResult(result);
		}

		@Override
		public Object getServerResult() {
			return this.mvcResult;
		}
	}


	private static class PrintingMvcResult implements MvcResult {
		/**
		 * MVC结果
		 */
		private final MvcResult mvcResult;

		public PrintingMvcResult(MvcResult mvcResult) {
			this.mvcResult = mvcResult;
		}

		@Override
		public MockHttpServletRequest getRequest() {
			return this.mvcResult.getRequest();
		}

		@Override
		public MockHttpServletResponse getResponse() {
			return this.mvcResult.getResponse();
		}

		@Nullable
		@Override
		public Object getHandler() {
			return this.mvcResult.getHandler();
		}

		@Nullable
		@Override
		public HandlerInterceptor[] getInterceptors() {
			return this.mvcResult.getInterceptors();
		}

		@Nullable
		@Override
		public ModelAndView getModelAndView() {
			return this.mvcResult.getModelAndView();
		}

		@Nullable
		@Override
		public Exception getResolvedException() {
			return this.mvcResult.getResolvedException();
		}

		@Override
		public FlashMap getFlashMap() {
			return this.mvcResult.getFlashMap();
		}

		@Override
		public Object getAsyncResult() {
			return this.mvcResult.getAsyncResult();
		}

		@Override
		public Object getAsyncResult(long timeToWait) {
			return this.mvcResult.getAsyncResult(timeToWait);
		}

		@Override
		public String toString() {
			// 创建 字符串写入器 对象
			StringWriter writer = new StringWriter();

			try {
				// 将处理结果写入 字符串写入器
				MockMvcResultHandlers.print(writer).handle(this);
			} catch (Exception ex) {
				// 捕获处理过程中发生的异常，并记录异常信息到 字符串写入器 中
				writer.append("Unable to format ")
						.append(String.valueOf(this))
						.append(": ")
						.append(ex.getMessage());
			}

			// 返回 字符串写入器 中的内容
			return writer.toString();
		}
	}

}
