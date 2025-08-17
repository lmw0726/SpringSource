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

package org.springframework.beans.propertyeditors;

import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.beans.PropertyEditorSupport;

/**
 * 用于字符串数组的自定义 {@link java.beans.PropertyEditor}。
 *
 * <p>字符串必须为 CSV 格式，并且可以自定义分隔符。
 * 默认情况下，结果中的值会去掉前后空白。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Dave Syer
 * @see org.springframework.util.StringUtils#delimitedListToStringArray
 * @see org.springframework.util.StringUtils#arrayToDelimitedString
 */
public class StringArrayPropertyEditor extends PropertyEditorSupport {

	/**
	 * 拆分字符串的默认分隔符：逗号（","）。
	 */
	public static final String DEFAULT_SEPARATOR = ",";


	private final String separator;

	@Nullable
	private final String charsToDelete;

	private final boolean emptyArrayAsNull;

	private final boolean trimValues;


	/**
	 * 为字符串数组创建一个新的 {@code StringArrayPropertyEditor}，使用默认分隔符（逗号）。
	 * <p>空文本（没有元素）将被转换为一个空数组。
	 */
	public StringArrayPropertyEditor() {
		this(DEFAULT_SEPARATOR, null, false);
	}

	/**
	 * 为字符串数组创建一个新的 {@code StringArrayPropertyEditor}，使用指定的分隔符。
	 * <p>空文本（没有元素）将被转换为一个空数组。
	 * @param separator 用于拆分 {@link String} 的分隔符
	 */
	public StringArrayPropertyEditor(String separator) {
		this(separator, null, false);
	}

	/**
	 * 使用指定分隔符创建一个新的 {@code StringArrayPropertyEditor}。
	 * @param separator 用于拆分 {@link String} 的分隔符
	 * @param emptyArrayAsNull 如果为 {@code true}，空字符串数组将被转换为 {@code null}
	 */
	public StringArrayPropertyEditor(String separator, boolean emptyArrayAsNull) {
		this(separator, null, emptyArrayAsNull);
	}

	/**
	 * 使用指定分隔符创建一个新的 {@code StringArrayPropertyEditor}。
	 * @param separator 用于拆分 {@link String} 的分隔符
	 * @param emptyArrayAsNull 如果为 {@code true}，空字符串数组将被转换为 {@code null}
	 * @param trimValues 如果为 {@code true}，解析后的数组值将去除空白（默认值为 true）
	 */
	public StringArrayPropertyEditor(String separator, boolean emptyArrayAsNull, boolean trimValues) {
		this(separator, null, emptyArrayAsNull, trimValues);
	}

	/**
	 * 使用指定分隔符创建一个新的 {@code StringArrayPropertyEditor}。
	 * @param separator 用于拆分 {@link String} 的分隔符
	 * @param charsToDelete 除了修剪输入字符串之外，还需要删除的字符集合。
	 *                      用于删除不需要的换行符，例如 "\r\n\f" 会删除字符串中的所有换行和回车符。
	 * @param emptyArrayAsNull 如果为 {@code true}，空字符串数组将被转换为 {@code null}
	 */
	public StringArrayPropertyEditor(String separator, @Nullable String charsToDelete, boolean emptyArrayAsNull) {
		this(separator, charsToDelete, emptyArrayAsNull, true);
	}

	/**
	 * 使用指定分隔符创建一个新的 {@code StringArrayPropertyEditor}。
	 * @param separator 用于拆分 {@link String} 的分隔符
	 * @param charsToDelete 除了修剪输入字符串之外，还需要删除的字符集合。
	 *                      用于删除不需要的换行符，例如 "\r\n\f" 会删除字符串中的所有换行和回车符。
	 * @param emptyArrayAsNull 如果为 {@code true}，空字符串数组将被转换为 {@code null}
	 * @param trimValues 如果为 {@code true}，解析后的数组值将去除空白（默认值为 true）
	 */
	public StringArrayPropertyEditor(
			String separator, @Nullable String charsToDelete, boolean emptyArrayAsNull, boolean trimValues) {

		this.separator = separator;
		this.charsToDelete = charsToDelete;
		this.emptyArrayAsNull = emptyArrayAsNull;
		this.trimValues = trimValues;
	}

	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		String[] array = StringUtils.delimitedListToStringArray(text, this.separator, this.charsToDelete);
		if (this.emptyArrayAsNull && array.length == 0) {
			setValue(null);
		}
		else {
			if (this.trimValues) {
				array = StringUtils.trimArrayElements(array);
			}
			setValue(array);
		}
	}

	@Override
	public String getAsText() {
		return StringUtils.arrayToDelimitedString(ObjectUtils.toObjectArray(getValue()), this.separator);
	}

}
