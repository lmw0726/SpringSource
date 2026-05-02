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

package org.springframework.aop.aspectj.annotation;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.aspectj.lang.reflect.PerClauseKind;

import org.springframework.aop.Advisor;
import org.springframework.aop.aspectj.AspectJProxyUtils;
import org.springframework.aop.aspectj.SimpleAspectInstanceFactory;
import org.springframework.aop.framework.ProxyCreatorSupport;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * 基于 AspectJ 的代理工厂，允许以编程方式构建
 * 包含 AspectJ 切面（代码风格以及注解风格）的代理。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Ramnivas Laddad
 * @since 2.0
 * @see #addAspect(Object)
 * @see #addAspect(Class)
 * @see #getProxy()
 * @see #getProxy(ClassLoader)
 * @see org.springframework.aop.framework.ProxyFactory
 */
@SuppressWarnings("serial")
public class AspectJProxyFactory extends ProxyCreatorSupport {

	/** singleton 切面实例的缓存。 */
	private static final Map<Class<?>, Object> aspectCache = new ConcurrentHashMap<>();

	private final AspectJAdvisorFactory aspectFactory = new ReflectiveAspectJAdvisorFactory();


	/**
	 * 创建新的 AspectJProxyFactory。
	 */
	public AspectJProxyFactory() {
	}

	/**
	 * 创建新的 AspectJProxyFactory。
	 * <p>将代理给定目标实现的所有接口。
	 * @param target 要被代理的目标对象
	 */
	public AspectJProxyFactory(Object target) {
		Assert.notNull(target, "Target object must not be null");
		setInterfaces(ClassUtils.getAllInterfaces(target));
		setTarget(target);
	}

	/**
	 * 创建新的 {@code AspectJProxyFactory}。
	 * 没有目标，只有接口。必须添加拦截器。
	 */
	public AspectJProxyFactory(Class<?>... interfaces) {
		setInterfaces(interfaces);
	}


	/**
	 * 将提供的切面实例添加到链中。提供的切面实例类型
	 * 必须是 singleton 切面。使用此方法时不会遵循真正的 singleton 生命周期，
	 * 调用者负责管理以这种方式添加的任何切面的生命周期。
	 * @param aspectInstance AspectJ 切面实例
	 */
	public void addAspect(Object aspectInstance) {
		Class<?> aspectClass = aspectInstance.getClass();
		String aspectName = aspectClass.getName();
		AspectMetadata am = createAspectMetadata(aspectClass, aspectName);
		if (am.getAjType().getPerClause().getKind() != PerClauseKind.SINGLETON) {
			throw new IllegalArgumentException(
					"Aspect class [" + aspectClass.getName() + "] does not define a singleton aspect");
		}
		addAdvisorsFromAspectInstanceFactory(
				new SingletonMetadataAwareAspectInstanceFactory(aspectInstance, aspectName));
	}

	/**
	 * 将所提供类型的切面添加到 advice 链末尾。
	 * @param aspectClass AspectJ 切面类
	 */
	public void addAspect(Class<?> aspectClass) {
		String aspectName = aspectClass.getName();
		AspectMetadata am = createAspectMetadata(aspectClass, aspectName);
		MetadataAwareAspectInstanceFactory instanceFactory = createAspectInstanceFactory(am, aspectClass, aspectName);
		addAdvisorsFromAspectInstanceFactory(instanceFactory);
	}


	/**
	 * 将所提供 {@link MetadataAwareAspectInstanceFactory} 中的所有
	 * {@link Advisor Advisors} 添加到当前链中。如果需要，暴露任何特殊用途的
	 * {@link Advisor Advisors}。
	 * @see AspectJProxyUtils#makeAdvisorChainAspectJCapableIfNecessary(List)
	 */
	private void addAdvisorsFromAspectInstanceFactory(MetadataAwareAspectInstanceFactory instanceFactory) {
		List<Advisor> advisors = this.aspectFactory.getAdvisors(instanceFactory);
		Class<?> targetClass = getTargetClass();
		Assert.state(targetClass != null, "Unresolvable target class");
		advisors = AopUtils.findAdvisorsThatCanApply(advisors, targetClass);
		AspectJProxyUtils.makeAdvisorChainAspectJCapableIfNecessary(advisors);
		AnnotationAwareOrderComparator.sort(advisors);
		addAdvisors(advisors);
	}

	/**
	 * 为提供的切面类型创建 {@link AspectMetadata} 实例。
	 */
	private AspectMetadata createAspectMetadata(Class<?> aspectClass, String aspectName) {
		AspectMetadata am = new AspectMetadata(aspectClass, aspectName);
		if (!am.getAjType().isAspect()) {
			throw new IllegalArgumentException("Class [" + aspectClass.getName() + "] is not a valid aspect type");
		}
		return am;
	}

	/**
	 * 为提供的切面类型创建 {@link MetadataAwareAspectInstanceFactory}。
	 * 如果切面类型没有 per 子句，则返回 {@link SingletonMetadataAwareAspectInstanceFactory}，
	 * 否则返回 {@link PrototypeAspectInstanceFactory}。
	 */
	private MetadataAwareAspectInstanceFactory createAspectInstanceFactory(
			AspectMetadata am, Class<?> aspectClass, String aspectName) {

		MetadataAwareAspectInstanceFactory instanceFactory;
		if (am.getAjType().getPerClause().getKind() == PerClauseKind.SINGLETON) {
			// 创建共享切面实例。
			Object instance = getSingletonAspectInstance(aspectClass);
			instanceFactory = new SingletonMetadataAwareAspectInstanceFactory(instance, aspectName);
		}
		else {
			// 为独立切面实例创建工厂。
			instanceFactory = new SimpleMetadataAwareAspectInstanceFactory(aspectClass, aspectName);
		}
		return instanceFactory;
	}

	/**
	 * 获取所提供切面类型的 singleton 切面实例。
	 * 如果实例缓存中找不到，则创建一个实例。
	 */
	private Object getSingletonAspectInstance(Class<?> aspectClass) {
		return aspectCache.computeIfAbsent(aspectClass,
				clazz -> new SimpleAspectInstanceFactory(clazz).getAspectInstance());
	}


	/**
	 * 根据此工厂中的设置创建新的代理。
	 * <p>可以重复调用。如果已添加或移除接口，效果会有所不同。
	 * 可以添加和移除拦截器。
	 * <p>使用默认类加载器：通常是线程上下文类加载器
	 * （如果创建代理需要）。
	 * @return 新代理
	 */
	@SuppressWarnings("unchecked")
	public <T> T getProxy() {
		return (T) createAopProxy().getProxy();
	}

	/**
	 * 根据此工厂中的设置创建新的代理。
	 * <p>可以重复调用。如果已添加或移除接口，效果会有所不同。
	 * 可以添加和移除拦截器。
	 * <p>使用给定类加载器（如果创建代理需要）。
	 * @param classLoader 用于创建代理的类加载器
	 * @return 新代理
	 */
	@SuppressWarnings("unchecked")
	public <T> T getProxy(ClassLoader classLoader) {
		return (T) createAopProxy().getProxy(classLoader);
	}

}
