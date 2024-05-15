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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.ResolvableType;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MimeType;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 一个私有的辅助类，用于处理可以通过 {@link ReactiveAdapterRegistry} 适配为 Reactor 流 {@link Publisher} 的“响应式”返回值类型。
 *
 * <p>在流式媒体类型存在或基于泛型类型时，此类返回的值可能被桥接到 {@link ResponseBodyEmitter} 以进行流式处理。
 *
 * <p>对于所有其他情况，{@code Publisher} 输出都会被收集，并桥接到 {@link DeferredResult} 以进行标准的异步请求处理。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
class ReactiveTypeHandler {
	/**
	 * 流式传输超时值，-1 表示无超时限制。
	 */
	private static final long STREAMING_TIMEOUT_VALUE = -1;

	/**
	 * JSON 流式传输的媒体类型列表。
	 */
	@SuppressWarnings("deprecation")
	private static final List<MediaType> JSON_STREAMING_MEDIA_TYPES =
			Arrays.asList(MediaType.APPLICATION_NDJSON, MediaType.APPLICATION_STREAM_JSON);

	/**
	 * 日志记录器。
	 */
	private static final Log logger = LogFactory.getLog(ReactiveTypeHandler.class);

	/**
	 * 反应式适配器注册表。
	 */
	private final ReactiveAdapterRegistry adapterRegistry;

	/**
	 * 任务执行器。
	 */
	private final TaskExecutor taskExecutor;

	/**
	 * 内容协商管理器。
	 */
	private final ContentNegotiationManager contentNegotiationManager;

	/**
	 * 任务执行器警告标志。
	 */
	private boolean taskExecutorWarning;


	public ReactiveTypeHandler() {
		this(ReactiveAdapterRegistry.getSharedInstance(), new SyncTaskExecutor(), new ContentNegotiationManager());
	}

	ReactiveTypeHandler(ReactiveAdapterRegistry registry, TaskExecutor executor, ContentNegotiationManager manager) {
		Assert.notNull(registry, "ReactiveAdapterRegistry is required");
		Assert.notNull(executor, "TaskExecutor is required");
		Assert.notNull(manager, "ContentNegotiationManager is required");
		this.adapterRegistry = registry;
		this.taskExecutor = executor;
		this.contentNegotiationManager = manager;
		// 如果任务执行器是 SimpleAsyncTaskExecutor 或 SyncTaskExecutor，则将 任务执行器警告标志 设置为true。
		this.taskExecutorWarning =
				(executor instanceof SimpleAsyncTaskExecutor || executor instanceof SyncTaskExecutor);
	}


	/**
	 * 检查类型是否可以适配为 Reactive Streams 的 {@link Publisher}。
	 */
	public boolean isReactiveType(Class<?> type) {
		return (this.adapterRegistry.getAdapter(type) != null);
	}


	/**
	 * 处理给定的响应式返回值，并决定是否将其适配为 {@link ResponseBodyEmitter} 或 {@link DeferredResult}。
	 *
	 * @return 用于流式传输的发射器，如果使用 {@link DeferredResult} 内部处理，则返回 {@code null}
	 */
	@Nullable
	public ResponseBodyEmitter handleValue(Object returnValue, MethodParameter returnType,
										   ModelAndViewContainer mav, NativeWebRequest request) throws Exception {

		// 确保返回值不为空
		Assert.notNull(returnValue, "Expected return value");
		// 获取适配器以处理返回值
		ReactiveAdapter adapter = this.adapterRegistry.getAdapter(returnValue.getClass());
		// 确保适配器不为空，否则抛出异常
		Assert.state(adapter != null, () -> "Unexpected return value: " + returnValue);

		// 获取返回类型的元素类型
		ResolvableType elementType = ResolvableType.forMethodParameter(returnType).getGeneric();
		// 将元素类型转换为 Class 对象
		Class<?> elementClass = elementType.toClass();

		// 获取请求支持的媒体类型
		Collection<MediaType> mediaTypes = getMediaTypes(request);
		// 在媒体类型集合中找到第一个具体媒体类型
		Optional<MediaType> mediaType = mediaTypes.stream().filter(MimeType::isConcrete).findFirst();

		// 如果返回值是多值的
		if (adapter.isMultiValue()) {
			// 如果支持文本事件流或返回类型是 ServerSentEvent
			if (mediaTypes.stream().anyMatch(MediaType.TEXT_EVENT_STREAM::includes) ||
					ServerSentEvent.class.isAssignableFrom(elementClass)) {
				logExecutorWarning(returnType);
				// 创建并返回 SseEmitter
				SseEmitter emitter = new SseEmitter(STREAMING_TIMEOUT_VALUE);
				// 创建 SseEmitter订阅器
				new SseEmitterSubscriber(emitter, this.taskExecutor).connect(adapter, returnValue);
				return emitter;
			}
			if (CharSequence.class.isAssignableFrom(elementClass)) {
				// 如果返回类型是 CharSequence
				logExecutorWarning(returnType);
				// 则创建并返回 ResponseBodyEmitter
				ResponseBodyEmitter emitter = getEmitter(mediaType.orElse(MediaType.TEXT_PLAIN));
				// 创建 TextEmitter订阅器
				new TextEmitterSubscriber(emitter, this.taskExecutor).connect(adapter, returnValue);
				return emitter;
			}
			// 遍历媒体类型
			for (MediaType type : mediaTypes) {
				for (MediaType streamingType : JSON_STREAMING_MEDIA_TYPES) {
					// 如果是 JSON 流式媒体类型之一
					if (streamingType.includes(type)) {
						logExecutorWarning(returnType);
						// 则创建并返回 ResponseBodyEmitter
						ResponseBodyEmitter emitter = getEmitter(streamingType);
						// 创建 JsonEmitter订阅器
						new JsonEmitterSubscriber(emitter, this.taskExecutor).connect(adapter, returnValue);
						return emitter;
					}
				}
			}
		}

		// 如果不是流式传输，则创建并返回 DeferredResult
		DeferredResult<Object> result = new DeferredResult<>();
		new DeferredResultSubscriber(result, adapter, elementType).connect(adapter, returnValue);
		// 启动 DeferredResult 的处理
		WebAsyncUtils.getAsyncManager(request).startDeferredResultProcessing(result, mav);

		// 返回空值
		return null;
	}

	@SuppressWarnings("unchecked")
	private Collection<MediaType> getMediaTypes(NativeWebRequest request)
			throws HttpMediaTypeNotAcceptableException {

		// 从请求属性中获取可生成的媒体类型集合
		Collection<MediaType> mediaTypes = (Collection<MediaType>) request.getAttribute(
				HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);

		// 如果可生成的媒体类型集合为空，则通过内容协商管理器解析媒体类型
		return CollectionUtils.isEmpty(mediaTypes) ?
				this.contentNegotiationManager.resolveMediaTypes(request) : mediaTypes;
	}

	private ResponseBodyEmitter getEmitter(MediaType mediaType) {
		return new ResponseBodyEmitter(STREAMING_TIMEOUT_VALUE) {
			@Override
			protected void extendResponse(ServerHttpResponse outputMessage) {
				outputMessage.getHeaders().setContentType(mediaType);
			}
		};
	}

	@SuppressWarnings("ConstantConditions")
	private void logExecutorWarning(MethodParameter returnType) {
		if (this.taskExecutorWarning && logger.isWarnEnabled()) {
			synchronized (this) {
				if (this.taskExecutorWarning) {
					String executorTypeName = this.taskExecutor.getClass().getSimpleName();
					logger.warn("\n!!!\n" +
							"Streaming through a reactive type requires an Executor to write to the response.\n" +
							"Please, configure a TaskExecutor in the MVC config under \"async support\".\n" +
							"The " + executorTypeName + " currently in use is not suitable under load.\n" +
							"-------------------------------\n" +
							"Controller:\t" + returnType.getContainingClass().getName() + "\n" +
							"Method:\t\t" + returnType.getMethod().getName() + "\n" +
							"Returning:\t" + ResolvableType.forMethodParameter(returnType) + "\n" +
							"!!!");
					this.taskExecutorWarning = false;
				}
			}
		}
	}


	private abstract static class AbstractEmitterSubscriber implements Subscriber<Object>, Runnable {
		/**
		 * 响应体发射器
		 */
		private final ResponseBodyEmitter emitter;

		/**
		 * 任务执行器
		 */
		private final TaskExecutor taskExecutor;

		/**
		 * 订阅方
		 */
		@Nullable
		private Subscription subscription;

		/**
		 * 元素引用
		 */
		private final AtomicReference<Object> elementRef = new AtomicReference<>();

		/**
		 * 错误
		 */
		@Nullable
		private Throwable error;

		/**
		 * 是否终止任务
		 */
		private volatile boolean terminated;

		/**
		 * 是否正在执行
		 */
		private final AtomicLong executing = new AtomicLong();

		/**
		 * 是否执行完毕
		 */
		private volatile boolean done;

		protected AbstractEmitterSubscriber(ResponseBodyEmitter emitter, TaskExecutor executor) {
			this.emitter = emitter;
			this.taskExecutor = executor;
		}

		public void connect(ReactiveAdapter adapter, Object returnValue) {
			// 转为发布器
			Publisher<Object> publisher = adapter.toPublisher(returnValue);
			// 订阅当前的订阅者
			publisher.subscribe(this);
		}

		protected ResponseBodyEmitter getEmitter() {
			return this.emitter;
		}

		@Override
		public final void onSubscribe(Subscription subscription) {
			// 设置当前对象的订阅
			this.subscription = subscription;

			// 在超时时关闭连接并完成
			this.emitter.onTimeout(() -> {
				if (logger.isTraceEnabled()) {
					logger.trace("Connection timeout for " + this.emitter);
				}
				// 终止连接
				terminate();
				// 完成
				this.emitter.complete();
			});

			// 将异常发送到流中并完成
			this.emitter.onError(this.emitter::completeWithError);

			// 请求一个数据项
			subscription.request(1);
		}

		@Override
		public final void onNext(Object element) {
			// 设置元素值
			this.elementRef.lazySet(element);

			// 尝试调度
			trySchedule();
		}

		@Override
		public final void onError(Throwable ex) {
			this.error = ex;
			this.terminated = true;
			trySchedule();
		}

		@Override
		public final void onComplete() {
			this.terminated = true;
			trySchedule();
		}

		private void trySchedule() {
			if (this.executing.getAndIncrement() == 0) {
				schedule();
			}
		}

		private void schedule() {
			try {
				// 在任务执行器中执行当前任务
				this.taskExecutor.execute(this);
			} catch (Throwable ex) {
				try {
					// 尝试终止当前任务
					terminate();
				} finally {
					// 执行计数递减
					this.executing.decrementAndGet();
					// 元素引用重置为空
					this.elementRef.lazySet(null);
				}
			}
		}

		@Override
		public void run() {
			if (this.done) {
				// 如果已完成，则重置元素引用为空并返回
				this.elementRef.lazySet(null);
				return;
			}

			// 在处理元素之前检查终端信号
			boolean isTerminated = this.terminated;

			// 获取当前元素
			Object element = this.elementRef.get();
			if (element != null) {
				// 如果有元素，则重置元素引用为空
				this.elementRef.lazySet(null);
				// 确保有订阅对象
				Assert.state(this.subscription != null, "No subscription");
				try {
					// 发送元素
					send(element);
					// 请求下一个元素
					this.subscription.request(1);
				} catch (final Throwable ex) {
					// 发送失败时终止当前任务
					if (logger.isTraceEnabled()) {
						logger.trace("Send for " + this.emitter + " failed: " + ex);
					}
					terminate();
					return;
				}
			}

			// 如果已终止
			if (isTerminated) {
				this.done = true;
				Throwable ex = this.error;
				this.error = null;
				if (ex != null) {
					// 如果存在错误，则以错误终止
					if (logger.isTraceEnabled()) {
						logger.trace("Publisher for " + this.emitter + " failed: " + ex);
					}
					this.emitter.completeWithError(ex);
				} else {
					// 如果没有错误，则以正常完成
					if (logger.isTraceEnabled()) {
						logger.trace("Publisher for " + this.emitter + " completed");
					}
					this.emitter.complete();
				}
				return;
			}

			// 如果还有其他任务在执行，则重新调度
			if (this.executing.decrementAndGet() != 0) {
				schedule();
			}
		}

		protected abstract void send(Object element) throws IOException;

		private void terminate() {
			// 设置完成标志为true
			this.done = true;
			// 取消订阅
			if (this.subscription != null) {
				this.subscription.cancel();
			}
		}
	}


	private static class SseEmitterSubscriber extends AbstractEmitterSubscriber {

		SseEmitterSubscriber(SseEmitter sseEmitter, TaskExecutor executor) {
			super(sseEmitter, executor);
		}

		@Override
		protected void send(Object element) throws IOException {
			if (element instanceof ServerSentEvent) {
				// 如果元素是ServerSentEvent类型，则进行适配并发送
				ServerSentEvent<?> event = (ServerSentEvent<?>) element;
				((SseEmitter) getEmitter()).send(adapt(event));
			} else {
				// 否则，发送元素并指定媒体类型为APPLICATION_JSON
				getEmitter().send(element, MediaType.APPLICATION_JSON);
			}
		}

		private SseEmitter.SseEventBuilder adapt(ServerSentEvent<?> sse) {
			// 创建SseEmitter.SseEventBuilder对象
			SseEmitter.SseEventBuilder builder = SseEmitter.event();
			// 获取SSE事件的ID
			String id = sse.id();
			// 获取SSE事件的名称
			String event = sse.event();
			// 获取SSE事件的重试时间
			Duration retry = sse.retry();
			// 获取SSE事件的注释
			String comment = sse.comment();
			// 获取SSE事件的数据
			Object data = sse.data();
			// 如果ID不为null，则设置到SSE事件构建器中
			if (id != null) {
				builder.id(id);
			}
			// 如果事件不为null，则设置为SSE事件的名称
			if (event != null) {
				builder.name(event);
			}
			// 如果数据不为null，则设置到SSE事件构建器中
			if (data != null) {
				builder.data(data);
			}
			// 如果重试时间不为null，则将其转换为毫秒并设置为SSE事件的重连时间
			if (retry != null) {
				builder.reconnectTime(retry.toMillis());
			}
			// 如果注释不为null，则设置为SSE事件的注释
			if (comment != null) {
				builder.comment(comment);
			}
			// 返回构建好的SSE事件构建器对象
			return builder;
		}
	}


	private static class JsonEmitterSubscriber extends AbstractEmitterSubscriber {

		JsonEmitterSubscriber(ResponseBodyEmitter emitter, TaskExecutor executor) {
			super(emitter, executor);
		}

		@Override
		protected void send(Object element) throws IOException {
			// 使用SseEmitter发送JSON格式的数据
			getEmitter().send(element, MediaType.APPLICATION_JSON);
			// 使用SseEmitter发送换行符，数据类型为纯文本
			getEmitter().send("\n", MediaType.TEXT_PLAIN);
		}
	}


	private static class TextEmitterSubscriber extends AbstractEmitterSubscriber {

		TextEmitterSubscriber(ResponseBodyEmitter emitter, TaskExecutor executor) {
			super(emitter, executor);
		}

		@Override
		protected void send(Object element) throws IOException {
			// 发送纯文本格式的元素
			getEmitter().send(element, MediaType.TEXT_PLAIN);
		}
	}


	private static class DeferredResultSubscriber implements Subscriber<Object> {
		/**
		 * 延迟结果
		 */
		private final DeferredResult<Object> result;

		/**
		 * 是否有多个返回值
		 */
		private final boolean multiValueSource;

		/**
		 * 收集值列表
		 */
		private final CollectedValuesList values;

		DeferredResultSubscriber(DeferredResult<Object> result, ReactiveAdapter adapter, ResolvableType elementType) {
			this.result = result;
			this.multiValueSource = adapter.isMultiValue();
			this.values = new CollectedValuesList(elementType);
		}

		public void connect(ReactiveAdapter adapter, Object returnValue) {
			// 将适配器转换为发布者
			Publisher<Object> publisher = adapter.toPublisher(returnValue);
			// 订阅发布者，将当前实例作为订阅者
			publisher.subscribe(this);
		}

		@Override
		public void onSubscribe(Subscription subscription) {
			// 当结果超时时取消订阅
			this.result.onTimeout(subscription::cancel);
			// 请求最大数量的元素
			subscription.request(Long.MAX_VALUE);
		}

		@Override
		public void onNext(Object element) {
			this.values.add(element);
		}

		@Override
		public void onError(Throwable ex) {
			this.result.setErrorResult(ex);
		}

		@Override
		public void onComplete() {
			if (this.values.size() > 1 || this.multiValueSource) {
				// 如果值的数量大于1或者是多值源，则将所有值设置为结果
				this.result.setResult(this.values);
			} else if (this.values.size() == 1) {
				// 如果值的数量等于1，则将该值设置为结果
				this.result.setResult(this.values.get(0));
			} else {
				// 如果没有值，则将结果设置为null
				this.result.setResult(null);
			}
		}
	}


	/**
	 * 收集值列表，其中所有元素都是指定类型。
	 */
	@SuppressWarnings("serial")
	static class CollectedValuesList extends ArrayList<Object> {

		/**
		 * 可解析的元素类型
		 */
		private final ResolvableType elementType;

		CollectedValuesList(ResolvableType elementType) {
			this.elementType = elementType;
		}

		public ResolvableType getReturnType() {
			return ResolvableType.forClassWithGenerics(List.class, this.elementType);
		}
	}

}
