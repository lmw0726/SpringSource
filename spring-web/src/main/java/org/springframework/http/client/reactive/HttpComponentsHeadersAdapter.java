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

package org.springframework.http.client.reactive;

import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpMessage;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;

import java.util.*;

/**
 * {@code MultiValueMap}实现，用于包装Apache HttpComponents HttpClient的头部。
 *
 * @author Rossen Stoyanchev
 * @since 5.3
 */
class HttpComponentsHeadersAdapter implements MultiValueMap<String, String> {
	/**
	 * Http消息
	 */
	private final HttpMessage message;


	HttpComponentsHeadersAdapter(HttpMessage message) {
		this.message = message;
	}


	@Override
	public String getFirst(String key) {
		// 获取指定键对应的第一个头部
		Header header = this.message.getFirstHeader(key);
		// 如果头部不为 null，则返回其值；
		// 否则返回 null
		return (header != null ? header.getValue() : null);
	}

	@Override
	public void add(String key, @Nullable String value) {
		this.message.addHeader(key, value);
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
		this.message.setHeader(key, value);
	}

	@Override
	public void setAll(Map<String, String> values) {
		values.forEach(this::set);
	}

	@Override
	public Map<String, String> toSingleValueMap() {
		// 创建一个 映射
		Map<String, String> map = CollectionUtils.newLinkedHashMap(size());

		// 将每个头部的名称和值放入 映射 中
		this.message.headerIterator().forEachRemaining(h -> map.putIfAbsent(h.getName(), h.getValue()));

		// 返回包含消息所有头部名称和值的 映射
		return map;
	}

	@Override
	public int size() {
		return this.message.getHeaders().length;
	}

	@Override
	public boolean isEmpty() {
		return (this.message.getHeaders().length == 0);
	}

	@Override
	public boolean containsKey(Object key) {
		return (key instanceof String && this.message.containsHeader((String) key));
	}

	@Override
	public boolean containsValue(Object value) {
		// 如果值是字符串类型
		return (value instanceof String &&
				// 并且检查是否有任何头部的值与给定值相等
				Arrays.stream(this.message.getHeaders()).anyMatch(h -> h.getValue().equals(value)));
	}

	@Nullable
	@Override
	public List<String> get(Object key) {
		List<String> values = null;
		// 如果消息中包含指定键的头部
		if (containsKey(key)) {
			// 获取指定键对应的所有头部
			Header[] headers = this.message.getHeaders((String) key);
			// 创建一个 值列表
			values = new ArrayList<>(headers.length);
			// 将每个头部的值添加到 值列表 中
			for (Header header : headers) {
				values.add(header.getValue());
			}
		}
		// 返回包含指定键所有头部值的列表，如果键不存在则返回 null
		return values;
	}

	@Nullable
	@Override
	public List<String> put(String key, List<String> values) {
		// 移除指定键的所有旧值，并返回旧值列表
		List<String> oldValues = remove(key);

		// 遍历新的值列表，并将每个值添加到指定键中
		values.forEach(value -> add(key, value));

		// 返回旧的值列表
		return oldValues;
	}

	@Nullable
	@Override
	public List<String> remove(Object key) {
		// 如果键是 字符串 类型
		if (key instanceof String) {
			// 获取指定键的所有旧值
			List<String> oldValues = get(key);
			// 移除消息中指定键对应的所有头部
			this.message.removeHeaders((String) key);
			// 返回被移除的旧值列表
			return oldValues;
		}
		// 如果键不是 字符串 类型，返回 null
		return null;
	}

	@Override
	public void putAll(Map<? extends String, ? extends List<String>> map) {
		map.forEach(this::put);
	}

	@Override
	public void clear() {
		this.message.setHeaders();
	}

	@Override
	public Set<String> keySet() {
		// 创建一个 键集合
		Set<String> keys = new LinkedHashSet<>(size());

		// 遍历消息中的所有头部
		for (Header header : this.message.getHeaders()) {
			// 将每个头部的名称添加到 键集合 中
			keys.add(header.getName());
		}

		// 返回包含消息所有头部名称的集合
		return keys;
	}

	@Override
	public Collection<List<String>> values() {
		// 创建一个 值列表
		Collection<List<String>> values = new ArrayList<>(size());

		// 遍历消息中的所有头部
		for (Header header : this.message.getHeaders()) {
			// 获取每个头部名称对应的值列表，并将其添加到 值列表 中
			values.add(get(header.getName()));
		}

		// 返回包含消息所有头部名称对应值列表的集合
		return values;
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
				return HttpComponentsHeadersAdapter.this.size();
			}
		};
	}


	@Override
	public String toString() {
		return HttpHeaders.formatHeaders(this);
	}


	private class EntryIterator implements Iterator<Entry<String, List<String>>> {
		/**
		 * 头部信息迭代器
		 */
		private final Iterator<Header> iterator = message.headerIterator();

		@Override
		public boolean hasNext() {
			return this.iterator.hasNext();
		}

		@Override
		public Entry<String, List<String>> next() {
			return new HeaderEntry(this.iterator.next().getName());
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
			// 获取 值列表
			List<String> values = HttpComponentsHeadersAdapter.this.get(this.key);
			// 如果值列表不为空，则返回该值列表。否则返回空的列表
			return values != null ? values : Collections.emptyList();
		}

		@Override
		public List<String> setValue(List<String> value) {
			// 获取当前键的所有旧值
			List<String> previousValues = getValue();
			// 设置指定键的新值
			HttpComponentsHeadersAdapter.this.put(this.key, value);
			// 返回旧的所有值
			return previousValues;
		}
	}

}
