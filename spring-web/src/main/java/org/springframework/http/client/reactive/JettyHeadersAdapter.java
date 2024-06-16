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

package org.springframework.http.client.reactive;

import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpFields;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;

import java.util.*;
import java.util.stream.Collectors;

/**
 * {@code MultiValueMap} 的实现，用于包装 Jetty HTTP 头信息。
 *
 * <p>此类的副本也存在于 server 包中！
 *
 * @author Rossen Stoyanchev
 * @since 5.3
 */
class JettyHeadersAdapter implements MultiValueMap<String, String> {
	/**
	 * 存放Http头部的Http字段
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
		// 创建一个 单值映射 对象
		Map<String, String> singleValueMap = CollectionUtils.newLinkedHashMap(this.headers.size());

		// 获取迭代器
		Iterator<HttpField> iterator = this.headers.iterator();
		iterator.forEachRemaining(field -> {
			// 如果单值映射不含有该字段名称
			if (!singleValueMap.containsKey(field.getName())) {
				// 将键值对添加到单值映射中
				singleValueMap.put(field.getName(), field.getValue());
			}
		});

		// 返回最终的 单值映射
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
		// 检查是否包含特定键
		if (containsKey(key)) {
			// 如果包含，则返回该键对应的所有值的列表
			return this.headers.getValuesList((String) key);
		}
		// 如果不包含特定键，则返回 null
		return null;
	}

	@Nullable
	@Override
	public List<String> put(String key, List<String> value) {
		// 获取指定键的值列表
		List<String> oldValues = get(key);
		// 将键值对添加到头部
		this.headers.put(key, value);
		// 返回旧值列表
		return oldValues;
	}

	@Nullable
	@Override
	public List<String> remove(Object key) {
		// 如果 键 是 字符串 类型的实例
		if (key instanceof String) {
			// 获取指定键的值列表
			List<String> oldValues = get(key);
			// 从头部信息移除指定键
			this.headers.remove((String) key);
			// 返回旧值列表
			return oldValues;
		}

		// 如果 键 不是 字符串 类型的实例，则返回 false
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
		 * 头部名称枚举
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
			// 获取该键对应的旧值列表
			List<String> previousValues = headers.getValuesList(this.key);
			// 将键值对添加到头部
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
			// 如果 当前名称 为 null
			if (this.currentName == null) {
				// 抛出 IllegalStateException 异常，指示迭代器中没有当前的头部
				throw new IllegalStateException("No current Header in iterator");
			}

			// 如果 Http头部 中不包含当前名称
			if (!headers.containsKey(this.currentName)) {
				// 抛出 IllegalStateException 异常，指示当前名称的头部不存在
				throw new IllegalStateException("Header not present: " + this.currentName);
			}

			// 移除 Http头部 中的当前名称
			headers.remove(this.currentName);
		}
	}

}
