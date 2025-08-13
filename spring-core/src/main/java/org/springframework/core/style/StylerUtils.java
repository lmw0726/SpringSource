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

package org.springframework.core.style;

/**
 * 简单的工具类，便于方便地访问值的样式化逻辑，
 * 主要用于支持描述性的日志消息。
 *
 * <p>对于更复杂的需求，请直接使用 {@link ValueStyler} 抽象。
 * 该类只是简单地在底层使用了一个共享的 {@link DefaultValueStyler} 实例。
 *
 * @author Keith Donald
 * @since 1.2.2
 * @see ValueStyler
 * @see DefaultValueStyler
 */
public abstract class StylerUtils {

	/**
	 * {@code style} 方法使用的默认 ValueStyler 实例。
	 * 同时也供本包中的 {@link ToStringCreator} 类使用。
	 */
	static final ValueStyler DEFAULT_VALUE_STYLER = new DefaultValueStyler();

	/**
	 * 根据默认约定格式化指定的值。
	 * @param value 需要格式化的对象值
	 * @return 格式化后的字符串
	 * @see DefaultValueStyler
	 */
	public static String style(Object value) {
		return DEFAULT_VALUE_STYLER.style(value);
	}

}
