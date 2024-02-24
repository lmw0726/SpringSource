/*
 * Copyright 2002-2019 the original author or authors.
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

import org.springframework.format.Formatter;

import java.text.ParseException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * {@link Formatter} 的实现，用于JSR-310 {@link java.time.Instant}，遵循JSR-310的解析规则来解析 Instant
 * (即，不使用可配置的 {@link java.time.format.DateTimeFormatter})：接受默认的 {@code ISO_INSTANT} 格式，
 * 以及 {@code RFC_1123_DATE_TIME}（通常用于 HTTP 日期头值），截至 Spring 4.3。
 *
 * @author Juergen Hoeller
 * @author Andrei Nevedomskii
 * @see java.time.Instant#parse
 * @see java.time.format.DateTimeFormatter#ISO_INSTANT
 * @see java.time.format.DateTimeFormatter#RFC_1123_DATE_TIME
 * @since 4.0
 */
public class InstantFormatter implements Formatter<Instant> {

	@Override
	public Instant parse(String text, Locale locale) throws ParseException {
		// 如果文本长度大于零，并且第一个字符是字母
		if (text.length() > 0 && Character.isAlphabetic(text.charAt(0))) {
			// 假设文本是类似于 "Tue, 3 Jun 2008 11:05:30 GMT" 的 RFC-1123 格式
			return Instant.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(text));
		} else {
			// 如果不符合以上条件，假设文本是类似于 "2007-12-03T10:15:30.00Z" 的 UTC 时间格式
			return Instant.parse(text);
		}
	}

	@Override
	public String print(Instant object, Locale locale) {
		return object.toString();
	}

}
