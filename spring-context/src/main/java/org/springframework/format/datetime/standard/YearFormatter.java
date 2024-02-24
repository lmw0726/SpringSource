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
import java.time.Year;
import java.util.Locale;

/**
 * {@link Formatter} 的实现，用于 JSR-310 {@link Year}，遵循 JSR-310 对 Year 的解析规则。
 *
 * @author Juergen Hoeller
 * @see Year#parse
 * @since 5.0.4
 */
class YearFormatter implements Formatter<Year> {

	@Override
	public Year parse(String text, Locale locale) throws ParseException {
		return Year.parse(text);
	}

	@Override
	public String print(Year object, Locale locale) {
		return object.toString();
	}

}
