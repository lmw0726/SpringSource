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

import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.*;

/**
 * {@link Errors} 接口的抽象实现。提供对评估错误的通用访问；
 * 但是，不定义 {@link ObjectError ObjectErrors} 和 {@link FieldError FieldErrors} 的具体管理。
 *
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @since 2.5.3
 */
@SuppressWarnings("serial")
public abstract class AbstractErrors implements Errors, Serializable {
	/**
	 * 嵌套路径
	 */
	private String nestedPath = "";

	/**
	 * 嵌套路径栈
	 */
	private final Deque<String> nestedPathStack = new ArrayDeque<>();


	@Override
	public void setNestedPath(@Nullable String nestedPath) {
		// 设置嵌套路径
		doSetNestedPath(nestedPath);
		// 清空嵌套路径栈
		this.nestedPathStack.clear();
	}

	@Override
	public String getNestedPath() {
		return this.nestedPath;
	}

	@Override
	public void pushNestedPath(String subPath) {
		// 将嵌套路径设置到嵌套路径栈中
		this.nestedPathStack.push(getNestedPath());
		// 设置嵌套路径
		doSetNestedPath(getNestedPath() + subPath);
	}

	@Override
	public void popNestedPath() throws IllegalStateException {
		try {
			// 弹出最顶上的嵌套路径
			String formerNestedPath = this.nestedPathStack.pop();
			// 设置嵌套路径栈
			doSetNestedPath(formerNestedPath);
		} catch (NoSuchElementException ex) {
			throw new IllegalStateException("Cannot pop nested path: no nested path on stack");
		}
	}

	/**
	 * 实际设置嵌套路径。
	 * 由 setNestedPath 和 pushNestedPath 委托。
	 */
	protected void doSetNestedPath(@Nullable String nestedPath) {
		// 如果嵌套路径为空，则设置为空字符串
		if (nestedPath == null) {
			nestedPath = "";
		}
		// 获取规范字段名称
		nestedPath = canonicalFieldName(nestedPath);
		// 如果嵌套路径有值，并且嵌套路径不以 . 结尾
		if (nestedPath.length() > 0 && !nestedPath.endsWith(Errors.NESTED_PATH_SEPARATOR)) {
			// 嵌套路径添加 . 符号
			nestedPath += Errors.NESTED_PATH_SEPARATOR;
		}
		this.nestedPath = nestedPath;
	}

	/**
	 * 根据此实例的嵌套路径，将给定字段转换为其完整路径。
	 */
	protected String fixedField(@Nullable String field) {
		// 如果字段有值
		if (StringUtils.hasLength(field)) {
			// 返回嵌套路径加上规范化的字段名称
			return getNestedPath() + canonicalFieldName(field);
		} else {
			// 否则，获取嵌套路径
			String path = getNestedPath();
			// 如果嵌套路径以 . 结尾，则返回 . 之前的字符串。否则返回嵌套路径。
			return (path.endsWith(Errors.NESTED_PATH_SEPARATOR) ?
					path.substring(0, path.length() - NESTED_PATH_SEPARATOR.length()) : path);
		}
	}

	/**
	 * 确定给定字段的规范字段名。
	 * <p>默认实现简单地按原样返回字段名。
	 *
	 * @param field 原始字段名
	 * @return 规范字段名
	 */
	protected String canonicalFieldName(String field) {
		return field;
	}


	@Override
	public void reject(String errorCode) {
		reject(errorCode, null, null);
	}

	@Override
	public void reject(String errorCode, String defaultMessage) {
		reject(errorCode, null, defaultMessage);
	}

	@Override
	public void rejectValue(@Nullable String field, String errorCode) {
		rejectValue(field, errorCode, null, null);
	}

	@Override
	public void rejectValue(@Nullable String field, String errorCode, String defaultMessage) {
		rejectValue(field, errorCode, null, defaultMessage);
	}


	@Override
	public boolean hasErrors() {
		return !getAllErrors().isEmpty();
	}

	@Override
	public int getErrorCount() {
		return getAllErrors().size();
	}

	@Override
	public List<ObjectError> getAllErrors() {
		List<ObjectError> result = new ArrayList<>();
		// 添加全局错误
		result.addAll(getGlobalErrors());
		// 添加字段错误
		result.addAll(getFieldErrors());
		return Collections.unmodifiableList(result);
	}

	@Override
	public boolean hasGlobalErrors() {
		return (getGlobalErrorCount() > 0);
	}

	@Override
	public int getGlobalErrorCount() {
		return getGlobalErrors().size();
	}

	@Override
	@Nullable
	public ObjectError getGlobalError() {
		// 获取全局错误
		List<ObjectError> globalErrors = getGlobalErrors();
		// 如果全局错误不为空则返回第一个错误，否则返回null。
		return (!globalErrors.isEmpty() ? globalErrors.get(0) : null);
	}

	@Override
	public boolean hasFieldErrors() {
		return (getFieldErrorCount() > 0);
	}

	@Override
	public int getFieldErrorCount() {
		return getFieldErrors().size();
	}

	@Override
	@Nullable
	public FieldError getFieldError() {
		// 获取字段错误
		List<FieldError> fieldErrors = getFieldErrors();
		// 如果字段错误不为空则返回第一个错误，否则返回null。
		return (!fieldErrors.isEmpty() ? fieldErrors.get(0) : null);
	}

	@Override
	public boolean hasFieldErrors(String field) {
		return (getFieldErrorCount(field) > 0);
	}

	@Override
	public int getFieldErrorCount(String field) {
		return getFieldErrors(field).size();
	}

	@Override
	public List<FieldError> getFieldErrors(String field) {
		// 获取字段错误列表
		List<FieldError> fieldErrors = getFieldErrors();

		// 创建一个结果列表用于存储匹配的字段错误
		List<FieldError> result = new ArrayList<>();

		// 修正字段名称
		String fixedField = fixedField(field);

		// 遍历字段错误列表
		for (FieldError error : fieldErrors) {
			// 如果字段错误与修正后的字段名称匹配
			if (isMatchingFieldError(fixedField, error)) {
				// 将匹配的字段错误添加到结果列表中
				result.add(error);
			}
		}

		// 返回一个不可修改的结果列表
		return Collections.unmodifiableList(result);
	}

	@Override
	@Nullable
	public FieldError getFieldError(String field) {
		// 根据字段获取字段错误
		List<FieldError> fieldErrors = getFieldErrors(field);
		// 如果字段错误不为空则返回第一个错误，否则返回null。
		return (!fieldErrors.isEmpty() ? fieldErrors.get(0) : null);
	}

	@Override
	@Nullable
	public Class<?> getFieldType(String field) {
		// 获取字段值
		Object value = getFieldValue(field);
		// 如果字段值不为空，则返回该值的类型；否则，返回null。
		return (value != null ? value.getClass() : null);
	}

	/**
	 * 检查给定的 FieldError 是否与给定字段匹配。
	 *
	 * @param field      我们正在查找 FieldErrors 的字段
	 * @param fieldError 候选的 FieldError
	 * @return FieldError 是否与给定字段匹配
	 */
	protected boolean isMatchingFieldError(String field, FieldError fieldError) {
		if (field.equals(fieldError.getField())) {
			// 如果字段名与错误中的字段名相同，则返回true。
			return true;
		}
		// 优化：使用 charAt 和 regionMatches 而不是 endsWith 和 startsWith (SPR-11304)
		// 计算字段的结束索引
		int endIndex = field.length() - 1;

		// 返回布尔值，表示是否满足以下条件：
		// 1. 结束索引大于等于 0
		// 2. 字段的最后一个字符是 '*'
		// 3. 字段的长度为 1 或者字段的前缀与字段错误的字段名称匹配
		return (endIndex >= 0 && field.charAt(endIndex) == '*' &&
				(endIndex == 0 || field.regionMatches(0, fieldError.getField(), 0, endIndex)));
	}


	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(getClass().getName());
		sb.append(": ").append(getErrorCount()).append(" errors");
		for (ObjectError error : getAllErrors()) {
			sb.append('\n').append(error);
		}
		return sb.toString();
	}

}
