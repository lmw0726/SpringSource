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

import org.springframework.http.server.PathContainer.Element;
import org.springframework.http.server.PathContainer.PathSegment;
import org.springframework.web.util.pattern.PathPattern.MatchingContext;

/**
 * 表示字面路径元素，其中包含单字符通配符 '?' 一次或多次（基本上匹配该位置的任何字符）。
 *
 * @author Andy Clement
 * @since 5.0
 */
class SingleCharWildcardedPathElement extends PathElement {
	/**
	 * 文本字符
	 */
	private final char[] text;

	/**
	 * 长度
	 */
	private final int len;

	/**
	 * 问号计数
	 */
	private final int questionMarkCount;

	/**
	 * 是否大小写敏感
	 */
	private final boolean caseSensitive;


	public SingleCharWildcardedPathElement(
			int pos, char[] literalText, int questionMarkCount, boolean caseSensitive, char separator) {

		super(pos, separator);
		// 初始化长度、问号计数和大小写敏感标志
		this.len = literalText.length;
		this.questionMarkCount = questionMarkCount;
		this.caseSensitive = caseSensitive;

		// 如果是大小写敏感的，则直接使用原始文本作为文本
		if (caseSensitive) {
			this.text = literalText;
		} else {
			// 否则，将文本转换为小写字符存储
			this.text = new char[literalText.length];
			for (int i = 0; i < this.len; i++) {
				this.text[i] = Character.toLowerCase(literalText[i]);
			}
		}
	}


	@Override
	public boolean matches(int pathIndex, MatchingContext matchingContext) {
		if (pathIndex >= matchingContext.pathLength) {
			// 如果路径索引大于等于匹配上下文的路径长度，则没有足够的路径来匹配此元素，返回false
			return false;
		}

		// 获取路径索引处的路径段元素
		Element element = matchingContext.pathElements.get(pathIndex);
		if (!(element instanceof PathSegment)) {
			// 如果不是路径段元素，则无法匹配，返回false
			return false;
		}

		// 获取路径段元素的值
		String value = ((PathSegment) element).valueToMatch();
		if (value.length() != this.len) {
			// 如果值的长度与文本的长度不相等，则没有足够的数据来匹配此路径元素，返回false
			return false;
		}

		// 逐个比较字符进行匹配
		// 如果是大小写敏感的
		if (this.caseSensitive) {
			// 逐个比较字符
			for (int i = 0; i < this.len; i++) {
				char ch = this.text[i];
				if ((ch != '?') && (ch != value.charAt((i)))) {
					// 如果字符不是 ‘?’，并且同一位置的值不相等，则返回false
					return false;
				}
			}
		} else {
			// 否则，进行不区分大小写的匹配，注意性能问题
			for (int i = 0; i < this.len; i++) {
				char ch = this.text[i];
				// TODO 如果进行大量不区分大小写的匹配，请重新审视性能
				if ((ch != '?') && (ch != Character.toLowerCase(value.charAt(i)))) {
					// 如果字符不是 ‘?’，并且同一位置的值忽略不相等，则返回false
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
			// 否则，继续匹配下一个元素
			return (this.next != null && this.next.matches(pathIndex, matchingContext));
		}
	}

	@Override
	public int getWildcardCount() {
		return this.questionMarkCount;
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
		return "SingleCharWildcarded(" + String.valueOf(this.text) + ")";
	}

}
