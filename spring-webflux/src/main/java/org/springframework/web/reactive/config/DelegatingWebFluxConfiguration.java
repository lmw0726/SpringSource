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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.MessageCodesResolver;
import org.springframework.validation.Validator;
import org.springframework.web.reactive.accept.RequestedContentTypeResolverBuilder;
import org.springframework.web.reactive.result.method.annotation.ArgumentResolverConfigurer;
import org.springframework.web.reactive.socket.server.WebSocketService;

import java.util.List;

/**
 * 一个 {@code WebFluxConfigurationSupport} 的子类，它检测并委托给所有类型为 {@link WebFluxConfigurer} 的 bean，
 * 允许它们自定义 {@code WebFluxConfigurationSupport} 提供的配置。这是实际由 {@link EnableWebFlux @EnableWebFlux}
 * 导入的类。
 *
 * @author Brian Clozel
 * @since 5.0
 */
@Configuration(proxyBeanMethods = false)
public class DelegatingWebFluxConfiguration extends WebFluxConfigurationSupport {

	/**
	 * 一个 WebFluxConfigurerComposite 类型的成员变量，用于管理 WebFluxConfigurer 实例的集合。
	 * WebFluxConfigurerComposite 类型对象是用来收集和管理 WebFluxConfigurer 实例的集合。
	 */
	private final WebFluxConfigurerComposite configurers = new WebFluxConfigurerComposite();

	/**
	 * 设置 WebFluxConfigurer 类型的 bean。如果配置器列表不为空，则将其添加到 configurers 中。
	 *
	 * @param configurers WebFluxConfigurer 类型的 bean 列表
	 */
	@Autowired(required = false)
	public void setConfigurers(List<WebFluxConfigurer> configurers) {
		if (!CollectionUtils.isEmpty(configurers)) {
			this.configurers.addWebFluxConfigurers(configurers);
		}
	}


	/**
	 * 通过调用 configurers 对象的 configureContentTypeResolver 方法，
	 * 对内容类型解析器进行配置。
	 *
	 * @param builder RequestedContentTypeResolverBuilder 对象，用于构建内容类型解析器
	 */
	@Override
	protected void configureContentTypeResolver(RequestedContentTypeResolverBuilder builder) {
		this.configurers.configureContentTypeResolver(builder);
	}

	/**
	 * 通过调用 configurers 对象的 addCorsMappings 方法，
	 * 向 CorsRegistry 中添加跨域映射配置。
	 *
	 * @param registry CorsRegistry 对象，用于添加跨域映射配置
	 */
	@Override
	protected void addCorsMappings(CorsRegistry registry) {
		this.configurers.addCorsMappings(registry);
	}

	/**
	 * 通过调用 configurers 对象的 configurePathMatching 方法，
	 * 配置路径匹配器。
	 *
	 * @param configurer PathMatchConfigurer 对象，用于配置路径匹配器
	 */
	@Override
	public void configurePathMatching(PathMatchConfigurer configurer) {
		this.configurers.configurePathMatching(configurer);
	}

	/**
	 * 通过调用 configurers 对象的 addResourceHandlers 方法，
	 * 向 ResourceHandlerRegistry 中添加资源处理器配置。
	 *
	 * @param registry ResourceHandlerRegistry 对象，用于添加资源处理器配置
	 */
	@Override
	protected void addResourceHandlers(ResourceHandlerRegistry registry) {
		this.configurers.addResourceHandlers(registry);
	}


	/**
	 * 通过调用 configurers 对象的 configureArgumentResolvers 方法，
	 * 配置参数解析器。
	 *
	 * @param configurer ArgumentResolverConfigurer 对象，用于配置参数解析器
	 */
	@Override
	protected void configureArgumentResolvers(ArgumentResolverConfigurer configurer) {
		this.configurers.configureArgumentResolvers(configurer);
	}

	/**
	 * 通过调用 configurers 对象的 configureHttpMessageCodecs 方法，
	 * 配置 HTTP 消息编解码器。
	 *
	 * @param configurer ServerCodecConfigurer 对象，用于配置 HTTP 消息编解码器
	 */
	@Override
	protected void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
		this.configurers.configureHttpMessageCodecs(configurer);
	}


	/**
	 * 通过调用 configurers 对象的 addFormatters 方法，
	 * 向 FormatterRegistry 中添加格式化程序。
	 *
	 * @param registry FormatterRegistry 对象，用于添加格式化程序
	 */
	@Override
	protected void addFormatters(FormatterRegistry registry) {
		this.configurers.addFormatters(registry);
	}

	/**
	 * 获取验证器。通过调用 configurers 对象的 getValidator 方法获取验证器，
	 * 如果未配置验证器，则返回默认的验证器。
	 *
	 * @return 验证器对象
	 */
	@Override
	protected Validator getValidator() {
		Validator validator = this.configurers.getValidator();
		return (validator != null ? validator : super.getValidator());
	}


	/**
	 * 获取消息代码解析器。通过调用 configurers 对象的 getMessageCodesResolver 方法获取消息代码解析器，
	 * 如果未配置消息代码解析器，则返回默认的消息代码解析器。
	 *
	 * @return 消息代码解析器对象
	 */
	@Override
	protected MessageCodesResolver getMessageCodesResolver() {
		MessageCodesResolver messageCodesResolver = this.configurers.getMessageCodesResolver();
		return (messageCodesResolver != null ? messageCodesResolver : super.getMessageCodesResolver());
	}

	/**
	 * 获取 WebSocketService。通过调用 configurers 对象的 getWebSocketService 方法获取 WebSocketService，
	 * 如果未配置 WebSocketService，则返回默认的 WebSocketService。
	 *
	 * @return WebSocketService 对象
	 */
	@Override
	protected WebSocketService getWebSocketService() {
		WebSocketService service = this.configurers.getWebSocketService();
		return (service != null ? service : super.getWebSocketService());
	}

	/**
	 * 配置视图解析器。通过调用 configurers 对象的 configureViewResolvers 方法配置视图解析器。
	 *
	 * @param registry ViewResolverRegistry 对象，用于配置视图解析器
	 */
	@Override
	protected void configureViewResolvers(ViewResolverRegistry registry) {
		this.configurers.configureViewResolvers(registry);
	}

}
