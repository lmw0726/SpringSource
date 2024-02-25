/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.format.number;

import org.springframework.format.Formatter;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Locale;

/**
 * 数字的抽象格式化器，
 * 提供了一个 {@link #getNumberFormat(java.util.Locale)} 模板方法。
 *
 * @author Juergen Hoeller
 * @author Keith Donald
 * @since 3.0
 */
public abstract class AbstractNumberFormatter implements Formatter<Number> {
	/**
	 * 是否进行宽松解析，默认不进行宽松解析
	 */
	private boolean lenient = false;


	/**
	 * 指定是否进行宽松解析。默认为false。
	 * <p>在宽松解析模式下，解析器可能允许不完全匹配格式的输入。
	 * 在严格解析模式下，输入必须与格式完全匹配。
	 *
	 * @param lenient 是否进行宽松解析
	 */
	public void setLenient(boolean lenient) {
		this.lenient = lenient;
	}


	@Override
	public String print(Number number, Locale locale) {
		return getNumberFormat(locale).format(number);
	}

	@Override
	public Number parse(String text, Locale locale) throws ParseException {
		// 获取指定 locale 的 NumberFormat 对象
		NumberFormat format = getNumberFormat(locale);

		// 创建 ParsePosition 对象，初始偏移量为 0
		ParsePosition position = new ParsePosition(0);

		// 解析文本为 Number 对象
		Number number = format.parse(text, position);

		// 如果解析出错，则抛出 ParseException
		if (position.getErrorIndex() != -1) {
			throw new ParseException(text, position.getIndex());
		}

		// 如果不是宽松模式
		if (!this.lenient) {
			// 如果文本长度与解析结束的位置不匹配，表示未完全解析完整个文本
			if (text.length() != position.getIndex()) {
				// 抛出 ParseException，指示未完全解析的部分
				throw new ParseException(text, position.getIndex());
			}
		}

		// 返回解析得到的 Number 对象
		return number;
	}

	/**
	 * 获取指定区域设置的具体NumberFormat。
	 *
	 * @param locale 当前区域设置
	 * @return NumberFormat实例（永远不会为null）
	 */
	protected abstract NumberFormat getNumberFormat(Locale locale);

}
