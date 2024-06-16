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

import org.apache.tomcat.util.buf.MessageBytes;
import org.apache.tomcat.util.http.MimeHeaders;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;

import java.util.*;
import java.util.stream.Collectors;

/**
 * {@code MultiValueMap} 的实现，用于包装 Tomcat HTTP 头信息。
 *
 * @author Brian Clozel
 * @since 5.1.1
 */
class TomcatHeadersAdapter implements MultiValueMap<String, String> {
	/**
	 * Mime 头部信息
	 */
	private final MimeHeaders headers;


	TomcatHeadersAdapter(MimeHeaders headers) {
		this.headers = headers;
	}


	@Override
	public String getFirst(String key) {
		return this.headers.getHeader(key);
	}

	@Override
	public void add(String key, @Nullable String value) {
		this.headers.addValue(key).setString(value);
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
		this.headers.setValue(key).setString(value);
	}

	@Override
	public void setAll(Map<String, String> values) {
		values.forEach(this::set);
	}

	@Override
	public Map<String, String> toSingleValueMap() {
		// 创建一个 单值映射 对象
		Map<String, String> singleValueMap = CollectionUtils.newLinkedHashMap(this.headers.size());

		// 遍历 当前对象 中的所有键，并将每个键的第一个值放入 单值映射 中
		this.keySet().forEach(key -> singleValueMap.put(key, getFirst(key)));

		// 返回最终的 单值映射
		return singleValueMap;
	}

	@Override
	public int size() {
		// 获取 头部信息 对象中所有头部名称的枚举
		Enumeration<String> names = this.headers.names();

		int size = 0;

		// 遍历枚举中的每个元素（头部名称）
		while (names.hasMoreElements()) {
			// 每遍历一个元素，头部数量加1
			size++;
			// 移动到枚举中的下一个元素（头部名称）
			names.nextElement();
		}

		// 返回统计得到的头部数量
		return size;
	}

	@Override
	public boolean isEmpty() {
		return (this.headers.size() == 0);
	}

	@Override
	public boolean containsKey(Object key) {
		// 如果 键 是 字符串 类型的实例
		if (key instanceof String) {
			// 查找具有 指定键 的头部在列表中的索引位置
			return (this.headers.findHeader((String) key, 0) != -1);
		}

		// 如果 键 不是 字符串 类型的实例，则返回 false
		return false;
	}

	@Override
	public boolean containsValue(Object value) {
		// 检查 值 是否是 字符串 类型
		if (value instanceof String) {
			// 创建一个新的 MessageBytes 对象
			MessageBytes needle = MessageBytes.newInstance();
			// 设置 MessageBytes 对象的字符串值为 当前值
			needle.setString((String) value);

			// 遍历头部的所有值
			for (int i = 0; i < this.headers.size(); i++) {
				// 如果头部的值等于 所需的MessageBytes对象，则返回 true
				if (this.headers.getValue(i).equals(needle)) {
					return true;
				}
			}
		}

		// 如果 值 不是 字符串 类型，或者没有找到匹配的值，则返回 false
		return false;
	}

	@Override
	@Nullable
	public List<String> get(Object key) {
		// 检查是否包含特定键
		if (containsKey(key)) {
			// 如果包含，则返回该键对应的所有值的列表
			return Collections.list(this.headers.values((String) key));
		}

		// 如果不包含特定键，则返回 null
		return null;
	}

	@Override
	@Nullable
	public List<String> put(String key, List<String> value) {
		// 获取特定键的所有旧值列表
		List<String> previousValues = get(key);

		// 移除该键对应的所有头部
		this.headers.removeHeader(key);

		// 将新的值列表中的每个元素添加到该键对应的头部中
		value.forEach(v -> this.headers.addValue(key).setString(v));

		// 返回之前该键对应的所有旧值列表
		return previousValues;
	}

	@Override
	@Nullable
	public List<String> remove(Object key) {
		// 检查给定的键是否是字符串类型
		if (key instanceof String) {
			// 获取该键对应的所有值
			List<String> previousValues = get(key);

			// 移除该键对应的所有头部
			this.headers.removeHeader((String) key);

			// 返回该键对应的所有旧值的列表
			return previousValues;
		}

		// 如果键不是字符串类型，则返回 null
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
		return keySet().stream().map(this::get).collect(Collectors.toList());
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
		private Enumeration<String> names = headers.names();

		@Override
		public boolean hasNext() {
			return this.names.hasMoreElements();
		}

		@Override
		public Entry<String, List<String>> next() {
			return new HeaderEntry(this.names.nextElement());
		}
	}


	private final class HeaderEntry implements Entry<String, List<String>> {
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

		@Nullable
		@Override
		public List<String> getValue() {
			return get(this.key);
		}

		@Nullable
		@Override
		public List<String> setValue(List<String> value) {
			// 获取当前头部中特定键的所有值
			List<String> previous = getValue();

			// 移除当前头部中特定键对应的所有头部
			headers.removeHeader(this.key);

			// 添加新的值到当前头部中特定键
			addAll(this.key, value);

			// 返回之前特定键对应的所有旧值的列表
			return previous;
		}
	}


	private class HeaderNames extends AbstractSet<String> {

		@Override
		public Iterator<String> iterator() {
			return new HeaderNamesIterator(headers.names());
		}

		@Override
		public int size() {
			// 获取头部名称的枚举器
			Enumeration<String> names = headers.names();

			// 初始化名称数量
			int size = 0;

			// 遍历枚举器中的每个元素（即每个头部名称）
			while (names.hasMoreElements()) {
				// 获取下一个元素（头部名称）
				names.nextElement();
				// 增加名称数量计数
				size++;
			}

			// 返回统计的名称数量
			return size;
		}
	}

	private final class HeaderNamesIterator implements Iterator<String> {

		/**
		 * 枚举字符串
		 */
		private final Enumeration<String> enumeration;

		/**
		 * 当前名称
		 */
		@Nullable
		private String currentName;

		private HeaderNamesIterator(Enumeration<String> enumeration) {
			this.enumeration = enumeration;
		}

		@Override
		public boolean hasNext() {
			return this.enumeration.hasMoreElements();
		}

		@Override
		public String next() {
			// 获取下一个元素作为当前名称
			this.currentName = this.enumeration.nextElement();
			// 返回当前名称
			return this.currentName;
		}

		@Override
		public void remove() {
			if (this.currentName == null) {
				// 如果当前名称为空，则抛出异常
				throw new IllegalStateException("No current Header in iterator");
			}
			// 寻找当前名称在头部信息的索引
			int index = headers.findHeader(this.currentName, 0);
			if (index == -1) {
				// 如果请求头不包含当前名称，则抛出异常
				throw new IllegalStateException("Header not present: " + this.currentName);
			}
			// 从头部信息中移除当前名称
			headers.removeHeader(index);
		}
	}

}
