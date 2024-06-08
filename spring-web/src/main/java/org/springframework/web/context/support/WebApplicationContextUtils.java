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

package org.springframework.web.context.support;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource.StubPropertySource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.*;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpSession;
import java.io.Serializable;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * 提供方便的方法来检索给定 ServletContext 的根 WebApplicationContext。
 * 这对于在自定义 Web 视图或 MVC 动作中编程访问 Spring 应用程序上下文很有用。
 *
 * <p>请注意，对于许多 Web 框架，有更方便的方式来访问根上下文，这些方式要么是 Spring 的一部分，要么作为外部库提供。
 * 这个辅助类只是访问根上下文的最通用方式。
 *
 * @author Juergen Hoeller
 * @see org.springframework.web.context.ContextLoader
 * @see org.springframework.web.servlet.FrameworkServlet
 * @see org.springframework.web.servlet.DispatcherServlet
 * @see org.springframework.web.jsf.FacesContextUtils
 * @see org.springframework.web.jsf.el.SpringBeanFacesELResolver
 */
public abstract class WebApplicationContextUtils {
	/**
	 * 是否存在FacesContext类
	 */
	private static final boolean jsfPresent =
			ClassUtils.isPresent("javax.faces.context.FacesContext", RequestContextHolder.class.getClassLoader());


	/**
	 * 查找此 Web 应用程序的根 WebApplicationContext，通常通过 org.springframework.web.context.ContextLoaderListener 加载。
	 * <p>如果在根上下文启动时发生异常，将重新抛出异常，以区分上下文启动失败和没有上下文。
	 *
	 * @param sc 要查找 Web 应用程序上下文的 ServletContext
	 * @return 此 Web 应用程序的根 WebApplicationContext
	 * @throws IllegalStateException 如果找不到根 WebApplicationContext
	 * @see org.springframework.web.context.WebApplicationContext#ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE
	 */
	public static WebApplicationContext getRequiredWebApplicationContext(ServletContext sc) throws IllegalStateException {
		// 获取 Web 应用程序上下文
		WebApplicationContext wac = getWebApplicationContext(sc);

		// 如果 Web 应用程序上下文为 null
		if (wac == null) {
			// 抛出 IllegalStateException 异常
			throw new IllegalStateException("No WebApplicationContext found: no ContextLoaderListener registered?");
		}

		// 返回 Web 应用程序上下文
		return wac;
	}

	/**
	 * 查找此 Web 应用程序的根 WebApplicationContext，通常通过 org.springframework.web.context.ContextLoaderListener 加载。
	 * <p>如果在根上下文启动时发生异常，将重新抛出异常，以区分上下文启动失败和没有上下文。
	 *
	 * @param sc 要查找 Web 应用程序上下文的 ServletContext
	 * @return 此 Web 应用程序的根 WebApplicationContext，如果没有则返回 {@code null}
	 * @see org.springframework.web.context.WebApplicationContext#ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE
	 */
	@Nullable
	public static WebApplicationContext getWebApplicationContext(ServletContext sc) {
		return getWebApplicationContext(sc, WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
	}

	/**
	 * 查找此 Web 应用程序的自定义 WebApplicationContext。
	 *
	 * @param sc       要查找 Web 应用程序上下文的 ServletContext
	 * @param attrName 要查找的 ServletContext 属性的名称
	 * @return 此 Web 应用程序的所需 WebApplicationContext，如果没有则返回 {@code null}
	 */
	@Nullable
	public static WebApplicationContext getWebApplicationContext(ServletContext sc, String attrName) {
		Assert.notNull(sc, "ServletContext must not be null");
		// 获取 Servlet上下文 中指定名称的属性
		Object attr = sc.getAttribute(attrName);
		// 如果属性为空，则返回空
		if (attr == null) {
			return null;
		}
		// 如果属性是RuntimeException类型，则抛出对应的异常
		if (attr instanceof RuntimeException) {
			throw (RuntimeException) attr;
		}
		// 如果属性是Error类型，则抛出对应的异常
		if (attr instanceof Error) {
			throw (Error) attr;
		}
		// 如果属性是Exception类型，则抛出IllegalStateException异常并包装成Exception
		if (attr instanceof Exception) {
			throw new IllegalStateException((Exception) attr);
		}
		// 如果属性不是 Web应用程序上下文 类型，则抛出IllegalStateException异常
		if (!(attr instanceof WebApplicationContext)) {
			throw new IllegalStateException("Context attribute is not of type WebApplicationContext: " + attr);
		}
		// 将属性转换为WebApplicationContext类型并返回
		return (WebApplicationContext) attr;
	}

	/**
	 * 查找此 Web 应用程序的唯一 WebApplicationContext：根 Web 应用程序上下文（首选）或在注册的 ServletContext 属性中找到的唯一 WebApplicationContext
	 * （通常来自当前 Web 应用程序中的单个 DispatcherServlet）。
	 * <p>请注意，DispatcherServlet 的上下文公开可以通过其 publishContext 属性进行控制，默认情况下为 true，但可以选择将其切换为仅发布单个上下文，
	 * 即使在 Web 应用程序中注册了多个 DispatcherServlet。
	 *
	 * @param sc 要查找 Web 应用程序上下文的 ServletContext
	 * @return 此 Web 应用程序的所需 WebApplicationContext，如果没有则返回 {@code null}
	 * @see #getWebApplicationContext(ServletContext)
	 * @see ServletContext#getAttributeNames()
	 * @since 4.2
	 */
	@Nullable
	public static WebApplicationContext findWebApplicationContext(ServletContext sc) {
		// 从 Servlet上下文 中获取 Web应用程序上下文 对象
		WebApplicationContext wac = getWebApplicationContext(sc);
		// 如果获取到的 Web应用程序上下文 为空
		if (wac == null) {
			// 遍历 Servlet上下文 中的所有属性
			Enumeration<String> attrNames = sc.getAttributeNames();
			while (attrNames.hasMoreElements()) {
				String attrName = attrNames.nextElement();
				Object attrValue = sc.getAttribute(attrName);
				// 如果属性值是 Web应用程序上下文 类型
				if (attrValue instanceof WebApplicationContext) {
					if (wac != null) {
						// 如果wac已经被赋值过了，则抛出异常
						throw new IllegalStateException("No unique WebApplicationContext found: more than one " +
								"DispatcherServlet registered with publishContext=true?");
					}
					// 将属性值赋给wac
					wac = (WebApplicationContext) attrValue;
				}
			}
		}
		// 返回获取到的 Web应用程序上下文 对象
		return wac;
	}


	/**
	 * 使用给定的 BeanFactory 注册 Web 特定的范围 ("request", "session", "globalSession")，
	 * 这些范围由 WebApplicationContext 使用。
	 *
	 * @param beanFactory 要配置的 BeanFactory
	 */
	public static void registerWebApplicationScopes(ConfigurableListableBeanFactory beanFactory) {
		registerWebApplicationScopes(beanFactory, null);
	}

	/**
	 * 使用给定的 BeanFactory 注册 Web 特定的范围 ("request", "session", "globalSession", "application")，
	 * 这些范围由 WebApplicationContext 使用。
	 *
	 * @param beanFactory 要配置的 BeanFactory
	 * @param sc          我们正在其中运行的 ServletContext
	 */
	public static void registerWebApplicationScopes(ConfigurableListableBeanFactory beanFactory,
													@Nullable ServletContext sc) {

		// 注册请求作用域
		beanFactory.registerScope(WebApplicationContext.SCOPE_REQUEST, new RequestScope());
		// 注册会话作用域
		beanFactory.registerScope(WebApplicationContext.SCOPE_SESSION, new SessionScope());
		// 如果 Servlet 上下文不为 null
		if (sc != null) {
			// 创建 Servlet 上下文作用域
			ServletContextScope appScope = new ServletContextScope(sc);
			// 注册应用程序作用域
			beanFactory.registerScope(WebApplicationContext.SCOPE_APPLICATION, appScope);
			// 将 Servlet 上下文作用域注册为 Servlet 上下文属性，以便 上下文清理监听器 可以检测到它。
			sc.setAttribute(ServletContextScope.class.getName(), appScope);
		}

		// 注册可解析的依赖项：Servlet请求
		beanFactory.registerResolvableDependency(ServletRequest.class, new RequestObjectFactory());
		// 注册可解析的依赖项：Servlet响应
		beanFactory.registerResolvableDependency(ServletResponse.class, new ResponseObjectFactory());
		// 注册可解析的依赖项：Http会话
		beanFactory.registerResolvableDependency(HttpSession.class, new SessionObjectFactory());
		// 注册可解析的依赖项：Web请求
		beanFactory.registerResolvableDependency(WebRequest.class, new WebRequestObjectFactory());
		if (jsfPresent) {
			// 如果 JSF 存在，则注册 JSF 相关的依赖项
			FacesDependencyRegistrar.registerFacesDependencies(beanFactory);
		}
	}

	/**
	 * 使用给定的 BeanFactory 注册 Web 特定的环境 bean（"contextParameters", "contextAttributes"），
	 * 这些 bean 由 WebApplicationContext 使用。
	 *
	 * @param bf the BeanFactory to configure
	 * @param sc the ServletContext that we're running within
	 */
	public static void registerEnvironmentBeans(ConfigurableListableBeanFactory bf, @Nullable ServletContext sc) {
		registerEnvironmentBeans(bf, sc, null);
	}

	/**
	 * 使用给定的 BeanFactory 注册 Web 特定的环境 bean（"contextParameters", "contextAttributes"），
	 * 这些 bean 由 WebApplicationContext 使用。
	 *
	 * @param bf             the BeanFactory to configure
	 * @param servletContext the ServletContext that we're running within
	 * @param servletConfig  the ServletConfig
	 */
	public static void registerEnvironmentBeans(ConfigurableListableBeanFactory bf,
												@Nullable ServletContext servletContext, @Nullable ServletConfig servletConfig) {

		// 如果 Servlet 上下文不为 null，并且 Bean工厂 中不包含名称为 servletContext 的bean
		if (servletContext != null && !bf.containsBean(WebApplicationContext.SERVLET_CONTEXT_BEAN_NAME)) {
			// 将 Servlet 上下文注册为单例 Bean
			bf.registerSingleton(WebApplicationContext.SERVLET_CONTEXT_BEAN_NAME, servletContext);
		}

		// 如果 Servlet 配置不为 null，并且 Bean工厂 中不包含名称为 servletConfig 的bean
		if (servletConfig != null && !bf.containsBean(ConfigurableWebApplicationContext.SERVLET_CONFIG_BEAN_NAME)) {
			// 将 Servlet 配置注册为单例 Bean
			bf.registerSingleton(ConfigurableWebApplicationContext.SERVLET_CONFIG_BEAN_NAME, servletConfig);
		}

		// 如果 Bean工厂 中不包含名称为 contextParameters 的bean
		if (!bf.containsBean(WebApplicationContext.CONTEXT_PARAMETERS_BEAN_NAME)) {
			// 创建参数映射
			Map<String, String> parameterMap = new HashMap<>();
			// 将 Servlet 上下文的初始化参数添加到参数映射中
			if (servletContext != null) {
				// 获取 Servlet上下文 中所有初始化参数的枚举
				Enumeration<?> paramNameEnum = servletContext.getInitParameterNames();
				// 遍历枚举中的每个元素
				while (paramNameEnum.hasMoreElements()) {
					// 获取参数名
					String paramName = (String) paramNameEnum.nextElement();
					// 将参数名和对应的参数值存入 参数映射 中
					parameterMap.put(paramName, servletContext.getInitParameter(paramName));
				}
			}
			// 将 Servlet 配置的初始化参数添加到参数映射中
			if (servletConfig != null) {
				// 获取 Servlet配置 中所有初始化参数的枚举
				Enumeration<?> paramNameEnum = servletConfig.getInitParameterNames();
				// 遍历枚举中的每个元素
				while (paramNameEnum.hasMoreElements()) {
					// 获取参数名
					String paramName = (String) paramNameEnum.nextElement();
					// 将参数名和对应的参数值存入 参数映射 中
					parameterMap.put(paramName, servletConfig.getInitParameter(paramName));
				}
			}
			// 将参数映射注册为单例 Bean
			bf.registerSingleton(WebApplicationContext.CONTEXT_PARAMETERS_BEAN_NAME,
					Collections.unmodifiableMap(parameterMap));
		}

		// 如果 Bean工厂 中不包含 WebApplicationContext.CONTEXT_ATTRIBUTES_BEAN_NAME
		if (!bf.containsBean(WebApplicationContext.CONTEXT_ATTRIBUTES_BEAN_NAME)) {
			// 创建属性映射
			Map<String, Object> attributeMap = new HashMap<>();
			// 将 Servlet 上下文的属性添加到属性映射中
			if (servletContext != null) {
				// 获取 Servlet上下文 中所有属性名的枚举
				Enumeration<?> attrNameEnum = servletContext.getAttributeNames();
				// 遍历枚举中的每个元素
				while (attrNameEnum.hasMoreElements()) {
					// 获取属性名
					String attrName = (String) attrNameEnum.nextElement();
					// 将属性名和对应的属性值存入 属性映射 中
					attributeMap.put(attrName, servletContext.getAttribute(attrName));
				}
			}
			// 将属性映射注册为单例 Bean
			bf.registerSingleton(WebApplicationContext.CONTEXT_ATTRIBUTES_BEAN_NAME,
					Collections.unmodifiableMap(attributeMap));
		}
	}

	/**
	 * 便利的 {@link #initServletPropertySources(MutablePropertySources, ServletContext, ServletConfig)} 变体，
	 * 总是为 {@link ServletConfig} 参数提供 {@code null}。
	 *
	 * @see #initServletPropertySources(MutablePropertySources, ServletContext, ServletConfig)
	 */
	public static void initServletPropertySources(MutablePropertySources propertySources, ServletContext servletContext) {
		initServletPropertySources(propertySources, servletContext, null);
	}

	/**
	 * 用给定的 {@code servletContext} 和 {@code servletConfig} 对象替换基于 {@code Servlet} 的 {@link StubPropertySource stub property sources}。
	 * <p>此方法与事务的性质有关，因为它可以调用任意次数，但将一次且仅一次地将 stub property sources 替换为它们相应的实际 property sources。
	 *
	 * @param sources        要初始化的 {@link MutablePropertySources}（不得为 {@code null}）
	 * @param servletContext 当前的 {@link ServletContext}（如果为 {@code null} 或者 {@link StandardServletEnvironment#SERVLET_CONTEXT_PROPERTY_SOURCE_NAME servlet context property source} 已经初始化，则会被忽略）
	 * @param servletConfig  当前的 {@link ServletConfig}（如果为 {@code null} 或者 {@link StandardServletEnvironment#SERVLET_CONFIG_PROPERTY_SOURCE_NAME servlet config property source} 已经初始化，则会被忽略）
	 * @see org.springframework.core.env.PropertySource.StubPropertySource
	 * @see org.springframework.core.env.ConfigurableEnvironment#getPropertySources()
	 */
	public static void initServletPropertySources(MutablePropertySources sources,
												  @Nullable ServletContext servletContext, @Nullable ServletConfig servletConfig) {

		Assert.notNull(sources, "'propertySources' must not be null");
		// 设置 servlet 上下文属性源的名称
		String name = StandardServletEnvironment.SERVLET_CONTEXT_PROPERTY_SOURCE_NAME;

		// 如果 servlet 上下文不为空，并且 sources 中对应的属性源是 StubPropertySource 类型，则替换为 ServletContextPropertySource
		if (servletContext != null && sources.get(name) instanceof StubPropertySource) {
			sources.replace(name, new ServletContextPropertySource(name, servletContext));
		}

		// 设置 servlet 配置属性源的名称
		name = StandardServletEnvironment.SERVLET_CONFIG_PROPERTY_SOURCE_NAME;

		// 如果 servlet 配置不为空，并且 sources 中对应的属性源是 StubPropertySource 类型，则替换为 ServletConfigPropertySource
		if (servletConfig != null && sources.get(name) instanceof StubPropertySource) {
			sources.replace(name, new ServletConfigPropertySource(name, servletConfig));
		}
	}

	/**
	 * 将当前的 RequestAttributes 实例作为 ServletRequestAttributes 返回。
	 *
	 * @see RequestContextHolder#currentRequestAttributes()
	 */
	private static ServletRequestAttributes currentRequestAttributes() {
		// 获取当前请求的属性
		RequestAttributes requestAttr = RequestContextHolder.currentRequestAttributes();

		// 如果当前请求属性不是 Servlet请求属性 类型
		if (!(requestAttr instanceof ServletRequestAttributes)) {
			// 抛出 IllegalStateException 异常
			throw new IllegalStateException("Current request is not a servlet request");
		}

		// 将请求属性转换为 Servlet请求属性 类型并返回
		return (ServletRequestAttributes) requestAttr;
	}


	/**
	 * 按需公开当前请求对象的工厂。
	 */
	@SuppressWarnings("serial")
	private static class RequestObjectFactory implements ObjectFactory<ServletRequest>, Serializable {

		@Override
		public ServletRequest getObject() {
			return currentRequestAttributes().getRequest();
		}

		@Override
		public String toString() {
			return "Current HttpServletRequest";
		}
	}


	/**
	 * 按需公开当前响应对象的工厂。
	 */
	@SuppressWarnings("serial")
	private static class ResponseObjectFactory implements ObjectFactory<ServletResponse>, Serializable {

		@Override
		public ServletResponse getObject() {
			// 获取当前请求属性中的 Servlet响应 对象
			ServletResponse response = currentRequestAttributes().getResponse();
			// 如果获取到的 Servlet响应 对象为空
			if (response == null) {
				// 抛出异常
				throw new IllegalStateException("Current servlet response not available - " +
						"consider using RequestContextFilter instead of RequestContextListener");
			}
			// 返回获取到的 Servlet响应 对象
			return response;
		}

		@Override
		public String toString() {
			return "Current HttpServletResponse";
		}
	}


	/**
	 * 按需公开当前会话对象的工厂。
	 */
	@SuppressWarnings("serial")
	private static class SessionObjectFactory implements ObjectFactory<HttpSession>, Serializable {

		@Override
		public HttpSession getObject() {
			return currentRequestAttributes().getRequest().getSession();
		}

		@Override
		public String toString() {
			return "Current HttpSession";
		}
	}


	/**
	 * 按需公开当前WebRequest对象的工厂。
	 */
	@SuppressWarnings("serial")
	private static class WebRequestObjectFactory implements ObjectFactory<WebRequest>, Serializable {

		@Override
		public WebRequest getObject() {
			ServletRequestAttributes requestAttr = currentRequestAttributes();
			return new ServletWebRequest(requestAttr.getRequest(), requestAttr.getResponse());
		}

		@Override
		public String toString() {
			return "Current ServletWebRequest";
		}
	}


	/**
	 * 内部类，以避免硬编码的JSF依赖项。
	 */
	private static class FacesDependencyRegistrar {

		public static void registerFacesDependencies(ConfigurableListableBeanFactory beanFactory) {
			// 注册可解析的依赖项：Faces上下文
			beanFactory.registerResolvableDependency(FacesContext.class, new ObjectFactory<FacesContext>() {
				@Override
				public FacesContext getObject() {
					// 获取当前的 JSF Faces上下文
					return FacesContext.getCurrentInstance();
				}

				@Override
				public String toString() {
					return "Current JSF FacesContext";
				}
			});

			// 注册可解析的依赖项：外部环境
			beanFactory.registerResolvableDependency(ExternalContext.class, new ObjectFactory<ExternalContext>() {
				@Override
				public ExternalContext getObject() {
					// 获取当前的 JSF 外部环境
					return FacesContext.getCurrentInstance().getExternalContext();
				}

				@Override
				public String toString() {
					return "Current JSF ExternalContext";
				}
			});
		}
	}

}
