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

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.server.PathContainer;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.UnsatisfiedServletRequestParameterException;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.AbstractHandlerMethodMapping;
import org.springframework.web.servlet.mvc.condition.*;
import org.springframework.web.util.ServletRequestPathUtils;
import org.springframework.web.util.WebUtils;
import org.springframework.web.util.pattern.PathPattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.*;

/**
 * 定义 {@link RequestMappingInfo} 之间映射请求和处理程序方法的类的抽象基类。
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public abstract class RequestMappingInfoHandlerMapping extends AbstractHandlerMethodMapping<RequestMappingInfo> {
	/**
	 * HTTP可选处理方法
	 */
	private static final Method HTTP_OPTIONS_HANDLE_METHOD;

	static {
		try {
			// 获取 handle 方法
			HTTP_OPTIONS_HANDLE_METHOD = HttpOptionsHandler.class.getMethod("handle");
		} catch (NoSuchMethodException ex) {
			// 不应该发生
			throw new IllegalStateException("Failed to retrieve internal handler method for HTTP OPTIONS", ex);
		}
	}


	protected RequestMappingInfoHandlerMapping() {
		// 设置处理器方法映射命名策略
		setHandlerMethodMappingNamingStrategy(new RequestMappingInfoHandlerMethodMappingNamingStrategy());
	}


	/**
	 * 获取与提供的 {@link RequestMappingInfo} 关联的 URL 路径模式。
	 */
	@Override
	@SuppressWarnings("deprecation")
	protected Set<String> getMappingPathPatterns(RequestMappingInfo info) {
		return info.getPatternValues();
	}

	@Override
	protected Set<String> getDirectPaths(RequestMappingInfo info) {
		return info.getDirectPaths();
	}

	/**
	 * 检查给定的 RequestMappingInfo 是否与当前请求匹配，并返回一个与当前请求匹配的（可能是新的）实例，
	 * 其条件与当前请求匹配 -- 例如一组 URL 模式的子集。
	 *
	 * @return 如果匹配，则返回一个信息；否则返回 {@code null}。
	 */
	@Override
	protected RequestMappingInfo getMatchingMapping(RequestMappingInfo info, HttpServletRequest request) {
		return info.getMatchingCondition(request);
	}

	/**
	 * 提供一个 Comparator 来对与请求匹配的 RequestMappingInfos 进行排序。
	 */
	@Override
	protected Comparator<RequestMappingInfo> getMappingComparator(final HttpServletRequest request) {
		return (info1, info2) -> info1.compareTo(info2, request);
	}

	@Override
	@Nullable
	protected HandlerMethod getHandlerInternal(HttpServletRequest request) throws Exception {
		request.removeAttribute(PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE);
		try {
			// 调用父类的 getHandlerInternal 方法获取处理程序
			return super.getHandlerInternal(request);
		} finally {
			// 清除请求的可产生的媒体类型属性
			ProducesRequestCondition.clearMediaTypesAttribute(request);
		}
	}

	/**
	 * 公开 URI 模板变量、矩阵变量和可生产的媒体类型的请求。
	 */
	@Override
	protected void handleMatch(RequestMappingInfo info, String lookupPath, HttpServletRequest request) {
		super.handleMatch(info, lookupPath, request);

		// 获取请求条件
		RequestCondition<?> condition = info.getActivePatternsCondition();
		if (condition instanceof PathPatternsRequestCondition) {
			// 提取匹配细节
			extractMatchDetails((PathPatternsRequestCondition) condition, lookupPath, request);
		} else {
			// 提取匹配细节
			extractMatchDetails((PatternsRequestCondition) condition, lookupPath, request);
		}

		if (!info.getProducesCondition().getProducibleMediaTypes().isEmpty()) {
			// 如果存在可生成的媒体类型，则将其设置为请求属性
			Set<MediaType> mediaTypes = info.getProducesCondition().getProducibleMediaTypes();
			request.setAttribute(PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE, mediaTypes);
		}
	}

	private void extractMatchDetails(
			PathPatternsRequestCondition condition, String lookupPath, HttpServletRequest request) {

		PathPattern bestPattern;
		Map<String, String> uriVariables;

		// 检查是否是空路径映射
		if (condition.isEmptyPathMapping()) {
			// 如果是空路径映射，则使用第一个模式
			bestPattern = condition.getFirstPattern();
			// URI 变量为空
			uriVariables = Collections.emptyMap();
		} else {
			// 如果不是空路径映射，则从请求路径中提取最佳模式
			PathContainer path = ServletRequestPathUtils.getParsedRequestPath(request).pathWithinApplication();
			// 获取第一个模式
			bestPattern = condition.getFirstPattern();
			// 匹配并提取路径
			PathPattern.PathMatchInfo result = bestPattern.matchAndExtract(path);
			Assert.notNull(result, () ->
					"Expected bestPattern: " + bestPattern + " to match lookupPath " + path);
			// 获取 URI 变量
			uriVariables = result.getUriVariables();
			// 设置矩阵变量
			request.setAttribute(MATRIX_VARIABLES_ATTRIBUTE, result.getMatrixVariables());
		}

		// 将最佳匹配模式和 URI 变量设置为请求属性
		request.setAttribute(BEST_MATCHING_PATTERN_ATTRIBUTE, bestPattern.getPatternString());
		request.setAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE, uriVariables);
	}

	private void extractMatchDetails(
			PatternsRequestCondition condition, String lookupPath, HttpServletRequest request) {

		String bestPattern;
		Map<String, String> uriVariables;
		if (condition.isEmptyPathMapping()) {
			// 如果条件为空路径映射，则最佳模式即为查找路径
			bestPattern = lookupPath;
			// URI变量设为空
			uriVariables = Collections.emptyMap();
		} else {
			// 否则，最佳模式为条件中的第一个模式
			bestPattern = condition.getPatterns().iterator().next();
			// 提取URI模板变量
			uriVariables = getPathMatcher().extractUriTemplateVariables(bestPattern, lookupPath);
			// 如果不应删除分号内容，则设置矩阵变量属性
			if (!getUrlPathHelper().shouldRemoveSemicolonContent()) {
				request.setAttribute(MATRIX_VARIABLES_ATTRIBUTE, extractMatrixVariables(request, uriVariables));
			}
			// 解码路径变量
			uriVariables = getUrlPathHelper().decodePathVariables(request, uriVariables);
		}
		// 设置最佳匹配模式和URI模板变量属性
		request.setAttribute(BEST_MATCHING_PATTERN_ATTRIBUTE, bestPattern);
		request.setAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE, uriVariables);
	}

	private Map<String, MultiValueMap<String, String>> extractMatrixVariables(
			HttpServletRequest request, Map<String, String> uriVariables) {

		// 创建一个用于存储解析结果的 LinkedHashMap
		Map<String, MultiValueMap<String, String>> result = new LinkedHashMap<>();

		// 对每个URI模板变量进行处理
		uriVariables.forEach((uriVarKey, uriVarValue) -> {

			// 查找等号的位置
			int equalsIndex = uriVarValue.indexOf('=');
			if (equalsIndex == -1) {
				// 如果没有找到等号，直接返回
				return;
			}

			// 查找分号的位置
			int semicolonIndex = uriVarValue.indexOf(';');
			if (semicolonIndex != -1 && semicolonIndex != 0) {
				// 如果找到分号且不在开头，则截取分号之前的内容作为新的值
				uriVarValue = uriVarValue.substring(0, semicolonIndex);
			}

			// 定义矩阵变量字符串
			String matrixVariables;
			if (semicolonIndex == -1 || semicolonIndex == 0 || equalsIndex < semicolonIndex) {
				// 如果没有分号，或者分号在开头，或者等号在分号之前，则矩阵变量即为整个值
				matrixVariables = uriVarValue;
			} else {
				// 否则，矩阵变量为分号之后的部分
				matrixVariables = uriVarValue.substring(semicolonIndex + 1);
			}

			// 解析矩阵变量，并进行URL解码
			MultiValueMap<String, String> vars = WebUtils.parseMatrixVariables(matrixVariables);
			result.put(uriVarKey, getUrlPathHelper().decodeMatrixVariables(request, vars));
		});
		// 返回解析结果
		return result;
	}

	/**
	 * 再次迭代所有的 RequestMappingInfo，查看是否有任何与 URL 匹配，根据不匹配的情况抛出异常。
	 *
	 * @throws HttpRequestMethodNotSupportedException 如果 URL 匹配但 HTTP 方法不匹配
	 * @throws HttpMediaTypeNotAcceptableException    如果 URL 匹配但不可接受/可生产的媒体类型不匹配
	 */
	@Override
	protected HandlerMethod handleNoMatch(
			Set<RequestMappingInfo> infos, String lookupPath, HttpServletRequest request) throws ServletException {

		// 创建 PartialMatchHelper 实例，用于处理部分匹配的情况
		PartialMatchHelper helper = new PartialMatchHelper(infos, request);

		// 如果匹配助手为空，则返回 null
		if (helper.isEmpty()) {
			return null;
		}

		// 如果存在方法不匹配的情况
		if (helper.hasMethodsMismatch()) {
			// 获取允许的方法集合
			Set<String> methods = helper.getAllowedMethods();
			// 如果是 OPTIONS 请求，则返回处理 OPTIONS 请求的 HandlerMethod
			if (HttpMethod.OPTIONS.matches(request.getMethod())) {
				// 获取消耗补丁媒体类型
				Set<MediaType> mediaTypes = helper.getConsumablePatchMediaTypes();
				HttpOptionsHandler handler = new HttpOptionsHandler(methods, mediaTypes);
				// 返回构建好的 处理器方法
				return new HandlerMethod(handler, HTTP_OPTIONS_HANDLE_METHOD);
			}
			// 否则，抛出请求方法不支持的异常
			throw new HttpRequestMethodNotSupportedException(request.getMethod(), methods);
		}

		// 如果存在 消费 不匹配的情况
		if (helper.hasConsumesMismatch()) {
			// 获取可消费的媒体类型集合
			Set<MediaType> mediaTypes = helper.getConsumableMediaTypes();
			MediaType contentType = null;
			// 如果请求包含内容类型，则解析内容类型
			if (StringUtils.hasLength(request.getContentType())) {
				try {
					// 解析内容类型
					contentType = MediaType.parseMediaType(request.getContentType());
				} catch (InvalidMediaTypeException ex) {
					throw new HttpMediaTypeNotSupportedException(ex.getMessage());
				}
			}
			// 抛出不支持的媒体类型异常
			throw new HttpMediaTypeNotSupportedException(contentType, new ArrayList<>(mediaTypes));
		}

		// 如果存在 produces 不匹配的情况
		if (helper.hasProducesMismatch()) {
			// 获取可生产的媒体类型集合
			Set<MediaType> mediaTypes = helper.getProducibleMediaTypes();
			// 抛出不可接受的媒体类型异常
			throw new HttpMediaTypeNotAcceptableException(new ArrayList<>(mediaTypes));
		}

		// 如果存在参数不匹配的情况
		if (helper.hasParamsMismatch()) {
			// 获取参数条件列表
			List<String[]> conditions = helper.getParamConditions();
			// 抛出未满足的 Servlet 请求参数异常
			throw new UnsatisfiedServletRequestParameterException(conditions, request.getParameterMap());
		}

		// 若没有任何不匹配的情况，则返回 null
		return null;
	}


	/**
	 * 聚合所有部分匹配并公开跨它们的方法检查。
	 */
	private static class PartialMatchHelper {

		/**
		 * 部分匹配的方法列表
		 */
		private final List<PartialMatch> partialMatches = new ArrayList<>();

		public PartialMatchHelper(Set<RequestMappingInfo> infos, HttpServletRequest request) {
			// 遍历所有的 RequestMappingInfo
			for (RequestMappingInfo info : infos) {
				// 如果当前 RequestMappingInfo 匹配当前请求，则添加到部分匹配列表中
				if (info.getActivePatternsCondition().getMatchingCondition(request) != null) {
					this.partialMatches.add(new PartialMatch(info, request));
				}
			}
		}

		/**
		 * 是否有任何部分匹配。
		 */
		public boolean isEmpty() {
			return this.partialMatches.isEmpty();
		}

		/**
		 * 是否有"方法"的部分匹配？
		 */
		public boolean hasMethodsMismatch() {
			// 遍历部分匹配列表
			for (PartialMatch match : this.partialMatches) {
				// 如果部分匹配中有匹配到请求方法的，则返回 false
				if (match.hasMethodsMatch()) {
					return false;
				}
			}
			// 如果没有部分匹配到请求方法的，则返回 true
			return true;
		}

		/**
		 * 是否有"方法"和"消耗"的部分匹配？
		 */
		public boolean hasConsumesMismatch() {
			// 遍历部分匹配列表
			for (PartialMatch match : this.partialMatches) {
				// 如果部分匹配中有匹配到请求消费类型的，则返回 false
				if (match.hasConsumesMatch()) {
					return false;
				}
			}
			// 如果没有部分匹配到请求消费类型的，则返回 true
			return true;
		}

		/**
		 * 是否有"方法"、"消耗"和"生产"的部分匹配？
		 */
		public boolean hasProducesMismatch() {
			// 遍历部分匹配列表
			for (PartialMatch match : this.partialMatches) {
				// 如果部分匹配中有匹配到请求生产类型的，则返回 false
				if (match.hasProducesMatch()) {
					return false;
				}
			}
			// 如果没有部分匹配到请求生产类型的，则返回 true
			return true;
		}

		/**
		 * 是否有"方法"、"消耗"、"生产"和"参数"的部分匹配？
		 */
		public boolean hasParamsMismatch() {
			// 遍历部分匹配列表
			for (PartialMatch match : this.partialMatches) {
				// 如果部分匹配中有匹配到请求参数的，则返回 false
				if (match.hasParamsMatch()) {
					return false;
				}
			}
			// 如果没有部分匹配到请求参数的，则返回 true
			return true;
		}

		/**
		 * 返回声明的 HTTP 方法。
		 */
		public Set<String> getAllowedMethods() {
			// 使用 LinkedHashSet 来保持顺序，避免重复的请求方法
			Set<String> result = new LinkedHashSet<>();
			// 遍历部分匹配列表
			for (PartialMatch match : this.partialMatches) {
				// 获取每个部分匹配中的请求方法，并添加到结果集合中
				for (RequestMethod method : match.getInfo().getMethodsCondition().getMethods()) {
					result.add(method.name());
				}
			}
			// 返回结果集合
			return result;
		}

		/**
		 * 返回声明的"可消耗"类型，但仅限于那些还匹配"方法"条件的类型。
		 */
		public Set<MediaType> getConsumableMediaTypes() {
			// 使用 LinkedHashSet 来保持顺序，避免重复的媒体类型
			Set<MediaType> result = new LinkedHashSet<>();
			// 遍历部分匹配列表
			for (PartialMatch match : this.partialMatches) {
				// 如果部分匹配包含请求方法
				if (match.hasMethodsMatch()) {
					// 添加该部分匹配中可消耗的媒体类型到结果集合中
					result.addAll(match.getInfo().getConsumesCondition().getConsumableMediaTypes());
				}
			}
			// 返回结果集合
			return result;
		}

		/**
		 * 返回声明的"可生产"类型，但仅限于那些也匹配"方法"和"消耗"条件的类型。
		 */
		public Set<MediaType> getProducibleMediaTypes() {
			// 使用 LinkedHashSet 来保持顺序，避免重复的媒体类型
			Set<MediaType> result = new LinkedHashSet<>();
			// 遍历部分匹配列表
			for (PartialMatch match : this.partialMatches) {
				// 如果部分匹配包含请求消耗类型
				if (match.hasConsumesMatch()) {
					// 添加该部分匹配中可生产的媒体类型到结果集合中
					result.addAll(match.getInfo().getProducesCondition().getProducibleMediaTypes());
				}
			}
			// 返回结果集合
			return result;
		}

		/**
		 * 返回声明的"参数"条件，但仅限于那些也匹配"方法"、"消耗"和"参数"条件的类型。
		 */
		public List<String[]> getParamConditions() {
			// 创建一个列表来保存结果
			List<String[]> result = new ArrayList<>();
			// 遍历部分匹配列表
			for (PartialMatch match : this.partialMatches) {
				// 如果部分匹配包含可接受的媒体类型
				if (match.hasProducesMatch()) {
					// 获取参数条件中的表达式集合
					Set<NameValueExpression<String>> set = match.getInfo().getParamsCondition().getExpressions();
					// 如果表达式集合不为空
					if (!CollectionUtils.isEmpty(set)) {
						int i = 0;
						// 创建一个数组来保存表达式字符串
						String[] array = new String[set.size()];
						// 遍历表达式集合
						for (NameValueExpression<String> expression : set) {
							// 将表达式字符串存储到数组中
							array[i++] = expression.toString();
						}
						// 将数组添加到结果列表中
						result.add(array);
					}
				}
			}
			// 返回结果列表
			return result;
		}

		/**
		 * 返回声明的"可消耗"类型，但仅限于那些已指定 PATCH，或根本没有方法的类型。
		 */
		public Set<MediaType> getConsumablePatchMediaTypes() {
			// 创建一个集合来保存结果
			Set<MediaType> result = new LinkedHashSet<>();
			// 遍历部分匹配列表
			for (PartialMatch match : this.partialMatches) {
				// 获取方法条件中的方法集合
				Set<RequestMethod> methods = match.getInfo().getMethodsCondition().getMethods();
				// 如果方法集合为空或包含 PATCH 方法
				if (methods.isEmpty() || methods.contains(RequestMethod.PATCH)) {
					// 将匹配信息中可接受的媒体类型添加到结果集合中
					result.addAll(match.getInfo().getConsumesCondition().getConsumableMediaTypes());
				}
			}
			// 返回结果集合
			return result;
		}


		/**
		 * 匹配 URL 路径的 RequestMappingInfo 的容器。
		 */
		private static class PartialMatch {
			/**
			 * 请求映射信息
			 */
			private final RequestMappingInfo info;

			/**
			 * 是否匹配方法
			 */
			private final boolean methodsMatch;

			/**
			 * 是否匹配消费函数
			 */
			private final boolean consumesMatch;

			/**
			 * 是否匹配生产函数
			 */
			private final boolean producesMatch;

			/**
			 * 是否匹配参数
			 */
			private final boolean paramsMatch;

			/**
			 * 创建一个新的 {@link PartialMatch} 实例。
			 *
			 * @param info    匹配 URL 路径的 RequestMappingInfo。
			 * @param request 当前请求
			 */
			public PartialMatch(RequestMappingInfo info, HttpServletRequest request) {
				this.info = info;
				this.methodsMatch = (info.getMethodsCondition().getMatchingCondition(request) != null);
				this.consumesMatch = (info.getConsumesCondition().getMatchingCondition(request) != null);
				this.producesMatch = (info.getProducesCondition().getMatchingCondition(request) != null);
				this.paramsMatch = (info.getParamsCondition().getMatchingCondition(request) != null);
			}

			public RequestMappingInfo getInfo() {
				return this.info;
			}

			public boolean hasMethodsMatch() {
				return this.methodsMatch;
			}

			public boolean hasConsumesMatch() {
				return (hasMethodsMatch() && this.consumesMatch);
			}

			public boolean hasProducesMatch() {
				return (hasConsumesMatch() && this.producesMatch);
			}

			public boolean hasParamsMatch() {
				return (hasProducesMatch() && this.paramsMatch);
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

		public HttpOptionsHandler(Set<String> declaredMethods, Set<MediaType> acceptPatch) {
			this.headers.setAllow(initAllowedHttpMethods(declaredMethods));
			this.headers.setAcceptPatch(new ArrayList<>(acceptPatch));
		}

		private static Set<HttpMethod> initAllowedHttpMethods(Set<String> declaredMethods) {
			Set<HttpMethod> result = new LinkedHashSet<>(declaredMethods.size());
			if (declaredMethods.isEmpty()) {
				// 如果声明的方法为空
				for (HttpMethod method : HttpMethod.values()) {
					if (method != HttpMethod.TRACE) {
						// 如果不是Trace方法，添加到结果集中
						result.add(method);
					}
				}
			} else {
				// 声明的方法不为空
				for (String method : declaredMethods) {
					HttpMethod httpMethod = HttpMethod.valueOf(method);
					result.add(httpMethod);
					if (httpMethod == HttpMethod.GET) {
						// 如果是Get方法，则添加到结果集中
						result.add(HttpMethod.HEAD);
					}
				}
				// 添加Options方法
				result.add(HttpMethod.OPTIONS);
			}
			return result;
		}

		@SuppressWarnings("unused")
		public HttpHeaders handle() {
			return this.headers;
		}
	}

}
