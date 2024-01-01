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

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.format.Formatter;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.MessageCodesResolver;
import org.springframework.validation.Validator;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.reactive.DispatcherHandler;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;
import org.springframework.web.reactive.accept.RequestedContentTypeResolverBuilder;
import org.springframework.web.reactive.function.server.support.HandlerFunctionAdapter;
import org.springframework.web.reactive.function.server.support.RouterFunctionMapping;
import org.springframework.web.reactive.function.server.support.ServerResponseResultHandler;
import org.springframework.web.reactive.handler.AbstractHandlerMapping;
import org.springframework.web.reactive.handler.WebFluxResponseStatusExceptionHandler;
import org.springframework.web.reactive.resource.ResourceUrlProvider;
import org.springframework.web.reactive.result.SimpleHandlerAdapter;
import org.springframework.web.reactive.result.method.annotation.*;
import org.springframework.web.reactive.result.view.ViewResolutionResultHandler;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.WebSocketService;
import org.springframework.web.reactive.socket.server.support.HandshakeWebSocketService;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import org.springframework.web.server.i18n.AcceptHeaderLocaleContextResolver;
import org.springframework.web.server.i18n.LocaleContextResolver;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Spring WebFlux 配置的主要类。
 *
 * <p>直接导入或扩展并重写受保护的方法以进行自定义。
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 5.0
 */
public class WebFluxConfigurationSupport implements ApplicationContextAware {

	/**
	 * CORS 配置映射
	 */
	@Nullable
	private Map<String, CorsConfiguration> corsConfigurations;

	/**
	 * 路径匹配配置器
	 */
	@Nullable
	private PathMatchConfigurer pathMatchConfigurer;

	/**
	 * 视图解析器注册表
	 */
	@Nullable
	private ViewResolverRegistry viewResolverRegistry;

	/**
	 * 应用程序上下文
	 */
	@Nullable
	private ApplicationContext applicationContext;


	/**
	 * 设置应用程序上下文实例。
	 *
	 * @param applicationContext 应用程序上下文实例
	 */
	@Override
	public void setApplicationContext(@Nullable ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
		if (applicationContext != null) {
				Assert.state(!applicationContext.containsBean("mvcContentNegotiationManager"),
						"The Java/XML config for Spring MVC and Spring WebFlux cannot both be enabled, " +
						"e.g. via @EnableWebMvc and @EnableWebFlux, in the same application.");
		}
	}


	/**
	 * 获取应用程序上下文实例。
	 *
	 * @return 应用程序上下文实例，可能为 null
	 */
	@Nullable
	public final ApplicationContext getApplicationContext() {
		return this.applicationContext;
	}

	/**
	 * 创建调度处理程序（DispatcherHandler）实例。
	 *
	 * @return 调度处理程序实例
	 */
	@Bean
	public DispatcherHandler webHandler() {
		return new DispatcherHandler();
	}

	/**
	 * 创建 Web 异常处理器（WebExceptionHandler）实例，并设置优先级为 0。
	 *
	 * @return Web 异常处理器实例
	 */
	@Bean
	@Order(0)
	public WebExceptionHandler responseStatusExceptionHandler() {
		return new WebFluxResponseStatusExceptionHandler();
	}

	/**
	 * 创建请求映射处理器（RequestMappingHandlerMapping）实例。
	 * 设置映射器顺序、内容类型解析器、路径前缀以及 CORS 配置等信息。
	 *
	 * @param contentTypeResolver 用于解析请求内容类型的解析器
	 * @return 请求映射处理器实例
	 */
	@Bean
	public RequestMappingHandlerMapping requestMappingHandlerMapping(
			@Qualifier("webFluxContentTypeResolver") RequestedContentTypeResolver contentTypeResolver) {

		// 创建请求映射处理器映射
		RequestMappingHandlerMapping mapping = createRequestMappingHandlerMapping();

		// 设置映射顺序、内容类型解析器
		mapping.setOrder(0);
		mapping.setContentTypeResolver(contentTypeResolver);

		// 获取路径匹配配置器
		PathMatchConfigurer configurer = getPathMatchConfigurer();

		// 配置抽象处理器映射和路径前缀
		configureAbstractHandlerMapping(mapping, configurer);
		Map<String, Predicate<Class<?>>> pathPrefixes = configurer.getPathPrefixes();

		// 如果路径前缀映射不为空，则设置到映射中
		if (pathPrefixes != null) {
			mapping.setPathPrefixes(pathPrefixes);
		}

		// 返回配置完成的请求映射处理器映射
		return mapping;
	}

	/**
	 * 配置抽象的处理器映射（AbstractHandlerMapping）。
	 * 设置 CORS 配置、是否使用尾部斜杠匹配和是否区分大小写匹配。
	 *
	 * @param mapping     要配置的抽象处理器映射实例
	 * @param configurer  路径匹配配置器，用于提供尾部斜杠匹配和区分大小写匹配信息
	 */
	private void configureAbstractHandlerMapping(AbstractHandlerMapping mapping, PathMatchConfigurer configurer) {
		// 设置 CORS 配置映射
		mapping.setCorsConfigurations(getCorsConfigurations());

		// 获取是否使用尾部斜杠匹配
		Boolean useTrailingSlashMatch = configurer.isUseTrailingSlashMatch();

		// 如果 useTrailingSlashMatch 不为空，则设置到映射中
		if (useTrailingSlashMatch != null) {
			mapping.setUseTrailingSlashMatch(useTrailingSlashMatch);
		}

		// 获取是否区分大小写匹配
		Boolean useCaseSensitiveMatch = configurer.isUseCaseSensitiveMatch();

		// 如果 useCaseSensitiveMatch 不为空，则设置到映射中
		if (useCaseSensitiveMatch != null) {
			mapping.setUseCaseSensitiveMatch(useCaseSensitiveMatch);
		}
	}

	/**
	 * 重写此方法以使用 {@link RequestMappingHandlerMapping} 的子类。
	 *
	 * @return {@link RequestMappingHandlerMapping} 的子类实例
	 */
	protected RequestMappingHandlerMapping createRequestMappingHandlerMapping() {
		return new RequestMappingHandlerMapping();
	}

	/**
	 * 创建用于 WebFlux 的请求内容类型解析器。
	 *
	 * @return 请求内容类型解析器 {@link RequestedContentTypeResolver}
	 */
	@Bean
	public RequestedContentTypeResolver webFluxContentTypeResolver() {
		RequestedContentTypeResolverBuilder builder = new RequestedContentTypeResolverBuilder();
		configureContentTypeResolver(builder);
		return builder.build();
	}

	/**
	 * 重写此方法以配置请求的内容类型如何解析。
	 *
	 * @param builder 请求内容类型解析器构建器 {@link RequestedContentTypeResolverBuilder}
	 */
	protected void configureContentTypeResolver(RequestedContentTypeResolverBuilder builder) {
	}

	/**
	 * 用于构建全局 CORS 配置的回调方法。此方法是最终方法。
	 * 使用 {@link #addCorsMappings(CorsRegistry)} 来自定义 CORS 配置。
	 *
	 * @return 包含跨源资源共享配置的映射 {@link Map}
	 */
	protected final Map<String, CorsConfiguration> getCorsConfigurations() {
		// 如果 CORS 配置映射为空
		if (this.corsConfigurations == null) {
			// 创建 CORS 注册表
			CorsRegistry registry = new CorsRegistry();

			// 添加 CORS 映射
			addCorsMappings(registry);

			// 获取 CORS 配置映射
			this.corsConfigurations = registry.getCorsConfigurations();
		}

		// 返回 CORS 配置映射
		return this.corsConfigurations;
	}

	/**
	 * 要配置跨源请求处理，请重写此方法。
	 *
	 * @param registry 跨源资源共享注册表 {@link CorsRegistry}
	 * @see CorsRegistry
	 */
	protected void addCorsMappings(CorsRegistry registry) {
	}

	/**
	 * 用于构建 {@link PathMatchConfigurer} 的回调方法。此方法是最终方法，
	 * 使用 {@link #configurePathMatching} 来自定义路径匹配。
	 *
	 * @return PathMatchConfigurer
	 */
	protected final PathMatchConfigurer getPathMatchConfigurer() {
		// 如果路径匹配配置器为空
		if (this.pathMatchConfigurer == null) {
			// 创建路径匹配配置器并配置路径匹配
			this.pathMatchConfigurer = new PathMatchConfigurer();
			configurePathMatching(this.pathMatchConfigurer);
		}

		// 返回路径匹配配置器
		return this.pathMatchConfigurer;
	}

	/**
	 * 要配置路径匹配选项，请重写此方法。
	 *
	 * @param configurer 路径匹配配置器 {@link PathMatchConfigurer}
	 */
	public void configurePathMatching(PathMatchConfigurer configurer) {
	}

	/**
	 * 创建一个 RouterFunctionMapping，并按照顺序设置为 -1（在 RequestMappingHandlerMapping 之前）。
	 *
	 * @param serverCodecConfigurer 服务器编解码器配置 {@link ServerCodecConfigurer}
	 * @return RouterFunctionMapping
	 */
	@Bean
	public RouterFunctionMapping routerFunctionMapping(ServerCodecConfigurer serverCodecConfigurer) {
		RouterFunctionMapping mapping = createRouterFunctionMapping();
		// 在 RequestMappingHandlerMapping 之前执行
		mapping.setOrder(-1);
		//设置消息阅读器
		mapping.setMessageReaders(serverCodecConfigurer.getReaders());
		configureAbstractHandlerMapping(mapping, getPathMatchConfigurer());
		return mapping;
	}

	/**
	 * 要创建 RouterFunctionMapping 的子类，请重写此方法。
	 *
	 * @return RouterFunctionMapping 子类实例
	 */
	protected RouterFunctionMapping createRouterFunctionMapping() {
		return new RouterFunctionMapping();
	}

	/**
	 * 创建一个处理器映射，按照 Integer.MAX_VALUE-1 的顺序对已映射的资源处理器进行排序。
	 * 若要配置资源处理，请重写 {@link #addResourceHandlers} 方法。
	 *
	 * @param resourceUrlProvider 用于提供资源 URL 的 {@link ResourceUrlProvider}
	 * @return 已映射资源处理器的处理器映射
	 */
	@Bean
	public HandlerMapping resourceHandlerMapping(ResourceUrlProvider resourceUrlProvider) {
		// 获取资源加载器，默认为应用程序上下文
		ResourceLoader resourceLoader = this.applicationContext;

		// 如果资源加载器为空，则使用默认的资源加载器
		if (resourceLoader == null) {
			resourceLoader = new DefaultResourceLoader();
		}

		// 创建资源处理器注册表
		ResourceHandlerRegistry registry = new ResourceHandlerRegistry(resourceLoader);

		// 设置资源 URL 提供程序和添加资源处理器
		registry.setResourceUrlProvider(resourceUrlProvider);
		addResourceHandlers(registry);

		// 获取处理器映射
		AbstractHandlerMapping handlerMapping = registry.getHandlerMapping();

		// 如果处理器映射不为空，则配置抽象处理器映射
		if (handlerMapping != null) {
			configureAbstractHandlerMapping(handlerMapping, getPathMatchConfigurer());
		} else {
			// 如果处理器映射为空，则创建一个空的处理器映射
			handlerMapping = new EmptyHandlerMapping();
		}

		// 返回配置完成的处理器映射
		return handlerMapping;
	}

	/**
	 * 返回一个资源 URL 提供者 {@link ResourceUrlProvider}。
	 */
	@Bean
	public ResourceUrlProvider resourceUrlProvider() {
		return new ResourceUrlProvider();
	}

	/**
	 * 要添加用于提供静态资源的资源处理器，请重写此方法。
	 *
	 * @param registry 用于资源处理的 {@link ResourceHandlerRegistry}
	 * @see ResourceHandlerRegistry
	 */
	protected void addResourceHandlers(ResourceHandlerRegistry registry) {
	}

	/**
	 * 创建一个 {@link RequestMappingHandlerAdapter} 实例。
	 *
	 * @param reactiveAdapterRegistry 用于适配响应式类型的 {@link ReactiveAdapterRegistry}
	 * @param serverCodecConfigurer   用于配置 HTTP 消息的 {@link ServerCodecConfigurer}
	 * @param conversionService       用于格式转换的 {@link FormattingConversionService}
	 * @param validator               用于验证的 {@link Validator}
	 * @return 创建的 {@link RequestMappingHandlerAdapter} 实例
	 */
	@Bean
	public RequestMappingHandlerAdapter requestMappingHandlerAdapter(
			@Qualifier("webFluxAdapterRegistry") ReactiveAdapterRegistry reactiveAdapterRegistry,
			ServerCodecConfigurer serverCodecConfigurer,
			@Qualifier("webFluxConversionService") FormattingConversionService conversionService,
			@Qualifier("webFluxValidator") Validator validator) {

		// 创建请求映射处理适配器
		RequestMappingHandlerAdapter adapter = createRequestMappingHandlerAdapter();

		// 设置消息读取器、Web 绑定初始化器和响应式适配器注册表
		adapter.setMessageReaders(serverCodecConfigurer.getReaders());
		adapter.setWebBindingInitializer(getConfigurableWebBindingInitializer(conversionService, validator));
		adapter.setReactiveAdapterRegistry(reactiveAdapterRegistry);

		// 创建参数解析器配置器
		ArgumentResolverConfigurer configurer = new ArgumentResolverConfigurer();

		// 配置参数解析器
		configureArgumentResolvers(configurer);

		// 设置参数解析器配置器到适配器中
		adapter.setArgumentResolverConfigurer(configurer);

		// 返回配置完成的适配器
		return adapter;
	}

	/**
	 * 重写此方法以插入{@link RequestMappingHandlerAdapter}的子类。
	 *
	 * @return 创建的{@link RequestMappingHandlerAdapter}实例
	 */
	protected RequestMappingHandlerAdapter createRequestMappingHandlerAdapter() {
		return new RequestMappingHandlerAdapter();
	}

	/**
	 * 配置自定义控制器方法参数的解析器。
	 *
	 * @param configurer {@link ArgumentResolverConfigurer}的配置器
	 */
	protected void configureArgumentResolvers(ArgumentResolverConfigurer configurer) {
	}

	/**
	 * 返回HTTP消息读取器和写入器的配置器。
	 * <p>使用{@link #configureHttpMessageCodecs(ServerCodecConfigurer)}配置读取器和写入器。
	 *
	 * @return {@link ServerCodecConfigurer}实例
	 */
	@Bean
	public ServerCodecConfigurer serverCodecConfigurer() {
		ServerCodecConfigurer serverCodecConfigurer = ServerCodecConfigurer.create();
		configureHttpMessageCodecs(serverCodecConfigurer);
		return serverCodecConfigurer;
	}

	/**
	 * 重写此方法以插入{@link LocaleContextResolver}的子类。
	 *
	 * @return 创建的{@link LocaleContextResolver}实例
	 */
	protected LocaleContextResolver createLocaleContextResolver() {
		return new AcceptHeaderLocaleContextResolver();
	}

	/**
	 * 创建{@link LocaleContextResolver} Bean。
	 *
	 * @return {@link LocaleContextResolver} Bean
	 */
	@Bean
	public LocaleContextResolver localeContextResolver() {
		return createLocaleContextResolver();
	}

	/**
	 * 配置要使用的HTTP消息读取器和写入器。
	 *
	 * @param configurer 用于配置的{@link ServerCodecConfigurer}
	 */
	protected void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
	}

	/**
	 * 返回用于初始化所有{@link WebDataBinder}实例的{@link ConfigurableWebBindingInitializer}。
	 *
	 * @param webFluxConversionService 用于注解控制器的{@link FormattingConversionService}
	 * @param webFluxValidator         用于验证的{@link Validator}
	 * @return {@link ConfigurableWebBindingInitializer}实例
	 */
	protected ConfigurableWebBindingInitializer getConfigurableWebBindingInitializer(
			FormattingConversionService webFluxConversionService, Validator webFluxValidator) {

		// 创建可配置的 Web 绑定初始化器
		ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();

		// 设置转换服务和验证器
		initializer.setConversionService(webFluxConversionService);
		initializer.setValidator(webFluxValidator);

		// 获取消息代码解析器
		MessageCodesResolver messageCodesResolver = getMessageCodesResolver();

		// 如果消息代码解析器不为空，则设置到初始化器中
		if (messageCodesResolver != null) {
			initializer.setMessageCodesResolver(messageCodesResolver);
		}

		// 返回配置完成的初始化器
		return initializer;
	}

	/**
	 * 返回一个用于注解控制器的{@link FormattingConversionService}。
	 * <p>参见{@link #addFormatters}作为重写此方法的替代方法。
	 *
	 * @return 用于注解控制器的{@link FormattingConversionService}
	 */
	@Bean
	public FormattingConversionService webFluxConversionService() {
		FormattingConversionService service = new DefaultFormattingConversionService();
		addFormatters(service);
		return service;
	}

	/**
	 * 重写此方法以向通用的{@link FormattingConversionService}添加自定义的{@link Converter}和/或{@link Formatter}委托。
	 *
	 * @param registry 格式化器注册表
	 * @see #webFluxConversionService()
	 */
	protected void addFormatters(FormatterRegistry registry) {
	}

	/**
	 * 返回一个用于适配响应式类型的{@link ReactiveAdapterRegistry}。
	 *
	 * @return 用于适配响应式类型的{@link ReactiveAdapterRegistry}
	 */
	@Bean
	public ReactiveAdapterRegistry webFluxAdapterRegistry() {
		return new ReactiveAdapterRegistry();
	}

	/**
	 * 返回一个全局的{@link Validator}实例，例如用于验证{@code @RequestBody}方法参数。
	 * <p>首先委托给{@link #getValidator()}方法。如果返回{@code null}，则在类路径中检查是否存在JSR-303实现，
	 * 然后创建一个{@code OptionalValidatorFactoryBean}。如果JSR-303实现不可用，则返回一个“无操作”的{@link Validator}。
	 *
	 * @return 全局的{@link Validator}实例
	 */
	@Bean
	public Validator webFluxValidator() {
		// 获取验证器
		Validator validator = getValidator();

		// 如果验证器为空
		if (validator == null) {
			// 检查是否存在 javax.validation.Validator 类
			if (ClassUtils.isPresent("javax.validation.Validator", getClass().getClassLoader())) {
				Class<?> clazz;
				try {
					// 尝试获取默认的验证器类
					String name = "org.springframework.validation.beanvalidation.OptionalValidatorFactoryBean";
					clazz = ClassUtils.forName(name, getClass().getClassLoader());
				} catch (ClassNotFoundException | LinkageError ex) {
					// 抛出 Bean 初始化异常，指示无法解析默认验证器类
					throw new BeanInitializationException("Failed to resolve default validator class", ex);
				}
				// 实例化默认的验证器类
				validator = (Validator) BeanUtils.instantiateClass(clazz);
			} else {
				// 如果不存在 javax.validation.Validator 类，则创建 NoOpValidator
				validator = new NoOpValidator();
			}
		}

		// 返回验证器
		return validator;
	}

	/**
	 * 覆盖此方法以提供自定义的{@link Validator}。
	 *
	 * @return 自定义的验证器（可为null）
	 */
	@Nullable
	protected Validator getValidator() {
		return null;
	}

	/**
	 * 覆盖此方法以提供自定义的{@link MessageCodesResolver}。
	 *
	 * @return 自定义的消息码解析器（可为null）
	 */
	@Nullable
	protected MessageCodesResolver getMessageCodesResolver() {
		return null;
	}

	/**
	 * 创建一个HandlerFunction适配器的Bean。
	 *
	 * @return HandlerFunction适配器实例
	 */
	@Bean
	public HandlerFunctionAdapter handlerFunctionAdapter() {
		return new HandlerFunctionAdapter();
	}

	/**
	 * 创建一个简单的Handler适配器的Bean。
	 *
	 * @return 简单的Handler适配器实例
	 */
	@Bean
	public SimpleHandlerAdapter simpleHandlerAdapter() {
		return new SimpleHandlerAdapter();
	}

	/**
	 * 配置WebFlux的WebSocket处理适配器。使用初始化的WebSocket服务来创建适配器，并降低适配器的优先级（为了向后兼容）。
	 *
	 * @return WebSocket处理适配器实例
	 */
	@Bean
	public WebSocketHandlerAdapter webFluxWebSocketHandlerAdapter() {
		WebSocketHandlerAdapter adapter = new WebSocketHandlerAdapter(initWebSocketService());

		// 暂时降低默认优先级，以保持向后兼容性
		int defaultOrder = adapter.getOrder();
		adapter.setOrder(defaultOrder + 1);

		return adapter;
	}


	/**
	 * 初始化WebSocket服务。如果未配置WebSocket服务，则尝试创建默认的握手WebSocket服务，如果创建失败则创建一个不支持升级策略的WebSocket服务。
	 *
	 * @return WebSocket服务实例
	 */
	private WebSocketService initWebSocketService() {
		WebSocketService service = getWebSocketService();
		if (service == null) {
			try {
				service = new HandshakeWebSocketService();
			} catch (IllegalStateException ex) {
				// 创建默认服务失败，可能是测试环境
				service = new NoUpgradeStrategyWebSocketService();
			}
		}
		return service;
	}

	/**
	 * 获取WebSocket服务。如果未配置，则返回null。
	 *
	 * @return WebSocket服务，如果未配置，则为null
	 */
	@Nullable
	protected WebSocketService getWebSocketService() {
		return null;
	}

	/**
	 * 处理ResponseEntity的结果处理器。
	 *
	 * @param reactiveAdapterRegistry 反应式适配器注册表
	 * @param serverCodecConfigurer   服务器编解码配置器
	 * @param contentTypeResolver     WebFlux内容类型解析器
	 * @return 处理ResponseEntity的结果处理器
	 */
	@Bean
	public ResponseEntityResultHandler responseEntityResultHandler(
			@Qualifier("webFluxAdapterRegistry") ReactiveAdapterRegistry reactiveAdapterRegistry,
			ServerCodecConfigurer serverCodecConfigurer,
			@Qualifier("webFluxContentTypeResolver") RequestedContentTypeResolver contentTypeResolver) {

		return new ResponseEntityResultHandler(serverCodecConfigurer.getWriters(),
				contentTypeResolver, reactiveAdapterRegistry);
	}

	/**
	 * 针对响应体的结果处理器。
	 *
	 * @param reactiveAdapterRegistry 反应式适配器注册表
	 * @param serverCodecConfigurer   服务器编解码配置器
	 * @param contentTypeResolver     WebFlux内容类型解析器
	 * @return 处理响应体的结果处理器
	 */
	@Bean
	public ResponseBodyResultHandler responseBodyResultHandler(
			@Qualifier("webFluxAdapterRegistry") ReactiveAdapterRegistry reactiveAdapterRegistry,
			ServerCodecConfigurer serverCodecConfigurer,
			@Qualifier("webFluxContentTypeResolver") RequestedContentTypeResolver contentTypeResolver) {

		return new ResponseBodyResultHandler(serverCodecConfigurer.getWriters(),
				contentTypeResolver, reactiveAdapterRegistry);
	}

	/**
	 * 视图解析结果处理器。
	 *
	 * @param reactiveAdapterRegistry 反应式适配器注册表
	 * @param contentTypeResolver     WebFlux内容类型解析器
	 * @return 视图解析结果处理器
	 */
	@Bean
	public ViewResolutionResultHandler viewResolutionResultHandler(
			@Qualifier("webFluxAdapterRegistry") ReactiveAdapterRegistry reactiveAdapterRegistry,
			@Qualifier("webFluxContentTypeResolver") RequestedContentTypeResolver contentTypeResolver) {

		// 获取视图解析器注册表
		ViewResolverRegistry registry = getViewResolverRegistry();

		// 获取视图解析器列表
		List<ViewResolver> resolvers = registry.getViewResolvers();

		// 创建 ViewResolutionResultHandler 实例
		ViewResolutionResultHandler handler = new ViewResolutionResultHandler(
				resolvers, contentTypeResolver, reactiveAdapterRegistry);

		// 设置默认视图和顺序
		handler.setDefaultViews(registry.getDefaultViews());
		handler.setOrder(registry.getOrder());

		// 返回配置完成的处理程序
		return handler;
	}

	/**
	 * 创建 ServerResponseResultHandler Bean 实例。
	 *
	 * @param serverCodecConfigurer 用于配置服务器编解码器的 ServerCodecConfigurer Bean
	 * @return ServerResponseResultHandler 实例
	 */
	@Bean
	public ServerResponseResultHandler serverResponseResultHandler(ServerCodecConfigurer serverCodecConfigurer) {
		// 获取视图解析器列表
		List<ViewResolver> resolvers = getViewResolverRegistry().getViewResolvers();

		// 创建 ServerResponseResultHandler 实例
		ServerResponseResultHandler handler = new ServerResponseResultHandler();

		// 设置消息写入器和视图解析器
		handler.setMessageWriters(serverCodecConfigurer.getWriters());
		handler.setViewResolvers(resolvers);

		// 返回配置完成的处理程序
		return handler;
	}

	/**
	 * 用于构建 ViewResolverRegistry 的回调方法。此方法是 final 的，使用 configureViewResolvers 来自定义视图解析器。
	 *
	 * @return ViewResolverRegistry 实例
	 * @see #configureViewResolvers
	 */
	protected final ViewResolverRegistry getViewResolverRegistry() {
		// 如果视图解析器注册表为空
		if (this.viewResolverRegistry == null) {
			// 创建视图解析器注册表并配置视图解析器
			this.viewResolverRegistry = new ViewResolverRegistry(this.applicationContext);
			configureViewResolvers(this.viewResolverRegistry);
		}

		// 返回视图解析器注册表
		return this.viewResolverRegistry;
	}

	/**
	 * 配置视图解析器以支持模板引擎。
	 *
	 * @param registry ViewResolverRegistry 实例
	 * @see ViewResolverRegistry
	 */
	protected void configureViewResolvers(ViewResolverRegistry registry) {
	}


	/**
	 * 一个空的 HandlerMapping 实现，继承自 AbstractHandlerMapping。
	 */
	private static final class EmptyHandlerMapping extends AbstractHandlerMapping {

		/**
		 * 获取处理程序的内部实现，此处返回一个空的 Mono。
		 *
		 * @param exchange 服务器Web交换对象
		 * @return 一个空的 Mono，表示没有找到对应的处理程序
		 */
		@Override
		public Mono<Object> getHandlerInternal(ServerWebExchange exchange) {
			return Mono.empty();
		}
	}


	/**
	 * 一个实现了 Validator 接口的类，用于进行空操作的验证器。
	 */
	private static final class NoOpValidator implements Validator {

		/**
		 * 判断是否支持验证给定的类。
		 *
		 * @param clazz 要验证的类
		 * @return 总是返回 false，表示不支持验证
		 */
		@Override
		public boolean supports(Class<?> clazz) {
			return false;
		}

		/**
		 * 验证目标对象，但不进行任何操作。
		 *
		 * @param target 要验证的对象（可为 null）
		 * @param errors 存储验证错误的对象
		 */
		@Override
		public void validate(@Nullable Object target, Errors errors) {
		}
	}


	/**
	 * 一个实现了 WebSocketService 接口的类，用于处理 WebSocket 请求的服务。
	 * 当没有合适的 RequestUpgradeStrategy 时，会抛出 IllegalStateException 异常。
	 */
	private static final class NoUpgradeStrategyWebSocketService implements WebSocketService {

		/**
		 * 处理 WebSocket 请求的方法。
		 *
		 * @param exchange         服务器 Web 交换对象
		 * @param webSocketHandler WebSocket 处理器
		 * @return 一个 Mono，表示异步处理的结果，当没有合适的 RequestUpgradeStrategy 时，会抛出 IllegalStateException 异常
		 */
		@Override
		public Mono<Void> handleRequest(ServerWebExchange exchange, WebSocketHandler webSocketHandler) {
			return Mono.error(new IllegalStateException("No suitable RequestUpgradeStrategy"));
		}
	}

}
