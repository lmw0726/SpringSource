/*
 * Copyright 2002-2022 the original author or authors.
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

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.Mergeable;
import org.springframework.lang.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tag collection class used to hold managed List elements, which may
 * include runtime bean references (to be resolved into bean objects).
 *
 * @param <E> the element type
 * @author Rod Johnson
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since 27.05.2003
 */
@SuppressWarnings("serial")
public class ManagedList<E> extends ArrayList<E> implements Mergeable, BeanMetadataElement {

	/**
	 * 元数据元素配置的源
	 */
	@Nullable
	private Object source;

	/**
	 * 元素类型名称
	 */
	@Nullable
	private String elementTypeName;

	/**
	 * 是否可以进行属性合并
	 */
	private boolean mergeEnabled;


	public ManagedList() {
	}

	public ManagedList(int initialCapacity) {
		super(initialCapacity);
	}


	/**
	 * 创建一个包含任意数量元素的新实例。
	 *
	 * @param elements 要包含在列表中的元素
	 * @param <E>      {@code List}的 元素类型
	 * @return 包含指定元素的 {@code ManagedList}
	 * @since 5.3.16
	 */
	@SafeVarargs
	public static <E> ManagedList<E> of(E... elements) {
		ManagedList<E> list = new ManagedList<>();
		Collections.addAll(list, elements);
		return list;
	}

	/**
	 * 设置此元数据元素的配置源 {@code Object}。
	 * <p> 对象的确切类型将取决于所使用的配置机制。
	 */
	public void setSource(@Nullable Object source) {
		this.source = source;
	}

	@Override
	@Nullable
	public Object getSource() {
		return this.source;
	}

	/**
	 * 设置用于此列表的默认元素类型名称(类名称)。
	 */
	public void setElementTypeName(String elementTypeName) {
		this.elementTypeName = elementTypeName;
	}

	/**
	 * 返回用于此列表的默认元素类型名称(类名称)。
	 */
	@Nullable
	public String getElementTypeName() {
		return this.elementTypeName;
	}

	/**
	 * 设置如果存在“父”集合值，是否应该为该集合启用合并。
	 */
	public void setMergeEnabled(boolean mergeEnabled) {
		this.mergeEnabled = mergeEnabled;
	}

	@Override
	public boolean isMergeEnabled() {
		return this.mergeEnabled;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<E> merge(@Nullable Object parent) {
		if (!this.mergeEnabled) {
			//如果无法合并，抛出异常
			throw new IllegalStateException("Not allowed to merge when the 'mergeEnabled' property is set to 'false'");
		}
		if (parent == null) {
			//如果要合并的父元素为空，返回当前的ManagedList
			return this;
		}
		if (!(parent instanceof List)) {
			//如果父元素不是List类型，抛出异常
			throw new IllegalArgumentException("Cannot merge with object of type [" + parent.getClass() + "]");
		}
		//重新构建一个新的ManagedList，先将父元素添加进来，再将当前元素添加进来
		List<E> merged = new ManagedList<>();
		merged.addAll((List<E>) parent);
		merged.addAll(this);
		return merged;
	}

}
