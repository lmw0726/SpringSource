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

package org.springframework.web.socket.sockjs.client;

import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;
import org.springframework.util.concurrent.SettableListenableFuture;
import org.springframework.web.client.*;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.sockjs.frame.SockJsFrame;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

/**
 * An {@code XhrTransport} implementation that uses a
 * {@link org.springframework.web.client.RestTemplate RestTemplate}.
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public class RestTemplateXhrTransport extends AbstractXhrTransport {

	private final RestOperations restTemplate;

	private TaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();


	public RestTemplateXhrTransport() {
		this(new RestTemplate());
	}

	public RestTemplateXhrTransport(RestOperations restTemplate) {
		Assert.notNull(restTemplate, "'restTemplate' is required");
		this.restTemplate = restTemplate;
	}


	/**
	 * Return the configured {@code RestTemplate}.
	 */
	public RestOperations getRestTemplate() {
		return this.restTemplate;
	}

	/**
	 * Configure the {@code TaskExecutor} to use to execute XHR receive requests.
	 * <p>By default {@link org.springframework.core.task.SimpleAsyncTaskExecutor
	 * SimpleAsyncTaskExecutor} is configured which creates a new thread every
	 * time the transports connects.
	 */
	public void setTaskExecutor(TaskExecutor taskExecutor) {
		Assert.notNull(taskExecutor, "TaskExecutor must not be null");
		this.taskExecutor = taskExecutor;
	}

	/**
	 * Return the configured {@code TaskExecutor}.
	 */
	public TaskExecutor getTaskExecutor() {
		return this.taskExecutor;
	}


	@Override
	protected void connectInternal(final TransportRequest transportRequest, final WebSocketHandler handler,
								   final URI receiveUrl, final HttpHeaders handshakeHeaders, final XhrClientSockJsSession session,
								   final SettableListenableFuture<WebSocketSession> connectFuture) {

		getTaskExecutor().execute(() -> {
			HttpHeaders httpHeaders = transportRequest.getHttpRequestHeaders();
			XhrRequestCallback requestCallback = new XhrRequestCallback(handshakeHeaders);
			XhrRequestCallback requestCallbackAfterHandshake = new XhrRequestCallback(httpHeaders);
			XhrReceiveExtractor responseExtractor = new XhrReceiveExtractor(session);
			while (true) {
				if (session.isDisconnected()) {
					session.afterTransportClosed(null);
					break;
				}
				try {
					if (logger.isTraceEnabled()) {
						logger.trace("Starting XHR receive request, url=" + receiveUrl);
					}
					getRestTemplate().execute(receiveUrl, HttpMethod.POST, requestCallback, responseExtractor);
					requestCallback = requestCallbackAfterHandshake;
				} catch (Exception ex) {
					if (!connectFuture.isDone()) {
						connectFuture.setException(ex);
					} else {
						session.handleTransportError(ex);
						session.afterTransportClosed(new CloseStatus(1006, ex.getMessage()));
					}
					break;
				}
			}
		});
	}

	@Override
	protected ResponseEntity<String> executeInfoRequestInternal(URI infoUrl, HttpHeaders headers) {
		RequestCallback requestCallback = new XhrRequestCallback(headers);
		return nonNull(this.restTemplate.execute(infoUrl, HttpMethod.GET, requestCallback, textResponseExtractor));
	}

	@Override
	public ResponseEntity<String> executeSendRequestInternal(URI url, HttpHeaders headers, TextMessage message) {
		RequestCallback requestCallback = new XhrRequestCallback(headers, message.getPayload());
		return nonNull(this.restTemplate.execute(url, HttpMethod.POST, requestCallback, textResponseExtractor));
	}

	private static <T> T nonNull(@Nullable T result) {
		Assert.state(result != null, "No result");
		return result;
	}


	/**
	 * 一个简单的 响应提取器，用于将响应体读取为String。
	 */
	private static final ResponseExtractor<ResponseEntity<String>> textResponseExtractor =
			response -> {
				// 将响应体转换为字符串
				String body = StreamUtils.copyToString(response.getBody(), SockJsFrame.CHARSET);

				// 创建一个带有状态码、头信息和字符串响应体的 ResponseEntity 对象
				return ResponseEntity.status(response.getRawStatusCode())
						.headers(response.getHeaders())
						.body(body);
			};


	/**
	 * 用于添加标头和（可选的）字符串内容的 RequestCallback。
	 */
	private static class XhrRequestCallback implements RequestCallback {
		/**
		 * Http头部
		 */
		private final HttpHeaders headers;

		/**
		 * 请求体
		 */
		@Nullable
		private final String body;

		public XhrRequestCallback(HttpHeaders headers) {
			this(headers, null);
		}

		public XhrRequestCallback(HttpHeaders headers, @Nullable String body) {
			this.headers = headers;
			this.body = body;
		}

		@Override
		public void doWithRequest(ClientHttpRequest request) throws IOException {
			// 将自定义的请求头添加到请求中
			request.getHeaders().putAll(this.headers);
			if (this.body != null) {
				// 如果请求体不为空
				if (request instanceof StreamingHttpOutputMessage) {
					// 如果请求对象是 流式处理Http输出消息 的实例，使用流式输出设置请求体
					((StreamingHttpOutputMessage) request).setBody(outputStream ->
							StreamUtils.copy(this.body, SockJsFrame.CHARSET, outputStream));
				} else {
					// 否则，直接将请求体内容拷贝到请求对象的输出流中
					StreamUtils.copy(this.body, SockJsFrame.CHARSET, request.getBody());
				}
			}
		}
	}

	/**
	 * 将HTTP响应的主体拆分为SockJS帧，并将其委托给{@link XhrClientSockJsSession}。
	 */
	private class XhrReceiveExtractor implements ResponseExtractor<Object> {
		/**
		 * SockJS会话
		 */
		private final XhrClientSockJsSession sockJsSession;

		public XhrReceiveExtractor(XhrClientSockJsSession sockJsSession) {
			this.sockJsSession = sockJsSession;
		}

		@Override
		public Object extractData(ClientHttpResponse response) throws IOException {
			// 解析原始状态码为 HttpStatus
			HttpStatus httpStatus = HttpStatus.resolve(response.getRawStatusCode());

			if (httpStatus == null) {
				// 如果无法解析状态码，则抛出未知的 HTTP 状态码异常
				throw new UnknownHttpStatusCodeException(
						response.getRawStatusCode(), response.getStatusText(), response.getHeaders(), null, null);
			}

			if (httpStatus != HttpStatus.OK) {
				// 如果状态码不是 OK，则抛出 HTTP 服务器错误异常
				throw new HttpServerErrorException(
						httpStatus, response.getStatusText(), response.getHeaders(), null, null);
			}

			if (logger.isTraceEnabled()) {
				// 如果日志级别为 TRACE，则记录收到的响应头
				logger.trace("XHR receive headers: " + response.getHeaders());
			}

			// 获取响应体的输入流和一个用于缓存数据的输出流
			InputStream is = response.getBody();
			ByteArrayOutputStream os = new ByteArrayOutputStream();

			// 循环读取输入流中的数据，直到结束
			while (true) {
				// 如果 SockJS 会话已经断开连接
				if (this.sockJsSession.isDisconnected()) {
					if (logger.isDebugEnabled()) {
						logger.debug("SockJS sockJsSession closed, closing response.");
					}
					// 关闭响应并退出循环
					response.close();
					break;
				}
				// 读取一个字节
				int b = is.read();
				// 如果已到达流的末尾
				if (b == -1) {
					// 如果输出流中有数据，则处理帧数据
					if (os.size() > 0) {
						handleFrame(os);
					}
					// 如果日志级别为 TRACE，则记录接收完成
					if (logger.isTraceEnabled()) {
						logger.trace("XHR receive completed");
					}
					// 退出循环
					break;
				}
				// 如果遇到换行符，则处理当前帧数据
				if (b == '\n') {
					handleFrame(os);
				} else {
					// 否则写入输出流
					os.write(b);
				}
			}

			// 返回 null，表示没有返回值
			return null;
		}

		private void handleFrame(ByteArrayOutputStream os) throws IOException {
			// 将输出流中的内容转换为字符串
			String content = os.toString(SockJsFrame.CHARSET.name());

			// 重置输出流
			os.reset();

			// 如果日志级别为 TRACE，则记录收到的内容
			if (logger.isTraceEnabled()) {
				logger.trace("XHR receive content: " + content);
			}

			if (!PRELUDE.equals(content)) {
				// 如果收到的内容不是预设值，则处理收到的帧内容
				this.sockJsSession.handleFrame(content);
			}
		}
	}

}
