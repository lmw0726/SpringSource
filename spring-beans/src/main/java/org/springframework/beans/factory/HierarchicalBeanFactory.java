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

import org.springframework.lang.Nullable;

/**
 * 由可属于层次结构的 bean 工厂实现的子接口。
 *
 * <p>允许以可配置的方式设置父级的 bean 工厂相应的 {@code setParentBeanFactory} 方法，
 * 可以在 ConfigurableBeanFactory 接口中找到。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 07.07.2003
 * @see org.springframework.beans.factory.config.ConfigurableBeanFactory#setParentBeanFactory
 */
public interface HierarchicalBeanFactory extends BeanFactory {

	/**
	 * 返回父级 bean 工厂，如果没有则返回 {@code null}。
	 */
	@Nullable
	BeanFactory getParentBeanFactory();

	/**
	 * 返回本地 bean 工厂是否包含给定名称的 bean，忽略祖先上下文中定义的 bean。
	 * <p>这是一个替代方法 {@code containsBean}，它忽略祖先 bean 工厂中给定名称的 bean。
	 *
	 * @param name 要查询的 bean 的名称
	 * @return 本地工厂中是否定义了给定名称的 bean
	 * @see BeanFactory#containsBean
	 */
	boolean containsLocalBean(String name);

}
