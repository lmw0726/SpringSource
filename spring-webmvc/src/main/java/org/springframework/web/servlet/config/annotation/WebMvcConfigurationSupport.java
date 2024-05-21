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

package org.springframework.web.servlet.config.annotation;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.SpringProperties;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.Formatter;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.http.MediaType;
import org.springframework.http.converter.*;
import org.springframework.http.converter.cbor.MappingJackson2CborHttpMessageConverter;
import org.springframework.http.converter.feed.AtomFeedHttpMessageConverter;
import org.springframework.http.converter.feed.RssChannelHttpMessageConverter;
import org.springframework.http.converter.json.*;
import org.springframework.http.converter.smile.MappingJackson2SmileHttpMessageConverter;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.PathMatcher;
import org.springframework.validation.Errors;
import org.springframework.validation.MessageCodesResolver;
import org.springframework.validation.Validator;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.method.support.CompositeUriComponentsContributor;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.servlet.*;
import org.springframework.web.servlet.function.support.HandlerFunctionAdapter;
import org.springframework.web.servlet.function.support.RouterFunctionMapping;
import org.springframework.web.servlet.handler.*;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;
import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.servlet.mvc.HttpRequestHandlerAdapter;
import org.springframework.web.servlet.mvc.SimpleControllerHandlerAdapter;
import org.springframework.web.servlet.mvc.annotation.ResponseStatusExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.*;
import org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver;
import org.springframework.web.servlet.resource.ResourceUrlProvider;
import org.springframework.web.servlet.resource.ResourceUrlProviderExposingInterceptor;
import org.springframework.web.servlet.support.SessionFlashMapManager;
import org.springframework.web.servlet.theme.FixedThemeResolver;
import org.springframework.web.servlet.view.DefaultRequestToViewNameTranslator;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.ViewResolverComposite;
import org.springframework.web.util.UrlPathHelper;
import org.springframework.web.util.pattern.PathPatternParser;

import javax.servlet.ServletContext;
import java.util.*;

/**
 * 提供MVC Java配置背后的配置的主类。通常通过在应用程序的{@link Configuration @Configuration}
 * 类中添加 {@link EnableWebMvc @EnableWebMvc} 进行导入。另一种更高级的选项是直接从此类继承
 * 并根据需要重写方法，记得在子类中添加 {@link Configuration @Configuration} 和在重写的
 * {@link Bean @Bean} 方法中添加 {@link Bean @Bean}。
 * 更多详情请参阅 {@link EnableWebMvc @EnableWebMvc} 的javadoc。
 *
 * <p>此类注册以下 {@link HandlerMapping HandlerMappings}:</p>
 * <ul>
 * <li>{@link RequestMappingHandlerMapping}
 * 按顺序0，用于将请求映射到注释的控制器方法。
 * <li>{@link HandlerMapping}
 * 按顺序1，将URL路径直接映射到视图名称。
 * <li>{@link BeanNameUrlHandlerMapping}
 * 按顺序2，将URL路径映射到控制器bean名称。
 * <li>{@link RouterFunctionMapping}
 * 按顺序3，用于映射{@linkplain org.springframework.web.servlet.function.RouterFunction 路由函数}。
 * <li>{@link HandlerMapping}
 * 按顺序{@code Integer.MAX_VALUE-1}，用于服务静态资源请求。
 * <li>{@link HandlerMapping}
 * 按顺序{@code Integer.MAX_VALUE}，将请求转发到默认servlet。
 * </ul>
 *
 * <p>注册以下 {@link HandlerAdapter HandlerAdapters}:</p>
 * <ul>
 * <li>{@link RequestMappingHandlerAdapter}
 * 用于处理带注释的控制器方法的请求。
 * <li>{@link HttpRequestHandlerAdapter}
 * 用于处理带有 {@link HttpRequestHandler HttpRequestHandlers} 的请求。
 * <li>{@link SimpleControllerHandlerAdapter}
 * 用于处理带有基于接口的 {@link Controller Controllers} 的请求。
 * <li>{@link HandlerFunctionAdapter}
 * 用于处理带有{@linkplain org.springframework.web.servlet.function.RouterFunction 路由函数}的请求。
 * </ul>
 *
 * <p>注册一个 {@link HandlerExceptionResolverComposite}，其包含以下异常解析链：</p>
 * <ul>
 * <li>{@link ExceptionHandlerExceptionResolver} 用于通过
 * {@link org.springframework.web.bind.annotation.ExceptionHandler} 方法处理异常。
 * <li>{@link ResponseStatusExceptionResolver} 用于处理带有
 * {@link org.springframework.web.bind.annotation.ResponseStatus} 注解的异常。
 * <li>{@link DefaultHandlerExceptionResolver} 用于解析已知的Spring异常类型。
 * </ul>
 *
 * <p>注册一个 {@link AntPathMatcher} 和一个 {@link UrlPathHelper}，用于以下组件：</p>
 * <ul>
 * <li> {@link RequestMappingHandlerMapping}
 * <li> 用于 ViewControllers 的 {@link HandlerMapping}
 * <li> 用于服务资源的 {@link HandlerMapping}
 * </ul>
 * 注意，这些bean可以通过 {@link PathMatchConfigurer} 进行配置。
 *
 * <p>默认情况下，{@link RequestMappingHandlerAdapter} 和
 * {@link ExceptionHandlerExceptionResolver} 配置了以下默认实例：</p>
 * <ul>
 * <li>一个 {@link ContentNegotiationManager}
 * <li>一个 {@link DefaultFormattingConversionService}
 * <li>如果类路径中有JSR-303实现，则为一个 {@link org.springframework.validation.beanvalidation.OptionalValidatorFactoryBean}
 * <li>根据类路径中可用的第三方库，配置一系列 {@link HttpMessageConverter HttpMessageConverters}。
 * </ul>
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author Sebastien Deleuze
 * @see EnableWebMvc
 * @see WebMvcConfigurer
 * @since 3.1
 */
public class WebMvcConfigurationSupport implements ApplicationContextAware, ServletContextAware {

	/**
	 * 由 {@code spring.xml.ignore} 系统属性控制的布尔标志，指示Spring忽略XML，即不初始化与XML相关的基础设施。
	 * <p>默认值为 "false"。
	 */
	private static final boolean shouldIgnoreXml = SpringProperties.getFlag("spring.xml.ignore");

	/**
	 * 是否存在 rome 包的标志。
	 */
	private static final boolean romePresent;

	/**
	 * 是否存在 jaxb2 包的标志。
	 */
	private static final boolean jaxb2Present;

	/**
	 * 是否存在 jackson2 包的标志。
	 */
	private static final boolean jackson2Present;

	/**
	 * 是否存在 jackson2Xml 包的标志。
	 */
	private static final boolean jackson2XmlPresent;

	/**
	 * 是否存在 jackson2Smile 包的标志。
	 */
	private static final boolean jackson2SmilePresent;

	/**
	 * 是否存在 jackson2Cbor 包的标志。
	 */
	private static final boolean jackson2CborPresent;

	/**
	 * 是否存在 gson 包的标志。
	 */
	private static final boolean gsonPresent;

	/**
	 * 是否存在 JSON-B 库的标志。
	 */
	private static final boolean jsonbPresent;

	/**
	 * 是否存在 Kotlin Serialization JSON 库的标志。
	 */
	private static final boolean kotlinSerializationJsonPresent;

	static {
		// 获取WebMvcConfigurationSupport类的类加载器
		ClassLoader classLoader = WebMvcConfigurationSupport.class.getClassLoader();
		// 检查Rome库是否存在
		romePresent = ClassUtils.isPresent("com.rometools.rome.feed.WireFeed", classLoader);
		// 检查JAXB2库是否存在
		jaxb2Present = ClassUtils.isPresent("javax.xml.bind.Binder", classLoader);
		// 检查Jackson2库是否存在，同时检查ObjectMapper和JsonGenerator类是否存在
		jackson2Present = ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper", classLoader) &&
				ClassUtils.isPresent("com.fasterxml.jackson.core.JsonGenerator", classLoader);
		// 检查Jackson2 XML库是否存在
		jackson2XmlPresent = ClassUtils.isPresent("com.fasterxml.jackson.dataformat.xml.XmlMapper", classLoader);
		// 检查Jackson2 Smile库是否存在
		jackson2SmilePresent = ClassUtils.isPresent("com.fasterxml.jackson.dataformat.smile.SmileFactory", classLoader);
		// 检查Jackson2 CBOR库是否存在
		jackson2CborPresent = ClassUtils.isPresent("com.fasterxml.jackson.dataformat.cbor.CBORFactory", classLoader);
		// 检查Gson库是否存在
		gsonPresent = ClassUtils.isPresent("com.google.gson.Gson", classLoader);
		// 检查JSON-B库是否存在
		jsonbPresent = ClassUtils.isPresent("javax.json.bind.Jsonb", classLoader);
		// 检查Kotlin Serialization JSON库是否存在
		kotlinSerializationJsonPresent = ClassUtils.isPresent("kotlinx.serialization.json.Json", classLoader);
	}

	/**
	 * 应用上下文
	 */
	@Nullable
	private ApplicationContext applicationContext;

	/**
	 * Servlet上下文
	 */
	@Nullable
	private ServletContext servletContext;

	/**
	 * 拦截器列表
	 */
	@Nullable
	private List<Object> interceptors;

	/**
	 * 路径匹配配置器
	 */
	@Nullable
	private PathMatchConfigurer pathMatchConfigurer;

	/**
	 * 内容协商管理器
	 */
	@Nullable
	private ContentNegotiationManager contentNegotiationManager;
	/**
	 * 参数解析器列表
	 */
	@Nullable
	private List<HandlerMethodArgumentResolver> argumentResolvers;

	/**
	 * 返回值处理器列表
	 */
	@Nullable
	private List<HandlerMethodReturnValueHandler> returnValueHandlers;

	/**
	 * 消息转换器列表
	 */
	@Nullable
	private List<HttpMessageConverter<?>> messageConverters;

	/**
	 * 路径模式 - 跨域配置 映射
	 */
	@Nullable
	private Map<String, CorsConfiguration> corsConfigurations;

	/**
	 * 异步支持配置器
	 */
	@Nullable
	private AsyncSupportConfigurer asyncSupportConfigurer;


	/**
	 * 设置Spring {@link ApplicationContext}，例如用于资源加载。
	 */
	@Override
	public void setApplicationContext(@Nullable ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	/**
	 * 返回关联的Spring {@link ApplicationContext}。
	 *
	 * @since 4.2
	 */
	@Nullable
	public final ApplicationContext getApplicationContext() {
		return this.applicationContext;
	}

	/**
	 * 设置 {@link javax.servlet.ServletContext}，例如用于资源处理、查找文件扩展名等。
	 */
	@Override
	public void setServletContext(@Nullable ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	/**
	 * 返回关联的 {@link javax.servlet.ServletContext}。
	 *
	 * @since 4.2
	 */
	@Nullable
	public final ServletContext getServletContext() {
		return this.servletContext;
	}


	/**
	 * 返回一个 {@link RequestMappingHandlerMapping}，按顺序为0，用于将请求映射到注释的控制器。
	 */
	@Bean
	@SuppressWarnings("deprecation")
	public RequestMappingHandlerMapping requestMappingHandlerMapping(
			@Qualifier("mvcContentNegotiationManager") ContentNegotiationManager contentNegotiationManager,
			@Qualifier("mvcConversionService") FormattingConversionService conversionService,
			@Qualifier("mvcResourceUrlProvider") ResourceUrlProvider resourceUrlProvider) {

		// 创建 请求映射处理程序映射 实例
		RequestMappingHandlerMapping mapping = createRequestMappingHandlerMapping();
		// 设置 请求映射处理程序映射 的顺序为0
		mapping.setOrder(0);
		// 设置拦截器
		mapping.setInterceptors(getInterceptors(conversionService, resourceUrlProvider));
		// 设置内容协商管理器
		mapping.setContentNegotiationManager(contentNegotiationManager);
		// 设置跨域配置
		mapping.setCorsConfigurations(getCorsConfigurations());

		// 获取路径匹配配置器
		PathMatchConfigurer pathConfig = getPathMatchConfigurer();
		if (pathConfig.getPatternParser() != null) {
			// 如果存在 路径模式解析器 ，则设置 路径模式解析器
			mapping.setPatternParser(pathConfig.getPatternParser());
		} else {
			// 否则，设置 默认的URL路径助手和 默认的路径匹配器
			mapping.setUrlPathHelper(pathConfig.getUrlPathHelperOrDefault());
			mapping.setPathMatcher(pathConfig.getPathMatcherOrDefault());

			// 获取是否使用后缀模式匹配
			Boolean useSuffixPatternMatch = pathConfig.isUseSuffixPatternMatch();
			if (useSuffixPatternMatch != null) {
				// 设置是否使用后缀模式匹配
				mapping.setUseSuffixPatternMatch(useSuffixPatternMatch);
			}
			// 获取是否仅使用已注册的后缀模式匹配
			Boolean useRegisteredSuffixPatternMatch = pathConfig.isUseRegisteredSuffixPatternMatch();
			if (useRegisteredSuffixPatternMatch != null) {
				// 设置是否仅使用已注册的后缀模式匹配
				mapping.setUseRegisteredSuffixPatternMatch(useRegisteredSuffixPatternMatch);
			}
		}
		// 获取是否使用尾部斜杠匹配
		Boolean useTrailingSlashMatch = pathConfig.isUseTrailingSlashMatch();
		if (useTrailingSlashMatch != null) {
			// 设置是否使用尾部斜杠匹配
			mapping.setUseTrailingSlashMatch(useTrailingSlashMatch);
		}
		if (pathConfig.getPathPrefixes() != null) {
			// 如果存在路径前缀，则设置路径前缀
			mapping.setPathPrefixes(pathConfig.getPathPrefixes());
		}

		return mapping;
	}

	/**
	 * 插入自定义 {@link RequestMappingHandlerMapping} 子类的受保护方法。
	 *
	 * @since 4.0
	 */
	protected RequestMappingHandlerMapping createRequestMappingHandlerMapping() {
		return new RequestMappingHandlerMapping();
	}

	/**
	 * 提供访问用于配置 {@link HandlerMapping} 实例的共享处理器拦截器。
	 * <p>此方法不能被重写；请使用 {@link #addInterceptors}。
	 */
	protected final Object[] getInterceptors(
			FormattingConversionService mvcConversionService,
			ResourceUrlProvider mvcResourceUrlProvider) {

		// 如果拦截器数组为空
		if (this.interceptors == null) {
			// 创建InterceptorRegistry实例
			InterceptorRegistry registry = new InterceptorRegistry();
			// 添加拦截器
			addInterceptors(registry);
			// 添加 转换服务暴露拦截器
			registry.addInterceptor(new ConversionServiceExposingInterceptor(mvcConversionService));
			// 添加 资源URL提供者暴露拦截器
			registry.addInterceptor(new ResourceUrlProviderExposingInterceptor(mvcResourceUrlProvider));
			// 获取所有拦截器并赋值给 当前类的拦截器数组
			this.interceptors = registry.getInterceptors();
		}
		// 返回拦截器数组
		return this.interceptors.toArray();
	}

	/**
	 * 重写此方法以添加Spring MVC拦截器，用于控制器调用的前后处理。
	 *
	 * @see InterceptorRegistry
	 */
	protected void addInterceptors(InterceptorRegistry registry) {
	}

	/**
	 * 构建 {@link PathMatchConfigurer} 的回调方法。
	 * 委托给 {@link #configurePathMatch}。
	 *
	 * @since 4.1
	 */
	protected PathMatchConfigurer getPathMatchConfigurer() {
		// 如果路径匹配配置器为空
		if (this.pathMatchConfigurer == null) {
			// 创建一个新的路径匹配配置器对象
			this.pathMatchConfigurer = new PathMatchConfigurer();
			// 配置路径匹配器
			configurePathMatch(this.pathMatchConfigurer);
		}
		// 返回路径匹配配置器对象
		return this.pathMatchConfigurer;
	}

	/**
	 * 重写此方法以配置路径匹配选项。
	 *
	 * @see PathMatchConfigurer
	 * @since 4.0.3
	 */
	protected void configurePathMatch(PathMatchConfigurer configurer) {
	}

	/**
	 * 返回一个全局的 {@link PathPatternParser} 实例，用于解析与 {@link org.springframework.http.server.RequestPath} 匹配的模式。
	 * 返回的实例可以通过 {@link #configurePathMatch(PathMatchConfigurer)} 进行配置。
	 *
	 * @since 5.3.4
	 */
	@Bean
	public PathPatternParser mvcPatternParser() {
		return getPathMatchConfigurer().getPatternParserOrDefault();
	}

	/**
	 * 返回一个全局的 {@link UrlPathHelper} 实例，该实例用于解析应用程序的请求映射路径。
	 * 该实例可以通过 {@link #configurePathMatch(PathMatchConfigurer)} 进行配置。
	 * <p><b>注意：</b> 仅在未启用解析模式时使用 {@link PathMatchConfigurer#setPatternParser}。
	 *
	 * @since 4.1
	 */
	@Bean
	public UrlPathHelper mvcUrlPathHelper() {
		return getPathMatchConfigurer().getUrlPathHelperOrDefault();
	}

	/**
	 * 返回一个全局的 {@link PathMatcher} 实例，用于使用字符串模式进行 URL 路径匹配。
	 * 返回的实例可以通过 {@link #configurePathMatch(PathMatchConfigurer)} 进行配置。
	 * <p><b>注意：</b> 仅在未启用解析模式时使用 {@link PathMatchConfigurer#setPatternParser}。
	 *
	 * @since 4.1
	 */
	@Bean
	public PathMatcher mvcPathMatcher() {
		return getPathMatchConfigurer().getPathMatcherOrDefault();
	}

	/**
	 * 返回一个 {@link ContentNegotiationManager} 实例，用于确定给定请求中请求的 {@linkplain MediaType 媒体类型}。
	 */
	@Bean
	public ContentNegotiationManager mvcContentNegotiationManager() {
		// 如果内容协商管理器为空
		if (this.contentNegotiationManager == null) {
			// 创建内容协商配置器对象，传入 Servlet 上下文
			ContentNegotiationConfigurer configurer = new ContentNegotiationConfigurer(this.servletContext);
			// 设置媒体类型为默认媒体类型
			configurer.mediaTypes(getDefaultMediaTypes());
			// 配置内容协商
			configureContentNegotiation(configurer);
			// 构建内容协商管理器并赋值给内容协商管理器属性
			this.contentNegotiationManager = configurer.buildContentNegotiationManager();
		}
		// 返回内容协商管理器对象
		return this.contentNegotiationManager;
	}

	protected Map<String, MediaType> getDefaultMediaTypes() {
		// 创建一个具有初始容量为 4 的哈希映射
		Map<String, MediaType> map = new HashMap<>(4);
		// 如果 Rome库 存在
		if (romePresent) {
			// 将 "atom" 映射到 APPLICATION_ATOM_XML 媒体类型
			map.put("atom", MediaType.APPLICATION_ATOM_XML);
			// 将 "rss" 映射到 APPLICATION_RSS_XML 媒体类型
			map.put("rss", MediaType.APPLICATION_RSS_XML);
		}
		// 如果不应忽略 XML 并且（JAXB2库 存在或 Jackson2 XML库 存在）
		if (!shouldIgnoreXml && (jaxb2Present || jackson2XmlPresent)) {
			// 将 "xml" 映射到 APPLICATION_XML 媒体类型
			map.put("xml", MediaType.APPLICATION_XML);
		}
		// 如果 Jackson2 存在或 Gson 存在或 Jsonb 存在
		if (jackson2Present || gsonPresent || jsonbPresent) {
			// 将 "json" 映射到 APPLICATION_JSON 媒体类型
			map.put("json", MediaType.APPLICATION_JSON);
		}
		// 如果 Jackson2 Smile库 存在
		if (jackson2SmilePresent) {
			// 将 "smile" 映射到 "application/x-jackson-smile" 媒体类型
			map.put("smile", MediaType.valueOf("application/x-jackson-smile"));
		}
		// 如果 Jackson2 CBOR库 存在
		if (jackson2CborPresent) {
			// 将 "cbor" 映射到 APPLICATION_CBOR 媒体类型
			map.put("cbor", MediaType.APPLICATION_CBOR);
		}
		// 返回映射
		return map;
	}

	/**
	 * 覆盖此方法以配置内容协商。
	 *
	 * @see DefaultServletHandlerConfigurer
	 */
	protected void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
	}

	/**
	 * 返回一个排序为1的处理程序映射，将URL路径直接映射到视图名称。要配置视图控制器，请覆盖 {@link #addViewControllers} 方法。
	 */
	@Bean
	@Nullable
	public HandlerMapping viewControllerHandlerMapping(
			@Qualifier("mvcConversionService") FormattingConversionService conversionService,
			@Qualifier("mvcResourceUrlProvider") ResourceUrlProvider resourceUrlProvider) {

		// 使用 应用程序上下文 创建一个 视图控制器注册表
		ViewControllerRegistry registry = new ViewControllerRegistry(this.applicationContext);
		// 添加视图控制器到注册表中
		addViewControllers(registry);

		// 构建处理程序映射
		AbstractHandlerMapping handlerMapping = registry.buildHandlerMapping();
		// 如果处理程序映射为空
		if (handlerMapping == null) {
			// 返回空
			return null;
		}

		// 获取路径匹配配置器
		PathMatchConfigurer pathConfig = getPathMatchConfigurer();
		// 如果路径配置的模式解析器不为空
		if (pathConfig.getPatternParser() != null) {
			// 设置处理程序映射的模式解析器
			handlerMapping.setPatternParser(pathConfig.getPatternParser());
		} else {
			// 否则设置 处理程序映射 的 URL路径助手。如果不存在，则设置默认的URL路径助手
			handlerMapping.setUrlPathHelper(pathConfig.getUrlPathHelperOrDefault());
			// 设置 处理程序映射 的 路径匹配器。如果不存在，则设置默认的路径匹配器
			handlerMapping.setPathMatcher(pathConfig.getPathMatcherOrDefault());
		}

		// 设置处理程序映射的拦截器
		handlerMapping.setInterceptors(getInterceptors(conversionService, resourceUrlProvider));
		// 设置处理程序映射的跨域配置
		handlerMapping.setCorsConfigurations(getCorsConfigurations());

		// 返回处理程序映射
		return handlerMapping;
	}

	/**
	 * 覆盖此方法以添加视图控制器。
	 *
	 * @see ViewControllerRegistry
	 */
	protected void addViewControllers(ViewControllerRegistry registry) {
	}

	/**
	 * 返回一个排序为2的 {@link BeanNameUrlHandlerMapping}，将 URL 路径映射到控制器 bean 名称。
	 */
	@Bean
	public BeanNameUrlHandlerMapping beanNameHandlerMapping(
			@Qualifier("mvcConversionService") FormattingConversionService conversionService,
			@Qualifier("mvcResourceUrlProvider") ResourceUrlProvider resourceUrlProvider) {

		// 创建 Bean名称URL处理器映射 实例
		BeanNameUrlHandlerMapping mapping = new BeanNameUrlHandlerMapping();
		// 设置处理器映射的顺序为 2
		mapping.setOrder(2);

		// 获取路径匹配配置器
		PathMatchConfigurer pathConfig = getPathMatchConfigurer();
		// 如果路径配置的模式解析器不为空
		if (pathConfig.getPatternParser() != null) {
			// 设置处理器映射的模式解析器
			mapping.setPatternParser(pathConfig.getPatternParser());
		} else {
			// 否则设置处理器映射的 URL 路径助手和路径匹配器为默认值
			mapping.setUrlPathHelper(pathConfig.getUrlPathHelperOrDefault());
			mapping.setPathMatcher(pathConfig.getPathMatcherOrDefault());
		}

		// 设置处理器映射的拦截器
		mapping.setInterceptors(getInterceptors(conversionService, resourceUrlProvider));
		// 设置处理器映射的跨域配置
		mapping.setCorsConfigurations(getCorsConfigurations());
		// 返回处理器映射实例
		return mapping;
	}

	/**
	 * 返回一个排序为3的 {@link RouterFunctionMapping}，用于映射 {@linkplain org.springframework.web.servlet.function.RouterFunction 路由函数}。
	 * 考虑重写以下更精细的方法之一：
	 * <ul>
	 * <li>{@link #addInterceptors} 添加处理程序拦截器。
	 * <li>{@link #addCorsMappings} 配置跨源请求处理。
	 * <li>{@link #configureMessageConverters} 添加自定义消息转换器。
	 * <li>{@link #configurePathMatch(PathMatchConfigurer)} 自定义 {@link PathPatternParser}。
	 * </ul>
	 *
	 * @since 5.2
	 */
	@Bean
	public RouterFunctionMapping routerFunctionMapping(
			@Qualifier("mvcConversionService") FormattingConversionService conversionService,
			@Qualifier("mvcResourceUrlProvider") ResourceUrlProvider resourceUrlProvider) {

		// 创建 路由器功能映射 实例
		RouterFunctionMapping mapping = new RouterFunctionMapping();
		// 设置 路由器功能映射 的顺序为 3
		mapping.setOrder(3);
		// 设置 路由器功能映射 的 拦截器
		mapping.setInterceptors(getInterceptors(conversionService, resourceUrlProvider));
		// 设置 路由器功能映射 的 跨域配置
		mapping.setCorsConfigurations(getCorsConfigurations());
		// 设置 路由器功能映射 的 消息转换器
		mapping.setMessageConverters(getMessageConverters());

		// 获取路径匹配配置器中的 模式解析器
		PathPatternParser patternParser = getPathMatchConfigurer().getPatternParser();
		// 如果模式解析器不为空
		if (patternParser != null) {
			// 设置 路由器功能映射 的 模式解析器
			mapping.setPatternParser(patternParser);
		}

		// 返回 路由器功能映射 实例
		return mapping;
	}

	/**
	 * 返回一个排序为 Integer.MAX_VALUE-1 的处理程序映射，具有映射的资源处理程序。要配置资源处理，请覆盖 {@link #addResourceHandlers} 方法。
	 */
	@Bean
	@Nullable
	public HandlerMapping resourceHandlerMapping(
			@Qualifier("mvcContentNegotiationManager") ContentNegotiationManager contentNegotiationManager,
			@Qualifier("mvcConversionService") FormattingConversionService conversionService,
			@Qualifier("mvcResourceUrlProvider") ResourceUrlProvider resourceUrlProvider) {

		// 断言应用程序上下文不为空
		Assert.state(this.applicationContext != null, "未设置 ApplicationContext");
		// 断言 Servlet 上下文不为空
		Assert.state(this.servletContext != null, "未设置 ServletContext");

		// 获取路径匹配配置器
		PathMatchConfigurer pathConfig = getPathMatchConfigurer();

		// 创建资源处理器注册表
		ResourceHandlerRegistry registry = new ResourceHandlerRegistry(this.applicationContext,
				this.servletContext, contentNegotiationManager, pathConfig.getUrlPathHelper());
		// 添加资源处理器到注册表中
		addResourceHandlers(registry);

		// 获取注册表中的处理器映射
		AbstractHandlerMapping handlerMapping = registry.getHandlerMapping();
		// 如果处理器映射为空
		if (handlerMapping == null) {
			// 返回空
			return null;
		}

		// 如果路径配置的模式解析器不为空
		if (pathConfig.getPatternParser() != null) {
			// 设置处理器映射的模式解析器
			handlerMapping.setPatternParser(pathConfig.getPatternParser());
		} else {
			// 否则设置处理器映射的 URL路径助手。如果不存在，则设置默认的URL路径助手
			handlerMapping.setUrlPathHelper(pathConfig.getUrlPathHelperOrDefault());
			// 设置处理器映射的 路径匹配器。如果不存在，则设置默认的路径匹配器
			handlerMapping.setPathMatcher(pathConfig.getPathMatcherOrDefault());
		}

		// 设置处理器映射的拦截器
		handlerMapping.setInterceptors(getInterceptors(conversionService, resourceUrlProvider));
		// 设置处理器映射的跨域配置
		handlerMapping.setCorsConfigurations(getCorsConfigurations());

		// 返回处理器映射
		return handlerMapping;
	}

	/**
	 * 覆盖此方法以添加用于服务静态资源的资源处理程序。
	 *
	 * @see ResourceHandlerRegistry
	 */
	protected void addResourceHandlers(ResourceHandlerRegistry registry) {
	}

	/**
	 * 用于 MVC 调度程序的 {@link ResourceUrlProvider} bean。
	 *
	 * @since 4.1
	 */
	@Bean
	public ResourceUrlProvider mvcResourceUrlProvider() {
		// 创建资源 URL 提供者实例
		ResourceUrlProvider urlProvider = new ResourceUrlProvider();
		// 设置资源 URL 提供者的 URL 路径助手为路径匹配配置器中的默认 URL 路径助手
		urlProvider.setUrlPathHelper(getPathMatchConfigurer().getUrlPathHelperOrDefault());
		// 设置资源 URL 提供者的路径匹配器为路径匹配配置器中的默认路径匹配器
		urlProvider.setPathMatcher(getPathMatchConfigurer().getPathMatcherOrDefault());
		// 返回资源 URL 提供者实例
		return urlProvider;
	}

	/**
	 * 返回排序为 Integer.MAX_VALUE 的处理程序映射，具有映射的默认 Servlet 处理程序。要配置 "默认" Servlet 处理，请覆盖 {@link #configureDefaultServletHandling} 方法。
	 */
	@Bean
	@Nullable
	public HandlerMapping defaultServletHandlerMapping() {
		// 断言 Servlet 上下文不为空
		Assert.state(this.servletContext != null, "未设置 ServletContext");

		// 创建默认 Servlet 处理器配置器
		DefaultServletHandlerConfigurer configurer = new DefaultServletHandlerConfigurer(this.servletContext);
		// 配置默认 Servlet 处理
		configureDefaultServletHandling(configurer);

		// 构建处理程序映射并返回
		return configurer.buildHandlerMapping();
	}

	/**
	 * 覆盖此方法以配置 "默认" Servlet 处理。
	 *
	 * @see DefaultServletHandlerConfigurer
	 */
	protected void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
	}

	/**
	 * 返回一个 {@link RequestMappingHandlerAdapter}，用于通过注释的控制器方法处理请求。考虑重写以下其他更细粒度的方法之一：
	 * <ul>
	 * <li>{@link #addArgumentResolvers} 添加自定义参数解析器。
	 * <li>{@link #addReturnValueHandlers} 添加自定义返回值处理程序。
	 * <li>{@link #configureMessageConverters} 添加自定义消息转换器。
	 * </ul>
	 */
	@Bean
	public RequestMappingHandlerAdapter requestMappingHandlerAdapter(
			@Qualifier("mvcContentNegotiationManager") ContentNegotiationManager contentNegotiationManager,
			@Qualifier("mvcConversionService") FormattingConversionService conversionService,
			@Qualifier("mvcValidator") Validator validator) {

		// 创建请求映射处理适配器
		RequestMappingHandlerAdapter adapter = createRequestMappingHandlerAdapter();
		// 设置内容协商管理器
		adapter.setContentNegotiationManager(contentNegotiationManager);
		// 设置消息转换器
		adapter.setMessageConverters(getMessageConverters());
		// 设置 Web 绑定初始化器
		adapter.setWebBindingInitializer(getConfigurableWebBindingInitializer(conversionService, validator));
		// 设置自定义参数解析器
		adapter.setCustomArgumentResolvers(getArgumentResolvers());
		// 设置自定义返回值处理器
		adapter.setCustomReturnValueHandlers(getReturnValueHandlers());

		// 如果存在 Jackson2库
		if (jackson2Present) {
			// 设置请求体通知为 JsonViewRequestBodyAdvice 实例
			adapter.setRequestBodyAdvice(Collections.singletonList(new JsonViewRequestBodyAdvice()));
			// 设置响应体通知为 JsonViewResponseBodyAdvice 实例
			adapter.setResponseBodyAdvice(Collections.singletonList(new JsonViewResponseBodyAdvice()));
		}

		// 获取异步支持配置器
		AsyncSupportConfigurer configurer = getAsyncSupportConfigurer();
		// 如果任务执行器不为空
		if (configurer.getTaskExecutor() != null) {
			// 设置任务执行器
			adapter.setTaskExecutor(configurer.getTaskExecutor());
		}
		// 如果超时时间不为空
		if (configurer.getTimeout() != null) {
			// 设置异步请求超时时间
			adapter.setAsyncRequestTimeout(configurer.getTimeout());
		}
		// 设置可调用拦截器
		adapter.setCallableInterceptors(configurer.getCallableInterceptors());
		// 设置延迟结果拦截器
		adapter.setDeferredResultInterceptors(configurer.getDeferredResultInterceptors());

		// 返回请求映射处理适配器
		return adapter;
	}

	/**
	 * 用于插入 {@link RequestMappingHandlerAdapter} 的自定义子类的受保护方法。
	 *
	 * @since 4.3
	 */
	protected RequestMappingHandlerAdapter createRequestMappingHandlerAdapter() {
		return new RequestMappingHandlerAdapter();
	}

	/**
	 * 返回一个 {@link HandlerFunctionAdapter}，用于通过 {@linkplain org.springframework.web.servlet.function.HandlerFunction 处理函数} 处理请求。
	 *
	 * @since 5.2
	 */
	@Bean
	public HandlerFunctionAdapter handlerFunctionAdapter() {
		// 创建处理函数适配器
		HandlerFunctionAdapter adapter = new HandlerFunctionAdapter();

		// 获取异步支持配置器
		AsyncSupportConfigurer configurer = getAsyncSupportConfigurer();
		// 如果超时时间不为空
		if (configurer.getTimeout() != null) {
			// 设置异步请求超时时间
			adapter.setAsyncRequestTimeout(configurer.getTimeout());
		}

		// 返回处理函数适配器
		return adapter;
	}

	/**
	 * 返回用于初始化所有 {@link WebDataBinder} 实例的 {@link ConfigurableWebBindingInitializer}。
	 */
	protected ConfigurableWebBindingInitializer getConfigurableWebBindingInitializer(
			FormattingConversionService mvcConversionService, Validator mvcValidator) {

		// 创建可配置的 Web 绑定初始化器
		ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
		// 设置转换服务
		initializer.setConversionService(mvcConversionService);
		// 设置验证器
		initializer.setValidator(mvcValidator);
		// 获取消息代码解析器
		MessageCodesResolver messageCodesResolver = getMessageCodesResolver();
		// 如果消息代码解析器不为空
		if (messageCodesResolver != null) {
			// 设置消息代码解析器
			initializer.setMessageCodesResolver(messageCodesResolver);
		}
		// 返回可配置的 Web 绑定初始化器
		return initializer;
	}

	/**
	 * 覆盖此方法以提供自定义的 {@link MessageCodesResolver}。
	 */
	@Nullable
	protected MessageCodesResolver getMessageCodesResolver() {
		return null;
	}

	/**
	 * 返回一个 {@link FormattingConversionService} 以与注解控制器一起使用。
	 * <p>可以通过添加 {@link #addFormatters} 方法来覆盖此方法。
	 */
	@Bean
	public FormattingConversionService mvcConversionService() {
		// 创建默认格式转换服务
		FormattingConversionService conversionService = new DefaultFormattingConversionService();
		// 向格式转换服务添加格式化器
		addFormatters(conversionService);
		// 返回格式转换服务
		return conversionService;
	}

	/**
	 * 覆盖此方法以向通用的 {@link FormattingConversionService} 添加自定义的 {@link Converter} 和/或 {@link Formatter} 代理。
	 *
	 * @see #mvcConversionService()
	 */
	protected void addFormatters(FormatterRegistry registry) {
	}

	/**
	 * 返回一个全局的 {@link Validator} 实例，用于验证 {@code @ModelAttribute} 和 {@code @RequestBody} 方法参数。
	 * 首先委托给 {@link #getValidator()}，如果返回 {@code null}，则检查类路径是否存在 JSR-303 实现，然后创建一个 {@code OptionalValidatorFactoryBean}。
	 * 如果 JSR-303 实现不可用，则返回一个空操作的 {@link Validator}。
	 */
	@Bean
	public Validator mvcValidator() {
		// 获取验证器
		Validator validator = getValidator();
		// 如果验证器为空
		if (validator == null) {
			// 如果 javax.validation.Validator 存在于类路径中
			if (ClassUtils.isPresent("javax.validation.Validator", getClass().getClassLoader())) {
				Class<?> clazz;
				try {
					// 尝试加载默认的验证器工厂类
					String className = "org.springframework.validation.beanvalidation.OptionalValidatorFactoryBean";
					clazz = ClassUtils.forName(className, WebMvcConfigurationSupport.class.getClassLoader());
				} catch (ClassNotFoundException | LinkageError ex) {
					// 如果加载失败，抛出 BeanInitializationException 异常
					throw new BeanInitializationException("Failed to resolve default validator class", ex);
				}
				// 实例化验证器工厂类
				validator = (Validator) BeanUtils.instantiateClass(clazz);
			} else {
				// 否则，创建一个 NoOpValidator 的实例
				validator = new NoOpValidator();
			}
		}
		// 返回验证器
		return validator;
	}

	/**
	 * 覆盖此方法以提供自定义的 {@link Validator}。
	 */
	@Nullable
	protected Validator getValidator() {
		return null;
	}

	/**
	 * 提供对由 {@link RequestMappingHandlerAdapter} 和 {@link ExceptionHandlerExceptionResolver} 使用的共享自定义参数解析器的访问权限。
	 * <p>此方法不能被覆盖；请改用 {@link #addArgumentResolvers}。
	 *
	 * @since 4.3
	 */
	protected final List<HandlerMethodArgumentResolver> getArgumentResolvers() {
		// 如果参数解析器为空
		if (this.argumentResolvers == null) {
			// 创建一个参数解析器列表
			this.argumentResolvers = new ArrayList<>();
			// 向参数解析器列表添加参数解析器
			addArgumentResolvers(this.argumentResolvers);
		}
		// 返回参数解析器列表
		return this.argumentResolvers;
	}

	/**
	 * 添加自定义的 {@link HandlerMethodArgumentResolver HandlerMethodArgumentResolvers} 以用于补充默认注册的解析器。
	 * <p>自定义参数解析器在内置解析器之前调用，除了那些依赖于注解的解析器（例如 {@code @RequestParameter}、{@code @PathVariable} 等）。后者可以通过直接配置 {@link RequestMappingHandlerAdapter} 进行自定义。
	 *
	 * @param argumentResolvers 自定义转换器的列表（最初为空列表）
	 */
	protected void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
	}

	/**
	 * 提供对由 {@link RequestMappingHandlerAdapter} 和 {@link ExceptionHandlerExceptionResolver} 使用的共享返回值处理程序的访问权限。
	 * <p>此方法不能被覆盖；请改用 {@link #addReturnValueHandlers}。
	 *
	 * @since 4.3
	 */
	protected final List<HandlerMethodReturnValueHandler> getReturnValueHandlers() {
		// 如果返回值处理器为空
		if (this.returnValueHandlers == null) {
			// 创建一个返回值处理器列表
			this.returnValueHandlers = new ArrayList<>();
			// 向返回值处理器列表添加返回值处理器
			addReturnValueHandlers(this.returnValueHandlers);
		}
		// 返回返回值处理器列表
		return this.returnValueHandlers;
	}

	/**
	 * 除了默认注册的处理程序外，添加自定义的 {@link HandlerMethodReturnValueHandler HandlerMethodReturnValueHandlers}。
	 * <p>自定义返回值处理程序在内置处理程序之前调用，除了依赖于注解的那些处理程序（例如 {@code @ResponseBody}、{@code @ModelAttribute} 等）。后者可以通过直接配置 {@link RequestMappingHandlerAdapter} 进行自定义。
	 *
	 * @param returnValueHandlers 自定义处理程序的列表（最初为空列表）
	 */
	protected void addReturnValueHandlers(List<HandlerMethodReturnValueHandler> returnValueHandlers) {
	}

	/**
	 * 提供对由 {@link RequestMappingHandlerAdapter} 和 {@link ExceptionHandlerExceptionResolver} 使用的共享 {@link HttpMessageConverter HttpMessageConverters} 的访问权限。
	 * <p>此方法不能被覆盖；请改用 {@link #configureMessageConverters}。另请参阅 {@link #addDefaultHttpMessageConverters}，以添加默认消息转换器。
	 */
	protected final List<HttpMessageConverter<?>> getMessageConverters() {
		// 如果消息转换器列表为空
		if (this.messageConverters == null) {
			// 创建一个消息转换器列表
			this.messageConverters = new ArrayList<>();
			// 配置消息转换器
			configureMessageConverters(this.messageConverters);
			// 如果消息转换器列表为空
			if (this.messageConverters.isEmpty()) {
				// 添加默认的 HTTP 消息转换器到消息转换器列表中
				addDefaultHttpMessageConverters(this.messageConverters);
			}
			// 扩展消息转换器列表
			extendMessageConverters(this.messageConverters);
		}
		// 返回消息转换器列表
		return this.messageConverters;
	}

	/**
	 * 覆盖此方法以添加自定义的 {@link HttpMessageConverter HttpMessageConverters}，以用于 {@link RequestMappingHandlerAdapter} 和 {@link ExceptionHandlerExceptionResolver}。
	 * <p>向列表中添加转换器将关闭默认情况下注册的转换器。另请参阅 {@link #addDefaultHttpMessageConverters}，以添加默认消息转换器。
	 *
	 * @param converters 要添加消息转换器的列表（最初为空列表）
	 */
	protected void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
	}

	/**
	 * 覆盖此方法以在配置后扩展或修改转换器列表。例如，允许注册默认转换器，然后通过此方法插入自定义转换器可能会很有用。
	 *
	 * @param converters 要扩展的已配置转换器列表
	 * @since 4.1.3
	 */
	protected void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
	}

	/**
	 * 向给定列表添加一组默认的 HttpMessageConverter 实例。子类可以从 {@link #configureMessageConverters} 中调用此方法。
	 *
	 * @param messageConverters 要添加默认消息转换器的列表
	 */
	protected final void addDefaultHttpMessageConverters(List<HttpMessageConverter<?>> messageConverters) {
		// 添加字节数组 HTTP 消息转换器到消息转换器列表
		messageConverters.add(new ByteArrayHttpMessageConverter());
		// 添加字符串 HTTP 消息转换器到消息转换器列表
		messageConverters.add(new StringHttpMessageConverter());
		// 添加资源 HTTP 消息转换器到消息转换器列表
		messageConverters.add(new ResourceHttpMessageConverter());
		// 添加资源区域 HTTP 消息转换器到消息转换器列表
		messageConverters.add(new ResourceRegionHttpMessageConverter());
		// 如果不应忽略 XML
		if (!shouldIgnoreXml) {
			try {
				// 尝试添加源 HTTP 消息转换器到消息转换器列表
				messageConverters.add(new SourceHttpMessageConverter<>());
			} catch (Throwable ex) {
				// 当没有可用的 TransformerFactory 实现时忽略异常
			}
		}
		// 添加全面的表单 HTTP 消息转换器到消息转换器列表
		messageConverters.add(new AllEncompassingFormHttpMessageConverter());

		// 如果存在 Rome库
		if (romePresent) {
			// 添加 AtomFeed HTTP 消息转换器到消息转换器列表
			messageConverters.add(new AtomFeedHttpMessageConverter());
			// 添加 RssChannel HTTP 消息转换器到消息转换器列表
			messageConverters.add(new RssChannelHttpMessageConverter());
		}

		// 如果不应忽略 XML
		if (!shouldIgnoreXml) {
			// 如果存在 Jackson2 XML库
			if (jackson2XmlPresent) {
				// 创建 Jackson2ObjectMapperBuilder 实例并配置为 XML
				Jackson2ObjectMapperBuilder builder = Jackson2ObjectMapperBuilder.xml();
				// 如果应用程序上下文不为空，则设置应用程序上下文
				if (this.applicationContext != null) {
					builder.applicationContext(this.applicationContext);
				}
				// 添加 MappingJackson2Xml HTTP 消息转换器到消息转换器列表
				messageConverters.add(new MappingJackson2XmlHttpMessageConverter(builder.build()));
			} else if (jaxb2Present) {
				// 否则如果存在 JAXB2库
				// 添加 Jaxb2RootElement HTTP 消息转换器到消息转换器列表
				messageConverters.add(new Jaxb2RootElementHttpMessageConverter());
			}
		}

		// 如果存在 Kotlin序列化JSON库
		if (kotlinSerializationJsonPresent) {
			// 添加 KotlinSerializationJson HTTP 消息转换器到消息转换器列表
			messageConverters.add(new KotlinSerializationJsonHttpMessageConverter());
		}

		// 如果存在 Jackson2库
		if (jackson2Present) {
			// 创建 Jackson2ObjectMapperBuilder 实例并配置为 JSON
			Jackson2ObjectMapperBuilder builder = Jackson2ObjectMapperBuilder.json();
			// 如果应用程序上下文不为空，则设置应用程序上下文
			if (this.applicationContext != null) {
				builder.applicationContext(this.applicationContext);
			}
			// 添加 MappingJackson2 HTTP 消息转换器到消息转换器列表
			messageConverters.add(new MappingJackson2HttpMessageConverter(builder.build()));
		} else if (gsonPresent) {
			// 否则如果存在 Gson库
			// 添加 Gson HTTP 消息转换器到消息转换器列表
			messageConverters.add(new GsonHttpMessageConverter());
		} else if (jsonbPresent) {
			// 否则如果存在 JSON-B库
			// 添加 Jsonb HTTP 消息转换器到消息转换器列表
			messageConverters.add(new JsonbHttpMessageConverter());
		}

		// 如果存在 Jackson2 Smile库
		if (jackson2SmilePresent) {
			// 创建 Jackson2ObjectMapperBuilder 实例并配置为 Smile
			Jackson2ObjectMapperBuilder builder = Jackson2ObjectMapperBuilder.smile();
			// 如果应用程序上下文不为空，则设置应用程序上下文
			if (this.applicationContext != null) {
				builder.applicationContext(this.applicationContext);
			}
			// 添加 MappingJackson2Smile HTTP 消息转换器到消息转换器列表
			messageConverters.add(new MappingJackson2SmileHttpMessageConverter(builder.build()));
		}

		// 如果存在 Jackson2 CBOR库
		if (jackson2CborPresent) {
			// 创建 Jackson2ObjectMapperBuilder 实例并配置为 CBOR
			Jackson2ObjectMapperBuilder builder = Jackson2ObjectMapperBuilder.cbor();
			// 如果应用程序上下文不为空，则设置应用程序上下文
			if (this.applicationContext != null) {
				builder.applicationContext(this.applicationContext);
			}
			// 添加 MappingJackson2Cbor HTTP 消息转换器到消息转换器列表
			messageConverters.add(new MappingJackson2CborHttpMessageConverter(builder.build()));
		}
	}

	/**
	 * 构建 {@link AsyncSupportConfigurer} 的回调方法。委托给 {@link #configureAsyncSupport(AsyncSupportConfigurer)}。
	 *
	 * @since 5.3.2
	 */
	protected AsyncSupportConfigurer getAsyncSupportConfigurer() {
		// 如果异步支持配置器为空
		if (this.asyncSupportConfigurer == null) {
			// 创建异步支持配置器实例
			this.asyncSupportConfigurer = new AsyncSupportConfigurer();
			// 配置异步支持
			configureAsyncSupport(this.asyncSupportConfigurer);
		}
		// 返回异步支持配置器
		return this.asyncSupportConfigurer;
	}

	/**
	 * 覆盖此方法以配置异步请求处理选项。
	 *
	 * @see AsyncSupportConfigurer
	 */
	protected void configureAsyncSupport(AsyncSupportConfigurer configurer) {
	}

	/**
	 * 为 {@link org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder} 返回一个 {@link CompositeUriComponentsContributor} 实例。
	 *
	 * @since 4.0
	 */
	@Bean
	public CompositeUriComponentsContributor mvcUriComponentsContributor(
			@Qualifier("mvcConversionService") FormattingConversionService conversionService,
			@Qualifier("requestMappingHandlerAdapter") RequestMappingHandlerAdapter requestMappingHandlerAdapter) {
		return new CompositeUriComponentsContributor(
				requestMappingHandlerAdapter.getArgumentResolvers(), conversionService);
	}

	/**
	 * 返回一个 {@link HttpRequestHandlerAdapter} 用于处理带有 {@link HttpRequestHandler HttpRequestHandlers} 的请求。
	 */
	@Bean
	public HttpRequestHandlerAdapter httpRequestHandlerAdapter() {
		return new HttpRequestHandlerAdapter();
	}

	/**
	 * 返回一个 {@link SimpleControllerHandlerAdapter} 用于处理基于接口的控制器的请求。
	 */
	@Bean
	public SimpleControllerHandlerAdapter simpleControllerHandlerAdapter() {
		return new SimpleControllerHandlerAdapter();
	}

	/**
	 * 返回一个包含一组异常解析器的 {@link HandlerExceptionResolverComposite}，通过 {@link #configureHandlerExceptionResolvers}
	 * 或 {@link #addDefaultHandlerExceptionResolvers} 获取。
	 * <p><strong>注意:</strong> 由于 CGLIB 的限制，此方法不能被设置为 final。与其覆盖它，不如考虑覆盖 {@link #configureHandlerExceptionResolvers}，
	 * 允许提供一组解析器。
	 */
	@Bean
	public HandlerExceptionResolver handlerExceptionResolver(
			@Qualifier("mvcContentNegotiationManager") ContentNegotiationManager contentNegotiationManager) {
		// 创建异常解析器列表
		List<HandlerExceptionResolver> exceptionResolvers = new ArrayList<>();
		// 配置异常解析器
		configureHandlerExceptionResolvers(exceptionResolvers);
		// 如果异常解析器列表为空
		if (exceptionResolvers.isEmpty()) {
			// 添加默认的异常解析器到异常解析器列表中
			addDefaultHandlerExceptionResolvers(exceptionResolvers, contentNegotiationManager);
		}
		// 扩展异常解析器列表
		extendHandlerExceptionResolvers(exceptionResolvers);
		// 创建异常解析器组合
		HandlerExceptionResolverComposite composite = new HandlerExceptionResolverComposite();
		// 设置异常解析器组合的顺序
		composite.setOrder(0);
		// 设置异常解析器组合的异常解析器列表
		composite.setExceptionResolvers(exceptionResolvers);
		// 返回异常解析器组合
		return composite;
	}

	/**
	 * 覆盖此方法以配置要使用的 {@link HandlerExceptionResolver HandlerExceptionResolvers} 列表。
	 * <p>向列表添加解析器会关闭默认解析器的注册，否则默认情况下会注册。还可以查看 {@link #addDefaultHandlerExceptionResolvers}
	 * 方法，该方法用于添加默认异常解析器。
	 *
	 * @param exceptionResolvers 要添加异常解析器的列表（最初为空列表）
	 */
	protected void configureHandlerExceptionResolvers(List<HandlerExceptionResolver> exceptionResolvers) {
	}

	/**
	 * 覆盖此方法以在配置后扩展或修改 {@link HandlerExceptionResolver HandlerExceptionResolvers} 列表。
	 * <p>这可能有用，例如允许注册默认解析器，然后通过此方法插入自定义解析器。
	 *
	 * @param exceptionResolvers 要扩展的已配置解析器列表。
	 * @since 4.3
	 */
	protected void extendHandlerExceptionResolvers(List<HandlerExceptionResolver> exceptionResolvers) {
	}

	/**
	 * 供子类使用的添加默认 {@link HandlerExceptionResolver HandlerExceptionResolvers} 的方法。
	 * <p>添加以下异常解析器：
	 * <ul>
	 * <li>{@link ExceptionHandlerExceptionResolver} 用于通过 {@link org.springframework.web.bind.annotation.ExceptionHandler} 方法处理异常。
	 * <li>{@link ResponseStatusExceptionResolver} 用于带有 {@link org.springframework.web.bind.annotation.ResponseStatus} 注解的异常。
	 * <li>{@link DefaultHandlerExceptionResolver} 用于解析已知的 Spring 异常类型。
	 * </ul>
	 */
	protected final void addDefaultHandlerExceptionResolvers(List<HandlerExceptionResolver> exceptionResolvers,
															 ContentNegotiationManager mvcContentNegotiationManager) {

		// 创建异常处理器异常解析器
		ExceptionHandlerExceptionResolver exceptionHandlerResolver = createExceptionHandlerExceptionResolver();
		// 设置 异常处理器异常解析器 的内容协商管理器
		exceptionHandlerResolver.setContentNegotiationManager(mvcContentNegotiationManager);
		// 设置 异常处理器异常解析器 的消息转换器
		exceptionHandlerResolver.setMessageConverters(getMessageConverters());
		// 设置 异常处理器异常解析器 的自定义参数解析器
		exceptionHandlerResolver.setCustomArgumentResolvers(getArgumentResolvers());
		// 设置 异常处理器异常解析器 的自定义返回值处理器
		exceptionHandlerResolver.setCustomReturnValueHandlers(getReturnValueHandlers());
		// 如果存在 Jackson2库
		if (jackson2Present) {
			// 设置 异常处理器异常解析器 的响应体通知
			exceptionHandlerResolver.setResponseBodyAdvice(Collections.singletonList(new JsonViewResponseBodyAdvice()));
		}
		// 如果应用程序上下文不为空
		if (this.applicationContext != null) {
			// 设置 异常处理器异常解析器 的应用程序上下文
			exceptionHandlerResolver.setApplicationContext(this.applicationContext);
		}
		// 执行属性设置完成方法
		exceptionHandlerResolver.afterPropertiesSet();
		// 添加 异常处理器异常解析器 到异常解析器列表中
		exceptionResolvers.add(exceptionHandlerResolver);

		// 创建响应状态异常解析器
		ResponseStatusExceptionResolver responseStatusResolver = new ResponseStatusExceptionResolver();
		// 设置响应状态异常解析器的消息源为应用程序上下文
		responseStatusResolver.setMessageSource(this.applicationContext);
		// 添加响应状态异常解析器到异常解析器列表中
		exceptionResolvers.add(responseStatusResolver);

		// 添加默认的处理器异常解析器到异常解析器列表中
		exceptionResolvers.add(new DefaultHandlerExceptionResolver());
	}

	/**
	 * 用于插入自定义 {@link ExceptionHandlerExceptionResolver} 子类的受保护方法。
	 *
	 * @since 4.3
	 */
	protected ExceptionHandlerExceptionResolver createExceptionHandlerExceptionResolver() {
		return new ExceptionHandlerExceptionResolver();
	}

	/**
	 * 注册一个 {@link ViewResolverComposite}，其中包含一系列视图解析器用于视图解析。
	 * 默认情况下，此解析器的顺序为 0，除非使用内容协商视图解析，此时顺序将提升至
	 * {@link org.springframework.core.Ordered#HIGHEST_PRECEDENCE Ordered.HIGHEST_PRECEDENCE}。
	 * <p>如果没有配置其他解析器，
	 * {@link ViewResolverComposite#resolveViewName(String, Locale)} 将返回 null，以允许其他潜在的
	 * {@link ViewResolver} bean 解析视图。
	 *
	 * @since 4.1
	 */
	@Bean
	public ViewResolver mvcViewResolver(
			@Qualifier("mvcContentNegotiationManager") ContentNegotiationManager contentNegotiationManager) {
		// 创建视图解析器注册表
		ViewResolverRegistry registry = new ViewResolverRegistry(contentNegotiationManager, this.applicationContext);
		// 配置视图解析器
		configureViewResolvers(registry);

		// 如果视图解析器列表为空，且应用程序上下文不为空
		if (registry.getViewResolvers().isEmpty() && this.applicationContext != null) {
			// 获取应用程序上下文中所有的视图解析器的 bean 名称
			String[] names = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
					this.applicationContext, ViewResolver.class, true, false);
			// 如果只有一个视图解析器的 bean 名称
			if (names.length == 1) {
				// 添加一个内部资源视图解析器到视图解析器列表中
				registry.getViewResolvers().add(new InternalResourceViewResolver());
			}
		}

		// 创建视图解析器组合
		ViewResolverComposite composite = new ViewResolverComposite();
		// 设置视图解析器组合的顺序
		composite.setOrder(registry.getOrder());
		// 设置视图解析器组合的视图解析器列表
		composite.setViewResolvers(registry.getViewResolvers());
		// 如果应用程序上下文不为空，则设置应用程序上下文
		if (this.applicationContext != null) {
			composite.setApplicationContext(this.applicationContext);
		}
		// 如果 Servlet 上下文不为空，则设置 Servlet 上下文
		if (this.servletContext != null) {
			composite.setServletContext(this.servletContext);
		}
		// 返回视图解析器组合
		return composite;
	}

	/**
	 * 覆盖此方法以配置视图解析。
	 *
	 * @see ViewResolverRegistry
	 */
	protected void configureViewResolvers(ViewResolverRegistry registry) {
	}

	/**
	 * 返回已注册的 {@link CorsConfiguration} 对象，由路径模式作为键。
	 *
	 * @since 4.2
	 */
	protected final Map<String, CorsConfiguration> getCorsConfigurations() {
		// 如果跨域配置为空
		if (this.corsConfigurations == null) {
			// 创建跨域注册表
			CorsRegistry registry = new CorsRegistry();
			// 添加跨域映射
			addCorsMappings(registry);
			// 获取跨域配置
			this.corsConfigurations = registry.getCorsConfigurations();
		}
		// 返回跨域配置
		return this.corsConfigurations;
	}

	/**
	 * 覆盖此方法以配置跨源请求处理。
	 *
	 * @see CorsRegistry
	 * @since 4.2
	 */
	protected void addCorsMappings(CorsRegistry registry) {
	}

	@Bean
	@Lazy
	public HandlerMappingIntrospector mvcHandlerMappingIntrospector() {
		return new HandlerMappingIntrospector();
	}

	@Bean
	public LocaleResolver localeResolver() {
		return new AcceptHeaderLocaleResolver();
	}

	@Bean
	public ThemeResolver themeResolver() {
		return new FixedThemeResolver();
	}

	@Bean
	public FlashMapManager flashMapManager() {
		return new SessionFlashMapManager();
	}

	@Bean
	public RequestToViewNameTranslator viewNameTranslator() {
		return new DefaultRequestToViewNameTranslator();
	}


	private static final class NoOpValidator implements Validator {

		@Override
		public boolean supports(Class<?> clazz) {
			return false;
		}

		@Override
		public void validate(@Nullable Object target, Errors errors) {
		}
	}

}
