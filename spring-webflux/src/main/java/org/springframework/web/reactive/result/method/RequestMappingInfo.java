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

package org.springframework.web.reactive.result.method;

import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;
import org.springframework.web.reactive.result.condition.*;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * RequestMappingInfo 类用于封装请求映射的信息，包括多个请求映射条件：
 * <ol>
 * <li>{@link PatternsRequestCondition}
 * <li>{@link RequestMethodsRequestCondition}
 * <li>{@link ParamsRequestCondition}
 * <li>{@link HeadersRequestCondition}
 * <li>{@link ConsumesRequestCondition}
 * <li>{@link ProducesRequestCondition}
 * <li>{@code RequestCondition}（可选的自定义请求条件）
 * </ol>
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public final class RequestMappingInfo implements RequestCondition<RequestMappingInfo> {

	/**
	 * 表示空路径模式的静态常量
	 */
	private static final PatternsRequestCondition EMPTY_PATTERNS = new PatternsRequestCondition();

	/**
	 * 表示空请求方法的静态常量
	 */
	private static final RequestMethodsRequestCondition EMPTY_REQUEST_METHODS = new RequestMethodsRequestCondition();

	/**
	 * 表示空请求参数的静态常量
	 */
	private static final ParamsRequestCondition EMPTY_PARAMS = new ParamsRequestCondition();

	/**
	 * 表示空请求头部的静态常量
	 */
	private static final HeadersRequestCondition EMPTY_HEADERS = new HeadersRequestCondition();

	/**
	 * 表示空内容类型的静态常量
	 */
	private static final ConsumesRequestCondition EMPTY_CONSUMES = new ConsumesRequestCondition();

	/**
	 * 表示空生产类型的静态常量
	 */
	private static final ProducesRequestCondition EMPTY_PRODUCES = new ProducesRequestCondition();

	/**
	 * 表示空自定义请求条件的静态常量
	 */
	private static final RequestConditionHolder EMPTY_CUSTOM = new RequestConditionHolder(null);


	/**
	 * 映射名称，可以为 null
	 */
	@Nullable
	private final String name;

	/**
	 * 匹配的路径模式条件
	 */
	private final PatternsRequestCondition patternsCondition;

	/**
	 * 匹配的请求方法条件
	 */
	private final RequestMethodsRequestCondition methodsCondition;

	/**
	 * 匹配的请求参数条件
	 */
	private final ParamsRequestCondition paramsCondition;

	/**
	 * 匹配的请求头部条件
	 */
	private final HeadersRequestCondition headersCondition;

	/**
	 * 匹配的内容类型条件
	 */
	private final ConsumesRequestCondition consumesCondition;

	/**
	 * 匹配的生产类型条件
	 */
	private final ProducesRequestCondition producesCondition;

	/**
	 * 自定义请求条件的持有者
	 */
	private final RequestConditionHolder customConditionHolder;

	/**
	 * 表示对象的哈希码
	 */
	private final int hashCode;

	/**
	 * 用于构建器配置的对象
	 */
	private final BuilderConfiguration options;


	/**
	 * 这是一个带有完整参数的构造函数，用于创建 RequestMappingInfo 实例，并接收映射名称以及多个请求映射条件作为参数。
	 * 不过它已被标记为过时（deprecated）。
	 *
	 * @param name     映射名称
	 * @param patterns 匹配的路径模式
	 * @param methods  匹配的请求方法
	 * @param params   匹配的请求参数
	 * @param headers  匹配的请求头部
	 * @param consumes 匹配的内容类型
	 * @param produces 匹配的生产类型
	 * @param custom   自定义的请求条件
	 * @deprecated 自 5.3.4 版本起，建议使用 {@link RequestMappingInfo.Builder} 通过 {@link #paths(String...)} 方法来构建。
	 */
	@Deprecated
	public RequestMappingInfo(@Nullable String name, @Nullable PatternsRequestCondition patterns,
							  @Nullable RequestMethodsRequestCondition methods, @Nullable ParamsRequestCondition params,
							  @Nullable HeadersRequestCondition headers, @Nullable ConsumesRequestCondition consumes,
							  @Nullable ProducesRequestCondition produces, @Nullable RequestCondition<?> custom) {
		// 调用另一个构造函数，并使用默认的 BuilderConfiguration
		this(name, patterns, methods, params, headers, consumes, produces, custom, new BuilderConfiguration());
	}


	/**
	 * 这个构造函数被标记为过时（deprecated），用于创建 RequestMappingInfo 的实例，并接收多个请求映射条件作为参数。
	 *
	 * @param patterns 匹配的路径模式
	 * @param methods  匹配的请求方法
	 * @param params   匹配的请求参数
	 * @param headers  匹配的请求头部
	 * @param consumes 匹配的内容类型
	 * @param produces 匹配的生产类型
	 * @param custom   自定义的请求条件
	 * @deprecated 自 5.3.4 版本起，建议使用 {@link RequestMappingInfo.Builder} 通过 {@link #paths(String...)} 方法来构建。
	 */
	@Deprecated
	public RequestMappingInfo(@Nullable PatternsRequestCondition patterns,
							  @Nullable RequestMethodsRequestCondition methods, @Nullable ParamsRequestCondition params,
							  @Nullable HeadersRequestCondition headers, @Nullable ConsumesRequestCondition consumes,
							  @Nullable ProducesRequestCondition produces, @Nullable RequestCondition<?> custom) {
		// 调用另一个构造函数，并将映射名称设置为 null
		this(null, patterns, methods, params, headers, consumes, produces, custom);
	}


	/**
	 * 这是一个已被标记为过时（deprecated）的构造函数，用于重新创建 RequestMappingInfo，指定自定义的请求条件。
	 *
	 * @param info                   原始的 RequestMappingInfo 实例
	 * @param customRequestCondition 自定义的请求条件
	 * @deprecated 自 5.3.4 版本起，建议使用  {@link Builder}  通过 {@link #mutate()} 方法来构建。
	 */
	@Deprecated
	public RequestMappingInfo(RequestMappingInfo info, @Nullable RequestCondition<?> customRequestCondition) {
		// 调用另一个构造函数，并传入原始 RequestMappingInfo 实例的信息以及自定义的请求条件
		this(info.name, info.patternsCondition, info.methodsCondition, info.paramsCondition, info.headersCondition,
				info.consumesCondition, info.producesCondition, customRequestCondition);
	}

	/**
	 * 私有构造函数，用于创建 RequestMappingInfo 的实例。
	 *
	 * @param name     映射名称
	 * @param patterns 匹配的路径模式
	 * @param methods  匹配的请求方法
	 * @param params   匹配的请求参数
	 * @param headers  匹配的请求头部
	 * @param consumes 匹配的内容类型
	 * @param produces 匹配的生产类型
	 * @param custom   自定义的请求条件
	 * @param options  构建器配置
	 */
	private RequestMappingInfo(@Nullable String name, @Nullable PatternsRequestCondition patterns,
							   @Nullable RequestMethodsRequestCondition methods, @Nullable ParamsRequestCondition params,
							   @Nullable HeadersRequestCondition headers, @Nullable ConsumesRequestCondition consumes,
							   @Nullable ProducesRequestCondition produces, @Nullable RequestCondition<?> custom,
							   BuilderConfiguration options) {
		// 初始化成员变量
		this.name = (StringUtils.hasText(name) ? name : null);
		this.patternsCondition = (patterns != null ? patterns : EMPTY_PATTERNS);
		this.methodsCondition = (methods != null ? methods : EMPTY_REQUEST_METHODS);
		this.paramsCondition = (params != null ? params : EMPTY_PARAMS);
		this.headersCondition = (headers != null ? headers : EMPTY_HEADERS);
		this.consumesCondition = (consumes != null ? consumes : EMPTY_CONSUMES);
		this.producesCondition = (produces != null ? produces : EMPTY_PRODUCES);
		this.customConditionHolder = (custom != null ? new RequestConditionHolder(custom) : EMPTY_CUSTOM);
		this.options = options;

		// 计算哈希码
		this.hashCode = calculateHashCode(
				this.patternsCondition, this.methodsCondition, this.paramsCondition, this.headersCondition,
				this.consumesCondition, this.producesCondition, this.customConditionHolder);
	}


	/**
	 * 获取映射的名称，如果没有设置名称则返回 null。
	 *
	 * @return 映射的名称，或者为 null
	 */
	@Nullable
	public String getName() {
		return this.name;
	}

	/**
	 * 获取此 {@link RequestMappingInfo} 的 URL 模式；
	 * 或者具有 0 个模式的实例，永远不会为 {@code null}。
	 */
	public PatternsRequestCondition getPatternsCondition() {
		return this.patternsCondition;
	}

	/**
	 * 返回不是模式的映射路径。
	 *
	 * @since 5.3
	 */
	public Set<String> getDirectPaths() {
		return this.patternsCondition.getDirectPaths();
	}

	/**
	 * 返回此 {@link RequestMappingInfo} 的 HTTP 请求方法；
	 * 或者具有 0 个请求方法的实例，永远不会为 {@code null}。
	 */
	public RequestMethodsRequestCondition getMethodsCondition() {
		return this.methodsCondition;
	}

	/**
	 * 返回此 {@link RequestMappingInfo} 的“参数”条件；
	 * 或者具有 0 个参数表达式的实例，永远不会为 {@code null}。
	 */
	public ParamsRequestCondition getParamsCondition() {
		return this.paramsCondition;
	}

	/**
	 * 返回此 {@link RequestMappingInfo} 的“头部”条件；
	 * 或者具有 0 个头部表达式的实例，永远不会为 {@code null}。
	 */
	public HeadersRequestCondition getHeadersCondition() {
		return this.headersCondition;
	}

	/**
	 * 返回此 {@link RequestMappingInfo} 的“消耗”条件；
	 * 或者具有 0 个消耗表达式的实例，永远不会为 {@code null}。
	 */
	public ConsumesRequestCondition getConsumesCondition() {
		return this.consumesCondition;
	}

	/**
	 * 返回此 {@link RequestMappingInfo} 的“生成”条件；
	 * 或者具有 0 个生成表达式的实例，永远不会为 {@code null}。
	 */
	public ProducesRequestCondition getProducesCondition() {
		return this.producesCondition;
	}

	/**
	 * 返回此 {@link RequestMappingInfo} 的“自定义”条件；或者 {@code null}。
	 */
	@Nullable
	public RequestCondition<?> getCustomCondition() {
		return this.customConditionHolder.getCondition();
	}


	/**
	 * 将“this”请求映射信息（即当前实例）与另一个请求映射信息实例合并。
	 * <p>示例：结合类型和方法级别的请求映射。
	 *
	 * @return 一个新的请求映射信息实例；永远不会为 {@code null}
	 */
	@Override
	public RequestMappingInfo combine(RequestMappingInfo other) {
		// 组合名称
		String name = combineNames(other);
		// 组合模式
		PatternsRequestCondition patterns = this.patternsCondition.combine(other.patternsCondition);
		// 组合请求方法
		RequestMethodsRequestCondition methods = this.methodsCondition.combine(other.methodsCondition);
		// 组合参数
		ParamsRequestCondition params = this.paramsCondition.combine(other.paramsCondition);
		// 组合头部
		HeadersRequestCondition headers = this.headersCondition.combine(other.headersCondition);
		// 组合消耗
		ConsumesRequestCondition consumes = this.consumesCondition.combine(other.consumesCondition);
		// 组合生成
		ProducesRequestCondition produces = this.producesCondition.combine(other.producesCondition);
		// 组合自定义条件
		RequestConditionHolder custom = this.customConditionHolder.combine(other.customConditionHolder);

		// 返回新的请求映射信息实例
		return new RequestMappingInfo(name, patterns,
				methods, params, headers, consumes, produces, custom.getCondition(), this.options);
	}

	/**
	 * 将两个请求映射信息的名称组合。
	 *
	 * @param other 另一个请求映射信息实例
	 * @return 组合后的名称；可能为 {@code null}
	 */
	@Nullable
	private String combineNames(RequestMappingInfo other) {
		if (this.name != null && other.name != null) {
			// 如果两个名称都不为 null，则以 "#" 连接两个名称
			return this.name + "#" + other.name;
		} else if (this.name != null) {
			// 如果当前实例的名称不为 null，返回当前实例的名称
			return this.name;
		} else {
			// 如果其他实例的名称不为 null，返回其他实例的名称
			return other.name;
		}
	}

	/**
	 * 检查此请求映射信息中的所有条件是否与提供的请求匹配，并返回一个可能包含针对当前请求定制条件的新的请求映射信息。
	 * <p>例如，返回的实例可能包含与当前请求匹配的 URL 模式子集，并按照最佳匹配模式进行排序。
	 *
	 * @param exchange 服务器 Web 交换对象
	 * @return 如果所有条件匹配，则返回一个新实例；否则返回 {@code null}
	 */
	@Override
	@Nullable
	public RequestMappingInfo getMatchingCondition(ServerWebExchange exchange) {
		// 获取与请求匹配的请求方法条件
		RequestMethodsRequestCondition methods = this.methodsCondition.getMatchingCondition(exchange);
		if (methods == null) {
			return null;
		}

		// 获取与请求匹配的参数条件
		ParamsRequestCondition params = this.paramsCondition.getMatchingCondition(exchange);
		if (params == null) {
			return null;
		}

		// 获取与请求匹配的头部条件
		HeadersRequestCondition headers = this.headersCondition.getMatchingCondition(exchange);
		if (headers == null) {
			return null;
		}

		// 在模式之前匹配“Content-Type”和“Accept”（解析和缓存的）
		ConsumesRequestCondition consumes = this.consumesCondition.getMatchingCondition(exchange);
		if (consumes == null) {
			return null;
		}

		ProducesRequestCondition produces = this.producesCondition.getMatchingCondition(exchange);
		if (produces == null) {
			return null;
		}

		// 获取与请求匹配的模式条件
		PatternsRequestCondition patterns = this.patternsCondition.getMatchingCondition(exchange);
		if (patterns == null) {
			return null;
		}

		// 获取与请求匹配的自定义条件
		RequestConditionHolder custom = this.customConditionHolder.getMatchingCondition(exchange);
		if (custom == null) {
			return null;
		}

		// 返回一个包含匹配条件的新的请求映射信息实例
		return new RequestMappingInfo(this.name, patterns,
				methods, params, headers, consumes, produces, custom.getCondition(), this.options);
	}

	/**
	 * 在请求的上下文中，比较“this”信息（即当前实例）与另一个信息。
	 * <p>注意：假设两个实例都是通过 {@link #getMatchingCondition(ServerWebExchange)} 获取的，
	 * 以确保它们具有与当前请求相关的条件内容。
	 */
	@Override
	public int compareTo(RequestMappingInfo other, ServerWebExchange exchange) {
		// 比较模式条件
		int result = this.patternsCondition.compareTo(other.getPatternsCondition(), exchange);
		if (result != 0) {
			return result;
		}

		// 比较参数条件
		result = this.paramsCondition.compareTo(other.getParamsCondition(), exchange);
		if (result != 0) {
			return result;
		}

		// 比较头部条件
		result = this.headersCondition.compareTo(other.getHeadersCondition(), exchange);
		if (result != 0) {
			return result;
		}

		// 比较消耗条件
		result = this.consumesCondition.compareTo(other.getConsumesCondition(), exchange);
		if (result != 0) {
			return result;
		}

		// 比较生成条件
		result = this.producesCondition.compareTo(other.getProducesCondition(), exchange);
		if (result != 0) {
			return result;
		}

		// 比较请求方法条件
		result = this.methodsCondition.compareTo(other.getMethodsCondition(), exchange);
		if (result != 0) {
			return result;
		}

		// 比较自定义条件
		result = this.customConditionHolder.compareTo(other.customConditionHolder, exchange);
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
		return (this.patternsCondition.equals(otherInfo.patternsCondition) &&
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

	/**
	 * 计算哈希码的静态方法。
	 *
	 * @param patterns URL 模式条件
	 * @param methods  请求方法条件
	 * @param params   参数条件
	 * @param headers  头部条件
	 * @param consumes 消耗条件
	 * @param produces 生成条件
	 * @param custom   自定义条件
	 * @return 计算得到的哈希码
	 */
	private static int calculateHashCode(
			PatternsRequestCondition patterns, RequestMethodsRequestCondition methods,
			ParamsRequestCondition params, HeadersRequestCondition headers,
			ConsumesRequestCondition consumes, ProducesRequestCondition produces,
			RequestConditionHolder custom) {

		return patterns.hashCode() * 31 + methods.hashCode() + params.hashCode() +
				headers.hashCode() + consumes.hashCode() + produces.hashCode() + custom.hashCode();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("{");
		if (!this.methodsCondition.isEmpty()) {
			Set<RequestMethod> httpMethods = this.methodsCondition.getMethods();
			builder.append(httpMethods.size() == 1 ? httpMethods.iterator().next() : httpMethods);
		}
		if (!this.patternsCondition.isEmpty()) {
			Set<PathPattern> patterns = this.patternsCondition.getPatterns();
			builder.append(' ').append(patterns.size() == 1 ? patterns.iterator().next() : patterns);
		}
		if (!this.paramsCondition.isEmpty()) {
			builder.append(", params ").append(this.paramsCondition);
		}
		if (!this.headersCondition.isEmpty()) {
			builder.append(", headers ").append(this.headersCondition);
		}
		if (!this.consumesCondition.isEmpty()) {
			builder.append(", consumes ").append(this.consumesCondition);
		}
		if (!this.producesCondition.isEmpty()) {
			builder.append(", produces ").append(this.producesCondition);
		}
		if (!this.customConditionHolder.isEmpty()) {
			builder.append(", and ").append(this.customConditionHolder);
		}
		builder.append('}');
		return builder.toString();
	}

	/**
	 * 返回一个构建器，通过修改当前实例来创建一个新的 RequestMappingInfo。
	 *
	 * @return 一个用于创建新的修改实例的构建器
	 * @since 5.3.4
	 */
	public Builder mutate() {
		return new MutateBuilder(this);
	}

	/**
	 * 使用给定的路径创建一个新的 {@code RequestMappingInfo.Builder}。
	 *
	 * @param paths 要使用的路径
	 */
	public static Builder paths(String... paths) {
		return new DefaultBuilder(paths);
	}


	/**
	 * 定义一个用于创建RequestMappingInfo的构建器接口。
	 */
	public interface Builder {

		/**
		 * 设置路径模式。
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
		 * <p>默认情况下，此项未设置。
		 */
		Builder headers(String... headers);

		/**
		 * 设置Consumes条件。
		 */
		Builder consumes(String... consumes);

		/**
		 * 设置Produces条件。
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
		 * 提供用于请求映射目的所需的额外配置。
		 */
		Builder options(BuilderConfiguration options);

		/**
		 * 构建RequestMappingInfo。
		 */
		RequestMappingInfo build();
	}


	/**
	 * DefaultBuilder是一个用于构建RequestMappingInfo对象的内部静态类，实现了Builder接口。
	 */
	private static class DefaultBuilder implements Builder {

		/**
		 * 请求映射的路径数组
		 */
		private String[] paths;

		/**
		 * 请求映射的请求方法数组
		 */
		@Nullable
		private RequestMethod[] methods;

		/**
		 * 请求映射的参数数组
		 */
		@Nullable
		private String[] params;

		/**
		 * 请求映射的头部数组
		 */
		@Nullable
		private String[] headers;

		/**
		 * 请求映射的Consumes数组
		 */
		@Nullable
		private String[] consumes;

		/**
		 * 请求映射的Produces数组
		 */
		@Nullable
		private String[] produces;

		/**
		 * 是否包含Content-Type头部
		 */
		private boolean hasContentType;

		/**
		 * 是否包含Accept头部
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
		 * 构建请求映射时的配置选项
		 */
		private BuilderConfiguration options = new BuilderConfiguration();

		/**
		 * 构造方法，通过传入路径数组初始化DefaultBuilder。
		 *
		 * @param paths 要设置的路径数组
		 */
		public DefaultBuilder(String... paths) {
			this.paths = paths;
		}

		/**
		 * 设置请求映射的路径数组。
		 *
		 * @param paths 要设置的路径数组
		 * @return 当前DefaultBuilder对象
		 */
		@Override
		public Builder paths(String... paths) {
			this.paths = paths;
			return this;
		}

		/**
		 * 设置请求映射的请求方法数组。
		 *
		 * @param methods 要设置的请求方法数组
		 * @return 当前DefaultBuilder对象
		 */
		@Override
		public DefaultBuilder methods(RequestMethod... methods) {
			this.methods = methods;
			return this;
		}

		/**
		 * 设置请求映射的参数数组。
		 *
		 * @param params 要设置的参数数组
		 * @return 当前DefaultBuilder对象
		 */
		@Override
		public DefaultBuilder params(String... params) {
			this.params = params;
			return this;
		}

		/**
		 * 设置请求映射的头部数组，并检查是否包含Content-Type和Accept头部。
		 *
		 * @param headers 要设置的头部数组
		 * @return 当前DefaultBuilder对象
		 */
		@Override
		public DefaultBuilder headers(String... headers) {
			for (String header : headers) {
				this.hasContentType = this.hasContentType ||
						header.contains("Content-Type") || header.contains("content-type");
				this.hasAccept = this.hasAccept ||
						header.contains("Accept") || header.contains("accept");
			}
			this.headers = headers;
			return this;
		}

		/**
		 * 设置请求映射的Consumes数组。
		 *
		 * @param consumes 要设置的Consumes数组
		 * @return 当前DefaultBuilder对象
		 */
		@Override
		public DefaultBuilder consumes(String... consumes) {
			this.consumes = consumes;
			return this;
		}

		/**
		 * 设置请求映射的Produces数组。
		 *
		 * @param produces 要设置的Produces数组
		 * @return 当前DefaultBuilder对象
		 */
		@Override
		public DefaultBuilder produces(String... produces) {
			this.produces = produces;
			return this;
		}

		/**
		 * 设置请求映射的名称。
		 *
		 * @param name 要设置的名称
		 * @return 当前DefaultBuilder对象
		 */
		@Override
		public DefaultBuilder mappingName(String name) {
			this.mappingName = name;
			return this;
		}

		/**
		 * 设置自定义的请求条件。
		 *
		 * @param condition 要设置的自定义请求条件
		 * @return 当前DefaultBuilder对象
		 */
		@Override
		public DefaultBuilder customCondition(RequestCondition<?> condition) {
			this.customCondition = condition;
			return this;
		}

		/**
		 * 设置构建请求映射时的配置选项。
		 *
		 * @param options 要设置的配置选项
		 * @return 当前DefaultBuilder对象
		 */
		@Override
		public Builder options(BuilderConfiguration options) {
			this.options = options;
			return this;
		}

		/**
		 * 构建并返回RequestMappingInfo对象。
		 *
		 * @return 构建的RequestMappingInfo对象
		 */
		@Override
		public RequestMappingInfo build() {
			// 从配置选项中获取路径模式解析器，如果为空则使用默认实例
			PathPatternParser parser = (this.options.getPatternParser() != null ?
					this.options.getPatternParser() : PathPatternParser.defaultInstance);

			// 从配置选项中获取请求内容类型解析器
			RequestedContentTypeResolver contentTypeResolver = this.options.getContentTypeResolver();

			// 构建RequestMappingInfo对象，根据条件是否为空来确定是否创建对应的条件对象
			return new RequestMappingInfo(this.mappingName,
					isEmpty(this.paths) ? null : new PatternsRequestCondition(parse(this.paths, parser)),
					ObjectUtils.isEmpty(this.methods) ?
							null : new RequestMethodsRequestCondition(this.methods),
					ObjectUtils.isEmpty(this.params) ?
							null : new ParamsRequestCondition(this.params),
					ObjectUtils.isEmpty(this.headers) ?
							null : new HeadersRequestCondition(this.headers),
					ObjectUtils.isEmpty(this.consumes) && !this.hasContentType ?
							null : new ConsumesRequestCondition(this.consumes, this.headers),
					ObjectUtils.isEmpty(this.produces) && !this.hasAccept ?
							null : new ProducesRequestCondition(this.produces, this.headers, contentTypeResolver),
					this.customCondition,
					this.options);

		}

		/**
		 * 解析路径模式数组并返回PathPattern列表。
		 *
		 * @param patterns 要解析的路径模式数组
		 * @param parser   路径模式解析器
		 * @return 解析后的PathPattern列表
		 */
		static List<PathPattern> parse(String[] patterns, PathPatternParser parser) {
			if (isEmpty(patterns)) {
				return Collections.emptyList();
			}
			List<PathPattern> result = new ArrayList<>(patterns.length);
			for (String path : patterns) {
				if (StringUtils.hasText(path) && !path.startsWith("/")) {
					path = "/" + path;
				}
				result.add(parser.parse(path));
			}
			return result;
		}

		/**
		 * 检查路径模式数组是否为空。
		 *
		 * @param patterns 要检查的路径模式数组
		 * @return 如果路径模式数组为空则返回true，否则返回false
		 */
		static boolean isEmpty(String[] patterns) {
			if (!ObjectUtils.isEmpty(patterns)) {
				for (String pattern : patterns) {
					if (StringUtils.hasText(pattern)) {
						return false;
					}
				}
			}
			return true;
		}
	}

	/**
	 * MutateBuilder是一个用于构建RequestMappingInfo对象的内部静态类，实现了Builder接口。
	 */
	private static class MutateBuilder implements Builder {

		/**
		 * 请求映射的名称
		 */
		@Nullable
		private String name;

		/**
		 * 请求映射的路径模式条件
		 */
		@Nullable
		private PatternsRequestCondition patternsCondition;

		/**
		 * 请求映射的请求方法条件
		 */
		private RequestMethodsRequestCondition methodsCondition;

		/**
		 * 请求映射的参数条件
		 */
		private ParamsRequestCondition paramsCondition;

		/**
		 * 请求映射的头部条件
		 */
		private HeadersRequestCondition headersCondition;

		/**
		 * 请求映射的Consumes条件
		 */
		private ConsumesRequestCondition consumesCondition;

		/**
		 * 请求映射的Produces条件
		 */
		private ProducesRequestCondition producesCondition;

		/**
		 * 自定义的请求条件持有者
		 */
		private RequestConditionHolder customConditionHolder;

		/**
		 * 构建请求映射时的配置选项
		 */
		private BuilderConfiguration options;

		/**
		 * 构造方法，通过传入其他的RequestMappingInfo对象初始化MutateBuilder。
		 *
		 * @param other 要复制的其他RequestMappingInfo对象
		 */
		public MutateBuilder(RequestMappingInfo other) {
			this.name = other.name;
			this.patternsCondition = other.patternsCondition;
			this.methodsCondition = other.methodsCondition;
			this.paramsCondition = other.paramsCondition;
			this.headersCondition = other.headersCondition;
			this.consumesCondition = other.consumesCondition;
			this.producesCondition = other.producesCondition;
			this.customConditionHolder = other.customConditionHolder;
			this.options = other.options;
		}

		/**
		 * 设置请求映射的路径模式条件。
		 *
		 * @param paths 要设置的路径模式数组
		 * @return 当前MutateBuilder对象
		 */
		@Override
		public Builder paths(String... paths) {
			// 从配置选项中获取路径模式解析器，如果为空则使用默认实例
			PathPatternParser parser = (this.options.getPatternParser() != null ?
					this.options.getPatternParser() : PathPatternParser.defaultInstance);

			// 根据传入的路径数组解析路径，如果路径为空则设置为null
			this.patternsCondition = (DefaultBuilder.isEmpty(paths) ?
					null : new PatternsRequestCondition(DefaultBuilder.parse(paths, parser)));

			return this;
		}

		/**
		 * 设置请求映射的请求方法条件。
		 *
		 * @param methods 要设置的请求方法数组
		 * @return 当前MutateBuilder对象
		 */
		@Override
		public Builder methods(RequestMethod... methods) {
			// 检查传入的请求方法数组是否为空，如果为空则设置为预定义的空请求方法条件，否则创建包含指定方法的请求方法条件
			this.methodsCondition = (ObjectUtils.isEmpty(methods) ?
					EMPTY_REQUEST_METHODS : new RequestMethodsRequestCondition(methods));

			return this;
		}

		/**
		 * 设置请求映射的参数条件。
		 *
		 * @param params 要设置的参数数组
		 * @return 当前MutateBuilder对象
		 */
		@Override
		public Builder params(String... params) {
			// 检查传入的参数数组是否为空，如果为空则设置为空参数条件，否则创建包含指定参数的参数条件
			this.paramsCondition = (ObjectUtils.isEmpty(params) ?
					EMPTY_PARAMS : new ParamsRequestCondition(params));
			return this;
		}

		/**
		 * 设置请求映射的头部条件。
		 *
		 * @param headers 要设置的头部数组
		 * @return 当前MutateBuilder对象
		 */
		@Override
		public Builder headers(String... headers) {
			// 检查传入的头部数组是否为空，如果为空则设置为空头部条件，否则创建包含指定头部的头部条件
			this.headersCondition = (ObjectUtils.isEmpty(headers) ?
					EMPTY_HEADERS : new HeadersRequestCondition(headers));
			return this;
		}

		/**
		 * 设置请求映射的Consumes条件。
		 *
		 * @param consumes 要设置的Consumes数组
		 * @return 当前MutateBuilder对象
		 */
		@Override
		public Builder consumes(String... consumes) {
			// 检查传入的Consumes数组是否为空，如果为空则设置为空Consumes条件，否则创建包含指定Consumes的Consumes条件
			this.consumesCondition = (ObjectUtils.isEmpty(consumes) ?
					EMPTY_CONSUMES : new ConsumesRequestCondition(consumes));
			return this;
		}

		/**
		 * 设置请求映射的Produces条件。
		 *
		 * @param produces 要设置的Produces数组
		 * @return 当前MutateBuilder对象
		 */
		@Override
		public Builder produces(String... produces) {
			// 检查传入的Produces数组是否为空，如果为空则设置为空Produces条件，否则创建包含指定Produces的Produces条件
			this.producesCondition = (ObjectUtils.isEmpty(produces) ?
					EMPTY_PRODUCES :
					new ProducesRequestCondition(produces, null, this.options.getContentTypeResolver()));
			return this;
		}

		/**
		 * 设置请求映射的名称。
		 *
		 * @param name 要设置的名称
		 * @return 当前MutateBuilder对象
		 */
		@Override
		public Builder mappingName(String name) {
			this.name = name;
			return this;
		}

		/**
		 * 设置自定义的请求条件。
		 *
		 * @param condition 要设置的自定义请求条件
		 * @return 当前MutateBuilder对象
		 */
		@Override
		public Builder customCondition(RequestCondition<?> condition) {
			this.customConditionHolder = new RequestConditionHolder(condition);
			return this;
		}

		/**
		 * 设置构建请求映射时的配置选项。
		 *
		 * @param options 要设置的配置选项
		 * @return 当前MutateBuilder对象
		 */
		@Override
		public Builder options(BuilderConfiguration options) {
			this.options = options;
			return this;
		}

		/**
		 * 构建并返回RequestMappingInfo对象。
		 *
		 * @return 构建的RequestMappingInfo对象
		 */
		@Override
		public RequestMappingInfo build() {
			return new RequestMappingInfo(this.name, this.patternsCondition,
					this.methodsCondition, this.paramsCondition, this.headersCondition,
					this.consumesCondition, this.producesCondition,
					this.customConditionHolder, this.options);
		}
	}


	/**
	 * 用于请求映射目的的配置选项容器。
	 * 此类配置用于创建 RequestMappingInfo 实例，但通常用于所有 RequestMappingInfo 实例。
	 *
	 * @see Builder#options
	 */
	public static class BuilderConfiguration {

		/**
		 * 用于存储路径模式解析器的私有变量
		 */
		@Nullable
		private PathPatternParser patternParser;

		/**
		 * 用于存储请求内容类型解析器的私有变量
		 */
		@Nullable
		private RequestedContentTypeResolver contentTypeResolver;

		public void setPatternParser(PathPatternParser patternParser) {
			this.patternParser = patternParser;
		}

		@Nullable
		public PathPatternParser getPatternParser() {
			return this.patternParser;
		}

		/**
		 * 设置用于 ProducesRequestCondition 的 ContentNegotiationManager。
		 * <p>默认情况下未设置。
		 */
		public void setContentTypeResolver(RequestedContentTypeResolver resolver) {
			this.contentTypeResolver = resolver;
		}

		@Nullable
		public RequestedContentTypeResolver getContentTypeResolver() {
			return this.contentTypeResolver;
		}
	}

}
