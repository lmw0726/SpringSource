/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.format.datetime.joda;

import java.util.TimeZone;

import org.joda.time.Chronology;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;

import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.i18n.TimeZoneAwareLocaleContext;
import org.springframework.lang.Nullable;

/**
 * 一个持有用户特定 Joda-Time 设置的上下文，例如用户的
 * Chronology（日历系统）和时区。
 *
 * <p>属性值为 {@code null} 表示用户未指定任何设置。
 *
 * @author Keith Donald
 * @since 3.0
 * @see JodaTimeContextHolder
 * @deprecated 自 5.3 起已弃用，建议使用标准 JSR-310 支持
 */
@Deprecated
public class JodaTimeContext {


	@Nullable
	private Chronology chronology;

	@Nullable
	private DateTimeZone timeZone;


	/**
	 * 设置用户的 chronology（日历系统）。
	 */
	public void setChronology(@Nullable Chronology chronology) {
		this.chronology = chronology;
	}

	/**
	 * 返回用户的 chronology（日历系统），如果有的话。
	 */
	@Nullable
	public Chronology getChronology() {
		return this.chronology;
	}

	/**
	 * 设置用户的时区。
	 * <p>或者，也可以在 {@link LocaleContextHolder} 上设置
	 * {@link TimeZoneAwareLocaleContext}。如果此处未提供设置，
	 * 本上下文类将回退到检查 locale 上下文。
	 * @see org.springframework.context.i18n.LocaleContextHolder#getTimeZone()
	 * @see org.springframework.context.i18n.LocaleContextHolder#setLocaleContext
	 */
	public void setTimeZone(@Nullable DateTimeZone timeZone) {
		this.timeZone = timeZone;
	}

	/**
	 * 返回用户的时区，如果有的话。
	 */
	@Nullable
	public DateTimeZone getTimeZone() {
		return this.timeZone;
	}


	/**
	 * 获取将本上下文的设置应用到基础 {@code formatter} 上的 DateTimeFormatter。
	 * @param formatter 建立默认格式化规则的基础格式化器，通常是与上下文无关的
	 * @return 带有上下文信息的 DateTimeFormatter
	 */
	public DateTimeFormatter getFormatter(DateTimeFormatter formatter) {
		if (this.chronology != null) {
			formatter = formatter.withChronology(this.chronology);
		}
		if (this.timeZone != null) {
			formatter = formatter.withZone(this.timeZone);
		}
		else {
			LocaleContext localeContext = LocaleContextHolder.getLocaleContext();
			if (localeContext instanceof TimeZoneAwareLocaleContext) {
				TimeZone timeZone = ((TimeZoneAwareLocaleContext) localeContext).getTimeZone();
				if (timeZone != null) {
					formatter = formatter.withZone(DateTimeZone.forTimeZone(timeZone));
				}
			}
		}
		return formatter;
	}

}
