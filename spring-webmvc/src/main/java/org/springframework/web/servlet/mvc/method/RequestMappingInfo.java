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

package org.springframework.web.servlet.mvc.method;

import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.PathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;
import org.springframework.web.servlet.mvc.condition.*;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.ServletRequestPathUtils;
import org.springframework.web.util.UrlPathHelper;
import org.springframework.web.util.pattern.PathPatternParser;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;

/**
 * 请求映射信息。是以下条件的组合：
 * <ol>
 * <li>{@link PathPatternsRequestCondition}，使用解析的 {@code PathPatterns}，或者
 * {@link PatternsRequestCondition}，使用通过 {@code PathMatcher} 的字符串模式
 * <li>{@link RequestMethodsRequestCondition}
 * <li>{@link ParamsRequestCondition}
 * <li>{@link HeadersRequestCondition}
 * <li>{@link ConsumesRequestCondition}
 * <li>{@link ProducesRequestCondition}
 * <li>{@code RequestCondition}（可选的自定义请求条件）
 * </ol>
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public final class RequestMappingInfo implements RequestCondition<RequestMappingInfo> {

	/**
	 * 空的路径模式请求条件
	 */
	private static final PathPatternsRequestCondition EMPTY_PATH_PATTERNS = new PathPatternsRequestCondition();

	/**
	 * 空的模式请求条件
	 */
	private static final PatternsRequestCondition EMPTY_PATTERNS = new PatternsRequestCondition();

	/**
	 * 空的请求方法请求条件
	 */
	private static final RequestMethodsRequestCondition EMPTY_REQUEST_METHODS = new RequestMethodsRequestCondition();

	/**
	 * 空的参数请求条件
	 */
	private static final ParamsRequestCondition EMPTY_PARAMS = new ParamsRequestCondition();

	/**
	 * 空的请求头请求条件
	 */
	private static final HeadersRequestCondition EMPTY_HEADERS = new HeadersRequestCondition();

	/**
	 * 空的消费条件
	 */
	private static final ConsumesRequestCondition EMPTY_CONSUMES = new ConsumesRequestCondition();

	/**
	 * 空的生产条件
	 */
	private static final ProducesRequestCondition EMPTY_PRODUCES = new ProducesRequestCondition();

	/**
	 * 空的自定义条件
	 */
	private static final RequestConditionHolder EMPTY_CUSTOM = new RequestConditionHolder(null);


	/**
	 * 名称
	 */
	@Nullable
	private final String name;

	/**
	 * 路径模式请求条件
	 */
	@Nullable
	private final PathPatternsRequestCondition pathPatternsCondition;

	/**
	 * 模式请求条件
	 */
	@Nullable
	private final PatternsRequestCondition patternsCondition;

	/**
	 * 请求方法请求条件
	 */
	private final RequestMethodsRequestCondition methodsCondition;

	/**
	 * 参数请求条件
	 */
	private final ParamsRequestCondition paramsCondition;

	/**
	 * 请求头请求条件
	 */
	private final HeadersRequestCondition headersCondition;

	/**
	 * 消费请求条件
	 */
	private final ConsumesRequestCondition consumesCondition;

	/**
	 * 生产请求条件
	 */
	private final ProducesRequestCondition producesCondition;

	/**
	 * 消费请求条件持有者
	 */
	private final RequestConditionHolder customConditionHolder;

	/**
	 * 哈希码
	 */
	private final int hashCode;

	/**
	 * 选项
	 */
	private final BuilderConfiguration options;


	/**
	 * 使用映射名称的完整构造函数。
	 *
	 * @deprecated 自 5.3 起，建议使用 {@link RequestMappingInfo.Builder} 通过 {@link #paths(String...)}。
	 */
	@Deprecated
	public RequestMappingInfo(@Nullable String name, @Nullable PatternsRequestCondition patterns,
							  @Nullable RequestMethodsRequestCondition methods, @Nullable ParamsRequestCondition params,
							  @Nullable HeadersRequestCondition headers, @Nullable ConsumesRequestCondition consumes,
							  @Nullable ProducesRequestCondition produces, @Nullable RequestCondition<?> custom) {
		// 如果是null，则使用的空的请求条件
		this(name, null,
				(patterns != null ? patterns : EMPTY_PATTERNS),
				(methods != null ? methods : EMPTY_REQUEST_METHODS),
				(params != null ? params : EMPTY_PARAMS),
				(headers != null ? headers : EMPTY_HEADERS),
				(consumes != null ? consumes : EMPTY_CONSUMES),
				(produces != null ? produces : EMPTY_PRODUCES),
				(custom != null ? new RequestConditionHolder(custom) : EMPTY_CUSTOM),
				new BuilderConfiguration());
	}

	/**
	 * 使用给定条件创建实例。
	 *
	 * @deprecated 自 5.3 起，建议使用 {@link RequestMappingInfo.Builder} 通过 {@link #paths(String...)}。
	 */
	@Deprecated
	public RequestMappingInfo(@Nullable PatternsRequestCondition patterns,
							  @Nullable RequestMethodsRequestCondition methods, @Nullable ParamsRequestCondition params,
							  @Nullable HeadersRequestCondition headers, @Nullable ConsumesRequestCondition consumes,
							  @Nullable ProducesRequestCondition produces, @Nullable RequestCondition<?> custom) {

		this(null, patterns, methods, params, headers, consumes, produces, custom);
	}

	/**
	 * 使用给定的自定义请求条件重新创建一个 RequestMappingInfo。
	 *
	 * @deprecated 自 5.3 起，建议使用 {@link #addCustomCondition(RequestCondition)}。
	 */
	@Deprecated
	public RequestMappingInfo(RequestMappingInfo info, @Nullable RequestCondition<?> customRequestCondition) {
		this(info.name, info.patternsCondition, info.methodsCondition, info.paramsCondition, info.headersCondition,
				info.consumesCondition, info.producesCondition, customRequestCondition);
	}

	private RequestMappingInfo(@Nullable String name,
							   @Nullable PathPatternsRequestCondition pathPatternsCondition,
							   @Nullable PatternsRequestCondition patternsCondition,
							   RequestMethodsRequestCondition methodsCondition, ParamsRequestCondition paramsCondition,
							   HeadersRequestCondition headersCondition, ConsumesRequestCondition consumesCondition,
							   ProducesRequestCondition producesCondition, RequestConditionHolder customCondition,
							   BuilderConfiguration options) {

		Assert.isTrue(pathPatternsCondition != null || patternsCondition != null,
				"Neither PathPatterns nor String patterns condition");

		this.name = (StringUtils.hasText(name) ? name : null);
		this.pathPatternsCondition = pathPatternsCondition;
		this.patternsCondition = patternsCondition;
		this.methodsCondition = methodsCondition;
		this.paramsCondition = paramsCondition;
		this.headersCondition = headersCondition;
		this.consumesCondition = consumesCondition;
		this.producesCondition = producesCondition;
		this.customConditionHolder = customCondition;
		this.options = options;

		this.hashCode = calculateHashCode(
				this.pathPatternsCondition, this.patternsCondition,
				this.methodsCondition, this.paramsCondition, this.headersCondition,
				this.consumesCondition, this.producesCondition, this.customConditionHolder);
	}


	/**
	 * 返回此映射的名称，或 {@code null}。
	 */
	@Nullable
	public String getName() {
		return this.name;
	}

	/**
	 * 返回在启用 {@link AbstractHandlerMapping#usesPathPatterns() 使用路径模式} 时使用的模式条件。
	 * <p>这与 {@link #getPatternsCondition()} 是相互排斥的，当一个返回 {@code null} 时，另一个返回一个实例。
	 *
	 * @see #getActivePatternsCondition()
	 * @since 5.3
	 */
	@Nullable
	public PathPatternsRequestCondition getPathPatternsCondition() {
		return this.pathPatternsCondition;
	}

	/**
	 * 返回当使用 {@link PathMatcher} 进行字符串模式匹配时的模式条件。
	 * <p>这与 {@link #getPathPatternsCondition()} 是相互排斥的，当一个返回 {@code null} 时，另一个返回一个实例。
	 */
	@Nullable
	public PatternsRequestCondition getPatternsCondition() {
		return this.patternsCondition;
	}

	/**
	 * 返回 {@link #getPathPatternsCondition()} 或 {@link #getPatternsCondition()}，取决于哪个不是 {@code null}。
	 *
	 * @since 5.3
	 */
	@SuppressWarnings("unchecked")
	public <T> RequestCondition<T> getActivePatternsCondition() {
		if (this.pathPatternsCondition != null) {
			// 如果路径模式条件不为空，则返回路径模式条件
			return (RequestCondition<T>) this.pathPatternsCondition;
		} else if (this.patternsCondition != null) {
			// 如果模式条件不为空，则返回模式条件
			return (RequestCondition<T>) this.patternsCondition;
		} else {
			// 如果两者都为空，这是一个不正常的状态，已经在构造函数中检查过
			throw new IllegalStateException();
		}
	}

	/**
	 * 返回不是模式的映射路径。
	 *
	 * @since 5.3
	 */
	public Set<String> getDirectPaths() {
		RequestCondition<?> condition = getActivePatternsCondition();
		return (condition instanceof PathPatternsRequestCondition ?
				// 如果条件是路径模式请求条件，则获取直接路径
				((PathPatternsRequestCondition) condition).getDirectPaths() :
				// 如果条件是模式请求条件，则获取直接路径
				((PatternsRequestCondition) condition).getDirectPaths());
	}

	/**
	 * 返回 {@link #getActivePatternsCondition() 激活的} 模式条件的模式作为字符串。
	 *
	 * @since 5.3
	 */
	public Set<String> getPatternValues() {
		RequestCondition<?> condition = getActivePatternsCondition();
		return (condition instanceof PathPatternsRequestCondition ?
				// 如果条件是路径模式请求条件，则获取模式值
				((PathPatternsRequestCondition) condition).getPatternValues() :
				// 如果条件是模式请求条件，则获取模式
				((PatternsRequestCondition) condition).getPatterns());
	}

	/**
	 * 返回此 {@link RequestMappingInfo} 的 HTTP 请求方法；
	 * 如果不存在请求方法，则返回一个包含 0 个请求方法的实例（永远不会为 {@code null}）。
	 */
	public RequestMethodsRequestCondition getMethodsCondition() {
		return this.methodsCondition;
	}

	/**
	 * 返回此 {@link RequestMappingInfo} 的 "parameters" 条件；
	 * 如果不存在参数，则返回一个包含 0 个参数表达式的实例（永远不会为 {@code null}）。
	 */
	public ParamsRequestCondition getParamsCondition() {
		return this.paramsCondition;
	}

	/**
	 * 返回此 {@link RequestMappingInfo} 的 "headers" 条件；
	 * 如果不存在头部表达式，则返回一个包含 0 个头部表达式的实例（永远不会为 {@code null}）。
	 */
	public HeadersRequestCondition getHeadersCondition() {
		return this.headersCondition;
	}

	/**
	 * 返回此 {@link RequestMappingInfo} 的 "consumes" 条件；
	 * 如果不存在消耗条件，则返回一个包含 0 个消耗表达式的实例（永远不会为 {@code null}）。
	 */
	public ConsumesRequestCondition getConsumesCondition() {
		return this.consumesCondition;
	}

	/**
	 * 返回此 {@link RequestMappingInfo} 的 "produces" 条件；
	 * 如果不存在生产条件，则返回一个包含 0 个生产表达式的实例（永远不会为 {@code null}）。
	 */
	public ProducesRequestCondition getProducesCondition() {
		return this.producesCondition;
	}

	/**
	 * 返回此 {@link RequestMappingInfo} 的 "custom" 条件，如果不存在则返回 {@code null}。
	 */
	@Nullable
	public RequestCondition<?> getCustomCondition() {
		return this.customConditionHolder.getCondition();
	}

	/**
	 * 根据当前实例创建一个新的实例，并添加给定的自定义条件。
	 *
	 * @param customCondition 要添加的自定义条件
	 * @since 5.3
	 */
	public RequestMappingInfo addCustomCondition(RequestCondition<?> customCondition) {
		return new RequestMappingInfo(this.name,
				this.pathPatternsCondition, this.patternsCondition,
				this.methodsCondition, this.paramsCondition, this.headersCondition,
				this.consumesCondition, this.producesCondition,
				new RequestConditionHolder(customCondition), this.options);
	}

	/**
	 * 将 "this" 请求映射信息（即当前实例）与另一个请求映射信息实例进行组合。
	 * <p>示例：组合类型级别和方法级别的请求映射。
	 *
	 * @return 一个新的请求映射信息实例；永远不会返回 {@code null}
	 */
	@Override
	public RequestMappingInfo combine(RequestMappingInfo other) {
		String name = combineNames(other);

		// 合并路径模式条件
		PathPatternsRequestCondition pathPatterns =
				(this.pathPatternsCondition != null && other.pathPatternsCondition != null ?
						this.pathPatternsCondition.combine(other.pathPatternsCondition) : null);

		// 合并模式条件
		PatternsRequestCondition patterns =
				(this.patternsCondition != null && other.patternsCondition != null ?
						this.patternsCondition.combine(other.patternsCondition) : null);

		// 合并请求方法条件
		RequestMethodsRequestCondition methods = this.methodsCondition.combine(other.methodsCondition);
		// 合并参数条件
		ParamsRequestCondition params = this.paramsCondition.combine(other.paramsCondition);
		// 合并标头条件
		HeadersRequestCondition headers = this.headersCondition.combine(other.headersCondition);
		// 合并消费条件
		ConsumesRequestCondition consumes = this.consumesCondition.combine(other.consumesCondition);
		// 合并生产条件
		ProducesRequestCondition produces = this.producesCondition.combine(other.producesCondition);
		// 合并自定义条件持有者
		RequestConditionHolder custom = this.customConditionHolder.combine(other.customConditionHolder);

		// 返回新的RequestMappingInfo对象
		return new RequestMappingInfo(name, pathPatterns, patterns,
				methods, params, headers, consumes, produces, custom, this.options);
	}

	@Nullable
	private String combineNames(RequestMappingInfo other) {
		if (this.name != null && other.name != null) {
			// 如果两个名称都不为空，则使用命名策略的分隔符连接它们
			String separator = RequestMappingInfoHandlerMethodMappingNamingStrategy.SEPARATOR;
			return this.name + separator + other.name;
		} else if (this.name != null) {
			// 如果当前名称不为空，则返回当前名称
			return this.name;
		} else {
			// 如果其他名称不为空，则返回其他名称
			return other.name;
		}
	}

	/**
	 * 检查此请求映射信息中的所有条件是否与提供的请求匹配，并返回一个根据当前请求定制条件的可能新的请求映射信息。
	 * <p>例如，返回的实例可能包含与当前请求匹配的 URL 模式的子集，并按照最佳匹配模式进行排序。
	 *
	 * @param request 当前请求的 HttpServletRequest 对象
	 * @return 在匹配情况下返回一个新的实例；否则返回 {@code null}
	 */
	@Override
	@Nullable
	public RequestMappingInfo getMatchingCondition(HttpServletRequest request) {
		RequestMethodsRequestCondition methods = this.methodsCondition.getMatchingCondition(request);
		if (methods == null) {
			// 如果方法请求条件为空，返回null
			return null;
		}
		ParamsRequestCondition params = this.paramsCondition.getMatchingCondition(request);
		if (params == null) {
			// 如果参数请求条件为空，返回null
			return null;
		}
		HeadersRequestCondition headers = this.headersCondition.getMatchingCondition(request);
		if (headers == null) {
			// 如果请求头请求条件为空，返回null
			return null;
		}
		ConsumesRequestCondition consumes = this.consumesCondition.getMatchingCondition(request);
		if (consumes == null) {
			// 如果消费请求条件为空，返回null
			return null;
		}
		ProducesRequestCondition produces = this.producesCondition.getMatchingCondition(request);
		if (produces == null) {
			// 如果生产请求条件为空，返回null
			return null;
		}
		PathPatternsRequestCondition pathPatterns = null;
		// 如果路径模式条件不为空
		if (this.pathPatternsCondition != null) {
			// 获取与请求匹配的路径模式条件
			pathPatterns = this.pathPatternsCondition.getMatchingCondition(request);
			if (pathPatterns == null) {
				// 如果路径模式请求条件为空，返回null
				return null;
			}
		}
		PatternsRequestCondition patterns = null;
		// 如果模式条件不为空
		if (this.patternsCondition != null) {
			// 获取与请求匹配的模式条件
			patterns = this.patternsCondition.getMatchingCondition(request);
			if (patterns == null) {
				// 如果模式条件为空，返回null
				return null;
			}
		}
		RequestConditionHolder custom = this.customConditionHolder.getMatchingCondition(request);
		if (custom == null) {
			// 如果自定义请求条件为空，返回null
			return null;
		}
		// 返回新的RequestMappingInfo对象
		return new RequestMappingInfo(this.name, pathPatterns, patterns,
				methods, params, headers, consumes, produces, custom, this.options);
	}

	/**
	 * 在请求的上下文中比较“此”信息（即当前实例）与另一个信息。
	 * <p>注意: 假设这两个实例都是通过 {@link #getMatchingCondition(HttpServletRequest)} 获得的，
	 * 以确保它们具有与当前请求相关的条件内容。
	 *
	 * @param other   另一个 RequestMappingInfo 实例
	 * @param request 当前请求的 HttpServletRequest 对象
	 * @return 比较结果的整数值
	 */
	@Override
	public int compareTo(RequestMappingInfo other, HttpServletRequest request) {
		int result;
		// 自动 vs 显式的 HTTP HEAD 映射
		if (HttpMethod.HEAD.matches(request.getMethod())) {
			result = this.methodsCondition.compareTo(other.getMethodsCondition(), request);
			if (result != 0) {
				return result;
			}
		}
		result = getActivePatternsCondition().compareTo(other.getActivePatternsCondition(), request);
		if (result != 0) {
			return result;
		}
		result = this.paramsCondition.compareTo(other.getParamsCondition(), request);
		if (result != 0) {
			return result;
		}
		result = this.headersCondition.compareTo(other.getHeadersCondition(), request);
		if (result != 0) {
			return result;
		}
		result = this.consumesCondition.compareTo(other.getConsumesCondition(), request);
		if (result != 0) {
			return result;
		}
		result = this.producesCondition.compareTo(other.getProducesCondition(), request);
		if (result != 0) {
			return result;
		}
		// 隐式（无方法） vs 显式的 HTTP 方法映射
		result = this.methodsCondition.compareTo(other.getMethodsCondition(), request);
		if (result != 0) {
			return result;
		}
		result = this.customConditionHolder.compareTo(other.customConditionHolder, request);
		if (result != 0) {
			return result;
		}
		return 0;
	}

	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof RequestMappingInfo)) {
			return false;
		}
		RequestMappingInfo otherInfo = (RequestMappingInfo) other;
		return (getActivePatternsCondition().equals(otherInfo.getActivePatternsCondition()) &&
				this.methodsCondition.equals(otherInfo.methodsCondition) &&
				this.paramsCondition.equals(otherInfo.paramsCondition) &&
				this.headersCondition.equals(otherInfo.headersCondition) &&
				this.consumesCondition.equals(otherInfo.consumesCondition) &&
				this.producesCondition.equals(otherInfo.producesCondition) &&
				this.customConditionHolder.equals(otherInfo.customConditionHolder));
	}

	@Override
	public int hashCode() {
		return this.hashCode;
	}

	@SuppressWarnings("ConstantConditions")
	private static int calculateHashCode(
			@Nullable PathPatternsRequestCondition pathPatterns, @Nullable PatternsRequestCondition patterns,
			RequestMethodsRequestCondition methods, ParamsRequestCondition params, HeadersRequestCondition headers,
			ConsumesRequestCondition consumes, ProducesRequestCondition produces, RequestConditionHolder custom) {

		return (pathPatterns != null ? pathPatterns : patterns).hashCode() * 31 +
				methods.hashCode() + params.hashCode() +
				headers.hashCode() + consumes.hashCode() + produces.hashCode() +
				custom.hashCode();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("{");
		// 如果方法条件不为空
		if (!this.methodsCondition.isEmpty()) {
			// 获取HTTP方法集合
			Set<RequestMethod> httpMethods = this.methodsCondition.getMethods();
			// 如果只有一个HTTP方法
			builder.append(httpMethods.size() == 1 ? httpMethods.iterator().next() : httpMethods);
		}

		// 模式条件永远不为空，至少有一个空路径""
		builder.append(' ').append(getActivePatternsCondition());

		// 如果参数条件不为空
		if (!this.paramsCondition.isEmpty()) {
			// 添加参数条件
			builder.append(", params ").append(this.paramsCondition);
		}
		// 如果标头条件不为空
		if (!this.headersCondition.isEmpty()) {
			// 添加标头条件
			builder.append(", headers ").append(this.headersCondition);
		}
		// 如果消费条件不为空
		if (!this.consumesCondition.isEmpty()) {
			// 添加消费条件
			builder.append(", consumes ").append(this.consumesCondition);
		}
		// 如果生产条件不为空
		if (!this.producesCondition.isEmpty()) {
			// 添加生产条件
			builder.append(", produces ").append(this.producesCondition);
		}
		// 如果自定义条件持有者不为空
		if (!this.customConditionHolder.isEmpty()) {
			// 添加自定义条件
			builder.append(", and ").append(this.customConditionHolder);
		}
		// 添加闭合括号
		builder.append('}');
		// 返回构建的字符串
		return builder.toString();
	}

	/**
	 * 返回一个构建器，通过修改当前实例来创建一个新的RequestMappingInfo。
	 *
	 * @return 一个用于创建新的、修改过的实例的构建器
	 * @since 5.3.4
	 */
	public Builder mutate() {
		return new MutateBuilder(this);
	}


	/**
	 * 使用给定的路径创建一个新的RequestMappingInfo.Builder。
	 *
	 * @param paths 要使用的路径
	 * @since 4.2
	 */
	public static Builder paths(String... paths) {
		return new DefaultBuilder(paths);
	}


	/**
	 * 定义用于创建 RequestMappingInfo 的构建器。
	 *
	 * @since 4.2
	 */
	public interface Builder {

		/**
		 * 设置 URL 路径模式。
		 */
		Builder paths(String... paths);

		/**
		 * 设置请求方法条件。
		 */
		Builder methods(RequestMethod... methods);

		/**
		 * 设置请求参数条件。
		 */
		Builder params(String... params);

		/**
		 * 设置头部条件。
		 * <p>默认情况下未设置。
		 */
		Builder headers(String... headers);

		/**
		 * 设置消费条件。
		 */
		Builder consumes(String... consumes);

		/**
		 * 设置生产条件。
		 */
		Builder produces(String... produces);

		/**
		 * 设置映射名称。
		 */
		Builder mappingName(String name);

		/**
		 * 设置要使用的自定义条件。
		 */
		Builder customCondition(RequestCondition<?> condition);

		/**
		 * 提供用于请求映射的其他配置。
		 */
		Builder options(BuilderConfiguration options);

		/**
		 * 构建 RequestMappingInfo。
		 */
		RequestMappingInfo build();
	}


	private static class DefaultBuilder implements Builder {

		/**
		 * 路径
		 */
		private String[] paths;

		/**
		 * 请求方法
		 */
		private RequestMethod[] methods = new RequestMethod[0];

		/**
		 * 请求参数
		 */
		private String[] params = new String[0];

		/**
		 * 请求头数组
		 */
		private String[] headers = new String[0];

		/**
		 * 消费函数
		 */
		private String[] consumes = new String[0];

		/**
		 * 生产函数
		 */
		private String[] produces = new String[0];

		/**
		 * 是否有内容类型
		 */
		private boolean hasContentType;

		/**
		 * 是否可接受
		 */
		private boolean hasAccept;

		/**
		 * 映射名称
		 */
		@Nullable
		private String mappingName;

		/**
		 * 自定义请求条件
		 */
		@Nullable
		private RequestCondition<?> customCondition;

		/**
		 * 可选项
		 */
		private BuilderConfiguration options = new BuilderConfiguration();

		public DefaultBuilder(String... paths) {
			this.paths = paths;
		}

		@Override
		public Builder paths(String... paths) {
			this.paths = paths;
			return this;
		}

		@Override
		public DefaultBuilder methods(RequestMethod... methods) {
			this.methods = methods;
			return this;
		}

		@Override
		public DefaultBuilder params(String... params) {
			this.params = params;
			return this;
		}

		@Override
		public DefaultBuilder headers(String... headers) {
			for (String header : headers) {
				// 检查是否包含Content-Type或content-type
				this.hasContentType = this.hasContentType ||
						header.contains("Content-Type") || header.contains("content-type");
				// 检查是否包含Accept或accept
				this.hasAccept = this.hasAccept ||
						header.contains("Accept") || header.contains("accept");
			}
			// 设置头部
			this.headers = headers;
			// 返回当前对象
			return this;
		}

		@Override
		public DefaultBuilder consumes(String... consumes) {
			this.consumes = consumes;
			return this;
		}

		@Override
		public DefaultBuilder produces(String... produces) {
			this.produces = produces;
			return this;
		}

		@Override
		public DefaultBuilder mappingName(String name) {
			this.mappingName = name;
			return this;
		}

		@Override
		public DefaultBuilder customCondition(RequestCondition<?> condition) {
			this.customCondition = condition;
			return this;
		}

		@Override
		public Builder options(BuilderConfiguration options) {
			this.options = options;
			return this;
		}

		@Override
		@SuppressWarnings("deprecation")
		public RequestMappingInfo build() {

			PathPatternsRequestCondition pathPatterns = null;
			PatternsRequestCondition patterns = null;

			if (this.options.patternParser != null) {
				// 如果模式解析器不为空
				pathPatterns = (ObjectUtils.isEmpty(this.paths) ?
						// 路径为空，则设置为一个空的路径模式请求条件
						EMPTY_PATH_PATTERNS :
						new PathPatternsRequestCondition(this.options.patternParser, this.paths));
			} else {
				// 如果模式解析器为空
				patterns = (ObjectUtils.isEmpty(this.paths) ?
						// 路径为空，则设置为一个空的模式请求条件
						EMPTY_PATTERNS :
						new PatternsRequestCondition(
								this.paths, null, this.options.getPathMatcher(),
								this.options.useSuffixPatternMatch(), this.options.useTrailingSlashMatch(),
								this.options.getFileExtensions()));
			}

			ContentNegotiationManager manager = this.options.getContentNegotiationManager();

			// 返回新的RequestMappingInfo对象
			return new RequestMappingInfo(
					this.mappingName, pathPatterns, patterns,
					ObjectUtils.isEmpty(this.methods) ?
							EMPTY_REQUEST_METHODS : new RequestMethodsRequestCondition(this.methods),
					ObjectUtils.isEmpty(this.params) ?
							EMPTY_PARAMS : new ParamsRequestCondition(this.params),
					ObjectUtils.isEmpty(this.headers) ?
							EMPTY_HEADERS : new HeadersRequestCondition(this.headers),
					ObjectUtils.isEmpty(this.consumes) && !this.hasContentType ?
							EMPTY_CONSUMES : new ConsumesRequestCondition(this.consumes, this.headers),
					ObjectUtils.isEmpty(this.produces) && !this.hasAccept ?
							EMPTY_PRODUCES : new ProducesRequestCondition(this.produces, this.headers, manager),
					this.customCondition != null ?
							new RequestConditionHolder(this.customCondition) : EMPTY_CUSTOM,
					this.options);
		}
	}


	private static class MutateBuilder implements Builder {

		/**
		 * 名称
		 */
		@Nullable
		private String name;

		/**
		 * 路径模式请求条件
		 */
		@Nullable
		private PathPatternsRequestCondition pathPatternsCondition;

		/**
		 * 模式请求条件
		 */
		@Nullable
		private PatternsRequestCondition patternsCondition;

		/**
		 * 请求方法请求条件
		 */
		private RequestMethodsRequestCondition methodsCondition;

		/**
		 * 参数请求条件
		 */
		private ParamsRequestCondition paramsCondition;

		/**
		 * 请求头请求条件
		 */
		private HeadersRequestCondition headersCondition;

		/**
		 * 消费请求条件
		 */
		private ConsumesRequestCondition consumesCondition;

		/**
		 * 生产请求条件
		 */
		private ProducesRequestCondition producesCondition;

		/**
		 * 自定义请求条件持有者
		 */
		private RequestConditionHolder customConditionHolder;

		/**
		 * 可选项
		 */
		private BuilderConfiguration options;

		public MutateBuilder(RequestMappingInfo other) {
			this.name = other.name;
			this.pathPatternsCondition = other.pathPatternsCondition;
			this.patternsCondition = other.patternsCondition;
			this.methodsCondition = other.methodsCondition;
			this.paramsCondition = other.paramsCondition;
			this.headersCondition = other.headersCondition;
			this.consumesCondition = other.consumesCondition;
			this.producesCondition = other.producesCondition;
			this.customConditionHolder = other.customConditionHolder;
			this.options = other.options;
		}

		@Override
		@SuppressWarnings("deprecation")
		public Builder paths(String... paths) {
			if (this.options.patternParser != null) {
				// 如果模式解析器不为空
				this.pathPatternsCondition = (ObjectUtils.isEmpty(paths) ?
						// 路径为空，则设置为一个空的模式请求条件
						EMPTY_PATH_PATTERNS : new PathPatternsRequestCondition(this.options.patternParser, paths));
			} else {
				// 如果模式解析器为空
				this.patternsCondition = (ObjectUtils.isEmpty(paths) ?
						// 路径为空，则设置为一个空的模式请求条件
						EMPTY_PATTERNS :
						new PatternsRequestCondition(
								paths, null, this.options.getPathMatcher(),
								this.options.useSuffixPatternMatch(), this.options.useTrailingSlashMatch(),
								this.options.getFileExtensions()));
			}
			// 返回当前对象
			return this;
		}

		@Override
		public Builder methods(RequestMethod... methods) {
			this.methodsCondition = (ObjectUtils.isEmpty(methods) ?
					EMPTY_REQUEST_METHODS : new RequestMethodsRequestCondition(methods));
			return this;
		}

		@Override
		public Builder params(String... params) {
			this.paramsCondition = (ObjectUtils.isEmpty(params) ?
					EMPTY_PARAMS : new ParamsRequestCondition(params));
			return this;
		}

		@Override
		public Builder headers(String... headers) {
			this.headersCondition = (ObjectUtils.isEmpty(headers) ?
					EMPTY_HEADERS : new HeadersRequestCondition(headers));
			return this;
		}

		@Override
		public Builder consumes(String... consumes) {
			this.consumesCondition = (ObjectUtils.isEmpty(consumes) ?
					EMPTY_CONSUMES : new ConsumesRequestCondition(consumes));
			return this;
		}

		@Override
		public Builder produces(String... produces) {
			this.producesCondition = (ObjectUtils.isEmpty(produces) ?
					EMPTY_PRODUCES :
					new ProducesRequestCondition(produces, null, this.options.getContentNegotiationManager()));
			return this;
		}

		@Override
		public Builder mappingName(String name) {
			this.name = name;
			return this;
		}

		@Override
		public Builder customCondition(RequestCondition<?> condition) {
			this.customConditionHolder = new RequestConditionHolder(condition);
			return this;
		}

		@Override
		public Builder options(BuilderConfiguration options) {
			this.options = options;
			return this;
		}

		@Override
		public RequestMappingInfo build() {
			return new RequestMappingInfo(this.name,
					this.pathPatternsCondition, this.patternsCondition,
					this.methodsCondition, this.paramsCondition, this.headersCondition,
					this.consumesCondition, this.producesCondition,
					this.customConditionHolder, this.options);
		}
	}


	/**
	 * 用于请求映射目的的配置选项容器。
	 * 此类配置用于创建 RequestMappingInfo 实例，但通常在所有 RequestMappingInfo 实例中都会使用。
	 *
	 * @see Builder#options
	 * @since 4.2
	 */
	public static class BuilderConfiguration {

		/**
		 * 路径模式解析器
		 */
		@Nullable
		private PathPatternParser patternParser;

		/**
		 * 路径匹配器
		 */
		@Nullable
		private PathMatcher pathMatcher;

		/**
		 * 是否在 模式请求条件 中应用尾部斜杠匹配。
		 */
		private boolean trailingSlashMatch = true;

		/**
		 * 是否在 模式请求条件 中应用后缀模式匹配。
		 */
		private boolean suffixPatternMatch = false;

		/**
		 * 设置后缀模式匹配是否仅限于注册的文件扩展名。
		 */
		private boolean registeredSuffixPatternMatch = false;

		/**
		 * 内容协商管理器
		 */
		@Nullable
		private ContentNegotiationManager contentNegotiationManager;


		/**
		 * 启用解析的 PathPattern 的使用，如 {@link AbstractHandlerMapping#setPatternParser(PathPatternParser)} 中所述。
		 * <p><strong>注意:</strong> 此属性与 {@link #setPathMatcher(PathMatcher)} 是互斥的。
		 * <p>默认情况下未启用。
		 *
		 * @since 5.3
		 */
		public void setPatternParser(@Nullable PathPatternParser patternParser) {
			this.patternParser = patternParser;
		}

		/**
		 * 返回 {@link #setPatternParser(PathPatternParser) 配置的} {@code PathPatternParser}，如果有的话。
		 *
		 * @since 5.3
		 */
		@Nullable
		public PathPatternParser getPatternParser() {
			return this.patternParser;
		}

		/**
		 * 设置用于 模式请求条件 的自定义 UrlPathHelper。
		 * <p>默认情况下未设置。
		 *
		 * @since 4.2.8
		 * @deprecated 从 5.3 开始，该路径是在外部解析并通过 {@link ServletRequestPathUtils#getCachedPathValue(ServletRequest)} 获取的
		 */
		@Deprecated
		public void setUrlPathHelper(@Nullable UrlPathHelper urlPathHelper) {
		}

		/**
		 * 返回已配置的 UrlPathHelper。
		 *
		 * @deprecated 从 5.3 开始，该路径是在外部解析并通过 {@link ServletRequestPathUtils#getCachedPathValue(ServletRequest)} 获取的；
		 * 此方法始终返回 {@link UrlPathHelper#defaultInstance}。
		 */
		@Nullable
		@Deprecated
		public UrlPathHelper getUrlPathHelper() {
			return UrlPathHelper.defaultInstance;
		}

		/**
		 * 设置用于 模式请求条件 的自定义 PathMatcher。
		 * <p>默认情况下未设置。
		 */
		public void setPathMatcher(@Nullable PathMatcher pathMatcher) {
			this.pathMatcher = pathMatcher;
		}

		/**
		 * 返回用于 模式请求条件 的自定义 PathMatcher，如果有的话。
		 */
		@Nullable
		public PathMatcher getPathMatcher() {
			return this.pathMatcher;
		}

		/**
		 * 设置是否在 模式请求条件 中应用尾部斜杠匹配。
		 * <p>默认情况下设置为 'true'。
		 */
		public void setTrailingSlashMatch(boolean trailingSlashMatch) {
			this.trailingSlashMatch = trailingSlashMatch;
		}

		/**
		 * 返回是否在 模式请求条件 中应用尾部斜杠匹配。
		 */
		public boolean useTrailingSlashMatch() {
			return this.trailingSlashMatch;
		}

		/**
		 * 设置是否在 模式请求条件 中应用后缀模式匹配。
		 * <p>默认情况下设置为 'false'。
		 *
		 * @see #setRegisteredSuffixPatternMatch(boolean)
		 * @deprecated 从 5.2.4 开始。请参阅 {@link RequestMappingHandlerMapping#setUseSuffixPatternMatch(boolean)} 上的过时说明。
		 */
		@Deprecated
		public void setSuffixPatternMatch(boolean suffixPatternMatch) {
			this.suffixPatternMatch = suffixPatternMatch;
		}

		/**
		 * 返回是否在 模式请求条件 中应用后缀模式匹配。
		 *
		 * @deprecated 从 5.2.4 开始。请参阅 {@link RequestMappingHandlerMapping#setUseSuffixPatternMatch(boolean)} 上的过时说明。
		 */
		@Deprecated
		public boolean useSuffixPatternMatch() {
			return this.suffixPatternMatch;
		}

		/**
		 * 设置后缀模式匹配是否仅限于注册的文件扩展名。设置此属性还会设置 {@code suffixPatternMatch=true}，
		 * 并且要求配置了 {@link #setContentNegotiationManager} 以便获取已注册的文件扩展名。
		 *
		 * @deprecated 从 5.2.4 开始。请参阅 {@link RequestMappingHandlerMapping} 中关于路径扩展配置选项的类级注释的说明。
		 */
		@Deprecated
		public void setRegisteredSuffixPatternMatch(boolean registeredSuffixPatternMatch) {
			// 设置是否使用已注册的后缀模式匹配
			this.registeredSuffixPatternMatch = registeredSuffixPatternMatch;
			// 如果使用已注册的后缀模式匹配或者当前已设置为使用后缀模式匹配
			this.suffixPatternMatch = (registeredSuffixPatternMatch || this.suffixPatternMatch);
		}

		/**
		 * 返回后缀模式匹配是否仅限于注册的文件扩展名。
		 *
		 * @deprecated 从 5.2.4 开始。请参阅 {@link RequestMappingHandlerMapping} 中关于路径扩展配置选项的类级注释的说明。
		 */
		@Deprecated
		public boolean useRegisteredSuffixPatternMatch() {
			return this.registeredSuffixPatternMatch;
		}

		/**
		 * 返回要用于后缀模式匹配的文件扩展名。如果 {@code registeredSuffixPatternMatch=true}，则从配置的 {@code内容协商管理器} 获取扩展名。
		 *
		 * @deprecated 从 5.2.4 开始。请参阅 {@link RequestMappingHandlerMapping} 中关于路径扩展配置选项的类级注释的说明。
		 */
		@Nullable
		@Deprecated
		public List<String> getFileExtensions() {
			// 如果使用已注册的后缀模式匹配，并且内容协商管理器不为 null
			if (useRegisteredSuffixPatternMatch() && this.contentNegotiationManager != null) {
				// 返回内容协商管理器中注册的所有文件扩展名
				return this.contentNegotiationManager.getAllFileExtensions();
			}
			// 否则返回 null
			return null;
		}

		/**
		 * 设置用于 生产请求条件 的内容协商管理器。
		 * <p>默认情况下未设置。
		 */
		public void setContentNegotiationManager(ContentNegotiationManager contentNegotiationManager) {
			this.contentNegotiationManager = contentNegotiationManager;
		}

		/**
		 * 返回用于 生产请求条件 的内容协商管理器，如果有的话。
		 */
		@Nullable
		public ContentNegotiationManager getContentNegotiationManager() {
			return this.contentNegotiationManager;
		}
	}

}
