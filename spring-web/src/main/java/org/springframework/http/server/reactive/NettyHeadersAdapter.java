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

import io.netty.handler.codec.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 用于包装Netty HTTP头的 {@code MultiValueMap} 实现。
 *
 * <p>客户端包中也存在此类的副本！
 *
 * @author Brian Clozel
 * @since 5.1.1
 */
class NettyHeadersAdapter implements MultiValueMap<String, String> {
	/**
	 * Http请求头
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
		// 如果值不为null
		if (value != null) {
			// 将键值对添加到 Http头部 中
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
		// 如果值不为null
		if (value != null) {
			// 将键值对设置到 Http头部 中
			this.headers.set(key, value);
		}
	}

	@Override
	public void setAll(Map<String, String> values) {
		values.forEach(this.headers::set);
	}

	@Override
	public Map<String, String> toSingleValueMap() {
		// 创建一个新的 LinkedHashMap 对象 单值映射
		Map<String, String> singleValueMap = CollectionUtils.newLinkedHashMap(this.headers.size());

		// 遍历 Http头部 中的每个条目
		this.headers.entries()
				.forEach(entry -> {
					// 如果 单值映射 中不包含当前条目的键
					if (!singleValueMap.containsKey(entry.getKey())) {
						// 则将该条目的键值对添加到 单值映射 中
						singleValueMap.put(entry.getKey(), entry.getValue());
					}
				});

		// 返回最终的 单值映射
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
		// 判断 值 是否为 String 类型
		return (value instanceof String &&
				// 获取 Http头部 中所有条目的流
				this.headers.entries().stream()
						// 判断流中是否存在与 值 相等的值
						.anyMatch(entry -> value.equals(entry.getValue())));
	}

	@Override
	@Nullable
	public List<String> get(Object key) {
		if (containsKey(key)) {
			// 如果含有该键，则获取该键对应的头部值列表
			return this.headers.getAll((String) key);
		}
		// 否则返回 null
		return null;
	}

	@Nullable
	@Override
	public List<String> put(String key, @Nullable List<String> value) {
		// 获取指定键 键 的所有旧值
		List<String> previousValues = this.headers.getAll(key);

		// 给 指定的键 设置新的值
		this.headers.set(key, value);

		// 返回所有旧值的列表
		return previousValues;
	}

	@Nullable
	@Override
	public List<String> remove(Object key) {
		// 如果 键 是 String 类型的实例
		if (key instanceof String) {
			// 获取指定键 键 的所有旧值，并存储在 以前的值列表 中
			List<String> previousValues = this.headers.getAll((String) key);

			// 移除指定键
			this.headers.remove((String) key);

			// 返回 以前的值 列表
			return previousValues;
		}

		// 如果 键 不是 String 类型的实例，则返回 null
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
			// 获取指定 键 的所有旧值，并存储在 以前的值列表 中
			List<String> previousValues = headers.getAll(this.key);

			// 给 指定的键 设置新的值
			headers.set(this.key, value);

			// 返回以前的值列表
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
			// 如果 当前名称 为 null
			if (this.currentName == null) {
				// 抛出 IllegalStateException 异常，指示迭代器中没有当前的头部
				throw new IllegalStateException("No current Header in iterator");
			}

			// 如果 Http头部 中不包含当前名称
			if (!headers.contains(this.currentName)) {
				// 抛出 IllegalStateException 异常，指示当前名称的头部不存在
				throw new IllegalStateException("Header not present: " + this.currentName);
			}

			// 移除 Http头部 中的当前名称
			headers.remove(this.currentName);
		}
	}

}
