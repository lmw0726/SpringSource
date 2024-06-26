/*
 * Copyright 2002-2015 the original author or authors.
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

import org.joda.time.Period;
import org.springframework.format.Formatter;

import java.text.ParseException;
import java.util.Locale;

/**
 * Joda-Time {@link Period} 的 {@link Formatter} 实现，遵循 Joda-Time 对 Period 的解析规则。
 *
 * 该格式化器将 Period 解析为字符串，并将字符串解析为 Period 对象。
 *
 * @author Juergen Hoeller
 * @since 4.2.4
 * @see Period#parse
 */
class PeriodFormatter implements Formatter<Period> {

	@Override
	public Period parse(String text, Locale locale) throws ParseException {
		return Period.parse(text);
	}

	@Override
	public String print(Period object, Locale locale) {
		return object.toString();
	}

}
