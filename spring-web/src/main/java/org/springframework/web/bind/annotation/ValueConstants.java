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

package org.springframework.web.bind.annotation;

/**
 * 绑定注解之间共享的常见值常量。
 *
 * @since 3.0.1
 * @author Juergen Hoeller
 */
public interface ValueConstants {

	/**
	 * 定义无默认值的常量 - 用作注解属性中无法使用的 {@code null} 的替代品。
	 * <p>这是一个人工排列的 16 个 Unicode 字符，
	 * 其唯一目的是永远不匹配用户声明的值。
	 * @see RequestParam#defaultValue()
	 * @see RequestHeader#defaultValue()
	 * @see CookieValue#defaultValue()
	 */
	String DEFAULT_NONE = "\n\t\t\n\t\t\n\uE000\uE001\uE002\n\t\t\t\t\n";

}
