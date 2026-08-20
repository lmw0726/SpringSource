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

import org.springframework.beans.PropertyAccessException;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * {@link BindingErrorProcessor} 的默认实现。
 *
 * <p>使用"required"错误码和字段名来解析字段缺失错误的消息码。
 *
 * <p>为给定的每个 {@code PropertyAccessException} 创建一个 {@code FieldError}，
 * 使用该异常的错误码（"typeMismatch"、"methodInvocation"）来解析消息码。
 *
 * @author Alef Arendsen
 * @author Juergen Hoeller
 * @since 1.2
 * @see #MISSING_FIELD_ERROR_CODE
 * @see DataBinder#setBindingErrorProcessor
 * @see BeanPropertyBindingResult#addError
 * @see BeanPropertyBindingResult#resolveMessageCodes
 * @see org.springframework.beans.PropertyAccessException#getErrorCode
 * @see org.springframework.beans.TypeMismatchException#ERROR_CODE
 * @see org.springframework.beans.MethodInvocationException#ERROR_CODE
 */
public class DefaultBindingErrorProcessor implements BindingErrorProcessor {

	/**
	 * 字段缺失错误（即在属性值列表中未找到的必填字段）将使用此错误码进行注册："required"。
	 */
	public static final String MISSING_FIELD_ERROR_CODE = "required";


	@Override
	public void processMissingFieldError(String missingField, BindingResult bindingResult) {
		// 使用"required"错误码创建字段错误。
		String fixedField = bindingResult.getNestedPath() + missingField;
		String[] codes = bindingResult.resolveMessageCodes(MISSING_FIELD_ERROR_CODE, missingField);
		Object[] arguments = getArgumentsForBindError(bindingResult.getObjectName(), fixedField);
		FieldError error = new FieldError(bindingResult.getObjectName(), fixedField, "", true,
				codes, arguments, "Field '" + fixedField + "' is required");
		bindingResult.addError(error);
	}

	@Override
	public void processPropertyAccessException(PropertyAccessException ex, BindingResult bindingResult) {
		// 使用异常的错误码（例如"typeMismatch"）创建字段错误。
		String field = ex.getPropertyName();
		Assert.state(field != null, "No field in exception");
		String[] codes = bindingResult.resolveMessageCodes(ex.getErrorCode(), field);
		Object[] arguments = getArgumentsForBindError(bindingResult.getObjectName(), field);
		Object rejectedValue = ex.getValue();
		if (ObjectUtils.isArray(rejectedValue)) {
			rejectedValue = StringUtils.arrayToCommaDelimitedString(ObjectUtils.toObjectArray(rejectedValue));
		}
		FieldError error = new FieldError(bindingResult.getObjectName(), field, rejectedValue, true,
				codes, arguments, ex.getLocalizedMessage());
		error.wrap(ex);
		bindingResult.addError(error);
	}

	/**
	 * 返回给定字段绑定错误的 FieldError 参数。
	 * 对每个缺失的必填字段和每个类型不匹配的情况都会调用此方法。
	 * <p>默认实现返回一个参数，表示字段名
	 * （类型为 DefaultMessageSourceResolvable，使用 "objectName.field" 和 "field" 作为码）。
	 * @param objectName 目标对象的名称
	 * @param field 导致绑定错误的字段
	 * @return 表示 FieldError 参数的 Object 数组
	 * @see org.springframework.validation.FieldError#getArguments
	 * @see org.springframework.context.support.DefaultMessageSourceResolvable
	 */
	protected Object[] getArgumentsForBindError(String objectName, String field) {
		String[] codes = new String[] {objectName + Errors.NESTED_PATH_SEPARATOR + field, field};
		return new Object[] {new DefaultMessageSourceResolvable(codes, field)};
	}

}
