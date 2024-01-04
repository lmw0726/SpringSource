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

package org.springframework.web.reactive.function.server;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.Conventions;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * 默认的RenderingResponse.Builder实现。
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 5.0
 */
final class DefaultRenderingResponseBuilder implements RenderingResponse.Builder {

	/**
	 * 模板名称
	 */
	private final String name;

	/**
	 * 状态码，默认为OK（200）
	 */
	private int status = HttpStatus.OK.value();

	/**
	 * HTTP响应头
	 */
	private final HttpHeaders headers = new HttpHeaders();

	/**
	 * 响应的Cookie列表
	 */
	private final MultiValueMap<String, ResponseCookie> cookies = new LinkedMultiValueMap<>();

	/**
	 * 模型数据
	 */
	private final Map<String, Object> model = new LinkedHashMap<>();


	public DefaultRenderingResponseBuilder(RenderingResponse other) {
		Assert.notNull(other, "RenderingResponse must not be null");
		this.name = other.name();
		this.status = (other instanceof DefaultRenderingResponse ?
				((DefaultRenderingResponse) other).statusCode : other.statusCode().value());
		this.headers.putAll(other.headers());
		this.model.putAll(other.model());
	}

	public DefaultRenderingResponseBuilder(String name) {
		Assert.notNull(name, "Name must not be null");
		this.name = name;
	}


	@Override
	public RenderingResponse.Builder status(HttpStatus status) {
		Assert.notNull(status, "HttpStatus must not be null");
		this.status = status.value();
		return this;
	}

	@Override
	public RenderingResponse.Builder status(int status) {
		this.status = status;
		return this;
	}

	@Override
	public RenderingResponse.Builder cookie(ResponseCookie cookie) {
		Assert.notNull(cookie, "ResponseCookie must not be null");
		this.cookies.add(cookie.getName(), cookie);
		return this;
	}

	@Override
	public RenderingResponse.Builder cookies(Consumer<MultiValueMap<String, ResponseCookie>> cookiesConsumer) {
		cookiesConsumer.accept(this.cookies);
		return this;
	}

	/**
	 * 向模型添加属性。
	 *
	 * @param attribute 要添加的属性
	 * @return RenderingResponse.Builder对象，用于链式调用
	 */
	@Override
	public RenderingResponse.Builder modelAttribute(Object attribute) {
		// 断言属性不为空，如果为空则抛出异常
		Assert.notNull(attribute, "Attribute must not be null");

		// 如果属性是集合类型且为空，则直接返回当前对象
		if (attribute instanceof Collection && ((Collection<?>) attribute).isEmpty()) {
			return this;
		}

		// 根据属性生成一个模型属性，并返回生成后的模型
		return modelAttribute(Conventions.getVariableName(attribute), attribute);
	}

	@Override
	public RenderingResponse.Builder modelAttribute(String name, @Nullable Object value) {
		Assert.notNull(name, "Name must not be null");
		this.model.put(name, value);
		return this;
	}

	@Override
	public RenderingResponse.Builder modelAttributes(Object... attributes) {
		modelAttributes(Arrays.asList(attributes));
		return this;
	}

	@Override
	public RenderingResponse.Builder modelAttributes(Collection<?> attributes) {
		attributes.forEach(this::modelAttribute);
		return this;
	}

	@Override
	public RenderingResponse.Builder modelAttributes(Map<String, ?> attributes) {
		this.model.putAll(attributes);
		return this;
	}

	@Override
	public RenderingResponse.Builder header(String headerName, String... headerValues) {
		for (String headerValue : headerValues) {
			this.headers.add(headerName, headerValue);
		}
		return this;
	}

	@Override
	public RenderingResponse.Builder headers(HttpHeaders headers) {
		this.headers.putAll(headers);
		return this;
	}

	@Override
	public Mono<RenderingResponse> build() {
		return Mono.just(
				new DefaultRenderingResponse(this.status, this.headers, this.cookies, this.name, this.model));
	}


	/**
	 * 默认的RenderingResponse实现类，继承自AbstractServerResponse。
	 */
	private static final class DefaultRenderingResponse extends DefaultServerResponseBuilder.AbstractServerResponse
			implements RenderingResponse {

		/**
		 * 模板名称
		 */
		private final String name;

		/**
		 * 模型数据
		 */
		private final Map<String, Object> model;

		public DefaultRenderingResponse(int statusCode, HttpHeaders headers,
										MultiValueMap<String, ResponseCookie> cookies, String name, Map<String, Object> model) {

			super(statusCode, headers, cookies, Collections.emptyMap());
			this.name = name;
			this.model = Collections.unmodifiableMap(new LinkedHashMap<>(model));
		}

		/**
		 * 获取模板名称。
		 *
		 * @return 模板名称
		 */
		@Override
		public String name() {
			return this.name;
		}

		/**
		 * 获取不可修改的模型映射。
		 *
		 * @return 模型映射
		 */
		@Override
		public Map<String, Object> model() {
			return this.model;
		}

		/**
		 * 内部方法，用于执行写入操作。
		 *
		 * @param exchange 服务器Web交换对象
		 * @param context  上下文对象
		 * @return Mono<Void>对象，表示写入操作完成的信号
		 */
		@Override
		protected Mono<Void> writeToInternal(ServerWebExchange exchange, Context context) {
			// 获取响应的内容类型
			MediaType contentType = exchange.getResponse().getHeaders().getContentType();

			// 获取当前的 Locale
			Locale locale = LocaleContextHolder.getLocale(exchange.getLocaleContext());

			// 创建视图解析器流
			Stream<ViewResolver> viewResolverStream = context.viewResolvers().stream();

			// 从视图解析器中解析视图名称并进行渲染
			return Flux.fromStream(viewResolverStream)
					// 解析视图名称
					.concatMap(viewResolver -> viewResolver.resolveViewName(name(), locale))
					.next() // 取第一个解析成功的视图
					.switchIfEmpty(Mono.error(() ->
							// 如果未找到视图，则抛出异常
							new IllegalArgumentException("Could not resolve view with name '" + name() + "'")))
					.flatMap(view -> {
						// 获取视图支持的媒体类型
						List<MediaType> mediaTypes = view.getSupportedMediaTypes();
						// 渲染视图，如果响应的内容类型为空且视图支持的媒体类型不为空，则使用第一个支持的媒体类型
						return view.render(
								model(),
								contentType == null && !mediaTypes.isEmpty() ? mediaTypes.get(0) : contentType,
								exchange
						);
					});
		}

	}

}
