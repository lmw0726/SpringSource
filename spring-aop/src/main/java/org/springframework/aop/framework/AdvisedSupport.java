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

package org.springframework.aop.framework;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.aopalliance.aop.Advice;

import org.springframework.aop.Advisor;
import org.springframework.aop.DynamicIntroductionAdvice;
import org.springframework.aop.IntroductionAdvisor;
import org.springframework.aop.IntroductionInfo;
import org.springframework.aop.TargetSource;
import org.springframework.aop.support.DefaultIntroductionAdvisor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.target.EmptyTargetSource;
import org.springframework.aop.target.SingletonTargetSource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;

/**
 * AOP 代理配置管理器的基类。
 * 它们本身不是 AOP 代理，但此类的子类通常是工厂，
 * 可从中直接获取 AOP 代理实例。
 *
 * <p>此类使子类无需处理 Advices 和 Advisors 的日常管理，
 * 但实际上不实现代理创建方法；这些方法由子类提供。
 *
 * <p>此类可序列化；子类不需要可序列化。
 * 此类用于保存代理的快照。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see org.springframework.aop.framework.AopProxy
 */
public class AdvisedSupport extends ProxyConfig implements Advised {

	/** 使用 Spring 2.0 中的 serialVersionUID 以实现互操作性。 */
	private static final long serialVersionUID = 2651364800145442165L;


	/**
	 * 没有目标且行为由 advisors 提供时的规范 TargetSource。
	 */
	public static final TargetSource EMPTY_TARGET_SOURCE = EmptyTargetSource.INSTANCE;


	/** 包保护以允许为提高效率而直接访问。 */
	TargetSource targetSource = EMPTY_TARGET_SOURCE;

	/** Advisors 是否已经针对特定目标类过滤。 */
	private boolean preFiltered = false;

	/** 要使用的 AdvisorChainFactory。 */
	AdvisorChainFactory advisorChainFactory = new DefaultAdvisorChainFactory();

	/** 以 Method 为键、advisor 链 List 为值的缓存。 */
	private transient Map<MethodCacheKey, List<Object>> methodCache;

	/**
	 * 代理要实现的接口。保存在 List 中以保持注册顺序，
	 * 从而按指定的接口顺序创建 JDK 代理。
	 */
	private List<Class<?>> interfaces = new ArrayList<>();

	/**
	 * Advisors 列表。如果添加的是 Advice，
	 * 它会先包装在 Advisor 中，再添加到此 List。
	 */
	private List<Advisor> advisors = new ArrayList<>();


	/**
	 * 供 JavaBean 使用的无参构造函数。
	 */
	public AdvisedSupport() {
		this.methodCache = new ConcurrentHashMap<>(32);
	}

	/**
	 * 使用给定参数创建 AdvisedSupport 实例。
	 * @param interfaces 被代理的接口
	 */
	public AdvisedSupport(Class<?>... interfaces) {
		this();
		setInterfaces(interfaces);
	}


	/**
	 * 将给定对象设置为目标。
	 * 会为该对象创建 SingletonTargetSource。
	 * @see #setTargetSource
	 * @see org.springframework.aop.target.SingletonTargetSource
	 */
	public void setTarget(Object target) {
		setTargetSource(new SingletonTargetSource(target));
	}

	@Override
	public void setTargetSource(@Nullable TargetSource targetSource) {
		this.targetSource = (targetSource != null ? targetSource : EMPTY_TARGET_SOURCE);
	}

	@Override
	public TargetSource getTargetSource() {
		return this.targetSource;
	}

	/**
	 * 设置要被代理的目标类，表示该代理应可强制转换为给定类。
	 * <p>内部会为给定目标类使用 {@link org.springframework.aop.target.EmptyTargetSource}。
	 * 所需的代理类型将在实际创建代理时确定。
	 * <p>这是设置 "targetSource" 或 "target" 的替代方案，
	 * 用于我们希望基于目标类（可以是接口或具体类）创建代理，
	 * 但没有完全可用的 TargetSource 的情况。
	 * @see #setTargetSource
	 * @see #setTarget
	 */
	public void setTargetClass(@Nullable Class<?> targetClass) {
		this.targetSource = EmptyTargetSource.forClass(targetClass);
	}

	@Override
	@Nullable
	public Class<?> getTargetClass() {
		return this.targetSource.getTargetClass();
	}

	@Override
	public void setPreFiltered(boolean preFiltered) {
		this.preFiltered = preFiltered;
	}

	@Override
	public boolean isPreFiltered() {
		return this.preFiltered;
	}

	/**
	 * 设置要使用的 advisor 链工厂。
	 * <p>默认为 {@link DefaultAdvisorChainFactory}。
	 */
	public void setAdvisorChainFactory(AdvisorChainFactory advisorChainFactory) {
		Assert.notNull(advisorChainFactory, "AdvisorChainFactory must not be null");
		this.advisorChainFactory = advisorChainFactory;
	}

	/**
	 * 返回要使用的 advisor 链工厂（绝不为 {@code null}）。
	 */
	public AdvisorChainFactory getAdvisorChainFactory() {
		return this.advisorChainFactory;
	}


	/**
	 * 设置要代理的接口。
	 */
	public void setInterfaces(Class<?>... interfaces) {
		Assert.notNull(interfaces, "Interfaces must not be null");
		this.interfaces.clear();
		for (Class<?> ifc : interfaces) {
			addInterface(ifc);
		}
	}

	/**
	 * 添加新的被代理接口。
	 * @param intf 要代理的附加接口
	 */
	public void addInterface(Class<?> intf) {
		Assert.notNull(intf, "Interface must not be null");
		if (!intf.isInterface()) {
			throw new IllegalArgumentException("[" + intf.getName() + "] is not an interface");
		}
		if (!this.interfaces.contains(intf)) {
			this.interfaces.add(intf);
			adviceChanged();
		}
	}

	/**
	 * 移除被代理接口。
	 * <p>如果给定接口未被代理，则不执行任何操作。
	 * @param intf 要从代理中移除的接口
	 * @return 如果接口已移除，则为 {@code true}；如果未找到该接口，
	 * 因而无法移除，则为 {@code false}
	 */
	public boolean removeInterface(Class<?> intf) {
		return this.interfaces.remove(intf);
	}

	@Override
	public Class<?>[] getProxiedInterfaces() {
		return ClassUtils.toClassArray(this.interfaces);
	}

	@Override
	public boolean isInterfaceProxied(Class<?> intf) {
		for (Class<?> proxyIntf : this.interfaces) {
			if (intf.isAssignableFrom(proxyIntf)) {
				return true;
			}
		}
		return false;
	}


	@Override
	public final Advisor[] getAdvisors() {
		return this.advisors.toArray(new Advisor[0]);
	}

	@Override
	public int getAdvisorCount() {
		return this.advisors.size();
	}

	@Override
	public void addAdvisor(Advisor advisor) {
		int pos = this.advisors.size();
		addAdvisor(pos, advisor);
	}

	@Override
	public void addAdvisor(int pos, Advisor advisor) throws AopConfigException {
		if (advisor instanceof IntroductionAdvisor) {
			validateIntroductionAdvisor((IntroductionAdvisor) advisor);
		}
		addAdvisorInternal(pos, advisor);
	}

	@Override
	public boolean removeAdvisor(Advisor advisor) {
		int index = indexOf(advisor);
		if (index == -1) {
			return false;
		}
		else {
			removeAdvisor(index);
			return true;
		}
	}

	@Override
	public void removeAdvisor(int index) throws AopConfigException {
		if (isFrozen()) {
			throw new AopConfigException("Cannot remove Advisor: Configuration is frozen.");
		}
		if (index < 0 || index > this.advisors.size() - 1) {
			throw new AopConfigException("Advisor index " + index + " is out of bounds: " +
					"This configuration only has " + this.advisors.size() + " advisors.");
		}

		Advisor advisor = this.advisors.remove(index);
		if (advisor instanceof IntroductionAdvisor) {
			IntroductionAdvisor ia = (IntroductionAdvisor) advisor;
			// 需要移除引介接口。
			for (Class<?> ifc : ia.getInterfaces()) {
				removeInterface(ifc);
			}
		}

		adviceChanged();
	}

	@Override
	public int indexOf(Advisor advisor) {
		Assert.notNull(advisor, "Advisor must not be null");
		return this.advisors.indexOf(advisor);
	}

	@Override
	public boolean replaceAdvisor(Advisor a, Advisor b) throws AopConfigException {
		Assert.notNull(a, "Advisor a must not be null");
		Assert.notNull(b, "Advisor b must not be null");
		int index = indexOf(a);
		if (index == -1) {
			return false;
		}
		removeAdvisor(index);
		addAdvisor(index, b);
		return true;
	}

	/**
	 * 将所有给定 advisor 添加到此代理配置中。
	 * @param advisors 要注册的 advisors
	 */
	public void addAdvisors(Advisor... advisors) {
		addAdvisors(Arrays.asList(advisors));
	}

	/**
	 * 将所有给定 advisor 添加到此代理配置中。
	 * @param advisors 要注册的 advisors
	 */
	public void addAdvisors(Collection<Advisor> advisors) {
		if (isFrozen()) {
			throw new AopConfigException("Cannot add advisor: Configuration is frozen.");
		}
		if (!CollectionUtils.isEmpty(advisors)) {
			for (Advisor advisor : advisors) {
				if (advisor instanceof IntroductionAdvisor) {
					validateIntroductionAdvisor((IntroductionAdvisor) advisor);
				}
				Assert.notNull(advisor, "Advisor must not be null");
				this.advisors.add(advisor);
			}
			adviceChanged();
		}
	}

	private void validateIntroductionAdvisor(IntroductionAdvisor advisor) {
		advisor.validateInterfaces();
		// 如果 advisor 通过了验证，则可以进行更改。
		Class<?>[] ifcs = advisor.getInterfaces();
		for (Class<?> ifc : ifcs) {
			addInterface(ifc);
		}
	}

	private void addAdvisorInternal(int pos, Advisor advisor) throws AopConfigException {
		Assert.notNull(advisor, "Advisor must not be null");
		if (isFrozen()) {
			throw new AopConfigException("Cannot add advisor: Configuration is frozen.");
		}
		if (pos > this.advisors.size()) {
			throw new IllegalArgumentException(
					"Illegal position " + pos + " in advisor list with size " + this.advisors.size());
		}
		this.advisors.add(pos, advisor);
		adviceChanged();
	}

	/**
	 * 允许不受控制地访问 {@link Advisor Advisors} 的 {@link List}。
	 * <p>请谨慎使用，并记得在进行任何修改时
	 * {@link #adviceChanged() 触发通知变更事件}。
	 */
	protected final List<Advisor> getAdvisorsInternal() {
		return this.advisors;
	}

	@Override
	public void addAdvice(Advice advice) throws AopConfigException {
		int pos = this.advisors.size();
		addAdvice(pos, advice);
	}

	/**
	 * 除非该 advice 实现 IntroductionInfo，否则不能以这种方式添加引介。
	 */
	@Override
	public void addAdvice(int pos, Advice advice) throws AopConfigException {
		Assert.notNull(advice, "Advice must not be null");
		if (advice instanceof IntroductionInfo) {
			// 对于这种引介，不需要 IntroductionAdvisor：
			// 它是完全自描述的。
			addAdvisor(pos, new DefaultIntroductionAdvisor(advice, (IntroductionInfo) advice));
		}
		else if (advice instanceof DynamicIntroductionAdvice) {
			// 对于这种引介，需要 IntroductionAdvisor。
			throw new AopConfigException("DynamicIntroductionAdvice may only be added as part of IntroductionAdvisor");
		}
		else {
			addAdvisor(pos, new DefaultPointcutAdvisor(advice));
		}
	}

	@Override
	public boolean removeAdvice(Advice advice) throws AopConfigException {
		int index = indexOf(advice);
		if (index == -1) {
			return false;
		}
		else {
			removeAdvisor(index);
			return true;
		}
	}

	@Override
	public int indexOf(Advice advice) {
		Assert.notNull(advice, "Advice must not be null");
		for (int i = 0; i < this.advisors.size(); i++) {
			Advisor advisor = this.advisors.get(i);
			if (advisor.getAdvice() == advice) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * 给定 advice 是否包含在此代理配置内的任何 advisor 中？
	 * @param advice 要检查是否包含的 advice
	 * @return 是否包含此 advice 实例
	 */
	public boolean adviceIncluded(@Nullable Advice advice) {
		if (advice != null) {
			for (Advisor advisor : this.advisors) {
				if (advisor.getAdvice() == advice) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * 统计给定类的 advices。
	 * @param adviceClass 要检查的 advice 类
	 * @return 此类或其子类的拦截器数量
	 */
	public int countAdvicesOfType(@Nullable Class<?> adviceClass) {
		int count = 0;
		if (adviceClass != null) {
			for (Advisor advisor : this.advisors) {
				if (adviceClass.isInstance(advisor.getAdvice())) {
					count++;
				}
			}
		}
		return count;
	}


	/**
	 * 基于此配置，为给定方法确定 {@link org.aopalliance.intercept.MethodInterceptor}
	 * 对象列表。
	 * @param method 被代理的方法
	 * @param targetClass 目标类
	 * @return MethodInterceptors 的 List（也可能包含 InterceptorAndDynamicMethodMatchers）
	 */
	public List<Object> getInterceptorsAndDynamicInterceptionAdvice(Method method, @Nullable Class<?> targetClass) {
		MethodCacheKey cacheKey = new MethodCacheKey(method);
		List<Object> cached = this.methodCache.get(cacheKey);
		if (cached == null) {
			cached = this.advisorChainFactory.getInterceptorsAndDynamicInterceptionAdvice(
					this, method, targetClass);
			this.methodCache.put(cacheKey, cached);
		}
		return cached;
	}

	/**
	 * 当 advice 已更改时调用。
	 */
	protected void adviceChanged() {
		this.methodCache.clear();
	}

	/**
	 * 在由无参构造函数创建的新实例上调用此方法，
	 * 以从给定对象创建配置的独立副本。
	 * @param other 要从中复制配置的 AdvisedSupport 对象
	 */
	protected void copyConfigurationFrom(AdvisedSupport other) {
		copyConfigurationFrom(other, other.targetSource, new ArrayList<>(other.advisors));
	}

	/**
	 * 从给定 AdvisedSupport 对象复制 AOP 配置，
	 * 但允许替换为新的 TargetSource 和给定的拦截器链。
	 * @param other 要从中获取代理配置的 AdvisedSupport 对象
	 * @param targetSource 新的 TargetSource
	 * @param advisors 链中的 Advisors
	 */
	protected void copyConfigurationFrom(AdvisedSupport other, TargetSource targetSource, List<Advisor> advisors) {
		copyFrom(other);
		this.targetSource = targetSource;
		this.advisorChainFactory = other.advisorChainFactory;
		this.interfaces = new ArrayList<>(other.interfaces);
		for (Advisor advisor : advisors) {
			if (advisor instanceof IntroductionAdvisor) {
				validateIntroductionAdvisor((IntroductionAdvisor) advisor);
			}
			Assert.notNull(advisor, "Advisor must not be null");
			this.advisors.add(advisor);
		}
		adviceChanged();
	}

	/**
	 * 构建此 AdvisedSupport 的仅配置副本，
	 * 并替换 TargetSource。
	 */
	AdvisedSupport getConfigurationOnlyCopy() {
		AdvisedSupport copy = new AdvisedSupport();
		copy.copyFrom(this);
		copy.targetSource = EmptyTargetSource.forClass(getTargetClass(), getTargetSource().isStatic());
		copy.advisorChainFactory = this.advisorChainFactory;
		copy.interfaces = new ArrayList<>(this.interfaces);
		copy.advisors = new ArrayList<>(this.advisors);
		return copy;
	}


	//---------------------------------------------------------------------
	// 序列化支持
	//---------------------------------------------------------------------

	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		// 依赖默认序列化；仅在反序列化后初始化状态。
		ois.defaultReadObject();

		// 初始化 transient 字段。
		this.methodCache = new ConcurrentHashMap<>(32);
	}

	@Override
	public String toProxyConfigString() {
		return toString();
	}

	/**
	 * 用于调试/诊断。
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(getClass().getName());
		sb.append(": ").append(this.interfaces.size()).append(" interfaces ");
		sb.append(ClassUtils.classNamesToString(this.interfaces)).append("; ");
		sb.append(this.advisors.size()).append(" advisors ");
		sb.append(this.advisors).append("; ");
		sb.append("targetSource [").append(this.targetSource).append("]; ");
		sb.append(super.toString());
		return sb.toString();
	}


	/**
	 * Method 周围的简单包装类。用作缓存方法时的键，
	 * 以便高效进行 equals 和 hashCode 比较。
	 */
	private static final class MethodCacheKey implements Comparable<MethodCacheKey> {

		private final Method method;

		private final int hashCode;

		public MethodCacheKey(Method method) {
			this.method = method;
			this.hashCode = method.hashCode();
		}

		@Override
		public boolean equals(@Nullable Object other) {
			return (this == other || (other instanceof MethodCacheKey &&
					this.method == ((MethodCacheKey) other).method));
		}

		@Override
		public int hashCode() {
			return this.hashCode;
		}

		@Override
		public String toString() {
			return this.method.toString();
		}

		@Override
		public int compareTo(MethodCacheKey other) {
			int result = this.method.getName().compareTo(other.method.getName());
			if (result == 0) {
				result = this.method.toString().compareTo(other.method.toString());
			}
			return result;
		}
	}

}
