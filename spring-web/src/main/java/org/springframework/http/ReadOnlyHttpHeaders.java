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

package org.springframework.http;

import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.*;
import java.util.stream.Collectors;

/**
 * {@code HttpHeaders} 对象，只能进行读取操作，不能写入。
 *
 * @author Brian Clozel
 * @author Sam Brannen
 * @since 5.1.1
 */
class ReadOnlyHttpHeaders extends HttpHeaders {

	private static final long serialVersionUID = -8578554704772377436L;

	/**
	 * 缓存的内容类型。
	 */
	@Nullable
	private MediaType cachedContentType;

	/**
	 * 缓存的接受类型。
	 */
	@Nullable
	private List<MediaType> cachedAccept;

	/**
	 * 创建一个只读的 HttpHeaders 对象。
	 *
	 * @param headers 要读取的 HttpHeaders 对象
	 */
	ReadOnlyHttpHeaders(MultiValueMap<String, String> headers) {
		super(headers);
	}

	// 以下为重写的方法，实现只读操作，不允许写入

	@Override
	public MediaType getContentType() {
		// 如果已缓存的内容类型不为空
		if (this.cachedContentType != null) {
			// 返回已缓存的内容类型
			return this.cachedContentType;
		} else {
			// 否则，调用父类的 getContentType 方法获取内容类型
			MediaType contentType = super.getContentType();

			// 将获取到的内容类型缓存起来
			this.cachedContentType = contentType;

			// 返回获取到的内容类型
			return contentType;
		}
	}

	@Override
	public List<MediaType> getAccept() {
		// 如果已缓存的Accept列表不为空，则直接返回
		if (this.cachedAccept != null) {
			return this.cachedAccept;
		} else {
			// 否则，调用父类方法获取Accept列表
			List<MediaType> accept = super.getAccept();
			// 缓存获取到的Accept列表
			this.cachedAccept = accept;
			// 返回获取到的Accept列表
			return accept;
		}
	}

	@Override
	public void clearContentHeaders() {
		// 无操作
	}

	@Override
	public List<String> get(Object key) {
		List<String> values = this.headers.get(key);
		return (values != null ? Collections.unmodifiableList(values) : null);
	}

	@Override
	public void add(String headerName, @Nullable String headerValue) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void addAll(String key, List<? extends String> values) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void addAll(MultiValueMap<String, String> values) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void set(String headerName, @Nullable String headerValue) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setAll(Map<String, String> values) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Map<String, String> toSingleValueMap() {
		return Collections.unmodifiableMap(this.headers.toSingleValueMap());
	}

	@Override
	public Set<String> keySet() {
		return Collections.unmodifiableSet(this.headers.keySet());
	}

	@Override
	public List<String> put(String key, List<String> value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<String> remove(Object key) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void putAll(Map<? extends String, ? extends List<String>> map) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Collection<List<String>> values() {
		return Collections.unmodifiableCollection(this.headers.values());
	}

	@Override
	public Set<Entry<String, List<String>>> entrySet() {
		// 使用流操作将头部映射项转换为简单不可变条目，并收集到 LinkedHashSet 中以保持原始顺序
		return this.headers.entrySet().stream().map(SimpleImmutableEntry::new)
				.collect(Collectors.collectingAndThen(
						// 保留条目的原始顺序
						Collectors.toCollection(LinkedHashSet::new),
						// 将结果集合转换为不可修改的集合
						Collections::unmodifiableSet));
	}

}
