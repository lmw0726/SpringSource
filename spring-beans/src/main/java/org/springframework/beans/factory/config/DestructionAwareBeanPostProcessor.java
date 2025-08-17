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

package org.springframework.beans.factory.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;

/**
 * {@link BeanPostProcessor} 的子接口，增加了销毁前的回调功能。
 *
 * <p>典型用法是在特定类型的 bean 上调用自定义销毁回调，
 * 与相应的初始化回调相匹配。
 *
 * @author Juergen Hoeller
 * @since 1.0.1
 */
public interface DestructionAwareBeanPostProcessor extends BeanPostProcessor {

	/**
	 * 在给定 bean 实例被销毁之前应用此 BeanPostProcessor，
	 * 例如调用自定义销毁回调。
	 *
	 * <p>与 {@link DisposableBean} 的 {@code destroy} 方法或自定义销毁方法类似，
	 * 此回调只适用于容器完全管理其生命周期的 bean，
	 * 通常是单例或具有作用域的 bean。
	 *
	 * @param bean 要销毁的 bean 实例
	 * @param beanName bean 的名称
	 * @throws org.springframework.beans.BeansException 出现错误时抛出
	 * @see org.springframework.beans.factory.DisposableBean#destroy()
	 * @see org.springframework.beans.factory.support.AbstractBeanDefinition#setDestroyMethodName(String)
	 */
	void postProcessBeforeDestruction(Object bean, String beanName) throws BeansException;

	/**
	 * 判断给定的 bean 实例是否需要由此后处理器执行销毁。
	 *
	 * <p>默认实现返回 {@code true}。如果 pre-5 版本的
	 * {@code DestructionAwareBeanPostProcessor} 没有提供该方法的具体实现，
	 * Spring 也会默认假定返回 {@code true}。
	 *
	 * @param bean 要检查的 bean 实例
	 * @return 如果最终需要调用 {@link #postProcessBeforeDestruction} 方法返回 {@code true}，
	 *         否则返回 {@code false}
	 * @since 4.3
	 */
	default boolean requiresDestruction(Object bean) {
		return true;
	}

}
