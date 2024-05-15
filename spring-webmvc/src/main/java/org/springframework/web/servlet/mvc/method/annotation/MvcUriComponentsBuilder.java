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

package org.springframework.web.servlet.mvc.method.annotation;

import org.aopalliance.intercept.MethodInterceptor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.target.EmptyTargetSource;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.cglib.core.SpringNamingPolicy;
import org.springframework.cglib.proxy.Callback;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.Factory;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.SynthesizingMethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.objenesis.ObjenesisException;
import org.springframework.objenesis.SpringObjenesis;
import org.springframework.util.*;
import org.springframework.util.ReflectionUtils.MethodFilter;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.annotation.RequestParamMethodArgumentResolver;
import org.springframework.web.method.support.CompositeUriComponentsContributor;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.*;

/**
 * 通过指向 Spring MVC 控制器上的 {@code @RequestMapping} 方法来创建 {@link org.springframework.web.util.UriComponentsBuilder} 的实例。
 *
 * <p>有几组方法：
 * <ul>
 * <li>静态的 {@code fromXxx(...)} 方法，使用当前请求的信息准备链接，这由调用 {@link org.springframework.web.servlet.support.ServletUriComponentsBuilder#fromCurrentServletMapping()} 决定。
 * <li>静态的 {@code fromXxx(UriComponentsBuilder,...)} 方法可以在操作请求上下文之外时给定一个 baseUrl。
 * <li>基于实例的 {@code withXxx(...)} 方法，其中 MvcUriComponentsBuilder 的实例通过 {@link #relativeTo(org.springframework.web.util.UriComponentsBuilder)} 使用 baseUrl 创建。
 * </ul>
 *
 * <p><strong>注意：</strong> 此类使用 "Forwarded"（<a href="https://tools.ietf.org/html/rfc7239">RFC 7239</a>）、
 * "X-Forwarded-Host"、"X-Forwarded-Port" 和 "X-Forwarded-Proto" 头中的值（如果存在）来反映客户端发起的协议和地址。
 * 考虑使用 {@code ForwardedHeaderFilter} 来选择从一个中心位置提取和使用这些头，或者丢弃这些头。
 * 有关此过滤器的更多信息，请参阅 Spring Framework 参考文档。
 *
 * @author Oliver Gierke
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 4.0
 */
public class MvcUriComponentsBuilder {

	/**
	 * Bean 工厂中 {@link CompositeUriComponentsContributor} 对象的已知名称。
	 */
	public static final String MVC_URI_COMPONENTS_CONTRIBUTOR_BEAN_NAME = "mvcUriComponentsContributor";


	private static final Log logger = LogFactory.getLog(MvcUriComponentsBuilder.class);

	private static final SpringObjenesis objenesis = new SpringObjenesis();

	private static final PathMatcher pathMatcher = new AntPathMatcher();

	private static final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

	private static final CompositeUriComponentsContributor defaultUriComponentsContributor;

	static {
		defaultUriComponentsContributor = new CompositeUriComponentsContributor(
				new PathVariableMethodArgumentResolver(), new RequestParamMethodArgumentResolver(false));
	}

	private final UriComponentsBuilder baseUrl;


	/**
	 * 默认构造函数。受保护，以防止直接实例化。
	 *
	 * @see #fromController(Class)
	 * @see #fromMethodName(Class, String, Object...)
	 * @see #fromMethodCall(Object)
	 * @see #fromMappingName(String)
	 * @see #fromMethod(Class, Method, Object...)
	 */
	protected MvcUriComponentsBuilder(UriComponentsBuilder baseUrl) {
		Assert.notNull(baseUrl, "'baseUrl' is required");
		this.baseUrl = baseUrl;
	}


	/**
	 * 使用基本 URL 创建此类的实例。之后调用基于实例的 {@code withXxx(...}} 方法之一将创建相对于给定基本 URL 的 URL。
	 */
	public static MvcUriComponentsBuilder relativeTo(UriComponentsBuilder baseUrl) {
		return new MvcUriComponentsBuilder(baseUrl);
	}


	/**
	 * 从控制器类的映射和当前请求信息（包括 Servlet 映射）创建一个 {@link UriComponentsBuilder}。
	 * 如果控制器包含多个映射，则仅使用第一个。
	 * <p><strong>注意：</strong> 如果找到 "Forwarded" 和 "X-Forwarded-*" 头，则此方法会从中提取值。请参阅类级别文档。
	 *
	 * @param controllerType 要为其构建 URI 的控制器
	 * @return 一个 UriComponentsBuilder 实例（永远不会为 {@code null}）
	 */
	public static UriComponentsBuilder fromController(Class<?> controllerType) {
		return fromController(null, controllerType);
	}

	/**
	 * {@link #fromController(Class)} 的替代方法，接受表示基本 URL 的 {@code UriComponentsBuilder}。
	 * 当在处理请求的上下文之外使用 MvcUriComponentsBuilder 或应用不匹配当前请求的自定义 baseUrl 时，此方法非常有用。
	 * <p><strong>注意：</strong> 如果找到 "Forwarded" 和 "X-Forwarded-*" 头，则此方法会从中提取值。请参阅类级别文档。
	 *
	 * @param builder        基本 URL 的构建器；构建器将被克隆，因此不会被修改，并且可以在后续调用中重用。
	 * @param controllerType 要为其构建 URI 的控制器
	 * @return 一个 UriComponentsBuilder 实例（永远不会为 {@code null}）
	 */
	public static UriComponentsBuilder fromController(@Nullable UriComponentsBuilder builder,
													  Class<?> controllerType) {

		builder = getBaseUrlToUse(builder);

		// 通过 PathConfigurer 外部配置的前缀。
		// 从外部配置获取路径前缀
		String prefix = getPathPrefix(controllerType);
		builder.path(prefix);

		// 获取类映射路径并附加到 builder
		String mapping = getClassMapping(controllerType);
		builder.path(mapping);

		return builder;
	}

	/**
	 * 从控制器方法的映射和方法参数值数组创建一个 {@link UriComponentsBuilder}。此方法委托给 {@link #fromMethod(Class, Method, Object...)}。
	 * <p><strong>注意：</strong> 如果找到 "Forwarded" 和 "X-Forwarded-*" 头，则此方法会从中提取值。请参阅类级别文档。
	 *
	 * @param controllerType 控制器类型
	 * @param methodName     方法名
	 * @param args           参数值
	 * @return 一个 UriComponentsBuilder 实例，永远不会为 {@code null}
	 * @throws IllegalArgumentException 如果没有匹配项或如果有多个匹配方法
	 */
	public static UriComponentsBuilder fromMethodName(Class<?> controllerType,
													  String methodName, Object... args) {
		// 获取方法
		Method method = getMethod(controllerType, methodName, args);
		// 从方法内部获取URL构建器
		return fromMethodInternal(null, controllerType, method, args);
	}

	/**
	 * {@link #fromMethodName(Class, String, Object...)} 的替代方法，接受表示基本 URL 的 {@code UriComponentsBuilder}。
	 * 当在处理请求的上下文之外使用 MvcUriComponentsBuilder 或应用不匹配当前请求的自定义 baseUrl 时，此方法非常有用。
	 * <p><strong>注意：</strong> 如果找到 "Forwarded" 和 "X-Forwarded-*" 头，则此方法会从中提取值。请参阅类级别文档。
	 *
	 * @param builder        基本 URL 的构建器；构建器将被克隆，因此不会被修改，并且可以在后续调用中重用。
	 * @param controllerType 控制器类型
	 * @param methodName     方法名
	 * @param args           参数值
	 * @return 一个 UriComponentsBuilder 实例，永远不会为 {@code null}
	 * @throws IllegalArgumentException 如果没有匹配项或如果有多个匹配方法
	 */
	public static UriComponentsBuilder fromMethodName(UriComponentsBuilder builder,
													  Class<?> controllerType, String methodName, Object... args) {

		Method method = getMethod(controllerType, methodName, args);
		return fromMethodInternal(builder, controllerType, method, args);
	}

	/**
	 * 从控制器方法的映射和方法参数值数组创建一个 {@link UriComponentsBuilder}。
	 * 参数值数组必须与控制器方法的签名匹配。
	 * {@code @RequestParam} 和 {@code @PathVariable} 的值用于构建 URI（通过 {@link org.springframework.web.method.support.UriComponentsContributor} 的实现），
	 * 而剩余的参数值将被忽略，可以是 {@code null}。
	 * <p><strong>注意：</strong> 如果找到 "Forwarded" 和 "X-Forwarded-*" 头，则此方法会从中提取值。请参阅类级别文档。
	 *
	 * @param controllerType 控制器类型
	 * @param method         控制器方法
	 * @param args           控制器方法的参数值
	 * @return 一个 UriComponentsBuilder 实例，永远不会为 {@code null}
	 * @since 4.2
	 */
	public static UriComponentsBuilder fromMethod(Class<?> controllerType, Method method, Object... args) {
		return fromMethodInternal(null, controllerType, method, args);
	}

	/**
	 * {@link #fromMethod(Class, Method, Object...)} 的替代方法，接受表示基本 URL 的 {@code UriComponentsBuilder}。
	 * 当在处理请求的上下文之外使用 MvcUriComponentsBuilder 或应用不匹配当前请求的自定义 baseUrl 时，此方法非常有用。
	 * <p><strong>注意：</strong> 如果找到 "Forwarded" 和 "X-Forwarded-*" 头，则此方法会从中提取值。请参阅类级别文档。
	 *
	 * @param baseUrl        基本 URL 的构建器；构建器将被克隆，因此不会被修改，并且可以在后续调用中重用。
	 * @param controllerType 控制器类型
	 * @param method         控制器方法
	 * @param args           控制器方法的参数值
	 * @return 一个 UriComponentsBuilder 实例（永远不会为 {@code null}）
	 * @since 4.2
	 */
	public static UriComponentsBuilder fromMethod(UriComponentsBuilder baseUrl,
												  @Nullable Class<?> controllerType, Method method, Object... args) {

		return fromMethodInternal(baseUrl,
				(controllerType != null ? controllerType : method.getDeclaringClass()), method, args);
	}

	/**
	 * 通过调用 "模拟" 控制器方法创建一个 {@link UriComponentsBuilder}。
	 * 然后使用控制器方法和提供的参数值来委托给 {@link #fromMethod(Class, Method, Object...)}。
	 * <p>例如，给定此控制器：
	 * <pre class="code">
	 * &#064;RequestMapping("/people/{id}/addresses")
	 * class AddressController {
	 *
	 *   &#064;RequestMapping("/{country}")
	 *   public HttpEntity&lt;Void&gt; getAddressesForCountry(&#064;PathVariable String country) { ... }
	 *
	 *   &#064;RequestMapping(value="/", method=RequestMethod.POST)
	 *   public void addAddress(Address address) { ... }
	 * }
	 * </pre>
	 * 可以创建一个 UriComponentsBuilder：
	 * <pre class="code">
	 * // 内联样式与静态导入 "MvcUriComponentsBuilder.on"
	 *
	 * MvcUriComponentsBuilder.fromMethodCall(
	 *     on(AddressController.class).getAddressesForCountry("US")).buildAndExpand(1);
	 *
	 * // 较长的形式对于重复调用（和 void 控制器方法）很有用
	 *
	 * AddressController controller = MvcUriComponentsBuilder.on(AddressController.class);
	 * controller.addAddress(null);
	 * builder = MvcUriComponentsBuilder.fromMethodCall(controller);
	 * controller.getAddressesForCountry("US")
	 * builder = MvcUriComponentsBuilder.fromMethodCall(controller);
	 * </pre>
	 * <p><strong>注意：</strong> 如果找到 "Forwarded" 和 "X-Forwarded-*" 头，则此方法会从中提取值。请参阅类级别文档。
	 *
	 * @param info 一个从 "模拟" 控制器调用返回的值，或者是在调用后的 "模拟" 控制器本身
	 * @return 一个 UriComponents 实例
	 * @see #on(Class)
	 * @see #controller(Class)
	 */
	public static UriComponentsBuilder fromMethodCall(Object info) {
		// 断言 info 是 MethodInvocationInfo 类型，否则抛出异常 "MethodInvocationInfo required"
		Assert.isInstanceOf(MethodInvocationInfo.class, info, "MethodInvocationInfo required");

		// 将 info 转换为 MethodInvocationInfo 类型
		MethodInvocationInfo invocationInfo = (MethodInvocationInfo) info;

		// 获取控制器类型
		Class<?> controllerType = invocationInfo.getControllerType();

		// 获取控制器方法
		Method method = invocationInfo.getControllerMethod();

		// 获取方法参数
		Object[] arguments = invocationInfo.getArgumentValues();

		// 调用 fromMethodInternal 方法
		return fromMethodInternal(null, controllerType, method, arguments);
	}

	/**
	 * {@link #fromMethodCall(Object)} 的替代方法，接受表示基本 URL 的 {@code UriComponentsBuilder}。
	 * 当在处理请求的上下文之外使用 MvcUriComponentsBuilder 或应用不匹配当前请求的自定义 baseUrl 时，此方法非常有用。
	 * <p><strong>注意：</strong> 如果找到 "Forwarded" 和 "X-Forwarded-*" 头，则此方法会从中提取值。请参阅类级别文档。
	 *
	 * @param builder 基本 URL 的构建器；构建器将被克隆，因此不会被修改，并且可以在后续调用中重用。
	 * @param info    要么是从 "模拟" 控制器调用返回的值，要么是 "模拟" 控制器本身在调用后的值
	 * @return UriComponents 实例
	 */
	public static UriComponentsBuilder fromMethodCall(UriComponentsBuilder builder, Object info) {
		// 断言 info 是 MethodInvocationInfo 类型，否则抛出异常 "MethodInvocationInfo required"
		Assert.isInstanceOf(MethodInvocationInfo.class, info, "MethodInvocationInfo required");

		// 将 info 转换为 MethodInvocationInfo 类型
		MethodInvocationInfo invocationInfo = (MethodInvocationInfo) info;

		// 获取控制器类型
		Class<?> controllerType = invocationInfo.getControllerType();

		// 获取控制器方法
		Method method = invocationInfo.getControllerMethod();

		// 获取方法参数
		Object[] arguments = invocationInfo.getArgumentValues();

		// 调用 fromMethodInternal 方法
		return fromMethodInternal(builder, controllerType, method, arguments);
	}

	/**
	 * 返回一个 "模拟" 控制器实例。当控制器上的 {@code @RequestMapping} 方法被调用时，会记住提供的参数值，然后可以使用结果通过 {@link #fromMethodCall(Object)} 创建 {@code UriComponentsBuilder}。
	 * <p>请注意，这是 {@link #controller(Class)} 的缩写版本，用于内联使用（使用静态导入），例如：
	 * <pre class="code">
	 * MvcUriComponentsBuilder.fromMethodCall(on(FooController.class).getFoo(1)).build();
	 * </pre>
	 * <p><strong>注意：</strong> 如果找到 "Forwarded" 和 "X-Forwarded-*" 头，则此方法会从中提取值。请参阅类级别文档。
	 *
	 * @param controllerType 目标控制器
	 */
	public static <T> T on(Class<T> controllerType) {
		return controller(controllerType);
	}

	/**
	 * 返回一个 "模拟" 控制器实例。当控制器上的 {@code @RequestMapping} 方法被调用时，会记住提供的参数值，然后可以使用结果通过 {@link #fromMethodCall(Object)} 创建 {@code UriComponentsBuilder}。
	 * <p>这是 {@link #on(Class)} 的长版本。当控制器方法返回 void 时，对于重复调用也是必需的。
	 * <pre class="code">
	 * FooController fooController = controller(FooController.class);
	 *
	 * fooController.saveFoo(1, null);
	 * builder = MvcUriComponentsBuilder.fromMethodCall(fooController);
	 *
	 * fooController.saveFoo(2, null);
	 * builder = MvcUriComponentsBuilder.fromMethodCall(fooController);
	 * </pre>
	 * <p><strong>注意：</strong> 如果找到 "Forwarded" 和 "X-Forwarded-*" 头，则此方法会从中提取值。请参阅类级别文档。
	 *
	 * @param controllerType 目标控制器
	 */
	public static <T> T controller(Class<T> controllerType) {
		Assert.notNull(controllerType, "'controllerType' must not be null");
		return ControllerMethodInvocationInterceptor.initProxy(controllerType, null);
	}

	/**
	 * 根据 Spring MVC 控制器方法的请求映射名称创建 URL。
	 * <p>配置的 {@link org.springframework.web.servlet.handler.HandlerMethodMappingNamingStrategy HandlerMethodMappingNamingStrategy} 在启动时确定控制器方法请求映射的名称。
	 * 默认情况下，所有映射都基于类名的大写字母作为前缀，然后是 "#" 作为分隔符，然后是方法名。例如，对于名为 PersonController，方法名为 getPerson 的类，映射的名称为 "PC#getPerson"。
	 * 如果命名约定未产生唯一结果，则可以通过 {@code @RequestMapping} 注解的 name 属性分配显式名称。
	 * <p>主要用于视图渲染技术和 EL 表达式。Spring URL 标签库将此方法注册为名为 "mvcUrl" 的函数。
	 * <p>例如，给定以下控制器：
	 * <pre class="code">
	 * &#064;RequestMapping("/people")
	 * class PersonController {
	 *
	 *   &#064;RequestMapping("/{id}")
	 *   public HttpEntity&lt;Void&gt; getPerson(&#064;PathVariable String id) { ... }
	 *
	 * }
	 * </pre>
	 * <p>一个 JSP 可以如下准备控制器方法的 URL：
	 *
	 * <pre class="code">
	 * &lt;%@ taglib uri="http://www.springframework.org/tags" prefix="s" %&gt;
	 *
	 * &lt;a href="${s:mvcUrl('PC#getPerson').arg(0,"123").build()}"&gt;Get Person&lt;/a&gt;
	 * </pre>
	 * <p>注意，不需要指定所有参数。只需要准备 URL 所需的参数即可，主要是 {@code @RequestParam} 和 {@code @PathVariable})。
	 *
	 * <p><strong>注意：</strong> 如果找到 "Forwarded" 和 "X-Forwarded-*" 头，则此方法会从中提取值。请参阅类级别文档。
	 *
	 * @param mappingName 映射名称
	 * @return 准备 URI 字符串的构建器
	 * @throws IllegalArgumentException 如果未找到映射名称或没有唯一匹配项
	 * @since 4.1
	 */
	public static MethodArgumentBuilder fromMappingName(String mappingName) {
		return fromMappingName(null, mappingName);
	}

	/**
	 * {@link #fromMappingName(String)} 的替代方法，接受表示基本 URL 的 {@code UriComponentsBuilder}。
	 * 当在处理请求的上下文之外使用 MvcUriComponentsBuilder 或应用不匹配当前请求的自定义 baseUrl 时，此方法非常有用。
	 * <p><strong>注意：</strong> 如果找到 "Forwarded" 和 "X-Forwarded-*" 头，则此方法会从中提取值。请参阅类级别文档。
	 *
	 * @param builder 基本 URL 的构建器；构建器将被克隆，因此不会被修改，并且可以在后续调用中重用。
	 * @param name    映射名称
	 * @return 准备 URI 字符串的构建器
	 * @throws IllegalArgumentException 如果未找到映射名称或没有唯一匹配项
	 * @since 4.2
	 */
	public static MethodArgumentBuilder fromMappingName(@Nullable UriComponentsBuilder builder, String name) {
		// 获取 Web应用上下文 实例
		WebApplicationContext wac = getWebApplicationContext();

		// 断言 Web应用上下文 不为空，否则抛出异常
		Assert.notNull(wac, "No WebApplicationContext.");

		// 获取类型为 请求映射信息处理器映射 的所有 bean
		Map<String, RequestMappingInfoHandlerMapping> map = wac.getBeansOfType(RequestMappingInfoHandlerMapping.class);

		// 声明一个存储 HandlerMethod 的列表
		List<HandlerMethod> handlerMethods = null;

		// 遍历 请求映射信息处理器映射 中的每个 RequestMappingInfoHandlerMapping 实例
		for (RequestMappingInfoHandlerMapping mapping : map.values()) {
			// 获取指定映射名称的 处理器方法
			handlerMethods = mapping.getHandlerMethodsForMappingName(name);
			// 如果找到了匹配的 处理器方法 则跳出循环
			if (handlerMethods != null) {
				break;
			}
		}

		// 如果没有找到匹配的 处理器方法，抛出异常 "Mapping not found: " + name
		if (handlerMethods == null) {
			throw new IllegalArgumentException("Mapping not found: " + name);
		} else if (handlerMethods.size() != 1) {
			// 如果找到的匹配 处理器方法 数量不唯一，抛出异常
			throw new IllegalArgumentException("No unique match for mapping " + name + ": " + handlerMethods);
		} else {
			// 如果找到了唯一匹配的 处理器方法
			// 获取第一个（也是唯一一个） 处理器方法
			HandlerMethod handlerMethod = handlerMethods.get(0);

			// 获取控制器类型
			Class<?> controllerType = handlerMethod.getBeanType();

			// 获取控制器方法
			Method method = handlerMethod.getMethod();

			// 返回一个新的 MethodArgumentBuilder 实例
			return new MethodArgumentBuilder(builder, controllerType, method);
		}
	}


	// 相对于基本的 UriComponentsBuilder 的实例方法...

	/**
	 * 用于与通过 {@link #relativeTo} 调用创建的此类实例一起使用的 {@link #fromController(Class)} 的替代方法。
	 * <p><strong>注意：</strong> 如果找到 "Forwarded" 和 "X-Forwarded-*" 头，则此方法会从中提取值。请参阅类级别文档。
	 *
	 * @since 4.2
	 */
	public UriComponentsBuilder withController(Class<?> controllerType) {
		return fromController(this.baseUrl, controllerType);
	}

	/**
	 * 用于与通过 {@link #relativeTo} 调用创建的此类实例一起使用的 {@link #fromMethodName(Class, String, Object...)} 的替代方法。
	 * <p><strong>注意：</strong> 如果找到 "Forwarded" 和 "X-Forwarded-*" 头，则此方法会从中提取值。请参阅类级别文档。
	 *
	 * @since 4.2
	 */
	public UriComponentsBuilder withMethodName(Class<?> controllerType, String methodName, Object... args) {
		return fromMethodName(this.baseUrl, controllerType, methodName, args);
	}

	/**
	 * 用于与通过 {@link #relativeTo} 调用创建的此类实例一起使用的 {@link #fromMethodCall(Object)} 的替代方法。
	 * <p><strong>注意：</strong> 如果找到 "Forwarded" 和 "X-Forwarded-*" 头，则此方法会从中提取值。请参阅类级别文档。
	 *
	 * @since 4.2
	 */
	public UriComponentsBuilder withMethodCall(Object invocationInfo) {
		return fromMethodCall(this.baseUrl, invocationInfo);
	}

	/**
	 * 用于与通过 {@link #relativeTo} 调用创建的此类实例一起使用的 {@link #fromMappingName(String)} 的替代方法。
	 * <p><strong>注意：</strong> 如果找到 "Forwarded" 和 "X-Forwarded-*" 头，则此方法会从中提取值。请参阅类级别文档。
	 *
	 * @since 4.2
	 */
	public MethodArgumentBuilder withMappingName(String mappingName) {
		return fromMappingName(this.baseUrl, mappingName);
	}

	/**
	 * 用于与通过 {@link #relativeTo} 调用创建的此类实例一起使用的 {@link #fromMethod(Class, Method, Object...)} 的替代方法。
	 * <p><strong>注意：</strong> 如果找到 "Forwarded" 和 "X-Forwarded-*" 头，则此方法会从中提取值。请参阅类级别文档。
	 *
	 * @since 4.2
	 */
	public UriComponentsBuilder withMethod(Class<?> controllerType, Method method, Object... args) {
		return fromMethod(this.baseUrl, controllerType, method, args);
	}


	private static UriComponentsBuilder fromMethodInternal(@Nullable UriComponentsBuilder builder,
														   Class<?> controllerType, Method method, Object... args) {

		// 获取基础 URL
		builder = getBaseUrlToUse(builder);

		// 通过 PathConfigurer 外部配置的前缀
		String prefix = getPathPrefix(controllerType);

		// 将前缀添加到 基础 URL 中
		builder.path(prefix);

		// 获取控制器类型的映射路径
		String typePath = getClassMapping(controllerType);

		// 获取方法的映射路径
		String methodPath = getMethodMapping(method);

		// 合并类型路径和方法路径
		String path = pathMatcher.combine(typePath, methodPath);

		// 如果路径不为空且不以 "/" 开头，则在前面加上 "/"
		if (StringUtils.hasLength(path) && !path.startsWith("/")) {
			path = "/" + path;
		}

		// 将合并后的路径添加到 基础 URL 的路径中
		builder.path(path);

		return applyContributors(builder, method, args);
	}

	private static UriComponentsBuilder getBaseUrlToUse(@Nullable UriComponentsBuilder baseUrl) {
		// 如果 基础URL 为 null
		return baseUrl == null ?
				// 使用当前 Servlet 映射构建 URI 组件
				ServletUriComponentsBuilder.fromCurrentServletMapping() :
				// 否则，克隆 基础URL 并返回
				baseUrl.cloneBuilder();
	}

	private static String getPathPrefix(Class<?> controllerType) {
		// 获取Web应用上下文
		WebApplicationContext wac = getWebApplicationContext();
		if (wac != null) {
			// 获取所有类型为 RequestMappingHandlerMapping 的 Bean
			Map<String, RequestMappingHandlerMapping> map = wac.getBeansOfType(RequestMappingHandlerMapping.class);
			for (RequestMappingHandlerMapping mapping : map.values()) {
				// 检查当前 mapping 是否为 控制类型处理程序
				if (mapping.isHandler(controllerType)) {
					// 获取 控制类型 的路径前缀
					String prefix = mapping.getPathPrefix(controllerType);
					if (prefix != null) {
						// 返回路径前缀
						return prefix;
					}
				}
			}
		}
		// 如果未找到路径前缀，返回空字符串
		return "";
	}

	private static String getClassMapping(Class<?> controllerType) {
		// 断言 控制类型 不为空
		Assert.notNull(controllerType, "'controllerType' must not be null");
		// 查找 控制类型 上的 RequestMapping 注解
		RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(controllerType, RequestMapping.class);
		// 如果找不到 RequestMapping 注解，则返回根路径
		if (mapping == null) {
			return "/";
		}
		// 获取 RequestMapping 注解中的路径
		String[] paths = mapping.path();
		// 如果路径为空或第一个路径为空，则返回根路径
		if (ObjectUtils.isEmpty(paths) || !StringUtils.hasLength(paths[0])) {
			return "/";
		}
		// 如果有多个路径，并且日志跟踪已启用，则记录日志
		if (paths.length > 1 && logger.isTraceEnabled()) {
			logger.trace("Using first of multiple paths on " + controllerType.getName());
		}
		// 返回第一个路径
		return paths[0];
	}

	private static String getMethodMapping(Method method) {
		Assert.notNull(method, "'method' must not be null");
		// 查找 方法 上的 RequestMapping 注解
		RequestMapping requestMapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
		// 如果找不到 RequestMapping 注解，则抛出异常
		if (requestMapping == null) {
			throw new IllegalArgumentException("No @RequestMapping on: " + method.toGenericString());
		}
		// 获取 RequestMapping 注解中的路径
		String[] paths = requestMapping.path();
		// 如果路径为空或第一个路径为空，则返回根路径
		if (ObjectUtils.isEmpty(paths) || !StringUtils.hasLength(paths[0])) {
			return "/";
		}
		// 如果有多个路径，并且日志跟踪已启用，则记录日志
		if (paths.length > 1 && logger.isTraceEnabled()) {
			logger.trace("Using first of multiple paths on " + method.toGenericString());
		}
		// 返回第一个路径
		return paths[0];
	}

	private static Method getMethod(Class<?> controllerType, final String methodName, final Object... args) {
		// 定义方法过滤器
		MethodFilter selector = method -> {
			// 获取方法名
			String name = method.getName();
			// 获取参数长度
			int argLength = method.getParameterCount();
			// 检查方法名和参数长度是否与给定的 方法名称 和 参数长度 相匹配
			return (name.equals(methodName) && argLength == args.length);
		};
		// 使用方法选择器从 控制类型 中选择方法
		Set<Method> methods = MethodIntrospector.selectMethods(controllerType, selector);
		// 如果找到唯一的方法，则返回该方法
		if (methods.size() == 1) {
			return methods.iterator().next();
		} else if (methods.size() > 1) {
			// 如果找到多个方法，则抛出异常
			throw new IllegalArgumentException(String.format(
					"Found two methods named '%s' accepting arguments %s in controller %s: [%s]",
					methodName, Arrays.asList(args), controllerType.getName(), methods));
		} else {
			// 如果未找到方法，则抛出异常
			throw new IllegalArgumentException("No method named '" + methodName + "' with " + args.length +
					" arguments found in controller " + controllerType.getName());
		}
	}

	private static UriComponentsBuilder applyContributors(UriComponentsBuilder builder, Method method, Object... args) {
		// 获取 CompositeUriComponentsContributor 实例
		CompositeUriComponentsContributor contributor = getUriComponentsContributor();

		// 获取方法参数数量和参数值数量
		int paramCount = method.getParameterCount();
		int argCount = args.length;
		// 检查方法参数数量是否与参数值数量相匹配
		if (paramCount != argCount) {
			throw new IllegalArgumentException("Number of method parameters " + paramCount +
					" does not match number of argument values " + argCount);
		}

		// 创建存储 URI 变量的 Map
		final Map<String, Object> uriVars = new HashMap<>();
		for (int i = 0; i < paramCount; i++) {
			// 创建方法参数对象
			MethodParameter param = new SynthesizingMethodParameter(method, i);
			// 初始化参数名称发现器
			param.initParameterNameDiscovery(parameterNameDiscoverer);
			// 构建方法参数
			contributor.contributeMethodArgument(param, args[i], builder, uriVars);
		}

		// 这可能不是所有的 URI 变量，提供我们到目前为止的变量..
		return builder.uriVariables(uriVars);
	}

	private static CompositeUriComponentsContributor getUriComponentsContributor() {
		// 获取 Web应用上下文
		WebApplicationContext wac = getWebApplicationContext();
		if (wac != null) {
			try {
				// 尝试从 Web应用上下文 中获取 MVC_URI_COMPONENTS_CONTRIBUTOR_BEAN_NAME 对应的 CompositeUriComponentsContributor 实例
				return wac.getBean(MVC_URI_COMPONENTS_CONTRIBUTOR_BEAN_NAME, CompositeUriComponentsContributor.class);
			} catch (NoSuchBeanDefinitionException ex) {
				// 如果找不到对应的 Bean，则忽略异常
			}
		}
		// 如果未能从 WebApplicationContext 中获取到指定的 Bean，则返回默认的 UriComponentsContributor
		return defaultUriComponentsContributor;
	}

	@Nullable
	private static WebApplicationContext getWebApplicationContext() {
		// 获取请求属性
		RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
		if (requestAttributes == null) {
			// 如果请求属性为空，则返回 null
			return null;
		}
		// 获取 HttpServletRequest 对象
		HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
		// 获取 Web应用上下文 属性名称
		String attributeName = DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE;
		// 从请求中获取 Web应用上下文
		WebApplicationContext wac = (WebApplicationContext) request.getAttribute(attributeName);
		if (wac == null) {
			// 如果 Web应用上下文 为空，则返回 null
			return null;
		}
		// 返回获取到的 Web应用上下文
		return wac;
	}


	/**
	 * 方法调用信息。
	 */
	public interface MethodInvocationInfo {

		/**
		 * 返回控制器类型。
		 */
		Class<?> getControllerType();

		/**
		 * 返回控制器方法。
		 */
		Method getControllerMethod();

		/**
		 * 返回参数值。
		 */
		Object[] getArgumentValues();
	}


	private static class ControllerMethodInvocationInterceptor
			implements org.springframework.cglib.proxy.MethodInterceptor, MethodInterceptor, MethodInvocationInfo {
		/**
		 * 控制类型
		 */
		private final Class<?> controllerType;

		/**
		 * 控制器方法
		 */
		@Nullable
		private Method controllerMethod;

		/**
		 * 参数值
		 */
		@Nullable
		private Object[] argumentValues;

		ControllerMethodInvocationInterceptor(Class<?> controllerType) {
			this.controllerType = controllerType;
		}

		@Override
		@Nullable
		public Object intercept(@Nullable Object obj, Method method, Object[] args, @Nullable MethodProxy proxy) {
			switch (method.getName()) {
				case "getControllerType":
					return this.controllerType;
				case "getControllerMethod":
					return this.controllerMethod;
				case "getArgumentValues":
					return this.argumentValues;
			}
			if (ReflectionUtils.isObjectMethod(method)) {
				// 如果是 Object 类中的方法，则通过反射调用该方法
				return ReflectionUtils.invokeMethod(method, obj, args);
			} else {
				// 否则，将方法及其参数值设置到相应的字段中
				this.controllerMethod = method;
				this.argumentValues = args;
				Class<?> returnType = method.getReturnType();
				try {
					// 尝试初始化代理对象，并返回
					return (returnType == void.class ? null : returnType.cast(initProxy(returnType, this)));
				} catch (Throwable ex) {
					// 如果初始化代理失败，则抛出异常
					throw new IllegalStateException(
							"Failed to create proxy for controller method return type: " + method, ex);
				}
			}
		}

		@Override
		@Nullable
		public Object invoke(org.aopalliance.intercept.MethodInvocation inv) throws Throwable {
			return intercept(inv.getThis(), inv.getMethod(), inv.getArguments(), null);
		}

		@Override
		public Class<?> getControllerType() {
			return this.controllerType;
		}

		@Override
		public Method getControllerMethod() {
			Assert.state(this.controllerMethod != null, "Not initialized yet");
			return this.controllerMethod;
		}

		@Override
		public Object[] getArgumentValues() {
			Assert.state(this.argumentValues != null, "Not initialized yet");
			return this.argumentValues;
		}


		@SuppressWarnings("unchecked")
		private static <T> T initProxy(
				Class<?> controllerType, @Nullable ControllerMethodInvocationInterceptor interceptor) {

			// 如果拦截器不为空，则使用给定的拦截器；
			// 否则，创建一个新的 ControllerMethodInvocationInterceptor
			interceptor = interceptor != null ?
					interceptor : new ControllerMethodInvocationInterceptor(controllerType);

			// 如果 控制类型 是 Object 类，则直接返回拦截器
			if (controllerType == Object.class) {
				return (T) interceptor;
			} else if (controllerType.isInterface()) {
				// 如果 控制类型 是接口，则创建代理工厂并返回代理对象
				ProxyFactory factory = new ProxyFactory(EmptyTargetSource.INSTANCE);
				// 添加控制类型接口
				factory.addInterface(controllerType);
				// 添加 方法调用信息
				factory.addInterface(MethodInvocationInfo.class);
				// 添加拦截器
				factory.addAdvice(interceptor);
				// 获取代理对象
				return (T) factory.getProxy();
			} else {
				// 否则，使用 Enhancer 创建 CGLIB 代理
				Enhancer enhancer = new Enhancer();
				// 设置父类为控制类型
				enhancer.setSuperclass(controllerType);
				// 设置 方法调用信息 接口
				enhancer.setInterfaces(new Class<?>[]{MethodInvocationInfo.class});
				// 设置命名策略
				enhancer.setNamingPolicy(SpringNamingPolicy.INSTANCE);
				// 设置方法回调类型
				enhancer.setCallbackType(org.springframework.cglib.proxy.MethodInterceptor.class);

				Class<?> proxyClass = enhancer.createClass();
				Object proxy = null;

				// 尝试使用 objenesis 创建代理对象
				if (objenesis.isWorthTrying()) {
					try {
						proxy = objenesis.newInstance(proxyClass, enhancer.getUseCache());
					} catch (ObjenesisException ex) {
						logger.debug("Failed to create controller proxy, falling back on default constructor", ex);
					}
				}

				// 如果无法使用 objenesis 创建代理对象，则使用默认构造函数创建
				if (proxy == null) {
					try {
						proxy = ReflectionUtils.accessibleConstructor(proxyClass).newInstance();
					} catch (Throwable ex) {
						throw new IllegalStateException(
								"Failed to create controller proxy or use default constructor", ex);
					}
				}

				// 设置代理对象的拦截器
				((Factory) proxy).setCallbacks(new Callback[]{interceptor});
				return (T) proxy;
			}
		}
	}


	/**
	 * 用于创建方法参数的 URL 的 Builder 类。
	 */
	public static class MethodArgumentBuilder {
		/**
		 * 参数类型
		 */
		private final Class<?> controllerType;

		/**
		 * 方法
		 */
		private final Method method;

		/**
		 * 参数值
		 */
		private final Object[] argumentValues;

		/**
		 * URL组成构建器
		 */
		private final UriComponentsBuilder baseUrl;

		/**
		 * 创建一个新的 {@link MethodArgumentBuilder} 实例。
		 *
		 * @since 4.2
		 */
		public MethodArgumentBuilder(Class<?> controllerType, Method method) {
			this(null, controllerType, method);
		}

		/**
		 * 创建一个新的 {@link MethodArgumentBuilder} 实例。
		 *
		 * @since 4.2
		 */
		public MethodArgumentBuilder(@Nullable UriComponentsBuilder baseUrl, Class<?> controllerType, Method method) {
			Assert.notNull(controllerType, "'controllerType' is required");
			Assert.notNull(method, "'method' is required");
			this.baseUrl = baseUrl != null ? baseUrl : UriComponentsBuilder.fromPath(getPath());
			this.controllerType = controllerType;
			this.method = method;
			this.argumentValues = new Object[method.getParameterCount()];
		}

		private static String getPath() {
			UriComponentsBuilder builder = ServletUriComponentsBuilder.fromCurrentServletMapping();
			String path = builder.build().getPath();
			return path != null ? path : "";
		}

		public MethodArgumentBuilder arg(int index, Object value) {
			this.argumentValues[index] = value;
			return this;
		}

		/**
		 * 仅在需要对扩展的 URI 变量应用强编码以用引号引用所有具有保留意义的字符时使用此方法。
		 *
		 * @since 5.0.8
		 */
		public MethodArgumentBuilder encode() {
			this.baseUrl.encode();
			return this;
		}

		public String build() {
			return fromMethodInternal(this.baseUrl, this.controllerType, this.method, this.argumentValues)
					.build().encode().toUriString();
		}

		public String buildAndExpand(Object... uriVars) {
			return fromMethodInternal(this.baseUrl, this.controllerType, this.method, this.argumentValues)
					.buildAndExpand(uriVars).encode().toString();
		}
	}

}
