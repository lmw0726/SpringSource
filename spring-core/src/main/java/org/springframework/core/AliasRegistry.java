/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.core;

/**
 * Common interface for managing aliases. Serves as a super-interface for
 * {@link org.springframework.beans.factory.support.BeanDefinitionRegistry}.
 *
 * @author Juergen Hoeller
 * @since 2.5.2
 */
public interface AliasRegistry {

	/**
	 * 给定一个名字，为它注册一个别名。
	 *
	 * @param name  规范名称
	 * @param alias 要注册的别名
	 * @throws IllegalStateException 如果别名已经在使用并且可能不会被覆盖
	 */
	void registerAlias(String name, String alias);

	/**
	 * 从此注册表中删除指定的别名。
	 *
	 * @param alias 要删除的别名
	 * @throws IllegalStateException 如果没有找到这样的别名
	 */
	void removeAlias(String alias);

	/**
	 * 确定给定的名称是否定义为别名 (与实际注册的组件的名称相反)。
	 *
	 * @param name 要检查的名称
	 * @return 给定的名称是否是别名
	 */
	boolean isAlias(String name);

	/**
	 * 返回给定名称的别名 (如果已定义)。
	 *
	 * @param name 要检查别名的名称
	 * @return 别名，如果没有，则为空数组
	 */
	String[] getAliases(String name);

}
