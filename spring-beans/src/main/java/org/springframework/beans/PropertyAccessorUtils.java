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
 * Utility methods for classes that perform bean property access
 * according to the {@link PropertyAccessor} interface.
 *
 * @author Juergen Hoeller
 * @since 1.2.6
 */
public abstract class PropertyAccessorUtils {

	/**
	 * Return the actual property name for the given property path.
	 *
	 * @param propertyPath the property path to determine the property name
	 *                     for (can include property keys, for example for specifying a map entry)
	 * @return the actual property name, without any key elements
	 */
	public static String getPropertyName(String propertyPath) {
		int separatorIndex = (propertyPath.endsWith(PropertyAccessor.PROPERTY_KEY_SUFFIX) ?
				propertyPath.indexOf(PropertyAccessor.PROPERTY_KEY_PREFIX_CHAR) : -1);
		return (separatorIndex != -1 ? propertyPath.substring(0, separatorIndex) : propertyPath);
	}

	/**
	 * Check whether the given property path indicates an indexed or nested property.
	 *
	 * @param propertyPath the property path to check
	 * @return whether the path indicates an indexed or nested property
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
	 * Determine the first nested property separator in the
	 * given property path, ignoring dots in keys (like "map[my.key]").
	 *
	 * @param propertyPath the property path to check
	 * @return the index of the nested property separator, or -1 if none
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
	 * Determine whether the given registered path matches the given property path,
	 * either indicating the property itself or an indexed element of the property.
	 *
	 * @param propertyPath   the property path (typically without index)
	 * @param registeredPath the registered path (potentially with index)
	 * @return whether the paths match
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
	 * Determine the canonical name for the given property path.
	 * Removes surrounding quotes from map keys:<br>
	 * {@code map['key']} &rarr; {@code map[key]}<br>
	 * {@code map["key"]} &rarr; {@code map[key]}
	 *
	 * @param propertyName the bean property path
	 * @return the canonical representation of the property path
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
	 * Determine the canonical names for the given property paths.
	 *
	 * @param propertyNames the bean property paths (as array)
	 * @return the canonical representation of the property paths
	 * (as array of the same size)
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
