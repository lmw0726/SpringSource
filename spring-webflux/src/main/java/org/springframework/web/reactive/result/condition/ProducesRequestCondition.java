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

import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.cors.reactive.CorsUtils;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;
import org.springframework.web.reactive.accept.RequestedContentTypeResolverBuilder;
import org.springframework.web.server.NotAcceptableStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;

import java.util.*;

/**
 * 逻辑或 (' || ') 请求条件，用于将请求的 'Accept' 头与媒体类型表达式列表进行匹配。
 * 支持两种媒体类型表达式，分别在 {@link RequestMapping#produces()} 和 {@link RequestMapping#headers()} 中描述，
 * 其中头部名称为 'Accept'。无论使用哪种语法，语义都是相同的。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public final class ProducesRequestCondition extends AbstractRequestCondition<ProducesRequestCondition> {

	/**
	 * 默认的请求内容类型解析器。
	 */
	private static final RequestedContentTypeResolver DEFAULT_CONTENT_TYPE_RESOLVER =
			new RequestedContentTypeResolverBuilder().build();

	/**
	 * 空的产生请求条件。
	 */
	private static final ProducesRequestCondition EMPTY_CONDITION = new ProducesRequestCondition();

	/**
	 * 媒体类型属性键名。
	 */
	private static final String MEDIA_TYPES_ATTRIBUTE = ProducesRequestCondition.class.getName() + ".MEDIA_TYPES";

	/**
	 * 包含 MediaType.ALL_VALUE 的媒体类型列表。
	 */
	private final List<ProduceMediaTypeExpression> mediaTypeAllList =
			Collections.singletonList(new ProduceMediaTypeExpression(MediaType.ALL_VALUE));

	/**
	 * 媒体类型表达式列表。
	 */
	private final List<ProduceMediaTypeExpression> expressions;

	/**
	 * 请求内容类型解析器。
	 */
	private final RequestedContentTypeResolver contentTypeResolver;


	/**
	 * 从 "produces" 表达式创建一个新实例。如果总共提供了 0 个表达式，此条件将匹配任何请求。
	 *
	 * @param produces 由 {@link RequestMapping#produces()} 定义语法的表达式
	 */
	public ProducesRequestCondition(String... produces) {
		this(produces, null);
	}

	/**
	 * 使用 "produces" 和 "header" 表达式创建一个新实例。忽略头部名称不是 'Accept' 或没有定义头部值的 "Header" 表达式。
	 * 如果总共提供了 0 个表达式，此条件将匹配任何请求。
	 *
	 * @param produces 由 {@link RequestMapping#produces()} 定义语法的表达式
	 * @param headers  由 {@link RequestMapping#headers()} 定义语法的表达式
	 */
	public ProducesRequestCondition(String[] produces, String[] headers) {
		this(produces, headers, null);
	}

	/**
	 * 与 {@link #ProducesRequestCondition(String[], String[])} 相同，但还接受 {@link ContentNegotiationManager}。
	 *
	 * @param produces 由 {@link RequestMapping#produces()} 定义语法的表达式
	 * @param headers  由 {@link RequestMapping#headers()} 定义语法的表达式
	 * @param resolver 用于确定请求的内容类型
	 */
	public ProducesRequestCondition(String[] produces, String[] headers, RequestedContentTypeResolver resolver) {
		this.expressions = parseExpressions(produces, headers);
		if (this.expressions.size() > 1) {
			Collections.sort(this.expressions);
		}
		this.contentTypeResolver = resolver != null ? resolver : DEFAULT_CONTENT_TYPE_RESOLVER;
	}

	/**
	 * 解析 "produces" 表达式和 "header" 表达式，返回产生媒体类型表达式列表。
	 *
	 * @param produces 由 {@link RequestMapping#produces()} 定义语法的表达式
	 * @param headers  由 {@link RequestMapping#headers()} 定义语法的表达式
	 * @return 产生媒体类型表达式列表
	 */
	private List<ProduceMediaTypeExpression> parseExpressions(String[] produces, String[] headers) {
		Set<ProduceMediaTypeExpression> result = null;

		// 解析 "header" 表达式
		if (!ObjectUtils.isEmpty(headers)) {
			for (String header : headers) {
				HeadersRequestCondition.HeaderExpression expr = new HeadersRequestCondition.HeaderExpression(header);

				// 检查头部名称是否为 'Accept'
				if ("Accept".equalsIgnoreCase(expr.name)) {
					for (MediaType mediaType : MediaType.parseMediaTypes(expr.value)) {
						if (result == null) {
							result = new LinkedHashSet<>();
						}
						result.add(new ProduceMediaTypeExpression(mediaType, expr.isNegated));
					}
				}
			}
		}

		// 解析 "produces" 表达式
		if (!ObjectUtils.isEmpty(produces)) {
			for (String produce : produces) {
				if (result == null) {
					result = new LinkedHashSet<>();
				}
				result.add(new ProduceMediaTypeExpression(produce));
			}
		}

		return (result != null ? new ArrayList<>(result) : Collections.emptyList());
	}


	/**
	 * 内部使用的私有构造函数，用于创建匹配条件。
	 * 注意，表达式列表既未排序，也未深度复制。
	 *
	 * @param expressions 表达式列表
	 * @param other       另一个 ProducesRequestCondition 实例
	 */
	private ProducesRequestCondition(List<ProduceMediaTypeExpression> expressions, ProducesRequestCondition other) {
		this.expressions = expressions;
		this.contentTypeResolver = other.contentTypeResolver;
	}


	/**
	 * 返回包含的 "produces" 表达式。
	 *
	 * @return "produces" 表达式集合
	 */
	public Set<MediaTypeExpression> getExpressions() {
		return new LinkedHashSet<>(this.expressions);
	}

	/**
	 * 返回包含的可产生媒体类型，不包括否定的表达式。
	 *
	 * @return 可产生媒体类型集合
	 */
	public Set<MediaType> getProducibleMediaTypes() {
		Set<MediaType> result = new LinkedHashSet<>();
		for (ProduceMediaTypeExpression expression : this.expressions) {
			if (!expression.isNegated()) {
				//如果不是否定表达式，则添加到可产生媒体类型集合
				result.add(expression.getMediaType());
			}
		}
		return result;
	}

	/**
	 * 条件是否包含任何媒体类型表达式。
	 *
	 * @return 如果没有表达式则返回 true，否则返回 false
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
	 * 如果 "other" 实例包含任何表达式，则返回 "other" 实例；否则返回当前实例。
	 * 实际上，这意味着方法级别的 "produces" 将覆盖类型级别的 "produces" 条件。
	 *
	 * @param other 另一个 ProducesRequestCondition 实例
	 * @return 如果 "other" 实例包含表达式，则返回 "other" 实例；否则返回当前实例
	 */
	@Override
	public ProducesRequestCondition combine(ProducesRequestCondition other) {
		return (!other.expressions.isEmpty() ? other : this);
	}

	/**
	 * 检查包含的任何媒体类型表达式是否与给定的请求的 'Content-Type' 标头匹配，并返回一个确保只包含匹配表达式的实例。
	 * 匹配通过 {@link MediaType#isCompatibleWith(MediaType)} 执行。
	 *
	 * @param exchange 当前交换对象
	 * @return 如果没有表达式，则返回相同实例；如果有匹配的表达式，则返回包含匹配表达式的新条件实例；如果没有匹配的表达式，则返回 null
	 */
	@Override
	@Nullable
	public ProducesRequestCondition getMatchingCondition(ServerWebExchange exchange) {
		if (CorsUtils.isPreFlightRequest(exchange.getRequest())) {
			// 如果是预检请求，返回空条件
			return EMPTY_CONDITION;
		}
		if (isEmpty()) {
			// 如果当前条件没有媒体类型表达式，返回当前实例
			return this;
		}
		List<ProduceMediaTypeExpression> result = getMatchingExpressions(exchange);
		if (!CollectionUtils.isEmpty(result)) {
			// 如果有匹配的表达式，返回一个新的条件实例
			return new ProducesRequestCondition(result, this);
		} else {
			try {
				// 尝试捕获异常，检查是否可以接受所有媒体类型
				if (MediaType.ALL.isPresentIn(getAcceptedMediaTypes(exchange))) {
					// 如果可以接受所有媒体类型，返回空条件
					return EMPTY_CONDITION;
				}
			} catch (NotAcceptableStatusException | UnsupportedMediaTypeStatusException ex) {
				// 捕获异常并忽略
			}
		}
		// 如果没有匹配的表达式且无法接受所有媒体类型，则返回null
		return null;

	}

	/**
	 * 获取匹配的媒体类型表达式
	 *
	 * @param exchange 当前的交换对象
	 * @return 匹配的媒体类型表达式列表，如果没有匹配的表达式则返回null
	 */
	@Nullable
	private List<ProduceMediaTypeExpression> getMatchingExpressions(ServerWebExchange exchange) {
		List<ProduceMediaTypeExpression> result = null;
		for (ProduceMediaTypeExpression expression : this.expressions) {
			if (expression.match(exchange)) {
				// 如果有匹配的表达式，添加到结果列表中
				if (null == result) {
					result = new ArrayList<>();
				}
				result.add(expression);
			}
		}
		return result;
	}


	/**
	 * 比较此条件和另一个"produces"条件的方法如下：
	 * <ol>
	 * <li>通过 {@link MediaType#sortByQualityValue(List)} 对'Accept'标头的媒体类型进行质量值排序，并遍历列表。
	 * <li>首先使用 {@link MediaType#equals(Object)}，然后使用 {@link MediaType#includes(MediaType)}，
	 * 获取每个"produces"条件中匹配媒体类型的第一个索引。
	 * <li>如果找到较低的索引，则该索引对应的条件胜出。
	 * <li>如果两个索引相等，则进一步使用 {@link MediaType#SPECIFICITY_COMPARATOR} 比较索引处的媒体类型。
	 * </ol>
	 * <p>假设两个实例均通过 {@link #getMatchingCondition(ServerWebExchange)} 获取，并且每个实例只包含匹配的可生产媒体类型表达式或为空。
	 *
	 * @param other    要比较的另一个"produces"条件
	 * @param exchange 当前交换对象
	 * @return 如果当前条件胜出，则返回正数；如果另一个条件胜出，则返回负数；如果相等，则返回0
	 */
	@Override
	public int compareTo(ProducesRequestCondition other, ServerWebExchange exchange) {
		try {
			// 获取请求中可接受的媒体类型列表
			List<MediaType> acceptedMediaTypes = getAcceptedMediaTypes(exchange);

			// 遍历每个可接受的媒体类型
			for (MediaType acceptedMediaType : acceptedMediaTypes) {
				// 获取在当前条件中匹配的媒体类型的索引
				int thisIndex = this.indexOfEqualMediaType(acceptedMediaType);
				int otherIndex = other.indexOfEqualMediaType(acceptedMediaType);

				// 比较匹配的媒体类型，如果不相等则返回比较结果
				int result = compareMatchingMediaTypes(this, thisIndex, other, otherIndex);
				if (result != 0) {
					return result;
				}

				// 获取在当前条件中包含的媒体类型的索引
				thisIndex = this.indexOfIncludedMediaType(acceptedMediaType);
				otherIndex = other.indexOfIncludedMediaType(acceptedMediaType);

				// 再次比较包含的媒体类型，如果不相等则返回比较结果
				result = compareMatchingMediaTypes(this, thisIndex, other, otherIndex);
				if (result != 0) {
					return result;
				}
			}

			// 所有可接受的媒体类型都匹配，返回0
			return 0;
		} catch (NotAcceptableStatusException ex) {
			// 不应该发生的异常，抛出 IllegalStateException
			throw new IllegalStateException("Cannot compare without having any requested media types", ex);
		}
	}

	/**
	 * 获取请求中可接受的媒体类型列表。
	 *
	 * @param exchange 当前交换对象
	 * @return 可接受的媒体类型列表
	 * @throws NotAcceptableStatusException 如果无法接受媒体类型，则抛出此异常
	 */
	private List<MediaType> getAcceptedMediaTypes(ServerWebExchange exchange) throws NotAcceptableStatusException {
		// 尝试从属性中获取已接受的媒体类型列表
		List<MediaType> result = exchange.getAttribute(MEDIA_TYPES_ATTRIBUTE);

		// 如果列表为空，则进行解析并存储到属性中
		if (result == null) {
			result = this.contentTypeResolver.resolveMediaTypes(exchange);
			exchange.getAttributes().put(MEDIA_TYPES_ATTRIBUTE, result);
		}

		// 返回解析或存储的媒体类型列表
		return result;
	}

	/**
	 * 查找与给定媒体类型相同的媒体类型在表达式列表中的索引位置。
	 *
	 * @param mediaType 要比较的媒体类型
	 * @return 如果找到相同的媒体类型，则返回其索引位置；否则返回 -1
	 */
	private int indexOfEqualMediaType(MediaType mediaType) {
		// 遍历表达式列表，比较每个媒体类型是否与给定媒体类型相同
		for (int i = 0; i < getExpressionsToCompare().size(); i++) {
			MediaType currentMediaType = getExpressionsToCompare().get(i).getMediaType();
			if (mediaType.getType().equalsIgnoreCase(currentMediaType.getType()) &&
					mediaType.getSubtype().equalsIgnoreCase(currentMediaType.getSubtype())) {
				// 找到相同的媒体类型，返回索引位置
				return i;
			}
		}
		// 未找到相同的媒体类型，返回 -1
		return -1;
	}


	/**
	 * 查找在表达式列表中包含给定媒体类型的媒体类型的索引位置。
	 *
	 * @param mediaType 要比较的媒体类型
	 * @return 如果找到包含给定媒体类型的媒体类型，则返回其索引位置；否则返回 -1
	 */
	private int indexOfIncludedMediaType(MediaType mediaType) {
		// 遍历表达式列表，检查每个媒体类型是否包含给定媒体类型
		for (int i = 0; i < getExpressionsToCompare().size(); i++) {
			if (mediaType.includes(getExpressionsToCompare().get(i).getMediaType())) {
				// 找到包含给定媒体类型的媒体类型，返回索引位置
				return i;
			}
		}
		// 未找到包含给定媒体类型的媒体类型，返回 -1
		return -1;
	}


	/**
	 * 比较匹配的媒体类型的条件。
	 *
	 * @param condition1 条件1
	 * @param index1     条件1中匹配的媒体类型索引
	 * @param condition2 条件2
	 * @param index2     条件2中匹配的媒体类型索引
	 * @return 如果索引不相等，则返回索引差；如果索引相等，则比较媒体类型表达式的优先级和媒体类型
	 */
	private int compareMatchingMediaTypes(ProducesRequestCondition condition1, int index1,
										  ProducesRequestCondition condition2, int index2) {

		int result = 0;
		if (index1 != index2) {
			// 如果索引不相等，返回索引差
			result = index2 - index1;
		} else if (index1 != -1) {
			// 如果索引相等且不为 -1，进一步比较媒体类型表达式的优先级和媒体类型
			ProduceMediaTypeExpression expr1 = condition1.getExpressionsToCompare().get(index1);
			ProduceMediaTypeExpression expr2 = condition2.getExpressionsToCompare().get(index2);
			// 比较媒体类型表达式的优先级
			result = expr1.compareTo(expr2);
			// 如果优先级相同，则比较媒体类型
			result = (result != 0 ? result : expr1.getMediaType().compareTo(expr2.getMediaType()));
		}
		return result;
	}


	/**
	 * 返回包含的“produces”表达式，如果为空，则返回一个包含 {@value MediaType#ALL_VALUE} 表达式的列表。
	 *
	 * @return 包含的“produces”表达式，如果为空，则为包含 {@value MediaType#ALL_VALUE} 表达式的列表
	 */
	private List<ProduceMediaTypeExpression> getExpressionsToCompare() {
		return (this.expressions.isEmpty() ? this.mediaTypeAllList : this.expressions);
	}


	/**
	 * 使用此方法清除包含已解析的请求媒体类型的 {@link #MEDIA_TYPES_ATTRIBUTE}。
	 *
	 * @param exchange 当前的交换信息
	 * @since 5.2
	 */
	public static void clearMediaTypesAttribute(ServerWebExchange exchange) {
		exchange.getAttributes().remove(MEDIA_TYPES_ATTRIBUTE);
	}


	/**
	 * 解析和匹配单个媒体类型表达式到请求的 'Accept' 头。
	 */
	class ProduceMediaTypeExpression extends AbstractMediaTypeExpression {

		/**
		 * 构造函数，根据媒体类型和否定标志创建实例。
		 *
		 * @param mediaType 媒体类型
		 * @param negated   是否否定
		 */
		ProduceMediaTypeExpression(MediaType mediaType, boolean negated) {
			super(mediaType, negated);
		}

		/**
		 * 构造函数，根据表达式创建实例。
		 *
		 * @param expression 媒体类型表达式
		 */
		ProduceMediaTypeExpression(String expression) {
			super(expression);
		}

		/**
		 * 匹配媒体类型的具体实现。
		 *
		 * @param exchange 当前的交换对象
		 * @return 如果媒体类型匹配，则返回 true；否则返回 false
		 * @throws NotAcceptableStatusException 如果不可接受的状态异常
		 */
		@Override
		protected boolean matchMediaType(ServerWebExchange exchange) throws NotAcceptableStatusException {
			// 获取请求可接受的媒体类型列表
			List<MediaType> acceptedMediaTypes = getAcceptedMediaTypes(exchange);

			// 遍历请求可接受的媒体类型
			for (MediaType acceptedMediaType : acceptedMediaTypes) {
				// 如果媒体类型兼容且参数匹配，则返回 true
				if (getMediaType().isCompatibleWith(acceptedMediaType) && matchParameters(acceptedMediaType)) {
					return true;
				}
			}

			// 没有匹配的情况下返回 false
			return false;
		}

		/**
		 * 匹配媒体类型的参数。
		 *
		 * @param acceptedMediaType 请求可接受的媒体类型
		 * @return 如果参数匹配，则返回 true；否则返回 false
		 */
		private boolean matchParameters(MediaType acceptedMediaType) {
			// 遍历媒体类型的参数名
			for (String name : getMediaType().getParameters().keySet()) {
				String s1 = getMediaType().getParameter(name);
				String s2 = acceptedMediaType.getParameter(name);
				// 如果参数值都有文本并且不相等，则返回 false
				if (StringUtils.hasText(s1) && StringUtils.hasText(s2) && !s1.equalsIgnoreCase(s2)) {
					return false;
				}
			}

			// 所有参数匹配返回 true
			return true;
		}
	}

}
