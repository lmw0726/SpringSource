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

package org.springframework.web.servlet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.core.log.LogFormatUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.ui.context.ThemeSource;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.async.WebAsyncManager;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.util.NestedServletException;
import org.springframework.web.util.ServletRequestPathUtils;
import org.springframework.web.util.WebUtils;

import javax.servlet.DispatcherType;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * HTTP请求处理器/控制器的中央调度程序，例如用于Web UI控制器或基于HTTP的远程服务导出器。分派到注册的处理程序以处理Web请求，提供方便的映射和异常处理功能。
 *
 * <p>此Servlet非常灵活：它可以与几乎任何工作流一起使用，只需安装适当的适配器类。它提供以下功能，使其与其他基于请求驱动的Web MVC框架不同：
 *
 * <ul>
 * <li>它基于JavaBeans配置机制。
 *
 * <li>它可以使用任何HandlerMapping实现 - 预构建的或作为应用程序的一部分提供 - 来控制请求的路由到处理程序对象。默认是
 * {@link org.springframework.web.servlet.handler.BeanNameUrlHandlerMapping} 和
 * {@link org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping}。HandlerMapping对象可以定义为
 * Servlet的应用程序上下文中的bean，实现HandlerMapping接口，覆盖默认的HandlerMapping（如果存在）。HandlerMappings可以
 * 给定任何bean名称（它们通过类型进行测试）。
 *
 * <li>它可以使用任何HandlerAdapter；这允许使用任何处理程序接口。默认的适配器是
 * {@link org.springframework.web.servlet.mvc.HttpRequestHandlerAdapter}、
 * {@link org.springframework.web.servlet.mvc.SimpleControllerHandlerAdapter}，用于Spring的
 * {@link org.springframework.web.HttpRequestHandler} 和
 * {@link org.springframework.web.servlet.mvc.Controller} 接口，分别。还将注册一个默认的
 * {@link org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter}。HandlerAdapter对象可以
 * 作为bean添加到应用程序上下文中，覆盖默认的HandlerAdapters。与HandlerMappings一样，HandlerAdapters可以给定
 * 任何bean名称（它们通过类型进行测试）。
 *
 * <li>分派程序的异常解析策略可以通过HandlerExceptionResolver指定，例如将某些异常映射到错误页面。默认是
 * {@link org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver}、
 * {@link org.springframework.web.servlet.mvc.annotation.ResponseStatusExceptionResolver} 和
 * {@link org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver}。这些HandlerExceptionResolvers
 * 可以通过应用程序上下文进行覆盖。HandlerExceptionResolver可以给定任何bean名称（它们通过类型进行测试）。
 *
 * <li>其视图解析策略可以通过ViewResolver实现指定，将符号视图名称解析为View对象。默认是
 * {@link org.springframework.web.servlet.view.InternalResourceViewResolver}。ViewResolver对象可以添加为应用程序上下文中的bean，
 * 覆盖默认的ViewResolver。ViewResolvers可以给定任何bean名称（它们通过类型进行测试）。
 *
 * <li>如果用户没有提供{@link View} 或视图名称，则配置的{@link RequestToViewNameTranslator} 将当前请求转换为视图名称。对应的bean名称
 * 为 "viewNameTranslator"；默认为 {@link org.springframework.web.servlet.view.DefaultRequestToViewNameTranslator}。
 *
 * <li>分派程序解析多部分请求的策略由{@link org.springframework.web.multipart.MultipartResolver} 实现确定。包含Apache Commons
 * FileUpload和Servlet 3的实现；典型选择是 {@link org.springframework.web.multipart.commons.CommonsMultipartResolver}。MultipartResolver
 * bean名称为 "multipartResolver"；默认为无。
 *
 * <li>其区域设置解析策略由{@link LocaleResolver} 确定。现成的实现通过HTTP接受标头、cookie或会话工作。LocaleResolver bean名称为
 * "localeResolver"；默认为 {@link org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver}。
 *
 * <li>其主题解析策略由{@link ThemeResolver} 确定。包括固定主题的实现以及cookie和会话存储。ThemeResolver bean名称为
 * "themeResolver"；默认为 {@link org.springframework.web.servlet.theme.FixedThemeResolver}。
 * </ul>
 *
 * <p><b>注意：仅当调度程序中存在相应的{@code HandlerMapping}（用于类型级别注解）和/或{@code HandlerAdapter}
 * （用于方法级别注解）时，才会处理{@code @RequestMapping}注解。</b> 默认情况下是这样的。但是，如果您正在定义自定义的{@code HandlerMappings}
 * 或{@code HandlerAdapters}，则需要确保相应的自定义{@code RequestMappingHandlerMapping}和/或{@code RequestMappingHandlerAdapter}
 * 也已定义 - 前提是您打算使用{@code @RequestMapping}。
 *
 * <p><b>Web应用程序可以定义任意数量的DispatcherServlet。</b> 每个Servlet将在其自己的命名空间中运行，加载自己的应用程序上下文，包括映射、处理程序等。仅由
 * {@link org.springframework.web.context.ContextLoaderListener} 加载的根应用程序上下文（如果有）将被共享。
 *
 * <p>从Spring 3.1开始，{@code DispatcherServlet}现在可以注入Web应用程序上下文，而不是在内部创建自己。这在支持Servlet 3.0+的环境中非常有用，该环境
 * 支持Servlet实例的编程注册。有关详细信息，请参阅{@link #DispatcherServlet(WebApplicationContext)}的javadoc。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Chris Beams
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @see org.springframework.web.HttpRequestHandler
 * @see org.springframework.web.servlet.mvc.Controller
 * @see org.springframework.web.context.ContextLoaderListener
 */
@SuppressWarnings("serial")
public class DispatcherServlet extends FrameworkServlet {

	/**
	 * 该命名空间中bean工厂中的MultipartResolver对象的常用名称。
	 */
	public static final String MULTIPART_RESOLVER_BEAN_NAME = "multipartResolver";

	/**
	 * 该命名空间中bean工厂中的LocaleResolver对象的常用名称。
	 */
	public static final String LOCALE_RESOLVER_BEAN_NAME = "localeResolver";

	/**
	 * 该命名空间中bean工厂中的ThemeResolver对象的常用名称。
	 */
	public static final String THEME_RESOLVER_BEAN_NAME = "themeResolver";

	/**
	 * 该命名空间中bean工厂中的HandlerMapping对象的常用名称。
	 * 仅在关闭“detectAllHandlerMappings”时使用。
	 *
	 * @see #setDetectAllHandlerMappings
	 */
	public static final String HANDLER_MAPPING_BEAN_NAME = "handlerMapping";

	/**
	 * 该命名空间中bean工厂中的HandlerAdapter对象的常用名称。
	 * 仅在关闭“detectAllHandlerAdapters”时使用。
	 *
	 * @see #setDetectAllHandlerAdapters
	 */
	public static final String HANDLER_ADAPTER_BEAN_NAME = "handlerAdapter";

	/**
	 * 该命名空间中bean工厂中的HandlerExceptionResolver对象的常用名称。
	 * 仅在关闭“detectAllHandlerExceptionResolvers”时使用。
	 *
	 * @see #setDetectAllHandlerExceptionResolvers
	 */
	public static final String HANDLER_EXCEPTION_RESOLVER_BEAN_NAME = "handlerExceptionResolver";

	/**
	 * 该命名空间中bean工厂中的RequestToViewNameTranslator对象的常用名称。
	 */
	public static final String REQUEST_TO_VIEW_NAME_TRANSLATOR_BEAN_NAME = "viewNameTranslator";

	/**
	 * 该命名空间中bean工厂中的ViewResolver对象的常用名称。
	 * 仅在关闭“detectAllViewResolvers”时使用。
	 *
	 * @see #setDetectAllViewResolvers
	 */
	public static final String VIEW_RESOLVER_BEAN_NAME = "viewResolver";

	/**
	 * 该命名空间中bean工厂中的FlashMapManager对象的常用名称。
	 */
	public static final String FLASH_MAP_MANAGER_BEAN_NAME = "flashMapManager";

	/**
	 * 请求属性，用于保存当前的Web应用程序上下文。
	 * 否则，仅可以通过标签等获得全局Web应用程序上下文。
	 *
	 * @see org.springframework.web.servlet.support.RequestContextUtils#findWebApplicationContext
	 */
	public static final String WEB_APPLICATION_CONTEXT_ATTRIBUTE = DispatcherServlet.class.getName() + ".CONTEXT";

	/**
	 * 请求属性，用于保存当前的LocaleResolver，可由视图检索。
	 *
	 * @see org.springframework.web.servlet.support.RequestContextUtils#getLocaleResolver
	 */
	public static final String LOCALE_RESOLVER_ATTRIBUTE = DispatcherServlet.class.getName() + ".LOCALE_RESOLVER";

	/**
	 * 请求属性，用于保存当前的ThemeResolver，可由视图检索。
	 *
	 * @see org.springframework.web.servlet.support.RequestContextUtils#getThemeResolver
	 */
	public static final String THEME_RESOLVER_ATTRIBUTE = DispatcherServlet.class.getName() + ".THEME_RESOLVER";

	/**
	 * 请求属性，用于保存当前的ThemeSource，可由视图检索。
	 *
	 * @see org.springframework.web.servlet.support.RequestContextUtils#getThemeSource
	 */
	public static final String THEME_SOURCE_ATTRIBUTE = DispatcherServlet.class.getName() + ".THEME_SOURCE";

	/**
	 * 请求属性的名称，保存着只读的{@code Map<String,?>}，其中包含了由先前请求保存的“input”闪存属性（如果有）。
	 *
	 * @see org.springframework.web.servlet.support.RequestContextUtils#getInputFlashMap(HttpServletRequest)
	 */
	public static final String INPUT_FLASH_MAP_ATTRIBUTE = DispatcherServlet.class.getName() + ".INPUT_FLASH_MAP";

	/**
	 * 请求属性的名称，保存着“output”{@link FlashMap}，其中包含了要在随后的请求中保存的属性。
	 *
	 * @see org.springframework.web.servlet.support.RequestContextUtils#getOutputFlashMap(HttpServletRequest)
	 */
	public static final String OUTPUT_FLASH_MAP_ATTRIBUTE = DispatcherServlet.class.getName() + ".OUTPUT_FLASH_MAP";

	/**
	 * 请求属性的名称，保存着{@link FlashMapManager}。
	 *
	 * @see org.springframework.web.servlet.support.RequestContextUtils#getFlashMapManager(HttpServletRequest)
	 */
	public static final String FLASH_MAP_MANAGER_ATTRIBUTE = DispatcherServlet.class.getName() + ".FLASH_MAP_MANAGER";

	/**
	 * 请求属性的名称，用于公开使用{@link HandlerExceptionResolver}解析但未渲染视图的异常（例如，设置状态码）。
	 */
	public static final String EXCEPTION_ATTRIBUTE = DispatcherServlet.class.getName() + ".EXCEPTION";

	/**
	 * 当找不到请求的映射处理程序时要使用的日志类别。
	 */
	public static final String PAGE_NOT_FOUND_LOG_CATEGORY = "org.springframework.web.servlet.PageNotFound";

	/**
	 * 定义DispatcherServlet的默认策略名称的类路径资源的名称（相对于DispatcherServlet类）。
	 */
	private static final String DEFAULT_STRATEGIES_PATH = "DispatcherServlet.properties";

	/**
	 * DispatcherServlet的默认策略属性的常见前缀。
	 */
	private static final String DEFAULT_STRATEGIES_PREFIX = "org.springframework.web.servlet";

	/**
	 * 当找不到请求的映射处理程序时要使用的额外记录器。
	 */
	protected static final Log pageNotFoundLogger = LogFactory.getLog(PAGE_NOT_FOUND_LOG_CATEGORY);

	/**
	 * 存储默认策略实现。
	 */
	@Nullable
	private static Properties defaultStrategies;

	/**
	 * 是否检测所有HandlerMappings，还是只期望存在“handlerMapping” bean。
	 */
	private boolean detectAllHandlerMappings = true;

	/**
	 * 是否检测所有HandlerAdapters，还是只期望存在“handlerAdapter” bean。
	 */
	private boolean detectAllHandlerAdapters = true;

	/**
	 * 是否检测所有HandlerExceptionResolvers，还是只期望存在“handlerExceptionResolver” bean。
	 */
	private boolean detectAllHandlerExceptionResolvers = true;

	/**
	 * 是否检测所有ViewResolvers，还是只期望存在“viewResolver” bean。
	 */
	private boolean detectAllViewResolvers = true;

	/**
	 * 如果找不到处理此请求的Handler，则是否抛出NoHandlerFoundException？
	 */
	private boolean throwExceptionIfNoHandlerFound = false;

	/**
	 * 在包含请求后执行请求属性清理吗？
	 */
	private boolean cleanupAfterInclude = true;

	/**
	 * 此Servlet使用的MultipartResolver。
	 */
	@Nullable
	private MultipartResolver multipartResolver;

	/**
	 * 此Servlet使用的LocaleResolver。
	 */
	@Nullable
	private LocaleResolver localeResolver;

	/**
	 * 此Servlet使用的ThemeResolver。
	 */
	@Nullable
	private ThemeResolver themeResolver;

	/**
	 * 此Servlet使用的HandlerMappings列表。
	 */
	@Nullable
	private List<HandlerMapping> handlerMappings;

	/**
	 * 此Servlet使用的HandlerAdapters列表。
	 */
	@Nullable
	private List<HandlerAdapter> handlerAdapters;

	/**
	 * 此Servlet使用的HandlerExceptionResolvers列表。
	 */
	@Nullable
	private List<HandlerExceptionResolver> handlerExceptionResolvers;

	/**
	 * 此Servlet使用的RequestToViewNameTranslator。
	 */
	@Nullable
	private RequestToViewNameTranslator viewNameTranslator;

	/**
	 * 此Servlet使用的FlashMapManager。
	 */
	@Nullable
	private FlashMapManager flashMapManager;

	/**
	 * 此Servlet使用的ViewResolvers列表。
	 */
	@Nullable
	private List<ViewResolver> viewResolvers;

	/**
	 * 是否解析请求路径
	 */
	private boolean parseRequestPath;


	/**
	 * 创建一个新的{@code DispatcherServlet}，它将根据默认值和通过servlet init-params提供的值创建自己的内部Web应用程序上下文。
	 * 在Servlet 2.5或更早的环境中通常使用，在那里Servlet注册的唯一选项是通过{@code web.xml}，它需要使用无参构造函数。
	 * <p>调用{@link #setContextConfigLocation}（init-param 'contextConfigLocation'）将决定由{@linkplain #DEFAULT_CONTEXT_CLASS 默认XmlWebApplicationContext}加载哪些XML文件。
	 * <p>调用{@link #setContextClass}（init-param 'contextClass'）将覆盖默认的{@code XmlWebApplicationContext}并允许指定另一个类，例如{@code AnnotationConfigWebApplicationContext}。
	 * <p>调用{@link #setContextInitializerClasses}（init-param 'contextInitializerClasses'）指示应使用哪些{@code ApplicationContextInitializer}类在刷新之前进一步配置内部应用程序上下文。
	 *
	 * @see #DispatcherServlet(WebApplicationContext)
	 */
	public DispatcherServlet() {
		super();
		setDispatchOptionsRequest(true);
	}

	/**
	 * 使用给定的Web应用程序上下文创建一个新的{@code DispatcherServlet}。
	 * 在Servlet 3.0+环境中，通过{@link ServletContext#addServlet} API可以对Servlet进行基于实例的注册，因此此构造函数非常有用。
	 * <p>使用此构造函数表示以下属性/ init-params 将被忽略：
	 * <ul>
	 * <li>{@link #setContextClass(Class)} / 'contextClass'</li>
	 * <li>{@link #setContextConfigLocation(String)} / 'contextConfigLocation'</li>
	 * <li>{@link #setContextAttribute(String)} / 'contextAttribute'</li>
	 * <li>{@link #setNamespace(String)} / 'namespace'</li>
	 * </ul>
	 * <p>给定的Web应用程序上下文可能已经{@linkplain ConfigurableApplicationContext#refresh() 刷新}，也可能尚未刷新（建议的方法）。
	 * 如果尚未刷新（推荐的方法），则会发生以下情况：
	 * <ul>
	 * <li>如果给定的上下文尚未具有{@linkplain ConfigurableApplicationContext#setParent 父上下文}，则根应用程序上下文将设置为父上下文。</li>
	 * <li>如果给定的上下文尚未分配{@linkplain ConfigurableApplicationContext#setId id}，则将为其分配一个id。</li>
	 * <li>{@code ServletContext}和{@code ServletConfig}对象将委托给应用程序上下文。</li>
	 * <li>将调用{@link #postProcessWebApplicationContext}。</li>
	 * <li>将应用通过“contextInitializerClasses”init-param指定的任何{@code ApplicationContextInitializer}。</li>
	 * <li>如果上下文实现了{@link ConfigurableApplicationContext}，将调用{@link ConfigurableApplicationContext#refresh refresh()}。</li>
	 * </ul>
	 * 如果上下文已经刷新，则不会发生上述任何情况，假设用户已根据其特定需求执行了这些操作（或没有执行）。
	 * <p>有关用法示例，请参阅{@link org.springframework.web.WebApplicationInitializer}。
	 *
	 * @param webApplicationContext 要使用的上下文
	 * @see #initWebApplicationContext
	 * @see #configureAndRefreshWebApplicationContext
	 * @see org.springframework.web.WebApplicationInitializer
	 */
	public DispatcherServlet(WebApplicationContext webApplicationContext) {
		super(webApplicationContext);
		setDispatchOptionsRequest(true);
	}


	/**
	 * 设置是否在此servlet的上下文中检测所有HandlerMapping bean。否则，只会期望存在一个名为“handlerMapping”的bean。
	 * <p>默认值为“true”。如果要求此servlet使用单个HandlerMapping，尽管上下文中定义了多个HandlerMapping bean，则关闭此选项。
	 */
	public void setDetectAllHandlerMappings(boolean detectAllHandlerMappings) {
		this.detectAllHandlerMappings = detectAllHandlerMappings;
	}

	/**
	 * 设置是否在此servlet的上下文中检测所有HandlerAdapter bean。否则，只会期望存在一个名为“handlerAdapter”的bean。
	 * <p>默认值为“true”。如果要求此servlet使用单个HandlerAdapter，尽管上下文中定义了多个HandlerAdapter bean，则关闭此选项。
	 */
	public void setDetectAllHandlerAdapters(boolean detectAllHandlerAdapters) {
		this.detectAllHandlerAdapters = detectAllHandlerAdapters;
	}

	/**
	 * 设置是否在此servlet的上下文中检测所有HandlerExceptionResolver bean。否则，只会期望存在一个名为“handlerExceptionResolver”的bean。
	 * <p>默认值为“true”。如果要求此servlet使用单个HandlerExceptionResolver，尽管上下文中定义了多个HandlerExceptionResolver bean，则关闭此选项。
	 */
	public void setDetectAllHandlerExceptionResolvers(boolean detectAllHandlerExceptionResolvers) {
		this.detectAllHandlerExceptionResolvers = detectAllHandlerExceptionResolvers;
	}

	/**
	 * 设置是否在此servlet的上下文中检测所有ViewResolver bean。否则，只会期望存在一个名为“viewResolver”的bean。
	 * <p>默认值为“true”。如果要求此servlet使用单个ViewResolver，尽管上下文中定义了多个ViewResolver bean，则关闭此选项。
	 */
	public void setDetectAllViewResolvers(boolean detectAllViewResolvers) {
		this.detectAllViewResolvers = detectAllViewResolvers;
	}

	/**
	 * 设置是否在找不到处理此请求的Handler时抛出NoHandlerFoundException。
	 * 然后，此异常可以由HandlerExceptionResolver或{@code @ExceptionHandler}控制器方法捕获。
	 * <p>请注意，如果使用{@link org.springframework.web.servlet.resource.DefaultServletHttpRequestHandler}，
	 * 则请求将始终转发到默认servlet，并且在这种情况下永远不会抛出NoHandlerFoundException。
	 * <p>默认值为“false”，这意味着DispatcherServlet通过Servlet响应发送NOT_FOUND错误。
	 *
	 * @since 4.0
	 */
	public void setThrowExceptionIfNoHandlerFound(boolean throwExceptionIfNoHandlerFound) {
		this.throwExceptionIfNoHandlerFound = throwExceptionIfNoHandlerFound;
	}

	/**
	 * 设置是否在包含请求后执行请求属性清理，即在DispatcherServlet在包含请求中处理后是否重置所有请求属性的原始状态。
	 * 否则，只会重置DispatcherServlet自己的请求属性，但不会重置用于JSP或视图设置的模型属性或特殊属性（例如JSTL的属性）。
	 * <p>默认值为“true”，强烈建议使用。视图不应依赖于由（动态）包含设置的请求属性。
	 * 这允许由包含的控制器渲染的JSP视图使用任何模型属性，即使与主JSP中的属性名称相同，也不会引起副作用。
	 * 仅在特殊需求时关闭此选项，例如故意允许主JSP访问由包含的控制器渲染的JSP视图的属性。
	 */
	public void setCleanupAfterInclude(boolean cleanupAfterInclude) {
		this.cleanupAfterInclude = cleanupAfterInclude;
	}


	/**
	 * 此实现调用{@link #initStrategies}。
	 */
	@Override
	protected void onRefresh(ApplicationContext context) {
		initStrategies(context);
	}

	/**
	 * 初始化此servlet使用的策略对象。
	 * <p>可以在子类中重写以初始化更多的策略对象。
	 */
	protected void initStrategies(ApplicationContext context) {
		// 初始化多部分解析器
		initMultipartResolver(context);
		// 初始化本地化解析器
		initLocaleResolver(context);
		// 初始化主题解析器
		initThemeResolver(context);
		// 初始化处理器映射
		initHandlerMappings(context);
		// 初始化处理器适配器
		initHandlerAdapters(context);
		// 初始化处理异常解析器
		initHandlerExceptionResolvers(context);
		// 初始化请求转视图名称翻译器
		initRequestToViewNameTranslator(context);
		// 初始化视图解析器
		initViewResolvers(context);
		// 初始化FlashMap管理器
		initFlashMapManager(context);
	}

	/**
	 * 初始化此类使用的 MultipartResolver。
	 * 如果在此命名空间的 BeanFactory 中未定义具有给定名称的 bean，则不提供多部分处理。
	 */
	private void initMultipartResolver(ApplicationContext context) {
		try {
			// 从应用程序上下文中获取 MultipartResolver 实例。
			this.multipartResolver = context.getBean(MULTIPART_RESOLVER_BEAN_NAME, MultipartResolver.class);
			// 如果跟踪日志级别已启用，则记录检测到的 MultipartResolver。
			if (logger.isTraceEnabled()) {
				logger.trace("Detected " + this.multipartResolver);
				// 如果调试日志级别已启用，则记录检测到的 MultipartResolver 类的简称。
			} else if (logger.isDebugEnabled()) {
				logger.debug("Detected " + this.multipartResolver.getClass().getSimpleName());
			}
		} catch (NoSuchBeanDefinitionException ex) {
			// 默认情况下不提供多部分解析器。
			this.multipartResolver = null;
			// 如果跟踪日志级别已启用，则记录未声明 MultipartResolver 的消息。
			if (logger.isTraceEnabled()) {
				logger.trace("No MultipartResolver '" + MULTIPART_RESOLVER_BEAN_NAME + "' declared");
			}
		}
	}

	/**
	 * 初始化此类使用的 LocaleResolver。
	 * 如果在此命名空间的 BeanFactory 中未定义具有给定名称的 bean，则默认使用 AcceptHeaderLocaleResolver。
	 */
	private void initLocaleResolver(ApplicationContext context) {
		try {
			// 从应用程序上下文中获取 LocaleResolver 实例。
			this.localeResolver = context.getBean(LOCALE_RESOLVER_BEAN_NAME, LocaleResolver.class);
			// 如果跟踪日志级别已启用，则记录检测到的 LocaleResolver。
			if (logger.isTraceEnabled()) {
				logger.trace("Detected " + this.localeResolver);
				// 如果调试日志级别已启用，则记录检测到的 LocaleResolver 类的简称。
			} else if (logger.isDebugEnabled()) {
				logger.debug("Detected " + this.localeResolver.getClass().getSimpleName());
			}
		} catch (NoSuchBeanDefinitionException ex) {
			// 我们需要使用默认的 LocaleResolver。
			this.localeResolver = getDefaultStrategy(context, LocaleResolver.class);
			// 如果跟踪日志级别已启用，则记录未声明 LocaleResolver 的消息。
			if (logger.isTraceEnabled()) {
				logger.trace("No LocaleResolver '" + LOCALE_RESOLVER_BEAN_NAME +
						"': using default [" + this.localeResolver.getClass().getSimpleName() + "]");
			}
		}
	}

	/**
	 * 初始化此类使用的ThemeResolver。
	 * <p>如果在此命名空间的BeanFactory中没有使用给定名称定义的bean，则我们默认使用FixedThemeResolver。
	 */
	private void initThemeResolver(ApplicationContext context) {
		try {
			// 从应用程序上下文中获取 ThemeResolver 实例。
			this.themeResolver = context.getBean(THEME_RESOLVER_BEAN_NAME, ThemeResolver.class);
			if (logger.isTraceEnabled()) {
				logger.trace("Detected " + this.themeResolver);
			} else if (logger.isDebugEnabled()) {
				logger.debug("Detected " + this.themeResolver.getClass().getSimpleName());
			}
		} catch (NoSuchBeanDefinitionException ex) {
			// 需要使用默认值。
			this.themeResolver = getDefaultStrategy(context, ThemeResolver.class);
			if (logger.isTraceEnabled()) {
				logger.trace("No ThemeResolver '" + THEME_RESOLVER_BEAN_NAME +
						"': using default [" + this.themeResolver.getClass().getSimpleName() + "]");
			}
		}
	}

	/**
	 * 初始化此类使用的HandlerMappings。
	 * <p>如果在此命名空间的BeanFactory中没有定义HandlerMapping bean，则我们默认使用BeanNameUrlHandlerMapping。
	 */
	private void initHandlerMappings(ApplicationContext context) {
		// 将 handlerMappings 设置为 null。
		this.handlerMappings = null;

		// 如果要检测所有HandlerMappings，则查找所有的 HandlerMappings。
		if (this.detectAllHandlerMappings) {
			// 在 ApplicationContext 中查找所有的 HandlerMappings，包括祖先上下文。
			Map<String, HandlerMapping> matchingBeans =
					BeanFactoryUtils.beansOfTypeIncludingAncestors(context, HandlerMapping.class, true, false);
			// 如果找到匹配的 HandlerMappings，则将其存储在 handlerMappings 中。
			if (!matchingBeans.isEmpty()) {
				this.handlerMappings = new ArrayList<>(matchingBeans.values());
				// 将 HandlerMappings 排序。
				AnnotationAwareOrderComparator.sort(this.handlerMappings);
			}
		} else {
			try {
				// 尝试从 ApplicationContext 中获取单个 HandlerMapping。
				HandlerMapping hm = context.getBean(HANDLER_MAPPING_BEAN_NAME, HandlerMapping.class);
				// 如果成功获取到 HandlerMapping，则将其存储在 handlerMappings 中。
				this.handlerMappings = Collections.singletonList(hm);
			} catch (NoSuchBeanDefinitionException ex) {
				// 如果未找到指定的 HandlerMapping，则忽略异常。
				// HandlerMappings 将在稍后的 getDefaultStrategies 方法中添加默认策略。
			}
		}

		// 确保至少有一个 HandlerMapping，如果 handlerMappings 为 null，则使用默认策略。
		if (this.handlerMappings == null) {
			// 从 DispatcherServlet.properties 中获取默认策略。
			this.handlerMappings = getDefaultStrategies(context, HandlerMapping.class);
			// 如果跟踪日志级别已启用，则记录未声明 HandlerMappings 的消息。
			if (logger.isTraceEnabled()) {
				logger.trace("No HandlerMappings declared for servlet '" + getServletName() +
						"': using default strategies from DispatcherServlet.properties");
			}
		}

		// 检查每个 HandlerMapping 是否使用路径模式，如果是，则将 parseRequestPath 设置为 true。
		for (HandlerMapping mapping : this.handlerMappings) {
			if (mapping.usesPathPatterns()) {
				this.parseRequestPath = true;
				break;
			}
		}
	}

	/**
	 * 初始化此类使用的HandlerAdapters。
	 * <p>如果在此命名空间的BeanFactory中未定义HandlerAdapter bean，则默认为SimpleControllerHandlerAdapter。
	 */
	private void initHandlerAdapters(ApplicationContext context) {
		// 将 handlerAdapters 设置为 null。
		this.handlerAdapters = null;

		// 如果要检测所有HandlerAdapters，则查找所有的 HandlerAdapters。
		if (this.detectAllHandlerAdapters) {
			// 在 ApplicationContext 中查找所有的 HandlerAdapters，包括祖先上下文。
			Map<String, HandlerAdapter> matchingBeans =
					BeanFactoryUtils.beansOfTypeIncludingAncestors(context, HandlerAdapter.class, true, false);
			// 如果找到匹配的 HandlerAdapters，则将其存储在 handlerAdapters 中。
			if (!matchingBeans.isEmpty()) {
				this.handlerAdapters = new ArrayList<>(matchingBeans.values());
				// 按排序顺序保留 HandlerAdapters。
				AnnotationAwareOrderComparator.sort(this.handlerAdapters);
			}
		} else {
			try {
				// 尝试从 ApplicationContext 中获取单个 HandlerAdapter。
				HandlerAdapter ha = context.getBean(HANDLER_ADAPTER_BEAN_NAME, HandlerAdapter.class);
				// 如果成功获取到 HandlerAdapter，则将其存储在 handlerAdapters 中。
				this.handlerAdapters = Collections.singletonList(ha);
			} catch (NoSuchBeanDefinitionException ex) {
				// 如果未找到指定的 HandlerAdapter，则忽略异常。
				// HandlerAdapters 将在稍后的 getDefaultStrategies 方法中添加默认策略。
			}
		}

		// 确保至少有一些 HandlerAdapters，如果 handlerAdapters 为 null，则使用默认策略。
		if (this.handlerAdapters == null) {
			// 从 DispatcherServlet.properties 中获取默认策略。
			this.handlerAdapters = getDefaultStrategies(context, HandlerAdapter.class);
			if (logger.isTraceEnabled()) {
				logger.trace("No HandlerAdapters declared for servlet '" + getServletName() +
						"': using default strategies from DispatcherServlet.properties");
			}
		}
	}

	/**
	 * 初始化此类使用的HandlerExceptionResolver。
	 * <p>如果在此命名空间的BeanFactory中未定义具有给定名称的bean，则默认为没有异常解析器。
	 */
	private void initHandlerExceptionResolvers(ApplicationContext context) {
		// 将 handlerExceptionResolvers 设置为 null。
		this.handlerExceptionResolvers = null;

		// 如果要检测所有处理器异常解析器，则查找所有的 处理器异常解析器。
		if (this.detectAllHandlerExceptionResolvers) {
			// 在 ApplicationContext 中查找所有的 处理器异常解析器，包括祖先上下文。
			Map<String, HandlerExceptionResolver> matchingBeans = BeanFactoryUtils
					.beansOfTypeIncludingAncestors(context, HandlerExceptionResolver.class, true, false);
			// 如果找到匹配的 处理器异常解析器，则将其存储在 handlerExceptionResolvers 中。
			if (!matchingBeans.isEmpty()) {
				this.handlerExceptionResolvers = new ArrayList<>(matchingBeans.values());
				// 按排序顺序保留 HandlerExceptionResolvers。
				AnnotationAwareOrderComparator.sort(this.handlerExceptionResolvers);
			}
		} else {
			try {
				// 尝试从 ApplicationContext 中获取单个 处理器异常解析器。
				HandlerExceptionResolver her =
						context.getBean(HANDLER_EXCEPTION_RESOLVER_BEAN_NAME, HandlerExceptionResolver.class);
				// 如果成功获取到 处理器异常解析器，则将其存储在 handlerExceptionResolvers 中。
				this.handlerExceptionResolvers = Collections.singletonList(her);
			} catch (NoSuchBeanDefinitionException ex) {
				// 如果未找到指定的 HandlerExceptionResolver，则忽略异常。
				// HandlerExceptionResolvers 将在稍后的 getDefaultStrategies 方法中添加默认策略。
			}
		}

		// 确保至少有一些 HandlerExceptionResolvers，如果 handlerExceptionResolvers 为 null，则使用默认策略。
		if (this.handlerExceptionResolvers == null) {
			// 从 DispatcherServlet.properties 中获取默认策略。
			this.handlerExceptionResolvers = getDefaultStrategies(context, HandlerExceptionResolver.class);
			if (logger.isTraceEnabled()) {
				logger.trace("No HandlerExceptionResolvers declared in servlet '" + getServletName() +
						"': using default strategies from DispatcherServlet.properties");
			}
		}
	}

	/**
	 * 初始化此servlet实例使用的RequestToViewNameTranslator。
	 * <p>如果未配置任何实现，则默认为DefaultRequestToViewNameTranslator。
	 */
	private void initRequestToViewNameTranslator(ApplicationContext context) {
		try {
			// 尝试从 ApplicationContext 中获取 RequestToViewNameTranslator 实例。
			this.viewNameTranslator =
					context.getBean(REQUEST_TO_VIEW_NAME_TRANSLATOR_BEAN_NAME, RequestToViewNameTranslator.class);
			if (logger.isTraceEnabled()) {
				logger.trace("Detected " + this.viewNameTranslator.getClass().getSimpleName());
			} else if (logger.isDebugEnabled()) {
				logger.debug("Detected " + this.viewNameTranslator);
			}
		} catch (NoSuchBeanDefinitionException ex) {
			// 如果未找到指定的 RequestToViewNameTranslator，则使用默认策略。
			this.viewNameTranslator = getDefaultStrategy(context, RequestToViewNameTranslator.class);
			// 如果跟踪日志级别已启用，则记录未找到 RequestToViewNameTranslator 的消息。
			if (logger.isTraceEnabled()) {
				logger.trace("No RequestToViewNameTranslator '" + REQUEST_TO_VIEW_NAME_TRANSLATOR_BEAN_NAME +
						"': using default [" + this.viewNameTranslator.getClass().getSimpleName() + "]");
			}
		}
	}

	/**
	 * 初始化此类使用的ViewResolvers。
	 * <p>如果在此命名空间的BeanFactory中未定义ViewResolver bean，则默认为InternalResourceViewResolver。
	 */
	private void initViewResolvers(ApplicationContext context) {
		this.viewResolvers = null;

		// 如果要检测所有的视图解析器
		if (this.detectAllViewResolvers) {
			// 在 ApplicationContext 中查找所有的 视图解析器，包括祖先上下文。
			Map<String, ViewResolver> matchingBeans =
					BeanFactoryUtils.beansOfTypeIncludingAncestors(context, ViewResolver.class, true, false);
			// 如果找到任意的 视图解析器 实例，则添加到列表中。
			if (!matchingBeans.isEmpty()) {
				this.viewResolvers = new ArrayList<>(matchingBeans.values());
				// 保持 ViewResolvers 的排序顺序。
				AnnotationAwareOrderComparator.sort(this.viewResolvers);
			}
		} else {
			try {
				// 尝试从 ApplicationContext 中获取单个 视图解析器。
				ViewResolver vr = context.getBean(VIEW_RESOLVER_BEAN_NAME, ViewResolver.class);
				// 如果找到 视图解析器 实例，则创建包含该实例的单例列表。
				this.viewResolvers = Collections.singletonList(vr);
			} catch (NoSuchBeanDefinitionException ex) {
				// 忽略，稍后将添加一个默认的 ViewResolver。
			}
		}

		// 确保我们至少有一个 ViewResolver，通过注册默认的 ViewResolver 来实现
		// 如果没有找到其他的 ViewResolver 实例。
		if (this.viewResolvers == null) {
			// 如果未找到指定的 视图解析器，则使用默认策略。
			this.viewResolvers = getDefaultStrategies(context, ViewResolver.class);
			if (logger.isTraceEnabled()) {
				logger.trace("No ViewResolvers declared for servlet '" + getServletName() +
						"': using default strategies from DispatcherServlet.properties");
			}
		}
	}

	/**
	 * 初始化此servlet实例使用的FlashMapManager。
	 * <p>如果未配置任何实现，则默认为org.springframework.web.servlet.support.DefaultFlashMapManager。
	 */
	private void initFlashMapManager(ApplicationContext context) {
		try {
			// 尝试从 ApplicationContext 中获取 单个 FlashMapManager 实例。
			this.flashMapManager = context.getBean(FLASH_MAP_MANAGER_BEAN_NAME, FlashMapManager.class);
			if (logger.isTraceEnabled()) {
				logger.trace("Detected " + this.flashMapManager.getClass().getSimpleName());
			} else if (logger.isDebugEnabled()) {
				logger.debug("Detected " + this.flashMapManager);
			}
		} catch (NoSuchBeanDefinitionException ex) {
			// 忽略，稍后将使用默认值。
			this.flashMapManager = getDefaultStrategy(context, FlashMapManager.class);
			if (logger.isTraceEnabled()) {
				logger.trace("No FlashMapManager '" + FLASH_MAP_MANAGER_BEAN_NAME +
						"': using default [" + this.flashMapManager.getClass().getSimpleName() + "]");
			}
		}
	}

	/**
	 * 返回此servlet的ThemeSource（如果有）；否则返回null。
	 * <p>默认情况下，如果WebApplicationContext实现了ThemeSource接口，则返回WebApplicationContext作为ThemeSource。
	 *
	 * @return ThemeSource（如果有）
	 * @see #getWebApplicationContext()
	 */
	@Nullable
	public final ThemeSource getThemeSource() {
		return (getWebApplicationContext() instanceof ThemeSource ? (ThemeSource) getWebApplicationContext() : null);
	}

	/**
	 * 获取此servlet的MultipartResolver（如果有）。
	 *
	 * @return 此servlet使用的MultipartResolver，如果没有（表示没有可用的多部分支持）则返回null
	 */
	@Nullable
	public final MultipartResolver getMultipartResolver() {
		return this.multipartResolver;
	}

	/**
	 * 返回在WebApplicationContext中按类型检测到或基于{@literal DispatcherServlet.properties}中的默认策略集初始化的已配置的HandlerMapping bean。
	 * <p><strong>注意：</strong>如果在调用{@link #onRefresh(ApplicationContext)}之前调用此方法，则可能返回{@code null}。
	 *
	 * @return 配置的映射的不可变列表，如果尚未初始化，则返回{@code null}
	 * @since 5.0
	 */
	@Nullable
	public final List<HandlerMapping> getHandlerMappings() {
		return (this.handlerMappings != null ? Collections.unmodifiableList(this.handlerMappings) : null);
	}

	/**
	 * 返回给定策略接口的默认策略对象。
	 * <p>默认实现委托给{@link #getDefaultStrategies}，期望列表中有一个对象。
	 *
	 * @param context           当前的WebApplicationContext
	 * @param strategyInterface 策略接口
	 * @return 对应的策略对象
	 * @see #getDefaultStrategies
	 */
	protected <T> T getDefaultStrategy(ApplicationContext context, Class<T> strategyInterface) {
		List<T> strategies = getDefaultStrategies(context, strategyInterface);
		if (strategies.size() != 1) {
			throw new BeanInitializationException(
					"DispatcherServlet needs exactly 1 strategy for interface [" + strategyInterface.getName() + "]");
		}
		return strategies.get(0);
	}

	/**
	 * 为给定的策略接口创建默认策略对象列表。
	 * <p>默认实现使用“DispatcherServlet.properties”文件（与DispatcherServlet类位于相同的包中）来确定类名。它通过上下文的BeanFactory实例化策略对象。
	 *
	 * @param context           当前的WebApplicationContext
	 * @param strategyInterface 策略接口
	 * @return 对应的策略对象列表
	 */
	@SuppressWarnings("unchecked")
	protected <T> List<T> getDefaultStrategies(ApplicationContext context, Class<T> strategyInterface) {
		if (defaultStrategies == null) {
			try {
				// 从属性文件中加载默认策略实现。
				// 目前这是严格内部使用的，不是供应用程序开发人员自定义的。
				ClassPathResource resource = new ClassPathResource(DEFAULT_STRATEGIES_PATH, DispatcherServlet.class);
				defaultStrategies = PropertiesLoaderUtils.loadProperties(resource);
			} catch (IOException ex) {
				throw new IllegalStateException("Could not load '" + DEFAULT_STRATEGIES_PATH + "': " + ex.getMessage());
			}
		}

		String key = strategyInterface.getName();
		String value = defaultStrategies.getProperty(key);
		if (value != null) {
			// 将字符串值拆分为类名数组。
			String[] classNames = StringUtils.commaDelimitedListToStringArray(value);
			List<T> strategies = new ArrayList<>(classNames.length);
			for (String className : classNames) {
				try {
					// 加载类并创建默认策略实例。
					Class<?> clazz = ClassUtils.forName(className, DispatcherServlet.class.getClassLoader());
					// 创建默认策略
					Object strategy = createDefaultStrategy(context, clazz);
					// 将策略添加到列表中。
					strategies.add((T) strategy);
				} catch (ClassNotFoundException ex) {
					throw new BeanInitializationException(
							"Could not find DispatcherServlet's default strategy class [" + className +
									"] for interface [" + key + "]", ex);
				} catch (LinkageError err) {
					throw new BeanInitializationException(
							"Unresolvable class definition for DispatcherServlet's default strategy class [" +
									className + "] for interface [" + key + "]", err);
				}
			}
			// 返回策略列表。
			return strategies;
		} else {
			// 如果未找到给定接口的默认策略，则返回空列表。
			return Collections.emptyList();
		}
	}

	/**
	 * 创建默认策略。
	 * <p>默认实现使用
	 * {@link org.springframework.beans.factory.config.AutowireCapableBeanFactory#createBean}。
	 *
	 * @param context 当前的WebApplicationContext
	 * @param clazz   要实例化的策略实现类
	 * @return 完全配置的策略实例
	 * @see org.springframework.context.ApplicationContext#getAutowireCapableBeanFactory()
	 * @see org.springframework.beans.factory.config.AutowireCapableBeanFactory#createBean
	 */
	protected Object createDefaultStrategy(ApplicationContext context, Class<?> clazz) {
		return context.getAutowireCapableBeanFactory().createBean(clazz);
	}


	/**
	 * 公开DispatcherServlet特定的请求属性并委托给{@link #doDispatch}来执行实际的分派。
	 */
	@Override
	protected void doService(HttpServletRequest request, HttpServletResponse response) throws Exception {
		// 记录请求
		logRequest(request);

		// 在包含请求时保留请求属性的快照，以便在包含后能够恢复原始属性。
		Map<String, Object> attributesSnapshot = null;
		if (WebUtils.isIncludeRequest(request)) {
			// 如果请求中含有保留属性，创建属性快照
			attributesSnapshot = new HashMap<>();
			Enumeration<?> attrNames = request.getAttributeNames();
			while (attrNames.hasMoreElements()) {
				String attrName = (String) attrNames.nextElement();
				// 如果需要在包含后清理属性，或者属性以默认策略前缀开头，则将其放入属性快照中。
				if (this.cleanupAfterInclude || attrName.startsWith(DEFAULT_STRATEGIES_PREFIX)) {
					attributesSnapshot.put(attrName, request.getAttribute(attrName));
				}
			}
		}

		// 将框架对象提供给处理程序和视图对象。
		// 在请求中设置WebApplicationContext
		request.setAttribute(WEB_APPLICATION_CONTEXT_ATTRIBUTE, getWebApplicationContext());
		// 在请求中设置本地化解析器
		request.setAttribute(LOCALE_RESOLVER_ATTRIBUTE, this.localeResolver);
		// 在请求中设置主题解析器
		request.setAttribute(THEME_RESOLVER_ATTRIBUTE, this.themeResolver);
		// 在请求中设置主题
		request.setAttribute(THEME_SOURCE_ATTRIBUTE, getThemeSource());

		// 如果存在闪存映射管理器，则处理闪存映射。
		if (this.flashMapManager != null) {
			// 检索和更新闪存映射
			FlashMap inputFlashMap = this.flashMapManager.retrieveAndUpdate(request, response);
			if (inputFlashMap != null) {
				// 将输入闪存映射放入请求属性中，以便处理程序和视图可以访问。
				request.setAttribute(INPUT_FLASH_MAP_ATTRIBUTE, Collections.unmodifiableMap(inputFlashMap));
			}
			// 创建一个新的输出闪存映射并将其放入请求属性中。
			request.setAttribute(OUTPUT_FLASH_MAP_ATTRIBUTE, new FlashMap());
			// 将闪存映射管理器本身也放入请求属性中。
			request.setAttribute(FLASH_MAP_MANAGER_ATTRIBUTE, this.flashMapManager);
		}

		// 如果需要解析请求路径，则解析并缓存请求路径。
		RequestPath previousRequestPath = null;
		if (this.parseRequestPath) {
			previousRequestPath = (RequestPath) request.getAttribute(ServletRequestPathUtils.PATH_ATTRIBUTE);
			ServletRequestPathUtils.parseAndCache(request);
		}

		try {
			// 执行请求分派
			doDispatch(request, response);
		} finally {
			if (!WebAsyncUtils.getAsyncManager(request).isConcurrentHandlingStarted()) {
				// 如果请求未异步处理，则在包含后恢复原始属性快照。
				if (attributesSnapshot != null) {
					restoreAttributesAfterInclude(request, attributesSnapshot);
				}
			}
			// 如果需要解析请求路径，则将原始请求路径重新设置回请求属性中。
			if (this.parseRequestPath) {
				ServletRequestPathUtils.setParsedRequestPath(previousRequestPath, request);
			}
		}
	}

	private void logRequest(HttpServletRequest request) {
		LogFormatUtils.traceDebug(logger, traceOn -> {
			String params;
			if (StringUtils.startsWithIgnoreCase(request.getContentType(), "multipart/")) {
				// 如果请求类型以"multipart/"开头，则参数为"multipart"。
				params = "multipart";
			} else if (isEnableLoggingRequestDetails()) {
				// 如果允许记录请求细节，则构建参数字符串。
				params = request.getParameterMap().entrySet().stream()
						.map(entry -> entry.getKey() + ":" + Arrays.toString(entry.getValue()))
						.collect(Collectors.joining(", "));
			} else {
				// 否则，如果参数映射为空，则参数为""；否则，参数为"masked"。
				params = (request.getParameterMap().isEmpty() ? "" : "masked");
			}

			String queryString = request.getQueryString();
			String queryClause = (StringUtils.hasLength(queryString) ? "?" + queryString : "");
			String dispatchType = (!DispatcherType.REQUEST.equals(request.getDispatcherType()) ?
					// 如果调度类型不是请求，则为调度类型，否则为空字符串。
					"\"" + request.getDispatcherType() + "\" dispatch for " : "");
			String message = (dispatchType + request.getMethod() + " \"" + getRequestUri(request) +
					queryClause + "\", parameters={" + params + "}");

			if (traceOn) {
				List<String> values = Collections.list(request.getHeaderNames());
				String headers = values.size() > 0 ? "masked" : "";
				if (isEnableLoggingRequestDetails()) {
					// 如果允许记录请求细节，则构建请求头字符串。
					headers = values.stream().map(name -> name + ":" + Collections.list(request.getHeaders(name)))
							.collect(Collectors.joining(", "));
				}
				// 返回包含请求消息和请求头的完整消息。
				return message + ", headers={" + headers + "} in DispatcherServlet '" + getServletName() + "'";
			} else {
				// 如果不跟踪请求，则只返回请求消息。
				return message;
			}
		});
	}

	/**
	 * 处理实际的请求分派到处理程序。
	 * <p>将通过按顺序应用Servlet的HandlerMappings来获取处理程序。
	 * 将通过查询Servlet安装的HandlerAdapters来获取HandlerAdapter，
	 * 找到支持处理程序类的第一个HandlerAdapter。
	 * <p>此方法处理所有HTTP方法。 HandlerAdapters或处理程序本身决定接受哪些方法。
	 *
	 * @param request  当前HTTP请求
	 * @param response 当前HTTP响应
	 * @throws Exception 在任何类型的处理失败时
	 */
	@SuppressWarnings("deprecation")
	protected void doDispatch(HttpServletRequest request, HttpServletResponse response) throws Exception {
		HttpServletRequest processedRequest = request;
		HandlerExecutionChain mappedHandler = null;
		boolean multipartRequestParsed = false;
		// 获取异步管理器
		WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);

		try {
			ModelAndView mv = null;
			Exception dispatchException = null;

			try {
				// 检查是否为多部分请求，返回处理后的请求对象。
				processedRequest = checkMultipart(request);
				// 是否已解析多部分请求
				multipartRequestParsed = (processedRequest != request);

				// 确定当前请求的处理程序。
				mappedHandler = getHandler(processedRequest);
				if (mappedHandler == null) {
					// 如果未找到处理程序，则触发"noHandlerFound"方法。
					noHandlerFound(processedRequest, response);
					return;
				}

				// 确定当前请求的处理程序适配器。
				HandlerAdapter ha = getHandlerAdapter(mappedHandler.getHandler());

				// 如果处理程序支持最后修改的头，则处理最后修改的头。
				String method = request.getMethod();
				boolean isGet = HttpMethod.GET.matches(method);
				if (isGet || HttpMethod.HEAD.matches(method)) {
					// 如果当前请求的方法是Get方法或者是Head方法
					// 获取最后修改的时间戳
					long lastModified = ha.getLastModified(request, mappedHandler.getHandler());
					if (new ServletWebRequest(request, response).checkNotModified(lastModified) && isGet) {
						// 如果是 Get 请求并且是没有修改过，则直接结束
						return;
					}
				}

				if (!mappedHandler.applyPreHandle(processedRequest, response)) {
					// 没有通过拦截器，直接返回
					return;
				}

				// 实际调用处理程序。
				mv = ha.handle(processedRequest, response, mappedHandler.getHandler());

				if (asyncManager.isConcurrentHandlingStarted()) {
					// 如果Web异步处理器已经开始并发处理了，直接结束
					return;
				}

				// 应用默认视图名称。
				applyDefaultViewName(processedRequest, mv);
				// 应用处理程序后处理。
				mappedHandler.applyPostHandle(processedRequest, response, mv);
			} catch (Exception ex) {
				dispatchException = ex;
			} catch (Throwable err) {
				// 处理器方法抛出的错误也将被处理。
				dispatchException = new NestedServletException("Handler dispatch failed", err);
			}
			// 处理调度结果。
			processDispatchResult(processedRequest, response, mappedHandler, mv, dispatchException);
		} catch (Exception ex) {
			// 处理异常。
			triggerAfterCompletion(processedRequest, response, mappedHandler, ex);
		} catch (Throwable err) {
			// 处理错误。
			triggerAfterCompletion(processedRequest, response, mappedHandler,
					new NestedServletException("Handler processing failed", err));
		} finally {
			if (asyncManager.isConcurrentHandlingStarted()) {
				// 如果是异步处理，则应用并发处理开始后的操作。
				if (mappedHandler != null) {
					mappedHandler.applyAfterConcurrentHandlingStarted(processedRequest, response);
				}
			} else {
				// 如果不是异步处理，则清理多部分请求使用的任何资源。
				if (multipartRequestParsed) {
					cleanupMultipart(processedRequest);
				}
			}
		}
	}

	/**
	 * 是否需要视图名称转换？
	 *
	 * @param request 当前的 HTTP 请求
	 * @param mv      ModelAndView 对象，可能为 null
	 * @throws Exception 如果在视图名称转换过程中发生异常
	 */
	private void applyDefaultViewName(HttpServletRequest request, @Nullable ModelAndView mv) throws Exception {
		if (mv != null && !mv.hasView()) {
			// 如果 ModelAndView 存在且没有设置视图，则尝试设置默认视图名称
			String defaultViewName = getDefaultViewName(request);
			if (defaultViewName != null) {
				mv.setViewName(defaultViewName);
			}
		}
	}

	/**
	 * 处理处理程序选择和处理程序调用的结果，该结果可能是一个 ModelAndView 或要解析为 ModelAndView 的异常。
	 *
	 * @param request       当前的 HTTP 请求
	 * @param response      当前的 HTTP 响应
	 * @param mappedHandler 已映射的处理程序链，可能为 null
	 * @param mv            ModelAndView 对象，可能为 null
	 * @param exception     异常对象，可能为 null
	 * @throws Exception 如果在处理结果过程中发生异常
	 */
	private void processDispatchResult(HttpServletRequest request, HttpServletResponse response,
									   @Nullable HandlerExecutionChain mappedHandler, @Nullable ModelAndView mv,
									   @Nullable Exception exception) throws Exception {

		boolean errorView = false;

		if (exception != null) {
			if (exception instanceof ModelAndViewDefiningException) {
				// 如果异常是 ModelAndViewDefiningException 类型，则直接提取 ModelAndView 对象
				logger.debug("ModelAndViewDefiningException encountered", exception);
				mv = ((ModelAndViewDefiningException) exception).getModelAndView();
			} else {
				// 否则，处理异常
				Object handler = (mappedHandler != null ? mappedHandler.getHandler() : null);
				mv = processHandlerException(request, response, handler, exception);
				errorView = (mv != null);
			}
		}

		// 处理程序是否返回了要渲染的视图？
		if (mv != null && !mv.wasCleared()) {
			// 如果存在 ModelAndView 对象且未被清除，则进行渲染
			render(mv, request, response);
			if (errorView) {
				// 如果是错误视图，则清除错误相关的请求属性
				WebUtils.clearErrorRequestAttributes(request);
			}
		} else {
			if (logger.isTraceEnabled()) {
				logger.trace("No view rendering, null ModelAndView returned.");
			}
		}

		if (WebAsyncUtils.getAsyncManager(request).isConcurrentHandlingStarted()) {
			// 如果异步处理已启动，则直接返回
			return;
		}

		if (mappedHandler != null) {
			// 触发处理程序链的 afterCompletion 方法
			mappedHandler.triggerAfterCompletion(request, response, null);
		}
	}

	/**
	 * 为给定请求构建 LocaleContext，将请求的主要区域设置为当前区域。
	 * <p>默认实现使用 Dispatcher 的 LocaleResolver 来获取当前区域，该区域在请求过程中可能会发生变化。
	 *
	 * @param request 当前的 HTTP 请求
	 * @return 对应的 LocaleContext
	 */
	@Override
	protected LocaleContext buildLocaleContext(final HttpServletRequest request) {
		LocaleResolver lr = this.localeResolver;
		if (lr instanceof LocaleContextResolver) {
			return ((LocaleContextResolver) lr).resolveLocaleContext(request);
		} else {
			return () -> (lr != null ? lr.resolveLocale(request) : request.getLocale());
		}
	}

	/**
	 * 将请求转换为多部分请求，并使多部分解析器可用。
	 * <p>如果未设置多部分解析器，则简单地使用现有请求。
	 *
	 * @param request 当前的 HTTP 请求
	 * @return 处理后的请求（如果需要，返回多部分包装器）
	 * @throws MultipartException 如果多部分处理过程中发生异常
	 * @see MultipartResolver#resolveMultipart
	 */
	protected HttpServletRequest checkMultipart(HttpServletRequest request) throws MultipartException {
		if (this.multipartResolver != null && this.multipartResolver.isMultipart(request)) {
			// 如果请求是多部分请求，则执行以下操作。
			if (WebUtils.getNativeRequest(request, MultipartHttpServletRequest.class) != null) {
				// 如果请求已经解析为MultipartHttpServletRequest，则记录跟踪消息。
				if (DispatcherType.REQUEST.equals(request.getDispatcherType())) {
					logger.trace("Request already resolved to MultipartHttpServletRequest, e.g. by MultipartFilter");
				}
			} else if (hasMultipartException(request)) {
				// 如果之前的多部分解析失败，则跳过重新解析。
				logger.debug("Multipart resolution previously failed for current request - " +
						"skipping re-resolution for undisturbed error rendering");
			} else {
				try {
					// 尝试解析多部分请求。
					return this.multipartResolver.resolveMultipart(request);
				} catch (MultipartException ex) {
					if (request.getAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE) != null) {
						// 如果在错误调度期间出现多部分解析失败，则记录错误信息。
						logger.debug("Multipart resolution failed for error dispatch", ex);
						// 在错误调度时继续处理错误请求
					} else {
						// 否则抛出多部分异常。
						throw ex;
					}
				}
			}
		}
		// 如果未返回，则返回原始请求。
		return request;
	}

	/**
	 * 检查 "javax.servlet.error.exception" 属性是否包含多部分异常。
	 *
	 * @param request 当前的 HTTP 请求
	 * @return 如果存在多部分异常则返回 true
	 */
	private boolean hasMultipartException(HttpServletRequest request) {
		Throwable error = (Throwable) request.getAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE);
		while (error != null) {
			if (error instanceof MultipartException) {
				return true;
			}
			error = error.getCause();
		}
		return false;
	}

	/**
	 * 清理给定多部分请求使用的任何资源（如果有）。
	 *
	 * @param request 当前的 HTTP 请求
	 * @see MultipartResolver#cleanupMultipart
	 */
	protected void cleanupMultipart(HttpServletRequest request) {
		if (this.multipartResolver != null) {
			// 获取原生的MultipartHttpServletRequest。
			MultipartHttpServletRequest multipartRequest =
					WebUtils.getNativeRequest(request, MultipartHttpServletRequest.class);
			if (multipartRequest != null) {
				// 如果multipartRequest不为空，则清理多部分请求。
				this.multipartResolver.cleanupMultipart(multipartRequest);
			}
		}
	}

	/**
	 * 返回此请求的 HandlerExecutionChain。
	 * <p>按顺序尝试所有处理程序映射。
	 *
	 * @param request 当前的 HTTP 请求
	 * @return HandlerExecutionChain，如果未找到处理程序则返回 {@code null}
	 * @throws Exception 如果处理 HandlerExecutionChain 时出现异常
	 */
	@Nullable
	protected HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception {
		if (this.handlerMappings != null) {
			// 如果存在处理程序映射列表，则遍历处理程序映射列表。
			for (HandlerMapping mapping : this.handlerMappings) {
				// 获取当前请求的处理程序执行链。
				HandlerExecutionChain handler = mapping.getHandler(request);
				if (handler != null) {
					// 如果处理程序执行链不为空，则返回处理程序执行链。
					return handler;
				}
			}
		}
		// 如果处理程序映射列表为空或未找到处理程序执行链，则返回null。
		return null;
	}

	/**
	 * 未找到处理程序 &rarr; 设置适当的 HTTP 响应状态。
	 *
	 * @param request  当前的 HTTP 请求
	 * @param response 当前的 HTTP 响应
	 * @throws Exception 如果准备响应失败
	 */
	protected void noHandlerFound(HttpServletRequest request, HttpServletResponse response) throws Exception {
		if (pageNotFoundLogger.isWarnEnabled()) {
			// 如果页面未找到日志记录器已启用，则记录警告消息。
			pageNotFoundLogger.warn("No mapping for " + request.getMethod() + " " + getRequestUri(request));
		}
		if (this.throwExceptionIfNoHandlerFound) {
			// 如果设置了抛出异常选项，则抛出NoHandlerFoundException。
			throw new NoHandlerFoundException(request.getMethod(), getRequestUri(request),
					new ServletServerHttpRequest(request).getHeaders());
		} else {
			// 否则，发送404未找到响应。
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
		}
	}

	/**
	 * 为此处理程序对象返回 HandlerAdapter。
	 *
	 * @param handler 要查找适配器的处理程序对象
	 * @throws ServletException 如果无法为处理程序找到 HandlerAdapter。这是一个致命错误。
	 */
	protected HandlerAdapter getHandlerAdapter(Object handler) throws ServletException {
		if (this.handlerAdapters != null) {
			// 如果存在处理程序适配器列表，则遍历处理程序适配器列表。
			for (HandlerAdapter adapter : this.handlerAdapters) {
				// 如果适配器支持当前处理程序执行链，则返回该适配器。
				if (adapter.supports(handler)) {
					return adapter;
				}
			}
		}
		throw new ServletException("No adapter for handler [" + handler +
				"]: The DispatcherServlet configuration needs to include a HandlerAdapter that supports this handler");
	}

	/**
	 * 通过已注册的 HandlerExceptionResolver 确定错误 ModelAndView。
	 *
	 * @param request  当前的 HTTP 请求
	 * @param response 当前的 HTTP 响应
	 * @param handler  执行的处理程序，如果在异常时没有选择处理程序，则为 {@code null}
	 *                 （例如，如果 multipart 解析失败）
	 * @param ex       处理程序执行期间抛出的异常
	 * @return 用于转发的相应 ModelAndView
	 * @throws Exception 如果找不到错误 ModelAndView
	 */
	@Nullable
	protected ModelAndView processHandlerException(HttpServletRequest request, HttpServletResponse response,
												   @Nullable Object handler, Exception ex) throws Exception {

		// 成功和错误响应可能使用不同的内容类型
		request.removeAttribute(HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE);

		// 检查已注册的HandlerExceptionResolvers...
		ModelAndView exMv = null;
		if (this.handlerExceptionResolvers != null) {
			// 如果存在处理异常解析器列表，则遍历处理异常解析器列表。
			for (HandlerExceptionResolver resolver : this.handlerExceptionResolvers) {
				// 尝试由异常解析器解析异常。
				exMv = resolver.resolveException(request, response, handler, ex);
				if (exMv != null) {
					// 如果成功解析异常，则退出循环。
					break;
				}
			}
		}
		if (exMv != null) {
			// 如果解析到异常视图，处理异常的方式取决于解析结果。
			if (exMv.isEmpty()) {
				// 如果解析到的ModelAndView为空，则将异常设置为请求属性，并返回null。
				request.setAttribute(EXCEPTION_ATTRIBUTE, ex);
				return null;
			}
			// 如果解析到的ModelAndView不为空，则进一步处理。
			// 如果异常视图不包含视图名称，则尝试获取默认视图名称。
			if (!exMv.hasView()) {
				// 获取默认视图名称。
				String defaultViewName = getDefaultViewName(request);
				if (defaultViewName != null) {
					exMv.setViewName(defaultViewName);
				}
			}
			// 记录已解析的错误视图信息。
			if (logger.isTraceEnabled()) {
				logger.trace("Using resolved error view: " + exMv, ex);
			} else if (logger.isDebugEnabled()) {
				logger.debug("Using resolved error view: " + exMv);
			}
			// 将错误请求的属性暴露给视图。
			WebUtils.exposeErrorRequestAttributes(request, ex, getServletName());
			return exMv;
		}

		// 如果未解析到异常视图，则抛出原始异常。
		throw ex;
	}

	/**
	 * 渲染给定的 ModelAndView。
	 * <p>这是请求处理的最后阶段。它可能涉及通过名称解析视图。
	 *
	 * @param mv       要渲染的 ModelAndView
	 * @param request  当前的 HTTP servlet 请求
	 * @param response 当前的 HTTP servlet 响应
	 * @throws ServletException 如果视图丢失或无法解析
	 * @throws Exception        如果渲染视图时出现问题
	 */
	protected void render(ModelAndView mv, HttpServletRequest request, HttpServletResponse response) throws Exception {
		// 确定请求的区域设置，并将其应用于响应。
		Locale locale = (this.localeResolver != null ? this.localeResolver.resolveLocale(request) : request.getLocale());
		response.setLocale(locale);

		View view;
		String viewName = mv.getViewName();
		if (viewName != null) {
			// 需要解析视图名称。
			view = resolveViewName(viewName, mv.getModelInternal(), locale, request);
			if (view == null) {
				// 如果无法解析视图名称，则抛出ServletException。
				throw new ServletException("Could not resolve view with name '" + mv.getViewName() +
						"' in servlet with name '" + getServletName() + "'");
			}
		} else {
			// 不需要查找：ModelAndView对象包含实际的View对象。
			view = mv.getView();
			if (view == null) {
				// 如果ModelAndView既不包含视图名称也不包含View对象，则抛出ServletException。
				throw new ServletException("ModelAndView [" + mv + "] neither contains a view name nor a " +
						"View object in servlet with name '" + getServletName() + "'");
			}
		}

		// 委托给View对象进行渲染。
		if (logger.isTraceEnabled()) {
			logger.trace("Rendering view [" + view + "] ");
		}
		try {
			if (mv.getStatus() != null) {
				// 如果ModelAndView包含状态信息，则设置响应状态。
				request.setAttribute(View.RESPONSE_STATUS_ATTRIBUTE, mv.getStatus());
				response.setStatus(mv.getStatus().value());
			}
			// 渲染视图。
			view.render(mv.getModelInternal(), request, response);
		} catch (Exception ex) {
			// 如果渲染过程中发生异常，则记录错误并抛出异常。
			if (logger.isDebugEnabled()) {
				logger.debug("Error rendering view [" + view + "]", ex);
			}
			throw ex;
		}
	}

	/**
	 * 将提供的请求转换为默认视图名称。
	 *
	 * @param request 当前的 HTTP servlet 请求
	 * @return 视图名称（如果找不到默认视图，则为 {@code null}）
	 * @throws Exception 如果视图名称翻译失败
	 */
	@Nullable
	protected String getDefaultViewName(HttpServletRequest request) throws Exception {
		return (this.viewNameTranslator != null ? this.viewNameTranslator.getViewName(request) : null);
	}

	/**
	 * 将给定的视图名称解析为 View 对象（用于渲染）。
	 * <p>默认实现询问此分发器的所有 ViewResolver。
	 * 可以根据特定的模型属性或请求参数重写以进行自定义解析策略。
	 *
	 * @param viewName 要解析的视图名称
	 * @param model    要传递给视图的模型
	 * @param locale   当前的区域设置
	 * @param request  当前的 HTTP servlet 请求
	 * @return View 对象，如果找不到则为 {@code null}
	 * @throws Exception 如果无法解析视图
	 *                   （通常是因为无法创建实际的 View 对象）
	 * @see ViewResolver#resolveViewName
	 */
	@Nullable
	protected View resolveViewName(String viewName, @Nullable Map<String, Object> model,
								   Locale locale, HttpServletRequest request) throws Exception {

		if (this.viewResolvers != null) {
			// 如果存在视图解析器列表，则遍历视图解析器列表。
			for (ViewResolver viewResolver : this.viewResolvers) {
				// 尝试由视图解析器解析视图。
				View view = viewResolver.resolveViewName(viewName, locale);
				if (view != null) {
					// 如果成功解析视图，则返回该视图。
					return view;
				}
			}
		}
		// 如果未解析到视图，则返回null。
		return null;
	}

	private void triggerAfterCompletion(HttpServletRequest request, HttpServletResponse response,
										@Nullable HandlerExecutionChain mappedHandler, Exception ex) throws Exception {

		if (mappedHandler != null) {
			// 触发MappedHandler的afterCompletion方法。
			mappedHandler.triggerAfterCompletion(request, response, ex);
		}
		throw ex;
	}

	/**
	 * 在包含后恢复请求属性。
	 *
	 * @param request            当前的 HTTP 请求
	 * @param attributesSnapshot 包含前请求属性的快照
	 */
	@SuppressWarnings("unchecked")
	private void restoreAttributesAfterInclude(HttpServletRequest request, Map<?, ?> attributesSnapshot) {
		// 需要将属性复制到单独的集合中，以避免在删除属性时对Enumeration造成副作用。
		Set<String> attrsToCheck = new HashSet<>();
		Enumeration<?> attrNames = request.getAttributeNames();
		while (attrNames.hasMoreElements()) {
			String attrName = (String) attrNames.nextElement();
			// 如果需要清理或属性名称以默认策略前缀开头，则添加到要检查的属性集合中。
			if (this.cleanupAfterInclude || attrName.startsWith(DEFAULT_STRATEGIES_PREFIX)) {
				attrsToCheck.add(attrName);
			}
		}

		// 添加可能已删除的属性
		attrsToCheck.addAll((Set<String>) attributesSnapshot.keySet());

		// 遍历要检查的属性集合，恢复原始值或相应地删除属性。
		for (String attrName : attrsToCheck) {
			Object attrValue = attributesSnapshot.get(attrName);
			if (attrValue == null) {
				// 如果属性值为空，则从请求中删除属性。
				request.removeAttribute(attrName);
			} else if (attrValue != request.getAttribute(attrName)) {
				// 如果属性值不为空且与请求中的属性值不同，则将原始值设置回去。
				request.setAttribute(attrName, attrValue);
			}
		}
	}

	private static String getRequestUri(HttpServletRequest request) {
		String uri = (String) request.getAttribute(WebUtils.INCLUDE_REQUEST_URI_ATTRIBUTE);
		if (uri == null) {
			// 如果包含请求URI属性为空，则使用请求的URI。
			uri = request.getRequestURI();
		}
		return uri;
	}

}
