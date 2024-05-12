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

import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.ResolvableType;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.server.DelegatingServerHttpResponse;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.filter.ShallowEtagHeaderFilter;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 处理类型为 {@link ResponseBodyEmitter} 及其子类（如 {@link SseEmitter}）的返回值。
 * 同时支持包装在 {@link ResponseEntity} 中的相同类型。
 *
 * <p>从 5.0 开始，还支持任何具有在 {@link ReactiveAdapterRegistry} 中注册的适配器的反应式返回值类型。
 *
 * @author Rossen Stoyanchev
 * @since 4.2
 */
public class ResponseBodyEmitterReturnValueHandler implements HandlerMethodReturnValueHandler {

	/**
	 * SSE事件消息转换器列表
	 */
	private final List<HttpMessageConverter<?>> sseMessageConverters;

	/**
	 * 响应式类型处理器
	 */
	private final ReactiveTypeHandler reactiveHandler;


	/**
	 * 使用默认的 {@link ReactiveAdapterRegistry} 实例、{@link org.springframework.core.task.SyncTaskExecutor} 和
	 * {@link ContentNegotiationManager}（带有 Accept 标头策略）的构造函数，以支持反应式类型。
	 */
	public ResponseBodyEmitterReturnValueHandler(List<HttpMessageConverter<?>> messageConverters) {
		Assert.notEmpty(messageConverters, "HttpMessageConverter List must not be empty");
		this.sseMessageConverters = initSseConverters(messageConverters);
		this.reactiveHandler = new ReactiveTypeHandler();
	}

	/**
	 * 完整构造函数，支持可插入的 "反应式" 类型支持。
	 *
	 * @param messageConverters 用于写入发出对象的转换器
	 * @param registry          用于反应式返回值类型支持
	 * @param executor          用于从反应式类型发出的项目的阻塞 I/O 写入
	 * @param manager           用于检测流式传输媒体类型
	 * @since 5.0
	 */
	public ResponseBodyEmitterReturnValueHandler(List<HttpMessageConverter<?>> messageConverters,
												 ReactiveAdapterRegistry registry, TaskExecutor executor, ContentNegotiationManager manager) {

		Assert.notEmpty(messageConverters, "HttpMessageConverter List must not be empty");
		this.sseMessageConverters = initSseConverters(messageConverters);
		this.reactiveHandler = new ReactiveTypeHandler(registry, executor, manager);
	}

	private static List<HttpMessageConverter<?>> initSseConverters(List<HttpMessageConverter<?>> converters) {
		// 遍历所有的 HttpMessageConverter
		for (HttpMessageConverter<?> converter : converters) {
			// 如果存在能够将 文本消息 写入 String 类型的转换器，直接返回
			if (converter.canWrite(String.class, MediaType.TEXT_PLAIN)) {
				return converters;
			}
		}

		// 如果没有能够将 文本消息 写入 String 类型的转换器，则新建一个列表用于存放转换器
		List<HttpMessageConverter<?>> result = new ArrayList<>(converters.size() + 1);

		// 将默认的 StringHttpMessageConverter 添加到列表中，并设置字符集为 UTF-8
		result.add(new StringHttpMessageConverter(StandardCharsets.UTF_8));

		// 将原来的转换器列表中的所有转换器添加到新的列表中
		result.addAll(converters);

		// 返回新的列表
		return result;
	}


	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		// 检查返回值类型是否是 ResponseEntity
		Class<?> bodyType = ResponseEntity.class.isAssignableFrom(returnType.getParameterType()) ?
				// 如果是 ResponseEntity，则获取其泛型参数类型
				ResolvableType.forMethodParameter(returnType).getGeneric().resolve() :
				// 如果不是 ResponseEntity，则直接获取返回值类型
				returnType.getParameterType();

		// 返回结果，判断是否是 ResponseBodyEmitter 或者响应式处理器支持的类型
		return (bodyType != null && (ResponseBodyEmitter.class.isAssignableFrom(bodyType) ||
				this.reactiveHandler.isReactiveType(bodyType)));
	}

	@Override
	@SuppressWarnings("resource")
	public void handleReturnValue(@Nullable Object returnValue, MethodParameter returnType,
								  ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws Exception {

		if (returnValue == null) {
			// 如果返回值为空，则设置请求已处理
			mavContainer.setRequestHandled(true);
			// 结束程序
			return;
		}

		// 获取 HttpServletResponse 对象，用于写入响应
		HttpServletResponse response = webRequest.getNativeResponse(HttpServletResponse.class);
		Assert.state(response != null, "No HttpServletResponse");
		// 创建 ServerHttpResponse 对象
		ServerHttpResponse outputMessage = new ServletServerHttpResponse(response);

		if (returnValue instanceof ResponseEntity) {
			// 如果返回值是 ResponseEntity
			ResponseEntity<?> responseEntity = (ResponseEntity<?>) returnValue;
			// 设置响应状态码
			response.setStatus(responseEntity.getStatusCodeValue());
			// 将响应头部信息放入输出消息中
			outputMessage.getHeaders().putAll(responseEntity.getHeaders());
			// 获取返回主体
			returnValue = responseEntity.getBody();
			// 获取嵌套的返回类型
			returnType = returnType.nested();
			if (returnValue == null) {
				// 如果返回主体为空，则设置请求已处理
				mavContainer.setRequestHandled(true);
				// 刷新响应
				outputMessage.flush();
				return;
			}
		}
		// 返回值是 ResponseBodyEmitter类型 或者 响应式类型
		// 获取 ServletRequest 对象
		ServletRequest request = webRequest.getNativeRequest(ServletRequest.class);
		Assert.state(request != null, "No ServletRequest");

		// 定义 ResponseBodyEmitter 对象，用于处理流式传输
		ResponseBodyEmitter emitter;
		if (returnValue instanceof ResponseBodyEmitter) {
			// 如果是 ResponseBodyEmitter 类型，强转为 ResponseBodyEmitter 返回
			emitter = (ResponseBodyEmitter) returnValue;
		} else {
			// 如果不是 ResponseBodyEmitter 类型，则交给响应式处理器处理
			emitter = this.reactiveHandler.handleValue(returnValue, returnType, mavContainer, webRequest);
			// 如果处理器返回为空，表示不支持流式传输，则直接写入头部信息并返回
			if (emitter == null) {
				// 非流式：写入头部而不提交响应。
				outputMessage.getHeaders().forEach((headerName, headerValues) -> {
					for (String headerValue : headerValues) {
						response.addHeader(headerName, headerValue);
					}
				});
				return;
			}
		}
		// 扩展响应对象
		emitter.extendResponse(outputMessage);

		// 禁用缓存
		ShallowEtagHeaderFilter.disableContentCaching(request);

		// 包装响应以忽略进一步的头部更改
		// 头部将在首次写入时刷新
		outputMessage = new StreamingServletServerHttpResponse(outputMessage);

		// 定义 HttpMessageConvertingHandler 对象，用于处理 HTTP 消息的转换
		HttpMessageConvertingHandler handler;
		try {
			// 创建 DeferredResult 对象，用于异步处理结果
			DeferredResult<?> deferredResult = new DeferredResult<>(emitter.getTimeout());
			// 启动 DeferredResult 异步处理
			WebAsyncUtils.getAsyncManager(webRequest).startDeferredResultProcessing(deferredResult, mavContainer);
			// 创建 HttpMessageConvertingHandler 对象
			handler = new HttpMessageConvertingHandler(outputMessage, deferredResult);
		} catch (Throwable ex) {
			// 初始化响应体为错误
			emitter.initializeWithError(ex);
			throw ex;
		}

		// 初始化 ResponseBodyEmitter
		emitter.initialize(handler);
	}


	/**
	 * ResponseBodyEmitter.Handler，使用 HttpMessageConverter 写入。
	 */
	private class HttpMessageConvertingHandler implements ResponseBodyEmitter.Handler {

		/**
		 * 输出流
		 */
		private final ServerHttpResponse outputMessage;

		/**
		 * 延迟结果
		 */
		private final DeferredResult<?> deferredResult;

		public HttpMessageConvertingHandler(ServerHttpResponse outputMessage, DeferredResult<?> deferredResult) {
			this.outputMessage = outputMessage;
			this.deferredResult = deferredResult;
		}

		@Override
		public void send(Object data, @Nullable MediaType mediaType) throws IOException {
			sendInternal(data, mediaType);
		}

		@SuppressWarnings("unchecked")
		private <T> void sendInternal(T data, @Nullable MediaType mediaType) throws IOException {
			// 遍历 SSE 消息转换器列表
			for (HttpMessageConverter<?> converter : ResponseBodyEmitterReturnValueHandler.this.sseMessageConverters) {
				// 如果当前转换器支持写入指定的数据类型和媒体类型
				if (converter.canWrite(data.getClass(), mediaType)) {
					// 使用转换器将数据写入输出消息中
					((HttpMessageConverter<T>) converter).write(data, mediaType, this.outputMessage);
					// 刷新输出流
					this.outputMessage.flush();
					return;
				}
			}
			// 如果没有合适的转换器，则抛出异常
			throw new IllegalArgumentException("No suitable converter for " + data.getClass());
		}

		@Override
		public void complete() {
			try {
				// 刷新输出消息，将缓冲区数据写入底层流中
				this.outputMessage.flush();
				// 设置延迟结果为null，表示成功完成
				this.deferredResult.setResult(null);
			} catch (IOException ex) {
				// 如果在刷新输出消息时发生IO异常，则将延迟结果设置为异常结果
				this.deferredResult.setErrorResult(ex);
			}
		}

		@Override
		public void completeWithError(Throwable failure) {
			this.deferredResult.setErrorResult(failure);
		}

		@Override
		public void onTimeout(Runnable callback) {
			this.deferredResult.onTimeout(callback);
		}

		@Override
		public void onError(Consumer<Throwable> callback) {
			this.deferredResult.onError(callback);
		}

		@Override
		public void onCompletion(Runnable callback) {
			this.deferredResult.onCompletion(callback);
		}
	}


	/**
	 * 包装器以在 HttpMessageConverter 引起的头部更改时静默忽略它们，
	 * 否则会导致 HttpHeaders 抛出异常。
	 */
	private static class StreamingServletServerHttpResponse extends DelegatingServerHttpResponse {
		/**
		 * 可变请求头
		 */
		private final HttpHeaders mutableHeaders = new HttpHeaders();

		public StreamingServletServerHttpResponse(ServerHttpResponse delegate) {
			super(delegate);
			this.mutableHeaders.putAll(delegate.getHeaders());
		}

		@Override
		public HttpHeaders getHeaders() {
			return this.mutableHeaders;
		}

	}

}
