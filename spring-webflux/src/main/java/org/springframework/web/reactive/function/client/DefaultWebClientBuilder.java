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

package org.springframework.web.reactive.function.client;

import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.HttpComponentsClientHttpConnector;
import org.springframework.http.client.reactive.JettyClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.ClientCodecConfigurer;
import org.springframework.lang.Nullable;
import org.springframework.util.*;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilderFactory;

import java.util.*;
import java.util.function.Consumer;

/**
 * WebClient.Builder 的默认实现类。
 * <p>
 * 该类提供了构建 WebClient 实例的各种配置选项。
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 5.0
 */
final class DefaultWebClientBuilder implements WebClient.Builder {
	/**
	 * 标识是否存在 Reactor 客户端
	 */
	private static final boolean reactorClientPresent;

	/**
	 * 标识是否存在 Jetty 客户端
	 */
	private static final boolean jettyClientPresent;

	/**
	 * 标识是否存在 Apache HttpComponents 客户端
	 */
	private static final boolean httpComponentsClientPresent;

	static {
		ClassLoader loader = DefaultWebClientBuilder.class.getClassLoader();
		reactorClientPresent = ClassUtils.isPresent("reactor.netty.http.client.HttpClient", loader);
		jettyClientPresent = ClassUtils.isPresent("org.eclipse.jetty.client.HttpClient", loader);
		httpComponentsClientPresent =
				ClassUtils.isPresent("org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient", loader) &&
						ClassUtils.isPresent("org.apache.hc.core5.reactive.ReactiveDataConsumer", loader);
	}

	/**
	 * 基础 URL
	 */
	@Nullable
	private String baseUrl;

	/**
	 * 默认 URI 变量
	 */
	@Nullable
	private Map<String, ?> defaultUriVariables;

	/**
	 * URI 构建工厂
	 */
	@Nullable
	private UriBuilderFactory uriBuilderFactory;

	/**
	 * 默认头部
	 */
	@Nullable
	private HttpHeaders defaultHeaders;

	/**
	 * 默认 Cookie
	 */
	@Nullable
	private MultiValueMap<String, String> defaultCookies;

	/**
	 * 默认请求配置
	 */
	@Nullable
	private Consumer<WebClient.RequestHeadersSpec<?>> defaultRequest;

	/**
	 * 过滤器列表
	 */
	@Nullable
	private List<ExchangeFilterFunction> filters;

	/**
	 * 客户端 HTTP 连接器
	 */
	@Nullable
	private ClientHttpConnector connector;

	/**
	 * 交换策略
	 */
	@Nullable
	private ExchangeStrategies strategies;

	/**
	 * 交换策略配置器列表
	 */
	@Nullable
	private List<Consumer<ExchangeStrategies.Builder>> strategiesConfigurers;

	/**
	 * 交换函数
	 */
	@Nullable
	private ExchangeFunction exchangeFunction;


	public DefaultWebClientBuilder() {
	}

	/**
	 * 使用另一个 DefaultWebClientBuilder 对象的配置创建新的 DefaultWebClientBuilder 实例。
	 *
	 * @param other 要复制配置的 DefaultWebClientBuilder 对象
	 */
	public DefaultWebClientBuilder(DefaultWebClientBuilder other) {
		Assert.notNull(other, "DefaultWebClientBuilder must not be null");

		this.baseUrl = other.baseUrl;
		this.defaultUriVariables = (other.defaultUriVariables != null ?
				new LinkedHashMap<>(other.defaultUriVariables) : null);
		this.uriBuilderFactory = other.uriBuilderFactory;

		if (other.defaultHeaders != null) {
			this.defaultHeaders = new HttpHeaders();
			this.defaultHeaders.putAll(other.defaultHeaders);
		} else {
			this.defaultHeaders = null;
		}

		this.defaultCookies = (other.defaultCookies != null ?
				new LinkedMultiValueMap<>(other.defaultCookies) : null);
		this.defaultRequest = other.defaultRequest;
		this.filters = (other.filters != null ? new ArrayList<>(other.filters) : null);

		this.connector = other.connector;
		this.strategies = other.strategies;
		this.strategiesConfigurers = (other.strategiesConfigurers != null ?
				new ArrayList<>(other.strategiesConfigurers) : null);
		this.exchangeFunction = other.exchangeFunction;
	}


	@Override
	public WebClient.Builder baseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
		return this;
	}

	@Override
	public WebClient.Builder defaultUriVariables(Map<String, ?> defaultUriVariables) {
		this.defaultUriVariables = defaultUriVariables;
		return this;
	}

	@Override
	public WebClient.Builder uriBuilderFactory(UriBuilderFactory uriBuilderFactory) {
		this.uriBuilderFactory = uriBuilderFactory;
		return this;
	}

	@Override
	public WebClient.Builder defaultHeader(String header, String... values) {
		initHeaders().put(header, Arrays.asList(values));
		return this;
	}

	@Override
	public WebClient.Builder defaultHeaders(Consumer<HttpHeaders> headersConsumer) {
		headersConsumer.accept(initHeaders());
		return this;
	}

	private HttpHeaders initHeaders() {
		if (this.defaultHeaders == null) {
			this.defaultHeaders = new HttpHeaders();
		}
		return this.defaultHeaders;
	}

	@Override
	public WebClient.Builder defaultCookie(String cookie, String... values) {
		initCookies().addAll(cookie, Arrays.asList(values));
		return this;
	}

	@Override
	public WebClient.Builder defaultCookies(Consumer<MultiValueMap<String, String>> cookiesConsumer) {
		cookiesConsumer.accept(initCookies());
		return this;
	}

	private MultiValueMap<String, String> initCookies() {
		if (this.defaultCookies == null) {
			this.defaultCookies = new LinkedMultiValueMap<>(3);
		}
		return this.defaultCookies;
	}

	@Override
	public WebClient.Builder defaultRequest(Consumer<WebClient.RequestHeadersSpec<?>> defaultRequest) {
		this.defaultRequest = this.defaultRequest != null ?
				this.defaultRequest.andThen(defaultRequest) : defaultRequest;
		return this;
	}

	@Override
	public WebClient.Builder filter(ExchangeFilterFunction filter) {
		Assert.notNull(filter, "ExchangeFilterFunction must not be null");
		initFilters().add(filter);
		return this;
	}

	@Override
	public WebClient.Builder filters(Consumer<List<ExchangeFilterFunction>> filtersConsumer) {
		filtersConsumer.accept(initFilters());
		return this;
	}

	private List<ExchangeFilterFunction> initFilters() {
		if (this.filters == null) {
			this.filters = new ArrayList<>();
		}
		return this.filters;
	}

	@Override
	public WebClient.Builder clientConnector(ClientHttpConnector connector) {
		this.connector = connector;
		return this;
	}

	/**
	 * 配置客户端编解码器。
	 *
	 * @param configurer 客户端编解码器配置器
	 * @return 返回更新后的 WebClient.Builder 实例
	 */
	@Override
	public WebClient.Builder codecs(Consumer<ClientCodecConfigurer> configurer) {
		// 检查是否存在策略配置器列表，如果为空则初始化一个新的列表
		if (this.strategiesConfigurers == null) {
			this.strategiesConfigurers = new ArrayList<>(4);
		}

		// 向策略配置器列表中添加一个配置器，用于设置编解码器
		this.strategiesConfigurers.add(builder -> builder.codecs(configurer));
		return this;
	}

	@Override
	public WebClient.Builder exchangeStrategies(ExchangeStrategies strategies) {
		this.strategies = strategies;
		return this;
	}

	@Override
	@Deprecated
	public WebClient.Builder exchangeStrategies(Consumer<ExchangeStrategies.Builder> configurer) {
		if (this.strategiesConfigurers == null) {
			this.strategiesConfigurers = new ArrayList<>(4);
		}
		this.strategiesConfigurers.add(configurer);
		return this;
	}

	@Override
	public WebClient.Builder exchangeFunction(ExchangeFunction exchangeFunction) {
		this.exchangeFunction = exchangeFunction;
		return this;
	}

	@Override
	public WebClient.Builder apply(Consumer<WebClient.Builder> builderConsumer) {
		builderConsumer.accept(this);
		return this;
	}

	@Override
	public WebClient.Builder clone() {
		return new DefaultWebClientBuilder(this);
	}

	/**
	 * 构建 WebClient 实例。
	 *
	 * @return 返回构建的 WebClient 实例
	 */
	@Override
	public WebClient build() {
		// 确定要使用的连接器
		ClientHttpConnector connectorToUse = (this.connector != null ? this.connector : initConnector());

		// 初始化 ExchangeFunction
		ExchangeFunction exchange = (this.exchangeFunction == null ?
				ExchangeFunctions.create(connectorToUse, initExchangeStrategies()) :
				this.exchangeFunction);

		// 应用过滤器到 ExchangeFunction
		ExchangeFunction filteredExchange = (this.filters != null ? this.filters.stream()
				.reduce(ExchangeFilterFunction::andThen)
				.map(filter -> filter.apply(exchange))
				.orElse(exchange) : exchange);

		// 复制默认的 HttpHeaders 和 Cookies
		HttpHeaders defaultHeaders = copyDefaultHeaders();
		MultiValueMap<String, String> defaultCookies = copyDefaultCookies();

		// 创建并返回新的 DefaultWebClient 实例
		return new DefaultWebClient(filteredExchange, initUriBuilderFactory(),
				defaultHeaders,
				defaultCookies,
				this.defaultRequest, new DefaultWebClientBuilder(this));
	}

	/**
	 * 初始化用于 WebClient 的默认 ClientHttpConnector。
	 * 根据可用的库初始化对应的 ClientHttpConnector。
	 *
	 * @return 返回初始化的 ClientHttpConnector
	 * @throws IllegalStateException 如果没有找到合适的默认 ClientHttpConnector
	 */
	private ClientHttpConnector initConnector() {
		if (reactorClientPresent) {
			return new ReactorClientHttpConnector();
		} else if (jettyClientPresent) {
			return new JettyClientHttpConnector();
		} else if (httpComponentsClientPresent) {
			return new HttpComponentsClientHttpConnector();
		}
		throw new IllegalStateException("No suitable default ClientHttpConnector found");
	}

	/**
	 * 初始化 ExchangeStrategies 实例。
	 * 如果配置了策略配置器，则应用这些配置器来构建 ExchangeStrategies 实例；否则返回默认的 ExchangeStrategies。
	 *
	 * @return 返回初始化的 ExchangeStrategies 实例
	 */
	private ExchangeStrategies initExchangeStrategies() {
		// 检查策略配置器列表是否为空
		if (CollectionUtils.isEmpty(this.strategiesConfigurers)) {
			// 如果列表为空，返回已有的策略对象（如果存在），否则返回默认策略对象
			return (this.strategies != null ? this.strategies : ExchangeStrategies.withDefaults());
		}

		// 创建 ExchangeStrategies.Builder 对象
		ExchangeStrategies.Builder builder = (this.strategies != null ? this.strategies.mutate() : ExchangeStrategies.builder());

		// 使用每个配置器对策略进行定制
		this.strategiesConfigurers.forEach(configurer -> configurer.accept(builder));

		// 构建最终的 ExchangeStrategies 对象
		return builder.build();
	}

	/**
	 * 初始化 UriBuilderFactory 实例。
	 * 如果已配置了 UriBuilderFactory，则返回该实例；
	 * 否则根据 baseUrl 初始化 DefaultUriBuilderFactory，并设置默认的 Uri 变量。
	 *
	 * @return 返回初始化的 UriBuilderFactory 实例
	 */
	private UriBuilderFactory initUriBuilderFactory() {
		// 检查是否已经存在 URI 构建工厂对象
		if (this.uriBuilderFactory != null) {
			// 如果已存在，则直接返回
			return this.uriBuilderFactory;
		}

		// 如果不存在，根据基础 URL 创建默认的 URI 构建工厂对象
		DefaultUriBuilderFactory factory = (this.baseUrl != null ?
				new DefaultUriBuilderFactory(this.baseUrl) : new DefaultUriBuilderFactory());

		// 设置默认 URI 变量到工厂对象
		factory.setDefaultUriVariables(this.defaultUriVariables);

		// 返回创建的 URI 构建工厂对象
		return factory;
	}

	/**
	 * 复制默认的 HttpHeaders。
	 * 如果存在默认的 HttpHeaders，则创建一个副本，并返回只读的 HttpHeaders 实例；否则返回 null。
	 *
	 * @return 返回复制的只读 HttpHeaders 实例，如果没有默认 HttpHeaders 则返回 null
	 */
	@Nullable
	private HttpHeaders copyDefaultHeaders() {
		// 检查是否已经设置了默认的 HTTP 头部信息
		if (this.defaultHeaders != null) {
			// 创建一个新的 HttpHeaders 对象，将原始 defaultHeaders 的内容复制到新对象中
			HttpHeaders copy = new HttpHeaders();
			this.defaultHeaders.forEach((key, values) -> copy.put(key, new ArrayList<>(values)));

			// 返回只读的 HttpHeaders 对象副本
			return HttpHeaders.readOnlyHttpHeaders(copy);
		} else {
			// 如果默认头部信息未设置，则返回 null
			return null;
		}
	}

	/**
	 * 复制默认的 Cookies。
	 * 如果存在默认的 Cookies，则创建一个副本，并返回不可修改的 MultiValueMap 实例；否则返回 null。
	 *
	 * @return 返回复制的不可修改的 MultiValueMap 实例，如果没有默认 Cookies 则返回 null
	 */
	@Nullable
	private MultiValueMap<String, String> copyDefaultCookies() {
		// 检查是否已经设置了默认的 Cookie 信息
		if (this.defaultCookies != null) {
			// 创建一个新的 MultiValueMap 对象，将原始 defaultCookies 的内容复制到新对象中
			MultiValueMap<String, String> copy = new LinkedMultiValueMap<>(this.defaultCookies.size());
			this.defaultCookies.forEach((key, values) -> copy.put(key, new ArrayList<>(values)));

			// 返回不可修改的 MultiValueMap 对象副本
			return CollectionUtils.unmodifiableMultiValueMap(copy);
		} else {
			// 如果默认 Cookie 信息未设置，则返回 null
			return null;
		}
	}

}
