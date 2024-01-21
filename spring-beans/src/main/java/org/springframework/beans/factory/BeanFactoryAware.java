/*
 * Copyright 2002-2012 the original author or authors.
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

import org.springframework.beans.BeansException;

/**
 * 由希望了解其所属 {@link BeanFactory} 的bean实现的接口。
 *
 * <p>例如，bean可以通过工厂查找协作bean（依赖查找）。请注意，大多数bean将选择通过相应的bean属性或构造函数参数（依赖注入）接收对协作bean的引用。
 *
 * <p>有关所有bean生命周期方法的列表，请参见
 * {@link BeanFactory BeanFactory javadocs}。
 *
 * @author Rod Johnson
 * @author Chris Beams
 * @see BeanNameAware
 * @see BeanClassLoaderAware
 * @see InitializingBean
 * @see org.springframework.context.ApplicationContextAware
 * @since 11.03.2003
 */
public interface BeanFactoryAware extends Aware {

	/**
	 * 回调，向bean实例提供所属的工厂。
	 * <p>在正常bean属性的填充之后但在初始化回调之前调用，例如
	 * {@link InitializingBean#afterPropertiesSet()} 或自定义的init方法。
	 *
	 * @param beanFactory 所有者BeanFactory（永远不为 {@code null}）。
	 *                    可以立即在bean上调用工厂的方法。
	 * @throws BeansException 在初始化错误的情况下
	 * @see BeanInitializationException
	 */
	void setBeanFactory(BeanFactory beanFactory) throws BeansException;

}
