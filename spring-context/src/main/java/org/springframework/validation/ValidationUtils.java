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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * 提供便捷方法的工具类，用于调用 {@link Validator} 和拒绝空字段。
 *
 * <p>在 {@code Validator} 实现中检查空字段的操作，
 * 使用 {@link #rejectIfEmpty} 或 {@link #rejectIfEmptyOrWhitespace} 时可以变成单行代码。
 *
 * @author Juergen Hoeller
 * @author Dmitriy Kopylenko
 * @since 06.05.2003
 * @see Validator
 * @see Errors
 */
public abstract class ValidationUtils {

	private static final Log logger = LogFactory.getLog(ValidationUtils.class);


	/**
	 * 为给定对象和 {@link Errors} 实例调用指定的 {@link Validator}。
	 * @param validator 要调用的 {@code Validator}
	 * @param target 要绑定参数的对象
	 * @param errors 用于存储错误的 {@link Errors} 实例
	 * @throws IllegalArgumentException 如果 {@code Validator} 或 {@code Errors}
	 * 参数为 {@code null}，或者指定的 {@code Validator} 不支持验证所给对象的类型
	 */
	public static void invokeValidator(Validator validator, Object target, Errors errors) {
		invokeValidator(validator, target, errors, (Object[]) null);
	}

	/**
	 * 为给定对象和 {@link Errors} 实例调用指定的 {@link Validator}/{@link SmartValidator}。
	 * @param validator 要调用的 {@code Validator}
	 * @param target 要绑定参数的对象
	 * @param errors 用于存储错误的 {@link Errors} 实例
	 * @param validationHints 要传递给验证引擎的一个或多个提示对象
	 * @throws IllegalArgumentException 如果 {@code Validator} 或 {@code Errors}
	 * 参数为 {@code null}，或者指定的 {@code Validator} 不支持验证所给对象的类型
	 */
	public static void invokeValidator(
			Validator validator, Object target, Errors errors, @Nullable Object... validationHints) {

		Assert.notNull(validator, "Validator must not be null");
		Assert.notNull(target, "Target object must not be null");
		Assert.notNull(errors, "Errors object must not be null");

		if (logger.isDebugEnabled()) {
			logger.debug("Invoking validator [" + validator + "]");
		}
		if (!validator.supports(target.getClass())) {
			throw new IllegalArgumentException(
					"Validator [" + validator.getClass() + "] does not support [" + target.getClass() + "]");
		}

		if (!ObjectUtils.isEmpty(validationHints) && validator instanceof SmartValidator) {
			((SmartValidator) validator).validate(target, errors, validationHints);
		}
		else {
			validator.validate(target, errors);
		}

		if (logger.isDebugEnabled()) {
			if (errors.hasErrors()) {
				logger.debug("Validator found " + errors.getErrorCount() + " errors");
			}
			else {
				logger.debug("Validator found no errors");
			}
		}
	}


	/**
	 * 如果值为空，则使用给定的错误代码拒绝该字段。
	 * <p>此处"空"值的含义为 {@code null} 或空字符串 ""。
	 * <p>不需要传入被验证字段所属的对象，
	 * 因为 {@link Errors} 实例可以自行解析字段值（它通常持有对目标对象的内部引用）。
	 * @param errors 用于注册错误的 {@code Errors} 实例
	 * @param field 要检查的字段名
	 * @param errorCode 错误代码，可作为消息键使用
	 */
	public static void rejectIfEmpty(Errors errors, String field, String errorCode) {
		rejectIfEmpty(errors, field, errorCode, null, null);
	}

	/**
	 * 如果值为空，则使用给定的错误代码和默认消息拒绝该字段。
	 * <p>此处"空"值的含义为 {@code null} 或空字符串 ""。
	 * <p>不需要传入被验证字段所属的对象，
	 * 因为 {@link Errors} 实例可以自行解析字段值（它通常持有对目标对象的内部引用）。
	 * @param errors 用于注册错误的 {@code Errors} 实例
	 * @param field 要检查的字段名
	 * @param errorCode 错误代码，可作为消息键使用
	 * @param defaultMessage 回退的默认消息
	 */
	public static void rejectIfEmpty(Errors errors, String field, String errorCode, String defaultMessage) {
		rejectIfEmpty(errors, field, errorCode, null, defaultMessage);
	}

	/**
	 * 如果值为空，则使用给定的错误代码和错误参数拒绝该字段。
	 * <p>此处"空"值的含义为 {@code null} 或空字符串 ""。
	 * <p>不需要传入被验证字段所属的对象，
	 * 因为 {@link Errors} 实例可以自行解析字段值（它通常持有对目标对象的内部引用）。
	 * @param errors 用于注册错误的 {@code Errors} 实例
	 * @param field 要检查的字段名
	 * @param errorCode 错误代码，可作为消息键使用
	 * @param errorArgs 错误参数，用于通过 MessageFormat 进行参数绑定
	 * （可以为 {@code null}）
	 */
	public static void rejectIfEmpty(Errors errors, String field, String errorCode, Object[] errorArgs) {
		rejectIfEmpty(errors, field, errorCode, errorArgs, null);
	}

	/**
	 * 如果值为空，则使用给定的错误代码、错误参数和默认消息拒绝该字段。
	 * <p>此处"空"值的含义为 {@code null} 或空字符串 ""。
	 * <p>不需要传入被验证字段所属的对象，
	 * 因为 {@link Errors} 实例可以自行解析字段值（它通常持有对目标对象的内部引用）。
	 * @param errors 用于注册错误的 {@code Errors} 实例
	 * @param field 要检查的字段名
	 * @param errorCode 错误代码，可作为消息键使用
	 * @param errorArgs 错误参数，用于通过 MessageFormat 进行参数绑定
	 * （可以为 {@code null}）
	 * @param defaultMessage 回退的默认消息
	 */
	public static void rejectIfEmpty(Errors errors, String field, String errorCode,
			@Nullable Object[] errorArgs, @Nullable String defaultMessage) {

		Assert.notNull(errors, "Errors object must not be null");
		Object value = errors.getFieldValue(field);
		if (value == null || !StringUtils.hasLength(value.toString())) {
			errors.rejectValue(field, errorCode, errorArgs, defaultMessage);
		}
	}

	/**
	 * 如果值为空或仅包含空白字符，则使用给定的错误代码拒绝该字段。
	 * <p>此处"空"值的含义为 {@code null}、空字符串 "" 或完全由空白字符组成。
	 * <p>不需要传入被验证字段所属的对象，
	 * 因为 {@link Errors} 实例可以自行解析字段值（它通常持有对目标对象的内部引用）。
	 * @param errors 用于注册错误的 {@code Errors} 实例
	 * @param field 要检查的字段名
	 * @param errorCode 错误代码，可作为消息键使用
	 */
	public static void rejectIfEmptyOrWhitespace(Errors errors, String field, String errorCode) {
		rejectIfEmptyOrWhitespace(errors, field, errorCode, null, null);
	}

	/**
	 * 如果值为空或仅包含空白字符，则使用给定的错误代码和默认消息拒绝该字段。
	 * <p>此处"空"值的含义为 {@code null}、空字符串 "" 或完全由空白字符组成。
	 * <p>不需要传入被验证字段所属的对象，
	 * 因为 {@link Errors} 实例可以自行解析字段值（它通常持有对目标对象的内部引用）。
	 * @param errors 用于注册错误的 {@code Errors} 实例
	 * @param field 要检查的字段名
	 * @param errorCode 错误代码，可作为消息键使用
	 * @param defaultMessage 回退的默认消息
	 */
	public static void rejectIfEmptyOrWhitespace(
			Errors errors, String field, String errorCode, String defaultMessage) {

		rejectIfEmptyOrWhitespace(errors, field, errorCode, null, defaultMessage);
	}

	/**
	 * 如果值为空或仅包含空白字符，则使用给定的错误代码和错误参数拒绝该字段。
	 * <p>此处"空"值的含义为 {@code null}、空字符串 "" 或完全由空白字符组成。
	 * <p>不需要传入被验证字段所属的对象，
	 * 因为 {@link Errors} 实例可以自行解析字段值（它通常持有对目标对象的内部引用）。
	 * @param errors 用于注册错误的 {@code Errors} 实例
	 * @param field 要检查的字段名
	 * @param errorCode 错误代码，可作为消息键使用
	 * @param errorArgs 错误参数，用于通过 MessageFormat 进行参数绑定
	 * （可以为 {@code null}）
	 */
	public static void rejectIfEmptyOrWhitespace(
			Errors errors, String field, String errorCode, @Nullable Object[] errorArgs) {

		rejectIfEmptyOrWhitespace(errors, field, errorCode, errorArgs, null);
	}

	/**
	 * 如果值为空或仅包含空白字符，则使用给定的错误代码、错误参数和默认消息拒绝该字段。
	 * <p>此处"空"值的含义为 {@code null}、空字符串 "" 或完全由空白字符组成。
	 * <p>不需要传入被验证字段所属的对象，
	 * 因为 {@link Errors} 实例可以自行解析字段值（它通常持有对目标对象的内部引用）。
	 * @param errors 用于注册错误的 {@code Errors} 实例
	 * @param field 要检查的字段名
	 * @param errorCode 错误代码，可作为消息键使用
	 * @param errorArgs 错误参数，用于通过 MessageFormat 进行参数绑定
	 * （可以为 {@code null}）
	 * @param defaultMessage 回退的默认消息
	 */
	public static void rejectIfEmptyOrWhitespace(
			Errors errors, String field, String errorCode, @Nullable Object[] errorArgs, @Nullable String defaultMessage) {

		Assert.notNull(errors, "Errors object must not be null");
		Object value = errors.getFieldValue(field);
		if (value == null ||!StringUtils.hasText(value.toString())) {
			errors.rejectValue(field, errorCode, errorArgs, defaultMessage);
		}
	}

}
