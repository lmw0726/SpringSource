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

package org.springframework.web.servlet.mvc.method.annotation;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 用于发送<a href="https://www.w3.org/TR/eventsource/">服务器发送的事件</a>的 {@link ResponseBodyEmitter} 的特化版本。
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 4.2
 */
public class SseEmitter extends ResponseBodyEmitter {

	/**
	 * 文本类型的媒体类型
	 */
	private static final MediaType TEXT_PLAIN = new MediaType("text", "plain", StandardCharsets.UTF_8);

	/**
	 * 创建一个新的 SseEmitter 实例。
	 */
	public SseEmitter() {
		super();
	}

	/**
	 * 使用自定义超时值创建 SseEmitter。
	 * <p>默认情况下未设置，此时将使用 MVC Java 配置或 MVC 命名空间中配置的默认值，或者如果未设置，则超时时间取决于底层服务器的默认值。
	 *
	 * @param timeout 超时值（以毫秒为单位）
	 * @since 4.2.2
	 */
	public SseEmitter(Long timeout) {
		super(timeout);
	}


	@Override
	protected void extendResponse(ServerHttpResponse outputMessage) {
		// 扩展响应
		super.extendResponse(outputMessage);

		// 获取响应头
		HttpHeaders headers = outputMessage.getHeaders();
		// 如果响应头中的内容类型为空，则设置为文本事件流
		if (headers.getContentType() == null) {
			headers.setContentType(MediaType.TEXT_EVENT_STREAM);
		}
	}

	/**
	 * 以单个 SSE "data" 行的格式发送对象。等效于：
	 * <pre>
	 * // SseEmitter 的静态导入
	 * SseEmitter emitter = new SseEmitter();
	 * emitter.send(event().data(myObject));
	 * </pre>
	 * <p>有关异常处理的重要说明，请参见 {@link ResponseBodyEmitter#send(Object) 父级 Javadoc}。
	 *
	 * @param object 要写入的对象
	 * @throws IOException                     发生 I/O 错误时引发
	 * @throws java.lang.IllegalStateException 封装了任何其他错误
	 */
	@Override
	public void send(Object object) throws IOException {
		send(object, null);
	}

	/**
	 * 以单个 SSE "data" 行的格式发送对象。等效于：
	 * <pre>
	 * // SseEmitter 的静态导入
	 * SseEmitter emitter = new SseEmitter();
	 * emitter.send(event().data(myObject, MediaType.APPLICATION_JSON));
	 * </pre>
	 * <p>有关异常处理的重要说明，请参见 {@link ResponseBodyEmitter#send(Object) 父级 Javadoc}。
	 *
	 * @param object    要写入的对象
	 * @param mediaType 用于选择 HttpMessageConverter 的 MediaType 提示
	 * @throws IOException 发生 I/O 错误时引发
	 */
	@Override
	public void send(Object object, @Nullable MediaType mediaType) throws IOException {
		send(event().data(object, mediaType));
	}

	/**
	 * 使用给定的构建器发送准备好的 SSE 事件。例如：
	 * <pre>
	 * // SseEmitter 的静态导入
	 * SseEmitter emitter = new SseEmitter();
	 * emitter.send(event().name("update").id("1").data(myObject));
	 * </pre>
	 *
	 * @param builder 用于 SSE 格式化事件的构建器
	 * @throws IOException 发生 I/O 错误时引发
	 */
	public void send(SseEventBuilder builder) throws IOException {
		// 构建数据集合
		Set<DataWithMediaType> dataToSend = builder.build();
		// 使用同步块确保线程安全
		synchronized (this) {
			// 遍历数据集合
			for (DataWithMediaType entry : dataToSend) {
				// 发送数据
				super.send(entry.getData(), entry.getMediaType());
			}
		}
	}

	@Override
	public String toString() {
		return "SseEmitter@" + ObjectUtils.getIdentityHexString(this);
	}


	public static SseEventBuilder event() {
		return new SseEventBuilderImpl();
	}


	/**
	 * SSE 事件的构建器。
	 */
	public interface SseEventBuilder {

		/**
		 * 添加一个 SSE "id" 行。
		 */
		SseEventBuilder id(String id);

		/**
		 * 添加一个 SSE "event" 行。
		 */
		SseEventBuilder name(String eventName);

		/**
		 * 添加一个 SSE "retry" 行。
		 */
		SseEventBuilder reconnectTime(long reconnectTimeMillis);

		/**
		 * 添加一个 SSE "comment" 行。
		 */
		SseEventBuilder comment(String comment);

		/**
		 * 添加一个 SSE "data" 行。
		 */
		SseEventBuilder data(Object object);

		/**
		 * 添加一个 SSE "data" 行。
		 */
		SseEventBuilder data(Object object, @Nullable MediaType mediaType);

		/**
		 * 返回要通过 {@link #send(Object, MediaType)} 写入的一个或多个对象-MediaType 对。
		 *
		 * @since 4.2.3
		 */
		Set<DataWithMediaType> build();
	}


	/**
	 * SSE 事件构建器的默认实现。
	 */
	private static class SseEventBuilderImpl implements SseEventBuilder {

		/**
		 * 要发送的带有媒体类型的数据集合
		 */
		private final Set<DataWithMediaType> dataToSend = new LinkedHashSet<>(4);

		/**
		 * 字符串构建器
		 */
		@Nullable
		private StringBuilder sb;

		@Override
		public SseEventBuilder id(String id) {
			append("id:").append(id).append('\n');
			return this;
		}

		@Override
		public SseEventBuilder name(String name) {
			append("event:").append(name).append('\n');
			return this;
		}

		@Override
		public SseEventBuilder reconnectTime(long reconnectTimeMillis) {
			append("retry:").append(String.valueOf(reconnectTimeMillis)).append('\n');
			return this;
		}

		@Override
		public SseEventBuilder comment(String comment) {
			append(':').append(comment).append('\n');
			return this;
		}

		@Override
		public SseEventBuilder data(Object object) {
			return data(object, null);
		}

		@Override
		public SseEventBuilder data(Object object, @Nullable MediaType mediaType) {
			append("data:");
			// 保存附加的文本
			saveAppendedText();
			// 添加带有媒体类型的构建器
			this.dataToSend.add(new DataWithMediaType(object, mediaType));
			append('\n');
			return this;

		}

		SseEventBuilderImpl append(String text) {
			if (this.sb == null) {
				this.sb = new StringBuilder();
			}
			this.sb.append(text);
			return this;
		}

		SseEventBuilderImpl append(char ch) {
			if (this.sb == null) {
				this.sb = new StringBuilder();
			}
			this.sb.append(ch);
			return this;
		}

		@Override
		public Set<DataWithMediaType> build() {
			if (!StringUtils.hasLength(this.sb) && this.dataToSend.isEmpty()) {
				return Collections.emptySet();
			}
			append('\n');
			saveAppendedText();
			return this.dataToSend;
		}

		private void saveAppendedText() {
			if (this.sb != null) {
				// 添加带有文本消息媒体类型的数据
				this.dataToSend.add(new DataWithMediaType(this.sb.toString(), TEXT_PLAIN));
				// 清空字符串构建器
				this.sb = null;
			}
		}
	}

}
