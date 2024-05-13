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

package org.springframework.web.servlet.mvc.method.annotation;

import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * 用于异步请求处理的控制器方法返回值类型，其中一个或多个对象被写入响应。
 *
 * <p>虽然 {@link org.springframework.web.context.request.async.DeferredResult} 用于生成单个结果，
 * 但 {@code ResponseBodyEmitter} 可用于发送多个对象，其中每个对象都使用兼容的 {@link org.springframework.http.converter.HttpMessageConverter} 编写。
 *
 * <p>作为返回类型本身支持，以及作为 {@link org.springframework.http.ResponseEntity} 中的一部分支持。
 *
 * <pre>
 * &#064;RequestMapping(value="/stream", method=RequestMethod.GET)
 * public ResponseBodyEmitter handle() {
 *     ResponseBodyEmitter emitter = new ResponseBodyEmitter();
 *     // 将 emitter 传递给另一个组件...
 *     return emitter;
 * }
 *
 * // 在另一个线程中
 * emitter.send(foo1);
 *
 * // 再次发送
 * emitter.send(foo2);
 *
 * // 完成
 * emitter.complete();
 * </pre>
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 4.2
 */
public class ResponseBodyEmitter {

	/**
	 * 过期时间
	 */
	@Nullable
	private final Long timeout;

	/**
	 * 处理器
	 */
	@Nullable
	private Handler handler;

	/**
	 * 在处理程序初始化之前存储发送的数据。
	 */
	private final Set<DataWithMediaType> earlySendAttempts = new LinkedHashSet<>(8);

	/**
	 * 在处理程序初始化之前存储成功完成。
	 */
	private boolean complete;

	/**
	 * 在处理程序初始化之前存储错误。
	 */
	@Nullable
	private Throwable failure;

	/**
	 * 在 I/O 错误之后，我们不直接调用 {@link #completeWithError}，而是等待 Servlet 容器通过容器线程调用我们，
	 * 在这时调用 completeWithError。此标志用于忽略可能来自 I/O 错误线程的 complete 或 completeWithError 的进一步调用，
	 * 例如来自应用程序的 try-catch 块。
	 */
	private boolean sendFailed;

	/**
	 * 超时回到处理器
	 */
	private final DefaultCallback timeoutCallback = new DefaultCallback();

	/**
	 * 错误回调处理器
	 */
	private final ErrorCallback errorCallback = new ErrorCallback();

	/**
	 * 完成时回调处理器
	 */
	private final DefaultCallback completionCallback = new DefaultCallback();


	/**
	 * 创建一个新的 ResponseBodyEmitter 实例。
	 */
	public ResponseBodyEmitter() {
		this.timeout = null;
	}

	/**
	 * 创建一个 ResponseBodyEmitter，带有自定义的超时值。
	 * <p>默认情况下未设置，此时将使用 MVC Java 配置或 MVC 命名空间中配置的默认值，或者如果未设置，则超时时间取决于底层服务器的默认值。
	 *
	 * @param timeout 超时值（以毫秒为单位）
	 */
	public ResponseBodyEmitter(Long timeout) {
		this.timeout = timeout;
	}


	/**
	 * 返回配置的超时值（如果有）。
	 */
	@Nullable
	public Long getTimeout() {
		return this.timeout;
	}


	synchronized void initialize(Handler handler) throws IOException {
		// 设置处理程序
		this.handler = handler;

		try {
			// 遍历早期发送尝试
			for (DataWithMediaType sendAttempt : this.earlySendAttempts) {
				// 设置数据和媒体类型的内部联系
				sendInternal(sendAttempt.getData(), sendAttempt.getMediaType());
			}
		} finally {
			// 清空早期发送尝试
			this.earlySendAttempts.clear();
		}

		// 如果已完成
		if (this.complete) {
			// 如果有错误
			if (this.failure != null) {
				// 使用错误完成处理程序
				this.handler.completeWithError(this.failure);
			} else {
				// 使用完成处理程序
				this.handler.complete();
			}
		} else {
			// 在超时时调用回调函数
			this.handler.onTimeout(this.timeoutCallback);
			// 在出现错误时调用回调函数
			this.handler.onError(this.errorCallback);
			// 在完成时调用回调函数
			this.handler.onCompletion(this.completionCallback);
		}
	}

	synchronized void initializeWithError(Throwable ex) {
		// 标记为已完成
		this.complete = true;
		// 设置失败信息
		this.failure = ex;
		// 清空早期发送尝试
		this.earlySendAttempts.clear();
		// 接受错误回调
		this.errorCallback.accept(ex);
	}

	/**
	 * 在更新响应的状态码和头之后调用，如果 ResponseBodyEmitter 包装在 ResponseEntity 中，但在提交响应之前调用，
	 * 即在将响应主体写入之前调用。
	 * <p>默认实现为空。
	 */
	protected void extendResponse(ServerHttpResponse outputMessage) {
	}

	/**
	 * 将给定对象写入响应。
	 * <p>如果发生任何异常，将通过调度返回到应用服务器，Spring MVC 将通过其异常处理机制传递异常。
	 * <p><strong>注意：</strong>如果发送失败并引发 IOException，则无需调用 {@link #completeWithError(Throwable)} 进行清理。
	 * 相反，Servlet 容器会创建一个通知，导致调度，Spring MVC 调用异常解析器并完成处理。
	 *
	 * @param object 要写入的对象
	 * @throws IOException                     发生 I/O 错误时引发
	 * @throws java.lang.IllegalStateException 封装了任何其他错误
	 */
	public void send(Object object) throws IOException {
		send(object, null);
	}

	/**
	 * {@link #send(Object)} 的重载变体，还接受用于序列化给定对象的 MediaType 提示。
	 *
	 * @param object    要写入的对象
	 * @param mediaType 用于选择 HttpMessageConverter 的 MediaType 提示
	 * @throws IOException 发生 I/O 错误时引发
	 */
	public synchronized void send(Object object, @Nullable MediaType mediaType) throws IOException {
		Assert.state(!this.complete,
				"ResponseBodyEmitter has already completed" +
						(this.failure != null ? " with error: " + this.failure : ""));
		sendInternal(object, mediaType);
	}

	private void sendInternal(Object object, @Nullable MediaType mediaType) throws IOException {
		// 如果处理程序不为空，则尝试发送数据
		if (this.handler != null) {
			try {
				this.handler.send(object, mediaType);
			} catch (IOException ex) {
				// 发送失败标记为真
				this.sendFailed = true;
				throw ex;
			} catch (Throwable ex) {
				// 发送失败标记为真，并抛出异常
				this.sendFailed = true;
				throw new IllegalStateException("Failed to send " + object, ex);
			}
		} else {
			// 否则将数据添加到早期发送尝试列表中
			this.earlySendAttempts.add(new DataWithMediaType(object, mediaType));
		}
	}

	/**
	 * 通过执行调度来完成请求处理，其中 Spring MVC 再次调用，完成请求处理生命周期。
	 * <p><strong>注意：</strong>此方法应由应用程序调用以完成请求处理。不应在容器相关事件之后使用，例如发送时的错误。
	 */
	public synchronized void complete() {
		// 忽略发送失败后的调用
		// 如果发送失败，则直接返回
		if (this.sendFailed) {
			return;
		}
		// 设置完成标志为 true
		this.complete = true;
		// 如果处理程序不为空，则调用处理程序的 complete 方法
		if (this.handler != null) {
			this.handler.complete();
		}
	}

	/**
	 * 使用错误完成请求处理。
	 * <p>通过执行调度进入应用服务器，Spring MVC 将通过其异常处理机制传递异常。但请注意，在请求处理的这个阶段，
	 * 响应已提交，响应状态无法再更改。
	 * <p><strong>注意：</strong>此方法应由应用程序调用以使用错误完成请求处理。不应在容器相关事件之后使用，
	 * 例如发送时的错误。
	 */
	public synchronized void completeWithError(Throwable ex) {
		// 忽略发送失败后的调用
		// 如果发送失败，则直接返回
		if (this.sendFailed) {
			return;
		}
		// 设置完成标志为 true，并设置失败原因
		this.complete = true;
		this.failure = ex;
		// 如果处理程序不为空，则调用处理程序的 completeWithError 方法，传入失败原因
		if (this.handler != null) {
			this.handler.completeWithError(ex);
		}
	}

	/**
	 * 注册超时时调用的代码。从容器线程调用此方法，当异步请求超时时。
	 */
	public synchronized void onTimeout(Runnable callback) {
		this.timeoutCallback.setDelegate(callback);
	}

	/**
	 * 注册异步请求处理过程中出现错误时调用的代码。当处理异步请求时发生错误时，从容器线程调用此方法。
	 *
	 * @since 5.0
	 */
	public synchronized void onError(Consumer<Throwable> callback) {
		this.errorCallback.setDelegate(callback);
	}

	/**
	 * 注册异步请求完成时调用的代码。从容器线程调用此方法，当异步请求由于任何原因（包括超时和网络错误）完成时。
	 * 此方法有助于检测 {@code ResponseBodyEmitter} 实例是否不再可用。
	 */
	public synchronized void onCompletion(Runnable callback) {
		this.completionCallback.setDelegate(callback);
	}


	@Override
	public String toString() {
		return "ResponseBodyEmitter@" + ObjectUtils.getIdentityHexString(this);
	}


	/**
	 * 处理发送事件数据、事件发送完成以及在超时、错误和任何原因（包括容器方面）完成时注册回调的合同。
	 */
	interface Handler {

		void send(Object data, @Nullable MediaType mediaType) throws IOException;

		void complete();

		void completeWithError(Throwable failure);

		void onTimeout(Runnable callback);

		void onError(Consumer<Throwable> callback);

		void onCompletion(Runnable callback);
	}


	/**
	 * 要写入的数据的简单持有者，以及用于选择消息转换器进行写入的 MediaType 提示。
	 */
	public static class DataWithMediaType {

		/**
		 * 数据
		 */
		private final Object data;

		/**
		 * 媒体类型
		 */
		@Nullable
		private final MediaType mediaType;

		public DataWithMediaType(Object data, @Nullable MediaType mediaType) {
			this.data = data;
			this.mediaType = mediaType;
		}

		public Object getData() {
			return this.data;
		}

		@Nullable
		public MediaType getMediaType() {
			return this.mediaType;
		}
	}


	private class DefaultCallback implements Runnable {

		/**
		 * 异步任务
		 */
		@Nullable
		private Runnable delegate;

		public void setDelegate(Runnable delegate) {
			this.delegate = delegate;
		}

		@Override
		public void run() {
			// 设置 ResponseBodyEmitter 的 complete 标志为 true
			ResponseBodyEmitter.this.complete = true;
			// 如果委托处理器不为空，则执行委托处理器的 run 方法
			if (this.delegate != null) {
				this.delegate.run();
			}
		}
	}


	private class ErrorCallback implements Consumer<Throwable> {
		/**
		 * 异常消费函数
		 */
		@Nullable
		private Consumer<Throwable> delegate;

		public void setDelegate(Consumer<Throwable> callback) {
			this.delegate = callback;
		}

		@Override
		public void accept(Throwable t) {
			// 设置 ResponseBodyEmitter 的 complete 标志为 true
			ResponseBodyEmitter.this.complete = true;
			// 如果委托处理器不为空，则将 t 传递给委托处理器
			if (this.delegate != null) {
				this.delegate.accept(t);
			}
		}
	}

}
