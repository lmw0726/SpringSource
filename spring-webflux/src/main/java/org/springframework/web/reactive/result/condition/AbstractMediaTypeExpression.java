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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.NotAcceptableStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;

/**
 * 支持媒体类型表达式，如 {@link RequestMapping#consumes()} 和 {@link RequestMapping#produces()} 中描述的。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
abstract class AbstractMediaTypeExpression implements Comparable<AbstractMediaTypeExpression>, MediaTypeExpression {
	/**
	 * 媒体类型
	 */
	private final MediaType mediaType;
	/**
	 * 是否为否定表达式
	 */
	private final boolean isNegated;

	/**
	 * 构造函数，根据表达式初始化媒体类型和否定标志。
	 *
	 * @param expression 媒体类型表达式
	 */
	AbstractMediaTypeExpression(String expression) {
		if (expression.startsWith("!")) {
			//如果表达式以 ! 开头
			this.isNegated = true;
			//表达式为!后的字符串
			expression = expression.substring(1);
		} else {
			//不是否定表达式
			this.isNegated = false;
		}
		//解析媒体类型
		this.mediaType = MediaType.parseMediaType(expression);
	}

	/**
	 * 构造函数，根据媒体类型和否定标志初始化对象。
	 *
	 * @param mediaType 媒体类型
	 * @param negated   是否为否定表达式
	 */
	AbstractMediaTypeExpression(MediaType mediaType, boolean negated) {
		this.mediaType = mediaType;
		this.isNegated = negated;
	}


	@Override
	public MediaType getMediaType() {
		return this.mediaType;
	}

	@Override
	public boolean isNegated() {
		return this.isNegated;
	}


	/**
	 * 判断是否匹配媒体类型。
	 *
	 * @param exchange 当前的服务器Web交换对象
	 * @return 如果匹配，则返回 true；否则返回 false
	 */
	public final boolean match(ServerWebExchange exchange) {
		try {
			// 调用 matchMediaType 方法检查是否匹配媒体类型
			boolean match = matchMediaType(exchange);
			// 检查匹配结果是否与是否否定标志相符，并返回结果
			return (!this.isNegated == match);
		} catch (NotAcceptableStatusException | UnsupportedMediaTypeStatusException ex) {
			// 如果发生 NotAcceptableStatusException 或 UnsupportedMediaTypeStatusException 异常，则返回 false
			return false;
		}
	}

	/**
	 * 抽象方法，用于检查是否匹配媒体类型。
	 *
	 * @param exchange 当前的服务器Web交换对象
	 * @return 如果匹配，则返回 true；否则返回 false
	 * @throws NotAcceptableStatusException     如果不可接受，则抛出 NotAcceptableStatusException 异常
	 * @throws UnsupportedMediaTypeStatusException 如果不支持的媒体类型，则抛出 UnsupportedMediaTypeStatusException 异常
	 */
	protected abstract boolean matchMediaType(ServerWebExchange exchange)
			throws NotAcceptableStatusException, UnsupportedMediaTypeStatusException;


	@Override
	public int compareTo(AbstractMediaTypeExpression other) {
		return MediaType.SPECIFICITY_COMPARATOR.compare(this.getMediaType(), other.getMediaType());
	}

	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (other == null || getClass() != other.getClass()) {
			return false;
		}
		AbstractMediaTypeExpression otherExpr = (AbstractMediaTypeExpression) other;
		return (this.mediaType.equals(otherExpr.mediaType) && this.isNegated == otherExpr.isNegated);
	}

	@Override
	public int hashCode() {
		return this.mediaType.hashCode();
	}

	@Override
	public String toString() {
		if (this.isNegated) {
			return '!' + this.mediaType.toString();
		}
		return this.mediaType.toString();
	}

}
