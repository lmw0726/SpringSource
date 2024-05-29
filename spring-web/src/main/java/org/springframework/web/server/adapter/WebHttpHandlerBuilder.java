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

package org.springframework.web.server.adapter;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.HttpHandlerDecoratorFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebHandler;
import org.springframework.web.server.handler.ExceptionHandlingWebHandler;
import org.springframework.web.server.handler.FilteringWebHandler;
import org.springframework.web.server.i18n.LocaleContextResolver;
import org.springframework.web.server.session.DefaultWebSessionManager;
import org.springframework.web.server.session.WebSessionManager;
import reactor.blockhound.BlockHound;
import reactor.blockhound.integration.BlockHoundIntegration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 此构建器有两个目的：
 *
 * <p>一个是组装一个处理链，该链由目标{@link WebHandler}组成，然后装饰了一组{@link WebFilter WebFilters}，
 * 然后进一步装饰了一组{@link WebExceptionHandler WebExceptionHandlers}。
 *
 * <p>第二个目的是将生成的处理链适配为{@link HttpHandler}：最低级别的反应式HTTP处理抽象，
 * 然后可以与任何受支持的运行时一起使用。 使用{@link HttpWebHandlerAdapter}完成适配。
 *
 * <p>处理链可以通过构建器方法手动组装，也可以通过{@link #applicationContext}从Spring {@link ApplicationContext}中检测到，
 * 或者两者混合使用。
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @see HttpWebHandlerAdapter
 * @since 5.0
 */
public final class WebHttpHandlerBuilder {

	/**
	 * 在bean工厂中用于目标WebHandler的众所周知的名称。
	 */
	public static final String WEB_HANDLER_BEAN_NAME = "webHandler";

	/**
	 * 在bean工厂中用于WebSessionManager的众所周知的名称。
	 */
	public static final String WEB_SESSION_MANAGER_BEAN_NAME = "webSessionManager";

	/**
	 * 在bean工厂中用于ServerCodecConfigurer的众所周知的名称。
	 */
	public static final String SERVER_CODEC_CONFIGURER_BEAN_NAME = "serverCodecConfigurer";

	/**
	 * 在bean工厂中用于LocaleContextResolver的众所周知的名称。
	 */
	public static final String LOCALE_CONTEXT_RESOLVER_BEAN_NAME = "localeContextResolver";

	/**
	 * 在bean工厂中用于ForwardedHeaderTransformer的众所周知的名称。
	 */
	public static final String FORWARDED_HEADER_TRANSFORMER_BEAN_NAME = "forwardedHeaderTransformer";

	/**
	 * WebHandler实例，用于处理HTTP请求。
	 */
	private final WebHandler webHandler;

	/**
	 * Spring应用程序上下文，用于检测组件是否在上下文中可用。
	 */
	@Nullable
	private final ApplicationContext applicationContext;

	/**
	 * 用于对HTTP请求进行过滤的WebFilter列表。
	 */
	private final List<WebFilter> filters = new ArrayList<>();

	/**
	 * 用于处理WebHandler中发生的异常的WebExceptionHandler列表。
	 */
	private final List<WebExceptionHandler> exceptionHandlers = new ArrayList<>();

	/**
	 * 用于装饰HttpHandler的函数，对请求进行进一步处理。
	 */
	@Nullable
	private Function<HttpHandler, HttpHandler> httpHandlerDecorator;

	/**
	 * Web会话管理器，用于管理Web会话。
	 */
	@Nullable
	private WebSessionManager sessionManager;

	/**
	 * 用于配置服务器编解码器的组件。
	 */
	@Nullable
	private ServerCodecConfigurer codecConfigurer;

	/**
	 * 用于解析和修改请求的区域设置上下文的解析器。
	 */
	@Nullable
	private LocaleContextResolver localeContextResolver;

	/**
	 * 用于从“Forwarded”和“X-Forwarded-*”头中提取信息并修改请求的转发头转换器。
	 */
	@Nullable
	private ForwardedHeaderTransformer forwardedHeaderTransformer;


	/**
	 * 从ApplicationContext初始化时使用的私有构造函数。
	 */
	private WebHttpHandlerBuilder(WebHandler webHandler, @Nullable ApplicationContext applicationContext) {
		Assert.notNull(webHandler, "WebHandler must not be null");
		this.webHandler = webHandler;
		this.applicationContext = applicationContext;
	}

	/**
	 * 复制构造函数。
	 */
	private WebHttpHandlerBuilder(WebHttpHandlerBuilder other) {
		this.webHandler = other.webHandler;
		this.applicationContext = other.applicationContext;
		this.filters.addAll(other.filters);
		this.exceptionHandlers.addAll(other.exceptionHandlers);
		this.sessionManager = other.sessionManager;
		this.codecConfigurer = other.codecConfigurer;
		this.localeContextResolver = other.localeContextResolver;
		this.forwardedHeaderTransformer = other.forwardedHeaderTransformer;
		this.httpHandlerDecorator = other.httpHandlerDecorator;
	}


	/**
	 * 创建一个新的构建器实例，指定目标处理程序。
	 *
	 * @param webHandler 请求的目标处理程序
	 * @return 准备好的构建器
	 */
	public static WebHttpHandlerBuilder webHandler(WebHandler webHandler) {
		return new WebHttpHandlerBuilder(webHandler, null);
	}

	/**
	 * 通过检测应用程序上下文中的bean来创建一个新的构建器实例。会检测到以下bean：
	 * <ul>
	 * <li>{@link WebHandler} [1] -- 根据名称{@link #WEB_HANDLER_BEAN_NAME}查找。
	 * <li>{@link WebFilter} [0..N] -- 根据类型检测并排序，参见{@link AnnotationAwareOrderComparator}。
	 * <li>{@link WebExceptionHandler} [0..N] -- 根据类型检测并排序。
	 * <li>{@link HttpHandlerDecoratorFactory} [0..N] -- 根据类型检测并排序。
	 * <li>{@link WebSessionManager} [0..1] -- 根据名称{@link #WEB_SESSION_MANAGER_BEAN_NAME}查找。
	 * <li>{@link ServerCodecConfigurer} [0..1] -- 根据名称{@link #SERVER_CODEC_CONFIGURER_BEAN_NAME}查找。
	 * <li>{@link LocaleContextResolver} [0..1] -- 根据名称{@link #LOCALE_CONTEXT_RESOLVER_BEAN_NAME}查找。
	 * </ul>
	 *
	 * @param context 用于查找bean的应用程序上下文
	 * @return 准备好的构建器
	 */
	public static WebHttpHandlerBuilder applicationContext(ApplicationContext context) {

		// 创建一个WebHttpHandlerBuilder实例
		WebHttpHandlerBuilder builder = new WebHttpHandlerBuilder(
				// 获取WebHandler实例
				context.getBean(WEB_HANDLER_BEAN_NAME, WebHandler.class), context);

		// 收集WebFilter实例并添加到过滤器列表中
		List<WebFilter> webFilters = context
				.getBeanProvider(WebFilter.class)
				.orderedStream()
				.collect(Collectors.toList());
		builder.filters(filters -> filters.addAll(webFilters));

		// 收集WebExceptionHandler实例并添加到异常处理器列表中
		List<WebExceptionHandler> exceptionHandlers = context
				.getBeanProvider(WebExceptionHandler.class)
				.orderedStream()
				.collect(Collectors.toList());
		builder.exceptionHandlers(handlers -> handlers.addAll(exceptionHandlers));

		// 添加HttpHandlerDecoratorFactory实例到构建器中
		context.getBeanProvider(HttpHandlerDecoratorFactory.class)
				.orderedStream()
				.forEach(builder::httpHandlerDecorator);

		try {
			// 设置会话管理器，如果存在的话
			builder.sessionManager(
					context.getBean(WEB_SESSION_MANAGER_BEAN_NAME, WebSessionManager.class));
		} catch (NoSuchBeanDefinitionException ex) {
			// 默认情况
		}

		try {
			// 设置编解码配置器，如果存在的话
			builder.codecConfigurer(
					context.getBean(SERVER_CODEC_CONFIGURER_BEAN_NAME, ServerCodecConfigurer.class));
		} catch (NoSuchBeanDefinitionException ex) {
			// 默认情况
		}

		try {
			// 设置区域上下文解析器，如果存在的话
			builder.localeContextResolver(
					context.getBean(LOCALE_CONTEXT_RESOLVER_BEAN_NAME, LocaleContextResolver.class));
		} catch (NoSuchBeanDefinitionException ex) {
			// 默认情况
		}

		try {
			// 设置转发头部转换器，如果存在的话
			builder.forwardedHeaderTransformer(
					context.getBean(FORWARDED_HEADER_TRANSFORMER_BEAN_NAME, ForwardedHeaderTransformer.class));
		} catch (NoSuchBeanDefinitionException ex) {
			// 默认情况
		}

		// 返回构建器
		return builder;
	}


	/**
	 * 添加给定的过滤器。
	 *
	 * @param filters 要添加的过滤器(s)
	 * @return 当前构建器实例
	 */
	public WebHttpHandlerBuilder filter(WebFilter... filters) {
		if (!ObjectUtils.isEmpty(filters)) {
			// 如果过滤器列表不为空，添加过滤器列表
			this.filters.addAll(Arrays.asList(filters));
			// 更新过滤器
			updateFilters();
		}
		// 返回当前实例
		return this;
	}

	/**
	 * 操作当前配置的过滤器的 "live" 列表。
	 *
	 * @param consumer 使用的 Consumer
	 * @return 当前构建器实例
	 */
	public WebHttpHandlerBuilder filters(Consumer<List<WebFilter>> consumer) {
		// 消费过滤器
		consumer.accept(this.filters);
		// 更新过滤器
		updateFilters();
		return this;
	}

	private void updateFilters() {
		if (this.filters.isEmpty()) {
			// 如果过滤器列表为空，则直接返回
			return;
		}

		// 过滤器列表中包含转发头部转换器的情况下，设置转发头部转换器属性
		List<WebFilter> filtersToUse = this.filters.stream()
				.peek(filter -> {
					if (filter instanceof ForwardedHeaderTransformer && this.forwardedHeaderTransformer == null) {
						// 如果拦截器是 转换请求头转换器，并且当前没有设置转换请求头转换器，则设置转换请求头转换器
						this.forwardedHeaderTransformer = (ForwardedHeaderTransformer) filter;
					}
				})
				// 过滤掉转发头部转换器
				.filter(filter -> !(filter instanceof ForwardedHeaderTransformer))
				.collect(Collectors.toList());

		// 清空原始过滤器列表，并将过滤器列表更新为过滤掉转发头部转换器后的列表
		this.filters.clear();
		this.filters.addAll(filtersToUse);
	}

	/**
	 * 添加给定的异常处理器。
	 *
	 * @param handlers 异常处理器(s)
	 * @return 当前构建器实例
	 */
	public WebHttpHandlerBuilder exceptionHandler(WebExceptionHandler... handlers) {
		if (!ObjectUtils.isEmpty(handlers)) {
			// 如果异常处理器列表不为空，添加异常处理器列表
			this.exceptionHandlers.addAll(Arrays.asList(handlers));
		}
		return this;
	}

	/**
	 * 操作当前配置的异常处理器的 "live" 列表。
	 *
	 * @param consumer 使用的 Consumer
	 * @return 当前构建器实例
	 */
	public WebHttpHandlerBuilder exceptionHandlers(Consumer<List<WebExceptionHandler>> consumer) {
		consumer.accept(this.exceptionHandlers);
		return this;
	}

	/**
	 * 配置要设置在 {@link ServerWebExchange WebServerExchange} 上的 {@link WebSessionManager}。
	 * 默认情况下，使用 {@link DefaultWebSessionManager}。
	 *
	 * @param manager 会话管理器
	 * @return 当前构建器实例
	 * @see HttpWebHandlerAdapter#setSessionManager(WebSessionManager)
	 */
	public WebHttpHandlerBuilder sessionManager(WebSessionManager manager) {
		this.sessionManager = manager;
		return this;
	}

	/**
	 * 是否已配置 {@code WebSessionManager}，无论是从 {@code ApplicationContext} 中检测到的还是通过 {@link #sessionManager} 显式配置的。
	 *
	 * @return 如果已配置 {@code WebSessionManager}，则为 true；否则为 false
	 * @since 5.0.9
	 */
	public boolean hasSessionManager() {
		return (this.sessionManager != null);
	}

	/**
	 * 配置要设置在 {@link ServerWebExchange WebServerExchange} 上的 {@link ServerCodecConfigurer}。
	 *
	 * @param codecConfigurer 编解码配置器
	 * @return 当前构建器实例
	 */
	public WebHttpHandlerBuilder codecConfigurer(ServerCodecConfigurer codecConfigurer) {
		this.codecConfigurer = codecConfigurer;
		return this;
	}


	/**
	 * 是否已配置 {@code ServerCodecConfigurer}，无论是从 {@code ApplicationContext} 中检测到的还是通过 {@link #codecConfigurer} 显式配置的。
	 *
	 * @return 如果已配置 {@code ServerCodecConfigurer}，则为 true；否则为 false
	 * @since 5.0.9
	 */
	public boolean hasCodecConfigurer() {
		return (this.codecConfigurer != null);
	}

	/**
	 * 配置要设置在 {@link ServerWebExchange WebServerExchange} 上的 {@link LocaleContextResolver}。
	 *
	 * @param localeContextResolver 区域环境解析器
	 * @return 当前构建器实例
	 */
	public WebHttpHandlerBuilder localeContextResolver(LocaleContextResolver localeContextResolver) {
		this.localeContextResolver = localeContextResolver;
		return this;
	}

	/**
	 * 是否已配置 {@code LocaleContextResolver}，无论是从 {@code ApplicationContext} 中检测到的还是通过 {@link #localeContextResolver} 显式配置的。
	 *
	 * @return 如果已配置 {@code LocaleContextResolver}，则为 true；否则为 false
	 * @since 5.0.9
	 */
	public boolean hasLocaleContextResolver() {
		return (this.localeContextResolver != null);
	}

	/**
	 * 配置 {@link ForwardedHeaderTransformer} 用于提取和/或删除转发的标头。
	 *
	 * @param transformer 转换器
	 * @return 当前构建器实例
	 * @since 5.1
	 */
	public WebHttpHandlerBuilder forwardedHeaderTransformer(ForwardedHeaderTransformer transformer) {
		this.forwardedHeaderTransformer = transformer;
		return this;
	}

	/**
	 * 是否配置了 {@code ForwardedHeaderTransformer}，无论是从 {@code ApplicationContext} 中检测到的还是通过 {@link #forwardedHeaderTransformer(ForwardedHeaderTransformer)} 显式配置的。
	 *
	 * @return 如果已配置 {@code ForwardedHeaderTransformer}，则为 true；否则为 false
	 * @since 5.1
	 */
	public boolean hasForwardedHeaderTransformer() {
		return (this.forwardedHeaderTransformer != null);
	}

	/**
	 * 配置一个 {@link Function} 来装饰由此构建器返回的 {@link HttpHandler}，这实际上包装了整个 {@link WebExceptionHandler} - {@link WebFilter} - {@link WebHandler} 处理链。这提供了在整个链之前访问请求和响应以及观察整个链结果的能力。
	 *
	 * @param handlerDecorator 要应用的装饰器
	 * @return 当前构建器实例
	 * @since 5.3
	 */
	public WebHttpHandlerBuilder httpHandlerDecorator(Function<HttpHandler, HttpHandler> handlerDecorator) {
		this.httpHandlerDecorator = (this.httpHandlerDecorator != null ?
				handlerDecorator.andThen(this.httpHandlerDecorator) : handlerDecorator);
		return this;
	}

	/**
	 * 是否通过 {@link #httpHandlerDecorator(Function)} 配置了 {@link HttpHandler} 的装饰器。
	 *
	 * @return 如果已配置 {@link HttpHandler} 的装饰器，则为 true；否则为 false
	 * @since 5.3
	 */
	public boolean hasHttpHandlerDecorator() {
		return (this.httpHandlerDecorator != null);
	}

	/**
	 * 构建 {@link HttpHandler} 实例
	 */
	public HttpHandler build() {
		// 创建过滤后的WebHandler
		WebHandler decorated = new FilteringWebHandler(this.webHandler, this.filters);
		// 添加异常处理功能到WebHandler
		decorated = new ExceptionHandlingWebHandler(decorated, this.exceptionHandlers);

		// 创建HttpWebHandlerAdapter，并设置相应的属性
		HttpWebHandlerAdapter adapted = new HttpWebHandlerAdapter(decorated);
		if (this.sessionManager != null) {
			adapted.setSessionManager(this.sessionManager);
		}
		if (this.codecConfigurer != null) {
			adapted.setCodecConfigurer(this.codecConfigurer);
		}
		if (this.localeContextResolver != null) {
			adapted.setLocaleContextResolver(this.localeContextResolver);
		}
		if (this.forwardedHeaderTransformer != null) {
			adapted.setForwardedHeaderTransformer(this.forwardedHeaderTransformer);
		}
		if (this.applicationContext != null) {
			adapted.setApplicationContext(this.applicationContext);
		}
		// 执行属性设置后的初始化操作
		adapted.afterPropertiesSet();

		// 如果有HttpHandlerDecorator，则应用装饰器，否则直接返回adapted
		return (this.httpHandlerDecorator != null ? this.httpHandlerDecorator.apply(adapted) : adapted);
	}

	/**
	 * 克隆此 {@link WebHttpHandlerBuilder}。
	 *
	 * @return 克隆的构建器实例
	 */
	@Override
	public WebHttpHandlerBuilder clone() {
		return new WebHttpHandlerBuilder(this);
	}


	/**
	 * 用于 spring-web 类的 {@code BlockHoundIntegration}。
	 *
	 * @since 5.3.6
	 */
	public static class SpringWebBlockHoundIntegration implements BlockHoundIntegration {

		@Override
		public void applyTo(BlockHound.Builder builder) {

			// 避免在 spring-web 中的任何地方出现硬引用（无需结构依赖）

			builder.allowBlockingCallsInside("org.springframework.http.MediaTypeFactory", "<clinit>");
			builder.allowBlockingCallsInside("org.springframework.web.util.HtmlUtils", "<clinit>");
		}
	}

}
