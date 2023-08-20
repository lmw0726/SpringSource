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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Tag collection class used to hold managed Set values, which may
 * include runtime bean references (to be resolved into bean objects).
 *
 * @param <E> the element type
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since 21.01.2004
 */
@SuppressWarnings("serial")
public class ManagedSet<E> extends LinkedHashSet<E> implements Mergeable, BeanMetadataElement {

	/**
	 * 源对象
	 */
	@Nullable
	private Object source;

	/**
	 * 元素类型名称
	 */
	@Nullable
	private String elementTypeName;

	/**
	 * 是否可以合并
	 */
	private boolean mergeEnabled;


	public ManagedSet() {
	}

	public ManagedSet(int initialCapacity) {
		super(initialCapacity);
	}


	/**
	 * 创建一个包含任意数量元素的新实例。
	 *
	 * @param elements 集合中要包含的元素
	 * @param <E>      {@code Set} 的元素类型
	 * @return 包含指定元素的 {@code ManagedSet}
	 * @since 5.3.16
	 */
	@SafeVarargs
	public static <E> ManagedSet<E> of(E... elements) {
		ManagedSet<E> set = new ManagedSet<>();
		Collections.addAll(set, elements);
		return set;
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
	 * 设置要用于此集合的默认元素类型名称 (类名)。
	 */
	public void setElementTypeName(@Nullable String elementTypeName) {
		this.elementTypeName = elementTypeName;
	}

	/**
	 * 返回要用于此集合的默认元素类型名称 (类名)。
	 */
	@Nullable
	public String getElementTypeName() {
		return this.elementTypeName;
	}

	/**
	 * 设置是否应为此集合启用合并 (如果存在 “父” 集合值)。
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
	public Set<E> merge(@Nullable Object parent) {
		if (!this.mergeEnabled) {
			//如果不允许合并，抛出异常
			throw new IllegalStateException("Not allowed to merge when the 'mergeEnabled' property is set to 'false'");
		}
		if (parent == null) {
			//如果父对象为空，返回原来的ManagedSet对象
			return this;
		}
		if (!(parent instanceof Set)) {
			//如果父对象不是Set类型，抛出异常
			throw new IllegalArgumentException("Cannot merge with object of type [" + parent.getClass() + "]");
		}
		//构建一个新的ManagedSet对象，先添加父对象的元素值，再添加原来的ManagedSet对象的元素值
		Set<E> merged = new ManagedSet<>();
		merged.addAll((Set<E>) parent);
		merged.addAll(this);
		return merged;
	}

}
