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

package org.springframework.context.support;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionCustomizer;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.metrics.ApplicationStartup;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * 通用的 ApplicationContext 实现，包含一个内部的 {@link org.springframework.beans.factory.support.DefaultListableBeanFactory}
 * 实例，不假设特定的 Bean 定义格式。实现了 {@link org.springframework.beans.factory.support.BeanDefinitionRegistry}
 * 接口，以允许将任何 Bean 定义读取器应用于它。
 *
 * <p>典型的用法是通过 {@link org.springframework.beans.factory.support.BeanDefinitionRegistry}
 * 接口注册各种 Bean 定义，然后调用 {@link #refresh()} 以使用应用程序上下文语义初始化这些 Bean
 * （处理 {@link org.springframework.context.ApplicationContextAware}，自动检测
 * {@link org.springframework.beans.factory.config.BeanFactoryPostProcessor BeanFactoryPostProcessors} 等）。
 *
 * <p>与其他每次刷新都会创建一个新的内部 BeanFactory 实例的 ApplicationContext 实现不同，
 * 该上下文的内部 BeanFactory 从一开始就是可用的，以便能够在其上注册 Bean 定义。只能调用一次 {@link #refresh()}。
 *
 * <p>使用示例：
 *
 * <pre class="code">
 * GenericApplicationContext ctx = new GenericApplicationContext();
 * XmlBeanDefinitionReader xmlReader = new XmlBeanDefinitionReader(ctx);
 * xmlReader.loadBeanDefinitions(new ClassPathResource("applicationContext.xml"));
 * PropertiesBeanDefinitionReader propReader = new PropertiesBeanDefinitionReader(ctx);
 * propReader.loadBeanDefinitions(new ClassPathResource("otherBeans.properties"));
 * ctx.refresh();
 *
 * MyBean myBean = (MyBean) ctx.getBean("myBean");
 * ...</pre>
 * <p>
 * 对于典型的 XML Bean 定义情况，只需使用 {@link ClassPathXmlApplicationContext}
 * 或 {@link FileSystemXmlApplicationContext}，这些更容易设置 - 但更不灵活，因为您只能对 XML
 * Bean 定义使用标准资源位置，而不是混合任意 Bean 定义格式。在 Web 环境中的等效部分是
 * {@link org.springframework.web.context.support.XmlWebApplicationContext}。
 * <p>
 * 对于那些应以可刷新的方式读取特殊 Bean 定义格式的自定义应用程序上下文实现，
 * 考虑从 {@link AbstractRefreshableApplicationContext} 基类派生。
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @see #registerBeanDefinition
 * @see #refresh()
 * @see org.springframework.beans.factory.xml.XmlBeanDefinitionReader
 * @see org.springframework.beans.factory.support.PropertiesBeanDefinitionReader
 * @since 1.1.2
 */
public class GenericApplicationContext extends AbstractApplicationContext implements BeanDefinitionRegistry {

	/**
	 * Bean工厂
	 */
	private final DefaultListableBeanFactory beanFactory;

	/**
	 * 资源加载器
	 */
	@Nullable
	private ResourceLoader resourceLoader;

	/**
	 * 是否是自定义类加载器
	 */
	private boolean customClassLoader = false;

	/**
	 * 是否刷新bean工厂
	 */
	private final AtomicBoolean refreshed = new AtomicBoolean();


	/**
	 * 创建一个新的 GenericApplicationContext。
	 *
	 * @see #registerBeanDefinition
	 * @see #refresh
	 */
	public GenericApplicationContext() {
		this.beanFactory = new DefaultListableBeanFactory();
	}

	/**
	 * 使用给定的 DefaultListableBeanFactory 创建一个新的 GenericApplicationContext。
	 *
	 * @param beanFactory 用于此上下文的 DefaultListableBeanFactory 实例
	 * @see #registerBeanDefinition
	 * @see #refresh
	 */
	public GenericApplicationContext(DefaultListableBeanFactory beanFactory) {
		Assert.notNull(beanFactory, "BeanFactory must not be null");
		this.beanFactory = beanFactory;
	}

	/**
	 * 使用给定的父级创建一个新的 GenericApplicationContext。
	 *
	 * @param parent 父级应用程序上下文
	 * @see #registerBeanDefinition
	 * @see #refresh
	 */
	public GenericApplicationContext(@Nullable ApplicationContext parent) {
		this();
		setParent(parent);
	}

	/**
	 * 使用给定的 DefaultListableBeanFactory 和父级创建一个新的 GenericApplicationContext。
	 *
	 * @param beanFactory 用于此上下文的 DefaultListableBeanFactory 实例
	 * @param parent      父级应用程序上下文
	 * @see #registerBeanDefinition
	 * @see #refresh
	 */
	public GenericApplicationContext(DefaultListableBeanFactory beanFactory, ApplicationContext parent) {
		this(beanFactory);
		setParent(parent);
	}


	/**
	 * 设置此应用程序上下文的父级，同时相应地设置内部 BeanFactory 的父级。
	 *
	 * @see org.springframework.beans.factory.config.ConfigurableBeanFactory#setParentBeanFactory
	 */
	@Override
	public void setParent(@Nullable ApplicationContext parent) {
		super.setParent(parent);
		this.beanFactory.setParentBeanFactory(getInternalParentBeanFactory());
	}

	@Override
	public void setApplicationStartup(ApplicationStartup applicationStartup) {
		super.setApplicationStartup(applicationStartup);
		this.beanFactory.setApplicationStartup(applicationStartup);
	}

	/**
	 * 设置是否允许通过注册具有相同名称的不同定义来覆盖bean定义，自动替换前者。
	 * 如果不允许，则会抛出异常。默认值为 "true"。
	 *
	 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory#setAllowBeanDefinitionOverriding
	 * @since 3.0
	 */
	public void setAllowBeanDefinitionOverriding(boolean allowBeanDefinitionOverriding) {
		this.beanFactory.setAllowBeanDefinitionOverriding(allowBeanDefinitionOverriding);
	}

	/**
	 * 设置是否允许bean之间的循环引用，并自动尝试解决它们。
	 * <p>默认值为 "true"。关闭此选项以在遇到循环引用时抛出异常，完全禁止循环引用。
	 *
	 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory#setAllowCircularReferences
	 * @since 3.0
	 */
	public void setAllowCircularReferences(boolean allowCircularReferences) {
		this.beanFactory.setAllowCircularReferences(allowCircularReferences);
	}

	/**
	 * 设置用于此上下文的ResourceLoader。如果设置了，上下文将所有的 {@code getResource} 调用委托给给定的ResourceLoader。
	 * 如果未设置，则将应用默认的资源加载。
	 * <p>指定自定义ResourceLoader的主要原因是以特定方式解析资源路径（没有URL前缀）。
	 * 默认行为是将这些路径解析为类路径位置。要将资源路径解析为文件系统位置，请在这里指定FileSystemResourceLoader。
	 * <p>还可以传递一个完整的ResourcePatternResolver，上下文将自动检测并用于 {@code getResources} 调用。否则，将应用默认的资源模式匹配。
	 *
	 * @see #getResource
	 * @see org.springframework.core.io.DefaultResourceLoader
	 * @see org.springframework.core.io.FileSystemResourceLoader
	 * @see org.springframework.core.io.support.ResourcePatternResolver
	 * @see #getResources
	 */
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}


	//---------------------------------------------------------------------
	// ResourceLoader / ResourcePatternResolver override if necessary
	//---------------------------------------------------------------------

	/**
	 * 此实现如果设置了上下文的ResourceLoader，则委托给上下文的ResourceLoader，否则回退到默认的超类行为。
	 *
	 * @see #setResourceLoader
	 */
	@Override
	public Resource getResource(String location) {
		if (this.resourceLoader != null) {
			return this.resourceLoader.getResource(location);
		}
		return super.getResource(location);
	}

	/**
	 * 此实现如果ResourceLoader实现了ResourcePatternResolver接口，则委托给上下文的ResourceLoader，
	 * 否则回退到默认的超类行为。
	 *
	 * @see #setResourceLoader
	 */
	@Override
	public Resource[] getResources(String locationPattern) throws IOException {
		// 检查资源加载器是否是资源模式解析器
		if (this.resourceLoader instanceof ResourcePatternResolver) {
			// 如果是资源模式解析器，则使用其解析指定位置模式的资源
			return ((ResourcePatternResolver) this.resourceLoader).getResources(locationPattern);
		}

		// 如果资源加载器不是资源模式解析器，则调用父类的方法获取资源
		return super.getResources(locationPattern);
	}

	@Override
	public void setClassLoader(@Nullable ClassLoader classLoader) {
		super.setClassLoader(classLoader);
		this.customClassLoader = true;
	}

	@Override
	@Nullable
	public ClassLoader getClassLoader() {
		if (this.resourceLoader != null && !this.customClassLoader) {
			return this.resourceLoader.getClassLoader();
		}
		return super.getClassLoader();
	}


	//---------------------------------------------------------------------
	// Implementations of AbstractApplicationContext's template methods
	//---------------------------------------------------------------------

	/**
	 * 什么也不做：我们持有一个内部的BeanFactory，并依赖调用者通过我们的公共方法（或BeanFactory的方法）注册bean。
	 *
	 * @see #registerBeanDefinition
	 */
	@Override
	protected final void refreshBeanFactory() throws IllegalStateException {
		if (!this.refreshed.compareAndSet(false, true)) {
			throw new IllegalStateException(
					"GenericApplicationContext does not support multiple refresh attempts: just call 'refresh' once");
		}
		this.beanFactory.setSerializationId(getId());
	}

	@Override
	protected void cancelRefresh(BeansException ex) {
		this.beanFactory.setSerializationId(null);
		super.cancelRefresh(ex);
	}

	/**
	 * 没有太多要做的事情：我们持有一个永远不会被释放的内部BeanFactory。
	 */
	@Override
	protected final void closeBeanFactory() {
		this.beanFactory.setSerializationId(null);
	}

	/**
	 * 返回由此上下文持有的单个内部BeanFactory（作为ConfigurableListableBeanFactory）。
	 */
	@Override
	public final ConfigurableListableBeanFactory getBeanFactory() {
		return this.beanFactory;
	}

	/**
	 * 返回此上下文的底层Bean工厂，可用于注册Bean定义。
	 * <p><b>注意：</b>您需要调用{@link #refresh()}来初始化
	 * Bean工厂及其包含的Bean，以应用上下文语义（自动检测BeanFactoryPostProcessors等）。
	 *
	 * @return 内部Bean工厂（作为DefaultListableBeanFactory）
	 */
	public final DefaultListableBeanFactory getDefaultListableBeanFactory() {
		return this.beanFactory;
	}

	@Override
	public AutowireCapableBeanFactory getAutowireCapableBeanFactory() throws IllegalStateException {
		assertBeanFactoryActive();
		return this.beanFactory;
	}


	//---------------------------------------------------------------------
	// Implementation of BeanDefinitionRegistry
	//---------------------------------------------------------------------

	@Override
	public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition)
			throws BeanDefinitionStoreException {

		this.beanFactory.registerBeanDefinition(beanName, beanDefinition);
	}

	@Override
	public void removeBeanDefinition(String beanName) throws NoSuchBeanDefinitionException {
		this.beanFactory.removeBeanDefinition(beanName);
	}

	@Override
	public BeanDefinition getBeanDefinition(String beanName) throws NoSuchBeanDefinitionException {
		return this.beanFactory.getBeanDefinition(beanName);
	}

	@Override
	public boolean isBeanNameInUse(String beanName) {
		return this.beanFactory.isBeanNameInUse(beanName);
	}

	@Override
	public void registerAlias(String beanName, String alias) {
		this.beanFactory.registerAlias(beanName, alias);
	}

	@Override
	public void removeAlias(String alias) {
		this.beanFactory.removeAlias(alias);
	}

	@Override
	public boolean isAlias(String beanName) {
		return this.beanFactory.isAlias(beanName);
	}


	//---------------------------------------------------------------------
	// 注册单个bean的便捷方法
	//---------------------------------------------------------------------

	/**
	 * 注册给定Bean类的Bean，可选择为自动装配过程提供显式构造函数参数。
	 *
	 * @param beanClass       Bean的类
	 * @param constructorArgs 提供给Spring构造函数解析算法的自定义参数值，
	 *                        可以解析所有参数或仅解析特定参数，其余参数将通过常规自动装配解析（可以为null或空）
	 * @since 5.2（在AnnotationConfigApplicationContext子类上自5.0起）
	 */
	public <T> void registerBean(Class<T> beanClass, Object... constructorArgs) {
		registerBean(null, beanClass, constructorArgs);
	}

	/**
	 * 注册给定Bean类的Bean，可选择为自动装配过程提供显式构造函数参数。
	 *
	 * @param beanName        Bean的名称（可以为null）
	 * @param beanClass       Bean的类
	 * @param constructorArgs 提供给Spring构造函数解析算法的自定义参数值，
	 *                        可以解析所有参数或仅解析特定参数，其余参数将通过常规自动装配解析（可以为null或空）
	 * @since 5.2（在AnnotationConfigApplicationContext子类上自5.0起）
	 */
	public <T> void registerBean(@Nullable String beanName, Class<T> beanClass, Object... constructorArgs) {
		registerBean(beanName, beanClass, (Supplier<T>) null,
				bd -> {
					for (Object arg : constructorArgs) {
						// bean定义中的构造参数添加当前参数值
						bd.getConstructorArgumentValues().addGenericArgumentValue(arg);
					}
				});
	}

	/**
	 * 从给定的Bean类注册一个Bean，可选择自定义其Bean定义元数据（通常声明为lambda表达式）。
	 *
	 * @param beanClass   Bean的类（解析为要自动装配的公共构造函数，可能只是默认构造函数）
	 * @param customizers 用于自定义工厂的{@link BeanDefinition}的一个或多个回调，例如设置lazy-init或primary标志
	 * @see #registerBean(String, Class, Supplier, BeanDefinitionCustomizer...)
	 * @since 5.0
	 */
	public final <T> void registerBean(Class<T> beanClass, BeanDefinitionCustomizer... customizers) {
		registerBean(null, beanClass, null, customizers);
	}

	/**
	 * 从给定的Bean类注册一个Bean，可选择自定义其Bean定义元数据（通常声明为lambda表达式）。
	 *
	 * @param beanName    Bean的名称（可以为{@code null}）
	 * @param beanClass   Bean的类（解析为要自动装配的公共构造函数，可能只是默认构造函数）
	 * @param customizers 用于自定义工厂的{@link BeanDefinition}的一个或多个回调，例如设置lazy-init或primary标志
	 * @see #registerBean(String, Class, Supplier, BeanDefinitionCustomizer...)
	 * @since 5.0
	 */
	public final <T> void registerBean(
			@Nullable String beanName, Class<T> beanClass, BeanDefinitionCustomizer... customizers) {

		registerBean(beanName, beanClass, null, customizers);
	}

	/**
	 * 注册一个来自给定bean类的bean，使用提供新实例的回调（通常声明为lambda表达式或方法引用），
	 * 可选地自定义其bean定义元数据（同样通常声明为lambda表达式）。
	 *
	 * @param beanClass   bean的类
	 * @param supplier    用于创建bean实例的回调
	 * @param customizers 用于自定义工厂的{@link BeanDefinition}的一个或多个回调，
	 *                    例如设置lazy-init或primary标志
	 * @see #registerBean(String, Class, Supplier, BeanDefinitionCustomizer...)
	 * @since 5.0
	 */
	public final <T> void registerBean(
			Class<T> beanClass, Supplier<T> supplier, BeanDefinitionCustomizer... customizers) {

		registerBean(null, beanClass, supplier, customizers);
	}

	/**
	 * 注册一个来自给定bean类的bean，使用提供新实例的回调（通常声明为lambda表达式或方法引用），
	 * 可选地自定义其bean定义元数据（同样通常声明为lambda表达式）。
	 * <p>此方法可以被重写以适应所有{@code registerBean}方法的注册机制（因为它们都委托给这个方法）。
	 *
	 * @param beanName    bean的名称（可能为{@code null}）
	 * @param beanClass   bean的类
	 * @param supplier    用于创建bean实例的回调（如果为{@code null}，则解析一个要自动装配的公共构造函数）
	 * @param customizers 用于自定义工厂的{@link BeanDefinition}的一个或多个回调，
	 *                    例如设置lazy-init或primary标志
	 * @since 5.0
	 */
	public <T> void registerBean(@Nullable String beanName, Class<T> beanClass,
								 @Nullable Supplier<T> supplier, BeanDefinitionCustomizer... customizers) {

		// 创建一个基于类派生的Bean定义
		ClassDerivedBeanDefinition beanDefinition = new ClassDerivedBeanDefinition(beanClass);

		// 如果提供了供应商（supplier），则将其设置为Bean定义的实例供应商
		if (supplier != null) {
			beanDefinition.setInstanceSupplier(supplier);
		}

		// 对于每个自定义器，调用其customize方法以自定义Bean定义
		for (BeanDefinitionCustomizer customizer : customizers) {
			customizer.customize(beanDefinition);
		}

		// 确定要使用的Bean名称，如果提供了beanName，则使用提供的名称，否则使用类名
		String nameToUse = (beanName != null ? beanName : beanClass.getName());

		// 注册Bean定义
		registerBeanDefinition(nameToUse, beanDefinition);
	}


	/**
	 * {@link RootBeanDefinition} 标记子类，用于基于 {@code #registerBean} 的注册，并为公共构造函数提供灵活的自动查询。
	 */
	@SuppressWarnings("serial")
	private static class ClassDerivedBeanDefinition extends RootBeanDefinition {

		public ClassDerivedBeanDefinition(Class<?> beanClass) {
			super(beanClass);
		}

		public ClassDerivedBeanDefinition(ClassDerivedBeanDefinition original) {
			super(original);
		}

		@Override
		@Nullable
		public Constructor<?>[] getPreferredConstructors() {
			// 获取Bean的类
			Class<?> clazz = getBeanClass();

			// 查找主要构造函数
			Constructor<?> primaryCtor = BeanUtils.findPrimaryConstructor(clazz);

			// 如果找到主要构造函数，则返回包含该构造函数的数组
			if (primaryCtor != null) {
				return new Constructor<?>[]{primaryCtor};
			}

			// 如果没有主要构造函数，则获取所有公共构造函数
			Constructor<?>[] publicCtors = clazz.getConstructors();

			// 如果有公共构造函数，则返回包含这些构造函数的数组，否则返回null
			if (publicCtors.length > 0) {
				return publicCtors;
			}
			return null;
		}

		@Override
		public RootBeanDefinition cloneBeanDefinition() {
			return new ClassDerivedBeanDefinition(this);
		}
	}

}
