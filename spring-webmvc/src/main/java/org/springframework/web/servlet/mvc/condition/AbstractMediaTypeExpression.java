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
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 支持 {@link RequestMapping#consumes()} 和 {@link RequestMapping#produces()} 中描述的媒体类型表达式。
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.1
 */
abstract class AbstractMediaTypeExpression implements MediaTypeExpression, Comparable<AbstractMediaTypeExpression> {

	/**
	 * 媒体类型
	 */
	private final MediaType mediaType;

	/**
	 * 是否是取反的
	 */
	private final boolean isNegated;


	AbstractMediaTypeExpression(String expression) {
		// 如果表达式以 "!" 开头
		if (expression.startsWith("!")) {
			// 设置 isNegated 为 true
			this.isNegated = true;
			// 去除表达式中的 "!" 符号
			expression = expression.substring(1);
		} else {
			// 否则，设置 isNegated 为 false
			this.isNegated = false;
		}
		// 解析表达式为 MediaType 对象
		this.mediaType = MediaType.parseMediaType(expression);
	}

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
