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

import org.springframework.http.HttpHeaders;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.cors.reactive.CorsUtils;
import org.springframework.web.server.NotAcceptableStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;

import java.util.*;

/**
 * 逻辑或（' || '）请求条件，用于将请求的 'Content-Type' 头与媒体类型表达式列表匹配。
 * 支持两种类型的媒体类型表达式，这些表达式在 {@link RequestMapping#consumes()} 和 {@link RequestMapping#headers()} 中描述，
 * 其中头部名称为 'Content-Type'。无论使用哪种语法，语义都是相同的。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public final class ConsumesRequestCondition extends AbstractRequestCondition<ConsumesRequestCondition> {

	/**
	 * 表示空条件的静态常量
	 */
	private static final ConsumesRequestCondition EMPTY_CONDITION = new ConsumesRequestCondition();

	/**
	 * 媒体类型表达式列表
	 */
	private final List<ConsumeMediaTypeExpression> expressions;

	/**
	 * 是否需要请求体，默认为 true
	 */
	private boolean bodyRequired = true;


	/**
	 * 从 0 或多个 "consumes" 表达式创建新实例。
	 *
	 * @param consumes 使用 {@link RequestMapping#consumes()} 中描述的语法；如果未提供表达式，则该条件将匹配每个请求
	 */
	public ConsumesRequestCondition(String... consumes) {
		this(consumes, null);
	}

	/**
	 * 使用 "consumes" 和 "header" 表达式创建新实例。
	 * 忽略不是 'Content-Type' 的“header”表达式，或未定义头部值的表达式。
	 * 如果总共提供了 0 个表达式，则该条件将匹配每个请求
	 *
	 * @param consumes 如 {@link RequestMapping#consumes()} 中描述的内容
	 * @param headers  如 {@link RequestMapping#headers()} 中描述的内容
	 */
	public ConsumesRequestCondition(String[] consumes, String[] headers) {
		this.expressions = parseExpressions(consumes, headers);
		if (this.expressions.size() > 1) {
			Collections.sort(this.expressions);
		}
	}

	/**
	 * 解析媒体类型表达式。
	 *
	 * @param consumes 媒体类型表达式数组，如 {@link RequestMapping#consumes()} 中描述的
	 * @param headers  头部表达式数组，如 {@link RequestMapping#headers()} 中描述的
	 * @return 解析后的媒体类型表达式列表
	 */
	private static List<ConsumeMediaTypeExpression> parseExpressions(String[] consumes, String[] headers) {
		Set<ConsumeMediaTypeExpression> result = null;

		// 遍历头部表达式数组
		if (!ObjectUtils.isEmpty(headers)) {
			for (String header : headers) {
				// 解析头部表达式
				HeadersRequestCondition.HeaderExpression expr = new HeadersRequestCondition.HeaderExpression(header);
				// 如果头部名称为 'Content-Type'
				if ("Content-Type".equalsIgnoreCase(expr.name)) {
					// 初始化结果集合
					result = (result != null ? result : new LinkedHashSet<>());
					// 将解析的媒体类型表达式添加到结果集合中
					for (MediaType mediaType : MediaType.parseMediaTypes(expr.value)) {
						result.add(new ConsumeMediaTypeExpression(mediaType, expr.isNegated));
					}
				}
			}
		}

		// 遍历媒体类型表达式数组
		if (!ObjectUtils.isEmpty(consumes)) {
			// 初始化结果集合
			result = (result != null ? result : new LinkedHashSet<>());
			// 将解析的媒体类型表达式添加到结果集合中
			for (String consume : consumes) {
				result.add(new ConsumeMediaTypeExpression(consume));
			}
		}

		// 返回解析后的媒体类型表达式列表
		return (result != null ? new ArrayList<>(result) : Collections.emptyList());
	}


	/**
	 * 内部使用的私有构造函数，用于创建匹配条件。
	 *
	 * @param expressions 媒体类型表达式列表
	 */
	private ConsumesRequestCondition(List<ConsumeMediaTypeExpression> expressions) {
		this.expressions = expressions;
	}


	/**
	 * 返回包含的媒体类型表达式。
	 *
	 * @return 包含的媒体类型表达式集合
	 */
	public Set<MediaTypeExpression> getExpressions() {
		return new LinkedHashSet<>(this.expressions);
	}

	/**
	 * 返回此条件的媒体类型，不包括否定表达式。
	 *
	 * @return 此条件的可消耗媒体类型集合
	 */
	public Set<MediaType> getConsumableMediaTypes() {
		Set<MediaType> result = new LinkedHashSet<>();
		for (ConsumeMediaTypeExpression expression : this.expressions) {
			// 如果不是否定表达式，则添加媒体类型到结果集合中
			if (!expression.isNegated()) {
				result.add(expression.getMediaType());
			}
		}
		return result;
	}


	/**
	 * 判断条件是否包含任何媒体类型表达式。
	 *
	 * @return 如果条件为空，则返回 true；否则返回 false
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
	 * 判断此条件是否预期请求具有请求体。
	 * <p>默认情况下，此值为 {@code true}，假设需要请求体，此条件与 "Content-Type" 头匹配，或者回退到 "Content-Type: application/octet-stream"。
	 * <p>如果设置为 {@code false}，并且请求不包含请求体，则此条件将自动匹配，即无需检查表达式。
	 *
	 * @param bodyRequired 请求是否预期包含请求体
	 * @since 5.2
	 */
	public void setBodyRequired(boolean bodyRequired) {
		this.bodyRequired = bodyRequired;
	}

	/**
	 * 返回 {@link #setBodyRequired(boolean)} 的设置。
	 *
	 * @return 如果请求预期包含请求体，则返回 true；否则返回 false
	 * @since 5.2
	 */
	public boolean isBodyRequired() {
		return this.bodyRequired;
	}


	/**
	 * 如果 "other" 实例具有任何表达式，则返回 "other" 实例；否则返回 "this" 实例。
	 * 实际上，这意味着方法级别的 "consumes" 会覆盖类型级别的 "consumes" 条件。
	 *
	 * @param other 另一个 ConsumesRequestCondition 实例
	 * @return 如果 "other" 实例具有表达式，则返回 "other" 实例；否则返回 "this" 实例
	 */
	@Override
	public ConsumesRequestCondition combine(ConsumesRequestCondition other) {
		return (!other.expressions.isEmpty() ? other : this);
	}


	/**
	 * 检查是否有任何包含的媒体类型表达式与给定的请求 'Content-Type' 头匹配，并返回一个保证只包含匹配表达式的实例。
	 * 匹配是通过 {@link MediaType#includes(MediaType)} 进行的。
	 *
	 * @param exchange 当前的交换对象
	 * @return 如果条件不包含表达式，则返回相同实例；
	 * 如果包含匹配表达式的新实例；
	 * 如果没有表达式匹配，则返回 {@code null}
	 */
	@Override
	public ConsumesRequestCondition getMatchingCondition(ServerWebExchange exchange) {
		ServerHttpRequest request = exchange.getRequest();
		// 如果是预检请求，则返回空条件
		if (CorsUtils.isPreFlightRequest(request)) {
			return EMPTY_CONDITION;
		}
		// 如果条件为空，则返回当前实例
		if (isEmpty()) {
			return this;
		}
		// 如果请求不包含请求体且不需要请求体，则返回空条件
		if (!hasBody(request) && !this.bodyRequired) {
			return EMPTY_CONDITION;
		}
		// 获取匹配的表达式集合
		List<ConsumeMediaTypeExpression> result = getMatchingExpressions(exchange);
		// 如果匹配的表达式集合不为空，则返回一个新的 ConsumesRequestCondition 实例；否则返回 null
		return !CollectionUtils.isEmpty(result) ? new ConsumesRequestCondition(result) : null;
	}

	/**
	 * 检查请求是否包含请求体。
	 *
	 * @param request 服务器 HTTP 请求对象
	 * @return 如果请求包含请求体，则返回 true；否则返回 false
	 */
	private boolean hasBody(ServerHttpRequest request) {
		String contentLength = request.getHeaders().getFirst(HttpHeaders.CONTENT_LENGTH);
		String transferEncoding = request.getHeaders().getFirst(HttpHeaders.TRANSFER_ENCODING);
		return StringUtils.hasText(transferEncoding) ||
				(StringUtils.hasText(contentLength) && !contentLength.trim().equals("0"));
	}

	/**
	 * 获取匹配的媒体类型表达式列表。
	 *
	 * @param exchange 服务器 Web 交换对象
	 * @return 匹配的媒体类型表达式列表；如果没有匹配，则返回 null
	 */
	@Nullable
	private List<ConsumeMediaTypeExpression> getMatchingExpressions(ServerWebExchange exchange) {
		List<ConsumeMediaTypeExpression> result = null;
		for (ConsumeMediaTypeExpression expression : this.expressions) {
			// 如果表达式匹配，则添加到结果集合中
			if (expression.match(exchange)) {
				result = result != null ? result : new ArrayList<>();
				result.add(expression);
			}
		}
		return result;
	}

	/**
	 * 返回：
	 * <ul>
	 * <li>如果两个条件具有相同数量的表达式，则返回 0
	 * <li>如果“this”具有更多或更具体的媒体类型表达式，则返回小于 0 的值
	 * <li>如果“other”具有更多或更具体的媒体类型表达式，则返回大于 0 的值
	 * </ul>
	 * <p>假设两个实例均通过 {@link #getMatchingCondition(ServerWebExchange)} 获取，并且每个实例仅包含匹配的可消耗媒体类型表达式，否则为空。
	 */
	@Override
	public int compareTo(ConsumesRequestCondition other, ServerWebExchange exchange) {
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
	 * 解析并将单个媒体类型表达式与请求的 'Content-Type' 头匹配。
	 */
	static class ConsumeMediaTypeExpression extends AbstractMediaTypeExpression {

		ConsumeMediaTypeExpression(String expression) {
			super(expression);
		}

		ConsumeMediaTypeExpression(MediaType mediaType, boolean negated) {
			super(mediaType, negated);
		}

		/**
		 * 检查是否匹配媒体类型。
		 *
		 * @param exchange 当前的服务器Web交换对象
		 * @return 如果匹配，则返回 true；否则返回 false
		 * @throws NotAcceptableStatusException        如果不可接受，则抛出 NotAcceptableStatusException 异常
		 * @throws UnsupportedMediaTypeStatusException 如果不支持的媒体类型，则抛出 UnsupportedMediaTypeStatusException 异常
		 */
		@Override
		protected boolean matchMediaType(ServerWebExchange exchange) throws UnsupportedMediaTypeStatusException {
			try {
				// 获取请求的 'Content-Type' 头的媒体类型
				MediaType contentType = exchange.getRequest().getHeaders().getContentType();
				// 如果 'Content-Type' 为空，则将其设置为 APPLICATION_OCTET_STREAM
				contentType = (contentType != null ? contentType : MediaType.APPLICATION_OCTET_STREAM);
				// 检查当前媒体类型是否包含 'Content-Type' 头的媒体类型
				return getMediaType().includes(contentType);
			} catch (InvalidMediaTypeException ex) {
				// 如果无法解析 'Content-Type' 头的媒体类型，则抛出 UnsupportedMediaTypeStatusException 异常
				throw new UnsupportedMediaTypeStatusException("Can't parse Content-Type [" +
						exchange.getRequest().getHeaders().getFirst("Content-Type") +
						"]: " + ex.getMessage());
			}
		}
	}

}
