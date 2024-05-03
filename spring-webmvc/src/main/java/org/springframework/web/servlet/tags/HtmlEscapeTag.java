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

package org.springframework.web.servlet.tags;

import javax.servlet.jsp.JspException;

/**
 * {@code <htmlEscape>} 标签为当前页面设置默认的 HTML 转义值。实际的值可以被支持转义的标签覆盖。
 * 默认值为 "false"。
 *
 * <p>注意：您也可以设置一个 "defaultHtmlEscape" 的 web.xml 上下文参数。页面级别的设置会覆盖上下文参数。
 *
 * <table>
 * <caption>属性摘要</caption>
 * <thead>
 * <tr>
 * <th>属性</th>
 * <th>是否必需？</th>
 * <th>运行时表达式？</th>
 * <th>描述</th>
 * </tr>
 * </thead>
 * <tbody>
 * <tr>
 * <td>defaultHtmlEscape</td>
 * <td>true</td>
 * <td>true</td>
 * <td>设置 HTML 转义的默认值，放入当前 PageContext。</td>
 * </tr>
 * </tbody>
 * </table>
 *
 * @author Juergen Hoeller
 * @see HtmlEscapingAwareTag#setHtmlEscape
 * @since 04.03.2003
 */
@SuppressWarnings("serial")
public class HtmlEscapeTag extends RequestContextAwareTag {

	/**
	 * 首付默认HTML转义
	 */
	private boolean defaultHtmlEscape;


	/**
	 * 设置 HTML 转义的默认值，放入当前 PageContext。
	 */
	public void setDefaultHtmlEscape(boolean defaultHtmlEscape) {
		this.defaultHtmlEscape = defaultHtmlEscape;
	}


	@Override
	protected int doStartTagInternal() throws JspException {
		getRequestContext().setDefaultHtmlEscape(this.defaultHtmlEscape);
		return EVAL_BODY_INCLUDE;
	}

}
