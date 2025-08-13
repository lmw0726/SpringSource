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

package org.springframework.core.codec;

import org.apache.commons.logging.Log;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.Map;

/**
 * 用于处理提示的常量和便捷方法。
 *
 * @author Rossen Stoyanchev
 * @since 5.1
 * @see ResourceRegionEncoder#BOUNDARY_STRING_HINT
 */
public abstract class Hints {

	/**
	 * 提示的名称，用于暴露一个前缀以关联日志消息。
	 */
	public static final String LOG_PREFIX_HINT = Log.class.getName() + ".PREFIX";

	/**
	 * 布尔类型提示的名称，用于指示是否应避免记录数据，可能是因为数据具有潜在敏感性，
	 * 或者因为它已被复合编码器（例如用于 multipart 请求的编码器）记录。
	 */
	public static final String SUPPRESS_LOGGING_HINT = Log.class.getName() + ".SUPPRESS_LOGGING";


	/**
	 * 通过 {@link Collections#singletonMap} 创建一个包含单个提示的 Map。
	 * @param hintName 提示名称
	 * @param value 提示值
	 * @return 创建的 Map
	 */
	public static Map<String, Object> from(String hintName, Object value) {
		return Collections.singletonMap(hintName, value);
	}

	/**
	 * 通过 {@link Collections#emptyMap()} 返回一个空的提示 Map。
	 * @return 空的 Map
	 */
	public static Map<String, Object> none() {
		return Collections.emptyMap();
	}

	/**
	 * 获取所需提示的值。
	 * @param hints 提示 Map
	 * @param hintName 所需提示的名称
	 * @param <T> 要强制转换的提示类型
	 * @return 提示值
	 * @throws IllegalArgumentException 如果未找到提示
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getRequiredHint(@Nullable Map<String, Object> hints, String hintName) {
		if (hints == null) {
			throw new IllegalArgumentException("No hints map for required hint '" + hintName + "'");
		}
		T hint = (T) hints.get(hintName);
		if (hint == null) {
			throw new IllegalArgumentException("Hints map must contain the hint '" + hintName + "'");
		}
		return hint;
	}

	/**
	 * 获取提示 {@link #LOG_PREFIX_HINT} 的值，如果存在则返回，否则返回空字符串。
	 * @param hints 传递给 encode 方法的提示
	 * @return 日志前缀
	 */
	public static String getLogPrefix(@Nullable Map<String, Object> hints) {
		return (hints != null ? (String) hints.getOrDefault(LOG_PREFIX_HINT, "") : "");
	}

	/**
	 * 是否根据提示 {@link #SUPPRESS_LOGGING_HINT} 抑制日志记录。
	 * @param hints 提示 Map
	 * @return 是否允许记录数据
	 */
	public static boolean isLoggingSuppressed(@Nullable Map<String, Object> hints) {
		return (hints != null && (boolean) hints.getOrDefault(SUPPRESS_LOGGING_HINT, false));
	}

	/**
	 * 合并两个提示 Map，如果两者都有值，则创建并复制到一个新的 Map 中；
	 * 如果其中一个非空，则返回非空的那个 Map；如果两者都为空，则返回一个空 Map。
	 * @param hints1 第一个提示 Map
	 * @param hints2 第二个提示 Map
	 * @return 包含来自两个 Map 的提示的单个 Map
	 */
	public static Map<String, Object> merge(Map<String, Object> hints1, Map<String, Object> hints2) {
		if (hints1.isEmpty() && hints2.isEmpty()) {
			return Collections.emptyMap();
		}
		else if (hints2.isEmpty()) {
			return hints1;
		}
		else if (hints1.isEmpty()) {
			return hints2;
		}
		else {
			Map<String, Object> result = CollectionUtils.newHashMap(hints1.size() + hints2.size());
			result.putAll(hints1);
			result.putAll(hints2);
			return result;
		}
	}

	/**
	 * 将单个提示合并到一个提示 Map 中，可能创建并复制所有提示到一个新的 Map，
	 * 或者如果提示 Map 为空，则创建一个新的单条目 Map。
	 * @param hints 要合并的提示 Map
	 * @param hintName 要合并的提示名称
	 * @param hintValue 要合并的提示值
	 * @return 包含所有提示的单个 Map
	 */
	public static Map<String, Object> merge(Map<String, Object> hints, String hintName, Object hintValue) {
		if (hints.isEmpty()) {
			return Collections.singletonMap(hintName, hintValue);
		}
		else {
			Map<String, Object> result = CollectionUtils.newHashMap(hints.size() + 1);
			result.putAll(hints);
			result.put(hintName, hintValue);
			return result;
		}
	}

	/**
	 * 如果提示包含 {@link #LOG_PREFIX_HINT} 并且给定日志器已启用 DEBUG 级别，
	 * 则通过 {@link DataBufferUtils#touch(DataBuffer, Object)} 将日志前缀作为提示应用于给定缓冲区。
	 * @param buffer 要处理的缓冲区
	 * @param hints 用于检查日志前缀的提示 Map
	 * @param logger 要检查其级别的日志器
	 * @since 5.3.2
	 */
	public static void touchDataBuffer(DataBuffer buffer, @Nullable Map<String, Object> hints, Log logger) {
		if (logger.isDebugEnabled() && hints != null) {
			Object logPrefix = hints.get(LOG_PREFIX_HINT);
			if (logPrefix != null) {
				DataBufferUtils.touch(buffer, logPrefix);
			}
		}
	}

}
