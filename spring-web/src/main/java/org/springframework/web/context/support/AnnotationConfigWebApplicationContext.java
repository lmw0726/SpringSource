/*
 * Copyright 2002-2022 the original author or authors.
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

import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.*;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.ContextLoader;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * {@link org.springframework.web.context.WebApplicationContext WebApplicationContext}
 * 的实现，接受 <em>组件类</em> 作为输入，特别是
 * {@link org.springframework.context.annotation.Configuration @Configuration}
 * 类，同时也可以是普通的 {@link org.springframework.stereotype.Component @Component}
 * 类，以及使用 {@code javax.inject} 注解的 JSR-330 兼容类。
 * <p>
 * 允许逐个注册类（将类名指定为配置位置）以及通过类路径扫描（将基本包指定为配置位置）进行注册。
 * <p>
 * 本质上相当于用于 Web 环境的 {@link org.springframework.context.annotation.AnnotationConfigApplicationContext
 * AnnotationConfigApplicationContext}。但与 {@code AnnotationConfigApplicationContext}
 * 不同，该类不扩展 {@link org.springframework.context.support.GenericApplicationContext
 * GenericApplicationContext}，因此不提供 {@code GenericApplicationContext} 中的一些方便的
 * {@code registerBean(...)} 方法。如果您希望在 Web 环境中将带有注解的 <em>组件类</em> 注册到
 * {@code GenericApplicationContext} 中，可以使用具有
 * {@link org.springframework.context.annotation.AnnotatedBeanDefinitionReader
 * AnnotatedBeanDefinitionReader} 的 {@code GenericWebApplicationContext}。有关详细信息和示例，请参阅
 * {@link GenericWebApplicationContext} 的 Javadoc。
 * <p>
 * 要使用此应用程序上下文，必须将 {@linkplain ContextLoader#CONTEXT_CLASS_PARAM "contextClass"}
 * ContextLoader 的上下文参数和/或 FrameworkServlet 的 "contextClass" init 参数设置为该类的完全限定名。
 * <p>
 * 从 Spring 3.1 开始，当使用 {@link org.springframework.web.WebApplicationInitializer WebApplicationInitializer}
 * 基于代码的替代方案时，此类还可以直接实例化并注入到 Spring 的 {@code DispatcherServlet} 或
 * {@code ContextLoaderListener} 中。有关详细信息和用法示例，请参阅其 Javadoc。
 * <p>
 * 与 {@link XmlWebApplicationContext} 不同，不会假定任何默认的配置类位置。相反，必须设置
 * {@linkplain ContextLoader#CONFIG_LOCATION_PARAM "contextConfigLocation"}
 * ContextLoader 的上下文参数和/或 FrameworkServlet 的 "contextConfigLocation" init 参数。param-value
 * 可以包含完全限定的类名和要扫描组件的基本包。有关这些位置如何处理的详细信息，请参阅 {@link #loadBeanDefinitions}。
 * <p>
 * 作为设置 "contextConfigLocation" 参数的替代方案，用户可以实现 {@link org.springframework.context.ApplicationContextInitializer
 * ApplicationContextInitializer} 并设置
 * {@linkplain ContextLoader#CONTEXT_INITIALIZER_CLASSES_PARAM "contextInitializerClasses"}
 * 上下文参数/初始化参数。在这种情况下，用户应优先考虑 {@link #refresh()} 和 {@link #scan(String...)}
 * 方法，而不是 {@link #setConfigLocation(String)} 方法，该方法主要供 {@code ContextLoader} 使用。
 * <p>
 * 注意：在存在多个 {@code @Configuration} 类的情况下，后来的 {@code @Bean} 定义将覆盖
 * 先前加载文件中定义的内容。这可以用来有意识地通过额外的 {@code @Configuration} 类覆盖某些 bean 定义。
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @see org.springframework.context.annotation.AnnotationConfigApplicationContext
 * @see org.springframework.web.context.support.GenericWebApplicationContext
 * @since 3.0
 */
public class AnnotationConfigWebApplicationContext extends AbstractRefreshableWebApplicationContext
		implements AnnotationConfigRegistry {

	/**
	 * Bean名称生成器
	 */
	@Nullable
	private BeanNameGenerator beanNameGenerator;

	/**
	 * Scope元数据解析器
	 */
	@Nullable
	private ScopeMetadataResolver scopeMetadataResolver;

	/**
	 * 组件类集合
	 */
	private final Set<Class<?>> componentClasses = new LinkedHashSet<>();

	/**
	 * 扫描的基础包类
	 */
	private final Set<String> basePackages = new LinkedHashSet<>();


	/**
	 * 设置用于 {@link AnnotatedBeanDefinitionReader} 和/或 {@link ClassPathBeanDefinitionScanner} 的自定义
	 * {@link BeanNameGenerator}。
	 * <p>默认值为 {@link org.springframework.context.annotation.AnnotationBeanNameGenerator}。
	 *
	 * @see AnnotatedBeanDefinitionReader#setBeanNameGenerator
	 * @see ClassPathBeanDefinitionScanner#setBeanNameGenerator
	 */
	public void setBeanNameGenerator(@Nullable BeanNameGenerator beanNameGenerator) {
		this.beanNameGenerator = beanNameGenerator;
	}

	/**
	 * 返回用于 {@link AnnotatedBeanDefinitionReader} 和/或 {@link ClassPathBeanDefinitionScanner} 的自定义
	 * {@link BeanNameGenerator}，如果有的话。
	 */
	@Nullable
	protected BeanNameGenerator getBeanNameGenerator() {
		return this.beanNameGenerator;
	}

	/**
	 * 设置用于 {@link AnnotatedBeanDefinitionReader} 和/或 {@link ClassPathBeanDefinitionScanner} 的自定义
	 * {@link ScopeMetadataResolver}。
	 * <p>默认为 {@link org.springframework.context.annotation.AnnotationScopeMetadataResolver}。
	 *
	 * @see AnnotatedBeanDefinitionReader#setScopeMetadataResolver
	 * @see ClassPathBeanDefinitionScanner#setScopeMetadataResolver
	 */
	public void setScopeMetadataResolver(@Nullable ScopeMetadataResolver scopeMetadataResolver) {
		this.scopeMetadataResolver = scopeMetadataResolver;
	}

	/**
	 * 返回用于 {@link AnnotatedBeanDefinitionReader} 和/或 {@link ClassPathBeanDefinitionScanner} 的自定义
	 * {@link ScopeMetadataResolver}（如果有的话）。
	 */
	@Nullable
	protected ScopeMetadataResolver getScopeMetadataResolver() {
		return this.scopeMetadataResolver;
	}


	/**
	 * 注册一个或多个要处理的组件类。
	 * <p>请注意，必须调用 {@link #refresh()} 以便上下文完全处理新的类。
	 *
	 * @param componentClasses 一个或多个组件类，例如 {@link org.springframework.context.annotation.Configuration @Configuration} 类
	 * @see #scan(String...)
	 * @see #loadBeanDefinitions(DefaultListableBeanFactory)
	 * @see #setConfigLocation(String)
	 * @see #refresh()
	 */
	@Override
	public void register(Class<?>... componentClasses) {
		Assert.notEmpty(componentClasses, "At least one component class must be specified");
		Collections.addAll(this.componentClasses, componentClasses);
	}

	/**
	 * 在指定的基础包内执行扫描。
	 * <p>请注意，必须调用 {@link #refresh()} 以便上下文完全处理新的类。
	 *
	 * @param basePackages 要检查组件类的包
	 * @see #loadBeanDefinitions(DefaultListableBeanFactory)
	 * @see #register(Class...)
	 * @see #setConfigLocation(String)
	 * @see #refresh()
	 */
	@Override
	public void scan(String... basePackages) {
		Assert.notEmpty(basePackages, "At least one base package must be specified");
		Collections.addAll(this.basePackages, basePackages);
	}


	/**
	 * 为由 {@link #register(Class...)} 指定的任何类和由 {@link #scan(String...)} 指定的任何包注册
	 * {@link org.springframework.beans.factory.config.BeanDefinition}。
	 * <p>对于由 {@link #setConfigLocation(String)} 或 {@link #setConfigLocations(String[])} 指定的任何值，
	 * 首先尝试将每个位置加载为类，如果类加载成功，则注册一个 {@code BeanDefinition}；
	 * 如果类加载失败（即引发 {@code ClassNotFoundException}），则假定该值是一个包，并尝试扫描其中的组件类。
	 * <p>启用默认的一组注解配置后处理器，以便可以使用 {@code @Autowired}、{@code @Required} 和相关注解。
	 * <p>配置类的 Bean 定义将使用生成的 bean 定义名称注册，除非为注解中的 {@code value} 属性提供了值。
	 *
	 * @param beanFactory 要加载 Bean 定义的 Bean 工厂
	 * @see #register(Class...)
	 * @see #scan(String...)
	 * @see #setConfigLocation(String)
	 * @see #setConfigLocations(String[])
	 * @see AnnotatedBeanDefinitionReader
	 * @see ClassPathBeanDefinitionScanner
	 */
	@Override
	protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) {
		// 获取注解式Bean定义阅读器
		AnnotatedBeanDefinitionReader reader = getAnnotatedBeanDefinitionReader(beanFactory);

		// 获取类路径Bean定义扫描器
		ClassPathBeanDefinitionScanner scanner = getClassPathBeanDefinitionScanner(beanFactory);

		// 获取配置的Bean名称生成器
		BeanNameGenerator beanNameGenerator = getBeanNameGenerator();
		if (beanNameGenerator != null) {
			// 设置Bean名称生成器到注解式Bean定义阅读器和类路径Bean定义扫描器
			reader.setBeanNameGenerator(beanNameGenerator);
			scanner.setBeanNameGenerator(beanNameGenerator);
			// 在Bean工厂中注册Bean名称生成器的单例实例
			beanFactory.registerSingleton(AnnotationConfigUtils.CONFIGURATION_BEAN_NAME_GENERATOR, beanNameGenerator);
		}

		// 获取配置的作用域元数据解析器
		ScopeMetadataResolver scopeMetadataResolver = getScopeMetadataResolver();
		if (scopeMetadataResolver != null) {
			// 设置作用域元数据解析器到注解式Bean定义阅读器和类路径Bean定义扫描器
			reader.setScopeMetadataResolver(scopeMetadataResolver);
			scanner.setScopeMetadataResolver(scopeMetadataResolver);
		}

		// 如果组件类集合非空，则注册组件类到注解式Bean定义阅读器
		if (!this.componentClasses.isEmpty()) {
			if (logger.isDebugEnabled()) {
				logger.debug("Registering component classes: [" +
						StringUtils.collectionToCommaDelimitedString(this.componentClasses) + "]");
			}
			// 将组件类注册到注解式Bean定义阅读器
			reader.register(ClassUtils.toClassArray(this.componentClasses));
		}

		// 如果基础包集合非空，则进行包扫描
		if (!this.basePackages.isEmpty()) {
			if (logger.isDebugEnabled()) {
				logger.debug("Scanning base packages: [" +
						StringUtils.collectionToCommaDelimitedString(this.basePackages) + "]");
			}
			// 将基础包名转为数组，然后进行包扫描
			scanner.scan(StringUtils.toStringArray(this.basePackages));
		}

		// 获取配置的Bean定义的位置数组
		String[] configLocations = getConfigLocations();
		if (configLocations != null) {
			// 遍历配置的Bean定义位置数组
			for (String configLocation : configLocations) {
				try {
					// 尝试加载配置的Bean定义类
					Class<?> clazz = ClassUtils.forName(configLocation, getClassLoader());
					if (logger.isTraceEnabled()) {
						logger.trace("Registering [" + configLocation + "]");
					}
					// 将加载的类注册到注解式Bean定义阅读器
					reader.register(clazz);
				} catch (ClassNotFoundException ex) {
					if (logger.isTraceEnabled()) {
						logger.trace("Could not load class for config location [" + configLocation +
								"] - trying package scan. " + ex);
					}
					// 如果加载失败，尝试进行包扫描
					int count = scanner.scan(configLocation);
					if (count == 0 && logger.isDebugEnabled()) {
						logger.debug("No component classes found for specified class/package [" + configLocation + "]");
					}
				}
			}
		}
	}


	/**
	 * 为给定的 Bean 工厂构建一个 {@link AnnotatedBeanDefinitionReader}。
	 * <p>这应该预先配置好 {@code Environment}（如果需要），但还没有配置 {@code BeanNameGenerator} 或 {@code ScopeMetadataResolver}。
	 *
	 * @param beanFactory 要加载 Bean 定义的 Bean 工厂
	 * @see #getEnvironment()
	 * @see #getBeanNameGenerator()
	 * @see #getScopeMetadataResolver()
	 * @since 4.1.9
	 */
	protected AnnotatedBeanDefinitionReader getAnnotatedBeanDefinitionReader(DefaultListableBeanFactory beanFactory) {
		return new AnnotatedBeanDefinitionReader(beanFactory, getEnvironment());
	}

	/**
	 * 为给定的 Bean 工厂构建一个 {@link ClassPathBeanDefinitionScanner}。
	 * <p>这应该预先配置好 {@code Environment}（如果需要），但还没有配置 {@code BeanNameGenerator} 或 {@code ScopeMetadataResolver}。
	 *
	 * @param beanFactory 要加载 Bean 定义的 Bean 工厂
	 * @see #getEnvironment()
	 * @see #getBeanNameGenerator()
	 * @see #getScopeMetadataResolver()
	 * @since 4.1.9
	 */
	protected ClassPathBeanDefinitionScanner getClassPathBeanDefinitionScanner(DefaultListableBeanFactory beanFactory) {
		return new ClassPathBeanDefinitionScanner(beanFactory, true, getEnvironment());
	}

}
