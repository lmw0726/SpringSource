/*
 * Copyright 2002-2016 the original author or authors.
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
import org.springframework.util.StringUtils;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import java.security.Principal;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

/**
 * 适用于 JSF {@link javax.faces.context.FacesContext} 的 {@link WebRequest} 适配器。
 *
 * <p>自 Spring 4.0 起，需要 JSF 2.0 或更高版本。
 *
 * @author Juergen Hoeller
 * @since 2.5.2
 */
public class FacesWebRequest extends FacesRequestAttributes implements NativeWebRequest {

	/**
	 * 为给定的 Faces上下文 创建一个新的 FacesWebRequest 适配器。
	 *
	 * @param facesContext 当前的 Faces上下文
	 * @see javax.faces.context.FacesContext#getCurrentInstance()
	 */
	public FacesWebRequest(FacesContext facesContext) {
		super(facesContext);
	}


	@Override
	public Object getNativeRequest() {
		return getExternalContext().getRequest();
	}

	@Override
	public Object getNativeResponse() {
		return getExternalContext().getResponse();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getNativeRequest(@Nullable Class<T> requiredType) {
		// 如果所需类型不为空
		if (requiredType != null) {
			// 获取外部上下文中的请求对象
			Object request = getExternalContext().getRequest();
			// 如果请求对象是所需类型的实例
			if (requiredType.isInstance(request)) {
				// 返回请求对象，强制转换为所需类型
				return (T) request;
			}
		}
		// 否则返回null
		return null;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getNativeResponse(@Nullable Class<T> requiredType) {
		// 如果 所需类型 不为 null
		if (requiredType != null) {
			// 获取外部上下文的响应对象
			Object response = getExternalContext().getResponse();
			// 如果响应对象是所需类型的实例
			if (requiredType.isInstance(response)) {
				// 返回响应对象，强制转换为所需类型
				return (T) response;
			}
		}
		// 否则返回 null
		return null;
	}


	@Override
	@Nullable
	public String getHeader(String headerName) {
		return getExternalContext().getRequestHeaderMap().get(headerName);
	}

	@Override
	@Nullable
	public String[] getHeaderValues(String headerName) {
		return getExternalContext().getRequestHeaderValuesMap().get(headerName);
	}

	@Override
	public Iterator<String> getHeaderNames() {
		return getExternalContext().getRequestHeaderMap().keySet().iterator();
	}

	@Override
	@Nullable
	public String getParameter(String paramName) {
		return getExternalContext().getRequestParameterMap().get(paramName);
	}

	@Override
	public Iterator<String> getParameterNames() {
		return getExternalContext().getRequestParameterNames();
	}

	@Override
	@Nullable
	public String[] getParameterValues(String paramName) {
		return getExternalContext().getRequestParameterValuesMap().get(paramName);
	}

	@Override
	public Map<String, String[]> getParameterMap() {
		return getExternalContext().getRequestParameterValuesMap();
	}

	@Override
	public Locale getLocale() {
		return getFacesContext().getExternalContext().getRequestLocale();
	}

	@Override
	public String getContextPath() {
		return getFacesContext().getExternalContext().getRequestContextPath();
	}

	@Override
	@Nullable
	public String getRemoteUser() {
		return getFacesContext().getExternalContext().getRemoteUser();
	}

	@Override
	@Nullable
	public Principal getUserPrincipal() {
		return getFacesContext().getExternalContext().getUserPrincipal();
	}

	@Override
	public boolean isUserInRole(String role) {
		return getFacesContext().getExternalContext().isUserInRole(role);
	}

	@Override
	public boolean isSecure() {
		return false;
	}

	@Override
	public boolean checkNotModified(long lastModifiedTimestamp) {
		return false;
	}

	@Override
	public boolean checkNotModified(@Nullable String eTag) {
		return false;
	}

	@Override
	public boolean checkNotModified(@Nullable String etag, long lastModifiedTimestamp) {
		return false;
	}

	@Override
	public String getDescription(boolean includeClientInfo) {
		// 获取外部上下文对象
		ExternalContext externalContext = getExternalContext();

		// 创建 字符串构建器 对象
		StringBuilder sb = new StringBuilder();
		// 添加请求上下文路径到字符串构建器中
		sb.append("context=").append(externalContext.getRequestContextPath());
		// 如果需要包含客户端信息
		if (includeClientInfo) {
			// 获取会话对象
			Object session = externalContext.getSession(false);
			// 如果会话对象不为 null
			if (session != null) {
				// 添加会话标识符到字符串构建器中
				sb.append(";session=").append(getSessionId());
			}
			// 获取远程用户信息
			String user = externalContext.getRemoteUser();
			// 如果用户信息不为空
			if (StringUtils.hasLength(user)) {
				// 添加用户信息到字符串构建器中
				sb.append(";user=").append(user);
			}
		}
		// 返回字符串构建器中的字符串表示
		return sb.toString();
	}


	@Override
	public String toString() {
		return "FacesWebRequest: " + getDescription(true);
	}

}
