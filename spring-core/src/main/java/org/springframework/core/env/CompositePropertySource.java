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

package org.springframework.core.env;

import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * 复合 {@link PropertySource} 实现，遍历一组 {@link PropertySource} 实例。
 * 在多个属性源共享相同名称的情况下很有必要，例如，当为 {@code @PropertySource}
 * 提供多个值时。
 *
 * <p>从 Spring 4.1.2 开始，此类扩展了 {@link EnumerablePropertySource} 而不是
 * 普通的 {@link PropertySource}，基于所有包含源的累积属性名称（尽可能）
 * 公开 {@link #getPropertyNames()}。
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @since 3.1.1
 */
public class CompositePropertySource extends EnumerablePropertySource<Object> {

	private final Set<PropertySource<?>> propertySources = new LinkedHashSet<>();


	/**
	 * 创建一个新的 {@code CompositePropertySource}。
	 * @param name 属性源的名称
	 */
	public CompositePropertySource(String name) {
		super(name);
	}


	@Override
	@Nullable
	public Object getProperty(String name) {
		for (PropertySource<?> propertySource : this.propertySources) {
			Object candidate = propertySource.getProperty(name);
			if (candidate != null) {
				return candidate;
			}
		}
		return null;
	}

	@Override
	public boolean containsProperty(String name) {
		for (PropertySource<?> propertySource : this.propertySources) {
			if (propertySource.containsProperty(name)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String[] getPropertyNames() {
		Set<String> names = new LinkedHashSet<>();
		for (PropertySource<?> propertySource : this.propertySources) {
			if (!(propertySource instanceof EnumerablePropertySource)) {
				throw new IllegalStateException(
						"Failed to enumerate property names due to non-enumerable property source: " + propertySource);
			}
			names.addAll(Arrays.asList(((EnumerablePropertySource<?>) propertySource).getPropertyNames()));
		}
		return StringUtils.toStringArray(names);
	}


	/**
	 * 将给定的 {@link PropertySource} 添加到链的末尾。
	 * @param propertySource 要添加的 PropertySource
	 */
	public void addPropertySource(PropertySource<?> propertySource) {
		this.propertySources.add(propertySource);
	}

	/**
	 * 将给定的 {@link PropertySource} 添加到链的开头。
	 * @param propertySource 要添加的 PropertySource
	 * @since 4.1
	 */
	public void addFirstPropertySource(PropertySource<?> propertySource) {
		List<PropertySource<?>> existing = new ArrayList<>(this.propertySources);
		this.propertySources.clear();
		this.propertySources.add(propertySource);
		this.propertySources.addAll(existing);
	}

	/**
	 * 返回此复合源持有的所有属性源。
	 * @since 4.1.1
	 */
	public Collection<PropertySource<?>> getPropertySources() {
		return this.propertySources;
	}


	@Override
	public String toString() {
		return getClass().getSimpleName() + " {name='" + this.name + "', propertySources=" + this.propertySources + "}";
	}

}
