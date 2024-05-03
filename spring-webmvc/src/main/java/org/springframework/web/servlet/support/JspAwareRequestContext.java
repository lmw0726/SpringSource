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

package org.springframework.web.servlet.support;

import org.springframework.lang.Nullable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.jstl.core.Config;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * JSP-aware（和 JSTL-aware）的 RequestContext 的子类，允许从 {@code javax.servlet.jsp.PageContext} 填充上下文。
 *
 * <p>此上下文将检测页面/请求/会话/应用程序范围内的 JSTL 区域设置属性，除了基类提供的回退区域设置策略。
 *
 * @author Juergen Hoeller
 * @see #getFallbackLocale
 * @since 1.1.4
 */
public class JspAwareRequestContext extends RequestContext {

	/**
	 * 页面上下文
	 */
	private PageContext pageContext;


	/**
	 * 为给定的页面上下文创建一个新的 JspAwareRequestContext，使用请求属性进行错误检索。
	 *
	 * @param pageContext 当前 JSP 页面上下文
	 */
	public JspAwareRequestContext(PageContext pageContext) {
		this(pageContext, null);
	}

	/**
	 * 为给定的页面上下文创建一个新的 JspAwareRequestContext，使用给定的模型属性进行错误检索。
	 *
	 * @param pageContext 当前 JSP 页面上下文
	 * @param model       当前视图的模型属性（可以为 {@code null}，使用请求属性进行错误检索）
	 */
	public JspAwareRequestContext(PageContext pageContext, @Nullable Map<String, Object> model) {
		super((HttpServletRequest) pageContext.getRequest(), (HttpServletResponse) pageContext.getResponse(),
				pageContext.getServletContext(), model);
		this.pageContext = pageContext;
	}


	/**
	 * 返回底层的 PageContext。
	 * 仅供此包中的协作类使用。
	 */
	protected final PageContext getPageContext() {
		return this.pageContext;
	}

	/**
	 * 此实现在页面、请求、会话或应用程序范围内检查 JSTL 区域设置属性；如果找不到，则返回 {@code HttpServletRequest.getLocale()}。
	 */
	@Override
	protected Locale getFallbackLocale() {
		if (jstlPresent) {
			// 如果存在JSTL，通过JSTL页面区域设置解析器解析当前页面的区域设置
			Locale locale = JstlPageLocaleResolver.getJstlLocale(getPageContext());
			if (locale != null) {
				// 如果区域设置不为空，则返回该区域设置
				return locale;
			}
		}
		// 否则，返回请求中的区域设置
		return getRequest().getLocale();
	}

	/**
	 * 此实现在页面、请求、会话或应用程序范围内检查 JSTL 时区属性；如果找不到，则返回 {@code null}。
	 */
	@Override
	protected TimeZone getFallbackTimeZone() {
		if (jstlPresent) {
			// 如果存在JSTL，通过JSTL页面区域设置解析器解析当前页面的时区
			TimeZone timeZone = JstlPageLocaleResolver.getJstlTimeZone(getPageContext());
			if (timeZone != null) {
				// 时区存在，则返回该时区
				return timeZone;
			}
		}
		// 否则，返回null
		return null;
	}


	/**
	 * 内部类，隔离了 JSTL 依赖项。
	 * 如果 JSTL API 存在，则仅在解析回退区域设置时调用。
	 */
	private static class JstlPageLocaleResolver {

		@Nullable
		public static Locale getJstlLocale(PageContext pageContext) {
			// 查找页面上下文中的Locale对象
			Object localeObject = Config.find(pageContext, Config.FMT_LOCALE);

			// 如果找到的LocaleObject是Locale类型，则直接返回；否则返回null
			return (localeObject instanceof Locale ? (Locale) localeObject : null);
		}

		@Nullable
		public static TimeZone getJstlTimeZone(PageContext pageContext) {
			// 查找页面上下文中的时区对象
			Object timeZoneObject = Config.find(pageContext, Config.FMT_TIME_ZONE);

			// 如果找到的timeZoneObject是TimeZone类型，则直接返回；否则返回null
			return (timeZoneObject instanceof TimeZone ? (TimeZone) timeZoneObject : null);
		}
	}

}
