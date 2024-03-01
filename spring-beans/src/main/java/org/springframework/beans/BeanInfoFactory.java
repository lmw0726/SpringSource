/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.beans;

import org.springframework.lang.Nullable;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;

/**
 * 用于为 Spring bean 创建 {@link BeanInfo} 实例的策略接口。
 * 可以用于插入自定义的 bean 属性解析策略（例如，用于 JVM 上的其他语言）或更高效的 {@link BeanInfo} 检索算法。
 *
 * <p>BeanInfoFactories 由 {@link CachedIntrospectionResults} 实例化，通过使用 {@link org.springframework.core.io.support.SpringFactoriesLoader} 实用工具类。
 *
 * 当要创建 {@link BeanInfo} 时，{@code CachedIntrospectionResults} 将遍历已发现的工厂，在每个工厂上调用 {@link #getBeanInfo(Class)}。
 * 如果返回 {@code null}，则将查询下一个工厂。如果没有工厂支持该类，则将创建标准的 {@link BeanInfo} 作为默认值。
 *
 * <p>注意，{@link org.springframework.core.io.support.SpringFactoriesLoader} 会根据 {@link org.springframework.core.annotation.Order @Order} 对 {@code BeanInfoFactory} 实例进行排序，以使具有更高优先级的实例首先出现。
 *
 * @author Arjen Poutsma
 * @since 3.2
 * @see CachedIntrospectionResults
 * @see org.springframework.core.io.support.SpringFactoriesLoader
 */
public interface BeanInfoFactory {

	/**
	 * 返回给定类的 bean 信息（如果支持）。
	 *
	 * @param beanClass 要检索 bean 信息的类
	 * @return BeanInfo，如果给定类不受支持，则为 {@code null}
	 * @throws IntrospectionException 如果发生异常
	 */
	@Nullable
	BeanInfo getBeanInfo(Class<?> beanClass) throws IntrospectionException;

}
