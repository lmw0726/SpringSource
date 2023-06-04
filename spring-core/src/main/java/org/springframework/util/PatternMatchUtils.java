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

import org.springframework.lang.Nullable;

/**
 * Utility methods for simple pattern matching, in particular for
 * Spring's typical "xxx*", "*xxx" and "*xxx*" pattern styles.
 *
 * @author Juergen Hoeller
 * @since 2.0
 */
public abstract class PatternMatchUtils {

	/**
	 * 将字符串与给定的模式匹配，支持以下简单的模式样式: “xxx*”，“*xxx”，“*xxx*” 和 “xxx*yyy” 匹配 (具有任意数量的模式部分) 以及直接相等。
	 *
	 * @param pattern 要匹配的模式
	 * @param str     要匹配的字符串
	 * @return 字符串是否与给定的模式匹配
	 */
	public static boolean simpleMatch(@Nullable String pattern, @Nullable String str) {
		if (pattern == null || str == null) {
			//如果两者中有一个为空,则返回false
			return false;
		}
		//找到第一个*
		int firstIndex = pattern.indexOf('*');
		if (firstIndex == -1) {
			//如果没有*号,则返回pattern和str是否相等
			return pattern.equals(str);
		}

		if (firstIndex == 0) {
			//如果*号在第一个位置，则pattern只有一个字符，则返回true
			if (pattern.length() == 1) {
				return true;
			}
			//获取下一个*号的位置
			int nextIndex = pattern.indexOf('*', 1);
			if (nextIndex == -1) {
				//如果没有下一个*号，则判断要匹配的字符串，是否以pattern后一位字符串结尾
				return str.endsWith(pattern.substring(1));
			}
			//获取两个*号之间的字符串 part
			String part = pattern.substring(1, nextIndex);
			if (part.isEmpty()) {
				//如果两个*号之间的字符串为空，递归调用，判断原字符串与*号后的字符串是否匹配
				return simpleMatch(pattern.substring(nextIndex), str);
			}
			//获取两个*号之间的字符串 part 在原字符串中出现的位置
			int partIndex = str.indexOf(part);
			while (partIndex != -1) {
				if (simpleMatch(pattern.substring(nextIndex), str.substring(partIndex + part.length()))) {
					//如果第二个*号后的字符串与原字符串中part 字符串模式匹配，则返回true
					return true;
				}
				//寻找下一个 part字符串出现在原字符串中的位置
				partIndex = str.indexOf(part, partIndex + 1);
			}
			//如果没有匹配上，则返回false
			return false;
		}
		//如果原字符串长度大于等于第一个*号的位置，且模式以原字符串中第一个*号之前的字符串开头，
		//并且两个截取firstIndex之后的字符串匹配，返回true，否则返回false。
		return (str.length() >= firstIndex &&
				pattern.startsWith(str.substring(0, firstIndex)) &&
				simpleMatch(pattern.substring(firstIndex), str.substring(firstIndex)));
	}

	/**
	 * 将字符串与给定的模式匹配，支持以下简单的模式样式: “xxx*”，“*xxx”，“*xxx*” 和 “xxx*yyy” 匹配 (具有任意数量的模式部分) 以及直接相等。
	 *
	 * @param patterns 要匹配的模式数组
	 * @param str      要匹配的字符串
	 * @return 字符串是否与任何给定模式匹配
	 */
	public static boolean simpleMatch(@Nullable String[] patterns, String str) {
		if (patterns != null) {
			for (String pattern : patterns) {
				if (simpleMatch(pattern, str)) {
					return true;
				}
			}
		}
		return false;
	}

}
