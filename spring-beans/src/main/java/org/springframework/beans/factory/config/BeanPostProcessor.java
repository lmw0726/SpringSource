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

package org.springframework.beans.factory.config;

import org.springframework.beans.BeansException;
import org.springframework.lang.Nullable;

/**
 * 工厂钩子，允许对新的bean实例进行自定义修改，例如检查标记接口或使用代理包装bean。
 *
 * 通常，通过标记接口等方式填充bean的后处理器将实现{@link #postProcessBeforeInitialization}，
 * 而通常使用代理包装bean的后处理器将实现{@link #postProcessAfterInitialization}。
 *
 * <h3>注册</h3>
 * <p>{@code ApplicationContext}可以在其bean定义中自动检测{@code BeanPostProcessor} bean，
 * 并将这些后处理器应用于随后创建的任何bean。普通的{@code BeanFactory}允许通过编程方式注册后处理器，
 * 将其应用于通过bean工厂创建的所有bean。
 *
 * <h3>排序</h3>
 * <p>在{@code ApplicationContext}中自动检测到的{@code BeanPostProcessor} bean将根据
 * {@link org.springframework.core.PriorityOrdered}和{@link org.springframework.core.Ordered}语义进行排序。
 * 相反，通过{@code BeanFactory}以编程方式注册的{@code BeanPostProcessor} bean将按照注册的顺序应用；
 * 通过实现{@code PriorityOrdered}或{@code Ordered}接口表达的任何排序语义将被忽略，对于通过编程方式注册的后处理器，
 * {@link org.springframework.core.annotation.Order @Order}注解也不会被考虑。
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @see InstantiationAwareBeanPostProcessor
 * @see DestructionAwareBeanPostProcessor
 * @see ConfigurableBeanFactory#addBeanPostProcessor
 * @see BeanFactoryPostProcessor
 * @since 10.10.2003
 */
public interface BeanPostProcessor {

	/**
	 * 将此 {@code BeanPostProcessor} 应用于给定的新bean实例 <i> 之前</i>的任何bean初始化回调
	 * (例如InitializingBean的 {@code afterPropertiesSet} 或自定义init-方法)。
	 * bean将已经用属性值填充。
	 * 返回的bean实例可能是围绕原始的包装器。
	 * <p> 默认实现按状态返回给定的 {@code bean}。
	 *
	 * @param bean     bean的新实例
	 * @param beanName bean名称
	 * @return 要使用的bean实例 (原始实例或包装实例); 如果 {@code null}，则不会调用后续的BeanPostProcessors
	 * @throws org.springframework.beans.BeansException 在错误的情况下
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet
	 */
	@Nullable
	default Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	/**
	 * 在任何bean初始化回调 (例如InitializingBean的 {@code afterPropertiesSet} 或自定义init方法) <i>之后</i>，
	 * 将此 {@code BeanPostProcessor} 应用于给定的新bean实例 。
	 * bean将已经用属性值填充。
	 * 返回的bean实例可能是围绕原始的包装器。
	 * <p> 如果是FactoryBean，则将为FactoryBean实例和FactoryBean创建的对象调用此回调 (从Spring 2.0开始)。
	 * 后处理器可以通过相应的 {@code bean instanceof FactoryBean} 检查来决定是应用于FactoryBean还是创建的对象，还是同时应用于这两者。
	 * <p> 与所有其他 {@code BeanPostProcessor} 回调相反，
	 * 该回调也将在由 {@link InstantiationAwareBeanPostProcessor#postProcessBeforeInstantiation} 方法触发的短路后调用。
	 * <p> 默认实现按状态返回给定的 {@code bean}。
	 *
	 * @param bean     bean的新实例
	 * @param beanName bean名称
	 * @return 要使用的bean实例 (原始实例或包装实例); 如果 {@code null}，则不会调用后续的BeanPostProcessors
	 * @throws org.springframework.beans.BeansException 在错误的情况下
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet
	 * @see org.springframework.beans.factory.FactoryBean
	 */
	@Nullable
	default Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

}
