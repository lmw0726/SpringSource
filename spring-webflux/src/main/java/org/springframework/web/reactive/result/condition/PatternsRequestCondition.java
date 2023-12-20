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

package org.springframework.web.reactive.result.condition;

import org.springframework.http.server.PathContainer;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.*;

/**
 * 逻辑或（' || '）请求条件，用于将请求与一组 URL 路径模式进行匹配。
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 5.0
 */
public final class PatternsRequestCondition extends AbstractRequestCondition<PatternsRequestCondition> {

	/**
	 * 表示空的路径模式集合
	 */
	private static final SortedSet<PathPattern> EMPTY_PATH_PATTERN =
			new TreeSet<>(Collections.singleton(PathPatternParser.defaultInstance.parse("")));

	/**
	 * 表示空的路径集合
	 */
	private static final Set<String> EMPTY_PATH = Collections.singleton("");

	/**
	 * URL路径模式的有序集合
	 */
	private final SortedSet<PathPattern> patterns;


	/**
	 * 使用给定的 URL 模式创建一个新实例。
	 *
	 * @param patterns 0 或更多个 URL 模式；如果为 0，则条件将与每个请求匹配。
	 */
	public PatternsRequestCondition(PathPattern... patterns) {
		this(ObjectUtils.isEmpty(patterns) ? Collections.emptyList() : Arrays.asList(patterns));
	}

	/**
	 * 使用给定的 URL 模式创建一个新实例。
	 */
	public PatternsRequestCondition(List<PathPattern> patterns) {
		this.patterns = (patterns.isEmpty() ? EMPTY_PATH_PATTERN : new TreeSet<>(patterns));
	}

	/**
	 * 使用给定的路径模式集合创建 PatternsRequestCondition 的私有构造函数。
	 *
	 * @param patterns 路径模式集合
	 */
	private PatternsRequestCondition(SortedSet<PathPattern> patterns) {
		this.patterns = patterns;
	}

	/**
	 * 获取路径模式集合。
	 *
	 * @return 路径模式集合
	 */
	public Set<PathPattern> getPatterns() {
		return this.patterns;
	}

	/**
	 * 获取路径模式集合。
	 *
	 * @return 路径模式集合
	 */
	@Override
	protected Collection<PathPattern> getContent() {
		return this.patterns;
	}


	/**
	 * 获取用于打印内容的离散项之间的符号。
	 *
	 * @return 用于打印的符号
	 */
	@Override
	protected String getToStringInfix() {
		return " || ";
	}


	/**
	 * 检查路径模式集合是否为空。
	 *
	 * @return 如果为空则为 true
	 */
	private boolean isEmptyPathMapping() {
		return this.patterns == EMPTY_PATH_PATTERN;
	}


	/**
	 * 返回不是模式的映射路径。
	 *
	 * @since 5.3
	 */
	public Set<String> getDirectPaths() {
		// 如果是空的路径映射
		if (isEmptyPathMapping()) {
			// 返回空的路径集合
			return EMPTY_PATH;
		}

		// 初始化结果集合为空集合
		Set<String> result = Collections.emptySet();

		// 对于每个路径模式
		for (PathPattern pattern : this.patterns) {
			// 如果模式不包含语法
			if (!pattern.hasPatternSyntax()) {
				// 如果结果集合为空，创建一个初始容量为1的 HashSet
				result = (result.isEmpty() ? new HashSet<>(1) : result);
				// 将模式字符串添加到结果集合中
				result.add(pattern.getPatternString());
			}
		}
		// 返回结果集合
		return result;
	}


	/**
	 * 返回一个新实例，其中包含当前实例（“this”）和“other”实例的URL模式，规则如下：
	 * <ul>
	 * <li>如果两个实例都有模式，则使用{@link PathPattern#combine(PathPattern)}将“this”中的模式与“other”中的模式组合起来。
	 * <li>如果只有一个实例有模式，则使用该实例的模式。
	 * <li>如果两个实例都没有模式，则使用空字符串（即""）。
	 * </ul>
	 */
	@Override
	public PatternsRequestCondition combine(PatternsRequestCondition other) {
		if (isEmptyPathMapping() && other.isEmptyPathMapping()) {
			return this;
		} else if (other.isEmptyPathMapping()) {
			return this;
		} else if (isEmptyPathMapping()) {
			return other;
		} else {
			// 创建一个排序的集合用于存储组合后的模式
			SortedSet<PathPattern> combined = new TreeSet<>();

			// 遍历当前实例中的模式
			for (PathPattern pattern1 : this.patterns) {
				// 遍历“other”实例中的模式
				for (PathPattern pattern2 : other.patterns) {
					// 将组合后的模式添加到集合中
					combined.add(pattern1.combine(pattern2));
				}
			}

			// 返回包含组合模式的新 PatternsRequestCondition 实例
			return new PatternsRequestCondition(combined);
		}
	}

	/**
	 * 检查是否有任何模式与给定的请求匹配，并返回一个确保包含匹配模式且已排序的实例。
	 *
	 * @param exchange 当前交换对象
	 * @return 如果条件不包含模式，则返回相同实例；
	 * 如果存在匹配的模式，则返回一个包含已排序匹配模式的新条件实例；
	 * 如果没有模式匹配，则返回{@code null}。
	 */
	@Override
	@Nullable
	public PatternsRequestCondition getMatchingCondition(ServerWebExchange exchange) {
		// 获取匹配的模式并进行排序
		SortedSet<PathPattern> matches = getMatchingPatterns(exchange);

		// 如果有匹配的模式，返回一个新的 PatternsRequestCondition 实例
		return (matches != null ? new PatternsRequestCondition(matches) : null);
	}

	/**
	 * 获取匹配的模式并进行排序。
	 *
	 * @param exchange 当前交换对象
	 * @return 如果有匹配的模式，则返回已排序的 TreeSet；否则返回{@code null}
	 */
	@Nullable
	private SortedSet<PathPattern> getMatchingPatterns(ServerWebExchange exchange) {
		// 获取请求中相对于应用程序的路径
		PathContainer lookupPath = exchange.getRequest().getPath().pathWithinApplication();

		// 用于存储匹配模式的 TreeSet
		TreeSet<PathPattern> result = null;

		// 对于每个模式
		for (PathPattern pattern : this.patterns) {
			// 如果模式匹配请求路径
			if (pattern.matches(lookupPath)) {
				// 如果结果集合为空，创建一个 TreeSet
				if (result == null) {
					result = new TreeSet<>();
				}
				// 将匹配的模式添加到结果集合中
				result.add(pattern);
			}
		}

		// 返回存储匹配模式的 TreeSet
		return result;
	}

	/**
	 * 基于它们包含的 URL 模式比较两个条件。
	 * 模式逐个比较，从顶部到底部。如果所有比较的模式都相等，但一个实例具有更多模式，则认为它是更接近的匹配。
	 * <p>假设两个实例都是通过{@link #getMatchingCondition(ServerWebExchange)}获取的，
	 * 确保它们仅包含与请求匹配并按最佳匹配排序的模式。
	 */
	@Override
	public int compareTo(PatternsRequestCondition other, ServerWebExchange exchange) {
		Iterator<PathPattern> iterator = this.patterns.iterator();
		Iterator<PathPattern> iteratorOther = other.getPatterns().iterator();
		while (iterator.hasNext() && iteratorOther.hasNext()) {
			// 比较两个模式的特异性
			int result = PathPattern.SPECIFICITY_COMPARATOR.compare(iterator.next(), iteratorOther.next());
			if (result != 0) {
				return result;
			}
		}
		if (iterator.hasNext()) {
			return -1;
		} else if (iteratorOther.hasNext()) {
			return 1;
		} else {
			return 0;
		}
	}

}
