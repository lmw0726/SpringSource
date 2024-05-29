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

package org.springframework.web.server;

import org.springframework.context.ApplicationContext;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.http.codec.multipart.Part;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.time.Instant;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 定义 HTTP 请求-响应交互的契约。提供对 HTTP 请求和响应的访问，
 * 并且还暴露了与服务器端处理相关的属性和功能，例如请求属性。
 * <p>
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface ServerWebExchange {

	/**
	 * 用于关联此交换的日志消息的 {@link #getAttributes() 属性} 的名称。
	 * 使用 {@link #getLogPrefix()} 获取基于此属性的一致格式的前缀。
	 * <p>
	 *
	 * @see #getLogPrefix()
	 * @since 5.1 起
	 */
	String LOG_ID_ATTRIBUTE = ServerWebExchange.class.getName() + ".LOG_ID";


	/**
	 * 返回当前的 HTTP 请求。
	 */
	ServerHttpRequest getRequest();

	/**
	 * 返回当前的 HTTP 响应。
	 */
	ServerHttpResponse getResponse();

	/**
	 * 返回当前交换的请求属性的可变映射。
	 */
	Map<String, Object> getAttributes();

	/**
	 * 返回请求属性值（如果存在）。
	 *
	 * @param name 属性名称
	 * @param <T>  属性类型
	 * @return 属性值
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	default <T> T getAttribute(String name) {
		return (T) getAttributes().get(name);
	}

	/**
	 * 返回请求属性值，如果不存在则抛出 {@link IllegalArgumentException}。
	 *
	 * @param name 属性名称
	 * @param <T>  属性类型
	 * @return 属性值
	 */
	@SuppressWarnings("unchecked")
	default <T> T getRequiredAttribute(String name) {
		T value = getAttribute(name);
		Assert.notNull(value, () -> "Required attribute '" + name + "' is missing");
		return value;
	}

	/**
	 * 返回请求属性值，或一个默认值。
	 *
	 * @param name         属性名称
	 * @param defaultValue 默认值
	 * @param <T>          属性类型
	 * @return 属性值
	 */
	@SuppressWarnings("unchecked")
	default <T> T getAttributeOrDefault(String name, T defaultValue) {
		return (T) getAttributes().getOrDefault(name, defaultValue);
	}

	/**
	 * 返回当前请求的 Web 会话。始终保证返回一个实例，该实例要么与客户端请求的会话 ID 匹配，
	 * 要么具有一个新的会话 ID，这是因为客户端未指定会话 ID 或基础会话已过期。
	 * 使用此方法不会自动创建会话。有关详细信息，请参阅 {@link WebSession}。
	 */
	Mono<WebSession> getSession();

	/**
	 * 返回请求的认证用户（如果有）。
	 */
	<T extends Principal> Mono<T> getPrincipal();

	/**
	 * 如果 Content-Type 是 {@code "application/x-www-form-urlencoded"}，则从请求体中返回表单数据，
	 * 否则返回一个空的映射。
	 * <p><strong>注意：</strong>调用此方法会导致请求体被完整读取和解析，并且生成的 {@code MultiValueMap}
	 * 被缓存，因此此方法可以安全地多次调用。
	 */
	Mono<MultiValueMap<String, String>> getFormData();

	/**
	 * 如果 Content-Type 是 {@code "multipart/form-data"}，则返回多部分请求的各部分，
	 * 否则返回一个空的映射。
	 * <p><strong>注意：</strong>调用此方法会导致请求体被完整读取和解析，并且生成的 {@code MultiValueMap}
	 * 被缓存，因此此方法可以安全地多次调用。
	 * <p><strong>注意：</strong>每个部分的 {@linkplain Part#content() 内容} 不会被缓存，并且只能读取一次。
	 */
	Mono<MultiValueMap<String, Part>> getMultipartData();

	/**
	 * 使用配置的 {@link org.springframework.web.server.i18n.LocaleContextResolver} 返回 {@link LocaleContext}。
	 */
	LocaleContext getLocaleContext();

	/**
	 * 返回与 Web 应用程序关联的 {@link ApplicationContext}，如果它是通过
	 * {@link org.springframework.web.server.adapter.WebHttpHandlerBuilder#applicationContext(ApplicationContext)}
	 * 初始化的。
	 *
	 * @see org.springframework.web.server.adapter.WebHttpHandlerBuilder#applicationContext(ApplicationContext)
	 * @since 5.0.3
	 */
	@Nullable
	ApplicationContext getApplicationContext();

	/**
	 * 如果在此契约中的一个 {@code checkNotModified} 方法被使用并且它们返回 true，则返回 {@code true}。
	 */
	boolean isNotModified();

	/**
	 * 仅使用上次修改时间戳的 {@link #checkNotModified(String, Instant)} 的重载变体。
	 *
	 * @param lastModified 上次修改的时间
	 * @return 请求是否符合未修改的条件
	 */
	boolean checkNotModified(Instant lastModified);

	/**
	 * 仅使用 {@code ETag}（实体标签）值的 {@link #checkNotModified(String, Instant)} 的重载变体。
	 *
	 * @param etag 底层资源的实体标签。
	 * @return 如果请求不需要进一步处理，则返回 true。
	 */
	boolean checkNotModified(String etag);

	/**
	 * 根据应用程序确定的给定 {@code ETag}（实体标签）和上次修改时间戳检查请求的资源是否已被修改。
	 * 同时透明地准备响应，设置 HTTP 状态，并在适用时添加 "ETag" 和 "Last-Modified" 头。
	 * 此方法适用于条件 GET/HEAD 请求以及条件 POST/PUT/DELETE 请求。
	 * <p><strong>注意：</strong>HTTP 规范建议同时设置 ETag 和 Last-Modified 值，但您也可以使用
	 * {@code #checkNotModified(String)} 或 {@link #checkNotModified(Instant)}。
	 *
	 * @param etag         应用程序为底层资源确定的实体标签。此参数将被加上引号（"）以确保必要时。
	 * @param lastModified 应用程序为底层资源确定的上次修改时间戳
	 * @return 如果请求不需要进一步处理，则返回 true。
	 */
	boolean checkNotModified(@Nullable String etag, Instant lastModified);

	/**
	 * 根据已注册的转换函数转换给定的 URL。默认情况下，此方法返回给定的 {@code url}，
	 * 但可以通过 {@link #addUrlTransformer} 注册额外的转换函数。
	 *
	 * @param url 要转换的 URL
	 * @return 转换后的 URL
	 */
	String transformUrl(String url);

	/**
	 * 注册一个额外的 URL 转换函数以与 {@link #transformUrl} 一起使用。
	 * 给定的函数可以用于插入身份验证 ID、CSRF 保护的随机数等。
	 * <p>请注意，给定的函数在任何先前注册的函数之后应用。
	 *
	 * @param transformer 要添加的 URL 转换函数
	 */
	void addUrlTransformer(Function<String, String> transformer);

	/**
	 * 返回一个日志消息前缀，用于关联此交换的消息。前缀基于属性 {@link #LOG_ID_ATTRIBUTE} 的值，
	 * 并附加一些额外的格式，使得前缀可以方便地被前置，而无需进一步格式化或分隔符。
	 *
	 * @return 日志消息前缀，如果 {@link #LOG_ID_ATTRIBUTE} 未设置，则返回空字符串。
	 * @since 5.1
	 */
	String getLogPrefix();

	/**
	 * 返回一个构建器，用于通过 {@link ServerWebExchangeDecorator} 包装此交换来更改其属性，
	 * 并返回修改后的值或委托回此实例。
	 */
	default Builder mutate() {
		return new DefaultServerWebExchangeBuilder(this);
	}


	/**
	 * 用于修改现有 {@link ServerWebExchange} 的构建器。
	 * 不再需要使用复杂的构建逻辑。
	 */
	interface Builder {

		/**
		 * 配置一个消费者来使用构建器修改当前请求。
		 * <p>实际上是这样：
		 * <pre>
		 * exchange.mutate().request(builder -&gt; builder.method(HttpMethod.PUT));
		 *
		 * // 对比...
		 *
		 * ServerHttpRequest request = exchange.getRequest().mutate()
		 *     .method(HttpMethod.PUT)
		 *     .build();
		 *
		 * exchange.mutate().request(request);
		 * </pre>
		 *
		 * @see ServerHttpRequest#mutate()
		 */
		Builder request(Consumer<ServerHttpRequest.Builder> requestBuilderConsumer);

		/**
		 * 设置要使用的请求，特别是在需要重写 {@link ServerHttpRequest} 方法时。
		 * 如果只是简单地修改请求属性，请参阅 {@link #request(Consumer)}。
		 *
		 * @see org.springframework.http.server.reactive.ServerHttpRequestDecorator
		 */
		Builder request(ServerHttpRequest request);

		/**
		 * 设置要使用的响应。
		 *
		 * @see org.springframework.http.server.reactive.ServerHttpResponseDecorator
		 */
		Builder response(ServerHttpResponse response);

		/**
		 * 设置此交换返回的 {@code Mono<Principal>}。
		 */
		Builder principal(Mono<Principal> principalMono);

		/**
		 * 构建一个具有已修改属性的 {@link ServerWebExchange} 装饰器。
		 */
		ServerWebExchange build();
	}

}
