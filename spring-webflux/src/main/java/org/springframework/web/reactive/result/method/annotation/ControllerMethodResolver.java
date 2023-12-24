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

package org.springframework.web.reactive.result.method.annotation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.KotlinDetector;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils.MethodFilter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.method.ControllerAdviceBean;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.annotation.ExceptionHandlerMethodResolver;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver;
import org.springframework.web.reactive.result.method.InvocableHandlerMethod;
import org.springframework.web.reactive.result.method.SyncHandlerMethodArgumentResolver;
import org.springframework.web.reactive.result.method.SyncInvocableHandlerMethod;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 包私有类，辅助 {@link RequestMappingHandlerAdapter} 解析、初始化和缓存在
 * {@code @Controller} 和 {@code @ControllerAdvice} 组件中声明的带注解方法。
 *
 * <p>辅助以下注解：
 * <ul>
 * <li>{@code @InitBinder}
 * <li>{@code @ModelAttribute}
 * <li>{@code @RequestMapping}
 * <li>{@code @ExceptionHandler}
 * </ul>
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
class ControllerMethodResolver {

	/**
	 * 用于匹配 {@link InitBinder @InitBinder} 方法的 MethodFilter。
	 */
	private static final MethodFilter INIT_BINDER_METHODS = method ->
			AnnotatedElementUtils.hasAnnotation(method, InitBinder.class);

	/**
	 * 用于匹配 {@link ModelAttribute @ModelAttribute} 方法的 MethodFilter。
	 */
	private static final MethodFilter MODEL_ATTRIBUTE_METHODS = method ->
			(!AnnotatedElementUtils.hasAnnotation(method, RequestMapping.class) &&
					AnnotatedElementUtils.hasAnnotation(method, ModelAttribute.class));


	/**
	 * 日志记录器，用于记录该类的日志信息
	 */
	private static final Log logger = LogFactory.getLog(ControllerMethodResolver.class);

	/**
	 * 用于解析 @InitBinder 注解的解析器列表
	 */
	private final List<SyncHandlerMethodArgumentResolver> initBinderResolvers;

	/**
	 * 用于解析 @ModelAttribute 注解的解析器列表
	 */
	private final List<HandlerMethodArgumentResolver> modelAttributeResolvers;

	/**
	 * 用于解析 @RequestMapping 注解的解析器列表
	 */
	private final List<HandlerMethodArgumentResolver> requestMappingResolvers;

	/**
	 * 用于解析 @ExceptionHandler 注解的解析器列表
	 */
	private final List<HandlerMethodArgumentResolver> exceptionHandlerResolvers;

	/**
	 * 反应式适配器注册表，用于处理反应式类型的适配器
	 */
	private final ReactiveAdapterRegistry reactiveAdapterRegistry;

	/**
	 * 缓存 @InitBinder 方法的映射，键为类，值为对应的方法集合
	 */
	private final Map<Class<?>, Set<Method>> initBinderMethodCache = new ConcurrentHashMap<>(64);

	/**
	 * 缓存 @ModelAttribute 方法的映射，键为类，值为对应的方法集合
	 */
	private final Map<Class<?>, Set<Method>> modelAttributeMethodCache = new ConcurrentHashMap<>(64);

	/**
	 * 缓存 @ExceptionHandler 方法的映射，键为类，值为对应的异常处理方法解析器
	 */
	private final Map<Class<?>, ExceptionHandlerMethodResolver> exceptionHandlerCache = new ConcurrentHashMap<>(64);

	/**
	 * 缓存 @ControllerAdvice 中 @InitBinder 方法的映射，键为 ControllerAdviceBean，值为对应的方法集合
	 */
	private final Map<ControllerAdviceBean, Set<Method>> initBinderAdviceCache = new LinkedHashMap<>(64);

	/**
	 * 缓存 @ControllerAdvice 中 @ModelAttribute 方法的映射，键为 ControllerAdviceBean，值为对应的方法集合
	 */
	private final Map<ControllerAdviceBean, Set<Method>> modelAttributeAdviceCache = new LinkedHashMap<>(64);

	/**
	 * 缓存 @ControllerAdvice 中 @ExceptionHandler 方法的映射，键为 ControllerAdviceBean，值为对应的异常处理方法解析器
	 */
	private final Map<ControllerAdviceBean, ExceptionHandlerMethodResolver> exceptionHandlerAdviceCache =
			new LinkedHashMap<>(64);

	/**
	 * 缓存会话属性处理器的映射，键为类，值为对应的会话属性处理器
	 */
	private final Map<Class<?>, SessionAttributesHandler> sessionAttributesHandlerCache = new ConcurrentHashMap<>(64);

	/**
	 * 构造函数，用于创建 {@code ControllerMethodResolver} 实例。
	 *
	 * @param customResolvers 自定义解析器的配置
	 * @param adapterRegistry 适配不同响应式类型的注册表
	 * @param context         可配置的应用程序上下文，用于解析方法参数默认值和检测 {@code @ControllerAdvice} beans
	 * @param readers         用于反序列化请求体的 HTTP 消息读取器列表
	 */
	ControllerMethodResolver(ArgumentResolverConfigurer customResolvers, ReactiveAdapterRegistry adapterRegistry,
							 ConfigurableApplicationContext context, List<HttpMessageReader<?>> readers) {

		// 确保传入的参数不为 null
		Assert.notNull(customResolvers, "ArgumentResolverConfigurer is required");
		Assert.notNull(adapterRegistry, "ReactiveAdapterRegistry is required");
		Assert.notNull(context, "ApplicationContext is required");
		Assert.notNull(readers, "HttpMessageReader List is required");

		// 初始化 @InitBinder 方法解析器列表
		this.initBinderResolvers = initBinderResolvers(customResolvers, adapterRegistry, context);

		// 初始化 @ModelAttribute 方法解析器列表
		this.modelAttributeResolvers = modelMethodResolvers(customResolvers, adapterRegistry, context);

		// 初始化 @RequestMapping 方法解析器列表
		this.requestMappingResolvers = requestMappingResolvers(customResolvers, adapterRegistry, context, readers);

		// 初始化 @ExceptionHandler 方法解析器列表
		this.exceptionHandlerResolvers = exceptionHandlerResolvers(customResolvers, adapterRegistry, context);

		// 初始化反应式适配器注册表
		this.reactiveAdapterRegistry = adapterRegistry;

		// 初始化 ControllerAdvice 缓存
		initControllerAdviceCaches(context);
	}

	/**
	 * 初始化 {@code @InitBinder} 解析器列表。
	 *
	 * @param customResolvers 自定义解析器的配置
	 * @param adapterRegistry 适配不同响应式类型的注册表
	 * @param context         可配置的应用程序上下文，用于解析方法参数默认值和检测 {@code @ControllerAdvice} beans
	 * @return 包含初始化的 {@code @InitBinder} 解析器的同步方法参数解析器列表
	 */
	private List<SyncHandlerMethodArgumentResolver> initBinderResolvers(
			ArgumentResolverConfigurer customResolvers, ReactiveAdapterRegistry adapterRegistry,
			ConfigurableApplicationContext context) {

		// 从自定义解析器中初始化解析器，并过滤出同步的处理程序方法参数解析器，返回过滤后的列表
		return initResolvers(customResolvers, adapterRegistry, context, false, Collections.emptyList()).stream()
				// 过滤出同步的处理程序方法参数解析器
				.filter(resolver -> resolver instanceof SyncHandlerMethodArgumentResolver)
				// 将解析器转换为 SyncHandlerMethodArgumentResolver 类型，并收集为列表
				.map(resolver -> (SyncHandlerMethodArgumentResolver) resolver)
				.collect(Collectors.toList());
	}

	/**
	 * 获取模型方法解析器列表。
	 *
	 * @param customResolvers 自定义解析器的配置
	 * @param adapterRegistry 适配不同响应式类型的注册表
	 * @param context         可配置的应用程序上下文，用于解析方法参数默认值和检测 {@code @ControllerAdvice} beans
	 * @return 包含初始化的模型方法解析器的方法参数解析器列表
	 */
	private static List<HandlerMethodArgumentResolver> modelMethodResolvers(
			ArgumentResolverConfigurer customResolvers, ReactiveAdapterRegistry adapterRegistry,
			ConfigurableApplicationContext context) {

		return initResolvers(customResolvers, adapterRegistry, context, true, Collections.emptyList());
	}

	private static List<HandlerMethodArgumentResolver> requestMappingResolvers(
			ArgumentResolverConfigurer customResolvers, ReactiveAdapterRegistry adapterRegistry,
			ConfigurableApplicationContext context, List<HttpMessageReader<?>> readers) {

		return initResolvers(customResolvers, adapterRegistry, context, true, readers);
	}

	private static List<HandlerMethodArgumentResolver> exceptionHandlerResolvers(
			ArgumentResolverConfigurer customResolvers, ReactiveAdapterRegistry adapterRegistry,
			ConfigurableApplicationContext context) {

		return initResolvers(customResolvers, adapterRegistry, context, false, Collections.emptyList());
	}

	/**
	 * 初始化方法参数解析器列表。
	 *
	 * @param customResolvers    自定义解析器的配置
	 * @param adapterRegistry    适配不同响应式类型的注册表
	 * @param context            可配置的应用程序上下文，用于解析方法参数默认值和检测 {@code @ControllerAdvice} beans
	 * @param supportDataBinding 是否支持数据绑定
	 * @param readers            HTTP 消息读取器列表
	 * @return 方法参数解析器列表
	 */
	private static List<HandlerMethodArgumentResolver> initResolvers(ArgumentResolverConfigurer customResolvers,
																	 ReactiveAdapterRegistry adapterRegistry, ConfigurableApplicationContext context,
																	 boolean supportDataBinding, List<HttpMessageReader<?>> readers) {

		// 从应用上下文中获取可配置的可列表化的 Bean 工厂
		ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();

		// 判断是否为 RequestMapping 方法
		boolean requestMappingMethod = !readers.isEmpty() && supportDataBinding;

		// 创建一个 HandlerMethodArgumentResolver 列表，用于处理方法参数
		List<HandlerMethodArgumentResolver> result = new ArrayList<>(30);

		// 添加注解型参数解析器
		// @RequestParam注解的解析器
		result.add(new RequestParamMethodArgumentResolver(beanFactory, adapterRegistry, false));
		// @RequestParam注解+Map参数类型的解析器
		result.add(new RequestParamMapMethodArgumentResolver(adapterRegistry));
		// @PathVariable注解解析器
		result.add(new PathVariableMethodArgumentResolver(beanFactory, adapterRegistry));
		// @PathVariable注解+Map参数类型的解析器
		result.add(new PathVariableMapMethodArgumentResolver(adapterRegistry));
		// @MatrixVariable注解解析器
		result.add(new MatrixVariableMethodArgumentResolver(beanFactory, adapterRegistry));
		// @MatrixVariable注解+Map参数类型的解析器
		result.add(new MatrixVariableMapMethodArgumentResolver(adapterRegistry));

		// 如果有读取器，则添加请求体和请求部分的参数解析器
		if (!readers.isEmpty()) {
			// @RequestBody 参数解析器
			result.add(new RequestBodyMethodArgumentResolver(readers, adapterRegistry));
			// @RequestPart 参数解析器
			result.add(new RequestPartMethodArgumentResolver(readers, adapterRegistry));
		}

		// 如果支持数据绑定，则添加模型属性的参数解析器
		if (supportDataBinding) {
			// @ModelAttribute 参数解析器
			result.add(new ModelAttributeMethodArgumentResolver(adapterRegistry, false));
		}

		// 添加注解型参数解析器，如请求头、Cookie、表达式、会话属性等
		// @RequestHeader注解 参数解析器
		result.add(new RequestHeaderMethodArgumentResolver(beanFactory, adapterRegistry));
		// @RequestHeader注解+Map类型的参数解析器
		result.add(new RequestHeaderMapMethodArgumentResolver(adapterRegistry));
		// @CookieValue 参数解析器
		result.add(new CookieValueMethodArgumentResolver(beanFactory, adapterRegistry));
		// @Value 参数解析器
		result.add(new ExpressionValueMethodArgumentResolver(beanFactory, adapterRegistry));
		// @SessionAttribute 参数解析器
		result.add(new SessionAttributeMethodArgumentResolver(beanFactory, adapterRegistry));
		// @RequestAttribute 参数解析器
		result.add(new RequestAttributeMethodArgumentResolver(beanFactory, adapterRegistry));

		// 基于类型的参数解析器，如 HttpEntity、Model、Errors 等
		if (!readers.isEmpty()) {
			result.add(new HttpEntityMethodArgumentResolver(readers, adapterRegistry));
		}
		result.add(new ModelMethodArgumentResolver(adapterRegistry));
		if (supportDataBinding) {
			result.add(new ErrorsMethodArgumentResolver(adapterRegistry));
		}
		// 解析 ServerWebExchange 相关的方法参数值
		result.add(new ServerWebExchangeMethodArgumentResolver(adapterRegistry));
		// 解析 Principal类型参数
		result.add(new PrincipalMethodArgumentResolver(adapterRegistry));

		// 如果是 RequestMapping 方法，则添加 SessionStatus 解析器
		if (requestMappingMethod) {
			result.add(new SessionStatusMethodArgumentResolver());
		}

		// 添加 WebSession 解析器
		result.add(new WebSessionMethodArgumentResolver(adapterRegistry));

		// 如果检测到 Kotlin 存在，则添加 Continuation 解析器
		if (KotlinDetector.isKotlinPresent()) {
			result.add(new ContinuationHandlerMethodArgumentResolver());
		}

		// 添加自定义的参数解析器
		result.addAll(customResolvers.getCustomResolvers());

		// 添加@RequestParam 注解的参数解析器
		result.add(new RequestParamMethodArgumentResolver(beanFactory, adapterRegistry, true));
		if (supportDataBinding) {
			// @ModelAttribute 注解的参数解析器
			result.add(new ModelAttributeMethodArgumentResolver(adapterRegistry, true));
		}

		return result;
	}

	/**
	 * 初始化 {@link ControllerAdvice} 缓存的私有方法。
	 *
	 * @param applicationContext 应用程序上下文
	 */
	private void initControllerAdviceCaches(ApplicationContext applicationContext) {
		// 查找带有注解的 ControllerAdviceBean
		List<ControllerAdviceBean> beans = ControllerAdviceBean.findAnnotatedBeans(applicationContext);

		// 遍历找到的 ControllerAdviceBean
		for (ControllerAdviceBean bean : beans) {
			Class<?> beanType = bean.getBeanType();
			if (beanType != null) {
				// 选择带有 @ModelAttribute 注解的方法
				Set<Method> attrMethods = MethodIntrospector.selectMethods(beanType, MODEL_ATTRIBUTE_METHODS);
				if (!attrMethods.isEmpty()) {
					this.modelAttributeAdviceCache.put(bean, attrMethods);
				}

				// 选择带有 @InitBinder 注解的方法
				Set<Method> binderMethods = MethodIntrospector.selectMethods(beanType, INIT_BINDER_METHODS);
				if (!binderMethods.isEmpty()) {
					this.initBinderAdviceCache.put(bean, binderMethods);
				}

				// 解析带有 @ExceptionHandler 注解的方法
				ExceptionHandlerMethodResolver resolver = new ExceptionHandlerMethodResolver(beanType);
				if (resolver.hasExceptionMappings()) {
					this.exceptionHandlerAdviceCache.put(bean, resolver);
				}
			}
		}

		// 记录日志，显示找到的 ControllerAdviceBean 的数量
		if (logger.isDebugEnabled()) {
			int modelSize = this.modelAttributeAdviceCache.size();
			int binderSize = this.initBinderAdviceCache.size();
			int handlerSize = this.exceptionHandlerAdviceCache.size();
			if (modelSize == 0 && binderSize == 0 && handlerSize == 0) {
				logger.debug("ControllerAdvice beans: none");
			} else {
				logger.debug("ControllerAdvice beans: " + modelSize + " @ModelAttribute, " + binderSize +
						" @InitBinder, " + handlerSize + " @ExceptionHandler");
			}
		}
	}


	/**
	 * 根据给定的 {@code @RequestMapping} 方法返回一个使用参数解析器初始化的 {@link InvocableHandlerMethod}。
	 *
	 * @param handlerMethod 处理方法
	 * @return 初始化后的 {@link InvocableHandlerMethod}
	 */
	public InvocableHandlerMethod getRequestMappingMethod(HandlerMethod handlerMethod) {
		// 创建一个 InvocableHandlerMethod 对象，并指定要调用的处理程序方法
		InvocableHandlerMethod invocable = new InvocableHandlerMethod(handlerMethod);

		// 设置该 InvocableHandlerMethod 使用的参数解析器
		invocable.setArgumentResolvers(this.requestMappingResolvers);

		// 设置该 InvocableHandlerMethod 使用的反应式适配器注册表
		invocable.setReactiveAdapterRegistry(this.reactiveAdapterRegistry);

		// 返回配置完毕的 InvocableHandlerMethod 对象
		return invocable;
	}

	/**
	 * 查找 {@code @ControllerAdvice} 组件中的 {@code @InitBinder} 方法或给定 {@code @RequestMapping} 方法的控制器中的方法。
	 *
	 * @param handlerMethod 给定的 {@code @RequestMapping} 方法的处理器方法
	 * @return {@code @InitBinder} 方法的列表
	 */
	public List<SyncInvocableHandlerMethod> getInitBinderMethods(HandlerMethod handlerMethod) {
		// 创建一个用于存储同步 SyncInvocableHandlerMethod 对象的列表
		List<SyncInvocableHandlerMethod> result = new ArrayList<>();

		// 获取处理程序方法的类型
		Class<?> handlerType = handlerMethod.getBeanType();

		// 处理全局方法的 @InitBinder
		this.initBinderAdviceCache.forEach((adviceBean, methods) -> {
			// 如果全局方法适用于处理程序方法的类型，则添加对应的初始化绑定器方法到列表中
			if (adviceBean.isApplicableToBeanType(handlerType)) {
				Object bean = adviceBean.resolveBean();
				methods.forEach(method -> result.add(getInitBinderMethod(bean, method)));
			}
		});

		// 处理当前处理程序类型上的 @InitBinder 方法
		this.initBinderMethodCache
				.computeIfAbsent(handlerType, clazz -> MethodIntrospector.selectMethods(handlerType, INIT_BINDER_METHODS))
				.forEach(method -> {
					Object bean = handlerMethod.getBean();
					result.add(getInitBinderMethod(bean, method));
				});

		// 返回包含初始化绑定器方法的 SyncInvocableHandlerMethod 对象列表
		return result;
	}

	/**
	 * 获取 {@code @InitBinder} 方法的 {@link SyncInvocableHandlerMethod}。
	 *
	 * @param bean   调用方法的 bean
	 * @param method {@code @InitBinder} 方法
	 * @return {@link SyncInvocableHandlerMethod} 对象
	 */
	private SyncInvocableHandlerMethod getInitBinderMethod(Object bean, Method method) {
		SyncInvocableHandlerMethod invocable = new SyncInvocableHandlerMethod(bean, method);
		invocable.setArgumentResolvers(this.initBinderResolvers);
		return invocable;
	}

	/**
	 * 查找{@code @ModelAttribute}方法，可在{@code @ControllerAdvice}组件或给定{@code @RequestMapping}方法的控制器中找到。
	 *
	 * @param handlerMethod 给定的处理方法
	 * @return 包含{@code @ModelAttribute}方法的InvocableHandlerMethod列表
	 */
	public List<InvocableHandlerMethod> getModelAttributeMethods(HandlerMethod handlerMethod) {
		// 创建一个用于存储 InvocableHandlerMethod 对象的列表
		List<InvocableHandlerMethod> result = new ArrayList<>();

		// 获取处理程序方法的类型
		Class<?> handlerType = handlerMethod.getBeanType();

		// 遍历缓存中的全局 @ModelAttribute 方法
		this.modelAttributeAdviceCache.forEach((adviceBean, methods) -> {
			// 如果全局方法适用于处理程序方法的类型，则创建相应的 InvocableHandlerMethod 并添加到列表中
			if (adviceBean.isApplicableToBeanType(handlerType)) {
				Object bean = adviceBean.resolveBean();
				methods.forEach(method -> result.add(createAttributeMethod(bean, method)));
			}
		});

		// 检索当前处理程序类型上的 @ModelAttribute 方法，并将其添加到列表中
		this.modelAttributeMethodCache
				.computeIfAbsent(handlerType, clazz -> MethodIntrospector.selectMethods(handlerType, MODEL_ATTRIBUTE_METHODS))
				.forEach(method -> {
					Object bean = handlerMethod.getBean();
					result.add(createAttributeMethod(bean, method));
				});

		// 返回包含 @ModelAttribute 方法的 InvocableHandlerMethod 对象列表
		return result;
	}

	/**
	 * 创建属性方法的 {@link InvocableHandlerMethod}。
	 *
	 * @param bean   调用方法的 bean
	 * @param method 属性方法
	 * @return {@link InvocableHandlerMethod} 对象
	 */
	private InvocableHandlerMethod createAttributeMethod(Object bean, Method method) {
		InvocableHandlerMethod invocable = new InvocableHandlerMethod(bean, method);
		invocable.setArgumentResolvers(this.modelAttributeResolvers);
		return invocable;
	}

	/**
	 * 查找在 {@code @ControllerAdvice} 组件或给定 {@code @RequestMapping} 方法的控制器中的 {@code @ExceptionHandler} 方法。
	 *
	 * @param ex            异常对象
	 * @param handlerMethod 处理方法
	 * @return {@link InvocableHandlerMethod} 对象，如果找不到匹配的方法则返回 {@code null}
	 */
	@Nullable
	public InvocableHandlerMethod getExceptionHandlerMethod(Throwable ex, HandlerMethod handlerMethod) {
		// 获取处理程序方法的类型
		Class<?> handlerType = handlerMethod.getBeanType();

		// 首先在控制器本地查找异常处理器方法
		Object targetBean = handlerMethod.getBean();
		Method targetMethod = this.exceptionHandlerCache
				.computeIfAbsent(handlerType, ExceptionHandlerMethodResolver::new)
				.resolveMethodByThrowable(ex);

		// 如果未在控制器本地找到异常处理器方法，则尝试全局异常处理器
		if (targetMethod == null) {
			for (Map.Entry<ControllerAdviceBean, ExceptionHandlerMethodResolver> entry : this.exceptionHandlerAdviceCache.entrySet()) {
				ControllerAdviceBean advice = entry.getKey();
				// 如果全局异常处理器适用于处理程序方法的类型，则查找匹配的异常处理器方法
				if (advice.isApplicableToBeanType(handlerType)) {
					targetBean = advice.resolveBean();
					targetMethod = entry.getValue().resolveMethodByThrowable(ex);
					if (targetMethod != null) {
						break;
					}
				}
			}
		}

		// 如果未找到匹配的异常处理器方法，则返回 null
		if (targetMethod == null) {
			return null;
		}

		// 创建 InvocableHandlerMethod 对象并配置相应的参数解析器
		InvocableHandlerMethod invocable = new InvocableHandlerMethod(targetBean, targetMethod);
		invocable.setArgumentResolvers(this.exceptionHandlerResolvers);

		// 返回配置好的异常处理器方法的 InvocableHandlerMethod 对象
		return invocable;
	}

	/**
	 * 根据给定的控制器方法返回基于类型级别的 {@code @SessionAttributes} 注解的处理器。
	 *
	 * @param handlerMethod 控制器方法
	 * @return {@link SessionAttributesHandler} 对象，处理给定控制器方法的 {@code @SessionAttributes} 注解
	 */
	public SessionAttributesHandler getSessionAttributesHandler(HandlerMethod handlerMethod) {
		// 获取处理程序方法的类型
		Class<?> handlerType = handlerMethod.getBeanType();

		// 从缓存中获取对应处理程序方法类型的 SessionAttributesHandler 实例
		SessionAttributesHandler result = this.sessionAttributesHandlerCache.get(handlerType);

		// 如果缓存中不存在对应的 SessionAttributesHandler 实例，则创建并缓存
		if (result == null) {
			// 使用同步块确保线程安全
			synchronized (this.sessionAttributesHandlerCache) {
				// 再次检查，避免在同步块外创建实例后，其他线程已经创建了该实例
				result = this.sessionAttributesHandlerCache.get(handlerType);
				if (result == null) {
					// 创建新的 SessionAttributesHandler 实例
					result = new SessionAttributesHandler(handlerType);
					// 将新创建的实例放入缓存中
					this.sessionAttributesHandlerCache.put(handlerType, result);
				}
			}
		}

		// 返回获取或创建的 SessionAttributesHandler 实例
		return result;

	}


}
