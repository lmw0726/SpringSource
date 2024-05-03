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

package org.springframework.web.servlet.support;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.context.NoSuchMessageException;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.ObjectError;
import org.springframework.web.util.HtmlUtils;

import java.beans.PropertyEditor;
import java.util.Arrays;
import java.util.List;

/**
 * 简单的适配器，用于公开字段或对象的绑定状态。
 * 由 JSP 绑定标签和 FreeMarker 宏都设置为变量。
 *
 * <p>显然，对象状态表示（即对象级别的错误而不是字段级别的错误）没有表达式和值，而只有错误代码和消息。
 * 为了简单起见，并且能够使用相同的标签和宏，相同的状态类用于两种情况。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Darren Davison
 * @see RequestContext#getBindStatus
 * @see org.springframework.web.servlet.tags.BindTag
 * @see org.springframework.web.servlet.view.AbstractTemplateView#setExposeSpringMacroHelpers
 */
public class BindStatus {

	/**
	 * 请求上下文
	 */
	private final RequestContext requestContext;

	/**
	 * 要解析值和错误的 bean 和属性路径
	 */
	private final String path;

	/**
	 * 是否对HTML进行转义
	 */
	private final boolean htmlEscape;

	/**
	 * 绑定表达式
	 */
	@Nullable
	private final String expression;

	/**
	 * 此绑定状态当前关联的 Errors 实例
	 */
	@Nullable
	private final Errors errors;

	/**
	 * 返回字段或对象的错误代码
	 */
	private final String[] errorCodes;

	/**
	 * 错误消息
	 */
	@Nullable
	private String[] errorMessages;

	/**
	 * 错误对象列表
	 */
	@Nullable
	private List<? extends ObjectError> objectErrors;

	/**
	 * 字段的当前值
	 */
	@Nullable
	private Object value;

	/**
	 * 字段的类型
	 */
	@Nullable
	private Class<?> valueType;

	/**
	 * 字段的实际值
	 */
	@Nullable
	private Object actualValue;

	/**
	 * 属性编辑器
	 */
	@Nullable
	private PropertyEditor editor;

	/**
	 * 绑定结果
	 */
	@Nullable
	private BindingResult bindingResult;


	/**
	 * 创建一个新的 BindStatus 实例，表示字段或对象状态。
	 *
	 * @param requestContext 当前的 RequestContext
	 * @param path           要解析值和错误的 bean 和属性路径（例如："customer.address.street"）
	 * @param htmlEscape     是否对错误消息和字符串值进行 HTML 转义
	 * @throws IllegalStateException 如果找不到相应的 Errors 对象
	 */
	public BindStatus(RequestContext requestContext, String path, boolean htmlEscape) throws IllegalStateException {
		// 设置请求上下文
		this.requestContext = requestContext;
		// 设置路径
		this.path = path;
		// 设置是否进行 HTML 转义
		this.htmlEscape = htmlEscape;

		// 确定对象和属性的名称
		String beanName;
		int dotPos = path.indexOf('.');
		if (dotPos == -1) {
			// 属性未设置，只有对象本身
			beanName = path;
			this.expression = null;
		} else {
			// bean名称为 . 之前的字符串
			beanName = path.substring(0, dotPos);
			// 表达式为 . 之后的字符串
			this.expression = path.substring(dotPos + 1);
		}

		// 获取给定对象名称的错误
		this.errors = requestContext.getErrors(beanName, false);

		if (this.errors != null) {
			// 如果存在错误对象
			// 常规情况：可以作为请求属性使用 BindingResult。
			// 可以确定给定表达式的错误代码和消息。
			// 可以使用表单控制器注册的自定义 PropertyEditor。
			if (this.expression != null) {
				// 如果存在表达式
				if ("*".equals(this.expression)) {
					// 表达式为 *，将全局错误对象设置为错误对象
					this.objectErrors = this.errors.getAllErrors();
				} else if (this.expression.endsWith("*")) {
					// 表达式以 * 结尾，将字段错误对象设置为错误对象
					this.objectErrors = this.errors.getFieldErrors(this.expression);
				} else {
					// 将字段错误对象设置为错误对象
					this.objectErrors = this.errors.getFieldErrors(this.expression);
					// 根据表达式获取字段值
					this.value = this.errors.getFieldValue(this.expression);
					// 根据表达式获取字段类型
					this.valueType = this.errors.getFieldType(this.expression);
					if (this.errors instanceof BindingResult) {
						// 如果错误对象是 BindingResult实例
						// 将当前错误设置为绑定结果
						this.bindingResult = (BindingResult) this.errors;
						// 通过绑定结果获取该表达式对应的原始字段值，作为实际值
						this.actualValue = this.bindingResult.getRawFieldValue(this.expression);
						// 通过绑定结果获取该表达式对应的属性编辑器，作为属性编辑器
						this.editor = this.bindingResult.findEditor(this.expression, null);
					} else {
						// 属性值即是实际值
						this.actualValue = this.value;
					}
				}
			} else {
				// 如果没有表达式，将全局错误对象设置为错误对象
				this.objectErrors = this.errors.getGlobalErrors();
			}
			// 根据错误对象初始化错误码
			this.errorCodes = initErrorCodes(this.objectErrors);
		} else {
			// 作为请求属性不可用 BindingResult：
			// 可能直接转发到表单视图。
			// 让我们尽力而为：如果合适，提取一个纯目标对象。
			// 根据 bean名称 获取模型对象
			Object target = requestContext.getModelObject(beanName);
			if (target == null) {
				throw new IllegalStateException("Neither BindingResult nor plain target object for bean name '" +
						beanName + "' available as request attribute");
			}
			if (this.expression != null && !"*".equals(this.expression) && !this.expression.endsWith("*")) {
				// 如果存在表达式，并且表达式不是 "*"，且表达式不是以 "*" 结尾
				// 获取模型对象的Bean包装器
				BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(target);
				// 根据表达式获取属性值
				this.value = bw.getPropertyValue(this.expression);
				// 根据表达式获取值类型
				this.valueType = bw.getPropertyType(this.expression);
				// 属性值即是实际值
				this.actualValue = this.value;
			}
			// 将错误码设置为空的字符串数组
			this.errorCodes = new String[0];
			// 将错误消息设置为空的字符串数组
			this.errorMessages = new String[0];
		}

		if (htmlEscape && this.value instanceof String) {
			// 如果需要 HTML 转义，且值是字符串，则执行 HTML 转义
			this.value = HtmlUtils.htmlEscape((String) this.value);
		}
	}

	/**
	 * 从 ObjectError 列表中提取错误代码。
	 */
	private static String[] initErrorCodes(List<? extends ObjectError> objectErrors) {
		// 创建一个包含错误代码的数组
		String[] errorCodes = new String[objectErrors.size()];

		// 遍历错误列表
		for (int i = 0; i < objectErrors.size(); i++) {
			// 获取错误对象
			ObjectError error = objectErrors.get(i);
			// 将错误代码存入数组
			errorCodes[i] = error.getCode();
		}

		// 返回错误代码数组
		return errorCodes;
	}


	/**
	 * 返回要解析值和错误的 bean 和属性路径（例如："customer.address.street"）。
	 */
	public String getPath() {
		return this.path;
	}

	/**
	 * 返回可以在 HTML 表单中作为相应字段的输入名称使用的绑定表达式，如果不是字段特定，则返回 {@code null}。
	 * <p>返回适用于重新提交的绑定路径，例如 "address.street"。
	 * 请注意，绑定标签所需的完整绑定路径是 "customer.address.street"，如果绑定到 "customer" bean。
	 */
	@Nullable
	public String getExpression() {
		return this.expression;
	}

	/**
	 * 返回字段的当前值，即属性值或已拒绝的更新，如果不是字段特定，则返回 {@code null}。
	 * <p>如果原始值已经是字符串，则该值将是 HTML 转义字符串。
	 */
	@Nullable
	public Object getValue() {
		return this.value;
	}

	/**
	 * 获取字段的 '{@code Class}' 类型。优先考虑此方法，而不是 '{@code getValue().getClass()}'，
	 * 因为 '{@code getValue()}' 可能返回 '{@code null}'。
	 */
	@Nullable
	public Class<?> getValueType() {
		return this.valueType;
	}

	/**
	 * 返回字段的实际值，即原始属性值，如果不可用，则返回 {@code null}。
	 */
	@Nullable
	public Object getActualValue() {
		return this.actualValue;
	}

	/**
	 * 返回字段的适当显示值，即如果不为 null，则为字符串化值，如果为 null，则为空字符串。
	 * <p>如果原始值不为 null，则该值将是 HTML 转义字符串：原始值的 {@code toString} 结果将被 HTML 转义。
	 */
	public String getDisplayValue() {
		// 如果值是字符串类型，则直接返回
		if (this.value instanceof String) {
			return (String) this.value;
		}

		// 如果值不为null
		if (this.value != null) {
			// 如果需要进行HTML转义，则对值进行转义后返回其字符串表示形式；
			// 否则直接返回其字符串表示形式
			return (this.htmlEscape ? HtmlUtils.htmlEscape(this.value.toString()) : this.value.toString());
		}

		// 如果值为null，则返回空字符串
		return "";
	}

	/**
	 * 返回此状态是否表示字段或对象错误。
	 */
	public boolean isError() {
		return (this.errorCodes.length > 0);
	}

	/**
	 * 返回字段或对象的错误代码（如果有）。
	 * 如果没有，则返回一个空数组而不是 null。
	 */
	public String[] getErrorCodes() {
		return this.errorCodes;
	}

	/**
	 * 返回字段或对象的第一个错误代码（如果有）。
	 */
	public String getErrorCode() {
		return (this.errorCodes.length > 0 ? this.errorCodes[0] : "");
	}

	/**
	 * 返回字段或对象的解析错误消息（如果有）。
	 * 如果没有，则返回一个空数组而不是 null。
	 */
	public String[] getErrorMessages() {
		return initErrorMessages();
	}

	/**
	 * 返回字段或对象的第一个错误消息（如果有）。
	 */
	public String getErrorMessage() {
		String[] errorMessages = initErrorMessages();
		return (errorMessages.length > 0 ? errorMessages[0] : "");
	}

	/**
	 * 返回一个错误消息字符串，将所有消息用给定的分隔符分隔。
	 *
	 * @param delimiter 分隔符字符串，例如 ", " 或 "<br>"
	 * @return 错误消息字符串
	 */
	public String getErrorMessagesAsString(String delimiter) {
		return StringUtils.arrayToDelimitedString(initErrorMessages(), delimiter);
	}

	/**
	 * 从 ObjectError 列表中提取错误消息。
	 */
	private String[] initErrorMessages() throws NoSuchMessageException {
		// 如果错误消息数组为null
		if (this.errorMessages == null) {
			// 如果对象错误列表不为null
			if (this.objectErrors != null) {
				// 创建一个数组以存储错误消息
				this.errorMessages = new String[this.objectErrors.size()];
				// 遍历对象错误列表
				for (int i = 0; i < this.objectErrors.size(); i++) {
					ObjectError error = this.objectErrors.get(i);
					// 获取每个错误的消息并存入数组中
					this.errorMessages[i] = this.requestContext.getMessage(error, this.htmlEscape);
				}
			} else {
				// 如果对象错误列表为null，则将错误消息数组初始化为空数组
				this.errorMessages = new String[0];
			}
		}
		// 返回错误消息数组
		return this.errorMessages;
	}

	/**
	 * 返回此绑定状态当前关联的 Errors 实例（通常是 BindingResult）。
	 *
	 * @return 当前 Errors 实例，如果没有则为 {@code null}
	 * @see org.springframework.validation.BindingResult
	 */
	@Nullable
	public Errors getErrors() {
		return this.errors;
	}

	/**
	 * 返回此绑定状态当前绑定到的属性的 PropertyEditor。
	 *
	 * @return 当前的 PropertyEditor，如果没有则为 {@code null}
	 */
	@Nullable
	public PropertyEditor getEditor() {
		return this.editor;
	}

	/**
	 * 查找与此绑定状态当前绑定到的属性关联的给定值类的 PropertyEditor。
	 *
	 * @param valueClass 需要编辑器的值类
	 * @return 关联的 PropertyEditor，如果没有则为 {@code null}
	 */
	@Nullable
	public PropertyEditor findEditor(Class<?> valueClass) {
		return (this.bindingResult != null ? this.bindingResult.findEditor(this.expression, valueClass) : null);
	}


	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("BindStatus: ");
		sb.append("expression=[").append(this.expression).append("]; ");
		sb.append("value=[").append(this.value).append(']');
		if (!ObjectUtils.isEmpty(this.errorCodes)) {
			sb.append("; errorCodes=").append(Arrays.asList(this.errorCodes));
		}
		return sb.toString();
	}

}
