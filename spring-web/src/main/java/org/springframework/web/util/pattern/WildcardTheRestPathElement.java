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

/**
 * 表示通配路径剩余部分的路径元素。在模式 '/foo/**' 中，/** 表示为 WildcardTheRestPathElement。
 *
 * @author Andy Clement
 * @since 5.0
 */
class WildcardTheRestPathElement extends PathElement {

	WildcardTheRestPathElement(int pos, char separator) {
		super(pos, separator);
	}


	@Override
	public boolean matches(int pathIndex, PathPattern.MatchingContext matchingContext) {
		// 如果还有更多数据，则必须以分隔符开头
		if (pathIndex < matchingContext.pathLength && !matchingContext.isSeparator(pathIndex)) {
			// 如果路径索引小于匹配上下文的路径长度，且匹配上下文中的路径索引处不是分隔符，则返回false
			return false;
		}
		if (matchingContext.determineRemainingPath) {
			// 如果需要确定剩余路径，则将剩余路径索引设置为匹配上下文的路径长度
			matchingContext.remainingPathIndex = matchingContext.pathLength;
		}
		// 返回true
		return true;
	}

	@Override
	public int getNormalizedLength() {
		return 1;
	}

	@Override
	public char[] getChars() {
		return (this.separator + "**").toCharArray();
	}

	@Override
	public int getWildcardCount() {
		return 1;
	}


	@Override
	public String toString() {
		return "WildcardTheRest(" + this.separator + "**)";
	}

}
