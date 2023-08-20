/*
 * Copyright 2002-2017 the original author or authors.
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

import java.util.Properties;

/**
 * Tag class which represents a Spring-managed {@link Properties} instance
 * that supports merging of parent/child definitions.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 */
@SuppressWarnings("serial")
public class ManagedProperties extends Properties implements Mergeable, BeanMetadataElement {

	@Nullable
	private Object source;

	private boolean mergeEnabled;


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
	public Object merge(@Nullable Object parent) {
		if (!this.mergeEnabled) {
			//如果不可合并，抛出异常
			throw new IllegalStateException("Not allowed to merge when the 'mergeEnabled' property is set to 'false'");
		}
		//如果父对象为空，返回原来的ManagedProperties对象
		if (parent == null) {
			return this;
		}
		if (!(parent instanceof Properties)) {
			//如果父对象不是Properties类型，抛出异常
			throw new IllegalArgumentException("Cannot merge with object of type [" + parent.getClass() + "]");
		}
		//构建一个新的ManagedProperties对象。先添加父对象的属性值，再添加本对象的属性值
		Properties merged = new ManagedProperties();
		merged.putAll((Properties) parent);
		merged.putAll(this);
		return merged;
	}

}
