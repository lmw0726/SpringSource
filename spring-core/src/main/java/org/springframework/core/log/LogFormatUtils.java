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

package org.springframework.core.log;

import org.apache.commons.logging.Log;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;

import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * 用于格式化和日志消息的工具方法类。
 *
 * <p>主要供框架内部与 Apache Commons Logging 配合使用，
 * 通常通过 {@code spring-jcl} 桥接实现，但也兼容其他 Commons Logging 桥接实现。
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 5.1
 */
public abstract class LogFormatUtils {

	private static final Pattern NEWLINE_PATTERN = Pattern.compile("[\n\r]");

	private static final Pattern CONTROL_CHARACTER_PATTERN = Pattern.compile("\\p{Cc}");


	/**
	 * {@link #formatValue(Object, int, boolean)} 的便捷版本，
	 * 当 {@code limitLength} 为 true 时，限制日志消息长度为 100 字符，
	 * 并且替换换行和控制字符。
	 * @param value 要格式化的值
	 * @param limitLength 是否限制长度为 100
	 * @return 格式化后的字符串
	 */
	public static String formatValue(@Nullable Object value, boolean limitLength) {
		return formatValue(value, (limitLength ? 100 : -1), limitLength);
	}

	/**
	 * 通过 {@code toString()} 格式化给定值，
	 * 如果值是 {@link CharSequence}，则加引号包裹；
	 * 超过指定 {@code maxLength} 截断；
	 * 并在 {@code replaceNewlinesAndControlCharacters} 为 true 时，将换行和控制字符替换为占位符。
	 * @param value 要格式化的值
	 * @param maxLength 最大长度，超过则截断，-1 表示无限制
	 * @param replaceNewlinesAndControlCharacters 是否替换换行和控制字符
	 * @return 格式化后的字符串
	 */
	public static String formatValue(
			@Nullable Object value, int maxLength, boolean replaceNewlinesAndControlCharacters) {

		if (value == null) {
			return "";
		}
		String result;
		try {
			result = ObjectUtils.nullSafeToString(value);
		}
		catch (Throwable ex) {
			result = ObjectUtils.nullSafeToString(ex);
		}
		if (maxLength != -1) {
			result = (result.length() > maxLength ? result.substring(0, maxLength) + " (truncated)..." : result);
		}
		if (replaceNewlinesAndControlCharacters) {
			result = NEWLINE_PATTERN.matcher(result).replaceAll("<EOL>");
			result = CONTROL_CHARACTER_PATTERN.matcher(result).replaceAll("?");
		}
		if (value instanceof CharSequence) {
			result = "\"" + result + "\"";
		}
		return result;
	}

	/**
	 * 该方法用来在 TRACE 与 DEBUG 两种日志级别下，根据不同的细节级别（或不同消息内容）
	 * 记录日志。相当于以下写法的简化版本：
	 * <pre class="code">
	 * if (logger.isDebugEnabled()) {
	 *   String str = logger.isTraceEnabled() ? "..." : "...";
	 *   if (logger.isTraceEnabled()) {
	 *     logger.trace(str);
	 *   }
	 *   else {
	 *     logger.debug(str);
	 *   }
	 * }
	 * </pre>
	 * @param logger 用于记录日志的 Logger
	 * @param messageFactory 接收一个 boolean（值为 {@link Log#isTraceEnabled()}）并返回日志消息的函数
	 */
	public static void traceDebug(Log logger, Function<Boolean, String> messageFactory) {
		if (logger.isDebugEnabled()) {
			boolean traceEnabled = logger.isTraceEnabled();
			String logMessage = messageFactory.apply(traceEnabled);
			if (traceEnabled) {
				logger.trace(logMessage);
			}
			else {
				logger.debug(logMessage);
			}
		}
	}

}
