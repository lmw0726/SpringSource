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

package org.springframework.core.env;

import org.springframework.lang.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * 只读的 {@code Map<String, String>} 实现，由系统属性或环境变量支持。
 *
 * <p>当 {@link SecurityManager} 禁止访问 {@link System#getProperties()} 或
 * {@link System#getenv()} 时，由 {@link AbstractEnvironment} 使用。正因为如此，
 * {@link #keySet()}、{@link #entrySet()} 和 {@link #values()} 的实现总是返回空集合，
 * 即使 {@link #get(Object)} 实际上可能返回非空值（如果当前安全管理器允许访问单个键）。
 *
 * @author Arjen Poutsma
 * @author Chris Beams
 * @since 3.0
 */
abstract class ReadOnlySystemAttributesMap implements Map<String, String> {

	@Override
	public boolean containsKey(Object key) {
		return (get(key) != null);
	}

	/**
	 * 返回指定键所映射的值，如果此映射不包含该键的映射关系，则返回 {@code null}。
	 * @param key 要检索的系统属性名称
	 * @throws IllegalArgumentException 如果给定的键不是字符串类型
	 */
	@Override
	@Nullable
	public String get(Object key) {
		if (!(key instanceof String)) {
			throw new IllegalArgumentException(
					"Type of key [" + key.getClass().getName() + "] must be java.lang.String");
		}
		return getSystemAttribute((String) key);
	}

	@Override
	public boolean isEmpty() {
		return false;
	}

	/**
	 * 返回底层系统属性的模板方法。
	 * <p>实现通常会在这里调用 {@link System#getProperty(String)} 或 {@link System#getenv(String)}。
	 */
	@Nullable
	protected abstract String getSystemAttribute(String attributeName);


	// 不支持的操作

	@Override
	public int size() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String put(String key, String value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean containsValue(Object value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String remove(Object key) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<String> keySet() {
		return Collections.emptySet();
	}

	@Override
	public void putAll(Map<? extends String, ? extends String> map) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Collection<String> values() {
		return Collections.emptySet();
	}

	@Override
	public Set<Entry<String, String>> entrySet() {
		return Collections.emptySet();
	}

}
