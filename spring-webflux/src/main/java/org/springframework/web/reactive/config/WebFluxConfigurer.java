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

package org.springframework.web.reactive.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.format.Formatter;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.lang.Nullable;
import org.springframework.validation.MessageCodesResolver;
import org.springframework.validation.Validator;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.reactive.accept.RequestedContentTypeResolverBuilder;
import org.springframework.web.reactive.result.method.annotation.ArgumentResolverConfigurer;
import org.springframework.web.reactive.socket.server.WebSocketService;

/**
 * 定义回调方法，用于自定义通过 {@link EnableWebFlux @EnableWebFlux} 启用的 WebFlux 应用程序的配置。
 *
 * <p>{@code @EnableWebFlux} 注解的配置类可以实现此接口以被回调，并有机会定制默认配置。考虑实现此接口并重写相关方法以满足您的需求。
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @see WebFluxConfigurationSupport
 * @see DelegatingWebFluxConfiguration
 * @since 5.0
 */
public interface WebFluxConfigurer {

	/**
	 * 配置处理带有注解控制器的请求时用于解析响应的内容类型。
	 *
	 * @param builder 用于配置要使用的解析器
	 */
	default void configureContentTypeResolver(RequestedContentTypeResolverBuilder builder) {
	}

	/**
	 * 配置“全局”跨源请求处理。配置的 CORS 映射适用于注解控制器、函数式端点和静态资源。
	 * <p>注解控制器可以通过 {@link org.springframework.web.bind.annotation.CrossOrigin @CrossOrigin} 进一步声明更精细的配置。
	 * 在这种情况下，此处声明的“全局”CORS 配置将与在控制器方法上定义的本地 CORS 配置进行 {@link org.springframework.web.cors.CorsConfiguration#combine(CorsConfiguration) 组合}。
	 *
	 * @see CorsRegistry
	 * @see CorsConfiguration#combine(CorsConfiguration)
	 */
	default void addCorsMappings(CorsRegistry registry) {
	}

	/**
	 * 配置路径匹配选项。
	 * <p>配置的路径匹配选项将用于映射到注解控制器，还包括 {@link #addResourceHandlers(ResourceHandlerRegistry) 静态资源}。
	 *
	 * @param configurer {@link PathMatchConfigurer} 实例
	 */
	default void configurePathMatching(PathMatchConfigurer configurer) {
	}

	/**
	 * 添加用于提供静态资源的资源处理程序。
	 *
	 * @see ResourceHandlerRegistry
	 */
	default void addResourceHandlers(ResourceHandlerRegistry registry) {
	}

	/**
	 * 配置用于自定义 {@code @RequestMapping} 方法参数解析器。
	 *
	 * @param configurer 要使用的配置器
	 */
	default void configureArgumentResolvers(ArgumentResolverConfigurer configurer) {
	}

	/**
	 * 配置用于从请求主体读取和向响应主体写入的 HTTP 消息读取器和写入器（用于注解控制器和函数式端点）。
	 * <p>默认情况下，只要类路径上存在相应的第三方库（例如 Jackson JSON、JAXB2 等），所有内置的读取器和写入器都会被配置。
	 *
	 * @param configurer 用于自定义读取器和写入器的配置器
	 */
	default void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
	}

	/**
	 * 为执行注解控制器方法参数的类型转换和格式化添加自定义 {@link Converter Converters} 和 {@link Formatter Formatters}。
	 */
	default void addFormatters(FormatterRegistry registry) {
	}

	/**
	 * 提供自定义的 {@link Validator}。
	 * <p>默认情况下，如果类路径上存在 bean 验证 API，则会创建用于标准 bean 验证的验证器。
	 * <p>配置的验证器用于验证注解控制器方法参数。
	 */
	@Nullable
	default Validator getValidator() {
		return null;
	}

	/**
	 * 提供自定义的 {@link MessageCodesResolver}，用于在注解控制器方法参数中进行数据绑定，而不是在 {@link org.springframework.validation.DataBinder} 中默认创建的解析器。
	 */
	@Nullable
	default MessageCodesResolver getMessageCodesResolver() {
		return null;
	}

	/**
	 * 提供用于创建 {@link org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter} 的 {@link WebSocketService}。
	 * 这可用于通过 {@link org.springframework.web.reactive.socket.server.RequestUpgradeStrategy} 配置服务器特定属性。
	 *
	 * @since 5.3
	 */
	@Nullable
	default WebSocketService getWebSocketService() {
		return null;
	}

	/**
	 * 配置视图解析，用于呈现带有视图和模型的响应，其中视图通常是 HTML 模板，但也可以基于 HTTP 消息写入器（例如 JSON、XML）。
	 * <p>配置的视图解析器将用于注解控制器和函数式端点。
	 */
	default void configureViewResolvers(ViewResolverRegistry registry) {
	}

}
