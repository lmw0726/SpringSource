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
import org.springframework.web.util.pattern.PathPattern.MatchingContext;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 正则表达式路径元素。用于表示路径的任何复杂元素。
 * 例如，在'<tt>/foo/&ast;_&ast;/&ast;_{foobar}</tt>' both <tt>*_*</tt> and <tt>*_{foobar}</tt>'都是{@link RegexPathElement}路径元素。
 * 派生自通用的{@link org.springframework.util.AntPathMatcher}方法。
 *
 * @author Andy Clement
 * @since 5.0
 */
class RegexPathElement extends PathElement {

	/**
	 * 用于匹配通配符和变量的正则表达式模式。
	 *
	 * <p>
	 * 这个模式可以匹配以下内容：
	 * 	<ul>
	 * 		<li>{@code ?}：匹配单个字符</li>
	 * 		<li>{@code *}：匹配零个或多个字符</li>
	 * 		<li>{@code {}}：用于定义变量模式的定界符</li>
	 * 	</ul>
	 * </p>
	 */
	private static final Pattern GLOB_PATTERN = Pattern.compile("\\?|\\*|\\{((?:\\{[^/]+?\\}|[^/{}]|\\\\[{}])+?)\\}");

	/**
	 * 默认的变量模式，匹配任意字符序列。
	 */
	private static final String DEFAULT_VARIABLE_PATTERN = "(.*)";


	/**
	 * 正则表达式模式的字符数组表示形式。
	 */
	private final char[] regex;

	/**
	 * 是否区分大小写。
	 */
	private final boolean caseSensitive;

	/**
	 * 编译后的正则表达式模式。
	 */
	private final Pattern pattern;

	/**
	 * 通配符的计数。
	 */
	private int wildcardCount;

	/**
	 * 变量名称列表。
	 */
	private final List<String> variableNames = new ArrayList<>();


	RegexPathElement(int pos, char[] regex, boolean caseSensitive, char[] completePattern, char separator) {
		super(pos, separator);
		this.regex = regex;
		this.caseSensitive = caseSensitive;
		this.pattern = buildPattern(regex, completePattern);
	}


	public Pattern buildPattern(char[] regex, char[] completePattern) {
		// 创建一个StringBuilder对象，用于构建模式
		StringBuilder patternBuilder = new StringBuilder();
		// 将正则表达式转换为字符串
		String text = new String(regex);
		// 创建一个匹配器，用于查找通配符
		Matcher matcher = GLOB_PATTERN.matcher(text);
		// 初始化匹配的结束位置
		int end = 0;

		// 循环查找通配符并替换
		while (matcher.find()) {
			// 将通配符之前的部分添加到模式构建器中
			patternBuilder.append(quote(text, end, matcher.start()));
			// 获取匹配到的通配符
			String match = matcher.group();
			// 处理问号通配符
			if ("?".equals(match)) {
				patternBuilder.append('.');
			} else if ("*".equals(match)) {
				// 处理星号通配符
				patternBuilder.append(".*");
				// 检查星号通配符是否被单词边界包围，若不是，则增加通配符计数
				int pos = matcher.start();
				if (pos < 1 || text.charAt(pos - 1) != '.') {
					// 与AntPathMatcher比较器兼容，' .* '不被视为通配符使用
					this.wildcardCount++;
				}
			} else if (match.startsWith("{") && match.endsWith("}")) {
				// 处理变量
				// 查找变量的冒号位置
				int colonIdx = match.indexOf(':');
				if (colonIdx == -1) {
					// 如果冒号不存在，使用默认变量模式
					patternBuilder.append(DEFAULT_VARIABLE_PATTERN);
					// 获取变量名并检查是否重复
					String variableName = matcher.group(1);
					if (this.variableNames.contains(variableName)) {
						// 如果变量名重复，则抛出异常
						throw new PatternParseException(this.pos, completePattern,
								PatternParseException.PatternMessage.ILLEGAL_DOUBLE_CAPTURE, variableName);
					}
					// 将变量名添加到变量名称列表
					this.variableNames.add(variableName);
				} else {
					// 如果冒号存在，使用自定义变量模式
					String variablePattern = match.substring(colonIdx + 1, match.length() - 1);
					patternBuilder.append('(');
					patternBuilder.append(variablePattern);
					patternBuilder.append(')');
					// 获取变量名并检查是否重复
					String variableName = match.substring(1, colonIdx);
					if (this.variableNames.contains(variableName)) {
						// 如果变量名重复，则抛出异常
						throw new PatternParseException(this.pos, completePattern,
								PatternParseException.PatternMessage.ILLEGAL_DOUBLE_CAPTURE, variableName);
					}
					// 将变量名添加到变量名称列表
					this.variableNames.add(variableName);
				}
			}
			// 更新匹配的结束位置
			end = matcher.end();
		}

		// 将剩余部分添加到模式构建器中
		patternBuilder.append(quote(text, end, text.length()));
		// 根据大小写敏感性返回相应的模式对象
		if (this.caseSensitive) {
			return Pattern.compile(patternBuilder.toString());
		} else {
			return Pattern.compile(patternBuilder.toString(), Pattern.CASE_INSENSITIVE);
		}
	}

	public List<String> getVariableNames() {
		return this.variableNames;
	}

	private String quote(String s, int start, int end) {
		if (start == end) {
			// 如果开始位置和结束位置，返回空字符串
			return "";
		}
		// 返回引号括起来的字符串
		return Pattern.quote(s.substring(start, end));
	}

	@Override
	public boolean matches(int pathIndex, MatchingContext matchingContext) {
		// 获取要匹配的文本
		String textToMatch = matchingContext.pathElementValue(pathIndex);
		// 使用模式对象创建一个匹配器
		Matcher matcher = this.pattern.matcher(textToMatch);
		// 进行匹配
		boolean matches = matcher.matches();

		// 如果匹配成功
		if (matches) {
			// 如果没有更多的模式
			if (isNoMorePattern()) {
				// 如果需要确定剩余路径
				// 并且变量名列表为空或文本匹配长度大于0
				if (matchingContext.determineRemainingPath &&
						(this.variableNames.isEmpty() || textToMatch.length() > 0)) {
					// 设置剩余路径索引为当前路径索引加1
					matchingContext.remainingPathIndex = pathIndex + 1;
					// 并将matches设为true
					matches = true;
				} else {
					// 否则，如果没有更多的模式，检查是否还有更多的数据
					// 如果模式正在捕获变量，则必须有一些实际数据与其绑定
					matches = (pathIndex + 1 >= matchingContext.pathLength) &&
							(this.variableNames.isEmpty() || textToMatch.length() > 0);
					if (!matches && matchingContext.isMatchOptionalTrailingSeparator()) {
						// 如果matches为false，并且匹配上下文中的可选尾随分隔符为真，则检查是否还有更多的数据和分隔符
						matches = (this.variableNames.isEmpty() || textToMatch.length() > 0) &&
								(pathIndex + 2 >= matchingContext.pathLength) &&
								matchingContext.isSeparator(pathIndex + 1);
					}
				}
			} else {
				// 否则，如果有下一个模式，则调用下一个模式的matches方法进行匹配
				matches = (this.next != null && this.next.matches(pathIndex + 1, matchingContext));
			}
		}

		// 如果匹配成功，并且需要提取变量
		if (matches && matchingContext.extractingVariables) {
			// 处理捕获的值
			if (this.variableNames.size() != matcher.groupCount()) {
				// 如果模式段中捕获组的数量与定义的URI模板变量数量不匹配，则抛出异常
				throw new IllegalArgumentException("The number of capturing groups in the pattern segment " +
						this.pattern + " does not match the number of URI template variables it defines, " +
						"which can occur if capturing groups are used in a URI template regex. " +
						"Use non-capturing groups instead.");
			}
			// 遍历捕获组
			for (int i = 1; i <= matcher.groupCount(); i++) {
				// 获取变量名和值
				String name = this.variableNames.get(i - 1);
				String value = matcher.group(i);
				// 将变量名和值存储到匹配上下文中
				matchingContext.set(name, value,
						(i == this.variableNames.size()) ?
								((PathSegment) matchingContext.pathElements.get(pathIndex)).parameters() :
								NO_PARAMETERS);
			}
		}
		// 返回匹配结果
		return matches;
	}

	@Override
	public int getNormalizedLength() {
		// 初始化变量名的总长度
		int varsLength = 0;
		// 计算所有变量名的总长度
		for (String variableName : this.variableNames) {
			varsLength += variableName.length();
		}
		// 计算模式字符串中除变量名外的部分长度
		return (this.regex.length - varsLength - this.variableNames.size());
	}

	@Override
	public char[] getChars() {
		return this.regex;
	}

	@Override
	public int getCaptureCount() {
		return this.variableNames.size();
	}

	@Override
	public int getWildcardCount() {
		return this.wildcardCount;
	}

	@Override
	public int getScore() {
		return (getCaptureCount() * CAPTURE_VARIABLE_WEIGHT + getWildcardCount() * WILDCARD_WEIGHT);
	}


	@Override
	public String toString() {
		return "Regex(" + String.valueOf(this.regex) + ")";
	}

}
