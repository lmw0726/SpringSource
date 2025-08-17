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

package org.springframework.beans.propertyeditors;

import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import java.beans.PropertyEditorSupport;

/**
 * {@link Character} 的编辑器，用于将 String 值转换为 Character 或 char 类型的属性。
 *
 * <p>注意 JDK 默认并不包含 {@link java.beans.PropertyEditor} 来处理 char 类型！
 * {@link org.springframework.beans.BeanWrapperImpl} 会默认注册此编辑器。
 *
 * <p>也支持从 Unicode 字符序列转换，例如 {@code u0041} ('A')。
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Rick Evans
 * @since 1.2
 * @see Character
 * @see org.springframework.beans.BeanWrapperImpl
 */
public class CharacterEditor extends PropertyEditorSupport {

	/**
	 * 标识字符串为 Unicode 字符序列的前缀。
	 */
	private static final String UNICODE_PREFIX = "\\u";

	/**
	 * Unicode 字符序列的长度。
	 */
	private static final int UNICODE_LENGTH = 6;

	/**
	 * 是否允许空字符串。
	 */
	private final boolean allowEmpty;


	/**
	 * 创建一个新的 CharacterEditor 实例。
	 * <p>"allowEmpty" 参数控制在解析时是否允许空字符串，即当调用 {@link #setAsText(String)} 转换文本时是否解释为 {@code null}。
	 * 如果为 {@code false}，遇到空字符串时会抛出 {@link IllegalArgumentException}。
	 * @param allowEmpty 是否允许空字符串
	 */
	public CharacterEditor(boolean allowEmpty) {
		this.allowEmpty = allowEmpty;
	}


	@Override
	public void setAsText(@Nullable String text) throws IllegalArgumentException {
		if (this.allowEmpty && !StringUtils.hasLength(text)) {
			// 将空字符串视为null值。
			setValue(null);
		}
		else if (text == null) {
			throw new IllegalArgumentException("null String cannot be converted to char type");
		}
		else if (isUnicodeCharacterSequence(text)) {
			setAsUnicode(text);
		}
		else if (text.length() == 1) {
			setValue(text.charAt(0));
		}
		else {
			throw new IllegalArgumentException("String [" + text + "] with length " +
					text.length() + " cannot be converted to char type: neither Unicode nor single character");
		}
	}

	@Override
	public String getAsText() {
		Object value = getValue();
		return (value != null ? value.toString() : "");
	}


	private boolean isUnicodeCharacterSequence(String sequence) {
		return (sequence.startsWith(UNICODE_PREFIX) && sequence.length() == UNICODE_LENGTH);
	}

	private void setAsUnicode(String text) {
		int code = Integer.parseInt(text.substring(UNICODE_PREFIX.length()), 16);
		setValue((char) code);
	}

}
