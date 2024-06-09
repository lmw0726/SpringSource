/*
 * Copyright 2002-2017 the original author or authors.
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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.util.concurrent.ListenableFuture;

import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

/**
 * 接口定义了一组基本的异步 RESTful 操作。由 {@link AsyncRestTemplate} 实现。
 * 不经常直接使用，但作为增强可测试性的一种有用选择，因为可以轻松地模拟或存根化。
 *
 * @author Arjen Poutsma
 * @see AsyncRestTemplate
 * @see RestOperations
 * @since 4.0
 * @deprecated 自 Spring 5.0 起，建议使用 {@link org.springframework.web.reactive.function.client.WebClient}
 */
@Deprecated
public interface AsyncRestOperations {

	/**
	 * 暴露同步的 Spring RestTemplate 以允许同步调用。
	 */
	RestOperations getRestOperations();


	// GET

	/**
	 * 通过在指定的 URL 上执行 GET 来异步检索实体。响应被转换并存储在 {@link ResponseEntity} 中。
	 * <p>使用给定的 URI 变量扩展 URI 模板变量（如果有）。
	 *
	 * @param url          URL
	 * @param responseType 返回值的类型
	 * @param uriVariables 用于扩展模板的变量
	 * @return 用 {@link Future} 包装的实体
	 */
	<T> ListenableFuture<ResponseEntity<T>> getForEntity(String url, Class<T> responseType,
														 Object... uriVariables) throws RestClientException;

	/**
	 * 通过在 URI 模板上执行 GET 来异步检索表示。响应被转换并存储在 {@link ResponseEntity} 中。
	 * <p>使用给定的映射扩展 URI 模板变量。
	 *
	 * @param url          URL
	 * @param responseType 返回值的类型
	 * @param uriVariables 包含 URI 模板变量的映射
	 * @return 用 {@link Future} 包装的实体
	 */
	<T> ListenableFuture<ResponseEntity<T>> getForEntity(String url, Class<T> responseType,
														 Map<String, ?> uriVariables) throws RestClientException;

	/**
	 * 通过在 URL 上执行 GET 来异步检索表示。响应被转换并存储在 {@link ResponseEntity} 中。
	 *
	 * @param url          URL
	 * @param responseType 返回值的类型
	 * @return 用 {@link Future} 包装的实体
	 */
	<T> ListenableFuture<ResponseEntity<T>> getForEntity(URI url, Class<T> responseType)
			throws RestClientException;


	// HEAD

	/**
	 * 异步检索由 URI 模板指定的资源的所有标头。
	 * <p>使用给定的 URI 变量扩展 URI 模板变量（如果有）。
	 *
	 * @param url          URL
	 * @param uriVariables 用于扩展模板的变量
	 * @return 用 {@link Future} 包装的该资源的所有 HTTP 标头
	 */
	ListenableFuture<HttpHeaders> headForHeaders(String url, Object... uriVariables)
			throws RestClientException;

	/**
	 * 异步检索由 URI 模板指定的资源的所有标头。
	 * <p>使用给定的映射扩展 URI 模板变量。
	 *
	 * @param url          URL
	 * @param uriVariables 包含 URI 模板变量的映射
	 * @return 用 {@link Future} 包装的该资源的所有 HTTP 标头
	 */
	ListenableFuture<HttpHeaders> headForHeaders(String url, Map<String, ?> uriVariables)
			throws RestClientException;

	/**
	 * 异步检索由 URL 指定的资源的所有标头。
	 *
	 * @param url URL
	 * @return 用 {@link Future} 包装的该资源的所有 HTTP 标头
	 */
	ListenableFuture<HttpHeaders> headForHeaders(URI url) throws RestClientException;


	// POST

	/**
	 * 通过将给定对象POST到URI模板来创建新资源，并异步返回{@code Location}头的值。此头通常指示新资源存储的位置。
	 * <p>如果有的话，使用给定的URI变量扩展URI模板变量。
	 *
	 * @param url          URL
	 * @param request      要POST的对象（可能为{@code null}）
	 * @param uriVariables 要扩展模板的变量
	 * @return 包装在{@link Future}中的{@code Location}头的值
	 * @see org.springframework.http.HttpEntity
	 */
	ListenableFuture<URI> postForLocation(String url, @Nullable HttpEntity<?> request, Object... uriVariables)
			throws RestClientException;

	/**
	 * 通过将给定对象POST到URI模板来创建新资源，并异步返回{@code Location}头的值。此头通常指示新资源存储的位置。
	 * <p>使用给定的映射扩展URI模板变量。
	 *
	 * @param url          URL
	 * @param request      要POST的对象（可能为{@code null}）
	 * @param uriVariables 要扩展模板的变量
	 * @return 包装在{@link Future}中的{@code Location}头的值
	 * @see org.springframework.http.HttpEntity
	 */
	ListenableFuture<URI> postForLocation(String url, @Nullable HttpEntity<?> request, Map<String, ?> uriVariables)
			throws RestClientException;

	/**
	 * 通过将给定对象POST到URL来创建新资源，并异步返回{@code Location}头的值。此头通常指示新资源存储的位置。
	 *
	 * @param url     URL
	 * @param request 要POST的对象（可能为{@code null}）
	 * @return 包装在{@link Future}中的{@code Location}头的值
	 * @see org.springframework.http.HttpEntity
	 */
	ListenableFuture<URI> postForLocation(URI url, @Nullable HttpEntity<?> request) throws RestClientException;

	/**
	 * 通过将给定对象POST到URI模板来创建新资源，并异步返回{@link ResponseEntity}作为响应。
	 * <p>如果有的话，使用给定的URI变量扩展URI模板变量。
	 *
	 * @param url          URL
	 * @param request      要POST的对象（可能为{@code null}）
	 * @param responseType 响应类型的Class
	 * @param uriVariables 要扩展模板的变量
	 * @return 包装在{@link Future}中的实体
	 * @see org.springframework.http.HttpEntity
	 */
	<T> ListenableFuture<ResponseEntity<T>> postForEntity(String url, @Nullable HttpEntity<?> request,
														  Class<T> responseType, Object... uriVariables) throws RestClientException;

	/**
	 * 通过将给定对象POST到URI模板来创建新资源，并异步返回{@link ResponseEntity}作为响应。
	 * <p>使用给定的映射扩展URI模板变量。
	 *
	 * @param url          URL
	 * @param request      要POST的对象（可能为{@code null}）
	 * @param responseType 响应类型的Class
	 * @param uriVariables 要扩展模板的变量
	 * @return 包装在{@link Future}中的实体
	 * @see org.springframework.http.HttpEntity
	 */
	<T> ListenableFuture<ResponseEntity<T>> postForEntity(String url, @Nullable HttpEntity<?> request,
														  Class<T> responseType, Map<String, ?> uriVariables) throws RestClientException;

	/**
	 * 通过将给定对象POST到URL来创建新资源，并异步返回{@link ResponseEntity}作为响应。
	 *
	 * @param url          URL
	 * @param request      要POST的对象（可能为{@code null}）
	 * @param responseType 响应类型的Class
	 * @return 包装在{@link Future}中的实体
	 * @see org.springframework.http.HttpEntity
	 */
	<T> ListenableFuture<ResponseEntity<T>> postForEntity(URI url, @Nullable HttpEntity<?> request,
														  Class<T> responseType) throws RestClientException;


	// PUT

	/**
	 * 通过将给定对象PUT到URI来创建或更新资源。
	 * <p>如果有的话，使用给定的URI变量扩展URI模板变量。
	 * <p>在完成后，Future将返回{@code null}结果。
	 *
	 * @param url          URL
	 * @param request      要PUT的对象（可能为{@code null}）
	 * @param uriVariables 要扩展模板的变量
	 * @see HttpEntity
	 */
	ListenableFuture<?> put(String url, @Nullable HttpEntity<?> request, Object... uriVariables)
			throws RestClientException;

	/**
	 * 通过将给定对象PUT到URI模板来创建新资源。
	 * <p>使用给定的映射扩展URI模板变量。
	 * <p>在完成后，Future将返回{@code null}结果。
	 *
	 * @param url          URL
	 * @param request      要PUT的对象（可能为{@code null}）
	 * @param uriVariables 要扩展模板的变量
	 * @see HttpEntity
	 */
	ListenableFuture<?> put(String url, @Nullable HttpEntity<?> request, Map<String, ?> uriVariables)
			throws RestClientException;

	/**
	 * 通过将给定对象PUT到URL来创建新资源。
	 * <p>在完成后，Future将返回{@code null}结果。
	 *
	 * @param url     URL
	 * @param request 要PUT的对象（可能为{@code null}）
	 * @see HttpEntity
	 */
	ListenableFuture<?> put(URI url, @Nullable HttpEntity<?> request) throws RestClientException;


	// DELETE

	/**
	 * 异步删除指定URI上的资源。
	 * <p>如果有的话，使用给定的URI变量扩展URI模板变量。
	 * <p>在完成后，Future将返回{@code null}结果。
	 *
	 * @param url          URL
	 * @param uriVariables 要扩展的模板中的变量
	 */
	ListenableFuture<?> delete(String url, Object... uriVariables) throws RestClientException;

	/**
	 * 异步删除指定URI上的资源。
	 * <p>如果有的话，使用给定的URI变量扩展URI模板变量。
	 * <p>在完成后，Future将返回{@code null}结果。
	 *
	 * @param url          URL
	 * @param uriVariables 要扩展的模板中的变量
	 */
	ListenableFuture<?> delete(String url, Map<String, ?> uriVariables) throws RestClientException;

	/**
	 * 异步删除指定URI上的资源。
	 * <p>如果有的话，使用给定的URI变量扩展URI模板变量。
	 * <p>在完成后，Future将返回{@code null}结果。
	 *
	 * @param url URL
	 */
	ListenableFuture<?> delete(URI url) throws RestClientException;


	// OPTIONS

	/**
	 * 异步返回给定URI的Allow头的值。
	 * <p>如果有的话，使用给定的URI变量扩展URI模板变量。
	 *
	 * @param url          URL
	 * @param uriVariables 要在模板中扩展的变量
	 * @return 包装在{@link Future}中的allow头的值
	 */
	ListenableFuture<Set<HttpMethod>> optionsForAllow(String url, Object... uriVariables)
			throws RestClientException;

	/**
	 * 异步返回给定URI的Allow头的值。
	 * <p>使用给定的映射扩展URI模板变量。
	 *
	 * @param url          URL
	 * @param uriVariables 要在模板中扩展的变量
	 * @return 包装在{@link Future}中的allow头的值
	 */
	ListenableFuture<Set<HttpMethod>> optionsForAllow(String url, Map<String, ?> uriVariables)
			throws RestClientException;

	/**
	 * 异步返回给定URL的Allow头的值。
	 *
	 * @param url URL
	 * @return 包装在{@link Future}中的allow头的值
	 */
	ListenableFuture<Set<HttpMethod>> optionsForAllow(URI url) throws RestClientException;


	// 交换

	/**
	 * 异步执行 HTTP 方法到给定的 URI 模板，将给定的请求实体写入请求，并将响应作为 {@link ResponseEntity} 返回。
	 * <p>使用给定的 URI 变量扩展 URI 模板变量（如果有）。
	 *
	 * @param url           URL
	 * @param method        HTTP 方法（GET、POST 等）
	 * @param requestEntity 要写入请求的实体（标头和/或正文）（可能为 {@code null}）
	 * @param responseType  返回值的类型
	 * @param uriVariables  要在模板中扩展的变量
	 * @return 用 {@link Future} 包装的响应实体
	 */
	<T> ListenableFuture<ResponseEntity<T>> exchange(String url, HttpMethod method,
													 @Nullable HttpEntity<?> requestEntity, Class<T> responseType, Object... uriVariables)
			throws RestClientException;

	/**
	 * 异步执行 HTTP 方法到给定的 URI 模板，将给定的请求实体写入请求，并将响应作为 {@link ResponseEntity} 返回。
	 * <p>使用给定的 URI 变量扩展 URI 模板变量（如果有）。
	 *
	 * @param url           URL
	 * @param method        HTTP 方法（GET、POST 等）
	 * @param requestEntity 要写入请求的实体（标头和/或正文）（可能为 {@code null}）
	 * @param responseType  返回值的类型
	 * @param uriVariables  要在模板中扩展的变量
	 * @return 用 {@link Future} 包装的响应实体
	 */
	<T> ListenableFuture<ResponseEntity<T>> exchange(String url, HttpMethod method,
													 @Nullable HttpEntity<?> requestEntity, Class<T> responseType,
													 Map<String, ?> uriVariables) throws RestClientException;

	/**
	 * 异步执行 HTTP 方法到给定的 URI 模板，将给定的请求实体写入请求，并将响应作为 {@link ResponseEntity} 返回。
	 *
	 * @param url           URL
	 * @param method        HTTP 方法（GET、POST 等）
	 * @param requestEntity 要写入请求的实体（标头和/或正文）（可能为 {@code null}）
	 * @param responseType  返回值的类型
	 * @return 用 {@link Future} 包装的响应实体
	 */
	<T> ListenableFuture<ResponseEntity<T>> exchange(URI url, HttpMethod method,
													 @Nullable HttpEntity<?> requestEntity, Class<T> responseType)
			throws RestClientException;

	/**
	 * 异步执行 HTTP 方法到给定的 URI 模板，将给定的请求实体写入请求，并将响应作为 {@link ResponseEntity} 返回。
	 * 给定的 {@link ParameterizedTypeReference} 用于传递泛型类型信息：
	 * <pre class="code">
	 * ParameterizedTypeReference&lt;List&lt;MyBean&gt;&gt; myBean =
	 *     new ParameterizedTypeReference&lt;List&lt;MyBean&gt;&gt;() {};
	 *
	 * ResponseEntity&lt;List&lt;MyBean&gt;&gt; response =
	 *     template.exchange("https://example.com", HttpMethod.GET, null, myBean);
	 * </pre>
	 *
	 * @param url           URL
	 * @param method        HTTP 方法（GET、POST 等）
	 * @param requestEntity 要写入请求的实体（标头和/或正文）（可能为 {@code null}）
	 * @param responseType  返回值的类型
	 * @param uriVariables  要在模板中扩展的变量
	 * @return 用 {@link Future} 包装的响应实体
	 */
	<T> ListenableFuture<ResponseEntity<T>> exchange(String url, HttpMethod method,
													 @Nullable HttpEntity<?> requestEntity, ParameterizedTypeReference<T> responseType,
													 Object... uriVariables) throws RestClientException;

	/**
	 * 异步执行 HTTP 方法到给定的 URI 模板，将给定的请求实体写入请求，并将响应作为 {@link ResponseEntity} 返回。
	 * 给定的 {@link ParameterizedTypeReference} 用于传递泛型类型信息：
	 * <pre class="code">
	 * ParameterizedTypeReference&lt;List&lt;MyBean&gt;&gt; myBean =
	 *     new ParameterizedTypeReference&lt;List&lt;MyBean&gt;&gt;() {};
	 *
	 * ResponseEntity&lt;List&lt;MyBean&gt;&gt; response =
	 *     template.exchange("https://example.com", HttpMethod.GET, null, myBean);
	 * </pre>
	 *
	 * @param url           URL
	 * @param method        HTTP 方法（GET、POST 等）
	 * @param requestEntity 要写入请求的实体（标头和/或正文）（可能为 {@code null}）
	 * @param responseType  返回值的类型
	 * @param uriVariables  要在模板中扩展的变量
	 * @return 用 {@link Future} 包装的响应实体
	 */
	<T> ListenableFuture<ResponseEntity<T>> exchange(String url, HttpMethod method,
													 @Nullable HttpEntity<?> requestEntity, ParameterizedTypeReference<T> responseType,
													 Map<String, ?> uriVariables) throws RestClientException;

	/**
	 * 异步执行HTTP方法到给定的URI模板，将给定的请求实体写入请求，并将响应作为{@link ResponseEntity}返回。
	 * 给定的{@link ParameterizedTypeReference}用于传递泛型类型信息：
	 * <pre class="code">
	 * ParameterizedTypeReference&lt;List&lt;MyBean&gt;&gt; myBean =
	 *     new ParameterizedTypeReference&lt;List&lt;MyBean&gt;&gt;() {};
	 *
	 * ResponseEntity&lt;List&lt;MyBean&gt;&gt; response =
	 *     template.exchange(&quot;https://example.com&quot;,HttpMethod.GET, null, myBean);
	 * </pre>
	 *
	 * @param url           URL
	 * @param method        HTTP方法（GET、POST等）
	 * @param requestEntity 要写入请求的实体（头部和/或正文）（可能为{@code null}）
	 * @param responseType  返回值的类型
	 * @return 作为实体包装在{@link Future}中的响应
	 */
	<T> ListenableFuture<ResponseEntity<T>> exchange(URI url, HttpMethod method,
													 @Nullable HttpEntity<?> requestEntity, ParameterizedTypeReference<T> responseType)
			throws RestClientException;


	// 通用执行

	/**
	 * 异步执行HTTP方法到给定的URI模板，使用{@link AsyncRequestCallback}准备请求，并使用{@link ResponseExtractor}读取响应。
	 * 如果有的话，使用给定的URI变量扩展URI模板变量。
	 *
	 * @param url               URL
	 * @param method            HTTP方法（GET、POST等）
	 * @param requestCallback   准备请求的对象
	 * @param responseExtractor 从响应中提取返回值的对象
	 * @param uriVariables      要在模板中扩展的变量
	 * @return 由{@link ResponseExtractor}返回的任意对象
	 */
	<T> ListenableFuture<T> execute(String url, HttpMethod method,
									@Nullable AsyncRequestCallback requestCallback, @Nullable ResponseExtractor<T> responseExtractor,
									Object... uriVariables) throws RestClientException;

	/**
	 * 异步执行HTTP方法到给定的URI模板，使用{@link AsyncRequestCallback}准备请求，并使用{@link ResponseExtractor}读取响应。
	 * 使用给定的URI变量映射扩展URI模板变量。
	 *
	 * @param url               URL
	 * @param method            HTTP方法（GET、POST等）
	 * @param requestCallback   准备请求的对象
	 * @param responseExtractor 从响应中提取返回值的对象
	 * @param uriVariables      要在模板中扩展的变量
	 * @return 由{@link ResponseExtractor}返回的任意对象
	 */
	<T> ListenableFuture<T> execute(String url, HttpMethod method,
									@Nullable AsyncRequestCallback requestCallback, @Nullable ResponseExtractor<T> responseExtractor,
									Map<String, ?> uriVariables) throws RestClientException;

	/**
	 * 异步执行HTTP方法到给定的URL，使用{@link AsyncRequestCallback}准备请求，并使用{@link ResponseExtractor}读取响应。
	 *
	 * @param url               URL
	 * @param method            HTTP方法（GET、POST等）
	 * @param requestCallback   准备请求的对象
	 * @param responseExtractor 从响应中提取返回值的对象
	 * @return 由{@link ResponseExtractor}返回的任意对象
	 */
	<T> ListenableFuture<T> execute(URI url, HttpMethod method,
									@Nullable AsyncRequestCallback requestCallback, @Nullable ResponseExtractor<T> responseExtractor)
			throws RestClientException;

}
