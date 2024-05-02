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

package org.springframework.web.servlet.tags.form;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.BodyTag;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * {@code <errors>} 标签将字段错误渲染为 HTML 的 'span' 标签。
 * 显示对象或特定字段的错误。
 *
 * <p>此标签支持三种主要用法模式：
 *
 * <ol>
 *     <li>仅字段 - 将 '{@code path}' 设置为字段名称（或路径）</li>
 *     <li>仅对象错误 - 省略 '{@code path}'</li>
 *     <li>所有错误 - 将 '{@code path}' 设置为 '{@code *}'</li>
 * </ol>
 *
 * <p>
 * <table>
 * <caption>属性摘要</caption>
 * <thead>
 * <tr>
 * <th class="colFirst">属性</th>
 * <th class="colOne">是否必需</th>
 * <th class="colOne">运行时表达式</th>
 * <th class="colLast">描述</th>
 * </tr>
 * </thead>
 * <tbody>
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
 * <td><p>delimiter</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>显示多个错误消息时的分隔符。默认为 br 标签。</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>dir</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 标准属性</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>element</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>用于渲染封闭错误的 HTML 元素。</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>htmlEscape</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>启用/禁用渲染值的 HTML 转义。</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>id</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 标准属性</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>lang</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 标准属性</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>onclick</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>ondblclick</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>onkeydown</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>onkeypress</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>onkeyup</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>onmousedown</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>onmousemove</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>onmouseout</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>onmouseover</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>onmouseup</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 事件属性</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>path</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>数据绑定的错误对象的路径</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>tabindex</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML 标准属性</p></td>
 * </tr>
 * <tr class="altColor">
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
 * @author Rick Evans
 * @since 2.0
 */
@SuppressWarnings("serial")
public class ErrorsTag extends AbstractHtmlElementBodyTag implements BodyTag {

	/**
	 * 在 {@link PageContext#PAGE_SCOPE 页面上下文范围} 中公开此标签将错误消息的密钥。
	 */
	public static final String MESSAGES_ATTRIBUTE = "messages";

	/**
	 * HTML '{@code span}' 标签。
	 */
	public static final String SPAN_TAG = "span";

	/**
	 * 用于渲染错误消息的 HTML 元素
	 */
	private String element = SPAN_TAG;

	/**
	 * 在错误消息之间使用的分隔符。
	 */
	private String delimiter = "<br/>";

	/**
	 * 在标签开始之前存储 'errors messages' 中存在的任何值。
	 */
	@Nullable
	private Object oldMessages;

	/**
	 * 错误消息是否被暴露
	 */
	private boolean errorMessagesWereExposed;


	/**
	 * 设置必须用于渲染错误消息的 HTML 元素。
	 * <p>默认为 HTML '{@code <span/>}' 标签。
	 */
	public void setElement(String element) {
		Assert.hasText(element, "'element' cannot be null or blank");
		this.element = element;
	}

	/**
	 * 获取必须用于渲染错误消息的 HTML 元素。
	 */
	public String getElement() {
		return this.element;
	}

	/**
	 * 设置在错误消息之间使用的分隔符。
	 * <p>默认为 HTML '{@code <br/>}' 标签。
	 */
	public void setDelimiter(String delimiter) {
		this.delimiter = delimiter;
	}

	/**
	 * 返回在错误消息之间使用的分隔符。
	 */
	public String getDelimiter() {
		return this.delimiter;
	}


	/**
	 * 获取 HTML '{@code id}' 属性的值。
	 * <p>将 '{@code .errors}' 追加到 {@link #getPropertyPath()} 返回的值，
	 * 或者将模型属性名称作为 '{@code <form:errors/>}' 标签的 '{@code path}' 属性被省略。
	 *
	 * @return HTML '{@code id}' 属性的值
	 * @see #getPropertyPath()
	 */
	@Override
	protected String autogenerateId() throws JspException {
		// 获取属性路径
		String path = getPropertyPath();
		// 如果路径为空或为通配符
		if (!StringUtils.hasLength(path) || "*".equals(path)) {
			// 获取请求作用域中的模型属性变量名
			path = (String) this.pageContext.getAttribute(FormTag.MODEL_ATTRIBUTE_VARIABLE_NAME, PageContext.REQUEST_SCOPE);
		}
		// 返回路径去除 "[]" 后加上 ".errors"
		return StringUtils.deleteAny(path, "[]") + ".errors";
	}

	/**
	 * 获取 HTML '{@code name}' 属性的值。
	 * <p>由于 '{@code name}' 属性不是 '{@code span}' 元素的有效属性，因此简单地返回 {@code null}。
	 */
	@Override
	@Nullable
	protected String getName() throws JspException {
		return null;
	}

	/**
	 * 是否应该进行此标签的渲染？
	 * <p>仅在配置的 {@link #setPath path} 存在错误时才输出。
	 *
	 * @return 仅当配置的 {@link #setPath path} 存在错误时返回 {@code true}
	 */
	@Override
	protected boolean shouldRender() throws JspException {
		try {
			return getBindStatus().isError();
		} catch (IllegalStateException ex) {
			// BindingResult 或目标对象都不可用。
			return false;
		}
	}

	@Override
	protected void renderDefaultContent(TagWriter tagWriter) throws JspException {
		// 开始标签
		tagWriter.startTag(getElement());
		// 写入默认属性
		writeDefaultAttributes(tagWriter);
		// 获取分隔符
		String delimiter = ObjectUtils.getDisplayString(evaluate("delimiter", getDelimiter()));
		// 获取绑定状态的错误消息数组
		String[] errorMessages = getBindStatus().getErrorMessages();
		// 遍历错误消息数组
		for (int i = 0; i < errorMessages.length; i++) {
			String errorMessage = errorMessages[i];
			// 如果不是第一个消息，追加分隔符
			if (i > 0) {
				tagWriter.appendValue(delimiter);
			}
			// 追加错误消息的显示字符串
			tagWriter.appendValue(getDisplayString(errorMessage));
		}
		// 结束标签
		tagWriter.endTag();
	}

	/**
	 * 在 {@link #MESSAGES_ATTRIBUTE 此键} 下在 {@link PageContext#PAGE_SCOPE 页面范围} 中公开任何绑定状态错误消息。
	 * <p>仅在 {@link #shouldRender()} 返回 {@code true} 时调用。
	 *
	 * @see #removeAttributes()
	 */
	@Override
	protected void exposeAttributes() throws JspException {
		// 获取绑定状态的错误消息数组，并转换为列表
		List<String> errorMessages = new ArrayList<>(Arrays.asList(getBindStatus().getErrorMessages()));
		// 保存旧消息变量
		this.oldMessages = this.pageContext.getAttribute(MESSAGES_ATTRIBUTE, PageContext.PAGE_SCOPE);
		// 在页面上下文中设置新的消息变量
		this.pageContext.setAttribute(MESSAGES_ATTRIBUTE, errorMessages, PageContext.PAGE_SCOPE);
		// 标记错误消息已经被暴露
		this.errorMessagesWereExposed = true;
	}

	/**
	 * 删除以前存储在 {@link #MESSAGES_ATTRIBUTE 此键} 下的绑定状态错误消息，
	 * 在 {@link PageContext#PAGE_SCOPE 页面范围} 中。
	 *
	 * @see #exposeAttributes()
	 */
	@Override
	protected void removeAttributes() {
		// 如果错误消息已经被暴露
		if (this.errorMessagesWereExposed) {
			// 如果旧消息不为空
			if (this.oldMessages != null) {
				// 将旧消息设置回 页面上下文
				this.pageContext.setAttribute(MESSAGES_ATTRIBUTE, this.oldMessages, PageContext.PAGE_SCOPE);
				// 将旧消息设为 null
				this.oldMessages = null;
			} else {
				// 否则移除消息变量
				this.pageContext.removeAttribute(MESSAGES_ATTRIBUTE, PageContext.PAGE_SCOPE);
			}
		}
	}

}
