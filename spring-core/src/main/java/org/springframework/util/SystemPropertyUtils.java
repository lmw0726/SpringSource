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

package org.springframework.util;

import org.springframework.lang.Nullable;

/**
 * 用于解析文本中占位符的辅助类。通常应用于文件路径。
 *
 * <p>文本中可以包含 {@code ${...}} 占位符，将被解析为系统属性：
 * 例如 {@code ${user.dir}}。默认值可以使用 ":" 分隔符在键和值之间提供。
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Dave Syer
 * @since 1.2.5
 * @see #PLACEHOLDER_PREFIX
 * @see #PLACEHOLDER_SUFFIX
 * @see System#getProperty(String)
 */
public abstract class SystemPropertyUtils {

	/** 系统属性占位符的前缀："${"。 */
	public static final String PLACEHOLDER_PREFIX = "${";

	/** 系统属性占位符的后缀："}"。 */
	public static final String PLACEHOLDER_SUFFIX = "}";

	/** 系统属性占位符的值分隔符：":"。 */
	public static final String VALUE_SEPARATOR = ":";


	private static final PropertyPlaceholderHelper strictHelper =
			new PropertyPlaceholderHelper(PLACEHOLDER_PREFIX, PLACEHOLDER_SUFFIX, VALUE_SEPARATOR, false);

	private static final PropertyPlaceholderHelper nonStrictHelper =
			new PropertyPlaceholderHelper(PLACEHOLDER_PREFIX, PLACEHOLDER_SUFFIX, VALUE_SEPARATOR, true);


	/**
	 * 解析给定文本中的 {@code ${...}} 占位符，并将其替换为相应的系统属性值。
	 * @param text 要解析的字符串
	 * @return 解析后的字符串
	 * @throws IllegalArgumentException 如果存在无法解析的占位符
	 * @see #PLACEHOLDER_PREFIX
	 * @see #PLACEHOLDER_SUFFIX
	 */
	public static String resolvePlaceholders(String text) {
		return resolvePlaceholders(text, false);
	}

	/**
	 * 解析给定文本中的 {@code ${...}} 占位符，并将其替换为相应的系统属性值。
	 * 如果未解析的占位符没有默认值且 ignoreUnresolvablePlaceholders 为 {@code true}，
	 * 则忽略该占位符并原样保留。
	 *
	 * @param text                           要解析的字符串
	 * @param ignoreUnresolvablePlaceholders 是否忽略无法解析的占位符
	 * @return 解析后的字符串
	 * @throws IllegalArgumentException 如果存在无法解析的占位符，且 ignoreUnresolvablePlaceholders 为 {@code false}
	 * @see #PLACEHOLDER_PREFIX
	 * @see #PLACEHOLDER_SUFFIX
	 */
	public static String resolvePlaceholders(String text, boolean ignoreUnresolvablePlaceholders) {
		if (text.isEmpty()) {
			return text;
		}
		PropertyPlaceholderHelper helper = (ignoreUnresolvablePlaceholders ? nonStrictHelper : strictHelper);
		return helper.replacePlaceholders(text, new SystemPropertyPlaceholderResolver(text));
	}


	/**
	 * 占位符解析器实现类，支持从系统属性和环境变量中解析占位符。
	 */
	private static class SystemPropertyPlaceholderResolver implements PropertyPlaceholderHelper.PlaceholderResolver {

		private final String text;

		public SystemPropertyPlaceholderResolver(String text) {
			this.text = text;
		}

		@Override
		@Nullable
		public String resolvePlaceholder(String placeholderName) {
			try {
				String propVal = System.getProperty(placeholderName);
				if (propVal == null) {
					// 退回使用系统环境变量查找
					propVal = System.getenv(placeholderName);
				}
				return propVal;
			}
			catch (Throwable ex) {
				System.err.println("Could not resolve placeholder '" + placeholderName + "' in [" +
						this.text + "] as system property: " + ex);
				return null;
			}
		}
	}

}
