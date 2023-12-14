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

package org.springframework.beans.factory.parsing;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link ComponentDefinition} implementation that holds one or more nested
 * {@link ComponentDefinition} instances, aggregating them into a named group
 * of components.
 *
 * @author Juergen Hoeller
 * @see #getNestedComponents()
 * @since 2.0.1
 */
public class CompositeComponentDefinition extends AbstractComponentDefinition {

	/**
	 * 名称
	 */
	private final String name;

	/**
	 * 源对象
	 */
	@Nullable
	private final Object source;

	/**
	 * 嵌套的组件定义列表
	 */
	private final List<ComponentDefinition> nestedComponents = new ArrayList<>();


	/**
	 * 创建一个新的CompositeComponentDefinition
	 *
	 * @param name   复合组件的名称
	 * @param source 定义复合组件根的源元素
	 */
	public CompositeComponentDefinition(String name, @Nullable Object source) {
		Assert.notNull(name, "Name must not be null");
		this.name = name;
		this.source = source;
	}


	@Override
	public String getName() {
		return this.name;
	}

	@Override
	@Nullable
	public Object getSource() {
		return this.source;
	}


	/**
	 * 将给定的组件添加为此复合组件的嵌套元素。
	 *
	 * @param component 要添加的嵌套组件
	 */
	public void addNestedComponent(ComponentDefinition component) {
		Assert.notNull(component, "ComponentDefinition must not be null");
		this.nestedComponents.add(component);
	}

	/**
	 * 返回此复合组件容纳的嵌套组件。
	 *
	 * @return 嵌套组件的数组，如果没有，则为空数组
	 */
	public ComponentDefinition[] getNestedComponents() {
		return this.nestedComponents.toArray(new ComponentDefinition[0]);
	}

}
