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

package org.springframework.web.bind;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.util.HtmlUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 将自动HTML转义添加到包装实例的错误包装器，以便在HTML视图中方便使用。可以通过RequestContext的{@code getErrors}方法轻松检索。
 *
 * <p>注意，BindTag不使用此类来避免不必要地创建ObjectError实例。它只是转义被复制到相应BindStatus实例中的消息和值。
 *
 * @author Juergen Hoeller
 * @see org.springframework.web.servlet.support.RequestContext#getErrors
 * @see org.springframework.web.servlet.tags.BindTag
 * @since 01.03.2003
 */
public class EscapedErrors implements Errors {
	/**
	 * 错误源
	 */
	private final Errors source;


	/**
	 * 为给定的源实例创建一个新的EscapedErrors实例。
	 */
	public EscapedErrors(Errors source) {
		Assert.notNull(source, "Errors source must not be null");
		this.source = source;
	}

	public Errors getSource() {
		return this.source;
	}


	@Override
	public String getObjectName() {
		return this.source.getObjectName();
	}

	@Override
	public void setNestedPath(String nestedPath) {
		this.source.setNestedPath(nestedPath);
	}

	@Override
	public String getNestedPath() {
		return this.source.getNestedPath();
	}

	@Override
	public void pushNestedPath(String subPath) {
		this.source.pushNestedPath(subPath);
	}

	@Override
	public void popNestedPath() throws IllegalStateException {
		this.source.popNestedPath();
	}


	@Override
	public void reject(String errorCode) {
		this.source.reject(errorCode);
	}

	@Override
	public void reject(String errorCode, String defaultMessage) {
		this.source.reject(errorCode, defaultMessage);
	}

	@Override
	public void reject(String errorCode, @Nullable Object[] errorArgs, @Nullable String defaultMessage) {
		this.source.reject(errorCode, errorArgs, defaultMessage);
	}

	@Override
	public void rejectValue(@Nullable String field, String errorCode) {
		this.source.rejectValue(field, errorCode);
	}

	@Override
	public void rejectValue(@Nullable String field, String errorCode, String defaultMessage) {
		this.source.rejectValue(field, errorCode, defaultMessage);
	}

	@Override
	public void rejectValue(@Nullable String field, String errorCode, @Nullable Object[] errorArgs,
							@Nullable String defaultMessage) {

		this.source.rejectValue(field, errorCode, errorArgs, defaultMessage);
	}

	@Override
	public void addAllErrors(Errors errors) {
		this.source.addAllErrors(errors);
	}


	@Override
	public boolean hasErrors() {
		return this.source.hasErrors();
	}

	@Override
	public int getErrorCount() {
		return this.source.getErrorCount();
	}

	@Override
	public List<ObjectError> getAllErrors() {
		return escapeObjectErrors(this.source.getAllErrors());
	}

	@Override
	public boolean hasGlobalErrors() {
		return this.source.hasGlobalErrors();
	}

	@Override
	public int getGlobalErrorCount() {
		return this.source.getGlobalErrorCount();
	}

	@Override
	public List<ObjectError> getGlobalErrors() {
		return escapeObjectErrors(this.source.getGlobalErrors());
	}

	@Override
	@Nullable
	public ObjectError getGlobalError() {
		return escapeObjectError(this.source.getGlobalError());
	}

	@Override
	public boolean hasFieldErrors() {
		return this.source.hasFieldErrors();
	}

	@Override
	public int getFieldErrorCount() {
		return this.source.getFieldErrorCount();
	}

	@Override
	public List<FieldError> getFieldErrors() {
		return this.source.getFieldErrors();
	}

	@Override
	@Nullable
	public FieldError getFieldError() {
		return this.source.getFieldError();
	}

	@Override
	public boolean hasFieldErrors(String field) {
		return this.source.hasFieldErrors(field);
	}

	@Override
	public int getFieldErrorCount(String field) {
		return this.source.getFieldErrorCount(field);
	}

	@Override
	public List<FieldError> getFieldErrors(String field) {
		return escapeObjectErrors(this.source.getFieldErrors(field));
	}

	@Override
	@Nullable
	public FieldError getFieldError(String field) {
		return escapeObjectError(this.source.getFieldError(field));
	}

	@Override
	@Nullable
	public Object getFieldValue(String field) {
		// 获取字段的值
		Object value = this.source.getFieldValue(field);

		// 如果值是字符串类型
		return (value instanceof String ?
				// 对字符串值进行 HTML 转义处理，然后返回
				HtmlUtils.htmlEscape((String) value) :
				// 否则直接返回值
				value);
	}

	@Override
	@Nullable
	public Class<?> getFieldType(String field) {
		return this.source.getFieldType(field);
	}

	@SuppressWarnings("unchecked")
	@Nullable
	private <T extends ObjectError> T escapeObjectError(@Nullable T source) {
		// 如果源对象为空，则返回 null
		if (source == null) {
			return null;
		}

		// 获取默认消息
		String defaultMessage = source.getDefaultMessage();

		if (defaultMessage != null) {
			// 如果默认消息不为空，则对其进行 HTML 转义处理
			defaultMessage = HtmlUtils.htmlEscape(defaultMessage);
		}

		// 如果源对象是 FieldError 类型
		if (source instanceof FieldError) {
			FieldError fieldError = (FieldError) source;

			// 获取被拒绝的值
			Object value = fieldError.getRejectedValue();

			if (value instanceof String) {
				// 如果值是字符串类型，则对其进行 HTML 转义处理
				value = HtmlUtils.htmlEscape((String) value);
			}

			// 返回一个新的 FieldError 对象，其中的值已经被转义处理
			return (T) new FieldError(
					fieldError.getObjectName(), fieldError.getField(), value, fieldError.isBindingFailure(),
					fieldError.getCodes(), fieldError.getArguments(), defaultMessage);
		} else {
			// 如果源对象是 ObjectError 类型，则返回一个新的 ObjectError 对象
			return (T) new ObjectError(
					source.getObjectName(), source.getCodes(), source.getArguments(), defaultMessage);
		}
	}

	private <T extends ObjectError> List<T> escapeObjectErrors(List<T> source) {
		// 创建一个新的列表，用于存储转义后的对象
		List<T> escaped = new ArrayList<>(source.size());

		// 遍历源列表中的每个对象
		for (T objectError : source) {
			// 对每个对象调用 escapeObjectError 方法进行转义，并将结果添加到新列表中
			escaped.add(escapeObjectError(objectError));
		}

		// 返回转义后的列表
		return escaped;
	}

}
