/*
 * Copyright 2002-2021 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.lang.Nullable;

import java.util.*;

/**
 * Utility class for working with Strings that have placeholder values in them.
 * A placeholder takes the form {@code ${name}}. Using {@code PropertyPlaceholderHelper}
 * these placeholders can be substituted for user-supplied values.
 *
 * <p>Values for substitution can be supplied using a {@link Properties} instance or
 * using a {@link PlaceholderResolver}.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @since 3.0
 */
public class PropertyPlaceholderHelper {

	private static final Log logger = LogFactory.getLog(PropertyPlaceholderHelper.class);

	private static final Map<String, String> wellKnownSimplePrefixes = new HashMap<>(4);

	static {
		wellKnownSimplePrefixes.put("}", "{");
		wellKnownSimplePrefixes.put("]", "[");
		wellKnownSimplePrefixes.put(")", "(");
	}


	private final String placeholderPrefix;

	private final String placeholderSuffix;

	private final String simplePrefix;

	@Nullable
	private final String valueSeparator;

	private final boolean ignoreUnresolvablePlaceholders;


	/**
	 * Creates a new {@code PropertyPlaceholderHelper} that uses the supplied prefix and suffix.
	 * Unresolvable placeholders are ignored.
	 *
	 * @param placeholderPrefix the prefix that denotes the start of a placeholder
	 * @param placeholderSuffix the suffix that denotes the end of a placeholder
	 */
	public PropertyPlaceholderHelper(String placeholderPrefix, String placeholderSuffix) {
		this(placeholderPrefix, placeholderSuffix, null, true);
	}

	/**
	 * 创建一个新的 {@code PropertyPlaceholderHelper}，它使用提供的前缀和后缀。
	 *
	 * @param placeholderPrefix              表示占位符起始的前缀
	 * @param placeholderSuffix              表示占位符结束的后缀
	 * @param valueSeparator                 占位符变量与关联默认值之间的分隔字符（如果有）
	 * @param ignoreUnresolvablePlaceholders 指示是否应忽略无法解析的占位符（{@code true}），还是引发异常（{@code false}）
	 */
	public PropertyPlaceholderHelper(String placeholderPrefix, String placeholderSuffix,
									 @Nullable String valueSeparator, boolean ignoreUnresolvablePlaceholders) {

		Assert.notNull(placeholderPrefix, "'placeholderPrefix' must not be null");
		Assert.notNull(placeholderSuffix, "'placeholderSuffix' must not be null");
		// 设置占位符的前缀和后缀
		this.placeholderPrefix = placeholderPrefix;
		this.placeholderSuffix = placeholderSuffix;

		// 检查是否存在简单的后缀前缀对应关系，如果有则设置简单前缀
		String simplePrefixForSuffix = wellKnownSimplePrefixes.get(this.placeholderSuffix);
		if (simplePrefixForSuffix != null && this.placeholderPrefix.endsWith(simplePrefixForSuffix)) {
			this.simplePrefix = simplePrefixForSuffix;
		} else {
			// 否则设置为占位符前缀
			this.simplePrefix = this.placeholderPrefix;
		}

		// 设置占位符值的分隔符
		this.valueSeparator = valueSeparator;

		// 设置是否忽略无法解析的占位符
		this.ignoreUnresolvablePlaceholders = ignoreUnresolvablePlaceholders;
	}


	/**
	 * Replaces all placeholders of format {@code ${name}} with the corresponding
	 * property from the supplied {@link Properties}.
	 *
	 * @param value      the value containing the placeholders to be replaced
	 * @param properties the {@code Properties} to use for replacement
	 * @return the supplied value with placeholders replaced inline
	 */
	public String replacePlaceholders(String value, final Properties properties) {
		Assert.notNull(properties, "'properties' must not be null");
		return replacePlaceholders(value, properties::getProperty);
	}

	/**
	 * 用从提供的 {@link PlaceholderResolver} 返回的值替换格式 {@code ${name}} 的所有占位符。
	 *
	 * @param value               包含要替换的占位符的值
	 * @param placeholderResolver 用于替换的 {@code PlaceholderResolver}
	 * @return 内联替换占位符的提供值
	 */
	public String replacePlaceholders(String value, PlaceholderResolver placeholderResolver) {
		Assert.notNull(value, "'value' must not be null");
		return parseStringValue(value, placeholderResolver, null);
	}

	protected String parseStringValue(
			String value, PlaceholderResolver placeholderResolver, @Nullable Set<String> visitedPlaceholders) {

		int startIndex = value.indexOf(this.placeholderPrefix);
		if (startIndex == -1) {
			//如果占位符前缀不存在，返回原来的字符串
			return value;
		}

		StringBuilder result = new StringBuilder(value);
		while (startIndex != -1) {
			//找到结束占位符的位置
			int endIndex = findPlaceholderEndIndex(result, startIndex);
			if (endIndex == -1) {
				//如果没有结束占位符，跳出循环
				startIndex = -1;
				break;
			}
			//找到${}内的占位符
			String placeholder = result.substring(startIndex + this.placeholderPrefix.length(), endIndex);
			String originalPlaceholder = placeholder;
			if (visitedPlaceholders == null) {
				//已经访问过的占位符集合
				visitedPlaceholders = new HashSet<>(4);
			}
			if (!visitedPlaceholders.add(originalPlaceholder)) {
				//如果该占位符已经访问过了，抛出IllegalArgumentException
				throw new IllegalArgumentException(
						"Circular placeholder reference '" + originalPlaceholder + "' in property definitions");
			}
			// 递归调用，解析占位符键中包含的占位符。
			placeholder = parseStringValue(placeholder, placeholderResolver, visitedPlaceholders);
			// 解析占位符成对应的值
			String propVal = placeholderResolver.resolvePlaceholder(placeholder);
			if (propVal == null && this.valueSeparator != null) {
				//如果值为空，且值分割符不为空，即对应含有默认值的情况：${name:defaultName}
				//获取分割符的位置
				int separatorIndex = placeholder.indexOf(this.valueSeparator);
				if (separatorIndex != -1) {
					//如果有分割符位置，获取前面的字符，作为实际占位符
					String actualPlaceholder = placeholder.substring(0, separatorIndex);
					//分隔符后面的字符则是默认值
					String defaultValue = placeholder.substring(separatorIndex + this.valueSeparator.length());
					//再次解析实际占位符对应的值
					propVal = placeholderResolver.resolvePlaceholder(actualPlaceholder);
					if (propVal == null) {
						//如果值为空，则取默认值
						propVal = defaultValue;
					}
				}
			}
			if (propVal != null) {
				//如果值不为空，递归解析该值，如yml文件中含有${server.port}的情况
				propVal = parseStringValue(propVal, placeholderResolver, visitedPlaceholders);
				//将解析后的值替换到原始值中
				result.replace(startIndex, endIndex + this.placeholderSuffix.length(), propVal);
				if (logger.isTraceEnabled()) {
					logger.trace("Resolved placeholder '" + placeholder + "'");
				}
				//移动到下一个占位符
				startIndex = result.indexOf(this.placeholderPrefix, startIndex + propVal.length());
			} else if (this.ignoreUnresolvablePlaceholders) {
				// 继续使用未处理的值。
				startIndex = result.indexOf(this.placeholderPrefix, endIndex + this.placeholderSuffix.length());
			} else {
				throw new IllegalArgumentException("Could not resolve placeholder '" +
						placeholder + "'" + " in value \"" + value + "\"");
			}
			//移除已访问过的占位符
			visitedPlaceholders.remove(originalPlaceholder);
		}
		return result.toString();
	}

	private int findPlaceholderEndIndex(CharSequence buf, int startIndex) {
		//占位符前缀的位置
		int index = startIndex + this.placeholderPrefix.length();
		//嵌套在占位符内的数量
		int withinNestedPlaceholder = 0;
		while (index < buf.length()) {
			if (StringUtils.substringMatch(buf, index, this.placeholderSuffix)) {
				//如果buffer在索引处后的若干个字符，刚好是占位符后缀
				if (withinNestedPlaceholder > 0) {
					//如果在占位符内，嵌套在占位符内的数量减一
					withinNestedPlaceholder--;
					//移动到占位符后缀长度后的位置
					index = index + this.placeholderSuffix.length();
				} else {
					//如果嵌套在占位符内的数量为0，则返回索引位置
					return index;
				}
			} else if (StringUtils.substringMatch(buf, index, this.simplePrefix)) {
				//如果buffer在索引处后的若干个字符，刚好是简单的占位符前缀如 { 符号
				//嵌套在占位符内的数量加一
				withinNestedPlaceholder++;
				//移动到简单占位符前缀长度后的位置
				index = index + this.simplePrefix.length();
			} else {
				//移动到下一个位置
				index++;
			}
		}
		return -1;
	}


	/**
	 * Strategy interface used to resolve replacement values for placeholders contained in Strings.
	 */
	@FunctionalInterface
	public interface PlaceholderResolver {

		/**
		 * 将提供的占位符名称解析为替换值。
		 *
		 * @param placeholderName 要解析的占位符的名称
		 * @return 替换值，如果不进行替换，则为 {@code null}
		 */
		@Nullable
		String resolvePlaceholder(String placeholderName);
	}

}
