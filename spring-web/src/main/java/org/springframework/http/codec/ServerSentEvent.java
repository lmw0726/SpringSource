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

package org.springframework.http.codec;

import org.springframework.lang.Nullable;

import java.time.Duration;

/**
 * 表示用于 Spring 响应式 Web 支持的服务器发送事件（Server-Sent Event）。
 * {@code Flux<ServerSentEvent>} 或 {@code Observable<ServerSentEvent>} 是
 * Spring MVC 的 {@code SseEmitter} 的响应式等价物。
 *
 * @param <T> 此事件包含的数据类型
 * @author Sebastien Deleuze
 * @author Arjen Poutsma
 * @see ServerSentEventHttpMessageWriter
 * @see <a href="https://www.w3.org/TR/eventsource/">Server-Sent Events W3C 推荐</a>
 * @since 5.0
 */
public final class ServerSentEvent<T> {

	/**
	 * 事件ID
	 */
	@Nullable
	private final String id;

	/**
	 * 事件类型
	 */
	@Nullable
	private final String event;

	/**
	 * 重新连接时间
	 */
	@Nullable
	private final Duration retry;

	/**
	 * 事件注释
	 */
	@Nullable
	private final String comment;

	/**
	 * 消息的数据字段
	 */
	@Nullable
	private final T data;


	private ServerSentEvent(@Nullable String id, @Nullable String event, @Nullable Duration retry,
							@Nullable String comment, @Nullable T data) {

		this.id = id;
		this.event = event;
		this.retry = retry;
		this.comment = comment;
		this.data = data;
	}


	/**
	 * 返回此事件的 {@code id} 字段（如果可用）。
	 */
	@Nullable
	public String id() {
		return this.id;
	}

	/**
	 * 返回此事件的 {@code event} 字段，如果可用。
	 */
	@Nullable
	public String event() {
		return this.event;
	}

	/**
	 * 返回此事件的 {@code retry} 字段（如果可用）。
	 */
	@Nullable
	public Duration retry() {
		return this.retry;
	}

	/**
	 * 返回此事件的注释（如果可用）。
	 */
	@Nullable
	public String comment() {
		return this.comment;
	}

	/**
	 * 返回此事件的 {@code data} 字段（如果可用）。
	 */
	@Nullable
	public T data() {
		return this.data;
	}


	@Override
	public String toString() {
		return ("ServerSentEvent [id = '" + this.id + "\', event='" + this.event + "\', retry=" +
				this.retry + ", comment='" + this.comment + "', data=" + this.data + ']');
	}


	/**
	 * 返回一个 {@code SseEvent} 的构建器。
	 *
	 * @param <T> 此事件包含的数据类型
	 * @return 构建器
	 */
	public static <T> Builder<T> builder() {
		return new BuilderImpl<>();
	}

	/**
	 * 返回一个 {@code SseEvent} 的构建器，并用给定的 {@linkplain #data() 数据} 填充。
	 *
	 * @param <T>  此事件包含的数据类型
	 * @param data 要填充的数据
	 * @return 构建器
	 */
	public static <T> Builder<T> builder(T data) {
		return new BuilderImpl<>(data);
	}


	/**
	 * {@code SseEvent} 的可变构建器。
	 *
	 * @param <T> 此事件包含的数据类型
	 */
	public interface Builder<T> {

		/**
		 * 设置 {@code id} 字段的值。
		 *
		 * @param id id 字段的值
		 * @return {@code this} 构建器
		 */
		Builder<T> id(String id);

		/**
		 * 设置 {@code event} 字段的值。
		 *
		 * @param event event 字段的值
		 * @return {@code this} 构建器
		 */
		Builder<T> event(String event);

		/**
		 * 设置 {@code retry} 字段的值。
		 *
		 * @param retry retry 字段的值
		 * @return {@code this} 构建器
		 */
		Builder<T> retry(Duration retry);

		/**
		 * 设置 SSE 注释。如果提供了多行注释，它将被转换为多个 SSE 注释行，如 Server-Sent Events W3C 推荐中定义的那样。
		 *
		 * @param comment 要设置的注释
		 * @return {@code this} 构建器
		 */
		Builder<T> comment(String comment);

		/**
		 * 设置 {@code data} 字段的值。如果 {@code data} 参数是多行 {@code String}，它将被转换为多个 {@code data} 字段行，
		 * 如 Server-Sent Events W3C 推荐中定义的那样。如果 {@code data} 不是 String，
		 * 它将通过 {@linkplain org.springframework.http.codec.json.Jackson2JsonEncoder 编码} 为 JSON。
		 *
		 * @param data data 字段的值
		 * @return {@code this} 构建器
		 */
		Builder<T> data(@Nullable T data);

		/**
		 * 构建事件。
		 *
		 * @return 构建的事件
		 */
		ServerSentEvent<T> build();
	}


	private static class BuilderImpl<T> implements Builder<T> {

		/**
		 * 事件ID
		 */
		@Nullable
		private String id;

		/**
		 * 事件类型
		 */
		@Nullable
		private String event;

		/**
		 * 重新连接的时间
		 */
		@Nullable
		private Duration retry;

		/**
		 * 事件注释
		 */
		@Nullable
		private String comment;

		/**
		 * 消息的数据字段
		 */
		@Nullable
		private T data;

		public BuilderImpl() {
		}

		public BuilderImpl(T data) {
			this.data = data;
		}

		@Override
		public Builder<T> id(String id) {
			this.id = id;
			return this;
		}

		@Override
		public Builder<T> event(String event) {
			this.event = event;
			return this;
		}

		@Override
		public Builder<T> retry(Duration retry) {
			this.retry = retry;
			return this;
		}

		@Override
		public Builder<T> comment(String comment) {
			this.comment = comment;
			return this;
		}

		@Override
		public Builder<T> data(@Nullable T data) {
			this.data = data;
			return this;
		}

		@Override
		public ServerSentEvent<T> build() {
			return new ServerSentEvent<>(this.id, this.event, this.retry, this.comment, this.data);
		}
	}

}
