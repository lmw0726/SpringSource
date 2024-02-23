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

package org.springframework.util;

import org.springframework.lang.Nullable;

/**
 * 解析字符串值的简单策略接口。
 * 被 {@link org.springframework.beans.factory.config.ConfigurableBeanFactory} 使用。
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see org.springframework.beans.factory.config.ConfigurableBeanFactory#resolveAliases
 * @see org.springframework.beans.factory.config.BeanDefinitionVisitor#BeanDefinitionVisitor(StringValueResolver)
 * @see org.springframework.beans.factory.config.PropertyPlaceholderConfigurer
 */
@FunctionalInterface
public interface StringValueResolver {

	/**
	 * 解析给定的字符串值，例如解析占位符。
	 *
	 * @param strVal 原始字符串值（永远不为 {@code null}）
	 * @return 已解析的字符串值（解析为 {@code null} 值时可能为 {@code null}），可能是原始字符串值本身
	 * （如果没有要解析的占位符或者在忽略无法解析的占位符时）
	 * @throws IllegalArgumentException 如果无法解析字符串值
	 */
	@Nullable
	String resolveStringValue(String strVal);

}
