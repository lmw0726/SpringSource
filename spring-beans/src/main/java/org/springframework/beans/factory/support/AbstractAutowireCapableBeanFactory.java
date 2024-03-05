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

package org.springframework.beans.factory.support;

import org.apache.commons.logging.Log;
import org.springframework.beans.*;
import org.springframework.beans.factory.*;
import org.springframework.beans.factory.config.*;
import org.springframework.core.*;
import org.springframework.lang.Nullable;
import org.springframework.util.*;
import org.springframework.util.ReflectionUtils.MethodCallback;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

/**
 * 抽象的Bean工厂超类，实现了默认的Bean创建功能，具有RootBeanDefinition类指定的完整功能。
 * 除了AbstractBeanFactory的createBean方法之外，还实现了org.springframework.beans.factory.config.AutowireCapableBeanFactory接口。
 * <p>
 * 提供Bean的创建（包括构造函数解析）、属性填充、装配（包括自动装配）和初始化。处理运行时Bean引用、解析托管集合、调用初始化方法等。
 * 支持按名称、按类型自动装配构造函数和属性。
 * <p>
 * 子类需要实现的主要模板方法是resolveDependency(DependencyDescriptor, String, Set, TypeConverter)，
 * 用于按类型进行自动装配。在具有搜索Bean定义能力的工厂中，匹配的Bean通常通过这种搜索来实现。对于其他工厂样式，
 * 可以实现简化的匹配算法。
 * <p>
 * 请注意，此类不假设或实现Bean定义注册表功能。有关org.springframework.beans.factory.ListableBeanFactory和
 * BeanDefinitionRegistry接口的实现，请参见DefaultListableBeanFactory，它们分别表示此类工厂的API和SPI视图。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Mark Fisher
 * @author Costin Leau
 * @author Chris Beams
 * @author Sam Brannen
 * @author Phillip Webb
 * @see RootBeanDefinition
 * @see DefaultListableBeanFactory
 * @see BeanDefinitionRegistry
 * @since 13.02.2004
 */
public abstract class AbstractAutowireCapableBeanFactory extends AbstractBeanFactory
		implements AutowireCapableBeanFactory {

	/**
	 * 创建Bean实例的策略。
	 */
	private InstantiationStrategy instantiationStrategy;

	/**
	 * 方法参数名称的解析策略。
	 */
	@Nullable
	private ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

	/**
	 * 是否自动尝试解析Bean之间的循环引用。
	 */
	private boolean allowCircularReferences = true;

	/**
	 * 在发生循环引用的情况下是否采取注入原始Bean实例的策略，
	 * 即使最终注入的Bean已被包装。
	 */
	private boolean allowRawInjectionDespiteWrapping = false;

	/**
	 * 要在依赖性检查和自动装配时忽略的依赖类型，作为Class对象的Set：例如，String。默认为无。
	 */
	private final Set<Class<?>> ignoredDependencyTypes = new HashSet<>();

	/**
	 * 在依赖检查和自动装配时忽略的依赖接口，作为Class对象的集合。
	 * 默认情况下，仅忽略BeanFactory接口。
	 */
	private final Set<Class<?>> ignoredDependencyInterfaces = new HashSet<>();

	/**
	 * 当前创建的bean的名称，用于从用户指定的Supplier接口回调触发的getBean etc调用的隐式依赖注册。
	 */
	private final NamedThreadLocal<String> currentlyCreatedBean = new NamedThreadLocal<>("Currently created bean");

	/**
	 * 未完成的FactoryBean实例的缓存：FactoryBean名称到BeanWrapper的映射。
	 */
	private final ConcurrentMap<String, BeanWrapper> factoryBeanInstanceCache = new ConcurrentHashMap<>();

	/**
	 * 工厂方法类-工厂方法候选者Map
	 * 工厂类的候选工厂方法的缓存
	 */
	private final ConcurrentMap<Class<?>, Method[]> factoryMethodCandidateCache = new ConcurrentHashMap<>();

	/**
	 * 过滤后的属性描述符的缓存：Bean类到PropertyDescriptor数组的映射。
	 */
	private final ConcurrentMap<Class<?>, PropertyDescriptor[]> filteredPropertyDescriptorsCache =
			new ConcurrentHashMap<>();


	/**
	 * Create a new AbstractAutowireCapableBeanFactory.
	 */
	public AbstractAutowireCapableBeanFactory() {
		// 调用父类构造函数
		super();
		// 忽略依赖接口：BeanNameAware、BeanFactoryAware、BeanClassLoaderAware
		ignoreDependencyInterface(BeanNameAware.class);
		ignoreDependencyInterface(BeanFactoryAware.class);
		ignoreDependencyInterface(BeanClassLoaderAware.class);
		// 根据是否在Native Image环境下选择实例化策略
		if (NativeDetector.inNativeImage()) {
			// 在Native Image环境下使用简单实例化策略
			this.instantiationStrategy = new SimpleInstantiationStrategy();
		} else {
			// 否则使用Cglib子类化实例化策略
			this.instantiationStrategy = new CglibSubclassingInstantiationStrategy();
		}
	}

	/**
	 * 使用给定的父bean工厂创建一个新的AbstractAutowireCapableBeanFactory。
	 *
	 * @param parentBeanFactory 父bean工厂，如果没有则为{@code null}
	 */
	public AbstractAutowireCapableBeanFactory(@Nullable BeanFactory parentBeanFactory) {
		this();
		setParentBeanFactory(parentBeanFactory);
	}


	/**
	 * 设置用于创建bean实例的实例化策略。默认为CglibSubclassingInstantiationStrategy。
	 *
	 * @param instantiationStrategy 用于创建bean实例的实例化策略
	 * @see CglibSubclassingInstantiationStrategy
	 */
	public void setInstantiationStrategy(InstantiationStrategy instantiationStrategy) {
		this.instantiationStrategy = instantiationStrategy;
	}

	/**
	 * 返回用于创建bean实例的实例化策略。
	 *
	 * @return 用于创建bean实例的实例化策略
	 */
	protected InstantiationStrategy getInstantiationStrategy() {
		return this.instantiationStrategy;
	}

	/**
	 * 设置用于解析方法参数名称（如果需要）的ParameterNameDiscoverer。
	 * 默认为DefaultParameterNameDiscoverer。
	 *
	 * @param parameterNameDiscoverer 用于解析方法参数名称的ParameterNameDiscoverer
	 */
	public void setParameterNameDiscoverer(@Nullable ParameterNameDiscoverer parameterNameDiscoverer) {
		this.parameterNameDiscoverer = parameterNameDiscoverer;
	}

	/**
	 * 如果需要，返回用于解析方法参数名称的ParameterNameDiscoverer。
	 */
	@Nullable
	protected ParameterNameDiscoverer getParameterNameDiscoverer() {
		return this.parameterNameDiscoverer;
	}

	/**
	 * 设置是否允许bean之间存在循环引用，并自动尝试解决它们。
	 * <p>请注意，循环引用解析意味着涉及的其中一个bean将接收对另一个尚未完全初始化的bean的引用。
	 * 这可能会导致初始化的细微和不太显着的副作用；但对于许多场景，它确实能够很好地工作。
	 * <p>默认为"true"。将其关闭以在遇到循环引用时抛出异常，完全禁止它们。
	 * <p><b>注意：</b>通常建议不要依赖于bean之间的循环引用。重构应用程序逻辑，使涉及的两个bean委托给一个封装了它们共同逻辑的第三个bean。
	 *
	 * @param allowCircularReferences 是否允许bean之间存在循环引用
	 */
	public void setAllowCircularReferences(boolean allowCircularReferences) {
		this.allowCircularReferences = allowCircularReferences;
	}

	/**
	 * 返回是否允许bean之间存在循环引用。
	 *
	 * @return 是否允许bean之间存在循环引用
	 * @see #setAllowCircularReferences
	 * @since 5.3.10
	 */
	public boolean isAllowCircularReferences() {
		return this.allowCircularReferences;
	}

	/**
	 * 设置是否允许将bean实例原始注入到另一个bean的属性中，尽管注入的bean最终会被包装
	 * （例如，通过AOP自动代理）。
	 * <p>这仅在无法以其他方式解析循环引用的情况下作为最后的手段使用：
	 * 基本上，优先考虑原始实例被注入，而不是整个bean装配过程失败。
	 * <p>默认为"false"，从Spring 2.0开始。将其打开以允许将未包装的原始bean注入到您的一些引用中，
	 * 这是Spring 1.2的（可以说不太干净的）默认行为。
	 * <p><b>注意：</b>通常建议不要依赖于bean之间的循环引用，特别是涉及到自动代理时。
	 *
	 * @param allowRawInjectionDespiteWrapping 是否允许将bean实例原始注入
	 * @see #setAllowCircularReferences
	 */
	public void setAllowRawInjectionDespiteWrapping(boolean allowRawInjectionDespiteWrapping) {
		this.allowRawInjectionDespiteWrapping = allowRawInjectionDespiteWrapping;
	}

	/**
	 * 返回是否允许将bean实例原始注入。
	 *
	 * @return 是否允许将bean实例原始注入
	 * @see #setAllowRawInjectionDespiteWrapping
	 * @since 5.3.10
	 */
	public boolean isAllowRawInjectionDespiteWrapping() {
		return this.allowRawInjectionDespiteWrapping;
	}

	/**
	 * 忽略自动装配时给定的依赖类型：
	 * 例如，String。默认为无。
	 *
	 * @param type 要忽略的依赖类型
	 */
	public void ignoreDependencyType(Class<?> type) {
		this.ignoredDependencyTypes.add(type);
	}

	/**
	 * 忽略自动装配时给定的依赖接口。
	 * <p>这通常由应用程序上下文使用来注册通过其他方式解析的依赖项，如BeanFactory通过BeanFactoryAware或
	 * ApplicationContext通过ApplicationContextAware。
	 * <p>默认情况下，只有BeanFactoryAware接口被忽略。
	 * 要忽略更多类型，请为每个类型调用此方法。
	 *
	 * @param ifc 要忽略的依赖接口
	 * @see org.springframework.beans.factory.BeanFactoryAware
	 * @see org.springframework.context.ApplicationContextAware
	 */
	public void ignoreDependencyInterface(Class<?> ifc) {
		this.ignoredDependencyInterfaces.add(ifc);
	}

	@Override
	public void copyConfigurationFrom(ConfigurableBeanFactory otherFactory) {
		// 调用父类方法复制配置
		super.copyConfigurationFrom(otherFactory);
		// 如果otherFactory是AbstractAutowireCapableBeanFactory类型，则进行相应配置复制
		if (otherFactory instanceof AbstractAutowireCapableBeanFactory) {
			AbstractAutowireCapableBeanFactory otherAutowireFactory =
					(AbstractAutowireCapableBeanFactory) otherFactory;
			// 复制实例化策略
			this.instantiationStrategy = otherAutowireFactory.instantiationStrategy;
			// 复制是否允许循环引用的设置
			this.allowCircularReferences = otherAutowireFactory.allowCircularReferences;
			// 复制忽略的依赖类型列表
			this.ignoredDependencyTypes.addAll(otherAutowireFactory.ignoredDependencyTypes);
			// 复制忽略的依赖接口列表
			this.ignoredDependencyInterfaces.addAll(otherAutowireFactory.ignoredDependencyInterfaces);
		}
	}


	//-------------------------------------------------------------------------
	// Typical methods for creating and populating external bean instances
	//-------------------------------------------------------------------------

	@Override
	@SuppressWarnings("unchecked")
	public <T> T createBean(Class<T> beanClass) throws BeansException {
		// 使用原型（prototype）bean定义，以避免将bean注册为依赖bean。
		RootBeanDefinition bd = new RootBeanDefinition(beanClass);
		// 设置bean的作用域为原型（prototype）
		bd.setScope(SCOPE_PROTOTYPE);
		bd.allowCaching = ClassUtils.isCacheSafe(beanClass, getBeanClassLoader());
		// 调用createBean方法创建bean实例
		return (T) createBean(beanClass.getName(), bd, null);
	}

	@Override
	public void autowireBean(Object existingBean) {
		// 使用非单例（non-singleton）bean定义，以避免将bean注册为依赖bean。
		RootBeanDefinition bd = new RootBeanDefinition(ClassUtils.getUserClass(existingBean));
		// 设置bean的作用域为原型（prototype）
		bd.setScope(SCOPE_PROTOTYPE);
		bd.allowCaching = ClassUtils.isCacheSafe(bd.getBeanClass(), getBeanClassLoader());
		// 创建bean包租昂起
		BeanWrapper bw = new BeanWrapperImpl(existingBean);
		// 初始化bean包装器
		initBeanWrapper(bw);
		// 填充属性
		populateBean(bd.getBeanClass().getName(), bd, bw);
	}

	@Override
	public Object configureBean(Object existingBean, String beanName) throws BeansException {
		// 将bean标记为已创建
		markBeanAsCreated(beanName);
		// 获取合并的bean定义
		BeanDefinition mbd = getMergedBeanDefinition(beanName);
		// 创建根bean定义
		RootBeanDefinition bd = null;
		if (mbd instanceof RootBeanDefinition) {
			// 如果合并bean定义是根bean定义
			RootBeanDefinition rbd = (RootBeanDefinition) mbd;
			//如果是原型bean则使用rbd，否则使用它的克隆作为根定义
			bd = (rbd.isPrototype() ? rbd : rbd.cloneBeanDefinition());
		}
		if (bd == null) {
			bd = new RootBeanDefinition(mbd);
		}
		// 如果不是原型（prototype），则将其设置为原型作用域
		if (!bd.isPrototype()) {
			bd.setScope(SCOPE_PROTOTYPE);
			// 允许缓存，如果bean类是缓存安全的
			bd.allowCaching = ClassUtils.isCacheSafe(ClassUtils.getUserClass(existingBean), getBeanClassLoader());
		}
		// 使用现有的bean实例创建BeanWrapper
		BeanWrapper bw = new BeanWrapperImpl(existingBean);
		// 初始化BeanWrapper
		initBeanWrapper(bw);
		// 填充bean属性
		populateBean(beanName, bd, bw);
		// 初始化bean并返回
		return initializeBean(beanName, existingBean, bd);
	}


	//-------------------------------------------------------------------------
	// Specialized methods for fine-grained control over the bean lifecycle
	//-------------------------------------------------------------------------

	@Override
	public Object createBean(Class<?> beanClass, int autowireMode, boolean dependencyCheck) throws BeansException {
		// 使用非单例的 bean 定义，以避免将 bean 注册为依赖 bean
		RootBeanDefinition bd = new RootBeanDefinition(beanClass, autowireMode, dependencyCheck);
		bd.setScope(SCOPE_PROTOTYPE);
		// 创建并返回 bean 实例
		return createBean(beanClass.getName(), bd, null);
	}

	@Override
	public Object autowire(Class<?> beanClass, int autowireMode, boolean dependencyCheck) throws BeansException {
		// 使用非单例的 bean 定义，以避免将 bean 注册为依赖 bean
		RootBeanDefinition bd = new RootBeanDefinition(beanClass, autowireMode, dependencyCheck);
		bd.setScope(SCOPE_PROTOTYPE);

		// 如果使用构造器自动装配模式，则使用自动装配构造器创建 bean 实例
		if (bd.getResolvedAutowireMode() == AUTOWIRE_CONSTRUCTOR) {
			return autowireConstructor(beanClass.getName(), bd, null, null).getWrappedInstance();
		} else {
			Object bean;
			// 使用特权代码块来实例化 bean
			if (System.getSecurityManager() != null) {
				bean = AccessController.doPrivileged(
						(PrivilegedAction<Object>) () -> getInstantiationStrategy().instantiate(bd, null, this),
						getAccessControlContext());
			} else {
				bean = getInstantiationStrategy().instantiate(bd, null, this);
			}
			// 填充 bean 的属性值
			populateBean(beanClass.getName(), bd, new BeanWrapperImpl(bean));
			return bean;
		}
	}

	@Override
	public void autowireBeanProperties(Object existingBean, int autowireMode, boolean dependencyCheck)
			throws BeansException {

		if (autowireMode == AUTOWIRE_CONSTRUCTOR) {
			throw new IllegalArgumentException("AUTOWIRE_CONSTRUCTOR not supported for existing bean instance");
		}

		// 使用非单例的 bean 定义，以避免将 bean 注册为依赖 bean
		RootBeanDefinition bd =
				new RootBeanDefinition(ClassUtils.getUserClass(existingBean), autowireMode, dependencyCheck);
		bd.setScope(SCOPE_PROTOTYPE);

		// 使用现有的 bean 实例创建并初始化 BeanWrapper
		BeanWrapper bw = new BeanWrapperImpl(existingBean);
		initBeanWrapper(bw);

		// 填充 bean 的属性值
		populateBean(bd.getBeanClass().getName(), bd, bw);
	}

	@Override
	public void applyBeanPropertyValues(Object existingBean, String beanName) throws BeansException {
		// 将 bean 标记为已创建
		markBeanAsCreated(beanName);

		// 获取合并的 bean 定义
		BeanDefinition bd = getMergedBeanDefinition(beanName);

		// 使用现有的 bean 实例创建并初始化 BeanWrapper
		BeanWrapper bw = new BeanWrapperImpl(existingBean);
		initBeanWrapper(bw);

		// 应用属性值到 bean 实例
		applyPropertyValues(beanName, bd, bw, bd.getPropertyValues());
	}

	@Override
	public Object initializeBean(Object existingBean, String beanName) {
		return initializeBean(beanName, existingBean, null);
	}

	/**
	 * 在初始化之前应用Bean后置处理器。
	 *
	 * @param existingBean 要初始化的现有Bean实例
	 * @param beanName     Bean的名称
	 * @return 经过处理后的Bean实例
	 * @throws BeansException 如果在处理期间发生错误
	 */
	@Override
	public Object applyBeanPostProcessorsBeforeInitialization(Object existingBean, String beanName)
			throws BeansException {

		// 对于已存在的bean，遍历所有的BeanPostProcessor，调用postProcessBeforeInitialization方法
		Object result = existingBean;
		for (BeanPostProcessor processor : getBeanPostProcessors()) {
			// 调用postProcessBeforeInitialization方法，并获取处理后的对象
			Object current = processor.postProcessBeforeInitialization(result, beanName);
			// 如果处理后的对象为null，则返回原始结果
			if (current == null) {
				return result;
			}
			// 将处理后的对象赋值给result，继续下一个BeanPostProcessor的处理
			result = current;
		}
		// 返回最终处理后的结果
		return result;
	}

	/**
	 * 在初始化之后应用Bean后置处理器。
	 *
	 * @param existingBean 要初始化的现有Bean实例
	 * @param beanName     Bean的名称
	 * @return 经过处理后的Bean实例
	 * @throws BeansException 如果在处理期间发生错误
	 */
	@Override
	public Object applyBeanPostProcessorsAfterInitialization(Object existingBean, String beanName)
			throws BeansException {

		Object result = existingBean;
		//遍历已注册的BeanPostProcessor实现类
		for (BeanPostProcessor processor : getBeanPostProcessors()) {
			//进行后置处理
			Object current = processor.postProcessAfterInitialization(result, beanName);
			if (current == null) {
				//如果后置处理的实例为空，返回未处理前的对象。
				return result;
			}
			//否则将bean实例重置为处理后的对象。
			result = current;
		}
		return result;
	}

	@Override
	public void destroyBean(Object existingBean) {
		new DisposableBeanAdapter(
				existingBean, getBeanPostProcessorCache().destructionAware, getAccessControlContext()).destroy();
	}


	//-------------------------------------------------------------------------
	// Delegate methods for resolving injection points
	//-------------------------------------------------------------------------

	@Override
	public Object resolveBeanByName(String name, DependencyDescriptor descriptor) {
		// 保存之前的注入点，设置当前注入点为指定的描述符
		InjectionPoint previousInjectionPoint = ConstructorResolver.setCurrentInjectionPoint(descriptor);
		try {
			// 获取具有指定依赖类型的 bean
			return getBean(name, descriptor.getDependencyType());
		} finally {
			// 恢复之前的注入点
			ConstructorResolver.setCurrentInjectionPoint(previousInjectionPoint);
		}
	}

	@Override
	@Nullable
	public Object resolveDependency(DependencyDescriptor descriptor, @Nullable String requestingBeanName) throws BeansException {
		return resolveDependency(descriptor, requestingBeanName, null, null);
	}


	//---------------------------------------------------------------------
	// Implementation of relevant AbstractBeanFactory template methods
	//---------------------------------------------------------------------

	/**
	 * 此类的中心方法: 创建bean实例，填充bean实例，应用后处理器等。
	 *
	 * @see #doCreateBean
	 */
	@Override
	protected Object createBean(String beanName, RootBeanDefinition mbd, @Nullable Object[] args)
			throws BeanCreationException {

		if (logger.isTraceEnabled()) {
			logger.trace("Creating instance of bean '" + beanName + "'");
		}
		RootBeanDefinition mbdToUse = mbd;

		//确保此时实际解析了 bean 类，并克隆 bean 定义以防动态解析的 Class 无法存储在共享的合并 bean 定义中。
		//解析bean类
		Class<?> resolvedClass = resolveBeanClass(mbd, beanName);
		if (resolvedClass != null && !mbd.hasBeanClass() && mbd.getBeanClassName() != null) {
			//如果解析出来了bean类，并且bean定义中的beanClass也是Class对象，且bean定义存在bean类名
			//复制bean定义，并设置该bean类
			mbdToUse = new RootBeanDefinition(mbd);
			mbdToUse.setBeanClass(resolvedClass);
		}

		// 准备方法覆盖。
		try {
			//检查所有覆盖方法中，每个覆盖方法中，指定类中的覆盖方法是否存在
			mbdToUse.prepareMethodOverrides();
		} catch (BeanDefinitionValidationException ex) {
			throw new BeanDefinitionStoreException(mbdToUse.getResourceDescription(),
					beanName, "Validation of method overrides failed", ex);
		}

		try {
			//让 BeanPostProcessors 有机会返回一个代理而不是目标 bean 实例。
			Object bean = resolveBeforeInstantiation(beanName, mbdToUse);
			if (bean != null) {
				return bean;
			}
		} catch (Throwable ex) {
			throw new BeanCreationException(mbdToUse.getResourceDescription(), beanName,
					"BeanPostProcessor before instantiation of bean failed", ex);
		}

		try {
			// 创建 bean 实例
			Object beanInstance = doCreateBean(beanName, mbdToUse, args);
			// 如果启用了跟踪日志，则记录 bean 实例创建完成的消息
			if (logger.isTraceEnabled()) {
				logger.trace("Finished creating instance of bean '" + beanName + "'");
			}
			return beanInstance;
		} catch (BeanCreationException | ImplicitlyAppearedSingletonException ex) {
			// 如果之前检测到有适当的 bean 创建上下文的异常，
			// 或者发现非法的单例状态需要通知给 DefaultSingletonBeanRegistry，则直接抛出异常
			throw ex;
		} catch (Throwable ex) {
			// 捕获并处理异常，将其封装为 BeanCreationException 并抛出
			throw new BeanCreationException(
					mbdToUse.getResourceDescription(), beanName, "Unexpected exception during bean creation", ex);
		}
	}

	/**
	 * 实际创建指定的 bean。在此时已经发生了预创建处理，例如检查 {@code postProcessBeforeInstantiation} 回调。
	 * <p>在默认 bean 实例化、使用工厂方法和自动装配构造函数之间进行区分。
	 *
	 * @param beanName 要创建的 bean 的名称
	 * @param mbd      bean 的合并定义
	 * @param args     用于构造函数或工厂方法调用的显式参数
	 * @return bean 的新实例
	 * @throws BeanCreationException 如果无法创建 bean
	 * @see #instantiateBean
	 * @see #instantiateUsingFactoryMethod
	 * @see #autowireConstructor
	 */
	protected Object doCreateBean(String beanName, RootBeanDefinition mbd, @Nullable Object[] args)
			throws BeanCreationException {

		// 实例化 bean。
		BeanWrapper instanceWrapper = null;
		if (mbd.isSingleton()) {
			// 如果是单例，则从缓存中获取实例包装器
			instanceWrapper = this.factoryBeanInstanceCache.remove(beanName);
		}
		if (instanceWrapper == null) {
			// 如果实例包装器为空，则创建 bean 实例
			instanceWrapper = createBeanInstance(beanName, mbd, args);
		}
		// 获取bean实例
		Object bean = instanceWrapper.getWrappedInstance();
		// 获取 bean 类型
		Class<?> beanType = instanceWrapper.getWrappedClass();
		if (beanType != NullBean.class) {
			// 如果 beanType 不是 NullBean，则更新解析的目标类型
			mbd.resolvedTargetType = beanType;
		}

		// 允许后处理器修改合并的 bean 定义。
		synchronized (mbd.postProcessingLock) {
			if (!mbd.postProcessed) {
				try {
					// 应用合并的 bean 定义后处理器
					applyMergedBeanDefinitionPostProcessors(mbd, beanType, beanName);
				} catch (Throwable ex) {
					throw new BeanCreationException(mbd.getResourceDescription(), beanName,
							"Post-processing of merged bean definition failed", ex);
				}
				mbd.postProcessed = true;
			}
		}

		// 急切地缓存单例，以便能够解析循环引用，即使被 BeanFactoryAware 等生命周期接口触发。
		boolean earlySingletonExposure = (mbd.isSingleton()  //单例模式
				&& this.allowCircularReferences //运行循环依赖
				&& isSingletonCurrentlyInCreation(beanName)); //当前单例bean是否正在创建
		if (earlySingletonExposure) {
			if (logger.isTraceEnabled()) {
				logger.trace("Eagerly caching bean '" + beanName +
						"' to allow for resolving potential circular references");
			}
			// 添加单例工厂，用于获取早期 bean 引用
			// 提前将创建的 bean 实例加入到 singletonFactories 中
			// 这里是为了后期避免循环依赖
			addSingletonFactory(beanName, () -> getEarlyBeanReference(beanName, mbd, bean));
		}

		// 初始化 bean 实例。
		Object exposedObject = bean;
		try {
			// 填充 bean 属性
			populateBean(beanName, mbd, instanceWrapper);
			// 初始化 bean
			exposedObject = initializeBean(beanName, exposedObject, mbd);
		} catch (Throwable ex) {
			if (ex instanceof BeanCreationException && beanName.equals(((BeanCreationException) ex).getBeanName())) {
				throw (BeanCreationException) ex;
			} else {
				throw new BeanCreationException(
						mbd.getResourceDescription(), beanName, "Initialization of bean failed", ex);
			}
		}

		if (earlySingletonExposure) {
			//如果是早期单例暴露，获取单例实例
			Object earlySingletonReference = getSingleton(beanName, false);
			if (earlySingletonReference != null) {
				if (exposedObject == bean) {
					// 如果 bean 没有被包装，则使用早期的单例引用
					exposedObject = earlySingletonReference;
				} else if (!this.allowRawInjectionDespiteWrapping && hasDependentBean(beanName)) {
					// 如果不允许原始注入且存在依赖的 bean
					String[] dependentBeans = getDependentBeans(beanName);
					Set<String> actualDependentBeans = new LinkedHashSet<>(dependentBeans.length);
					for (String dependentBean : dependentBeans) {
						if (!removeSingletonIfCreatedForTypeCheckOnly(dependentBean)) {
							//如果bean没有被创建过，则添加该依赖bean
							actualDependentBeans.add(dependentBean);
						}
					}
					if (!actualDependentBeans.isEmpty()) {
						// 如果存在依赖的 bean，则抛出异常，说明存在循环引用
						throw new BeanCurrentlyInCreationException(beanName,
								"Bean with name '" + beanName + "' has been injected into other beans [" +
										StringUtils.collectionToCommaDelimitedString(actualDependentBeans) +
										"] in its raw version as part of a circular reference, but has eventually been " +
										"wrapped. This means that said other beans do not use the final version of the " +
										"bean. This is often the result of over-eager type matching - consider using " +
										"'getBeanNamesForType' with the 'allowEagerInit' flag turned off, for example.");
					}
				}
			}
		}

		try {
			// 如果需要，注册 bean 以便在销毁时进行清理
			registerDisposableBeanIfNecessary(beanName, bean, mbd);
		} catch (BeanDefinitionValidationException ex) {
			throw new BeanCreationException(
					mbd.getResourceDescription(), beanName, "Invalid destruction signature", ex);
		}

		return exposedObject;
	}

	@Override
	@Nullable
	protected Class<?> predictBeanType(String beanName, RootBeanDefinition mbd, Class<?>... typesToMatch) {
		// 类型推断
		Class<?> targetType = determineTargetType(beanName, mbd, typesToMatch);
		// 如果目标类型不为空且bean定义不是合成的，并且存在SmartInstantiationAwareBeanPostProcessors，则尝试预测实例化之前的类型
		if (targetType != null && !mbd.isSynthetic() && hasInstantiationAwareBeanPostProcessors()) {
			// 如果typesToMatch只包含FactoryBean.class，则只匹配FactoryBean
			boolean matchingOnlyFactoryBean = typesToMatch.length == 1 && typesToMatch[0] == FactoryBean.class;
			// 遍历所有的SmartInstantiationAwareBeanPostProcessors
			for (SmartInstantiationAwareBeanPostProcessor bp : getBeanPostProcessorCache().smartInstantiationAware) {
				// 预测bean的类型
				Class<?> predicted = bp.predictBeanType(targetType, beanName);
				// 如果预测结果不为空，并且满足只匹配FactoryBean的条件，则返回预测结果
				if (predicted != null &&
						(!matchingOnlyFactoryBean || FactoryBean.class.isAssignableFrom(predicted))) {
					return predicted;
				}
			}
		}
		// 返回目标类型
		return targetType;
	}

	/**
	 * 确定给定bean定义的目标类型。
	 *
	 * @param beanName     bean的名称 (用于错误处理)
	 * @param mbd          bean的合并bean定义
	 * @param typesToMatch 在内部类型匹配的情况下要匹配的类型 (也表示返回的 {@code Class} 永远不会暴露于应用程序代码)
	 * @return bean的类型 (如果可确定)，否则为 {@code null}
	 */
	@Nullable
	protected Class<?> determineTargetType(String beanName, RootBeanDefinition mbd, Class<?>... typesToMatch) {
		//获取bean定义中的目标类型
		Class<?> targetType = mbd.getTargetType();
		if (targetType != null) {
			//目标类型不为空，返回改类型
			return targetType;
		}
		if (mbd.getFactoryMethodName() == null) {
			//如果没有指定工厂方法名，调用resolveBeanClass解析目标类型。
			targetType = resolveBeanClass(mbd, beanName, typesToMatch);
		} else {
			//有工厂方法名，调用getTypeForFactoryMethod解析目标类型
			targetType = getTypeForFactoryMethod(beanName, mbd, typesToMatch);
		}
		if (ObjectUtils.isEmpty(typesToMatch) || getTempClassLoader() == null) {
			//如果要匹配的类型为空或者临时类加载器为空，缓存到当前的bean定义中的解析好的目标类型
			mbd.resolvedTargetType = targetType;
		}
		return targetType;
	}

	/**
	 * 确定基于工厂方法的给定bean定义的目标类型。仅当没有为目标bean注册的单例实例时才调用。
	 * <p>此实现决定匹配 {@link #createBean} 的不同创建策略的类型。尽可能地，我们将执行静态类型检查，以避免创建目标bean。
	 *
	 * @param beanName     bean的名称 (用于错误处理)
	 * @param mbd          bean的合并bean定义
	 * @param typesToMatch 在内部类型匹配的情况下要匹配的类型 (也表示返回的 {@code Class} 永远不会暴露于应用程序代码)
	 * @return bean的类型 (如果可确定)，否则为 {@code null}
	 * @see #createBean
	 */
	@Nullable
	protected Class<?> getTypeForFactoryMethod(String beanName, RootBeanDefinition mbd, Class<?>... typesToMatch) {
		//获取已经缓存的工厂方法返回的类型
		ResolvableType cachedReturnType = mbd.factoryMethodReturnType;
		if (cachedReturnType != null) {
			//如果该解析类型不为空，通过该解析类型解析返回类型
			return cachedReturnType.resolve();
		}

		Class<?> commonType = null;
		//获取用于内省的工厂方法
		Method uniqueCandidate = mbd.factoryMethodToIntrospect;

		if (uniqueCandidate == null) {
			//如果该工厂方法为空
			Class<?> factoryClass;
			//是否为静态方法
			boolean isStatic = true;
			//获取工厂bean名称
			String factoryBeanName = mbd.getFactoryBeanName();
			if (factoryBeanName == null) {
				//如果工厂bean名称为空，调用resolveBeanClass 解析出工厂方法类型
				factoryClass = resolveBeanClass(mbd, beanName, typesToMatch);
			} else {
				//如果该工厂bean名称存在
				if (factoryBeanName.equals(beanName)) {
					//工厂bean名称与当前bean名称相同，抛出异常
					throw new BeanDefinitionStoreException(mbd.getResourceDescription(), beanName,
							"factory-bean reference points back to the same bean definition");
				}
				// 检查工厂类上声明的工厂方法返回类型。
				factoryClass = getType(factoryBeanName);
				isStatic = false;
			}

			if (factoryClass == null) {
				//如果工厂类为空，返回null
				return null;
			}
			factoryClass = ClassUtils.getUserClass(factoryClass);

			//如果所有工厂方法都具有相同的返回类型，则返回该类型。由于类型转换自动转换，无法清楚地找到确切的方法!
			//获取构造函数的参数个数
			int minNrOfArgs =
					(mbd.hasConstructorArgumentValues() ? mbd.getConstructorArgumentValues().getArgumentCount() : 0);
			//获取工厂类的当前类的所有方法以及父类或父接口的所有方法。并添加进工厂方法候选者缓存中。
			Method[] candidates = this.factoryMethodCandidateCache.computeIfAbsent(factoryClass,
					clazz -> ReflectionUtils.getUniqueDeclaredMethods(clazz, ReflectionUtils.USER_DECLARED_METHODS));

			for (Method candidate : candidates) {
				//遍历候选方法
				if (Modifier.isStatic(candidate.getModifiers()) == isStatic && mbd.isFactoryMethod(candidate) &&
						candidate.getParameterCount() >= minNrOfArgs) {
					//如果当前方法的static修饰符与isStatic相同，且当前方法的方法名和工厂方法名相同，并且当前方法的参数个数>=构造函数的参数个数。
					if (candidate.getTypeParameters().length > 0) {
						//如果方法里有泛型参数
						try {
							//获取所有的参数类型
							Class<?>[] paramTypes = candidate.getParameterTypes();
							String[] paramNames = null;
							//获取参数名称发现者
							ParameterNameDiscoverer pnd = getParameterNameDiscoverer();
							if (pnd != null) {
								//获取所有参数的参数名称
								paramNames = pnd.getParameterNames(candidate);
							}
							//获取bean定义中的构造参数值
							ConstructorArgumentValues cav = mbd.getConstructorArgumentValues();
							Set<ConstructorArgumentValues.ValueHolder> usedValueHolders = new HashSet<>(paramTypes.length);
							Object[] args = new Object[paramTypes.length];
							for (int i = 0; i < args.length; i++) {
								//遍历方法的每个参数位置，按照索引或者类型查找该参数的值持有者
								ConstructorArgumentValues.ValueHolder valueHolder = cav.getArgumentValue(
										i, paramTypes[i], (paramNames != null ? paramNames[i] : null), usedValueHolders);
								if (valueHolder == null) {
									//如果值持有者为空， 查找与给定类型匹配的下一个通用参数值
									valueHolder = cav.getGenericArgumentValue(null, null, usedValueHolders);
								}
								if (valueHolder != null) {
									//如果值持有者不为空，则获取参数值。
									args[i] = valueHolder.getValue();
									usedValueHolders.add(valueHolder);
								}
							}
							//获取方法的返回类型
							Class<?> returnType = AutowireUtils.resolveReturnTypeForFactoryMethod(
									candidate, args, getBeanClassLoader());
							//如果通用类型为空，且返回类型与当前的方法的类型相同，将当前方法设置为唯一候选方法
							if (commonType == null && returnType == candidate.getReturnType()) {
								uniqueCandidate = candidate;
							} else {
								uniqueCandidate = null;
							}
							//确认通用类型和当前方法返回类型的共同祖先
							commonType = ClassUtils.determineCommonAncestor(returnType, commonType);
							if (commonType == null) {
								//如果共同祖先为空，返回null。
								//发现不明确的返回类型: 返回null表示 “不可确定”。
								return null;
							}
						} catch (Throwable ex) {
							if (logger.isDebugEnabled()) {
								logger.debug("Failed to resolve generic return type for factory method: " + ex);
							}
						}
					} else {
						//如果通用类型为空，则将当前方法设置为唯一候选者
						if (commonType == null) {
							uniqueCandidate = candidate;
						} else {
							uniqueCandidate = null;
						}
						//确认通用类型和当前方法返回类型的共同祖先
						commonType = ClassUtils.determineCommonAncestor(candidate.getReturnType(), commonType);
						if (commonType == null) {
							//如果共同祖先为空，返回null。
							//发现不明确的返回类型: 返回null表示 “不可确定”。
							return null;
						}
					}
				}
			}
			//将唯一候选者方法缓存起来
			mbd.factoryMethodToIntrospect = uniqueCandidate;
			if (commonType == null) {
				//如果共同类型为空，返回null
				return null;
			}
		}

		// 找到常见的返回类型: 所有工厂方法都返回相同的类型。
		// 对于非参数化的唯一候选者，缓存目标工厂方法的完整类型声明上下文。
		//如果唯一候选方法不为空，通过ResolvableType.forMethodReturnType解析出返回类型。
		//否则通过 共同类型 解析出返回类型
		cachedReturnType = (uniqueCandidate != null ?
				ResolvableType.forMethodReturnType(uniqueCandidate) : ResolvableType.forClass(commonType));
		//缓存工厂方法的返回类型
		mbd.factoryMethodReturnType = cachedReturnType;
		//解析成Class对象返回。
		return cachedReturnType.resolve();
	}

	/**
	 * 此实现尝试查询FactoryBean的泛型参数元数据（如果存在）以确定对象类型。
	 * 如果不存在，即FactoryBean声明为原始类型，将在未应用bean属性的情况下，
	 * 在FactoryBean的普通实例上检查FactoryBean的{@code getObjectType}方法。
	 * 如果这还没有返回类型，并且{@code allowInit}为{@code true}，
	 * 则会使用完整的FactoryBean创建作为后备（通过委派到超类的实现）。
	 * <p>仅在单例FactoryBean的情况下应用FactoryBean的快捷方式检查。
	 * 如果FactoryBean实例本身不保持为单例，则将完全创建它以检查其公开对象的类型。
	 *
	 * @param beanName  bean名称
	 * @param mbd       bean的根bean定义
	 * @param allowInit 是否允许完整初始化FactoryBean以检索对象类型
	 * @return FactoryBean的对象类型，如果找不到则为{@code null}
	 */
	@Override
	protected ResolvableType getTypeForFactoryBean(String beanName, RootBeanDefinition mbd, boolean allowInit) {
		// 检查bean定义本身是否通过属性定义了类型
		ResolvableType result = getTypeForFactoryBeanFromAttributes(mbd);
		if (result != ResolvableType.NONE) {
			return result;
		}

		// 获取bean的ResolvableType，如果存在beanClass则使用它，否则为NONE
		ResolvableType beanType = (mbd.hasBeanClass() ? ResolvableType.forClass(mbd.getBeanClass()) : ResolvableType.NONE);

		// 对于通过实例提供的bean，尝试获取目标类型和bean类的泛型类型
		if (mbd.getInstanceSupplier() != null) {
			// 获取目标类型的泛型
			result = getFactoryBeanGeneric(mbd.targetType);
			if (result.resolve() != null) {
				// 如果存在泛型，则返回该已解析类型
				return result;
			}
			// 获取bean类的泛型
			result = getFactoryBeanGeneric(beanType);
			if (result.resolve() != null) {
				//如果存在泛型，则返回该bean类
				return result;
			}
		}

		// 考虑工厂方法
		String factoryBeanName = mbd.getFactoryBeanName();
		String factoryMethodName = mbd.getFactoryMethodName();

		// 扫描工厂Bean的方法
		if (factoryBeanName != null) {
			// 如果存在工厂bean名称
			if (factoryMethodName != null) {
				// 并且存在工厂方法名称
				// 尝试从工厂方法的声明中获取FactoryBean的对象类型，而不需要实例化包含的bean
				BeanDefinition factoryBeanDefinition = getBeanDefinition(factoryBeanName);
				Class<?> factoryBeanClass;
				if (factoryBeanDefinition instanceof AbstractBeanDefinition &&
						((AbstractBeanDefinition) factoryBeanDefinition).hasBeanClass()) {
					// 如果工厂bean定义是AbstractBeanDefinition类型，并且有bean类，则获取该bean类
					factoryBeanClass = ((AbstractBeanDefinition) factoryBeanDefinition).getBeanClass();
				} else {
					// 通过工厂bean名称和工厂bean定义获取合并bean定义
					RootBeanDefinition fbmbd = getMergedBeanDefinition(factoryBeanName, factoryBeanDefinition);
					// 使用上述的工厂bean名称和合并bean定义推断类型
					factoryBeanClass = determineTargetType(factoryBeanName, fbmbd);
				}
				if (factoryBeanClass != null) {
					// 如果工厂bean类型存在，则通过该工厂bean类型和工厂方法获取类型
					result = getTypeForFactoryBeanFromMethod(factoryBeanClass, factoryMethodName);
					if (result.resolve() != null) {
						// 如果这个类型存在，则返回
						return result;
					}
				}
			}
			// 如果以上情况均不可解析，且引用的工厂bean尚不存在，则退出 - 我们不希望强制创建另一个bean来获取FactoryBean的对象类型...
			if (!isBeanEligibleForMetadataCaching(factoryBeanName)) {
				return ResolvableType.NONE;
			}
		}

		// 如果允许，可以创建工厂bean并提前调用getObjectType()
		if (allowInit) {
			// 如果允许初始化，则获取工厂bean
			FactoryBean<?> factoryBean = (mbd.isSingleton() ?
					getSingletonFactoryBeanForTypeCheck(beanName, mbd) :
					getNonSingletonFactoryBeanForTypeCheck(beanName, mbd));
			if (factoryBean != null) {
				// 尝试从这个实例的早期阶段获取FactoryBean的对象类型
				Class<?> type = getTypeForFactoryBean(factoryBean);
				if (type != null) {
					return ResolvableType.forClass(type);
				}
				// 如果没有找到类型的快捷方式FactoryBean实例：回退到完整创建FactoryBean实例
				return super.getTypeForFactoryBean(beanName, mbd, true);
			}
		}

		// 如果factoryBeanName为空且存在beanClass且factoryMethodName不为空，则无法进行早期bean实例化：从静态工厂方法签名或类继承层次结构中确定FactoryBean的类型...
		if (factoryBeanName == null && mbd.hasBeanClass() && factoryMethodName != null) {
			return getTypeForFactoryBeanFromMethod(mbd.getBeanClass(), factoryMethodName);
		}

		// 获取bean的泛型类型
		result = getFactoryBeanGeneric(beanType);
		if (result.resolve() != null) {
			return result;
		}

		return ResolvableType.NONE;
	}

	private ResolvableType getFactoryBeanGeneric(@Nullable ResolvableType type) {
		if (type == null) {
			return ResolvableType.NONE;
		}
		return type.as(FactoryBean.class).getGeneric();
	}

	/**
	 * 对给定的bean类进行内省，尝试在其中查找通用的{@code FactoryBean}对象类型声明的工厂方法。
	 *
	 * @param beanClass         要查找工厂方法的bean类
	 * @param factoryMethodName 工厂方法的名称
	 * @return 通用的{@code FactoryBean}对象类型，如果没有则为{@code null}
	 */
	private ResolvableType getTypeForFactoryBeanFromMethod(Class<?> beanClass, String factoryMethodName) {
		// CGLIB子类方法隐藏泛型参数；查看原始用户类。
		Class<?> factoryBeanClass = ClassUtils.getUserClass(beanClass);
		FactoryBeanMethodTypeFinder finder = new FactoryBeanMethodTypeFinder(factoryMethodName);
		// 使用ReflectionUtils.doWithMethods检查工厂Bean类中的方法，查找返回类型
		ReflectionUtils.doWithMethods(factoryBeanClass, finder, ReflectionUtils.USER_DECLARED_METHODS);
		// 返回查找结果
		return finder.getResult();
	}

	/**
	 * 此实现尝试查询FactoryBean的泛型参数元数据（如果存在）以确定对象类型。如果不存在，
	 * 即FactoryBean声明为原始类型，则在未应用bean属性的FactoryBean的纯实例上检查FactoryBean的{@code getObjectType}方法。
	 * 如果此方法尚未返回类型，则使用FactoryBean的完全创建作为后备（通过委托给超类的实现）。
	 * <p>仅在单例FactoryBean的情况下应用对FactoryBean的快捷检查。如果FactoryBean实例本身不作为单例保存，
	 * 则将完全创建它以检查其暴露对象的类型。
	 */
	@Override
	@Deprecated
	@Nullable
	protected Class<?> getTypeForFactoryBean(String beanName, RootBeanDefinition mbd) {
		return getTypeForFactoryBean(beanName, mbd, true).resolve();
	}

	/**
	 * 获取对指定bean的早期访问引用，通常用于解析循环引用。
	 *
	 * @param beanName bean的名称（出错处理时使用）
	 * @param mbd      bean的合并定义
	 * @param bean     原始bean实例
	 * @return 作为bean引用公开的对象
	 */
	protected Object getEarlyBeanReference(String beanName, RootBeanDefinition mbd, Object bean) {
		// 将bean赋给暴露对象
		Object exposedObject = bean;
		// 如果不是合成对象并且存在实例化感知的Bean后置处理器
		if (!mbd.isSynthetic() && hasInstantiationAwareBeanPostProcessors()) {
			// 遍历实例化感知的Bean后置处理器集合
			for (SmartInstantiationAwareBeanPostProcessor bp : getBeanPostProcessorCache().smartInstantiationAware) {
				// 调用后置处理器的getEarlyBeanReference方法，获取提前暴露的Bean引用
				exposedObject = bp.getEarlyBeanReference(exposedObject, beanName);
			}
		}
		// 返回暴露对象
		return exposedObject;
	}


	//---------------------------------------------------------------------
	// Implementation methods
	//---------------------------------------------------------------------

	/**
	 * 获取用于{@code getObjectType()}调用的“快捷”单例FactoryBean实例，而无需完全初始化FactoryBean。
	 *
	 * @param beanName bean的名称
	 * @param mbd      bean的bean定义
	 * @return FactoryBean实例，如果我们无法获取快捷FactoryBean实例，则返回{@code null}
	 */
	@Nullable
	private FactoryBean<?> getSingletonFactoryBeanForTypeCheck(String beanName, RootBeanDefinition mbd) {
		synchronized (getSingletonMutex()) {
			// 从缓存中获取包装器
			BeanWrapper bw = this.factoryBeanInstanceCache.get(beanName);
			// 如果已经存在，直接返回包装器中的实例
			if (bw != null) {
				return (FactoryBean<?>) bw.getWrappedInstance();
			}
			// 从单例缓存中获取实例
			Object beanInstance = getSingleton(beanName, false);
			// 如果实例是FactoryBean类型，直接返回
			if (beanInstance instanceof FactoryBean) {
				return (FactoryBean<?>) beanInstance;
			}
			// 检查当前bean是否正在创建中，或者依赖的工厂bean是否正在创建中
			if (isSingletonCurrentlyInCreation(beanName) || (mbd.getFactoryBeanName() != null && isSingletonCurrentlyInCreation(mbd.getFactoryBeanName()))) {
				return null;
			}

			Object instance;
			try {
				// 标记该bean当前正在创建中
				beforeSingletonCreation(beanName);
				// 在实例化bean之前，让BeanPostProcessors有机会返回代理而不是目标bean实例
				instance = resolveBeforeInstantiation(beanName, mbd);
				// 如果没有返回代理，尝试创建bean实例
				if (instance == null) {
					bw = createBeanInstance(beanName, mbd, null);
					instance = bw.getWrappedInstance();
				}
			} catch (UnsatisfiedDependencyException ex) {
				// 不要吞掉异常，可能是配置错误
				throw ex;
			} catch (BeanCreationException ex) {
				// 不要吞掉链接错误，因为它包含第一次出现时的完整堆栈跟踪... 之后只是一个普通的NoClassDefFoundError。
				if (ex.contains(LinkageError.class)) {
					throw ex;
				}
				// 实例化失败，可能是因为太早...
				if (logger.isDebugEnabled()) {
					logger.debug("Bean creation exception on singleton FactoryBean type check: " + ex);
				}
				onSuppressedException(ex);
				return null;
			} finally {
				// 完成该bean的部分创建
				afterSingletonCreation(beanName);
			}

			// 获取FactoryBean实例
			FactoryBean<?> fb = getFactoryBean(beanName, instance);
			// 将包装器放入缓存
			if (bw != null) {
				this.factoryBeanInstanceCache.put(beanName, bw);
			}
			return fb;
		}
	}

	/**
	 * 获取用于{@code getObjectType()}调用的“快捷”非单例FactoryBean实例，而无需完全初始化FactoryBean。
	 *
	 * @param beanName bean的名称
	 * @param mbd      bean的bean定义
	 * @return FactoryBean实例，如果我们无法获取快捷FactoryBean实例，则返回{@code null}
	 */
	@Nullable
	private FactoryBean<?> getNonSingletonFactoryBeanForTypeCheck(String beanName, RootBeanDefinition mbd) {
		// 如果该原型bean当前正在创建中，返回null
		if (isPrototypeCurrentlyInCreation(beanName)) {
			return null;
		}

		Object instance;
		try {
			// 标记该bean当前正在创建中
			beforePrototypeCreation(beanName);
			// 在实例化bean之前，让BeanPostProcessors有机会返回代理而不是目标bean实例
			instance = resolveBeforeInstantiation(beanName, mbd);
			// 如果没有返回代理，尝试创建bean实例
			if (instance == null) {
				BeanWrapper bw = createBeanInstance(beanName, mbd, null);
				instance = bw.getWrappedInstance();
			}
		} catch (UnsatisfiedDependencyException ex) {
			// 不要吞掉异常，可能是配置错误
			throw ex;
		} catch (BeanCreationException ex) {
			// 实例化失败，可能是因为太早...
			if (logger.isDebugEnabled()) {
				logger.debug("Bean creation exception on non-singleton FactoryBean type check: " + ex);
			}
			onSuppressedException(ex);
			return null;
		} finally {
			// 完成该bean的部分创建
			afterPrototypeCreation(beanName);
		}

		return getFactoryBean(beanName, instance);
	}

	/**
	 * 将MergedBeanDefinitionPostProcessors应用于指定的bean定义，调用它们的{@code postProcessMergedBeanDefinition}方法。
	 *
	 * @param mbd      bean的合并bean定义
	 * @param beanType 托管bean实例的实际类型
	 * @param beanName bean的名称
	 * @see MergedBeanDefinitionPostProcessor#postProcessMergedBeanDefinition
	 */
	protected void applyMergedBeanDefinitionPostProcessors(RootBeanDefinition mbd, Class<?> beanType, String beanName) {
		for (MergedBeanDefinitionPostProcessor processor : getBeanPostProcessorCache().mergedDefinition) {
			processor.postProcessMergedBeanDefinition(mbd, beanType, beanName);
		}
	}

	/**
	 * 应用前实例化后处理器，解决指定bean是否存在前实例化快捷方式。
	 *
	 * @param beanName bean名称
	 * @param mbd      bean的bean定义
	 * @return 快速确定的bean实例的方式，如果没有bean实例，则为 {@code null}
	 */
	@Nullable
	protected Object resolveBeforeInstantiation(String beanName, RootBeanDefinition mbd) {
		Object bean = null;
		if (Boolean.FALSE.equals(mbd.beforeInstantiationResolved)) {
			//如果实例化后处理器未启动，直接返回null
			return bean;
		}
		// 确保bean类实际上在这一点上得到解决。
		if (!mbd.isSynthetic() && hasInstantiationAwareBeanPostProcessors()) {
			//如果bean定义不是合成的，并且有实例化感知bean后置处理器
			Class<?> targetType = determineTargetType(beanName, mbd);
			if (targetType != null) {
				//如果目标类型不为空，实例化前应用bean后置处理
				bean = applyBeanPostProcessorsBeforeInstantiation(targetType, beanName);
				if (bean != null) {
					//如果bean不为空，实例化后，应用bean后置处理
					bean = applyBeanPostProcessorsAfterInitialization(bean, beanName);
				}
			}
		}
		mbd.beforeInstantiationResolved = (bean != null);
		return bean;
	}

	/**
	 * 将InstantiationAwareBeanPostProcessors应用于指定的bean定义 (按类和名称)，调用其 {@code postProcessBeforeInstantiation} 方法。
	 * <p> 任何返回的对象都将用作bean，而不是实际实例化目标bean。后处理器的 {@code null} 返回值将导致目标bean被实例化。
	 *
	 * @param beanClass 要实例化的bean的类
	 * @param beanName  bean名称
	 * @return 要使用的bean对象而不是目标bean的默认实例，或 {@code null}
	 * @see InstantiationAwareBeanPostProcessor#postProcessBeforeInstantiation
	 */
	@Nullable
	protected Object applyBeanPostProcessorsBeforeInstantiation(Class<?> beanClass, String beanName) {
		for (InstantiationAwareBeanPostProcessor bp : getBeanPostProcessorCache().instantiationAware) {
			//遍历实例化感知bean后置处理器，调用在实例化前后置处理
			Object result = bp.postProcessBeforeInstantiation(beanClass, beanName);
			if (result != null) {
				//如果后置处理的返回值不为空，则停止后续实例化感知bean后置处理器的处理，并返回该对象。
				return result;
			}
		}
		return null;
	}

	/**
	 * 使用适当的实例化策略为指定的 bean 创建一个新实例：工厂方法、构造函数自动装配或简单实例化。
	 *
	 * @param beanName bean 的名称
	 * @param mbd      bean 的定义
	 * @param args     用于构造函数或工厂方法调用的显式参数
	 * @return 新实例的 BeanWrapper
	 * @see #obtainFromSupplier
	 * @see #instantiateUsingFactoryMethod
	 * @see #autowireConstructor
	 * @see #instantiateBean
	 */
	protected BeanWrapper createBeanInstance(String beanName, RootBeanDefinition mbd, @Nullable Object[] args) {
		// 确保此时实际解析了 bean 类。
		// 解析 bean 的类
		Class<?> beanClass = resolveBeanClass(mbd, beanName);

		// 检查 beanClass 是否为 public，如果不是，并且不允许非公共访问，则抛出异常
		if (beanClass != null && !Modifier.isPublic(beanClass.getModifiers()) && !mbd.isNonPublicAccessAllowed()) {
			throw new BeanCreationException(mbd.getResourceDescription(), beanName,
					"Bean class isn't public, and non-public access not allowed: " + beanClass.getName());
		}

		// 获取实例供应商
		Supplier<?> instanceSupplier = mbd.getInstanceSupplier();
		if (instanceSupplier != null) {
			// 如果存在实例供应商，则从供应商获取实例
			return obtainFromSupplier(instanceSupplier, beanName);
		}

		// 检查是否存在工厂方法，如果有，则使用工厂方法实例化 bean
		if (mbd.getFactoryMethodName() != null) {
			return instantiateUsingFactoryMethod(beanName, mbd, args);
		}

		// 重新创建相同的 bean 时的快捷方式...
		boolean resolved = false;
		boolean autowireNecessary = false;
		// 检查是否已经解析构造函数或工厂方法
		if (args == null) {
			synchronized (mbd.constructorArgumentLock) {
				if (mbd.resolvedConstructorOrFactoryMethod != null) {
					resolved = true;
					autowireNecessary = mbd.constructorArgumentsResolved;
				}
			}
		}
		// 如果已经解析，则根据是否需要自动装配执行相应的实例化操作
		if (resolved) {
			if (autowireNecessary) {
				// 如果已解析且需要自动装配，则执行自动装配构造函数
				return autowireConstructor(beanName, mbd, null, null);
			} else {
				// 如果已解析但不需要自动装配，则直接实例化 bean
				return instantiateBean(beanName, mbd);
			}
		}

		// 确定解析的构造函数
		// 主要是检查已经注册的SmartInstantiationAwareBeanPostProcessor
		Constructor<?>[] ctors = determineConstructorsFromBeanPostProcessors(beanClass, beanName);
		if (ctors != null || mbd.getResolvedAutowireMode() == AUTOWIRE_CONSTRUCTOR ||
				mbd.hasConstructorArgumentValues() || !ObjectUtils.isEmpty(args)) {
			// 如果存在候选构造函数或已解析的自动装配模式为 AUTOWIRE_CONSTRUCTOR，
			// 或存在构造函数参数值，或提供了构造函数参数，则执行自动装配构造函数
			return autowireConstructor(beanName, mbd, ctors, args);
		}

		// 选择构造方法，创建bean
		ctors = mbd.getPreferredConstructors();
		if (ctors != null) {
			// 如果存在首选构造函数，则执行自动装配构造函数
			return autowireConstructor(beanName, mbd, ctors, null);
		}

		// 没有特殊处理：简单使用无参构造函数。
		// 执行默认的实例化 bean
		return instantiateBean(beanName, mbd);
	}

	/**
	 * 从给定的供应商获取一个 bean 实例。
	 *
	 * @param instanceSupplier 已配置的供应商
	 * @param beanName         相应的 bean 名称
	 * @return 新实例的 BeanWrapper
	 * @see #getObjectForBeanInstance
	 * @since 5.0
	 */
	protected BeanWrapper obtainFromSupplier(Supplier<?> instanceSupplier, String beanName) {
		Object instance;

		// 获取当前正在创建的外部 bean
		String outerBean = this.currentlyCreatedBean.get();
		// 将当前正在创建的 bean 设置为当前 beanName
		this.currentlyCreatedBean.set(beanName);
		try {
			// 尝试获取实例
			instance = instanceSupplier.get();
		} finally {
			// 恢复当前正在创建的 bean 为外部 bean
			if (outerBean != null) {
				this.currentlyCreatedBean.set(outerBean);
			} else {
				this.currentlyCreatedBean.remove();
			}
		}

		// 如果实例为 null，则使用 NullBean
		if (instance == null) {
			instance = new NullBean();
		}
		// 使用 BeanWrapperImpl 封装实例
		BeanWrapper bw = new BeanWrapperImpl(instance);
		// 初始化 BeanWrapper
		initBeanWrapper(bw);
		// 返回封装的 BeanWrapper
		return bw;
	}

	/**
	 * 重写是为了将当前创建的bean隐式注册为依赖于在 {@link Supplier} 回调期间以编程方式检索的其他bean。
	 *
	 * @see #obtainFromSupplier
	 * @since 5.0
	 */
	@Override
	protected Object getObjectForBeanInstance(
			Object beanInstance, String name, String beanName, @Nullable RootBeanDefinition mbd) {
		//当前已创建的bean名称
		String currentlyCreatedBean = this.currentlyCreatedBean.get();
		if (currentlyCreatedBean != null) {
			//为bean名称将当前已bean名称注册为依赖bean。
			registerDependentBean(beanName, currentlyCreatedBean);
		}

		return super.getObjectForBeanInstance(beanInstance, name, beanName, mbd);
	}

	/**
	 * 确定用于给定bean的候选构造函数，检查所有已注册的{@link SmartInstantiationAwareBeanPostProcessor SmartInstantiationAwareBeanPostProcessors}。
	 *
	 * @param beanClass bean的原始类
	 * @param beanName  bean的名称
	 * @return 候选构造函数，如果没有指定，则为{@code null}
	 * @throws org.springframework.beans.BeansException 如果出现错误
	 * @see org.springframework.beans.factory.config.SmartInstantiationAwareBeanPostProcessor#determineCandidateConstructors
	 */
	@Nullable
	protected Constructor<?>[] determineConstructorsFromBeanPostProcessors(@Nullable Class<?> beanClass, String beanName)
			throws BeansException {

		// 如果beanClass不为null，并且存在InstantiationAwareBeanPostProcessors，则遍历处理器列表
		if (beanClass != null && hasInstantiationAwareBeanPostProcessors()) {
			for (SmartInstantiationAwareBeanPostProcessor bp : getBeanPostProcessorCache().smartInstantiationAware) {
				// 确定候选构造函数
				Constructor<?>[] ctors = bp.determineCandidateConstructors(beanClass, beanName);
				// 如果构造函数不为null，则返回
				if (ctors != null) {
					return ctors;
				}
			}
		}
		return null;
	}

	/**
	 * 使用默认构造函数实例化给定的bean。
	 *
	 * @param beanName bean的名称
	 * @param mbd      bean的定义
	 * @return 一个新实例的BeanWrapper
	 */
	protected BeanWrapper instantiateBean(String beanName, RootBeanDefinition mbd) {
		try {
			Object beanInstance;
			if (System.getSecurityManager() != null) {
				// 在有安全管理器的情况下，通过特权动作进行实例化
				beanInstance = AccessController.doPrivileged(
						(PrivilegedAction<Object>) () -> getInstantiationStrategy().instantiate(mbd, beanName, this),
						getAccessControlContext());
			} else {
				// 在没有安全管理器的情况下，直接实例化
				beanInstance = getInstantiationStrategy().instantiate(mbd, beanName, this);
			}
			// 使用实例化后的bean创建BeanWrapper
			BeanWrapper bw = new BeanWrapperImpl(beanInstance);
			// 初始化BeanWrapper
			initBeanWrapper(bw);
			// 返回BeanWrapper
			return bw;
		} catch (Throwable ex) {
			// 捕获异常，抛出BeanCreationException
			throw new BeanCreationException(
					mbd.getResourceDescription(), beanName, "Instantiation of bean failed", ex);
		}
	}

	/**
	 * 使用命名的工厂方法实例化 bean。如果 mbd 参数指定的是类而不是 factoryBean，则该方法可能是静态的，
	 * 或者是通过依赖注入配置的工厂对象本身上的实例变量。
	 *
	 * @param beanName     bean 的名称
	 * @param mbd          bean 的定义
	 * @param explicitArgs 通过程序传递的参数值，通过 getBean 方法传递，如果没有则为 {@code null}
	 *                     （暗示使用 bean 定义中的构造函数参数值）
	 * @return 新实例的 BeanWrapper
	 * @see #getBean(String, Object[])
	 */
	protected BeanWrapper instantiateUsingFactoryMethod(
			String beanName, RootBeanDefinition mbd, @Nullable Object[] explicitArgs) {

		return new ConstructorResolver(this).instantiateUsingFactoryMethod(beanName, mbd, explicitArgs);
	}

	/**
	 * "自动构造"（使用按类型的构造函数参数）行为。
	 * 如果指定了显式的构造函数参数值，则还会匹配所有剩余的参数与Bean工厂中的Bean。
	 * <p>这对应于构造函数注入：在此模式下，Spring Bean工厂能够托管期望基于构造函数进行依赖项解析的组件。
	 *
	 * @param beanName     Bean的名称
	 * @param mbd          Bean的Bean定义
	 * @param ctors        选择的候选构造函数
	 * @param explicitArgs 通过getBean方法以编程方式传递的参数值，
	 *                     如果没有（表示使用Bean定义中的构造函数参数值），则为{@code null}
	 * @return 一个新实例的BeanWrapper
	 */
	protected BeanWrapper autowireConstructor(
			String beanName, RootBeanDefinition mbd, @Nullable Constructor<?>[] ctors, @Nullable Object[] explicitArgs) {

		return new ConstructorResolver(this).autowireConstructor(beanName, mbd, ctors, explicitArgs);
	}

	/**
	 * 使用bean定义中的属性值填充给定的BeanWrapper中的bean实例。
	 *
	 * @param beanName bean的名称
	 * @param mbd      bean的定义
	 * @param bw       带有bean实例的BeanWrapper
	 */
	@SuppressWarnings("deprecation")  // 忽略过期的 postProcessPropertyValues
	protected void populateBean(String beanName, RootBeanDefinition mbd, @Nullable BeanWrapper bw) {
		if (bw == null) {
			// 如果BeanWrapper为null，则抛出BeanCreationException
			if (mbd.hasPropertyValues()) {
				throw new BeanCreationException(
						mbd.getResourceDescription(), beanName, "Cannot apply property values to null instance");
			} else {
				// 如果没有属性值需要设置，直接跳过属性设置阶段
				return;
			}
		}

		// 给任何InstantiationAwareBeanPostProcessors一个机会在设置属性之前修改bean的状态。
		// 这可以用来支持字段注入的各种风格。
		if (!mbd.isSynthetic() && hasInstantiationAwareBeanPostProcessors()) {
			//如果Bean定义不是合成的，并且实现了后置处理器接口
			for (InstantiationAwareBeanPostProcessor bp : getBeanPostProcessorCache().instantiationAware) {
				// 如果该实例不是在实例化后再处理，则跳过填充属性。
				if (!bp.postProcessAfterInstantiation(bw.getWrappedInstance(), beanName)) {
					return;
				}
			}
		}

		// 获取属性值对
		PropertyValues pvs = (mbd.hasPropertyValues() ? mbd.getPropertyValues() : null);

		//获取已解析的装配模式
		int resolvedAutowireMode = mbd.getResolvedAutowireMode();
		if (resolvedAutowireMode == AUTOWIRE_BY_NAME || resolvedAutowireMode == AUTOWIRE_BY_TYPE) {
			//如果装配模式是按照名称装配或者按照类型装配，构建 MutablePropertyValues 对象后，调用不同的方法进行装配属性。
			MutablePropertyValues newPvs = new MutablePropertyValues(pvs);
			// 根据autowire by name的模式添加属性值
			if (resolvedAutowireMode == AUTOWIRE_BY_NAME) {
				autowireByName(beanName, mbd, bw, newPvs);
			}
			// 根据autowire by type的模式添加属性值
			if (resolvedAutowireMode == AUTOWIRE_BY_TYPE) {
				autowireByType(beanName, mbd, bw, newPvs);
			}
			pvs = newPvs;
		}

		// 判断该bean是否有实例化后感知后置处理器
		boolean hasInstAwareBpps = hasInstantiationAwareBeanPostProcessors();
		// 判断该bean是否需要检查依赖
		boolean needsDepCheck = (mbd.getDependencyCheck() != AbstractBeanDefinition.DEPENDENCY_CHECK_NONE);

		PropertyDescriptor[] filteredPds = null;
		if (hasInstAwareBpps) {
			//如果有实例化后感知后置处理器
			if (pvs == null) {
				pvs = mbd.getPropertyValues();
			}
			//遍历实例化后感知后置处理器
			for (InstantiationAwareBeanPostProcessor bp : getBeanPostProcessorCache().instantiationAware) {
				// 调用后置处理器的postProcessProperties方法处理属性
				PropertyValues pvsToUse = bp.postProcessProperties(pvs, bw.getWrappedInstance(), beanName);
				if (pvsToUse == null) {
					if (filteredPds == null) {
						// 设置过滤后的属性
						filteredPds = filterPropertyDescriptorsForDependencyCheck(bw, mbd.allowCaching);
					}
					// 调用后置处理器的postProcessPropertyValues方法处理属性值
					pvsToUse = bp.postProcessPropertyValues(pvs, filteredPds, bw.getWrappedInstance(), beanName);
					if (pvsToUse == null) {
						// 如果属性值为空，则跳过属性填充阶段。
						return;
					}
				}
				pvs = pvsToUse;
			}
		}
		if (needsDepCheck) {
			//如果需要检查依赖
			if (filteredPds == null) {
				// 设置过滤后的属性值
				filteredPds = filterPropertyDescriptorsForDependencyCheck(bw, mbd.allowCaching);
			}
			// 检查依赖关系
			checkDependencies(beanName, mbd, filteredPds, pvs);
		}

		if (pvs != null) {
			// 如果属性值存在，填充属性值
			applyPropertyValues(beanName, mbd, bw, pvs);
		}
	}

	/**
	 * 如果自动装配模式设置为“按照名称装配”，则使用该工厂中其他bean的引用填充任何缺失的属性值。
	 *
	 * @param beanName 正在连接的bean的名称。
	 *                 用于调试消息；在功能上不使用。
	 * @param mbd      通过自动装配更新的bean定义
	 * @param bw       我们可以从中获取有关bean的信息的BeanWrapper
	 * @param pvs      用于注册已连接对象的PropertyValues
	 */
	protected void autowireByName(
			String beanName, AbstractBeanDefinition mbd, BeanWrapper bw, MutablePropertyValues pvs) {

		// 获取所有未满足的非简单属性的属性名数组
		String[] propertyNames = unsatisfiedNonSimpleProperties(mbd, bw);
		for (String propertyName : propertyNames) {
			// 如果容器中包含该属性名对应的bean，则进行按照名称自动装配
			if (containsBean(propertyName)) {
				// 获取容器中的bean，并将其添加到PropertyValues中
				Object bean = getBean(propertyName);
				pvs.add(propertyName, bean);
				// 注册依赖关系
				registerDependentBean(propertyName, beanName);
				if (logger.isTraceEnabled()) {
					logger.trace("Added autowiring by name from bean name '" + beanName +
							"' via property '" + propertyName + "' to bean named '" + propertyName + "'");
				}
			} else {
				// 如果容器中没有找到匹配的bean，则记录日志
				if (logger.isTraceEnabled()) {
					logger.trace("Not autowiring property '" + propertyName + "' of bean '" + beanName +
							"' by name: no matching bean found");
				}
			}
		}
	}

	/**
	 * 抽象方法定义“按类型自动装配”（按类型装配bean属性）的行为。
	 * <p>这类似于PicoContainer默认行为，在此行为中，bean工厂中必须有恰好一个与属性类型匹配的bean。
	 * 这使得bean工厂易于配置用于小型命名空间，但对于较大的应用程序而言，与标准Spring行为相比效果不佳。
	 *
	 * @param beanName 要按类型自动装配的bean的名称
	 * @param mbd      要通过自动装配更新的合并的bean定义
	 * @param bw       可以从中获取有关bean的信息的BeanWrapper
	 * @param pvs      用于注册已连接对象的PropertyValues
	 */
	protected void autowireByType(
			String beanName, AbstractBeanDefinition mbd, BeanWrapper bw, MutablePropertyValues pvs) {
		//获取类型转换器
		TypeConverter converter = getCustomTypeConverter();
		if (converter == null) {
			// 类型转换器为空，则将bean包装器设置为类型转换器
			converter = bw;
		}
		//自动装配bean名称
		Set<String> autowiredBeanNames = new LinkedHashSet<>(4);
		// 获取未满足条件的非简单属性名称
		String[] propertyNames = unsatisfiedNonSimpleProperties(mbd, bw);
		for (String propertyName : propertyNames) {
			try {
				// 获取属性描述
				PropertyDescriptor pd = bw.getPropertyDescriptor(propertyName);
				// 不要尝试按类型自动装配Object类型：即使它在技术上是一个未满足的、非简单的属性，也永远没有意义。
				if (Object.class != pd.getPropertyType()) {
					//如果该属性的类型不是Object类型
					//获取该属性的set方法参数
					MethodParameter methodParam = BeanUtils.getWriteMethodParameter(pd);
					// 在存在优先级排序的后处理器的情况下，不允许为类型匹配启用急切初始化。
					boolean eager = !(bw.getWrappedInstance() instanceof PriorityOrdered);
					// 创建依赖描述
					DependencyDescriptor desc = new AutowireByTypeDependencyDescriptor(methodParam, eager);
					// 解析依赖，获取自动装配参数
					Object autowiredArgument = resolveDependency(desc, beanName, autowiredBeanNames, converter);
					if (autowiredArgument != null) {
						// 添加到可变属性值对中
						pvs.add(propertyName, autowiredArgument);
					}
					for (String autowiredBeanName : autowiredBeanNames) {
						// 根据bean名称注册依赖
						registerDependentBean(autowiredBeanName, beanName);
						if (logger.isTraceEnabled()) {
							logger.trace("Autowiring by type from bean name '" + beanName + "' via property '" +
									propertyName + "' to bean named '" + autowiredBeanName + "'");
						}
					}
					// 清空自动装配的bean名称
					autowiredBeanNames.clear();
				}
			} catch (BeansException ex) {
				throw new UnsatisfiedDependencyException(mbd.getResourceDescription(), beanName, propertyName, ex);
			}
		}
	}


	/**
	 * 返回未满足的非简单bean属性数组。
	 * 这些可能是对工厂中其他bean的未满足引用。不包括简单属性，如基本类型或字符串。
	 *
	 * @param mbd 使用该bean定义创建的合并的bean定义
	 * @param bw  使用该BeanWrapper创建的BeanWrapper
	 * @return bean属性名称数组
	 * @see org.springframework.beans.BeanUtils#isSimpleProperty
	 */
	protected String[] unsatisfiedNonSimpleProperties(AbstractBeanDefinition mbd, BeanWrapper bw) {
		Set<String> result = new TreeSet<>();
		// 获取bean定义的属性值对
		PropertyValues pvs = mbd.getPropertyValues();
		// 从bean包装器获取属性描述
		PropertyDescriptor[] pds = bw.getPropertyDescriptors();
		for (PropertyDescriptor pd : pds) {
			if (pd.getWriteMethod() != null && !isExcludedFromDependencyCheck(pd) && !pvs.contains(pd.getName()) &&
					!BeanUtils.isSimpleProperty(pd.getPropertyType())) {
				// 如果该属性描述有可写方法，如果依赖检测中没有被忽略，
				// 并且属性值对中没有该属性名称，并且该属性类类型不是一个简单类，则添加该类名
				result.add(pd.getName());
			}
		}
		return StringUtils.toStringArray(result);
	}

	/**
	 * 从给定的BeanWrapper中提取一组过滤后的属性描述符，排除已忽略的依赖类型或在已忽略的依赖接口上定义的属性。
	 *
	 * @param bw    使用BeanWrapper创建的BeanWrapper
	 * @param cache 是否为给定的Bean类缓存过滤后的PropertyDescriptor
	 * @return 过滤后的PropertyDescriptor
	 * @see #isExcludedFromDependencyCheck
	 * @see #filterPropertyDescriptorsForDependencyCheck(org.springframework.beans.BeanWrapper)
	 */
	protected PropertyDescriptor[] filterPropertyDescriptorsForDependencyCheck(BeanWrapper bw, boolean cache) {
		// 从缓存中获取属性描述符数组
		PropertyDescriptor[] filtered = this.filteredPropertyDescriptorsCache.get(bw.getWrappedClass());
		// 如果缓存中不存在，则进行过滤，并将结果缓存起来
		if (filtered == null) {
			// 对属性描述符进行过滤以进行依赖检查
			filtered = filterPropertyDescriptorsForDependencyCheck(bw);
			// 如果需要缓存，则将结果放入缓存中
			if (cache) {
				PropertyDescriptor[] existing = this.filteredPropertyDescriptorsCache.putIfAbsent(bw.getWrappedClass(), filtered);
				// 如果已存在缓存，则使用已存在的结果
				if (existing != null) {
					filtered = existing;
				}
			}
		}
		return filtered;
	}

	/**
	 * 从给定的BeanWrapper中提取一组过滤后的属性描述符，排除已忽略的依赖类型或在已忽略的依赖接口上定义的属性。
	 *
	 * @param bw 使用BeanWrapper创建的BeanWrapper
	 * @return 过滤后的PropertyDescriptor
	 * @see #isExcludedFromDependencyCheck
	 */
	protected PropertyDescriptor[] filterPropertyDescriptorsForDependencyCheck(BeanWrapper bw) {
		List<PropertyDescriptor> pds = new ArrayList<>(Arrays.asList(bw.getPropertyDescriptors()));
		pds.removeIf(this::isExcludedFromDependencyCheck);
		return pds.toArray(new PropertyDescriptor[0]);
	}

	/**
	 * 确定给定的bean属性是否被排除在依赖性检查之外。
	 * <p>此实现排除由CGLIB定义的属性以及类型与被忽略的依赖类型匹配的属性，或者由被忽略的依赖接口定义的属性。
	 *
	 * @param pd bean属性的PropertyDescriptor
	 * @return bean属性是否被排除
	 * @see #ignoreDependencyType(Class)
	 * @see #ignoreDependencyInterface(Class)
	 */
	protected boolean isExcludedFromDependencyCheck(PropertyDescriptor pd) {
		return (AutowireUtils.isExcludedFromDependencyCheck(pd) ||
				this.ignoredDependencyTypes.contains(pd.getPropertyType()) ||
				AutowireUtils.isSetterDefinedInInterface(pd, this.ignoredDependencyInterfaces));
	}

	/**
	 * 如果需要，执行依赖性检查，确保所有公开的属性都已设置。
	 * 依赖性检查可以是对象（协作bean）、简单类型（基本类型和字符串）或全部（两者）。
	 *
	 * @param beanName bean的名称
	 * @param mbd      bean创建时使用的合并bean定义
	 * @param pds      目标bean的相关属性描述符
	 * @param pvs      要应用于bean的属性值
	 * @throws UnsatisfiedDependencyException 如果依赖项无法满足
	 * @see #isExcludedFromDependencyCheck(java.beans.PropertyDescriptor)
	 */
	protected void checkDependencies(
			String beanName, AbstractBeanDefinition mbd, PropertyDescriptor[] pds, @Nullable PropertyValues pvs)
			throws UnsatisfiedDependencyException {

		// 获取依赖检查模式
		int dependencyCheck = mbd.getDependencyCheck();
		// 遍历属性描述符数组
		for (PropertyDescriptor pd : pds) {
			// 如果属性有写方法且不在显式指定的属性值集合中
			if (pd.getWriteMethod() != null && (pvs == null || !pvs.contains(pd.getName()))) {
				// 检查属性类型是否是简单类型
				boolean isSimple = BeanUtils.isSimpleProperty(pd.getPropertyType());
				// 根据依赖检查模式判断是否不满足依赖
				boolean unsatisfied = (dependencyCheck == AbstractBeanDefinition.DEPENDENCY_CHECK_ALL) ||
						(isSimple && dependencyCheck == AbstractBeanDefinition.DEPENDENCY_CHECK_SIMPLE) ||
						(!isSimple && dependencyCheck == AbstractBeanDefinition.DEPENDENCY_CHECK_OBJECTS);
				// 如果不满足依赖，则抛出异常
				if (unsatisfied) {
					throw new UnsatisfiedDependencyException(mbd.getResourceDescription(), beanName, pd.getName(),
							"Set this property value or disable dependency checking for this bean.");
				}
			}
		}
	}

	/**
	 * 应用给定的属性值，解析此bean工厂中对其他bean的任何运行时引用。
	 * 必须使用深度复制，以便不会永久修改此属性。
	 *
	 * @param beanName bean的名称，用于提供更好的异常信息
	 * @param mbd      合并的bean定义
	 * @param bw       封装目标对象的BeanWrapper
	 * @param pvs      新的属性值
	 */
	protected void applyPropertyValues(String beanName, BeanDefinition mbd, BeanWrapper bw, PropertyValues pvs) {
		// 如果属性值为空，直接返回
		if (pvs.isEmpty()) {
			return;
		}

		// 如果启用了安全管理器，并且BeanWrapper是BeanWrapperImpl的实例，设置安全上下文
		if (System.getSecurityManager() != null && bw instanceof BeanWrapperImpl) {
			((BeanWrapperImpl) bw).setSecurityContext(getAccessControlContext());
		}

		MutablePropertyValues mpvs = null;
		List<PropertyValue> original;

		// 如果属性值是MutablePropertyValues的实例，直接使用，否则将其转换为List<PropertyValue>
		if (pvs instanceof MutablePropertyValues) {
			mpvs = (MutablePropertyValues) pvs;
			if (mpvs.isConverted()) {
				// 如果已转换，直接使用预转换的值
				try {
					//为实例化对象设置属性值，依赖注入的真正实现在这里
					bw.setPropertyValues(mpvs);
					return;
				} catch (BeansException ex) {
					throw new BeanCreationException(
							mbd.getResourceDescription(), beanName, "Error setting property values", ex);
				}
			}
			original = mpvs.getPropertyValueList();
		} else {
			// 如果属性值对不是 MutablePropertyValues 类型，则直接使用原始类型
			original = Arrays.asList(pvs.getPropertyValues());
		}

		// 获取自定义的类型转换器，如果没有，则使用BeanWrapper本身作为转换器
		TypeConverter converter = getCustomTypeConverter();
		if (converter == null) {
			converter = bw;
		}
		// 创建bean定义值解析器
		BeanDefinitionValueResolver valueResolver = new BeanDefinitionValueResolver(this, beanName, mbd, converter);

		// 创建深拷贝，解析值中的任何引用
		List<PropertyValue> deepCopy = new ArrayList<>(original.size());
		boolean resolveNecessary = false;
		for (PropertyValue pv : original) {
			if (pv.isConverted()) {
				deepCopy.add(pv);
			} else {
				// 属性名称
				String propertyName = pv.getName();
				// 原始值
				Object originalValue = pv.getValue();
				if (originalValue == AutowiredPropertyMarker.INSTANCE) {
					// 处理自动注入的标记
					// 获取set方法
					Method writeMethod = bw.getPropertyDescriptor(propertyName).getWriteMethod();
					if (writeMethod == null) {
						// 没有set方法，抛出异常
						throw new IllegalArgumentException("Autowire marker for property without write method: " + pv);
					}
					// 将原始值设置为依赖描述
					originalValue = new DependencyDescriptor(new MethodParameter(writeMethod, 0), true);
				}
				// 解析原始值，获取解析后的值
				Object resolvedValue = valueResolver.resolveValueIfNecessary(pv, originalValue);
				Object convertedValue = resolvedValue;
				boolean convertible = bw.isWritableProperty(propertyName) &&
						!PropertyAccessorUtils.isNestedOrIndexedProperty(propertyName);
				if (convertible) {
					// 如果属性可转换，进行类型转换
					convertedValue = convertForProperty(resolvedValue, propertyName, bw, converter);
				}
				// 可能将转换后的值存储在合并的Bean定义中，以避免为每个创建的Bean实例重新转换
				if (resolvedValue == originalValue) {
					//如果原始值和转换后的值相同
					if (convertible) {
						// 如果可以转换，设置转换后的值
						pv.setConvertedValue(convertedValue);
					}
					// 添加进深拷贝数组
					deepCopy.add(pv);
				} else if (convertible && originalValue instanceof TypedStringValue &&
						!((TypedStringValue) originalValue).isDynamic() &&
						!(convertedValue instanceof Collection || ObjectUtils.isArray(convertedValue))) {
					// 如果原始值是静态的TypedStringValue，并且转换后的值不是集合或数组，则将其设置为已转换的值
					pv.setConvertedValue(convertedValue);
					deepCopy.add(pv);
				} else {
					// 需要解析的情况，创建新的PropertyValue，并将其添加到深拷贝中
					resolveNecessary = true;
					deepCopy.add(new PropertyValue(pv, convertedValue));
				}
			}
		}
		// 如果是MutablePropertyValues，并且不需要解析，则设置为已转换
		if (mpvs != null && !resolveNecessary) {
			mpvs.setConverted();
		}

		// 设置深拷贝的属性值
		try {
			// 进行属性依赖注入，依赖注入的真正实现在这里
			bw.setPropertyValues(new MutablePropertyValues(deepCopy));
		} catch (BeansException ex) {
			throw new BeanCreationException(
					mbd.getResourceDescription(), beanName, "Error setting property values", ex);
		}
	}

	/**
	 * 将给定的值转换为指定目标属性。
	 *
	 * @param value        要转换的值
	 * @param propertyName 目标属性的名称
	 * @param bw           BeanWrapper 对象
	 * @param converter    类型转换器
	 * @return 转换后的对象
	 */
	@Nullable
	private Object convertForProperty(
			@Nullable Object value, String propertyName, BeanWrapper bw, TypeConverter converter) {

		// 如果转换器是 BeanWrapperImpl 类型，则调用其 convertForProperty 方法进行属性转换；
		// 这里主要是因为 BeanWrapperImpl 实现了 PropertyEditorRegistry 接口
		if (converter instanceof BeanWrapperImpl) {
			return ((BeanWrapperImpl) converter).convertForProperty(value, propertyName);
		} else {
			// 否则，获取属性描述符。
			PropertyDescriptor pd = bw.getPropertyDescriptor(propertyName);
			// 根据属性描述符获取可写方法
			MethodParameter methodParam = BeanUtils.getWriteMethodParameter(pd);
			// 使用转换器转换类型
			return converter.convertIfNecessary(value, pd.getPropertyType(), methodParam);
		}
	}


	/**
	 * 初始化给定的bean实例，应用工厂回调、初始化方法和bean后处理器。
	 * <p>对于传统定义的bean，从{@link #createBean}调用，对于现有bean实例，从{@link #initializeBean}调用。
	 *
	 * @param beanName 工厂中的bean名称（用于调试目的）
	 * @param bean     可能需要初始化的新bean实例
	 * @param mbd      bean创建时使用的bean定义（如果给定现有bean实例，也可以为{@code null}）
	 * @return 初始化的bean实例（可能被包装）
	 * @see BeanNameAware
	 * @see BeanClassLoaderAware
	 * @see BeanFactoryAware
	 * @see #applyBeanPostProcessorsBeforeInitialization
	 * @see #invokeInitMethods
	 * @see #applyBeanPostProcessorsAfterInitialization
	 */
	protected Object initializeBean(String beanName, Object bean, @Nullable RootBeanDefinition mbd) {
		// 如果安全管理器不为空
		if (System.getSecurityManager() != null) {
			// 通过特权访问执行invokeAwareMethods方法
			AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
				invokeAwareMethods(beanName, bean);
				return null;
			}, getAccessControlContext());
		} else {
			// 否则直接调用invokeAwareMethods方法
			invokeAwareMethods(beanName, bean);
		}

		// 将wrappedBean初始化为原始的bean
		Object wrappedBean = bean;
		// 如果不存在根bean定义 或 根bean定义不是合成对象
		if (mbd == null || !mbd.isSynthetic()) {
			// 调用Bean后置处理器的applyBeanPostProcessorsBeforeInitialization方法
			wrappedBean = applyBeanPostProcessorsBeforeInitialization(wrappedBean, beanName);
		}

		try {
			// 调用invokeInitMethods方法，执行初始化方法
			invokeInitMethods(beanName, wrappedBean, mbd);
		} catch (Throwable ex) {
			// 捕获初始化方法抛出的异常，封装为BeanCreationException并抛出
			throw new BeanCreationException(
					(mbd != null ? mbd.getResourceDescription() : null),
					beanName, "Invocation of init method failed", ex);
		}

		// 如果不存在根bean定义 或 根bean定义不是合成对象
		if (mbd == null || !mbd.isSynthetic()) {
			// 调用Bean后置处理器的applyBeanPostProcessorsAfterInitialization方法
			wrappedBean = applyBeanPostProcessorsAfterInitialization(wrappedBean, beanName);
		}

		// 返回处理后的bean包装对象
		return wrappedBean;
	}

	/**
	 * 调用bean的感知方法（Aware methods）。
	 * 如果bean实现了Aware接口，会调用相应的方法。
	 *
	 * @param beanName bean的名称（用于调试目的）
	 * @param bean     bean实例
	 */
	private void invokeAwareMethods(String beanName, Object bean) {
		// 如果bean实现了Aware接口
		if (bean instanceof Aware) {
			// 如果bean同时实现了BeanNameAware接口
			if (bean instanceof BeanNameAware) {
				// 调用setBeanName方法设置bean的名称
				((BeanNameAware) bean).setBeanName(beanName);
			}
			// 如果bean同时实现了BeanClassLoaderAware接口
			if (bean instanceof BeanClassLoaderAware) {
				// 获取Bean类加载器
				ClassLoader bcl = getBeanClassLoader();
				// 如果Bean类加载器不为空，则调用setBeanClassLoader方法设置Bean类加载器
				if (bcl != null) {
					((BeanClassLoaderAware) bean).setBeanClassLoader(bcl);
				}
			}
			// 如果bean同时实现了BeanFactoryAware接口
			if (bean instanceof BeanFactoryAware) {
				// 调用setBeanFactory方法设置BeanFactory为当前AbstractAutowireCapableBeanFactory
				((BeanFactoryAware) bean).setBeanFactory(AbstractAutowireCapableBeanFactory.this);
			}
		}
	}

	/**
	 * 给予bean在所有属性设置完成后作出反应的机会，并了解其所属的Bean工厂（即这个对象）。
	 * 这意味着检查bean是否实现了InitializingBean接口或定义了自定义的初始化方法，
	 * 如果是，则调用必要的回调方法。
	 *
	 * @param beanName 在工厂中的bean名称（用于调试目的）
	 * @param bean     可能需要初始化的新bean实例
	 * @param mbd      bean创建时使用的合并的bean定义
	 *                 （如果给定了现有bean实例，则可能为null）
	 * @throws Throwable 如果由init方法或调用过程引发
	 * @see #invokeCustomInitMethod
	 */
	protected void invokeInitMethods(String beanName, Object bean, @Nullable RootBeanDefinition mbd)
			throws Throwable {

		// 检查bean是否实现了InitializingBean接口
		boolean isInitializingBean = (bean instanceof InitializingBean);

		// 如果是InitializingBean，并且bean没有外部管理的afterPropertiesSet方法，则调用其afterPropertiesSet方法
		if (isInitializingBean && (mbd == null || !mbd.hasAnyExternallyManagedInitMethod("afterPropertiesSet"))) {
			if (logger.isTraceEnabled()) {
				logger.trace("Invoking afterPropertiesSet() on bean with name '" + beanName + "'");
			}

			// 使用特权访问方式调用afterPropertiesSet方法
			if (System.getSecurityManager() != null) {
				try {
					AccessController.doPrivileged((PrivilegedExceptionAction<Object>) () -> {
						//调用 afterPropertiesSet 方法
						((InitializingBean) bean).afterPropertiesSet();
						return null;
					}, getAccessControlContext());
				} catch (PrivilegedActionException pae) {
					throw pae.getException();
				}
			} else {
				// 直接调用afterPropertiesSet方法
				((InitializingBean) bean).afterPropertiesSet();
			}
		}

		// 如果存在BeanDefinition，并且bean的类不是NullBean，则检查initMethodName，并调用自定义的初始化方法
		if (mbd != null && bean.getClass() != NullBean.class) {
			// 获取初始化方法名称
			String initMethodName = mbd.getInitMethodName();
			if (StringUtils.hasLength(initMethodName) &&
					!(isInitializingBean && "afterPropertiesSet".equals(initMethodName)) &&
					!mbd.hasAnyExternallyManagedInitMethod(initMethodName)) {
				// 并且bean没有外部管理的初始化方法名称的方法
				// 调用自定义的初始化方法
				invokeCustomInitMethod(beanName, bean, mbd);
			}
		}
	}

	/**
	 * 在给定的bean上调用指定的自定义初始化方法。
	 * 由invokeInitMethods调用。
	 * <p>可以在子类中覆盖，以自定义解析带有参数的init方法。
	 *
	 * @see #invokeInitMethods
	 */
	protected void invokeCustomInitMethod(String beanName, Object bean, RootBeanDefinition mbd)
			throws Throwable {

		// 获取初始化方法的名称
		String initMethodName = mbd.getInitMethodName();
		// 检查是否设置了初始化方法，如果没有，则抛出异常
		Assert.state(initMethodName != null, "No init method set");
		// 根据是否允许访问非公共方法选择查找方法的方式
		Method initMethod = (mbd.isNonPublicAccessAllowed() ?
				BeanUtils.findMethod(bean.getClass(), initMethodName) :
				ClassUtils.getMethodIfAvailable(bean.getClass(), initMethodName));

		// 如果找不到初始化方法，根据是否强制执行抛出异常或记录日志
		if (initMethod == null) {
			if (mbd.isEnforceInitMethod()) {
				throw new BeanDefinitionValidationException("Could not find an init method named '" +
						initMethodName + "' on bean with name '" + beanName + "'");
			} else {
				if (logger.isTraceEnabled()) {
					logger.trace("No default init method named '" + initMethodName +
							"' found on bean with name '" + beanName + "'");
				}
				// 忽略不存在的默认生命周期方法
				return;
			}
		}

		// 如果日志启用，记录将要调用的初始化方法
		if (logger.isTraceEnabled()) {
			logger.trace("Invoking init method  '" + initMethodName + "' on bean with name '" + beanName + "'");
		}

		// 获取要调用的方法，考虑接口中的方法
		Method methodToInvoke = ClassUtils.getInterfaceMethodIfPossible(initMethod, bean.getClass());

		// 如果启用了安全管理器，使用特权块进行访问控制和调用
		if (System.getSecurityManager() != null) {
			// 使用特权块使方法可访问
			AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
				ReflectionUtils.makeAccessible(methodToInvoke);
				return null;
			});
			try {
				// 使用特权块调用初始化方法
				AccessController.doPrivileged((PrivilegedExceptionAction<Object>)
						() -> methodToInvoke.invoke(bean), getAccessControlContext());
			} catch (PrivilegedActionException pae) {
				// 处理特权块中的异常
				InvocationTargetException ex = (InvocationTargetException) pae.getException();
				throw ex.getTargetException();
			}
		} else {
			try {
				// 否则，直接使方法可访问并调用初始化方法
				ReflectionUtils.makeAccessible(methodToInvoke);
				methodToInvoke.invoke(bean);
			} catch (InvocationTargetException ex) {
				// 处理调用初始化方法时的异常
				throw ex.getTargetException();
			}
		}
	}


	/**
	 * 应用所有已注册BeanPostProcessors的 {@code postProcessAfterInitialization} 回调，
	 * 使它们有机会对从FactoryBeans获得的对象进行后处理 (例如，自动代理它们)。
	 *
	 * @see #applyBeanPostProcessorsAfterInitialization
	 */
	@Override
	protected Object postProcessObjectFromFactoryBean(Object object, String beanName) {
		//初始化后应用Bean Post处理器
		return applyBeanPostProcessorsAfterInitialization(object, beanName);
	}

	/**
	 * 重写以清除 FactoryBean 实例缓存。
	 *
	 * @param beanName 要移除的bean名称
	 */
	@Override
	protected void removeSingleton(String beanName) {
		synchronized (getSingletonMutex()) {
			super.removeSingleton(beanName);
			// 移除未完成的FactoryBean实例缓存
			this.factoryBeanInstanceCache.remove(beanName);
		}
	}

	/**
	 * 重写以清除 FactoryBean 实例缓存。
	 */
	@Override
	protected void clearSingletonCache() {
		synchronized (getSingletonMutex()) {
			super.clearSingletonCache();
			this.factoryBeanInstanceCache.clear();
		}
	}

	/**
	 * 将日志记录器暴露给协作委托。
	 *
	 * @since 5.0.7
	 */
	Log getLogger() {
		return logger;
	}


	/**
	 * Spring 的经典 autowire="byType" 模式的特殊 DependencyDescriptor 变体。
	 * 总是可选的；从不考虑参数名称来选择主要的候选项。
	 */
	@SuppressWarnings("serial")
	private static class AutowireByTypeDependencyDescriptor extends DependencyDescriptor {

		public AutowireByTypeDependencyDescriptor(MethodParameter methodParameter, boolean eager) {
			super(methodParameter, false, eager);
		}

		@Override
		public String getDependencyName() {
			return null;
		}
	}


	/**
	 * 用于查找 {@link FactoryBean} 类型信息的 {@link MethodCallback}。
	 */
	private static class FactoryBeanMethodTypeFinder implements MethodCallback {
		/**
		 * 工厂方法名称
		 */
		private final String factoryMethodName;

		private ResolvableType result = ResolvableType.NONE;

		FactoryBeanMethodTypeFinder(String factoryMethodName) {
			this.factoryMethodName = factoryMethodName;
		}

		@Override
		public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
			// 检查方法是否是 FactoryBean 方法
			if (isFactoryBeanMethod(method)) {
				// 获取方法返回类型的 ResolvableType
				ResolvableType returnType = ResolvableType.forMethodReturnType(method);
				// 获取 FactoryBean 返回值的泛型类型
				ResolvableType candidate = returnType.as(FactoryBean.class).getGeneric();
				// 如果结果为 ResolvableType.NONE，则直接赋值为 candidate
				if (this.result == ResolvableType.NONE) {
					this.result = candidate;
				} else {
					// 否则，尝试确定结果与 candidate 的最近共同祖先类型
					Class<?> resolvedResult = this.result.resolve();
					Class<?> commonAncestor = ClassUtils.determineCommonAncestor(candidate.resolve(), resolvedResult);
					// 如果最近共同祖先类型与结果不相等，则更新结果为最近共同祖先类型
					if (!ObjectUtils.nullSafeEquals(resolvedResult, commonAncestor)) {
						this.result = ResolvableType.forClass(commonAncestor);
					}
				}
			}
		}

		private boolean isFactoryBeanMethod(Method method) {
			return (method.getName().equals(this.factoryMethodName) &&
					FactoryBean.class.isAssignableFrom(method.getReturnType()));
		}

		ResolvableType getResult() {
			Class<?> resolved = this.result.resolve();
			boolean foundResult = resolved != null && resolved != Object.class;
			return (foundResult ? this.result : ResolvableType.NONE);
		}
	}

}
