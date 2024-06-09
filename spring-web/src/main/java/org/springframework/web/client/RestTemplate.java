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
import org.springframework.core.SpringProperties;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.support.InterceptingHttpAccessor;
import org.springframework.http.converter.*;
import org.springframework.http.converter.cbor.MappingJackson2CborHttpMessageConverter;
import org.springframework.http.converter.feed.AtomFeedHttpMessageConverter;
import org.springframework.http.converter.feed.RssChannelHttpMessageConverter;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.http.converter.json.JsonbHttpMessageConverter;
import org.springframework.http.converter.json.KotlinSerializationJsonHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.smile.MappingJackson2SmileHttpMessageConverter;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.DefaultUriBuilderFactory.EncodingMode;
import org.springframework.web.util.UriTemplateHandler;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 同步客户端用于执行 HTTP 请求，提供了一个简单的模板方法 API，覆盖了诸如 JDK
 * {@code HttpURLConnection}、Apache HttpComponents 等底层 HTTP 客户端库。
 *
 * <p>RestTemplate 提供了常见场景的 HTTP 方法模板，以及支持较少见情况的通用 {@code exchange}
 * 和 {@code execute} 方法。
 *
 * <p><strong>注意：</strong>从 5.0 版本开始，此类处于维护模式，仅接受对变更和错误的轻微请求。
 * 请考虑使用 {@code org.springframework.web.reactive.client.WebClient}，它具有更现代的 API，
 * 支持同步、异步和流式场景。
 *
 * @author Arjen Poutsma
 * @author Brian Clozel
 * @author Roy Clarkson
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Sebastien Deleuze
 * @see HttpMessageConverter
 * @see RequestCallback
 * @see ResponseExtractor
 * @see ResponseErrorHandler
 * @since 3.0
 */
public class RestTemplate extends InterceptingHttpAccessor implements RestOperations {

	/**
	 * 由系统属性 {@code spring.xml.ignore} 控制的布尔标志，指示 Spring 是否忽略 XML，
	 * 即不初始化与 XML 相关的基础设施。
	 * <p>默认值为 "false"。
	 */
	private static final boolean shouldIgnoreXml = SpringProperties.getFlag("spring.xml.ignore");

	/**
	 * 是否存在Rome工具包
	 */
	private static final boolean romePresent;

	/**
	 * 是否存在JAXB2
	 */
	private static final boolean jaxb2Present;

	/**
	 * 是否存在Jackson2
	 */
	private static final boolean jackson2Present;

	/**
	 * 是否存在Jackson2 XML
	 */
	private static final boolean jackson2XmlPresent;

	/**
	 * 是否存在Jackson2 Smile
	 */
	private static final boolean jackson2SmilePresent;

	/**
	 * 是否存在Jackson2 CBOR
	 */
	private static final boolean jackson2CborPresent;

	/**
	 * 是否存在Gson
	 */
	private static final boolean gsonPresent;

	/**
	 * 是否存在JSON-B
	 */
	private static final boolean jsonbPresent;

	/**
	 * 是否存在Kotlin Serialization JSON
	 */
	private static final boolean kotlinSerializationJsonPresent;

	static {
		// 获取RestTemplate类的类加载器
		ClassLoader classLoader = RestTemplate.class.getClassLoader();
		// 检查是否存在Rome工具包
		romePresent = ClassUtils.isPresent("com.rometools.rome.feed.WireFeed", classLoader);
		// 检查是否存在JAXB2
		jaxb2Present = ClassUtils.isPresent("javax.xml.bind.Binder", classLoader);
		// 检查是否存在Jackson2
		jackson2Present = ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper", classLoader) &&
				ClassUtils.isPresent("com.fasterxml.jackson.core.JsonGenerator", classLoader);
		// 检查是否存在Jackson2 XML
		jackson2XmlPresent = ClassUtils.isPresent("com.fasterxml.jackson.dataformat.xml.XmlMapper", classLoader);
		// 检查是否存在Jackson2 Smile
		jackson2SmilePresent = ClassUtils.isPresent("com.fasterxml.jackson.dataformat.smile.SmileFactory", classLoader);
		// 检查是否存在Jackson2 CBOR
		jackson2CborPresent = ClassUtils.isPresent("com.fasterxml.jackson.dataformat.cbor.CBORFactory", classLoader);
		// 检查是否存在Gson
		gsonPresent = ClassUtils.isPresent("com.google.gson.Gson", classLoader);
		// 检查是否存在JSON-B
		jsonbPresent = ClassUtils.isPresent("javax.json.bind.Jsonb", classLoader);
		// 检查是否存在Kotlin Serialization JSON
		kotlinSerializationJsonPresent = ClassUtils.isPresent("kotlinx.serialization.json.Json", classLoader);
	}

	/**
	 * 消息转换器列表
	 */
	private final List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();

	/**
	 * 响应错误处理器
	 */
	private ResponseErrorHandler errorHandler = new DefaultResponseErrorHandler();

	/**
	 * URL模板处理器
	 */
	private UriTemplateHandler uriTemplateHandler;

	/**
	 * 响应头提取器
	 */
	private final ResponseExtractor<HttpHeaders> headersExtractor = new HeadersExtractor();


	/**
	 * 使用默认设置创建{@link RestTemplate}的新实例。初始化默认的{@link HttpMessageConverter HttpMessageConverters}。
	 */
	public RestTemplate() {
		// 添加字节数组消息转换器
		this.messageConverters.add(new ByteArrayHttpMessageConverter());
		// 添加字符串消息转换器
		this.messageConverters.add(new StringHttpMessageConverter());
		// 添加资源消息转换器
		this.messageConverters.add(new ResourceHttpMessageConverter(false));
		// 如果不应忽略 XML，尝试添加源消息转换器
		if (!shouldIgnoreXml) {
			try {
				this.messageConverters.add(new SourceHttpMessageConverter<>());
			} catch (Error err) {
				// 当没有 TransformerFactory 实现可用时忽略
			}
		}
		// 添加全面的表单消息转换器
		this.messageConverters.add(new AllEncompassingFormHttpMessageConverter());

		// 如果存在 Rome 工具包，添加 Atom Feed 和 RSS Channel 消息转换器
		if (romePresent) {
			this.messageConverters.add(new AtomFeedHttpMessageConverter());
			this.messageConverters.add(new RssChannelHttpMessageConverter());
		}

		// 如果不应忽略 XML，则根据可用性添加 XML 消息转换器
		if (!shouldIgnoreXml) {
			// 如果 Jackson2 XML 存在，则添加 Jackson2 XML 消息转换器；
			if (jackson2XmlPresent) {
				this.messageConverters.add(new MappingJackson2XmlHttpMessageConverter());
			} else if (jaxb2Present) {
				// 如果 JAXB2 存在，则添加 JAXB2 根元素消息转换器
				this.messageConverters.add(new Jaxb2RootElementHttpMessageConverter());
			}
		}

		// 根据可用性添加 JSON 消息转换器
		if (jackson2Present) {
			// 如果存在Jackson2，添加 映射Jackson2 消息转换器
			this.messageConverters.add(new MappingJackson2HttpMessageConverter());
		} else if (gsonPresent) {
			// 如果存在Gson，添加 Gson 消息转换器
			this.messageConverters.add(new GsonHttpMessageConverter());
		} else if (jsonbPresent) {
			// 如果存在JSON-B ，添加 Jsonb 消息转换器
			this.messageConverters.add(new JsonbHttpMessageConverter());
		} else if (kotlinSerializationJsonPresent) {
			// 如果存在Kotlin Serialization JSON ，添加 Kotlin序列化Json 消息转换器
			this.messageConverters.add(new KotlinSerializationJsonHttpMessageConverter());
		}

		// 如果存在 Jackson2 Smile，添加 Smile 消息转换器
		if (jackson2SmilePresent) {
			this.messageConverters.add(new MappingJackson2SmileHttpMessageConverter());
		}
		// 如果存在 Jackson2 CBOR，添加 CBOR 消息转换器
		if (jackson2CborPresent) {
			this.messageConverters.add(new MappingJackson2CborHttpMessageConverter());
		}

		// 初始化 URI 模板处理器
		this.uriTemplateHandler = initUriTemplateHandler();
	}

	/**
	 * 基于给定的{@link ClientHttpRequestFactory}创建{@link RestTemplate}的新实例。
	 *
	 * @param requestFactory 要使用的HTTP请求工厂
	 * @see org.springframework.http.client.SimpleClientHttpRequestFactory
	 * @see org.springframework.http.client.HttpComponentsClientHttpRequestFactory
	 */
	public RestTemplate(ClientHttpRequestFactory requestFactory) {
		this();
		setRequestFactory(requestFactory);
	}

	/**
	 * 使用给定的{@link HttpMessageConverter}列表创建{@link RestTemplate}的新实例。
	 *
	 * @param messageConverters 要使用的{@link HttpMessageConverter}列表
	 * @since 3.2.7
	 */
	public RestTemplate(List<HttpMessageConverter<?>> messageConverters) {
		// 验证并添加消息转换器到消息转换器列表中
		validateConverters(messageConverters);
		// 将传入的消息转换器列表添加到当前消息转换器列表中
		this.messageConverters.addAll(messageConverters);
		// 初始化 URI 模板处理器
		this.uriTemplateHandler = initUriTemplateHandler();
	}


	private static DefaultUriBuilderFactory initUriTemplateHandler() {
		// 创建默认的 URI 构建工厂
		DefaultUriBuilderFactory uriFactory = new DefaultUriBuilderFactory();
		// 设置编码模式为 URI 组件模式，保持向后兼容
		uriFactory.setEncodingMode(EncodingMode.URI_COMPONENT);
		// 返回 URI 构建工厂实例
		return uriFactory;
	}


	/**
	 * 设置要使用的消息体转换器。
	 * <p>这些转换器用于在 HTTP 请求和响应之间进行转换。
	 */
	public void setMessageConverters(List<HttpMessageConverter<?>> messageConverters) {
		// 验证消息转换器列表
		validateConverters(messageConverters);
		// 如果传入的消息转换器列表与当前消息转换器列表不同
		if (this.messageConverters != messageConverters) {
			// 清空当前消息转换器列表
			this.messageConverters.clear();
			// 将传入的消息转换器列表添加到当前消息转换器列表中
			this.messageConverters.addAll(messageConverters);
		}
	}

	private void validateConverters(List<HttpMessageConverter<?>> messageConverters) {
		Assert.notEmpty(messageConverters, "At least one HttpMessageConverter is required");
		Assert.noNullElements(messageConverters, "The HttpMessageConverter list must not contain null elements");
	}

	/**
	 * 返回消息体转换器列表。
	 * <p>返回的 {@link List} 是活动的，并可能会被附加。
	 */
	public List<HttpMessageConverter<?>> getMessageConverters() {
		return this.messageConverters;
	}

	/**
	 * 设置错误处理器。
	 * <p>默认情况下，RestTemplate 使用 {@link DefaultResponseErrorHandler}。
	 */
	public void setErrorHandler(ResponseErrorHandler errorHandler) {
		Assert.notNull(errorHandler, "ResponseErrorHandler must not be null");
		this.errorHandler = errorHandler;
	}

	/**
	 * 返回错误处理器。
	 */
	public ResponseErrorHandler getErrorHandler() {
		return this.errorHandler;
	}

	/**
	 * 配置默认的 URI 变量值。这是一个快捷方式，相当于：
	 * <pre class="code">
	 * DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory();
	 * handler.setDefaultUriVariables(...);
	 *
	 * RestTemplate restTemplate = new RestTemplate();
	 * restTemplate.setUriTemplateHandler(handler);
	 * </pre>
	 *
	 * @param uriVars 默认的 URI 变量值
	 * @since 4.3
	 */
	@SuppressWarnings("deprecation")
	public void setDefaultUriVariables(Map<String, ?> uriVars) {
		// 如果 URI 模板处理器是 DefaultUriBuilderFactory 类型的
		if (this.uriTemplateHandler instanceof DefaultUriBuilderFactory) {
			// 设置默认 URI 变量
			((DefaultUriBuilderFactory) this.uriTemplateHandler).setDefaultUriVariables(uriVars);
		} else if (this.uriTemplateHandler instanceof org.springframework.web.util.AbstractUriTemplateHandler) {
			// 如果 URI 模板处理器是 AbstractUriTemplateHandler 类型的
			// 设置默认 URI 变量
			((org.springframework.web.util.AbstractUriTemplateHandler) this.uriTemplateHandler)
					.setDefaultUriVariables(uriVars);
		} else {
			// 如果 URI 模板处理器不支持此操作
			throw new IllegalArgumentException(
					"This property is not supported with the configured UriTemplateHandler.");
		}
	}

	/**
	 * 配置 URI 模板扩展策略。
	 * <p>默认情况下，使用 {@link DefaultUriBuilderFactory}，为了向后兼容，
	 * 将编码模式设置为 {@link EncodingMode#URI_COMPONENT URI_COMPONENT}。从 5.0.8 开始，建议使用 {@link EncodingMode#TEMPLATE_AND_VALUES TEMPLATE_AND_VALUES}。
	 * <p><strong>注意：</strong>在 5.0 中，默认使用 {@link DefaultUriBuilderFactory}
	 * (在 4.3 中已弃用) 作为默认值，以及默认的 {@code parsePath} 属性的变更（从 false 变为 true）。
	 *
	 * @param handler 要使用的 URI 模板处理器
	 */
	public void setUriTemplateHandler(UriTemplateHandler handler) {
		Assert.notNull(handler, "UriTemplateHandler must not be null");
		this.uriTemplateHandler = handler;
	}

	/**
	 * 返回配置的 URI 模板处理器。
	 */
	public UriTemplateHandler getUriTemplateHandler() {
		return this.uriTemplateHandler;
	}


	// GET

	@Override
	@Nullable
	public <T> T getForObject(String url, Class<T> responseType, Object... uriVariables) throws RestClientException {
		// 创建请求回调对象，根据响应类型设置请求头信息
		RequestCallback requestCallback = acceptHeaderRequestCallback(responseType);
		// 创建 HTTP 消息转换器提取器，用于从响应中提取特定类型的对象
		HttpMessageConverterExtractor<T> responseExtractor =
				new HttpMessageConverterExtractor<>(responseType, getMessageConverters(), logger);
		// 执行 HTTP GET 请求，并返回执行结果
		return execute(url, HttpMethod.GET, requestCallback, responseExtractor, uriVariables);
	}

	@Override
	@Nullable
	public <T> T getForObject(String url, Class<T> responseType, Map<String, ?> uriVariables) throws RestClientException {
		// 创建请求回调对象，用于设置请求头信息，根据响应类型设置 Accept 头
		RequestCallback requestCallback = acceptHeaderRequestCallback(responseType);
		// 创建 HTTP 消息转换器提取器，用于从响应中提取特定类型的对象
		HttpMessageConverterExtractor<T> responseExtractor =
				new HttpMessageConverterExtractor<>(responseType, getMessageConverters(), logger);
		// 执行 HTTP GET 请求，并返回执行结果
		return execute(url, HttpMethod.GET, requestCallback, responseExtractor, uriVariables);
	}

	@Override
	@Nullable
	public <T> T getForObject(URI url, Class<T> responseType) throws RestClientException {
		// 创建请求回调对象，用于设置请求头信息，根据响应类型设置 Accept 头
		RequestCallback requestCallback = acceptHeaderRequestCallback(responseType);
		// 创建 HTTP 消息转换器提取器，用于从响应中提取特定类型的对象
		HttpMessageConverterExtractor<T> responseExtractor =
				new HttpMessageConverterExtractor<>(responseType, getMessageConverters(), logger);
		// 执行 HTTP GET 请求，并返回执行结果
		return execute(url, HttpMethod.GET, requestCallback, responseExtractor);
	}

	@Override
	public <T> ResponseEntity<T> getForEntity(String url, Class<T> responseType, Object... uriVariables)
			throws RestClientException {

		// 创建请求回调对象，用于设置请求头信息，根据响应类型设置 Accept 头
		RequestCallback requestCallback = acceptHeaderRequestCallback(responseType);
		// 创建响应提取器对象，用于提取响应实体
		ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(responseType);
		// 执行 HTTP GET 请求，获取响应实体并返回
		return nonNull(execute(url, HttpMethod.GET, requestCallback, responseExtractor, uriVariables));
	}

	@Override
	public <T> ResponseEntity<T> getForEntity(String url, Class<T> responseType, Map<String, ?> uriVariables)
			throws RestClientException {

		// 创建请求回调对象，用于设置请求头信息，根据响应类型设置 Accept 头
		RequestCallback requestCallback = acceptHeaderRequestCallback(responseType);
		// 创建响应提取器对象，用于提取响应实体
		ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(responseType);
		// 执行 HTTP GET 请求，获取响应实体并返回（确保结果非空）
		return nonNull(execute(url, HttpMethod.GET, requestCallback, responseExtractor, uriVariables));
	}

	@Override
	public <T> ResponseEntity<T> getForEntity(URI url, Class<T> responseType) throws RestClientException {
		// 创建请求回调对象，用于设置请求头信息，根据响应类型设置 Accept 头
		RequestCallback requestCallback = acceptHeaderRequestCallback(responseType);
		// 创建响应提取器对象，用于提取响应实体
		ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(responseType);
		// 执行 HTTP GET 请求，获取响应实体并返回（确保结果非空）
		return nonNull(execute(url, HttpMethod.GET, requestCallback, responseExtractor));
	}


	// HEAD

	@Override
	public HttpHeaders headForHeaders(String url, Object... uriVariables) throws RestClientException {
		return nonNull(execute(url, HttpMethod.HEAD, null, headersExtractor(), uriVariables));
	}

	@Override
	public HttpHeaders headForHeaders(String url, Map<String, ?> uriVariables) throws RestClientException {
		return nonNull(execute(url, HttpMethod.HEAD, null, headersExtractor(), uriVariables));
	}

	@Override
	public HttpHeaders headForHeaders(URI url) throws RestClientException {
		return nonNull(execute(url, HttpMethod.HEAD, null, headersExtractor()));
	}


	// POST

	@Override
	@Nullable
	public URI postForLocation(String url, @Nullable Object request, Object... uriVariables)
			throws RestClientException {

		// 创建请求回调对象，用于设置 HTTP 实体
		RequestCallback requestCallback = httpEntityCallback(request);
		// 执行 HTTP POST 请求，获取响应头信息，提取 Location 头
		HttpHeaders headers = execute(url, HttpMethod.POST, requestCallback, headersExtractor(), uriVariables);
		// 返回 Location 头的值（如果存在）
		return (headers != null ? headers.getLocation() : null);
	}

	@Override
	@Nullable
	public URI postForLocation(String url, @Nullable Object request, Map<String, ?> uriVariables)
			throws RestClientException {

		// 创建请求回调对象，用于设置 HTTP 实体
		RequestCallback requestCallback = httpEntityCallback(request);
		// 执行 HTTP POST 请求，获取响应头信息，提取 Location 头的值
		HttpHeaders headers = execute(url, HttpMethod.POST, requestCallback, headersExtractor(), uriVariables);
		// 返回 Location 头的值（如果存在）
		return (headers != null ? headers.getLocation() : null);
	}

	@Override
	@Nullable
	public URI postForLocation(URI url, @Nullable Object request) throws RestClientException {
		// 创建请求回调对象，用于设置 HTTP 实体
		RequestCallback requestCallback = httpEntityCallback(request);
		// 执行 HTTP POST 请求，获取响应头信息，提取 Location 头的值
		HttpHeaders headers = execute(url, HttpMethod.POST, requestCallback, headersExtractor());
		// 返回 Location 头的值（如果存在）
		return (headers != null ? headers.getLocation() : null);
	}

	@Override
	@Nullable
	public <T> T postForObject(String url, @Nullable Object request, Class<T> responseType,
							   Object... uriVariables) throws RestClientException {

		// 创建请求回调对象，用于设置 HTTP 实体和响应类型
		RequestCallback requestCallback = httpEntityCallback(request, responseType);
		// 创建响应提取器，用于将响应转换为指定的类型
		HttpMessageConverterExtractor<T> responseExtractor =
				new HttpMessageConverterExtractor<>(responseType, getMessageConverters(), logger);
		// 执行 HTTP POST 请求，并将响应转换为指定类型后返回
		return execute(url, HttpMethod.POST, requestCallback, responseExtractor, uriVariables);
	}

	@Override
	@Nullable
	public <T> T postForObject(String url, @Nullable Object request, Class<T> responseType,
							   Map<String, ?> uriVariables) throws RestClientException {

		// 创建请求回调对象，用于设置 HTTP 实体和响应类型
		RequestCallback requestCallback = httpEntityCallback(request, responseType);
		// 创建响应提取器，用于将响应转换为指定的类型
		HttpMessageConverterExtractor<T> responseExtractor =
				new HttpMessageConverterExtractor<>(responseType, getMessageConverters(), logger);
		// 执行 HTTP POST 请求，并将响应转换为指定类型后返回
		return execute(url, HttpMethod.POST, requestCallback, responseExtractor, uriVariables);
	}

	@Override
	@Nullable
	public <T> T postForObject(URI url, @Nullable Object request, Class<T> responseType)
			throws RestClientException {

		// 创建请求回调对象，用于设置 HTTP 实体和响应类型
		RequestCallback requestCallback = httpEntityCallback(request, responseType);
		// 创建响应提取器，用于将响应转换为指定的类型
		HttpMessageConverterExtractor<T> responseExtractor =
				new HttpMessageConverterExtractor<>(responseType, getMessageConverters());
		// 执行 HTTP POST 请求，并将响应转换为指定类型后返回
		return execute(url, HttpMethod.POST, requestCallback, responseExtractor);
	}

	@Override
	public <T> ResponseEntity<T> postForEntity(String url, @Nullable Object request,
											   Class<T> responseType, Object... uriVariables) throws RestClientException {

		// 创建请求回调对象，用于设置 HTTP 实体和响应类型
		RequestCallback requestCallback = httpEntityCallback(request, responseType);
		// 创建响应提取器，用于将响应转换为 ResponseEntity 对象
		ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(responseType);
		// 执行 HTTP POST 请求，并将响应转换为 ResponseEntity 对象后返回
		return nonNull(execute(url, HttpMethod.POST, requestCallback, responseExtractor, uriVariables));
	}

	@Override
	public <T> ResponseEntity<T> postForEntity(String url, @Nullable Object request,
											   Class<T> responseType, Map<String, ?> uriVariables) throws RestClientException {

		// 创建请求回调对象，用于设置 HTTP 实体和响应类型
		RequestCallback requestCallback = httpEntityCallback(request, responseType);
		// 创建响应提取器，用于将响应转换为 ResponseEntity 对象
		ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(responseType);
		// 执行 HTTP POST 请求，并将响应转换为 ResponseEntity 对象后返回
		return nonNull(execute(url, HttpMethod.POST, requestCallback, responseExtractor, uriVariables));
	}

	@Override
	public <T> ResponseEntity<T> postForEntity(URI url, @Nullable Object request, Class<T> responseType)
			throws RestClientException {

		// 创建请求回调对象，用于设置 HTTP 实体和响应类型
		RequestCallback requestCallback = httpEntityCallback(request, responseType);
		// 创建响应提取器，用于将响应转换为 ResponseEntity 对象
		ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(responseType);
		// 执行 HTTP POST 请求，并将响应转换为 ResponseEntity 对象后返回
		return nonNull(execute(url, HttpMethod.POST, requestCallback, responseExtractor));
	}


	// PUT

	@Override
	public void put(String url, @Nullable Object request, Object... uriVariables)
			throws RestClientException {

		// 创建请求回调对象，用于设置 HTTP 实体
		RequestCallback requestCallback = httpEntityCallback(request);
		// 执行 HTTP PUT 请求
		execute(url, HttpMethod.PUT, requestCallback, null, uriVariables);
	}

	@Override
	public void put(String url, @Nullable Object request, Map<String, ?> uriVariables)
			throws RestClientException {

		// 创建请求回调对象，用于设置 HTTP 实体
		RequestCallback requestCallback = httpEntityCallback(request);
		// 执行 HTTP PUT 请求
		execute(url, HttpMethod.PUT, requestCallback, null, uriVariables);
	}

	@Override
	public void put(URI url, @Nullable Object request) throws RestClientException {
		// 创建请求回调对象，用于设置 HTTP 实体
		RequestCallback requestCallback = httpEntityCallback(request);
		// 执行 HTTP PUT 请求
		execute(url, HttpMethod.PUT, requestCallback, null);
	}


	// PATCH

	@Override
	@Nullable
	public <T> T patchForObject(String url, @Nullable Object request, Class<T> responseType,
								Object... uriVariables) throws RestClientException {

		// 创建请求回调对象，用于设置 HTTP 实体
		RequestCallback requestCallback = httpEntityCallback(request, responseType);
		// 创建响应提取器，用于从响应中提取结果
		HttpMessageConverterExtractor<T> responseExtractor =
				new HttpMessageConverterExtractor<>(responseType, getMessageConverters(), logger);
		// 执行 HTTP PATCH 请求，并返回结果
		return execute(url, HttpMethod.PATCH, requestCallback, responseExtractor, uriVariables);
	}

	@Override
	@Nullable
	public <T> T patchForObject(String url, @Nullable Object request, Class<T> responseType,
								Map<String, ?> uriVariables) throws RestClientException {

		// 创建请求回调对象，用于设置 HTTP 实体
		RequestCallback requestCallback = httpEntityCallback(request, responseType);
		// 创建响应提取器，用于从响应中提取结果
		HttpMessageConverterExtractor<T> responseExtractor =
				new HttpMessageConverterExtractor<>(responseType, getMessageConverters(), logger);
		// 执行 HTTP PATCH 请求，并返回结果
		return execute(url, HttpMethod.PATCH, requestCallback, responseExtractor, uriVariables);
	}

	@Override
	@Nullable
	public <T> T patchForObject(URI url, @Nullable Object request, Class<T> responseType)
			throws RestClientException {

		// 创建请求回调对象，用于设置 HTTP 实体
		RequestCallback requestCallback = httpEntityCallback(request, responseType);
		// 创建响应提取器，用于从响应中提取结果
		HttpMessageConverterExtractor<T> responseExtractor =
				new HttpMessageConverterExtractor<>(responseType, getMessageConverters());
		// 执行 HTTP PATCH 请求，并返回结果
		return execute(url, HttpMethod.PATCH, requestCallback, responseExtractor);
	}


	// DELETE

	@Override
	public void delete(String url, Object... uriVariables) throws RestClientException {
		execute(url, HttpMethod.DELETE, null, null, uriVariables);
	}

	@Override
	public void delete(String url, Map<String, ?> uriVariables) throws RestClientException {
		execute(url, HttpMethod.DELETE, null, null, uriVariables);
	}

	@Override
	public void delete(URI url) throws RestClientException {
		execute(url, HttpMethod.DELETE, null, null);
	}


	// OPTIONS

	@Override
	public Set<HttpMethod> optionsForAllow(String url, Object... uriVariables) throws RestClientException {
		// 创建响应头提取器，用于提取 HTTP 响应头信息
		ResponseExtractor<HttpHeaders> headersExtractor = headersExtractor();
		// 执行 HTTP OPTIONS 请求，不带请求体，获取响应头信息
		HttpHeaders headers = execute(url, HttpMethod.OPTIONS, null, headersExtractor, uriVariables);
		// 返回允许的 HTTP 方法集合，若为空则返回空集合
		return (headers != null ? headers.getAllow() : Collections.emptySet());
	}

	@Override
	public Set<HttpMethod> optionsForAllow(String url, Map<String, ?> uriVariables) throws RestClientException {
		// 创建响应头提取器，用于提取 HTTP 响应头信息
		ResponseExtractor<HttpHeaders> headersExtractor = headersExtractor();
		// 执行 HTTP OPTIONS 请求，不带请求体，获取响应头信息
		HttpHeaders headers = execute(url, HttpMethod.OPTIONS, null, headersExtractor, uriVariables);
		// 返回允许的 HTTP 方法集合，若为空则返回空集合
		return (headers != null ? headers.getAllow() : Collections.emptySet());
	}

	@Override
	public Set<HttpMethod> optionsForAllow(URI url) throws RestClientException {
		// 创建用于提取 HTTP 响应头信息的响应提取器
		ResponseExtractor<HttpHeaders> headersExtractor = headersExtractor();
		// 执行 HTTP OPTIONS 请求，不带请求体，获取响应头信息
		HttpHeaders headers = execute(url, HttpMethod.OPTIONS, null, headersExtractor);
		// 返回允许的 HTTP 方法集合，若为空则返回空集合
		return (headers != null ? headers.getAllow() : Collections.emptySet());
	}


	// 交换方法

	@Override
	public <T> ResponseEntity<T> exchange(String url, HttpMethod method,
										  @Nullable HttpEntity<?> requestEntity, Class<T> responseType, Object... uriVariables)
			throws RestClientException {

		// 创建用于发送 HTTP 请求的请求回调对象
		RequestCallback requestCallback = httpEntityCallback(requestEntity, responseType);
		// 创建用于提取 HTTP 响应的响应提取器
		ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(responseType);
		// 执行 HTTP 请求，并获取响应实体，如果为 null，则返回 null
		return nonNull(execute(url, method, requestCallback, responseExtractor, uriVariables));
	}

	@Override
	public <T> ResponseEntity<T> exchange(String url, HttpMethod method,
										  @Nullable HttpEntity<?> requestEntity, Class<T> responseType, Map<String, ?> uriVariables)
			throws RestClientException {

		// 创建用于发送 HTTP 请求的请求回调对象
		RequestCallback requestCallback = httpEntityCallback(requestEntity, responseType);
		// 创建用于提取 HTTP 响应的响应提取器
		ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(responseType);
		// 执行 HTTP 请求，并获取响应实体，如果为 null，则返回 null
		return nonNull(execute(url, method, requestCallback, responseExtractor, uriVariables));
	}

	@Override
	public <T> ResponseEntity<T> exchange(URI url, HttpMethod method, @Nullable HttpEntity<?> requestEntity,
										  Class<T> responseType) throws RestClientException {

		// 创建用于发送 HTTP 请求的请求回调对象
		RequestCallback requestCallback = httpEntityCallback(requestEntity, responseType);
		// 创建用于提取 HTTP 响应的响应提取器
		ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(responseType);
		// 执行 HTTP 请求，并获取响应实体，如果为 null，则返回 null
		return nonNull(execute(url, method, requestCallback, responseExtractor));
	}

	@Override
	public <T> ResponseEntity<T> exchange(String url, HttpMethod method, @Nullable HttpEntity<?> requestEntity,
										  ParameterizedTypeReference<T> responseType, Object... uriVariables) throws RestClientException {

		// 获取响应类型的类型信息
		Type type = responseType.getType();
		// 创建用于发送 HTTP 请求的请求回调对象
		RequestCallback requestCallback = httpEntityCallback(requestEntity, type);
		// 创建用于提取 HTTP 响应的响应提取器
		ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(type);
		// 执行 HTTP 请求，并获取响应实体，如果为 null，则返回 null
		return nonNull(execute(url, method, requestCallback, responseExtractor, uriVariables));
	}

	@Override
	public <T> ResponseEntity<T> exchange(String url, HttpMethod method, @Nullable HttpEntity<?> requestEntity,
										  ParameterizedTypeReference<T> responseType, Map<String, ?> uriVariables) throws RestClientException {

		// 获取响应类型的类型信息
		Type type = responseType.getType();
		// 创建用于发送 HTTP 请求的请求回调对象
		RequestCallback requestCallback = httpEntityCallback(requestEntity, type);
		// 创建用于提取 HTTP 响应的响应提取器
		ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(type);
		// 执行 HTTP 请求，并获取响应实体，如果为 null，则返回 null
		return nonNull(execute(url, method, requestCallback, responseExtractor, uriVariables));
	}

	@Override
	public <T> ResponseEntity<T> exchange(URI url, HttpMethod method, @Nullable HttpEntity<?> requestEntity,
										  ParameterizedTypeReference<T> responseType) throws RestClientException {

		// 获取响应类型的类型信息
		Type type = responseType.getType();
		// 创建用于发送 HTTP 请求的请求回调对象
		RequestCallback requestCallback = httpEntityCallback(requestEntity, type);
		// 创建用于提取 HTTP 响应的响应提取器
		ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(type);
		// 执行 HTTP 请求，并获取响应实体，如果为 null，则返回 null
		return nonNull(execute(url, method, requestCallback, responseExtractor));
	}

	@Override
	public <T> ResponseEntity<T> exchange(RequestEntity<?> entity, Class<T> responseType)
			throws RestClientException {

		// 创建用于发送 HTTP 请求的请求回调对象
		RequestCallback requestCallback = httpEntityCallback(entity, responseType);
		// 创建用于提取 HTTP 响应的响应提取器
		ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(responseType);
		// 执行 HTTP 请求，并获取响应实体，如果为 null，则返回 null
		return nonNull(doExecute(resolveUrl(entity), entity.getMethod(), requestCallback, responseExtractor));
	}

	@Override
	public <T> ResponseEntity<T> exchange(RequestEntity<?> entity, ParameterizedTypeReference<T> responseType)
			throws RestClientException {

		// 获取响应类型的 Type 对象
		Type type = responseType.getType();
		// 创建用于发送 HTTP 请求的请求回调对象
		RequestCallback requestCallback = httpEntityCallback(entity, type);
		// 创建用于提取 HTTP 响应的响应提取器
		ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(type);
		// 执行 HTTP 请求，并获取响应实体，如果为 null，则返回 null
		return nonNull(doExecute(resolveUrl(entity), entity.getMethod(), requestCallback, responseExtractor));
	}

	private URI resolveUrl(RequestEntity<?> entity) {
		if (entity instanceof RequestEntity.UriTemplateRequestEntity) {
			// 如果实体是 Uri模板请求实体 类型
			RequestEntity.UriTemplateRequestEntity<?> ext = (RequestEntity.UriTemplateRequestEntity<?>) entity;
			// 如果提供了变量映射
			if (ext.getVars() != null) {
				// 使用变量映射扩展URI模板
				return this.uriTemplateHandler.expand(ext.getUriTemplate(), ext.getVars());
			} else if (ext.getVarsMap() != null) {
				// 使用变量Map扩展URI模板
				return this.uriTemplateHandler.expand(ext.getUriTemplate(), ext.getVarsMap());
			} else {
				// 如果没有提供变量，则抛出异常
				throw new IllegalStateException("No variables specified for URI template: " + ext.getUriTemplate());
			}
		} else {
			// 否则直接返回URL
			return entity.getUrl();
		}
	}


	// 通用执行方法

	/**
	 * {@inheritDoc}
	 * <p>若只提供 {@code RequestCallback} 或 {@code ResponseExtractor}，但不提供两者，可考虑使用以下方法：
	 * <ul>
	 * <li>{@link #acceptHeaderRequestCallback(Class)}
	 * <li>{@link #httpEntityCallback(Object)}
	 * <li>{@link #httpEntityCallback(Object, Type)}
	 * <li>{@link #responseEntityExtractor(Type)}
	 * </ul>
	 */
	@Override
	@Nullable
	public <T> T execute(String url, HttpMethod method, @Nullable RequestCallback requestCallback,
						 @Nullable ResponseExtractor<T> responseExtractor, Object... uriVariables) throws RestClientException {

		// 扩展URI模板
		URI expanded = getUriTemplateHandler().expand(url, uriVariables);
		// 执行请求并返回结果
		return doExecute(expanded, method, requestCallback, responseExtractor);
	}

	/**
	 * {@inheritDoc}
	 * <p>若只提供 {@code RequestCallback} 或 {@code ResponseExtractor}，但不提供两者，可考虑使用以下方法：
	 * <ul>
	 * <li>{@link #acceptHeaderRequestCallback(Class)}
	 * <li>{@link #httpEntityCallback(Object)}
	 * <li>{@link #httpEntityCallback(Object, Type)}
	 * <li>{@link #responseEntityExtractor(Type)}
	 * </ul>
	 */
	@Override
	@Nullable
	public <T> T execute(String url, HttpMethod method, @Nullable RequestCallback requestCallback,
						 @Nullable ResponseExtractor<T> responseExtractor, Map<String, ?> uriVariables)
			throws RestClientException {

		// 扩展URI模板
		URI expanded = getUriTemplateHandler().expand(url, uriVariables);
		// 执行请求并返回结果
		return doExecute(expanded, method, requestCallback, responseExtractor);
	}

	/**
	 * {@inheritDoc}
	 * <p>若只提供 {@code RequestCallback} 或 {@code ResponseExtractor}，但不提供两者，可考虑使用以下方法：
	 * <ul>
	 * <li>{@link #acceptHeaderRequestCallback(Class)}
	 * <li>{@link #httpEntityCallback(Object)}
	 * <li>{@link #httpEntityCallback(Object, Type)}
	 * <li>{@link #responseEntityExtractor(Type)}
	 * </ul>
	 */
	@Override
	@Nullable
	public <T> T execute(URI url, HttpMethod method, @Nullable RequestCallback requestCallback,
						 @Nullable ResponseExtractor<T> responseExtractor) throws RestClientException {

		return doExecute(url, method, requestCallback, responseExtractor);
	}

	/**
	 * 在提供的 URI 上执行给定的方法。
	 * <p>使用 {@link RequestCallback} 处理 {@link ClientHttpRequest}，使用 {@link ResponseExtractor} 处理响应。
	 *
	 * @param url               要连接的完全扩展的 URL
	 * @param method            要执行的 HTTP 方法（GET、POST 等）
	 * @param requestCallback   准备请求的对象（可以为 {@code null}）
	 * @param responseExtractor 从响应中提取返回值的对象（可以为 {@code null}）
	 * @return 由 {@link ResponseExtractor} 返回的任意对象
	 */
	@Nullable
	protected <T> T doExecute(URI url, @Nullable HttpMethod method, @Nullable RequestCallback requestCallback,
							  @Nullable ResponseExtractor<T> responseExtractor) throws RestClientException {

		Assert.notNull(url, "URI is required");
		Assert.notNull(method, "HttpMethod is required");
		ClientHttpResponse response = null;
		try {
			// 创建请求
			ClientHttpRequest request = createRequest(url, method);
			if (requestCallback != null) {
				// 如果有请求回调，则执行回调方法
				requestCallback.doWithRequest(request);
			}
			// 执行请求并获取响应
			response = request.execute();
			// 处理响应
			handleResponse(url, method, response);
			// 如果有响应提取器，则提取响应数据并返回
			return (responseExtractor != null ? responseExtractor.extractData(response) : null);
		} catch (IOException ex) {
			// 获取资源信息
			String resource = url.toString();
			String query = url.getRawQuery();
			resource = (query != null ? resource.substring(0, resource.indexOf('?')) : resource);
			// 抛出资源访问异常
			throw new ResourceAccessException("I/O error on " + method.name() +
					" request for \"" + resource + "\": " + ex.getMessage(), ex);
		} finally {
			// 关闭响应
			if (response != null) {
				response.close();
			}
		}
	}

	/**
	 * 处理给定的响应，执行适当的日志记录并在必要时调用 {@link ResponseErrorHandler}。
	 * <p>可在子类中进行覆盖。
	 *
	 * @param url      要连接的完全扩展的 URL
	 * @param method   要执行的 HTTP 方法（GET、POST 等）
	 * @param response 结果 {@link ClientHttpResponse}
	 * @throws IOException 如果从 {@link ResponseErrorHandler} 传播
	 * @see #setErrorHandler
	 * @since 4.1.6
	 */
	protected void handleResponse(URI url, HttpMethod method, ClientHttpResponse response) throws IOException {
		// 获取响应错误处理器对象
		ResponseErrorHandler errorHandler = getErrorHandler();
		// 检查是否有错误
		boolean hasError = errorHandler.hasError(response);
		// 如果日志级别为 DEBUG，记录响应状态信息
		if (logger.isDebugEnabled()) {
			try {
				// 获取响应状态码
				int code = response.getRawStatusCode();
				// 尝试解析状态码为 HttpStatus 对象
				HttpStatus status = HttpStatus.resolve(code);
				// 记录响应状态信息
				logger.debug("Response " + (status != null ? status : code));
			} catch (IOException ex) {
				// 忽略 IOException
			}
		}
		if (hasError) {
			// 如果存在错误，则交由错误处理器处理
			errorHandler.handleError(url, method, response);
		}
	}

	/**
	 * 返回一个{@code RequestCallback}，根据给定的响应类型设置请求{@code Accept}头，与配置的消息转换器进行交叉检查。
	 */
	public <T> RequestCallback acceptHeaderRequestCallback(Class<T> responseType) {
		return new AcceptHeaderRequestCallback(responseType);
	}

	/**
	 * 返回一个将给定对象写入请求流的{@code RequestCallback}实现。
	 */
	public <T> RequestCallback httpEntityCallback(@Nullable Object requestBody) {
		return new HttpEntityRequestCallback(requestBody);
	}

	/**
	 * 返回一个{@code RequestCallback}实现，它执行以下操作：
	 * <ol>
	 * <li>根据给定的响应类型设置请求{@code Accept}头，与配置的消息转换器进行交叉检查。
	 * <li>将给定对象写入请求流。
	 * </ol>
	 */
	public <T> RequestCallback httpEntityCallback(@Nullable Object requestBody, Type responseType) {
		return new HttpEntityRequestCallback(requestBody, responseType);
	}

	/**
	 * 返回一个准备{@link ResponseEntity}的{@code ResponseExtractor}。
	 */
	public <T> ResponseExtractor<ResponseEntity<T>> responseEntityExtractor(Type responseType) {
		return new ResponseEntityResponseExtractor<>(responseType);
	}

	/**
	 * 返回一个{@link HttpHeaders}的响应提取器。
	 */
	protected ResponseExtractor<HttpHeaders> headersExtractor() {
		return this.headersExtractor;
	}

	private static <T> T nonNull(@Nullable T result) {
		Assert.state(result != null, "No result");
		return result;
	}


	/**
	 * 准备请求的接受标头的请求回调实现。
	 */
	private class AcceptHeaderRequestCallback implements RequestCallback {

		/**
		 * 响应类型
		 */
		@Nullable
		private final Type responseType;

		public AcceptHeaderRequestCallback(@Nullable Type responseType) {
			this.responseType = responseType;
		}

		@Override
		public void doWithRequest(ClientHttpRequest request) throws IOException {
			if (this.responseType != null) {
				// 获取所有支持的媒体类型列表
				List<MediaType> allSupportedMediaTypes = getMessageConverters().stream()
						// 筛选出能够读取指定响应类型的消息转换器
						.filter(converter -> canReadResponse(this.responseType, converter))
						// 获取每个转换器支持的媒体类型列表
						.flatMap((HttpMessageConverter<?> converter) -> getSupportedMediaTypes(this.responseType, converter))
						// 去重
						.distinct()
						// 根据优先级排序
						.sorted(MediaType.SPECIFICITY_COMPARATOR)
						// 转换为列表
						.collect(Collectors.toList());
				if (logger.isDebugEnabled()) {
					logger.debug("Accept=" + allSupportedMediaTypes);
				}
				// 设置请求头的Accept字段为所有支持的媒体类型
				request.getHeaders().setAccept(allSupportedMediaTypes);
			}
		}

		private boolean canReadResponse(Type responseType, HttpMessageConverter<?> converter) {
			// 获取响应类型
			Class<?> responseClass = (responseType instanceof Class ? (Class<?>) responseType : null);
			if (responseClass != null) {
				// 如果响应类型为Class，则使用canRead方法检查是否能够读取该类型的响应
				return converter.canRead(responseClass, null);
			} else if (converter instanceof GenericHttpMessageConverter) {
				// 如果转换器是GenericHttpMessageConverter的实例
				GenericHttpMessageConverter<?> genericConverter = (GenericHttpMessageConverter<?>) converter;
				// 调用canRead方法检查是否能够读取该响应类型
				return genericConverter.canRead(responseType, null, null);
			}
			// 否则返回false
			return false;
		}

		private Stream<MediaType> getSupportedMediaTypes(Type type, HttpMessageConverter<?> converter) {
			// 获取原始类型
			Type rawType = (type instanceof ParameterizedType ? ((ParameterizedType) type).getRawType() : type);
			// 将原始类型转换为 Class 对象
			Class<?> clazz = (rawType instanceof Class ? (Class<?>) rawType : null);
			// 获取支持的媒体类型列表
			return (clazz != null ? converter.getSupportedMediaTypes(clazz) : converter.getSupportedMediaTypes())
					.stream()
					.map(mediaType -> {
						if (mediaType.getCharset() != null) {
							// 如果媒体类型的字符集不为空，则创建一个新的媒体类型对象
							return new MediaType(mediaType.getType(), mediaType.getSubtype());
						}
						return mediaType;
					});
		}
	}


	/**
	 * 请求回调实现，将给定对象写入请求流。
	 */
	private class HttpEntityRequestCallback extends AcceptHeaderRequestCallback {
		/**
		 * Http请求实体
		 */
		private final HttpEntity<?> requestEntity;

		public HttpEntityRequestCallback(@Nullable Object requestBody) {
			this(requestBody, null);
		}

		public HttpEntityRequestCallback(@Nullable Object requestBody, @Nullable Type responseType) {
			// 调用父类构造函数
			super(responseType);
			// 初始化请求实体
			if (requestBody instanceof HttpEntity) {
				this.requestEntity = (HttpEntity<?>) requestBody;
			} else if (requestBody != null) {
				this.requestEntity = new HttpEntity<>(requestBody);
			} else {
				this.requestEntity = HttpEntity.EMPTY;
			}
		}

		@Override
		@SuppressWarnings("unchecked")
		public void doWithRequest(ClientHttpRequest httpRequest) throws IOException {
			// 调用父类的doWithRequest方法，执行请求的初始化设置
			super.doWithRequest(httpRequest);
			// 获取请求体
			Object requestBody = this.requestEntity.getBody();
			// 如果请求体为空
			if (requestBody == null) {
				// 获取请求头和请求实体的头信息
				HttpHeaders httpHeaders = httpRequest.getHeaders();
				HttpHeaders requestHeaders = this.requestEntity.getHeaders();
				if (!requestHeaders.isEmpty()) {
					// 如果请求实体的头信息不为空，则将其复制到请求头中
					requestHeaders.forEach((key, values) -> httpHeaders.put(key, new ArrayList<>(values)));
				}
				if (httpHeaders.getContentLength() < 0) {
					// 如果请求头中的内容长度小于0，则设置为0
					httpHeaders.setContentLength(0L);
				}
			} else {
				// 获取请求体的类型和内容类型
				Class<?> requestBodyClass = requestBody.getClass();
				// 获取响应体类型
				Type requestBodyType = (this.requestEntity instanceof RequestEntity ?
						((RequestEntity<?>) this.requestEntity).getType() : requestBodyClass);
				// 获取请求头
				HttpHeaders httpHeaders = httpRequest.getHeaders();
				// 获取请求实体中的请求头
				HttpHeaders requestHeaders = this.requestEntity.getHeaders();
				// 获取请求内容类型
				MediaType requestContentType = requestHeaders.getContentType();
				// 遍历消息转换器
				for (HttpMessageConverter<?> messageConverter : getMessageConverters()) {
					// 如果消息转换器是通用的
					if (messageConverter instanceof GenericHttpMessageConverter) {
						GenericHttpMessageConverter<Object> genericConverter =
								(GenericHttpMessageConverter<Object>) messageConverter;
						// 如果该转换器支持写操作
						if (genericConverter.canWrite(requestBodyType, requestBodyClass, requestContentType)) {
							if (!requestHeaders.isEmpty()) {
								// 将请求头的信息复制到http请求的头中
								requestHeaders.forEach((key, values) -> httpHeaders.put(key, new ArrayList<>(values)));
							}
							// 记录请求体的信息
							logBody(requestBody, requestContentType, genericConverter);
							// 使用消息转换器写入http请求中
							genericConverter.write(requestBody, requestBodyType, requestContentType, httpRequest);
							return;
						}
					} else if (messageConverter.canWrite(requestBodyClass, requestContentType)) {
						// 如果消息转换器支持写操作
						if (!requestHeaders.isEmpty()) {
							// 将请求头的信息复制到http请求的头中
							requestHeaders.forEach((key, values) -> httpHeaders.put(key, new ArrayList<>(values)));
						}
						// 记录请求体的信息，并使用消息转换器写入http请求中
						logBody(requestBody, requestContentType, messageConverter);
						// 使用消息转换器写入http请求中
						((HttpMessageConverter<Object>) messageConverter).write(
								requestBody, requestContentType, httpRequest);
						return;
					}
				}
				// 如果找不到合适的消息转换器，则抛出异常
				String message = "No HttpMessageConverter for " + requestBodyClass.getName();
				if (requestContentType != null) {
					message += " and content type \"" + requestContentType + "\"";
				}
				// 抛出异常
				throw new RestClientException(message);
			}
		}

		private void logBody(Object body, @Nullable MediaType mediaType, HttpMessageConverter<?> converter) {
			// 如果日志级别为调试模式
			if (logger.isDebugEnabled()) {
				// 如果媒体类型不为空
				if (mediaType != null) {
					// 记录调试信息：将body作为“媒体类型”写入日志
					logger.debug("Writing [" + body + "] as \"" + mediaType + "\"");
				} else {
					// 记录调试信息：将body与转换器类名一起写入日志
					logger.debug("Writing [" + body + "] with " + converter.getClass().getName());
				}
			}
		}
	}


	/**
	 * {@link HttpEntity} 的响应提取器。
	 */
	private class ResponseEntityResponseExtractor<T> implements ResponseExtractor<ResponseEntity<T>> {

		/**
		 * Http消息转换器提取器
		 */
		@Nullable
		private final HttpMessageConverterExtractor<T> delegate;

		public ResponseEntityResponseExtractor(@Nullable Type responseType) {
			// 如果响应类型不为空，且不是 Void 类
			if (responseType != null && Void.class != responseType) {
				// 创建一个 Http消息转换器提取器 对象作为委托对象
				this.delegate = new HttpMessageConverterExtractor<>(responseType, getMessageConverters(), logger);
			} else {
				// 否则，将委托设置为 null
				this.delegate = null;
			}
		}

		@Override
		public ResponseEntity<T> extractData(ClientHttpResponse response) throws IOException {
			// 如果委托不为空
			if (this.delegate != null) {
				// 从响应中提取数据
				T body = this.delegate.extractData(response);
				// 创建一个带有状态码、头信息和响应体的 响应实体 对象
				return ResponseEntity.status(response.getRawStatusCode()).headers(response.getHeaders()).body(body);
			} else {
				// 否则，创建一个只带有状态码和头信息的 响应实体 对象
				return ResponseEntity.status(response.getRawStatusCode()).headers(response.getHeaders()).build();
			}
		}
	}


	/**
	 * 提取响应 {@link HttpHeaders} 的响应提取器。
	 */
	private static class HeadersExtractor implements ResponseExtractor<HttpHeaders> {

		@Override
		public HttpHeaders extractData(ClientHttpResponse response) {
			return response.getHeaders();
		}
	}

}
