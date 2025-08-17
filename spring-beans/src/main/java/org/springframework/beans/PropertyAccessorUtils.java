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

package org.springframework.beans;

import org.springframework.lang.Nullable;

/**
 * 根据 {@link PropertyAccessor} 接口执行Bean属性访问的类的实用方法。
 *
 * @author Juergen Hoeller
 * @since 1.2.6
 */
public abstract class PropertyAccessorUtils {

	/**
	 * 返回给定属性路径的实际属性名称。
	 *
	 * @param propertyPath 要确定属性名称的属性路径
	 *                     （可以包含属性键，例如用于指定映射条目）
	 * @return 实际的属性名称，不包含任何键元素
	 */
	public static String getPropertyName(String propertyPath) {
		int separatorIndex = (propertyPath.endsWith(PropertyAccessor.PROPERTY_KEY_SUFFIX) ?
				propertyPath.indexOf(PropertyAccessor.PROPERTY_KEY_PREFIX_CHAR) : -1);
		return (separatorIndex != -1 ? propertyPath.substring(0, separatorIndex) : propertyPath);
	}

	/**
	 * 检查给定的属性路径是否表示索引属性或嵌套属性。
	 *
	 * @param propertyPath 要检查的属性路径
	 * @return 路径是否表示索引属性或嵌套属性
	 */
	public static boolean isNestedOrIndexedProperty(@Nullable String propertyPath) {
		if (propertyPath == null) {
			return false;
		}
		for (int i = 0; i < propertyPath.length(); i++) {
			char ch = propertyPath.charAt(i);
			if (ch == PropertyAccessor.NESTED_PROPERTY_SEPARATOR_CHAR ||
					ch == PropertyAccessor.PROPERTY_KEY_PREFIX_CHAR) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 确定给定属性路径中的第一个嵌套属性分隔符，忽略键中的点（如"map[my.key]"）。
	 *
	 * @param propertyPath 要检查的属性路径
	 * @return 嵌套属性分隔符的索引，如果没有则返回-1
	 */
	public static int getFirstNestedPropertySeparatorIndex(String propertyPath) {
		return getNestedPropertySeparatorIndex(propertyPath, false);
	}

	/**
	 * 确定给定属性路径中第一个嵌套属性分隔符，忽略键中的点（如 "map[my.key]"）。
	 *
	 * @param propertyPath 要检查的属性路径
	 * @return 嵌套属性分隔符的索引，如果没有则返回-1
	 */
	public static int getLastNestedPropertySeparatorIndex(String propertyPath) {
		return getNestedPropertySeparatorIndex(propertyPath, true);
	}

	/**
	 * 确定给定属性路径中的第一个（或最后一个）嵌套属性分隔符，忽略键中的点（如"map[my.key]"）。
	 *
	 * @param propertyPath 要检查的属性路径
	 * @param last         是否返回最后一个分隔符而不是第一个
	 * @return 嵌套属性分隔符的索引，如果没有则返回-1
	 */
	private static int getNestedPropertySeparatorIndex(String propertyPath, boolean last) {
		// 初始化 inKey 变量为 false，表示当前不在键的内部
		boolean inKey = false;
		// 获取 propertyPath 的长度
		int length = propertyPath.length();
		// 如果是 last，则从后向前遍历，否则从前向后遍历
		int i = (last ? length - 1 : 0);
		// 遍历 propertyPath
		while (last ? i >= 0 : i < length) {
			// 根据当前字符进行不同的处理
			switch (propertyPath.charAt(i)) {
				// 如果是属性键的前缀或后缀字符，则更新 inKey 变量的值
				case PropertyAccessor.PROPERTY_KEY_PREFIX_CHAR:
				case PropertyAccessor.PROPERTY_KEY_SUFFIX_CHAR:
					inKey = !inKey;
					break;
				// 如果是嵌套属性分隔符，且当前不在键的内部，则返回当前位置
				case PropertyAccessor.NESTED_PROPERTY_SEPARATOR_CHAR:
					if (!inKey) {
						return i;
					}
			}
			// 如果是 last，则递减索引 i，否则递增索引 i
			if (last) {
				i--;
			} else {
				i++;
			}
		}
		// 如果没有找到嵌套属性分隔符，则返回 -1
		return -1;
	}

	/**
	 * 确定给定的已注册路径是否匹配给定的属性路径，
	 * 指示属性本身或属性的索引元素。
	 *
	 * @param propertyPath   属性路径（通常不带索引）
	 * @param registeredPath 已注册路径（可能带有索引）
	 * @return 路径是否匹配
	 */
	public static boolean matchesProperty(String registeredPath, String propertyPath) {
		if (!registeredPath.startsWith(propertyPath)) {
			return false;
		}
		if (registeredPath.length() == propertyPath.length()) {
			return true;
		}
		if (registeredPath.charAt(propertyPath.length()) != PropertyAccessor.PROPERTY_KEY_PREFIX_CHAR) {
			return false;
		}
		return (registeredPath.indexOf(PropertyAccessor.PROPERTY_KEY_SUFFIX_CHAR, propertyPath.length() + 1) ==
				registeredPath.length() - 1);
	}

	/**
	 * 确定给定属性路径的规范名称。
	 * 移除映射键周围的引号：<br>
	 * {@code map['key']} &rarr; {@code map[key]}<br>
	 * {@code map["key"]} &rarr; {@code map[key]}
	 *
	 * @param propertyName Bean属性路径
	 * @return 属性路径的规范表示
	 */
	public static String canonicalPropertyName(@Nullable String propertyName) {
		if (propertyName == null) {
			return "";
		}

		StringBuilder sb = new StringBuilder(propertyName);
		int searchIndex = 0;
		while (searchIndex != -1) {
			int keyStart = sb.indexOf(PropertyAccessor.PROPERTY_KEY_PREFIX, searchIndex);
			searchIndex = -1;
			if (keyStart != -1) {
				int keyEnd = sb.indexOf(
						PropertyAccessor.PROPERTY_KEY_SUFFIX, keyStart + PropertyAccessor.PROPERTY_KEY_PREFIX.length());
				if (keyEnd != -1) {
					String key = sb.substring(keyStart + PropertyAccessor.PROPERTY_KEY_PREFIX.length(), keyEnd);
					if ((key.startsWith("'") && key.endsWith("'")) || (key.startsWith("\"") && key.endsWith("\""))) {
						sb.delete(keyStart + 1, keyStart + 2);
						sb.delete(keyEnd - 2, keyEnd - 1);
						keyEnd = keyEnd - 2;
					}
					searchIndex = keyEnd + PropertyAccessor.PROPERTY_KEY_SUFFIX.length();
				}
			}
		}
		return sb.toString();
	}

	/**
	 * 确定给定属性路径的规范名称。
	 *
	 * @param propertyNames Bean属性路径（数组形式）
	 * @return 属性路径的规范表示
	 * （相同大小的数组）
	 * @see #canonicalPropertyName(String)
	 */
	@Nullable
	public static String[] canonicalPropertyNames(@Nullable String[] propertyNames) {
		if (propertyNames == null) {
			return null;
		}
		String[] result = new String[propertyNames.length];
		for (int i = 0; i < propertyNames.length; i++) {
			result[i] = canonicalPropertyName(propertyNames[i]);
		}
		return result;
	}

}
