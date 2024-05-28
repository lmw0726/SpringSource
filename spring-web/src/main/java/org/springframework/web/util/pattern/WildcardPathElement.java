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
import org.springframework.web.util.pattern.PathPattern.MatchingContext;

/**
 * 通配符路径元素。在模式 '/foo/*goo' 中，* 由 WildcardPathElement 表示。
 * 在路径内，它至少匹配一个字符，但在路径的末尾它可以匹配零个字符。
 *
 * @author Andy Clement
 * @since 5.0
 */
class WildcardPathElement extends PathElement {

	public WildcardPathElement(int pos, char separator) {
		super(pos, separator);
	}


	/**
	 * 匹配 WildcardPathElement 相当简单。从 candidateIndex 开始扫描 candidate，直到找到下一个分隔符或候选路径的结尾。
	 */
	@Override
	public boolean matches(int pathIndex, MatchingContext matchingContext) {
		String segmentData = null;
		// 如果存在，则断言它是一个段
		if (pathIndex < matchingContext.pathLength) {
			// 如果路径索引小于路径长，获取当前的路径元素
			Element element = matchingContext.pathElements.get(pathIndex);
			if (!(element instanceof PathContainer.PathSegment)) {
				// 如果当前的路径元素不是 路径段类型，则返回false。
				// 不应匹配分隔符
				return false;
			}
			// 获取路径段的值
			segmentData = ((PathContainer.PathSegment) element).valueToMatch();
			// 路径索引+1
			pathIndex++;
		}

		if (isNoMorePattern()) {
			// 如果没有更多的元素
			if (matchingContext.determineRemainingPath) {
				// 如果需要确定剩余路径。，则将剩余路径索引设置为当前的路径索引
				matchingContext.remainingPathIndex = pathIndex;
				return true;
			} else {
				if (pathIndex == matchingContext.pathLength) {
					// 并且路径数据也已经用完
					return true;
				} else {
					// 如果可选斜杠打开...
					// 并且有至少一个字符匹配 *...
					// 并且下一个路径元素是候选的结尾...
					// 并且最后一个元素是分隔符
					return (matchingContext.isMatchOptionalTrailingSeparator() &&
							segmentData != null && segmentData.length() > 0 &&
							(pathIndex + 1) == matchingContext.pathLength &&
							matchingContext.isSeparator(pathIndex));
				}
			}
		} else {
			// 在路径内（例如 /aa/*/bb）必须至少有一个字符与通配符匹配
			if (segmentData == null || segmentData.length() == 0) {
				return false;
			}
			// 递归匹配下一个路径元素
			return (this.next != null && this.next.matches(pathIndex, matchingContext));
		}
	}

	@Override
	public int getNormalizedLength() {
		return 1;
	}

	@Override
	public char[] getChars() {
		return new char[]{'*'};
	}

	@Override
	public int getWildcardCount() {
		return 1;
	}

	@Override
	public int getScore() {
		return WILDCARD_WEIGHT;
	}


	@Override
	public String toString() {
		return "Wildcard(*)";
	}

}
