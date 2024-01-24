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

package org.springframework.beans.factory;

/**
 * 需要在由{@link BeanFactory}设置了所有bean属性之后做出反应的bean实现的接口：
 * 例如，执行自定义初始化，或者仅仅检查是否已设置了所有必需的属性。
 *
 * <p>实现{@code InitializingBean}的另一种方法是指定自定义初始化方法，例如在XML bean定义中。
 * 有关所有bean生命周期方法的列表，请参见{@link BeanFactory BeanFactory javadocs}。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see DisposableBean
 * @see org.springframework.beans.factory.config.BeanDefinition#getPropertyValues()
 * @see org.springframework.beans.factory.support.AbstractBeanDefinition#getInitMethodName()
 */
public interface InitializingBean {

	/**
	 * 在包含的{@code BeanFactory}设置了所有bean属性并满足{@link BeanFactoryAware}、
	 * {@code ApplicationContextAware}等之后调用。
	 * <p>此方法允许bean实例在设置了所有bean属性后执行其整体配置的验证和最终初始化。
	 *
	 * @throws Exception 如果存在配置错误（例如未设置必需属性）或由于其他原因导致初始化失败
	 */
	void afterPropertiesSet() throws Exception;

}
