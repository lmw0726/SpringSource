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

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.*;
import org.springframework.core.env.Environment;
import org.springframework.core.env.EnvironmentCapable;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.PatternMatchUtils;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 一个 Bean 定义扫描器，用于检测类路径上的 Bean 候选者，并将相应的 Bean 定义注册到给定的注册表（{@code BeanFactory} 或 {@code ApplicationContext}）中。
 *
 * <p>候选类通过可配置的类型过滤器进行检测。默认过滤器包括使用 Spring 的
 * {@link org.springframework.stereotype.Component @Component}、
 * {@link org.springframework.stereotype.Repository @Repository}、
 * {@link org.springframework.stereotype.Service @Service} 或
 * {@link org.springframework.stereotype.Controller @Controller} 原型注解的类。
 *
 * <p>还支持 Java EE 6 的 {@link javax.annotation.ManagedBean} 和 JSR-330 的 {@link javax.inject.Named} 注解（如果可用）。
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 2.5
 * @see AnnotationConfigApplicationContext#scan
 * @see org.springframework.stereotype.Component
 * @see org.springframework.stereotype.Repository
 * @see org.springframework.stereotype.Service
 * @see org.springframework.stereotype.Controller
 */
public class ClassPathBeanDefinitionScanner extends ClassPathScanningCandidateComponentProvider {


	private final BeanDefinitionRegistry registry;

	private BeanDefinitionDefaults beanDefinitionDefaults = new BeanDefinitionDefaults();

	@Nullable
	private String[] autowireCandidatePatterns;

	private BeanNameGenerator beanNameGenerator = AnnotationBeanNameGenerator.INSTANCE;

	private ScopeMetadataResolver scopeMetadataResolver = new AnnotationScopeMetadataResolver();

	private boolean includeAnnotationConfig = true;


	/**
	 * 为给定的 Bean 工厂创建一个新的 {@code ClassPathBeanDefinitionScanner}。
	 * @param registry 要加载 Bean 定义的 {@code BeanFactory}，以 {@code BeanDefinitionRegistry} 的形式提供
	 */
	public ClassPathBeanDefinitionScanner(BeanDefinitionRegistry registry) {
		this(registry, true);
	}

	/**
	 * 为给定的 Bean 工厂创建一个新的 {@code ClassPathBeanDefinitionScanner}。
	 * <p>如果传入的 Bean 工厂不仅实现了 {@code BeanDefinitionRegistry} 接口，
	 * 还实现了 {@code ResourceLoader} 接口，则它也将被用作默认的 {@code ResourceLoader}。
	 * 这通常是 {@link org.springframework.context.ApplicationContext} 实现的情况。
	 * <p>如果传入的是普通的 {@code BeanDefinitionRegistry}，默认的 {@code ResourceLoader}
	 * 将是 {@link org.springframework.core.io.support.PathMatchingResourcePatternResolver}。
	 * <p>如果传入的 Bean 工厂还实现了 {@link EnvironmentCapable}，则此 reader 将使用其环境。
	 * 否则，reader 将初始化并使用 {@link org.springframework.core.env.StandardEnvironment}。
	 * 所有 {@code ApplicationContext} 实现都是 {@code EnvironmentCapable} 的，
	 * 而普通的 {@code BeanFactory} 实现则不是。
	 * @param registry 要加载 Bean 定义的 {@code BeanFactory}，以 {@code BeanDefinitionRegistry} 的形式提供
	 * @param useDefaultFilters 是否包含 {@link org.springframework.stereotype.Component @Component}、
	 * {@link org.springframework.stereotype.Repository @Repository}、
	 * {@link org.springframework.stereotype.Service @Service} 和
	 * {@link org.springframework.stereotype.Controller @Controller} 原型注解的默认过滤器
	 * @see #setResourceLoader
	 * @see #setEnvironment
	 */
	public ClassPathBeanDefinitionScanner(BeanDefinitionRegistry registry, boolean useDefaultFilters) {
		this(registry, useDefaultFilters, getOrCreateEnvironment(registry));
	}

	/**
	 * 为给定的 Bean 工厂创建一个新的 {@code ClassPathBeanDefinitionScanner}，
	 * 并在评估 Bean 定义配置元数据时使用给定的 {@link Environment}。
	 * <p>如果传入的 Bean 工厂不仅实现了 {@code BeanDefinitionRegistry} 接口，
	 * 还实现了 {@link ResourceLoader} 接口，则它也将被用作默认的 {@code ResourceLoader}。
	 * 这通常是 {@link org.springframework.context.ApplicationContext} 实现的情况。
	 * <p>如果传入的是普通的 {@code BeanDefinitionRegistry}，默认的 {@code ResourceLoader}
	 * 将是 {@link org.springframework.core.io.support.PathMatchingResourcePatternResolver}。
	 * @param registry 要加载 Bean 定义的 {@code BeanFactory}，以 {@code BeanDefinitionRegistry} 的形式提供
	 * @param useDefaultFilters 是否包含 {@link org.springframework.stereotype.Component @Component}、
	 * {@link org.springframework.stereotype.Repository @Repository}、
	 * {@link org.springframework.stereotype.Service @Service} 和
	 * {@link org.springframework.stereotype.Controller @Controller} 原型注解的默认过滤器
	 * @param environment 在评估 Bean 定义配置元数据时使用的 Spring {@link Environment}
	 * @since 3.1
	 * @see #setResourceLoader
	 */
	public ClassPathBeanDefinitionScanner(BeanDefinitionRegistry registry, boolean useDefaultFilters,
			Environment environment) {

		this(registry, useDefaultFilters, environment,
				(registry instanceof ResourceLoader ? (ResourceLoader) registry : null));
	}

	/**
	 * 为给定的 Bean 工厂创建一个新的 {@code ClassPathBeanDefinitionScanner}，
	 * 并在评估 Bean 定义配置元数据时使用给定的 {@link Environment}。
	 * @param registry 要加载 Bean 定义的 {@code BeanFactory}，以 {@code BeanDefinitionRegistry} 的形式提供
	 * @param useDefaultFilters 是否包含 {@link org.springframework.stereotype.Component @Component}、
	 * {@link org.springframework.stereotype.Repository @Repository}、
	 * {@link org.springframework.stereotype.Service @Service} 和
	 * {@link org.springframework.stereotype.Controller @Controller} 原型注解的默认过滤器
	 * @param environment 在评估 Bean 定义配置元数据时使用的 Spring {@link Environment}
	 * @param resourceLoader 要使用的 {@link ResourceLoader}
	 * @since 4.3.6
	 */
	public ClassPathBeanDefinitionScanner(BeanDefinitionRegistry registry, boolean useDefaultFilters,
			Environment environment, @Nullable ResourceLoader resourceLoader) {

		Assert.notNull(registry, "BeanDefinitionRegistry must not be null");
		this.registry = registry;
		//注册默认的拦截器
		if (useDefaultFilters) {
			//为@Component 注册默认过滤器。这将隐式注册所有具有@Component 元注释的注释，包括@Repository、@Service 和@Controller 原型注释
			registerDefaultFilters();
		}
		setEnvironment(environment);
		setResourceLoader(resourceLoader);
	}


	/**
	 * 返回此扫描器操作的 BeanDefinitionRegistry。
	 */
	@Override
	public final BeanDefinitionRegistry getRegistry() {
		return this.registry;
	}

	/**
	 * 设置检测到的 Bean 所使用的默认值。
	 * @see BeanDefinitionDefaults
	 */
	public void setBeanDefinitionDefaults(@Nullable BeanDefinitionDefaults beanDefinitionDefaults) {
		this.beanDefinitionDefaults =
				(beanDefinitionDefaults != null ? beanDefinitionDefaults : new BeanDefinitionDefaults());
	}

	/**
	 * 返回检测到的 Bean 所使用的默认值（永不为 {@code null}）。
	 * @since 4.1
	 */
	public BeanDefinitionDefaults getBeanDefinitionDefaults() {
		return this.beanDefinitionDefaults;
	}

	/**
	 * 设置用于确定自动注入候选者的名称匹配模式。
	 * @param autowireCandidatePatterns 要匹配的模式
	 */
	public void setAutowireCandidatePatterns(@Nullable String... autowireCandidatePatterns) {
		this.autowireCandidatePatterns = autowireCandidatePatterns;
	}

	/**
	 * 设置用于检测到的 Bean 类的 BeanNameGenerator。
	 * <p>默认是 {@link AnnotationBeanNameGenerator}。
	 */
	public void setBeanNameGenerator(@Nullable BeanNameGenerator beanNameGenerator) {
		this.beanNameGenerator =
				(beanNameGenerator != null ? beanNameGenerator : AnnotationBeanNameGenerator.INSTANCE);
	}

	/**
	 * 设置用于检测到的 Bean 类的 ScopeMetadataResolver。
	 * 注意，这将覆盖任何自定义的 "scopedProxyMode" 设置。
	 * <p>默认是 {@link AnnotationScopeMetadataResolver}。
	 * @see #setScopedProxyMode
	 */
	public void setScopeMetadataResolver(@Nullable ScopeMetadataResolver scopeMetadataResolver) {
		this.scopeMetadataResolver =
				(scopeMetadataResolver != null ? scopeMetadataResolver : new AnnotationScopeMetadataResolver());
	}

	/**
	 * 指定非单例作用域 Bean 的代理行为。
	 * 注意，这将覆盖任何自定义的 "scopeMetadataResolver" 设置。
	 * <p>默认是 {@link ScopedProxyMode#NO}。
	 * @see #setScopeMetadataResolver
	 */
	public void setScopedProxyMode(ScopedProxyMode scopedProxyMode) {
		this.scopeMetadataResolver = new AnnotationScopeMetadataResolver(scopedProxyMode);
	}

	/**
	 * 指定是否注册注解配置后处理器。
	 * <p>默认是注册后处理器。关闭此项可以忽略注解或以不同方式处理它们。
	 */
	public void setIncludeAnnotationConfig(boolean includeAnnotationConfig) {
		this.includeAnnotationConfig = includeAnnotationConfig;
	}


	/**
	 * 在指定的基础包内执行扫描。
	 * @param basePackages 要检查带注解类的包
	 * @return 注册的 Bean 数量
	 */
	public int scan(String... basePackages) {
		int beanCountAtScanStart = this.registry.getBeanDefinitionCount();

		doScan(basePackages);

		// 如果需要，注册注解配置处理器。
		if (this.includeAnnotationConfig) {
			AnnotationConfigUtils.registerAnnotationConfigProcessors(this.registry);
		}

		return (this.registry.getBeanDefinitionCount() - beanCountAtScanStart);
	}

	/**
	 * 在指定的基础包内执行扫描，返回注册的 Bean 定义。
	 * <p>此方法<i>不</i>注册注解配置处理器，而是将其留给调用者处理。
	 * @param basePackages 要检查带注解类的包
	 * @return 注册的 Bean 定义集合，用于工具注册目的（永不为 {@code null}）
	 */
	protected Set<BeanDefinitionHolder> doScan(String... basePackages) {
		Assert.notEmpty(basePackages, "At least one base package must be specified");

		Set<BeanDefinitionHolder> beanDefinitions = new LinkedHashSet<>();

		// 遍历所有基础扫描包
		for (String basePackage : basePackages) {
			// 查找包中的候选组件
			Set<BeanDefinition> candidates = findCandidateComponents(basePackage);

			// 处理每个候选组件
			for (BeanDefinition candidate : candidates) {
				// 解析作用域元数据并设置作用域
				ScopeMetadata scopeMetadata = this.scopeMetadataResolver.resolveScopeMetadata(candidate);
				candidate.setScope(scopeMetadata.getScopeName());

				// 生成 Bean 名称
				String beanName = this.beanNameGenerator.generateBeanName(candidate, this.registry);

				// 如果是抽象 Bean 定义，进行后处理
				if (candidate instanceof AbstractBeanDefinition) {
					postProcessBeanDefinition((AbstractBeanDefinition) candidate, beanName);
				}

				// 如果是注解 Bean 定义，处理通用定义注解
				if (candidate instanceof AnnotatedBeanDefinition) {
					AnnotationConfigUtils.processCommonDefinitionAnnotations((AnnotatedBeanDefinition) candidate);
				}

				// 检查候选 Bean 是否可以注册
				if (checkCandidate(beanName, candidate)) {
					// 创建 Bean 定义持有者
					BeanDefinitionHolder definitionHolder = new BeanDefinitionHolder(candidate, beanName);

					// 应用作用域代理模式
					definitionHolder =
							AnnotationConfigUtils.applyScopedProxyMode(scopeMetadata, definitionHolder, this.registry);

					// 添加到 Bean 定义集合
					beanDefinitions.add(definitionHolder);

					// 注册 Bean 定义到注册表
					registerBeanDefinition(definitionHolder, this.registry);
				}
			}
		}

		// 返回所有扫描到的 Bean 定义
		return beanDefinitions;
	}

	/**
	 * 将进一步的设置应用到给定的 Bean 定义，这些设置超出了从扫描组件类中检索到的内容。
	 * @param beanDefinition 扫描得到的 Bean 定义
	 * @param beanName 为给定 Bean 生成的 Bean 名称
	 */
	protected void postProcessBeanDefinition(AbstractBeanDefinition beanDefinition, String beanName) {
		beanDefinition.applyDefaults(this.beanDefinitionDefaults);
		if (this.autowireCandidatePatterns != null) {
			beanDefinition.setAutowireCandidate(PatternMatchUtils.simpleMatch(this.autowireCandidatePatterns, beanName));
		}
	}

	/**
	 * 使用给定的注册表注册指定的 Bean。
	 * <p>可以在子类中重写，例如用于调整注册过程或为每个扫描到的 Bean 注册额外的 Bean 定义。
	 * @param definitionHolder Bean 的定义加上 Bean 名称
	 * @param registry 要注册 Bean 的 BeanDefinitionRegistry
	 */
	protected void registerBeanDefinition(BeanDefinitionHolder definitionHolder, BeanDefinitionRegistry registry) {
		BeanDefinitionReaderUtils.registerBeanDefinition(definitionHolder, registry);
	}


	/**
	 * 检查给定候选者的 Bean 名称，确定相应的 Bean 定义是否需要注册或是否与现有定义冲突。
	 * @param beanName 建议的 Bean 名称
	 * @param beanDefinition 相应的 Bean 定义
	 * @return {@code true} 表示 Bean 可以按原样注册；{@code false} 表示应跳过，
	 * 因为指定名称存在兼容的现有 Bean 定义
	 * @throws ConflictingBeanDefinitionException 如果指定名称存在不兼容的现有 Bean 定义
	 */
	protected boolean checkCandidate(String beanName, BeanDefinition beanDefinition) throws IllegalStateException {
		if (!this.registry.containsBeanDefinition(beanName)) {
			return true;
		}
		BeanDefinition existingDef = this.registry.getBeanDefinition(beanName);
		BeanDefinition originatingDef = existingDef.getOriginatingBeanDefinition();
		if (originatingDef != null) {
			existingDef = originatingDef;
		}
		if (isCompatible(beanDefinition, existingDef)) {
			return false;
		}
		throw new ConflictingBeanDefinitionException("Annotation-specified bean name '" + beanName +
				"' for bean class [" + beanDefinition.getBeanClassName() + "] conflicts with existing, " +
				"non-compatible bean definition of same name and class [" + existingDef.getBeanClassName() + "]");
	}

	/**
	 * 确定给定的新 Bean 定义是否与给定的现有 Bean 定义兼容。
	 * <p>默认实现认为当现有 Bean 定义来自同一来源或来自非扫描来源时，它们是兼容的。
	 * @param newDefinition 新的 Bean 定义，来源于扫描
	 * @param existingDefinition 现有的 Bean 定义，可能是显式定义的或之前扫描生成的
	 * @return 这些定义是否被视为兼容，新定义将被跳过而保留现有定义
	 */
	protected boolean isCompatible(BeanDefinition newDefinition, BeanDefinition existingDefinition) {
		return (!(existingDefinition instanceof ScannedGenericBeanDefinition) ||  // 显式注册的覆盖 Bean
				(newDefinition.getSource() != null && newDefinition.getSource().equals(existingDefinition.getSource())) ||  // 同一文件被扫描两次
				newDefinition.equals(existingDefinition));  // 同一个等效类被扫描两次
	}


	/**
	 * 如果可能，从给定的注册表获取 Environment，否则返回新的 StandardEnvironment。
	 */
	private static Environment getOrCreateEnvironment(BeanDefinitionRegistry registry) {
		Assert.notNull(registry, "BeanDefinitionRegistry must not be null");
		if (registry instanceof EnvironmentCapable) {
			return ((EnvironmentCapable) registry).getEnvironment();
		}
		return new StandardEnvironment();
	}

}
