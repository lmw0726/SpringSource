/*
 * Copyright 2002-2022 the original author or authors.
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

import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.TimeZoneAwareLocaleContext;
import org.springframework.lang.Nullable;
import org.springframework.ui.context.Theme;
import org.springframework.ui.context.ThemeSource;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.servlet.*;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * Utility class for easy access to request-specific state which has been
 * set by the {@link org.springframework.web.servlet.DispatcherServlet}.
 *
 * <p>Supports lookup of current WebApplicationContext, LocaleResolver,
 * Locale, ThemeResolver, Theme, and MultipartResolver.
 *
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @see RequestContext
 * @see org.springframework.web.servlet.DispatcherServlet
 * @since 03.03.2003
 */
public abstract class RequestContextUtils {

	/**
	 * The name of the bean to use to determine if an implementation of
	 * {@link RequestDataValueProcessor} has been configured.
	 *
	 * @since 4.2.1
	 */
	public static final String REQUEST_DATA_VALUE_PROCESSOR_BEAN_NAME = "requestDataValueProcessor";


	/**
	 * Look for the WebApplicationContext associated with the DispatcherServlet
	 * that has initiated request processing, and for the global context if none
	 * was found associated with the current request. The global context will
	 * be found via the ServletContext or via ContextLoader's current context.
	 * <p>NOTE: This variant remains compatible with Servlet 2.5, explicitly
	 * checking a given ServletContext instead of deriving it from the request.
	 *
	 * @param request        current HTTP request
	 * @param servletContext current servlet context
	 * @return the request-specific WebApplicationContext, or the global one
	 * if no request-specific context has been found, or {@code null} if none
	 * @see DispatcherServlet#WEB_APPLICATION_CONTEXT_ATTRIBUTE
	 * @see WebApplicationContextUtils#getWebApplicationContext(ServletContext)
	 * @see ContextLoader#getCurrentWebApplicationContext()
	 * @since 4.2.1
	 */
	@Nullable
	public static WebApplicationContext findWebApplicationContext(
			HttpServletRequest request, @Nullable ServletContext servletContext) {

		WebApplicationContext webApplicationContext = (WebApplicationContext) request.getAttribute(
				DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE);
		if (webApplicationContext == null) {
			if (servletContext != null) {
				webApplicationContext = WebApplicationContextUtils.getWebApplicationContext(servletContext);
			}
			if (webApplicationContext == null) {
				webApplicationContext = ContextLoader.getCurrentWebApplicationContext();
			}
		}
		return webApplicationContext;
	}

	/**
	 * Look for the WebApplicationContext associated with the DispatcherServlet
	 * that has initiated request processing, and for the global context if none
	 * was found associated with the current request. The global context will
	 * be found via the ServletContext or via ContextLoader's current context.
	 * <p>NOTE: This variant requires Servlet 3.0+ and is generally recommended
	 * for forward-looking custom user code.
	 *
	 * @param request current HTTP request
	 * @return the request-specific WebApplicationContext, or the global one
	 * if no request-specific context has been found, or {@code null} if none
	 * @see #findWebApplicationContext(HttpServletRequest, ServletContext)
	 * @see ServletRequest#getServletContext()
	 * @see ContextLoader#getCurrentWebApplicationContext()
	 * @since 4.2.1
	 */
	@Nullable
	public static WebApplicationContext findWebApplicationContext(HttpServletRequest request) {
		return findWebApplicationContext(request, request.getServletContext());
	}

	/**
	 * Return the {@link LocaleResolver} that has been bound to the request by the
	 * {@link DispatcherServlet}.
	 *
	 * @param request current HTTP request
	 * @return the current {@code LocaleResolver}, or {@code null} if not found
	 */
	@Nullable
	public static LocaleResolver getLocaleResolver(HttpServletRequest request) {
		return (LocaleResolver) request.getAttribute(DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE);
	}

	/**
	 * Retrieve the current locale from the given request, using the
	 * {@link LocaleResolver} bound to the request by the {@link DispatcherServlet}
	 * (if available), falling back to the request's locale based on the
	 * {@code Accept-Language} header or the default locale for the server.
	 * <p>This method serves as a straightforward alternative to the standard
	 * Servlet {@link javax.servlet.http.HttpServletRequest#getLocale()} method,
	 * falling back to the latter if no more specific locale has been found.
	 * <p>Consider using {@link org.springframework.context.i18n.LocaleContextHolder#getLocale()}
	 * which will normally be populated with the same Locale.
	 *
	 * @param request current HTTP request
	 * @return the current locale for the given request, either from the
	 * LocaleResolver or from the plain request itself
	 * @see #getLocaleResolver
	 * @see org.springframework.context.i18n.LocaleContextHolder#getLocale()
	 */
	public static Locale getLocale(HttpServletRequest request) {
		LocaleResolver localeResolver = getLocaleResolver(request);
		return (localeResolver != null ? localeResolver.resolveLocale(request) : request.getLocale());
	}

	/**
	 * Retrieve the current time zone from the given request, using the
	 * {@link TimeZoneAwareLocaleContext} in the {@link LocaleResolver} bound to
	 * the request by the {@link DispatcherServlet} (if available).
	 * <p>Note: This method returns {@code null} if no specific time zone can be
	 * resolved for the given request. This is in contrast to {@link #getLocale}
	 * where it is always possible to fall back to the request's locale based on the
	 * {@code Accept-Language} header or the default locale for the server.
	 * <p>Consider using {@link org.springframework.context.i18n.LocaleContextHolder#getTimeZone()}
	 * which will normally be populated with the same {@code TimeZone}: that method only
	 * differs in terms of its fallback to the system time zone if the {@code LocaleResolver}
	 * hasn't provided a specific time zone (instead of this method's {@code null}).
	 *
	 * @param request current HTTP request
	 * @return the current time zone for the given request, either from the
	 * {@code TimeZoneAwareLocaleContext} or {@code null} if none associated
	 * @see #getLocaleResolver
	 * @see org.springframework.context.i18n.LocaleContextHolder#getTimeZone()
	 */
	@Nullable
	public static TimeZone getTimeZone(HttpServletRequest request) {
		LocaleResolver localeResolver = getLocaleResolver(request);
		if (localeResolver instanceof LocaleContextResolver) {
			LocaleContext localeContext = ((LocaleContextResolver) localeResolver).resolveLocaleContext(request);
			if (localeContext instanceof TimeZoneAwareLocaleContext) {
				return ((TimeZoneAwareLocaleContext) localeContext).getTimeZone();
			}
		}
		return null;
	}

	/**
	 * Return the ThemeResolver that has been bound to the request by the
	 * DispatcherServlet.
	 *
	 * @param request current HTTP request
	 * @return the current ThemeResolver, or {@code null} if not found
	 */
	@Nullable
	public static ThemeResolver getThemeResolver(HttpServletRequest request) {
		return (ThemeResolver) request.getAttribute(DispatcherServlet.THEME_RESOLVER_ATTRIBUTE);
	}

	/**
	 * Return the ThemeSource that has been bound to the request by the
	 * DispatcherServlet.
	 *
	 * @param request current HTTP request
	 * @return the current ThemeSource
	 */
	@Nullable
	public static ThemeSource getThemeSource(HttpServletRequest request) {
		return (ThemeSource) request.getAttribute(DispatcherServlet.THEME_SOURCE_ATTRIBUTE);
	}

	/**
	 * Retrieve the current theme from the given request, using the ThemeResolver
	 * and ThemeSource bound to the request by the DispatcherServlet.
	 *
	 * @param request current HTTP request
	 * @return the current theme, or {@code null} if not found
	 * @see #getThemeResolver
	 */
	@Nullable
	public static Theme getTheme(HttpServletRequest request) {
		ThemeResolver themeResolver = getThemeResolver(request);
		ThemeSource themeSource = getThemeSource(request);
		if (themeResolver != null && themeSource != null) {
			String themeName = themeResolver.resolveThemeName(request);
			return themeSource.getTheme(themeName);
		} else {
			return null;
		}
	}

	/**
	 * 从重定向之前的请求中返回只读的“输入”flash属性。
	 *
	 * @param request 当前请求
	 * @return 一个只读Map，如果没有找到则返回null
	 * @see FlashMap
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	public static Map<String, ?> getInputFlashMap(HttpServletRequest request) {
		return (Map<String, ?>) request.getAttribute(DispatcherServlet.INPUT_FLASH_MAP_ATTRIBUTE);
	}

	/**
	 * 返回“输出”FlashMap以保存重定向后请求的属性。
	 *
	 * @param request 当前请求
	 * @return 一个FlashMap实例，在DispatcherServlet处理的请求中永远不会为null
	 */
	public static FlashMap getOutputFlashMap(HttpServletRequest request) {
		return (FlashMap) request.getAttribute(DispatcherServlet.OUTPUT_FLASH_MAP_ATTRIBUTE);
	}

	/**
	 * 返回FlashMapManager实例以保存闪存属性。
	 * <p>从5.0开始，可以使用方便的方法saveOutputFlashMap来保存“输出”FlashMap。
	 *
	 * @param request 当前请求
	 * @return 一个FlashMapManager实例，在DispatcherServlet处理的请求中永远不会为null
	 */
	@Nullable
	public static FlashMapManager getFlashMapManager(HttpServletRequest request) {
		return (FlashMapManager) request.getAttribute(DispatcherServlet.FLASH_MAP_MANAGER_ATTRIBUTE);
	}

	/**
	 * 方便的方法，检索“输出”FlashMap，使用目标URL的路径和查询参数更新它，然后使用FlashMapManager保存它。
	 *
	 * @param location 重定向的目标URL
	 * @param request  当前请求
	 * @param response 当前响应
	 * @since 5.0
	 */
	public static void saveOutputFlashMap(String location, HttpServletRequest request, HttpServletResponse response) {
		// 获取输出闪存映射
		FlashMap flashMap = getOutputFlashMap(request);
		if (CollectionUtils.isEmpty(flashMap)) {
			return;
		}

		// 从给定的位置构建 UriComponents 实例
		UriComponents uriComponents = UriComponentsBuilder.fromUriString(location).build();

		// 设置目标请求路径和目标请求参数到闪存映射中
		flashMap.setTargetRequestPath(uriComponents.getPath());
		flashMap.addTargetRequestParams(uriComponents.getQueryParams());

		// 获取闪存映射管理器
		FlashMapManager manager = getFlashMapManager(request);
		Assert.state(manager != null, "No FlashMapManager. Is this a DispatcherServlet handled request?");
		// 保存输出闪存映射到会话中
		manager.saveOutputFlashMap(flashMap, request, response);
	}

}
