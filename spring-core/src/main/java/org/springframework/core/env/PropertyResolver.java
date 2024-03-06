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

package org.springframework.core.env;

import org.springframework.lang.Nullable;

/**
 * 用于根据任何底层源解析属性的接口。
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @see Environment
 * @see PropertySourcesPropertyResolver
 * @since 3.1
 */
public interface PropertyResolver {

	/**
	 * 返回给定属性键是否可供解析，即给定键的值是否不为 {@code null}。
	 *
	 * @param key 要检查的属性键
	 * @return 如果属性键可供解析，则为 {@code true}；否则为 {@code false}
	 */
	boolean containsProperty(String key);

	/**
	 * 返回与给定键关联的属性值，如果无法解析该键，则返回 {@code null}。
	 *
	 * @param key 要解析的属性名称
	 * @see #getProperty(String, String)
	 * @see #getProperty(String, Class)
	 * @see #getRequiredProperty(String)
	 */
	@Nullable
	String getProperty(String key);

	/**
	 * 返回与给定键关联的属性值，如果无法解析键，则返回 {@code defaultValue}。
	 *
	 * @param key          要解析的属性名称
	 * @param defaultValue 如果找不到值，则返回的默认值
	 * @return 给定键的属性值，如果无法解析键，则为 {@code defaultValue}
	 * @see #getRequiredProperty(String)
	 * @see #getProperty(String, Class)
	 */
	String getProperty(String key, String defaultValue);

	/**
	 * 返回与给定键关联的属性值，如果无法解析键，则返回 {@code null}。
	 *
	 * @param key        要解析的属性名称
	 * @param targetType 属性值的期望类型
	 * @return 给定键的属性值，如果无法解析键，则为 {@code null}
	 * @see #getRequiredProperty(String, Class)
	 */
	@Nullable
	<T> T getProperty(String key, Class<T> targetType);

	/**
	 * 返回与给定键关联的属性值，如果无法解析键，则返回 {@code defaultValue}。
	 *
	 * @param key          要解析的属性名称
	 * @param targetType   属性值的期望类型
	 * @param defaultValue 如果找不到值，则返回的默认值
	 * @return 给定键的属性值，如果无法解析键，则为 {@code defaultValue}
	 * @see #getRequiredProperty(String, Class)
	 */
	<T> T getProperty(String key, Class<T> targetType, T defaultValue);

	/**
	 * 返回与给定键关联的属性值（永远不会为 {@code null}）。
	 *
	 * @param key 要解析的属性名称
	 * @return 给定键的属性值
	 * @throws IllegalStateException 如果无法解析给定键
	 * @see #getRequiredProperty(String, Class)
	 */
	String getRequiredProperty(String key) throws IllegalStateException;

	/**
	 * 返回与给定键关联的属性值，转换为给定的 targetType（永远不会为 {@code null}）。
	 *
	 * @param key        要解析的属性名称
	 * @param targetType 目标类型
	 * @return 给定键的属性值
	 * @throws IllegalStateException 如果无法解析给定键
	 */
	<T> T getRequiredProperty(String key, Class<T> targetType) throws IllegalStateException;

	/**
	 * 解析给定文本中的 ${...} 占位符，将其替换为通过 {@link #getProperty} 解析的相应属性值。
	 * 不能解析的占位符（没有默认值）将被忽略并保持不变。
	 *
	 * @param text 要解析的字符串
	 * @return 解析后的字符串（永远不会为 {@code null}）
	 * @throws IllegalArgumentException 如果给定的文本为 {@code null}
	 * @see #resolveRequiredPlaceholders
	 */
	String resolvePlaceholders(String text);

	/**
	 * 解析给定文本中的 ${...} 占位符，用 {@link #getProperty} 解析的相应属性值替换它们。
	 * 无默认值的不可解析的占位符将导致抛出IllegalArgumentException。
	 *
	 * @return 要解析的字符串 (从不为 {@code null})
	 * @throws IllegalArgumentException 如果给定的文本为 {@code null} 或任何占位符是无法解析的
	 */
	String resolveRequiredPlaceholders(String text) throws IllegalArgumentException;

}
