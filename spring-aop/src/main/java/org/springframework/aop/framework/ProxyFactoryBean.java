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

package org.springframework.aop.framework;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.Interceptor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.Advisor;
import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.adapter.AdvisorAdapterRegistry;
import org.springframework.aop.framework.adapter.GlobalAdvisorAdapterRegistry;
import org.springframework.aop.framework.adapter.UnknownAdviceTypeException;
import org.springframework.aop.target.SingletonTargetSource;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.FactoryBeanNotInitializedException;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * {@link org.springframework.beans.factory.FactoryBean} 实现，基于 Spring
 * {@link org.springframework.beans.factory.BeanFactory} 中的 bean 构建 AOP 代理。
 *
 * <p>{@link org.aopalliance.intercept.MethodInterceptor MethodInterceptors} 和
 * {@link org.springframework.aop.Advisor Advisors} 通过当前 bean 工厂中的 bean
 * 名称列表识别，该列表通过 "interceptorNames" 属性指定。
 * 列表中的最后一项可以是目标 bean 或
 * {@link org.springframework.aop.TargetSource} 的名称；不过通常最好
 * 改用 "targetName"/"target"/"targetSource" 属性。
 *
 * <p>可以在工厂级别添加全局拦截器和 advisor。当拦截器列表中包含
 * "xxx*" 项时，指定的全局项会被展开到该列表中，按给定前缀匹配
 * bean 名称（例如 "global*" 将同时匹配 "globalBean1" 和
 * "globalBean2"，"*" 匹配所有已定义的拦截器）。匹配到的
 * 拦截器如果实现了 {@link org.springframework.core.Ordered} 接口，
 * 将按照其返回的顺序值应用。
 *
 * <p>给定代理接口时创建 JDK 代理；否则为实际目标类创建 CGLIB 代理。
 * 注意，后者只有在目标类没有 final 方法时才可工作，因为运行时
 * 会创建动态子类。
 *
 * <p>可以将从此工厂获得的代理强制转换为 {@link Advised}，
 * 或获取 ProxyFactoryBean 引用并以编程方式操作它。
 * 这不适用于已有的 prototype 引用，因为它们彼此独立。不过，
 * 它适用于随后从工厂获得的 prototype。拦截变更会立即作用于
 * singleton（包括已有引用）。但是，要更改接口或目标，必须从
 * 工厂获取新实例。这意味着从工厂获得的 singleton 实例并不具有
 * 相同的对象标识。不过，它们确实具有相同的拦截器和目标，
 * 并且更改任何引用都会更改所有对象。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #setInterceptorNames
 * @see #setProxyInterfaces
 * @see org.aopalliance.intercept.MethodInterceptor
 * @see org.springframework.aop.Advisor
 * @see Advised
 */
@SuppressWarnings("serial")
public class ProxyFactoryBean extends ProxyCreatorSupport
		implements FactoryBean<Object>, BeanClassLoaderAware, BeanFactoryAware {

	/**
	 * 拦截器列表值中的此后缀表示展开全局项。
	 */
	public static final String GLOBAL_SUFFIX = "*";


	protected final Log logger = LogFactory.getLog(getClass());

	@Nullable
	private String[] interceptorNames;

	@Nullable
	private String targetName;

	private boolean autodetectInterfaces = true;

	private boolean singleton = true;

	private AdvisorAdapterRegistry advisorAdapterRegistry = GlobalAdvisorAdapterRegistry.getInstance();

	private boolean freezeProxy = false;

	@Nullable
	private transient ClassLoader proxyClassLoader = ClassUtils.getDefaultClassLoader();

	private transient boolean classLoaderConfigured = false;

	@Nullable
	private transient BeanFactory beanFactory;

	/** 是否已初始化 advisor 链。 */
	private boolean advisorChainInitialized = false;

	/** 如果这是 singleton，则为缓存的 singleton 代理实例。 */
	@Nullable
	private Object singletonInstance;


	/**
	 * 设置要代理的接口名称。如果未给定接口，
	 * 将为实际类创建 CGLIB 代理。
	 * <p>这本质上等同于 "setInterfaces" 方法，
	 * 但映射了 TransactionProxyFactoryBean 的 "setProxyInterfaces"。
	 * @see #setInterfaces
	 * @see AbstractSingletonProxyFactoryBean#setProxyInterfaces
	 */
	public void setProxyInterfaces(Class<?>[] proxyInterfaces) throws ClassNotFoundException {
		setInterfaces(proxyInterfaces);
	}

	/**
	 * 设置 Advice/Advisor bean 名称列表。在 bean 工厂中使用此 factory bean 时，
	 * 必须始终设置该属性。
	 * <p>引用的 bean 应为 Interceptor、Advisor 或 Advice 类型。
	 * 列表中的最后一项可以是工厂中任意 bean 的名称。
	 * 如果它既不是 Advice 也不是 Advisor，则会添加新的 SingletonTargetSource
	 * 来包装它。如果设置了 "target"、"targetSource" 或 "targetName" 属性，
	 * 则不能使用这样的目标 bean，此时 "interceptorNames" 数组必须只包含
	 * Advice/Advisor bean 名称。
	 * <p><b>注意：在 "interceptorNames" 列表中将目标 bean 指定为最后一个名称
	 * 已被弃用，并将在未来的 Spring 版本中移除。</b>
	 * 请改用 {@link #setTargetName "targetName"} 属性。
	 * @see org.aopalliance.intercept.MethodInterceptor
	 * @see org.springframework.aop.Advisor
	 * @see org.aopalliance.aop.Advice
	 * @see org.springframework.aop.target.SingletonTargetSource
	 */
	public void setInterceptorNames(String... interceptorNames) {
		this.interceptorNames = interceptorNames;
	}

	/**
	 * 设置目标 bean 的名称。这是将目标名称指定在 "interceptorNames"
	 * 数组末尾的替代方式。
	 * <p>也可以分别通过 "target"/"targetSource" 属性，
	 * 直接指定目标对象或 TargetSource 对象。
	 * @see #setInterceptorNames(String[])
	 * @see #setTarget(Object)
	 * @see #setTargetSource(org.springframework.aop.TargetSource)
	 */
	public void setTargetName(String targetName) {
		this.targetName = targetName;
	}

	/**
	 * 设置在未指定代理接口时是否自动检测代理接口。
	 * <p>默认值为 "true"。关闭此标志可在未指定接口时
	 * 为完整目标类创建 CGLIB 代理。
	 * @see #setProxyTargetClass
	 */
	public void setAutodetectInterfaces(boolean autodetectInterfaces) {
		this.autodetectInterfaces = autodetectInterfaces;
	}

	/**
	 * 设置 singleton 属性的值。控制此工厂是否应始终返回同一个代理实例
	 * （这意味着相同的目标），或者是否应返回新的 prototype 实例，
	 * 这意味着如果目标和拦截器来自 prototype bean 定义，
	 * 它们也可以是新实例。这允许精细控制对象图中的独立性/唯一性。
	 */
	public void setSingleton(boolean singleton) {
		this.singleton = singleton;
	}

	/**
	 * 指定要使用的 AdvisorAdapterRegistry。
	 * 默认值为全局 AdvisorAdapterRegistry。
	 * @see org.springframework.aop.framework.adapter.GlobalAdvisorAdapterRegistry
	 */
	public void setAdvisorAdapterRegistry(AdvisorAdapterRegistry advisorAdapterRegistry) {
		this.advisorAdapterRegistry = advisorAdapterRegistry;
	}

	@Override
	public void setFrozen(boolean frozen) {
		this.freezeProxy = frozen;
	}

	/**
	 * 设置用于生成代理类的 ClassLoader。
	 * <p>默认值为 bean ClassLoader，即包含该 bean 的 BeanFactory
	 * 用于加载所有 bean 类的 ClassLoader。可在此处为特定代理覆盖。
	 */
	public void setProxyClassLoader(@Nullable ClassLoader classLoader) {
		this.proxyClassLoader = classLoader;
		this.classLoaderConfigured = (classLoader != null);
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		if (!this.classLoaderConfigured) {
			this.proxyClassLoader = classLoader;
		}
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
		checkInterceptorNames();
	}


	/**
	 * 返回代理。客户端从此 factory bean 获取 bean 时调用。
	 * 创建要由此工厂返回的 AOP 代理实例。
	 * 对于 singleton，该实例将被缓存；对于代理，则会在每次调用
	 * {@code getObject()} 时创建。
	 * @return 反映此工厂当前状态的全新 AOP 代理
	 */
	@Override
	@Nullable
	public Object getObject() throws BeansException {
		initializeAdvisorChain();
		if (isSingleton()) {
			return getSingletonInstance();
		}
		else {
			if (this.targetName == null) {
				logger.info("Using non-singleton proxies with singleton targets is often undesirable. " +
						"Enable prototype proxies by setting the 'targetName' property.");
			}
			return newPrototypeInstance();
		}
	}

	/**
	 * 返回代理的类型。如果已创建 singleton 实例，则检查该实例；
	 * 否则回退到代理接口（只有一个时）、目标 bean 类型，
	 * 或 TargetSource 的目标类。
	 * @see org.springframework.aop.TargetSource#getTargetClass
	 */
	@Override
	public Class<?> getObjectType() {
		synchronized (this) {
			if (this.singletonInstance != null) {
				return this.singletonInstance.getClass();
			}
		}
		Class<?>[] ifcs = getProxiedInterfaces();
		if (ifcs.length == 1) {
			return ifcs[0];
		}
		else if (ifcs.length > 1) {
			return createCompositeInterface(ifcs);
		}
		else if (this.targetName != null && this.beanFactory != null) {
			return this.beanFactory.getType(this.targetName);
		}
		else {
			return getTargetClass();
		}
	}

	@Override
	public boolean isSingleton() {
		return this.singleton;
	}


	/**
	 * 为给定接口创建组合接口 Class，
	 * 在一个 Class 中实现这些给定接口。
	 * <p>默认实现会为给定接口构建 JDK 代理类。
	 * @param interfaces 要合并的接口
	 * @return 作为 Class 的合并后接口
	 * @see java.lang.reflect.Proxy#getProxyClass
	 */
	protected Class<?> createCompositeInterface(Class<?>[] interfaces) {
		return ClassUtils.createCompositeInterface(interfaces, this.proxyClassLoader);
	}

	/**
	 * 返回此类代理对象的 singleton 实例，
	 * 如果尚未创建则延迟创建它。
	 * @return 共享的 singleton 代理
	 */
	private synchronized Object getSingletonInstance() {
		if (this.singletonInstance == null) {
			this.targetSource = freshTargetSource();
			if (this.autodetectInterfaces && getProxiedInterfaces().length == 0 && !isProxyTargetClass()) {
				// 依赖 AOP 基础设施告诉我们要代理哪些接口。
				Class<?> targetClass = getTargetClass();
				if (targetClass == null) {
					throw new FactoryBeanNotInitializedException("Cannot determine target class for proxy");
				}
				setInterfaces(ClassUtils.getAllInterfacesForClass(targetClass, this.proxyClassLoader));
			}
			// 初始化共享的 singleton 实例。
			super.setFrozen(this.freezeProxy);
			this.singletonInstance = getProxy(createAopProxy());
		}
		return this.singletonInstance;
	}

	/**
	 * 创建此类所创建代理对象的新 prototype 实例，
	 * 由独立的 AdvisedSupport 配置提供支持。
	 * @return 完全独立的代理，可单独操作其 advice
	 */
	private synchronized Object newPrototypeInstance() {
		// 在 prototype 的情况下，需要为代理提供
		// 一个独立的配置实例。
		// 在这种情况下，没有代理会拥有此对象配置的实例，
		// 而是拥有一个独立副本。
		ProxyCreatorSupport copy = new ProxyCreatorSupport(getAopProxyFactory());

		// 副本需要全新的 advisor 链和全新的 TargetSource。
		TargetSource targetSource = freshTargetSource();
		copy.copyConfigurationFrom(this, targetSource, freshAdvisorChain());
		if (this.autodetectInterfaces && getProxiedInterfaces().length == 0 && !isProxyTargetClass()) {
			// 依赖 AOP 基础设施告诉我们要代理哪些接口。
			Class<?> targetClass = targetSource.getTargetClass();
			if (targetClass != null) {
				copy.setInterfaces(ClassUtils.getAllInterfacesForClass(targetClass, this.proxyClassLoader));
			}
		}
		copy.setFrozen(this.freezeProxy);

		return getProxy(copy.createAopProxy());
	}

	/**
	 * 返回要暴露的代理对象。
	 * <p>默认实现使用工厂的 bean 类加载器调用 {@code getProxy}。
	 * 可以重写以指定自定义类加载器。
	 * @param aopProxy 要从中获取代理的已准备 AopProxy 实例
	 * @return 要暴露的代理对象
	 * @see AopProxy#getProxy(ClassLoader)
	 */
	protected Object getProxy(AopProxy aopProxy) {
		return aopProxy.getProxy(this.proxyClassLoader);
	}

	/**
	 * 检查 interceptorNames 列表是否以目标名称作为最后一个元素。
	 * 如果找到，则从列表中移除最后一个名称并将其设置为 targetName。
	 */
	private void checkInterceptorNames() {
		if (!ObjectUtils.isEmpty(this.interceptorNames)) {
			String finalName = this.interceptorNames[this.interceptorNames.length - 1];
			if (this.targetName == null && this.targetSource == EMPTY_TARGET_SOURCE) {
				// 链中的最后一个名称可能是 Advisor/Advice 或目标/TargetSource。
				// 遗憾的是我们并不知道；必须查看 bean 的类型。
				if (!finalName.endsWith(GLOBAL_SUFFIX) && !isNamedBeanAnAdvisorOrAdvice(finalName)) {
					// 目标不是拦截器。
					this.targetName = finalName;
					if (logger.isDebugEnabled()) {
						logger.debug("Bean with name '" + finalName + "' concluding interceptor chain " +
								"is not an advisor class: treating it as a target or TargetSource");
					}
					this.interceptorNames = Arrays.copyOf(this.interceptorNames, this.interceptorNames.length - 1);
				}
			}
		}
	}

	/**
	 * 查看 bean 工厂元数据，以确定此 bean 名称
	 * （它结束 interceptorNames 列表）是 Advisor、Advice，
	 * 还是可能是目标。
	 * @param beanName 要检查的 bean 名称
	 * @return 如果它是 Advisor 或 Advice，则为 {@code true}
	 */
	private boolean isNamedBeanAnAdvisorOrAdvice(String beanName) {
		Assert.state(this.beanFactory != null, "No BeanFactory set");
		Class<?> namedBeanClass = this.beanFactory.getType(beanName);
		if (namedBeanClass != null) {
			return (Advisor.class.isAssignableFrom(namedBeanClass) || Advice.class.isAssignableFrom(namedBeanClass));
		}
		// 如果无法判断，则将其视为目标 bean。
		if (logger.isDebugEnabled()) {
			logger.debug("Could not determine type of bean with name '" + beanName +
					"' - assuming it is neither an Advisor nor an Advice");
		}
		return false;
	}

	/**
	 * 创建 advisor（拦截器）链。每次添加新的 prototype 实例时，
	 * 从 BeanFactory 获取的 advisor 都将刷新。
	 * 通过工厂 API 以编程方式添加的拦截器不受此类变更影响。
	 */
	private synchronized void initializeAdvisorChain() throws AopConfigException, BeansException {
		if (!this.advisorChainInitialized && !ObjectUtils.isEmpty(this.interceptorNames)) {
			if (this.beanFactory == null) {
				throw new IllegalStateException("No BeanFactory available anymore (probably due to serialization) " +
						"- cannot resolve interceptor names " + Arrays.toString(this.interceptorNames));
			}

			// 除非我们使用属性指定了 targetSource，否则 Globals 不能位于最后...
			if (this.interceptorNames[this.interceptorNames.length - 1].endsWith(GLOBAL_SUFFIX) &&
					this.targetName == null && this.targetSource == EMPTY_TARGET_SOURCE) {
				throw new AopConfigException("Target required after globals");
			}

			// 从 bean 名称实体化拦截器链。
			for (String name : this.interceptorNames) {
				if (name.endsWith(GLOBAL_SUFFIX)) {
					if (!(this.beanFactory instanceof ListableBeanFactory)) {
						throw new AopConfigException(
								"Can only use global advisors or interceptors with a ListableBeanFactory");
					}
					addGlobalAdvisors((ListableBeanFactory) this.beanFactory,
							name.substring(0, name.length() - GLOBAL_SUFFIX.length()));
				}

				else {
					// 如果到达这里，则需要添加一个命名拦截器。
					// 必须检查它是 singleton 还是 prototype。
					Object advice;
					if (this.singleton || this.beanFactory.isSingleton(name)) {
						// 将实际的 Advisor/Advice 添加到链中。
						advice = this.beanFactory.getBean(name);
					}
					else {
						// 它是 prototype Advice 或 Advisor：替换为 prototype。
						// 避免仅为了 advisor 链初始化而不必要地创建 prototype bean。
						advice = new PrototypePlaceholderAdvisor(name);
					}
					addAdvisorOnChainCreation(advice);
				}
			}

			this.advisorChainInitialized = true;
		}
	}


	/**
	 * 返回独立的 advisor 链。
	 * 每次返回新的 prototype 实例时都需要这样做，
	 * 以便返回不同的 prototype Advisors 和 Advices 实例。
	 */
	private List<Advisor> freshAdvisorChain() {
		Advisor[] advisors = getAdvisors();
		List<Advisor> freshAdvisors = new ArrayList<>(advisors.length);
		for (Advisor advisor : advisors) {
			if (advisor instanceof PrototypePlaceholderAdvisor) {
				PrototypePlaceholderAdvisor pa = (PrototypePlaceholderAdvisor) advisor;
				if (logger.isDebugEnabled()) {
					logger.debug("Refreshing bean named '" + pa.getBeanName() + "'");
				}
				// 将占位符替换为通过 getBean 查找得到的全新 prototype 实例
				if (this.beanFactory == null) {
					throw new IllegalStateException("No BeanFactory available anymore (probably due to " +
							"serialization) - cannot resolve prototype advisor '" + pa.getBeanName() + "'");
				}
				Object bean = this.beanFactory.getBean(pa.getBeanName());
				Advisor refreshedAdvisor = namedBeanToAdvisor(bean);
				freshAdvisors.add(refreshedAdvisor);
			}
			else {
				// 添加共享实例。
				freshAdvisors.add(advisor);
			}
		}
		return freshAdvisors;
	}

	/**
	 * 添加所有全局拦截器和切点。
	 */
	private void addGlobalAdvisors(ListableBeanFactory beanFactory, String prefix) {
		String[] globalAdvisorNames =
				BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, Advisor.class);
		String[] globalInterceptorNames =
				BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, Interceptor.class);
		if (globalAdvisorNames.length > 0 || globalInterceptorNames.length > 0) {
			List<Object> beans = new ArrayList<>(globalAdvisorNames.length + globalInterceptorNames.length);
			for (String name : globalAdvisorNames) {
				if (name.startsWith(prefix)) {
					beans.add(beanFactory.getBean(name));
				}
			}
			for (String name : globalInterceptorNames) {
				if (name.startsWith(prefix)) {
					beans.add(beanFactory.getBean(name));
				}
			}
			AnnotationAwareOrderComparator.sort(beans);
			for (Object bean : beans) {
				addAdvisorOnChainCreation(bean);
			}
		}
	}

	/**
	 * 在 advice 链创建时调用。
	 * <p>将给定 advice、advisor 或对象添加到拦截器列表。
	 * 由于存在这三种可能性，无法对签名使用更强类型。
	 * @param next advice、advisor 或目标对象
	 */
	private void addAdvisorOnChainCreation(Object next) {
		// 必要时需要转换为 Advisor，以便我们的源引用
		// 与从超类拦截器中找到的内容匹配。
		addAdvisor(namedBeanToAdvisor(next));
	}

	/**
	 * 返回创建代理时要使用的 TargetSource。如果目标不是在 interceptorNames
	 * 列表末尾指定的，则 TargetSource 将是此类的 TargetSource 成员。
	 * 否则，我们会获取目标 bean，并在必要时将其包装在 TargetSource 中。
	 */
	private TargetSource freshTargetSource() {
		if (this.targetName == null) {
			// 不刷新目标：未在 'interceptorNames' 中指定 bean 名称
			return this.targetSource;
		}
		else {
			if (this.beanFactory == null) {
				throw new IllegalStateException("No BeanFactory available anymore (probably due to serialization) " +
						"- cannot resolve target with name '" + this.targetName + "'");
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Refreshing target with name '" + this.targetName + "'");
			}
			Object target = this.beanFactory.getBean(this.targetName);
			return (target instanceof TargetSource ? (TargetSource) target : new SingletonTargetSource(target));
		}
	}

	/**
	 * 将以下对象转换为 Advisor 或 TargetSource：该对象来源于对
	 * interceptorNames 数组中某个名称调用 getBean()。
	 */
	private Advisor namedBeanToAdvisor(Object next) {
		try {
			return this.advisorAdapterRegistry.wrap(next);
		}
		catch (UnknownAdviceTypeException ex) {
			// 我们期望它是 Advisor 或 Advice，
			// 但它不是。这是配置错误。
			throw new AopConfigException("Unknown advisor type " + next.getClass() +
					"; can only include Advisor or Advice type beans in interceptorNames chain " +
					"except for last entry which may also be target instance or TargetSource", ex);
		}
	}

	/**
	 * 在 advice 更改时丢弃并重新缓存 singleton。
	 */
	@Override
	protected void adviceChanged() {
		super.adviceChanged();
		if (this.singleton) {
			logger.debug("Advice has changed; re-caching singleton instance");
			synchronized (this) {
				this.singletonInstance = null;
			}
		}
	}


	//---------------------------------------------------------------------
	// 序列化支持
	//---------------------------------------------------------------------

	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		// 依赖默认序列化；只在反序列化后初始化状态。
		ois.defaultReadObject();

		// 初始化 transient 字段。
		this.proxyClassLoader = ClassUtils.getDefaultClassLoader();
	}


	/**
	 * 当创建代理时需要用 prototype 替换 bean 时，
	 * 用于拦截器链中。
	 */
	private static class PrototypePlaceholderAdvisor implements Advisor, Serializable {

		private final String beanName;

		private final String message;

		public PrototypePlaceholderAdvisor(String beanName) {
			this.beanName = beanName;
			this.message = "Placeholder for prototype Advisor/Advice with bean name '" + beanName + "'";
		}

		public String getBeanName() {
			return this.beanName;
		}

		@Override
		public Advice getAdvice() {
			throw new UnsupportedOperationException("Cannot invoke methods: " + this.message);
		}

		@Override
		public boolean isPerInstance() {
			throw new UnsupportedOperationException("Cannot invoke methods: " + this.message);
		}

		@Override
		public String toString() {
			return this.message;
		}
	}

}
