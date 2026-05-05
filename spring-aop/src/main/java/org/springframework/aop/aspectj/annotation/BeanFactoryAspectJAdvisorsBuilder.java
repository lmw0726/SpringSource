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

package org.springframework.aop.aspectj.annotation;

import org.aspectj.lang.reflect.PerClauseKind;
import org.springframework.aop.Advisor;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用于从 BeanFactory 中检索 @AspectJ bean 并基于它们构建 Spring Advisor 的辅助类，
 * 供自动代理使用。
 *
 * @author Juergen Hoeller
 * @since 2.0.2
 * @see AnnotationAwareAspectJAutoProxyCreator
 */
public class BeanFactoryAspectJAdvisorsBuilder {

	private final ListableBeanFactory beanFactory;

	private final AspectJAdvisorFactory advisorFactory;

	@Nullable
	private volatile List<String> aspectBeanNames;

	private final Map<String, List<Advisor>> advisorsCache = new ConcurrentHashMap<>();

	private final Map<String, MetadataAwareAspectInstanceFactory> aspectFactoryCache = new ConcurrentHashMap<>();


	/**
	 * 为给定的 BeanFactory 创建一个新的 BeanFactoryAspectJAdvisorsBuilder。
	 * @param beanFactory 要扫描的 ListableBeanFactory
	 */
	public BeanFactoryAspectJAdvisorsBuilder(ListableBeanFactory beanFactory) {
		this(beanFactory, new ReflectiveAspectJAdvisorFactory(beanFactory));
	}

	/**
	 * 为给定的 BeanFactory 创建一个新的 BeanFactoryAspectJAdvisorsBuilder。
	 * @param beanFactory 要扫描的 ListableBeanFactory
	 * @param advisorFactory 用于构建每个 Advisor 的 AspectJAdvisorFactory
	 */
	public BeanFactoryAspectJAdvisorsBuilder(ListableBeanFactory beanFactory, AspectJAdvisorFactory advisorFactory) {
		Assert.notNull(beanFactory, "ListableBeanFactory must not be null");
		Assert.notNull(advisorFactory, "AspectJAdvisorFactory must not be null");
		this.beanFactory = beanFactory;
		this.advisorFactory = advisorFactory;
	}


	/**
	 * 在当前 bean factory 中查找带有 AspectJ 注解的切面 bean，
	 * 并返回表示它们的 Spring AOP Advisor 列表。
	 * <p>为每个 AspectJ 通知方法创建一个 Spring Advisor。
	 * @return {@link org.springframework.aop.Advisor} bean 列表
	 * @see #isEligibleBean
	 */
	public List<Advisor> buildAspectJAdvisors() {
		// 获取已缓存的切面 bean 名称列表
		List<String> aspectNames = this.aspectBeanNames;
		// 如果还没有缓存
		if (aspectNames == null) {
			synchronized (this) {
				aspectNames = this.aspectBeanNames;
				if (aspectNames == null) {
					// 用于存放最终生成的 Advisor 列表
					List<Advisor> advisors = new ArrayList<>();
					// 用于存放所有切面 bean 名称
					aspectNames = new ArrayList<>();
					// 获取容器中所有 bean 名称
					String[] beanNames = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
							this.beanFactory, Object.class, true, false);
					// 遍历所有 bean
					for (String beanName : beanNames) {
						// 如果不是合法bean，跳过
						if (!isEligibleBean(beanName)) {
							continue;
						}
						// ⚠️ 注意：
						// 这里只获取类型，不实例化 bean（避免提前创建，导致无法织入 AOP）
						// 我们必须小心不要急切实例化 bean，因为在这种情况下它们
						// 会被 Spring 容器缓存，但不会被织入。
						// 获取 bean 类型（
						Class<?> beanType = this.beanFactory.getType(beanName, false);
						// 如果类型为空，跳过
						if (beanType == null) {
							continue;
						}
						// 判断该 bean 是否是一个 Aspect（@Aspect 标注的切面类）
						if (this.advisorFactory.isAspect(beanType)) {
							// 记录切面 bean 名称
							aspectNames.add(beanName);
							// 构建切面元数据（包含切面类型、实例化模型等）
							AspectMetadata amd = new AspectMetadata(beanType, beanName);
							// 判断切面实例化模型（如 singleton / perthis / pertarget 等）
							if (amd.getAjType().getPerClause().getKind() == PerClauseKind.SINGLETON) {
								// 单例切面：创建工厂（用于获取切面实例）
								MetadataAwareAspectInstanceFactory factory =
										new BeanFactoryAspectInstanceFactory(this.beanFactory, beanName);
								// 解析该切面，生成 Advisor
								List<Advisor> classAdvisors = this.advisorFactory.getAdvisors(factory);
								// 如果该 bean 是单例
								if (this.beanFactory.isSingleton(beanName)) {
									// 缓存 Advisor（避免重复解析）
									this.advisorsCache.put(beanName, classAdvisors);
								}
								else {
									// 非单例：缓存工厂（每次动态创建切面实例）
									this.aspectFactoryCache.put(beanName, factory);
								}
								// 加入最终结果
								advisors.addAll(classAdvisors);
							}
							else {
								// 每个目标或每个这个。
								if (this.beanFactory.isSingleton(beanName)) {
									throw new IllegalArgumentException("Bean with name '" + beanName +
											"' is a singleton, but aspect instantiation model is not singleton");
								}
								// 创建 Prototype 类型的切面实例工厂
								MetadataAwareAspectInstanceFactory factory =
										new PrototypeAspectInstanceFactory(this.beanFactory, beanName);
								// 缓存工厂
								this.aspectFactoryCache.put(beanName, factory);
								// 解析切面生成 Advisor，并加入结果
								advisors.addAll(this.advisorFactory.getAdvisors(factory));
							}
						}
					}
					// 缓存切面名称列表
					this.aspectBeanNames = aspectNames;
					// 返回解析出来的 Advisor
					return advisors;
				}
			}
		}

		// 如果切面列表为空，直接返回空集合
		if (aspectNames.isEmpty()) {
			return Collections.emptyList();
		}
		// 用于存放最终 Advisor
		List<Advisor> advisors = new ArrayList<>();
		// 遍历所有切面名称
		for (String aspectName : aspectNames) {
			// 从缓存中获取 Advisor
			List<Advisor> cachedAdvisors = this.advisorsCache.get(aspectName);
			if (cachedAdvisors != null) {
				// 如果缓存存在，直接使用
				advisors.addAll(cachedAdvisors);
			}
			else {
				// 否则，从工厂中获取（prototype / perthis 等）
				MetadataAwareAspectInstanceFactory factory = this.aspectFactoryCache.get(aspectName);
				// 动态解析 Advisor
				advisors.addAll(this.advisorFactory.getAdvisors(factory));
			}
		}
		// 返回最终 Advisor 列表
		return advisors;
	}

	/**
	 * 返回具有给定名称的切面 bean 是否符合条件。
	 * @param beanName 切面 bean 的名称
	 * @return 该 bean 是否符合条件
	 */
	protected boolean isEligibleBean(String beanName) {
		return true;
	}

}