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

package org.springframework.web.servlet.tags;

import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.util.JavaScriptUtils;
import org.springframework.web.util.TagUtils;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * {@code <message>} 标签在当前页面的范围内查找消息。消息使用 ApplicationContext 解析，因此支持国际化。
 *
 * <p>检测 HTML 转义设置，可以在此标签实例、页面级别或 {@code web.xml} 级别上进行设置。还可以应用 JavaScript 转义。
 *
 * <p>如果未设置 "code" 或无法解析，则将使用 "text" 作为默认消息。因此，此标签也可用于对任何文本进行 HTML 转义。
 *
 * <p>消息参数可以通过 {@link #setArguments(Object) arguments} 属性或使用嵌套的 {@code <spring:argument>} 标签指定。
 *
 * <table>
 * <caption>属性摘要</caption>
 * <thead>
 * <tr>
 * <th>属性</th>
 * <th>必需？</th>
 * <th>运行时表达式？</th>
 * <th>描述</th>
 * </tr>
 * </thead>
 * <tbody>
 * <tr>
 * <td>arguments</td>
 * <td>false</td>
 * <td>true</td>
 * <td>为此标签设置可选的消息参数，作为 (逗号分隔的) 字符串（每个字符串参数可以包含 JSP EL）、对象数组（用作参数数组）或单个对象（用作单个参数）。</td>
 * </tr>
 * <tr>
 * <td>argumentSeparator</td>
 * <td>false</td>
 * <td>true</td>
 * <td>用于分割参数字符串值的分隔符字符；默认为 '逗号' (',')。</td>
 * </tr>
 * <tr>
 * <td>code</td>
 * <td>false</td>
 * <td>true</td>
 * <td>在查找消息时要使用的代码（键）。如果未提供 code，则将使用 text 属性。</td>
 * </tr>
 * <tr>
 * <td>htmlEscape</td>
 * <td>false</td>
 * <td>true</td>
 * <td>为此标签设置 HTML 转义，作为布尔值。覆盖当前页面的默认 HTML 转义设置。</td>
 * </tr>
 * <tr>
 * <td>javaScriptEscape</td>
 * <td>false</td>
 * <td>true</td>
 * <td>为此标签设置 JavaScript 转义，作为布尔值。默认为 false。</td>
 * </tr>
 * <tr>
 * <td>message</td>
 * <td>false</td>
 * <td>true</td>
 * <td>一个 MessageSourceResolvable 参数（直接或通过 JSP EL）。与 Spring 自己的验证错误类一起使用时非常适合，它们都实现了 MessageSourceResolvable 接口。
 * 例如，这允许您迭代表单中的所有错误，将每个错误（使用运行时表达式）作为此 'message' 属性的值，从而简化了这些错误消息的显示。</td>
 * </tr>
 * <tr>
 * <td>scope</td>
 * <td>false</td>
 * <td>true</td>
 * <td>将结果导出到变量时要使用的作用域。仅在同时设置 var 时使用此属性。可能的值为 page、request、session 和 application。</td>
 * </tr>
 * <tr>
 * <td>text</td>
 * <td>false</td>
 * <td>true</td>
 * <td>在找不到给定代码的消息时要输出的默认文本。如果 text 和 code 都未设置，则标签将输出 null。</td>
 * </tr>
 * <tr>
 * <td>var</td>
 * <td>false</td>
 * <td>true</td>
 * <td>将结果绑定到页面、请求、会话或应用程序范围时要使用的字符串。如果未指定，则结果将输出到写入器（即通常直接到 JSP）。</td>
 * </tr>
 * </tbody>
 * </table>
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Nicholas Williams
 * @see #setCode
 * @see #setText
 * @see #setHtmlEscape
 * @see #setJavaScriptEscape
 * @see HtmlEscapeTag#setDefaultHtmlEscape
 * @see org.springframework.web.util.WebUtils#HTML_ESCAPE_CONTEXT_PARAM
 * @see ArgumentTag
 */
@SuppressWarnings("serial")
public class MessageTag extends HtmlEscapingAwareTag implements ArgumentAware {

	/**
	 * 默认用于拆分参数字符串的分隔符：逗号 (",")。
	 */
	public static final String DEFAULT_ARGUMENT_SEPARATOR = ",";

	/**
	 * 可解析的消息源
	 */
	@Nullable
	private MessageSourceResolvable message;

	/**
	 * 消息代码
	 */
	@Nullable
	private String code;

	/**
	 * 消息参数
	 */
	@Nullable
	private Object arguments;

	/**
	 * 拆分参数字符串的分隔符。
	 * 默认为逗号 (",")。
	 */
	private String argumentSeparator = DEFAULT_ARGUMENT_SEPARATOR;

	/**
	 * 嵌套的消息参数
	 */
	private List<Object> nestedArguments = Collections.emptyList();

	/**
	 * 消息文本
	 */
	@Nullable
	private String text;

	/**
	 * PageContext 属性名称
	 */
	@Nullable
	private String var;

	/**
	 * 变量的作用域。
	 */
	private String scope = TagUtils.SCOPE_PAGE;

	/**
	 * 是否对 JavaScript 进行转义
	 */
	private boolean javaScriptEscape = false;


	/**
	 * 设置此标签的 MessageSourceResolvable。
	 * <p>如果指定了 MessageSourceResolvable，则它有效地覆盖了此标签上指定的任何代码、参数或文本。
	 */
	public void setMessage(MessageSourceResolvable message) {
		this.message = message;
	}

	/**
	 * 设置此标签的消息代码。
	 */
	public void setCode(String code) {
		this.code = code;
	}

	/**
	 * 为此标签设置可选的消息参数，作为逗号分隔的字符串（每个字符串参数可以包含 JSP EL）、对象数组（用作参数数组）或单个对象（用作单个参数）。
	 */
	public void setArguments(Object arguments) {
		this.arguments = arguments;
	}

	/**
	 * 设置用于拆分参数字符串的分隔符。
	 * 默认为逗号 (",")。
	 *
	 * @see #setArguments
	 */
	public void setArgumentSeparator(String argumentSeparator) {
		this.argumentSeparator = argumentSeparator;
	}

	@Override
	public void addArgument(@Nullable Object argument) throws JspTagException {
		this.nestedArguments.add(argument);
	}

	/**
	 * 设置此标签的消息文本。
	 */
	public void setText(String text) {
		this.text = text;
	}

	/**
	 * 设置 PageContext 属性名称，用于公开一个包含解析后消息的变量。
	 *
	 * @see #setScope
	 * @see javax.servlet.jsp.PageContext#setAttribute
	 */
	public void setVar(String var) {
		this.var = var;
	}

	/**
	 * 设置要导出变量的作用域。
	 * 默认为 SCOPE_PAGE ("page")。
	 *
	 * @see #setVar
	 * @see org.springframework.web.util.TagUtils#SCOPE_PAGE
	 * @see javax.servlet.jsp.PageContext#setAttribute
	 */
	public void setScope(String scope) {
		this.scope = scope;
	}

	/**
	 * 设置此标签的 JavaScript 转义，作为布尔值。
	 * 默认为 "false"。
	 */
	public void setJavaScriptEscape(boolean javaScriptEscape) throws JspException {
		this.javaScriptEscape = javaScriptEscape;
	}


	@Override
	protected final int doStartTagInternal() throws JspException, IOException {
		this.nestedArguments = new ArrayList<>();
		return EVAL_BODY_INCLUDE;
	}

	/**
	 * 解析消息，根据需要进行转义，并将其写入页面（或公开为变量）。
	 *
	 * @see #resolveMessage()
	 * @see org.springframework.web.util.HtmlUtils#htmlEscape(String)
	 * @see org.springframework.web.util.JavaScriptUtils#javaScriptEscape(String)
	 * @see #writeMessage(String)
	 */
	@Override
	public int doEndTag() throws JspException {
		try {
			// 解析未转义的消息。
			String msg = resolveMessage();

			// 如果需要，进行 HTML 和/或 JavaScript 转义。
			msg = htmlEscape(msg);
			// 如果需要对JavaScript进行转义，则进行转义
			msg = this.javaScriptEscape ? JavaScriptUtils.javaScriptEscape(msg) : msg;

			// 如果需要，公开为变量，否则写入页面。
			if (this.var != null) {
				// 在页面上下文中公开这个变量
				this.pageContext.setAttribute(this.var, msg, TagUtils.getScope(this.scope));
			} else {
				// 否则写入页面
				writeMessage(msg);
			}

			return EVAL_PAGE;
		} catch (IOException ex) {
			throw new JspTagException(ex.getMessage(), ex);
		} catch (NoSuchMessageException ex) {
			throw new JspTagException(getNoSuchMessageExceptionDescription(ex));
		}
	}

	@Override
	public void release() {
		super.release();
		this.arguments = null;
	}


	/**
	 * 将指定的消息解析为具体的消息字符串。
	 * 返回的消息字符串应该是未转义的。
	 */
	protected String resolveMessage() throws JspException, NoSuchMessageException {
		MessageSource messageSource = getMessageSource();

		// 评估指定的 MessageSourceResolvable（如果有）。
		if (this.message != null) {
			// 我们有一个给定的 MessageSourceResolvable。
			// 如果有可解析的消息源，根据请求的区域设置进行语言切换
			return messageSource.getMessage(this.message, getRequestContext().getLocale());
		}

		if (this.code != null || this.text != null) {
			// 我们有要解析的代码或默认文本。
			// 解析参数
			Object[] argumentsArray = resolveArguments(this.arguments);
			if (!this.nestedArguments.isEmpty()) {
				// 如果有嵌套参数，则将嵌套惨胡添加到参数数组中。
				argumentsArray = appendArguments(argumentsArray, this.nestedArguments.toArray());
			}

			if (this.text != null) {
				// 我们有要考虑的备用文本。
				// 根据请求的区域设置进行语言切换
				String msg = messageSource.getMessage(
						this.code, argumentsArray, this.text, getRequestContext().getLocale());
				return (msg != null ? msg : "");
			} else {
				// 我们没有要考虑的备用文本。
				// 根据请求的区域设置进行语言切换
				return messageSource.getMessage(
						this.code, argumentsArray, getRequestContext().getLocale());
			}
		}

		throw new JspTagException("No resolvable message");
	}

	private Object[] appendArguments(@Nullable Object[] sourceArguments, Object[] additionalArguments) {
		if (ObjectUtils.isEmpty(sourceArguments)) {
			// 如果源参数数组为空，则直接返回附加参数数组
			return additionalArguments;
		}

		// 创建新的参数数组
		Object[] arguments = new Object[sourceArguments.length + additionalArguments.length];

		// 将源参数数组复制到新的参数数组中
		System.arraycopy(sourceArguments, 0, arguments, 0, sourceArguments.length);

		// 将附加参数数组复制到新的参数数组中
		System.arraycopy(additionalArguments, 0, arguments, sourceArguments.length, additionalArguments.length);

		// 返回新的参数数组
		return arguments;
	}

	/**
	 * 解析给定的参数对象为参数数组。
	 *
	 * @param arguments 指定的参数对象
	 * @return 解析后的参数数组
	 * @throws JspException 如果参数转换失败
	 * @see #setArguments
	 */
	@Nullable
	protected Object[] resolveArguments(@Nullable Object arguments) throws JspException {
		if (arguments instanceof String) {
			// 如果参数是字符串类型，则将其按照指定的参数分隔符拆分为字符串数组返回
			return StringUtils.delimitedListToStringArray((String) arguments, this.argumentSeparator);
		} else if (arguments instanceof Object[]) {
			// 如果参数是数组类型，则直接返回
			return (Object[]) arguments;
		} else if (arguments instanceof Collection) {
			// 如果参数是集合类型，则转换为数组返回
			return ((Collection<?>) arguments).toArray();
		} else if (arguments != null) {
			// 如果参数不为空，则假设为单个参数对象，封装为数组返回
			return new Object[]{arguments};
		} else {
			// 如果参数为空，则返回 null
			return null;
		}
	}

	/**
	 * 将消息写入页面。
	 * <p>可以在子类中进行重写，例如用于测试目的。
	 *
	 * @param msg 要写入的消息
	 * @throws IOException 如果写入失败
	 */
	protected void writeMessage(String msg) throws IOException {
		this.pageContext.getOut().write(msg);
	}

	/**
	 * 使用当前 RequestContext 的应用程序上下文作为 MessageSource。
	 */
	protected MessageSource getMessageSource() {
		return getRequestContext().getMessageSource();
	}

	/**
	 * 返回默认的异常消息。
	 */
	protected String getNoSuchMessageExceptionDescription(NoSuchMessageException ex) {
		return ex.getMessage();
	}

}
