/*
 * Copyright 2002-2018 the original author or authors.
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

import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * 运行时用于合并的bean定义的后处理器回调接口。
 * {@link BeanPostProcessor}实现可以实现此子接口，以便对Spring {@code BeanFactory}用于创建bean实例的合并bean定义（原始bean定义的处理副本）进行后处理。
 *
 * <p> {@link #postProcessMergedBeanDefinition}方法可以例如内省bean定义，以准备一些缓存的元数据，然后再后处理bean的实际实例。也允许修改bean定义，但<i>只</i>适用于实际用于并发修改的定义属性。基本上，这仅适用于在{@link RootBeanDefinition}本身上定义的操作，而不适用于其基类的属性。
 *
 * @author Juergen Hoeller
 * @see org.springframework.beans.factory.config.ConfigurableBeanFactory#getMergedBeanDefinition
 * @since 2.5
 */
public interface MergedBeanDefinitionPostProcessor extends BeanPostProcessor {

	/**
	 * 后处理指定bean的给定合并bean定义。
	 *
	 * @param beanDefinition bean的合并bean定义
	 * @param beanType       托管bean实例的实际类型
	 * @param beanName       bean的名称
	 * @see AbstractAutowireCapableBeanFactory#applyMergedBeanDefinitionPostProcessors
	 */
	void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName);

	/**
	 * 通知指定名称的bean定义已重置，并且此后处理器应清除受影响bean的所有元数据。<p> 默认实现为空。
	 *
	 * @param beanName bean名称
	 * @see DefaultListableBeanFactory#resetBeanDefinition
	 * @since 5.1
	 */
	default void resetBeanDefinition(String beanName) {
	}

}
