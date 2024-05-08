/*
 * Copyright 2002-2012 the original author or authors.
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

import org.springframework.util.Assert;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.springframework.web.util.UrlPathHelper;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 基于请求 URL 返回视图名称的 {@code Controllers} 的抽象基类。
 *
 * <p>提供了从 URL 中确定视图名称的基础设施，并可配置的 URL 查找。
 * 有关后者的信息，请参见 {@code alwaysUseFullPath} 和 {@code urlDecode} 属性。
 *
 * @author Juergen Hoeller
 * @see #setAlwaysUseFullPath
 * @see #setUrlDecode
 * @since 1.2.6
 */
public abstract class AbstractUrlViewController extends AbstractController {

	/**
	 * URL路径助手
	 */
	private UrlPathHelper urlPathHelper = new UrlPathHelper();


	/**
	 * 设置是否 URL 查找应始终使用当前 Servlet 上下文中的完整路径。
	 * 否则，如果适用，则使用当前 Servlet 映射中的路径（即在 web.xml 中的 ".../*" Servlet 映射的情况下）。
	 * 默认为 "false"。
	 *
	 * @see org.springframework.web.util.UrlPathHelper#setAlwaysUseFullPath
	 */
	public void setAlwaysUseFullPath(boolean alwaysUseFullPath) {
		this.urlPathHelper.setAlwaysUseFullPath(alwaysUseFullPath);
	}

	/**
	 * 设置是否应对上下文路径和请求 URI 进行 URL 解码。
	 * Servlet API 返回它们 <i>未解码</i>，与 Servlet 路径相反。
	 * <p>使用请求编码或 Servlet 规范（ISO-8859-1）中的默认编码。
	 *
	 * @see org.springframework.web.util.UrlPathHelper#setUrlDecode
	 */
	public void setUrlDecode(boolean urlDecode) {
		this.urlPathHelper.setUrlDecode(urlDecode);
	}

	/**
	 * 设置是否应从请求 URI 中删除 ";"（分号）内容。
	 *
	 * @see org.springframework.web.util.UrlPathHelper#setRemoveSemicolonContent(boolean)
	 */
	public void setRemoveSemicolonContent(boolean removeSemicolonContent) {
		this.urlPathHelper.setRemoveSemicolonContent(removeSemicolonContent);
	}

	/**
	 * 设置要用于查找路径解析的 URL路径助手。
	 * <p>使用此方法可以使用自定义子类重写默认的 URL路径助手，
	 * 或者在多个 方法名解析器 和 处理器映射 中共享公共 URL路径助手 设置。
	 *
	 * @see org.springframework.web.servlet.handler.AbstractUrlHandlerMapping#setUrlPathHelper
	 */
	public void setUrlPathHelper(UrlPathHelper urlPathHelper) {
		Assert.notNull(urlPathHelper, "UrlPathHelper must not be null");
		this.urlPathHelper = urlPathHelper;
	}

	/**
	 * 返回用于解析查找路径的 URL路径助手。
	 */
	protected UrlPathHelper getUrlPathHelper() {
		return this.urlPathHelper;
	}


	/**
	 * 检索要用于查找的 URL 路径，并委托给 {@link #getViewNameForRequest}。
	 * 还将 {@link RequestContextUtils#getInputFlashMap} 的内容添加到模型中。
	 */
	@Override
	protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) {
		// 获取视图名称
		String viewName = getViewNameForRequest(request);
		if (logger.isTraceEnabled()) {
			logger.trace("Returning view name '" + viewName + "'");
		}
		// 获取输入闪存映射和视图名称，构建模型和视图对象
		return new ModelAndView(viewName, RequestContextUtils.getInputFlashMap(request));
	}

	/**
	 * 根据给定的查找路径返回此请求的要渲染的视图名称。由 {@link #handleRequestInternal} 调用。
	 *
	 * @param request 当前 HTTP 请求
	 * @return 此请求的视图名称（永远不会是 {@code null}）
	 * @see #handleRequestInternal
	 * @see #setAlwaysUseFullPath
	 * @see #setUrlDecode
	 */
	protected abstract String getViewNameForRequest(HttpServletRequest request);

}
