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

import org.springframework.util.StringUtils;

import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;

/**
 * 内部的 {@link DateTimeFormatter} 实用程序。
 *
 * @author Juergen Hoeller
 * @since 5.3.5
 */
abstract class DateTimeFormatterUtils {

	/**
	 * 创建严格的 {@link DateTimeFormatter}。
	 * <p>
	 * 使用严格的解析以与 Joda-Time 和标准 DateFormat 行为一致：
	 * 否则，像对于非闰年的二月29日这样的溢出将不会被拒绝。
	 * 但是，使用严格的解析，年份数字需要指定为 'u'...
	 *
	 * @param pattern 要使用的模式
	 * @return 严格的 {@link DateTimeFormatter}
	 */
	static DateTimeFormatter createStrictDateTimeFormatter(String pattern) {
		// 将模式中的 "yy" 替换为 "uu"，以适应 Java 8 的新日期时间格式
		String patternToUse = StringUtils.replace(pattern, "yy", "uu");
		// 使用给定的模式创建并返回一个 DateTimeFormatter 对象，同时设置解析样式为 STRICT
		return DateTimeFormatter.ofPattern(patternToUse).withResolverStyle(ResolverStyle.STRICT);
	}

}
