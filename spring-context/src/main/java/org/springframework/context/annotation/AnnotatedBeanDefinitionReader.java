/*
 * Copyright 2002-2019 the original author or authors.
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

import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionCustomizer;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AutowireCandidateQualifier;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.core.env.Environment;
import org.springframework.core.env.EnvironmentCapable;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.lang.annotation.Annotation;
import java.util.function.Supplier;

/**
 * 用于编程式注册 Bean 类的便捷适配器。
 *
 * <p>这是 {@link ClassPathBeanDefinitionScanner} 的替代方案，应用相同的注解解析逻辑，
 * 但仅适用于显式注册的类。
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Sam Brannen
 * @author Phillip Webb
 * @see AnnotationConfigApplicationContext#register
 * @since 3.0
 */
public class AnnotatedBeanDefinitionReader {

	private final BeanDefinitionRegistry registry;

	private BeanNameGenerator beanNameGenerator = AnnotationBeanNameGenerator.INSTANCE;

	private ScopeMetadataResolver scopeMetadataResolver = new AnnotationScopeMetadataResolver();

	private ConditionEvaluator conditionEvaluator;


	/**
	 * 为给定的注册表创建一个新的 {@code AnnotatedBeanDefinitionReader}。
	 * <p>如果注册表实现了 {@link EnvironmentCapable}（例如 {@code ApplicationContext}），
	 * 则 {@link Environment} 将被继承；否则将创建并使用一个新的
	 * {@link StandardEnvironment}。
	 *
	 * @param registry 用于加载 Bean 定义的 {@code BeanFactory}，
	 *                 以 {@code BeanDefinitionRegistry} 形式提供
	 * @see #AnnotatedBeanDefinitionReader(BeanDefinitionRegistry, Environment)
	 * @see #setEnvironment(Environment)
	 */
	public AnnotatedBeanDefinitionReader(BeanDefinitionRegistry registry) {
		//使用Bean定义注册器注册一个基于注解的Bean定义读取器
		//如果是环境能力接口，环境将会被继承。否则就创建一个标准的环境。
		this(registry, getOrCreateEnvironment(registry));
	}

	/**
	 * 为给定的注册表创建一个新的 {@code AnnotatedBeanDefinitionReader}，
	 * 使用给定的 {@link Environment}。
	 *
	 * @param registry    用于加载 Bean 定义的 {@code BeanFactory}，
	 *                    以 {@code BeanDefinitionRegistry} 形式提供
	 * @param environment 在评估 Bean 定义 profile 时使用的 {@code Environment}
	 * @since 3.1
	 */
	public AnnotatedBeanDefinitionReader(BeanDefinitionRegistry registry, Environment environment) {
		Assert.notNull(registry, "BeanDefinitionRegistry must not be null");
		Assert.notNull(environment, "Environment must not be null");
		this.registry = registry;
		//条件推断器
		this.conditionEvaluator = new ConditionEvaluator(registry, environment, null);
		//在给定的注册表中注册所有相关的注解配置处理器。
		AnnotationConfigUtils.registerAnnotationConfigProcessors(this.registry);
	}


	/**
	 * 获取此读取器操作的 BeanDefinitionRegistry。
	 */
	public final BeanDefinitionRegistry getRegistry() {
		return this.registry;
	}

	/**
	 * 设置在评估是否应注册 {@link Conditional @Conditional} 注解的组件类时
	 * 使用的 {@code Environment}。
	 * <p>默认值为 {@link StandardEnvironment}。
	 *
	 * @see #registerBean(Class, String, Class...)
	 */
	public void setEnvironment(Environment environment) {
		this.conditionEvaluator = new ConditionEvaluator(this.registry, environment, null);
	}

	/**
	 * 设置用于检测到的 Bean 类的 {@code BeanNameGenerator}。
	 * <p>默认值为 {@link AnnotationBeanNameGenerator}。
	 */
	public void setBeanNameGenerator(@Nullable BeanNameGenerator beanNameGenerator) {
		this.beanNameGenerator =
				(beanNameGenerator != null ? beanNameGenerator : AnnotationBeanNameGenerator.INSTANCE);
	}

	/**
	 * 设置用于注册的组件类的 {@code ScopeMetadataResolver}。
	 * <p>默认值为 {@link AnnotationScopeMetadataResolver}。
	 */
	public void setScopeMetadataResolver(@Nullable ScopeMetadataResolver scopeMetadataResolver) {
		this.scopeMetadataResolver =
				(scopeMetadataResolver != null ? scopeMetadataResolver : new AnnotationScopeMetadataResolver());
	}


	/**
	 * 注册一个或多个要处理的组件类。
	 * <p>调用 {@code register} 是幂等的；多次添加同一个组件类不会产生额外效果。
	 *
	 * @param componentClasses 一个或多个组件类，
	 *                         例如 {@link Configuration @Configuration} 类
	 */
	public void register(Class<?>... componentClasses) {
		for (Class<?> componentClass : componentClasses) {
			registerBean(componentClass);
		}
	}

	/**
	 * 从给定的 Bean 类注册一个 Bean，其元数据从类声明的注解中获取。
	 *
	 * @param beanClass Bean 的类
	 */
	public void registerBean(Class<?> beanClass) {
		doRegisterBean(beanClass, null, null, null, null);
	}

	/**
	 * 从给定的 Bean 类注册一个 Bean，其元数据从类声明的注解中获取。
	 *
	 * @param beanClass Bean 的类
	 * @param name      Bean 的显式名称
	 *                  （若为 {@code null} 则生成默认 Bean 名称）
	 * @since 5.2
	 */
	public void registerBean(Class<?> beanClass, @Nullable String name) {
		doRegisterBean(beanClass, name, null, null, null);
	}

	/**
	 * 从给定的 Bean 类注册一个 Bean，其元数据从类声明的注解中获取。
	 *
	 * @param beanClass  Bean 的类
	 * @param qualifiers 除 Bean 类级别限定符外，还需考虑的特定限定符注解
	 */
	@SuppressWarnings("unchecked")
	public void registerBean(Class<?> beanClass, Class<? extends Annotation>... qualifiers) {
		doRegisterBean(beanClass, null, qualifiers, null, null);
	}

	/**
	 * 从给定的 Bean 类注册一个 Bean，其元数据从类声明的注解中获取。
	 *
	 * @param beanClass  Bean 的类
	 * @param name       Bean 的显式名称
	 *                   （若为 {@code null} 则生成默认 Bean 名称）
	 * @param qualifiers 除 Bean 类级别限定符外，还需考虑的特定限定符注解
	 */
	@SuppressWarnings("unchecked")
	public void registerBean(Class<?> beanClass, @Nullable String name,
							 Class<? extends Annotation>... qualifiers) {

		doRegisterBean(beanClass, name, qualifiers, null, null);
	}

	/**
	 * 从给定的 Bean 类注册一个 Bean，其元数据从类声明的注解中获取，
	 * 使用给定的 supplier 获取新实例（可声明为 lambda 表达式或方法引用）。
	 *
	 * @param beanClass Bean 的类
	 * @param supplier  创建 Bean 实例的回调
	 *                  （可以为 {@code null}）
	 * @since 5.0
	 */
	public <T> void registerBean(Class<T> beanClass, @Nullable Supplier<T> supplier) {
		doRegisterBean(beanClass, null, null, supplier, null);
	}

	/**
	 * 从给定的 Bean 类注册一个 Bean，其元数据从类声明的注解中获取，
	 * 使用给定的 supplier 获取新实例（可声明为 lambda 表达式或方法引用）。
	 *
	 * @param beanClass Bean 的类
	 * @param name      Bean 的显式名称
	 *                  （若为 {@code null} 则生成默认 Bean 名称）
	 * @param supplier  创建 Bean 实例的回调
	 *                  （可以为 {@code null}）
	 * @since 5.0
	 */
	public <T> void registerBean(Class<T> beanClass, @Nullable String name, @Nullable Supplier<T> supplier) {
		doRegisterBean(beanClass, name, null, supplier, null);
	}

	/**
	 * 从给定的 Bean 类注册一个 Bean，其元数据从类声明的注解中获取。
	 *
	 * @param beanClass   Bean 的类
	 * @param name        Bean 的显式名称
	 *                    （若为 {@code null} 则生成默认 Bean 名称）
	 * @param supplier    创建 Bean 实例的回调
	 *                    （可以为 {@code null}）
	 * @param customizers 一个或多个用于自定义工厂 {@link BeanDefinition} 的回调，
	 *                    例如设置 lazy-init 或 primary 标志
	 * @since 5.2
	 */
	public <T> void registerBean(Class<T> beanClass, @Nullable String name, @Nullable Supplier<T> supplier,
								 BeanDefinitionCustomizer... customizers) {

		doRegisterBean(beanClass, name, null, supplier, customizers);
	}

	/**
	 * 从给定的 Bean 类注册一个 Bean，其元数据从类声明的注解中获取。
	 *
	 * @param beanClass   Bean 的类
	 * @param name        Bean 的显式名称
	 * @param qualifiers 除 Bean 类级别限定符外，还需考虑的特定限定符注解（如有）
	 * @param supplier    创建 Bean 实例的回调
	 *                    （可以为 {@code null}）
	 * @param customizers 一个或多个用于自定义工厂 {@link BeanDefinition} 的回调，
	 *                    例如设置 lazy-init 或 primary 标志
	 * @since 5.0
	 */
	private <T> void doRegisterBean(Class<T> beanClass, @Nullable String name,
									@Nullable Class<? extends Annotation>[] qualifiers, @Nullable Supplier<T> supplier,
									@Nullable BeanDefinitionCustomizer[] customizers) {
		//注解通用Bean定义
		AnnotatedGenericBeanDefinition abd = new AnnotatedGenericBeanDefinition(beanClass);
		//根据@Condition决定是否跳过该Bean的注册
		if (this.conditionEvaluator.shouldSkip(abd.getMetadata())) {
			return;
		}
		//设置回调函数
		abd.setInstanceSupplier(supplier);
		//解析适合于提供的 bean 定义的 ScopeMetadata
		ScopeMetadata scopeMetadata = this.scopeMetadataResolver.resolveScopeMetadata(abd);
		abd.setScope(scopeMetadata.getScopeName());
		String beanName = (name != null ? name : this.beanNameGenerator.generateBeanName(abd, this.registry));
		//处理通用定义注解
		AnnotationConfigUtils.processCommonDefinitionAnnotations(abd);
		if (qualifiers != null) {
			for (Class<? extends Annotation> qualifier : qualifiers) {
				if (Primary.class == qualifier) {
					abd.setPrimary(true);
				} else if (Lazy.class == qualifier) {
					abd.setLazyInit(true);
				} else {
					abd.addQualifier(new AutowireCandidateQualifier(qualifier));
				}
			}
		}
		if (customizers != null) {
			for (BeanDefinitionCustomizer customizer : customizers) {
				customizer.customize(abd);
			}
		}

		BeanDefinitionHolder definitionHolder = new BeanDefinitionHolder(abd, beanName);
		//应用代理范围模式
		definitionHolder = AnnotationConfigUtils.applyScopedProxyMode(scopeMetadata, definitionHolder, this.registry);
		//注册Bean定义
		BeanDefinitionReaderUtils.registerBeanDefinition(definitionHolder, this.registry);
	}


	/**
	 * 如果可能，从给定的注册表获取 Environment，否则返回一个新的 StandardEnvironment。
	 */
	private static Environment getOrCreateEnvironment(BeanDefinitionRegistry registry) {
		Assert.notNull(registry, "BeanDefinitionRegistry must not be null");
		if (registry instanceof EnvironmentCapable) {
			return ((EnvironmentCapable) registry).getEnvironment();
		}
		//初始化标准环境
		return new StandardEnvironment();
	}

}
