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

package org.springframework.web.util;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.util.*;

import javax.servlet.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;
import java.util.*;

/**
 * 用于 Web 应用程序的各种实用程序。
 * 被各种框架类使用。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 */
public abstract class WebUtils {

	/**
	 * 用于包含请求 URI 的标准 Servlet 2.3+ 规范请求属性。
	 * <p>如果通过 {@code RequestDispatcher} 包含，当前资源将看到原始请求。
	 * 它自己的请求 URI 被公开为请求属性。
	 */
	public static final String INCLUDE_REQUEST_URI_ATTRIBUTE = "javax.servlet.include.request_uri";

	/**
	 * 用于包含上下文路径的标准 Servlet 2.3+ 规范请求属性。
	 * <p>如果通过 {@code RequestDispatcher} 包含，当前资源将看到原始上下文路径。
	 * 它自己的上下文路径被公开为请求属性。
	 */
	public static final String INCLUDE_CONTEXT_PATH_ATTRIBUTE = "javax.servlet.include.context_path";

	/**
	 * 用于包含 Servlet 路径的标准 Servlet 2.3+ 规范请求属性。
	 * <p>如果通过 {@code RequestDispatcher} 包含，当前资源将看到原始 Servlet 路径。
	 * 它自己的 Servlet 路径被公开为请求属性。
	 */
	public static final String INCLUDE_SERVLET_PATH_ATTRIBUTE = "javax.servlet.include.servlet_path";

	/**
	 * 用于包含路径信息的标准 Servlet 2.3+ 规范请求属性。
	 * <p>如果通过 {@code RequestDispatcher} 包含，当前资源将看到原始路径信息。
	 * 它自己的路径信息被公开为请求属性。
	 */
	public static final String INCLUDE_PATH_INFO_ATTRIBUTE = "javax.servlet.include.path_info";

	/**
	 * 用于包含查询字符串的标准 Servlet 2.3+ 规范请求属性。
	 * <p>如果通过 {@code RequestDispatcher} 包含，当前资源将看到原始查询字符串。
	 * 它自己的查询字符串被公开为请求属性。
	 */
	public static final String INCLUDE_QUERY_STRING_ATTRIBUTE = "javax.servlet.include.query_string";

	/**
	 * 用于转发请求 URI 的标准 Servlet 2.4+ 规范请求属性。
	 * <p>如果通过 RequestDispatcher 转发，当前资源将看到自己的请求 URI。
	 * 原始请求 URI 被公开为请求属性。
	 */
	public static final String FORWARD_REQUEST_URI_ATTRIBUTE = "javax.servlet.forward.request_uri";

	/**
	 * 标准的 Servlet 2.4+ 规范请求属性，用于转发上下文路径。
	 * <p>如果通过 RequestDispatcher 转发，当前资源将看到自己的上下文路径。
	 * 原始上下文路径被公开为请求属性。
	 */
	public static final String FORWARD_CONTEXT_PATH_ATTRIBUTE = "javax.servlet.forward.context_path";

	/**
	 * 标准的 Servlet 2.4+ 规范请求属性，用于转发 Servlet 路径。
	 * <p>如果通过 RequestDispatcher 转发，当前资源将看到自己的 Servlet 路径。
	 * 原始 Servlet 路径被公开为请求属性。
	 */
	public static final String FORWARD_SERVLET_PATH_ATTRIBUTE = "javax.servlet.forward.servlet_path";

	/**
	 * 标准的 Servlet 2.4+ 规范请求属性，用于转发路径信息。
	 * <p>如果通过 RequestDispatcher 转发，当前资源将看到自己的路径信息。
	 * 原始路径信息被公开为请求属性。
	 */
	public static final String FORWARD_PATH_INFO_ATTRIBUTE = "javax.servlet.forward.path_info";

	/**
	 * 标准的 Servlet 2.4+ 规范请求属性，用于转发查询字符串。
	 * <p>如果通过 RequestDispatcher 转发，当前资源将看到自己的查询字符串。
	 * 原始查询字符串被公开为请求属性。
	 */
	public static final String FORWARD_QUERY_STRING_ATTRIBUTE = "javax.servlet.forward.query_string";

	/**
	 * 标准的 Servlet 2.3+ 规范请求属性，用于错误页面状态码。
	 * <p>要暴露给标记为错误页面的 JSP 页面，当直接转发到它们时，而不是通过 Servlet 容器的错误页面解析机制。
	 */
	public static final String ERROR_STATUS_CODE_ATTRIBUTE = "javax.servlet.error.status_code";

	/**
	 * 标准的 Servlet 2.3+ 规范请求属性，用于错误页面异常类型。
	 * <p>要暴露给标记为错误页面的 JSP 页面，当直接转发到它们时，而不是通过 Servlet 容器的错误页面解析机制。
	 */
	public static final String ERROR_EXCEPTION_TYPE_ATTRIBUTE = "javax.servlet.error.exception_type";

	/**
	 * 标准的 Servlet 2.3+ 规范请求属性，用于错误页面消息。
	 * <p>要暴露给标记为错误页面的 JSP 页面，当直接转发到它们时，而不是通过 Servlet 容器的错误页面解析机制。
	 */
	public static final String ERROR_MESSAGE_ATTRIBUTE = "javax.servlet.error.message";

	/**
	 * 标准的 Servlet 2.3+ 规范请求属性，用于错误页面异常。
	 * <p>要暴露给标记为错误页面的 JSP 页面，当直接转发到它们时，而不是通过 Servlet 容器的错误页面解析机制。
	 */
	public static final String ERROR_EXCEPTION_ATTRIBUTE = "javax.servlet.error.exception";

	/**
	 * 标准的 Servlet 2.3+ 规范请求属性，用于错误页面请求 URI。
	 * <p>要暴露给标记为错误页面的 JSP 页面，当直接转发到它们时，而不是通过 Servlet 容器的错误页面解析机制。
	 */
	public static final String ERROR_REQUEST_URI_ATTRIBUTE = "javax.servlet.error.request_uri";

	/**
	 * 标准的 Servlet 2.3+ 规范请求属性，用于错误页面的 Servlet 名称。
	 * <p>要暴露给标记为错误页面的 JSP 页面，当直接转发到它们时，而不是通过 Servlet 容器的错误页面解析机制。
	 */
	public static final String ERROR_SERVLET_NAME_ATTRIBUTE = "javax.servlet.error.servlet_name";

	/**
	 * 内容类型字符串中字符集子句的前缀：";charset="。
	 */
	public static final String CONTENT_TYPE_CHARSET_PREFIX = ";charset=";

	/**
	 * Servlet 规范指定的默认字符编码，当 {@code request.getCharacterEncoding} 返回 {@code null} 时使用。
	 *
	 * @see ServletRequest#getCharacterEncoding
	 */
	public static final String DEFAULT_CHARACTER_ENCODING = "ISO-8859-1";

	/**
	 * Servlet 规范的标准上下文属性，指定当前 Web 应用程序的临时目录，类型为 {@code java.io.File}。
	 */
	public static final String TEMP_DIR_CONTEXT_ATTRIBUTE = "javax.servlet.context.tempdir";

	/**
	 * 在 Servlet 上下文级别（即 {@code web.xml} 中的上下文参数）的 HTML 转义参数："defaultHtmlEscape"。
	 */
	public static final String HTML_ESCAPE_CONTEXT_PARAM = "defaultHtmlEscape";

	/**
	 * 在 Servlet 上下文中，用于响应编码的 HTML 转义参数（即 {@code web.xml} 中的上下文参数）："responseEncodedHtmlEscape"。
	 *
	 * @since 4.1.2
	 */
	public static final String RESPONSE_ENCODED_HTML_ESCAPE_CONTEXT_PARAM = "responseEncodedHtmlEscape";

	/**
	 * 在 Servlet 上下文级别的 Web 应用程序根键参数（即 {@code web.xml} 中的上下文参数）："webAppRootKey"。
	 */
	public static final String WEB_APP_ROOT_KEY_PARAM = "webAppRootKey";

	/**
	 * 默认的 Web 应用程序根键："webapp.root"。
	 */
	public static final String DEFAULT_WEB_APP_ROOT_KEY = "webapp.root";

	/**
	 * 图像按钮的名称后缀。
	 */
	public static final String[] SUBMIT_IMAGE_SUFFIXES = {".x", ".y"};

	/**
	 * 互斥会话属性的键。
	 */
	public static final String SESSION_MUTEX_ATTRIBUTE = WebUtils.class.getName() + ".MUTEX";


	/**
	 * 将系统属性设置为 Web 应用程序根目录。系统属性的键可以在 {@code web.xml} 中使用 "webAppRootKey" 上下文参数定义。
	 * 默认值为 "webapp.root"。
	 * <p>可用于支持 {@code System.getProperty} 值的工具，如在日志文件位置中的 log4j 的 "${key}" 语法中的替换。
	 *
	 * @param servletContext Web 应用程序的 Servlet 上下文
	 * @throws IllegalStateException 如果系统属性已设置，或者如果 WAR 文件未展开
	 * @see #WEB_APP_ROOT_KEY_PARAM
	 * @see #DEFAULT_WEB_APP_ROOT_KEY
	 * @see WebAppRootListener
	 */
	public static void setWebAppRootSystemProperty(ServletContext servletContext) throws IllegalStateException {
		// 断言 Servlet上下文 不为空
		Assert.notNull(servletContext, "ServletContext must not be null");

		// 获取 Servlet上下文 的真实路径
		String root = servletContext.getRealPath("/");
		if (root == null) {
			// 如果根路径为null，抛出异常
			throw new IllegalStateException(
					"Cannot set web app root system property when WAR file is not expanded");
		}

		// 获取 Web 应用程序根键参数
		String param = servletContext.getInitParameter(WEB_APP_ROOT_KEY_PARAM);
		// 如果参数不为null，则使用参数作为key，否则使用 默认的 Web 应用程序根键
		String key = (param != null ? param : DEFAULT_WEB_APP_ROOT_KEY);

		// 获取旧的系统属性值
		String oldValue = System.getProperty(key);
		if (oldValue != null && !StringUtils.pathEquals(oldValue, root)) {
			// 如果旧值不为null，且与当前根路径不同，则抛出异常
			throw new IllegalStateException("Web app root system property already set to different value: '" +
					key + "' = [" + oldValue + "] instead of [" + root + "] - " +
					"Choose unique values for the 'webAppRootKey' context-param in your web.xml files!");
		}

		// 设置系统属性值
		System.setProperty(key, root);
		// 记录日志
		servletContext.log("Set web app root system property: '" + key + "' = [" + root + "]");
	}

	/**
	 * 删除指向 Web 应用程序根目录的系统属性。在关闭 Web 应用程序时调用。
	 *
	 * @param servletContext Web 应用程序的 Servlet 上下文
	 * @see #setWebAppRootSystemProperty
	 */
	public static void removeWebAppRootSystemProperty(ServletContext servletContext) {
		// 断言ServletContext不为空
		Assert.notNull(servletContext, "ServletContext must not be null");

		// 获取 Web 应用程序根键参数
		String param = servletContext.getInitParameter(WEB_APP_ROOT_KEY_PARAM);
		// 如果参数不为null，则使用参数作为key，否则使用 默认的 Web 应用程序根键
		String key = (param != null ? param : DEFAULT_WEB_APP_ROOT_KEY);
		// 移除系统属性值
		System.getProperties().remove(key);
	}

	/**
	 * 返回是否为 Web 应用程序启用了默认的 HTML 转义，即 {@code web.xml} 中的 "defaultHtmlEscape" 上下文参数的值（如果有）。
	 * <p>此方法区分没有指定任何参数和指定了实际布尔值之间的区别，允许在全局级别没有设置时具有上下文特定的默认值。
	 *
	 * @param servletContext Web 应用程序的 Servlet 上下文
	 * @return 给定应用程序是否启用了默认的 HTML 转义（{@code null} = 没有显式默认值）
	 */
	@Nullable
	public static Boolean getDefaultHtmlEscape(@Nullable ServletContext servletContext) {
		if (servletContext == null) {
			// 如果不存在 Servlet上下文 ，则返回null
			return null;
		}
		// 获取 Servlet上下文中的HTML转义参数的值
		String param = servletContext.getInitParameter(HTML_ESCAPE_CONTEXT_PARAM);
		// 如果该参数不为空，且有文本，则返回对应的布尔值；否则返回null
		return (StringUtils.hasText(param) ? Boolean.valueOf(param) : null);
	}

	/**
	 * 返回是否应在 HTML 转义字符时使用响应编码，因此仅使用 UTF-* 编码转义 XML 标记重要字符。
	 * 该选项为带有 ServletContext 参数的 Web 应用程序启用，
	 * 即 {@code web.xml} 中的 "responseEncodedHtmlEscape" 上下文参数的值（如果有）。
	 * <p>此方法区分没有指定任何参数和指定了实际布尔值之间的区别，允许在全局级别没有设置时具有上下文特定的默认值。
	 *
	 * @param servletContext Web 应用程序的 Servlet 上下文
	 * @return 是否应使用响应编码进行 HTML 转义（{@code null} = 没有显式默认值）
	 * @since 4.1.2
	 */
	@Nullable
	public static Boolean getResponseEncodedHtmlEscape(@Nullable ServletContext servletContext) {
		if (servletContext == null) {
			// 如果不存在 Servlet上下文 ，则返回null
			return null;
		}
		// 获取在 Servlet 上下文中，用于响应编码的 HTML 转义参数的值
		String param = servletContext.getInitParameter(RESPONSE_ENCODED_HTML_ESCAPE_CONTEXT_PARAM);
		// 如果该参数不为空，且有文本，则返回对应的布尔值；否则返回null
		return (StringUtils.hasText(param) ? Boolean.valueOf(param) : null);
	}

	/**
	 * 返回当前 Web 应用程序的临时目录，由 Servlet 容器提供。
	 *
	 * @param servletContext Web 应用程序的 Servlet 上下文
	 * @return 表示临时目录的 File
	 */
	public static File getTempDir(ServletContext servletContext) {
		Assert.notNull(servletContext, "ServletContext must not be null");
		return (File) servletContext.getAttribute(TEMP_DIR_CONTEXT_ATTRIBUTE);
	}

	/**
	 * 返回 Web 应用程序中给定路径的实际路径，由 Servlet 容器提供。
	 * <p>如果路径尚未以斜杠开头，则添加斜杠，并且如果路径无法解析为资源，则抛出 FileNotFoundException
	 * （与 ServletContext 的 {@code getRealPath} 相反，后者返回 null）。
	 *
	 * @param servletContext Web 应用程序的 Servlet 上下文
	 * @param path           Web 应用程序中的路径
	 * @return 对应的实际路径
	 * @throws FileNotFoundException 如果路径无法解析为资源
	 * @see javax.servlet.ServletContext#getRealPath
	 */
	public static String getRealPath(ServletContext servletContext, String path) throws FileNotFoundException {
		Assert.notNull(servletContext, "ServletContext must not be null");
		// 如果 路径 不是以"/"开头，则添加"/"前缀
		if (!path.startsWith("/")) {
			path = "/" + path;
		}
		// 获取路径在服务器文件系统中的真实路径
		String realPath = servletContext.getRealPath(path);
		if (realPath == null) {
			// 如果不存在真实路径，则抛出文件未找到异常
			throw new FileNotFoundException(
					"ServletContext resource [" + path + "] cannot be resolved to absolute file path - " +
							"web application archive not expanded?");
		}
		// 返回路径在服务器文件系统中的真实路径
		return realPath;
	}

	/**
	 * 确定给定请求的会话 ID（如果有）。
	 *
	 * @param request 当前 HTTP 请求
	 * @return 会话 ID，如果没有则为 {@code null}
	 */
	@Nullable
	public static String getSessionId(HttpServletRequest request) {
		Assert.notNull(request, "Request must not be null");
		// 获取当前请求的会话，若会话不存在则返回null
		HttpSession session = request.getSession(false);
		// 如果 会话不为null，则返回会话ID；否则返回null
		return (session != null ? session.getId() : null);
	}

	/**
	 * 检查给定请求是否具有给定名称的会话属性。
	 * 如果没有会话或会话没有此类属性，则返回 null。
	 * 如果之前不存在会话，则不创建新会话！
	 *
	 * @param request 当前 HTTP 请求
	 * @param name    会话属性的名称
	 * @return 会话属性的值，如果找不到则为 {@code null}
	 */
	@Nullable
	public static Object getSessionAttribute(HttpServletRequest request, String name) {
		Assert.notNull(request, "Request must not be null");
		// 获取当前请求的会话，若会话不存在则返回null
		HttpSession session = request.getSession(false);
		// 如果 会话不为null，则从会话返回指定名称的会话值；否则返回null
		return (session != null ? session.getAttribute(name) : null);
	}

	/**
	 * 检查给定请求是否具有给定名称的会话属性。
	 * 如果没有会话或会话没有此类属性，则抛出异常。
	 * 如果之前不存在会话，则不创建新会话！
	 *
	 * @param request 当前 HTTP 请求
	 * @param name    会话属性的名称
	 * @return 会话属性的值，如果找不到则为 {@code null}
	 * @throws IllegalStateException 如果找不到会话属性
	 */
	public static Object getRequiredSessionAttribute(HttpServletRequest request, String name)
			throws IllegalStateException {

		// 获取会话属性
		Object attr = getSessionAttribute(request, name);
		if (attr == null) {
			// 如果获取不到 会话属性，抛出 IllegalStateException 异常
			throw new IllegalStateException("No session attribute '" + name + "' found");
		}
		// 返回会话属性
		return attr;
	}

	/**
	 * 将具有给定名称的会话属性设置为给定值。
	 * 如果值为 null，则删除会话属性，如果存在会话。
	 * 如果不需要，不创建新会话！
	 *
	 * @param request 当前 HTTP 请求
	 * @param name    会话属性的名称
	 * @param value   会话属性的值
	 */
	public static void setSessionAttribute(HttpServletRequest request, String name, @Nullable Object value) {
		Assert.notNull(request, "Request must not be null");
		// 如果值不为空，将其设置为会话属性
		if (value != null) {
			// 获取请求对象的会话，并将属性名和属性值设置到会话中
			request.getSession().setAttribute(name, value);
		} else {
			//  如果值为空，获取请求对象的会话，不存在则返回null
			HttpSession session = request.getSession(false);
			if (session != null) {
				// 如果会话存在，从会话中移除指定名称的属性
				session.removeAttribute(name);
			}
		}
	}

	/**
	 * 返回给定会话的最佳可用互斥体：
	 * 即，对于给定会话同步的对象。
	 * <p>如果可用，则返回会话互斥体属性；通常，
	 * 这意味着需要在 {@code web.xml} 中定义 HttpSessionMutexListener。
	 * 如果找不到互斥体属性，则回退到 HttpSession 本身。
	 * <p>会话互斥体在会话的整个生命周期中保证是相同的对象，
	 * 可以通过 {@code SESSION_MUTEX_ATTRIBUTE} 常量定义的键来获取。
	 * 它作为在当前会话上锁定的安全引用。
	 * <p>在许多情况下，HttpSession 引用本身也是一个安全的互斥体，
	 * 因为对于相同的活动逻辑会话，它始终是相同的对象引用。
	 * 但是，这在不同的 servlet 容器之间不能保证；唯一 100% 安全的方式是会话互斥体。
	 *
	 * @param session 要为其找到互斥体的 HttpSession
	 * @return 互斥体对象（永远不会为 {@code null}）
	 * @see #SESSION_MUTEX_ATTRIBUTE
	 * @see HttpSessionMutexListener
	 */
	public static Object getSessionMutex(HttpSession session) {
		Assert.notNull(session, "Session must not be null");
		// 获取会话中的互斥锁
		Object mutex = session.getAttribute(SESSION_MUTEX_ATTRIBUTE);
		// 如果 互斥锁 为空，则将其设置为 会话对象本身
		if (mutex == null) {
			mutex = session;
		}
		// 返回 互斥锁 对象
		return mutex;
	}


	/**
	 * 返回指定类型的适当请求对象（如果可用），
	 * 根据需要取消包装给定请求。
	 *
	 * @param request      要内省的 servlet 请求
	 * @param requiredType 所需请求对象的类型
	 * @return 匹配的请求对象，如果没有该类型的请求对象可用，则为 {@code null}
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	public static <T> T getNativeRequest(ServletRequest request, @Nullable Class<T> requiredType) {
		// 如果 所需请求对象的类型 不为空
		if (requiredType != null) {
			// 如果 所需请求对象的类型 是 ServletRequest 的实例
			if (requiredType.isInstance(request)) {
				// 将 请求对象 强制转换为 T 类型并返回
				return (T) request;
			} else if (request instanceof ServletRequestWrapper) {
				// 如果 请求对象 是 ServletRequestWrapper 的实例
				// 传入原始请求和 所需请求对象的类型，递归调用 getNativeRequest 方法，并返回结果
				return getNativeRequest(((ServletRequestWrapper) request).getRequest(), requiredType);
			}
		}
		// 如果以上条件都不满足，则返回 null
		return null;
	}

	/**
	 * 返回指定类型的适当响应对象（如果可用），
	 * 根据需要取消包装给定响应。
	 *
	 * @param response     要内省的 servlet 响应
	 * @param requiredType 所需响应对象的类型
	 * @return 匹配的响应对象，如果没有该类型的响应对象可用，则为 {@code null}
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	public static <T> T getNativeResponse(ServletResponse response, @Nullable Class<T> requiredType) {
		// 如果 所需请求对象的类型 不为空
		if (requiredType != null) {
			// 如果 所需请求对象的类型 是 ServletResponse 的实例
			if (requiredType.isInstance(response)) {
				// 将 响应对象 强制转换为 T 类型并返回
				return (T) response;
			} else if (response instanceof ServletResponseWrapper) {
				// 如果 响应对象 是 ServletResponseWrapper 的实例
				// 传入原始响应和 所需请求对象的类型，递归调用 getNativeResponse 方法，并返回结果
				return getNativeResponse(((ServletResponseWrapper) response).getResponse(), requiredType);
			}
		}
		// 如果以上条件都不满足，则返回 null
		return null;
	}

	/**
	 * 确定给定请求是否为包含请求，
	 * 即不是来自外部的顶级 HTTP 请求。
	 * <p>检查是否存在 "javax.servlet.include.request_uri"
	 * 请求属性。可以检查仅在包含请求中存在的任何请求属性。
	 *
	 * @param request 当前 servlet 请求
	 * @return 给定请求是否为包含请求
	 */
	public static boolean isIncludeRequest(ServletRequest request) {
		return (request.getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE) != null);
	}

	/**
	 * 将 Servlet 规范中的错误属性作为 {@link javax.servlet.http.HttpServletRequest} 属性公开，
	 * 键在 Servlet 2.3 规范中定义，用于直接呈现错误页面而不是通过 Servlet 容器的错误页面解析：
	 * {@code javax.servlet.error.status_code}、
	 * {@code javax.servlet.error.exception_type}、
	 * {@code javax.servlet.error.message}、
	 * {@code javax.servlet.error.exception}、
	 * {@code javax.servlet.error.request_uri}、
	 * {@code javax.servlet.error.servlet_name}。
	 * <p>不会覆盖已经存在的值，以便尊重在之前明确公开的属性值。
	 * <p>默认情况下公开状态码 200。显式设置 "javax.servlet.error.status_code"
	 * 属性（之前或之后）以公开不同的状态码。
	 *
	 * @param request     当前 servlet 请求
	 * @param ex          遇到的异常
	 * @param servletName 引发异常的 servlet 的名称
	 */
	public static void exposeErrorRequestAttributes(HttpServletRequest request, Throwable ex,
													@Nullable String servletName) {

		// 如果请求中不存在 错误页面状态码 的属性，则将其暴露为 200状态码
		exposeRequestAttributeIfNotPresent(request, ERROR_STATUS_CODE_ATTRIBUTE, HttpServletResponse.SC_OK);
		// 如果请求中不存在 错误页面异常类型 的属性，则将其暴露为异常的类名
		exposeRequestAttributeIfNotPresent(request, ERROR_EXCEPTION_TYPE_ATTRIBUTE, ex.getClass());
		// 如果请求中不存在 错误页面消息 的属性，则将其暴露为异常消息
		exposeRequestAttributeIfNotPresent(request, ERROR_MESSAGE_ATTRIBUTE, ex.getMessage());
		// 如果请求中不存在 错误页面异常 的属性，则将其暴露为异常对象
		exposeRequestAttributeIfNotPresent(request, ERROR_EXCEPTION_ATTRIBUTE, ex);
		// 如果请求中不存在 错误页面请求URI 的属性，则将其暴露为请求的 URI
		exposeRequestAttributeIfNotPresent(request, ERROR_REQUEST_URI_ATTRIBUTE, request.getRequestURI());
		// 如果 servlet名称 不为空
		if (servletName != null) {
			// 如果请求中不存在 错误页面的Servlet名称 的属性，则将其暴露为 servlet 名称
			exposeRequestAttributeIfNotPresent(request, ERROR_SERVLET_NAME_ATTRIBUTE, servletName);
		}
	}

	/**
	 * 如果不存在，公开指定的请求属性。
	 *
	 * @param request 当前 servlet 请求
	 * @param name    属性的名称
	 * @param value   属性的建议值
	 */
	private static void exposeRequestAttributeIfNotPresent(ServletRequest request, String name, Object value) {
		if (request.getAttribute(name) == null) {
			// 如果指定名称的属性值为空，则在请求中设置属性名和属性值
			request.setAttribute(name, value);
		}
	}

	/**
	 * 清除 Servlet 规范中的错误属性作为 {@link javax.servlet.http.HttpServletRequest} 属性，
	 * 键在 Servlet 2.3 规范中定义：
	 * {@code javax.servlet.error.status_code}、
	 * {@code javax.servlet.error.exception_type}、
	 * {@code javax.servlet.error.message}、
	 * {@code javax.servlet.error.exception}、
	 * {@code javax.servlet.error.request_uri}、
	 * {@code javax.servlet.error.servlet_name}。
	 *
	 * @param request 当前 servlet 请求
	 */
	public static void clearErrorRequestAttributes(HttpServletRequest request) {
		// 从请求中移除 错误页面状态码 属性
		request.removeAttribute(ERROR_STATUS_CODE_ATTRIBUTE);
		// 从请求中移除 错误页面异常类型 属性
		request.removeAttribute(ERROR_EXCEPTION_TYPE_ATTRIBUTE);
		// 从请求中移除 错误页面消息 属性
		request.removeAttribute(ERROR_MESSAGE_ATTRIBUTE);
		// 从请求中移除 错误页面异常 属性
		request.removeAttribute(ERROR_EXCEPTION_ATTRIBUTE);
		// 从请求中移除 错误页面请求URI 属性
		request.removeAttribute(ERROR_REQUEST_URI_ATTRIBUTE);
		// 从请求中移除 错误页面的Servlet名称 属性
		request.removeAttribute(ERROR_SERVLET_NAME_ATTRIBUTE);
	}

	/**
	 * 检索具有给定名称的第一个 Cookie。注意，多个 Cookie 可以具有相同的名称但不同的路径或域。
	 *
	 * @param request 当前 servlet 请求
	 * @param name    cookie 名称
	 * @return 具有给定名称的第一个 Cookie，如果找不到则返回 {@code null}
	 */
	@Nullable
	public static Cookie getCookie(HttpServletRequest request, String name) {
		Assert.notNull(request, "Request must not be null");
		// 获取请求中的所有 Cookie
		Cookie[] cookies = request.getCookies();
		// 如果 cookies 不为空
		if (cookies != null) {
			// 遍历 cookies 数组
			for (Cookie cookie : cookies) {
				// 如果当前 cookie 的名称与指定的名称相同，返回该 cookie
				if (name.equals(cookie.getName())) {
					return cookie;
				}
			}
		}
		// 如果没有找到匹配的 cookie，则返回 null
		return null;
	}

	/**
	 * 检查是否在请求中发送了特定的 input type="submit" 参数，
	 * 无论是通过按钮（直接使用名称）还是通过图像（名称 + ".x" 或名称 + ".y"）。
	 *
	 * @param request 当前 HTTP 请求
	 * @param name    参数的名称
	 * @return 参数是否被发送
	 * @see #SUBMIT_IMAGE_SUFFIXES
	 */
	public static boolean hasSubmitParameter(ServletRequest request, String name) {
		Assert.notNull(request, "Request must not be null");
		// 如果请求参数中指定名称的参数不为空，则返回 true
		if (request.getParameter(name) != null) {
			return true;
		}
		// 遍历提交图像后缀数组
		for (String suffix : SUBMIT_IMAGE_SUFFIXES) {
			// 如果请求参数中指定名称加上后缀的参数不为空，则返回 true
			if (request.getParameter(name + suffix) != null) {
				return true;
			}
		}
		// 如果以上条件都不满足，则返回 false
		return false;
	}

	/**
	 * 从给定的请求参数中获取具名参数。
	 * <p>有关查找算法的说明，请参阅 {@link #findParameterValue(java.util.Map, String)}。
	 *
	 * @param request 当前 HTTP 请求
	 * @param name    请求参数的 <i>逻辑</i> 名称
	 * @return 参数的值，如果参数不存在于给定请求中则返回 {@code null}
	 */
	@Nullable
	public static String findParameterValue(ServletRequest request, String name) {
		return findParameterValue(request.getParameterMap(), name);
	}

	/**
	 * 从给定的请求参数中获取具名参数。
	 * <p>此方法将尝试使用以下算法获取参数值：
	 * <ol>
	 * <li>尝试仅使用给定的 <i>逻辑</i> 名称获取参数值。
	 * 这处理的参数的形式为 <tt>logicalName = value</tt>。对于普通参数，
	 * 例如使用隐藏的 HTML 表单字段提交的参数，这将返回请求的值。</li>
	 * <li>尝试从参数名称中获取参数值，其中请求中的参数名称的形式为
	 * <tt>logicalName_value = xyz</tt>，其中 "_" 是配置的分隔符。
	 * 这处理使用 HTML 表单提交按钮提交的参数值。</li>
	 * <li>如果在上一步中获得的值具有 ".x" 或 ".y" 后缀，则删除它。
	 * 这处理了使用 HTML 表单图像按钮提交值的情况。在这种情况下，
	 * 请求中的参数实际上将是 <tt>logicalName_value.x = 123</tt> 的形式。</li>
	 * </ol>
	 *
	 * @param parameters 可用的参数映射
	 * @param name       请求参数的 <i>逻辑</i> 名称
	 * @return 参数的值，如果参数不存在于给定请求中则返回 {@code null}
	 */
	@Nullable
	public static String findParameterValue(Map<String, ?> parameters, String name) {
		// 首先尝试将其作为常规的 name=value 参数获取
		// 获取指定名称的属性值
		Object value = parameters.get(name);
		if (value instanceof String[]) {
			// 如果属性值是字符串数组，返回第一个值
			String[] values = (String[]) value;
			return (values.length > 0 ? values[0] : null);
		} else if (value != null) {
			// 如果属性值不为空，返回属性值
			return value.toString();
		}
		// 如果尚未有值，则尝试将其作为 name_value=xyz 参数获取
		String prefix = name + "_";
		// 遍历参数名称
		for (String paramName : parameters.keySet()) {
			// 如果参数名称以指定前缀开头
			if (paramName.startsWith(prefix)) {
				// 支持图像按钮，这些按钮将参数提交为 name_value.x=123
				for (String suffix : SUBMIT_IMAGE_SUFFIXES) {
					// 并且该参数名称以图像后缀结尾
					if (paramName.endsWith(suffix)) {
						// 则获取除了前缀和后缀外的字符串
						return paramName.substring(prefix.length(), paramName.length() - suffix.length());
					}
				}
				// 否则，返回前缀后的字符串
				return paramName.substring(prefix.length());
			}
		}
		// 我们找不到参数值...
		return null;
	}

	/**
	 * 返回一个包含具有给定前缀的所有参数的映射。
	 * 将单个值映射到 String，将多个值映射到 String 数组。
	 * <p>例如，使用前缀 "spring_"，"spring_param1" 和
	 * "spring_param2" 会得到一个 Map，其中 "param1" 和 "param2" 是键。
	 *
	 * @param request HTTP 请求，在其中查找参数
	 * @param prefix  参数名称的开头
	 *                （如果这是 null 或空字符串，则所有参数都匹配）
	 * @return 包含请求参数的映射 <b>不包含前缀</b>，
	 * 包含 String 或 String 数组作为值
	 * @see javax.servlet.ServletRequest#getParameterNames
	 * @see javax.servlet.ServletRequest#getParameterValues
	 * @see javax.servlet.ServletRequest#getParameterMap
	 */
	public static Map<String, Object> getParametersStartingWith(ServletRequest request, @Nullable String prefix) {
		Assert.notNull(request, "Request must not be null");
		// 获取请求中的所有参数名
		Enumeration<String> paramNames = request.getParameterNames();
		// 创建一个 TreeMap 来存储参数
		Map<String, Object> params = new TreeMap<>();
		// 如果前缀为空，则将其设置为空字符串
		if (prefix == null) {
			prefix = "";
		}
		// 当参数名枚举不为空，且还有元素时循环处理参数
		while (paramNames != null && paramNames.hasMoreElements()) {
			// 获取下一个参数名
			String paramName = paramNames.nextElement();
			// 如果前缀为空或者参数名以前缀开头
			if (prefix.isEmpty() || paramName.startsWith(prefix)) {
				// 去除前缀后的参数名
				String unprefixed = paramName.substring(prefix.length());
				// 获取参数的值数组
				String[] values = request.getParameterValues(paramName);
				// 如果值为空数组或者长度为 0，则不做处理
				if (values == null || values.length == 0) {
					// 不做任何操作，没有找到任何值。
				} else if (values.length > 1) {
					// 如果值数组长度大于 1，则将参数名和值数组存储到参数 Map 中
					params.put(unprefixed, values);
				} else {
					// 如果值数组长度为 1，则将参数名和单个值存储到参数 Map 中
					params.put(unprefixed, values[0]);
				}
			}
		}
		// 返回存储参数的 Map
		return params;
	}

	/**
	 * 解析给定的带矩阵变量的字符串。示例字符串将如下所示 {@code "q1=a;q1=b;q2=a,b,c"}。
	 * 结果映射将包含键 {@code "q1"} 和 {@code "q2"}，值分别为 {@code ["a","b"]} 和
	 * {@code ["a","b","c"]}。
	 *
	 * @param matrixVariables 未解析的矩阵变量字符串
	 * @return 包含矩阵变量名称和值的映射（永不为 {@code null}）
	 * @since 3.2
	 */
	public static MultiValueMap<String, String> parseMatrixVariables(String matrixVariables) {
		// 创建一个 LinkedMultiValueMap 对象来存储结果
		MultiValueMap<String, String> result = new LinkedMultiValueMap<>();
		// 如果 未解析的矩阵变量字符串 为空或者没有文本内容，则直接返回空的结果
		if (!StringUtils.hasText(matrixVariables)) {
			return result;
		}
		// 使用分号将 未解析的矩阵变量字符串 分割成多个键值对
		StringTokenizer pairs = new StringTokenizer(matrixVariables, ";");
		// 遍历每个键值对
		while (pairs.hasMoreTokens()) {
			// 获取下一个键值对
			String pair = pairs.nextToken();
			// 查找键值对中的等号索引
			int index = pair.indexOf('=');
			// 如果存在等号索引
			if (index != -1) {
				// 获取键
				String name = pair.substring(0, index);
				// 如果键为 "jsessionid" 则跳过
				if (name.equalsIgnoreCase("jsessionid")) {
					continue;
				}
				// 获取值的原始字符串
				String rawValue = pair.substring(index + 1);
				// 将逗号分隔的值字符串转换为字符串数组，并添加到结果中
				for (String value : StringUtils.commaDelimitedListToStringArray(rawValue)) {
					result.add(name, value);
				}
			} else {
				// 如果键值对中没有等号，则将整个键作为键，空字符串作为值添加到结果中
				result.add(pair, "");
			}
		}
		// 返回结果
		return result;
	}

	/**
	 * 检查给定的请求来源是否在允许的来源列表中。
	 * 包含 "*" 的列表表示允许所有来源。
	 * 空列表表示只允许同源。
	 *
	 * <p><strong>注意：</strong>从 5.1 开始，此方法忽略了指定了
	 * 客户端发起地址的 {@code "Forwarded"} 和 {@code "X-Forwarded-*"} 头。
	 * 考虑使用 {@code ForwardedHeaderFilter} 来提取和使用这些头，或者丢弃这些头。
	 *
	 * @return 如果请求来源有效则返回 {@code true}，否则返回 {@code false}
	 * @see <a href="https://tools.ietf.org/html/rfc6454">RFC 6454: The Web Origin Concept</a>
	 * @since 4.1.5
	 */
	public static boolean isValidOrigin(HttpRequest request, Collection<String> allowedOrigins) {
		Assert.notNull(request, "Request must not be null");
		Assert.notNull(allowedOrigins, "Allowed origins must not be null");

		// 获取请求的来源头信息
		String origin = request.getHeaders().getOrigin();
		if (origin == null || allowedOrigins.contains("*")) {
			// 如果来源为空，或者允许所有来源，则返回 true
			return true;
		} else if (CollectionUtils.isEmpty(allowedOrigins)) {
			// 如果允许的来源列表为空，则检查是否是同源请求
			return isSameOrigin(request);
		} else {
			// 如果允许的来源列表不为空，则检查请求的来源是否在允许的列表中
			return allowedOrigins.contains(origin);
		}
	}

	/**
	 * 检查请求是否是同源的，基于 {@code Origin}、{@code Host}、{@code Forwarded}、
	 * {@code X-Forwarded-Proto}、{@code X-Forwarded-Host} 和 {@code X-Forwarded-Port} 头。
	 *
	 * <p><strong>注意：</strong>从 5.1 开始，此方法忽略了指定了
	 * 客户端发起地址的 {@code "Forwarded"} 和 {@code "X-Forwarded-*"} 头。
	 * 考虑使用 {@code ForwardedHeaderFilter} 来提取和使用这些头，或者丢弃这些头。
	 *
	 * @return 如果请求是同源的则返回 {@code true}，否则返回 {@code false}
	 * @since 4.2
	 */
	public static boolean isSameOrigin(HttpRequest request) {
		// 获取请求的头部信息
		HttpHeaders headers = request.getHeaders();
		// 获取请求的来源信息
		String origin = headers.getOrigin();
		if (origin == null) {
			// 如果来源为空，则返回 true
			return true;
		}

		String scheme;
		String host;
		int port;
		// 如果请求是 ServletServerHttpRequest 的实例
		if (request instanceof ServletServerHttpRequest) {
			// 以更高效的方式获取方案、主机和端口：仅需要用于来源比较的方案、主机、端口
			HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();
			scheme = servletRequest.getScheme();
			host = servletRequest.getServerName();
			port = servletRequest.getServerPort();
		} else {
			// 否则，从请求的 URI 中获取方案、主机和端口
			URI uri = request.getURI();
			scheme = uri.getScheme();
			host = uri.getHost();
			port = uri.getPort();
		}

		// 从来源头信息构建 UriComponents
		UriComponents originUrl = UriComponentsBuilder.fromOriginHeader(origin).build();
		// 检查方案、主机和端口是否与来源头信息相匹配
		return (ObjectUtils.nullSafeEquals(scheme, originUrl.getScheme()) &&
				ObjectUtils.nullSafeEquals(host, originUrl.getHost()) &&
				getPort(scheme, port) == getPort(originUrl.getScheme(), originUrl.getPort()));
	}

	private static int getPort(@Nullable String scheme, int port) {
		// 如果端口为 -1
		if (port == -1) {
			// 如果方案是 http 或者 ws，则端口为 80
			if ("http".equals(scheme) || "ws".equals(scheme)) {
				port = 80;
				// 如果方案是 https 或者 wss，则端口为 443
			} else if ("https".equals(scheme) || "wss".equals(scheme)) {
				port = 443;
			}
		}
		// 返回端口
		return port;
	}

}
