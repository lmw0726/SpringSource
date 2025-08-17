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

package org.springframework.beans.factory.support;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Method;
import java.util.Properties;

/**
 * 一个基础的 {@link AutowireCandidateResolver} 实现，当依赖项以泛型类型声明时
 * （例如 Repository<Customer>），会与候选 Bean 的类型进行完整的泛型类型匹配。
 *
 * <p>该类是 {@link org.springframework.beans.factory.annotation.QualifierAnnotationAutowireCandidateResolver}
 * 的基类，将所有非注解相关的解析逻辑封装在这一层，提供通用的泛型感知支持。
 *
 * <p>它通过检查候选 Bean 的实际类型（包括泛型信息）来确保自动装配的精确性，
 * 避免因泛型不匹配而导致错误的 Bean 被注入（如注入 Repository<Order> 到期望 Repository<Customer> 的位置）。
 *
 * @author Juergen Hoeller
 * @since 4.0
 */
public class GenericTypeAwareAutowireCandidateResolver extends SimpleAutowireCandidateResolver
		implements BeanFactoryAware, Cloneable {

	@Nullable
	private BeanFactory beanFactory;


	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	@Nullable
	protected final BeanFactory getBeanFactory() {
		return this.beanFactory;
	}


	@Override
	public boolean isAutowireCandidate(BeanDefinitionHolder bdHolder, DependencyDescriptor descriptor) {
		if (!super.isAutowireCandidate(bdHolder, descriptor)) {
			// 如果显式为false，则不进行任何其他检查...
			return false;
		}
		return checkGenericTypeMatch(bdHolder, descriptor);
	}

	/**
	 * 将给定的依赖类型（包含泛型信息）与候选 Bean 定义进行匹配，判断是否兼容。
	 *
	 * <p>此方法用于处理带有泛型的依赖注入场景（如注入 {@code List<String>} 或 {@code Map<String, MyBean>}），
	 * 检查目标 Bean 的实际类型是否满足依赖所要求的泛型结构。
	 *
	 * <p>匹配过程优先使用 Bean 定义中已解析的类型（如工厂方法返回类型、targetType），若不可用，
	 * 则尝试通过 BeanFactory 获取实际类型或回退到 Bean 类本身。对于无法完全解析泛型的情况，
	 * 支持某些实用的“回退匹配”规则（如 Properties 可匹配任意 Map 类型）。
	 *
	 * @param bdHolder     Bean 定义持有者，包含 Bean 名称和定义信息
	 * @param descriptor   依赖描述符，包含依赖的类型（含泛型）及是否允许回退匹配
	 * @return             如果依赖类型与目标类型兼容（包括泛型匹配），返回 {@code true}；否则返回 {@code false}
	 */
	protected boolean checkGenericTypeMatch(BeanDefinitionHolder bdHolder, DependencyDescriptor descriptor) {
		ResolvableType dependencyType = descriptor.getResolvableType();
		if (dependencyType.getType() instanceof Class) {
			// 无泛型信息 -> 已知是原始类匹配，无需进一步检查泛型
			return true;
		}

		ResolvableType targetType = null;
		boolean cacheType = false;
		RootBeanDefinition rbd = null;
		if (bdHolder.getBeanDefinition() instanceof RootBeanDefinition) {
			rbd = (RootBeanDefinition) bdHolder.getBeanDefinition();
		}
		if (rbd != null) {
			targetType = rbd.targetType;
			if (targetType == null) {
				cacheType = true;
				// 首先检查工厂方法的返回类型（如适用）
				targetType = getReturnTypeForFactoryMethod(rbd, descriptor);
				if (targetType == null) {
					RootBeanDefinition dbd = getResolvedDecoratedDefinition(rbd);
					if (dbd != null) {
						targetType = dbd.targetType;
						if (targetType == null) {
							targetType = getReturnTypeForFactoryMethod(dbd, descriptor);
						}
					}
				}
			}
		}

		if (targetType == null) {
			// 常规情况：普通 Bean 实例，且 BeanFactory 可用
			if (this.beanFactory != null) {
				Class<?> beanType = this.beanFactory.getType(bdHolder.getBeanName());
				if (beanType != null) {
					targetType = ResolvableType.forClass(ClassUtils.getUserClass(beanType));
				}
			}
			// 回退：未设置 BeanFactory 或无法解析类型
			// -> 若适用，尽量基于目标类进行匹配
			if (targetType == null && rbd != null && rbd.hasBeanClass() && rbd.getFactoryMethodName() == null) {
				Class<?> beanClass = rbd.getBeanClass();
				if (!FactoryBean.class.isAssignableFrom(beanClass)) {
					targetType = ResolvableType.forClass(ClassUtils.getUserClass(beanClass));
				}
			}
		}

		if (targetType == null) {
			return true;
		}
		if (cacheType) {
			rbd.targetType = targetType;
		}
		if (descriptor.fallbackMatchAllowed() &&
				(targetType.hasUnresolvableGenerics() || targetType.resolve() == Properties.class)) {
			// 允许回退匹配时：
			// - 若目标类型泛型无法解析（如原始类型 HashMap），允许匹配（如 Map<String, String>）
			// - java.util.Properties 被视为 Map<String, String> 的语义等价物，可匹配任意 Map 泛型
			return true;
		}
		// 完整检查复杂泛型类型的兼容性
		return dependencyType.isAssignableFrom(targetType);
	}

	@Nullable
	protected RootBeanDefinition getResolvedDecoratedDefinition(RootBeanDefinition rbd) {
		BeanDefinitionHolder decDef = rbd.getDecoratedDefinition();
		if (decDef != null && this.beanFactory instanceof ConfigurableListableBeanFactory) {
			ConfigurableListableBeanFactory clbf = (ConfigurableListableBeanFactory) this.beanFactory;
			if (clbf.containsBeanDefinition(decDef.getBeanName())) {
				BeanDefinition dbd = clbf.getMergedBeanDefinition(decDef.getBeanName());
				if (dbd instanceof RootBeanDefinition) {
					return (RootBeanDefinition) dbd;
				}
			}
		}
		return null;
	}

	@Nullable
	protected ResolvableType getReturnTypeForFactoryMethod(RootBeanDefinition rbd, DependencyDescriptor descriptor) {
		// 应该通常已在 BeanFactory 预解析阶段设置，适用于各种工厂方法场景
		ResolvableType returnType = rbd.factoryMethodReturnType;
		if (returnType == null) {
			Method factoryMethod = rbd.getResolvedFactoryMethod();
			if (factoryMethod != null) {
				returnType = ResolvableType.forMethodReturnType(factoryMethod);
			}
		}
		if (returnType != null) {
			Class<?> resolvedClass = returnType.resolve();
			if (resolvedClass != null && descriptor.getDependencyType().isAssignableFrom(resolvedClass)) {
				// 仅当返回类型足够表达当前依赖时才使用工厂方法元数据
				// 否则可能是单例实例已注册，应以其实际类型为准
				return returnType;
			}
		}
		return null;
	}


	/**
	 * 该实现通过标准的 {@link Cloneable} 支持克隆所有实例字段，允许对克隆后的实例
	 * 通过新的 {@link #setBeanFactory} 调用进行重新配置。
	 *
	 * @see #clone()
	 */
	@Override
	public AutowireCandidateResolver cloneIfNecessary() {
		try {
			return (AutowireCandidateResolver) clone();
		}
		catch (CloneNotSupportedException ex) {
			throw new IllegalStateException(ex);
		}
	}

}
