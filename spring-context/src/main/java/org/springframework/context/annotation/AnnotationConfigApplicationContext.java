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

package org.springframework.context.annotation;

import org.springframework.beans.factory.config.BeanDefinitionCustomizer;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.metrics.StartupStep;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.util.Arrays;
import java.util.function.Supplier;

/**
 * 独立的应用上下文，接受 <em>组件类</em> 作为输入，特别是带有 {@link Configuration @Configuration} 注解的类，
 * 但也支持普通的 {@link org.springframework.stereotype.Component @Component} 类型以及使用 {@code javax.inject} 注解的 JSR-330 兼容类。
 *
 * <p>允许通过 {@link #register(Class...)} 逐个注册类，也可以使用 {@link #scan(String...)} 进行类路径扫描。
 *
 * <p>在存在多个 {@code @Configuration} 类的情况下，后面的类中定义的 {@link Bean @Bean} 方法将覆盖先前类中定义的方法。
 * 这可以用于有意覆盖某些 Bean 定义的情况，通过额外的 {@code @Configuration} 类。
 *
 * <p>请参阅 {@link Configuration @Configuration} 的 javadoc 以获取使用示例。
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @see #register
 * @see #scan
 * @see AnnotatedBeanDefinitionReader
 * @see ClassPathBeanDefinitionScanner
 * @see org.springframework.context.support.GenericXmlApplicationContext
 * @since 3.0
 */
public class AnnotationConfigApplicationContext extends GenericApplicationContext implements AnnotationConfigRegistry {

	/**
	 * 注解Bean定义读取器
	 */
	private final AnnotatedBeanDefinitionReader reader;

	/**
	 * 类路径Bean定义扫描器
	 */
	private final ClassPathBeanDefinitionScanner scanner;


	/**
	 * 创建一个新的 AnnotationConfigApplicationContext，需要通过 {@link #register} 调用进行填充，然后手动 {@linkplain #refresh 刷新}。
	 */
	public AnnotationConfigApplicationContext() {
		//创建注解Bean相关读取器，默认的SratupStep
		StartupStep createAnnotatedBeanDefReader = this.getApplicationStartup().start("spring.context.annotated-bean-reader.create");
		//创建Bean定义读取器
		this.reader = new AnnotatedBeanDefinitionReader(this);
		//结束创建步骤，提供一个接口供子类拓展。一般用于记录步骤启动的一些指标。
		createAnnotatedBeanDefReader.end();
		//创建类路径Bean定义扫描器，扫描相关路径上的Component或者Bean
		this.scanner = new ClassPathBeanDefinitionScanner(this);
	}

	/**
	 * 使用给定的 DefaultListableBeanFactory 创建一个新的 AnnotationConfigApplicationContext。
	 *
	 * @param beanFactory 用于此上下文的 DefaultListableBeanFactory 实例
	 */
	public AnnotationConfigApplicationContext(DefaultListableBeanFactory beanFactory) {
		super(beanFactory);
		//初始化注解Bean定义读取器
		this.reader = new AnnotatedBeanDefinitionReader(this);
		//初始化类路径Bean定义扫描器
		this.scanner = new ClassPathBeanDefinitionScanner(this);
	}

	/**
	 * 创建一个新的 AnnotationConfigApplicationContext，从给定的组件类派生 Bean 定义，并自动刷新上下文。
	 *
	 * @param componentClasses 一个或多个组件类，例如，{@link Configuration @Configuration} 注解的类
	 */
	public AnnotationConfigApplicationContext(Class<?>... componentClasses) {
		this();
		//注册组件类
		register(componentClasses);
		//刷新组件类
		refresh();
	}

	/**
	 * 创建一个新的 AnnotationConfigApplicationContext，扫描给定包中的组件类，
	 * 为这些组件类注册 Bean 定义，并自动刷新上下文。
	 *
	 * @param basePackages 用于扫描组件类的包
	 */
	public AnnotationConfigApplicationContext(String... basePackages) {
		this();
		//扫描指定的包
		scan(basePackages);
		//刷新组件类
		refresh();
	}


	/**
	 * 传播给定的自定义 {@code Environment} 到底层的
	 * {@link AnnotatedBeanDefinitionReader} 和 {@link ClassPathBeanDefinitionScanner}。
	 */
	@Override
	public void setEnvironment(ConfigurableEnvironment environment) {
		// 调用父类的setEnvironment方法，设置环境信息
		super.setEnvironment(environment);

		// 设置注解式Bean定义阅读器的环境信息
		this.reader.setEnvironment(environment);

		// 设置类路径Bean定义扫描器的环境信息
		this.scanner.setEnvironment(environment);
	}

	/**
	 * 提供用于 {@link AnnotatedBeanDefinitionReader} 和/或 {@link ClassPathBeanDefinitionScanner} 的自定义 {@link BeanNameGenerator}，如果有的话。
	 * <p>默认是 {@link AnnotationBeanNameGenerator}。
	 * <p>对此方法的任何调用必须在对 {@link #register(Class...)} 和/或 {@link #scan(String...)} 的调用之前发生。
	 *
	 * @see AnnotatedBeanDefinitionReader#setBeanNameGenerator
	 * @see ClassPathBeanDefinitionScanner#setBeanNameGenerator
	 * @see AnnotationBeanNameGenerator
	 * @see FullyQualifiedAnnotationBeanNameGenerator
	 */
	public void setBeanNameGenerator(BeanNameGenerator beanNameGenerator) {
		// 设置注解式Bean定义阅读器的Bean名称生成器
		this.reader.setBeanNameGenerator(beanNameGenerator);

		// 设置类路径Bean定义扫描器的Bean名称生成器
		this.scanner.setBeanNameGenerator(beanNameGenerator);

		// 在Bean工厂中注册单例Bean，用于配置类的Bean名称生成器
		getBeanFactory().registerSingleton(
				AnnotationConfigUtils.CONFIGURATION_BEAN_NAME_GENERATOR, beanNameGenerator);
	}

	/**
	 * 设置用于注册的组件类的 {@link ScopeMetadataResolver}。
	 * <p>默认是 {@link AnnotationScopeMetadataResolver}。
	 * <p>对此方法的任何调用必须在对 {@link #register(Class...)} 和/或 {@link #scan(String...)} 的调用之前发生。
	 */
	public void setScopeMetadataResolver(ScopeMetadataResolver scopeMetadataResolver) {
		// 设置注解式Bean定义阅读器的作用域元数据解析器
		this.reader.setScopeMetadataResolver(scopeMetadataResolver);

		// 设置类路径Bean定义扫描器的作用域元数据解析器
		this.scanner.setScopeMetadataResolver(scopeMetadataResolver);
	}


	//---------------------------------------------------------------------
	// Implementation of AnnotationConfigRegistry
	//---------------------------------------------------------------------

	/**
	 * 注册一个或多个要处理的组件类。
	 * <p>注意，必须调用 {@link #refresh()} 以便上下文能够完全处理新的类。
	 *
	 * @param componentClasses 一个或多个组件类，例如，{@link Configuration @Configuration} 类
	 * @see #scan(String...)
	 * @see #refresh()
	 */
	@Override
	public void register(Class<?>... componentClasses) {
		Assert.notEmpty(componentClasses, "At least one component class must be specified");
		//注册组件类
		StartupStep registerComponentClass = this.getApplicationStartup().start("spring.context.component-classes.register")
				.tag("classes", () -> Arrays.toString(componentClasses));
		//注解Bean定义读取器注册该组件
		this.reader.register(componentClasses);
		//注册完毕
		registerComponentClass.end();
	}

	/**
	 * 在指定的基本包中执行扫描。
	 * <p>注意，必须调用 {@link #refresh()} 以便上下文能够完全处理新的类。
	 *
	 * @param basePackages 要扫描的组件类所在的包
	 * @see #register(Class...)
	 * @see #refresh()
	 */
	@Override
	public void scan(String... basePackages) {
		// 检查基础包是否为空，至少要指定一个基础包
		Assert.notEmpty(basePackages, "At least one base package must be specified");

		// 启动扫描基础包的启动步骤，并记录扫描的基础包信息
		StartupStep scanPackages = this.getApplicationStartup().start("spring.context.base-packages.scan")
				.tag("packages", () -> Arrays.toString(basePackages));

		// 使用类路径Bean定义扫描器扫描指定的基础包
		this.scanner.scan(basePackages);

		// 结束扫描基础包的启动步骤
		scanPackages.end();
	}


	//---------------------------------------------------------------------
	// 将超类registerBean调用调整为AnnotatedBeanDefinitionReader
	//---------------------------------------------------------------------

	@Override
	public <T> void registerBean(@Nullable String beanName, Class<T> beanClass,
								 @Nullable Supplier<T> supplier, BeanDefinitionCustomizer... customizers) {

		this.reader.registerBean(beanClass, beanName, supplier, customizers);
	}

}
