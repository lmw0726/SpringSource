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

package org.springframework.web.client;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.lang.Nullable;

import java.net.URI;
import java.util.Map;
import java.util.Set;

/**
 * 接口指定了一组基本的 RESTful 操作。
 * {@link RestTemplate} 实现了这个接口。
 * 通常不直接使用，但是对于增强可测试性很有用，因为它可以很容易地被模拟或存根化。
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @see RestTemplate
 * @since 3.0
 */
public interface RestOperations {

	// GET

	/**
	 * 通过对指定的 URL 执行 GET 方法来检索表示。
	 * 响应（如果有）将被转换并返回。
	 * <p>URI 模板变量使用给定的 URI 变量（如果有）扩展。
	 *
	 * @param url          URL
	 * @param responseType 返回值的类型
	 * @param uriVariables 用于扩展模板的变量
	 * @return 转换后的对象
	 */
	@Nullable
	<T> T getForObject(String url, Class<T> responseType, Object... uriVariables) throws RestClientException;

	/**
	 * 通过对 URI 模板执行 GET 来检索表示。
	 * 响应（如果有）将被转换并返回。
	 * <p>URI 模板变量使用给定的映射进行扩展。
	 *
	 * @param url          URL
	 * @param responseType 返回值的类型
	 * @param uriVariables 包含 URI 模板变量的映射
	 * @return 转换后的对象
	 */
	@Nullable
	<T> T getForObject(String url, Class<T> responseType, Map<String, ?> uriVariables) throws RestClientException;

	/**
	 * 通过对 URL 执行 GET 来检索表示。
	 * 响应（如果有）将被转换并返回。
	 *
	 * @param url          URL
	 * @param responseType 返回值的类型
	 * @return 转换后的对象
	 */
	@Nullable
	<T> T getForObject(URI url, Class<T> responseType) throws RestClientException;

	/**
	 * 通过对指定的 URL 执行 GET 来检索实体。
	 * 响应将被转换并存储在 {@link ResponseEntity} 中。
	 * <p>URI 模板变量使用给定的 URI 变量（如果有）扩展。
	 *
	 * @param url          URL
	 * @param responseType 返回值的类型
	 * @param uriVariables 用于扩展模板的变量
	 * @return 实体
	 * @since 3.0.2
	 */
	<T> ResponseEntity<T> getForEntity(String url, Class<T> responseType, Object... uriVariables)
			throws RestClientException;

	/**
	 * 通过对 URI 模板执行 GET 来检索表示。
	 * 响应将被转换并存储在 {@link ResponseEntity} 中。
	 * <p>URI 模板变量使用给定的映射进行扩展。
	 *
	 * @param url          URL
	 * @param responseType 返回值的类型
	 * @param uriVariables 包含 URI 模板变量的映射
	 * @return 转换后的对象
	 * @since 3.0.2
	 */
	<T> ResponseEntity<T> getForEntity(String url, Class<T> responseType, Map<String, ?> uriVariables)
			throws RestClientException;

	/**
	 * 通过对 URL 执行 GET 来检索表示。
	 * 响应将被转换并存储在 {@link ResponseEntity} 中。
	 *
	 * @param url          URL
	 * @param responseType 返回值的类型
	 * @return 转换后的对象
	 * @since 3.0.2
	 */
	<T> ResponseEntity<T> getForEntity(URI url, Class<T> responseType) throws RestClientException;


	// HEAD

	/**
	 * 检索由URI模板指定的资源的所有标头。
	 * <p>如果有的话，URI模板变量将使用给定的URI变量进行扩展。
	 *
	 * @param url          URL
	 * @param uriVariables 扩展模板的变量
	 * @return 该资源的所有HTTP标头
	 */
	HttpHeaders headForHeaders(String url, Object... uriVariables) throws RestClientException;

	/**
	 * 检索由URI模板指定的资源的所有标头。
	 * <p>URI模板变量将使用给定的映射进行扩展。
	 *
	 * @param url          URL
	 * @param uriVariables URI模板的变量
	 * @return 该资源的所有HTTP标头
	 */
	HttpHeaders headForHeaders(String url, Map<String, ?> uriVariables) throws RestClientException;

	/**
	 * 检索由URL指定的资源的所有标头。
	 *
	 * @param url URL
	 * @return 该资源的所有HTTP标头
	 */
	HttpHeaders headForHeaders(URI url) throws RestClientException;


	// POST

	/**
	 * 通过将给定对象POST到URI模板来创建新资源，并返回{@code Location}标头的值。此标头通常指示存储新资源的位置。
	 * <p>如果有的话，URI模板变量将使用给定的URI变量进行扩展。
	 * <p>{@code request}参数可以是{@link HttpEntity}，以便将其他HTTP标头添加到请求中。
	 * <p>实体的主体，或者{@code request}本身，可以是{@link org.springframework.util.MultiValueMap MultiValueMap}，
	 * 用于创建多部分请求。 {@code MultiValueMap}中的值可以是代表部分主体的任何对象，或者是代表带有主体和标头的部分的{@link org.springframework.http.HttpEntity HttpEntity}。
	 *
	 * @param url          URL
	 * @param request      要POST的对象（可以为{@code null}）
	 * @param uriVariables 要扩展模板的变量
	 * @return {@code Location}标头的值
	 * @see HttpEntity
	 */
	@Nullable
	URI postForLocation(String url, @Nullable Object request, Object... uriVariables) throws RestClientException;

	/**
	 * 通过将给定对象POST到URI模板来创建新资源，并返回{@code Location}标头的值。此标头通常指示存储新资源的位置。
	 * <p>URI模板变量将使用给定的映射进行扩展。
	 * <p>{@code request}参数可以是{@link HttpEntity}，以便将其他HTTP标头添加到请求中。
	 * <p>实体的主体，或者{@code request}本身，可以是{@link org.springframework.util.MultiValueMap MultiValueMap}，
	 * 用于创建多部分请求。 {@code MultiValueMap}中的值可以是代表部分主体的任何对象，或者是代表带有主体和标头的部分的{@link org.springframework.http.HttpEntity HttpEntity}。
	 *
	 * @param url          URL
	 * @param request      要POST的对象（可以为{@code null}）
	 * @param uriVariables 要扩展模板的变量
	 * @return {@code Location}标头的值
	 * @see HttpEntity
	 */
	@Nullable
	URI postForLocation(String url, @Nullable Object request, Map<String, ?> uriVariables)
			throws RestClientException;

	/**
	 * 通过将给定对象POST到URL来创建新资源，并返回{@code Location}标头的值。此标头通常指示存储新资源的位置。
	 * <p>{@code request}参数可以是{@link HttpEntity}，以便将其他HTTP标头添加到请求中。
	 * <p>实体的主体，或者{@code request}本身，可以是{@link org.springframework.util.MultiValueMap MultiValueMap}，
	 * 用于创建多部分请求。 {@code MultiValueMap}中的值可以是代表部分主体的任何对象，或者是代表带有主体和标头的部分的{@link org.springframework.http.HttpEntity HttpEntity}。
	 *
	 * @param url     URL
	 * @param request 要POST的对象（可以为{@code null}）
	 * @return {@code Location}标头的值
	 * @see HttpEntity
	 */
	@Nullable
	URI postForLocation(URI url, @Nullable Object request) throws RestClientException;

	/**
	 * 通过将给定对象 POST 到 URI 模板创建新资源，并返回响应中找到的表示。
	 * <p>URI 模板变量使用给定的 URI 变量（如果有）扩展。
	 * <p>如果要向请求中添加其他 HTTP 标头，则{@code request}参数可以是{@link HttpEntity}。
	 * <p>实体的主体，或者{@code request}本身，可以是{@link org.springframework.util.MultiValueMap MultiValueMap}，
	 * 以创建一个多部分请求。 {@code MultiValueMap}中的值可以是表示部分主体的任何对象，或者表示带有主体和标头的部分的{@link org.springframework.http.HttpEntity HttpEntity}。
	 *
	 * @param url          URL
	 * @param request      要 POST 的对象（可以为{@code null}）
	 * @param responseType 返回值的类型
	 * @param uriVariables 用于扩展模板的变量
	 * @return 转换后的对象
	 * @see HttpEntity
	 */
	@Nullable
	<T> T postForObject(String url, @Nullable Object request, Class<T> responseType,
						Object... uriVariables) throws RestClientException;

	/**
	 * 通过将给定对象 POST 到 URI 模板创建新资源，并返回响应中找到的表示。
	 * <p>URI 模板变量使用给定的映射进行扩展。
	 * <p>如果要向请求中添加其他 HTTP 标头，则{@code request}参数可以是{@link HttpEntity}。
	 * <p>实体的主体，或者{@code request}本身，可以是{@link org.springframework.util.MultiValueMap MultiValueMap}，
	 * 以创建一个多部分请求。 {@code MultiValueMap}中的值可以是表示部分主体的任何对象，或者表示带有主体和标头的部分的{@link org.springframework.http.HttpEntity HttpEntity}。
	 *
	 * @param url          URL
	 * @param request      要 POST 的对象（可以为{@code null}）
	 * @param responseType 返回值的类型
	 * @param uriVariables 用于扩展模板的变量
	 * @return 转换后的对象
	 * @see HttpEntity
	 */
	@Nullable
	<T> T postForObject(String url, @Nullable Object request, Class<T> responseType,
						Map<String, ?> uriVariables) throws RestClientException;

	/**
	 * 通过将给定对象 POST 到 URL 创建新资源，并返回响应中找到的表示。
	 * <p>如果要向请求中添加其他 HTTP 标头，则{@code request}参数可以是{@link HttpEntity}。
	 * <p>实体的主体，或者{@code request}本身，可以是{@link org.springframework.util.MultiValueMap MultiValueMap}，
	 * 以创建一个多部分请求。 {@code MultiValueMap}中的值可以是表示部分主体的任何对象，或者表示带有主体和标头的部分的{@link org.springframework.http.HttpEntity HttpEntity}。
	 *
	 * @param url          URL
	 * @param request      要 POST 的对象（可以为{@code null}）
	 * @param responseType 返回值的类型
	 * @return 转换后的对象
	 * @see HttpEntity
	 */
	@Nullable
	<T> T postForObject(URI url, @Nullable Object request, Class<T> responseType) throws RestClientException;

	/**
	 * 通过将给定对象 POST 到 URI 模板创建新资源，并将响应作为{@link ResponseEntity}返回。
	 * <p>URI 模板变量使用给定的 URI 变量（如果有）扩展。
	 * <p>如果要向请求中添加其他 HTTP 标头，则{@code request}参数可以是{@link HttpEntity}。
	 * <p>实体的主体，或者{@code request}本身，可以是{@link org.springframework.util.MultiValueMap MultiValueMap}，
	 * 以创建一个多部分请求。 {@code MultiValueMap}中的值可以是表示部分主体的任何对象，或者表示带有主体和标头的部分的{@link org.springframework.http.HttpEntity HttpEntity}。
	 *
	 * @param url          URL
	 * @param request      要 POST 的对象（可以为{@code null}）
	 * @param uriVariables 用于扩展模板的变量
	 * @return 转换后的对象
	 * @see HttpEntity
	 * @since 3.0.2
	 */
	<T> ResponseEntity<T> postForEntity(String url, @Nullable Object request, Class<T> responseType,
										Object... uriVariables) throws RestClientException;

	/**
	 * 通过将给定对象 POST 到 URI 模板创建新资源，并将响应作为{@link HttpEntity}返回。
	 * <p>URI 模板变量使用给定的映射进行扩展。
	 * <p>如果要向请求中添加其他 HTTP 标头，则{@code request}参数可以是{@link HttpEntity}。
	 * <p>实体的主体，或者{@code request}本身，可以是{@link org.springframework.util.MultiValueMap MultiValueMap}，
	 * 以创建一个多部分请求。 {@code MultiValueMap}中的值可以是表示部分主体的任何对象，或者表示带有主体和标头的部分的{@link org.springframework.http.HttpEntity HttpEntity}。
	 *
	 * @param url          URL
	 * @param request      要 POST 的对象（可以为{@code null}）
	 * @param uriVariables 用于扩展模板的变量
	 * @return 转换后的对象
	 * @see HttpEntity
	 * @since 3.0.2
	 */
	<T> ResponseEntity<T> postForEntity(String url, @Nullable Object request, Class<T> responseType,
										Map<String, ?> uriVariables) throws RestClientException;

	/**
	 * 通过将给定对象 POST 到 URL 创建新资源，并将响应作为{@link ResponseEntity}返回。
	 * <p>如果要向请求中添加其他 HTTP 标头，则{@code request}参数可以是{@link HttpEntity}。
	 * <p>实体的主体，或者{@code request}本身，可以是{@link org.springframework.util.MultiValueMap MultiValueMap}，
	 * 以创建一个多部分请求。 {@code MultiValueMap}中的值可以是表示部分主体的任何对象，或者表示带有主体和标头的部分的{@link org.springframework.http.HttpEntity HttpEntity}。
	 *
	 * @param url     URL
	 * @param request 要 POST 的对象（可以为{@code null}）
	 * @return 转换后的对象
	 * @see HttpEntity
	 * @since 3.0.2
	 */
	<T> ResponseEntity<T> postForEntity(URI url, @Nullable Object request, Class<T> responseType)
			throws RestClientException;


	// PUT

	/**
	 * 通过将给定对象PUT到URI来创建或更新资源。
	 * <p>如果有的话，URI模板变量将使用给定的URI变量进行扩展。
	 * <p>{@code request}参数可以是{@link HttpEntity}，以便将其他HTTP标头添加到请求中。
	 *
	 * @param url          URL
	 * @param request      要PUT的对象（可能为{@code null}）
	 * @param uriVariables 要扩展模板的变量
	 * @see HttpEntity
	 */
	void put(String url, @Nullable Object request, Object... uriVariables) throws RestClientException;

	/**
	 * 通过将给定对象PUT到URI模板来创建新资源。
	 * <p>URI模板变量将使用给定的映射进行扩展。
	 * <p>{@code request}参数可以是{@link HttpEntity}，以便将其他HTTP标头添加到请求中。
	 *
	 * @param url          URL
	 * @param request      要PUT的对象（可能为{@code null}）
	 * @param uriVariables 要扩展模板的变量
	 * @see HttpEntity
	 */
	void put(String url, @Nullable Object request, Map<String, ?> uriVariables) throws RestClientException;

	/**
	 * 通过将给定对象PUT到URL来创建新资源。
	 * <p>{@code request}参数可以是{@link HttpEntity}，以便将其他HTTP标头添加到请求中。
	 *
	 * @param url     URL
	 * @param request 要PUT的对象（可能为{@code null}）
	 * @see HttpEntity
	 */
	void put(URI url, @Nullable Object request) throws RestClientException;


	// PATCH

	/**
	 * 通过将给定对象PATCH到URI模板来更新资源，并返回响应中找到的表示形式。
	 * <p>如果有的话，URI模板变量将使用给定的URI变量进行扩展。
	 * <p>{@code request}参数可以是{@link HttpEntity}，以便将其他HTTP标头添加到请求中。
	 * <p><b>注意：标准的JDK HTTP库不支持HTTP PATCH。您需要使用Apache HttpComponents或OkHttp请求工厂。</b>
	 *
	 * @param url          URL
	 * @param request      要PATCH的对象（可能为{@code null}）
	 * @param responseType 返回值的类型
	 * @param uriVariables 要扩展模板的变量
	 * @return 转换后的对象
	 * @see HttpEntity
	 * @see RestTemplate#setRequestFactory
	 * @see org.springframework.http.client.HttpComponentsAsyncClientHttpRequestFactory
	 * @see org.springframework.http.client.OkHttp3ClientHttpRequestFactory
	 * @since 4.3.5
	 */
	@Nullable
	<T> T patchForObject(String url, @Nullable Object request, Class<T> responseType, Object... uriVariables)
			throws RestClientException;

	/**
	 * 通过将给定对象PATCH到URI模板来更新资源，并返回响应中找到的表示形式。
	 * <p>URI模板变量将使用给定的映射进行扩展。
	 * <p>{@code request}参数可以是{@link HttpEntity}，以便将其他HTTP标头添加到请求中。
	 * <p><b>注意：标准的JDK HTTP库不支持HTTP PATCH。您需要使用Apache HttpComponents或OkHttp请求工厂。</b>
	 *
	 * @param url          URL
	 * @param request      要PATCH的对象（可能为{@code null}）
	 * @param responseType 返回值的类型
	 * @param uriVariables 要扩展模板的变量
	 * @return 转换后的对象
	 * @see HttpEntity
	 * @see RestTemplate#setRequestFactory
	 * @see org.springframework.http.client.HttpComponentsAsyncClientHttpRequestFactory
	 * @see org.springframework.http.client.OkHttp3ClientHttpRequestFactory
	 * @since 4.3.5
	 */
	@Nullable
	<T> T patchForObject(String url, @Nullable Object request, Class<T> responseType,
						 Map<String, ?> uriVariables) throws RestClientException;

	/**
	 * 通过将给定对象PATCH到URL来更新资源，并返回响应中找到的表示形式。
	 * <p>{@code request}参数可以是{@link HttpEntity}，以便将其他HTTP标头添加到请求中。
	 * <p><b>注意：标准的JDK HTTP库不支持HTTP PATCH。您需要使用Apache HttpComponents或OkHttp请求工厂。</b>
	 *
	 * @param url          URL
	 * @param request      要PATCH的对象（可能为{@code null}）
	 * @param responseType 返回值的类型
	 * @return 转换后的对象
	 * @see HttpEntity
	 * @see RestTemplate#setRequestFactory
	 * @see org.springframework.http.client.HttpComponentsAsyncClientHttpRequestFactory
	 * @see org.springframework.http.client.OkHttp3ClientHttpRequestFactory
	 * @since 4.3.5
	 */
	@Nullable
	<T> T patchForObject(URI url, @Nullable Object request, Class<T> responseType)
			throws RestClientException;



	// DELETE

	/**
	 * 删除指定 URI 上的资源。
	 * <p>URI 模板变量使用给定的 URI 变量（如果有）扩展。
	 *
	 * @param url          URL
	 * @param uriVariables 要在模板中扩展的变量
	 */
	void delete(String url, Object... uriVariables) throws RestClientException;

	/**
	 * 删除指定 URI 上的资源。
	 * <p>URI 模板变量使用给定的映射进行扩展。
	 *
	 * @param url          URL
	 * @param uriVariables 要扩展模板的变量
	 */
	void delete(String url, Map<String, ?> uriVariables) throws RestClientException;

	/**
	 * 删除指定的 URL 上的资源。
	 *
	 * @param url URL
	 */
	void delete(URI url) throws RestClientException;


	// OPTIONS

	/**
	 * 返回给定 URI 的 Allow 标头的值。
	 * <p>URI 模板变量使用给定的 URI 变量（如果有）扩展。
	 *
	 * @param url          URL
	 * @param uriVariables 要在模板中扩展的变量
	 * @return allow 标头的值
	 */
	Set<HttpMethod> optionsForAllow(String url, Object... uriVariables) throws RestClientException;

	/**
	 * 返回给定 URI 的 Allow 标头的值。
	 * <p>URI 模板变量使用给定的映射进行扩展。
	 *
	 * @param url          URL
	 * @param uriVariables 要在模板中扩展的变量
	 * @return allow 标头的值
	 */
	Set<HttpMethod> optionsForAllow(String url, Map<String, ?> uriVariables) throws RestClientException;

	/**
	 * 返回给定 URL 的 Allow 标头的值。
	 *
	 * @param url URL
	 * @return allow 标头的值
	 */
	Set<HttpMethod> optionsForAllow(URI url) throws RestClientException;


	// 交换方法

	/**
	 * 执行给定 URI 模板的 HTTP 方法，将给定的请求实体写入请求，并将响应作为{@link ResponseEntity}返回。
	 * <p>URI 模板变量使用给定的 URI 变量（如果有）扩展。
	 *
	 * @param url           URL
	 * @param method        HTTP 方法（GET、POST 等）
	 * @param requestEntity 要写入请求的实体（可以为{@code null}）
	 * @param responseType  要将响应转换为的类型，或者对于无正文的情况为{@code Void.class}
	 * @param uriVariables  要在模板中扩展的变量
	 * @return 实体作为响应
	 * @since 3.0.2
	 */
	<T> ResponseEntity<T> exchange(String url, HttpMethod method, @Nullable HttpEntity<?> requestEntity,
								   Class<T> responseType, Object... uriVariables) throws RestClientException;

	/**
	 * 执行给定 URI 模板的 HTTP 方法，将给定的请求实体写入请求，并将响应作为{@link ResponseEntity}返回。
	 * <p>URI 模板变量使用给定的 URI 变量（如果有）扩展。
	 *
	 * @param url           URL
	 * @param method        HTTP 方法（GET、POST 等）
	 * @param requestEntity 要写入请求的实体（可以为{@code null}）
	 * @param responseType  要将响应转换为的类型，或者对于无正文的情况为{@code Void.class}
	 * @param uriVariables  要在模板中扩展的变量
	 * @return 实体作为响应
	 * @since 3.0.2
	 */
	<T> ResponseEntity<T> exchange(String url, HttpMethod method, @Nullable HttpEntity<?> requestEntity,
								   Class<T> responseType, Map<String, ?> uriVariables) throws RestClientException;

	/**
	 * 执行给定 URI 的 HTTP 方法，将给定的请求实体写入请求，并将响应作为{@link ResponseEntity}返回。
	 *
	 * @param url           URL
	 * @param method        HTTP 方法（GET、POST 等）
	 * @param requestEntity 要写入请求的实体（可以为{@code null}）
	 * @param responseType  要将响应转换为的类型，或者对于无正文的情况为{@code Void.class}
	 * @return 实体作为响应
	 * @since 3.0.2
	 */
	<T> ResponseEntity<T> exchange(URI url, HttpMethod method, @Nullable HttpEntity<?> requestEntity,
								   Class<T> responseType) throws RestClientException;

	/**
	 * 执行HTTP方法到给定的URI模板，将给定的请求实体写入请求，并将响应作为{@link ResponseEntity}返回。
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
	 * @param requestEntity 要写入请求的实体（可能为{@code null}）
	 * @param responseType  要将响应转换为的类型，或者{@code Void.class}表示无主体
	 * @param uriVariables  要在模板中扩展的变量
	 * @return 响应作为实体
	 * @since 3.2
	 */
	<T> ResponseEntity<T> exchange(String url, HttpMethod method, @Nullable HttpEntity<?> requestEntity,
								   ParameterizedTypeReference<T> responseType, Object... uriVariables) throws RestClientException;

	/**
	 * 执行HTTP方法到给定的URI模板，将给定的请求实体写入请求，并将响应作为{@link ResponseEntity}返回。
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
	 * @param requestEntity 要写入请求的实体（可能为{@code null}）
	 * @param responseType  要将响应转换为的类型，或者{@code Void.class}表示无主体
	 * @param uriVariables  要在模板中扩展的变量
	 * @return 响应作为实体
	 * @since 3.2
	 */
	<T> ResponseEntity<T> exchange(String url, HttpMethod method, @Nullable HttpEntity<?> requestEntity,
								   ParameterizedTypeReference<T> responseType, Map<String, ?> uriVariables) throws RestClientException;

	/**
	 * 执行HTTP方法到给定的URI模板，将给定的请求实体写入请求，并将响应作为{@link ResponseEntity}返回。
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
	 * @param requestEntity 要写入请求的实体（可能为{@code null}）
	 * @param responseType  要将响应转换为的类型，或者{@code Void.class}表示无主体
	 * @return 响应作为实体
	 * @since 3.2
	 */
	<T> ResponseEntity<T> exchange(URI url, HttpMethod method, @Nullable HttpEntity<?> requestEntity,
								   ParameterizedTypeReference<T> responseType) throws RestClientException;

	/**
	 * 执行给定的{@link RequestEntity}中指定的请求，并将响应作为{@link ResponseEntity}返回。
	 * 通常与{@code RequestEntity}上的静态构建方法结合使用，例如：
	 * <pre class="code">
	 * MyRequest body = ...
	 * RequestEntity request = RequestEntity
	 *     .post(new URI(&quot;https://example.com/foo&quot;))
	 *     .accept(MediaType.APPLICATION_JSON)
	 *     .body(body);
	 * ResponseEntity&lt;MyResponse&gt; response = template.exchange(request, MyResponse.class);
	 * </pre>
	 *
	 * @param requestEntity 要写入请求的实体
	 * @param responseType  要将响应转换为的类型，或者{@code Void.class}表示无主体
	 * @return 响应作为实体
	 * @since 4.1
	 */
	<T> ResponseEntity<T> exchange(RequestEntity<?> requestEntity, Class<T> responseType)
			throws RestClientException;

	/**
	 * 执行给定的{@link RequestEntity}中指定的请求，并将响应作为{@link ResponseEntity}返回。
	 * 给定的{@link ParameterizedTypeReference}用于传递泛型类型信息：
	 * <pre class="code">
	 * MyRequest body = ...
	 * RequestEntity request = RequestEntity
	 *     .post(new URI(&quot;https://example.com/foo&quot;))
	 *     .accept(MediaType.APPLICATION_JSON)
	 *     .body(body);
	 * ParameterizedTypeReference&lt;List&lt;MyResponse&gt;&gt; myBean =
	 *     new ParameterizedTypeReference&lt;List&lt;MyResponse&gt;&gt;() {};
	 * ResponseEntity&lt;List&lt;MyResponse&gt;&gt; response = template.exchange(request, myBean);
	 * </pre>
	 *
	 * @param requestEntity 要写入请求的实体
	 * @param responseType  要将响应转换为的类型，或者{@code Void.class}表示无主体
	 * @return 响应作为实体
	 * @since 4.1
	 */
	<T> ResponseEntity<T> exchange(RequestEntity<?> requestEntity, ParameterizedTypeReference<T> responseType)
			throws RestClientException;

	// 通用执行方法

	/**
	 * 执行HTTP方法到给定的URI模板，使用{@link RequestCallback}准备请求，并使用{@link ResponseExtractor}读取响应。
	 * 如果有的话，URI模板变量将使用给定的URI变量进行扩展。
	 *
	 * @param url               URL
	 * @param method            HTTP方法（GET、POST等）
	 * @param requestCallback   准备请求的对象
	 * @param responseExtractor 从响应中提取返回值的对象
	 * @param uriVariables      要在模板中扩展的变量
	 * @return 由{@link ResponseExtractor}返回的任意对象
	 */
	@Nullable
	<T> T execute(String url, HttpMethod method, @Nullable RequestCallback requestCallback,
				  @Nullable ResponseExtractor<T> responseExtractor, Object... uriVariables)
			throws RestClientException;

	/**
	 * 执行HTTP方法到给定的URI模板，使用{@link RequestCallback}准备请求，并使用{@link ResponseExtractor}读取响应。
	 * URI模板变量将使用给定的URI变量映射进行扩展。
	 *
	 * @param url               URL
	 * @param method            HTTP方法（GET、POST等）
	 * @param requestCallback   准备请求的对象
	 * @param responseExtractor 从响应中提取返回值的对象
	 * @param uriVariables      要在模板中扩展的变量
	 * @return 由{@link ResponseExtractor}返回的任意对象
	 */
	@Nullable
	<T> T execute(String url, HttpMethod method, @Nullable RequestCallback requestCallback,
				  @Nullable ResponseExtractor<T> responseExtractor, Map<String, ?> uriVariables)
			throws RestClientException;

	/**
	 * 执行HTTP方法到给定的URL，使用{@link RequestCallback}准备请求，并使用{@link ResponseExtractor}读取响应。
	 *
	 * @param url               URL
	 * @param method            HTTP方法（GET、POST等）
	 * @param requestCallback   准备请求的对象
	 * @param responseExtractor 从响应中提取返回值的对象
	 * @return 由{@link ResponseExtractor}返回的任意对象
	 */
	@Nullable
	<T> T execute(URI url, HttpMethod method, @Nullable RequestCallback requestCallback,
				  @Nullable ResponseExtractor<T> responseExtractor) throws RestClientException;

}
