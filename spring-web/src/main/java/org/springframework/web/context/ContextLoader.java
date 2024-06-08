/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.context;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import javax.servlet.ServletContext;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 执行根应用程序上下文的实际初始化工作。由 {@link ContextLoaderListener} 调用。
 *
 * <p>在 {@code web.xml} 的 context-param 级别查找 {@link #CONTEXT_CLASS_PARAM "contextClass"} 参数
 * 以指定上下文类类型，如果未找到，则退回到 {@link org.springframework.web.context.support.XmlWebApplicationContext}。
 * 使用默认的 ContextLoader 实现，任何指定的上下文类都需要实现 {@link ConfigurableWebApplicationContext} 接口。
 *
 * <p>处理 {@link #CONFIG_LOCATION_PARAM "contextConfigLocation"} context-param 并将其值传递给上下文实例，
 * 将其解析为可能的多个文件路径，这些文件路径可以由任意数量的逗号和空格分隔，例如
 * "WEB-INF/applicationContext1.xml, WEB-INF/applicationContext2.xml"。
 * 还支持 Ant 样式路径模式，例如 "WEB-INF/*Context.xml,WEB-INF/spring*.xml" 或 "WEB-INF/&#42;&#42;/*Context.xml"。
 * 如果未明确指定，则上下文实现应该使用默认位置（对于 XmlWebApplicationContext 为 "/WEB-INF/applicationContext.xml"）。
 *
 * <p>注意：在多个配置位置的情况下，后面加载的文件中的 bean 定义将覆盖先前加载的文件中的 bean 定义，至少在使用
 * Spring 的默认 ApplicationContext 实现之一时是这样的。可以利用这一点通过额外的 XML 文件有意覆盖某些 bean 定义。
 *
 * <p>除了加载根应用程序上下文之外，此类还可以选择加载或获取并连接到根应用程序上下文的共享父上下文。有关更多信息，请参见
 * {@link #loadParentContext(ServletContext)} 方法。
 *
 * <p>从 Spring 3.1 开始，{@code ContextLoader} 支持通过 {@link #ContextLoader(WebApplicationContext)} 构造函数
 * 注入根 Web 应用程序上下文，从而允许在 Servlet 3.0+ 环境中进行编程配置。有关用法示例，请参见
 * {@link org.springframework.web.WebApplicationInitializer}。
 *
 * @author Juergen Hoeller
 * @author Colin Sampaleanu
 * @author Sam Brannen
 * @see ContextLoaderListener
 * @see ConfigurableWebApplicationContext
 * @see org.springframework.web.context.support.XmlWebApplicationContext
 * @since 17.02.2003
 */
public class ContextLoader {

	/**
	 * 用于根 Web应用程序上下文 的配置参数，用作底层 BeanFactory 的序列化 id：{@value}。
	 */
	public static final String CONTEXT_ID_PARAM = "contextId";

	/**
	 * Servlet 上下文参数的名称（即，{@value}），可指定根上下文的配置位置，否则将使用实现的默认值。
	 *
	 * @see org.springframework.web.context.support.XmlWebApplicationContext#DEFAULT_CONFIG_LOCATION
	 */
	public static final String CONFIG_LOCATION_PARAM = "contextConfigLocation";

	/**
	 * 要使用的根 Web应用程序上下文 实现类的配置参数：{@value}。
	 *
	 * @see #determineContextClass(ServletContext)
	 */
	public static final String CONTEXT_CLASS_PARAM = "contextClass";

	/**
	 * 用于初始化根 Web 应用程序上下文的 {@link ApplicationContextInitializer} 类的配置参数：{@value}。
	 *
	 * @see #customizeContext(ServletContext, ConfigurableWebApplicationContext)
	 */
	public static final String CONTEXT_INITIALIZER_CLASSES_PARAM = "contextInitializerClasses";

	/**
	 * 用于初始化当前应用程序中所有 Web 应用程序上下文的全局 {@link ApplicationContextInitializer} 类的配置参数：{@value}。
	 *
	 * @see #customizeContext(ServletContext, ConfigurableWebApplicationContext)
	 */
	public static final String GLOBAL_INITIALIZER_CLASSES_PARAM = "globalInitializerClasses";

	/**
	 * 这些字符的任意数量被认为是单个 init-param 字符串值中多个值之间的分隔符。
	 */
	private static final String INIT_PARAM_DELIMITERS = ",; \t\n";

	/**
	 * 定义 上下文加载器 的默认策略名称的类路径资源的名称（相对于 ContextLoader 类）。
	 */
	private static final String DEFAULT_STRATEGIES_PATH = "ContextLoader.properties";


	private static final Properties defaultStrategies;

	static {
		// 从属性文件中加载默认策略实现。
		// 目前，这是严格内部的，不应由应用程序开发人员自定义。
		try {
			// 加载默认策略文件
			ClassPathResource resource = new ClassPathResource(DEFAULT_STRATEGIES_PATH, ContextLoader.class);
			// 使用 PropertiesLoaderUtils 加载资源文件
			defaultStrategies = PropertiesLoaderUtils.loadProperties(resource);
		} catch (IOException ex) {
			// 如果发生 I/O 异常，抛出 IllegalStateException 异常
			throw new IllegalStateException("Could not load 'ContextLoader.properties': " + ex.getMessage());
		}
	}


	/**
	 * （线程上下文）ClassLoader 到相应的“当前”WebApplicationContext 的映射。
	 */
	private static final Map<ClassLoader, WebApplicationContext> currentContextPerThread =
			new ConcurrentHashMap<>(1);

	/**
	 * 如果 ContextLoader 类部署在 Web 应用程序 ClassLoader 本身中，则为“当前”WebApplicationContext。
	 */
	@Nullable
	private static volatile WebApplicationContext currentContext;


	/**
	 * 此加载程序管理的根 Web应用程序上下文 实例。
	 */
	@Nullable
	private WebApplicationContext context;

	/**
	 * 实际要应用于上下文的 ApplicationContextInitializer 实例。
	 */
	private final List<ApplicationContextInitializer<ConfigurableApplicationContext>> contextInitializers =
			new ArrayList<>();


	/**
	 * 创建一个新的ContextLoader，它将根据"contextClass"和"contextConfigLocation" servlet context参数创建一个基于Web的应用程序上下文。
	 * 有关每个参数的默认值的详细信息，请参见类级别的文档。
	 * 此构造函数通常在将ContextLoaderListener子类声明为web.xml中的<listener>时使用，因为需要一个无参数的构造函数。
	 * 创建的应用程序上下文将注册到ServletContext中，名称为WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE，
	 * 并且子类可以在容器关闭时调用closeWebApplicationContext方法来关闭应用程序上下文。
	 *
	 * @see #ContextLoader(WebApplicationContext)
	 * @see #initWebApplicationContext(ServletContext)
	 * @see #closeWebApplicationContext(ServletContext)
	 */
	public ContextLoader() {
	}

	/**
	 * 使用给定的应用程序上下文创建一个新的ContextLoader。此构造函数在Servlet 3.0+环境中非常有用，
	 * 其中通过ServletContext.addListener API可以实现基于实例的监听器的注册。
	 * 上下文可能已经刷新，也可能尚未刷新。如果它(a)是ConfigurableWebApplicationContext的实现，
	 * 并且(b)尚未刷新（推荐的方法），那么将会发生以下情况:
	 * 如果给定的上下文还没有分配id，则将为其分配一个id
	 * ServletContext和ServletConfig对象将被委托给应用程序上下文
	 * 将调用customizeContext方法
	 * 将应用通过"contextInitializerClasses" init-param 指定的任何ApplicationContextInitializer
	 * 将调用ConfigurableApplicationContext.refresh方法
	 * 如果上下文已经刷新或不实现ConfigurableWebApplicationContext，则假定用户根据自己的特定需求执行了这些操作（或未执行）。
	 * 有关用法示例，请参见org.springframework.web.WebApplicationInitializer。
	 * 无论如何，给定的应用程序上下文都将注册到ServletContext中，名称为WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE，
	 * 并且子类可以在容器关闭时调用closeWebApplicationContext方法来关闭应用程序上下文。
	 *
	 * @param context 要管理的应用程序上下文
	 * @see #initWebApplicationContext(ServletContext)
	 * @see #closeWebApplicationContext(ServletContext)
	 */
	public ContextLoader(WebApplicationContext context) {
		this.context = context;
	}


	/**
	 * 指定要用于初始化此ContextLoader使用的应用程序上下文的ApplicationContextInitializer实例。
	 *
	 * @see #configureAndRefreshWebApplicationContext
	 * @see #customizeContext
	 * @since 4.2
	 */
	@SuppressWarnings("unchecked")
	public void setContextInitializers(@Nullable ApplicationContextInitializer<?>... initializers) {
		// 如果初始化器列表不为空，则遍历初始化器列表
		if (initializers != null) {
			for (ApplicationContextInitializer<?> initializer : initializers) {
				// 将初始化器转换为 可配置的应用程序上下文 类型，并添加到上下文初始化器列表中
				this.contextInitializers.add((ApplicationContextInitializer<ConfigurableApplicationContext>) initializer);
			}
		}
	}


	/**
	 * 初始化给定servlet上下文的Spring的Web应用程序上下文，
	 * 使用在构造时提供的应用程序上下文，或者根据"contextClass"和"contextConfigLocation"上下文参数创建一个新的应用程序上下文。
	 *
	 * @param servletContext 当前servlet上下文
	 * @return 新的WebApplicationContext
	 * @see #ContextLoader(WebApplicationContext)
	 * @see #CONTEXT_CLASS_PARAM
	 * @see #CONFIG_LOCATION_PARAM
	 */
	public WebApplicationContext initWebApplicationContext(ServletContext servletContext) {
		if (servletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE) != null) {
			// 如果 Servlet上下文 中已经存在根Web应用程序上下文，则抛出异常
			throw new IllegalStateException(
					"Cannot initialize context because there is already a root application context present - " +
							"check whether you have multiple ContextLoader* definitions in your web.xml!");
		}

		// 记录初始化根Web应用程序上下文的日志信息
		servletContext.log("Initializing Spring root WebApplicationContext");
		Log logger = LogFactory.getLog(ContextLoader.class);
		if (logger.isInfoEnabled()) {
			logger.info("Root WebApplicationContext: initialization started");
		}
		long startTime = System.currentTimeMillis();

		try {
			// 将上下文存储在本地实例变量中，以保证它在 Servlet上下文 关闭时可用。
			if (this.context == null) {
				// 如果上下文为空，则创建Web应用程序上下文
				this.context = createWebApplicationContext(servletContext);
			}
			// 如果上下文是 可配置的Web应用程序上下文 类型
			if (this.context instanceof ConfigurableWebApplicationContext) {
				ConfigurableWebApplicationContext cwac = (ConfigurableWebApplicationContext) this.context;
				if (!cwac.isActive()) {
					// 上下文尚未刷新 -> 提供诸如设置父上下文、设置应用程序上下文ID等服务
					if (cwac.getParent() == null) {
						// 如果上下文实例被注入但没有显式的父上下文 -> 确定根Web应用程序上下文的父上下文（如果有）
						ApplicationContext parent = loadParentContext(servletContext);
						cwac.setParent(parent);
					}
					// 配置和刷新Web应用程序上下文
					configureAndRefreshWebApplicationContext(cwac, servletContext);
				}
			}
			// 将根Web应用程序上下文设置为 Servlet上下文 属性
			servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, this.context);

			// 设置当前线程的上下文加载器
			ClassLoader ccl = Thread.currentThread().getContextClassLoader();
			if (ccl == ContextLoader.class.getClassLoader()) {
				// 如果类加载器是当前的类加载器，则将当前应用上下文设置为 根Web应用程序上下文
				currentContext = this.context;
			} else if (ccl != null) {
				// 如果当前线程的类加载器存在，则将其和 根Web应用程序上下文 缓存起来
				currentContextPerThread.put(ccl, this.context);
			}

			// 记录根Web应用程序上下文初始化完成的日志信息
			if (logger.isInfoEnabled()) {
				long elapsedTime = System.currentTimeMillis() - startTime;
				logger.info("Root WebApplicationContext initialized in " + elapsedTime + " ms");
			}

			// 返回根Web应用程序上下文
			return this.context;
		} catch (RuntimeException | Error ex) {
			// 记录上下文初始化失败的日志信息，并将异常设置为ServletContext属性
			logger.error("Context initialization failed", ex);
			servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, ex);
			throw ex;
		}
	}

	/**
	 * 实例化此加载程序的根WebApplicationContext，可以是默认的上下文类，也可以是自定义上下文类（如果指定）。
	 * 此实现期望自定义上下文实现ConfigurableWebApplicationContext接口。
	 * 可以在子类中重写。
	 * 此外，会在刷新上下文之前调用customizeContext，允许子类对上下文进行自定义修改。
	 *
	 * @param sc 当前servlet上下文
	 * @return 根WebApplicationContext
	 * @see ConfigurableWebApplicationContext
	 */
	protected WebApplicationContext createWebApplicationContext(ServletContext sc) {
		// 确定要使用的上下文类
		Class<?> contextClass = determineContextClass(sc);
		// 如果上下文类不是 可配置的Web应用程序上下文 的子类，则抛出异常
		if (!ConfigurableWebApplicationContext.class.isAssignableFrom(contextClass)) {
			throw new ApplicationContextException("Custom context class [" + contextClass.getName() +
					"] is not of type [" + ConfigurableWebApplicationContext.class.getName() + "]");
		}
		// 实例化上下文类并返回
		return (ConfigurableWebApplicationContext) BeanUtils.instantiateClass(contextClass);
	}

	/**
	 * 返回要使用的WebApplicationContext实现类，可以是默认的XmlWebApplicationContext，也可以是自定义的上下文类（如果指定）。
	 *
	 * @param servletContext 当前servlet上下文
	 * @return 要使用的WebApplicationContext实现类
	 * @see #CONTEXT_CLASS_PARAM
	 * @see org.springframework.web.context.support.XmlWebApplicationContext
	 */
	protected Class<?> determineContextClass(ServletContext servletContext) {
		// 获取上下文类名
		String contextClassName = servletContext.getInitParameter(CONTEXT_CLASS_PARAM);
		if (contextClassName != null) {
			// 尝试加载自定义上下文类
			try {
				return ClassUtils.forName(contextClassName, ClassUtils.getDefaultClassLoader());
			} catch (ClassNotFoundException ex) {
				// 加载失败则抛出异常
				throw new ApplicationContextException(
						"Failed to load custom context class [" + contextClassName + "]", ex);
			}
		} else {
			// 如果未指定上下文类名，则使用默认策略中的类名
			contextClassName = defaultStrategies.getProperty(WebApplicationContext.class.getName());
			// 尝试加载默认上下文类
			try {
				return ClassUtils.forName(contextClassName, ContextLoader.class.getClassLoader());
			} catch (ClassNotFoundException ex) {
				// 加载失败则抛出异常
				throw new ApplicationContextException(
						"Failed to load default context class [" + contextClassName + "]", ex);
			}
		}
	}

	protected void configureAndRefreshWebApplicationContext(ConfigurableWebApplicationContext wac, ServletContext sc) {
		// 如果应用程序上下文的ID仍然是其原始默认值，则基于可用信息分配一个更有用的ID
		if (ObjectUtils.identityToString(wac).equals(wac.getId())) {
			// 应用程序上下文id仍设置为其原始默认值 -> 根据可用信息分配更有用的id
			String idParam = sc.getInitParameter(CONTEXT_ID_PARAM);
			if (idParam != null) {
				// 如果配置了ID参数，则将ID设置为参数值
				wac.setId(idParam);
			} else {
				// 否则根据上下文路径生成一个默认ID
				wac.setId(ConfigurableWebApplicationContext.APPLICATION_CONTEXT_ID_PREFIX +
						ObjectUtils.getDisplayString(sc.getContextPath()));
			}
		}

		// 设置应用程序上下文的 Servlet上下文
		wac.setServletContext(sc);

		// 设置应用程序上下文的配置位置
		String configLocationParam = sc.getInitParameter(CONFIG_LOCATION_PARAM);
		if (configLocationParam != null) {
			wac.setConfigLocation(configLocationParam);
		}

		// 应用程序上下文的环境的initPropertySources将在刷新上下文时被调用
		// 在这里提前执行以确保servlet属性源准备就绪，以便在刷新之前用于任何后处理或初始化
		ConfigurableEnvironment env = wac.getEnvironment();
		if (env instanceof ConfigurableWebEnvironment) {
			((ConfigurableWebEnvironment) env).initPropertySources(sc, null);
		}

		// 自定义上下文
		customizeContext(sc, wac);

		// 刷新应用程序上下文
		wac.refresh();
	}

	/**
	 * 在为此ContextLoader提供的配置位置提供上下文后，但在上下文刷新之前，自定义由此ContextLoader创建的ConfigurableWebApplicationContext。
	 * 默认实现确定通过上下文init参数指定了哪些（如果有）上下文初始化器类，并使用给定的Web应用程序上下文调用每个ApplicationContextInitializer。
	 * 任何ApplicationContextInitializers实现org.springframework.core.Ordered Ordered或标记有@ Order Order的都将被适当地排序。
	 *
	 * @param sc  当前servlet上下文
	 * @param wac 新创建的应用程序上下文
	 * @see #CONTEXT_INITIALIZER_CLASSES_PARAM
	 * @see ApplicationContextInitializer#initialize(ConfigurableApplicationContext)
	 */
	protected void customizeContext(ServletContext sc, ConfigurableWebApplicationContext wac) {
		// 确定要应用的上下文初始化器类列表
		List<Class<ApplicationContextInitializer<ConfigurableApplicationContext>>> initializerClasses =
				determineContextInitializerClasses(sc);

		// 遍历初始化器类列表
		for (Class<ApplicationContextInitializer<ConfigurableApplicationContext>> initializerClass : initializerClasses) {
			// 解析初始化器的泛型参数类型
			Class<?> initializerContextClass =
					GenericTypeResolver.resolveTypeArgument(initializerClass, ApplicationContextInitializer.class);
			// 如果初始化器的泛型参数类型，不是当前应用程序上下文类型的子类型，则抛出异常
			if (initializerContextClass != null && !initializerContextClass.isInstance(wac)) {
				throw new ApplicationContextException(String.format(
						"Could not apply context initializer [%s] since its generic parameter [%s] " +
								"is not assignable from the type of application context used by this " +
								"context loader: [%s]", initializerClass.getName(), initializerContextClass.getName(),
						wac.getClass().getName()));
			}
			// 实例化初始化器并添加到上下文初始化器列表中
			this.contextInitializers.add(BeanUtils.instantiateClass(initializerClass));
		}

		// 对上下文初始化器列表进行排序
		AnnotationAwareOrderComparator.sort(this.contextInitializers);
		// 遍历上下文初始化器列表，对每个初始化器进行初始化操作
		for (ApplicationContextInitializer<ConfigurableApplicationContext> initializer : this.contextInitializers) {
			initializer.initialize(wac);
		}
	}

	/**
	 * 返回要使用的 {@link ApplicationContextInitializer} 实现类，如果已由 {@link #CONTEXT_INITIALIZER_CLASSES_PARAM} 指定。
	 *
	 * @param servletContext 当前 servlet 上下文
	 * @see #CONTEXT_INITIALIZER_CLASSES_PARAM
	 */
	protected List<Class<ApplicationContextInitializer<ConfigurableApplicationContext>>>
	determineContextInitializerClasses(ServletContext servletContext) {

		// 创建存储初始化器类的列表
		List<Class<ApplicationContextInitializer<ConfigurableApplicationContext>>> classes =
				new ArrayList<>();

		// 获取全局初始化器类名参数
		String globalClassNames = servletContext.getInitParameter(GLOBAL_INITIALIZER_CLASSES_PARAM);
		if (globalClassNames != null) {
			// 如果存在全局初始化器类名参数，则解析并添加到列表中
			for (String className : StringUtils.tokenizeToStringArray(globalClassNames, INIT_PARAM_DELIMITERS)) {
				classes.add(loadInitializerClass(className));
			}
		}

		// 获取本地初始化器类名参数
		String localClassNames = servletContext.getInitParameter(CONTEXT_INITIALIZER_CLASSES_PARAM);
		if (localClassNames != null) {
			// 如果存在本地初始化器类名参数，则解析并添加到列表中
			for (String className : StringUtils.tokenizeToStringArray(localClassNames, INIT_PARAM_DELIMITERS)) {
				classes.add(loadInitializerClass(className));
			}
		}

		// 返回初始化器类列表
		return classes;
	}

	@SuppressWarnings("unchecked")
	private Class<ApplicationContextInitializer<ConfigurableApplicationContext>> loadInitializerClass(String className) {
		try {
			// 加载初始化器类
			Class<?> clazz = ClassUtils.forName(className, ClassUtils.getDefaultClassLoader());
			// 如果类不是ApplicationContextInitializer的子类，则抛出异常
			if (!ApplicationContextInitializer.class.isAssignableFrom(clazz)) {
				throw new ApplicationContextException(
						"Initializer class does not implement ApplicationContextInitializer interface: " + clazz);
			}
			// 将类转换为ApplicationContextInitializer<ConfigurableApplicationContext>类型并返回
			return (Class<ApplicationContextInitializer<ConfigurableApplicationContext>>) clazz;
		} catch (ClassNotFoundException ex) {
			// 加载类失败则抛出异常
			throw new ApplicationContextException("Failed to load context initializer class [" + className + "]", ex);
		}
	}

	/**
	 * 模板方法，默认实现（可以由子类覆盖），用于加载或获取将用作根 WebApplicationContext 的父上下文的 ApplicationContext 实例。
	 * 如果方法的返回值为 null，则不设置父上下文。
	 * 主要在此处加载父上下文的原因是允许多个根 Web 应用程序上下文都是共享 EAR 上下文的子级，或者也可以共享对 EJB 可见的相同父上下文。
	 * 对于纯 Web 应用程序，通常不需要担心是否有父上下文用作根 Web 应用程序上下文。
	 * 默认实现只返回 {@code null}，截至 5.0 版本。
	 *
	 * @param servletContext 当前 servlet 上下文
	 * @return 父应用程序上下文，如果没有则为 {@code null}
	 */
	@Nullable
	protected ApplicationContext loadParentContext(ServletContext servletContext) {
		return null;
	}

	/**
	 * 关闭给定 servlet 上下文的 Spring Web 应用程序上下文。
	 * 如果覆盖了 {@link #loadParentContext(ServletContext)}，可能还需要覆盖此方法。
	 *
	 * @param servletContext WebApplicationContext 运行的 ServletContext
	 */
	public void closeWebApplicationContext(ServletContext servletContext) {
		servletContext.log("Closing Spring root WebApplicationContext");
		try {
			// 如果上下文是 可配置的Web应用程序上下文 类型，则关闭上下文
			if (this.context instanceof ConfigurableWebApplicationContext) {
				((ConfigurableWebApplicationContext) this.context).close();
			}
		} finally {
			// 清理当前线程的上下文加载器和 Servlet上下文 属性
			ClassLoader ccl = Thread.currentThread().getContextClassLoader();
			if (ccl == ContextLoader.class.getClassLoader()) {
				// 如果当前线程的上下文加载器是 当前类的类加载器，则删除当前线程的上下文
				currentContext = null;
			} else if (ccl != null) {
				// 当前线程的类加载器存在，则移除出缓存
				currentContextPerThread.remove(ccl);
			}
			servletContext.removeAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
		}
	}


	/**
	 * 获取当前线程的 Spring 根 Web 应用程序上下文（即当前线程的上下文 ClassLoader，它需要是 Web 应用程序的 ClassLoader）。
	 *
	 * @return 当前根 Web 应用程序上下文，如果没有找到则为 {@code null}
	 * @see org.springframework.web.context.support.SpringBeanAutowiringSupport
	 */
	@Nullable
	public static WebApplicationContext getCurrentWebApplicationContext() {
		// 获取当前线程的上下文类加载器
		ClassLoader ccl = Thread.currentThread().getContextClassLoader();
		// 如果上下文类加载器不为空
		if (ccl != null) {
			// 从 每个线程的当前上下文 中获取当前线程对应的 Web应用程序上下文
			WebApplicationContext ccpt = currentContextPerThread.get(ccl);
			// 如果当前线程对应的 Web应用程序上下文 不为空，则返回它
			if (ccpt != null) {
				return ccpt;
			}
		}
		// 否则返回当前上下文
		return currentContext;
	}

}
