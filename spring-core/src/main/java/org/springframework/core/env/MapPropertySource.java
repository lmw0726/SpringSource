/*
 * Copyright 2002-2020 the original author or authors.
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

import java.util.Map;

/**
 * 从 {@code Map} 对象中读取键和值的 {@link PropertySource}。
 * 底层映射不应包含任何 {@code null} 值，以符合 {@link #getProperty} 和
 * {@link #containsProperty} 的语义。
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 * @see PropertiesPropertySource
 */
public class MapPropertySource extends EnumerablePropertySource<Map<String, Object>> {

	/**
	 * 使用给定的名称和 {@code Map} 创建一个新的 {@code MapPropertySource}。
	 * @param name 关联的名称
	 * @param source Map 源（为了获得一致的 {@link #getProperty} 和
	 * {@link #containsProperty} 行为，不应包含 {@code null} 值）
	 */
	public MapPropertySource(String name, Map<String, Object> source) {
		super(name, source);
	}


	@Override
	@Nullable
	public Object getProperty(String name) {
		return this.source.get(name);
	}

	@Override
	public boolean containsProperty(String name) {
		return this.source.containsKey(name);
	}

	@Override
	public String[] getPropertyNames() {
		return StringUtils.toStringArray(this.source.keySet());
	}

}
