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
 * Adapter that takes a JSR-303 {@code javax.validator.Validator} and
 * exposes it as a Spring {@link org.springframework.validation.Validator}
 * while also exposing the original JSR-303 Validator interface itself.
 *
 * <p>Can be used as a programmatic wrapper. Also serves as base class for
 * {@link CustomValidatorBean} and {@link LocalValidatorFactoryBean},
 * and as the primary implementation of the {@link SmartValidator} interface.
 *
 * <p>As of Spring Framework 5.0, this adapter is fully compatible with
 * Bean Validation 1.1 as well as 2.0.
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
	 * Create a new SpringValidatorAdapter for the given JSR-303 Validator.
	 *
	 * @param targetValidator the JSR-303 Validator to wrap
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
	// Implementation of Spring Validator interface
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
	 * Turn the specified validation hints into JSR-303 validation groups.
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
	 * Process the given JSR-303 ConstraintViolations, adding corresponding errors to
	 * the provided Spring {@link Errors} object.
	 *
	 * @param violations the JSR-303 ConstraintViolation results
	 * @param errors     the Spring errors object to register to
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
						// Can do custom FieldError registration with invalid value from ConstraintViolation,
						// as necessary for Hibernate Validator compatibility (non-indexed set path in field)
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
						// Got no BindingResult - can only do standard rejectValue call
						// with automatic extraction of the current field value
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
	 * Determine a field for the given constraint violation.
	 * <p>The default implementation returns the stringified property path.
	 *
	 * @param violation the current JSR-303 ConstraintViolation
	 * @return the Spring-reported field (for use with {@link Errors})
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
	 * Determine a Spring-reported error code for the given constraint descriptor.
	 * <p>The default implementation returns the simple class name of the descriptor's
	 * annotation type. Note that the configured
	 * {@link org.springframework.validation.MessageCodesResolver} will automatically
	 * generate error code variations which include the object name and the field name.
	 *
	 * @param descriptor the JSR-303 ConstraintDescriptor for the current violation
	 * @return a corresponding error code (for use with {@link Errors})
	 * @see javax.validation.metadata.ConstraintDescriptor#getAnnotation()
	 * @see org.springframework.validation.MessageCodesResolver
	 * @since 4.2
	 */
	protected String determineErrorCode(ConstraintDescriptor<?> descriptor) {
		return descriptor.getAnnotation().annotationType().getSimpleName();
	}

	/**
	 * Return FieldError arguments for a validation error on the given field.
	 * Invoked for each violated constraint.
	 * <p>The default implementation returns a first argument indicating the field name
	 * (see {@link #getResolvableField}). Afterwards, it adds all actual constraint
	 * annotation attributes (i.e. excluding "message", "groups" and "payload") in
	 * alphabetical order of their attribute names.
	 * <p>Can be overridden to e.g. add further attributes from the constraint descriptor.
	 *
	 * @param objectName the name of the target object
	 * @param field      the field that caused the binding error
	 * @param descriptor the JSR-303 constraint descriptor
	 * @return the Object array that represents the FieldError arguments
	 * @see org.springframework.validation.FieldError#getArguments
	 * @see org.springframework.context.support.DefaultMessageSourceResolvable
	 * @see org.springframework.validation.DefaultBindingErrorProcessor#getArgumentsForBindError
	 */
	protected Object[] getArgumentsForConstraint(String objectName, String field, ConstraintDescriptor<?> descriptor) {
		List<Object> arguments = new ArrayList<>();
		arguments.add(getResolvableField(objectName, field));
		// Using a TreeMap for alphabetical ordering of attribute names
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
	 * Build a resolvable wrapper for the specified field, allowing to resolve the field's
	 * name in a {@code MessageSource}.
	 * <p>The default implementation returns a first argument indicating the field:
	 * of type {@code DefaultMessageSourceResolvable}, with "objectName.field" and "field"
	 * as codes, and with the plain field name as default message.
	 *
	 * @param objectName the name of the target object
	 * @param field      the field that caused the binding error
	 * @return a corresponding {@code MessageSourceResolvable} for the specified field
	 * @see #getArgumentsForConstraint
	 * @since 4.3
	 */
	protected MessageSourceResolvable getResolvableField(String objectName, String field) {
		String[] codes = new String[]{objectName + Errors.NESTED_PATH_SEPARATOR + field, field};
		return new DefaultMessageSourceResolvable(codes, field);
	}

	/**
	 * Extract the rejected value behind the given constraint violation,
	 * for exposure through the Spring errors representation.
	 *
	 * @param field         the field that caused the binding error
	 * @param violation     the corresponding JSR-303 ConstraintViolation
	 * @param bindingResult a Spring BindingResult for the backing object
	 *                      which contains the current field's value
	 * @return the invalid value to expose as part of the field error
	 * @see javax.validation.ConstraintViolation#getInvalidValue()
	 * @see org.springframework.validation.FieldError#getRejectedValue()
	 * @since 4.2
	 */
	@Nullable
	protected Object getRejectedValue(String field, ConstraintViolation<Object> violation, BindingResult bindingResult) {
		Object invalidValue = violation.getInvalidValue();
		if (!field.isEmpty() && !field.contains("[]") &&
				(invalidValue == violation.getLeafBean() || field.contains("[") || field.contains("."))) {
			// Possibly a bean constraint with property path: retrieve the actual property value.
			// However, explicitly avoid this for "address[]" style paths that we can't handle.
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
	// Implementation of JSR-303 Validator interface
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
			// Ignore if just being asked for plain JSR-303 Validator
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
