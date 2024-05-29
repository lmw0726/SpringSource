/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.web.server.adapter;

import org.springframework.context.ApplicationContext;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Hints;
import org.springframework.http.*;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.codec.multipart.Part;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.*;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import org.springframework.web.server.i18n.LocaleContextResolver;
import org.springframework.web.server.session.WebSessionManager;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Default implementation of {@link ServerWebExchange}.
 * {@link ServerWebExchange}的默认实现
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class DefaultServerWebExchange implements ServerWebExchange {

	/**
	 * 包私有的常量列表，表示安全的 HTTP 方法，即不修改资源的方法。
	 */
	private static final List<HttpMethod> SAFE_METHODS = Arrays.asList(HttpMethod.GET, HttpMethod.HEAD);

	/**
	 * 表示表单数据的可解析类型。
	 */
	private static final ResolvableType FORM_DATA_TYPE =
			ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, String.class);

	/**
	 * 表示多部分数据的可解析类型。
	 */
	private static final ResolvableType MULTIPART_DATA_TYPE = ResolvableType.forClassWithGenerics(
			MultiValueMap.class, String.class, Part.class);

	/**
	 * 空表单数据的 Mono。
	 */
	private static final Mono<MultiValueMap<String, String>> EMPTY_FORM_DATA =
			Mono.just(CollectionUtils.unmodifiableMultiValueMap(new LinkedMultiValueMap<String, String>(0)))
					.cache();

	/**
	 * 空多部分数据的 Mono。
	 */
	private static final Mono<MultiValueMap<String, Part>> EMPTY_MULTIPART_DATA =
			Mono.just(CollectionUtils.unmodifiableMultiValueMap(new LinkedMultiValueMap<String, Part>(0)))
					.cache();


	/**
	 * HTTP 请求对象。
	 */
	private final ServerHttpRequest request;

	/**
	 * HTTP 响应对象。
	 */
	private final ServerHttpResponse response;

	/**
	 * 保存请求属性的并发哈希映射。
	 */
	private final Map<String, Object> attributes = new ConcurrentHashMap<>();

	/**
	 * Web 会话的 Mono。
	 */
	private final Mono<WebSession> sessionMono;

	/**
	 * 区域设置上下文解析器。
	 */
	private final LocaleContextResolver localeContextResolver;

	/**
	 * 表单数据的 Mono。
	 */
	private final Mono<MultiValueMap<String, String>> formDataMono;

	/**
	 * 多部分数据的 Mono。
	 */
	private final Mono<MultiValueMap<String, Part>> multipartDataMono;

	/**
	 * 应用程序上下文。
	 */
	@Nullable
	private final ApplicationContext applicationContext;

	/**
	 * 标记是否为未修改的状态。
	 */
	private volatile boolean notModified;

	/**
	 * URL 转换函数。
	 */
	private Function<String, String> urlTransformer = url -> url;

	/**
	 * 日志标识。
	 */
	@Nullable
	private Object logId;

	/**
	 * 日志前缀。
	 */
	private String logPrefix = "";


	public DefaultServerWebExchange(ServerHttpRequest request, ServerHttpResponse response,
									WebSessionManager sessionManager, ServerCodecConfigurer codecConfigurer,
									LocaleContextResolver localeContextResolver) {

		this(request, response, sessionManager, codecConfigurer, localeContextResolver, null);
	}

	DefaultServerWebExchange(ServerHttpRequest request, ServerHttpResponse response,
							 WebSessionManager sessionManager, ServerCodecConfigurer codecConfigurer,
							 LocaleContextResolver localeContextResolver, @Nullable ApplicationContext applicationContext) {

		Assert.notNull(request, "'request' is required");
		Assert.notNull(response, "'response' is required");
		Assert.notNull(sessionManager, "'sessionManager' is required");
		Assert.notNull(codecConfigurer, "'codecConfigurer' is required");
		Assert.notNull(localeContextResolver, "'localeContextResolver' is required");

		// 在首次调用getLogPrefix() 之前进行初始化
		this.attributes.put(ServerWebExchange.LOG_ID_ATTRIBUTE, request.getId());

		// 初始化请求对象
		this.request = request;
		// 初始化响应对象
		this.response = response;
		// 初始化 WebSession 的 Mono，并进行缓存
		this.sessionMono = sessionManager.getSession(this).cache();
		// 初始化 LocaleContextResolver
		this.localeContextResolver = localeContextResolver;
		// 初始化表单数据的 Mono
		this.formDataMono = initFormData(request, codecConfigurer, getLogPrefix());
		// 初始化多部分数据的 Mono
		this.multipartDataMono = initMultipartData(request, codecConfigurer, getLogPrefix());
		// 初始化应用程序上下文
		this.applicationContext = applicationContext;
	}

	@SuppressWarnings("unchecked")
	private static Mono<MultiValueMap<String, String>> initFormData(ServerHttpRequest request,
																	ServerCodecConfigurer configurer, String logPrefix) {

		try {
			// 获取请求的内容类型
			MediaType contentType = request.getHeaders().getContentType();
			// 如果内容类型为表单数据
			if (MediaType.APPLICATION_FORM_URLENCODED.isCompatibleWith(contentType)) {
				// 从配置中获取能够读取表单数据的HttpMessageReader
				return ((HttpMessageReader<MultiValueMap<String, String>>) configurer.getReaders().stream()
						.filter(reader -> reader.canRead(FORM_DATA_TYPE, MediaType.APPLICATION_FORM_URLENCODED))
						.findFirst()
						// 如果没有找到适合的HttpMessageReader则抛出异常
						.orElseThrow(() -> new IllegalStateException("No form data HttpMessageReader.")))
						// 读取请求中的表单数据并转换为Mono
						.readMono(FORM_DATA_TYPE, request, Hints.from(Hints.LOG_PREFIX_HINT, logPrefix))
						// 如果读取结果为空则切换到空的表单数据
						.switchIfEmpty(EMPTY_FORM_DATA)
						// 缓存读取结果
						.cache();
			}
		} catch (InvalidMediaTypeException ex) {
			// 忽略无效的媒体类型异常
		}
		// 返回空的表单数据
		return EMPTY_FORM_DATA;
	}

	@SuppressWarnings("unchecked")
	private static Mono<MultiValueMap<String, Part>> initMultipartData(ServerHttpRequest request,
																	   ServerCodecConfigurer configurer, String logPrefix) {

		try {
			// 获取请求的内容类型
			MediaType contentType = request.getHeaders().getContentType();
			// 如果内容类型为多部分表单数据
			if (MediaType.MULTIPART_FORM_DATA.isCompatibleWith(contentType)) {
				// 从配置中获取能够读取多部分表单数据的HttpMessageReader
				return ((HttpMessageReader<MultiValueMap<String, Part>>) configurer.getReaders().stream()
						.filter(reader -> reader.canRead(MULTIPART_DATA_TYPE, MediaType.MULTIPART_FORM_DATA))
						.findFirst()
						// 如果没有找到适合的HttpMessageReader则抛出异常
						.orElseThrow(() -> new IllegalStateException("No multipart HttpMessageReader.")))
						// 读取请求中的多部分表单数据并转换为Mono
						.readMono(MULTIPART_DATA_TYPE, request, Hints.from(Hints.LOG_PREFIX_HINT, logPrefix))
						// 如果读取结果为空则切换到空的多部分表单数据
						.switchIfEmpty(EMPTY_MULTIPART_DATA)
						// 缓存读取结果
						.cache();
			}
		} catch (InvalidMediaTypeException ex) {
			// 忽略无效的媒体类型异常
		}
		// 返回空的多部分表单数据
		return EMPTY_MULTIPART_DATA;
	}


	@Override
	public ServerHttpRequest getRequest() {
		return this.request;
	}

	private HttpHeaders getRequestHeaders() {
		return getRequest().getHeaders();
	}

	@Override
	public ServerHttpResponse getResponse() {
		return this.response;
	}

	private HttpHeaders getResponseHeaders() {
		return getResponse().getHeaders();
	}

	@Override
	public Map<String, Object> getAttributes() {
		return this.attributes;
	}

	@Override
	public Mono<WebSession> getSession() {
		return this.sessionMono;
	}

	@Override
	public <T extends Principal> Mono<T> getPrincipal() {
		return Mono.empty();
	}

	@Override
	public Mono<MultiValueMap<String, String>> getFormData() {
		return this.formDataMono;
	}

	@Override
	public Mono<MultiValueMap<String, Part>> getMultipartData() {
		return this.multipartDataMono;
	}

	@Override
	public LocaleContext getLocaleContext() {
		return this.localeContextResolver.resolveLocaleContext(this);
	}

	@Override
	@Nullable
	public ApplicationContext getApplicationContext() {
		return this.applicationContext;
	}

	@Override
	public boolean isNotModified() {
		return this.notModified;
	}

	@Override
	public boolean checkNotModified(Instant lastModified) {
		return checkNotModified(null, lastModified);
	}

	@Override
	public boolean checkNotModified(String etag) {
		return checkNotModified(etag, Instant.MIN);
	}

	@Override
	public boolean checkNotModified(@Nullable String etag, Instant lastModified) {
		// 获取响应的状态码
		HttpStatus status = getResponse().getStatusCode();
		if (this.notModified || (status != null && !HttpStatus.OK.equals(status))) {
			// 如果是未修改或者状态码不为OK，则返回未修改标志
			return this.notModified;
		}

		// 按照优先级顺序评估条件
		// 参考：https://tools.ietf.org/html/rfc7232#section-6

		// 验证If-Unmodified-Since条件
		if (validateIfUnmodifiedSince(lastModified)) {
			if (this.notModified) {
				// 如果未修改标志已经设置，则设置响应状态码为412（Precondition Failed）
				getResponse().setStatusCode(HttpStatus.PRECONDITION_FAILED);
			}
			// 返回未修改标志
			return this.notModified;
		}

		// 如果没有匹配ETag，则进行验证
		boolean validated = validateIfNoneMatch(etag);
		if (!validated) {
			// 如果If-None-Match条件未通过，则验证If-Modified-Since条件
			validateIfModifiedSince(lastModified);
		}

		// 更新响应

		// 判断请求方法是否为GET或HEAD
		boolean isHttpGetOrHead = SAFE_METHODS.contains(getRequest().getMethod());
		if (this.notModified) {
			// 如果未修改标志已经设置，则根据请求方法设置响应状态码
			getResponse().setStatusCode(isHttpGetOrHead ?
					HttpStatus.NOT_MODIFIED : HttpStatus.PRECONDITION_FAILED);
		}
		if (isHttpGetOrHead) {
			// 如果是GET或HEAD请求，则根据情况设置Last-Modified和ETag头部字段
			// 如果 最后更新的时间 在 1970-01-01 00:00:00 UTC 之后，并且响应头部的Last-Modified字段未设置
			if (lastModified.isAfter(Instant.EPOCH) && getResponseHeaders().getLastModified() == -1) {
				// 将响应头部的Last-Modified字段设置为 最后更新的时间的毫秒数
				getResponseHeaders().setLastModified(lastModified.toEpochMilli());
			}
			// 如果ETag不为空，并且响应头部的ETag字段未设置
			if (StringUtils.hasLength(etag) && getResponseHeaders().getETag() == null) {
				// 根据需要对ETag进行填充，并设置到响应头部的ETag字段中
				getResponseHeaders().setETag(padEtagIfNecessary(etag));
			}
		}

		return this.notModified;
	}

	private boolean validateIfUnmodifiedSince(Instant lastModified) {
		// 如果 最后更新的时间 在 1970-01-01 00:00:00 UTC 之前，则验证失败
		if (lastModified.isBefore(Instant.EPOCH)) {
			return false;
		}
		// 获取请求头部的If-Unmodified-Since字段
		long ifUnmodifiedSince = getRequestHeaders().getIfUnmodifiedSince();
		// 如果If-Unmodified-Since字段未设置，则无法执行此验证
		if (ifUnmodifiedSince == -1) {
			return false;
		}
		// 我们将执行此验证...
		// 将If-Unmodified-Since字段转换为Instant
		Instant sinceInstant = Instant.ofEpochMilli(ifUnmodifiedSince);
		// 根据秒精度截断最后更新的时间，并检查是否在sinceInstant之前
		this.notModified = sinceInstant.isBefore(lastModified.truncatedTo(ChronoUnit.SECONDS));
		return true;
	}

	private boolean validateIfNoneMatch(@Nullable String etag) {
		// 如果ETag为空，则验证失败
		if (!StringUtils.hasLength(etag)) {
			return false;
		}
		List<String> ifNoneMatch;
		try {
			// 获取请求头部的If-None-Match字段
			ifNoneMatch = getRequestHeaders().getIfNoneMatch();
		} catch (IllegalArgumentException ex) {
			return false;
		}
		// 如果If-None-Match字段为空，则无法执行此验证
		if (ifNoneMatch.isEmpty()) {
			return false;
		}
		// 我们将执行此验证...
		// 如果ETag以"W/"开头，则移除"W/"
		etag = padEtagIfNecessary(etag);
		if (etag.startsWith("W/")) {
			etag = etag.substring(2);
		}
		// 遍历请求头部的ETags，比较与当前ETag是否匹配
		for (String clientEtag : ifNoneMatch) {
			// 根据RFC 7232规范，比较弱ETag和强ETag
			if (StringUtils.hasLength(clientEtag)) {
				if (clientEtag.startsWith("W/")) {
					clientEtag = clientEtag.substring(2);
				}
				// 如果客户端ETag与当前ETag匹配，则设置为未修改状态
				if (clientEtag.equals(etag)) {
					this.notModified = true;
					break;
				}
			}
		}
		return true;
	}

	private String padEtagIfNecessary(String etag) {
		// 如果ETag为空，则返回ETag
		if (!StringUtils.hasLength(etag)) {
			return etag;
		}
		// 如果ETag以双引号或"W/"开头，并且以双引号结尾，则返回ETag
		if ((etag.startsWith("\"") || etag.startsWith("W/\"")) && etag.endsWith("\"")) {
			return etag;
		}
		// 否则，将ETag用双引号包裹后返回
		return "\"" + etag + "\"";
	}

	private boolean validateIfModifiedSince(Instant lastModified) {
		// 如果lastModified早于Epoch时间，则返回false
		if (lastModified.isBefore(Instant.EPOCH)) {
			return false;
		}
		// 获取If-Modified-Since头部的时间戳
		long ifModifiedSince = getRequestHeaders().getIfModifiedSince();
		// 如果没有提供If-Modified-Since头部，则返回false
		if (ifModifiedSince == -1) {
			return false;
		}
		// 执行此验证...
		// 如果lastModified时间戳晚于或等于If-Modified-Since头部提供的时间戳，则设置为未修改，否则为修改
		this.notModified = ChronoUnit.SECONDS.between(lastModified, Instant.ofEpochMilli(ifModifiedSince)) >= 0;
		return true;
	}

	@Override
	public String transformUrl(String url) {
		return this.urlTransformer.apply(url);
	}

	@Override
	public void addUrlTransformer(Function<String, String> transformer) {
		Assert.notNull(transformer, "'encoder' must not be null");
		this.urlTransformer = this.urlTransformer.andThen(transformer);
	}

	@Override
	public String getLogPrefix() {
		// 获取名为LOG_ID_ATTRIBUTE的属性值
		Object value = getAttribute(LOG_ID_ATTRIBUTE);
		// 如果logId与值不同，则更新logId和logPrefix
		if (this.logId != value) {
			this.logId = value;
			// 如果值不为空，则设置logPrefix为"[value] "，否则为空字符串
			this.logPrefix = value != null ? "[" + value + "] " : "";
		}
		// 返回logPrefix
		return this.logPrefix;
	}

}
