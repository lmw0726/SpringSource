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

import org.springframework.http.server.PathContainer.PathSegment;
import org.springframework.lang.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 表示捕获路径的一部分作为变量的路径元素。在模式'/foo/{bar}/goo'中，{bar}表示为 {@link CaptureVariablePathElement}。
 * 必须至少有一个字符绑定到变量。
 *
 * @author Andy Clement
 * @since 5.0
 */
class CaptureVariablePathElement extends PathElement {
	/**
	 * 变量名
	 */
	private final String variableName;

	/**
	 * 约束模式
	 */
	@Nullable
	private final Pattern constraintPattern;


	/**
	 * 创建一个新的 {@link CaptureVariablePathElement} 实例。
	 *
	 * @param pos               此捕获元素的模式中的位置
	 * @param captureDescriptor 形式为 {AAAAA[:pattern]}
	 */
	CaptureVariablePathElement(int pos, char[] captureDescriptor, boolean caseSensitive, char separator) {
		super(pos, separator);
		// 初始化冒号位置
		int colon = -1;
		for (int i = 0; i < captureDescriptor.length; i++) {
			// 查找冒号的位置
			if (captureDescriptor[i] == ':') {
				colon = i;
				break;
			}
		}
		if (colon == -1) {
			// 如果没有冒号，将变量名称设置为捕获描述符的子字符串
			this.variableName = new String(captureDescriptor, 1, captureDescriptor.length - 2);
			// 并将约束模式设置为null
			this.constraintPattern = null;
		} else {
			// 否则，将变量名称设置为冒号之前的子字符串
			this.variableName = new String(captureDescriptor, 1, colon - 1);
			if (caseSensitive) {
				// 如果是大小写敏感的，则使用指定的正则表达式创建约束模式
				this.constraintPattern = Pattern.compile(
						new String(captureDescriptor, colon + 1, captureDescriptor.length - colon - 2));
			} else {
				// 否则，使用指定的正则表达式和不区分大小写标志创建约束模式
				this.constraintPattern = Pattern.compile(
						new String(captureDescriptor, colon + 1, captureDescriptor.length - colon - 2),
						Pattern.CASE_INSENSITIVE);
			}
		}
	}


	@Override
	public boolean matches(int pathIndex, PathPattern.MatchingContext matchingContext) {
		if (pathIndex >= matchingContext.pathLength) {
			// 如果路径索引大于等于路径最大长度，表示没有足够的路径来匹配此元素，返回false
			return false;
		}
		// 获取候选捕获值
		String candidateCapture = matchingContext.pathElementValue(pathIndex);
		if (candidateCapture.length() == 0) {
			// 候选捕获值为空，返回false
			return false;
		}

		if (this.constraintPattern != null) {
			// TODO 可能的优化 - 仅在其余模式匹配时才进行正则表达式匹配？
			// 利益可能会因模式而异
			Matcher matcher = this.constraintPattern.matcher(candidateCapture);
			if (matcher.groupCount() != 0) {
				// 如果匹配到了多个值，抛出异常
				throw new IllegalArgumentException(
						"No capture groups allowed in the constraint regex: " + this.constraintPattern.pattern());
			}
			if (!matcher.matches()) {
				// 没有匹配任何一个值，返回false
				return false;
			}
		}

		boolean match = false;
		// 将路径索引加1
		pathIndex++;
		// 如果没有更多的模式
		if (isNoMorePattern()) {
			// 如果需要确定剩余路径
			if (matchingContext.determineRemainingPath) {
				// 设置剩余路径索引为当前路径索引，并将匹配标志设为true
				matchingContext.remainingPathIndex = pathIndex;
				match = true;
			} else {
				// 需要至少一个字符 #SPR15264
				// 如果当前路径索引等于匹配上下文的路径长度，则将匹配标志设为true
				match = (pathIndex == matchingContext.pathLength);
				// 如果不匹配，并且可选地匹配尾随分隔符
				if (!match && matchingContext.isMatchOptionalTrailingSeparator()) {
					match = //(nextPos > candidateIndex) &&
							// 如果当前路径索引加1等于匹配上下文的路径长度，并且当前路径索引处是分隔符，则将匹配标志设为true
							(pathIndex + 1) == matchingContext.pathLength &&
									matchingContext.isSeparator(pathIndex);
				}
			}
		} else {
			// 否则，如果存在下一个模式
			if (this.next != null) {
				// 则调用下一个模式的matches方法进行匹配
				match = this.next.matches(pathIndex, matchingContext);
			}
		}

		// 如果匹配成功并且需要提取变量
		if (match && matchingContext.extractingVariables) {
			// 则将变量名称、候选捕获和路径段的参数存储到匹配上下文中
			matchingContext.set(this.variableName, candidateCapture,
					((PathSegment) matchingContext.pathElements.get(pathIndex - 1)).parameters());
		}
		return match;
	}

	public String getVariableName() {
		return this.variableName;
	}

	@Override
	public int getNormalizedLength() {
		return 1;
	}

	@Override
	public char[] getChars() {
		// 创建一个StringBuilder对象，用于构建路径段的字符表示
		StringBuilder sb = new StringBuilder();
		// 添加变量名到字符串构建器中
		sb.append('{');
		sb.append(this.variableName);
		// 如果约束模式不为空，则添加约束模式到字符串构建器中
		if (this.constraintPattern != null) {
			sb.append(':').append(this.constraintPattern.pattern());
		}
		// 添加结束标记到字符串构建器中
		sb.append('}');
		// 返回构建的字符串表示的字符数组
		return sb.toString().toCharArray();
	}

	@Override
	public int getWildcardCount() {
		return 0;
	}

	@Override
	public int getCaptureCount() {
		return 1;
	}

	@Override
	public int getScore() {
		return CAPTURE_VARIABLE_WEIGHT;
	}


	@Override
	public String toString() {
		return "CaptureVariable({" + this.variableName +
				(this.constraintPattern != null ? ":" + this.constraintPattern.pattern() : "") + "})";
	}

}
