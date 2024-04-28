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

package org.springframework.web.servlet.mvc.method.annotation;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.*;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.log.LogFormatUtils;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.ui.ModelMap;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils.MethodFilter;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.support.*;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.async.*;
import org.springframework.web.method.ControllerAdviceBean;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.annotation.*;
import org.springframework.web.method.support.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.mvc.annotation.ModelAndViewResolver;
import org.springframework.web.servlet.mvc.method.AbstractHandlerMethodAdapter;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.springframework.web.util.WebUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link AbstractHandlerMethodAdapter}的扩展，支持带有{@link RequestMapping @RequestMapping}注解的{@link HandlerMethod HandlerMethods}。
 *
 * <p>通过{@link #setCustomArgumentResolvers}和{@link #setCustomReturnValueHandlers}添加对自定义参数和返回值类型的支持，
 * 或者，为了重新配置所有参数和返回值类型，使用{@link #setArgumentResolvers}和{@link #setReturnValueHandlers}。
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @see HandlerMethodArgumentResolver
 * @see HandlerMethodReturnValueHandler
 * @since 3.1
 */
public class RequestMappingHandlerAdapter extends AbstractHandlerMethodAdapter
		implements BeanFactoryAware, InitializingBean {

	/**
	 * 由{@code spring.xml.ignore}系统属性控制的布尔标志，指示Spring忽略XML，即不初始化与XML相关的基础设施。
	 * <p>默认值为"false"。
	 */
	private static final boolean shouldIgnoreXml = SpringProperties.getFlag("spring.xml.ignore");

	/**
	 * 匹配{@link InitBinder @InitBinder}方法的MethodFilter。
	 */
	public static final MethodFilter INIT_BINDER_METHODS = method ->
			AnnotatedElementUtils.hasAnnotation(method, InitBinder.class);

	/**
	 * 匹配{@link ModelAttribute @ModelAttribute}方法的MethodFilter。
	 */
	public static final MethodFilter MODEL_ATTRIBUTE_METHODS = method ->
			(!AnnotatedElementUtils.hasAnnotation(method, RequestMapping.class) &&
					AnnotatedElementUtils.hasAnnotation(method, ModelAttribute.class));

	/**
	 * 自定义参数解析器列表
	 */
	@Nullable
	private List<HandlerMethodArgumentResolver> customArgumentResolvers;

	/**
	 * 参数解析器
	 */
	@Nullable
	private HandlerMethodArgumentResolverComposite argumentResolvers;

	/**
	 * @InitBinder 方法的参数解析器
	 */
	@Nullable
	private HandlerMethodArgumentResolverComposite initBinderArgumentResolvers;

	/**
	 * 自定义的返回值处理器
	 */
	@Nullable
	private List<HandlerMethodReturnValueHandler> customReturnValueHandlers;

	/**
	 * 返回值处理器
	 */
	@Nullable
	private HandlerMethodReturnValueHandlerComposite returnValueHandlers;

	/**
	 * 模板和视图解析器列表
	 */
	@Nullable
	private List<ModelAndViewResolver> modelAndViewResolvers;

	/**
	 * 内容协商管理器
	 */
	private ContentNegotiationManager contentNegotiationManager = new ContentNegotiationManager();

	/**
	 * Http消息转换器列表
	 */
	private List<HttpMessageConverter<?>> messageConverters;

	/**
	 * 请求响应体建言
	 */
	private final List<Object> requestResponseBodyAdvice = new ArrayList<>();

	/**
	 * Web绑定初始值设定
	 */
	@Nullable
	private WebBindingInitializer webBindingInitializer;

	/**
	 * 异步线程池
	 */
	private AsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor("MvcAsync");

	/**
	 * 异步请求超时时间
	 */
	@Nullable
	private Long asyncRequestTimeout;

	/**
	 * Callable处理拦截器
	 */
	private CallableProcessingInterceptor[] callableInterceptors = new CallableProcessingInterceptor[0];

	/**
	 * 延迟结果处理拦截器列表
	 */
	private DeferredResultProcessingInterceptor[] deferredResultInterceptors = new DeferredResultProcessingInterceptor[0];

	/**
	 * 响应式适配注册器
	 */
	private ReactiveAdapterRegistry reactiveAdapterRegistry = ReactiveAdapterRegistry.getSharedInstance();

	/**
	 * 重定向时，是否忽略默认模型
	 */
	private boolean ignoreDefaultModelOnRedirect = false;

	/**
	 * 用于@SessionAttributes注解的处理器的缓存时间（以秒为单位）
	 */
	private int cacheSecondsForSessionAttributeHandlers = 0;

	/**
	 * 是否在会话上同步执行请求
	 */
	private boolean synchronizeOnSession = false;

	/**
	 * 会话属性存储策略
	 */
	@Nullable
	private SessionAttributeStore sessionAttributeStore = new DefaultSessionAttributeStore();

	/**
	 * 参数名称发现器
	 */
	private ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

	/**
	 * 可配置的bean工厂
	 */
	@Nullable
	private ConfigurableBeanFactory beanFactory;

	/**
	 * session属性类型与Session属性处理器映射
	 */
	private final Map<Class<?>, SessionAttributesHandler> sessionAttributesHandlerCache = new ConcurrentHashMap<>(64);

	/**
	 * 处理器类型与标记有@InitBinder方法映射
	 */
	private final Map<Class<?>, Set<Method>> initBinderCache = new ConcurrentHashMap<>(64);

	/**
	 * 控制器建言Bean与标记有@InitBinder方法映射
	 */
	private final Map<ControllerAdviceBean, Set<Method>> initBinderAdviceCache = new LinkedHashMap<>();

	/**
	 * 处理类型与标记有 @ModelAttribute 方法的映射
	 */
	private final Map<Class<?>, Set<Method>> modelAttributeCache = new ConcurrentHashMap<>(64);

	/**
	 * 控制器建言Bean与标记有 @ModelAttribute 方法的映射
	 */
	private final Map<ControllerAdviceBean, Set<Method>> modelAttributeAdviceCache = new LinkedHashMap<>();


	public RequestMappingHandlerAdapter() {
		// 初始化消息转换器列表，预设容量为 4
		this.messageConverters = new ArrayList<>(4);

		// 添加字节数组 HTTP 消息转换器
		this.messageConverters.add(new ByteArrayHttpMessageConverter());

		// 添加字符串 HTTP 消息转换器
		this.messageConverters.add(new StringHttpMessageConverter());

		// 如果不应忽略 XML，则尝试添加源 HTTP 消息转换器
		if (!shouldIgnoreXml) {
			try {
				this.messageConverters.add(new SourceHttpMessageConverter<>());
			} catch (Error err) {
				// 当没有可用的 TransformerFactory 实现时忽略异常
			}
		}

		// 添加全面的表单 HTTP 消息转换器
		this.messageConverters.add(new AllEncompassingFormHttpMessageConverter());
	}


	/**
	 * 设置自定义参数解析器。自定义解析器在内置解析器之后进行排序。
	 * 若要覆盖内置的参数解析支持，请使用{@link #setArgumentResolvers}方法。
	 */
	public void setCustomArgumentResolvers(@Nullable List<HandlerMethodArgumentResolver> argumentResolvers) {
		this.customArgumentResolvers = argumentResolvers;
	}

	/**
	 * 返回自定义参数解析器列表，如果没有则返回null。
	 */
	@Nullable
	public List<HandlerMethodArgumentResolver> getCustomArgumentResolvers() {
		return this.customArgumentResolvers;
	}

	/**
	 * 配置完整的参数类型列表，从而覆盖默认配置的解析器。
	 */
	public void setArgumentResolvers(@Nullable List<HandlerMethodArgumentResolver> argumentResolvers) {
		if (argumentResolvers == null) {
			// 如果参数解析器不存在，则设置参数解析器为 null
			this.argumentResolvers = null;
		} else {
			// 否则，创建 HandlerMethodArgumentResolverComposite 实例，并添加参数解析器
			this.argumentResolvers = new HandlerMethodArgumentResolverComposite();
			this.argumentResolvers.addResolvers(argumentResolvers);
		}
	}

	/**
	 * 返回配置的参数解析器，如果尚未通过 {@link #afterPropertiesSet()} 初始化，则可能为 null。
	 */
	@Nullable
	public List<HandlerMethodArgumentResolver> getArgumentResolvers() {
		return (this.argumentResolvers != null ? this.argumentResolvers.getResolvers() : null);
	}

	/**
	 * 配置 {@code @InitBinder} 方法中支持的参数类型。
	 */
	public void setInitBinderArgumentResolvers(@Nullable List<HandlerMethodArgumentResolver> argumentResolvers) {
		if (argumentResolvers == null) {
			// 如果参数解析器不存在，则设置初始化绑定器参数解析器为 null
			this.initBinderArgumentResolvers = null;
		} else {
			// 否则，创建 HandlerMethodArgumentResolverComposite 实例，并添加参数解析器
			this.initBinderArgumentResolvers = new HandlerMethodArgumentResolverComposite();
			this.initBinderArgumentResolvers.addResolvers(argumentResolvers);
		}
	}

	/**
	 * 返回 {@code @InitBinder} 方法的参数解析器，如果尚未通过 {@link #afterPropertiesSet()} 初始化，则可能为 null。
	 */
	@Nullable
	public List<HandlerMethodArgumentResolver> getInitBinderArgumentResolvers() {
		return (this.initBinderArgumentResolvers != null ? this.initBinderArgumentResolvers.getResolvers() : null);
	}

	/**
	 * 提供自定义返回值类型的处理器。自定义处理器在内置处理器之后排序。
	 * 要覆盖内置的返回值处理支持，请使用 {@link #setReturnValueHandlers}。
	 */
	public void setCustomReturnValueHandlers(@Nullable List<HandlerMethodReturnValueHandler> returnValueHandlers) {
		this.customReturnValueHandlers = returnValueHandlers;
	}

	/**
	 * 返回自定义的返回值处理器，如果没有则返回null。
	 */
	@Nullable
	public List<HandlerMethodReturnValueHandler> getCustomReturnValueHandlers() {
		return this.customReturnValueHandlers;
	}

	/**
	 * 配置支持的返回值类型的完整列表，从而覆盖默认情况下配置的处理程序。
	 */
	public void setReturnValueHandlers(@Nullable List<HandlerMethodReturnValueHandler> returnValueHandlers) {
		if (returnValueHandlers == null) {
			// 如果返回值处理器不存在，则设置返回值处理器为 null
			this.returnValueHandlers = null;
		} else {
			// 否则，创建 HandlerMethodReturnValueHandlerComposite 实例，并添加返回值处理器
			this.returnValueHandlers = new HandlerMethodReturnValueHandlerComposite();
			this.returnValueHandlers.addHandlers(returnValueHandlers);
		}
	}

	/**
	 * 返回配置的处理程序，如果尚未通过{@link #afterPropertiesSet()}初始化，则可能返回null。
	 */
	@Nullable
	public List<HandlerMethodReturnValueHandler> getReturnValueHandlers() {
		return (this.returnValueHandlers != null ? this.returnValueHandlers.getHandlers() : null);
	}

	/**
	 * 设置自定义的ModelAndViewResolver列表。
	 * <p><strong>注意：</strong>此方法仅用于向后兼容。但是，建议重新编写一个
	 * {@code ModelAndViewResolver}作为{@link HandlerMethodReturnValueHandler}。
	 * 由于{@link HandlerMethodReturnValueHandler#supportsReturnType}方法无法实现，
	 * 因此无法在两者之间建立适配器。因此，{@code ModelAndViewResolver}s始终在所有其他返回值处理器之后调用。
	 * <p>{@code HandlerMethodReturnValueHandler}提供了更好的访问返回类型和控制器方法信息的方式，
	 * 并且可以自由地相对于其他返回值处理器进行排序。
	 */
	public void setModelAndViewResolvers(@Nullable List<ModelAndViewResolver> modelAndViewResolvers) {
		this.modelAndViewResolvers = modelAndViewResolvers;
	}

	/**
	 * 返回配置的ModelAndViewResolver列表，如果没有配置则返回null。
	 */
	@Nullable
	public List<ModelAndViewResolver> getModelAndViewResolvers() {
		return this.modelAndViewResolvers;
	}

	/**
	 * 设置用于确定请求的媒体类型的ContentNegotiationManager。
	 * 如果没有设置，将使用默认构造函数。
	 */
	public void setContentNegotiationManager(ContentNegotiationManager contentNegotiationManager) {
		this.contentNegotiationManager = contentNegotiationManager;
	}

	/**
	 * 为支持读取和/或写入请求和响应主体的参数解析器和返回值处理器提供转换器。
	 */
	public void setMessageConverters(List<HttpMessageConverter<?>> messageConverters) {
		this.messageConverters = messageConverters;
	}

	/**
	 * 返回配置的消息体转换器。
	 */
	public List<HttpMessageConverter<?>> getMessageConverters() {
		return this.messageConverters;
	}

	/**
	 * 添加一个或多个{@code RequestBodyAdvice}实例以拦截在{@code @RequestBody}和
	 * {@code HttpEntity}方法参数之前读取和转换的请求。
	 */
	public void setRequestBodyAdvice(@Nullable List<RequestBodyAdvice> requestBodyAdvice) {
		if (requestBodyAdvice != null) {
			this.requestResponseBodyAdvice.addAll(requestBodyAdvice);
		}
	}

	/**
	 * 添加一个或多个{@code ResponseBodyAdvice}实例以拦截在{@code @ResponseBody}或
	 * {@code ResponseEntity}返回值写入响应体之前。
	 */
	public void setResponseBodyAdvice(@Nullable List<ResponseBodyAdvice<?>> responseBodyAdvice) {
		if (responseBodyAdvice != null) {
			this.requestResponseBodyAdvice.addAll(responseBodyAdvice);
		}
	}

	/**
	 * 提供一个带有"全局"初始化的WebBindingInitializer以应用于每个DataBinder实例。
	 */
	public void setWebBindingInitializer(@Nullable WebBindingInitializer webBindingInitializer) {
		this.webBindingInitializer = webBindingInitializer;
	}

	/**
	 * 返回配置的WebBindingInitializer，如果没有配置则返回null。
	 */
	@Nullable
	public WebBindingInitializer getWebBindingInitializer() {
		return this.webBindingInitializer;
	}

	/**
	 * 设置当控制器方法返回一个{@code Callable}时使用的默认{@code AsyncTaskExecutor}。
	 * 控制器方法可以通过返回一个{@code WebAsyncTask}来覆盖此默认设置。
	 * <p>默认情况下，使用{@link SimpleAsyncTaskExecutor}实例。
	 * 建议在生产环境中更改默认设置，因为简单的执行器不会重用线程。
	 */
	public void setTaskExecutor(AsyncTaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	/**
	 * 指定并发处理应在多少毫秒后超时。在Servlet 3中，超时从主请求处理线程退出开始，
	 * 并在请求再次分派以进一步处理并发生成的结果时结束。
	 * <p>如果未设置此值，则使用底层实现的默认超时。
	 *
	 * @param timeout 超时值（以毫秒为单位）
	 */
	public void setAsyncRequestTimeout(long timeout) {
		this.asyncRequestTimeout = timeout;
	}

	/**
	 * 配置 {@code CallableProcessingInterceptor} 以在异步请求上注册。
	 *
	 * @param interceptors 要注册的拦截器
	 */
	public void setCallableInterceptors(List<CallableProcessingInterceptor> interceptors) {
		this.callableInterceptors = interceptors.toArray(new CallableProcessingInterceptor[0]);
	}

	/**
	 * 配置 {@code DeferredResultProcessingInterceptor} 以在异步请求上注册。
	 *
	 * @param interceptors 要注册的拦截器
	 */
	public void setDeferredResultInterceptors(List<DeferredResultProcessingInterceptor> interceptors) {
		this.deferredResultInterceptors = interceptors.toArray(new DeferredResultProcessingInterceptor[0]);
	}

	/**
	 * 配置支持作为控制器方法返回值的反应库类型的注册表。
	 *
	 * @since 5.0.5
	 */
	public void setReactiveAdapterRegistry(ReactiveAdapterRegistry reactiveAdapterRegistry) {
		this.reactiveAdapterRegistry = reactiveAdapterRegistry;
	}

	/**
	 * 返回配置的反应类型适配器注册表。
	 *
	 * @since 5.0
	 */
	public ReactiveAdapterRegistry getReactiveAdapterRegistry() {
		return this.reactiveAdapterRegistry;
	}

	/**
	 * 默认情况下，渲染和重定向场景都使用"default"模型的内容。或者，控制器方法可以声明一个{@link RedirectAttributes}参数并使用它来为重定向提供属性。
	 * <p>将此标志设置为{@code true}保证在重定向场景中永远不会使用"default"模型，即使没有声明RedirectAttributes参数。将其设置为{@code false}意味着如果控制器方法没有声明RedirectAttributes参数，则可以在重定向中使用"default"模型。
	 * <p>默认设置是{@code false}，但新应用程序应该考虑将其设置为{@code true}。
	 *
	 * @see RedirectAttributes
	 */
	public void setIgnoreDefaultModelOnRedirect(boolean ignoreDefaultModelOnRedirect) {
		this.ignoreDefaultModelOnRedirect = ignoreDefaultModelOnRedirect;
	}

	/**
	 * 指定存储会话属性的策略。默认是{@link org.springframework.web.bind.support.DefaultSessionAttributeStore}，
	 * 将会话属性存储在HttpSession中，属性名与模型中的相同。
	 */
	public void setSessionAttributeStore(SessionAttributeStore sessionAttributeStore) {
		this.sessionAttributeStore = sessionAttributeStore;
	}

	/**
	 * 设置用于@SessionAttributes注解的处理器的缓存时间（以秒为单位）。
	 * <p>可能的值有：
	 * <ul>
	 * <li>-1：不生成与缓存相关的头部信息</li>
	 * <li>0（默认值）："Cache-Control: no-store"将阻止缓存</li>
	 * <li>1或更高："Cache-Control: max-age=seconds"将要求缓存内容；
	 * 当处理会话属性时，不建议使用此设置</li>
	 * </ul>
	 * <p>与"cacheSeconds"属性不同，后者将应用于所有通用处理器（但不适用于@SessionAttributes注解的处理器），
	 * 此设置仅适用于@SessionAttributes处理器。
	 *
	 * @see #setCacheSeconds
	 * @see org.springframework.web.bind.annotation.SessionAttributes
	 */
	public void setCacheSecondsForSessionAttributeHandlers(int cacheSecondsForSessionAttributeHandlers) {
		this.cacheSecondsForSessionAttributeHandlers = cacheSecondsForSessionAttributeHandlers;
	}

	/**
	 * 设置是否应在会话上同步控制器执行，以序列化来自同一客户端的并行调用。
	 * <p>更具体地说，如果此标志为"true"，则{@code handleRequestInternal}方法的执行将同步。
	 * 最佳可用的会话互斥锁将用于同步；理想情况下，这将是HttpSessionMutexListener暴露的互斥锁。
	 * <p>在会话的整个生命周期中，会话互斥锁保证是相同的对象，可以在{@code SESSION_MUTEX_ATTRIBUTE}常量定义的键下找到。
	 * 它作为对当前会话的安全引用进行锁定的安全参考。
	 * <p>在许多情况下，HttpSession引用本身也是一个安全的互斥锁，因为它总是相同的对象引用，
	 * 对于相同的活动逻辑会话。然而，这在不同的servlet容器之间不能保证；唯一100%安全的方法是一个会话互斥锁。
	 *
	 * @see org.springframework.web.util.HttpSessionMutexListener
	 * @see org.springframework.web.util.WebUtils#getSessionMutex(javax.servlet.http.HttpSession)
	 */
	public void setSynchronizeOnSession(boolean synchronizeOnSession) {
		this.synchronizeOnSession = synchronizeOnSession;
	}

	/**
	 * 设置用于解析方法参数名称（如果需要，例如默认属性名称）的ParameterNameDiscoverer。
	 * <p>默认值为{@link org.springframework.core.DefaultParameterNameDiscoverer}。
	 */
	public void setParameterNameDiscoverer(ParameterNameDiscoverer parameterNameDiscoverer) {
		this.parameterNameDiscoverer = parameterNameDiscoverer;
	}

	/**
	 * 需要一个ConfigurableBeanFactory来解析方法参数默认值中的表达式。
	 */
	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		if (beanFactory instanceof ConfigurableBeanFactory) {
			this.beanFactory = (ConfigurableBeanFactory) beanFactory;
		}
	}

	/**
	 * 返回此bean实例的拥有者工厂，如果没有则为null。
	 */
	@Nullable
	protected ConfigurableBeanFactory getBeanFactory() {
		return this.beanFactory;
	}


	@Override
	public void afterPropertiesSet() {
		// 首先初始化控制器通知缓存，可能会添加 ResponseBody 通知 Bean
		initControllerAdviceCache();

		if (this.argumentResolvers == null) {
			// 如果参数解析不存在，获取默认的参数解析器
			List<HandlerMethodArgumentResolver> resolvers = getDefaultArgumentResolvers();
			// 创建 HandlerMethodArgumentResolverComposite 实例，并添加参数解析器列表
			this.argumentResolvers = new HandlerMethodArgumentResolverComposite().addResolvers(resolvers);
		}
		if (this.initBinderArgumentResolvers == null) {
			// 如果初始化绑定器参数解析器不存在，获取默认的初始化绑定器参数解析器
			List<HandlerMethodArgumentResolver> resolvers = getDefaultInitBinderArgumentResolvers();
			// 创建 HandlerMethodArgumentResolverComposite 实例，并添加初始化绑定器参数解析器
			this.initBinderArgumentResolvers = new HandlerMethodArgumentResolverComposite().addResolvers(resolvers);
		}
		if (this.returnValueHandlers == null) {
			// 如果返回值处理器不存在，获取默认的返回值处理器
			List<HandlerMethodReturnValueHandler> handlers = getDefaultReturnValueHandlers();
			// 创建 HandlerMethodReturnValueHandlerComposite 实例并添加返回值处理器
			this.returnValueHandlers = new HandlerMethodReturnValueHandlerComposite().addHandlers(handlers);
		}
	}

	private void initControllerAdviceCache() {
		// 检查应用程序上下文是否为空，如果为空则直接返回
		if (getApplicationContext() == null) {
			return;
		}

		// 查找带有@ControllerAdvice注解的Bean
		List<ControllerAdviceBean> adviceBeans = ControllerAdviceBean.findAnnotatedBeans(getApplicationContext());

		// 创建一个用于存储RequestBodyAdvice和ResponseBodyAdvice的列表
		List<Object> requestResponseBodyAdviceBeans = new ArrayList<>();

		// 遍历所有的ControllerAdviceBean
		for (ControllerAdviceBean adviceBean : adviceBeans) {
			// 获取Bean的类型
			Class<?> beanType = adviceBean.getBeanType();
			// 如果类型无法解析，抛出异常
			if (beanType == null) {
				throw new IllegalStateException("Unresolvable type for ControllerAdviceBean: " + adviceBean);
			}
			// 选择带有@ModelAttribute注解的方法
			Set<Method> attrMethods = MethodIntrospector.selectMethods(beanType, MODEL_ATTRIBUTE_METHODS);
			// 如果存在这样的方法，将它们添加到modelAttributeAdviceCache中
			if (!attrMethods.isEmpty()) {
				this.modelAttributeAdviceCache.put(adviceBean, attrMethods);
			}
			// 选择带有@InitBinder注解的方法
			Set<Method> binderMethods = MethodIntrospector.selectMethods(beanType, INIT_BINDER_METHODS);
			// 如果存在这样的方法，将它们添加到initBinderAdviceCache中
			if (!binderMethods.isEmpty()) {
				this.initBinderAdviceCache.put(adviceBean, binderMethods);
			}
			if (RequestBodyAdvice.class.isAssignableFrom(beanType) || ResponseBodyAdvice.class.isAssignableFrom(beanType)) {
				// 如果Bean是RequestBodyAdvice或ResponseBodyAdvice的子类，将其添加到requestResponseBodyAdviceBeans列表中
				requestResponseBodyAdviceBeans.add(adviceBean);
			}
		}

		if (!requestResponseBodyAdviceBeans.isEmpty()) {
			// 如果有RequestBodyAdvice或ResponseBodyAdvice的Bean，将它们添加到requestResponseBodyAdvice列表的开头
			this.requestResponseBodyAdvice.addAll(0, requestResponseBodyAdviceBeans);
		}

		// 如果日志级别为DEBUG，输出ControllerAdvice beans的信息
		if (logger.isDebugEnabled()) {
			int modelSize = this.modelAttributeAdviceCache.size();
			int binderSize = this.initBinderAdviceCache.size();
			int reqCount = getBodyAdviceCount(RequestBodyAdvice.class);
			int resCount = getBodyAdviceCount(ResponseBodyAdvice.class);
			// 如果没有找到任何ControllerAdvice beans，输出"none"
			if (modelSize == 0 && binderSize == 0 && reqCount == 0 && resCount == 0) {
				logger.debug("ControllerAdvice beans: none");
			} else {
				// 输出找到的ControllerAdvice beans的数量和类型
				logger.debug("ControllerAdvice beans: " + modelSize + " @ModelAttribute, " + binderSize +
						" @InitBinder, " + reqCount + " RequestBodyAdvice, " + resCount + " ResponseBodyAdvice");
			}
		}
	}

	// 计算所有建议，包括明确的注册。

	private int getBodyAdviceCount(Class<?> adviceType) {
		// 获取requestResponseBodyAdvice列表
		List<Object> advice = this.requestResponseBodyAdvice;

		// 判断adviceType是否为RequestBodyAdvice类型
		return RequestBodyAdvice.class.isAssignableFrom(adviceType) ?
				// 如果是，则获取RequestBodyAdvice类型的advice数量
				RequestResponseBodyAdviceChain.getAdviceByType(advice, RequestBodyAdvice.class).size() :
				// 如果不是，则获取ResponseBodyAdvice类型的advice数量
				RequestResponseBodyAdviceChain.getAdviceByType(advice, ResponseBodyAdvice.class).size();
	}

	/**
	 * 返回要使用的参数解析器列表，包括内置解析器和通过{@link #setCustomArgumentResolvers}提供的自定义解析器。
	 */
	private List<HandlerMethodArgumentResolver> getDefaultArgumentResolvers() {
		List<HandlerMethodArgumentResolver> resolvers = new ArrayList<>(30);

		// 基于注解的参数解析
		// 添加请求参数方法解析器
		resolvers.add(new RequestParamMethodArgumentResolver(getBeanFactory(), false));
		// 添加请求参数Map方法解析器
		resolvers.add(new RequestParamMapMethodArgumentResolver());
		// 添加路径变量方法解析器
		resolvers.add(new PathVariableMethodArgumentResolver());
		// 添加路径变量Map方法解析器
		resolvers.add(new PathVariableMapMethodArgumentResolver());
		// 添加矩阵变量方法解析器
		resolvers.add(new MatrixVariableMethodArgumentResolver());
		// 添加矩阵变量Map方法解析器
		resolvers.add(new MatrixVariableMapMethodArgumentResolver());
		// 添加Servlet模型属性方法处理器
		resolvers.add(new ServletModelAttributeMethodProcessor(false));
		// 添加请求响应体方法处理器
		resolvers.add(new RequestResponseBodyMethodProcessor(getMessageConverters(), this.requestResponseBodyAdvice));
		// 添加请求部分Part方法参数解析器
		resolvers.add(new RequestPartMethodArgumentResolver(getMessageConverters(), this.requestResponseBodyAdvice));
		// 添加请求头方法参数解析器
		resolvers.add(new RequestHeaderMethodArgumentResolver(getBeanFactory()));
		// 添加请求头Map方法参数解析器
		resolvers.add(new RequestHeaderMapMethodArgumentResolver());
		// 添加 Servlet Cookie 方法参数解析器
		resolvers.add(new ServletCookieValueMethodArgumentResolver(getBeanFactory()));
		// 添加表达式值方法参数解析器
		resolvers.add(new ExpressionValueMethodArgumentResolver(getBeanFactory()));
		// 添加会话属性方法参数解析器
		resolvers.add(new SessionAttributeMethodArgumentResolver());
		// 添加请求属性方法参数解析器
		resolvers.add(new RequestAttributeMethodArgumentResolver());

		// 基于类型的参数解析
		// 添加Servlet请求方法参数解析器
		resolvers.add(new ServletRequestMethodArgumentResolver());
		// 添加Servlet响应方法参数解析器
		resolvers.add(new ServletResponseMethodArgumentResolver());
		// 添加HttpEntity方法参数处理器
		resolvers.add(new HttpEntityMethodProcessor(getMessageConverters(), this.requestResponseBodyAdvice));
		// 添加 重定向属性 方法参数解析器
		resolvers.add(new RedirectAttributesMethodArgumentResolver());
		// 添加模型方法处理器
		resolvers.add(new ModelMethodProcessor());
		// 添加Map方法处理器
		resolvers.add(new MapMethodProcessor());
		// 添加错误方法参数解析器
		resolvers.add(new ErrorsMethodArgumentResolver());
		// 添加会话状态值方法参数解析器
		resolvers.add(new SessionStatusMethodArgumentResolver());
		// 添加UriComponentsBuilder方法参数解析器
		resolvers.add(new UriComponentsBuilderMethodArgumentResolver());
		if (KotlinDetector.isKotlinPresent()) {
			// 添加Kotlin Coroutine处理者方法参数解析器
			resolvers.add(new ContinuationHandlerMethodArgumentResolver());
		}

		// 自定义参数解析
		if (getCustomArgumentResolvers() != null) {
			resolvers.addAll(getCustomArgumentResolvers());
		}

		// 捕获所有
		// 添加Principal方法参数解析器
		resolvers.add(new PrincipalMethodArgumentResolver());
		// 添加 RequestParam方法参数解析器
		resolvers.add(new RequestParamMethodArgumentResolver(getBeanFactory(), true));
		// 添加 Servlet模型属性方法处理器
		resolvers.add(new ServletModelAttributeMethodProcessor(true));

		return resolvers;
	}

	/**
	 * 返回要用于{@code @InitBinder}方法的参数解析器列表，包括内置解析器和自定义解析器。
	 */
	private List<HandlerMethodArgumentResolver> getDefaultInitBinderArgumentResolvers() {
		List<HandlerMethodArgumentResolver> resolvers = new ArrayList<>(20);

		// 基于注解的参数解析
		// 添加用于解析请求参数的参数解析器，不包括默认值
		resolvers.add(new RequestParamMethodArgumentResolver(getBeanFactory(), false));
		// 添加用于解析请求参数映射的参数解析器
		resolvers.add(new RequestParamMapMethodArgumentResolver());
		// 添加用于解析路径变量的参数解析器
		resolvers.add(new PathVariableMethodArgumentResolver());
		// 添加用于解析路径变量映射的参数解析器
		resolvers.add(new PathVariableMapMethodArgumentResolver());
		// 添加用于解析矩阵变量的参数解析器
		resolvers.add(new MatrixVariableMethodArgumentResolver());
		// 添加用于解析矩阵变量映射的参数解析器
		resolvers.add(new MatrixVariableMapMethodArgumentResolver());
		// 添加用于解析表达式值的参数解析器
		resolvers.add(new ExpressionValueMethodArgumentResolver(getBeanFactory()));
		// 添加用于解析会话属性的参数解析器
		resolvers.add(new SessionAttributeMethodArgumentResolver());
		// 添加用于解析请求属性的参数解析器
		resolvers.add(new RequestAttributeMethodArgumentResolver());

		// 基于类型的参数解析
		// 添加用于解析Servlet请求的参数解析器
		resolvers.add(new ServletRequestMethodArgumentResolver());
		// 添加用于解析Servlet响应的参数解析器
		resolvers.add(new ServletResponseMethodArgumentResolver());

		// 自定义参数解析
		if (getCustomArgumentResolvers() != null) {
			// 如果存在自定义参数解析器，则添加到参数解析器列表中
			resolvers.addAll(getCustomArgumentResolvers());
		}

		// 捕获所有
		// 添加用于解析Principal的参数解析器
		resolvers.add(new PrincipalMethodArgumentResolver());
		// 添加用于解析请求参数，包括默认值的参数解析器
		resolvers.add(new RequestParamMethodArgumentResolver(getBeanFactory(), true));

		return resolvers;
	}

	/**
	 * 返回要使用的返回值处理器列表，包括内置处理器和通过{@link #setReturnValueHandlers}提供的自定义处理器。
	 */
	private List<HandlerMethodReturnValueHandler> getDefaultReturnValueHandlers() {
		List<HandlerMethodReturnValueHandler> handlers = new ArrayList<>(20);

		// 单一用途的返回值类型
		// 添加用于处理ModelAndView返回值的处理器
		handlers.add(new ModelAndViewMethodReturnValueHandler());
		// 添加用于处理Model返回值的处理器
		handlers.add(new ModelMethodProcessor());
		// 添加用于处理View返回值的处理器
		handlers.add(new ViewMethodReturnValueHandler());
		// 添加用于处理ResponseBodyEmitter返回值的处理器
		handlers.add(new ResponseBodyEmitterReturnValueHandler(getMessageConverters(),
				this.reactiveAdapterRegistry, this.taskExecutor, this.contentNegotiationManager));
		// 添加用于处理StreamingResponseBody返回值的处理器
		handlers.add(new StreamingResponseBodyReturnValueHandler());
		// 添加用于处理HttpEntity返回值的处理器
		handlers.add(new HttpEntityMethodProcessor(getMessageConverters(),
				this.contentNegotiationManager, this.requestResponseBodyAdvice));
		// 添加用于处理HttpHeaders返回值的处理器
		handlers.add(new HttpHeadersReturnValueHandler());
		// 添加用于处理Callable返回值的处理器
		handlers.add(new CallableMethodReturnValueHandler());
		// 添加用于处理DeferredResult返回值的处理器
		handlers.add(new DeferredResultMethodReturnValueHandler());
		// 添加用于处理AsyncTask返回值的处理器
		handlers.add(new AsyncTaskMethodReturnValueHandler(this.beanFactory));

		// 基于注解的返回值类型
		// 添加用于处理ServletModelAttribute注解的返回值的处理器
		handlers.add(new ServletModelAttributeMethodProcessor(false));
		// 添加用于处理RequestResponseBody注解的返回值的处理器
		handlers.add(new RequestResponseBodyMethodProcessor(getMessageConverters(),
				this.contentNegotiationManager, this.requestResponseBodyAdvice));

		// 多用途的返回值类型
		// 添加用于处理ViewName返回值的处理器
		handlers.add(new ViewNameMethodReturnValueHandler());
		// 添加用于处理Map返回值的处理器
		handlers.add(new MapMethodProcessor());

		// 自定义返回值类型
		if (getCustomReturnValueHandlers() != null) {
			// 如果存在自定义返回值处理器，则添加到处理器列表中
			handlers.addAll(getCustomReturnValueHandlers());
		}

		// 捕获所有
		if (!CollectionUtils.isEmpty(getModelAndViewResolvers())) {
			// 如果存在ModelAndViewResolver，则添加用于处理ModelAndView的处理器
			handlers.add(new ModelAndViewResolverMethodReturnValueHandler(getModelAndViewResolvers()));
		} else {
			// 否则添加用于处理ServletModelAttribute的处理器
			handlers.add(new ServletModelAttributeMethodProcessor(true));
		}

		return handlers;
	}


	/**
	 * 总是返回 {@code true}，因为任何方法参数和返回值类型都将以某种方式进行处理。
	 * 如果一个方法参数没有被任何 HandlerMethodArgumentResolver 识别，那么如果它是一个简单的类型，
	 * 则将其解释为请求参数，否则将其解释为模型属性。
	 * 如果一个返回值没有被任何 HandlerMethodReturnValueHandler 识别，那么将其解释为模型属性。
	 */
	@Override
	protected boolean supportsInternal(HandlerMethod handlerMethod) {
		return true;
	}

	@Override
	protected ModelAndView handleInternal(HttpServletRequest request,
										  HttpServletResponse response, HandlerMethod handlerMethod) throws Exception {

		ModelAndView mav;
		// 检查请求
		checkRequest(request);

		// 如果需要，在同步块中执行invokeHandlerMethod。
		if (this.synchronizeOnSession) {
			// 获取HttpSession，如果存在则获取其锁对象，进行同步处理
			HttpSession session = request.getSession(false);
			if (session != null) {
				// 获取会话的锁对象
				Object mutex = WebUtils.getSessionMutex(session);
				synchronized (mutex) {
					// 加上互斥锁并调用invokeHandlerMethod处理请求
					mav = invokeHandlerMethod(request, response, handlerMethod);
				}
			} else {
				// 无HttpSession可用 -> 不需要互斥锁
				mav = invokeHandlerMethod(request, response, handlerMethod);
			}
		} else {
			// 完全不需要对会话进行同步...
			mav = invokeHandlerMethod(request, response, handlerMethod);
		}

		if (!response.containsHeader(HEADER_CACHE_CONTROL)) {
			// 如果响应中不包含HEADER_CACHE_CONTROL头部
			if (getSessionAttributesHandler(handlerMethod).hasSessionAttributes()) {
				// 如果处理器方法有会话属性，则应用缓存秒数
				applyCacheSeconds(response, this.cacheSecondsForSessionAttributeHandlers);
			} else {
				// 否则准备响应
				prepareResponse(response);
			}
		}

		return mav;
	}

	/**
	 * 这个实现总是返回-1。一个{@code @RequestMapping}方法可以
	 * 计算lastModified值，调用{@link WebRequest#checkNotModified(long)},
	 * 如果那个调用的结果为{@code true}，则返回{@code null}。
	 */
	@Override
	@SuppressWarnings("deprecation")
	protected long getLastModifiedInternal(HttpServletRequest request, HandlerMethod handlerMethod) {
		return -1;
	}


	/**
	 * 返回给定处理器类型（从不为null）的{@link SessionAttributesHandler}实例。
	 */
	private SessionAttributesHandler getSessionAttributesHandler(HandlerMethod handlerMethod) {
		return this.sessionAttributesHandlerCache.computeIfAbsent(
				handlerMethod.getBeanType(),
				type -> new SessionAttributesHandler(type, this.sessionAttributeStore));
	}

	/**
	 * 调用{@link RequestMapping}处理器方法，如果需要视图解析，则准备一个{@link ModelAndView}。
	 *
	 * @see #createInvocableHandlerMethod(HandlerMethod)
	 * @since 4.2
	 */
	@Nullable
	protected ModelAndView invokeHandlerMethod(HttpServletRequest request,
											   HttpServletResponse response, HandlerMethod handlerMethod) throws Exception {

		// 创建ServletWebRequest对象，包装请求和响应对象
		ServletWebRequest webRequest = new ServletWebRequest(request, response);
		try {
			// 获取数据绑定器工厂
			WebDataBinderFactory binderFactory = getDataBinderFactory(handlerMethod);
			// 获取模型工厂
			ModelFactory modelFactory = getModelFactory(handlerMethod, binderFactory);

			// 创建ServletInvocableHandlerMethod对象
			ServletInvocableHandlerMethod invocableMethod = createInvocableHandlerMethod(handlerMethod);
			if (this.argumentResolvers != null) {
				// 设置参数解析器
				invocableMethod.setHandlerMethodArgumentResolvers(this.argumentResolvers);
			}
			if (this.returnValueHandlers != null) {
				// 设置返回值处理器
				invocableMethod.setHandlerMethodReturnValueHandlers(this.returnValueHandlers);
			}
			// 设置数据绑定器工厂
			invocableMethod.setDataBinderFactory(binderFactory);
			// 设置参数名称发现器
			invocableMethod.setParameterNameDiscoverer(this.parameterNameDiscoverer);

			// 创建ModelAndViewContainer对象
			ModelAndViewContainer mavContainer = new ModelAndViewContainer();
			// 添加所有输入闪存属性到模型中
			mavContainer.addAllAttributes(RequestContextUtils.getInputFlashMap(request));
			// 初始化模型
			modelFactory.initModel(webRequest, mavContainer, invocableMethod);
			// 设置重定向时，是否忽略默认模型
			mavContainer.setIgnoreDefaultModelOnRedirect(this.ignoreDefaultModelOnRedirect);

			// 创建AsyncWebRequest对象，用于异步处理请求
			AsyncWebRequest asyncWebRequest = WebAsyncUtils.createAsyncWebRequest(request, response);
			// 设置异步请求超时时间
			asyncWebRequest.setTimeout(this.asyncRequestTimeout);

			// 获取WebAsyncManager对象
			WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);
			// 设置异步请求执行线程池
			asyncManager.setTaskExecutor(this.taskExecutor);
			// 在异步管理器中设置异步请求
			asyncManager.setAsyncWebRequest(asyncWebRequest);
			// 注册 Callable拦截器
			asyncManager.registerCallableInterceptors(this.callableInterceptors);
			// 注册 延迟结果拦截器
			asyncManager.registerDeferredResultInterceptors(this.deferredResultInterceptors);

			if (asyncManager.hasConcurrentResult()) {
				// 如果有并发结果，则恢复执行并发结果的处理
				Object result = asyncManager.getConcurrentResult();
				// 从并发结果上下文中获取第一个对象，作为模型和视图容器对象
				mavContainer = (ModelAndViewContainer) asyncManager.getConcurrentResultContext()[0];
				// 清除异步管理器中的并发结果
				asyncManager.clearConcurrentResult();
				LogFormatUtils.traceDebug(logger, traceOn -> {
					String formatted = LogFormatUtils.formatValue(result, !traceOn);
					return "Resume with async result [" + formatted + "]";
				});
				// 包装并发结果
				invocableMethod = invocableMethod.wrapConcurrentResult(result);
			}

			// 调用invokeAndHandle方法处理请求
			invocableMethod.invokeAndHandle(webRequest, mavContainer);
			if (asyncManager.isConcurrentHandlingStarted()) {
				// 如果异步处理已经开始，则返回null
				return null;
			}

			// 获取ModelAndView对象
			return getModelAndView(mavContainer, modelFactory, webRequest);
		} finally {
			// 请求处理完成
			webRequest.requestCompleted();
		}
	}

	/**
	 * 从给定的{@link HandlerMethod}定义创建一个{@link ServletInvocableHandlerMethod}。
	 *
	 * @param handlerMethod 一个{@link HandlerMethod}定义
	 * @return 相应的{@link ServletInvocableHandlerMethod}（或其自定义子类）
	 * @since 4.2
	 */
	protected ServletInvocableHandlerMethod createInvocableHandlerMethod(HandlerMethod handlerMethod) {
		return new ServletInvocableHandlerMethod(handlerMethod);
	}

	private ModelFactory getModelFactory(HandlerMethod handlerMethod, WebDataBinderFactory binderFactory) {
		// 获取会话属性处理器
		SessionAttributesHandler sessionAttrHandler = getSessionAttributesHandler(handlerMethod);
		// 获取处理器类型
		Class<?> handlerType = handlerMethod.getBeanType();
		// 获取标记有@ModelAttribute的方法集合
		Set<Method> methods = this.modelAttributeCache.get(handlerType);
		if (methods == null) {
			// 根据处理器类型获取标记有@ModelAttribute的方法
			methods = MethodIntrospector.selectMethods(handlerType, MODEL_ATTRIBUTE_METHODS);
			// 添加到缓存中
			this.modelAttributeCache.put(handlerType, methods);
		}
		List<InvocableHandlerMethod> attrMethods = new ArrayList<>();
		// 全局方法优先
		this.modelAttributeAdviceCache.forEach((controllerAdviceBean, methodSet) -> {
			if (controllerAdviceBean.isApplicableToBeanType(handlerType)) {
				// 如果控制器通知bean适用于处理器类型，则执行以下操作
				// 解析控制器通知bean
				Object bean = controllerAdviceBean.resolveBean();
				// 遍历方法集合
				for (Method method : methodSet) {
					// 创建模型属性方法并添加到attrMethods集合中
					attrMethods.add(createModelAttributeMethod(binderFactory, bean, method));
				}
			}

		});
		// 遍历方法集合
		for (Method method : methods) {
			// 获取处理器方法对应的bean
			Object bean = handlerMethod.getBean();
			// 创建模型属性方法并添加到attrMethods集合中
			attrMethods.add(createModelAttributeMethod(binderFactory, bean, method));
		}
		// 返回新的ModelFactory对象，包含模型属性方法、数据绑定器工厂和会话属性处理器
		return new ModelFactory(attrMethods, binderFactory, sessionAttrHandler);
	}

	private InvocableHandlerMethod createModelAttributeMethod(WebDataBinderFactory factory, Object bean, Method method) {
		// 创建InvocableHandlerMethod对象，用于调用模型属性方法
		InvocableHandlerMethod attrMethod = new InvocableHandlerMethod(bean, method);
		if (this.argumentResolvers != null) {
			// 设置参数解析器
			attrMethod.setHandlerMethodArgumentResolvers(this.argumentResolvers);
		}
		// 设置参数名称发现器
		attrMethod.setParameterNameDiscoverer(this.parameterNameDiscoverer);
		// 设置数据绑定工厂
		attrMethod.setDataBinderFactory(factory);
		// 返回创建的InvocableHandlerMethod对象
		return attrMethod;
	}

	private WebDataBinderFactory getDataBinderFactory(HandlerMethod handlerMethod) throws Exception {
		// 获取处理类型
		Class<?> handlerType = handlerMethod.getBeanType();
		// 获取标记有@InitBinder的方法
		Set<Method> methods = this.initBinderCache.get(handlerType);
		if (methods == null) {
			// 根据处理器类型获取标记有@InitBinder的方法
			methods = MethodIntrospector.selectMethods(handlerType, INIT_BINDER_METHODS);
			// 添加到缓存中
			this.initBinderCache.put(handlerType, methods);
		}
		List<InvocableHandlerMethod> initBinderMethods = new ArrayList<>();
		// 全局方法优先
		// 遍历initBinderAdviceCache，处理控制器通知bean的初始化绑定方法
		this.initBinderAdviceCache.forEach((controllerAdviceBean, methodSet) -> {
			if (controllerAdviceBean.isApplicableToBeanType(handlerType)) {
				// 如果控制器通知bean适用于处理器类型，则执行以下操作
				// 解析控制器通知bean
				Object bean = controllerAdviceBean.resolveBean();
				// 遍历方法集合，创建初始化绑定方法并添加到initBinderMethods集合中
				for (Method method : methodSet) {
					initBinderMethods.add(createInitBinderMethod(bean, method));
				}
			}
		});
		// 遍历方法集合，创建初始化绑定方法并添加到initBinderMethods集合中
		for (Method method : methods) {
			Object bean = handlerMethod.getBean();
			initBinderMethods.add(createInitBinderMethod(bean, method));
		}
		// 创建数据绑定器工厂并返回
		return createDataBinderFactory(initBinderMethods);
	}

	private InvocableHandlerMethod createInitBinderMethod(Object bean, Method method) {
		// 创建InvocableHandlerMethod对象，用于调用初始化绑定方法
		InvocableHandlerMethod binderMethod = new InvocableHandlerMethod(bean, method);
		// 设置参数解析器
		if (this.initBinderArgumentResolvers != null) {
			binderMethod.setHandlerMethodArgumentResolvers(this.initBinderArgumentResolvers);
		}
		// 设置数据绑定器工厂
		binderMethod.setDataBinderFactory(new DefaultDataBinderFactory(this.webBindingInitializer));
		// 设置参数名称发现器
		binderMethod.setParameterNameDiscoverer(this.parameterNameDiscoverer);
		// 返回创建的InvocableHandlerMethod对象
		return binderMethod;
	}

	/**
	 * 创建一个新的InitBinderDataBinderFactory实例的模板方法。
	 * <p>默认实现创建一个ServletRequestDataBinderFactory。
	 * 这可以针对自定义的ServletRequestDataBinder子类进行覆盖。
	 *
	 * @param binderMethods {@code @InitBinder}方法列表
	 * @return 要使用的InitBinderDataBinderFactory实例
	 * @throws Exception 如果状态无效或参数错误，则抛出异常
	 */
	protected InitBinderDataBinderFactory createDataBinderFactory(List<InvocableHandlerMethod> binderMethods)
			throws Exception {

		return new ServletRequestDataBinderFactory(binderMethods, getWebBindingInitializer());
	}

	@Nullable
	private ModelAndView getModelAndView(ModelAndViewContainer mavContainer,
										 ModelFactory modelFactory, NativeWebRequest webRequest) throws Exception {

		// 更新模型
		modelFactory.updateModel(webRequest, mavContainer);
		if (mavContainer.isRequestHandled()) {
			// 如果请求已处理，则返回null
			return null;
		}
		// 获取模型
		ModelMap model = mavContainer.getModel();
		// 创建ModelAndView对象
		ModelAndView mav = new ModelAndView(mavContainer.getViewName(), model, mavContainer.getStatus());
		if (!mavContainer.isViewReference()) {
			// 如果不是视图引用，则设置视图
			mav.setView((View) mavContainer.getView());
		}
		if (model instanceof RedirectAttributes) {
			// 如果是模型是重定向属性类型，获取闪存映射
			Map<String, ?> flashAttributes = ((RedirectAttributes) model).getFlashAttributes();
			// 获取原生请求
			HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
			if (request != null) {
				// 设置闪存映射到请求上下文中
				RequestContextUtils.getOutputFlashMap(request).putAll(flashAttributes);
			}
		}
		// 返回模型和视图对象
		return mav;
	}

}
