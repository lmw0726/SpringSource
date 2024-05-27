/*
 * Copyright 2002-2019 the original author or authors.
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

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.util.pattern.PatternParseException.PatternMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.PatternSyntaxException;

/**
 * URI 模板模式的解析器。它将路径模式分解为一系列 {@link PathElement PathElements}，存储在链表中。
 * 实例可重用，但不是线程安全的。
 *
 * @author Andy Clement
 * @since 5.0
 */
class InternalPathPatternParser {
	/**
	 * 路径模式解析器
	 */
	private final PathPatternParser parser;

	/**
	 * 解析的输入数据
	 */
	private char[] pathPatternData = new char[0];

	/**
	 * 输入数据的长度
	 */
	private int pathPatternLength;

	/**
	 * 当前解析位置
	 */
	int pos;

	/**
	 * 在特定路径元素中的 ? 字符数
	 */
	private int singleCharWildcardCount;

	/**
	 * 在特定路径元素中是否使用 * 字符
	 */
	private boolean wildcard = false;

	/**
	 * 在特定路径元素中是否使用 {*...} 构造
	 */
	private boolean isCaptureTheRestVariable = false;

	/**
	 * 解析器是否进入了特定路径元素中的 {...} 变量捕获块
	 */
	private boolean insideVariableCapture = false;

	/**
	 * 在特定路径元素中发生的变量捕获数
	 */
	private int variableCaptureCount = 0;

	/**
	 * 特定路径元素中最近路径元素的起始位置。
	 */
	private int pathElementStart;

	/**
	 * 特定路径元素中最近变量捕获的起始位置。
	 */
	private int variableCaptureStart;

	/**
	 * 此路径模式中的变量捕获
	 */
	@Nullable
	private List<String> capturedVariableNames;

	/**
	 * 当前正在构建的路径元素链的头部
	 */
	@Nullable
	private PathElement headPE;

	/**
	 * 链中最近构造的路径元素
	 */
	@Nullable
	private PathElement currentPE;


	/**
	 * 用于 {@link PathPatternParser#parse} 中的包私有构造函数。
	 *
	 * @param parentParser 指向无状态的公共解析器的引用
	 */
	InternalPathPatternParser(PathPatternParser parentParser) {
		this.parser = parentParser;
	}


	/**
	 * {@link PathPatternParser#parse(String)} 的包私有委托。
	 */
	public PathPattern parse(String pathPattern) throws PatternParseException {
		Assert.notNull(pathPattern, "Path pattern must not be null");

		// 将路径模式转换为字符数组
		this.pathPatternData = pathPattern.toCharArray();
		// 记录路径模式的长度
		this.pathPatternLength = this.pathPatternData.length;
		// 初始化头部路径元素和当前路径元素
		this.headPE = null;
		this.currentPE = null;
		// 初始化捕获变量名称数组为空
		this.capturedVariableNames = null;
		// 记录路径元素的起始位置
		this.pathElementStart = -1;
		// 初始化位置指针为0
		this.pos = 0;
		// 重置路径元素状态
		resetPathElementState();

		// 遍历路径模式字符数组
		while (this.pos < this.pathPatternLength) {
			// 获取当前字符
			char ch = this.pathPatternData[this.pos];
			// 获取路径选项的分隔符
			char separator = this.parser.getPathOptions().separator();
			// 如果当前字符是分隔符
			if (ch == separator) {
				// 如果路径元素已开始，则创建路径元素并压入栈
				if (this.pathElementStart != -1) {
					pushPathElement(createPathElement());
				}
				// 如果下一个字符是双通配符，则创建双通配符路径元素并压入栈
				if (peekDoubleWildcard()) {
					pushPathElement(new WildcardTheRestPathElement(this.pos, separator));
					this.pos += 2;
				} else {
					// 否则创建分隔符路径元素并压入栈
					pushPathElement(new SeparatorPathElement(this.pos, separator));
				}
			} else {
				// 如果路径元素未开始，则记录起始位置
				if (this.pathElementStart == -1) {
					this.pathElementStart = this.pos;
				}
				// 处理特殊字符
				if (ch == '?') {
					// 当前字符是 ‘?’，‘？’字符数量+1
					this.singleCharWildcardCount++;
				} else if (ch == '{') {
					// 如果已在变量捕获内部，则抛出异常
					if (this.insideVariableCapture) {
						throw new PatternParseException(this.pos, this.pathPatternData,
								PatternMessage.ILLEGAL_NESTED_CAPTURE);
					}
					// 进入变量捕获模式
					this.insideVariableCapture = true;
					this.variableCaptureStart = this.pos;
				} else if (ch == '}') {
					// 如果不在变量捕获内部，则抛出异常
					if (!this.insideVariableCapture) {
						throw new PatternParseException(this.pos, this.pathPatternData,
								PatternMessage.MISSING_OPEN_CAPTURE);
					}
					// 退出变量捕获模式
					this.insideVariableCapture = false;
					// 如果是捕获剩余变量且后面还有字符，则抛出异常
					if (this.isCaptureTheRestVariable && (this.pos + 1) < this.pathPatternLength) {
						throw new PatternParseException(this.pos + 1, this.pathPatternData,
								PatternMessage.NO_MORE_DATA_EXPECTED_AFTER_CAPTURE_THE_REST);
					}
					// 记录变量捕获数量
					this.variableCaptureCount++;
				} else if (ch == ':') {
					// 如果在变量捕获内部且不是捕获剩余变量，则跳过捕获正则表达式
					if (this.insideVariableCapture && !this.isCaptureTheRestVariable) {
						skipCaptureRegex();
						this.insideVariableCapture = false;
						this.variableCaptureCount++;
					}
				} else if (ch == '*') {
					// 如果在变量捕获内部且当前字符前面是 '{'，则设置为捕获剩余变量
					if (this.insideVariableCapture && this.variableCaptureStart == this.pos - 1) {
						this.isCaptureTheRestVariable = true;
					}
					// 标记为通配符
					this.wildcard = true;
				}
				// 检查变量捕获名称是否符合 Java 标识符规范
				if (this.insideVariableCapture) {
					if ((this.variableCaptureStart + 1 + (this.isCaptureTheRestVariable ? 1 : 0)) == this.pos &&
							!Character.isJavaIdentifierStart(ch)) {
						// 检查变量捕获名称的第一个字符是否为 Java 标识符的起始字符，如果不是则抛出异常
						throw new PatternParseException(this.pos, this.pathPatternData,
								PatternMessage.ILLEGAL_CHARACTER_AT_START_OF_CAPTURE_DESCRIPTOR,
								Character.toString(ch));
					} else if ((this.pos > (this.variableCaptureStart + 1 + (this.isCaptureTheRestVariable ? 1 : 0)) &&
							!Character.isJavaIdentifierPart(ch) && ch != '-')) {
						// 检查变量捕获名称中间的字符是否符合 Java 标识符规范，如果不是则抛出异常
						throw new PatternParseException(this.pos, this.pathPatternData,
								PatternMessage.ILLEGAL_CHARACTER_IN_CAPTURE_DESCRIPTOR,
								Character.toString(ch));
					}
				}
			}
			// 移动位置指针到下一个字符
			this.pos++;
		}
		// 如果路径元素未结束，则创建路径元素并压入栈
		if (this.pathElementStart != -1) {
			pushPathElement(createPathElement());
		}
		// 返回路径模式对象
		return new PathPattern(pathPattern, this.parser, this.headPE);
	}

	/**
	 * 刚刚碰到 ':' 并希望跳过该变量的正则表达式规范。pos 将指向 ':'，我们希望跳过直到 }。
	 * <p>
	 * 嵌套的 {...} 对不必转义：<tt>/abc/{var:x{1,2}}/def</tt>
	 * <p>转义的 } 将不会被视为正则表达式的结尾：<tt>/abc/{var:x\\{y:}/def</tt>
	 * <p>一个不应表示正则表达式结束的分隔符可以被转义：
	 */
	private void skipCaptureRegex() {
		// 移动到下一个字符位置
		this.pos++;
		// 记录正则表达式的起始位置
		int regexStart = this.pos;
		// 记录嵌套的 {...} 的深度
		int curlyBracketDepth = 0;
		// 记录前一个字符是否为反斜杠
		boolean previousBackslash = false;

		// 遍历路径模式字符数组
		while (this.pos < this.pathPatternLength) {
			char ch = this.pathPatternData[this.pos];
			// 如果当前字符为反斜杠且前一个字符不是反斜杠，则跳过
			if (ch == '\\' && !previousBackslash) {
				this.pos++;
				previousBackslash = true;
				continue;
			}
			// 如果当前字符为 '{' 且前一个字符不是反斜杠，则增加嵌套深度
			if (ch == '{' && !previousBackslash) {
				curlyBracketDepth++;
			} else if (ch == '}' && !previousBackslash) {
				// 如果当前字符为 '}' 且前一个字符不是反斜杠
				if (curlyBracketDepth == 0) {
					// 如果正则表达式的起始位置等于当前位置，则抛出异常
					if (regexStart == this.pos) {
						throw new PatternParseException(regexStart, this.pathPatternData,
								PatternMessage.MISSING_REGEX_CONSTRAINT);
					}
					return;
				}
				// 减少嵌套深度
				curlyBracketDepth--;
			}
			// 如果当前字符为路径分隔符且前一个字符不是反斜杠，则抛出异常
			if (ch == this.parser.getPathOptions().separator() && !previousBackslash) {
				throw new PatternParseException(this.pos, this.pathPatternData,
						PatternMessage.MISSING_CLOSE_CAPTURE);
			}
			// 移动到下一个字符位置
			this.pos++;
			// 重置前一个字符为非反斜杠状态
			previousBackslash = false;
		}

		// 如果遍历结束仍未找到与 '{' 匹配的 '}'，则抛出异常
		throw new PatternParseException(this.pos - 1, this.pathPatternData,
				PatternMessage.MISSING_CLOSE_CAPTURE);
	}

	/**
	 * 在处理分隔符后，快速查看是否后面跟着一个双通配符（仅作为最后一个路径元素）。
	 */
	private boolean peekDoubleWildcard() {
		// 如果当前位置加2等于路径模式长度
		if ((this.pos + 2) >= this.pathPatternLength) {
			return false;
		}
		if (this.pathPatternData[this.pos + 1] != '*' || this.pathPatternData[this.pos + 2] != '*') {
			// 如果后面两个字符不是 '**'，则返回false
			return false;
		}
		// 获取路径分隔符
		char separator = this.parser.getPathOptions().separator();
		// 如果当前位置加3小于路径模式长度且下一个字符为分隔符，则抛出异常
		if ((this.pos + 3) < this.pathPatternLength && this.pathPatternData[this.pos + 3] == separator) {
			throw new PatternParseException(this.pos, this.pathPatternData,
					PatternMessage.NO_MORE_DATA_EXPECTED_AFTER_CAPTURE_THE_REST);
		}
		// 判断当前位置加3是否等于路径模式长度，如果是则返回true，否则返回false
		return (this.pos + 3 == this.pathPatternLength);
	}

	/**
	 * 将路径元素推送到正在构建的链中。
	 *
	 * @param newPathElement 要添加的新路径元素
	 */
	private void pushPathElement(PathElement newPathElement) {
		if (newPathElement instanceof CaptureTheRestPathElement) {
			// 如果新路径元素是捕获剩余路径元素
			// 在这个东西前面必须有一个分隔符
			// currentPE 应该是一个 SeparatorPathElement
			if (this.currentPE == null) {
				// 如果当前路径元素为空，则将新路径元素设置为头部路径元素和当前路径元素
				this.headPE = newPathElement;
				this.currentPE = newPathElement;
			} else if (this.currentPE instanceof SeparatorPathElement) {
				// 如果当前路径元素是分隔符路径元素
				PathElement peBeforeSeparator = this.currentPE.prev;
				if (peBeforeSeparator == null) {
					// 如果分隔符前面没有其他元素，则将新路径元素设置为头部路径元素
					this.headPE = newPathElement;
					newPathElement.prev = null;
				} else {
					// 否则将新路径元素插入到分隔符前面的路径元素后面
					peBeforeSeparator.next = newPathElement;
					newPathElement.prev = peBeforeSeparator;
				}
				// 更新当前路径元素为新路径元素
				this.currentPE = newPathElement;
			} else {
				// 如果当前路径元素不是分隔符路径元素，则抛出异常
				throw new IllegalStateException("Expected SeparatorPathElement but was " + this.currentPE);
			}
		} else {
			// 如果新路径元素不是捕获剩余路径元素
			if (this.headPE == null) {
				// 如果头部路径元素为空，则将新路径元素设置为头部路径元素和当前路径元素
				this.headPE = newPathElement;
				this.currentPE = newPathElement;
			} else if (this.currentPE != null) {
				// 如果当前路径元素不为空，则将新路径元素插入到当前路径元素后面
				this.currentPE.next = newPathElement;
				newPathElement.prev = this.currentPE;
				// 更新当前路径元素为新路径元素
				this.currentPE = newPathElement;
			}
		}

		// 重置路径元素状态
		resetPathElementState();
	}

	private char[] getPathElementText() {
		// 创建路径元素文本字符数组
		char[] pathElementText = new char[this.pos - this.pathElementStart];
		// 将路径模式数据中的路径元素文本复制到新数组中
		System.arraycopy(this.pathPatternData, this.pathElementStart, pathElementText, 0,
				this.pos - this.pathElementStart);
		// 返回路径元素文本字符数组
		return pathElementText;
	}

	/**
	 * 使用自上一个路径元素处理以来积累的知识来确定要创建的路径元素类型。
	 *
	 * @return 新路径元素
	 */
	private PathElement createPathElement() {
		if (this.insideVariableCapture) {
			// 如果当前在捕获变量的内部但未闭合，则抛出解析异常
			throw new PatternParseException(this.pos, this.pathPatternData, PatternMessage.MISSING_CLOSE_CAPTURE);
		}

		PathElement newPE = null;
		// 获取分割字符
		char separator = this.parser.getPathOptions().separator();

		if (this.variableCaptureCount > 0) {
			// 如果捕获到了变量
			if (this.variableCaptureCount == 1 && this.pathElementStart == this.variableCaptureStart &&
					this.pathPatternData[this.pos - 1] == '}') {
				// 如果捕获到的变量数量为1，并且路径元素的起始位置等于变量捕获的起始位置，并且当前的数据为 ‘}’
				if (this.isCaptureTheRestVariable) {
					// 如果是 {*....} 模式，则创建对应的捕获剩余部分的路径元素
					newPE = new CaptureTheRestPathElement(
							this.pathElementStart, getPathElementText(), separator);
				} else {
					// 如果是对该元素的完全捕获（可能带有约束），例如：/foo/{abc}/，则创建捕获变量路径元素
					try {
						newPE = new CaptureVariablePathElement(this.pathElementStart, getPathElementText(),
								this.parser.isCaseSensitive(), separator);
					} catch (PatternSyntaxException pse) {
						// 如果正则表达式的语法异常，则抛出解析异常
						throw new PatternParseException(pse,
								findRegexStart(this.pathPatternData, this.pathElementStart) + pse.getIndex(),
								this.pathPatternData, PatternMessage.REGEX_PATTERN_SYNTAX_EXCEPTION);
					}
					// 记录捕获的变量名
					recordCapturedVariable(this.pathElementStart,
							((CaptureVariablePathElement) newPE).getVariableName());
				}
			} else {
				if (this.isCaptureTheRestVariable) {
					// 如果捕获的是整个路径，则不应该与其他构造混合，抛出解析异常
					throw new PatternParseException(this.pathElementStart, this.pathPatternData,
							PatternMessage.CAPTURE_ALL_IS_STANDALONE_CONSTRUCT);
				}
				// 如果是正则表达式模式，则创建正则表达式路径元素
				RegexPathElement newRegexSection = new RegexPathElement(this.pathElementStart,
						getPathElementText(), this.parser.isCaseSensitive(),
						this.pathPatternData, separator);
				// 记录捕获的变量名
				for (String variableName : newRegexSection.getVariableNames()) {
					recordCapturedVariable(this.pathElementStart, variableName);
				}
				newPE = newRegexSection;
			}
		} else {
			if (this.wildcard) {
				// 如果使用了 ‘*’ 字符
				if (this.pos - 1 == this.pathElementStart) {
					// 如果是通配符模式，则创建通配符路径元素
					newPE = new WildcardPathElement(this.pathElementStart, separator);
				} else {
					// 如果不是单独的通配符，则创建正则表达式路径元素
					newPE = new RegexPathElement(this.pathElementStart, getPathElementText(),
							this.parser.isCaseSensitive(), this.pathPatternData, separator);
				}
			} else if (this.singleCharWildcardCount != 0) {
				// 如果包含单字符通配符，则创建单字符通配符路径元素
				newPE = new SingleCharWildcardedPathElement(this.pathElementStart, getPathElementText(),
						this.singleCharWildcardCount, this.parser.isCaseSensitive(), separator);
			} else {
				// 否则，创建字面路径元素
				newPE = new LiteralPathElement(this.pathElementStart, getPathElementText(),
						this.parser.isCaseSensitive(), separator);
			}
		}

		return newPE;
	}

	/**
	 * 对于表示捕获变量的路径元素，定位约束模式。
	 * 假设有约束模式。
	 *
	 * @param data   完整的路径表达式，例如 /aaa/bbb/{ccc:...}
	 * @param offset 兴趣捕获模式的开始位置
	 * @return 表达式中的':'字符后面的索引，相对于整个表达式的开始
	 */
	private int findRegexStart(char[] data, int offset) {
		int pos = offset;
		while (pos < data.length) {
			// 在数据中查找下一个冒号的位置
			if (data[pos] == ':') {
				// 如果找到冒号，则返回其后一个位置
				return pos + 1;
			}
			pos++;
		}
		// 如果没有找到冒号，则返回-1
		return -1;
	}

	/**
	 * 重置在路径元素处理过程中计算的所有标志和位置标记。
	 */
	private void resetPathElementState() {
		this.pathElementStart = -1;
		this.singleCharWildcardCount = 0;
		this.insideVariableCapture = false;
		this.variableCaptureCount = 0;
		this.wildcard = false;
		this.isCaptureTheRestVariable = false;
		this.variableCaptureStart = -1;
	}

	/**
	 * 记录新的捕获变量。如果与现有变量冲突，则报告错误。
	 */
	private void recordCapturedVariable(int pos, String variableName) {
		if (this.capturedVariableNames == null) {
			// 如果捕获的变量名列表为空，则创建一个新的列表
			this.capturedVariableNames = new ArrayList<>();
		}
		if (this.capturedVariableNames.contains(variableName)) {
			// 如果已经捕获了相同的变量名，则抛出解析异常
			throw new PatternParseException(pos, this.pathPatternData,
					PatternMessage.ILLEGAL_DOUBLE_CAPTURE, variableName);
		}
		// 将新的变量名添加到捕获的变量名列表中
		this.capturedVariableNames.add(variableName);
	}

}
