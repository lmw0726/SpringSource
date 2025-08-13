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

package org.springframework.core.style;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 工具类，用于构建美观的 {@code toString()} 方法，
 * 并支持可插拔的样式约定。默认情况下，ToStringCreator 遵循
 * Spring 的 {@code toString()} 样式规范。
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 1.2.2
 */
public class ToStringCreator {

	/**
	 * ToStringCreator 使用的默认 ToStringStyler 实例。
	 */
	private static final ToStringStyler DEFAULT_TO_STRING_STYLER =
			new DefaultToStringStyler(StylerUtils.DEFAULT_VALUE_STYLER);


	private final StringBuilder buffer = new StringBuilder(256);

	private final ToStringStyler styler;

	private final Object object;

	private boolean styledFirstField;


	/**
	 * 为给定对象创建一个 ToStringCreator 实例。
	 * @param obj 需要转成字符串的对象
	 */
	public ToStringCreator(Object obj) {
		this(obj, (ToStringStyler) null);
	}

	/**
	 * 为给定对象创建一个 ToStringCreator 实例，并使用提供的样式。
	 * @param obj 需要转成字符串的对象
	 * @param styler 封装了格式化打印指令的 ValueStyler
	 */
	public ToStringCreator(Object obj, @Nullable ValueStyler styler) {
		this(obj, new DefaultToStringStyler(styler != null ? styler : StylerUtils.DEFAULT_VALUE_STYLER));
	}

	/**
	 * 为给定对象创建一个 ToStringCreator 实例，并使用提供的样式。
	 * @param obj 需要转成字符串的对象
	 * @param styler 封装了格式化打印指令的 ToStringStyler
	 */
	public ToStringCreator(Object obj, @Nullable ToStringStyler styler) {
		Assert.notNull(obj, "The object to be styled must not be null");
		this.object = obj;
		this.styler = (styler != null ? styler : DEFAULT_TO_STRING_STYLER);
		this.styler.styleStart(this.buffer, this.object);
	}


	/**
	 * 追加一个 byte 类型字段值。
	 * @param fieldName 字段名，通常是成员变量名
	 * @param value 字段值
	 * @return 返回自身，支持链式调用
	 */
	public ToStringCreator append(String fieldName, byte value) {
		return append(fieldName, Byte.valueOf(value));
	}

	/**
	 * 追加一个 short 类型字段值。
	 * @param fieldName 字段名，通常是成员变量名
	 * @param value 字段值
	 * @return 返回自身，支持链式调用
	 */
	public ToStringCreator append(String fieldName, short value) {
		return append(fieldName, Short.valueOf(value));
	}

	/**
	 * 追加一个 int 类型字段值。
	 * @param fieldName 字段名，通常是成员变量名
	 * @param value 字段值
	 * @return 返回自身，支持链式调用
	 */
	public ToStringCreator append(String fieldName, int value) {
		return append(fieldName, Integer.valueOf(value));
	}

	/**
	 * 追加一个 long 类型字段值。
	 * @param fieldName 字段名，通常是成员变量名
	 * @param value 字段值
	 * @return 返回自身，支持链式调用
	 */
	public ToStringCreator append(String fieldName, long value) {
		return append(fieldName, Long.valueOf(value));
	}

	/**
	 * 追加一个 float 类型字段值。
	 * @param fieldName 字段名，通常是成员变量名
	 * @param value 字段值
	 * @return 返回自身，支持链式调用
	 */
	public ToStringCreator append(String fieldName, float value) {
		return append(fieldName, Float.valueOf(value));
	}

	/**
	 * 追加一个 double 类型字段值。
	 * @param fieldName 字段名，通常是成员变量名
	 * @param value 字段值
	 * @return 返回自身，支持链式调用
	 */
	public ToStringCreator append(String fieldName, double value) {
		return append(fieldName, Double.valueOf(value));
	}

	/**
	 * 追加一个 boolean 类型字段值。
	 * @param fieldName 字段名，通常是成员变量名
	 * @param value 字段值
	 * @return 返回自身，支持链式调用
	 */
	public ToStringCreator append(String fieldName, boolean value) {
		return append(fieldName, Boolean.valueOf(value));
	}

	/**
	 * 追加一个字段值。
	 * @param fieldName 字段名，通常是成员变量名
	 * @param value 字段值（可为 null）
	 * @return 返回自身，支持链式调用
	 */
	public ToStringCreator append(String fieldName, @Nullable Object value) {
		printFieldSeparatorIfNecessary();
		this.styler.styleField(this.buffer, fieldName, value);
		return this;
	}

	private void printFieldSeparatorIfNecessary() {
		if (this.styledFirstField) {
			this.styler.styleFieldSeparator(this.buffer);
		}
		else {
			this.styledFirstField = true;
		}
	}

	/**
	 * 追加提供的值。
	 * @param value 需要追加的值
	 * @return 返回自身，支持链式调用
	 */
	public ToStringCreator append(Object value) {
		this.styler.styleValue(this.buffer, value);
		return this;
	}


	/**
	 * 返回 ToStringCreator 构建的字符串表示。
	 */
	@Override
	public String toString() {
		this.styler.styleEnd(this.buffer, this.object);
		return this.buffer.toString();
	}

}
