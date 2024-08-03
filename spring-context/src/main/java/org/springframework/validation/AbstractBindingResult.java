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

package org.springframework.validation;

import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.beans.PropertyEditor;
import java.io.Serializable;
import java.util.*;

/**
 * {@link BindingResult} 接口及其超接口 {@link Errors} 的抽象实现。封装了对 {@link ObjectError ObjectErrors}
 * 和 {@link FieldError FieldErrors} 的常见管理。
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @see Errors
 * @since 2.0
 */
@SuppressWarnings("serial")
public abstract class AbstractBindingResult extends AbstractErrors implements BindingResult, Serializable {
	/**
	 * 对象名称
	 */
	private final String objectName;

	/**
	 * 消息编码解析器
	 */
	private MessageCodesResolver messageCodesResolver = new DefaultMessageCodesResolver();

	/**
	 * 错误列表
	 */
	private final List<ObjectError> errors = new ArrayList<>();

	/**
	 * 字段 —— 类型映射
	 */
	private final Map<String, Class<?>> fieldTypes = new HashMap<>();

	/**
	 * 字段 —— 值映射
	 */
	private final Map<String, Object> fieldValues = new HashMap<>();

	/**
	 * 抑制字段列表
	 */
	private final Set<String> suppressedFields = new HashSet<>();


	/**
	 * 创建一个新的 AbstractBindingResult 实例。
	 *
	 * @param objectName 目标对象的名称
	 * @see DefaultMessageCodesResolver
	 */
	protected AbstractBindingResult(String objectName) {
		this.objectName = objectName;
	}


	/**
	 * 设置用于将错误解析为消息代码的策略。默认为 DefaultMessageCodesResolver。
	 *
	 * @param messageCodesResolver 用于解析消息代码的策略
	 * @see DefaultMessageCodesResolver
	 */
	public void setMessageCodesResolver(MessageCodesResolver messageCodesResolver) {
		Assert.notNull(messageCodesResolver, "MessageCodesResolver must not be null");
		this.messageCodesResolver = messageCodesResolver;
	}

	/**
	 * 返回用于将错误解析为消息代码的策略。
	 *
	 * @return 用于解析消息代码的策略
	 */
	public MessageCodesResolver getMessageCodesResolver() {
		return this.messageCodesResolver;
	}


	//---------------------------------------------------------------------
	// Errors接口的实现
	//---------------------------------------------------------------------

	@Override
	public String getObjectName() {
		return this.objectName;
	}

	@Override
	public void reject(String errorCode, @Nullable Object[] errorArgs, @Nullable String defaultMessage) {
		addError(new ObjectError(getObjectName(), resolveMessageCodes(errorCode), errorArgs, defaultMessage));
	}

	@Override
	public void rejectValue(@Nullable String field, String errorCode, @Nullable Object[] errorArgs,
							@Nullable String defaultMessage) {

		// 如果嵌套路径和字段都没有长度
		if (!StringUtils.hasLength(getNestedPath()) && !StringUtils.hasLength(field)) {
			// 处于嵌套对象层次结构的顶层，当前层次不是字段而是顶层对象
			// 在这里我们能做的最好是注册一个全局错误
			reject(errorCode, errorArgs, defaultMessage);
			return;
		}

		// 修正字段名称
		String fixedField = fixedField(field);

		// 获取修正字段的实际值
		Object newVal = getActualFieldValue(fixedField);

		// 创建一个新的 FieldError 对象
		FieldError fe = new FieldError(getObjectName(), fixedField, newVal, false,
				resolveMessageCodes(errorCode, field), errorArgs, defaultMessage);

		// 添加错误到错误列表中
		addError(fe);
	}

	@Override
	public void addAllErrors(Errors errors) {
		// 如果错误对象的名称与当前对象的名称不相同
		if (!errors.getObjectName().equals(getObjectName())) {
			// 抛出 IllegalArgumentException 异常，提示错误对象需要具有相同的对象名称
			throw new IllegalArgumentException("Errors object needs to have same object name");
		}

		// 将所有错误添加到当前对象的错误列表中
		this.errors.addAll(errors.getAllErrors());
	}

	@Override
	public boolean hasErrors() {
		return !this.errors.isEmpty();
	}

	@Override
	public int getErrorCount() {
		return this.errors.size();
	}

	@Override
	public List<ObjectError> getAllErrors() {
		return Collections.unmodifiableList(this.errors);
	}

	@Override
	public List<ObjectError> getGlobalErrors() {
		// 创建一个结果列表用于存储非字段错误
		List<ObjectError> result = new ArrayList<>();

		// 遍历当前对象的所有错误
		for (ObjectError objectError : this.errors) {
			// 如果错误不是字段错误
			if (!(objectError instanceof FieldError)) {
				// 将该错误添加到结果列表中
				result.add(objectError);
			}
		}

		// 返回一个不可修改的结果列表
		return Collections.unmodifiableList(result);
	}

	@Override
	@Nullable
	public ObjectError getGlobalError() {
		// 遍历当前对象的所有错误
		for (ObjectError objectError : this.errors) {
			// 如果错误不是字段错误
			if (!(objectError instanceof FieldError)) {
				// 返回该错误
				return objectError;
			}
		}

		// 如果没有找到非字段错误，返回 null
		return null;
	}

	@Override
	public List<FieldError> getFieldErrors() {
		// 创建一个结果列表用于存储字段错误
		List<FieldError> result = new ArrayList<>();

		// 遍历当前对象的所有错误
		for (ObjectError objectError : this.errors) {
			// 如果错误是字段错误
			if (objectError instanceof FieldError) {
				// 将字段错误添加到结果列表中
				result.add((FieldError) objectError);
			}
		}

		// 返回一个不可修改的结果列表
		return Collections.unmodifiableList(result);
	}

	@Override
	@Nullable
	public FieldError getFieldError() {
		// 遍历当前对象的所有错误
		for (ObjectError objectError : this.errors) {
			// 如果错误是字段错误
			if (objectError instanceof FieldError) {
				// 返回字段错误
				return (FieldError) objectError;
			}
		}

		// 如果没有找到字段错误，返回 null
		return null;
	}

	@Override
	public List<FieldError> getFieldErrors(String field) {
		// 创建一个结果列表用于存储匹配的字段错误
		List<FieldError> result = new ArrayList<>();

		// 修正字段名称
		String fixedField = fixedField(field);

		// 遍历当前对象的所有错误
		for (ObjectError objectError : this.errors) {
			// 如果错误是字段错误并且与修正后的字段名称匹配
			if (objectError instanceof FieldError && isMatchingFieldError(fixedField, (FieldError) objectError)) {
				// 将匹配的字段错误添加到结果列表中
				result.add((FieldError) objectError);
			}
		}

		// 返回一个不可修改的结果列表
		return Collections.unmodifiableList(result);
	}

	@Override
	@Nullable
	public FieldError getFieldError(String field) {
		// 修正字段名称
		String fixedField = fixedField(field);

		// 遍历当前对象的所有错误
		for (ObjectError objectError : this.errors) {
			// 如果错误是字段错误
			if (objectError instanceof FieldError) {
				// 将错误转换为 FieldError 类型
				FieldError fieldError = (FieldError) objectError;

				// 如果字段错误与修正后的字段名称匹配
				if (isMatchingFieldError(fixedField, fieldError)) {
					// 返回匹配的字段错误
					return fieldError;
				}
			}
		}

		// 如果没有找到匹配的字段错误，返回 null
		return null;
	}

	@Override
	@Nullable
	public Object getFieldValue(String field) {
		// 获取指定字段的字段错误
		FieldError fieldError = getFieldError(field);

		// 使用被拒绝的值（如果存在错误），否则使用当前字段值
		if (fieldError != null) {
			// 获取被拒绝的值
			Object value = fieldError.getRejectedValue();
			// 对于绑定失败（如类型不匹配）的情况，不应用格式化
			return (fieldError.isBindingFailure() || getTarget() == null ? value : formatFieldValue(field, value));
		} else if (getTarget() != null) {
			// 如果目标对象存在，获取实际字段值并应用格式化
			Object value = getActualFieldValue(fixedField(field));
			return formatFieldValue(field, value);
		} else {
			// 如果没有字段错误且目标对象不存在，返回字段的原始值
			return this.fieldValues.get(field);
		}
	}

	/**
	 * 此默认实现基于实际字段值（如果有）来确定字段类型。子类应重写此方法，以便从描述符中确定字段类型，即使对于 {@code null} 值也是如此。
	 *
	 * @see #getActualFieldValue
	 */
	@Override
	@Nullable
	public Class<?> getFieldType(@Nullable String field) {
		// 如果目标对象存在
		if (getTarget() != null) {
			// 获取修正后的字段值
			Object value = getActualFieldValue(fixedField(field));
			// 如果字段值不为 null，返回字段值的类类型
			if (value != null) {
				return value.getClass();
			}
		}

		// 返回字段类型的原始值
		return this.fieldTypes.get(field);
	}


	//---------------------------------------------------------------------
	// BindingResult 接口的实现
	//---------------------------------------------------------------------

	/**
	 * 返回一个模型 Map，暴露了一个 {@link #MODEL_KEY_PREFIX MODEL_KEY_PREFIX} + objectName 的 Errors 实例
	 * 和目标对象本身。
	 * <p>请注意，每次调用此方法时，Map 都会被构建。将内容添加到 Map 中然后重新调用此方法将不起作用。
	 * <p>由此方法返回的模型 Map 中的属性通常会包含在用于表单视图的 ModelAndView 中，该视图使用 Spring 的 bind 标签，
	 * 需要访问 Errors 实例。
	 *
	 * @see #getObjectName
	 * @see #MODEL_KEY_PREFIX
	 */
	@Override
	public Map<String, Object> getModel() {
		Map<String, Object> model = new LinkedHashMap<>(2);
		// 从名称到目标对象的映射。
		model.put(getObjectName(), getTarget());
		// 即使没有错误，也包含 Errors 实例。
		model.put(MODEL_KEY_PREFIX + getObjectName(), this);
		return model;
	}

	@Override
	@Nullable
	public Object getRawFieldValue(String field) {
		return (getTarget() != null ? getActualFieldValue(fixedField(field)) : null);
	}

	/**
	 * 此实现委托给 {@link #getPropertyEditorRegistry() PropertyEditorRegistry} 的编辑器查找功能（如果可用）。
	 */
	@Override
	@Nullable
	public PropertyEditor findEditor(@Nullable String field, @Nullable Class<?> valueType) {
		// 获取属性编辑器注册表
		PropertyEditorRegistry editorRegistry = getPropertyEditorRegistry();

		// 如果属性编辑器注册表不为 null
		if (editorRegistry != null) {
			// 使用提供的值类型，如果值类型为 null，则获取字段类型
			Class<?> valueTypeToUse = valueType;
			if (valueTypeToUse == null) {
				valueTypeToUse = getFieldType(field);
			}
			// 从注册表中查找与值类型和字段名称匹配的自定义编辑器
			return editorRegistry.findCustomEditor(valueTypeToUse, fixedField(field));
		} else {
			// 如果属性编辑器注册表为 null，返回 null
			return null;
		}
	}

	/**
	 * 此实现返回 {@code null}。
	 */
	@Override
	@Nullable
	public PropertyEditorRegistry getPropertyEditorRegistry() {
		return null;
	}

	@Override
	public String[] resolveMessageCodes(String errorCode) {
		return getMessageCodesResolver().resolveMessageCodes(errorCode, getObjectName());
	}

	@Override
	public String[] resolveMessageCodes(String errorCode, @Nullable String field) {
		return getMessageCodesResolver().resolveMessageCodes(
				errorCode, getObjectName(), fixedField(field), getFieldType(field));
	}

	@Override
	public void addError(ObjectError error) {
		this.errors.add(error);
	}

	@Override
	public void recordFieldValue(String field, Class<?> type, @Nullable Object value) {
		this.fieldTypes.put(field, type);
		this.fieldValues.put(field, value);
	}

	/**
	 * 将指定的禁止字段标记为已抑制。
	 * <p>数据绑定器会为每个被检测到的目标禁止字段的字段值调用此方法。
	 *
	 * @see DataBinder#setAllowedFields
	 */
	@Override
	public void recordSuppressedField(String field) {
		this.suppressedFields.add(field);
	}

	/**
	 * 返回绑定过程中被抑制的字段列表。
	 * <p>可以用来确定是否有任何字段值目标指向了禁止字段。
	 *
	 * @see DataBinder#setAllowedFields
	 */
	@Override
	public String[] getSuppressedFields() {
		return StringUtils.toStringArray(this.suppressedFields);
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof BindingResult)) {
			return false;
		}
		BindingResult otherResult = (BindingResult) other;
		return (getObjectName().equals(otherResult.getObjectName()) &&
				ObjectUtils.nullSafeEquals(getTarget(), otherResult.getTarget()) &&
				getAllErrors().equals(otherResult.getAllErrors()));
	}

	@Override
	public int hashCode() {
		return getObjectName().hashCode();
	}


	//---------------------------------------------------------------------
	// 子类需要实现/重写的模板方法
	//---------------------------------------------------------------------

	/**
	 * 返回被包装的目标对象。
	 */
	@Override
	@Nullable
	public abstract Object getTarget();

	/**
	 * 提取给定字段的实际值。
	 *
	 * @param field 要检查的字段
	 * @return 字段的当前值
	 */
	@Nullable
	protected abstract Object getActualFieldValue(String field);

	/**
	 * 格式化指定字段的给定值。
	 * <p>默认实现只是按原样返回字段值。
	 *
	 * @param field 要检查的字段
	 * @param value 字段的值（可能是绑定错误的拒绝值，也可能是实际的字段值）
	 * @return 格式化后的值
	 */
	@Nullable
	protected Object formatFieldValue(String field, @Nullable Object value) {
		return value;
	}

}
