/*
 * Copyright 2002-2018 the original author or authors.
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
import java.time.Month;
import java.util.Locale;

/**
 * {@link Formatter} 的实现，用于JSR-310 {@link Month}，将给定的字符串解析为 Month 枚举值（不区分大小写）。
 *
 * @author Juergen Hoeller
 * @since 5.0.4
 * @see Month#valueOf
 */
class MonthFormatter implements Formatter<Month> {

	@Override
	public Month parse(String text, Locale locale) throws ParseException {
		return Month.valueOf(text.toUpperCase());
	}

	@Override
	public String print(Month object, Locale locale) {
		return object.toString();
	}

}
