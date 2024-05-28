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

import org.springframework.web.util.pattern.PathPattern.MatchingContext;

/**
 * 分隔符路径元素。在模式 '/foo/bar' 中，两个 '/' 的出现将由 SeparatorPathElement 表示（如果使用默认的 '/' 分隔符）。
 *
 * @author Andy Clement
 * @since 5.0
 */
class SeparatorPathElement extends PathElement {

	SeparatorPathElement(int pos, char separator) {
		super(pos, separator);
	}


	/**
	 * 匹配分隔符很容易，基本上 candidateIndex 处的字符必须是分隔符。
	 */
	@Override
	public boolean matches(int pathIndex, MatchingContext matchingContext) {
		// 如果路径索引小于匹配上下文的路径长度，且匹配上下文中的路径索引处是分隔符
		if (pathIndex < matchingContext.pathLength && matchingContext.isSeparator(pathIndex)) {
			// 如果没有更多的模式
			if (isNoMorePattern()) {
				// 如果确定需要确定剩余路径
				if (matchingContext.determineRemainingPath) {
					// 设置剩余路径索引为当前路径索引加1，并返回true
					matchingContext.remainingPathIndex = pathIndex + 1;
					return true;
				} else {
					// 否则，返回当前路径索引加1是否等于匹配上下文的路径长度
					return (pathIndex + 1 == matchingContext.pathLength);
				}
			} else {
				// 否则，将路径索引加1，并继续匹配下一个元素
				pathIndex++;
				return (this.next != null && this.next.matches(pathIndex, matchingContext));
			}
		}
		// 如果路径索引处不是分隔符，则返回false
		return false;
	}

	@Override
	public int getNormalizedLength() {
		return 1;
	}

	@Override
	public char[] getChars() {
		return new char[]{this.separator};
	}


	@Override
	public String toString() {
		return "Separator(" + this.separator + ")";
	}

}
