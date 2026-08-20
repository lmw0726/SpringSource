/*
 * Copyright 2002-2013 the original author or authors.
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

import java.text.ParseException;
import java.util.Locale;

import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormatter;

import org.springframework.format.Parser;

/**
 * 使用 {@link org.joda.time.format.DateTimeFormatter} 解析 Joda {@link org.joda.time.LocalTime} 实例。
 *
 * @author Juergen Hoeller
 * @since 4.0
 * @deprecated 从 5.3 版本起弃用，建议使用标准 JSR-310 支持
 */
@Deprecated
public final class LocalTimeParser implements Parser<LocalTime> {

	private final DateTimeFormatter formatter;


	/**
	 * 创建一个新的 DateTimeParser。
	 * @param formatter Joda DateTimeFormatter 实例
	 */
	public LocalTimeParser(DateTimeFormatter formatter) {
		this.formatter = formatter;
	}


	@Override
	public LocalTime parse(String text, Locale locale) throws ParseException {
		return JodaTimeContextHolder.getFormatter(this.formatter, locale).parseLocalTime(text);
	}

}
