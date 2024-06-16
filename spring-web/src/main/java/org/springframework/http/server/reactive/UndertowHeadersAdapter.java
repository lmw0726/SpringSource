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

import io.undertow.util.HeaderMap;
import io.undertow.util.HeaderValues;
import io.undertow.util.HttpString;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;

import java.util.*;
import java.util.stream.Collectors;

/**
 * {@code MultiValueMap} 实现用于包装 Undertow HTTP 头信息。
 *
 * @author Brian Clozel
 * @author Sam Brannen
 * @since 5.1.1
 */
class UndertowHeadersAdapter implements MultiValueMap<String, String> {
	/**
	 * Undertow 头部映射
	 */
	private final HeaderMap headers;


	UndertowHeadersAdapter(HeaderMap headers) {
		this.headers = headers;
	}


	@Override
	public String getFirst(String key) {
		return this.headers.getFirst(key);
	}

	@Override
	public void add(String key, @Nullable String value) {
		this.headers.add(HttpString.tryFromString(key), value);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void addAll(String key, List<? extends String> values) {
		this.headers.addAll(HttpString.tryFromString(key), (List<String>) values);
	}

	@Override
	public void addAll(MultiValueMap<String, String> values) {
		values.forEach((key, list) -> this.headers.addAll(HttpString.tryFromString(key), list));
	}

	@Override
	public void set(String key, @Nullable String value) {
		this.headers.put(HttpString.tryFromString(key), value);
	}

	@Override
	public void setAll(Map<String, String> values) {
		values.forEach((key, list) -> this.headers.put(HttpString.tryFromString(key), list));
	}

	@Override
	public Map<String, String> toSingleValueMap() {
		// 创建一个 单值映射表 对象
		Map<String, String> singleValueMap = CollectionUtils.newLinkedHashMap(this.headers.size());

		// 遍历 头部映射
		this.headers.forEach(values ->
				// 将每个 HeaderValues 对象的头部名称和第一个值放入 单值映射表
				singleValueMap.put(values.getHeaderName().toString(), values.getFirst())
		);

		// 返回 单值映射表
		return singleValueMap;
	}

	@Override
	public int size() {
		return this.headers.size();
	}

	@Override
	public boolean isEmpty() {
		return (this.headers.size() == 0);
	}

	@Override
	public boolean containsKey(Object key) {
		return (key instanceof String && this.headers.contains((String) key));
	}

	@Override
	public boolean containsValue(Object value) {
		// 判断 值 是否为 字符串 类型，并且在 头部映射 的所有头部值中是否有包含该值
		return (value instanceof String &&
				this.headers.getHeaderNames().stream()
						.map(this.headers::get)
						.anyMatch(values -> values.contains(value)));
	}

	@Override
	@Nullable
	public List<String> get(Object key) {
		// 如果 键 是 字符串 类型的实例
		if (key instanceof String) {
			// 返回 头部信息 中对应键 的值
			return this.headers.get((String) key);
		}

		// 如果 键 不是 字符串 类型的实例，则返回 null
		return null;
	}

	@Override
	@Nullable
	public List<String> put(String key, List<String> value) {
		// 获取指定键  的所有旧值
		HeaderValues previousValues = this.headers.get(key);

		// 将新的值  放入 头部信息 中，对应的键通过 HttpString.tryFromString(key) 转换得到
		this.headers.putAll(HttpString.tryFromString(key), value);

		// 返回之前该键对应的所有旧值
		return previousValues;
	}

	@Override
	@Nullable
	public List<String> remove(Object key) {
		// 如果键是 字符串 类型的实例
		if (key instanceof String) {
			// 删除指定键，并获取删除的旧值列表
			Collection<String> removed = this.headers.remove((String) key);
			if (removed != null) {
				// 如果删除成功，返回获取到的旧值列表
				return new ArrayList<>(removed);
			}
		}
		// 如果 键 不是 字符串 类型的实例，则返回null
		return null;
	}

	@Override
	public void putAll(Map<? extends String, ? extends List<String>> map) {
		map.forEach((key, values) ->
				this.headers.putAll(HttpString.tryFromString(key), values));
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
		return this.headers.getHeaderNames().stream()
				.map(this.headers::get)
				.collect(Collectors.toList());
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
		return org.springframework.http.HttpHeaders.formatHeaders(this);
	}


	private class EntryIterator implements Iterator<Entry<String, List<String>>> {
		/**
		 * Http头部名称迭代器
		 */
		private Iterator<HttpString> names = headers.getHeaderNames().iterator();

		@Override
		public boolean hasNext() {
			return this.names.hasNext();
		}

		@Override
		public Entry<String, List<String>> next() {
			return new HeaderEntry(this.names.next());
		}
	}


	private class HeaderEntry implements Entry<String, List<String>> {

		/**
		 * Http字符串
		 */
		private final HttpString key;

		HeaderEntry(HttpString key) {
			this.key = key;
		}

		@Override
		public String getKey() {
			return this.key.toString();
		}

		@Override
		public List<String> getValue() {
			return headers.get(this.key);
		}

		@Override
		public List<String> setValue(List<String> value) {
			// 获取指定键的旧值列表
			List<String> previousValues = headers.get(this.key);
			// 将指定键更新为新值列表
			headers.putAll(this.key, value);
			// 返回旧值列表
			return previousValues;
		}
	}


	private class HeaderNames extends AbstractSet<String> {

		@Override
		public Iterator<String> iterator() {
			return new HeaderNamesIterator(headers.getHeaderNames().iterator());
		}

		@Override
		public int size() {
			return headers.getHeaderNames().size();
		}
	}

	private final class HeaderNamesIterator implements Iterator<String> {

		/**
		 * Http字符串迭代器
		 */
		private final Iterator<HttpString> iterator;

		/**
		 * 当前名称
		 */
		@Nullable
		private String currentName;

		private HeaderNamesIterator(Iterator<HttpString> iterator) {
			this.iterator = iterator;
		}

		@Override
		public boolean hasNext() {
			return this.iterator.hasNext();
		}

		@Override
		public String next() {
			// 获取下一个字符串作为当前名称
			this.currentName = this.iterator.next().toString();
			// 返回当前名称
			return this.currentName;
		}

		@Override
		public void remove() {
			if (this.currentName == null) {
				// 如果当前名称为空，则抛出异常
				throw new IllegalStateException("No current Header in iterator");
			}
			if (!headers.contains(this.currentName)) {
				// 如果请求头不包含当前名称，则抛出异常
				throw new IllegalStateException("Header not present: " + this.currentName);
			}
			// 从请求头中移除当前名称
			headers.remove(this.currentName);
		}
	}

}
