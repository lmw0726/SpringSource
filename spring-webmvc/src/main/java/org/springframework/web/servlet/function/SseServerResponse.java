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

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.DelegatingServerHttpResponse;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * 用于发送 <a href="https://www.w3.org/TR/eventsource/">Server-Sent Events</a> 的 {@link ServerResponse} 实现。
 *
 * @author Arjen Poutsma
 * @since 5.3.2
 */
final class SseServerResponse extends AbstractServerResponse {
	/**
	 * SSE消费者
	 */
	private final Consumer<SseBuilder> sseConsumer;
	/**
	 * 过期时间
	 */
	@Nullable
	private final Duration timeout;


	private SseServerResponse(Consumer<SseBuilder> sseConsumer, @Nullable Duration timeout) {
		super(200, createHeaders(), emptyCookies());
		this.sseConsumer = sseConsumer;
		this.timeout = timeout;
	}

	private static HttpHeaders createHeaders() {
		// 创建一个新的 HttpHeaders 实例
		HttpHeaders headers = new HttpHeaders();
		// 设置内容类型为文本事件流
		headers.setContentType(MediaType.TEXT_EVENT_STREAM);
		// 设置缓存控制为不缓存
		headers.setCacheControl(CacheControl.noCache());
		// 返回设置好的 HttpHeaders 对象
		return headers;
	}

	private static MultiValueMap<String, Cookie> emptyCookies() {
		return CollectionUtils.toMultiValueMap(Collections.emptyMap());
	}


	@Nullable
	@Override
	protected ModelAndView writeToInternal(HttpServletRequest request, HttpServletResponse response,
										   Context context) throws ServletException, IOException {

		// 创建一个 DeferredResult 实例
		DeferredResult<?> result;
		if (this.timeout != null) {
			// 如果设置了超时时间，则使用超时时间构造
			result = new DeferredResult<>(this.timeout.toMillis());
		} else {
			// 否则使用默认构造
			result = new DeferredResult<>();
		}

		// 异步写入响应结果
		DefaultAsyncServerResponse.writeAsync(request, response, result);
		// 使用 SSE 消费者处理 SSE 请求
		this.sseConsumer.accept(new DefaultSseBuilder(response, context, result));
		// 返回空值
		return null;
	}


	public static ServerResponse create(Consumer<SseBuilder> sseConsumer, @Nullable Duration timeout) {
		Assert.notNull(sseConsumer, "SseConsumer must not be null");

		return new SseServerResponse(sseConsumer, timeout);
	}


	private static final class DefaultSseBuilder implements SseBuilder {
		/**
		 * 换行符
		 */
		private static final byte[] NL_NL = new byte[]{'\n', '\n'};

		/**
		 * 输出消息
		 */
		private final ServerHttpResponse outputMessage;

		/**
		 * 延迟结果
		 */
		private final DeferredResult<?> deferredResult;

		/**
		 * Http消息转换器
		 */
		private final List<HttpMessageConverter<?>> messageConverters;

		/**
		 * 消息构建器
		 */
		private final StringBuilder builder = new StringBuilder();

		/**
		 * 是否发送失败
		 */
		private boolean sendFailed;


		public DefaultSseBuilder(HttpServletResponse response, Context context, DeferredResult<?> deferredResult) {
			this.outputMessage = new ServletServerHttpResponse(response);
			this.deferredResult = deferredResult;
			// 从上下文中获取消息转换器
			this.messageConverters = context.messageConverters();
		}

		@Override
		public void send(Object object) throws IOException {
			data(object);
		}

		@Override
		public SseBuilder id(String id) {
			Assert.hasLength(id, "Id must not be empty");
			return field("id", id);
		}

		@Override
		public SseBuilder event(String eventName) {
			Assert.hasLength(eventName, "Name must not be empty");
			return field("event", eventName);
		}

		@Override
		public SseBuilder retry(Duration duration) {
			// 检查持续时间是否为 null
			Assert.notNull(duration, "Duration must not be null");
			// 将持续时间转换为毫秒字符串
			String millis = Long.toString(duration.toMillis());
			// 构建 "retry" 字段
			return field("retry", millis);
		}

		@Override
		public SseBuilder comment(String comment) {
			// 检查注释是否为空
			Assert.hasLength(comment, "Comment must not be empty");
			// 将注释按换行符分割为多行
			String[] lines = comment.split("\n");
			// 遍历每一行注释并添加到字段中
			for (String line : lines) {
				field("", line);
			}
			return this;
		}

		private SseBuilder field(String name, String value) {
			this.builder.append(name).append(':').append(value).append('\n');
			return this;
		}

		@Override
		public void data(Object object) throws IOException {
			Assert.notNull(object, "Object must not be null");

			if (object instanceof String) {
				// 如果数据是String类型，直接写入
				writeString((String) object);
			} else {
				// 否则写入Object对象
				writeObject(object);
			}
		}

		private void writeString(String string) throws IOException {
			// 将字符串按换行符分割为多行
			String[] lines = string.split("\n");
			// 遍历每一行并将其添加到字段中
			for (String line : lines) {
				field("data", line);
			}
			// 添加换行符
			this.builder.append('\n');

			try {
				// 将构建的消息体写入输出流并刷新
				OutputStream body = this.outputMessage.getBody();
				body.write(builderBytes());
				body.flush();
			} catch (IOException ex) {
				// 发送失败时抛出异常
				this.sendFailed = true;
				throw ex;
			} finally {
				// 重置构建器
				this.builder.setLength(0);
			}
		}

		@SuppressWarnings("unchecked")
		private void writeObject(Object data) throws IOException {
			// 添加 data 字段
			this.builder.append("data:");
			try {
				// 将构造的消息写入输出流
				this.outputMessage.getBody().write(builderBytes());

				// 获取数据的类型，并遍历消息转换器
				Class<?> dataClass = data.getClass();
				for (HttpMessageConverter<?> converter : this.messageConverters) {
					// 如果消息转换器支持将数据写为 JSON 格式
					if (converter.canWrite(dataClass, MediaType.APPLICATION_JSON)) {
						// 强制转换为支持写入 JSON 的消息转换器
						HttpMessageConverter<Object> objectConverter = (HttpMessageConverter<Object>) converter;
						// 创建一个用于写入响应的 ServerHttpResponse
						ServerHttpResponse response = new MutableHeadersServerHttpResponse(this.outputMessage);
						// 使用消息转换器将数据写入响应体，并指定媒体类型为 JSON
						objectConverter.write(data, MediaType.APPLICATION_JSON, response);
						// 写入两个换行符表示消息结束
						this.outputMessage.getBody().write(NL_NL);
						// 刷新输出流
						this.outputMessage.flush();
						return;
					}
				}
			} catch (IOException ex) {
				// 发送失败标志置为 true，并抛出异常
				this.sendFailed = true;
				throw ex;
			} finally {
				// 清空构建器内容
				this.builder.setLength(0);
			}
		}

		private byte[] builderBytes() {
			return this.builder.toString().getBytes(StandardCharsets.UTF_8);
		}

		@Override
		public void error(Throwable t) {
			// 如果发送失败，则直接返回，不再执行后续操作
			if (this.sendFailed) {
				return;
			}
			// 将异常设置为DeferredResult的错误结果
			this.deferredResult.setErrorResult(t);
		}

		@Override
		public void complete() {
			// 如果发送失败，则直接返回，不再执行后续操作
			if (this.sendFailed) {
				return;
			}
			try {
				// 刷新输出消息
				this.outputMessage.flush();
				// 将DeferredResult设置为成功结果
				this.deferredResult.setResult(null);
			} catch (IOException ex) {
				// 如果发生IO异常，则将异常设置为DeferredResult的错误结果
				this.deferredResult.setErrorResult(ex);
			}
		}

		@Override
		public SseBuilder onTimeout(Runnable onTimeout) {
			this.deferredResult.onTimeout(onTimeout);
			return this;
		}

		@Override
		public SseBuilder onError(Consumer<Throwable> onError) {
			this.deferredResult.onError(onError);
			return this;
		}

		@Override
		public SseBuilder onComplete(Runnable onCompletion) {
			this.deferredResult.onCompletion(onCompletion);
			return this;
		}


		/**
		 * 包装以在 HttpMessageConverter 改变 HttpHeaders 时静默忽略抛出异常的类。
		 */
		private static final class MutableHeadersServerHttpResponse extends DelegatingServerHttpResponse {
			/**
			 * 可变的请求头
			 */
			private final HttpHeaders mutableHeaders = new HttpHeaders();

			public MutableHeadersServerHttpResponse(ServerHttpResponse delegate) {
				super(delegate);
				this.mutableHeaders.putAll(delegate.getHeaders());
			}

			@Override
			public HttpHeaders getHeaders() {
				return this.mutableHeaders;
			}

		}

	}
}
