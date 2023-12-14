/*
 * Copyright 2002-2016 the original author or authors.
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

import java.lang.reflect.Constructor;

/**
 * Extension of the {@link InstantiationAwareBeanPostProcessor} interface,
 * adding a callback for predicting the eventual type of a processed bean.
 *
 * <p><b>NOTE:</b> This interface is a special purpose interface, mainly for
 * internal use within the framework. In general, application-provided
 * post-processors should simply implement the plain {@link BeanPostProcessor}
 * interface or derive from the {@link InstantiationAwareBeanPostProcessorAdapter}
 * class. New methods might be added to this interface even in point releases.
 *
 * @author Juergen Hoeller
 * @see InstantiationAwareBeanPostProcessorAdapter
 * @since 2.0.3
 */
public interface SmartInstantiationAwareBeanPostProcessor extends InstantiationAwareBeanPostProcessor {

	/**
	 * 预测最终从此处理器的 {@link #postProcessBeforeInstantiation} 回调返回的bean的类型。
	 * <p> 默认实现返回 {@code null}。
	 *
	 * @param beanClass bean原始类
	 * @param beanName  bean名称
	 * @return bean的类型，如果不可预测，则为 {@code null}
	 * @throws org.springframework.beans.BeansException 在错误的情况下
	 */
	@Nullable
	default Class<?> predictBeanType(Class<?> beanClass, String beanName) throws BeansException {
		return null;
	}

	/**
	 * 确定要用于给定bean的候选构造函数。
	 * <p> 默认实现返回 {@code null}。
	 *
	 * @param beanClass bean的原始类型（从不为 {@code null}）
	 * @param beanName  bean名称
	 * @return 候选构造函数，或者 {@code null} (如果未指定)
	 * @throws org.springframework.beans.BeansException in case of errors
	 */
	@Nullable
	default Constructor<?>[] determineCandidateConstructors(Class<?> beanClass, String beanName)
			throws BeansException {

		return null;
	}

	/**
	 * 获取用于提前访问指定bean的引用，通常用于解析循环引用。
	 * <p> 此回调使后处理器有机会尽早公开包装器-即在目标bean实例完全初始化之前。
	 * 暴露的对象应该等同于 {@link #postProcessBeforeInitialization} {@link #postProcessAfterInitialization} 会以其他方式暴露的对象。
	 * 请注意，除非后处理器返回与所述后进程回调不同的包装，否则此方法返回的对象将用作bean引用。
	 * 换句话说: 这些后处理回调可能最终会公开相同的引用，或者从这些后续回调中返回原始bean实例
	 * (如果已经为该方法的调用构建了受影响bean的包装器，则默认情况下将其公开为最终bean引用)。
	 * <p> 默认实现按状态返回给定的 {@code bean}。
	 *
	 * @param bean     原始bean实例
	 * @param beanName bean名称
	 * @return 要作为bean引用公开的对象 (通常以传入的bean实例为默认值)
	 * @throws org.springframework.beans.BeansException 在错误的情况下
	 */
	default Object getEarlyBeanReference(Object bean, String beanName) throws BeansException {
		return bean;
	}

}
