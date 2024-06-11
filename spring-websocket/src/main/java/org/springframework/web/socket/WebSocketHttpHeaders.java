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

package org.springframework.web.socket;

import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * {@link org.springframework.http.HttpHeaders} 的变体，增加了对 WebSocket 规范 RFC 6455 定义的 HTTP 头的支持。
 * <p>
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class WebSocketHttpHeaders extends HttpHeaders {

	/**
	 * 表示 {@code Sec-WebSocket-Accept} 头。
	 */
	public static final String SEC_WEBSOCKET_ACCEPT = "Sec-WebSocket-Accept";

	/**
	 * 表示 {@code Sec-WebSocket-Extensions} 头。
	 */
	public static final String SEC_WEBSOCKET_EXTENSIONS = "Sec-WebSocket-Extensions";

	/**
	 * 表示 {@code Sec-WebSocket-Key} 头。
	 */
	public static final String SEC_WEBSOCKET_KEY = "Sec-WebSocket-Key";

	/**
	 * 表示 {@code Sec-WebSocket-Protocol} 头。
	 */
	public static final String SEC_WEBSOCKET_PROTOCOL = "Sec-WebSocket-Protocol";

	/**
	 * 表示 {@code Sec-WebSocket-Version} 头。
	 */
	public static final String SEC_WEBSOCKET_VERSION = "Sec-WebSocket-Version";

	/**
	 * 序列化版本UID。
	 */
	private static final long serialVersionUID = -6644521016187828916L;

	/**
	 * 包含头信息的 HttpHeaders 对象。
	 */
	private final HttpHeaders headers;


	/**
	 * 创建一个新实例。
	 */
	public WebSocketHttpHeaders() {
		this(new HttpHeaders());
	}

	/**
	 * 创建一个包装给定预先存在的 HttpHeaders 的实例，并且也将所有更改传播给它。
	 *
	 * @param headers 要包装的 HTTP 头
	 */
	public WebSocketHttpHeaders(HttpHeaders headers) {
		this.headers = headers;
	}

	/**
	 * 返回一个只读的 {@code WebSocketHttpHeaders} 对象。
	 *
	 * @deprecated 自 5.1.16 起，推荐调用 {@link #WebSocketHttpHeaders(HttpHeaders)}，使用 {@link HttpHeaders#readOnlyHttpHeaders(HttpHeaders)} 的只读包装器
	 */
	@Deprecated
	public static WebSocketHttpHeaders readOnlyWebSocketHttpHeaders(WebSocketHttpHeaders headers) {
		return new WebSocketHttpHeaders(HttpHeaders.readOnlyHttpHeaders(headers));
	}

	/**
	 * 设置 {@code Sec-WebSocket-Accept} 头的（新）值。
	 *
	 * @param secWebSocketAccept 头的值
	 */
	public void setSecWebSocketAccept(@Nullable String secWebSocketAccept) {
		set(SEC_WEBSOCKET_ACCEPT, secWebSocketAccept);
	}

	/**
	 * 返回 {@code Sec-WebSocket-Accept} 头的值。
	 *
	 * @return 头的值
	 */
	@Nullable
	public String getSecWebSocketAccept() {
		return getFirst(SEC_WEBSOCKET_ACCEPT);
	}

	/**
	 * 返回 {@code Sec-WebSocket-Extensions} 头的值。
	 *
	 * @return 头的值
	 */
	public List<WebSocketExtension> getSecWebSocketExtensions() {
		// 获取SEC_WEBSOCKET_EXTENSIONS头部的值列表
		List<String> values = get(SEC_WEBSOCKET_EXTENSIONS);
		// 如果值列表为空，则返回空列表
		if (CollectionUtils.isEmpty(values)) {
			return Collections.emptyList();
		} else {
			// 否则，解析每个值并添加到结果列表中
			List<WebSocketExtension> result = new ArrayList<>(values.size());
			for (String value : values) {
				result.addAll(WebSocketExtension.parseExtensions(value));
			}
			return result;
		}
	}

	/**
	 * 设置 {@code Sec-WebSocket-Extensions} 头的（新）值。
	 *
	 * @param extensions 头的值
	 */
	public void setSecWebSocketExtensions(List<WebSocketExtension> extensions) {
		// 创建一个具有指定初始容量的 ArrayList
		List<String> result = new ArrayList<>(extensions.size());

		// 遍历 WebSocketExtension 列表
		for (WebSocketExtension extension : extensions) {
			// 将每个 WebSocketExtension 对象转换为字符串并添加到 result 中
			result.add(extension.toString());
		}

		// 将 SEC_WEBSOCKET_EXTENSIONS 键设置为逗号分隔的字符串，其中包含 result 列表中的所有元素
		set(SEC_WEBSOCKET_EXTENSIONS, toCommaDelimitedString(result));
	}

	/**
	 * 设置 {@code Sec-WebSocket-Key} 头的（新）值。
	 *
	 * @param secWebSocketKey 头的值
	 */
	public void setSecWebSocketKey(@Nullable String secWebSocketKey) {
		set(SEC_WEBSOCKET_KEY, secWebSocketKey);
	}

	/**
	 * 返回 {@code Sec-WebSocket-Key} 头的值。
	 *
	 * @return 头的值
	 */
	@Nullable
	public String getSecWebSocketKey() {
		return getFirst(SEC_WEBSOCKET_KEY);
	}

	/**
	 * 设置 {@code Sec-WebSocket-Protocol} 头的（新）值。
	 *
	 * @param secWebSocketProtocol 头的值
	 */
	public void setSecWebSocketProtocol(String secWebSocketProtocol) {
		set(SEC_WEBSOCKET_PROTOCOL, secWebSocketProtocol);
	}

	/**
	 * 设置 {@code Sec-WebSocket-Protocol} 头的（新）值。
	 *
	 * @param secWebSocketProtocols 头的值
	 */
	public void setSecWebSocketProtocol(List<String> secWebSocketProtocols) {
		set(SEC_WEBSOCKET_PROTOCOL, toCommaDelimitedString(secWebSocketProtocols));
	}

	/**
	 * 返回 {@code Sec-WebSocket-Key} 头的值。
	 *
	 * @return 头的值
	 */
	public List<String> getSecWebSocketProtocol() {
		// 获取SEC_WEBSOCKET_PROTOCOL头部的值列表
		List<String> values = get(SEC_WEBSOCKET_PROTOCOL);
		// 如果值列表为空，则返回空列表
		if (CollectionUtils.isEmpty(values)) {
			return Collections.emptyList();
		} else if (values.size() == 1) {
			// 如果值列表大小为1，则返回值列表的单个值作为列表
			return getValuesAsList(SEC_WEBSOCKET_PROTOCOL);
		} else {
			// 否则，直接返回值列表
			return values;
		}
	}

	/**
	 * 设置 {@code Sec-WebSocket-Version} 头的（新）值。
	 *
	 * @param secWebSocketVersion 头的值
	 */
	public void setSecWebSocketVersion(@Nullable String secWebSocketVersion) {
		set(SEC_WEBSOCKET_VERSION, secWebSocketVersion);
	}

	/**
	 * 返回 {@code Sec-WebSocket-Version} 头的值。
	 *
	 * @return 头的值
	 */
	@Nullable
	public String getSecWebSocketVersion() {
		return getFirst(SEC_WEBSOCKET_VERSION);
	}

	// 单字符串方法

	/**
	 * 返回给定头名称的第一个头值（如果存在）。
	 *
	 * @param headerName 头名称
	 * @return 第一个头值；或者 {@code null}
	 */
	@Override
	@Nullable
	public String getFirst(String headerName) {
		return this.headers.getFirst(headerName);
	}

	/**
	 * 将给定的单个头值添加到给定名称下。
	 *
	 * @param headerName  头名称
	 * @param headerValue 头值
	 * @throws UnsupportedOperationException 如果不支持添加头
	 * @see #put(String, List)
	 * @see #set(String, String)
	 */
	@Override
	public void add(String headerName, @Nullable String headerValue) {
		this.headers.add(headerName, headerValue);
	}

	/**
	 * 在给定名称下设置给定的单个头值。
	 *
	 * @param headerName  头名称
	 * @param headerValue 头值
	 * @throws UnsupportedOperationException 如果不支持添加头
	 * @see #put(String, List)
	 * @see #add(String, String)
	 */
	@Override
	public void set(String headerName, @Nullable String headerValue) {
		this.headers.set(headerName, headerValue);
	}

	@Override
	public void setAll(Map<String, String> values) {
		this.headers.setAll(values);
	}

	@Override
	public Map<String, String> toSingleValueMap() {
		return this.headers.toSingleValueMap();
	}

	// Map实现

	@Override
	public int size() {
		return this.headers.size();
	}

	@Override
	public boolean isEmpty() {
		return this.headers.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return this.headers.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return this.headers.containsValue(value);
	}

	@Override
	public List<String> get(Object key) {
		return this.headers.get(key);
	}

	@Override
	public List<String> put(String key, List<String> value) {
		return this.headers.put(key, value);
	}

	@Override
	public List<String> remove(Object key) {
		return this.headers.remove(key);
	}

	@Override
	public void putAll(Map<? extends String, ? extends List<String>> m) {
		this.headers.putAll(m);
	}

	@Override
	public void clear() {
		this.headers.clear();
	}

	@Override
	public Set<String> keySet() {
		return this.headers.keySet();
	}

	@Override
	public Collection<List<String>> values() {
		return this.headers.values();
	}

	@Override
	public Set<Entry<String, List<String>>> entrySet() {
		return this.headers.entrySet();
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof WebSocketHttpHeaders)) {
			return false;
		}
		WebSocketHttpHeaders otherHeaders = (WebSocketHttpHeaders) other;
		return this.headers.equals(otherHeaders.headers);
	}

	@Override
	public int hashCode() {
		return this.headers.hashCode();
	}

	@Override
	public String toString() {
		return this.headers.toString();
	}

}
