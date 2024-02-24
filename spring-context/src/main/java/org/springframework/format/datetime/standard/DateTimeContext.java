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

import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.i18n.TimeZoneAwareLocaleContext;
import org.springframework.lang.Nullable;

import java.time.ZoneId;
import java.time.chrono.Chronology;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

/**
 * 一个上下文，保存用户特定的 <code>java.time</code>（JSR-310）设置，例如用户的年表（日历系统）和时区。
 * <p>{@code null} 属性值表示用户未指定设置。
 *
 * @author Juergen Hoeller
 * @see DateTimeContextHolder
 * @since 4.0
 */
public class DateTimeContext {
	/**
	 * 用于保存用户特定的年表（日历系统）。
	 */
	@Nullable
	private Chronology chronology;
	/**
	 * 用于保存用户特定的时区。
	 */
	@Nullable
	private ZoneId timeZone;


	/**
	 * 设置用户的年表（日历系统）。
	 * @param chronology 用户的年表
	 */
	public void setChronology(@Nullable Chronology chronology) {
		this.chronology = chronology;
	}

	/**
	 * 返回用户的年表（日历系统），如果有的话。
	 * @return 用户的年表，如果没有则为 null
	 */
	@Nullable
	public Chronology getChronology() {
		return this.chronology;
	}

	/**
	 * 设置用户的时区。
	 * <p>或者，在 {@link LocaleContextHolder} 上设置 {@link TimeZoneAwareLocaleContext}。
	 * 如果未提供设置，则此上下文类将回退到检查语言环境上下文。
	 *
	 * @param timeZone 用户的时区
	 * @see org.springframework.context.i18n.LocaleContextHolder#getTimeZone()
	 * @see org.springframework.context.i18n.LocaleContextHolder#setLocaleContext
	 */
	public void setTimeZone(@Nullable ZoneId timeZone) {
		this.timeZone = timeZone;
	}

	/**
	 * 返回用户的时区，如果有的话。
	 *
	 * @return 用户的时区，如果没有则为 null
	 */
	@Nullable
	public ZoneId getTimeZone() {
		return this.timeZone;
	}


	/**
	 * 获取应用了此上下文设置的 DateTimeFormatter 到基础 {@code formatter}。
	 *
	 * @param formatter 基础格式化器，用于建立默认的格式化规则，通常是与上下文无关的
	 * @return 上下文相关的 DateTimeFormatter
	 */
	public DateTimeFormatter getFormatter(DateTimeFormatter formatter) {
		// 如果指定了年表，则将其应用于 DateTimeFormatter 对象
		if (this.chronology != null) {
			formatter = formatter.withChronology(this.chronology);
		}
		// 如果指定了时区，则将其应用于 DateTimeFormatter 对象
		if (this.timeZone != null) {
			formatter = formatter.withZone(this.timeZone);
		} else {
			// 否则，尝试从当前的 LocaleContext 中获取时区
			LocaleContext localeContext = LocaleContextHolder.getLocaleContext();
			// 如果当前的 LocaleContext 是 TimeZoneAwareLocaleContext 的实例
			if (localeContext instanceof TimeZoneAwareLocaleContext) {
				// 从 TimeZoneAwareLocaleContext 中获取时区信息
				TimeZone timeZone = ((TimeZoneAwareLocaleContext) localeContext).getTimeZone();
				// 如果时区不为空，则将其转换为 ZoneId 并应用于 DateTimeFormatter 对象
				if (timeZone != null) {
					formatter = formatter.withZone(timeZone.toZoneId());
				}
			}
		}
		// 返回应用了年表和时区的 DateTimeFormatter 对象
		return formatter;
	}

}
