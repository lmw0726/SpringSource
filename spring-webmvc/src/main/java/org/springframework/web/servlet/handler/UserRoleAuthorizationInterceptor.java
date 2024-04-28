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

package org.springframework.web.servlet.handler;

import org.springframework.lang.Nullable;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 拦截器，通过HttpServletRequest的isUserInRole方法评估用户的角色来检查当前用户的授权。
 *
 * @author Juergen Hoeller
 * @see javax.servlet.http.HttpServletRequest#isUserInRole
 * @since 20.06.2003
 */
public class UserRoleAuthorizationInterceptor implements HandlerInterceptor {

	/**
	 * 授权的角色名称数组
	 */
	@Nullable
	private String[] authorizedRoles;


	/**
	 * 设置此拦截器应将其视为已授权的角色。
	 *
	 * @param authorizedRoles 角色名称数组
	 */
	public final void setAuthorizedRoles(String... authorizedRoles) {
		this.authorizedRoles = authorizedRoles;
	}


	@Override
	public final boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws ServletException, IOException {

		// 如果有授权角色
		if (this.authorizedRoles != null) {
			// 遍历每个授权角色
			for (String role : this.authorizedRoles) {
				// 如果请求中包含该角色，则返回 true
				if (request.isUserInRole(role)) {
					return true;
				}
			}
		}

		// 处理未授权的情况
		handleNotAuthorized(request, response, handler);

		// 返回 false
		return false;
	}

	/**
	 * 处理根据此拦截器未授权的请求。
	 * 默认实现发送HTTP状态码403（"forbidden"）。
	 * <p>此方法可以重写以编写自定义消息，转发或重定向到某些错误页面或登录页面，或抛出ServletException。
	 *
	 * @param request  当前HTTP请求
	 * @param response 当前HTTP响应
	 * @param handler  选择的处理程序以执行类型和/或实例评估
	 * @throws javax.servlet.ServletException 如果存在内部错误
	 * @throws java.io.IOException            在写入响应时出现I/O错误
	 */
	protected void handleNotAuthorized(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws ServletException, IOException {
		// 设置 403 未授权状态码
		response.sendError(HttpServletResponse.SC_FORBIDDEN);
	}

}
