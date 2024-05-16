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

import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpMediaTypeException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.servlet.mvc.condition.HeadersRequestCondition.HeaderExpression;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * 逻辑或 (' || ') 请求条件，用于将请求的 'Accept' 标头与媒体类型表达式列表进行匹配。
 * 支持两种媒体类型表达式，分别在 {@link RequestMapping#produces()} 和 {@link RequestMapping#headers()} 中描述，
 * 其中头部名称为 'Accept'。
 * 无论使用哪种语法，语义都是相同的。
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public final class ProducesRequestCondition extends AbstractRequestCondition<ProducesRequestCondition> {
	/**
	 * 默认的内容协商管理器。
	 */
	private static final ContentNegotiationManager DEFAULT_CONTENT_NEGOTIATION_MANAGER =
			new ContentNegotiationManager();

	/**
	 * 空条件，表示没有任何匹配的情况。
	 */
	private static final ProducesRequestCondition EMPTY_CONDITION = new ProducesRequestCondition();

	/**
	 * 包含{@value MediaType#ALL_VALUE}表达式的列表。
	 */
	private static final List<ProduceMediaTypeExpression> MEDIA_TYPE_ALL_LIST =
			Collections.singletonList(new ProduceMediaTypeExpression(MediaType.ALL_VALUE));

	/**
	 * 用于存储已解析的请求媒体类型的属性名称。
	 */
	private static final String MEDIA_TYPES_ATTRIBUTE = ProducesRequestCondition.class.getName() + ".MEDIA_TYPES";

	/**
	 * 包含生产媒体类型表达式的列表。
	 */
	private final List<ProduceMediaTypeExpression> expressions;

	/**
	 * 内容协商管理器。
	 */
	private final ContentNegotiationManager contentNegotiationManager;


	/**
	 * 从 "produces" 表达式创建一个新实例。如果总共提供了 0 个表达式，则此条件将匹配任何请求。
	 *
	 * @param produces 使用 {@link RequestMapping#produces()} 定义的语法表示的表达式
	 */
	public ProducesRequestCondition(String... produces) {
		this(produces, null, null);
	}

	/**
	 * 使用 "produces" 和 "header" 表达式创建一个新实例。
	 * 忽略了 "header" 表达式中头部名称不是 'Accept' 或未定义头部值的情况。
	 * 如果总共提供了 0 个表达式，则此条件将匹配任何请求。
	 *
	 * @param produces 使用 {@link RequestMapping#produces()} 定义的语法表示的表达式
	 * @param headers  使用 {@link RequestMapping#headers()} 定义的语法表示的表达式
	 */
	public ProducesRequestCondition(String[] produces, @Nullable String[] headers) {
		this(produces, headers, null);
	}

	/**
	 * 与 {@link #ProducesRequestCondition(String[], String[])} 相同，但还接受一个 {@link ContentNegotiationManager}。
	 *
	 * @param produces 使用 {@link RequestMapping#produces()} 定义的语法表示的表达式
	 * @param headers  使用 {@link RequestMapping#headers()} 定义的语法表示的表达式
	 * @param manager  用于确定请求的媒体类型
	 */
	public ProducesRequestCondition(String[] produces, @Nullable String[] headers,
									@Nullable ContentNegotiationManager manager) {

		// 解析表达式并设置表达式列表
		this.expressions = parseExpressions(produces, headers);
		// 如果表达式列表的大小大于 1，则对表达式列表进行排序
		if (this.expressions.size() > 1) {
			Collections.sort(this.expressions);
		}
		// 设置内容协商管理器
		this.contentNegotiationManager = manager != null ? manager : DEFAULT_CONTENT_NEGOTIATION_MANAGER;
	}

	private List<ProduceMediaTypeExpression> parseExpressions(String[] produces, @Nullable String[] headers) {
		// 初始化结果集合
		Set<ProduceMediaTypeExpression> result = null;
		// 如果请求头不为空
		if (!ObjectUtils.isEmpty(headers)) {
			// 遍历请求头
			for (String header : headers) {
				// 解析请求头表达式
				HeaderExpression expr = new HeaderExpression(header);
				// 如果请求头为 Accept 且值不为空
				if ("Accept".equalsIgnoreCase(expr.name) && expr.value != null) {
					// 解析媒体类型并添加到结果集合中
					for (MediaType mediaType : MediaType.parseMediaTypes(expr.value)) {
						// 如果结果集合为空，则创建一个新的 LinkedHashSet
						result = (result != null ? result : new LinkedHashSet<>());
						// 添加解析后的媒体类型表达式到结果集合中
						result.add(new ProduceMediaTypeExpression(mediaType, expr.isNegated));
					}
				}
			}
		}
		// 如果 produces 不为空
		if (!ObjectUtils.isEmpty(produces)) {
			// 遍历 produces
			for (String produce : produces) {
				// 如果结果集合为空，则创建一个新的 LinkedHashSet
				result = (result != null ? result : new LinkedHashSet<>());
				// 添加媒体类型表达式到结果集合中
				result.add(new ProduceMediaTypeExpression(produce));
			}
		}
		// 返回结果集合的列表形式，如果结果集合为空则返回空列表
		return (result != null ? new ArrayList<>(result) : Collections.emptyList());
	}

	/**
	 * 内部使用的私有构造函数，用于创建匹配条件。
	 * 注意，expressions 列表既不排序也不深度复制。
	 */
	private ProducesRequestCondition(List<ProduceMediaTypeExpression> expressions, ProducesRequestCondition other) {
		this.expressions = expressions;
		this.contentNegotiationManager = other.contentNegotiationManager;
	}


	/**
	 * 返回包含的 "produces" 表达式。
	 */
	public Set<MediaTypeExpression> getExpressions() {
		return new LinkedHashSet<>(this.expressions);
	}

	/**
	 * 返回包含的可生产媒体类型，不包括否定的表达式。
	 */
	public Set<MediaType> getProducibleMediaTypes() {
		// 初始化结果集合
		Set<MediaType> result = new LinkedHashSet<>();
		// 遍历表达式列表
		for (ProduceMediaTypeExpression expression : this.expressions) {
			// 如果表达式不是否定的
			if (!expression.isNegated()) {
				// 将媒体类型添加到结果集合中
				result.add(expression.getMediaType());
			}
		}
		// 返回结果集合
		return result;
	}

	/**
	 * 判断条件是否没有任何媒体类型表达式。
	 */
	@Override
	public boolean isEmpty() {
		return this.expressions.isEmpty();
	}

	@Override
	protected List<ProduceMediaTypeExpression> getContent() {
		return this.expressions;
	}

	@Override
	protected String getToStringInfix() {
		return " || ";
	}

	/**
	 * 如果 "other" 实例有任何表达式，则返回 "other"；否则返回 "this" 实例。
	 * 实际上，这意味着方法级别的 "produces" 会覆盖类型级别的 "produces" 条件。
	 */
	@Override
	public ProducesRequestCondition combine(ProducesRequestCondition other) {
		return (!other.expressions.isEmpty() ? other : this);
	}

	/**
	 * 检查包含的任何媒体类型表达式是否与给定的请求 'Content-Type' 标头匹配，并返回一个保证只包含匹配表达式的实例。
	 * 匹配是通过 {@link MediaType#isCompatibleWith(MediaType)} 执行的。
	 *
	 * @param request 当前请求
	 * @return 如果没有表达式，则返回相同的实例；如果有匹配的表达式，则返回一个新的条件；如果没有表达式匹配，则返回 {@code null}。
	 */
	@Override
	@Nullable
	public ProducesRequestCondition getMatchingCondition(HttpServletRequest request) {
		// 如果是预检请求，返回空条件
		if (CorsUtils.isPreFlightRequest(request)) {
			return EMPTY_CONDITION;
		}

		// 如果当前对象为空，则返回当前对象
		if (isEmpty()) {
			return this;
		}

		List<MediaType> acceptedMediaTypes;
		try {
			// 获取请求中接受的媒体类型列表
			acceptedMediaTypes = getAcceptedMediaTypes(request);
		} catch (HttpMediaTypeException ex) {
			// 如果发生异常，则返回空
			return null;
		}

		// 获取匹配的媒体类型表达式列表
		List<ProduceMediaTypeExpression> result = getMatchingExpressions(acceptedMediaTypes);
		// 如果匹配到了表达式，则返回包含匹配结果的 ProducesRequestCondition 对象
		if (!CollectionUtils.isEmpty(result)) {
			return new ProducesRequestCondition(result, this);
		} else if (MediaType.ALL.isPresentIn(acceptedMediaTypes)) {
			// 如果接受所有媒体类型，返回空条件
			return EMPTY_CONDITION;
		} else {
			// 否则返回空
			return null;
		}
	}

	@Nullable
	private List<ProduceMediaTypeExpression> getMatchingExpressions(List<MediaType> acceptedMediaTypes) {
		// 初始化结果列表
		List<ProduceMediaTypeExpression> result = null;
		// 遍历媒体类型表达式列表
		for (ProduceMediaTypeExpression expression : this.expressions) {
			// 如果表达式匹配了接受的媒体类型列表
			if (expression.match(acceptedMediaTypes)) {
				// 如果结果列表为空，则创建一个新的 ArrayList
				result = result != null ? result : new ArrayList<>();
				// 将匹配的表达式添加到结果列表中
				result.add(expression);
			}
		}
		// 返回结果列表
		return result;
	}

	/**
	 * 比较此 "produces" 条件和另一个条件的顺序如下：
	 * <ol>
	 * <li>通过 {@link MediaType#sortByQualityValue(List)} 按质量值对 'Accept' 标头媒体类型进行排序，并遍历列表。
	 * <li>首先使用 {@link MediaType#equals(Object)} 和 {@link MediaType#includes(MediaType)} 在每个 "produces" 条件中获取匹配媒体类型的第一个索引。
	 * <li>如果找到更低的索引，则该索引处的条件获胜。
	 * <li>如果两个索引相等，则进一步使用 {@link MediaType#SPECIFICITY_COMPARATOR} 比较索引处的媒体类型。
	 * </ol>
	 * <p>假设这两个实例都是通过 {@link #getMatchingCondition(HttpServletRequest)} 获得的，并且每个实例只包含匹配的可生成媒体类型表达式，否则为空。
	 */
	@Override
	public int compareTo(ProducesRequestCondition other, HttpServletRequest request) {
		try {
			// 获取请求中接受的媒体类型列表
			List<MediaType> acceptedMediaTypes = getAcceptedMediaTypes(request);
			// 遍历接受的媒体类型列表
			for (MediaType acceptedMediaType : acceptedMediaTypes) {
				// 在当前对象中查找与接受的媒体类型相等的索引
				int thisIndex = this.indexOfEqualMediaType(acceptedMediaType);
				// 在另一个对象中查找与接受的媒体类型相等的索引
				int otherIndex = other.indexOfEqualMediaType(acceptedMediaType);
				// 比较相等的媒体类型
				int result = compareMatchingMediaTypes(this, thisIndex, other, otherIndex);
				// 如果比较结果不为 0，则返回结果
				if (result != 0) {
					return result;
				}

				// 在当前对象中查找包含接受的媒体类型的索引
				thisIndex = this.indexOfIncludedMediaType(acceptedMediaType);
				// 在另一个对象中查找包含接受的媒体类型的索引
				otherIndex = other.indexOfIncludedMediaType(acceptedMediaType);
				// 比较包含的媒体类型
				result = compareMatchingMediaTypes(this, thisIndex, other, otherIndex);
				// 如果比较结果不为 0，则返回结果
				if (result != 0) {
					return result;
				}
			}
			// 如果所有媒体类型都匹配，则返回 0
			return 0;
		} catch (HttpMediaTypeNotAcceptableException ex) {
			// 不应该发生
			throw new IllegalStateException("Cannot compare without having any requested media types", ex);
		}
	}

	@SuppressWarnings("unchecked")
	private List<MediaType> getAcceptedMediaTypes(HttpServletRequest request)
			throws HttpMediaTypeNotAcceptableException {

		// 从请求属性中获取媒体类型列表
		List<MediaType> result = (List<MediaType>) request.getAttribute(MEDIA_TYPES_ATTRIBUTE);
		// 如果媒体类型列表为空
		if (result == null) {
			// 使用内容协商管理器解析媒体类型列表
			result = this.contentNegotiationManager.resolveMediaTypes(new ServletWebRequest(request));
			// 将解析后的媒体类型列表设置到请求属性中
			request.setAttribute(MEDIA_TYPES_ATTRIBUTE, result);
		}
		// 返回媒体类型列表
		return result;
	}

	private int indexOfEqualMediaType(MediaType mediaType) {
		// 遍历要比较的表达式列表
		for (int i = 0; i < getExpressionsToCompare().size(); i++) {
			// 获取当前媒体类型
			MediaType currentMediaType = getExpressionsToCompare().get(i).getMediaType();
			// 如果当前媒体类型的类型和子类型与指定的媒体类型匹配，则返回索引
			if (mediaType.getType().equalsIgnoreCase(currentMediaType.getType()) &&
					mediaType.getSubtype().equalsIgnoreCase(currentMediaType.getSubtype())) {
				return i;
			}
		}
		// 如果没有找到匹配的媒体类型，则返回 -1
		return -1;
	}

	private int indexOfIncludedMediaType(MediaType mediaType) {
		// 遍历要比较的表达式列表
		for (int i = 0; i < getExpressionsToCompare().size(); i++) {
			// 如果指定的媒体类型包含当前表达式的媒体类型，则返回索引
			if (mediaType.includes(getExpressionsToCompare().get(i).getMediaType())) {
				return i;
			}
		}
		// 如果没有找到匹配的媒体类型，则返回 -1
		return -1;
	}

	private int compareMatchingMediaTypes(ProducesRequestCondition condition1, int index1,
										  ProducesRequestCondition condition2, int index2) {

		// 初始化结果为 0
		int result = 0;
		// 如果索引不相等
		if (index1 != index2) {
			// 计算结果为索引2减去索引1
			result = index2 - index1;
		} else if (index1 != -1) {
			// 如果索引不为 -1，获取要比较的表达式
			ProduceMediaTypeExpression expr1 = condition1.getExpressionsToCompare().get(index1);
			ProduceMediaTypeExpression expr2 = condition2.getExpressionsToCompare().get(index2);
			// 比较两个表达式
			result = expr1.compareTo(expr2);
			// 如果比较结果为 0，则比较媒体类型
			result = (result != 0) ? result : expr1.getMediaType().compareTo(expr2.getMediaType());
		}
		// 返回结果
		return result;
	}

	/**
	 * 返回包含的 "produces" 表达式，如果为空，则返回一个包含 {@value MediaType#ALL_VALUE} 表达式的列表。
	 */
	private List<ProduceMediaTypeExpression> getExpressionsToCompare() {
		return (this.expressions.isEmpty() ? MEDIA_TYPE_ALL_LIST : this.expressions);
	}


	/**
	 * 使用此方法清除包含解析的请求媒体类型的 {@link #MEDIA_TYPES_ATTRIBUTE}。
	 *
	 * @param request 当前请求
	 * @since 5.2
	 */
	public static void clearMediaTypesAttribute(HttpServletRequest request) {
		request.removeAttribute(MEDIA_TYPES_ATTRIBUTE);
	}


	/**
	 * 解析并匹配单个媒体类型表达式到请求的 'Accept' 头部。
	 */
	static class ProduceMediaTypeExpression extends AbstractMediaTypeExpression {

		ProduceMediaTypeExpression(MediaType mediaType, boolean negated) {
			super(mediaType, negated);
		}

		ProduceMediaTypeExpression(String expression) {
			super(expression);
		}

		/**
		 * 检查表达式是否与提供的接受媒体类型列表匹配。
		 *
		 * @param acceptedMediaTypes 请求的接受媒体类型列表
		 * @return 如果匹配返回 true；否则返回 false
		 */
		public final boolean match(List<MediaType> acceptedMediaTypes) {
			boolean match = matchMediaType(acceptedMediaTypes);
			return !isNegated() == match;
		}

		/**
		 * 检查表达式的媒体类型是否与提供的接受媒体类型列表中的任意一个兼容。
		 *
		 * @param acceptedMediaTypes 请求的接受媒体类型列表
		 * @return 如果有兼容的媒体类型返回 true；否则返回 false
		 */
		private boolean matchMediaType(List<MediaType> acceptedMediaTypes) {
			// 遍历所有接受的媒体类型
			for (MediaType acceptedMediaType : acceptedMediaTypes) {
				// 如果当前媒体类型与接受的媒体类型兼容并且参数匹配
				if (getMediaType().isCompatibleWith(acceptedMediaType) && matchParameters(acceptedMediaType)) {
					return true; // 返回 true 表示匹配成功
				}
			}
			// 如果没有任何兼容的媒体类型，则返回 false
			return false;
		}

		/**
		 * 检查表达式的媒体类型参数是否与提供的接受媒体类型的参数匹配。
		 *
		 * @param acceptedMediaType 请求的接受媒体类型
		 * @return 如果参数匹配返回 true；否则返回 false
		 */
		private boolean matchParameters(MediaType acceptedMediaType) {
			// 遍历当前媒体类型的所有参数名称
			for (String name : getMediaType().getParameters().keySet()) {
				// 获取当前媒体类型和接受的媒体类型中的参数值
				String s1 = getMediaType().getParameter(name);
				String s2 = acceptedMediaType.getParameter(name);
				// 如果两个参数值都存在且不相等，则返回 false
				if (StringUtils.hasText(s1) && StringUtils.hasText(s2) && !s1.equalsIgnoreCase(s2)) {
					return false;
				}
			}
			// 如果所有参数都匹配，则返回 true
			return true;
		}
	}

}
