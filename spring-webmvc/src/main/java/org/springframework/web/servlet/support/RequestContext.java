/*
 * Copyright 2002-2019 the original author or authors.
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

import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.SimpleTimeZoneAwareLocaleContext;
import org.springframework.context.i18n.TimeZoneAwareLocaleContext;
import org.springframework.lang.Nullable;
import org.springframework.ui.context.Theme;
import org.springframework.ui.context.ThemeSource;
import org.springframework.ui.context.support.ResourceBundleThemeSource;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.EscapedErrors;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.LocaleContextResolver;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.ThemeResolver;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UrlPathHelper;
import org.springframework.web.util.WebUtils;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.jstl.core.Config;
import java.util.*;

/**
 * 请求特定状态的上下文持有者，如当前 Web 应用程序上下文、当前区域设置、当前主题和潜在的绑定错误。
 * 提供对本地化消息和 Errors 实例的便捷访问。
 *
 * <p>适合在视图中公开，并在 JSP 的 "useBean" 标签、JSP 脚本块、JSTL EL 等中使用。
 * 对于没有访问 servlet 请求的视图（如 FreeMarker 模板）是必需的。
 *
 * <p>可以手动实例化，也可以通过 AbstractView 的 "requestContextAttribute" 属性自动公开为视图的模型属性。
 *
 * <p>还可以在 DispatcherServlet 请求之外工作，访问根 WebApplicationContext 并对区域设置（HttpServletRequest 的主要区域设置）进行适当的回退。
 *
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @see org.springframework.web.servlet.DispatcherServlet
 * @see org.springframework.web.servlet.view.AbstractView#setRequestContextAttribute
 * @see org.springframework.web.servlet.view.UrlBasedViewResolver#setRequestContextAttribute
 * @since 03.03.2003
 */
public class RequestContext {

	/**
	 * 如果 RequestContext 找不到 ThemeResolver，则使用的默认主题名称。
	 * 仅适用于非 DispatcherServlet 请求。
	 * <p>与 AbstractThemeResolver 的默认值相同，但此处未链接以避免包依赖关系。
	 *
	 * @see org.springframework.web.servlet.theme.AbstractThemeResolver#ORIGINAL_DEFAULT_THEME_NAME
	 */
	public static final String DEFAULT_THEME_NAME = "theme";

	/**
	 * 用于 RequestContext 使用的当前 Web 应用程序上下文的请求属性。
	 * 默认情况下，暴露 DispatcherServlet 的上下文（或根上下文作为回退）。
	 */
	public static final String WEB_APPLICATION_CONTEXT_ATTRIBUTE = RequestContext.class.getName() + ".CONTEXT";


	/**
	 * 是否存在JSTL
	 */
	protected static final boolean jstlPresent = ClassUtils.isPresent(
			"javax.servlet.jsp.jstl.core.Config", RequestContext.class.getClassLoader());

	/**
	 * Http请求
	 */
	private HttpServletRequest request;

	/**
	 * Http响应
	 */
	@Nullable
	private HttpServletResponse response;

	/**
	 * 模型Map
	 */
	@Nullable
	private Map<String, Object> model;

	/**
	 * Web应用上下文
	 */
	private WebApplicationContext webApplicationContext;

	/**
	 * 区域设置
	 */
	@Nullable
	private Locale locale;

	/**
	 * 时区
	 */
	@Nullable
	private TimeZone timeZone;

	/**
	 * 主题
	 */
	@Nullable
	private Theme theme;

	/**
	 * 是否默认HTML转义
	 */
	@Nullable
	private Boolean defaultHtmlEscape;

	/**
	 * 是否默认使用响应编码进行 HTML 转义
	 */
	@Nullable
	private Boolean responseEncodedHtmlEscape;

	/**
	 * URL路径助手
	 */
	private UrlPathHelper urlPathHelper;

	/**
	 * 请求数据值处理器
	 */
	@Nullable
	private RequestDataValueProcessor requestDataValueProcessor;

	/**
	 * 绑定对象名称-错误实例映射
	 */
	@Nullable
	private Map<String, Errors> errorsMap;


	/**
	 * 为给定的请求创建一个新的 RequestContext，使用请求属性来检索错误。
	 * <p>这只适用于 InternalResourceViews，因为 Errors 实例是模型的一部分，通常不会作为请求属性公开。
	 * 它通常将在 JSP 或自定义标签中使用。
	 * <p><b>仅在 DispatcherServlet 请求中有效。</b>
	 * 传入 ServletContext 可以回退到根 WebApplicationContext。
	 *
	 * @param request 当前 HTTP 请求
	 * @see org.springframework.web.servlet.DispatcherServlet
	 * @see #RequestContext(javax.servlet.http.HttpServletRequest, javax.servlet.ServletContext)
	 */
	public RequestContext(HttpServletRequest request) {
		this(request, null, null, null);
	}

	/**
	 * 为给定的请求创建一个新的 RequestContext，使用请求属性来检索错误。
	 * <p>这只适用于 InternalResourceViews，因为 Errors 实例是模型的一部分，通常不会作为请求属性公开。
	 * 它通常将在 JSP 或自定义标签中使用。
	 * <p><b>仅在 DispatcherServlet 请求中有效。</b>
	 * 传入 ServletContext 可以回退到根 WebApplicationContext。
	 *
	 * @param request  当前 HTTP 请求
	 * @param response 当前 HTTP 响应
	 * @see org.springframework.web.servlet.DispatcherServlet
	 * @see #RequestContext(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, javax.servlet.ServletContext, Map)
	 */
	public RequestContext(HttpServletRequest request, HttpServletResponse response) {
		this(request, response, null, null);
	}

	/**
	 * 为给定的请求创建一个新的 RequestContext，使用请求属性来检索错误。
	 * <p>这只适用于 InternalResourceViews，因为 Errors 实例是模型的一部分，通常不会作为请求属性公开。
	 * 它通常将在 JSP 或自定义标签中使用。
	 * <p>如果指定了 ServletContext，则 RequestContext 还将与根 WebApplicationContext（在 DispatcherServlet 外部）一起工作。
	 *
	 * @param request        当前 HTTP 请求
	 * @param servletContext Web 应用程序的 Servlet 上下文（可以为 {@code null}；用于回退到根 WebApplicationContext）
	 * @see org.springframework.web.context.WebApplicationContext
	 * @see org.springframework.web.servlet.DispatcherServlet
	 */
	public RequestContext(HttpServletRequest request, @Nullable ServletContext servletContext) {
		this(request, null, servletContext, null);
	}

	/**
	 * 为给定的请求创建一个新的 RequestContext，使用给定的模型属性来检索错误。
	 * <p>这适用于所有 View 实现。它通常由 View 实现使用。
	 * <p><b>仅在 DispatcherServlet 请求中有效。</b>
	 * 传入 ServletContext 可以回退到根 WebApplicationContext。
	 *
	 * @param request 当前 HTTP 请求
	 * @param model   当前视图的模型属性（可以为 {@code null}，使用请求属性来检索错误）
	 * @see org.springframework.web.servlet.DispatcherServlet
	 * @see #RequestContext(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, javax.servlet.ServletContext, Map)
	 */
	public RequestContext(HttpServletRequest request, @Nullable Map<String, Object> model) {
		this(request, null, null, model);
	}

	/**
	 * 为给定的请求创建一个新的 RequestContext，使用给定的模型属性来检索错误。
	 * <p>这适用于所有 View 实现。它通常由 View 实现使用。
	 * <p>如果指定了 ServletContext，则 RequestContext 还将与根 WebApplicationContext（在 DispatcherServlet 外部）一起工作。
	 *
	 * @param request        当前 HTTP 请求
	 * @param response       当前 HTTP 响应
	 * @param servletContext Web 应用程序的 Servlet 上下文（可以为 {@code null}；用于回退到根 WebApplicationContext）
	 * @param model          当前视图的模型属性（可以为 {@code null}，使用请求属性来检索错误）
	 * @see org.springframework.web.context.WebApplicationContext
	 * @see org.springframework.web.servlet.DispatcherServlet
	 */
	public RequestContext(HttpServletRequest request, @Nullable HttpServletResponse response,
						  @Nullable ServletContext servletContext, @Nullable Map<String, Object> model) {
		// 设置Http请求
		this.request = request;
		// 设置Http响应
		this.response = response;
		// 设置模型对象
		this.model = model;

		// 获取 WebApplicationContext，从 DispatcherServlet 或根上下文中获取。
		// 必须指定 ServletContext 才能回退到根上下文！
		// 获取 Web应用上下文
		WebApplicationContext wac = (WebApplicationContext) request.getAttribute(WEB_APPLICATION_CONTEXT_ATTRIBUTE);
		if (wac == null) {
			// 如果不存在 Web应用上下文 ，则从 Servelet上下文中获取 Web应用上下文
			wac = RequestContextUtils.findWebApplicationContext(request, servletContext);
			if (wac == null) {
				// 如果还获取不到，则抛出异常
				throw new IllegalStateException("No WebApplicationContext found: not in a DispatcherServlet " +
						"request and no ContextLoaderListener registered?");
			}
		}
		// 设置 Web应用上下文
		this.webApplicationContext = wac;

		Locale locale = null;
		TimeZone timeZone = null;

		// 确定 RequestContext 使用的区域设置。
		// 获取本地环境解析器
		LocaleResolver localeResolver = RequestContextUtils.getLocaleResolver(request);
		if (localeResolver instanceof LocaleContextResolver) {
			// 本地环境解析器是 LocaleContextResolver 类型
			// 解析请求，获取 区域设置上下文
			LocaleContext localeContext = ((LocaleContextResolver) localeResolver).resolveLocaleContext(request);
			// 获取区域设置
			locale = localeContext.getLocale();
			if (localeContext instanceof TimeZoneAwareLocaleContext) {
				// 如果 区域设置上下文 是 TimeZoneAwareLocaleContext 类型，则获取时区
				timeZone = ((TimeZoneAwareLocaleContext) localeContext).getTimeZone();
			}
		} else if (localeResolver != null) {
			// 如果 本地环境解析器存爱，则解析请求获取区域设置
			// 尝试使用 LocaleResolver（在 DispatcherServlet 请求中）。
			locale = localeResolver.resolveLocale(request);
		}
		// 设置 区域设置 和 时区
		this.locale = locale;
		this.timeZone = timeZone;

		// 从 web.xml 中的 "defaultHtmlEscape" context-param 中确定默认的 HTML 转义设置（如果有）。
		this.defaultHtmlEscape = WebUtils.getDefaultHtmlEscape(this.webApplicationContext.getServletContext());

		// 从 web.xml 中的 "responseEncodedHtmlEscape" context-param 中确定响应编码的 HTML 转义设置（如果有）。
		this.responseEncodedHtmlEscape =
				WebUtils.getResponseEncodedHtmlEscape(this.webApplicationContext.getServletContext());
		// 创建一个新的URL路径助手
		this.urlPathHelper = new UrlPathHelper();

		if (this.webApplicationContext.containsBean(RequestContextUtils.REQUEST_DATA_VALUE_PROCESSOR_BEAN_NAME)) {
			// 如果 Web应用上下文 中包含 RequestDataValueProcessor 实例，则获取该实例并设置 请求数据值处理器
			this.requestDataValueProcessor = this.webApplicationContext.getBean(
					RequestContextUtils.REQUEST_DATA_VALUE_PROCESSOR_BEAN_NAME, RequestDataValueProcessor.class);
		}
	}


	/**
	 * 返回底层的 HttpServletRequest。仅供此包中的协作类使用。
	 */
	protected final HttpServletRequest getRequest() {
		return this.request;
	}

	/**
	 * 返回底层的 ServletContext。仅供此包中的协作类使用。
	 */
	@Nullable
	protected final ServletContext getServletContext() {
		return this.webApplicationContext.getServletContext();
	}

	/**
	 * 返回当前的 WebApplicationContext。
	 */
	public final WebApplicationContext getWebApplicationContext() {
		return this.webApplicationContext;
	}

	/**
	 * 将当前的 WebApplicationContext 返回为 MessageSource。
	 */
	public final MessageSource getMessageSource() {
		return this.webApplicationContext;
	}

	/**
	 * 返回此 RequestContext 封装的模型 Map（如果有）。
	 *
	 * @return 填充的模型 Map；如果没有可用的，则返回 {@code null}
	 */
	@Nullable
	public final Map<String, Object> getModel() {
		return this.model;
	}

	/**
	 * 返回当前区域设置（如果没有，则回退到请求区域设置；永远不为 {@code null}）。
	 * <p>通常来自 DispatcherServlet 的 {@link LocaleResolver}。
	 * 还包括对 JSTL 的 Locale 属性的回退检查。
	 *
	 * @see RequestContextUtils#getLocale
	 */
	public final Locale getLocale() {
		return (this.locale != null ? this.locale : getFallbackLocale());
	}

	/**
	 * 返回当前时区（如果从请求中无法推导，则返回 {@code null}）。
	 * <p>通常来自 DispatcherServlet 的 {@link LocaleContextResolver}。
	 * 还包括对 JSTL 的 TimeZone 属性的回退检查。
	 *
	 * @see RequestContextUtils#getTimeZone
	 */
	@Nullable
	public TimeZone getTimeZone() {
		return (this.timeZone != null ? this.timeZone : getFallbackTimeZone());
	}

	/**
	 * 确定此上下文的回退区域设置。
	 * <p>默认实现检查请求、会话或应用程序范围中的 JSTL 区域设置属性；
	 * 如果未找到，则返回 {@code HttpServletRequest.getLocale()}。
	 *
	 * @return 回退区域设置（永远不为 {@code null}）
	 * @see javax.servlet.http.HttpServletRequest#getLocale()
	 */
	protected Locale getFallbackLocale() {
		if (jstlPresent) {
			// 如果JSTL存在，则根据Http请求 和 Servlet应用上下文 通过JSTL本地化解析器获取区域设置
			Locale locale = JstlLocaleResolver.getJstlLocale(getRequest(), getServletContext());
			if (locale != null) {
				// 如果区域设置存在，则返回该实例。
				return locale;
			}
		}
		// 否则，通过Http请求获取区域设置
		return getRequest().getLocale();
	}

	/**
	 * 确定此上下文的回退时区。
	 * <p>默认实现检查请求、会话或应用程序范围中的 JSTL 时区属性；如果未找到，则返回 {@code null}。
	 *
	 * @return 回退时区（如果从请求中无法推导，则返回 {@code null}）
	 */
	@Nullable
	protected TimeZone getFallbackTimeZone() {
		if (jstlPresent) {
			// 如果JSTL存在，则根据Http请求 和 Servlet应用上下文 通过JSTL本地化解析器获取时区
			TimeZone timeZone = JstlLocaleResolver.getJstlTimeZone(getRequest(), getServletContext());
			if (timeZone != null) {
				// 如果时区存在，则返回该时区
				return timeZone;
			}
		}
		// 否则，返回null
		return null;
	}

	/**
	 * 将当前区域设置更改为指定的区域设置，
	 * 通过配置的 {@link LocaleResolver} 存储新的区域设置。
	 *
	 * @param locale 新的区域设置
	 * @see LocaleResolver#setLocale
	 * @see #changeLocale(java.util.Locale, java.util.TimeZone)
	 */
	public void changeLocale(Locale locale) {
		// 获取本地环境解析器
		LocaleResolver localeResolver = RequestContextUtils.getLocaleResolver(this.request);
		if (localeResolver == null) {
			// 如果没有本地环境解析器，抛出异常
			throw new IllegalStateException("Cannot change locale if no LocaleResolver configured");
		}
		// 设置当前请求、响应 以及 区域设置 到本地环境解析器中
		localeResolver.setLocale(this.request, this.response, locale);
		this.locale = locale;
	}

	/**
	 * 将当前区域设置更改为指定的区域设置和时区上下文，
	 * 通过配置的 {@link LocaleResolver} 存储新的区域设置上下文。
	 *
	 * @param locale   新的区域设置
	 * @param timeZone 新的时区
	 * @see LocaleContextResolver#setLocaleContext
	 * @see org.springframework.context.i18n.SimpleTimeZoneAwareLocaleContext
	 */
	public void changeLocale(Locale locale, TimeZone timeZone) {
		// 获取本地环境解析器
		LocaleResolver localeResolver = RequestContextUtils.getLocaleResolver(this.request);
		if (!(localeResolver instanceof LocaleContextResolver)) {
			// 如果本地环境解析器 不是 LocaleContextResolver 类型，抛出异常
			throw new IllegalStateException("Cannot change locale context if no LocaleContextResolver configured");
		}
		// 设置当前请求、响应以及区域设置和时区到 LocaleContextResolver 中
		((LocaleContextResolver) localeResolver).setLocaleContext(this.request, this.response,
				new SimpleTimeZoneAwareLocaleContext(locale, timeZone));
		this.locale = locale;
		this.timeZone = timeZone;
	}

	/**
	 * 返回当前主题（永远不为 {@code null}）。
	 * <p>在不使用主题支持时，采用延迟解析以提高效率。
	 */
	public Theme getTheme() {
		if (this.theme == null) {
			// 懒惰地确定此 RequestContext 使用的主题。
			this.theme = RequestContextUtils.getTheme(this.request);
			if (this.theme == null) {
				// 没有 ThemeResolver 和 ThemeSource 可用 -> 尝试使用回退主题。
				this.theme = getFallbackTheme();
			}
		}
		return this.theme;
	}

	/**
	 * 确定此上下文的回退主题。
	 * <p>默认实现返回默认主题（名称为 "theme"）。
	 *
	 * @return 回退主题（永远不为 {@code null}）
	 */
	protected Theme getFallbackTheme() {
		// 获取当前请求的主题源
		ThemeSource themeSource = RequestContextUtils.getThemeSource(getRequest());

		if (themeSource == null) {
			// 如果不存在主题源，则创建一个ResourceBundleThemeSource作为默认的主题源
			themeSource = new ResourceBundleThemeSource();
		}

		// 获取名为DEFAULT_THEME_NAME的主题
		Theme theme = themeSource.getTheme(DEFAULT_THEME_NAME);

		if (theme == null) {
			// 如果获取不到的主题，则抛出异常
			throw new IllegalStateException("No theme defined and no fallback theme found");
		}

		// 返回获取到的主题
		return theme;
	}

	/**
	 * 将当前主题更改为指定的主题，
	 * 通过配置的 {@link ThemeResolver} 存储新的主题名称。
	 *
	 * @param theme 新的主题
	 * @see ThemeResolver#setThemeName
	 */
	public void changeTheme(@Nullable Theme theme) {
		// 获取当前请求的主题解析器
		ThemeResolver themeResolver = RequestContextUtils.getThemeResolver(this.request);

		if (themeResolver == null) {
			// 如果没有主题解析器，则抛出异常
			throw new IllegalStateException("Cannot change theme if no ThemeResolver configured");
		}

		// 设置当前请求的主题名称
		themeResolver.setThemeName(this.request, this.response, (theme != null ? theme.getName() : null));

		// 更改主题为当前请求的主题
		this.theme = theme;
	}

	/**
	 * 将当前主题更改为指定名称的主题，
	 * 通过配置的 {@link ThemeResolver} 存储新的主题名称。
	 *
	 * @param themeName 新主题的名称
	 * @see ThemeResolver#setThemeName
	 */
	public void changeTheme(String themeName) {
		// 获取当前请求的主题解析器
		ThemeResolver themeResolver = RequestContextUtils.getThemeResolver(this.request);
		if (themeResolver == null) {
			// 如果没有主题解析器，则抛出异常
			throw new IllegalStateException("Cannot change theme if no ThemeResolver configured");
		}
		// 设置主题名称
		themeResolver.setThemeName(this.request, this.response, themeName);
		// 在下一次调用 getTheme 时重新解析。
		this.theme = null;
	}

	/**
	 * 设置（取消）消息和错误的默认HTML转义，在此RequestContext的范围内。
	 * <p>默认情况下为应用程序范围设置（在web.xml中的"context-param"中的"defaultHtmlEscape"）。
	 *
	 * @param defaultHtmlEscape 是否启用默认HTML转义
	 * @see org.springframework.web.util.WebUtils#getDefaultHtmlEscape
	 */
	public void setDefaultHtmlEscape(boolean defaultHtmlEscape) {
		this.defaultHtmlEscape = defaultHtmlEscape;
	}

	/**
	 * 默认的 HTML 转义是否处于活动状态？如果没有明确的默认值，则回退为 {@code false}。
	 */
	public boolean isDefaultHtmlEscape() {
		return (this.defaultHtmlEscape != null && this.defaultHtmlEscape.booleanValue());
	}

	/**
	 * 返回默认的 HTML 转义设置，区分未指定默认值和明确的值。
	 *
	 * @return 默认的 HTML 转义是否启用（null = 没有明确的默认值）
	 */
	@Nullable
	public Boolean getDefaultHtmlEscape() {
		return this.defaultHtmlEscape;
	}

	/**
	 * HTML 转义是否默认使用响应编码？
	 * 如果启用，只有 XML 标记的重要字符将使用 UTF-* 编码转义。
	 * <p>在没有明确的默认值的情况下回退为 {@code true}，从 Spring 4.2 开始。
	 *
	 * @since 4.1.2
	 */
	public boolean isResponseEncodedHtmlEscape() {
		return (this.responseEncodedHtmlEscape == null || this.responseEncodedHtmlEscape.booleanValue());
	}

	/**
	 * 返回关于默认使用响应编码进行 HTML 转义设置的默认设置，
	 * 区分未指定默认值和明确的值。
	 *
	 * @return 默认是否启用使用响应编码 HTML 转义（null = 没有明确的默认值）
	 * @since 4.1.2
	 */
	@Nullable
	public Boolean getResponseEncodedHtmlEscape() {
		return this.responseEncodedHtmlEscape;
	}


	/**
	 * 设置用于上下文路径和请求 URI 解码的 UrlPathHelper。
	 * 可以用于传递共享的 UrlPathHelper 实例。
	 * <p>始终可用默认的 UrlPathHelper。
	 */
	public void setUrlPathHelper(UrlPathHelper urlPathHelper) {
		Assert.notNull(urlPathHelper, "UrlPathHelper must not be null");
		this.urlPathHelper = urlPathHelper;
	}

	/**
	 * 返回用于上下文路径和请求 URI 解码的 UrlPathHelper。
	 * 可以用于配置当前的 UrlPathHelper。
	 * <p>始终可用默认的 UrlPathHelper。
	 */
	public UrlPathHelper getUrlPathHelper() {
		return this.urlPathHelper;
	}

	/**
	 * 从 WebApplicationContext 中获取用于请求数据值处理的 RequestDataValueProcessor 实例，
	 * 名称为 {@code "requestDataValueProcessor"}。
	 * 如果没有找到匹配的 bean，则返回 {@code null}。
	 */
	@Nullable
	public RequestDataValueProcessor getRequestDataValueProcessor() {
		return this.requestDataValueProcessor;
	}

	/**
	 * 返回原始请求的上下文路径，即指示当前 Web 应用程序的路径。这对于构建到应用程序内其他资源的链接很有用。
	 * <p>委托给 UrlPathHelper 进行解码。
	 *
	 * @see javax.servlet.http.HttpServletRequest#getContextPath
	 * @see #getUrlPathHelper
	 */
	public String getContextPath() {
		return this.urlPathHelper.getOriginatingContextPath(this.request);
	}

	/**
	 * 返回给定相对 URL 的上下文感知 URL。
	 *
	 * @param relativeUrl 相对 URL 部分
	 * @return 指向服务器的绝对路径的 URL（相应地进行 URL 编码）
	 */
	public String getContextUrl(String relativeUrl) {
		// 构建完整的URL路径
		String url = getContextPath() + relativeUrl;
		if (this.response != null) {
			// 如果有Http相应，则编码URL
			url = this.response.encodeURL(url);
		}
		return url;
	}

	/**
	 * 使用占位符（用花括号 {@code {}} 括起来的命名键）返回相对 URL 的上下文感知 URL。
	 * 例如，发送相对 URL {@code foo/{bar}?spam={spam}} 和参数映射 {@code {bar=baz,spam=nuts}}，
	 * 结果将是 {@code [contextpath]/foo/baz?spam=nuts}。
	 *
	 * @param relativeUrl 相对 URL 部分
	 * @param params      要插入到 URL 中的参数映射
	 * @return 一个指向服务器的绝对路径的 URL（相应地进行 URL 编码）
	 */
	public String getContextUrl(String relativeUrl, Map<String, ?> params) {
		// 构建完整的URL路径
		String url = getContextPath() + relativeUrl;
		// 使用UriComponentsBuilder构建URL
		url = UriComponentsBuilder.fromUriString(url)
				// 替换URL中的占位符参数
				.buildAndExpand(params)
				// 编码URL
				.encode()
				// 转换为URI对象
				.toUri()
				// 转换为ASCII字符串
				.toASCIIString();

		if (this.response != null) {
			// 如果存在响应对象，则对URL进行编码
			url = this.response.encodeURL(url);
		}

		return url;
	}

	/**
	 * 返回当前 servlet 内的 URL 映射路径，包括原始请求的上下文路径和 servlet 路径。
	 * 这对于在应用程序内构建到其他资源的链接很有用，其中使用类似 {@code "/main/*"} 的 servlet 映射。
	 * <p>委托给 UrlPathHelper 来确定上下文路径和 servlet 路径。
	 */
	public String getPathToServlet() {
		// 获取请求的原始上下文路径
		String path = this.urlPathHelper.getOriginatingContextPath(this.request);

		// 如果请求的路径包含在Servlet映射中，则添加Servlet路径到原始上下文路径中
		if (StringUtils.hasText(this.urlPathHelper.getPathWithinServletMapping(this.request))) {
			path += this.urlPathHelper.getOriginatingServletPath(this.request);
		}

		// 返回拼接后的路径
		return path;
	}

	/**
	 * 返回原始请求的请求 URI，即，不包含参数的调用 URL。
	 * 这对于作为 HTML 表单操作目标特别有用，可能与原始查询字符串结合使用。
	 * <p>委托给 UrlPathHelper 进行解码。
	 *
	 * @see #getQueryString
	 * @see org.springframework.web.util.UrlPathHelper#getOriginatingRequestUri
	 * @see #getUrlPathHelper
	 */
	public String getRequestUri() {
		return this.urlPathHelper.getOriginatingRequestUri(this.request);
	}

	/**
	 * 返回当前请求的查询字符串，即，请求路径后的部分。
	 * 这对于在与原始请求 URI 结合使用时构建 HTML 表单操作目标特别有用。
	 * <p>委托给 UrlPathHelper 进行解码。
	 *
	 * @see #getRequestUri
	 * @see org.springframework.web.util.UrlPathHelper#getOriginatingQueryString
	 * @see #getUrlPathHelper
	 */
	public String getQueryString() {
		return this.urlPathHelper.getOriginatingQueryString(this.request);
	}

	/**
	 * 检索给定代码的消息，使用 "defaultHtmlEscape" 设置。
	 *
	 * @param code           消息的代码
	 * @param defaultMessage 如果查找失败，则返回的字符串
	 * @return 消息
	 */
	public String getMessage(String code, String defaultMessage) {
		return getMessage(code, null, defaultMessage, isDefaultHtmlEscape());
	}

	/**
	 * 检索给定代码的消息，使用 "defaultHtmlEscape" 设置。
	 *
	 * @param code           消息的代码
	 * @param args           消息的参数，如果没有则为 {@code null}
	 * @param defaultMessage 如果查找失败，则返回的字符串
	 * @return 消息
	 */
	public String getMessage(String code, @Nullable Object[] args, String defaultMessage) {
		return getMessage(code, args, defaultMessage, isDefaultHtmlEscape());
	}

	/**
	 * 检索给定代码的消息，使用 "defaultHtmlEscape" 设置。
	 *
	 * @param code           消息的代码
	 * @param args           消息的参数列表，如果没有则为 {@code null}
	 * @param defaultMessage 如果查找失败，则返回的字符串
	 * @return 消息
	 */
	public String getMessage(String code, @Nullable List<?> args, String defaultMessage) {
		return getMessage(code, (args != null ? args.toArray() : null), defaultMessage, isDefaultHtmlEscape());
	}

	/**
	 * 检索给定代码的消息。
	 *
	 * @param code           消息的代码
	 * @param args           消息的参数，如果没有则为 {@code null}
	 * @param defaultMessage 如果查找失败，则返回的字符串
	 * @param htmlEscape     如果消息应该进行 HTML 转义
	 * @return 消息
	 */
	public String getMessage(String code, @Nullable Object[] args, String defaultMessage, boolean htmlEscape) {
		// 进行国际化转换
		String msg = this.webApplicationContext.getMessage(code, args, defaultMessage, getLocale());
		if (msg == null) {
			// 如果转换后的消息为空，则返回空字符串
			return "";
		}
		// 如果需要HTML转义，则进行转义
		return (htmlEscape ? HtmlUtils.htmlEscape(msg) : msg);
	}

	/**
	 * 检索给定代码的消息，使用 "defaultHtmlEscape" 设置。
	 *
	 * @param code 消息的代码
	 * @return 消息
	 * @throws org.springframework.context.NoSuchMessageException 如果未找到
	 */
	public String getMessage(String code) throws NoSuchMessageException {
		return getMessage(code, null, isDefaultHtmlEscape());
	}

	/**
	 * 检索给定代码的消息，使用 "defaultHtmlEscape" 设置。
	 *
	 * @param code 消息的代码
	 * @param args 消息的参数，如果没有则为 {@code null}
	 * @return 消息
	 * @throws org.springframework.context.NoSuchMessageException 如果未找到
	 */
	public String getMessage(String code, @Nullable Object[] args) throws NoSuchMessageException {
		return getMessage(code, args, isDefaultHtmlEscape());
	}

	/**
	 * 检索给定代码的消息，使用 "defaultHtmlEscape" 设置。
	 *
	 * @param code 消息的代码
	 * @param args 消息的参数列表，如果没有则为 {@code null}
	 * @return 消息
	 * @throws org.springframework.context.NoSuchMessageException 如果未找到
	 */
	public String getMessage(String code, @Nullable List<?> args) throws NoSuchMessageException {
		return getMessage(code, (args != null ? args.toArray() : null), isDefaultHtmlEscape());
	}

	/**
	 * 检索给定代码的消息。
	 *
	 * @param code       消息的代码
	 * @param args       消息的参数，如果没有则为 {@code null}
	 * @param htmlEscape 如果消息应该进行 HTML 转义
	 * @return 消息
	 * @throws org.springframework.context.NoSuchMessageException 如果未找到
	 */
	public String getMessage(String code, @Nullable Object[] args, boolean htmlEscape) throws NoSuchMessageException {
		// 进行国际化转换
		String msg = this.webApplicationContext.getMessage(code, args, getLocale());
		// 如果需要HTML转义，则进行转义
		return (htmlEscape ? HtmlUtils.htmlEscape(msg) : msg);
	}

	/**
	 * 检索给定的 MessageSourceResolvable（例如 ObjectError 实例），使用 "defaultHtmlEscape" 设置。
	 *
	 * @param resolvable MessageSourceResolvable
	 * @return 消息
	 * @throws org.springframework.context.NoSuchMessageException 如果未找到
	 */
	public String getMessage(MessageSourceResolvable resolvable) throws NoSuchMessageException {
		return getMessage(resolvable, isDefaultHtmlEscape());
	}

	/**
	 * 检索给定的 MessageSourceResolvable（例如 ObjectError 实例）。
	 *
	 * @param resolvable MessageSourceResolvable
	 * @param htmlEscape 如果消息应该进行 HTML 转义
	 * @return 消息
	 * @throws org.springframework.context.NoSuchMessageException 如果未找到
	 */
	public String getMessage(MessageSourceResolvable resolvable, boolean htmlEscape) throws NoSuchMessageException {
		// 进行国际化转换
		String msg = this.webApplicationContext.getMessage(resolvable, getLocale());
		// 如果需要HTML转义，则进行转义
		return (htmlEscape ? HtmlUtils.htmlEscape(msg) : msg);
	}

	/**
	 * 检索给定代码的主题消息。
	 * <p>请注意，主题消息永远不会进行 HTML 转义，因为它们通常表示主题特定的资源路径，而不是客户端可见的消息。
	 *
	 * @param code           消息的代码
	 * @param defaultMessage 如果查找失败，则返回的字符串
	 * @return 消息
	 */
	public String getThemeMessage(String code, String defaultMessage) {
		// 进行国际化转换
		String msg = getTheme().getMessageSource().getMessage(code, null, defaultMessage, getLocale());
		// 如果转换后的消息为空，则返回空字符串
		return (msg != null ? msg : "");
	}

	/**
	 * 检索给定代码的主题消息。
	 * <p>请注意，主题消息永远不会进行 HTML 转义，因为它们通常表示主题特定的资源路径，而不是客户端可见的消息。
	 *
	 * @param code           消息的代码
	 * @param args           消息的参数，如果没有则为 {@code null}
	 * @param defaultMessage 如果查找失败，则返回的字符串
	 * @return 消息
	 */
	public String getThemeMessage(String code, @Nullable Object[] args, String defaultMessage) {
		// 进行国际化转换
		String msg = getTheme().getMessageSource().getMessage(code, args, defaultMessage, getLocale());
		// 如果转换后的消息为空，则返回空字符串
		return (msg != null ? msg : "");
	}

	/**
	 * 检索给定代码的主题消息。
	 * <p>请注意，主题消息永远不会进行 HTML 转义，因为它们通常表示主题特定的资源路径，而不是客户端可见的消息。
	 *
	 * @param code           消息的代码
	 * @param args           消息的参数列表，如果没有则为 {@code null}
	 * @param defaultMessage 如果查找失败，则返回的字符串
	 * @return 消息
	 */
	public String getThemeMessage(String code, @Nullable List<?> args, String defaultMessage) {
		// 进行国际化转换
		String msg = getTheme().getMessageSource().getMessage(code, (args != null ? args.toArray() : null),
				defaultMessage, getLocale());
		// 如果转换后的消息为空，则返回空字符串
		return (msg != null ? msg : "");
	}

	/**
	 * 检索给定代码的主题消息。
	 * <p>请注意，主题消息永远不会进行 HTML 转义，因为它们通常表示主题特定的资源路径，而不是客户端可见的消息。
	 *
	 * @param code 消息的代码
	 * @return 消息
	 * @throws org.springframework.context.NoSuchMessageException 如果未找到
	 */
	public String getThemeMessage(String code) throws NoSuchMessageException {
		return getTheme().getMessageSource().getMessage(code, null, getLocale());
	}

	/**
	 * 检索给定代码的主题消息。
	 * <p>请注意，主题消息永远不会进行 HTML 转义，因为它们通常表示主题特定的资源路径，而不是客户端可见的消息。
	 *
	 * @param code 消息的代码
	 * @param args 消息的参数，如果没有则为 {@code null}
	 * @return 消息
	 * @throws org.springframework.context.NoSuchMessageException 如果未找到
	 */
	public String getThemeMessage(String code, @Nullable Object[] args) throws NoSuchMessageException {
		return getTheme().getMessageSource().getMessage(code, args, getLocale());
	}

	/**
	 * 检索给定代码的主题消息。
	 * <p>请注意，主题消息永远不会进行 HTML 转义，因为它们通常表示主题特定的资源路径，而不是客户端可见的消息。
	 *
	 * @param code 消息的代码
	 * @param args 消息的参数列表，如果没有则为 {@code null}
	 * @return 消息
	 * @throws org.springframework.context.NoSuchMessageException 如果未找到
	 */
	public String getThemeMessage(String code, @Nullable List<?> args) throws NoSuchMessageException {
		return getTheme().getMessageSource().getMessage(code, (args != null ? args.toArray() : null), getLocale());
	}

	/**
	 * 在当前主题中检索给定的 MessageSourceResolvable。
	 * <p>请注意，主题消息永远不会进行 HTML 转义，因为它们通常表示主题特定的资源路径，而不是客户端可见的消息。
	 *
	 * @param resolvable MessageSourceResolvable
	 * @return 消息
	 * @throws org.springframework.context.NoSuchMessageException 如果未找到
	 */
	public String getThemeMessage(MessageSourceResolvable resolvable) throws NoSuchMessageException {
		return getTheme().getMessageSource().getMessage(resolvable, getLocale());
	}

	/**
	 * 使用 "defaultHtmlEscape" 设置检索给定绑定对象的 Errors 实例。
	 *
	 * @param name 绑定对象的名称
	 * @return Errors 实例，如果未找到则为 {@code null}
	 */
	@Nullable
	public Errors getErrors(String name) {
		return getErrors(name, isDefaultHtmlEscape());
	}

	/**
	 * 检索给定绑定对象的 Errors 实例。
	 *
	 * @param name       绑定对象的名称
	 * @param htmlEscape 创建一个是否进行自动 HTML 转义的 Errors 实例？
	 * @return Errors 实例，如果未找到则为 {@code null}
	 */
	@Nullable
	public Errors getErrors(String name, boolean htmlEscape) {
		// 如果errorsMap为null，则初始化为一个HashMap
		if (this.errorsMap == null) {
			this.errorsMap = new HashMap<>();
		}

		// 从errorsMap中获取指定名称的Errors对象
		Errors errors = this.errorsMap.get(name);

		// 是否需要将Errors对象放入errorsMap中的标志
		boolean put = false;

		if (errors == null) {
			// 如果errors为null，则尝试从模型中获取Errors对象
			errors = (Errors) getModelObject(BindingResult.MODEL_KEY_PREFIX + name);
			// 为了向后兼容性，检查旧的BindException前缀。
			if (errors instanceof BindException) {
				// 如果错误是绑定异常，则将错误设置为绑定结果
				errors = ((BindException) errors).getBindingResult();
			}
			// 如果仍然获取到的Errors对象为null，则返回null
			if (errors == null) {
				return null;
			}
			put = true;
		}

		// 如果需要进行HTML转义，并且Errors对象不是EscapedErrors的实例
		if (htmlEscape && !(errors instanceof EscapedErrors)) {
			// 则创建一个EscapedErrors对象进行转义
			errors = new EscapedErrors(errors);
			put = true;
		}
		// 如果不需要进行HTML转义，并且Errors对象是EscapedErrors的实例
		else if (!htmlEscape && errors instanceof EscapedErrors) {
			// 则获取其原始Errors对象
			errors = ((EscapedErrors) errors).getSource();
			put = true;
		}

		// 如果需要将Errors对象放入errorsMap中，则进行放入操作
		if (put) {
			this.errorsMap.put(name, errors);
		}

		// 返回获取到的Errors对象
		return errors;
	}

	/**
	 * 检索给定模型名称的模型对象，可以从模型或请求属性中检索。
	 *
	 * @param modelName 模型对象的名称
	 * @return 模型对象
	 */
	@Nullable
	protected Object getModelObject(String modelName) {
		if (this.model != null) {
			// 模型不为空，直接从模型中获取
			return this.model.get(modelName);
		} else {
			// 否则，尝试从请求属性中获取
			return this.request.getAttribute(modelName);
		}
	}

	/**
	 * 使用 "defaultHtmlEscape" 设置为给定绑定对象创建 BindStatus。
	 *
	 * @param path 绑定对象的 bean 和属性路径，用于解析值和错误（例如 "person.age"）
	 * @return 新的 BindStatus 实例
	 * @throws IllegalStateException 如果找不到对应的 Errors 对象
	 */
	public BindStatus getBindStatus(String path) throws IllegalStateException {
		return new BindStatus(this, path, isDefaultHtmlEscape());
	}

	/**
	 * 使用 "defaultHtmlEscape" 设置为给定绑定对象创建 BindStatus。
	 *
	 * @param path       绑定对象的 bean 和属性路径，用于解析值和错误（例如 "person.age"）
	 * @param htmlEscape 创建一个是否进行自动 HTML 转义的 BindStatus？
	 * @return 新的 BindStatus 实例
	 * @throws IllegalStateException 如果找不到对应的 Errors 对象
	 */
	public BindStatus getBindStatus(String path, boolean htmlEscape) throws IllegalStateException {
		return new BindStatus(this, path, htmlEscape);
	}


	/**
	 * 内部类，隔离了 JSTL 的依赖。
	 * 如果 JSTL API 存在，只需调用此类来解析后备区域设置。
	 */
	private static class JstlLocaleResolver {

		@Nullable
		public static Locale getJstlLocale(HttpServletRequest request, @Nullable ServletContext servletContext) {
			// 从请求属性中获取Locale对象
			Object localeObject = Config.get(request, Config.FMT_LOCALE);

			// 如果获取到的Locale对象为null，则尝试从Session中获取
			if (localeObject == null) {
				// 获取Http会话
				HttpSession session = request.getSession(false);
				if (session != null) {
					// 根据 JSTL配置和Http会话 获取本地化对象
					localeObject = Config.get(session, Config.FMT_LOCALE);
				}
				// 如果Session中也未获取到Locale对象，并且ServletContext不为null，则尝试从ServletContext中获取
				if (localeObject == null && servletContext != null) {
					// 如果没能获取到本地化对象，并且存在Servlet上下文，则根据 JSTL配置和Servlet上下文 获取本地化对象
					localeObject = Config.get(servletContext, Config.FMT_LOCALE);
				}
			}

			// 如果获取到的对象是Locale类型，则直接返回；否则返回null
			return (localeObject instanceof Locale ? (Locale) localeObject : null);
		}

		@Nullable
		public static TimeZone getJstlTimeZone(HttpServletRequest request, @Nullable ServletContext servletContext) {
			// 根据 JSTL配置和Http请求 获取时区对象
			Object timeZoneObject = Config.get(request, Config.FMT_TIME_ZONE);
			if (timeZoneObject == null) {
				// 时区对象为空，尝试从Session中获取
				HttpSession session = request.getSession(false);
				if (session != null) {
					// 如果存在会话，根据 JSTL配置和会话 获取时区对象
					timeZoneObject = Config.get(session, Config.FMT_TIME_ZONE);
				}
				if (timeZoneObject == null && servletContext != null) {
					// 如果时区对象仍为空，并且存在Servlet上下文
					// 根据 JSTL配置和Servlet上下文 获取时区对象
					timeZoneObject = Config.get(servletContext, Config.FMT_TIME_ZONE);
				}
			}
			// 如果时区对象是 TimeZone 类型，则直接返回；否则返回null。
			return (timeZoneObject instanceof TimeZone ? (TimeZone) timeZoneObject : null);
		}
	}

}
