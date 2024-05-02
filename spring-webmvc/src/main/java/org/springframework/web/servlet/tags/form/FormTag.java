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
import org.springframework.core.Conventions;
import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.support.RequestDataValueProcessor;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriUtils;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Map;

/**
 * {@code <form>} 标签渲染一个 HTML 'form' 标签，并向内部标签公开绑定路径。
 *
 * <p>当填充视图数据时，用户应将表单对象放入 {@link org.springframework.web.servlet.ModelAndView ModelAndView} 中。
 * 可以使用 {@link #setModelAttribute "modelAttribute"} 属性配置此表单对象的名称。
 *
 * <p>
 * <table>
 * <caption>属性摘要</caption>
 * <thead>
 * <tr>
 * <th class="colFirst">属性</th>
 * <th class="colOne">是否必需？</th>
 * <th class="colOne">运行时表达式？</th>
 * <th class="colLast">描述</th>
 * </tr>
 * </thead>
 * <tbody>
 * <tr class="altColor">
 * <td><p>acceptCharset</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>指定服务器接受此表单的输入数据的字符编码列表。值是一个用空格和/或逗号分隔的字符集值列表。
 * 客户端必须将此列表解释为互斥列表，即，服务器能够接受每个实体接收的任何单个字符编码。</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>action</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 必需属性</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>cssClass</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 可选属性</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>cssStyle</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 可选属性</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>dir</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 标准属性</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>enctype</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 可选属性</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>htmlEscape</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>启用/禁用呈现值的 HTML 转义。</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>id</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 标准属性</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>lang</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 标准属性</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>method</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 可选属性</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>methodParam</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>用于除 GET 和 POST 之外的 HTTP 方法的参数名称。默认为 '_method'。</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>modelAttribute</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>表单对象公开的模型属性名称。默认为 'command'。</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>name</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 标准属性 - 为了向后兼容而添加</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>onclick</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>ondblclick</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>onkeydown</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>onkeypress</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>onkeyup</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>onmousedown</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>onmousemove</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>onmouseout</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>onmouseover</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>onmouseup</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>onreset</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>onsubmit</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>servletRelativeAction</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>要附加到当前 Servlet 路径的操作引用</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>target</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 可选属性</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>title</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 标准属性</p></td>
 * </tr>
 * </tbody>
 * </table>
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Scott Andrews
 * @author Rossen Stoyanchev
 * @since 2.0
 */
@SuppressWarnings("serial")
public class FormTag extends AbstractHtmlElementTag {

	/**
	 * 默认的 HTTP 方法，用于将表单值发送到服务器： "post"。
	 */
	private static final String DEFAULT_METHOD = "post";

	/**
	 * 默认属性名称： "command"。
	 */
	public static final String DEFAULT_COMMAND_NAME = "command";

	/**
	 * '{@code modelAttribute}' 设置的名称。
	 */
	private static final String MODEL_ATTRIBUTE = "modelAttribute";

	/**
	 * {@link javax.servlet.jsp.PageContext} 属性的名称，用于公开表单对象名称。
	 */
	public static final String MODEL_ATTRIBUTE_VARIABLE_NAME =
			Conventions.getQualifiedAttributeName(AbstractFormTag.class, MODEL_ATTRIBUTE);

	/**
	 * 默认方法参数，即 {@code _method}。
	 */
	private static final String DEFAULT_METHOD_PARAM = "_method";

	private static final String FORM_TAG = "form";

	private static final String INPUT_TAG = "input";

	private static final String ACTION_ATTRIBUTE = "action";

	private static final String METHOD_ATTRIBUTE = "method";

	private static final String TARGET_ATTRIBUTE = "target";

	private static final String ENCTYPE_ATTRIBUTE = "enctype";

	private static final String ACCEPT_CHARSET_ATTRIBUTE = "accept-charset";

	private static final String ONSUBMIT_ATTRIBUTE = "onsubmit";

	private static final String ONRESET_ATTRIBUTE = "onreset";

	private static final String AUTOCOMPLETE_ATTRIBUTE = "autocomplete";

	private static final String NAME_ATTRIBUTE = "name";

	private static final String VALUE_ATTRIBUTE = "value";

	private static final String TYPE_ATTRIBUTE = "type";


	/**
	 * 标签写入器
	 */
	@Nullable
	private TagWriter tagWriter;

	/**
	 * 模型属性
	 */
	private String modelAttribute = DEFAULT_COMMAND_NAME;

	/**
	 * 名称
	 */
	@Nullable
	private String name;

	/**
	 * 请求的URL路径
	 */
	@Nullable
	private String action;

	/**
	 * Servelt相对请求路径
	 */
	@Nullable
	private String servletRelativeAction;

	/**
	 * 请求方法
	 */
	private String method = DEFAULT_METHOD;

	/**
	 * 目标
	 */
	@Nullable
	private String target;

	/**
	 * 编码方式
	 */
	@Nullable
	private String enctype;

	/**
	 * 接受的字符集
	 */
	@Nullable
	private String acceptCharset;

	/**
	 *提交表单时触发的事件
	 */
	@Nullable
	private String onsubmit;

	/**
	 * 重置表单时触发的事件
	 */
	@Nullable
	private String onreset;

	/**
	 *表单是否应启用自动完成功能
	 */
	@Nullable
	private String autocomplete;

	/**
	 * 方法参数
	 */
	private String methodParam = DEFAULT_METHOD_PARAM;

	/**
	 * 缓存前一个嵌套路径，以便可以重置它。
	 */
	@Nullable
	private String previousNestedPath;


	/**
	 * 设置模型中表单属性的名称。
	 * <p>可以是运行时表达式。
	 */
	public void setModelAttribute(String modelAttribute) {
		this.modelAttribute = modelAttribute;
	}

	/**
	 * 获取模型中表单属性的名称。
	 */
	protected String getModelAttribute() {
		return this.modelAttribute;
	}

	/**
	 * 设置 '{@code name}' 属性的值。
	 * <p>可以是运行时表达式。
	 * <p>对于 XHTML 1.0，name 不是有效属性。但是，有时需要向后兼容。
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * 获取 '{@code name}' 属性的值。
	 */
	@Override
	@Nullable
	protected String getName() throws JspException {
		return this.name;
	}

	/**
	 * 设置 '{@code action}' 属性的值。
	 * <p>可以是运行时表达式。
	 */
	public void setAction(@Nullable String action) {
		this.action = (action != null ? action : "");
	}

	/**
	 * 获取 '{@code action}' 属性的值。
	 */
	@Nullable
	protected String getAction() {
		return this.action;
	}

	/**
	 * 通过要附加到当前 Servlet 路径的值设置 '{@code action}' 属性的值。
	 * <p>可以是运行时表达式。
	 *
	 * @since 3.2.3
	 */
	public void setServletRelativeAction(@Nullable String servletRelativeAction) {
		this.servletRelativeAction = servletRelativeAction;
	}

	/**
	 * 获取 '{@code action}' 属性的 Servlet 相对值。
	 *
	 * @since 3.2.3
	 */
	@Nullable
	protected String getServletRelativeAction() {
		return this.servletRelativeAction;
	}

	/**
	 * 设置 '{@code method}' 属性的值。
	 * <p>可以是运行时表达式。
	 */
	public void setMethod(String method) {
		this.method = method;
	}

	/**
	 * 获取 '{@code method}' 属性的值。
	 */
	protected String getMethod() {
		return this.method;
	}

	/**
	 * 设置 '{@code target}' 属性的值。
	 * <p>可以是运行时表达式。
	 */
	public void setTarget(String target) {
		this.target = target;
	}

	/**
	 * 获取 '{@code target}' 属性的值。
	 */
	@Nullable
	public String getTarget() {
		return this.target;
	}

	/**
	 * 设置 '{@code enctype}' 属性的值。
	 * <p>可以是运行时表达式。
	 */
	public void setEnctype(String enctype) {
		this.enctype = enctype;
	}

	/**
	 * 获取 '{@code enctype}' 属性的值。
	 */
	@Nullable
	protected String getEnctype() {
		return this.enctype;
	}

	/**
	 * 设置 '{@code acceptCharset}' 属性的值。
	 * <p>可以是运行时表达式。
	 */
	public void setAcceptCharset(String acceptCharset) {
		this.acceptCharset = acceptCharset;
	}

	/**
	 * 获取 '{@code acceptCharset}' 属性的值。
	 */
	@Nullable
	protected String getAcceptCharset() {
		return this.acceptCharset;
	}

	/**
	 * 设置 '{@code onsubmit}' 属性的值。
	 * <p>可以是运行时表达式。
	 */
	public void setOnsubmit(String onsubmit) {
		this.onsubmit = onsubmit;
	}

	/**
	 * 获取 '{@code onsubmit}' 属性的值。
	 */
	@Nullable
	protected String getOnsubmit() {
		return this.onsubmit;
	}

	/**
	 * 设置 '{@code onreset}' 属性的值。
	 * <p>可以是运行时表达式。
	 */
	public void setOnreset(String onreset) {
		this.onreset = onreset;
	}

	/**
	 * 获取 '{@code onreset}' 属性的值。
	 */
	@Nullable
	protected String getOnreset() {
		return this.onreset;
	}

	/**
	 * 设置 '{@code autocomplete}' 属性的值。
	 * 可以是运行时表达式。
	 */
	public void setAutocomplete(String autocomplete) {
		this.autocomplete = autocomplete;
	}

	/**
	 * 获取 '{@code autocomplete}' 属性的值。
	 */
	@Nullable
	protected String getAutocomplete() {
		return this.autocomplete;
	}

	/**
	 * 设置非浏览器支持的 HTTP 方法的请求参数名称。
	 */
	public void setMethodParam(String methodParam) {
		this.methodParam = methodParam;
	}

	/**
	 * 获取非浏览器支持的 HTTP 方法的请求参数名称。
	 *
	 * @since 4.2.3
	 */
	protected String getMethodParam() {
		return this.methodParam;
	}

	/**
	 * 确定浏览器是否支持 HTTP 方法（即 GET 或 POST）。
	 */
	protected boolean isMethodBrowserSupported(String method) {
		return ("get".equalsIgnoreCase(method) || "post".equalsIgnoreCase(method));
	}


	/**
	 * 写入 '{@code form}' 标签块的开头部分，并将表单对象放入模型中（如果适用）。
	 *
	 * @return 标签是否已渲染？
	 */
	@Override
	protected int writeTagContent(TagWriter tagWriter) throws JspException {
		// 设置标签写入器
		this.tagWriter = tagWriter;

		// 开始 FORM 标签
		tagWriter.startTag(FORM_TAG);
		// 写入默认属性
		writeDefaultAttributes(tagWriter);
		// 写入 ACTION 属性
		tagWriter.writeAttribute(ACTION_ATTRIBUTE, resolveAction());
		// 写入可选的 METHOD 属性
		writeOptionalAttribute(tagWriter, METHOD_ATTRIBUTE, getHttpMethod());
		// 写入可选的 TARGET 属性
		writeOptionalAttribute(tagWriter, TARGET_ATTRIBUTE, getTarget());
		// 写入可选的 ENCTYPE 属性
		writeOptionalAttribute(tagWriter, ENCTYPE_ATTRIBUTE, getEnctype());
		// 写入可选的 ACCEPT-CHARSET 属性
		writeOptionalAttribute(tagWriter, ACCEPT_CHARSET_ATTRIBUTE, getAcceptCharset());
		// 写入可选的 ONSUBMIT 属性
		writeOptionalAttribute(tagWriter, ONSUBMIT_ATTRIBUTE, getOnsubmit());
		// 写入可选的 ONRESET 属性
		writeOptionalAttribute(tagWriter, ONRESET_ATTRIBUTE, getOnreset());
		// 写入可选的 AUTOCOMPLETE 属性
		writeOptionalAttribute(tagWriter, AUTOCOMPLETE_ATTRIBUTE, getAutocomplete());

		// 强制标签闭合
		tagWriter.forceBlock();

		// 如果方法不受浏览器支持
		if (!isMethodBrowserSupported(getMethod())) {
			// 确保方法是支持的 HTTP 方法
			assertHttpMethod(getMethod());
			// 获取方法参数的名称和类型
			String inputName = getMethodParam();
			String inputType = "hidden";
			// 开始 INPUT 标签
			tagWriter.startTag(INPUT_TAG);
			// 写入可选的 TYPE 属性
			writeOptionalAttribute(tagWriter, TYPE_ATTRIBUTE, inputType);
			// 写入可选的 NAME 属性
			writeOptionalAttribute(tagWriter, NAME_ATTRIBUTE, inputName);
			// 写入可选的 VALUE 属性
			writeOptionalAttribute(tagWriter, VALUE_ATTRIBUTE, processFieldValue(inputName, getMethod(), inputType));
			// 结束 INPUT 标签
			tagWriter.endTag();
		}

		// 暴露表单对象名称以供嵌套标签使用
		String modelAttribute = resolveModelAttribute();
		this.pageContext.setAttribute(MODEL_ATTRIBUTE_VARIABLE_NAME, modelAttribute, PageContext.REQUEST_SCOPE);

		// 保存先前的 嵌套路径，构建并暴露当前的 嵌套路径。
		// 使用请求范围将 嵌套路径 暴露给包含的页面。
		this.previousNestedPath =
				(String) this.pageContext.getAttribute(NESTED_PATH_VARIABLE_NAME, PageContext.REQUEST_SCOPE);
		this.pageContext.setAttribute(NESTED_PATH_VARIABLE_NAME,
				modelAttribute + PropertyAccessor.NESTED_PROPERTY_SEPARATOR, PageContext.REQUEST_SCOPE);

		// 返回评估正文的结果
		return EVAL_BODY_INCLUDE;
	}

	private String getHttpMethod() {
		return (isMethodBrowserSupported(getMethod()) ? getMethod() : DEFAULT_METHOD);
	}

	private void assertHttpMethod(String method) {
		for (HttpMethod httpMethod : HttpMethod.values()) {
			if (httpMethod.name().equalsIgnoreCase(method)) {
				return;
			}
		}
		throw new IllegalArgumentException("Invalid HTTP method: " + method);
	}

	/**
	 * 自动生成的 ID 对应于表单对象名称。
	 */
	@Override
	protected String autogenerateId() throws JspException {
		return resolveModelAttribute();
	}

	/**
	 * {@link #evaluate 解析并返回} 表单对象的名称。
	 *
	 * @throws IllegalArgumentException 如果表单对象解析为 {@code null}
	 */
	protected String resolveModelAttribute() throws JspException {
		// 获取已解析好的模型属性
		Object resolvedModelAttribute = evaluate(MODEL_ATTRIBUTE, getModelAttribute());
		// 如果不存在 已解析好的模型属性
		if (resolvedModelAttribute == null) {
			// 抛出 IllegalArgumentException
			throw new IllegalArgumentException(MODEL_ATTRIBUTE + " must not be null");
		}
		// 返回 已解析好的模型属性 的字符串形式
		return (String) resolvedModelAttribute;
	}

	/**
	 * 解析 '{@code action}' 属性的值。
	 * <p>如果用户配置了 '{@code action}' 值，则使用评估此值的结果。 如果用户配置了 '{@code servletRelativeAction}' 值，
	 * 则将该值前置到上下文和 servlet 路径中，然后使用结果。 否则，使用 {@link org.springframework.web.servlet.support.RequestContext#getRequestUri()
	 * 发起的 URI}。
	 *
	 * @return 用于 '{@code action}' 属性的值
	 */
	protected String resolveAction() throws JspException {
		// 获取 action路径
		String action = getAction();
		// 获取Servlet 相对action路径
		String servletRelativeAction = getServletRelativeAction();
		// 如果 action路径有值
		if (StringUtils.hasText(action)) {
			// 评估 action路径
			action = getDisplayString(evaluate(ACTION_ATTRIBUTE, action));
			// 处理 action路径
			return processAction(action);
		} else if (StringUtils.hasText(servletRelativeAction)) {
			// 如果 Servlet 相对action路径有值
			String pathToServlet = getRequestContext().getPathToServlet();
			// 如果 Servlet 相对action路径 以 '/' 开头且不以  上下文路径 开头
			if (servletRelativeAction.startsWith("/") &&
					!servletRelativeAction.startsWith(getRequestContext().getContextPath())) {
				// 将 Servlet相对t路径 设置为 上下文路径+SServlet 相对action路径
				servletRelativeAction = pathToServlet + servletRelativeAction;
			}
			// 评估 Servlet相对t路径
			servletRelativeAction = getDisplayString(evaluate(ACTION_ATTRIBUTE, servletRelativeAction));
			// 处理 Servlet相对t路径
			return processAction(servletRelativeAction);
		} else {
			// 获取请求 URI
			String requestUri = getRequestContext().getRequestUri();
			// 获取字符编码
			String encoding = this.pageContext.getResponse().getCharacterEncoding();
			try {
				// 编码请求 URI
				requestUri = UriUtils.encodePath(requestUri, encoding);
			} catch (UnsupportedCharsetException ex) {
				// 不应该发生 - 如果发生了，继续使用 requestUri
			}
			// 获取响应对象
			ServletResponse response = this.pageContext.getResponse();
			// 如果响应对象是 HttpServletResponse 的实例
			if (response instanceof HttpServletResponse) {
				// 编码请求 URI
				requestUri = ((HttpServletResponse) response).encodeURL(requestUri);
				// 获取查询字符串
				String queryString = getRequestContext().getQueryString();
				// 如果查询字符串包含文本
				if (StringUtils.hasText(queryString)) {
					// 将查询字符串添加到请求 URI 中
					requestUri += "?" + HtmlUtils.htmlEscape(queryString);
				}
			}
			// 如果请求 URI 包含文本
			if (StringUtils.hasText(requestUri)) {
				// 处理请求 URI
				return processAction(requestUri);
			} else {
				// 否则抛出 IllegalArgumentException
				throw new IllegalArgumentException("Attribute 'action' is required. " +
						"Attempted to resolve against current request URI but request URI was null.");
			}
		}
	}

	/**
	 * 如果配置了 {@link RequestDataValueProcessor} 实例，则通过该实例处理操作，否则返回未修改的操作。
	 */
	private String processAction(String action) {
		// 获取请求数据值处理器
		RequestDataValueProcessor processor = getRequestContext().getRequestDataValueProcessor();
		// 获取 Servlet 请求
		ServletRequest request = this.pageContext.getRequest();
		// 如果处理器不为空且请求是 HttpServletRequest 的实例
		if (processor != null && request instanceof HttpServletRequest) {
			// 处理 action
			action = processor.processAction((HttpServletRequest) request, action, getHttpMethod());
		}
		// 返回处理后的 action
		return action;
	}

	/**
	 * 写入 '{@code form}' 标签块的末尾部分。
	 * <p>包括表单的 '{@code enctype}' 属性（如果指定的话）以及浏览器不支持的 HTTP 方法。
	 *
	 * @return 标签是否已渲染？
	 */
	@Override
	public int doEndTag() throws JspException {
		// 获取请求数据值处理器
		RequestDataValueProcessor processor = getRequestContext().getRequestDataValueProcessor();
		// 获取 Servlet 请求
		ServletRequest request = this.pageContext.getRequest();
		// 如果处理器不为空且请求是 HttpServletRequest 的实例
		if (processor != null && request instanceof HttpServletRequest) {
			// 写入额外的隐藏字段
			writeHiddenFields(processor.getExtraHiddenFields((HttpServletRequest) request));
		}
		// 断言确保标签写入器不为空
		Assert.state(this.tagWriter != null, "No TagWriter set");
		// 结束标签
		this.tagWriter.endTag();
		// 返回评估页面的结果
		return EVAL_PAGE;
	}

	/**
	 * 将给定的值作为隐藏字段写入。
	 */
	private void writeHiddenFields(@Nullable Map<String, String> hiddenFields) throws JspException {
		// 如果隐藏字段集合不为空
		if (!CollectionUtils.isEmpty(hiddenFields)) {
			// 断言确保标签写入器不为空
			Assert.state(this.tagWriter != null, "No TagWriter set");
			// 追加标签内容
			this.tagWriter.appendValue("<div>\n");
			// 遍历隐藏字段集合
			for (Map.Entry<String, String> entry : hiddenFields.entrySet()) {
				// 追加隐藏字段的输入标签
				this.tagWriter.appendValue("<input type=\"hidden\" ");
				// 追加隐藏字段的 name 和 value 属性
				this.tagWriter.appendValue("name=\"" + entry.getKey() + "\" value=\"" + entry.getValue() + "\" ");
				this.tagWriter.appendValue("/>\n");
			}
			// 追加结束标签
			this.tagWriter.appendValue("</div>");
		}
	}

	/**
	 * 清除存储的 {@link TagWriter}。
	 */
	@Override
	public void doFinally() {
		super.doFinally();

		this.pageContext.removeAttribute(MODEL_ATTRIBUTE_VARIABLE_NAME, PageContext.REQUEST_SCOPE);
		if (this.previousNestedPath != null) {
			// 暴露先前的 nestedPath 值。
			this.pageContext.setAttribute(NESTED_PATH_VARIABLE_NAME, this.previousNestedPath, PageContext.REQUEST_SCOPE);
		} else {
			// 移除暴露的 nestedPath 值。
			this.pageContext.removeAttribute(NESTED_PATH_VARIABLE_NAME, PageContext.REQUEST_SCOPE);
		}
		this.tagWriter = null;
		this.previousNestedPath = null;
	}


	/**
	 * 覆盖解析 CSS 类，因为不支持错误类。
	 */
	@Override
	protected String resolveCssClass() throws JspException {
		return ObjectUtils.getDisplayString(evaluate("cssClass", getCssClass()));
	}

	/**
	 * 表单不支持此操作。
	 *
	 * @throws UnsupportedOperationException 总是抛出
	 */
	@Override
	public void setPath(String path) {
		throw new UnsupportedOperationException("The 'path' attribute is not supported for forms");
	}

	/**
	 * 表单不支持此操作。
	 *
	 * @throws UnsupportedOperationException 总是抛出
	 */
	@Override
	public void setCssErrorClass(String cssErrorClass) {
		throw new UnsupportedOperationException("The 'cssErrorClass' attribute is not supported for forms");
	}

}
