/*
 * Copyright 2002-2007 the original author or authors.
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
 * 封装根据 Spring 约定对值进行字符串样式处理的策略接口。
 *
 * @author Keith Donald
 * @since 1.2.2
 */
public interface ValueStyler {

	/**
	 * 对给定的值进行样式处理，返回字符串表示。
	 * @param value 需要样式处理的对象值
	 * @return 样式化后的字符串
	 */
	String style(@Nullable Object value);

}
