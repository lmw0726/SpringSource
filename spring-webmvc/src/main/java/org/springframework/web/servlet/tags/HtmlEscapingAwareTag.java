/*
 * Copyright 2002-2015 the original author or authors.
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

import org.springframework.lang.Nullable;
import org.springframework.web.util.HtmlUtils;

import javax.servlet.jsp.JspException;

/**
 * 用于输出可能需要进行 HTML 转义的内容的标签的超类。
 *
 * <p>提供了一个 "htmlEscape" 属性，用于显式指定是否应用 HTML 转义。如果未设置，
 * 则使用页面级别的默认值（例如来自 HtmlEscapeTag）或应用程序范围的默认值
 * （在 {@code web.xml} 中的 "defaultHtmlEscape" 上下文参数）。
 *
 * @author Juergen Hoeller
 * @author Brian Clozel
 * @see #setHtmlEscape
 * @see HtmlEscapeTag
 * @see org.springframework.web.servlet.support.RequestContext#isDefaultHtmlEscape
 * @see org.springframework.web.util.WebUtils#getDefaultHtmlEscape
 * @see org.springframework.web.util.WebUtils#getResponseEncodedHtmlEscape
 * @since 1.1
 */
@SuppressWarnings("serial")
public abstract class HtmlEscapingAwareTag extends RequestContextAwareTag {

	/**
	 * 是否对HTML进行转义
	 */
	@Nullable
	private Boolean htmlEscape;


	/**
	 * 设置此标签的 HTML 转义，作为布尔值。
	 * 覆盖当前页面的默认 HTML 转义设置。
	 *
	 * @see HtmlEscapeTag#setDefaultHtmlEscape
	 */
	public void setHtmlEscape(boolean htmlEscape) throws JspException {
		this.htmlEscape = htmlEscape;
	}

	/**
	 * 返回此标签的 HTML 转义设置，如果没有被覆盖，则返回默认设置。
	 *
	 * @see #isDefaultHtmlEscape()
	 */
	protected boolean isHtmlEscape() {
		if (this.htmlEscape != null) {
			// 如果已经设置了 Html转义 属性，则返回其值
			return this.htmlEscape.booleanValue();
		} else {
			// 否则，返回默认的 HTML 转义设置
			return isDefaultHtmlEscape();
		}
	}

	/**
	 * 返回此标签的适用默认 HTML 转义设置。
	 * <p>默认实现检查 RequestContext 的设置，如果没有明确给出默认值，则返回 {@code false}。
	 *
	 * @see #getRequestContext()
	 */
	protected boolean isDefaultHtmlEscape() {
		return getRequestContext().isDefaultHtmlEscape();
	}

	/**
	 * 返回用于此标签是否使用响应编码的 HTML 转义的适用默认值。
	 * <p>默认实现检查 RequestContext 的设置，如果没有明确给出默认值，则返回 {@code false}。
	 *
	 * @see #getRequestContext()
	 * @since 4.1.2
	 */
	protected boolean isResponseEncodedHtmlEscape() {
		return getRequestContext().isResponseEncodedHtmlEscape();
	}

	/**
	 * 如果启用了 "htmlEscape" 设置，则对给定的字符串进行 HTML 转义。
	 * <p>如果也启用了 "responseEncodedHtmlEscape" 设置，则会考虑响应编码。
	 *
	 * @param content 要转义的字符串
	 * @return 转义后的字符串
	 * @see #isHtmlEscape()
	 * @see #isResponseEncodedHtmlEscape()
	 * @since 4.1.2
	 */
	protected String htmlEscape(String content) {
		String out = content;
		// 检查是否需要进行 HTML 转义
		if (isHtmlEscape()) {
			// 检查是否使用了响应编码的 HTML 转义
			if (isResponseEncodedHtmlEscape()) {
				// 使用响应的字符编码进行 HTML 转义
				out = HtmlUtils.htmlEscape(content, this.pageContext.getResponse().getCharacterEncoding());
			} else {
				// 使用默认的字符编码进行 HTML 转义
				out = HtmlUtils.htmlEscape(content);
			}
		}
		return out;
	}

}
