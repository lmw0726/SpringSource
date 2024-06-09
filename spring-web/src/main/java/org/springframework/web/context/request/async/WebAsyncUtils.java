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

package org.springframework.web.context.request.async;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.WebRequest;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 与处理异步 Web 请求相关的实用方法。
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 3.2
 */
public abstract class WebAsyncUtils {

	/**
	 * 包含 {@link WebAsyncManager} 的名称属性。
	 */
	public static final String WEB_ASYNC_MANAGER_ATTRIBUTE =
			WebAsyncManager.class.getName() + ".WEB_ASYNC_MANAGER";


	/**
	 * 获取当前请求的 {@link WebAsyncManager}，如果不存在，则创建并将其与请求关联。
	 *
	 * @param servletRequest 当前请求
	 * @return 当前请求的 WebAsyncManager
	 */
	public static WebAsyncManager getAsyncManager(ServletRequest servletRequest) {
		// 初始化 Web异步管理器 对象
		WebAsyncManager asyncManager = null;
		// 从 Servlet 请求属性中获取 Web异步管理器 对象
		Object asyncManagerAttr = servletRequest.getAttribute(WEB_ASYNC_MANAGER_ATTRIBUTE);
		// 如果 Servlet 请求属性中存在 Web异步管理器 对象，则将其赋值给 异步管理器 对象
		if (asyncManagerAttr instanceof WebAsyncManager) {
			asyncManager = (WebAsyncManager) asyncManagerAttr;
		}
		// 如果 异步管理器 为空，则创建一个新的 Web异步管理器 对象，并将其设置到 Servlet 请求属性中
		if (asyncManager == null) {
			asyncManager = new WebAsyncManager();
			servletRequest.setAttribute(WEB_ASYNC_MANAGER_ATTRIBUTE, asyncManager);
		}
		// 返回 异步管理器 对象
		return asyncManager;
	}

	/**
	 * 获取当前请求的 {@link WebAsyncManager}，如果不存在，则创建并将其与请求关联。
	 *
	 * @param webRequest 当前请求
	 * @return 当前请求的 WebAsyncManager
	 */
	public static WebAsyncManager getAsyncManager(WebRequest webRequest) {
		// 设置作用域为 请求范围
		int scope = RequestAttributes.SCOPE_REQUEST;
		// 初始化 Web异步管理器 为null
		WebAsyncManager asyncManager = null;
		// 从 Web请求 中获取 WEB_ASYNC_MANAGER_ATTRIBUTE 属性
		Object asyncManagerAttr = webRequest.getAttribute(WEB_ASYNC_MANAGER_ATTRIBUTE, scope);
		// 如果属性是 Web异步管理器 类型，则将 异步管理器 设置为该属性
		if (asyncManagerAttr instanceof WebAsyncManager) {
			asyncManager = (WebAsyncManager) asyncManagerAttr;
		}
		// 如果 异步管理器 仍为null，则新建一个 Web异步管理器 对象
		if (asyncManager == null) {
			asyncManager = new WebAsyncManager();
			// 将新建的 异步管理器 设置为 web请求 的WEB_ASYNC_MANAGER_ATTRIBUTE属性
			webRequest.setAttribute(WEB_ASYNC_MANAGER_ATTRIBUTE, asyncManager, scope);
		}
		// 返回 异步管理器
		return asyncManager;
	}

	/**
	 * 创建一个 AsyncWebRequest 实例。默认情况下，将创建一个 StandardServletAsyncWebRequest 实例。
	 *
	 * @param request  当前请求
	 * @param response 当前响应
	 * @return AsyncWebRequest 实例（永远不会为 null）
	 */
	public static AsyncWebRequest createAsyncWebRequest(HttpServletRequest request, HttpServletResponse response) {
		return new StandardServletAsyncWebRequest(request, response);
	}

}
