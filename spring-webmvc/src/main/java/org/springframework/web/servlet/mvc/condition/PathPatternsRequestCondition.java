/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.web.servlet.mvc.condition;

import org.springframework.http.server.PathContainer;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.web.util.ServletRequestPathUtils;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 逻辑或 (' || ') 请求条件，用于将请求与一组 URL 路径模式匹配。
 *
 * <p>与 {@link PatternsRequestCondition} 不同，此条件使用解析过的 {@link PathPattern}
 * 而不是与 {@link org.springframework.util.AntPathMatcher AntPathMatcher} 进行字符串模式匹配。
 *
 * @author Rossen Stoyanchev
 * @since 5.3
 */
public final class PathPatternsRequestCondition extends AbstractRequestCondition<PathPatternsRequestCondition> {

	/**
	 * 空的路径模式集合
	 */
	private static final SortedSet<PathPattern> EMPTY_PATH_PATTERN =
			new TreeSet<>(Collections.singleton(new PathPatternParser().parse("")));
	/**
	 * 空的路径集合
	 */
	private static final Set<String> EMPTY_PATH = Collections.singleton("");

	/**
	 * 路径模式集合
	 */
	private final SortedSet<PathPattern> patterns;


	/**
	 * 默认构造函数，生成一个 {@code ""}（空路径）映射。
	 */
	public PathPatternsRequestCondition() {
		this(EMPTY_PATH_PATTERN);
	}

	/**
	 * 构造函数，使用指定的模式。
	 */
	public PathPatternsRequestCondition(PathPatternParser parser, String... patterns) {
		this(parse(parser, patterns));
	}

	private static SortedSet<PathPattern> parse(PathPatternParser parser, String... patterns) {
		// 如果没有指定路径模式或所有模式为空字符串
		if (patterns.length == 0 || (patterns.length == 1 && !StringUtils.hasText(patterns[0]))) {
			// 返回空路径模式
			return EMPTY_PATH_PATTERN;
		}
		// 创建一个有序集合用于存储解析后的路径模式
		SortedSet<PathPattern> result = new TreeSet<>();
		// 遍历所有指定的路径模式
		for (String path : patterns) {
			// 如果路径模式不为空且不以斜杠开头，则添加斜杠
			if (StringUtils.hasText(path) && !path.startsWith("/")) {
				path = "/" + path;
			}
			// 解析路径模式并添加到结果集合中
			result.add(parser.parse(path));
		}
		// 返回结果集合
		return result;
	}

	private PathPatternsRequestCondition(SortedSet<PathPattern> patterns) {
		this.patterns = patterns;
	}

	/**
	 * 返回此条件中的模式。如果只需要第一个（顶部）模式，请使用 {@link #getFirstPattern()}。
	 */
	public Set<PathPattern> getPatterns() {
		return this.patterns;
	}

	@Override
	protected Collection<PathPattern> getContent() {
		return this.patterns;
	}

	@Override
	protected String getToStringInfix() {
		return " || ";
	}

	/**
	 * 返回第一个模式。
	 */
	public PathPattern getFirstPattern() {
		return this.patterns.first();
	}

	/**
	 * 此条件是否是 ""（空路径）映射。
	 */
	public boolean isEmptyPathMapping() {
		return this.patterns == EMPTY_PATH_PATTERN;
	}

	/**
	 * 返回不是模式的映射路径。
	 */
	public Set<String> getDirectPaths() {
		// 如果是空的路径映射
		if (isEmptyPathMapping()) {
			// 返回空路径集合
			return EMPTY_PATH;
		}
		// 创建空的结果集合
		Set<String> result = Collections.emptySet();
		// 遍历所有路径模式
		for (PathPattern pattern : this.patterns) {
			// 如果模式没有模式语法
			if (!pattern.hasPatternSyntax()) {
				// 如果结果集合为空，则创建一个初始容量为 1 的 HashSet
				result = (result.isEmpty() ? new HashSet<>(1) : result);
				// 将模式字符串添加到结果集合中
				result.add(pattern.getPatternString());
			}
		}
		// 返回结果集合
		return result;
	}

	/**
	 * 返回 {@link #getPatterns()} 映射到字符串。
	 */
	public Set<String> getPatternValues() {
		return (isEmptyPathMapping() ? EMPTY_PATH :
				getPatterns().stream().map(PathPattern::getPatternString).collect(Collectors.toSet()));
	}

	/**
	 * 返回一个新实例，其中包含来自当前实例（“this”）和“other”实例的 URL 模式，如下所示：
	 * <ul>
	 * <li>如果两个实例都有模式，则使用“this”中的模式与“other”中的模式组合，使用 {@link PathPattern#combine(PathPattern)}。
	 * <li>如果只有一个实例有模式，则使用它们。
	 * <li>如果两个实例都没有模式，则使用空字符串（即 ""）。
	 * </ul>
	 */
	@Override
	public PathPatternsRequestCondition combine(PathPatternsRequestCondition other) {
		// 如果当前对象和另一个对象都是空路径映射
		if (isEmptyPathMapping() && other.isEmptyPathMapping()) {
			// 返回当前对象
			return this;
		} else if (other.isEmptyPathMapping()) {
			// 如果另一个对象是空路径映射，返回当前对象
			return this;
		} else if (isEmptyPathMapping()) {
			// 如果当前对象是空路径映射，返回另一个对象
			return other;
		} else {
			// 创建一个有序集合用于存储合并后的路径模式
			SortedSet<PathPattern> combined = new TreeSet<>();
			// 遍历当前对象的所有路径模式
			for (PathPattern pattern1 : this.patterns) {
				// 遍历另一个对象的所有路径模式
				for (PathPattern pattern2 : other.patterns) {
					// 将两个路径模式进行组合，并添加到合并集合中
					combined.add(pattern1.combine(pattern2));
				}
			}
			// 返回一个新的 PathPatternsRequestCondition 对象，其中包含合并后的路径模式
			return new PathPatternsRequestCondition(combined);
		}
	}

	/**
	 * 检查是否有任何模式与给定请求匹配，并返回一个保证包含匹配模式、排序的实例。
	 *
	 * @param request 当前请求
	 * @return 如果条件不包含模式，则返回相同实例；如果包含模式，则返回包含排序匹配模式的新实例；如果没有模式匹配，则返回 {@code null}。
	 */
	@Override
	@Nullable
	public PathPatternsRequestCondition getMatchingCondition(HttpServletRequest request) {
		// 获取请求中的路径，并将其解析为 PathContainer 对象，然后获取该路径在应用程序中的路径
		PathContainer path = ServletRequestPathUtils.getParsedRequestPath(request).pathWithinApplication();
		// 获取与路径匹配的所有路径模式
		SortedSet<PathPattern> matches = getMatchingPatterns(path);
		// 如果匹配到了路径模式，则返回一个新的 PathPatternsRequestCondition 对象，其中包含匹配的路径模式；否则返回 null
		return (matches != null ? new PathPatternsRequestCondition(matches) : null);
	}

	@Nullable
	private SortedSet<PathPattern> getMatchingPatterns(PathContainer path) {
		// 创建一个空的 TreeSet 用于存储匹配的路径模式
		TreeSet<PathPattern> result = null;
		// 遍历所有路径模式
		for (PathPattern pattern : this.patterns) {
			// 如果当前路径模式与给定的路径匹配
			if (pattern.matches(path)) {
				// 如果结果集合为空，则创建一个新的 TreeSet
				result = (result != null ? result : new TreeSet<>());
				// 将匹配的路径模式添加到结果集合中
				result.add(pattern);
			}
		}
		// 返回结果集合
		return result;
	}

	/**
	 * 根据它们包含的URL模式比较两个条件。
	 * 模式逐个比较，从顶部到底部。如果所有比较的模式都匹配相等，但一个实例具有更多模式，则认为它是一个更接近的匹配。
	 * <p>假设这两个实例都是通过 {@link #getMatchingCondition(HttpServletRequest)} 获取的，
	 * 以确保它们仅包含与请求匹配且以最佳匹配项排序的模式。
	 */
	@Override
	public int compareTo(PathPatternsRequestCondition other, HttpServletRequest request) {
		// 获取当前对象和另一个对象的迭代器
		Iterator<PathPattern> iterator = this.patterns.iterator();
		Iterator<PathPattern> iteratorOther = other.getPatterns().iterator();
		// 遍历两个迭代器，比较每个路径模式的优先级
		while (iterator.hasNext() && iteratorOther.hasNext()) {
			// 比较两个路径模式的优先级
			int result = PathPattern.SPECIFICITY_COMPARATOR.compare(iterator.next(), iteratorOther.next());
			// 如果优先级不同，则返回比较结果
			if (result != 0) {
				return result;
			}
		}
		// 如果当前对象的迭代器还有剩余元素，则当前对象的优先级较低
		if (iterator.hasNext()) {
			return -1;
		} else if (iteratorOther.hasNext()) {
			// 如果另一个对象的迭代器还有剩余元素，则当前对象的优先级较高
			return 1;
		} else {
			// 否则优先级相同
			return 0;
		}
	}

}
