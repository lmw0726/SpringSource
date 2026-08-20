/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.validation.beanvalidation;

import org.springframework.beans.NotReadablePropertyException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.validation.*;

import javax.validation.ConstraintViolation;
import javax.validation.ElementKind;
import javax.validation.Path;
import javax.validation.ValidationException;
import javax.validation.executable.ExecutableValidator;
import javax.validation.metadata.BeanDescriptor;
import javax.validation.metadata.ConstraintDescriptor;
import java.io.Serializable;
import java.util.*;

/**
 * 适配器，接受 JSR-303 的 {@code javax.validator.Validator}，
 * 将其作为 Spring 的 {@link org.springframework.validation.Validator} 暴露出来，
 * 同时也暴露原始的 JSR-303 Validator 接口本身。
 *
 * <p>可用作编程式包装器。同时也是 {@link CustomValidatorBean} 和
 * {@link LocalValidatorFactoryBean} 的基类，
 * 以及 {@link SmartValidator} 接口的主要实现。
 *
 * <p>从 Spring Framework 5.0 开始，此适配器完全兼容
 * Bean Validation 1.1 和 2.0。
 *
 * @author Juergen Hoeller
 * @see SmartValidator
 * @see CustomValidatorBean
 * @see LocalValidatorFactoryBean
 * @since 3.0
 */
public class SpringValidatorAdapter implements SmartValidator, javax.validation.Validator {

	private static final Set<String> internalAnnotationAttributes = new HashSet<>(4);

	static {
		internalAnnotationAttributes.add("message");
		internalAnnotationAttributes.add("groups");
		internalAnnotationAttributes.add("payload");
	}

	@Nullable
	private javax.validation.Validator targetValidator;


	/**
	 * 为给定的 JSR-303 Validator 创建新的 SpringValidatorAdapter。
	 *
	 * @param targetValidator 要包装的 JSR-303 Validator
	 */
	public SpringValidatorAdapter(javax.validation.Validator targetValidator) {
		Assert.notNull(targetValidator, "Target Validator must not be null");
		this.targetValidator = targetValidator;
	}

	SpringValidatorAdapter() {
	}

	void setTargetValidator(javax.validation.Validator targetValidator) {
		this.targetValidator = targetValidator;
	}


	//---------------------------------------------------------------------
	// Spring Validator 接口的实现
	//---------------------------------------------------------------------

	@Override
	public boolean supports(Class<?> clazz) {
		return (this.targetValidator != null);
	}

	@Override
	public void validate(Object target, Errors errors) {
		if (this.targetValidator != null) {
			processConstraintViolations(this.targetValidator.validate(target), errors);
		}
	}

	@Override
	public void validate(Object target, Errors errors, Object... validationHints) {
		if (this.targetValidator != null) {
			processConstraintViolations(
					this.targetValidator.validate(target, asValidationGroups(validationHints)), errors);
		}
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	@Override
	public void validateValue(
			Class<?> targetType, String fieldName, @Nullable Object value, Errors errors, Object... validationHints) {

		if (this.targetValidator != null) {
			processConstraintViolations(this.targetValidator.validateValue(
					(Class) targetType, fieldName, value, asValidationGroups(validationHints)), errors);
		}
	}

	/**
	 * 将指定的验证提示转换为 JSR-303 验证分组。
	 *
	 * @since 5.1
	 */
	private Class<?>[] asValidationGroups(Object... validationHints) {
		Set<Class<?>> groups = new LinkedHashSet<>(4);
		for (Object hint : validationHints) {
			if (hint instanceof Class) {
				groups.add((Class<?>) hint);
			}
		}
		return ClassUtils.toClassArray(groups);
	}

	/**
	 * 处理给定的 JSR-303 ConstraintViolations，将对应的错误添加到
	 * 提供的 Spring {@link Errors} 对象中。
	 *
	 * @param violations JSR-303 ConstraintViolation 结果
	 * @param errors     要注册到的 Spring 错误对象
	 */
	@SuppressWarnings("serial")
	protected void processConstraintViolations(Set<ConstraintViolation<Object>> violations, Errors errors) {
		for (ConstraintViolation<Object> violation : violations) {
			String field = determineField(violation);
			FieldError fieldError = errors.getFieldError(field);
			if (fieldError == null || !fieldError.isBindingFailure()) {
				try {
					ConstraintDescriptor<?> cd = violation.getConstraintDescriptor();
					String errorCode = determineErrorCode(cd);
					Object[] errorArgs = getArgumentsForConstraint(errors.getObjectName(), field, cd);
					if (errors instanceof BindingResult) {
					// 可以使用 ConstraintViolation 中的无效值进行自定义 FieldError 注册，
					// 这对于 Hibernate Validator 兼容性是必要的（字段中的非索引 set 路径）
					BindingResult bindingResult = (BindingResult) errors;
						String nestedField = bindingResult.getNestedPath() + field;
						if (nestedField.isEmpty()) {
							String[] errorCodes = bindingResult.resolveMessageCodes(errorCode);
							ObjectError error = new ViolationObjectError(
									errors.getObjectName(), errorCodes, errorArgs, violation, this);
							bindingResult.addError(error);
						} else {
							Object rejectedValue = getRejectedValue(field, violation, bindingResult);
							String[] errorCodes = bindingResult.resolveMessageCodes(errorCode, field);
							FieldError error = new ViolationFieldError(errors.getObjectName(), nestedField,
									rejectedValue, errorCodes, errorArgs, violation, this);
							bindingResult.addError(error);
						}
					} else {
					// 没有 BindingResult - 只能进行标准的 rejectValue 调用，
					// 并自动提取当前字段值
					errors.rejectValue(field, errorCode, errorArgs, violation.getMessage());
					}
				} catch (NotReadablePropertyException ex) {
					throw new IllegalStateException("JSR-303 validated property '" + field +
							"' does not have a corresponding accessor for Spring data binding - " +
							"check your DataBinder's configuration (bean property versus direct field access)", ex);
				}
			}
		}
	}

	/**
	 * 确定给定约束违规的字段。
	 * <p>默认实现返回字符串化的属性路径。
	 *
	 * @param violation 当前的 JSR-303 ConstraintViolation
	 * @return Spring 报告的字段（用于 {@link Errors}）
	 * @see javax.validation.ConstraintViolation#getPropertyPath()
	 * @see org.springframework.validation.FieldError#getField()
	 * @since 4.2
	 */
	protected String determineField(ConstraintViolation<Object> violation) {
		Path path = violation.getPropertyPath();
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (Path.Node node : path) {
			if (node.isInIterable()) {
				sb.append('[');
				Object index = node.getIndex();
				if (index == null) {
					index = node.getKey();
				}
				if (index != null) {
					sb.append(index);
				}
				sb.append(']');
			}
			String name = node.getName();
			if (name != null && node.getKind() == ElementKind.PROPERTY && !name.startsWith("<")) {
				if (!first) {
					sb.append('.');
				}
				first = false;
				sb.append(name);
			}
		}
		return sb.toString();
	}

	/**
	 * 确定给定约束描述符的 Spring 报告的错误代码。
	 * <p>默认实现返回描述符注解类型的简单类名。注意，已配置的
	 * {@link org.springframework.validation.MessageCodesResolver} 将自动生成
	 * 包含对象名和字段名的错误代码变体。
	 *
	 * @param descriptor 当前违规的 JSR-303 ConstraintDescriptor
	 * @return 对应的错误代码（用于 {@link Errors}）
	 * @see javax.validation.metadata.ConstraintDescriptor#getAnnotation()
	 * @see org.springframework.validation.MessageCodesResolver
	 * @since 4.2
	 */
	protected String determineErrorCode(ConstraintDescriptor<?> descriptor) {
		return descriptor.getAnnotation().annotationType().getSimpleName();
	}

	/**
	 * 返回给定字段验证错误的 FieldError 参数。
	 * 每次违反约束时都会调用。
	 * <p>默认实现返回一个表示字段名称的第一个参数
	 * （参见 {@link #getResolvableField}）。然后，按属性名称的字母顺序
	 * 添加所有实际的约束注解属性（即排除 "message"、"groups" 和 "payload"）。
	 * <p>可以被覆盖以例如添加约束描述符中的其他属性。
	 *
	 * @param objectName 目标对象的名称
	 * @param field      导致绑定错误的字段
	 * @param descriptor JSR-303 约束描述符
	 * @return 表示 FieldError 参数的 Object 数组
	 * @see org.springframework.validation.FieldError#getArguments
	 * @see org.springframework.context.support.DefaultMessageSourceResolvable
	 * @see org.springframework.validation.DefaultBindingErrorProcessor#getArgumentsForBindError
	 */
	protected Object[] getArgumentsForConstraint(String objectName, String field, ConstraintDescriptor<?> descriptor) {
		List<Object> arguments = new ArrayList<>();
		arguments.add(getResolvableField(objectName, field));
		// 使用 TreeMap 按字母顺序排列属性名称
		Map<String, Object> attributesToExpose = new TreeMap<>();
		descriptor.getAttributes().forEach((attributeName, attributeValue) -> {
			if (!internalAnnotationAttributes.contains(attributeName)) {
				if (attributeValue instanceof String) {
					attributeValue = new ResolvableAttribute(attributeValue.toString());
				}
				attributesToExpose.put(attributeName, attributeValue);
			}
		});
		arguments.addAll(attributesToExpose.values());
		return arguments.toArray();
	}

	/**
	 * 为指定字段构建可解析的包装器，允许在 {@code MessageSource} 中解析字段的名称。
	 * <p>默认实现返回一个表示字段的第一个参数：
	 * 类型为 {@code DefaultMessageSourceResolvable}，以 "objectName.field" 和 "field"
	 * 作为代码，以纯字段名作为默认消息。
	 *
	 * @param objectName 目标对象的名称
	 * @param field      导致绑定错误的字段
	 * @return 指定字段对应的 {@code MessageSourceResolvable}
	 * @see #getArgumentsForConstraint
	 * @since 4.3
	 */
	protected MessageSourceResolvable getResolvableField(String objectName, String field) {
		String[] codes = new String[]{objectName + Errors.NESTED_PATH_SEPARATOR + field, field};
		return new DefaultMessageSourceResolvable(codes, field);
	}

	/**
	 * 提取给定约束违规背后的拒绝值，
	 * 以便通过 Spring 错误表示进行暴露。
	 *
	 * @param field         导致绑定错误的字段
	 * @param violation     对应的 JSR-303 ConstraintViolation
	 * @param bindingResult 包含当前字段值的后备对象的 Spring BindingResult
	 * @return 要作为字段错误一部分暴露的无效值
	 * @see javax.validation.ConstraintViolation#getInvalidValue()
	 * @see org.springframework.validation.FieldError#getRejectedValue()
	 * @since 4.2
	 */
	@Nullable
	protected Object getRejectedValue(String field, ConstraintViolation<Object> violation, BindingResult bindingResult) {
		Object invalidValue = violation.getInvalidValue();
		if (!field.isEmpty() && !field.contains("[]") &&
				(invalidValue == violation.getLeafBean() || field.contains("[") || field.contains("."))) {
			// 可能是具有属性路径的 bean 约束：检索实际的属性值。
			// 但是，显式地避免处理 "address[]" 样式的路径，因为我们无法处理。
			invalidValue = bindingResult.getRawFieldValue(field);
		}
		return invalidValue;
	}

	/**
	 * 指示此违规的插值消息是否还有剩余的占位符，因此需要对其应用 {@link java.text.MessageFormat}。
	 * 在将 Bean Validation 定义的消息（来自 {@code ValidationMessages.properties}）渲染为 Spring 的 MessageSource 中的默认消息时调用。
	 * <p>默认实现将 Spring 风格的 "{0}" 占位符作为需要 {@link java.text.MessageFormat} 的指示。
	 * 任何其他占位符或转义语法出现通常都是不匹配的，可能来自正则表达式模式值或类似的情况。
	 * 请注意，标准的 Bean Validation 根本不支持 "{0}" 样式的占位符；这通常是在 Spring MessageSource 资源束中使用的功能。
	 *
	 * @param violation Bean Validation 约束违规，包括其消息中的命名属性引用的 BV 定义的插值
	 * @return 如果要应用 {@code java.text.MessageFormat}，则为 {@code true}；如果应该直接使用违规消息，则为 {@code false}
	 * @see #getArgumentsForConstraint
	 * @since 5.1.8
	 */
	protected boolean requiresMessageFormat(ConstraintViolation<?> violation) {
		return containsSpringStylePlaceholder(violation.getMessage());
	}

	private static boolean containsSpringStylePlaceholder(@Nullable String message) {
		return (message != null && message.contains("{0}"));
	}


	//---------------------------------------------------------------------
	// JSR-303 Validator 接口的实现
	//---------------------------------------------------------------------

	@Override
	public <T> Set<ConstraintViolation<T>> validate(T object, Class<?>... groups) {
		Assert.state(this.targetValidator != null, "No target Validator set");
		return this.targetValidator.validate(object, groups);
	}

	@Override
	public <T> Set<ConstraintViolation<T>> validateProperty(T object, String propertyName, Class<?>... groups) {
		Assert.state(this.targetValidator != null, "No target Validator set");
		return this.targetValidator.validateProperty(object, propertyName, groups);
	}

	@Override
	public <T> Set<ConstraintViolation<T>> validateValue(
			Class<T> beanType, String propertyName, Object value, Class<?>... groups) {

		Assert.state(this.targetValidator != null, "No target Validator set");
		return this.targetValidator.validateValue(beanType, propertyName, value, groups);
	}

	@Override
	public BeanDescriptor getConstraintsForClass(Class<?> clazz) {
		Assert.state(this.targetValidator != null, "No target Validator set");
		return this.targetValidator.getConstraintsForClass(clazz);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T unwrap(@Nullable Class<T> type) {
		Assert.state(this.targetValidator != null, "No target Validator set");
		try {
			return (type != null ? this.targetValidator.unwrap(type) : (T) this.targetValidator);
		} catch (ValidationException ex) {
		// 如果只是请求普通的 JSR-303 Validator，则忽略
		if (javax.validation.Validator.class == type) {
				return (T) this.targetValidator;
			}
			throw ex;
		}
	}

	@Override
	public ExecutableValidator forExecutables() {
		Assert.state(this.targetValidator != null, "No target Validator set");
		return this.targetValidator.forExecutables();
	}


	/**
	 * 一个字符串属性的包装器，可以通过{@code MessageSource}解析，否则将原始属性作为默认值回退。
	 */
	@SuppressWarnings("serial")
	private static class ResolvableAttribute implements MessageSourceResolvable, Serializable {
		/**
		 * 可解析的字符串
		 */
		private final String resolvableString;

		public ResolvableAttribute(String resolvableString) {
			this.resolvableString = resolvableString;
		}

		@Override
		public String[] getCodes() {
			return new String[]{this.resolvableString};
		}

		@Override
		@Nullable
		public Object[] getArguments() {
			return null;
		}

		@Override
		public String getDefaultMessage() {
			return this.resolvableString;
		}

		@Override
		public String toString() {
			return this.resolvableString;
		}
	}


	/**
	 * {@code ObjectError} 的子类，具有 Spring 风格的默认消息渲染。
	 */
	@SuppressWarnings("serial")
	private static class ViolationObjectError extends ObjectError implements Serializable {
		/**
		 * Spring校验器适配器
		 */
		@Nullable
		private transient SpringValidatorAdapter adapter;

		/**
		 * 违反约束
		 */
		@Nullable
		private transient ConstraintViolation<?> violation;

		public ViolationObjectError(String objectName, String[] codes, Object[] arguments,
									ConstraintViolation<?> violation, SpringValidatorAdapter adapter) {

			super(objectName, codes, arguments, violation.getMessage());
			this.adapter = adapter;
			this.violation = violation;
			wrap(violation);
		}

		@Override
		public boolean shouldRenderDefaultMessage() {
			return (this.adapter != null && this.violation != null ?
					this.adapter.requiresMessageFormat(this.violation) :
					containsSpringStylePlaceholder(getDefaultMessage()));
		}
	}


	/**
	 * {@code FieldError} 的子类，具有 Spring 风格的默认消息渲染。
	 */
	@SuppressWarnings("serial")
	private static class ViolationFieldError extends FieldError implements Serializable {
		/**
		 * Spring校验器适配器
		 */
		@Nullable
		private transient SpringValidatorAdapter adapter;

		/**
		 * 违反约束
		 */
		@Nullable
		private transient ConstraintViolation<?> violation;

		public ViolationFieldError(String objectName, String field, @Nullable Object rejectedValue, String[] codes,
								   Object[] arguments, ConstraintViolation<?> violation, SpringValidatorAdapter adapter) {

			super(objectName, field, rejectedValue, false, codes, arguments, violation.getMessage());
			this.adapter = adapter;
			this.violation = violation;
			wrap(violation);
		}

		@Override
		public boolean shouldRenderDefaultMessage() {
			return (this.adapter != null && this.violation != null ?
					this.adapter.requiresMessageFormat(this.violation) :
					containsSpringStylePlaceholder(getDefaultMessage()));
		}
	}

}
