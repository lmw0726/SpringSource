/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.validation;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * 封装字段错误，即拒绝特定字段值的原因。
 *
 * <p>有关如何为 {@code FieldError} 构建消息代码列表的详细信息，请参见 {@link DefaultMessageCodesResolver} 的 Javadoc。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2003-03-10
 * @see DefaultMessageCodesResolver
 */
@SuppressWarnings("serial")
public class FieldError extends ObjectError {
	/**
	 * 字段名
	 */
	private final String field;
	/**
	 * 拒绝的值
	 */
	@Nullable
	private final Object rejectedValue;

	/**
	 * 是否绑定错误
	 */
	private final boolean bindingFailure;


	/**
	 * 创建一个新的 FieldError 实例。
	 *
	 * @param objectName       受影响对象的名称
	 * @param field            对象的受影响字段
	 * @param defaultMessage   用于解析此消息的默认消息
	 */
	public FieldError(String objectName, String field, String defaultMessage) {
		this(objectName, field, null, false, null, null, defaultMessage);
	}

	/**
	 * 创建一个新的 FieldError 实例。
	 *
	 * @param objectName       受影响对象的名称
	 * @param field            对象的受影响字段
	 * @param rejectedValue    拒绝的字段值
	 * @param bindingFailure   是否此错误表示绑定失败（如类型不匹配）；否则，它是验证失败
	 * @param codes            用于解析此消息的代码
	 * @param arguments        用于解析此消息的参数数组
	 * @param defaultMessage   用于解析此消息的默认消息
	 */
	public FieldError(String objectName, String field, @Nullable Object rejectedValue, boolean bindingFailure,
					  @Nullable String[] codes, @Nullable Object[] arguments, @Nullable String defaultMessage) {

		super(objectName, codes, arguments, defaultMessage);
		Assert.notNull(field, "Field must not be null");
		this.field = field;
		this.rejectedValue = rejectedValue;
		this.bindingFailure = bindingFailure;
	}


	/**
	 * 返回对象的受影响字段。
	 */
	public String getField() {
		return this.field;
	}

	/**
	 * 返回被拒绝的字段值。
	 */
	@Nullable
	public Object getRejectedValue() {
		return this.rejectedValue;
	}

	/**
	 * 返回此错误是否表示绑定失败（如类型不匹配）；否则它是验证失败。
	 */
	public boolean isBindingFailure() {
		return this.bindingFailure;
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!super.equals(other)) {
			return false;
		}
		FieldError otherError = (FieldError) other;
		return (getField().equals(otherError.getField()) &&
				ObjectUtils.nullSafeEquals(getRejectedValue(), otherError.getRejectedValue()) &&
				isBindingFailure() == otherError.isBindingFailure());
	}

	@Override
	public int hashCode() {
		int hashCode = super.hashCode();
		hashCode = 29 * hashCode + getField().hashCode();
		hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(getRejectedValue());
		hashCode = 29 * hashCode + (isBindingFailure() ? 1 : 0);
		return hashCode;
	}

	@Override
	public String toString() {
		return "Field error in object '" + getObjectName() + "' on field '" + this.field +
				"': rejected value [" + ObjectUtils.nullSafeToString(this.rejectedValue) + "]; " +
				resolvableToString();
	}

}
