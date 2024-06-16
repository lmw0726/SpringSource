/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.http.server.reactive;

import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpFields;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 用于包装Jetty HTTP头的 {@code MultiValueMap} 实现。
 *
 * <p>注意：客户端包中存在此类的副本！
 *
 * @author Brian Clozel
 * @since 5.1.1
 */
class JettyHeadersAdapter implements MultiValueMap<String, String> {

	/**
	 * 存放请求头的Http字段
	 */
	private final HttpFields headers;


	JettyHeadersAdapter(HttpFields headers) {
		this.headers = headers;
	}


	@Override
	public String getFirst(String key) {
		return this.headers.get(key);
	}

	@Override
	public void add(String key, @Nullable String value) {
		this.headers.add(key, value);
	}

	@Override
	public void addAll(String key, List<? extends String> values) {
		values.forEach(value -> add(key, value));
	}

	@Override
	public void addAll(MultiValueMap<String, String> values) {
		values.forEach(this::addAll);
	}

	@Override
	public void set(String key, @Nullable String value) {
		this.headers.put(key, value);
	}

	@Override
	public void setAll(Map<String, String> values) {
		values.forEach(this::set);
	}

	@Override
	public Map<String, String> toSingleValueMap() {
		// 创建一个 LinkedHashMap，用于存储头部字段名和对应的唯一值
		Map<String, String> singleValueMap = CollectionUtils.newLinkedHashMap(this.headers.size());

		// 获取请求头集合的迭代器
		Iterator<HttpField> iterator = this.headers.iterator();

		// 使用迭代器遍历 请求头集合 中的每个 Http字段 对象
		iterator.forEachRemaining(field -> {
			// 如果 唯一值的映射 中不包含当前字段名
			if (!singleValueMap.containsKey(field.getName())) {
				// 将字段名和值添加到 唯一值的映射 中
				singleValueMap.put(field.getName(), field.getValue());
			}
		});

		// 返回包含唯一值的映射
		return singleValueMap;
	}

	@Override
	public int size() {
		return this.headers.getFieldNamesCollection().size();
	}

	@Override
	public boolean isEmpty() {
		return (this.headers.size() == 0);
	}

	@Override
	public boolean containsKey(Object key) {
		return (key instanceof String && this.headers.containsKey((String) key));
	}

	@Override
	public boolean containsValue(Object value) {
		return (value instanceof String &&
				this.headers.stream().anyMatch(field -> field.contains((String) value)));
	}

	@Nullable
	@Override
	public List<String> get(Object key) {
		if (containsKey(key)) {
			// 如果 请求头 中包含指定的 键，则返回与该键关联的所有值的列表
			return this.headers.getValuesList((String) key);
		}
		// 如果 请求头 中不包含指定的 键，则返回null
		return null;
	}

	@Nullable
	@Override
	public List<String> put(String key, List<String> value) {
		// 获取指定键在 请求头 中的旧值列表
		List<String> oldValues = get(key);

		// 将指定键的值更新为新的值
		this.headers.put(key, value);

		// 返回旧值列表
		return oldValues;
	}

	@Nullable
	@Override
	public List<String> remove(Object key) {
		if (key instanceof String) {
			// 如果键是String类型的实例

			// 获取指定键在请求头中的旧值列表
			List<String> oldValues = get(key);

			// 移除请求头中指定键对应的条目
			this.headers.remove((String) key);

			// 返回获取到的旧值列表
			return oldValues;
		}

		// 如果 键 不是String类型的实例，则返回null
		return null;

	}

	@Override
	public void putAll(Map<? extends String, ? extends List<String>> map) {
		map.forEach(this::put);
	}

	@Override
	public void clear() {
		this.headers.clear();
	}

	@Override
	public Set<String> keySet() {
		return new HeaderNames();
	}

	@Override
	public Collection<List<String>> values() {
		return this.headers.getFieldNamesCollection().stream()
				.map(this.headers::getValuesList).collect(Collectors.toList());
	}

	@Override
	public Set<Entry<String, List<String>>> entrySet() {
		return new AbstractSet<Entry<String, List<String>>>() {
			@Override
			public Iterator<Entry<String, List<String>>> iterator() {
				return new EntryIterator();
			}

			@Override
			public int size() {
				return headers.size();
			}
		};
	}


	@Override
	public String toString() {
		return HttpHeaders.formatHeaders(this);
	}


	private class EntryIterator implements Iterator<Entry<String, List<String>>> {
		/**
		 * Http字段名称枚举
		 */
		private final Enumeration<String> names = headers.getFieldNames();

		@Override
		public boolean hasNext() {
			return this.names.hasMoreElements();
		}

		@Override
		public Entry<String, List<String>> next() {
			return new HeaderEntry(this.names.nextElement());
		}
	}


	private class HeaderEntry implements Entry<String, List<String>> {
		/**
		 * 键
		 */
		private final String key;

		HeaderEntry(String key) {
			this.key = key;
		}

		@Override
		public String getKey() {
			return this.key;
		}

		@Override
		public List<String> getValue() {
			return headers.getValuesList(this.key);
		}

		@Override
		public List<String> setValue(List<String> value) {
			// 获取指定键的旧值列表
			List<String> previousValues = headers.getValuesList(this.key);
			// 将指定键更新为新值列表
			headers.put(this.key, value);
			// 返回旧值列表
			return previousValues;
		}
	}


	private class HeaderNames extends AbstractSet<String> {

		@Override
		public Iterator<String> iterator() {
			return new HeaderNamesIterator(headers.getFieldNamesCollection().iterator());
		}

		@Override
		public int size() {
			return headers.getFieldNamesCollection().size();
		}
	}


	private final class HeaderNamesIterator implements Iterator<String> {
		/**
		 * 迭代器
		 */
		private final Iterator<String> iterator;

		/**
		 * 当前名称
		 */
		@Nullable
		private String currentName;

		private HeaderNamesIterator(Iterator<String> iterator) {
			this.iterator = iterator;
		}

		@Override
		public boolean hasNext() {
			return this.iterator.hasNext();
		}

		@Override
		public String next() {
			// 获取下一个字符串作为当前名称
			this.currentName = this.iterator.next();
			// 返回当前名称
			return this.currentName;
		}

		@Override
		public void remove() {
			if (this.currentName == null) {
				// 如果当前名称为空，则抛出异常
				throw new IllegalStateException("No current Header in iterator");
			}
			if (!headers.containsKey(this.currentName)) {
				// 如果请求头不包含当前名称，则抛出异常
				throw new IllegalStateException("Header not present: " + this.currentName);
			}
			// 从请求头中移除当前名称
			headers.remove(this.currentName);
		}
	}

}
