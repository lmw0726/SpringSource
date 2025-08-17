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

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.lang.Nullable;

/**
 * 用于确定特定 bean 定义是否符合特定依赖项自动注入候选资格的策略接口。
 *
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @since 2.5
 */
public interface AutowireCandidateResolver {

	/**
	 * 确定给定的 bean 定义是否符合给定依赖项的自动注入候选资格。
	 * <p>默认实现检查
	 * {@link org.springframework.beans.factory.config.BeanDefinition#isAutowireCandidate()}。
	 *
	 * @param bdHolder   包含 bean 名称和别名的 bean 定义持有者
	 * @param descriptor 目标方法参数或字段的描述符
	 * @return 该 bean 定义是否符合自动注入候选资格
	 * @see org.springframework.beans.factory.config.BeanDefinition#isAutowireCandidate()
	 */
	default boolean isAutowireCandidate(BeanDefinitionHolder bdHolder, DependencyDescriptor descriptor) {
		return bdHolder.getBeanDefinition().isAutowireCandidate();
	}

	/**
	 * 确定给定的描述符是否实际上是必需的。
	 * <p>默认实现检查{@link DependencyDescriptor#isRequired()}。
	 *
	 * @param descriptor 目标方法参数或字段的描述符
	 * @return 描述符是否标记为必需，或者可能以其他方式指示为非必需（例如通过参数注解）
	 * @see DependencyDescriptor#isRequired()
	 * @since 5.0
	 */
	default boolean isRequired(DependencyDescriptor descriptor) {
		return descriptor.isRequired();
	}

	/**
	 * 确定给定的描述符是否声明了超出类型的限定符
	 *（通常——但不一定——是某种特定的注解）。
	 * <p>默认实现返回 {@code false}。
	 *
	 * @param descriptor 目标方法参数或字段的描述符
	 * @return 该描述符是否声明了限定符，从而在类型匹配的基础上进一步缩小候选范围
	 * @see org.springframework.beans.factory.annotation.QualifierAnnotationAutowireCandidateResolver#hasQualifier
	 * @since 5.1
	 */
	default boolean hasQualifier(DependencyDescriptor descriptor) {
		return false;
	}

	/**
	 * 确定给定依赖是否建议使用默认值。
	 * <p>默认实现简单地返回{@code null}。
	 *
	 * @param descriptor 目标方法参数或字段的描述符
	 * @return 建议的值（通常是表达式字符串），如果没有找到则为{@code null}
	 * @since 3.0
	 */
	@Nullable
	default Object getSuggestedValue(DependencyDescriptor descriptor) {
		return null;
	}

	/**
	 * 如果注入点有要求，则构建一个代理，用于延迟解析实际的依赖目标。
	 * <p>默认实现直接返回 {@code null}。
	 *
	 * @param descriptor 注入点对应的目标方法参数或字段的描述符
	 * @param beanName   包含该注入点的 bean 的名称
	 * @return 实际依赖目标的延迟解析代理；如果应直接解析，则返回 {@code null}
	 * @since 4.0
	 */
	@Nullable
	default Object getLazyResolutionProxyIfNecessary(DependencyDescriptor descriptor, @Nullable String beanName) {
		return null;
	}

	/**
	 * 如有必要，返回此解析器实例的克隆副本，保留其本地配置，并允许克隆后的实例与新的 bean 工厂关联；
	 * 如果该解析器无须保留状态，则返回原始实例本身。
	 * <p>默认实现通过默认类构造函数创建一个独立实例，假设没有需要复制的特定配置状态。
	 * 子类可以重写此方法，以支持自定义的配置状态处理，或实现标准的 {@link Cloneable} 接口
	 *（如 Spring 自身可配置的 {@code AutowireCandidateResolver} 变体所做），也可以直接返回 {@code this}
	 *（如 {@link SimpleAutowireCandidateResolver} 所示）。
	 *
	 * @see GenericTypeAwareAutowireCandidateResolver#cloneIfNecessary()
	 * @see DefaultListableBeanFactory#copyConfigurationFrom
	 * @since 5.2.7
	 */
	default AutowireCandidateResolver cloneIfNecessary() {
		return BeanUtils.instantiateClass(getClass());
	}

}
