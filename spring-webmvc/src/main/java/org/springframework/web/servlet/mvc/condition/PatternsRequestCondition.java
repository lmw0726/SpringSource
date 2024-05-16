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

package org.springframework.web.servlet.mvc.condition;

import org.springframework.lang.Nullable;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.ObjectUtils;
import org.springframework.util.PathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UrlPathHelper;
import org.springframework.web.util.pattern.PathPattern;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * 一个逻辑或 (' || ') 请求条件，用于将请求与一组 URL 路径模式进行匹配。
 *
 * <p>与 {@link PathPatternsRequestCondition} 相比，该条件使用字符串模式匹配，而不是已解析的 {@link PathPattern}s。
 * 使用 {@link org.springframework.util.AntPathMatcher AntPathMatcher} 进行匹配。
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class PatternsRequestCondition extends AbstractRequestCondition<PatternsRequestCondition> {

	/**
	 * 空的路径模式集合
	 */
	private final static Set<String> EMPTY_PATH_PATTERN = Collections.singleton("");

	/**
	 * 模式集合
	 */
	private final Set<String> patterns;

	/**
	 * 路径匹配器
	 */
	private final PathMatcher pathMatcher;

	/**
	 * 是否使用后缀模式匹配
	 */
	private final boolean useSuffixPatternMatch;

	/**
	 * 是否使用尾部斜杠匹配
	 */
	private final boolean useTrailingSlashMatch;

	/**
	 * 文件扩展名列表
	 */
	private final List<String> fileExtensions = new ArrayList<>();


	/**
	 * 使用 URL 模式创建一个新实例，如果必要，会在模式前加上 "/"。
	 *
	 * @param patterns 0 或多个 URL 模式；如果为 0，则该条件将匹配每个请求
	 */
	public PatternsRequestCondition(String... patterns) {
		this(patterns, true, null);
	}

	/**
	 * 使用 {@link PathMatcher} 和匹配尾部斜杠的标志创建一个 {@link #PatternsRequestCondition(String...)} 的变体。
	 *
	 * @since 5.3
	 */
	public PatternsRequestCondition(String[] patterns, boolean useTrailingSlashMatch,
									@Nullable PathMatcher pathMatcher) {

		this(patterns, null, pathMatcher, useTrailingSlashMatch);
	}

	/**
	 * 使用 {@link UrlPathHelper}、{@link PathMatcher} 和是否匹配尾部斜杠的标志创建一个 {@link #PatternsRequestCondition(String...)} 的变体。
	 * <p>从 5.3 开始，路径通过静态方法 {@link UrlPathHelper#getResolvedLookupPath} 获取，不需要传入 {@code UrlPathHelper}。
	 *
	 * @since 5.2.4
	 * @deprecated 从 5.3 开始，使用 {@link #PatternsRequestCondition(String[], boolean, PathMatcher)} 代替。
	 */
	@Deprecated
	public PatternsRequestCondition(String[] patterns, @Nullable UrlPathHelper urlPathHelper,
									@Nullable PathMatcher pathMatcher, boolean useTrailingSlashMatch) {

		this(patterns, urlPathHelper, pathMatcher, false, useTrailingSlashMatch);
	}

	/**
	 * 使用 {@link UrlPathHelper}、{@link PathMatcher}、是否使用后缀模式匹配的标志和是否匹配尾部斜杠的标志创建一个 {@link #PatternsRequestCondition(String...)} 的变体。
	 * <p>从 5.3 开始，路径通过静态方法 {@link UrlPathHelper#getResolvedLookupPath} 获取，不需要传入 {@code UrlPathHelper}。
	 *
	 * @deprecated 从 5.2.4 开始。请参阅 {@link org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping} 类级别的注释，了解有关路径扩展配置选项的废弃信息。
	 */
	@Deprecated
	public PatternsRequestCondition(String[] patterns, @Nullable UrlPathHelper urlPathHelper,
									@Nullable PathMatcher pathMatcher, boolean useSuffixPatternMatch,
									boolean useTrailingSlashMatch) {

		this(patterns, urlPathHelper, pathMatcher, useSuffixPatternMatch, useTrailingSlashMatch, null);
	}

	/**
	 * 使用 {@link UrlPathHelper}、{@link PathMatcher}、是否使用后缀模式匹配的标志、是否匹配尾部斜杠的标志和特定扩展名的标志创建一个 {@link #PatternsRequestCondition(String...)} 的变体。
	 * <p>从 5.3 开始，路径通过静态方法 {@link UrlPathHelper#getResolvedLookupPath} 获取，不需要传入 {@code UrlPathHelper}。
	 *
	 * @deprecated 从 5.2.4 开始。请参阅 {@link org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping} 类级别的注释，了解有关路径扩展配置选项的废弃信息。
	 */
	@Deprecated
	public PatternsRequestCondition(String[] patterns, @Nullable UrlPathHelper urlPathHelper,
									@Nullable PathMatcher pathMatcher, boolean useSuffixPatternMatch,
									boolean useTrailingSlashMatch, @Nullable List<String> fileExtensions) {

		// 初始化路径模式
		this.patterns = initPatterns(patterns);
		// 初始化路径匹配器
		this.pathMatcher = pathMatcher != null ? pathMatcher : new AntPathMatcher();
		// 设置是否使用后缀模式匹配
		this.useSuffixPatternMatch = useSuffixPatternMatch;
		// 设置是否使用尾部斜杠匹配
		this.useTrailingSlashMatch = useTrailingSlashMatch;

		// 如果文件扩展名数组不为空
		if (fileExtensions != null) {
			// 遍历文件扩展名数组
			for (String fileExtension : fileExtensions) {
				// 如果文件扩展名不以 "." 开头，则添加 "."
				if (fileExtension.charAt(0) != '.') {
					fileExtension = "." + fileExtension;
				}
				// 将文件扩展名添加到集合中
				this.fileExtensions.add(fileExtension);
			}
		}
	}

	private static Set<String> initPatterns(String[] patterns) {
		// 如果没有模式
		if (!hasPattern(patterns)) {
			// 返回空路径模式
			return EMPTY_PATH_PATTERN;
		}

		// 创建一个链式哈希集合，用于存储模式
		Set<String> result = new LinkedHashSet<>(patterns.length);
		// 遍历所有模式
		for (String pattern : patterns) {
			// 如果模式不为空且不以 "/" 开头，则添加 "/"
			if (StringUtils.hasLength(pattern) && !pattern.startsWith("/")) {
				pattern = "/" + pattern;
			}
			// 将模式添加到集合中
			result.add(pattern);
		}

		// 返回结果集合
		return result;
	}

	private static boolean hasPattern(String[] patterns) {
		// 如果模式数组不为空
		if (!ObjectUtils.isEmpty(patterns)) {
			// 遍历所有模式
			for (String pattern : patterns) {
				// 如果模式不为空白，则返回 true
				if (StringUtils.hasText(pattern)) {
					return true;
				}
			}
		}
		// 否则返回 false
		return false;
	}

	/**
	 * 用于组合和匹配时的私有构造函数。
	 */
	private PatternsRequestCondition(Set<String> patterns, PatternsRequestCondition other) {
		this.patterns = patterns;
		this.pathMatcher = other.pathMatcher;
		this.useSuffixPatternMatch = other.useSuffixPatternMatch;
		this.useTrailingSlashMatch = other.useTrailingSlashMatch;
		this.fileExtensions.addAll(other.fileExtensions);
	}


	public Set<String> getPatterns() {
		return this.patterns;
	}

	@Override
	protected Collection<String> getContent() {
		return this.patterns;
	}

	@Override
	protected String getToStringInfix() {
		return " || ";
	}

	/**
	 * 判断条件是否为空路径映射。
	 */
	public boolean isEmptyPathMapping() {
		return this.patterns == EMPTY_PATH_PATTERN;
	}

	/**
	 * 返回不是模式的映射路径。
	 *
	 * @since 5.3
	 */
	public Set<String> getDirectPaths() {
		// 如果是空路径映射
		if (isEmptyPathMapping()) {
			// 返回空路径模式
			return EMPTY_PATH_PATTERN;
		}

		// 创建空集合作为结果
		Set<String> result = Collections.emptySet();
		// 遍历所有模式
		for (String pattern : this.patterns) {
			// 如果模式不是通配符模式
			if (!this.pathMatcher.isPattern(pattern)) {
				// 如果结果集合为空，则创建一个新的 HashSet
				result = (result.isEmpty() ? new HashSet<>(1) : result);
				// 将模式添加到结果集合中
				result.add(pattern);
			}
		}

		// 返回结果集合
		return result;
	}

	/**
	 * 返回一个新实例，该实例包含当前实例 ("this") 和 "other" 实例中的 URL 模式，方式如下：
	 * <ul>
	 * <li>如果两个实例都有模式，则使用 {@link PathMatcher#combine(String, String)} 方法将 "this" 中的模式与 "other" 中的模式结合起来。
	 * <li>如果只有一个实例有模式，则使用它们。
	 * <li>如果两个实例都没有模式，则使用空字符串 (即 "")。
	 * </ul>
	 */
	@Override
	public PatternsRequestCondition combine(PatternsRequestCondition other) {
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
		}

		// 创建 LinkedHashSet 用于存储合并后的模式
		Set<String> result = new LinkedHashSet<>();
		// 如果当前对象和另一个对象的模式都不为空
		if (!this.patterns.isEmpty() && !other.patterns.isEmpty()) {
			// 遍历当前对象的模式
			for (String pattern1 : this.patterns) {
				// 遍历另一个对象的模式
				for (String pattern2 : other.patterns) {
					// 将两个模式进行组合，并添加到结果集合中
					result.add(this.pathMatcher.combine(pattern1, pattern2));
				}
			}
		}

		// 返回一个新的 PatternsRequestCondition 对象，其中包含合并后的模式
		return new PatternsRequestCondition(result, this);
	}

	/**
	 * 检查是否有任何模式匹配给定的请求，并返回一个确保包含匹配模式的实例，通过 {@link PathMatcher#getPatternComparator(String)} 进行排序。
	 * <p>通过以下顺序进行匹配模式的获取：
	 * <ul>
	 * <li>直接匹配
	 * <li>模式匹配，如果模式没有包含 "."，则会添加 ".*"
	 * <li>模式匹配
	 * <li>模式匹配，如果模式不以 "/" 结尾，则会添加 "/"
	 * </ul>
	 *
	 * @param request 当前请求
	 * @return 如果条件不包含模式，则返回相同的实例；如果包含模式但未匹配，则返回 null；否则返回一个新的实例，其中包含排序后的匹配模式
	 */
	@Override
	@Nullable
	public PatternsRequestCondition getMatchingCondition(HttpServletRequest request) {
		// 获取解析后的查找路径
		String lookupPath = UrlPathHelper.getResolvedLookupPath(request);
		// 获取与查找路径匹配的所有模式
		List<String> matches = getMatchingPatterns(lookupPath);
		// 如果匹配到了模式
		if (!matches.isEmpty()) {
			// 创建一个新的 PatternsRequestCondition 对象，其中包含匹配的模式
			return new PatternsRequestCondition(new LinkedHashSet<>(matches), this);
		} else {
			// 否则返回 null
			return null;
		}
	}

	/**
	 * 查找与给定查找路径匹配的模式。调用此方法应产生与调用 {@link #getMatchingCondition} 等效的结果。
	 * 提供此方法是为了在没有请求可用时（例如，内省、工具等）使用。
	 *
	 * @param lookupPath 要匹配现有模式的查找路径
	 * @return 一组匹配的模式，按匹配度排序，最佳匹配位于顶部
	 */
	public List<String> getMatchingPatterns(String lookupPath) {
		// 初始化匹配模式的列表
		List<String> matches = null;
		// 遍历当前对象的所有模式
		for (String pattern : this.patterns) {
			// 获取与当前模式匹配的路径
			String match = getMatchingPattern(pattern, lookupPath);
			// 如果匹配到了路径
			if (match != null) {
				// 如果匹配列表为空，则创建一个新的 ArrayList
				matches = (matches != null ? matches : new ArrayList<>());
				// 将匹配的路径添加到匹配列表中
				matches.add(match);
			}
		}

		// 如果匹配列表为空
		if (matches == null) {
			// 返回空列表
			return Collections.emptyList();
		}
		// 如果匹配列表中有多个匹配项
		if (matches.size() > 1) {
			// 使用路径匹配器的模式比较器对匹配列表进行排序
			matches.sort(this.pathMatcher.getPatternComparator(lookupPath));
		}
		// 返回匹配列表
		return matches;
	}

	@Nullable
	private String getMatchingPattern(String pattern, String lookupPath) {
		// 如果模式与查找路径完全匹配，则返回模式
		if (pattern.equals(lookupPath)) {
			return pattern;
		}

		// 如果启用了后缀模式匹配
		if (this.useSuffixPatternMatch) {
			// 如果文件扩展名集合不为空，并且查找路径包含 . 符号
			if (!this.fileExtensions.isEmpty() && lookupPath.indexOf('.') != -1) {
				// 遍历文件扩展名集合
				for (String extension : this.fileExtensions) {
					// 如果模式与查找路径加上文件扩展名匹配，则返回模式加上文件扩展名
					if (this.pathMatcher.match(pattern + extension, lookupPath)) {
						return pattern + extension;
					}
				}
			} else {
				// 否则，尝试使用通配符模式匹配
				// 判断模式是否有后缀
				boolean hasSuffix = pattern.indexOf('.') != -1;
				// 如果模式没有后缀，并且模式加上通配符匹配查找路径，则返回模式加上通配符
				if (!hasSuffix && this.pathMatcher.match(pattern + ".*", lookupPath)) {
					return pattern + ".*";
				}
			}
		}

		// 如果模式匹配查找路径，则返回模式
		if (this.pathMatcher.match(pattern, lookupPath)) {
			return pattern;
		}

		// 如果启用了尾部斜杠匹配
		if (this.useTrailingSlashMatch) {
			// 如果模式不以斜杠结尾，并且模式加上斜杠匹配查找路径
			if (!pattern.endsWith("/") && this.pathMatcher.match(pattern + "/", lookupPath)) {
				// 返回模式加上斜杠
				return pattern + "/";
			}
		}

		// 如果没有匹配到，则返回 null
		return null;
	}

	/**
	 * 根据它们包含的 URL 模式比较两个条件。
	 * 模式按顺序一次比较，从顶部到底部，通过 {@link PathMatcher#getPatternComparator(String)} 进行比较。
	 * 如果所有比较的模式都相等，但一个实例具有更多的模式，则将其视为更接近的匹配。
	 * <p>假设两个实例都是通过 {@link #getMatchingCondition(HttpServletRequest)} 获取的，因此每个实例只包含与请求匹配的模式，或者为空。
	 */
	@Override
	public int compareTo(PatternsRequestCondition other, HttpServletRequest request) {
		// 获取解析后的查找路径
		String lookupPath = UrlPathHelper.getResolvedLookupPath(request);
		// 获取路径比较器
		Comparator<String> patternComparator = this.pathMatcher.getPatternComparator(lookupPath);
		// 获取当前对象的模式迭代器
		Iterator<String> iterator = this.patterns.iterator();
		// 获取另一个对象的模式迭代器
		Iterator<String> iteratorOther = other.patterns.iterator();
		// 遍历当前对象的模式和另一个对象的模式
		while (iterator.hasNext() && iteratorOther.hasNext()) {
			// 比较当前对象的模式和另一个对象的模式
			int result = patternComparator.compare(iterator.next(), iteratorOther.next());
			// 如果比较结果不为 0，则返回结果
			if (result != 0) {
				return result;
			}
		}
		// 如果当前对象的模式迭代器还有剩余元素，则返回 -1
		if (iterator.hasNext()) {
			return -1;
		} else if (iteratorOther.hasNext()) {
			// 如果另一个对象的模式迭代器还有剩余元素，则返回 1
			return 1;
		} else { // 否则，返回 0
			return 0;
		}
	}

}
