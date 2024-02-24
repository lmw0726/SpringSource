/*
 * Copyright 2002-2016 the original author or authors.
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

import org.springframework.format.Printer;

import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Locale;

/**
 * {@link Printer} 的实现，用于 JSR-310 {@link java.time.temporal.TemporalAccessor}，
 * 使用 {@link java.time.format.DateTimeFormatter}（如果可用）。
 *
 * @author Juergen Hoeller
 * @see DateTimeContextHolder#getFormatter
 * @see java.time.format.DateTimeFormatter#format(java.time.temporal.TemporalAccessor)
 * @since 4.0
 */
public final class TemporalAccessorPrinter implements Printer<TemporalAccessor> {
	/**
	 * 日期格式化器
	 */
	private final DateTimeFormatter formatter;


	/**
	 * 创建一个新的 TemporalAccessorPrinter。
	 *
	 * @param formatter 基础的 DateTimeFormatter 实例
	 */
	public TemporalAccessorPrinter(DateTimeFormatter formatter) {
		this.formatter = formatter;
	}


	@Override
	public String print(TemporalAccessor partial, Locale locale) {
		return DateTimeContextHolder.getFormatter(this.formatter, locale).format(partial);
	}

}
