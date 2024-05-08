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

package org.springframework.web.servlet.mvc;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.support.RequestContextUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 一个简单的控制器，总是返回预先配置的视图，并可选择设置响应状态码。可以使用提供的配置属性配置视图和状态。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Keith Donald
 * @author Rossen Stoyanchev
 */
public class ParameterizableViewController extends AbstractController {

	/**
	 * 视图对象
	 */
	@Nullable
	private Object view;

	/**
	 * Http状态码
	 */
	@Nullable
	private HttpStatus statusCode;

	/**
	 * 此属性用于指示请求是否在控制器中完全处理，并且不应使用视图进行渲染。
	 */
	private boolean statusOnly;


	public ParameterizableViewController() {
		super(false);
		// 设置支持的方法为Get、Head方法
		setSupportedMethods(HttpMethod.GET.name(), HttpMethod.HEAD.name());
	}

	/**
	 * 设置 ModelAndView 要返回的视图名称，由 DispatcherServlet 通过 ViewResolver 解析。将覆盖任何现有的视图名称或 View。
	 */
	public void setViewName(@Nullable String viewName) {
		this.view = viewName;
	}

	/**
	 * 返回要委托的视图的名称，如果使用 View 实例，则返回 {@code null}。
	 */
	@Nullable
	public String getViewName() {
		if (this.view instanceof String) {
			// 如果视图对象是字符串类型
			String viewName = (String) this.view;
			if (getStatusCode() != null && getStatusCode().is3xxRedirection()) {
				// 如果状态码是 3xx 重定向，并且视图名称以 "redirect:" 开头，则直接返回视图名称，否则加上 "redirect:" 前缀
				return viewName.startsWith("redirect:") ? viewName : "redirect:" + viewName;
			} else {
				return viewName;
			}
		}
		return null;
	}

	/**
	 * 设置 ModelAndView 要返回的 View 对象。将覆盖任何现有的视图名称或 View。
	 *
	 * @since 4.1
	 */
	public void setView(View view) {
		this.view = view;
	}

	/**
	 * 返回 View 对象，如果使用视图名称，则返回 {@code null}。
	 *
	 * @since 4.1
	 */
	@Nullable
	public View getView() {
		return (this.view instanceof View ? (View) this.view : null);
	}

	/**
	 * 配置此控制器应在响应上设置的 HTTP 状态码。
	 * <p>
	 * 当配置了以 "redirect:" 前缀的视图名称时，就不需要设置此属性，因为 RedirectView 将会处理。然而，此属性仍可用于重写 {@code RedirectView} 的 3xx 状态码。要完全控制重定向，请提供 {@code RedirectView} 实例。
	 * <p>
	 * 如果状态码是 204，并且没有配置视图，则请求将在控制器中完全处理。
	 *
	 * @since 4.1
	 */
	public void setStatusCode(@Nullable HttpStatus statusCode) {
		this.statusCode = statusCode;
	}

	/**
	 * 返回已配置的 HTTP 状态码，或 {@code null}。
	 *
	 * @since 4.1
	 */
	@Nullable
	public HttpStatus getStatusCode() {
		return this.statusCode;
	}


	/**
	 * 此属性用于指示请求是否在控制器中完全处理，并且不应使用视图进行渲染。与 {@link #setStatusCode} 结合使用时很有用。
	 * <p>
	 * 默认设置为 {@code false}。
	 *
	 * @since 4.1
	 */
	public void setStatusOnly(boolean statusOnly) {
		this.statusOnly = statusOnly;
	}

	/**
	 * 请求是否在控制器中完全处理。
	 */
	public boolean isStatusOnly() {
		return this.statusOnly;
	}


	/**
	 * 返回指定视图名称的 ModelAndView 对象。
	 * <p>
	 * {@link RequestContextUtils#getInputFlashMap "input" FlashMap} 的内容也将添加到模型中。
	 *
	 * @see #getViewName()
	 */
	@Override
	protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		// 获取视图名称
		String viewName = getViewName();

		if (getStatusCode() != null) {
			// 如果状态码不为空
			if (getStatusCode().is3xxRedirection()) {
				// 如果状态码是 3xx 重定向，则将状态码设置为响应属性
				request.setAttribute(View.RESPONSE_STATUS_ATTRIBUTE, getStatusCode());
			} else {
				// 设置响应状态码
				response.setStatus(getStatusCode().value());
				// 如果状态码是 204（No Content），且视图名称为空，则直接返回 null
				if (getStatusCode().equals(HttpStatus.NO_CONTENT) && viewName == null) {
					return null;
				}
			}
		}

		// 如果仅返回状态码，则直接返回 null
		if (isStatusOnly()) {
			return null;
		}

		// 创建 ModelAndView 对象，并添加请求的“输入”闪存映射
		ModelAndView modelAndView = new ModelAndView();
		modelAndView.addAllObjects(RequestContextUtils.getInputFlashMap(request));
		if (viewName != null) {
			// 如果视图名称存在，则设置视图名称
			modelAndView.setViewName(viewName);
		} else {
			// 否则设置视图对象
			modelAndView.setView(getView());
		}
		return modelAndView;
	}

	@Override
	public String toString() {
		return "ParameterizableViewController [" + formatStatusAndView() + "]";
	}

	private String formatStatusAndView() {
		StringBuilder sb = new StringBuilder();
		if (this.statusCode != null) {
			sb.append("status=").append(this.statusCode);
		}
		if (this.view != null) {
			sb.append(sb.length() != 0 ? ", " : "");
			String viewName = getViewName();
			sb.append("view=").append(viewName != null ? "\"" + viewName + "\"" : this.view);
		}
		return sb.toString();
	}
}
