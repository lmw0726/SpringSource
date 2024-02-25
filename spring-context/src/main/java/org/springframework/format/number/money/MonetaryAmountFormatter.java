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

package org.springframework.format.number.money;

import org.springframework.format.Formatter;
import org.springframework.lang.Nullable;

import javax.money.MonetaryAmount;
import javax.money.format.MonetaryAmountFormat;
import javax.money.format.MonetaryFormats;
import java.util.Locale;

/**
 * 用于 JSR-354 {@link javax.money.MonetaryAmount} 值的格式化器，
 * 委托给 {@link javax.money.format.MonetaryAmountFormat#format} 和 {@link javax.money.format.MonetaryAmountFormat#parse}。
 * <p>
 * 该格式化器将 MonetaryAmount 解析为字符串，并将字符串解析为 MonetaryAmount 对象。
 *
 * @author Juergen Hoeller
 * @see #getMonetaryAmountFormat
 * @since 4.2
 */
public class MonetaryAmountFormatter implements Formatter<MonetaryAmount> {
	/**
	 * 格式名称
	 */
	@Nullable
	private String formatName;


	/**
	 * 创建一个基于区域设置的 MonetaryAmountFormatter。
	 */
	public MonetaryAmountFormatter() {
	}

	/**
	 * 根据给定的格式名称创建一个新的 MonetaryAmountFormatter。
	 *
	 * @param formatName 格式名称，在运行时由 JSR-354 提供程序解析
	 */
	public MonetaryAmountFormatter(String formatName) {
		this.formatName = formatName;
	}


	/**
	 * 指定格式名称，由运行时的 JSR-354 提供程序解析。
	 * <p>默认值为无，基于当前区域设置获取 {@link MonetaryAmountFormat}。
	 */
	public void setFormatName(String formatName) {
		this.formatName = formatName;
	}


	@Override
	public String print(MonetaryAmount object, Locale locale) {
		return getMonetaryAmountFormat(locale).format(object);
	}

	@Override
	public MonetaryAmount parse(String text, Locale locale) {
		return getMonetaryAmountFormat(locale).parse(text);
	}


	/**
	 * 获取给定区域设置的 MonetaryAmountFormat。
	 * <p>默认实现简单地调用 {@link javax.money.format.MonetaryFormats#getAmountFormat}
	 * 使用配置的格式名称或给定的区域设置。
	 *
	 * @param locale 当前区域设置
	 * @return MonetaryAmountFormat（永不为 {@code null}）
	 * @see #setFormatName
	 */
	protected MonetaryAmountFormat getMonetaryAmountFormat(Locale locale) {
		if (this.formatName != null) {
			return MonetaryFormats.getAmountFormat(this.formatName);
		} else {
			return MonetaryFormats.getAmountFormat(locale);
		}
	}

}
