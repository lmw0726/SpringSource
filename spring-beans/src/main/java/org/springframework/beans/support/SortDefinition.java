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

package org.springframework.beans.support;

/**
 * 用于按属性对 bean 实例进行排序的定义。
 *
 * @author Juergen Hoeller
 * @since 26.05.2003
 */
public interface SortDefinition {

	/**
	 * 返回用于比较的 bean 属性名。
	 * 也可以是嵌套 bean 属性路径。
	 */
	String getProperty();

	/**
	 * 返回在 String 值比较中是否忽略大小写。
	 */
	boolean isIgnoreCase();

	/**
	 * 返回是否按升序（true）或降序（false）排序。
	 */
	boolean isAscending();

}
