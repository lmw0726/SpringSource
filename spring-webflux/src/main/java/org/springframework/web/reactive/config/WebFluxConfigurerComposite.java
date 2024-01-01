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

package org.springframework.web.reactive.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.format.Formatter;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.MessageCodesResolver;
import org.springframework.validation.Validator;
import org.springframework.web.reactive.accept.RequestedContentTypeResolverBuilder;
import org.springframework.web.reactive.result.method.annotation.ArgumentResolverConfigurer;
import org.springframework.web.reactive.socket.server.WebSocketService;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 委托给一个或多个其他人的 {@link WebFluxConfigurer}。
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class WebFluxConfigurerComposite implements WebFluxConfigurer {
	/**
	 * WebFlux自定义配置列表
	 */
	private final List<WebFluxConfigurer> delegates = new ArrayList<>();


	public void addWebFluxConfigurers(List<WebFluxConfigurer> configurers) {
		if (!CollectionUtils.isEmpty(configurers)) {
			this.delegates.addAll(configurers);
		}
	}


	/**
	 * 配置内容类型解析器，将配置委托给所有的代理对象。
	 *
	 * @param builder 用于配置内容类型解析器的构建器
	 */
	@Override
	public void configureContentTypeResolver(RequestedContentTypeResolverBuilder builder) {
		this.delegates.forEach(delegate -> delegate.configureContentTypeResolver(builder));
	}

	/**
	 * 添加跨域映射，将配置委托给所有的代理对象。
	 *
	 * @param registry 跨域注册表
	 */
	@Override
	public void addCorsMappings(CorsRegistry registry) {
		this.delegates.forEach(delegate -> delegate.addCorsMappings(registry));
	}

	/**
	 * 配置路径匹配选项，将配置委托给所有的代理对象。
	 *
	 * @param configurer 路径匹配配置器
	 */
	@Override
	public void configurePathMatching(PathMatchConfigurer configurer) {
		this.delegates.forEach(delegate -> delegate.configurePathMatching(configurer));
	}

	/**
	 * 添加资源处理程序，委托给所有代理对象处理。
	 *
	 * @param registry 资源处理程序注册表
	 */
	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		this.delegates.forEach(delegate -> delegate.addResourceHandlers(registry));
	}

	/**
	 * 获取 WebSocketService 实例，如果有多个 WebFluxConfigurer 实现了 WebSocketService 工厂方法，
	 * 则抛出 IllegalStateException 异常。
	 *
	 * @return WebSocketService 实例
	 */
	@Nullable
	@Override
	public WebSocketService getWebSocketService() {
		return createSingleBean(WebFluxConfigurer::getWebSocketService, WebSocketService.class);
	}

	/**
	 * 配置自定义 @RequestMapping 方法参数的解析器。
	 *
	 * @param configurer 用于配置解析器的 configurer
	 */
	@Override
	public void configureArgumentResolvers(ArgumentResolverConfigurer configurer) {
		this.delegates.forEach(delegate -> delegate.configureArgumentResolvers(configurer));
	}

	/**
	 * 配置用于从请求体读取和向响应体写入的 HTTP 消息读取器和写入器，适用于处理注解控制器和功能性端点。
	 * <p>默认情况下，只要类路径上存在对应的第三方库，就会配置所有内置的读取器和写入器，比如 Jackson JSON、JAXB2 等。
	 *
	 * @param configurer 用于自定义读取器和写入器的 configurer
	 */
	@Override
	public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
		this.delegates.forEach(delegate -> delegate.configureHttpMessageCodecs(configurer));
	}

	/**
	 * 添加自定义的 {@link Converter 转换器} 和 {@link Formatter 格式化器}，
	 * 用于执行注解控制器方法参数的类型转换和格式化。
	 *
	 * @param registry 格式化器注册表
	 */
	@Override
	public void addFormatters(FormatterRegistry registry) {
		this.delegates.forEach(delegate -> delegate.addFormatters(registry));
	}

	/**
	 * 获取 {@link Validator 验证器} 实例，用于验证注解控制器方法参数。
	 *
	 * @return 验证器实例；若存在多个 {@code WebFluxConfigurer} 实现了验证器工厂方法，则抛出异常
	 */
	@Override
	public Validator getValidator() {
		return createSingleBean(WebFluxConfigurer::getValidator, Validator.class);
	}

	/**
	 * 获取 {@link MessageCodesResolver 消息码解析器} 实例，用于数据绑定在注解控制器方法参数中的使用。
	 *
	 * @return 消息码解析器实例；若存在多个 {@code WebFluxConfigurer} 实现了消息码解析器工厂方法，则抛出异常
	 */
	@Override
	public MessageCodesResolver getMessageCodesResolver() {
		return createSingleBean(WebFluxConfigurer::getMessageCodesResolver, MessageCodesResolver.class);
	}

	/**
	 * 配置视图解析器，用于渲染带有视图和模型的响应，其中视图通常是 HTML 模板，
	 * 但也可以基于 HTTP 消息编写器（例如 JSON、XML）。
	 * <p>配置的视图解析器将用于注解控制器和功能性端点。
	 *
	 * @param registry 视图解析器注册表
	 */
	@Override
	public void configureViewResolvers(ViewResolverRegistry registry) {
		this.delegates.forEach(delegate -> delegate.configureViewResolvers(registry));
	}

	/**
	 * 创建单个 bean 的辅助方法，根据提供的工厂函数和 bean 类型创建单个 bean。
	 *
	 * @param factory  用于创建 bean 的函数
	 * @param beanType bean 的类型
	 * @param <T>      bean 的类型
	 * @return 如果有一个 bean 被创建，则返回该 bean；如果没有被创建，返回 null；如果存在多个 bean 被创建，则抛出 IllegalStateException 异常
	 */
	@Nullable
	private <T> T createSingleBean(Function<WebFluxConfigurer, T> factory, Class<T> beanType) {
		List<T> result = this.delegates.stream()
				.map(factory)
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
		if (result.isEmpty()) {
			return null;
		} else if (result.size() == 1) {
			return result.get(0);
		} else {
			throw new IllegalStateException("More than one WebFluxConfigurer implements " +
					beanType.getSimpleName() + " factory method.");
		}
	}

}
