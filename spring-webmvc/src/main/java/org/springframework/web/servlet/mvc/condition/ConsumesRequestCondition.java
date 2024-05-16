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

import org.springframework.http.HttpHeaders;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.servlet.mvc.condition.HeadersRequestCondition.HeaderExpression;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * 逻辑或（{@code ' || ' }）请求条件，将请求的'Content-Type'头与媒体类型表达式列表进行匹配。支持两种类型的媒体类型表达式，
 * 这些表达式在{@link RequestMapping#consumes()}和{@link RequestMapping#headers()}中描述，其中头部名称为'Content-Type'。
 * 无论使用哪种语法，语义都是相同的。
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public final class ConsumesRequestCondition extends AbstractRequestCondition<ConsumesRequestCondition> {

	/**
	 * 空条件
	 */
	private static final ConsumesRequestCondition EMPTY_CONDITION = new ConsumesRequestCondition();

	/**
	 * 包含消费媒体类型表达式的列表。
	 */
	private final List<ConsumeMediaTypeExpression> expressions;

	/**
	 * 是否需要请求体的标志，默认为true。
	 */
	private boolean bodyRequired = true;


	/**
	 * 从0个或更多个“consumes”表达式创建一个新实例。
	 *
	 * @param consumes 使用{@link RequestMapping#consumes()}中描述的语法；如果提供了0个表达式，则条件将匹配到每个请求
	 */
	public ConsumesRequestCondition(String... consumes) {
		this(consumes, null);
	}

	/**
	 * 使用“consumes”和“header”表达式创建一个新实例。如果提供的总表达式数量为0，则条件将匹配每个请求。
	 * 忽略头部名称不是'Content-Type'或没有定义头部值的“Header”表达式。
	 *
	 * @param consumes 如{@link RequestMapping#consumes()}中描述的方式
	 * @param headers  如{@link RequestMapping#headers()}中描述的方式
	 */
	public ConsumesRequestCondition(String[] consumes, @Nullable String[] headers) {
		this.expressions = parseExpressions(consumes, headers);
		if (this.expressions.size() > 1) {
			Collections.sort(this.expressions);
		}
	}

	private static List<ConsumeMediaTypeExpression> parseExpressions(String[] consumes, @Nullable String[] headers) {
		// 初始化结果集合
		Set<ConsumeMediaTypeExpression> result = null;
		// 如果请求头不为空，则遍历请求头
		if (!ObjectUtils.isEmpty(headers)) {
			for (String header : headers) {
				// 解析请求头表达式
				HeaderExpression expr = new HeaderExpression(header);
				// 如果请求头为 "Content-Type" 且值不为空
				if ("Content-Type".equalsIgnoreCase(expr.name) && expr.value != null) {
					// 初始化结果集合（如果为空）
					result = (result != null ? result : new LinkedHashSet<>());
					// 解析请求头中的媒体类型并添加到结果集合
					for (MediaType mediaType : MediaType.parseMediaTypes(expr.value)) {
						result.add(new ConsumeMediaTypeExpression(mediaType, expr.isNegated));
					}
				}
			}
		}
		// 如果consumes不为空，则遍历consumes
		if (!ObjectUtils.isEmpty(consumes)) {
			// 初始化结果集合（如果为空）
			result = (result != null ? result : new LinkedHashSet<>());
			// 将consumes中的媒体类型添加到结果集合
			for (String consume : consumes) {
				result.add(new ConsumeMediaTypeExpression(consume));
			}
		}
		// 将结果集合转换为列表返回（如果结果集合不为空），否则返回空列表
		return (result != null ? new ArrayList<>(result) : Collections.emptyList());
	}

	/**
	 * 用于在创建匹配条件时的私有构造函数。
	 * 注意，表达式列表既不排序也不深度复制。
	 *
	 * @param expressions 匹配条件的媒体类型表达式列表
	 */
	private ConsumesRequestCondition(List<ConsumeMediaTypeExpression> expressions) {
		this.expressions = expressions;
	}


	/**
	 * 返回包含的MediaType表达式。
	 */
	public Set<MediaTypeExpression> getExpressions() {
		return new LinkedHashSet<>(this.expressions);
	}

	/**
	 * 返回此条件的可消费媒体类型，不包括否定表达式。
	 */
	public Set<MediaType> getConsumableMediaTypes() {
		// 初始化结果集合
		Set<MediaType> result = new LinkedHashSet<>();
		// 遍历表达式集合
		for (ConsumeMediaTypeExpression expression : this.expressions) {
			// 如果表达式未被否定，则将其媒体类型添加到结果集合
			if (!expression.isNegated()) {
				result.add(expression.getMediaType());
			}
		}
		// 返回结果集合
		return result;
	}

	/**
	 * 是否具有任何媒体类型表达式的条件。
	 */
	@Override
	public boolean isEmpty() {
		return this.expressions.isEmpty();
	}

	@Override
	protected Collection<ConsumeMediaTypeExpression> getContent() {
		return this.expressions;
	}

	@Override
	protected String getToStringInfix() {
		return " || ";
	}

	/**
	 * 此条件是否期望请求具有正文。
	 * 默认情况下，假设请求正文是必需的，并且此条件匹配“Content-Type”头，或者回退到“Content-Type: application/octet-stream”。
	 * 如果设置为{@code false}，并且请求没有正文，则此条件将自动匹配，即无需检查表达式。
	 *
	 * @param bodyRequired 是否期望请求具有正文
	 * @since 5.2
	 */
	public void setBodyRequired(boolean bodyRequired) {
		this.bodyRequired = bodyRequired;
	}

	/**
	 * 返回{@link #setBodyRequired(boolean)}的设置。
	 *
	 * @since 5.2
	 */
	public boolean isBodyRequired() {
		return this.bodyRequired;
	}


	/**
	 * 如果“other”实例具有任何表达式，则返回“other”实例；否则返回“this”实例。
	 * 在实践中，这意味着方法级别的“consumes”将覆盖类型级别的“consumes”条件。
	 */
	@Override
	public ConsumesRequestCondition combine(ConsumesRequestCondition other) {
		return (!other.expressions.isEmpty() ? other : this);
	}

	/**
	 * 检查是否有任何包含的媒体类型表达式与给定的请求“Content-Type”头匹配，并返回一个保证只包含匹配表达式的实例。
	 * 匹配是通过{@link MediaType#includes(MediaType)}进行的。
	 *
	 * @param request 当前请求
	 * @return 如果条件不包含任何表达式，则返回相同的实例；或者仅包含匹配表达式的新实例；如果没有表达式匹配，则返回{@code null}
	 */
	@Override
	@Nullable
	public ConsumesRequestCondition getMatchingCondition(HttpServletRequest request) {
		// 检查是否是预检请求，如果是，返回一个空条件
		if (CorsUtils.isPreFlightRequest(request)) {
			return EMPTY_CONDITION;
		}

		// 如果当前条件为空，则返回当前条件
		if (isEmpty()) {
			return this;
		}

		// 如果请求没有主体，并且主体不是必需的，则返回一个空条件
		if (!hasBody(request) && !this.bodyRequired) {
			return EMPTY_CONDITION;
		}
		// 通用的媒体类型被缓存在MimeTypeUtils的级别上
		// 尝试解析请求的内容类型，如果无法解析，则返回null
		MediaType contentType;
		try {
			contentType = StringUtils.hasLength(request.getContentType()) ?
					MediaType.parseMediaType(request.getContentType()) :
					MediaType.APPLICATION_OCTET_STREAM;
		} catch (InvalidMediaTypeException ex) {
			return null;
		}

		// 获取与请求内容类型匹配的表达式列表
		List<ConsumeMediaTypeExpression> result = getMatchingExpressions(contentType);

		// 如果匹配的表达式列表不为空，则返回一个新的消费条件
		return !CollectionUtils.isEmpty(result) ? new ConsumesRequestCondition(result) : null;
	}

	private boolean hasBody(HttpServletRequest request) {
		// 获取请求头中的 Content-Length 和 Transfer-Encoding
		String contentLength = request.getHeader(HttpHeaders.CONTENT_LENGTH);
		String transferEncoding = request.getHeader(HttpHeaders.TRANSFER_ENCODING);

		// 如果 Transfer-Encoding 存在，或者 Content-Length 存在且不是 "0"，则返回 true
		return StringUtils.hasText(transferEncoding) ||
				(StringUtils.hasText(contentLength) && !contentLength.trim().equals("0"));
	}

	@Nullable
	private List<ConsumeMediaTypeExpression> getMatchingExpressions(MediaType contentType) {
		// 初始化结果列表
		List<ConsumeMediaTypeExpression> result = null;

		// 遍历当前请求条件中的媒体类型表达式
		for (ConsumeMediaTypeExpression expression : this.expressions) {
			// 如果当前媒体类型表达式匹配请求的内容类型
			if (expression.match(contentType)) {
				// 如果结果列表为 null，则初始化
				result = result != null ? result : new ArrayList<>();
				// 将匹配的媒体类型表达式添加到结果列表中
				result.add(expression);
			}
		}

		// 返回结果列表
		return result;
	}

	/**
	 * 返回：
	 * <ul>
	 * <li>如果两个条件具有相同数量的表达式，则返回0
	 * <li>如果“this”具有更多或更具体的媒体类型表达式，则返回小于0
	 * <li>如果“other”具有更多或更具体的媒体类型表达式，则返回大于0
	 * </ul>
	 * <p>假设两个实例都是通过{@link #getMatchingCondition(HttpServletRequest)}获得的，
	 * 每个实例只包含匹配的可消费媒体类型表达式或为空。
	 */
	@Override
	public int compareTo(ConsumesRequestCondition other, HttpServletRequest request) {
		if (this.expressions.isEmpty() && other.expressions.isEmpty()) {
			return 0;
		} else if (this.expressions.isEmpty()) {
			return 1;
		} else if (other.expressions.isEmpty()) {
			return -1;
		} else {
			return this.expressions.get(0).compareTo(other.expressions.get(0));
		}
	}


	/**
	 * 解析并匹配单个媒体类型表达式到请求的 'Content-Type' 头部。
	 */
	static class ConsumeMediaTypeExpression extends AbstractMediaTypeExpression {

		/**
		 * 使用字符串表达式构造一个 ConsumeMediaTypeExpression 实例。
		 *
		 * @param expression 媒体类型表达式
		 */
		ConsumeMediaTypeExpression(String expression) {
			super(expression);
		}

		/**
		 * 使用 MediaType 实例和是否取反标志构造一个 ConsumeMediaTypeExpression 实例。
		 *
		 * @param mediaType 媒体类型
		 * @param negated   是否取反
		 */
		ConsumeMediaTypeExpression(MediaType mediaType, boolean negated) {
			super(mediaType, negated);
		}

		/**
		 * 检查表达式是否与提供的内容类型匹配。
		 *
		 * @param contentType 请求的内容类型
		 * @return 如果匹配返回 true；否则返回 false
		 */
		public final boolean match(MediaType contentType) {
			boolean match = getMediaType().includes(contentType);
			return !isNegated() == match;
		}
	}

}
