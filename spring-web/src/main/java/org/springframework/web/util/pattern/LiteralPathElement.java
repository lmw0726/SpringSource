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

package org.springframework.web.util.pattern;

import org.springframework.http.server.PathContainer;
import org.springframework.http.server.PathContainer.Element;
import org.springframework.http.server.PathContainer.PathSegment;
import org.springframework.web.util.pattern.PathPattern.MatchingContext;

/**
 * 字面路径元素。在模式'/foo/bar/goo'中，有三个字面路径元素'foo'、'bar'和'goo'。
 *
 * @author Andy Clement
 * @since 5.0
 */
class LiteralPathElement extends PathElement {
	/**
	 * 文本
	 */
	private final char[] text;

	/**
	 * 路径长度
	 */
	private final int len;

	/**
	 * 是否忽略大小写
	 */
	private final boolean caseSensitive;


	public LiteralPathElement(int pos, char[] literalText, boolean caseSensitive, char separator) {
		super(pos, separator);
		this.len = literalText.length;
		this.caseSensitive = caseSensitive;
		if (caseSensitive) {
			// 如果忽略大小写，则将文本设置文当前字面量
			this.text = literalText;
		} else {
			// 强制所有文本转换为小写，以加快匹配速度
			this.text = new char[literalText.length];
			for (int i = 0; i < this.len; i++) {
				// 将文本转换为小写
				this.text[i] = Character.toLowerCase(literalText[i]);
			}
		}
	}


	@Override
	public boolean matches(int pathIndex, MatchingContext matchingContext) {
		if (pathIndex >= matchingContext.pathLength) {
			// 没有更多路径可以匹配此元素
			return false;
		}
		// 获取当前路径索引的元素
		Element element = matchingContext.pathElements.get(pathIndex);
		if (!(element instanceof PathContainer.PathSegment)) {
			// 如果当前元素不是路径片段，返回false
			return false;
		}
		// 获取将要匹配的值
		String value = ((PathSegment) element).valueToMatch();
		if (value.length() != this.len) {
			// 数据不足以匹配此路径元素
			return false;
		}

		if (this.caseSensitive) {
			// 如果要忽略大小写
			for (int i = 0; i < this.len; i++) {
				if (value.charAt(i) != this.text[i]) {
					// 如果任意一个相同位置的值不相同，则返回false
					return false;
				}
			}
		} else {
			for (int i = 0; i < this.len; i++) {
				// TODO 如果执行大量不区分大小写的匹配，请重新评估性能
				if (Character.toLowerCase(value.charAt(i)) != this.text[i]) {
					// 如果任意一个相同位置的值与其大写的字符不相同，则返回false
					return false;
				}
			}
		}

		// 将路径索引加1
		pathIndex++;
		// 如果没有更多的模式
		if (isNoMorePattern()) {
			// 如果需要确定剩余路径
			if (matchingContext.determineRemainingPath) {
				// 设置剩余路径索引为当前路径索引，并返回true
				matchingContext.remainingPathIndex = pathIndex;
				return true;
			} else {
				// 否则，如果当前路径索引等于匹配上下文的路径长度，则返回true；
				if (pathIndex == matchingContext.pathLength) {
					return true;
				} else {
					// 否则，如果可选地匹配尾随分隔符
					// 并且当前路径索引加1等于匹配上下文的路径长度
					// 并且当前路径索引处是分隔符，则返回true
					return (matchingContext.isMatchOptionalTrailingSeparator() &&
							(pathIndex + 1) == matchingContext.pathLength &&
							matchingContext.isSeparator(pathIndex));
				}
			}
		} else {
			// 否则，如果存在下一个模式，则调用下一个模式的matches方法进行匹配
			return (this.next != null && this.next.matches(pathIndex, matchingContext));
		}
	}

	@Override
	public int getNormalizedLength() {
		return this.len;
	}

	@Override
	public char[] getChars() {
		return this.text;
	}


	@Override
	public String toString() {
		return "Literal(" + String.valueOf(this.text) + ")";
	}

}
