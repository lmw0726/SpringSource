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
 * Factory hook that allows for custom modification of new bean instances &mdash;
 * for example, checking for marker interfaces or wrapping beans with proxies.
 *
 * <p>Typically, post-processors that populate beans via marker interfaces
 * or the like will implement {@link #postProcessBeforeInitialization},
 * while post-processors that wrap beans with proxies will normally
 * implement {@link #postProcessAfterInitialization}.
 *
 * <h3>Registration</h3>
 * <p>An {@code ApplicationContext} can autodetect {@code BeanPostProcessor} beans
 * in its bean definitions and apply those post-processors to any beans subsequently
 * created. A plain {@code BeanFactory} allows for programmatic registration of
 * post-processors, applying them to all beans created through the bean factory.
 *
 * <h3>Ordering</h3>
 * <p>{@code BeanPostProcessor} beans that are autodetected in an
 * {@code ApplicationContext} will be ordered according to
 * {@link org.springframework.core.PriorityOrdered} and
 * {@link org.springframework.core.Ordered} semantics. In contrast,
 * {@code BeanPostProcessor} beans that are registered programmatically with a
 * {@code BeanFactory} will be applied in the order of registration; any ordering
 * semantics expressed through implementing the
 * {@code PriorityOrdered} or {@code Ordered} interface will be ignored for
 * programmatically registered post-processors. Furthermore, the
 * {@link org.springframework.core.annotation.Order @Order} annotation is not
 * taken into account for {@code BeanPostProcessor} beans.
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
