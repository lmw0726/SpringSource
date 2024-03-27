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

import org.springframework.beans.BeanUtils;
import org.springframework.context.*;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.SourceFilteringListener;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.i18n.SimpleLocaleContext;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.ConfigurableWebEnvironment;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.async.CallableProcessingInterceptor;
import org.springframework.web.context.request.async.WebAsyncManager;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.context.support.ServletRequestHandledEvent;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.context.support.XmlWebApplicationContext;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.util.NestedServletException;
import org.springframework.web.util.WebUtils;

import javax.servlet.DispatcherType;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/**
 * Spring Web框架的基础Servlet。提供与Spring应用程序上下文的集成，是基于JavaBean的整体解决方案。
 *
 * <p>此类提供以下功能：
 * <ul>
 * <li>管理每个Servlet的{@link org.springframework.web.context.WebApplicationContext WebApplicationContext}实例。
 * Servlet的配置由Servlet命名空间中的bean确定。
 * <li>在请求处理时发布事件，无论请求是否成功处理。
 * </ul>
 *
 * <p>子类必须实现{@link #doService}来处理请求。因为这扩展了{@link HttpServletBean}而不是直接扩展HttpServlet，
 * 所以bean属性会自动映射到它上面。子类可以覆盖{@link #initFrameworkServlet()}进行自定义初始化。
 *
 * <p>检测Servlet init-param级别上的“contextClass”参数，
 * 如果未找到，则回退到默认的上下文类{@link org.springframework.web.context.support.XmlWebApplicationContext}。
 * 请注意，使用默认的{@code FrameworkServlet}时，自定义上下文类需要实现{@link org.springframework.web.context.ConfigurableWebApplicationContext
 * ConfigurableWebApplicationContext} SPI。
 *
 * <p>接受一个可选的“contextInitializerClasses”servlet init-param，该参数指定一个或多个{@link org.springframework.context.ApplicationContextInitializer
 * ApplicationContextInitializer}类。托管的Web应用程序上下文将委托给这些初始化程序，允许进行附加的编程配置，
 * 例如添加属性源或针对{@linkplain org.springframework.context.ConfigurableApplicationContext#getEnvironment() 上下文的环境}激活配置文件。
 * 另请参见{@link org.springframework.web.context.ContextLoader}，其支持具有相同语义的“contextInitializerClasses”上下文参数，用于“根”Web应用程序上下文。
 *
 * <p>将“contextConfigLocation”servlet init-param传递到上下文实例，
 * 将其解析为可能包含多个文件路径的多个文件路径，这些路径可以由任意数量的逗号和空格分隔，例如“test-servlet.xml，myServlet.xml”。
 * 如果未明确指定，则上下文实现应该从Servlet的命名空间构建默认位置。
 *
 * <p>注意：在多个配置位置的情况下，后面加载的文件中的bean定义将覆盖在先前加载的文件中定义的bean，
 * 至少在使用Spring的默认ApplicationContext实现时是这样。这可以利用来通过额外的XML文件有意覆盖某些bean定义。
 *
 * <p>默认命名空间是“'servlet-name'-servlet”，例如，对于servlet-name“test”的servlet，
 * 使用的命名空间将解析为“test-servlet”（导致XmlWebApplicationContext具有“/WEB-INF/test-servlet.xml”默认位置）。
 * 也可以通过“namespace”servlet init-param显式设置命名空间。
 *
 * <p>从Spring 3.1开始，{@code FrameworkServlet}现在可以注入Web应用程序上下文，而不是在内部创建自己的Web应用程序上下文。
 * 这在支持Servlet 3.0+的环境中很有用，该环境支持Servlet实例的编程式注册。有关详细信息，请参见{@link #FrameworkServlet(WebApplicationContext)}
 * Javadoc。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Chris Beams
 * @author Rossen Stoyanchev
 * @author Phillip Webb
 * @see #doService
 * @see #setContextClass
 * @see #setContextConfigLocation
 * @see #setContextInitializerClasses
 * @see #setNamespace
 */
@SuppressWarnings("serial")
public abstract class FrameworkServlet extends HttpServletBean implements ApplicationContextAware {

	/**
	 * WebApplicationContext命名空间的后缀。如果在上下文中为此类的servlet给定了名称“test”，
	 * 则servlet使用的命名空间将解析为“test-servlet”。
	 */
	public static final String DEFAULT_NAMESPACE_SUFFIX = "-servlet";

	/**
	 * FrameworkServlet的默认上下文类。
	 *
	 * @see org.springframework.web.context.support.XmlWebApplicationContext
	 */
	public static final Class<?> DEFAULT_CONTEXT_CLASS = XmlWebApplicationContext.class;

	/**
	 * ServletContext属性的前缀，用于查找WebApplicationContext。
	 * 完成是servlet名称。
	 */
	public static final String SERVLET_CONTEXT_PREFIX = FrameworkServlet.class.getName() + ".CONTEXT.";

	/**
	 * 在单个init-param String值中，这些字符中的任意数量被视为分隔符。
	 */
	private static final String INIT_PARAM_DELIMITERS = ",; \t\n";


	/**
	 * 用于查找WebApplicationContext的ServletContext属性。
	 */
	@Nullable
	private String contextAttribute;

	/**
	 * 要创建的WebApplicationContext实现类。
	 */
	private Class<?> contextClass = DEFAULT_CONTEXT_CLASS;

	/**
	 * 要分配的WebApplicationContext id。
	 */
	@Nullable
	private String contextId;

	/**
	 * 此servlet的命名空间。
	 */
	@Nullable
	private String namespace;

	/**
	 * 显式的上下文配置位置。
	 */
	@Nullable
	private String contextConfigLocation;

	/**
	 * 要应用于上下文的实际ApplicationContextInitializer实例。
	 */
	private final List<ApplicationContextInitializer<ConfigurableApplicationContext>> contextInitializers =
			new ArrayList<>();

	/**
	 * 通过init参数设置的逗号分隔的ApplicationContextInitializer类名。
	 */
	@Nullable
	private String contextInitializerClasses;

	/**
	 * 是否将上下文发布为ServletContext属性？
	 */
	private boolean publishContext = true;

	/**
	 * 是否在每个请求结束时发布一个ServletRequestHandledEvent？
	 */
	private boolean publishEvents = true;

	/**
	 * 是否将LocaleContext和RequestAttributes公开为子线程可继承？
	 */
	private boolean threadContextInheritable = false;

	/**
	 * 是否将HTTP OPTIONS请求分派到{@link #doService}？
	 */
	private boolean dispatchOptionsRequest = false;

	/**
	 * 是否将HTTP TRACE请求分派到{@link #doService}？
	 */
	private boolean dispatchTraceRequest = false;

	/**
	 * 是否记录潜在敏感信息（在DEBUG时记录请求参数+在TRACE时记录标头）。
	 */
	private boolean enableLoggingRequestDetails = false;

	/**
	 * 此servlet的WebApplicationContext。
	 */
	@Nullable
	private WebApplicationContext webApplicationContext;

	/**
	 * 如果WebApplicationContext是通过{@link #setApplicationContext}注入的。
	 */
	private boolean webApplicationContextInjected = false;

	/**
	 * 用于检测onRefresh是否已调用的标志。
	 */
	private volatile boolean refreshEventReceived;

	/**
	 * 用于同步onRefresh执行的监视器。
	 */
	private final Object onRefreshMonitor = new Object();


	/**
	 * 创建一个新的{@code FrameworkServlet}，它将基于默认值和通过servlet init-params提供的值创建自己的内部web应用程序上下文。
	 * 通常在Servlet 2.5或更早版本的环境中使用，其中Servlet注册的唯一选项是通过{@code web.xml}使用无参构造函数。
	 * <p>调用{@link #setContextConfigLocation}（init-param 'contextConfigLocation'）将决定由
	 * {@linkplain #DEFAULT_CONTEXT_CLASS 默认XmlWebApplicationContext} 加载哪些XML文件
	 * <p>调用{@link #setContextClass}（init-param 'contextClass'）会覆盖默认的{@code XmlWebApplicationContext}，
	 * 并允许指定替代类，例如{@code AnnotationConfigWebApplicationContext}。
	 * <p>调用{@link #setContextInitializerClasses}（init-param 'contextInitializerClasses'）
	 * 指示应使用哪些{@link ApplicationContextInitializer}类来在刷新之前进一步配置内部应用程序上下文。
	 *
	 * @see #FrameworkServlet(WebApplicationContext)
	 */
	public FrameworkServlet() {
	}

	/**
	 * 创建一个新的{@code FrameworkServlet}，使用给定的Web应用程序上下文。在Servlet 3.0+环境中，通过{@link ServletContext#addServlet} API可以对Servlet进行基于实例的注册。
	 * <p>使用此构造函数表示以下属性/ init-params 将被忽略：
	 * <ul>
	 * <li>{@link #setContextClass(Class)} / 'contextClass'</li>
	 * <li>{@link #setContextConfigLocation(String)} / 'contextConfigLocation'</li>
	 * <li>{@link #setContextAttribute(String)} / 'contextAttribute'</li>
	 * <li>{@link #setNamespace(String)} / 'namespace'</li>
	 * </ul>
	 * <p>给定的Web应用程序上下文可能已经{@linkplain ConfigurableApplicationContext#refresh() 刷新}，也可能尚未刷新（推荐的方法）。
	 * 如果（a）它是{@link ConfigurableWebApplicationContext}的实现，并且（b）尚未刷新（推荐的方法），则将发生以下情况：
	 * <ul>
	 * <li>如果给定的上下文尚未具有{@linkplain ConfigurableApplicationContext#setParent 父级}，则将根应用程序上下文设置为父级。</li>
	 * <li>如果给定的上下文尚未被分配{@linkplain ConfigurableApplicationContext#setId ID}，则将为其分配ID。</li>
	 * <li>{@code ServletContext} 和 {@code ServletConfig} 对象将被委托给应用程序上下文。</li>
	 * <li>将调用{@link #postProcessWebApplicationContext}。</li>
	 * <li>将应用任何通过“contextInitializerClasses”init-param或通过{@link #setContextInitializers}属性指定的
	 * {@link ApplicationContextInitializer ApplicationContextInitializers}。</li>
	 * <li>将调用{@link ConfigurableApplicationContext#refresh refresh()}。</li>
	 * </ul>
	 * 如果上下文已经刷新或不实现{@code ConfigurableWebApplicationContext}，则假设用户已根据其特定需求执行了这些操作（或未执行）。
	 * <p>有关用法示例，请参见{@link org.springframework.web.WebApplicationInitializer}。
	 *
	 * @param webApplicationContext 要使用的上下文
	 * @see #initWebApplicationContext
	 * @see #configureAndRefreshWebApplicationContext
	 * @see org.springframework.web.WebApplicationInitializer
	 */
	public FrameworkServlet(WebApplicationContext webApplicationContext) {
		this.webApplicationContext = webApplicationContext;
	}


	/**
	 * 设置应该用于检索此servlet应该使用的{@link WebApplicationContext}的ServletContext属性的名称。
	 */
	public void setContextAttribute(@Nullable String contextAttribute) {
		this.contextAttribute = contextAttribute;
	}

	/**
	 * 返回应该用于检索此servlet应该使用的{@link WebApplicationContext}的ServletContext属性的名称。
	 */
	@Nullable
	public String getContextAttribute() {
		return this.contextAttribute;
	}

	/**
	 * 设置自定义的上下文类。此类必须是{@link org.springframework.web.context.WebApplicationContext}类型。
	 * <p>当使用默认的FrameworkServlet实现时，上下文类还必须实现{@link org.springframework.web.context.ConfigurableWebApplicationContext}接口。
	 *
	 * @see #createWebApplicationContext
	 */
	public void setContextClass(Class<?> contextClass) {
		this.contextClass = contextClass;
	}

	/**
	 * 返回自定义的上下文类。
	 */
	public Class<?> getContextClass() {
		return this.contextClass;
	}

	/**
	 * 指定自定义的WebApplicationContext id，用作底层BeanFactory的序列化id。
	 */
	public void setContextId(@Nullable String contextId) {
		this.contextId = contextId;
	}

	/**
	 * 返回自定义的WebApplicationContext id（如果有）。
	 */
	@Nullable
	public String getContextId() {
		return this.contextId;
	}

	/**
	 * 设置此servlet的自定义命名空间，用于构建默认上下文配置位置。
	 */
	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	/**
	 * 返回此servlet的命名空间，如果未设置自定义命名空间，则返回默认方案：例如，对于命名为“test”的servlet，“test-servlet”。
	 */
	public String getNamespace() {
		return (this.namespace != null ? this.namespace : getServletName() + DEFAULT_NAMESPACE_SUFFIX);
	}

	/**
	 * 显式设置上下文配置位置，而不是依赖于从命名空间构建的默认位置。此位置字符串可以包含由任意数量的逗号和空格分隔的多个位置。
	 */
	public void setContextConfigLocation(@Nullable String contextConfigLocation) {
		this.contextConfigLocation = contextConfigLocation;
	}

	/**
	 * 返回显式的上下文配置位置（如果有）。
	 */
	@Nullable
	public String getContextConfigLocation() {
		return this.contextConfigLocation;
	}

	/**
	 * 指定应该使用哪些{@link ApplicationContextInitializer}实例来初始化此{@code FrameworkServlet}使用的应用程序上下文。
	 *
	 * @see #configureAndRefreshWebApplicationContext
	 * @see #applyInitializers
	 */
	@SuppressWarnings("unchecked")
	public void setContextInitializers(@Nullable ApplicationContextInitializer<?>... initializers) {
		if (initializers != null) {
			for (ApplicationContextInitializer<?> initializer : initializers) {
				this.contextInitializers.add((ApplicationContextInitializer<ConfigurableApplicationContext>) initializer);
			}
		}
	}

	/**
	 * 指定完全限定的{@link ApplicationContextInitializer}类名称集合，即可选的“contextInitializerClasses”servlet init-param。
	 *
	 * @see #configureAndRefreshWebApplicationContext(ConfigurableWebApplicationContext)
	 * @see #applyInitializers(ConfigurableApplicationContext)
	 */
	public void setContextInitializerClasses(String contextInitializerClasses) {
		this.contextInitializerClasses = contextInitializerClasses;
	}

	/**
	 * 设置是否将此servlet的上下文发布为ServletContext属性，可供Web容器中的所有对象使用。默认为“true”。
	 * <p>这在测试期间特别方便，尽管让其他应用程序对象以这种方式访问上下文是否是一种良好的做法还有待商榷。
	 */
	public void setPublishContext(boolean publishContext) {
		this.publishContext = publishContext;
	}

	/**
	 * 设置此servlet是否应在每个请求结束时发布ServletRequestHandledEvent。默认为“true”；
	 * 可以关闭以稍微提高性能，前提是没有ApplicationListeners依赖于这些事件。
	 *
	 * @see org.springframework.web.context.support.ServletRequestHandledEvent
	 */
	public void setPublishEvents(boolean publishEvents) {
		this.publishEvents = publishEvents;
	}

	/**
	 * 设置是否将LocaleContext和RequestAttributes作为可继承的暴露给子线程（使用{@link java.lang.InheritableThreadLocal}）。
	 * <p>默认为“false”，以避免在生成的后台线程上产生副作用。
	 * 将其切换为“true”以启用继承，用于在请求处理期间生成并且仅在该请求中使用的自定义子线程
	 * （即，在其初始任务后结束，而不重新使用线程）。
	 * <p><b>警告：</b>如果您正在访问配置为根据需要（例如JDK {@link java.util.concurrent.ThreadPoolExecutor}）在需要时可能添加新线程的线程池，
	 * 则不要为子线程使用继承，因为这将使继承的上下文暴露给这样的池化线程。
	 */
	public void setThreadContextInheritable(boolean threadContextInheritable) {
		this.threadContextInheritable = threadContextInheritable;
	}

	/**
	 * 设置此servlet是否应将HTTP OPTIONS请求调度到{@link #doService}方法。
	 * <p>在{@code FrameworkServlet}中，默认值为“false”，应用{@link javax.servlet.http.HttpServlet}的默认行为
	 * （即枚举所有标准HTTP请求方法作为响应OPTIONS请求）。
	 * 但是请注意，从4.3开始，由于{@code DispatcherServlet}对OPTIONS的内置支持，
	 * 默认情况下将此属性设置为“true”。
	 * <p>如果您希望OPTIONS请求通过常规调度链传递，就像其他HTTP请求一样，则打开此标志。
	 * 这通常意味着您的控制器将接收这些请求；确保这些端点实际上能够处理OPTIONS请求。
	 * <p>请注意，如果您的控制器恰好没有设置'Allow'标头（作为OPTIONS响应所需的），则无论如何都会应用HttpServlet的默认OPTIONS处理。
	 */
	public void setDispatchOptionsRequest(boolean dispatchOptionsRequest) {
		this.dispatchOptionsRequest = dispatchOptionsRequest;
	}

	/**
	 * 设置此servlet是否应将HTTP TRACE请求调度到{@link #doService}方法。
	 * <p>默认为“false”，应用{@link javax.servlet.http.HttpServlet}的默认行为（即将接收到的消息反射给客户端）。
	 * <p>如果您希望TRACE请求通过常规调度链传递，就像其他HTTP请求一样，则打开此标志。
	 * 这通常意味着您的控制器将接收这些请求；确保这些端点实际上能够处理TRACE请求。
	 * <p>请注意，如果您的控制器恰好未生成内容类型为'message/http'的响应（作为TRACE响应所需的），
	 * 则无论如何都会应用HttpServlet的默认TRACE处理。
	 */
	public void setDispatchTraceRequest(boolean dispatchTraceRequest) {
		this.dispatchTraceRequest = dispatchTraceRequest;
	}

	/**
	 * 是否记录请求参数和标头的DEBUG级别和TRACE级别的日志。两者都可能包含敏感信息。
	 * <p>默认设置为{@code false}，以便不显示请求详细信息。
	 *
	 * @param enable 是否启用
	 * @since 5.1
	 */
	public void setEnableLoggingRequestDetails(boolean enable) {
		this.enableLoggingRequestDetails = enable;
	}

	/**
	 * 是否允许在DEBUG和TRACE级别记录可能敏感的请求详细信息。
	 *
	 * @since 5.1
	 */
	public boolean isEnableLoggingRequestDetails() {
		return this.enableLoggingRequestDetails;
	}

	/**
	 * 由Spring通过{@link ApplicationContextAware}调用以注入当前应用程序上下文。
	 * 此方法允许FrameworkServlets注册为Spring bean，而不是{@link #findWebApplicationContext()找到}
	 * {@link org.springframework.web.context.ContextLoaderListener引导}上下文。
	 * <p>主要添加以支持在嵌入式Servlet容器中的使用。
	 *
	 * @since 4.0
	 */
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		if (this.webApplicationContext == null && applicationContext instanceof WebApplicationContext) {
			this.webApplicationContext = (WebApplicationContext) applicationContext;
			this.webApplicationContextInjected = true;
		}
	}


	/**
	 * 覆盖了{@link HttpServletBean}的方法，在设置任何bean属性后调用。
	 * 创建此servlet的WebApplicationContext。
	 */
	@Override
	protected final void initServletBean() throws ServletException {
		getServletContext().log("Initializing Spring " + getClass().getSimpleName() + " '" + getServletName() + "'");
		if (logger.isInfoEnabled()) {
			logger.info("Initializing Servlet '" + getServletName() + "'");
		}
		long startTime = System.currentTimeMillis();

		try {
			// 初始化Web应用程序上下文
			this.webApplicationContext = initWebApplicationContext();
			// 初始化框架Servlet
			initFrameworkServlet();
		} catch (ServletException | RuntimeException ex) {
			logger.error("Context initialization failed", ex);
			throw ex;
		}

		if (logger.isDebugEnabled()) {
			String value = this.enableLoggingRequestDetails ?
					"shown which may lead to unsafe logging of potentially sensitive data" :
					"masked to prevent unsafe logging of potentially sensitive data";
			logger.debug("enableLoggingRequestDetails='" + this.enableLoggingRequestDetails +
					"': request parameters and headers will be " + value);
		}

		if (logger.isInfoEnabled()) {
			logger.info("Completed initialization in " + (System.currentTimeMillis() - startTime) + " ms");
		}
	}

	/**
	 * 初始化并发布此servlet的WebApplicationContext。
	 * <p>委托给{@link #createWebApplicationContext}来实际创建上下文。可以在子类中进行重写。
	 *
	 * @return WebApplicationContext实例
	 * @see #FrameworkServlet(WebApplicationContext)
	 * @see #setContextClass
	 * @see #setContextConfigLocation
	 */
	protected WebApplicationContext initWebApplicationContext() {
		WebApplicationContext rootContext =
				WebApplicationContextUtils.getWebApplicationContext(getServletContext());
		WebApplicationContext wac = null;

		if (this.webApplicationContext != null) {
			// 在构造时注入了上下文实例->使用它
			wac = this.webApplicationContext;
			if (wac instanceof ConfigurableWebApplicationContext) {
				ConfigurableWebApplicationContext cwac = (ConfigurableWebApplicationContext) wac;
				if (!cwac.isActive()) {
					// 上下文尚未刷新->提供诸如设置父上下文、设置应用程序上下文ID等服务
					if (cwac.getParent() == null) {
						// 没有显式指定父上下文->将根应用程序上下文（如果有的话；可能为null）设置为父上下文
						cwac.setParent(rootContext);
					}
					// 配置和刷新Web应用程序上下文
					configureAndRefreshWebApplicationContext(cwac);
				}
			}
		}
		if (wac == null) {
			// 在构造时未注入上下文实例->查看是否在servlet上下文中注册了一个。如果存在，则假定已经设置了父上下文（如果有的话）
			// 并且用户已执行了任何初始化，例如设置上下文ID
			wac = findWebApplicationContext();
		}
		if (wac == null) {
			// 未为此servlet定义上下文实例->创建一个本地实例
			wac = createWebApplicationContext(rootContext);
		}

		if (!this.refreshEventReceived) {
			// 上下文不是具有刷新支持的ConfigurableApplicationContext，或者在构造时注入的上下文已经刷新
			// 在此手动触发初始onRefresh
			synchronized (this.onRefreshMonitor) {
				onRefresh(wac);
			}
		}

		if (this.publishContext) {
			// 将上下文作为servlet上下文属性发布
			String attrName = getServletContextAttributeName();
			getServletContext().setAttribute(attrName, wac);
		}

		return wac;
	}

	/**
	 * 从具有{@link #setContextAttribute 配置名称}的{@code ServletContext}属性中检索{@code WebApplicationContext}。
	 * 在初始化（或调用）此servlet之前，{@code WebApplicationContext}必须已经加载并存储在{@code ServletContext}中。
	 * 子类可以覆盖此方法以提供不同的{@code WebApplicationContext}检索策略。
	 *
	 * @return 此servlet的WebApplicationContext，如果未找到则返回{@code null}
	 * @see #getContextAttribute()
	 */
	@Nullable
	protected WebApplicationContext findWebApplicationContext() {
		// 获取上下文属性名称
		String attrName = getContextAttribute();
		// 如果上下文属性名称为空，则返回 null
		if (attrName == null) {
			return null;
		}
		// 通过上下文属性名称从 Servlet 上下文中获取 Web 应用程序上下文
		WebApplicationContext wac =
				WebApplicationContextUtils.getWebApplicationContext(getServletContext(), attrName);
		// 如果未找到 Web 应用程序上下文，则抛出 IllegalStateException 异常
		if (wac == null) {
			throw new IllegalStateException("No WebApplicationContext found: initializer not registered?");
		}
		// 返回获取的 Web 应用程序上下文
		return wac;
	}

	/**
	 * 实例化此servlet的WebApplicationContext，可以是默认的{@link org.springframework.web.context.support.XmlWebApplicationContext}
	 * 或{@link #setContextClass 自定义上下文类}（如果已设置）。
	 * <p>此实现期望自定义上下文实现{@link org.springframework.web.context.ConfigurableWebApplicationContext}接口。
	 * 可以在子类中覆盖。
	 * <p>不要忘记将此servlet实例注册为创建上下文的应用程序侦听器（以触发其{@link #onRefresh 回调}），并在返回上下文实例之前调用
	 * {@link org.springframework.context.ConfigurableApplicationContext#refresh()}。
	 *
	 * @param parent 要使用的父ApplicationContext，如果没有，则为{@code null}
	 * @return 此servlet的WebApplicationContext
	 * @see org.springframework.web.context.support.XmlWebApplicationContext
	 */
	protected WebApplicationContext createWebApplicationContext(@Nullable ApplicationContext parent) {
		// 获取上下文类
		Class<?> contextClass = getContextClass();
		// 如果上下文类不是 ConfigurableWebApplicationContext 类的子类，则抛出 ApplicationContextException 异常
		if (!ConfigurableWebApplicationContext.class.isAssignableFrom(contextClass)) {
			throw new ApplicationContextException(
					"Fatal initialization error in servlet with name '" + getServletName() +
							"': custom WebApplicationContext class [" + contextClass.getName() +
							"] is not of type ConfigurableWebApplicationContext");
		}
		// 实例化上下文类对象
		ConfigurableWebApplicationContext wac =
				(ConfigurableWebApplicationContext) BeanUtils.instantiateClass(contextClass);

		// 设置环境
		wac.setEnvironment(getEnvironment());
		// 设置父级上下文
		wac.setParent(parent);
		// 获取上下文配置位置
		String configLocation = getContextConfigLocation();
		// 如果配置位置不为空，则设置配置位置
		if (configLocation != null) {
			wac.setConfigLocation(configLocation);
		}
		// 配置并刷新 Web应用程序上下文
		configureAndRefreshWebApplicationContext(wac);

		// 返回配置的 WebApplicationContext 对象
		return wac;
	}

	protected void configureAndRefreshWebApplicationContext(ConfigurableWebApplicationContext wac) {
		if (ObjectUtils.identityToString(wac).equals(wac.getId())) {
			// 如果应用程序上下文的 id 仍然是其原始默认值，则基于可用信息分配一个更有用的 id
			if (this.contextId != null) {
				// 如果设置了自定义的上下文 id，则将其设置为上下文的 id
				wac.setId(this.contextId);
			} else {
				// 生成默认的 id
				wac.setId(ConfigurableWebApplicationContext.APPLICATION_CONTEXT_ID_PREFIX +
						ObjectUtils.getDisplayString(getServletContext().getContextPath()) + '/' + getServletName());
			}
		}

		// 设置 Servlet 上下文和 Servlet 配置
		wac.setServletContext(getServletContext());
		wac.setServletConfig(getServletConfig());
		// 设置命名空间
		wac.setNamespace(getNamespace());
		// 添加应用程序监听器
		wac.addApplicationListener(new SourceFilteringListener(wac, new ContextRefreshListener()));

		// 上下文环境的 #initPropertySources 方法将在刷新上下文时被调用，
		// 在此处及早调用以确保 Servlet 属性源位于 #refresh 之前的任何后处理或初始化中
		ConfigurableEnvironment env = wac.getEnvironment();
		if (env instanceof ConfigurableWebEnvironment) {
			((ConfigurableWebEnvironment) env).initPropertySources(getServletContext(), getServletConfig());
		}

		// 后处理 WebApplicationContext
		postProcessWebApplicationContext(wac);
		// 应用初始化器
		applyInitializers(wac);
		// 刷新 WebApplicationContext
		wac.refresh();
	}

	/**
	 * 实例化此servlet的WebApplicationContext，可以是默认的
	 * {@link org.springframework.web.context.support.XmlWebApplicationContext}，
	 * 或{@link #setContextClass 自定义上下文类}（如果已设置）。
	 * 委托给# createWebApplicationContext(ApplicationContext)。
	 *
	 * @param parent 要使用的父WebApplicationContext，如果没有，则为{@code null}
	 * @return 此servlet的WebApplicationContext
	 * @see org.springframework.web.context.support.XmlWebApplicationContext
	 * @see #createWebApplicationContext(ApplicationContext)
	 */
	protected WebApplicationContext createWebApplicationContext(@Nullable WebApplicationContext parent) {
		return createWebApplicationContext((ApplicationContext) parent);
	}

	/**
	 * 在刷新并激活此servlet的上下文之前后处理给定的WebApplicationContext。
	 * <p>默认实现为空。 {@code refresh()}将在此方法返回后自动调用。
	 * <p>请注意，此方法旨在允许子类修改应用程序上下文，
	 * 而{@link #initWebApplicationContext}旨在允许最终用户通过使用
	 * {@link ApplicationContextInitializer ApplicationContextInitializers} 来修改上下文。
	 *
	 * @param wac 配置的WebApplicationContext（尚未刷新）
	 * @see #createWebApplicationContext
	 * @see #initWebApplicationContext
	 * @see ConfigurableWebApplicationContext#refresh()
	 */
	protected void postProcessWebApplicationContext(ConfigurableWebApplicationContext wac) {
	}

	/**
	 * 在刷新之前将WebApplicationContext委派给由“contextInitializerClasses”servlet init-param指定的任何{@link ApplicationContextInitializer}实例。
	 * <p>另请参见{@link #postProcessWebApplicationContext}，
	 * 该方法旨在允许子类（与最终用户相反）修改应用程序上下文，
	 * 并且在此方法之前立即调用。
	 *
	 * @param wac 配置的WebApplicationContext（尚未刷新）
	 * @see #createWebApplicationContext
	 * @see #postProcessWebApplicationContext
	 * @see ConfigurableApplicationContext#refresh()
	 */
	protected void applyInitializers(ConfigurableApplicationContext wac) {
		// 获取全局初始化器类名
		String globalClassNames = getServletContext().getInitParameter(ContextLoader.GLOBAL_INITIALIZER_CLASSES_PARAM);
		// 如果存在全局初始化器类名，则加载并添加到上下文初始化器列表中
		if (globalClassNames != null) {
			for (String className : StringUtils.tokenizeToStringArray(globalClassNames, INIT_PARAM_DELIMITERS)) {
				this.contextInitializers.add(loadInitializer(className, wac));
			}
		}

		// 如果存在上下文初始化器类名，则加载并添加到上下文初始化器列表中
		if (this.contextInitializerClasses != null) {
			for (String className : StringUtils.tokenizeToStringArray(this.contextInitializerClasses, INIT_PARAM_DELIMITERS)) {
				this.contextInitializers.add(loadInitializer(className, wac));
			}
		}

		// 对上下文初始化器列表进行排序
		AnnotationAwareOrderComparator.sort(this.contextInitializers);
		// 遍历上下文初始化器列表，并初始化 WebApplicationContext
		for (ApplicationContextInitializer<ConfigurableApplicationContext> initializer : this.contextInitializers) {
			initializer.initialize(wac);
		}
	}

	@SuppressWarnings("unchecked")
	private ApplicationContextInitializer<ConfigurableApplicationContext> loadInitializer(
			String className, ConfigurableApplicationContext wac) {
		try {
			// 加载初始化器类
			Class<?> initializerClass = ClassUtils.forName(className, wac.getClassLoader());
			// 解析初始化器类的泛型参数
			Class<?> initializerContextClass = GenericTypeResolver.resolveTypeArgument(initializerClass, ApplicationContextInitializer.class);
			// 检查初始化器类的泛型参数是否与应用程序上下文的类型兼容
			if (initializerContextClass != null && !initializerContextClass.isInstance(wac)) {
				throw new ApplicationContextException(String.format(
						"Could not apply context initializer [%s] since its generic parameter [%s] " +
								"is not assignable from the type of application context used by this " +
								"framework servlet: [%s]", initializerClass.getName(), initializerContextClass.getName(),
						wac.getClass().getName()));
			}
			// 实例化初始化器类
			return BeanUtils.instantiateClass(initializerClass, ApplicationContextInitializer.class);
		} catch (ClassNotFoundException ex) {
			// 处理类加载异常
			throw new ApplicationContextException(String.format("Could not load class [%s] specified " +
					"via 'contextInitializerClasses' init-param", className), ex);
		}
	}

	/**
	 * 返回此servlet的WebApplicationContext的ServletContext属性名称。
	 * <p>默认实现返回{@code SERVLET_CONTEXT_PREFIX + servlet名称}。
	 *
	 * @see #SERVLET_CONTEXT_PREFIX
	 * @see #getServletName
	 */
	public String getServletContextAttributeName() {
		return SERVLET_CONTEXT_PREFIX + getServletName();
	}

	/**
	 * 返回此servlet的WebApplicationContext。
	 */
	@Nullable
	public final WebApplicationContext getWebApplicationContext() {
		return this.webApplicationContext;
	}

	/**
	 * 在设置任何bean属性并加载WebApplicationContext之后调用此方法。
	 * 默认实现为空；子类可以覆盖此方法以执行所需的任何初始化。
	 *
	 * @throws ServletException 如果出现初始化异常
	 */
	protected void initFrameworkServlet() throws ServletException {
	}

	/**
	 * 刷新此servlet的应用程序上下文，以及servlet的依赖状态。
	 *
	 * @see #getWebApplicationContext()
	 * @see org.springframework.context.ConfigurableApplicationContext#refresh()
	 */
	public void refresh() {
		WebApplicationContext wac = getWebApplicationContext();
		if (!(wac instanceof ConfigurableApplicationContext)) {
			throw new IllegalStateException("WebApplicationContext does not support refresh: " + wac);
		}
		((ConfigurableApplicationContext) wac).refresh();
	}

	/**
	 * 接收来自此servlet的WebApplicationContext的刷新事件的回调。
	 * <p>默认实现调用{@link #onRefresh}，触发刷新此servlet的上下文相关状态。
	 *
	 * @param event 传入的ApplicationContext事件
	 */
	public void onApplicationEvent(ContextRefreshedEvent event) {
		this.refreshEventReceived = true;
		synchronized (this.onRefreshMonitor) {
			onRefresh(event.getApplicationContext());
		}
	}

	/**
	 * 模板方法，可重写以添加特定于servlet的刷新工作。
	 * 在成功刷新上下文后调用。
	 * <p>此实现为空。
	 *
	 * @param context 当前的WebApplicationContext
	 * @see #refresh()
	 */
	protected void onRefresh(ApplicationContext context) {
		// 对于子类：默认情况下不执行任何操作。
	}

	/**
	 * 关闭此servlet的WebApplicationContext。
	 *
	 * @see org.springframework.context.ConfigurableApplicationContext#close()
	 */
	@Override
	public void destroy() {
		getServletContext().log("Destroying Spring FrameworkServlet '" + getServletName() + "'");
		// 仅在本地管理时才调用WebApplicationContext的close()...
		if (this.webApplicationContext instanceof ConfigurableApplicationContext && !this.webApplicationContextInjected) {
			((ConfigurableApplicationContext) this.webApplicationContext).close();
		}
	}


	/**
	 * 重写父类实现以拦截PATCH请求。
	 */
	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		// 解析 HTTP 方法
		HttpMethod httpMethod = HttpMethod.resolve(request.getMethod());
		// 如果是 PATCH 方法或者无法解析方法，则调用自定义的处理方法
		if (httpMethod == HttpMethod.PATCH || httpMethod == null) {
			processRequest(request, response);
		} else {
			// 否则调用父类的 service 方法处理请求
			super.service(request, response);
		}
	}

	/**
	 * 将GET请求委托给processRequest/doService。
	 * <p>还将由HttpServlet的{@code doHead}默认实现调用，使用一个{@code NoBodyResponse}来捕获内容长度。
	 *
	 * @see #doService
	 * @see #doHead
	 */
	@Override
	protected final void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		processRequest(request, response);
	}

	/**
	 * 将POST请求委托给{@link #processRequest}。
	 *
	 * @see #doService
	 */
	@Override
	protected final void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		processRequest(request, response);
	}

	/**
	 * 将PUT请求委托给{@link #processRequest}。
	 *
	 * @see #doService
	 */
	@Override
	protected final void doPut(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		processRequest(request, response);
	}

	/**
	 * 将DELETE请求委托给{@link #processRequest}。
	 *
	 * @see #doService
	 */
	@Override
	protected final void doDelete(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		processRequest(request, response);
	}

	/**
	 * 将OPTIONS请求委托给{@link #processRequest}（如果需要）。
	 * <p>否则，应用HttpServlet的标准OPTIONS处理，并且在分派后仍然没有设置“Allow”标头时也适用。
	 *
	 * @see #doService
	 */
	@Override
	protected void doOptions(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		// 如果是 OPTIONS 请求或者是预检请求
		if (this.dispatchOptionsRequest || CorsUtils.isPreFlightRequest(request)) {
			// 处理请求
			processRequest(request, response);
			if (response.containsHeader("Allow")) {
				// 如果响应中包含 "Allow" 头部，则说明处理程序已经正确地响应了 OPTIONS 请求，直接返回即可
				return;
			}
		}

		// 使用响应包装器以始终将 PATCH 添加到允许的方法中
		super.doOptions(request, new HttpServletResponseWrapper(response) {
			@Override
			public void setHeader(String name, String value) {
				// 如果是设置 "Allow" 头部，则在现有值的基础上添加 PATCH 方法
				if ("Allow".equals(name)) {
					value = (StringUtils.hasLength(value) ? value + ", " : "") + HttpMethod.PATCH.name();
				}
				super.setHeader(name, value);
			}
		});
	}

	/**
	 * 将TRACE请求委托给{@link #processRequest}（如果需要）。
	 * <p>否则，应用HttpServlet的标准TRACE处理。
	 *
	 * @see #doService
	 */
	@Override
	protected void doTrace(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		if (this.dispatchTraceRequest) {
			processRequest(request, response);
			if ("message/http".equals(response.getContentType())) {
				// 来自处理程序的正确TRACE响应 - 我们完成了。
				return;
			}
		}
		super.doTrace(request, response);
	}

	/**
	 * 处理此请求，无论结果如何都发布一个事件。
	 * <p>实际的事件处理由抽象的{@link #doService}模板方法执行。
	 */
	protected final void processRequest(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		long startTime = System.currentTimeMillis();
		Throwable failureCause = null;

		// 保存之前的 LocaleContext
		LocaleContext previousLocaleContext = LocaleContextHolder.getLocaleContext();
		// 构建新的 LocaleContext
		LocaleContext localeContext = buildLocaleContext(request);

		// 保存之前的 RequestAttributes
		RequestAttributes previousAttributes = RequestContextHolder.getRequestAttributes();
		// 构建新的 ServletRequestAttributes
		ServletRequestAttributes requestAttributes = buildRequestAttributes(request, response, previousAttributes);

		// 获取或创建 WebAsyncManager，并注册 Callable 拦截器
		WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);
		asyncManager.registerCallableInterceptor(FrameworkServlet.class.getName(), new RequestBindingInterceptor());

		// 初始化上下文
		initContextHolders(request, localeContext, requestAttributes);

		try {
			// 处理请求
			doService(request, response);
		} catch (ServletException | IOException ex) {
			// 记录异常
			failureCause = ex;
			throw ex;
		} catch (Throwable ex) {
			// 记录异常
			failureCause = ex;
			throw new NestedServletException("Request processing failed", ex);
		} finally {
			// 重置上下文
			resetContextHolders(request, previousLocaleContext, previousAttributes);
			// 如果 requestAttributes 不为空，则通知请求处理完成
			if (requestAttributes != null) {
				requestAttributes.requestCompleted();
			}
			// 记录请求处理结果
			logResult(request, response, failureCause, asyncManager);
			// 发布请求处理完成事件
			publishRequestHandledEvent(request, response, startTime, failureCause);
		}
	}

	/**
	 * 为给定请求构建LocaleContext，将请求的主要区域设置为当前区域。
	 *
	 * @param request 当前HTTP请求
	 * @return 相应的LocaleContext，如果没有要绑定的则为{@code null}
	 * @see LocaleContextHolder#setLocaleContext
	 */
	@Nullable
	protected LocaleContext buildLocaleContext(HttpServletRequest request) {
		return new SimpleLocaleContext(request.getLocale());
	}

	/**
	 * 为给定请求构建ServletRequestAttributes（可能还包含对响应的引用），考虑预先绑定的属性（及其类型）。
	 *
	 * @param request            当前HTTP请求
	 * @param response           当前HTTP响应
	 * @param previousAttributes 先前绑定的RequestAttributes实例，如果有的话
	 * @return 要绑定的ServletRequestAttributes，如果要保留先前绑定的实例，则为{@code null}（如果之前未绑定任何实例，则不绑定任何实例）
	 * @see RequestContextHolder#setRequestAttributes
	 */
	@Nullable
	protected ServletRequestAttributes buildRequestAttributes(HttpServletRequest request,
															  @Nullable HttpServletResponse response, @Nullable RequestAttributes previousAttributes) {

		if (previousAttributes == null || previousAttributes instanceof ServletRequestAttributes) {
			// 如果之前的 RequestAttributes 为空或者是 ServletRequestAttributes 类型，则创建新的 ServletRequestAttributes 实例
			return new ServletRequestAttributes(request, response);
		} else {
			// 保留预先绑定的 RequestAttributes 实例
			return null;
		}
	}

	private void initContextHolders(HttpServletRequest request,
									@Nullable LocaleContext localeContext, @Nullable RequestAttributes requestAttributes) {

		if (localeContext != null) {
			// 如果 localeContext 不为空，则设置 LocaleContextHolder 的上下文
			LocaleContextHolder.setLocaleContext(localeContext, this.threadContextInheritable);
		}
		if (requestAttributes != null) {
			// 如果 requestAttributes 不为空，则设置 RequestContextHolder 的上下文
			RequestContextHolder.setRequestAttributes(requestAttributes, this.threadContextInheritable);
		}
	}

	private void resetContextHolders(HttpServletRequest request,
									 @Nullable LocaleContext prevLocaleContext, @Nullable RequestAttributes previousAttributes) {

		// 恢复先前的 LocaleContextHolder 和 RequestContextHolder 上下文
		LocaleContextHolder.setLocaleContext(prevLocaleContext, this.threadContextInheritable);
		RequestContextHolder.setRequestAttributes(previousAttributes, this.threadContextInheritable);
	}

	private void logResult(HttpServletRequest request, HttpServletResponse response,
						   @Nullable Throwable failureCause, WebAsyncManager asyncManager) {

		// 如果日志级别不是 DEBUG，则直接返回
		if (!logger.isDebugEnabled()) {
			return;
		}

		// 获取请求的 DispatcherType
		DispatcherType dispatchType = request.getDispatcherType();
		// 判断是否是初始的请求分发
		boolean initialDispatch = (dispatchType == DispatcherType.REQUEST);

		// 如果存在失败原因，则进行相应日志记录
		if (failureCause != null) {
			if (!initialDispatch) {
				// FORWARD/ERROR/ASYNC：仅记录最少的信息（应该已经有足够的上下文信息）
				if (logger.isDebugEnabled()) {
					logger.debug("Unresolved failure from \"" + dispatchType + "\" dispatch: " + failureCause);
				}
			} else if (logger.isTraceEnabled()) {
				logger.trace("Failed to complete request", failureCause);
			} else {
				logger.debug("Failed to complete request: " + failureCause);
			}
			return;
		}

		// 如果异步处理已经开始，则记录相应日志并返回
		if (asyncManager.isConcurrentHandlingStarted()) {
			logger.debug("Exiting but response remains open for further handling");
			return;
		}

		// 获取响应状态码和头部信息
		int status = response.getStatus();
		String headers = "";

		if (logger.isTraceEnabled()) {
			// 获取响应头部的名称集合
			Collection<String> names = response.getHeaderNames();
			// 如果允许记录请求细节，则将头部信息拼接成字符串
			if (this.enableLoggingRequestDetails) {
				headers = names.stream().map(name -> name + ":" + response.getHeaders(name))
						.collect(Collectors.joining(", "));
			} else {
				// 如果不允许记录请求细节，且头部信息集合为空，则置空字符串，否则标记为“masked”
				headers = names.isEmpty() ? "" : "masked";
			}
			// 组装头部信息字符串
			headers = ", headers={" + headers + "}";
		}

		// 根据不同的分发类型进行相应的日志记录
		if (!initialDispatch) {
			logger.debug("Exiting from \"" + dispatchType + "\" dispatch, status " + status + headers);
		} else {
			// 解析HTTP状态码
			HttpStatus httpStatus = HttpStatus.resolve(status);
			logger.debug("Completed " + (httpStatus != null ? httpStatus : status) + headers);
		}
	}

	private void publishRequestHandledEvent(HttpServletRequest request, HttpServletResponse response,
											long startTime, @Nullable Throwable failureCause) {

		if (this.publishEvents && this.webApplicationContext != null) {
			// 无论成功与否，都发布一个事件。
			long processingTime = System.currentTimeMillis() - startTime;
			this.webApplicationContext.publishEvent(
					new ServletRequestHandledEvent(this,
							request.getRequestURI(), request.getRemoteAddr(),
							request.getMethod(), getServletConfig().getServletName(),
							WebUtils.getSessionId(request), getUsernameForRequest(request),
							processingTime, failureCause, response.getStatus()));
		}
	}

	/**
	 * 确定给定请求的用户名。
	 * <p>默认实现获取UserPrincipal的名称（如果有）。
	 * 可以在子类中重写。
	 *
	 * @param request 当前HTTP请求
	 * @return 用户名，如果未找到则为{@code null}
	 * @see javax.servlet.http.HttpServletRequest#getUserPrincipal()
	 */
	@Nullable
	protected String getUsernameForRequest(HttpServletRequest request) {
		Principal userPrincipal = request.getUserPrincipal();
		return (userPrincipal != null ? userPrincipal.getName() : null);
	}


	/**
	 * 子类必须实现此方法来处理请求，
	 * 接收GET、POST、PUT和DELETE的集中回调。
	 * <p>该契约基本上与HttpServlet的通常被重写的{@code doGet}或{@code doPost}方法的契约相同。
	 * <p>此类拦截调用以确保进行异常处理和事件发布。
	 *
	 * @param request  当前HTTP请求
	 * @param response 当前HTTP响应
	 * @throws Exception 处理失败时抛出任何类型的异常
	 * @see javax.servlet.http.HttpServlet#doGet
	 * @see javax.servlet.http.HttpServlet#doPost
	 */
	protected abstract void doService(HttpServletRequest request, HttpServletResponse response)
			throws Exception;


	/**
	 * 仅接收来自此servlet的WebApplicationContext的事件的ApplicationListener端点，
	 * 将事件委托给FrameworkServlet实例的{@code onApplicationEvent}方法。
	 */
	private class ContextRefreshListener implements ApplicationListener<ContextRefreshedEvent> {

		@Override
		public void onApplicationEvent(ContextRefreshedEvent event) {
			FrameworkServlet.this.onApplicationEvent(event);
		}
	}


	/**
	 * CallableProcessingInterceptor实现，用于初始化和重置FrameworkServlet的上下文持有者，
	 * 即LocaleContextHolder和RequestContextHolder。
	 */
	private class RequestBindingInterceptor implements CallableProcessingInterceptor {

		@Override
		public <T> void preProcess(NativeWebRequest webRequest, Callable<T> task) {
			// 从 WebRequest 中获取 HttpServletRequest 对象。
			HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
			// 如果获取到了 HttpServletRequest 对象，则执行以下操作。
			if (request != null) {
				// 从 WebRequest 中获取 HttpServletResponse 对象。
				HttpServletResponse response = webRequest.getNativeResponse(HttpServletResponse.class);
				// 初始化上下文持有者。
				initContextHolders(request, buildLocaleContext(request),
						buildRequestAttributes(request, response, null));
			}
		}

		@Override
		public <T> void postProcess(NativeWebRequest webRequest, Callable<T> task, Object concurrentResult) {
			// 从 WebRequest 中获取 HttpServletRequest 对象。
			HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
			// 如果获取到了 HttpServletRequest 对象，则执行以下操作。
			if (request != null) {
				// 重置上下文持有者。
				resetContextHolders(request, null, null);
			}
		}
	}

}
