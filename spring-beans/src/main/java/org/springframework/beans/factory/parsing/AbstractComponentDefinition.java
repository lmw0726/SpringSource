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

package org.springframework.beans.factory.parsing;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanReference;

/**
 * {@link ComponentDefinition}的基础实现，提供了{@link #getDescription}的基本实现，
 * 该实现委托给{@link #getName}。还提供了{@link #toString}的基础实现，
 * 该实现按照推荐的实现策略委托给{@link #getDescription}。还提供了
 * {@link #getInnerBeanDefinitions}和{@link #getBeanReferences}的默认实现，
 * 返回空数组。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 */
public abstract class AbstractComponentDefinition implements ComponentDefinition {

	/**
	 * 委托给 {@link #getName}.
	 */
	@Override
	public String getDescription() {
		return getName();
	}

	/**
	 * 返回一个空数组
	 */
	@Override
	public BeanDefinition[] getBeanDefinitions() {
		return new BeanDefinition[0];
	}

	/**
	 * 返回一个空数组。
	 */
	@Override
	public BeanDefinition[] getInnerBeanDefinitions() {
		return new BeanDefinition[0];
	}

	/**
	 * 返回一个空数组。
	 */
	@Override
	public BeanReference[] getBeanReferences() {
		return new BeanReference[0];
	}

	/**
	 * 委托给 {@link #getDescription}.
	 */
	@Override
	public String toString() {
		return getDescription();
	}

}
