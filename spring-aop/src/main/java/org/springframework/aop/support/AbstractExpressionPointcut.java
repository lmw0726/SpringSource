/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.aop.support;

import java.io.Serializable;

import org.springframework.lang.Nullable;

/**
 * 表达式切点的抽象超类，提供位置和表达式属性。
 *
 * @author Rod Johnson
 * @author Rob Harrop
 * @since 2.0
 * @see #setLocation
 * @see #setExpression
 */
@SuppressWarnings("serial")
public abstract class AbstractExpressionPointcut implements ExpressionPointcut, Serializable {

	@Nullable
	private String location;

	@Nullable
	private String expression;


	/**
	 * 设置用于调试的位置。
	 */
	public void setLocation(@Nullable String location) {
		this.location = location;
	}

	/**
	 * 如果可用，返回关于切点表达式的位置信息。
	 * 这对调试很有用。
	 * @return 作为人类可读 String 的位置信息，如果没有可用信息则返回 {@code null}
	 */
	@Nullable
	public String getLocation() {
		return this.location;
	}

	public void setExpression(@Nullable String expression) {
		this.expression = expression;
		try {
			onSetExpression(expression);
		}
		catch (IllegalArgumentException ex) {
			// 如果可能，填充位置信息。
			if (this.location != null) {
				throw new IllegalArgumentException("Invalid expression at location [" + this.location + "]: " + ex);
			}
			else {
				throw ex;
			}
		}
	}

	/**
	 * 设置新的切点表达式时调用。
	 * 如果可能，应在此时解析表达式。
	 * <p>此实现为空。
	 * @param expression 要设置的表达式
	 * @throws IllegalArgumentException 如果表达式无效
	 * @see #setExpression
	 */
	protected void onSetExpression(@Nullable String expression) throws IllegalArgumentException {
	}

	/**
	 * 返回此切点的表达式。
	 */
	@Override
	@Nullable
	public String getExpression() {
		return this.expression;
	}

}
