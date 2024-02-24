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

package org.springframework.format.support;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.format.Formatter;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.beans.PropertyEditor;
import java.beans.PropertyEditorSupport;

/**
 * 适配器，用于桥接 {@link Formatter} 和 {@link PropertyEditor}。
 *
 * @author Juergen Hoeller
 * @since 4.2
 */
public class FormatterPropertyEditorAdapter extends PropertyEditorSupport {
	/**
	 * 格式化器
	 */
	private final Formatter<Object> formatter;


	/**
	 * 为给定的 {@link Formatter} 创建一个新的 {@code FormatterPropertyEditorAdapter}。
	 *
	 * @param formatter 要包装的 {@link Formatter}
	 */
	@SuppressWarnings("unchecked")
	public FormatterPropertyEditorAdapter(Formatter<?> formatter) {
		Assert.notNull(formatter, "Formatter must not be null");
		this.formatter = (Formatter<Object>) formatter;
	}


	/**
	 * 确定 {@link Formatter} 声明的字段类型。
	 *
	 * @return 在包装的 {@link Formatter} 实现中声明的字段类型（永远不会为 {@code null}）
	 * @throws IllegalArgumentException 如果无法推断出 {@link Formatter} 声明的字段类型
	 */
	public Class<?> getFieldType() {
		return FormattingConversionService.getFieldType(this.formatter);
	}


	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		// 如果文本不为空
		if (StringUtils.hasText(text)) {
			try {
				// 使用当前语言环境解析文本，并设置值
				setValue(this.formatter.parse(text, LocaleContextHolder.getLocale()));
			} catch (IllegalArgumentException ex) {
				// 如果解析失败，则抛出 IllegalArgumentException 异常
				throw ex;
			} catch (Throwable ex) {
				// 如果发生其他异常，则抛出 IllegalArgumentException 异常，附带解析失败的文本信息
				throw new IllegalArgumentException("Parse attempt failed for value [" + text + "]", ex);
			}
		} else {
			// 如果文本为空，则设置值为 null
			setValue(null);
		}
	}

	@Override
	public String getAsText() {
		// 获取当前值
		Object value = getValue();
		// 如果值不为空，则使用当前语言环境将值格式化为字符串；否则返回空字符串
		return (value != null ? this.formatter.print(value, LocaleContextHolder.getLocale()) : "");
	}

}
