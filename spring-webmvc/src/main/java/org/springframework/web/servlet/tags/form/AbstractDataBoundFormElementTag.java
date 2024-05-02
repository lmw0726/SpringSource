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

package org.springframework.web.servlet.tags.form;

import org.springframework.beans.PropertyAccessor;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.support.BindStatus;
import org.springframework.web.servlet.support.RequestDataValueProcessor;
import org.springframework.web.servlet.tags.EditorAwareTag;
import org.springframework.web.servlet.tags.NestedPathTag;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import java.beans.PropertyEditor;

/**
 * 所有数据绑定感知 JSP 表单标签的基础标签。
 *
 * <p>提供常见的 {@link #setPath 路径} 和 {@link #setId id} 属性。
 * 提供子类用于访问其绑定值的 {@link BindStatus} 的实用方法，
 * 以及与 {@link TagWriter} 进行交互的 {@link #writeOptionalAttribute 写入可选属性}。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 */
@SuppressWarnings("serial")
public abstract class AbstractDataBoundFormElementTag extends AbstractFormTag implements EditorAwareTag {

	/**
	 * 在此标签范围内公开的路径变量的名称："nestedPath"。
	 * 与 {@link org.springframework.web.servlet.tags.NestedPathTag#NESTED_PATH_VARIABLE_NAME} 的值相同。
	 */
	protected static final String NESTED_PATH_VARIABLE_NAME = NestedPathTag.NESTED_PATH_VARIABLE_NAME;


	/**
	 * 来自 {@link FormTag#setModelAttribute 表单对象} 的属性路径。
	 */
	@Nullable
	private String path;

	/**
	 * '{@code id}' 属性的值。
	 */
	@Nullable
	private String id;

	/**
	 * 此标签的 {@link BindStatus}。
	 */
	@Nullable
	private BindStatus bindStatus;


	/**
	 * 设置来自 {@link FormTag#setModelAttribute 表单对象} 的属性路径。
	 * 可能是运行时表达式。
	 */
	public void setPath(String path) {
		this.path = path;
	}

	/**
	 * 获取用于 {@link FormTag#setModelAttribute 表单对象} 的 {@link #evaluate 解析后的} 属性路径。
	 */
	protected final String getPath() throws JspException {
		String resolvedPath = (String) evaluate("path", this.path);
		return (resolvedPath != null ? resolvedPath : "");
	}

	/**
	 * 设置 '{@code id}' 属性的值。
	 * <p>可能是运行时表达式；默认为 {@link #getName()} 的值。
	 * 注意，默认值对于某些标签可能无效。
	 */
	@Override
	public void setId(@Nullable String id) {
		this.id = id;
	}

	/**
	 * 获取 '{@code id}' 属性的值。
	 */
	@Override
	@Nullable
	public String getId() {
		return this.id;
	}


	/**
	 * 将默认属性集写入提供的 {@link TagWriter}。
	 * 进一步的抽象子类应该覆盖此方法以添加任何额外的默认属性，但是<strong>必须</strong>记得调用 {@code super} 方法。
	 * <p>当/如果具体的子类希望呈现默认属性时，应该调用此方法。
	 *
	 * @param tagWriter 要写入任何属性的 {@link TagWriter}
	 */
	protected void writeDefaultAttributes(TagWriter tagWriter) throws JspException {
		writeOptionalAttribute(tagWriter, "id", resolveId());
		writeOptionalAttribute(tagWriter, "name", getName());
	}

	/**
	 * 确定此标签的 '{@code id}' 属性值，如果未指定则自动生成一个。
	 *
	 * @see #getId()
	 * @see #autogenerateId()
	 */
	@Nullable
	protected String resolveId() throws JspException {
		// 评估 ID 属性
		Object id = evaluate("id", getId());
		// 如果 ID 不为空
		if (id != null) {
			// 将 ID 转换为字符串
			String idString = id.toString();
			// 如果 ID 字符串不为空或不只包含空白字符
			return (StringUtils.hasText(idString) ? idString : null);
		}
		// 否则，自动生成 ID
		return autogenerateId();
	}

	/**
	 * 为此标签自动生成 '{@code id}' 属性值。
	 * <p>默认实现简单地委托给 {@link #getName()}，删除无效字符（例如 "[" 或 "]"）。
	 */
	@Nullable
	protected String autogenerateId() throws JspException {
		String name = getName();
		return (name != null ? StringUtils.deleteAny(name, "[]") : null);
	}

	/**
	 * 获取 HTML '{@code name}' 属性的值。
	 * <p>默认实现简单地委托给 {@link #getPropertyPath()}，将属性路径用作名称。
	 * 在大多数情况下，这是可取的，因为它与服务器端对数据绑定的期望相连。但是，一些子类可能希望更改 '{@code name}' 属性的值而不更改绑定路径。
	 *
	 * @return HTML '{@code name}' 属性的值
	 */
	@Nullable
	protected String getName() throws JspException {
		return getPropertyPath();
	}

	/**
	 * 获取此标签的 {@link BindStatus}。
	 */
	protected BindStatus getBindStatus() throws JspException {
		// 如果绑定状态为空
		if (this.bindStatus == null) {
			// 获取嵌套路径
			String nestedPath = getNestedPath();
			// 获取要使用的路径
			String pathToUse = (nestedPath != null ? nestedPath + getPath() : getPath());
			// 如果路径以嵌套属性分隔符结尾，则移除尾部的分隔符
			if (pathToUse.endsWith(PropertyAccessor.NESTED_PROPERTY_SEPARATOR)) {
				pathToUse = pathToUse.substring(0, pathToUse.length() - 1);
			}
			// 创建新的绑定状态对象
			this.bindStatus = new BindStatus(getRequestContext(), pathToUse, false);
		}
		// 返回绑定状态对象
		return this.bindStatus;
	}

	/**
	 * 获取由 {@link NestedPathTag} 可能已公开的嵌套路径的值。
	 */
	@Nullable
	protected String getNestedPath() {
		return (String) this.pageContext.getAttribute(NESTED_PATH_VARIABLE_NAME, PageContext.REQUEST_SCOPE);
	}

	/**
	 * 为此标签构建属性路径，包括嵌套路径但<i>不</i>以表单属性的名称为前缀。
	 *
	 * @see #getNestedPath()
	 * @see #getPath()
	 */
	protected String getPropertyPath() throws JspException {
		String expression = getBindStatus().getExpression();
		return (expression != null ? expression : "");
	}

	/**
	 * 获取绑定的值。
	 *
	 * @see #getBindStatus()
	 */
	@Nullable
	protected final Object getBoundValue() throws JspException {
		return getBindStatus().getValue();
	}

	/**
	 * 获取用于绑定到此标签的值的 {@link PropertyEditor}（如果有）。
	 */
	@Nullable
	protected PropertyEditor getPropertyEditor() throws JspException {
		return getBindStatus().getEditor();
	}

	/**
	 * 为 {@link EditorAwareTag} 公开 {@link PropertyEditor}。
	 * <p>对于内部渲染目的，请使用 {@link #getPropertyEditor()}。
	 */
	@Override
	@Nullable
	public final PropertyEditor getEditor() throws JspException {
		return getPropertyEditor();
	}

	/**
	 * 获取给定值的显示字符串，由 {@link BindStatus} 可能已注册为该值的 Class 的 {@link PropertyEditor} 转换。
	 */
	protected String convertToDisplayString(@Nullable Object value) throws JspException {
		PropertyEditor editor = (value != null ? getBindStatus().findEditor(value.getClass()) : null);
		return getDisplayString(value, editor);
	}

	/**
	 * 通过 {@link RequestDataValueProcessor} 实例处理给定的表单字段，如果配置了该实例，则返回相同的值。
	 */
	protected final String processFieldValue(@Nullable String name, String value, String type) {
		// 获取请求数据值处理器
		RequestDataValueProcessor processor = getRequestContext().getRequestDataValueProcessor();
		// 获取 Servlet 请求
		ServletRequest request = this.pageContext.getRequest();
		// 如果存在请求数据值处理器且请求是 HttpServletRequest 的实例
		if (processor != null && request instanceof HttpServletRequest) {
			// 处理表单字段值
			value = processor.processFormFieldValue((HttpServletRequest) request, name, value, type);
		}
		// 返回处理后的值
		return value;
	}

	/**
	 * 处理 {@link BindStatus} 实例。
	 */
	@Override
	public void doFinally() {
		super.doFinally();
		this.bindStatus = null;
	}

}
