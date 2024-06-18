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

import io.netty.handler.codec.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 用于包装Netty HTTP头部的{@code MultiValueMap}实现。
 *
 * <p><b>注意：</b> 在服务器包中存在此类的副本！
 *
 * @author Rossen Stoyanchev
 * @since 5.3
 */
class NettyHeadersAdapter implements MultiValueMap<String, String> {
	/**
	 * Http头部信息
	 */
	private final HttpHeaders headers;


	NettyHeadersAdapter(HttpHeaders headers) {
		this.headers = headers;
	}


	@Override
	@Nullable
	public String getFirst(String key) {
		return this.headers.get(key);
	}

	@Override
	public void add(String key, @Nullable String value) {
		if (value != null) {
			// 如果值不为空，则添加键值到 Http头部信息 中
			this.headers.add(key, value);
		}
	}

	@Override
	public void addAll(String key, List<? extends String> values) {
		this.headers.add(key, values);
	}

	@Override
	public void addAll(MultiValueMap<String, String> values) {
		values.forEach(this.headers::add);
	}

	@Override
	public void set(String key, @Nullable String value) {
		if (value != null) {
			// 如果值不为空，则将键值设置到 Http头部信息 中
			this.headers.set(key, value);
		}
	}

	@Override
	public void setAll(Map<String, String> values) {
		values.forEach(this.headers::set);
	}

	@Override
	public Map<String, String> toSingleValueMap() {
		// 创建一个 单值映射
		Map<String, String> singleValueMap = CollectionUtils.newLinkedHashMap(this.headers.size());
		// 遍历当前 Http头部 的每一个条目
		this.headers.entries()
				.forEach(entry -> {
					// 如果 单值映射 中不包含条目的键
					if (!singleValueMap.containsKey(entry.getKey())) {
						// 将条目的键值对放入 单值映射 中
						singleValueMap.put(entry.getKey(), entry.getValue());
					}
				});
		// 返回处理后的 单值映射
		return singleValueMap;
	}

	@Override
	public int size() {
		return this.headers.names().size();
	}

	@Override
	public boolean isEmpty() {
		return this.headers.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return (key instanceof String && this.headers.contains((String) key));
	}

	@Override
	public boolean containsValue(Object value) {
		// 检查 值是否是 字符串 类型
		return (value instanceof String &&
				// 并且 Http头部信息 的条目流中是否有任何条目的值与给定值相等
				this.headers.entries().stream()
						.anyMatch(entry -> value.equals(entry.getValue())));
	}

	@Override
	@Nullable
	public List<String> get(Object key) {
		if (containsKey(key)) {
			// 如果含有该键，则获取该键所有的 头部值
			return this.headers.getAll((String) key);
		}
		return null;
	}

	@Nullable
	@Override
	public List<String> put(String key, @Nullable List<String> value) {
		// 获取指定键的所有旧值
		List<String> previousValues = this.headers.getAll(key);
		// 设置指定键的新值
		this.headers.set(key, value);
		// 返回旧的所有值
		return previousValues;
	}

	@Nullable
	@Override
	public List<String> remove(Object key) {
		// 如果键是 字符串 类型
		if (key instanceof String) {
			// 获取指定键的所有旧值
			List<String> previousValues = this.headers.getAll((String) key);
			// 移除指定键的所有条目
			this.headers.remove((String) key);
			// 返回旧的所有值
			return previousValues;
		}
		// 如果键不是 字符串 类型，返回 null
		return null;
	}

	@Override
	public void putAll(Map<? extends String, ? extends List<String>> map) {
		map.forEach(this.headers::set);
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
		return this.headers.names().stream()
				.map(this.headers::getAll).collect(Collectors.toList());
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
		 * 名称迭代器
		 */
		private Iterator<String> names = headers.names().iterator();

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
			return headers.getAll(this.key);
		}

		@Override
		public List<String> setValue(List<String> value) {
			// 获取指定键的所有旧值
			List<String> previousValues = headers.getAll(this.key);
			// 设置指定键的新值
			headers.set(this.key, value);
			// 返回旧的所有值
			return previousValues;
		}
	}


	private class HeaderNames extends AbstractSet<String> {

		@Override
		public Iterator<String> iterator() {
			return new HeaderNamesIterator(headers.names().iterator());
		}

		@Override
		public int size() {
			return headers.names().size();
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
			this.currentName = this.iterator.next();
			return this.currentName;
		}

		@Override
		public void remove() {
			// 如果当前名称为空
			if (this.currentName == null) {
				// 抛出 IllegalStateException 异常
				throw new IllegalStateException("No current Header in iterator");
			}

			// 如果 头部信息 不包含当前名称的头部
			if (!headers.contains(this.currentName)) {
				// 抛出 IllegalStateException 异常
				throw new IllegalStateException("Header not present: " + this.currentName);
			}

			// 移除当前名称对应的头部
			headers.remove(this.currentName);
		}
	}

}
