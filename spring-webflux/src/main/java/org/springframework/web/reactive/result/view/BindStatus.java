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

package org.springframework.web.reactive.result.view;

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
 * 由 FreeMarker 宏和其他标签库设置为变量。
 *
 * <p>显然，对象状态表示（即对象级别的错误而不是字段级别的错误）没有表达式和值，而只有错误代码和消息。
 * 为了简单起见并且能够使用相同的标签和宏，同样的状态类用于这两种情况。
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @see RequestContext#getBindStatus
 * @since 5.0
 */
public class BindStatus {
	/**
	 * 请求上下文
	 */
	private final RequestContext requestContext;

	/**
	 * 路径
	 */
	private final String path;

	/**
	 * 是否进行 HTML 转义
	 */
	private final boolean htmlEscape;

	/**
	 * 表达式，用于表达式求值
	 */
	private final String expression;

	/**
	 * 错误信息，用于存储验证错误信息
	 */
	private final Errors errors;

	/**
	 * 错误代码数组，用于存储错误代码
	 */
	private final String[] errorCodes;

	/**
	 * 错误消息数组，用于存储错误消息
	 */
	private String[] errorMessages;

	/**
	 * 对象错误列表，用于存储对象级别的错误信息
	 */
	private List<? extends ObjectError> objectErrors;

	/**
	 * 值，用于存储属性值
	 */
	private Object value;

	/**
	 * 值类型，用于存储属性值的类型
	 */
	private Class<?> valueType;

	/**
	 * 实际值，用于存储属性的实际值
	 */
	private Object actualValue;

	/**
	 * 属性编辑器，用于处理属性值的编辑器
	 */
	private PropertyEditor editor;

	/**
	 * 绑定结果，用于存储绑定结果
	 */
	private BindingResult bindingResult;


	public BindStatus(RequestContext requestContext, String path, boolean htmlEscape) throws IllegalStateException {
		// 请求上下文
		this.requestContext = requestContext;
// 路径
		this.path = path;
// 是否进行HTML转义
		this.htmlEscape = htmlEscape;

		String beanName;
		int dotPos = path.indexOf('.');
		if (dotPos == -1) {
			// 如果路径中没有'.'，将整个路径作为bean名称
			beanName = path;
			// 表达式为空
			this.expression = null;
		} else {
			// 否则，取'.'之前的部分作为bean名称，'.'之后的部分作为表达式
			beanName = path.substring(0, dotPos);
			this.expression = path.substring(dotPos + 1);
		}

// 获取给定beanName的错误信息
		this.errors = requestContext.getErrors(beanName, false);

		if (this.errors != null) {
			// 通常情况: BindingResult可用作请求属性。
			// 可以确定给定表达式的错误代码和消息。
			// 可以使用由表单控制器注册的自定义PropertyEditor。
			if (this.expression != null) {
				if ("*".equals(this.expression)) {
					// 如果表达式为'*'，获取所有错误信息
					this.objectErrors = this.errors.getAllErrors();
				} else if (this.expression.endsWith("*")) {
					// 如果表达式以'*'结尾，获取字段级别的错误信息
					this.objectErrors = this.errors.getFieldErrors(this.expression);
				} else {
					// 否则，获取指定字段的错误信息，并获取字段的值、类型等信息
					this.objectErrors = this.errors.getFieldErrors(this.expression);
					this.value = this.errors.getFieldValue(this.expression);
					this.valueType = this.errors.getFieldType(this.expression);
					if (this.errors instanceof BindingResult) {
						// 如果是绑定结果，获取原始字段值、编辑器等信息
						this.bindingResult = (BindingResult) this.errors;
						this.actualValue = this.bindingResult.getRawFieldValue(this.expression);
						this.editor = this.bindingResult.findEditor(this.expression, null);
					} else {
						this.actualValue = this.value;
					}
				}
			} else {
				// 如果没有指定表达式，获取全局错误信息
				this.objectErrors = this.errors.getGlobalErrors();
			}
			// 初始化错误代码数组
			this.errorCodes = initErrorCodes(this.objectErrors);
		} else {
			// 如果没有错误，获取目标对象，并根据表达式获取字段值、类型等信息
			Object target = requestContext.getModelObject(beanName);
			if (target == null) {
				throw new IllegalStateException(
						"Neither BindingResult nor plain target object for bean name '" +
								beanName + "' available as request attribute");
			}
			if (this.expression != null && !"*".equals(this.expression) && !this.expression.endsWith("*")) {
				// 如果表达式不为空且不为通配符形式，获取目标对象的指定字段值、类型和实际值
				BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(target);
				this.value = bw.getPropertyValue(this.expression);
				this.valueType = bw.getPropertyType(this.expression);
				this.actualValue = this.value;
			}

			// 初始化错误代码和错误消息数组
			this.errorCodes = new String[0];
			this.errorMessages = new String[0];
		}

		if (htmlEscape && this.value instanceof String) {
			// 如果需要进行HTML转义且字段值为字符串，进行HTML转义处理
			this.value = HtmlUtils.htmlEscape((String) this.value);
		}
	}

	/**
	 * 从 ObjectError 列表中提取错误代码。
	 */
	private static String[] initErrorCodes(List<? extends ObjectError> objectErrors) {
		String[] errorCodes = new String[objectErrors.size()];
		for (int i = 0; i < objectErrors.size(); i++) {
			ObjectError error = objectErrors.get(i);
			errorCodes[i] = error.getCode();
		}
		return errorCodes;
	}


	/**
	 * 返回值和错误将被解析的 bean 和属性路径（例如："customer.address.street"）。
	 */
	public String getPath() {
		return this.path;
	}

	/**
	 * 返回可用作 HTML 表单中相应字段的输入名称的绑定表达式，如果不是字段特定，则返回 {@code null}。
	 * <p>返回适合重新提交的绑定路径，例如："address.street"。
	 * 请注意，绑定标签所需的完整绑定路径为 "customer.address.street"，如果绑定到 "customer" bean。
	 */
	@Nullable
	public String getExpression() {
		return this.expression;
	}

	/**
	 * 返回字段的当前值，即属性值或被拒绝的更新，如果不是字段特定，则返回 {@code null}。
	 * <p>如果原始值已经是字符串，则该值将是经过 HTML 转义的字符串。
	 */
	@Nullable
	public Object getValue() {
		return this.value;
	}

	/**
	 * 获取字段的 '{@code Class}' 类型。最好使用此方法，而不是 '{@code getValue().getClass()}'，
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
	 * 返回字段的合适显示值，即如果值不为 null，则返回值的字符串形式，如果值为 null，则返回空字符串。
	 * <p>如果原始值不为 null，则该值将是经过 HTML 转义的字符串：原始值的 {@code toString} 结果将会被 HTML 转义。
	 */
	public String getDisplayValue() {
		if (this.value instanceof String) {
			return (String) this.value;
		}
		if (this.value != null) {
			return (this.htmlEscape ?
					HtmlUtils.htmlEscape(this.value.toString()) : this.value.toString());
		}
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
	 * 如果没有错误，则返回空数组而不是 null。
	 */
	public String[] getErrorCodes() {
		return this.errorCodes;
	}

	/**
	 * 返回字段或对象的第一个错误代码（如果有）。
	 */
	public String getErrorCode() {
		return (!ObjectUtils.isEmpty(this.errorCodes) ? this.errorCodes[0] : "");
	}

	/**
	 * 返回字段或对象的解析错误消息（如果有）。
	 * 如果没有错误，则返回空数组而不是 null。
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
	 * 返回一个错误消息字符串，用给定的分隔符连接所有消息。
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
		if (this.errorMessages == null) {
			if (this.objectErrors != null) {
				this.errorMessages = new String[this.objectErrors.size()];
				for (int i = 0; i < this.objectErrors.size(); i++) {
					ObjectError error = this.objectErrors.get(i);
					this.errorMessages[i] = this.requestContext.getMessage(error, this.htmlEscape);
				}
			} else {
				this.errorMessages = new String[0];
			}
		}
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
	 * @return 当前 PropertyEditor，如果没有则为 {@code null}
	 */
	@Nullable
	public PropertyEditor getEditor() {
		return this.editor;
	}

	/**
	 * 查找与此绑定状态当前绑定的属性关联的给定值类的 PropertyEditor。
	 *
	 * @param valueClass 需要编辑器的值类
	 * @return 相关联的 PropertyEditor，如果没有则为 {@code null}
	 */
	@Nullable
	public PropertyEditor findEditor(Class<?> valueClass) {
		return (this.bindingResult != null ?
				this.bindingResult.findEditor(this.expression, valueClass) : null);
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
