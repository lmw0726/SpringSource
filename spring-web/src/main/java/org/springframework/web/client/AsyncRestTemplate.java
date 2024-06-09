/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.client;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureAdapter;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriTemplateHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * <strong>Spring 的异步客户端 HTTP 访问的中心类。</strong>
 * 与 {@link RestTemplate} 类似地暴露方法，但返回 {@link ListenableFuture} 封装而不是具体结果。
 *
 * <p>{@code AsyncRestTemplate} 通过 {@link #getRestOperations()} 方法公开同步的 {@link RestTemplate}，
 * 并与该 {@code RestTemplate} 共享 {@linkplain #setErrorHandler 错误处理器} 和
 * {@linkplain #setMessageConverters 消息转换器}。
 *
 * <p><strong>注意：</strong>默认情况下，{@code AsyncRestTemplate} 依赖于标准 JDK 设施来建立 HTTP 连接。
 * 您可以通过使用接受 {@link org.springframework.http.client.AsyncClientHttpRequestFactory} 的构造函数，
 * 切换到使用不同的 HTTP 库，如 Apache HttpComponents、Netty 和 OkHttp。
 *
 * <p>更多信息，请参阅 {@link RestTemplate} API 文档。
 *
 * @author Arjen Poutsma
 * @see RestTemplate
 * @since 4.0
 * @deprecated 从 Spring 5.0 开始，推荐使用 {@link org.springframework.web.reactive.function.client.WebClient}
 */
@Deprecated
public class AsyncRestTemplate extends org.springframework.http.client.support.InterceptingAsyncHttpAccessor
		implements AsyncRestOperations {

	/**
	 * Rest模板
	 */
	private final RestTemplate syncTemplate;


	/**
	 * 使用默认设置创建 {@code AsyncRestTemplate} 的新实例。
	 * <p>此构造函数使用 {@link SimpleAsyncTaskExecutor} 与 {@link SimpleClientHttpRequestFactory} 结合，
	 * 用于异步执行。
	 */
	public AsyncRestTemplate() {
		this(new SimpleAsyncTaskExecutor());
	}

	/**
	 * 使用给定的 {@link AsyncTaskExecutor} 创建 {@code AsyncRestTemplate} 的新实例。
	 * <p>此构造函数使用 {@link SimpleClientHttpRequestFactory} 与给定的 {@code AsyncTaskExecutor} 结合，
	 * 用于异步执行。
	 */
	public AsyncRestTemplate(AsyncListenableTaskExecutor taskExecutor) {
		Assert.notNull(taskExecutor, "AsyncTaskExecutor must not be null");
		// 创建一个简单的客户端 HTTP 请求工厂
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		// 设置任务执行器
		requestFactory.setTaskExecutor(taskExecutor);
		// 使用请求工厂创建一个同步模板
		this.syncTemplate = new RestTemplate(requestFactory);
		// 设置异步请求工厂
		setAsyncRequestFactory(requestFactory);
	}

	/**
	 * 使用给定的 {@link org.springframework.http.client.AsyncClientHttpRequestFactory} 创建 {@code AsyncRestTemplate} 的新实例。
	 * <p>此构造函数将给定的异步 {@code AsyncClientHttpRequestFactory} 强制转换为 {@link ClientHttpRequestFactory}。
	 * 由于 Spring 提供的所有 {@code ClientHttpRequestFactory} 实现也实现了 {@code AsyncClientHttpRequestFactory}，
	 * 这不应导致 {@code ClassCastException}。
	 */
	public AsyncRestTemplate(org.springframework.http.client.AsyncClientHttpRequestFactory asyncRequestFactory) {
		this(asyncRequestFactory, (ClientHttpRequestFactory) asyncRequestFactory);
	}

	/**
	 * 使用给定的异步和同步请求工厂创建 {@code AsyncRestTemplate} 的新实例。
	 *
	 * @param asyncRequestFactory 异步请求工厂
	 * @param syncRequestFactory  同步请求工厂
	 */
	public AsyncRestTemplate(org.springframework.http.client.AsyncClientHttpRequestFactory asyncRequestFactory,
							 ClientHttpRequestFactory syncRequestFactory) {

		this(asyncRequestFactory, new RestTemplate(syncRequestFactory));
	}

	/**
	 * 使用给定的 {@link org.springframework.http.client.AsyncClientHttpRequestFactory} 和同步的 {@link RestTemplate} 创建 {@code AsyncRestTemplate} 的新实例。
	 *
	 * @param requestFactory 异步请求工厂
	 * @param restTemplate   要使用的同步模板
	 */
	public AsyncRestTemplate(org.springframework.http.client.AsyncClientHttpRequestFactory requestFactory,
							 RestTemplate restTemplate) {

		Assert.notNull(restTemplate, "RestTemplate must not be null");
		this.syncTemplate = restTemplate;
		setAsyncRequestFactory(requestFactory);
	}


	/**
	 * 设置错误处理器。
	 * <p>默认情况下，AsyncRestTemplate 使用 {@link org.springframework.web.client.DefaultResponseErrorHandler}。
	 */
	public void setErrorHandler(ResponseErrorHandler errorHandler) {
		this.syncTemplate.setErrorHandler(errorHandler);
	}

	/**
	 * 返回错误处理器。
	 */
	public ResponseErrorHandler getErrorHandler() {
		return this.syncTemplate.getErrorHandler();
	}

	/**
	 * 配置默认的 URI 变量值。这是一个快捷方式，相当于：
	 * <pre class="code">
	 * DefaultUriTemplateHandler handler = new DefaultUriTemplateHandler();
	 * handler.setDefaultUriVariables(...);
	 *
	 * AsyncRestTemplate restTemplate = new AsyncRestTemplate();
	 * restTemplate.setUriTemplateHandler(handler);
	 * </pre>
	 *
	 * @param defaultUriVariables 默认的 URI 变量值
	 * @since 4.3
	 */
	@SuppressWarnings("deprecation")
	public void setDefaultUriVariables(Map<String, ?> defaultUriVariables) {
		// 获取同步模板的 URI 模板处理器
		UriTemplateHandler handler = this.syncTemplate.getUriTemplateHandler();
		// 检查 URI 模板处理器类型并设置默认 URI 变量
		if (handler instanceof DefaultUriBuilderFactory) {
			((DefaultUriBuilderFactory) handler).setDefaultUriVariables(defaultUriVariables);
		} else if (handler instanceof org.springframework.web.util.AbstractUriTemplateHandler) {
			((org.springframework.web.util.AbstractUriTemplateHandler) handler)
					.setDefaultUriVariables(defaultUriVariables);
		} else {
			throw new IllegalArgumentException(
					"This property is not supported with the configured UriTemplateHandler.");
		}
	}

	/**
	 * 此属性与{@code RestTemplate}上的相应属性具有相同的目的。有关详细信息，请参阅
	 * {@link RestTemplate#setUriTemplateHandler}。
	 *
	 * @param handler 要使用的URI模板处理程序
	 */
	public void setUriTemplateHandler(UriTemplateHandler handler) {
		this.syncTemplate.setUriTemplateHandler(handler);
	}

	/**
	 * 返回配置的URI模板处理程序。
	 */
	public UriTemplateHandler getUriTemplateHandler() {
		return this.syncTemplate.getUriTemplateHandler();
	}

	@Override
	public RestOperations getRestOperations() {
		return this.syncTemplate;
	}

	/**
	 * 设置要使用的消息体转换器。
	 * <p>这些转换器用于在HTTP请求和响应之间进行转换。
	 */
	public void setMessageConverters(List<HttpMessageConverter<?>> messageConverters) {
		this.syncTemplate.setMessageConverters(messageConverters);
	}

	/**
	 * 返回消息体转换器。
	 */
	public List<HttpMessageConverter<?>> getMessageConverters() {
		return this.syncTemplate.getMessageConverters();
	}


	// GET

	@Override
	public <T> ListenableFuture<ResponseEntity<T>> getForEntity(String url, Class<T> responseType, Object... uriVariables)
			throws RestClientException {

		// 创建异步请求回调，设置接受的媒体类型
		AsyncRequestCallback requestCallback = acceptHeaderRequestCallback(responseType);
		// 创建响应实体提取器
		ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(responseType);
		// 执行 GET 请求，无请求体，获取异步响应，包括 URI 变量
		return execute(url, HttpMethod.GET, requestCallback, responseExtractor, uriVariables);
	}

	@Override
	public <T> ListenableFuture<ResponseEntity<T>> getForEntity(String url, Class<T> responseType,
																Map<String, ?> uriVariables) throws RestClientException {

		// 创建异步请求回调，设置接受的媒体类型
		AsyncRequestCallback requestCallback = acceptHeaderRequestCallback(responseType);
		// 创建响应实体提取器
		ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(responseType);
		// 执行 GET 请求，无请求体，获取异步响应，包括 URI 变量
		return execute(url, HttpMethod.GET, requestCallback, responseExtractor, uriVariables);
	}

	@Override
	public <T> ListenableFuture<ResponseEntity<T>> getForEntity(URI url, Class<T> responseType)
			throws RestClientException {

		// 创建异步请求回调，设置接受的媒体类型
		AsyncRequestCallback requestCallback = acceptHeaderRequestCallback(responseType);
		// 创建响应实体提取器
		ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(responseType);
		// 执行 GET 请求，无请求体，获取异步响应
		return execute(url, HttpMethod.GET, requestCallback, responseExtractor);
	}


	// HEAD

	@Override
	public ListenableFuture<HttpHeaders> headForHeaders(String url, Object... uriVariables)
			throws RestClientException {

		// 创建响应头提取器
		ResponseExtractor<HttpHeaders> headersExtractor = headersExtractor();
		// 执行 HEAD 请求，不需要请求体，获取异步响应，使用 URI 变量
		return execute(url, HttpMethod.HEAD, null, headersExtractor, uriVariables);
	}

	@Override
	public ListenableFuture<HttpHeaders> headForHeaders(String url, Map<String, ?> uriVariables)
			throws RestClientException {

		// 创建响应头提取器
		ResponseExtractor<HttpHeaders> headersExtractor = headersExtractor();
		// 执行 HEAD 请求，不需要请求体，获取异步响应，使用 URI 变量
		return execute(url, HttpMethod.HEAD, null, headersExtractor, uriVariables);
	}

	@Override
	public ListenableFuture<HttpHeaders> headForHeaders(URI url) throws RestClientException {
		// 创建响应头提取器
		ResponseExtractor<HttpHeaders> headersExtractor = headersExtractor();
		// 执行 HEAD 请求，不需要请求体，获取异步响应
		return execute(url, HttpMethod.HEAD, null, headersExtractor);
	}


	// POST

	@Override
	public ListenableFuture<URI> postForLocation(String url, @Nullable HttpEntity<?> request, Object... uriVars)
			throws RestClientException {

		// 创建 HTTP 实体回调
		AsyncRequestCallback callback = httpEntityCallback(request);
		// 创建响应头提取器
		ResponseExtractor<HttpHeaders> extractor = headersExtractor();
		// 执行 POST 请求，获取异步响应
		ListenableFuture<HttpHeaders> future = execute(url, HttpMethod.POST, callback, extractor, uriVars);
		// 将异步响应适配为 Location 头
		return adaptToLocationHeader(future);
	}

	@Override
	public ListenableFuture<URI> postForLocation(String url, @Nullable HttpEntity<?> request, Map<String, ?> uriVars)
			throws RestClientException {

		// 创建 HTTP 实体回调
		AsyncRequestCallback callback = httpEntityCallback(request);
		// 创建响应头提取器
		ResponseExtractor<HttpHeaders> extractor = headersExtractor();
		// 执行 POST 请求，获取异步响应
		ListenableFuture<HttpHeaders> future = execute(url, HttpMethod.POST, callback, extractor, uriVars);
		// 将异步响应适配为 Location 头
		return adaptToLocationHeader(future);
	}

	@Override
	public ListenableFuture<URI> postForLocation(URI url, @Nullable HttpEntity<?> request)
			throws RestClientException {

		// 创建 HTTP 实体回调
		AsyncRequestCallback callback = httpEntityCallback(request);
		// 创建响应头提取器
		ResponseExtractor<HttpHeaders> extractor = headersExtractor();
		// 执行 POST 请求，获取异步响应
		ListenableFuture<HttpHeaders> future = execute(url, HttpMethod.POST, callback, extractor);
		// 将异步响应适配为 Location 头
		return adaptToLocationHeader(future);
	}

	private static ListenableFuture<URI> adaptToLocationHeader(ListenableFuture<HttpHeaders> future) {
		return new ListenableFutureAdapter<URI, HttpHeaders>(future) {
			@Override
			@Nullable
			protected URI adapt(HttpHeaders headers) throws ExecutionException {
				return headers.getLocation();
			}
		};
	}

	@Override
	public <T> ListenableFuture<ResponseEntity<T>> postForEntity(String url, @Nullable HttpEntity<?> request,
																 Class<T> responseType, Object... uriVariables) throws RestClientException {

		// 创建 HTTP 实体回调
		AsyncRequestCallback requestCallback = httpEntityCallback(request, responseType);
		// 创建响应提取器
		ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(responseType);
		// 执行 POST 请求
		return execute(url, HttpMethod.POST, requestCallback, responseExtractor, uriVariables);
	}

	@Override
	public <T> ListenableFuture<ResponseEntity<T>> postForEntity(String url, @Nullable HttpEntity<?> request,
																 Class<T> responseType, Map<String, ?> uriVariables) throws RestClientException {

		// 创建 HTTP 实体回调
		AsyncRequestCallback requestCallback = httpEntityCallback(request, responseType);
		// 创建响应提取器
		ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(responseType);
		// 执行 POST 请求
		return execute(url, HttpMethod.POST, requestCallback, responseExtractor, uriVariables);
	}

	@Override
	public <T> ListenableFuture<ResponseEntity<T>> postForEntity(URI url,
																 @Nullable HttpEntity<?> request, Class<T> responseType) throws RestClientException {

		// 创建 HTTP 实体回调
		AsyncRequestCallback requestCallback = httpEntityCallback(request, responseType);
		// 创建响应提取器
		ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(responseType);
		// 执行 POST 请求
		return execute(url, HttpMethod.POST, requestCallback, responseExtractor);
	}


	// PUT

	@Override
	public ListenableFuture<?> put(String url, @Nullable HttpEntity<?> request, Object... uriVars)
			throws RestClientException {
		// 创建 HTTP 实体回调
		AsyncRequestCallback requestCallback = httpEntityCallback(request);
		// 执行 PUT 请求
		return execute(url, HttpMethod.PUT, requestCallback, null, uriVars);
	}

	@Override
	public ListenableFuture<?> put(String url, @Nullable HttpEntity<?> request, Map<String, ?> uriVars)
			throws RestClientException {
		// 创建 HTTP 实体回调
		AsyncRequestCallback requestCallback = httpEntityCallback(request);
		// 执行 PUT 请求
		return execute(url, HttpMethod.PUT, requestCallback, null, uriVars);
	}

	@Override
	public ListenableFuture<?> put(URI url, @Nullable HttpEntity<?> request) throws RestClientException {
		// 创建 HTTP 实体回调
		AsyncRequestCallback requestCallback = httpEntityCallback(request);
		// 执行 PUT 请求
		return execute(url, HttpMethod.PUT, requestCallback, null);
	}


	// DELETE

	@Override
	public ListenableFuture<?> delete(String url, Object... uriVariables) throws RestClientException {
		return execute(url, HttpMethod.DELETE, null, null, uriVariables);
	}

	@Override
	public ListenableFuture<?> delete(String url, Map<String, ?> uriVariables) throws RestClientException {
		return execute(url, HttpMethod.DELETE, null, null, uriVariables);
	}

	@Override
	public ListenableFuture<?> delete(URI url) throws RestClientException {
		return execute(url, HttpMethod.DELETE, null, null);
	}


	// OPTIONS

	@Override
	public ListenableFuture<Set<HttpMethod>> optionsForAllow(String url, Object... uriVars)
			throws RestClientException {

		// 创建响应头提取器
		ResponseExtractor<HttpHeaders> extractor = headersExtractor();
		// 执行带有 URI 变量的 OPTIONS 请求并返回结果
		ListenableFuture<HttpHeaders> future = execute(url, HttpMethod.OPTIONS, null, extractor, uriVars);
		// 将结果转换为 Allow 头信息
		return adaptToAllowHeader(future);
	}

	@Override
	public ListenableFuture<Set<HttpMethod>> optionsForAllow(String url, Map<String, ?> uriVars)
			throws RestClientException {

		// 创建响应提取器对象，用于提取 HTTP 响应的头信息
		ResponseExtractor<HttpHeaders> extractor = headersExtractor();
		// 执行 OPTIONS 请求，并返回包含响应头信息的 ListenableFuture 对象
		ListenableFuture<HttpHeaders> future = execute(url, HttpMethod.OPTIONS, null, extractor, uriVars);
		// 将异步响应适配为允许的 HTTP 头信息
		return adaptToAllowHeader(future);
	}

	@Override
	public ListenableFuture<Set<HttpMethod>> optionsForAllow(URI url) throws RestClientException {
		// 创建响应头提取器
		ResponseExtractor<HttpHeaders> extractor = headersExtractor();
		// 执行 OPTIONS 请求并返回结果
		ListenableFuture<HttpHeaders> future = execute(url, HttpMethod.OPTIONS, null, extractor);
		// 将结果转换为 Allow 头信息
		return adaptToAllowHeader(future);
	}

	private static ListenableFuture<Set<HttpMethod>> adaptToAllowHeader(ListenableFuture<HttpHeaders> future) {
		return new ListenableFutureAdapter<Set<HttpMethod>, HttpHeaders>(future) {
			@Override
			protected Set<HttpMethod> adapt(HttpHeaders headers) throws ExecutionException {
				return headers.getAllow();
			}
		};
	}

	// 交换方法

	@Override
	public <T> ListenableFuture<ResponseEntity<T>> exchange(String url, HttpMethod method,
															@Nullable HttpEntity<?> requestEntity, Class<T> responseType, Object... uriVariables)
			throws RestClientException {

		// 创建异步请求的请求回调对象
		AsyncRequestCallback requestCallback = httpEntityCallback(requestEntity, responseType);
		// 创建响应提取器对象，用于将响应转换为 ResponseEntity<T> 类型的对象
		ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(responseType);
		// 调用 execute 方法执行异步请求，并返回结果
		return execute(url, method, requestCallback, responseExtractor, uriVariables);
	}

	@Override
	public <T> ListenableFuture<ResponseEntity<T>> exchange(String url, HttpMethod method,
															@Nullable HttpEntity<?> requestEntity, Class<T> responseType, Map<String, ?> uriVariables)
			throws RestClientException {

		// 创建异步请求回调
		AsyncRequestCallback requestCallback = httpEntityCallback(requestEntity, responseType);
		// 创建响应提取器
		ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(responseType);
		// 执行 HTTP 请求并返回结果
		return execute(url, method, requestCallback, responseExtractor, uriVariables);
	}

	@Override
	public <T> ListenableFuture<ResponseEntity<T>> exchange(URI url, HttpMethod method,
															@Nullable HttpEntity<?> requestEntity, Class<T> responseType) throws RestClientException {

		// 创建异步请求的请求回调对象
		AsyncRequestCallback requestCallback = httpEntityCallback(requestEntity, responseType);
		// 创建响应提取器对象，用于将响应转换为 ResponseEntity<T> 类型的对象
		ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(responseType);
		// 调用 execute 方法执行异步请求，并返回结果
		return execute(url, method, requestCallback, responseExtractor);
	}

	@Override
	public <T> ListenableFuture<ResponseEntity<T>> exchange(String url, HttpMethod method,
															@Nullable HttpEntity<?> requestEntity, ParameterizedTypeReference<T> responseType,
															Object... uriVariables) throws RestClientException {

		// 获取响应类型的 Type 对象
		Type type = responseType.getType();
		// 创建异步请求回调
		AsyncRequestCallback requestCallback = httpEntityCallback(requestEntity, type);
		// 创建响应提取器
		ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(type);
		// 执行 HTTP 请求并返回结果
		return execute(url, method, requestCallback, responseExtractor, uriVariables);
	}

	@Override
	public <T> ListenableFuture<ResponseEntity<T>> exchange(String url, HttpMethod method,
															@Nullable HttpEntity<?> requestEntity, ParameterizedTypeReference<T> responseType,
															Map<String, ?> uriVariables) throws RestClientException {

		// 获取响应类型的 Type 对象
		Type type = responseType.getType();
		// 创建异步请求回调
		AsyncRequestCallback requestCallback = httpEntityCallback(requestEntity, type);
		// 创建响应提取器
		ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(type);
		// 执行 HTTP 请求并返回结果
		return execute(url, method, requestCallback, responseExtractor, uriVariables);
	}

	@Override
	public <T> ListenableFuture<ResponseEntity<T>> exchange(URI url, HttpMethod method,
															@Nullable HttpEntity<?> requestEntity, ParameterizedTypeReference<T> responseType)
			throws RestClientException {

		// 获取响应类型的 Type 对象
		Type type = responseType.getType();
		// 创建异步请求回调
		AsyncRequestCallback requestCallback = httpEntityCallback(requestEntity, type);
		// 创建响应提取器
		ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(type);
		// 执行 HTTP 请求并返回结果
		return execute(url, method, requestCallback, responseExtractor);
	}


	// 通用执行方法

	@Override
	public <T> ListenableFuture<T> execute(String url, HttpMethod method, @Nullable AsyncRequestCallback requestCallback,
										   @Nullable ResponseExtractor<T> responseExtractor, Object... uriVariables) throws RestClientException {
		// 根据 URI 模板和变量获取扩展后的 URI
		URI expanded = getUriTemplateHandler().expand(url, uriVariables);
		// 执行 HTTP 请求
		return doExecute(expanded, method, requestCallback, responseExtractor);
	}

	@Override
	public <T> ListenableFuture<T> execute(String url, HttpMethod method,
										   @Nullable AsyncRequestCallback requestCallback, @Nullable ResponseExtractor<T> responseExtractor,
										   Map<String, ?> uriVariables) throws RestClientException {

		// 根据 URI 模板和变量获取扩展后的 URI
		URI expanded = getUriTemplateHandler().expand(url, uriVariables);
		// 执行 HTTP 请求
		return doExecute(expanded, method, requestCallback, responseExtractor);
	}

	@Override
	public <T> ListenableFuture<T> execute(URI url, HttpMethod method,
										   @Nullable AsyncRequestCallback requestCallback,
										   @Nullable ResponseExtractor<T> responseExtractor) throws RestClientException {

		return doExecute(url, method, requestCallback, responseExtractor);
	}

	/**
	 * 在提供的 URI 上执行给定的方法。使用 {@link RequestCallback} 处理
	 * {@link org.springframework.http.client.ClientHttpRequest}；使用 {@link ResponseExtractor} 处理响应。
	 *
	 * @param url               要连接的完全展开的 URL
	 * @param method            要执行的 HTTP 方法（GET、POST 等）
	 * @param requestCallback   准备请求的对象（可以为 {@code null}）
	 * @param responseExtractor 从响应中提取返回值的对象（可以为 {@code null}）
	 * @return 任意对象，由 {@link ResponseExtractor} 返回
	 */
	protected <T> ListenableFuture<T> doExecute(URI url, HttpMethod method,
												@Nullable AsyncRequestCallback requestCallback,
												@Nullable ResponseExtractor<T> responseExtractor) throws RestClientException {

		Assert.notNull(url, "'url' must not be null");
		Assert.notNull(method, "'method' must not be null");
		try {
			// 创建异步请求
			org.springframework.http.client.AsyncClientHttpRequest request = createAsyncRequest(url, method);
			if (requestCallback != null) {
				// 如果存在请求回调，执行回调函数
				requestCallback.doWithRequest(request);
			}
			// 执行异步请求，获取响应的 ListenableFuture 对象
			ListenableFuture<ClientHttpResponse> responseFuture = request.executeAsync();
			// 返回响应抽取器的 Future 对象
			return new ResponseExtractorFuture<>(method, url, responseFuture, responseExtractor);
		} catch (IOException ex) {
			// 抛出资源访问异常
			throw new ResourceAccessException("I/O error on " + method.name() +
					" request for \"" + url + "\":" + ex.getMessage(), ex);
		}
	}

	private void logResponseStatus(HttpMethod method, URI url, ClientHttpResponse response) {
		// 如果日志级别为 DEBUG，则记录异步请求的信息
		if (logger.isDebugEnabled()) {
			try {
				// 使用 DEBUG 日志记录异步请求的信息，包括请求方法、URL、响应状态码和状态文本
				logger.debug("Async " + method.name() + " request for \"" + url + "\" resulted in " +
						response.getRawStatusCode() + " (" + response.getStatusText() + ")");
			} catch (IOException ex) {
				// 忽略可能出现的 IO 异常
			}
		}
	}

	private void handleResponseError(HttpMethod method, URI url, ClientHttpResponse response) throws IOException {
		if (logger.isDebugEnabled()) {
			try {
				// 记录异步请求结果
				logger.debug("Async " + method.name() + " request for \"" + url + "\" resulted in " +
						response.getRawStatusCode() + " (" + response.getStatusText() + "); invoking error handler");
			} catch (IOException ex) {
				// 忽略异常
			}
		}
		// 调用错误处理器处理错误
		getErrorHandler().handleError(url, method, response);
	}

	/**
	 * 返回一个请求回调实现，根据给定的响应类型和配置的{@linkplain #getMessageConverters()消息转换器}，
	 * 准备请求{@code Accept}头。
	 */
	protected <T> AsyncRequestCallback acceptHeaderRequestCallback(Class<T> responseType) {
		return new AsyncRequestCallbackAdapter(this.syncTemplate.acceptHeaderRequestCallback(responseType));
	}

	/**
	 * 返回一个请求回调实现，将给定的对象写入请求流中。
	 */
	protected <T> AsyncRequestCallback httpEntityCallback(@Nullable HttpEntity<T> requestBody) {
		return new AsyncRequestCallbackAdapter(this.syncTemplate.httpEntityCallback(requestBody));
	}

	/**
	 * 返回一个请求回调实现，将给定的对象写入请求流中。
	 */
	protected <T> AsyncRequestCallback httpEntityCallback(@Nullable HttpEntity<T> request, Type responseType) {
		return new AsyncRequestCallbackAdapter(this.syncTemplate.httpEntityCallback(request, responseType));
	}

	/**
	 * 返回一个{@link ResponseEntity}的响应提取器。
	 */
	protected <T> ResponseExtractor<ResponseEntity<T>> responseEntityExtractor(Type responseType) {
		return this.syncTemplate.responseEntityExtractor(responseType);
	}

	/**
	 * 返回一个{@link HttpHeaders}的响应提取器。
	 */
	protected ResponseExtractor<HttpHeaders> headersExtractor() {
		return this.syncTemplate.headersExtractor();
	}


	/**
	 * {@link #doExecute(URI, HttpMethod, AsyncRequestCallback, ResponseExtractor)}返回的Future。
	 */
	private class ResponseExtractorFuture<T> extends ListenableFutureAdapter<T, ClientHttpResponse> {
		/**
		 * 请求方法
		 */
		private final HttpMethod method;

		/**
		 * URL
		 */
		private final URI url;

		/**
		 * 响应提取器
		 */
		@Nullable
		private final ResponseExtractor<T> responseExtractor;

		public ResponseExtractorFuture(HttpMethod method, URI url,
									   ListenableFuture<ClientHttpResponse> clientHttpResponseFuture,
									   @Nullable ResponseExtractor<T> responseExtractor) {

			super(clientHttpResponseFuture);
			this.method = method;
			this.url = url;
			this.responseExtractor = responseExtractor;
		}

		@Override
		@Nullable
		protected final T adapt(ClientHttpResponse response) throws ExecutionException {
			try {
				// 如果错误处理器中没有错误
				if (!getErrorHandler().hasError(response)) {
					// 记录响应状态
					logResponseStatus(this.method, this.url, response);
				} else {
					// 处理响应错误
					handleResponseError(this.method, this.url, response);
				}
				// 转换响应
				return convertResponse(response);
			} catch (Throwable ex) {
				// 抛出执行异常
				throw new ExecutionException(ex);
			} finally {
				// 关闭响应
				response.close();
			}
		}

		@Nullable
		protected T convertResponse(ClientHttpResponse response) throws IOException {
			return (this.responseExtractor != null ? this.responseExtractor.extractData(response) : null);
		}
	}


	/**
	 * 将 {@link RequestCallback} 适配为 {@link AsyncRequestCallback} 接口的适配器。
	 */
	private static class AsyncRequestCallbackAdapter implements AsyncRequestCallback {
		/**
		 * 请求回调
		 */
		private final RequestCallback adaptee;

		/**
		 * 根据给定的 {@link RequestCallback} 创建一个新的 {@code AsyncRequestCallbackAdapter}。
		 *
		 * @param requestCallback 用于创建此适配器的回调
		 */
		public AsyncRequestCallbackAdapter(RequestCallback requestCallback) {
			this.adaptee = requestCallback;
		}

		@Override
		public void doWithRequest(final org.springframework.http.client.AsyncClientHttpRequest request)
				throws IOException {

			this.adaptee.doWithRequest(new ClientHttpRequest() {
				@Override
				public ClientHttpResponse execute() throws IOException {
					throw new UnsupportedOperationException("execute not supported");
				}

				@Override
				public OutputStream getBody() throws IOException {
					return request.getBody();
				}

				@Override
				@Nullable
				public HttpMethod getMethod() {
					return request.getMethod();
				}

				@Override
				public String getMethodValue() {
					return request.getMethodValue();
				}

				@Override
				public URI getURI() {
					return request.getURI();
				}

				@Override
				public HttpHeaders getHeaders() {
					return request.getHeaders();
				}
			});
		}
	}

}
