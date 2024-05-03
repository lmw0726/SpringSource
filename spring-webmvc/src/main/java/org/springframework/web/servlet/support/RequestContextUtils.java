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
 * 用于轻松访问由 {@link org.springframework.web.servlet.DispatcherServlet} 设置的请求特定状态的实用程序类。
 *
 * <p>支持查找当前的 WebApplicationContext、LocaleResolver、Locale、ThemeResolver、Theme 和 MultipartResolver。
 *
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @see RequestContext
 * @see org.springframework.web.servlet.DispatcherServlet
 * @since 03.03.2003
 */
public abstract class RequestContextUtils {

	/**
	 * 用于确定是否已配置 {@link RequestDataValueProcessor} 实现的 Bean 的名称。
	 *
	 * @since 4.2.1
	 */
	public static final String REQUEST_DATA_VALUE_PROCESSOR_BEAN_NAME = "requestDataValueProcessor";


	/**
	 * 查找启动请求处理的 DispatcherServlet 关联的 WebApplicationContext，
	 * 如果没有与当前请求关联的 WebApplicationContext，则查找全局上下文。全局上下文将
	 * 通过 ServletContext 或 ContextLoader 的当前上下文找到。
	 * <p>注意: 此变体保持与 Servlet 2.5 的兼容性，明确检查给定的 ServletContext 而不是从请求中派生它。
	 *
	 * @param request        当前 HTTP 请求
	 * @param servletContext 当前 Servlet 上下文
	 * @return 请求特定的 WebApplicationContext，如果未找到请求特定的上下文，则为全局上下文，如果没有，则为 {@code null}
	 * @see DispatcherServlet#WEB_APPLICATION_CONTEXT_ATTRIBUTE
	 * @see WebApplicationContextUtils#getWebApplicationContext(ServletContext)
	 * @see ContextLoader#getCurrentWebApplicationContext()
	 * @since 4.2.1
	 */
	@Nullable
	public static WebApplicationContext findWebApplicationContext(
			HttpServletRequest request, @Nullable ServletContext servletContext) {

		// 从请求属性中获取 Web应用上下文
		WebApplicationContext webApplicationContext = (WebApplicationContext) request.getAttribute(
				DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE);

		// 如果获取不到的 Web应用上下文
		if (webApplicationContext == null) {
			// 如果 Servlet上下文 存在，则尝试从 Servlet上下文 中获取 Web应用上下文
			if (servletContext != null) {
				webApplicationContext = WebApplicationContextUtils.getWebApplicationContext(servletContext);
			}
			// 如果仍然获取不到的 Web应用上下文，则尝试从当前的 ContextLoader 中获取
			if (webApplicationContext == null) {
				webApplicationContext = ContextLoader.getCurrentWebApplicationContext();
			}
		}

		// 返回获取到的WebApplicationContext
		return webApplicationContext;
	}

	/**
	 * 查找启动请求处理的 DispatcherServlet 关联的 WebApplicationContext，
	 * 如果没有与当前请求关联的 WebApplicationContext，则查找全局上下文。全局上下文将通过 ServletContext 或 ContextLoader 的当前上下文找到。
	 * <p>注意: 此变体要求 Servlet 3.0+，通常建议用于前瞻性的自定义用户代码。
	 *
	 * @param request 当前 HTTP 请求
	 * @return 请求特定的 WebApplicationContext，如果未找到请求特定的上下文，则为全局上下文，如果没有，则为 {@code null}
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
	 * 返回已由 {@link DispatcherServlet} 绑定到请求的 {@link LocaleResolver}。
	 *
	 * @param request 当前 HTTP 请求
	 * @return 当前的 {@code LocaleResolver}，如果未找到则为 {@code null}
	 */
	@Nullable
	public static LocaleResolver getLocaleResolver(HttpServletRequest request) {
		return (LocaleResolver) request.getAttribute(DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE);
	}

	/**
	 * 从给定请求中检索当前区域设置，使用由 {@link DispatcherServlet} 绑定到请求的 {@link LocaleResolver}（如果可用），如果未找到则根据 {@code Accept-Language} 头或服务器的默认区域设置回退到请求的区域设置。
	 * <p>此方法是标准 Servlet {@link javax.servlet.http.HttpServletRequest#getLocale()} 方法的简单替代方法，如果未找到更具体的区域设置，则会回退到后者。
	 * <p>考虑使用 {@link org.springframework.context.i18n.LocaleContextHolder#getLocale()}，它通常将填充相同的区域设置。
	 *
	 * @param request 当前 HTTP 请求
	 * @return 给定请求的当前区域设置，从 LocaleResolver 或请求本身获取
	 * @see #getLocaleResolver
	 * @see org.springframework.context.i18n.LocaleContextHolder#getLocale()
	 */
	public static Locale getLocale(HttpServletRequest request) {
		// 获取LocaleResolver
		LocaleResolver localeResolver = getLocaleResolver(request);
		// 如果获取到的LocaleResolver不为null，则使用它来解析Locale；否则从请求中获取Locale
		return (localeResolver != null ? localeResolver.resolveLocale(request) : request.getLocale());
	}

	/**
	 * 从给定请求中检索当前时区，使用由 DispatcherServlet 绑定到请求的 LocaleResolver 中的 TimeZoneAwareLocaleContext。
	 * <p>注意: 如果无法为给定请求解析特定时区，则此方法返回 {@code null}。这与 {@link #getLocale} 相反，在此方法中，始终可以根据请求的“Accept-Language”头或服务器的默认区域设置回退到请求的区域设置。
	 * <p>考虑使用 {@link org.springframework.context.i18n.LocaleContextHolder#getTimeZone()}，通常将填充相同的 {@code TimeZone}：该方法仅在没有提供特定时区的 {@code LocaleResolver} 的情况下与此方法的 {@code null} 不同。
	 *
	 * @param request 当前 HTTP 请求
	 * @return 给定请求的当前时区，从 {@code TimeZoneAwareLocaleContext} 或 {@code null} 中获取
	 * @see #getLocaleResolver
	 * @see org.springframework.context.i18n.LocaleContextHolder#getTimeZone()
	 */
	@Nullable
	public static TimeZone getTimeZone(HttpServletRequest request) {
		// 获取LocaleResolver
		LocaleResolver localeResolver = getLocaleResolver(request);

		// 如果LocaleResolver是LocaleContextResolver的实例
		if (localeResolver instanceof LocaleContextResolver) {
			// 解析LocaleContext
			LocaleContext localeContext = ((LocaleContextResolver) localeResolver).resolveLocaleContext(request);
			// 如果LocaleContext是TimeZoneAwareLocaleContext的实例
			if (localeContext instanceof TimeZoneAwareLocaleContext) {
				// 返回TimeZone
				return ((TimeZoneAwareLocaleContext) localeContext).getTimeZone();
			}
		}

		// 如果无法获取到TimeZone，则返回null
		return null;
	}

	/**
	 * 返回已由 DispatcherServlet 绑定到请求的 ThemeResolver。
	 *
	 * @param request 当前 HTTP 请求
	 * @return 当前的 ThemeResolver，如果未找到，则为 {@code null}
	 */
	@Nullable
	public static ThemeResolver getThemeResolver(HttpServletRequest request) {
		return (ThemeResolver) request.getAttribute(DispatcherServlet.THEME_RESOLVER_ATTRIBUTE);
	}

	/**
	 * 返回已由 DispatcherServlet 绑定到请求的 ThemeSource。
	 *
	 * @param request 当前 HTTP 请求
	 * @return 当前的 ThemeSource
	 */
	@Nullable
	public static ThemeSource getThemeSource(HttpServletRequest request) {
		return (ThemeSource) request.getAttribute(DispatcherServlet.THEME_SOURCE_ATTRIBUTE);
	}

	/**
	 * 从给定请求中检索当前主题，使用由 DispatcherServlet 绑定到请求的 ThemeResolver 和 ThemeSource。
	 *
	 * @param request 当前 HTTP 请求
	 * @return 当前主题，如果未找到则为 {@code null}
	 * @see #getThemeResolver
	 */
	@Nullable
	public static Theme getTheme(HttpServletRequest request) {
		// 获取ThemeResolver和ThemeSource
		ThemeResolver themeResolver = getThemeResolver(request);
		ThemeSource themeSource = getThemeSource(request);

		// 如果ThemeResolver和ThemeSource都不为null
		if (themeResolver != null && themeSource != null) {
			// 解析主题名称
			String themeName = themeResolver.resolveThemeName(request);
			// 返回对应的主题
			return themeSource.getTheme(themeName);
		} else {
			// 如果其中一个为null，则返回null
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
