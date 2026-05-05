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

package org.springframework.aop.framework.autoproxy;

import org.aopalliance.aop.Advice;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.Advisor;
import org.springframework.aop.Pointcut;
import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.AopInfrastructureBean;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.framework.ProxyProcessorSupport;
import org.springframework.aop.framework.adapter.AdvisorAdapterRegistry;
import org.springframework.aop.framework.adapter.GlobalAdvisorAdapterRegistry;
import org.springframework.aop.target.SingletonTargetSource;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.SmartInstantiationAwareBeanPostProcessor;
import org.springframework.core.SmartClassLoader;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link org.springframework.beans.factory.config.BeanPostProcessor} 实现，
 * 它使用 AOP 代理包装每个合格 bean，在调用 bean 本身之前委托给指定拦截器。
 *
 * <p>此类区分“通用”拦截器和“特定”拦截器：前者由它创建的所有代理共享，
 * 后者对每个 bean 实例唯一。可以没有任何通用拦截器。如果存在，
 * 它们通过 interceptorNames 属性设置。与
 * {@link org.springframework.aop.framework.ProxyFactoryBean} 一样，
 * 使用当前工厂中的拦截器名称而不是 bean 引用，以便正确处理
 * prototype advisor 和拦截器：例如，支持有状态 mixin。
 * {@link #setInterceptorNames "interceptorNames"} 条目支持任何 advice 类型。
 *
 * <p>如果有大量 bean 需要使用相似代理进行包装（即委托给相同拦截器），
 * 这种自动代理尤其有用。无需为 x 个目标 bean 定义 x 个重复代理，
 * 可以在 bean 工厂中注册单个这样的后处理器来达到相同效果。
 *
 * <p>子类可以应用任意策略决定是否代理某个 bean，例如按类型、
 * 按名称、按定义细节等。它们还可以返回只应应用于特定 bean 实例的
 * 附加拦截器。一个简单的具体实现是 {@link BeanNameAutoProxyCreator}，
 * 通过给定名称识别要被代理的 bean。
 *
 * <p>可以使用任意数量的 {@link TargetSourceCreator} 实现来创建
 * 自定义目标源：例如，用于池化 prototype 对象。只要 TargetSourceCreator
 * 指定了自定义 {@link org.springframework.aop.TargetSource}，即使没有 advice
 * 也会发生自动代理。如果未设置 TargetSourceCreator，或没有任何匹配项，
 * 默认将使用 {@link org.springframework.aop.target.SingletonTargetSource}
 * 包装目标 bean 实例。
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @author Rob Harrop
 * @author Sam Brannen
 * @since 13.10.2003
 * @see #setInterceptorNames
 * @see #getAdvicesAndAdvisorsForBean
 * @see BeanNameAutoProxyCreator
 * @see DefaultAdvisorAutoProxyCreator
 */
@SuppressWarnings("serial")
public abstract class AbstractAutoProxyCreator extends ProxyProcessorSupport
		implements SmartInstantiationAwareBeanPostProcessor, BeanFactoryAware {

	/**
	 * 子类使用的便捷常量：表示“不代理”的返回值。
	 * @see #getAdvicesAndAdvisorsForBean
	 */
	@Nullable
	protected static final Object[] DO_NOT_PROXY = null;

	/**
	 * 子类使用的便捷常量：表示
	 * “不使用附加拦截器进行代理，只使用通用拦截器”的返回值。
	 * @see #getAdvicesAndAdvisorsForBean
	 */
	protected static final Object[] PROXY_WITHOUT_ADDITIONAL_INTERCEPTORS = new Object[0];


	/** 子类可用的 Logger。 */
	protected final Log logger = LogFactory.getLog(getClass());

	/** 默认值为全局 AdvisorAdapterRegistry。 */
	private AdvisorAdapterRegistry advisorAdapterRegistry = GlobalAdvisorAdapterRegistry.getInstance();

	/**
	 * 指示代理是否应被冻结。从父类重写，
	 * 以防止配置过早被冻结。
	 */
	private boolean freezeProxy = false;

	/** 默认没有通用拦截器。 */
	private String[] interceptorNames = new String[0];

	private boolean applyCommonInterceptorsFirst = true;

	@Nullable
	private TargetSourceCreator[] customTargetSourceCreators;

	@Nullable
	private BeanFactory beanFactory;

	private final Set<String> targetSourcedBeans = Collections.newSetFromMap(new ConcurrentHashMap<>(16));

	private final Map<Object, Object> earlyProxyReferences = new ConcurrentHashMap<>(16);

	private final Map<Object, Class<?>> proxyTypes = new ConcurrentHashMap<>(16);

	private final Map<Object, Boolean> advisedBeans = new ConcurrentHashMap<>(256);


	/**
	 * 设置代理是否应被冻结，以防止 advice
	 * 在代理创建后被添加到其中。
	 * <p>从父类重写，以防止代理配置
	 * 在代理创建前被冻结。
	 */
	@Override
	public void setFrozen(boolean frozen) {
		this.freezeProxy = frozen;
	}

	@Override
	public boolean isFrozen() {
		return this.freezeProxy;
	}

	/**
	 * 指定要使用的 {@link AdvisorAdapterRegistry}。
	 * <p>默认值为全局 {@link AdvisorAdapterRegistry}。
	 * @see org.springframework.aop.framework.adapter.GlobalAdvisorAdapterRegistry
	 */
	public void setAdvisorAdapterRegistry(AdvisorAdapterRegistry advisorAdapterRegistry) {
		this.advisorAdapterRegistry = advisorAdapterRegistry;
	}

	/**
	 * 设置按此顺序应用的自定义 {@code TargetSourceCreators}。
	 * 如果列表为空，或它们全部返回 null，则会为每个 bean
	 * 创建 {@link SingletonTargetSource}。
	 * <p>注意，即使没有找到 advice 或 advisor，TargetSourceCreators
	 * 也会对目标 bean 生效。如果 {@code TargetSourceCreator}
	 * 为特定 bean 返回 {@link TargetSource}，则该 bean 无论如何都会被代理。
	 * <p>只有当此后处理器用于 {@link BeanFactory} 且其
	 * {@link BeanFactoryAware} 回调被触发时，才能调用 {@code TargetSourceCreators}。
	 * @param targetSourceCreators {@code TargetSourceCreators} 列表。
	 * 顺序很重要：将使用第一个匹配的 {@code TargetSourceCreator}
	 * （即第一个返回非 null 的创建器）返回的 {@code TargetSource}。
	 */
	public void setCustomTargetSourceCreators(TargetSourceCreator... targetSourceCreators) {
		this.customTargetSourceCreators = targetSourceCreators;
	}

	/**
	 * 设置通用拦截器。它们必须是当前工厂中的 bean 名称。
	 * 它们可以是 Spring 支持的任何 advice 或 advisor 类型。
	 * <p>如果未设置此属性，将有零个通用拦截器。
	 * 如果只需要诸如匹配 Advisor 这样的“特定”拦截器，这是完全有效的。
	 */
	public void setInterceptorNames(String... interceptorNames) {
		this.interceptorNames = interceptorNames;
	}

	/**
	 * 设置通用拦截器是否应应用在 bean 特定拦截器之前。
	 * 默认值为 "true"；否则，将先应用 bean 特定拦截器。
	 */
	public void setApplyCommonInterceptorsFirst(boolean applyCommonInterceptorsFirst) {
		this.applyCommonInterceptorsFirst = applyCommonInterceptorsFirst;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	/**
	 * 返回所属的 {@link BeanFactory}。
	 * 可能为 {@code null}，因为此后处理器不需要属于某个 bean 工厂。
	 */
	@Nullable
	protected BeanFactory getBeanFactory() {
		return this.beanFactory;
	}


	@Override
	@Nullable
	public Class<?> predictBeanType(Class<?> beanClass, String beanName) {
		if (this.proxyTypes.isEmpty()) {
			return null;
		}
		Object cacheKey = getCacheKey(beanClass, beanName);
		return this.proxyTypes.get(cacheKey);
	}

	@Override
	@Nullable
	public Constructor<?>[] determineCandidateConstructors(Class<?> beanClass, String beanName) {
		return null;
	}

	@Override
	public Object getEarlyBeanReference(Object bean, String beanName) {
		Object cacheKey = getCacheKey(bean.getClass(), beanName);
		this.earlyProxyReferences.put(cacheKey, bean);
		return wrapIfNecessary(bean, beanName, cacheKey);
	}

	@Override
	public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) {
		// 生成缓存 key（通常基于 bean类 + bean名称）
		Object cacheKey = getCacheKey(beanClass, beanName);

		// 如果 bean名称 为空 或 当前 bean 不是自定义 目标源 的 bean
		if (!StringUtils.hasLength(beanName) || !this.targetSourcedBeans.contains(beanName)) {
			// 如果该 bean 已经在 advisedBeans 缓存中
			if (this.advisedBeans.containsKey(cacheKey)) {
				// 直接返回 null，表示不进行代理创建
				return null;
			}
			// 如果是基础设施类（如 Advice、Advisor 等）或者需要跳过
			if (isInfrastructureClass(beanClass) || shouldSkip(beanClass, beanName)) {
				// 标记该 bean 不需要增强（false）
				this.advisedBeans.put(cacheKey, Boolean.FALSE);
				// 不做代理
				return null;
			}
		}

		// ===================== 核心逻辑 =====================
		// 如果有自定义 TargetSource，则在这里创建代理。
		// 抑制目标 bean 不必要的默认实例化：
		// 目标源 将以自定义方式处理目标实例。
		TargetSource targetSource = getCustomTargetSource(beanClass, beanName);
		// 如果存在自定义 目标源，说明需要特殊代理处理
		if (targetSource != null) {
			// 如果 bean名称 有值，则记录到 targetSourcedBeans 集合中
			if (StringUtils.hasLength(beanName)) {
				this.targetSourcedBeans.add(beanName);
			}
			// 获取该 bean 对应的拦截器和增强器（AOP 核心：Advice/Advisor）
			Object[] specificInterceptors = getAdvicesAndAdvisorsForBean(beanClass, beanName, targetSource);
			// 创建代理对象（JDK 动态代理 或 CGLIB）
			Object proxy = createProxy(beanClass, beanName, specificInterceptors, targetSource);
			// 将代理类型缓存起来
			this.proxyTypes.put(cacheKey, proxy.getClass());
			// 返回代理对象
			return proxy;
		}
		// 如果没有自定义 目标源，则返回 null，继续正常实例化流程
		return null;
	}

	@Override
	public PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName) {
		return pvs;  // 跳过 postProcessPropertyValues
	}

	/**
	 * 如果 bean 被子类识别为需要代理，则使用配置的拦截器创建代理。
	 * @see #getAdvicesAndAdvisorsForBean
	 */
	@Override
	public Object postProcessAfterInitialization(@Nullable Object bean, String beanName) {
		if (bean != null) {
			Object cacheKey = getCacheKey(bean.getClass(), beanName);
			if (this.earlyProxyReferences.remove(cacheKey) != bean) {
				return wrapIfNecessary(bean, beanName, cacheKey);
			}
		}
		return bean;
	}


	/**
	 * 为给定 bean 类和 bean 名称构建缓存键。
	 * <p>注意：自 4.2.3 起，此实现不再返回拼接后的 class/name String，
	 * 而是返回尽可能高效的缓存键：普通 bean 名称；
	 * 如果是 {@code FactoryBean}，则前置 {@link BeanFactory#FACTORY_BEAN_PREFIX}；
	 * 或者如果未指定 bean 名称，则原样使用给定 bean {@code Class}。
	 * @param beanClass bean 类
	 * @param beanName bean 名称
	 * @return 给定类和名称的缓存键
	 */
	protected Object getCacheKey(Class<?> beanClass, @Nullable String beanName) {
		if (StringUtils.hasLength(beanName)) {
			return (FactoryBean.class.isAssignableFrom(beanClass) ?
					BeanFactory.FACTORY_BEAN_PREFIX + beanName : beanName);
		}
		else {
			return beanClass;
		}
	}

	/**
	 * 如有必要，包装给定 bean，即如果它符合被代理的条件。
	 * @param bean 原始 bean 实例
	 * @param beanName bean 的名称
	 * @param cacheKey 元数据访问的缓存键
	 * @return 包装该 bean 的代理，或原样返回原始 bean 实例
	 */
	protected Object wrapIfNecessary(Object bean, String beanName, Object cacheKey) {
		// 如果 bean名称 不为空，并且该 bean 已经是“自定义 TargetSource 处理过的”
		if (StringUtils.hasLength(beanName) && this.targetSourcedBeans.contains(beanName)) {
			return bean;
		}
		// 如果缓存中标记该 bean 不需要增强（
		if (Boolean.FALSE.equals(this.advisedBeans.get(cacheKey))) {
			// 直接返回原对象
			return bean;
		}
		// 如果是基础设施类（Advice / Advisor 等） 或 需要跳过
		if (isInfrastructureClass(bean.getClass()) || shouldSkip(bean.getClass(), beanName)) {
			// 标记为“不需要代理”，避免后续重复判断
			this.advisedBeans.put(cacheKey, Boolean.FALSE);
			// 返回原始 bean
			return bean;
		}

		// ===================== 核心逻辑：判断是否需要创建代理 =====================
		// 获取该 bean 对应的增强器（Advisor / Advice）
		// 如果有 advice，则创建代理。
		Object[] specificInterceptors = getAdvicesAndAdvisorsForBean(bean.getClass(), beanName, null);
		// 如果返回结果不是 DO_NOT_PROXY（说明需要代理）
		if (specificInterceptors != DO_NOT_PROXY) {
			// 标记该 bean 需要被代理
			this.advisedBeans.put(cacheKey, Boolean.TRUE);
			// 创建代理对象
			Object proxy = createProxy(
					bean.getClass(), beanName, specificInterceptors, new SingletonTargetSource(bean));
			// 缓存代理类型（避免重复创建）
			this.proxyTypes.put(cacheKey, proxy.getClass());
			// 返回代理对象
			return proxy;
		}
		// 如果不需要代理，缓存标记为 FALSE
		this.advisedBeans.put(cacheKey, Boolean.FALSE);
		// 返回原始 bean
		return bean;
	}

	/**
	 * 返回给定 bean 类是否表示基础设施类，
	 * 这种类绝不应被代理。
	 * <p>默认实现将 Advices、Advisors 和
	 * AopInfrastructureBeans 视为基础设施类。
	 * @param beanClass bean 的类
	 * @return bean 是否表示基础设施类
	 * @see org.aopalliance.aop.Advice
	 * @see org.springframework.aop.Advisor
	 * @see org.springframework.aop.framework.AopInfrastructureBean
	 * @see #shouldSkip
	 */
	protected boolean isInfrastructureClass(Class<?> beanClass) {
		boolean retVal = Advice.class.isAssignableFrom(beanClass) ||
				Pointcut.class.isAssignableFrom(beanClass) ||
				Advisor.class.isAssignableFrom(beanClass) ||
				AopInfrastructureBean.class.isAssignableFrom(beanClass);
		if (retVal && logger.isTraceEnabled()) {
			logger.trace("Did not attempt to auto-proxy infrastructure class [" + beanClass.getName() + "]");
		}
		return retVal;
	}

	/**
	 * 如果给定 bean 不应被此后处理器考虑用于自动代理，
	 * 子类应重写此方法以返回 {@code true}。
	 * <p>有时需要能够避免这种情况发生，例如它会导致循环引用，
	 * 或者需要保留现有目标实例。此实现返回 {@code false}，除非 bean 名称
	 * 根据 {@code AutowireCapableBeanFactory} 约定表示“原始实例”。
	 * @param beanClass bean 的类
	 * @param beanName bean 的名称
	 * @return 是否跳过给定 bean
	 * @see org.springframework.beans.factory.config.AutowireCapableBeanFactory#ORIGINAL_INSTANCE_SUFFIX
	 */
	protected boolean shouldSkip(Class<?> beanClass, String beanName) {
		return AutoProxyUtils.isOriginalInstance(beanName, beanClass);
	}

	/**
	 * 为 bean 实例创建目标源。如果设置了 TargetSourceCreators，则使用它们。
	 * 如果不应使用自定义 TargetSource，则返回 {@code null}。
	 * <p>此实现使用 "customTargetSourceCreators" 属性。
	 * 子类可以重写此方法以使用不同机制。
	 * @param beanClass 要为其创建 TargetSource 的 bean 类
	 * @param beanName bean 的名称
	 * @return 此 bean 的 TargetSource
	 * @see #setCustomTargetSourceCreators
	 */
	@Nullable
	protected TargetSource getCustomTargetSource(Class<?> beanClass, String beanName) {
		// 不能为直接注册的 singleton 创建复杂目标源。
		if (this.customTargetSourceCreators != null &&
				this.beanFactory != null && this.beanFactory.containsBean(beanName)) {
			for (TargetSourceCreator tsc : this.customTargetSourceCreators) {
				TargetSource ts = tsc.getTargetSource(beanClass, beanName);
				if (ts != null) {
					// 找到了匹配的 TargetSource。
					if (logger.isTraceEnabled()) {
						logger.trace("TargetSourceCreator [" + tsc +
								"] found custom TargetSource for bean with name '" + beanName + "'");
					}
					return ts;
				}
			}
		}

		// 未找到自定义 TargetSource。
		return null;
	}

	/**
	 * 为给定 bean 创建 AOP 代理。
	 * @param beanClass bean 的类
	 * @param beanName bean 的名称
	 * @param specificInterceptors 特定于此 bean 的拦截器集合
	 * （可以为空，但不能为 null）
	 * @param targetSource 代理的 TargetSource，
	 * 已预先配置为访问该 bean
	 * @return bean 的 AOP 代理
	 * @see #buildAdvisors
	 */
	protected Object createProxy(Class<?> beanClass, @Nullable String beanName,
			@Nullable Object[] specificInterceptors, TargetSource targetSource) {
		// ===================== 暴露目标类（供后续使用） =====================
		// 如果 BeanFactory 支持配置（一般都是）
		if (this.beanFactory instanceof ConfigurableListableBeanFactory) {
			// 记录目标类（用于后续判断是否使用 CGLIB）
			AutoProxyUtils.exposeTargetClass((ConfigurableListableBeanFactory) this.beanFactory, beanName, beanClass);
		}
		// ===================== 创建 ProxyFactory =====================
		// ProxyFactory 是 Spring AOP 创建代理的核心类
		ProxyFactory proxyFactory = new ProxyFactory();
		// 复制当前 AutoProxyCreator 的配置（如是否使用 CGLIB、拦截器等）
		proxyFactory.copyFrom(this);
		// ===================== 决定代理方式 =====================
		// 如果强制使用 CGLIB（proxyTargetClass = true）
		if (proxyFactory.isProxyTargetClass()) {
			// 显式处理 JDK 代理目标和 lambda（用于引介 advice 场景）
			if (Proxy.isProxyClass(beanClass) || ClassUtils.isLambdaClass(beanClass)) {
				// 必须允许引介；不能只将接口设置为代理自身的接口。
				// 必须手动添加接口（否则无法正确代理）
				for (Class<?> ifc : beanClass.getInterfaces()) {
					// 把接口加入代理工厂
					proxyFactory.addInterface(ifc);
				}
			}
		}
		else {
			// 未强制 proxyTargetClass 标志，应用默认检查...
			// 如果没有强制指定代理方式（默认逻辑）
			// 判断是否应该使用 CGLIB（比如没有接口）
			if (shouldProxyTargetClass(beanClass, beanName)) {
				// 强制使用 CGLIB
				proxyFactory.setProxyTargetClass(true);
			}
			else {
				// 使用 JDK 动态代理（基于接口）
				evaluateProxyInterfaces(beanClass, proxyFactory);
			}
		}
		// ===================== 构建 Advisor（增强链） =====================
		// 根据 bean名称 和拦截器生成 Advisor 数组
		Advisor[] advisors = buildAdvisors(beanName, specificInterceptors);
		// 将 Advisor 加入代理工厂
		proxyFactory.addAdvisors(advisors);
		// ===================== 设置目标对象 =====================
		// 设置 TargetSource（包装原始 bean）
		proxyFactory.setTargetSource(targetSource);
		// ===================== 自定义扩展 =====================
		// 子类可以对 ProxyFactory 做进一步定制
		customizeProxyFactory(proxyFactory);
		// ===================== 冻结代理配置 =====================
		// 是否冻结代理（禁止后续修改）
		proxyFactory.setFrozen(this.freezeProxy);
		// 如果 Advisor 已经预过滤（无需再次匹配）
		if (advisorsPreFiltered()) {
			proxyFactory.setPreFiltered(true);
		}
		// ===================== 选择类加载器 =====================
		// 获取用于生成代理类的 ClassLoader
		// 如果 bean 类不是在覆盖类加载器中本地加载的，则使用原始 ClassLoader
		ClassLoader classLoader = getProxyClassLoader();
		// 如果是 SmartClassLoader 且不是原始类加载器
		if (classLoader instanceof SmartClassLoader && classLoader != beanClass.getClassLoader()) {
			// 使用原始 ClassLoader（避免类加载问题）
			classLoader = ((SmartClassLoader) classLoader).getOriginalClassLoader();
		}
		// ===================== 创建代理对象（最终步骤） =====================
		// 生成代理对象（JDK 或 CGLIB）
		return proxyFactory.getProxy(classLoader);
	}

	/**
	 * 确定给定 bean 是否应使用其目标类而不是接口进行代理。
	 * <p>检查对应 bean 定义的
	 * {@link AutoProxyUtils#PRESERVE_TARGET_CLASS_ATTRIBUTE "preserveTargetClass" 属性}。
	 * @param beanClass bean 的类
	 * @param beanName bean 的名称
	 * @return 给定 bean 是否应使用其目标类进行代理
	 * @see AutoProxyUtils#shouldProxyTargetClass
	 */
	protected boolean shouldProxyTargetClass(Class<?> beanClass, @Nullable String beanName) {
		return (this.beanFactory instanceof ConfigurableListableBeanFactory &&
				AutoProxyUtils.shouldProxyTargetClass((ConfigurableListableBeanFactory) this.beanFactory, beanName));
	}

	/**
	 * 返回子类返回的 Advisor 是否已经预过滤，
	 * 已匹配 bean 的目标类，从而在为 AOP 调用构建 advisor 链时
	 * 可以跳过 ClassFilter 检查。
	 * <p>默认值为 {@code false}。如果子类总是返回预过滤的 Advisor，
	 * 可以重写此方法。
	 * @return Advisor 是否已预过滤
	 * @see #getAdvicesAndAdvisorsForBean
	 * @see org.springframework.aop.framework.Advised#setPreFiltered
	 */
	protected boolean advisorsPreFiltered() {
		return false;
	}

	/**
	 * 确定给定 bean 的 advisor，包括特定拦截器和通用拦截器，
	 * 全部适配为 Advisor 接口。
	 * @param beanName bean 的名称
	 * @param specificInterceptors 特定于此 bean 的拦截器集合
	 * （可以为空，但不能为 null）
	 * @return 给定 bean 的 Advisor 列表
	 */
	protected Advisor[] buildAdvisors(@Nullable String beanName, @Nullable Object[] specificInterceptors) {
		// 正确处理 prototypes...
		Advisor[] commonInterceptors = resolveInterceptorNames();

		List<Object> allInterceptors = new ArrayList<>();
		if (specificInterceptors != null) {
			if (specificInterceptors.length > 0) {
				// specificInterceptors 可能等于 PROXY_WITHOUT_ADDITIONAL_INTERCEPTORS
				allInterceptors.addAll(Arrays.asList(specificInterceptors));
			}
			if (commonInterceptors.length > 0) {
				if (this.applyCommonInterceptorsFirst) {
					allInterceptors.addAll(0, Arrays.asList(commonInterceptors));
				}
				else {
					allInterceptors.addAll(Arrays.asList(commonInterceptors));
				}
			}
		}
		if (logger.isTraceEnabled()) {
			int nrOfCommonInterceptors = commonInterceptors.length;
			int nrOfSpecificInterceptors = (specificInterceptors != null ? specificInterceptors.length : 0);
			logger.trace("Creating implicit proxy for bean '" + beanName + "' with " + nrOfCommonInterceptors +
					" common interceptors and " + nrOfSpecificInterceptors + " specific interceptors");
		}

		Advisor[] advisors = new Advisor[allInterceptors.size()];
		for (int i = 0; i < allInterceptors.size(); i++) {
			advisors[i] = this.advisorAdapterRegistry.wrap(allInterceptors.get(i));
		}
		return advisors;
	}

	/**
	 * 将指定的拦截器名称解析为 Advisor 对象。
	 * @see #setInterceptorNames
	 */
	private Advisor[] resolveInterceptorNames() {
		BeanFactory bf = this.beanFactory;
		ConfigurableBeanFactory cbf = (bf instanceof ConfigurableBeanFactory ? (ConfigurableBeanFactory) bf : null);
		List<Advisor> advisors = new ArrayList<>();
		for (String beanName : this.interceptorNames) {
			if (cbf == null || !cbf.isCurrentlyInCreation(beanName)) {
				Assert.state(bf != null, "BeanFactory required for resolving interceptor names");
				Object next = bf.getBean(beanName);
				advisors.add(this.advisorAdapterRegistry.wrap(next));
			}
		}
		return advisors.toArray(new Advisor[0]);
	}

	/**
	 * 子类可以选择实现此方法：例如，
	 * 更改所暴露的接口。
	 * <p>默认实现为空。
	 * @param proxyFactory 已经配置了 TargetSource 和接口的 ProxyFactory，
	 * 将在此方法返回后立即用于创建代理
	 */
	protected void customizeProxyFactory(ProxyFactory proxyFactory) {
	}


	/**
	 * 返回给定 bean 是否要被代理，以及要应用哪些附加
	 * advice（例如 AOP Alliance 拦截器）和 advisor。
	 * @param beanClass 要进行 advising 的 bean 类
	 * @param beanName bean 的名称
	 * @param customTargetSource {@link #getCustomTargetSource} 方法返回的 TargetSource：
	 * 可能会被忽略。如果未使用自定义目标源，则为 {@code null}。
	 * @return 特定 bean 的附加拦截器数组；
	 * 或空数组，表示没有附加拦截器但有通用拦截器；
	 * 或 {@code null}，表示完全不代理，连通用拦截器也不使用。
	 * 请参阅常量 DO_NOT_PROXY 和 PROXY_WITHOUT_ADDITIONAL_INTERCEPTORS。
	 * @throws BeansException 如果发生错误
	 * @see #DO_NOT_PROXY
	 * @see #PROXY_WITHOUT_ADDITIONAL_INTERCEPTORS
	 */
	@Nullable
	protected abstract Object[] getAdvicesAndAdvisorsForBean(Class<?> beanClass, String beanName,
			@Nullable TargetSource customTargetSource) throws BeansException;

}
