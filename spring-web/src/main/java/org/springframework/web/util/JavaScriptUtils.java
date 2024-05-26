/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.util;

/**
 * JavaScript转义工具类。
 * 根据JavaScript 1.5推荐进行转义。
 *
 * <p>参考：
 * <a href="https://developer.mozilla.org/en-US/docs/JavaScript/Guide/Values,_variables,_and_literals#String_literals">
 * Mozilla开发者网络上的JavaScript指南</a>。
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Rossen Stoyanchev
 * @since 1.1.1
 */
public abstract class JavaScriptUtils {

	/**
	 * 将JavaScript特殊字符转义为转义字符。
	 *
	 * @param input 输入字符串
	 * @return 带有转义字符的字符串
	 */
	public static String javaScriptEscape(String input) {
		// 创建一个 StringBuilder，其初始容量为输入字符串的长度
		StringBuilder filtered = new StringBuilder(input.length());
		// 前一个字符的初始化为 NULL 字符
		char prevChar = '\u0000';
		char c;
		// 遍历输入字符串的每个字符
		for (int i = 0; i < input.length(); i++) {
			c = input.charAt(i);
			// 根据字符类型进行处理
			if (c == '"') {
				filtered.append("\\\"");
			} else if (c == '\'') {
				filtered.append("\\'");
			} else if (c == '\\') {
				filtered.append("\\\\");
			} else if (c == '/') {
				filtered.append("\\/");
			} else if (c == '\t') {
				filtered.append("\\t");
			} else if (c == '\n') {
				// 如果当前字符是换行符
				if (prevChar != '\r') {
					filtered.append("\\n");
				}
			} else if (c == '\r') {
				// 如果当前字符是回车符，替换为换行符
				filtered.append("\\n");
			} else if (c == '\f') {
				filtered.append("\\f");
			} else if (c == '\b') {
				filtered.append("\\b");
			} else if (c == '\013') {
				// Java中没有 '\v'，使用VT ASCII字符的八进制值
				// 使用 VT ASCII 字符的八进制值替代 '\v'
				filtered.append("\\v");
			} else if (c == '<') {
				filtered.append("\\u003C");
			} else if (c == '>') {
				filtered.append("\\u003E");
			} else if (c == '\u2028') {
				// PS（ECMA-262中的行终止符）的Unicode
				// 替换 PS 和 LS Unicode 字符
				filtered.append("\\u2028");
			} else if (c == '\u2029') {
				// LS（ECMA-262中的行终止符）的Unicode
				filtered.append("\\u2029");
			} else {
				// 其他字符直接追加
				filtered.append(c);
			}
			// 更新前一个字符
			prevChar = c;

		}
		// 返回过滤后的字符串
		return filtered.toString();
	}

}
