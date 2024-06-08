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

package org.springframework.web.context.request;

import org.springframework.lang.Nullable;

import java.security.Principal;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

/**
 * 用于 Web 请求的通用接口。
 * 主要用于通用的 Web 请求拦截器，使它们能够访问通用请求元数据，而不是实际处理请求。
 *
 * @author Juergen Hoeller
 * @author Brian Clozel
 * @see WebRequestInterceptor
 * @since 2.0
 */
public interface WebRequest extends RequestAttributes {

	/**
	 * 返回给定名称的请求头，如果没有则返回 {@code null}。
	 * <p>在多值头的情况下检索第一个头值。
	 *
	 * @see javax.servlet.http.HttpServletRequest#getHeader(String)
	 * @since 3.0
	 */
	@Nullable
	String getHeader(String headerName);

	/**
	 * 返回给定头名称的请求头值数组，如果没有则返回 {@code null}。
	 * <p>单个值头将作为仅包含一个元素的数组公开。
	 *
	 * @see javax.servlet.http.HttpServletRequest#getHeaders(String)
	 * @since 3.0
	 */
	@Nullable
	String[] getHeaderValues(String headerName);

	/**
	 * 返回请求头名称的迭代器。
	 *
	 * @see javax.servlet.http.HttpServletRequest#getHeaderNames()
	 * @since 3.0
	 */
	Iterator<String> getHeaderNames();

	/**
	 * 返回给定名称的请求参数，如果没有则返回 {@code null}。
	 * <p>在多值参数的情况下检索第一个参数值。
	 *
	 * @see javax.servlet.http.HttpServletRequest#getParameter(String)
	 */
	@Nullable
	String getParameter(String paramName);

	/**
	 * 返回给定参数名称的请求参数值数组，如果没有则返回 {@code null}。
	 * <p>单个值参数将作为仅包含一个元素的数组公开。
	 *
	 * @see javax.servlet.http.HttpServletRequest#getParameterValues(String)
	 */
	@Nullable
	String[] getParameterValues(String paramName);

	/**
	 * 返回请求参数名称的迭代器。
	 *
	 * @see javax.servlet.http.HttpServletRequest#getParameterNames()
	 * @since 3.0
	 */
	Iterator<String> getParameterNames();

	/**
	 * 返回请求参数的不可变 Map，其中参数名称为映射键，参数值为映射值。映射值将为 String 数组类型。
	 * <p>单个值参数将作为仅包含一个元素的数组公开。
	 *
	 * @see javax.servlet.http.HttpServletRequest#getParameterMap()
	 */
	Map<String, String[]> getParameterMap();

	/**
	 * 返回此请求的主要 Locale。
	 *
	 * @see javax.servlet.http.HttpServletRequest#getLocale()
	 */
	Locale getLocale();

	/**
	 * 返回此请求的上下文路径（通常是当前 Web 应用程序映射到的基本路径）。
	 *
	 * @see javax.servlet.http.HttpServletRequest#getContextPath()
	 */
	String getContextPath();

	/**
	 * 返回此请求的远程用户，如果有的话。
	 *
	 * @see javax.servlet.http.HttpServletRequest#getRemoteUser()
	 */
	@Nullable
	String getRemoteUser();

	/**
	 * 返回此请求的用户主体，如果有的话。
	 *
	 * @see javax.servlet.http.HttpServletRequest#getUserPrincipal()
	 */
	@Nullable
	Principal getUserPrincipal();

	/**
	 * 确定用户是否具有给定角色的请求。
	 *
	 * @see javax.servlet.http.HttpServletRequest#isUserInRole(String)
	 */
	boolean isUserInRole(String role);

	/**
	 * 返回此请求是否通过安全传输机制（例如 SSL）发送。
	 *
	 * @see javax.servlet.http.HttpServletRequest#isSecure()
	 */
	boolean isSecure();

	/**
	 * 检查所请求的资源是否已根据应用程序确定的提供的最后修改时间戳（即所谓的上次修改时间）进行了修改。
	 * <p>在适用时，这也将透明地设置“Last-Modified”响应头和 HTTP 状态。
	 * <p>典型用法：
	 * <pre class="code">
	 * public String myHandleMethod(WebRequest request, Model model) {
	 *   long lastModified = // 应用程序特定的计算
	 *   if (request.checkNotModified(lastModified)) {
	 *     // 快捷退出 - 不需要进一步处理
	 *     return null;
	 *   }
	 *   // 进一步的请求处理，实际上构建内容
	 *   model.addAttribute(...);
	 *   return "myViewName";
	 * }</pre>
	 * <p>此方法适用于条件 GET/HEAD 请求，但也适用于条件 POST/PUT/DELETE 请求。
	 * <p><strong>注意：</strong>您可以使用此 {@code #checkNotModified(long)} 方法；或
	 * {@link #checkNotModified(String)}。如果要强制执行强实体标签和 Last-Modified 值，
	 * 则应使用 {@link #checkNotModified(String, long)}，正如 HTTP 规范推荐的那样。
	 * <p>如果设置了“If-Modified-Since”标头但无法解析为日期值，则此方法将忽略该标头并继续设置响应上的最后修改时间戳。
	 *
	 * @param lastModifiedTimestamp 应用程序为基础资源确定的最后修改时间戳（以毫秒为单位）
	 * @return 请求是否符合未修改的条件，允许中止请求处理，并依赖响应告知客户端内容未被修改
	 */
	boolean checkNotModified(long lastModifiedTimestamp);

	/**
	 * 检查所请求的资源是否已根据应用程序确定的提供的 ETag（实体标签）进行了修改。
	 * <p>在适用时，这也将透明地设置“ETag”响应头和 HTTP 状态。
	 * <p>典型用法：
	 * <pre class="code">
	 * public String myHandleMethod(WebRequest request, Model model) {
	 *   String eTag = // 应用程序特定的计算
	 *   if (request.checkNotModified(eTag)) {
	 *     // 快捷退出 - 不需要进一步处理
	 *     return null;
	 *   }
	 *   // 进一步的请求处理，实际上构建内容
	 *   model.addAttribute(...);
	 *   return "myViewName";
	 * }</pre>
	 * <p><strong>注意：</strong>您可以使用此 {@code #checkNotModified(String)} 方法；或
	 * {@link #checkNotModified(long)}。如果要强制执行强实体标签和 Last-Modified 值，
	 * 则应使用 {@link #checkNotModified(String, long)}。
	 *
	 * @param etag 应用程序为基础资源确定的实体标签。如果需要，此参数将用引号（"）进行填充。
	 * @return 如果请求不需要进一步处理，则返回 true。
	 */
	boolean checkNotModified(String etag);

	/**
	 * 检查所请求的资源是否已根据应用程序确定的提供的 ETag（实体标签）和最后修改时间戳进行了修改。
	 * <p>在适用时，这也将透明地设置“ETag”和“Last-Modified”响应头以及 HTTP 状态。
	 * <p>典型用法：
	 * <pre class="code">
	 * public String myHandleMethod(WebRequest request, Model model) {
	 *   String eTag = // 应用程序特定的计算
	 *   long lastModified = // 应用程序特定的计算
	 *   if (request.checkNotModified(eTag, lastModified)) {
	 *     // 快捷退出 - 不需要进一步处理
	 *     return null;
	 *   }
	 *   // 进一步的请求处理，实际上构建内容
	 *   model.addAttribute(...);
	 *   return "myViewName";
	 * }</pre>
	 * <p>此方法适用于条件 GET/HEAD 请求，但也适用于条件 POST/PUT/DELETE 请求。
	 * <p><strong>注意：</strong>HTTP 规范建议同时设置 ETag 和 Last-Modified 值，
	 * 但您也可以使用 {@code #checkNotModified(String)} 或 {@link #checkNotModified(long)}。
	 *
	 * @param etag                  应用程序为基础资源确定的实体标签。如果需要，此参数将用引号（"）进行填充。
	 * @param lastModifiedTimestamp 应用程序为基础资源确定的最后修改时间戳（以毫秒为单位）
	 * @return 如果请求不需要进一步处理，则返回 true。
	 * @since 4.2
	 */
	boolean checkNotModified(@Nullable String etag, long lastModifiedTimestamp);

	/**
	 * 获取此请求的简短描述，通常包含请求 URI 和会话 ID。
	 *
	 * @param includeClientInfo 是否包含客户端特定信息，如会话 ID 和用户名
	 * @return 所请求的描述字符串
	 */
	String getDescription(boolean includeClientInfo);

}
