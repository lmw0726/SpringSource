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
import org.springframework.http.server.PathContainer.Separator;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * 表示已解析的路径模式。包括一系列路径元素，用于快速匹配和积累计算状态，以便快速比较模式。
 *
 * <p>{@code PathPattern} 使用以下规则匹配 URL 路径：<br>
 * <ul>
 * <li>{@code ?} 匹配一个字符</li>
 * <li>{@code *} 匹配路径段内的零个或多个字符</li>
 * <li>{@code **} 匹配零个或多个<em>路径段</em>直到路径结束</li>
 * <li><code>{spring}</code> 匹配一个 <em>路径段</em> 并将其作为名为 "spring" 的变量捕获</li>
 * <li><code>{spring:[a-z]+}</code> 将正则表达式 {@code [a-z]+} 作为名为 "spring" 的路径变量匹配</li>
 * <li><code>{*spring}</code> 匹配零个或多个 <em>路径段</em> 直到路径结束，并将其作为名为 "spring" 的变量捕获</li>
 * </ul>
 *
 * <p><strong>注意：</strong> 与{@link org.springframework.util.AntPathMatcher}不同，{@code **}仅支持在模式的末尾。
 * 例如 {@code /pages/{**}} 是有效的，但 {@code /pages/{**}/details} 不是。相同的规则也适用于捕获变体 <code>{*spring}</code>。
 * 目的是消除在比较模式时的歧义。
 *
 * <h3>示例</h3>
 * <ul>
 * <li>{@code /pages/t?st.html} &mdash; 匹配 {@code /pages/test.html} 和 {@code /pages/tXst.html}，但不匹配 {@code /pages/toast.html}</li>
 * <li>{@code /resources/*.png} &mdash; 匹配 {@code resources} 目录中的所有 {@code .png} 文件</li>
 * <li><code>/resources/&#42;&#42;</code> &mdash; 匹配 {@code /resources/} 路径下的所有文件，包括 {@code /resources/image.png} 和 {@code /resources/css/spring.css}</li>
 * <li><code>/resources/{&#42;path}</code> &mdash; 匹配 {@code /resources/} 路径下的所有文件，
 * 以及 {@code /resources}，并将它们的相对路径捕获为名为 "path" 的变量；{@code /resources/image.png} 将匹配 "path" &rarr; "/image.png"，
 * 而 {@code /resources/css/spring.css} 将匹配 "path" &rarr; "/css/spring.css"</li>
 * <li><code>/resources/{filename:\\w+}.dat</code> 将匹配 {@code /resources/spring.dat}
 * 并将值 {@code "spring"} 分配给 {@code filename} 变量</li>
 * </ul>
 *
 * @author Andy Clement
 * @author Rossen Stoyanchev
 * @see PathContainer
 * @since 5.0
 */
public class PathPattern implements Comparable<PathPattern> {

	/**
	 * 空的路径容器
	 */
	private static final PathContainer EMPTY_PATH = PathContainer.parsePath("");

	/**
	 * 用于按特定性排序模式的比较器：
	 * <ol>
	 * <li>空实例位于最后。
	 * <li>最后一个通配符模式位于最后。
	 * <li>如果两个模式都是通配符模式，则考虑长度（较长者胜出）。
	 * <li>比较通配符和捕获变量计数（较小者胜出）。
	 * <li>考虑长度（较长者胜出）。
	 * </ol>
	 */
	public static final Comparator<PathPattern> SPECIFICITY_COMPARATOR =
			Comparator.nullsLast(
					Comparator.<PathPattern>
									comparingInt(p -> p.isCatchAll() ? 1 : 0)
							.thenComparingInt(p -> p.isCatchAll() ? scoreByNormalizedLength(p) : 0)
							.thenComparingInt(PathPattern::getScore)
							.thenComparingInt(PathPattern::scoreByNormalizedLength)
			);


	/**
	 * 解析后的模式文本。
	 */
	private final String patternString;

	/**
	 * 用于构建此模式的解析器。
	 */
	private final PathPatternParser parser;

	/**
	 * 用于解析模式的选项。
	 */
	private final PathContainer.Options pathOptions;

	/**
	 * 如果此模式没有尾部斜杠，则允许候选项包含一个斜杠并仍然成功匹配。
	 */
	private final boolean matchOptionalTrailingSeparator;

	/**
	 * 在解析时，此模式是否以区分大小写的方式匹配候选项。
	 */
	private final boolean caseSensitive;

	/**
	 * 此模式的解析的路径元素链中的第一个路径元素。
	 */
	@Nullable
	private final PathElement head;

	/**
	 * 此模式中捕获的变量数。
	 */
	private int capturedVariableCount;

	/**
	 * 标准化长度试图测量模式的“活动”部分。
	 * 它通过假设所有捕获的变量的标准化长度为1来计算。
	 * 实际上，这意味着更改变量名称的长度不会更改模式的活动部分的长度。
	 * 在比较两个模式时很有用。
	 */
	private int normalizedLength;

	/**
	 * 模式是否以“<分隔符>”结尾。
	 */
	private boolean endsWithSeparatorWildcard = false;

	/**
	 * 分数用于快速比较模式。
	 * 不同的模式组件被赋予不同的权重。
	 * “较低的分数”更具体。
	 * 当前权重：
	 * - 捕获的变量价值为1
	 * - 通配符价值为100
	 */
	private int score;

	/**
	 * 模式是否以 {*...} 结尾。
	 */
	private boolean catchAll = false;


	PathPattern(String patternText, PathPatternParser parser, @Nullable PathElement head) {
		this.patternString = patternText;
		this.parser = parser;
		this.pathOptions = parser.getPathOptions();
		this.matchOptionalTrailingSeparator = parser.isMatchOptionalTrailingSeparator();
		this.caseSensitive = parser.isCaseSensitive();
		this.head = head;

		// 计算用于快速比较的字段
		PathElement elem = head;
		while (elem != null) {
			// 统计捕获变量的数量
			this.capturedVariableCount += elem.getCaptureCount();
			// 计算规范化长度
			this.normalizedLength += elem.getNormalizedLength();
			// 计算得分
			this.score += elem.getScore();
			// 如果元素是捕获剩余部分或通配符捕获剩余部分，则设置标志为真
			if (elem instanceof CaptureTheRestPathElement || elem instanceof WildcardTheRestPathElement) {
				this.catchAll = true;
			}
			// 如果元素是分隔符，并且其下一个元素是通配符并且后面没有其他元素，则设置标志为真
			if (elem instanceof SeparatorPathElement && elem.next instanceof WildcardPathElement && elem.next.next == null) {
				this.endsWithSeparatorWildcard = true;
			}
			elem = elem.next;
		}
	}


	/**
	 * 返回解析为创建此 路径模式 的原始 String。
	 */
	public String getPatternString() {
		return this.patternString;
	}

	/**
	 * 返回模式字符串是否包含需要使用 {@link #matches(PathContainer)} 的模式语法，
	 * 或者是否是一个常规字符串，可以直接与其他字符串进行比较。
	 *
	 * @since 5.2
	 */
	public boolean hasPatternSyntax() {
		return (this.score > 0 || this.catchAll || this.patternString.indexOf('?') != -1);
	}

	/**
	 * 检查此模式是否与给定路径匹配。
	 *
	 * @param pathContainer 要尝试匹配的候选路径
	 * @return 如果路径与此模式匹配，则为 {@code true}
	 */
	public boolean matches(PathContainer pathContainer) {
		if (this.head == null) {
			// 如果头部为空， 如果路径容器的长度为0，则返回true；
			// 或者如果允许可选的尾部分隔符并且路径容器只是一个分隔符，则返回true。
			return !hasLength(pathContainer) ||
					(this.matchOptionalTrailingSeparator && pathContainerIsJustSeparator(pathContainer));
		} else if (!hasLength(pathContainer)) {
			if (this.head instanceof WildcardTheRestPathElement || this.head instanceof CaptureTheRestPathElement) {
				// 如果路径容器为空且头部为捕获剩余部分或通配符捕获剩余部分，则允许捕获变量绑定为空
				// 将允许 CaptureTheRest 将变量绑定为空
				pathContainer = EMPTY_PATH;
			} else {
				// 如果头部不是捕获剩余部分或通配符捕获剩余部分，且路径容器为空，则不匹配
				return false;
			}
		}
		// 创建匹配上下文
		MatchingContext matchingContext = new MatchingContext(pathContainer, false);
		// 使用头部路径元素进行匹配
		return this.head.matches(0, matchingContext);
	}

	/**
	 * 将此模式与给定 URI 路径匹配，并返回提取的 URI 模板变量以及路径参数（矩阵变量）。
	 *
	 * @param pathContainer 要尝试匹配的候选路径
	 * @return 带有提取的变量的信息对象，如果没有匹配，则为 {@code null}
	 */
	@Nullable
	public PathMatchInfo matchAndExtract(PathContainer pathContainer) {
		if (this.head == null) {
			// 如果头部为空
			// 如果路径容器的长度大于0，并且（不允许可选的尾部分隔符或者路径容器不只是一个分隔符），则返回null；
			// 否则返回空的路径匹配信息。
			return (hasLength(pathContainer) &&
					!(this.matchOptionalTrailingSeparator && pathContainerIsJustSeparator(pathContainer)) ?
					null : PathMatchInfo.EMPTY);
		} else if (!hasLength(pathContainer)) {
			// 如果路径容器为空
			if (this.head instanceof WildcardTheRestPathElement || this.head instanceof CaptureTheRestPathElement) {
				// 如果头部是通配符捕获剩余部分或捕获剩余部分，则允许捕获变量绑定为空
				// 将允许 CaptureTheRest 将变量绑定为空
				pathContainer = EMPTY_PATH;
			} else {
				// 如果头部不是通配符捕获剩余部分或捕获剩余部分，则返回null
				return null;
			}
		}
		// 创建匹配上下文
		MatchingContext matchingContext = new MatchingContext(pathContainer, true);
		// 使用头部路径元素进行匹配，并返回匹配结果
		return this.head.matches(0, matchingContext) ? matchingContext.getPathMatchResult() : null;
	}

	/**
	 * 匹配给定路径的开头，并返回未由此模式覆盖的剩余部分。
	 * 这对于在每个级别逐步匹配路径的嵌套路由非常有用。
	 *
	 * @param pathContainer 要尝试匹配的候选路径
	 * @return 匹配结果的信息对象，如果没有匹配，则为 {@code null}
	 */
	@Nullable
	public PathRemainingMatchInfo matchStartOfPath(PathContainer pathContainer) {
		if (this.head == null) {
			// 如果头部为空，则返回一个包含空路径和路径容器的路径剩余匹配信息对象
			return new PathRemainingMatchInfo(EMPTY_PATH, pathContainer);
		} else if (!hasLength(pathContainer)) {
			// 如果路径容器为空，则返回null
			return null;
		}

		// 创建匹配上下文
		MatchingContext matchingContext = new MatchingContext(pathContainer, true);
		// 设置允许额外的路径匹配
		matchingContext.setMatchAllowExtraPath();
		// 使用头部路径元素进行匹配
		boolean matches = this.head.matches(0, matchingContext);
		if (!matches) {
			// 如果不匹配，则返回null
			return null;
		} else {
			PathContainer pathMatched;
			PathContainer pathRemaining;
			if (matchingContext.remainingPathIndex == pathContainer.elements().size()) {
				// 如果匹配的索引等于路径容器的元素数量，则匹配的路径为整个路径容器，剩余路径为空
				pathMatched = pathContainer;
				pathRemaining = EMPTY_PATH;
			} else {
				// 否则，匹配的路径为从0到匹配索引的子路径，剩余路径为从匹配索引到结束的子路径
				pathMatched = pathContainer.subPath(0, matchingContext.remainingPathIndex);
				pathRemaining = pathContainer.subPath(matchingContext.remainingPathIndex);
			}
			// 返回路径剩余匹配信息对象
			return new PathRemainingMatchInfo(pathMatched, pathRemaining, matchingContext.getPathMatchResult());
		}
	}

	/**
	 * 确定给定路径的模式映射部分。
	 * <p>例如：<ul>
	 * <li>'{@code /docs/cvs/commit.html}' 和 '{@code /docs/cvs/commit.html} &rarr; ''</li>
	 * <li>'{@code /docs/*}' 和 '{@code /docs/cvs/commit}' &rarr; '{@code cvs/commit}'</li>
	 * <li>'{@code /docs/cvs/*.html}' 和 '{@code /docs/cvs/commit.html} &rarr; '{@code commit.html}'</li>
	 * <li>'{@code /docs/**}' 和 '{@code /docs/cvs/commit} &rarr; '{@code cvs/commit}'</li>
	 * </ul>
	 * <p><b>注意:</b>
	 * <ul>
	 * <li>假设 {@link #matches} 对于相同的路径返回 {@code true}，但不会强制执行此规定。
	 * <li>返回结果中的分隔符的重复出现将被移除
	 * <li>返回结果中的前导和尾随分隔符将被移除
	 * </ul>
	 *
	 * @param path 与此模式匹配的路径
	 * @return 由模式匹配的路径子集，如果没有由模式元素匹配的路径，则为 ""（空字符串）
	 */
	public PathContainer extractPathWithinPattern(PathContainer path) {
		// 获取路径中的元素列表
		List<Element> pathElements = path.elements();
		// 路径元素的数量
		int pathElementsCount = pathElements.size();

		// 起始索引初始化为0
		int startIndex = 0;
		// 查找第一个不是分隔符或字面值的路径元素（即第一个基于模式的元素）
		// 初始化 路径元素 为链表头部
		PathElement elem = this.head;
		// 遍历链表
		while (elem != null) {
			// 如果当前元素具有通配符或捕获计数，则跳出循环
			if (elem.getWildcardCount() != 0 || elem.getCaptureCount() != 0) {
				break;
			}
			// 移动到下一个元素并更新起始索引
			elem = elem.next;
			// 起始索引位置+1
			startIndex++;
		}
		// 如果没有找到模式片段，则返回一个空的路径容器
		if (elem == null) {
			// 没有模式片段
			return PathContainer.parsePath("");
		}

		// 跳过在结果中的前导分隔符
		while (startIndex < pathElementsCount && (pathElements.get(startIndex) instanceof Separator)) {
			// 在起始索引小于路径元素数量，且起始索引处的元素是分隔符时
			// 起始索引位置+1
			startIndex++;
		}

		// 结束索引初始化为路径元素的数量
		int endIndex = pathElements.size();
		// 跳过在结果中的尾随分隔符
		while (endIndex > 0 && (pathElements.get(endIndex - 1) instanceof Separator)) {
			// 在结束索引大于0且结束索引减一处的元素是分隔符时
			// 结束索引位置-1
			endIndex--;
		}

		// 检查是否存在多个相邻的分隔符
		boolean multipleAdjacentSeparators = false;
		// 遍历起始索引到结束索引之间的元素
		for (int i = startIndex; i < (endIndex - 1); i++) {
			if ((pathElements.get(i) instanceof Separator) && (pathElements.get(i + 1) instanceof Separator)) {
				// 如果当前元素及下一个元素都是分隔符，则将布尔值设置为true，并跳出循环
				multipleAdjacentSeparators = true;
				break;
			}
		}

		PathContainer resultPath = null;
		// 如果存在多个相邻的分隔符
		if (multipleAdjacentSeparators) {
			// 重建路径，去除重复的相邻分隔符
			// 创建一个StringBuilder对象，用于构建新的路径字符串
			StringBuilder sb = new StringBuilder();
			// 初始化索引为起始索引
			int i = startIndex;
			// 遍历起始索引到结束索引之间的元素
			while (i < endIndex) {
				// 获取当前索引处的元素
				Element e = pathElements.get(i++);
				// 将元素的值添加到StringBuilder中
				sb.append(e.value());
				// 如果当前元素是分隔符
				if (e instanceof Separator) {
					// 继续向后查找，直到找到不是分隔符的元素或达到结束索引
					while (i < endIndex && (pathElements.get(i) instanceof Separator)) {
						i++;
					}
				}
			}
			// 解析新构建的路径字符串为路径容器
			resultPath = PathContainer.parsePath(sb.toString(), this.pathOptions);
		} else if (startIndex >= endIndex) {
			// 如果起始索引大于等于结束索引，则返回一个空路径容器
			resultPath = PathContainer.parsePath("");
		} else {
			// 否则，返回路径的子路径
			resultPath = path.subPath(startIndex, endIndex);
		}
		return resultPath;
	}

	/**
	 * 与提供的模式比较此模式：如果此模式更具体，则返回 -1，0，+1。
	 * 目标是首先对更具体的模式进行排序。
	 */
	@Override
	public int compareTo(@Nullable PathPattern otherPattern) {
		int result = SPECIFICITY_COMPARATOR.compare(this, otherPattern);
		return (result == 0 && otherPattern != null ?
				this.patternString.compareTo(otherPattern.patternString) : result);
	}

	/**
	 * 将此模式与另一个模式组合。
	 */
	public PathPattern combine(PathPattern pattern2string) {
		// 如果其中一个为空，则结果为另一个。如果两者都为空，则结果为 ""
		if (!StringUtils.hasLength(this.patternString)) {
			// 如果当前模式字符串为空
			if (!StringUtils.hasLength(pattern2string.patternString)) {
				// 并且参数模式字符串也为空，则返回解析空字符串后的结果
				return this.parser.parse("");
			} else {
				// 如果参数模式字符串不为空，则直接返回参数模式
				return pattern2string;
			}
		} else if (!StringUtils.hasLength(pattern2string.patternString)) {
			// 如果参数模式字符串为空，则返回当前模式
			return this;
		}

		// 处理特殊情况的模式组合
		// /* + /hotel => /hotel
		// /*.* + /*.html => /*.html
		// 但是：
		// /usr + /user => /usr/user
		// /{foo} + /bar => /{foo}/bar
		if (!this.patternString.equals(pattern2string.patternString) && this.capturedVariableCount == 0 &&
				matches(PathContainer.parsePath(pattern2string.patternString))) {
			// 如果当前模式字符串与参数模式字符串不相等，并且当前模式没有捕获变量
			// 并且与参数模式匹配，则返回参数模式
			return pattern2string;
		}


		// /hotels/* + /booking => /hotels/booking
		// /hotels/* + booking => /hotels/booking
		if (this.endsWithSeparatorWildcard) {
			// 如果当前模式以分隔符通配符结尾，则将当前模式字符串的倒数第二个字符（即分隔符通配符）替换为参数模式字符串
			return this.parser.parse(concat(
					this.patternString.substring(0, this.patternString.length() - 2),
					pattern2string.patternString));
		}

		// /hotels + /booking => /hotels/booking
		// /hotels + booking => /hotels/booking
		// 是否有文件前缀/后缀需要考虑？
		int starDotPos1 = this.patternString.indexOf("*.");
		// 如果当前模式具有捕获变量，或者不包含通配符"*."，或者分隔符为"."
		if (this.capturedVariableCount != 0 || starDotPos1 == -1 || getSeparator() == '.') {
			// 直接将两个模式字符串连接起来
			return this.parser.parse(concat(this.patternString, pattern2string.patternString));
		}

		// 处理带有文件扩展名的情况
		// /*.html + /hotel => /hotel.html
		// /*.html + /hotel.* => /hotel.html
		// 查找当前模式字符串中的第一个文件扩展名
		String firstExtension = this.patternString.substring(starDotPos1 + 1);
		// 获取参数模式字符串中的文件名及扩展名
		String p2string = pattern2string.patternString;
		// 查找参数模式字符串中的第一个"."的位置
		int dotPos2 = p2string.indexOf('.');
		// 如果参数模式字符串中不包含"."，则文件名为整个参数模式字符串，扩展名为空字符串；
		// 否则，文件名为"."之前的部分，扩展名为"."之后的部分
		String file2 = (dotPos2 == -1 ? p2string : p2string.substring(0, dotPos2));
		String secondExtension = (dotPos2 == -1 ? "" : p2string.substring(dotPos2));
		// 判断当前模式字符串的扩展名是否为通配符（".*"或空字符串），以及参数模式字符串的扩展名是否为通配符
		boolean firstExtensionWild = (firstExtension.equals(".*") || firstExtension.isEmpty());
		boolean secondExtensionWild = (secondExtension.equals(".*") || secondExtension.isEmpty());

		if (!firstExtensionWild && !secondExtensionWild) {
			// 如果两个模式字符串都包含文件扩展名，则抛出异常
			throw new IllegalArgumentException(
					"Cannot combine patterns: " + this.patternString + " and " + pattern2string);
		}
		// 返回处理后的模式
		return this.parser.parse(file2 + (firstExtensionWild ? secondExtension : firstExtension));
	}

	@Override
	public boolean equals(@Nullable Object other) {
		if (!(other instanceof PathPattern)) {
			return false;
		}
		PathPattern otherPattern = (PathPattern) other;
		return (this.patternString.equals(otherPattern.getPatternString()) &&
				getSeparator() == otherPattern.getSeparator() &&
				this.caseSensitive == otherPattern.caseSensitive);
	}

	@Override
	public int hashCode() {
		return (this.patternString.hashCode() + getSeparator()) * 17 + (this.caseSensitive ? 1 : 0);
	}

	@Override
	public String toString() {
		return this.patternString;
	}


	int getScore() {
		return this.score;
	}

	boolean isCatchAll() {
		return this.catchAll;
	}

	/**
	 * 获取规范化长度，它试图衡量模式的“活动”部分。
	 * 它通过假设所有捕获变量的规范化长度为1来计算。
	 * 这有效地意味着更改变量名称的长度不会更改模式的活动部分的长度。
	 * 在比较两个模式时很有用。
	 */
	int getNormalizedLength() {
		return this.normalizedLength;
	}

	char getSeparator() {
		return this.pathOptions.separator();
	}

	int getCapturedVariableCount() {
		return this.capturedVariableCount;
	}

	String toChainString() {
		// 创建一个StringJoiner对象，用于将路径元素连接成一个字符串，以空格为分隔符
		StringJoiner stringJoiner = new StringJoiner(" ");
		PathElement pe = this.head;
		// 从链表头部开始遍历链表
		while (pe != null) {
			// 将每个路径元素的字符串表示添加到StringJoiner中
			stringJoiner.add(pe.toString());
			pe = pe.next;
		}
		// 返回连接后的字符串表示
		return stringJoiner.toString();
	}

	/**
	 * 返回从遍历路径元素链构建的模式的字符串形式。
	 *
	 * @return 模式的字符串形式
	 */
	String computePatternString() {
		// 创建一个StringBuilder对象，用于构建路径元素的字符表示的字符串
		StringBuilder sb = new StringBuilder();
		PathElement pe = this.head;
		// 从链表头部开始遍历链表
		while (pe != null) {
			// 将每个路径元素的字符表示添加到StringBuilder中
			sb.append(pe.getChars());
			pe = pe.next;
		}
		// 返回构建的字符串
		return sb.toString();
	}

	/**
	 * 获取头部路径元素。
	 *
	 * @return 头部路径元素
	 */
	@Nullable
	PathElement getHeadSection() {
		return this.head;
	}

	/**
	 * 将两个路径连接在一起，如果需要则包括分隔符。
	 * 如果第一个路径以分隔符结尾且第二个路径以分隔符开头，则删除多余的分隔符。
	 *
	 * @param path1 第一个路径
	 * @param path2 第二个路径
	 * @return 连接的路径，如果需要则包括分隔符
	 */
	private String concat(String path1, String path2) {
		// 检查路径1是否以分隔符结尾
		boolean path1EndsWithSeparator = (path1.charAt(path1.length() - 1) == getSeparator());
		// 检查路径2是否以分隔符开头
		boolean path2StartsWithSeparator = (path2.charAt(0) == getSeparator());

		if (path1EndsWithSeparator && path2StartsWithSeparator) {
			// 如果路径1以分隔符结尾，且路径2以分隔符开头
			// 则将路径1和路径2拼接起来，但只保留路径2的第一个分隔符之后的部分
			return path1 + path2.substring(1);
		} else if (path1EndsWithSeparator || path2StartsWithSeparator) {
			// 如果路径1或路径2其中一个以分隔符结尾或以分隔符开头，则直接将两个路径拼接起来
			return path1 + path2;
		} else {
			// 如果两个路径都不以分隔符结尾或以分隔符开头，则在路径1和路径2之间添加一个分隔符，并将它们拼接起来
			return path1 + getSeparator() + path2;
		}
	}

	/**
	 * 检查容器是否不为null并且具有多于零个元素。
	 *
	 * @param container 路径容器
	 * @return {@code true} 如果具有多于零个元素
	 */
	private boolean hasLength(@Nullable PathContainer container) {
		return container != null && container.elements().size() > 0;
	}

	private static int scoreByNormalizedLength(PathPattern pattern) {
		return -pattern.getNormalizedLength();
	}

	private boolean pathContainerIsJustSeparator(PathContainer pathContainer) {
		return pathContainer.value().length() == 1 &&
				pathContainer.value().charAt(0) == getSeparator();
	}


	/**
	 * 用于保存基于给定匹配路径的模式提取的URI变量和路径参数（矩阵变量）的持有者。
	 */
	public static class PathMatchInfo {

		/**
		 * 空的路径匹配信息
		 */
		private static final PathMatchInfo EMPTY = new PathMatchInfo(Collections.emptyMap(), Collections.emptyMap());

		/**
		 * URI变量
		 */
		private final Map<String, String> uriVariables;

		/**
		 * 矩阵变量
		 */
		private final Map<String, MultiValueMap<String, String>> matrixVariables;

		PathMatchInfo(Map<String, String> uriVars, @Nullable Map<String, MultiValueMap<String, String>> matrixVars) {
			this.uriVariables = Collections.unmodifiableMap(uriVars);
			this.matrixVariables = (matrixVars != null ?
					Collections.unmodifiableMap(matrixVars) : Collections.emptyMap());
		}

		/**
		 * 返回提取的URI变量。
		 */
		public Map<String, String> getUriVariables() {
			return this.uriVariables;
		}

		/**
		 * 返回每个路径段的矩阵变量映射，以URI变量名称为键。
		 */
		public Map<String, MultiValueMap<String, String>> getMatrixVariables() {
			return this.matrixVariables;
		}

		@Override
		public String toString() {
			return "PathMatchInfo[uriVariables=" + this.uriVariables + ", " +
					"matrixVariables=" + this.matrixVariables + "]";
		}
	}


	/**
	 * 用于保存模式的开头匹配结果的持有者。
	 * 提供对未与模式匹配的剩余路径以及在匹配的第一部分中绑定的任何变量的访问。
	 */
	public static class PathRemainingMatchInfo {

		/**
		 * 模式匹配的路径部分
		 */
		private final PathContainer pathMatched;

		/**
		 * 未与模式匹配的路径部分
		 */
		private final PathContainer pathRemaining;

		/**
		 * 路径匹配信息
		 */
		private final PathMatchInfo pathMatchInfo;


		PathRemainingMatchInfo(PathContainer pathMatched, PathContainer pathRemaining) {
			this(pathMatched, pathRemaining, PathMatchInfo.EMPTY);
		}

		PathRemainingMatchInfo(PathContainer pathMatched, PathContainer pathRemaining,
							   PathMatchInfo pathMatchInfo) {
			this.pathRemaining = pathRemaining;
			this.pathMatched = pathMatched;
			this.pathMatchInfo = pathMatchInfo;
		}

		/**
		 * 返回模式匹配的路径部分。
		 *
		 * @since 5.3
		 */
		public PathContainer getPathMatched() {
			return this.pathMatched;
		}

		/**
		 * 返回未与模式匹配的路径部分。
		 */
		public PathContainer getPathRemaining() {
			return this.pathRemaining;
		}

		/**
		 * 返回在成功匹配的路径部分中绑定的变量，或者返回一个空映射。
		 */
		public Map<String, String> getUriVariables() {
			return this.pathMatchInfo.getUriVariables();
		}

		/**
		 * 返回每个绑定变量的路径参数。
		 */
		public Map<String, MultiValueMap<String, String>> getMatrixVariables() {
			return this.pathMatchInfo.getMatrixVariables();
		}
	}


	/**
	 * 封装了在尝试进行匹配时的上下文。包括一些固定状态，比如当前正在考虑进行匹配的候选项，但也包括一些用于提取变量的累加器。
	 */
	class MatchingContext {

		/**
		 * 候选路径容器，用于进行匹配。
		 */
		final PathContainer candidate;

		/**
		 * 路径元素列表，表示当前正在考虑进行匹配的路径的元素。
		 */
		final List<Element> pathElements;

		/**
		 * 路径元素的数量。
		 */
		final int pathLength;

		/**
		 * 提取的URI变量的映射，其中键是变量名，值是对应的值。
		 */
		@Nullable
		private Map<String, String> extractedUriVariables;

		/**
		 * 提取的矩阵变量的映射，其中键是变量名，值是对应的矩阵变量。
		 */
		@Nullable
		private Map<String, MultiValueMap<String, String>> extractedMatrixVariables;

		/**
		 * 指示是否正在提取变量。
		 */
		boolean extractingVariables;

		/**
		 * 指示是否需要确定剩余路径。
		 */
		boolean determineRemainingPath = false;

		/**
		 * 如果determineRemainingPath为true，则将其设置为候选项中模式完成匹配的位置 - 即指向未被消耗的剩余路径
		 */
		int remainingPathIndex;

		public MatchingContext(PathContainer pathContainer, boolean extractVariables) {
			this.candidate = pathContainer;
			this.pathElements = pathContainer.elements();
			this.pathLength = this.pathElements.size();
			this.extractingVariables = extractVariables;
		}

		public void setMatchAllowExtraPath() {
			this.determineRemainingPath = true;
		}

		public boolean isMatchOptionalTrailingSeparator() {
			return matchOptionalTrailingSeparator;
		}

		public void set(String key, String value, MultiValueMap<String, String> parameters) {
			// 如果提取的URI变量映射为空，则创建一个新的HashMap对象
			if (this.extractedUriVariables == null) {
				this.extractedUriVariables = new HashMap<>();
			}
			// 将提取的键值对添加到提取的URI变量映射中
			this.extractedUriVariables.put(key, value);

			// 如果参数不为空
			if (!parameters.isEmpty()) {
				// 如果提取的矩阵变量映射为空，则创建一个新的HashMap对象
				if (this.extractedMatrixVariables == null) {
					this.extractedMatrixVariables = new HashMap<>();
				}
				// 将参数键值对添加到提取的矩阵变量映射中，使用不可修改的多值映射进行包装
				this.extractedMatrixVariables.put(key, CollectionUtils.unmodifiableMultiValueMap(parameters));
			}
		}

		public PathMatchInfo getPathMatchResult() {
			// 如果提取的URI变量映射为空，则返回空的路径匹配信息
			if (this.extractedUriVariables == null) {
				return PathMatchInfo.EMPTY;
			} else {
				// 否则，返回包含提取的URI变量映射和提取的矩阵变量映射的路径匹配信息对象
				return new PathMatchInfo(this.extractedUriVariables, this.extractedMatrixVariables);
			}
		}

		/**
		 * 返回指定索引处的元素是否为分隔符。
		 *
		 * @param pathIndex 分隔符可能的索引
		 * @return 如果元素是分隔符则返回 {@code true}
		 */
		boolean isSeparator(int pathIndex) {
			return this.pathElements.get(pathIndex) instanceof Separator;
		}

		/**
		 * 返回指定元素的解码值。
		 *
		 * @param pathIndex 路径元素索引
		 * @return 解码后的值
		 */
		String pathElementValue(int pathIndex) {
			// 获取路径索引处的路径元素，如果路径索引小于路径长度，则获取对应的元素，否则设为null
			Element element = (pathIndex < this.pathLength) ? this.pathElements.get(pathIndex) : null;
			// 如果元素是路径段对象，则返回其值以匹配
			if (element instanceof PathContainer.PathSegment) {
				return ((PathContainer.PathSegment) element).valueToMatch();
			}
			// 否则返回空字符串
			return "";
		}
	}

}
