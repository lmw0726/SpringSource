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

package org.springframework.web.servlet.function;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.*;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 默认的{@link EntityResponse.Builder}实现。
 *
 * @param <T> 实体类型
 * @author Arjen Poutsma
 * @since 5.2
 */
final class DefaultEntityResponseBuilder<T> implements EntityResponse.Builder<T> {

	/**
	 * 资源区域列表类型
	 */
	private static final Type RESOURCE_REGION_LIST_TYPE =
			new ParameterizedTypeReference<List<ResourceRegion>>() {
			}.getType();

	/**
	 * 实体对象
	 */
	private final T entity;

	/**
	 * 实体类型
	 */
	private final Type entityType;

	/**
	 * Http状态码
	 */
	private int status = HttpStatus.OK.value();

	/**
	 * 响应标头
	 */
	private final HttpHeaders headers = new HttpHeaders();

	/**
	 * Cookie
	 */
	private final MultiValueMap<String, Cookie> cookies = new LinkedMultiValueMap<>();


	private DefaultEntityResponseBuilder(T entity, @Nullable Type entityType) {
		this.entity = entity;
		this.entityType = (entityType != null) ? entityType : entity.getClass();
	}

	@Override
	public EntityResponse.Builder<T> status(HttpStatus status) {
		Assert.notNull(status, "HttpStatus must not be null");
		this.status = status.value();
		return this;
	}

	@Override
	public EntityResponse.Builder<T> status(int status) {
		this.status = status;
		return this;
	}

	@Override
	public EntityResponse.Builder<T> cookie(Cookie cookie) {
		Assert.notNull(cookie, "Cookie must not be null");
		this.cookies.add(cookie.getName(), cookie);
		return this;
	}

	@Override
	public EntityResponse.Builder<T> cookies(
			Consumer<MultiValueMap<String, Cookie>> cookiesConsumer) {
		cookiesConsumer.accept(this.cookies);
		return this;
	}

	@Override
	public EntityResponse.Builder<T> header(String headerName, String... headerValues) {
		for (String headerValue : headerValues) {
			this.headers.add(headerName, headerValue);
		}
		return this;
	}

	@Override
	public EntityResponse.Builder<T> headers(Consumer<HttpHeaders> headersConsumer) {
		headersConsumer.accept(this.headers);
		return this;
	}

	@Override
	public EntityResponse.Builder<T> allow(HttpMethod... allowedMethods) {
		this.headers.setAllow(new LinkedHashSet<>(Arrays.asList(allowedMethods)));
		return this;
	}

	@Override
	public EntityResponse.Builder<T> allow(Set<HttpMethod> allowedMethods) {
		this.headers.setAllow(allowedMethods);
		return this;
	}

	@Override
	public EntityResponse.Builder<T> contentLength(long contentLength) {
		this.headers.setContentLength(contentLength);
		return this;
	}

	@Override
	public EntityResponse.Builder<T> contentType(MediaType contentType) {
		this.headers.setContentType(contentType);
		return this;
	}

	@Override
	public EntityResponse.Builder<T> eTag(String etag) {
		// 如果ETag值不以双引号或"W/"开头，则添加双引号
		if (!etag.startsWith("\"") && !etag.startsWith("W/\"")) {
			etag = "\"" + etag;
		}
		// 如果ETag值不以双引号结尾，则添加双引号
		if (!etag.endsWith("\"")) {
			etag = etag + "\"";
		}
		// 设置ETag头部信息
		this.headers.setETag(etag);
		return this;
	}

	@Override
	public EntityResponse.Builder<T> lastModified(ZonedDateTime lastModified) {
		this.headers.setLastModified(lastModified);
		return this;
	}

	@Override
	public EntityResponse.Builder<T> lastModified(Instant lastModified) {
		this.headers.setLastModified(lastModified);
		return this;
	}

	@Override
	public EntityResponse.Builder<T> location(URI location) {
		this.headers.setLocation(location);
		return this;
	}

	@Override
	public EntityResponse.Builder<T> cacheControl(CacheControl cacheControl) {
		this.headers.setCacheControl(cacheControl);
		return this;
	}

	@Override
	public EntityResponse.Builder<T> varyBy(String... requestHeaders) {
		this.headers.setVary(Arrays.asList(requestHeaders));
		return this;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	@Override
	public EntityResponse<T> build() {
		if (this.entity instanceof CompletionStage) {
			// 如果实体是CompletionStage类型，创建一个CompletionStageEntityResponse对象
			CompletionStage completionStage = (CompletionStage) this.entity;
			return new CompletionStageEntityResponse(this.status, this.headers, this.cookies,
					completionStage, this.entityType);
		} else if (DefaultAsyncServerResponse.reactiveStreamsPresent) {
			// 如果实体不是CompletionStage类型且存在响应流
			// 获取响应流适配器
			ReactiveAdapter adapter = ReactiveAdapterRegistry.getSharedInstance().getAdapter(this.entity.getClass());
			if (adapter != null) {
				// 使用 响应流适配器 将实体转换为Publisher类型，然后创建一个PublisherEntityResponse对象
				Publisher<T> publisher = adapter.toPublisher(this.entity);
				return new PublisherEntityResponse(this.status, this.headers, this.cookies, publisher, this.entityType);
			}
		}
		// 否则，创建一个DefaultEntityResponse对象
		return new DefaultEntityResponse<>(this.status, this.headers, this.cookies, this.entity, this.entityType);
	}


	/**
	 * 从给定对象返回一个新的{@link EntityResponse.Builder}。
	 */
	public static <T> EntityResponse.Builder<T> fromObject(T t) {
		return new DefaultEntityResponseBuilder<>(t, null);
	}

	/**
	 * 从给定对象和类型引用返回一个新的{@link EntityResponse.Builder}。
	 */
	public static <T> EntityResponse.Builder<T> fromObject(T t, ParameterizedTypeReference<?> bodyType) {
		return new DefaultEntityResponseBuilder<>(t, bodyType.getType());
	}


	/**
	 * 用于同步主体的默认{@link EntityResponse}实现。
	 */
	private static class DefaultEntityResponse<T> extends AbstractServerResponse implements EntityResponse<T> {
		/**
		 * 实体对象
		 */
		private final T entity;

		/**
		 * 实体类型
		 */
		private final Type entityType;

		public DefaultEntityResponse(int statusCode, HttpHeaders headers,
									 MultiValueMap<String, Cookie> cookies, T entity, Type entityType) {

			super(statusCode, headers, cookies);
			this.entity = entity;
			this.entityType = entityType;
		}

		@Override
		public T entity() {
			return this.entity;
		}

		@Override
		protected ModelAndView writeToInternal(HttpServletRequest servletRequest,
											   HttpServletResponse servletResponse, Context context)
				throws ServletException, IOException {

			writeEntityWithMessageConverters(this.entity, servletRequest, servletResponse, context);
			return null;
		}

		@SuppressWarnings({"unchecked", "resource"})
		protected void writeEntityWithMessageConverters(Object entity, HttpServletRequest request,
														HttpServletResponse response, ServerResponse.Context context)
				throws ServletException, IOException {
			// 创建ServletServerHttpResponse对象
			ServletServerHttpResponse serverResponse = new ServletServerHttpResponse(response);
			// 获取响应的媒体类型
			MediaType contentType = getContentType(response);
			// 获取实体类
			Class<?> entityClass = entity.getClass();
			// 获取实体类型
			Type entityType = this.entityType;

			// 如果实体类不是 InputStreamResource，且继承自 Resource 类
			if (entityClass != InputStreamResource.class && Resource.class.isAssignableFrom(entityClass)) {
				// 设置响应头表示支持部分请求
				serverResponse.getHeaders().set(HttpHeaders.ACCEPT_RANGES, "bytes");
				// 获取 Range 头信息
				String rangeHeader = request.getHeader(HttpHeaders.RANGE);
				if (rangeHeader != null) {
					// 如果存在 Range 头信息，将实体转为Resource类型
					Resource resource = (Resource) entity;
					try {
						// 解析请求中的 Range 头信息
						List<HttpRange> httpRanges = HttpRange.parseRanges(rangeHeader);
						// 返回部分内容响应
						serverResponse.getServletResponse().setStatus(HttpStatus.PARTIAL_CONTENT.value());
						// 将资源转换为资源区间列表
						entity = HttpRange.toResourceRegions(httpRanges, resource);
						// 重新设置实体类
						entityClass = entity.getClass();
						// 将实体类型设置为 List<ResourceRegion>
						entityType = RESOURCE_REGION_LIST_TYPE;
					} catch (IllegalArgumentException ex) {
						// 如果解析 Range 头失败，则返回请求范围不可满足状态码
						serverResponse.getHeaders().set(HttpHeaders.CONTENT_RANGE, "bytes */" + resource.contentLength());
						serverResponse.getServletResponse().setStatus(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE.value());
					}
				}
			}

			// 遍历消息转换器，寻找可写入指定实体的消息转换器
			for (HttpMessageConverter<?> messageConverter : context.messageConverters()) {
				if (messageConverter instanceof GenericHttpMessageConverter<?>) {
					// 获取泛型消息转换器
					GenericHttpMessageConverter<Object> genericMessageConverter =
							(GenericHttpMessageConverter<Object>) messageConverter;
					if (genericMessageConverter.canWrite(entityType, entityClass, contentType)) {
						// 如果泛型消息转换器可以写入指定实体
						// 使用泛型消息转换器写入响应
						genericMessageConverter.write(entity, entityType, contentType, serverResponse);
						return;
					}
				}
				if (messageConverter.canWrite(entityClass, contentType)) {
					// 如果消息转换器可以写入指定实体
					// 使用消息转换器写入响应
					((HttpMessageConverter<Object>) messageConverter).write(entity, contentType, serverResponse);
					return;
				}
			}

			// 如果找不到可写入指定实体的消息转换器，则抛出媒体类型不可接受的异常
			List<MediaType> producibleMediaTypes = producibleMediaTypes(context.messageConverters(), entityClass);
			throw new HttpMediaTypeNotAcceptableException(producibleMediaTypes);
		}

		@Nullable
		private static MediaType getContentType(HttpServletResponse response) {
			try {
				// 尝试解析响应的Content-Type，并移除质量值
				return MediaType.parseMediaType(response.getContentType()).removeQualityValue();
			} catch (InvalidMediaTypeException ex) {
				// 如果解析失败，返回null
				return null;
			}
		}

		protected void tryWriteEntityWithMessageConverters(Object entity, HttpServletRequest request,
														   HttpServletResponse response, ServerResponse.Context context) throws ServletException, IOException {
			try {
				// 使用消息转换器将实体写入响应
				writeEntityWithMessageConverters(entity, request, response, context);
			} catch (IOException | ServletException ex) {
				// 处理可能的IO异常或Servlet异常
				handleError(ex, request, response, context);
			}
		}

		private static List<MediaType> producibleMediaTypes(
				List<HttpMessageConverter<?>> messageConverters,
				Class<?> entityClass) {

			return messageConverters.stream()
					// 过滤出能够写入指定实体类的消息转换器
					.filter(messageConverter -> messageConverter.canWrite(entityClass, null))
					// 将每个消息转换器支持的媒体类型展开为流
					.flatMap(messageConverter -> messageConverter.getSupportedMediaTypes(entityClass).stream())
					// 收集所有媒体类型为列表
					.collect(Collectors.toList());
		}

	}


	/**
	 * {@link CompletionStage}体的{@link EntityResponse}实现。
	 */
	private static class CompletionStageEntityResponse<T> extends DefaultEntityResponse<CompletionStage<T>> {

		public CompletionStageEntityResponse(int statusCode, HttpHeaders headers,
											 MultiValueMap<String, Cookie> cookies, CompletionStage<T> entity, Type entityType) {

			super(statusCode, headers, cookies, entity, entityType);
		}

		@Override
		protected ModelAndView writeToInternal(HttpServletRequest servletRequest, HttpServletResponse servletResponse,
											   Context context) throws ServletException, IOException {

			// 创建异步延迟结果对象
			DeferredResult<ServerResponse> deferredResult = createDeferredResult(servletRequest, servletResponse, context);
			// 将延迟结果写入响应，并立即返回
			DefaultAsyncServerResponse.writeAsync(servletRequest, servletResponse, deferredResult);
			// 返回 null，表示不立即返回结果，而是通过异步结果进行响应
			return null;
		}

		private DeferredResult<ServerResponse> createDeferredResult(HttpServletRequest request, HttpServletResponse response,
																	Context context) {

			// 创建一个 DeferredResult 对象，用于异步处理响应
			DeferredResult<ServerResponse> result = new DeferredResult<>();
			// 异步处理实体内容
			entity().handle((value, ex) -> {
				// 如果出现异常
				if (ex != null) {
					// 处理异常
					if (ex instanceof CompletionException && ex.getCause() != null) {
						ex = ex.getCause();
					}
					// 生成错误响应
					ServerResponse errorResponse = errorResponse(ex, request);
					if (errorResponse != null) {
						result.setResult(errorResponse);
					} else {
						result.setErrorResult(ex);
					}
				} else {
					try {
						// 尝试写入实体内容到响应中
						tryWriteEntityWithMessageConverters(value, request, response, context);
						// 写入完成，设置结果为 null
						result.setResult(null);
					} catch (ServletException | IOException writeException) {
						// 如果写入过程中出现异常，设置错误结果
						result.setErrorResult(writeException);
					}
				}
				// 返回 null，表示结果已处理完毕
				return null;
			});
			// 返回 DeferredResult 对象，以便异步处理结果
			return result;
		}

	}


	/**
	 * 用于异步{@link Publisher}主体的{@link EntityResponse}实现。
	 */
	private static class PublisherEntityResponse<T> extends DefaultEntityResponse<Publisher<T>> {

		public PublisherEntityResponse(int statusCode, HttpHeaders headers,
									   MultiValueMap<String, Cookie> cookies, Publisher<T> entity, Type entityType) {

			super(statusCode, headers, cookies, entity, entityType);
		}

		@Override
		protected ModelAndView writeToInternal(HttpServletRequest servletRequest, HttpServletResponse servletResponse,
											   Context context) throws ServletException, IOException {

			// 创建一个 DeferredResult 对象，用于异步处理响应
			DeferredResult<?> deferredResult = new DeferredResult<>();
			// 异步写入响应
			DefaultAsyncServerResponse.writeAsync(servletRequest, servletResponse, deferredResult);
			// 订阅实体内容的异步处理
			entity().subscribe(new DeferredResultSubscriber(servletRequest, servletResponse, context, deferredResult));
			// 返回 null，表示结果已处理完毕
			return null;
		}


		private class DeferredResultSubscriber implements Subscriber<T> {

			/**
			 * Servlet请求
			 */
			private final HttpServletRequest servletRequest;

			/**
			 * Servlet响应
			 */
			private final HttpServletResponse servletResponse;

			/**
			 * 上下文
			 */
			private final Context context;

			/**
			 * 延迟结果
			 */
			private final DeferredResult<?> deferredResult;

			/**
			 * 订阅者
			 */
			@Nullable
			private Subscription subscription;


			public DeferredResultSubscriber(HttpServletRequest servletRequest,
											HttpServletResponse servletResponse, Context context,
											DeferredResult<?> deferredResult) {

				this.servletRequest = servletRequest;
				this.servletResponse = new NoContentLengthResponseWrapper(servletResponse);
				this.context = context;
				this.deferredResult = deferredResult;
			}

			@Override
			public void onSubscribe(Subscription s) {
				if (this.subscription == null) {
					this.subscription = s;
					this.subscription.request(1);
				} else {
					s.cancel();
				}
			}

			@Override
			public void onNext(T t) {
				// 断言确保订阅对象不为 null
				Assert.state(this.subscription != null, "No subscription");
				try {
					// 尝试使用消息转换器写入实体内容
					tryWriteEntityWithMessageConverters(t, this.servletRequest, this.servletResponse, this.context);
					// 刷新输出流
					this.servletResponse.getOutputStream().flush();
					// 请求一个元素
					this.subscription.request(1);
				} catch (ServletException | IOException ex) {
					// 发生异常时，取消订阅并设置 DeferredResult 的错误结果
					this.subscription.cancel();
					this.deferredResult.setErrorResult(ex);
				}
			}

			@Override
			public void onError(Throwable t) {
				try {
					// 处理错误
					handleError(t, this.servletRequest, this.servletResponse, this.context);
				} catch (ServletException | IOException handlingThrowable) {
					// 发生处理异常时，设置 DeferredResult 的错误结果
					this.deferredResult.setErrorResult(handlingThrowable);
				}
			}

			@Override
			public void onComplete() {
				try {
					// 刷新响应输出流
					this.servletResponse.getOutputStream().flush();
					// 设置 DeferredResult 的结果为 null，表示成功
					this.deferredResult.setResult(null);
				} catch (IOException ex) {
					// 发生 IO 异常时，设置 DeferredResult 的错误结果
					this.deferredResult.setErrorResult(ex);
				}
			}
		}


		private static class NoContentLengthResponseWrapper extends HttpServletResponseWrapper {

			public NoContentLengthResponseWrapper(HttpServletResponse response) {
				super(response);
			}

			@Override
			public void addIntHeader(String name, int value) {
				if (!HttpHeaders.CONTENT_LENGTH.equals(name)) {
					super.addIntHeader(name, value);
				}
			}

			@Override
			public void addHeader(String name, String value) {
				if (!HttpHeaders.CONTENT_LENGTH.equals(name)) {
					super.addHeader(name, value);
				}
			}

			@Override
			public void setContentLength(int len) {
			}

			@Override
			public void setContentLengthLong(long len) {
			}
		}
	}

}
