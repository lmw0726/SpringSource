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

package org.springframework.core.style;

import org.springframework.lang.Nullable;

/**
 * 用于格式化输出 {@code toString()} 方法的策略接口。
 * 封装了打印算法；具体的工作流程应由其他对象（如构建器）提供。
 *
 * @author Keith Donald
 * @since 1.2.2
 */
public interface ToStringStyler {

	/**
	 * 在字段样式化之前，对 {@code toString()} 输出的对象进行样式处理。
	 * @param buffer 要写入的字符串缓冲区
	 * @param obj 要样式化的对象
	 */
	void styleStart(StringBuilder buffer, Object obj);

	/**
	 * 在字段样式化之后，对 {@code toString()} 输出的对象进行样式处理。
	 * @param buffer 要写入的字符串缓冲区
	 * @param obj 要样式化的对象
	 */
	void styleEnd(StringBuilder buffer, Object obj);

	/**
	 * 对字段值进行样式处理。
	 * @param buffer 要写入的字符串缓冲区
	 * @param fieldName 字段名称
	 * @param value 字段值
	 */
	void styleField(StringBuilder buffer, String fieldName, @Nullable Object value);

	/**
	 * 对给定的值进行样式处理。
	 * @param buffer 要写入的字符串缓冲区
	 * @param value 字段值
	 */
	void styleValue(StringBuilder buffer, Object value);

	/**
	 * 对字段分隔符进行样式处理。
	 * @param buffer 要写入的字符串缓冲区
	 */
	void styleFieldSeparator(StringBuilder buffer);

}
