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

package org.springframework.format.datetime.standard;

import org.springframework.core.NamedThreadLocal;
import org.springframework.lang.Nullable;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * 用于线程本地用户 {@link DateTimeContext} 的持有者。
 *
 * @author Juergen Hoeller
 * @see org.springframework.context.i18n.LocaleContextHolder
 * @since 4.0
 */
public final class DateTimeContextHolder {
	/**
	 * 日期时间上下文持有者
	 */
	private static final ThreadLocal<DateTimeContext> dateTimeContextHolder =
			new NamedThreadLocal<>("DateTimeContext");


	private DateTimeContextHolder() {
	}


	/**
	 * 重置当前线程的 DateTimeContext。
	 */
	public static void resetDateTimeContext() {
		dateTimeContextHolder.remove();
	}

	/**
	 * 将给定的 DateTimeContext 关联到当前线程。
	 *
	 * @param dateTimeContext 当前的 DateTimeContext，
	 *                        或 {@code null} 以重置线程绑定的上下文
	 */
	public static void setDateTimeContext(@Nullable DateTimeContext dateTimeContext) {
		// 如果 dateTimeContext 为 null，则重置 dateTimeContext
		if (dateTimeContext == null) {
			resetDateTimeContext();
		} else {
			// 否则设置 dateTimeContext 到 dateTimeContextHolder
			dateTimeContextHolder.set(dateTimeContext);
		}
	}

	/**
	 * 返回当前线程关联的 DateTimeContext（如果有）。
	 *
	 * @return 当前的 DateTimeContext，如果没有则返回 {@code null}
	 */
	@Nullable
	public static DateTimeContext getDateTimeContext() {
		return dateTimeContextHolder.get();
	}

	/**
	 * 获取应用于给定基础格式化程序的用户特定设置的 DateTimeFormatter。
	 *
	 * @param formatter 基础格式化程序，用于建立默认格式规则（通常是用户独立的）
	 * @param locale    当前用户的区域设置（如果不可用，则可能为 {@code null}）
	 * @return 用户特定的 DateTimeFormatter
	 */
	public static DateTimeFormatter getFormatter(DateTimeFormatter formatter, @Nullable Locale locale) {
		// 如果 locale 不为 null，则使用 formatter.withLocale(locale)，否则使用原始的 formatter
		DateTimeFormatter formatterToUse = (locale != null ? formatter.withLocale(locale) : formatter);
		// 获取 DateTimeContext
		DateTimeContext context = getDateTimeContext();
		// 如果 context 不为 null，则尝试从 context 中获取 formatterToUse，否则直接返回 formatterToUse
		return (context != null ? context.getFormatter(formatterToUse) : formatterToUse);
	}

}
