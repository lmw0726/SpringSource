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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.pattern.PathPattern.MatchingContext;

import java.util.List;

/**
 * 表示捕获路径的其余部分的路径元素。在模式'/foo/{*foobar}'中，/{*foobar}表示为 {@link CaptureTheRestPathElement}。
 *
 * @author Andy Clement
 * @since 5.0
 */
class CaptureTheRestPathElement extends PathElement {
	/**
	 * 变量名称
	 */
	private final String variableName;


	/**
	 * 创建一个新的 {@link CaptureTheRestPathElement} 实例。
	 *
	 * @param pos               路径元素在路径模式文本中的位置
	 * @param captureDescriptor 包含类似 '{' '*' 'a' 'b' '}' 内容的字符数组
	 * @param separator         路径模式中使用的分隔符
	 */
	CaptureTheRestPathElement(int pos, char[] captureDescriptor, char separator) {
		super(pos, separator);
		this.variableName = new String(captureDescriptor, 2, captureDescriptor.length - 3);
	}


	@Override
	public boolean matches(int pathIndex, MatchingContext matchingContext) {
		// 不需要处理 'match start' 检查，因为这将捕获所有内容，而且不能跟随任何其他内容
		// 断言 next == null

		// 如果有更多的数据，则必须以分隔符开头
		if (pathIndex < matchingContext.pathLength && !matchingContext.isSeparator(pathIndex)) {
			// 如果路径索引小于匹配上下文的路径长度，且匹配上下文中的路径索引处不是分隔符，则返回false
			return false;
		}
		if (matchingContext.determineRemainingPath) {
			// 如果需要确定剩余路径，则将剩余路径索引设置为匹配上下文的路径长度
			matchingContext.remainingPathIndex = matchingContext.pathLength;
		}
		// 如果需要提取变量
		if (matchingContext.extractingVariables) {
			// 从所有剩余的段收集参数
			// 初始化参数收集器
			MultiValueMap<String, String> parametersCollector = null;
			// 遍历路径索引后的所有元素
			for (int i = pathIndex; i < matchingContext.pathLength; i++) {
				// 获取当前索引处的路径元素
				Element element = matchingContext.pathElements.get(i);
				// 如果是路径段元素
				if (element instanceof PathSegment) {
					// 获取路径段的参数
					MultiValueMap<String, String> parameters = ((PathSegment) element).parameters();
					// 如果参数不为空
					if (!parameters.isEmpty()) {
						// 如果参数收集器为空，则创建一个新的参数收集器
						if (parametersCollector == null) {
							parametersCollector = new LinkedMultiValueMap<>();
						}
						// 将参数添加到参数收集器中
						parametersCollector.addAll(parameters);
					}
				}
			}
			// 将变量名称、路径字符串和参数收集器存储到匹配上下文中
			matchingContext.set(this.variableName, pathToString(pathIndex, matchingContext.pathElements),
					parametersCollector == null ? NO_PARAMETERS : parametersCollector);
		}
		// 返回true
		return true;
	}

	private String pathToString(int fromSegment, List<Element> pathElements) {
		// 创建一个StringBuilder对象，用于构建路径字符串
		StringBuilder sb = new StringBuilder();
		// 从指定段开始遍历路径元素列表
		for (int i = fromSegment, max = pathElements.size(); i < max; i++) {
			// 获取当前索引处的路径元素
			Element element = pathElements.get(i);
			// 如果是路径段元素，则将其值添加到StringBuilder中
			if (element instanceof PathSegment) {
				sb.append(((PathSegment) element).valueToMatch());
			} else {
				// 否则，将路径元素的值添加到StringBuilder中
				sb.append(element.value());
			}
		}
		// 返回构建的路径字符串
		return sb.toString();
	}

	@Override
	public int getNormalizedLength() {
		return 1;
	}

	@Override
	public char[] getChars() {
		return ("/{*" + this.variableName + "}").toCharArray();
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
	public String toString() {
		return "CaptureTheRest(/{*" + this.variableName + "})";
	}

}
