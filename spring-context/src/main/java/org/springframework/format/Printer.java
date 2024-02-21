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

package org.springframework.format;

import java.util.Locale;

/**
 * 打印类型为T的对象以供显示。
 *
 * @author Keith Donald
 * @since 3.0
 * @param <T> 此Printer打印的对象类型
 */
@FunctionalInterface
public interface Printer<T> {

	/**
	 * 打印类型为T的对象以供显示。
	 * @param object 要打印的实例
	 * @param locale 当前用户语言环境
	 * @return 打印的文本字符串
	 */
	String print(T object, Locale locale);

}
