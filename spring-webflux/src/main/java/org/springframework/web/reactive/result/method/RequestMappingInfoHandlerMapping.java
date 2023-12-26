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

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.server.PathContainer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.result.condition.NameValueExpression;
import org.springframework.web.reactive.result.condition.ProducesRequestCondition;
import org.springframework.web.server.*;
import org.springframework.web.util.pattern.PathPattern;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 抽象基类，用于 {@link RequestMappingInfo} 定义请求和处理程序方法之间的映射。
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 5.0
 */
public abstract class RequestMappingInfoHandlerMapping extends AbstractHandlerMethodMapping<RequestMappingInfo> {

	private static final Method HTTP_OPTIONS_HANDLE_METHOD;

	static {
		try {
			HTTP_OPTIONS_HANDLE_METHOD = HttpOptionsHandler.class.getMethod("handle");
		} catch (NoSuchMethodException ex) {
			// 不应该发生
			throw new IllegalStateException("No handler for HTTP OPTIONS", ex);
		}
	}

	/**
	 * 返回不是模式的请求映射路径。
	 *
	 * @param info 请求映射信息
	 * @return 不是模式的请求映射路径集合
	 */
	@Override
	protected Set<String> getDirectPaths(RequestMappingInfo info) {
		return info.getDirectPaths();
	}

	/**
	 * 检查给定的 {@link RequestMappingInfo} 是否与当前请求匹配，
	 * 并返回一个（可能是新的）实例，其条件与当前请求匹配，例如具有 URL 模式的子集。
	 *
	 * @return 匹配时的信息；否则为 {@code null}。
	 */
	@Override
	protected RequestMappingInfo getMatchingMapping(RequestMappingInfo info, ServerWebExchange exchange) {
		return info.getMatchingCondition(exchange);
	}

	/**
	 * 提供一个比较器，用于对匹配到的请求进行排序。
	 */
	@Override
	protected Comparator<RequestMappingInfo> getMappingComparator(final ServerWebExchange exchange) {
		return (info1, info2) -> info1.compareTo(info2, exchange);
	}

	/**
	 * 获取内部的处理方法。
	 *
	 * @param exchange 当前的交换对象
	 * @return 处理方法的 Mono 对象，可能为空
	 */
	@Override
	public Mono<HandlerMethod> getHandlerInternal(ServerWebExchange exchange) {
		exchange.getAttributes().remove(HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE);
		return super.getHandlerInternal(exchange)
				.doOnTerminate(() -> ProducesRequestCondition.clearMediaTypesAttribute(exchange));
	}

	/**
	 * 公开 URI 模板变量、矩阵变量和请求中的可生成媒体类型。
	 */
	@Override
	protected void handleMatch(RequestMappingInfo info, HandlerMethod handlerMethod, ServerWebExchange exchange) {
		// 调用父类方法处理匹配
		super.handleMatch(info, handlerMethod, exchange);

		// 获取请求的路径
		PathContainer lookupPath = exchange.getRequest().getPath().pathWithinApplication();

		PathPattern bestPattern;
		Map<String, String> uriVariables;
		Map<String, MultiValueMap<String, String>> matrixVariables;

		Set<PathPattern> patterns = info.getPatternsCondition().getPatterns();
		if (patterns.isEmpty()) {
			// 如果模式集合为空，直接解析请求路径
			bestPattern = getPathPatternParser().parse(lookupPath.value());
			uriVariables = Collections.emptyMap();
			matrixVariables = Collections.emptyMap();
		} else {
			// 否则，取第一个模式进行匹配和提取
			bestPattern = patterns.iterator().next();
			PathPattern.PathMatchInfo result = bestPattern.matchAndExtract(lookupPath);
			Assert.notNull(result, () ->
					"Expected bestPattern: " + bestPattern + " to match lookupPath " + lookupPath);
			uriVariables = result.getUriVariables();
			matrixVariables = result.getMatrixVariables();
		}

		// 将匹配结果放入 Exchange 的属性中
		exchange.getAttributes().put(BEST_MATCHING_HANDLER_ATTRIBUTE, handlerMethod);
		exchange.getAttributes().put(BEST_MATCHING_PATTERN_ATTRIBUTE, bestPattern);
		exchange.getAttributes().put(URI_TEMPLATE_VARIABLES_ATTRIBUTE, uriVariables);
		exchange.getAttributes().put(MATRIX_VARIABLES_ATTRIBUTE, matrixVariables);

		// 如果请求的处理方法有指定的输出媒体类型，放入 Exchange 的属性中
		if (!info.getProducesCondition().getProducibleMediaTypes().isEmpty()) {
			Set<MediaType> mediaTypes = info.getProducesCondition().getProducibleMediaTypes();
			exchange.getAttributes().put(PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE, mediaTypes);
		}
	}

	/**
	 * 再次迭代所有的 RequestMappingInfos，查看是否有任何 URL 匹配，
	 * 并根据情况引发异常。
	 *
	 * @throws MethodNotAllowedException           如果通过 URL 匹配但不通过 HTTP 方法
	 * @throws UnsupportedMediaTypeStatusException 如果通过 URL 和 HTTP 方法匹配但不通过可消耗的媒体类型
	 * @throws NotAcceptableStatusException        如果通过 URL 和 HTTP 方法匹配但不通过可生成的媒体类型
	 * @throws ServerWebInputException             如果通过 URL 和 HTTP 方法匹配但不通过查询参数条件
	 */
	@Override
	protected HandlerMethod handleNoMatch(Set<RequestMappingInfo> infos, ServerWebExchange exchange) throws Exception {
		// 创建 PartialMatchHelper 对象，用于帮助处理部分匹配
		PartialMatchHelper helper = new PartialMatchHelper(infos, exchange);

		// 如果没有匹配的部分，返回 null
		if (helper.isEmpty()) {
			return null;
		}

		// 获取当前请求
		ServerHttpRequest request = exchange.getRequest();

		// 如果存在请求方法不匹配的情况
		if (helper.hasMethodsMismatch()) {
			String httpMethod = request.getMethodValue();
			Set<HttpMethod> methods = helper.getAllowedMethods();

			// 如果是 OPTIONS 请求，创建 HttpOptionsHandler 处理器
			if (HttpMethod.OPTIONS.matches(httpMethod)) {
				Set<MediaType> mediaTypes = helper.getConsumablePatchMediaTypes();
				HttpOptionsHandler handler = new HttpOptionsHandler(methods, mediaTypes);
				return new HandlerMethod(handler, HTTP_OPTIONS_HANDLE_METHOD);
			}

			// 否则，抛出 MethodNotAllowedException 异常
			throw new MethodNotAllowedException(httpMethod, methods);
		}

		// 如果存在请求内容类型不匹配的情况
		if (helper.hasConsumesMismatch()) {
			Set<MediaType> mediaTypes = helper.getConsumableMediaTypes();
			MediaType contentType;
			try {
				// 获取请求头中的内容类型
				contentType = request.getHeaders().getContentType();
			} catch (InvalidMediaTypeException ex) {
				// 处理无效的内容类型异常，抛出 UnsupportedMediaTypeStatusException 异常
				throw new UnsupportedMediaTypeStatusException(ex.getMessage());
			}
			throw new UnsupportedMediaTypeStatusException(contentType, new ArrayList<>(mediaTypes), exchange.getRequest().getMethod());
		}

		// 如果存在响应内容类型不匹配的情况
		if (helper.hasProducesMismatch()) {
			Set<MediaType> mediaTypes = helper.getProducibleMediaTypes();
			// 抛出 NotAcceptableStatusException 异常
			throw new NotAcceptableStatusException(new ArrayList<>(mediaTypes));
		}

		// 如果存在请求参数不匹配的情况
		if (helper.hasParamsMismatch()) {
			// 抛出 ServerWebInputException 异常
			throw new ServerWebInputException(
					"Unsatisfied query parameter conditions: " + helper.getParamConditions() +
							", actual parameters: " + request.getQueryParams());
		}

		// 如果以上情况都没有匹配，返回 null
		return null;
	}


	/**
	 * 聚合所有部分匹配并公开跨它们的检查方法。
	 */
	private static class PartialMatchHelper {

		private final List<PartialMatch> partialMatches = new ArrayList<>();

		/**
		 * 构造函数，初始化部分匹配信息。
		 *
		 * @param infos     请求映射信息集合
		 * @param exchange  当前的交换对象
		 */
		public PartialMatchHelper(Set<RequestMappingInfo> infos, ServerWebExchange exchange) {
			this.partialMatches.addAll(infos.stream()
					.filter(info -> info.getPatternsCondition().getMatchingCondition(exchange) != null)
					.map(info -> new PartialMatch(info, exchange))
					.collect(Collectors.toList()));
		}


		/**
		 * 是否有任何部分匹配。
		 */
		public boolean isEmpty() {
			return this.partialMatches.isEmpty();
		}

		/**
		 * 是否有任何“方法”方面的部分匹配。
		 */
		public boolean hasMethodsMismatch() {
			return this.partialMatches.stream()
					.noneMatch(PartialMatch::hasMethodsMatch);
		}

		/**
		 * 是否有任何“方法”和“consumes”方面的部分匹配。
		 */
		public boolean hasConsumesMismatch() {
			return this.partialMatches.stream()
					.noneMatch(PartialMatch::hasConsumesMatch);
		}

		/**
		 * 是否有任何“方法”、“consumes”和“produces”方面的部分匹配。
		 */
		public boolean hasProducesMismatch() {
			return this.partialMatches.stream()
					.noneMatch(PartialMatch::hasProducesMatch);
		}

		/**
		 * 是否有任何“方法”、“consumes”、“produces”和“params”方面的部分匹配。
		 */
		public boolean hasParamsMismatch() {
			return this.partialMatches.stream()
					.noneMatch(PartialMatch::hasParamsMatch);
		}

		/**
		 * 返回声明的HTTP方法。
		 */
		public Set<HttpMethod> getAllowedMethods() {
			return this.partialMatches.stream()
					.flatMap(m -> m.getInfo().getMethodsCondition().getMethods().stream())
					.map(requestMethod -> HttpMethod.resolve(requestMethod.name()))
					.collect(Collectors.toSet());
		}

		/**
		 * 返回声明的“可消耗”类型，但仅限于那些也符合“方法”条件的类型。
		 */
		public Set<MediaType> getConsumableMediaTypes() {
			return this.partialMatches.stream()
					.filter(PartialMatch::hasMethodsMatch)
					.flatMap(m -> m.getInfo().getConsumesCondition().getConsumableMediaTypes().stream())
					.collect(Collectors.toCollection(LinkedHashSet::new));
		}

		/**
		 * 返回声明的“可生产”类型，但仅限于那些也符合“方法”和“消耗”条件的类型。
		 */
		public Set<MediaType> getProducibleMediaTypes() {
			return this.partialMatches.stream()
					.filter(PartialMatch::hasConsumesMatch)
					.flatMap(m -> m.getInfo().getProducesCondition().getProducibleMediaTypes().stream())
					.collect(Collectors.toCollection(LinkedHashSet::new));
		}

		/**
		 * 返回声明的“参数”条件，但仅限于那些也符合“方法”、“消耗”和“参数”条件的类型。
		 */
		public List<Set<NameValueExpression<String>>> getParamConditions() {
			return this.partialMatches.stream()
					.filter(PartialMatch::hasProducesMatch)
					.map(match -> match.getInfo().getParamsCondition().getExpressions())
					.collect(Collectors.toList());
		}

		/**
		 * 返回声明的“可消耗”类型，但仅限于那些指定了PATCH的类型，或者根本没有指定任何方法的类型。
		 */
		public Set<MediaType> getConsumablePatchMediaTypes() {
			Set<MediaType> result = new LinkedHashSet<>();
			// 遍历部分匹配对象列表
			for (PartialMatch match : this.partialMatches) {
				// 获取请求映射信息对象的请求方法条件中的请求方法集合
				Set<RequestMethod> methods = match.getInfo().getMethodsCondition().getMethods();
				// 如果请求方法为空或包含 PATCH 方法
				if (methods.isEmpty() || methods.contains(RequestMethod.PATCH)) {
					// 将部分匹配对象的请求映射信息中的可消耗媒体类型添加到结果集合中
					result.addAll(match.getInfo().getConsumesCondition().getConsumableMediaTypes());
				}
			}
		// 返回最终结果集合
			return result;

		}


		/**
		 * 匹配 URL 路径的 RequestMappingInfo 容器。
		 */
		private static class PartialMatch {

			/**
			 * 请求映射信息
			 */
			private final RequestMappingInfo info;
			/**
			 * 方法匹配
			 */
			private final boolean methodsMatch;
			/**
			 * 请求内容类型匹配
			 */
			private final boolean consumesMatch;
			/**
			 * 响应内容类型匹配
			 */
			private final boolean producesMatch;
			/**
			 * 请求参数匹配
			 */
			private final boolean paramsMatch;

			/**
			 * 创建一个新的 PartialMatch 实例。
			 *
			 * @param info     匹配 URL 路径的 RequestMappingInfo
			 * @param exchange 当前的交换信息
			 */
			public PartialMatch(RequestMappingInfo info, ServerWebExchange exchange) {
				this.info = info;
				this.methodsMatch = info.getMethodsCondition().getMatchingCondition(exchange) != null;
				this.consumesMatch = info.getConsumesCondition().getMatchingCondition(exchange) != null;
				this.producesMatch = info.getProducesCondition().getMatchingCondition(exchange) != null;
				this.paramsMatch = info.getParamsCondition().getMatchingCondition(exchange) != null;
			}

			/**
			 * 返回请求映射信息。
			 *
			 * @return 请求映射信息对象
			 */
			public RequestMappingInfo getInfo() {
				return this.info;
			}

			/**
			 * 检查是否存在方法匹配。
			 *
			 * @return 如果存在方法匹配，则为true；否则为false
			 */
			public boolean hasMethodsMatch() {
				return this.methodsMatch;
			}

			/**
			 * 检查是否存在"consumes"匹配，同时检查是否存在方法匹配。
			 *
			 * @return 如果存在"consumes"匹配并且存在方法匹配，则为true；否则为false
			 */
			public boolean hasConsumesMatch() {
				return hasMethodsMatch() && this.consumesMatch;
			}

			/**
			 * 检查是否存在"produces"匹配，同时检查是否存在"consumes"匹配。
			 *
			 * @return 如果存在"produces"匹配并且存在"consumes"匹配，则为true；否则为false
			 */
			public boolean hasProducesMatch() {
				return hasConsumesMatch() && this.producesMatch;
			}


			/**
			 * 检查是否存在"params"匹配，同时检查是否存在"produces"匹配。
			 *
			 * @return 如果存在"params"匹配并且存在"produces"匹配，则为true；否则为false
			 */
			public boolean hasParamsMatch() {
				return hasProducesMatch() && this.paramsMatch;
			}

			@Override
			public String toString() {
				return this.info.toString();
			}
		}
	}

	/**
	 * HTTP OPTIONS 的默认处理程序。
	 */
	private static class HttpOptionsHandler {

		/**
		 * HTTP请求头
		 */
		private final HttpHeaders headers = new HttpHeaders();


		/**
		 * 创建 HTTP OPTIONS 处理程序。
		 *
		 * @param declaredMethods 声明的 HTTP 方法集合
		 * @param acceptPatch     接受的 PATCH 媒体类型集合
		 */
		public HttpOptionsHandler(Set<HttpMethod> declaredMethods, Set<MediaType> acceptPatch) {
			this.headers.setAllow(initAllowedHttpMethods(declaredMethods));
			this.headers.setAcceptPatch(new ArrayList<>(acceptPatch));
		}


		/**
		 * 初始化允许的 HTTP 方法集合。
		 *
		 * @param declaredMethods 声明的 HTTP 方法集合
		 * @return 允许的 HTTP 方法集合
		 */
		private static Set<HttpMethod> initAllowedHttpMethods(Set<HttpMethod> declaredMethods) {
			if (declaredMethods.isEmpty()) {
				// 如果未声明任何方法，则返回除了TRACE之外的所有HttpMethod
				return EnumSet.allOf(HttpMethod.class).stream()
						.filter(method -> method != HttpMethod.TRACE)
						.collect(Collectors.toSet());
			} else {
				// 如果已声明方法，则使用LinkedHashSet来保持顺序，并添加HEAD和OPTIONS方法
				Set<HttpMethod> result = new LinkedHashSet<>(declaredMethods);
				if (result.contains(HttpMethod.GET)) {
					result.add(HttpMethod.HEAD);
				}
				result.add(HttpMethod.OPTIONS);
				return result;
			}
		}

		/**
		 * 处理 HTTP OPTIONS 请求的默认处理器。
		 *
		 * @return HTTP 头信息
		 */
		@SuppressWarnings("unused")
		public HttpHeaders handle() {
			return this.headers;
		}
	}

}
