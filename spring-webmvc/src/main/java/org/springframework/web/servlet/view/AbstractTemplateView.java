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

package org.springframework.web.servlet.view;

import org.springframework.web.servlet.support.RequestContext;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 基于模板的视图技术（如 FreeMarker）的适配器基类，具有在其模型中使用请求和会话属性的能力，
 * 并具有为 Spring 的 FreeMarker 宏库公开辅助对象的选项。
 *
 * <p>JSP/JSTL 和其他视图技术自动可以访问 HttpServletRequest 对象，从而可以获取当前用户的请求/会话属性，
 * 此外，它们能够作为请求属性自行创建和缓存辅助对象。
 *
 * @author Juergen Hoeller
 * @author Darren Davison
 * @see AbstractTemplateViewResolver
 * @see org.springframework.web.servlet.view.freemarker.FreeMarkerView
 * @since 1.0.2
 */
public abstract class AbstractTemplateView extends AbstractUrlBasedView {

	/**
	 * 在模板模型中的 RequestContext 实例的变量名，
	 * 可供 Spring 的宏使用：例如用于创建 BindStatus 对象。
	 */
	public static final String SPRING_MACRO_REQUEST_CONTEXT_ATTRIBUTE = "springMacroRequestContext";

	/**
	 * 是否需要暴露请求属性
	 */
	private boolean exposeRequestAttributes = false;

	/**
	 * 是否允许请求覆盖
	 */
	private boolean allowRequestOverride = false;

	/**
	 * 是否保存会话属性
	 */
	private boolean exposeSessionAttributes = false;

	/**
	 * 是否允许会话覆盖
	 */
	private boolean allowSessionOverride = false;

	/**
	 * 是否暴露Spring宏助手
	 */
	private boolean exposeSpringMacroHelpers = true;


	/**
	 * 设置是否应在与模板合并之前将所有请求属性添加到模型中。
	 * 默认值为 "false"。
	 */
	public void setExposeRequestAttributes(boolean exposeRequestAttributes) {
		this.exposeRequestAttributes = exposeRequestAttributes;
	}

	/**
	 * 设置是否允许 HttpServletRequest 属性覆盖（隐藏）控制器生成的同名模型属性。
	 * 默认值为 "false"，如果发现同名的请求属性和模型属性，将抛出异常。
	 */
	public void setAllowRequestOverride(boolean allowRequestOverride) {
		this.allowRequestOverride = allowRequestOverride;
	}

	/**
	 * 设置是否应将所有 HttpSession 属性添加到模型中，在与模板合并之前。
	 * 默认值为 "false"。
	 */
	public void setExposeSessionAttributes(boolean exposeSessionAttributes) {
		this.exposeSessionAttributes = exposeSessionAttributes;
	}

	/**
	 * 设置是否公开用于 Spring 的宏库的 RequestContext，
	 * 在名称 "springMacroRequestContext" 下。默认值为 "true"。
	 * <p>目前对于 Spring 的 FreeMarker 默认宏是需要的。
	 * 请注意，这 <i>不</i> 是使用 HTML 表单的模板所必需的，
	 * <i>除非</i>您希望利用 Spring 辅助宏。
	 *
	 * @see #SPRING_MACRO_REQUEST_CONTEXT_ATTRIBUTE
	 */
	public void setExposeSpringMacroHelpers(boolean exposeSpringMacroHelpers) {
		this.exposeSpringMacroHelpers = exposeSpringMacroHelpers;
	}


	/**
	 * 重写父类的方法，用于渲染合并后的输出模型
	 *
	 * @param model    模型
	 * @param request  HttpServletRequest
	 * @param response HttpServletResponse
	 * @throws Exception 异常
	 */
	@Override
	protected final void renderMergedOutputModel(
			Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws Exception {

		// 判断是否需要暴露请求属性
		if (this.exposeRequestAttributes) {
			Map<String, Object> exposed = null;
			// 遍历请求属性
			for (Enumeration<String> en = request.getAttributeNames(); en.hasMoreElements(); ) {
				String attribute = en.nextElement();
				// 如果模型中已存在同名对象，且不允许覆盖，则抛出异常
				if (model.containsKey(attribute) && !this.allowRequestOverride) {
					throw new ServletException("Cannot expose request attribute '" + attribute +
							"' because of an existing model object of the same name");
				}
				Object attributeValue = request.getAttribute(attribute);
				// 如果开启了调试模式，记录暴露的请求属性
				if (logger.isDebugEnabled()) {
					exposed = exposed != null ? exposed : new LinkedHashMap<>();
					exposed.put(attribute, attributeValue);
				}
				// 将请求属性添加到模型中
				model.put(attribute, attributeValue);
			}
			// 如果开启了跟踪模式，记录暴露的请求属性
			if (logger.isTraceEnabled() && exposed != null) {
				logger.trace("Exposed request attributes to model: " + exposed);
			}
		}

		// 判断是否需要暴露会话属性
		if (this.exposeSessionAttributes) {
			HttpSession session = request.getSession(false);
			// 如果存在会话
			if (session != null) {
				Map<String, Object> exposed = null;
				// 遍历会话属性
				for (Enumeration<String> en = session.getAttributeNames(); en.hasMoreElements(); ) {
					String attribute = en.nextElement();
					// 如果模型中已存在同名对象，且不允许覆盖，则抛出异常
					if (model.containsKey(attribute) && !this.allowSessionOverride) {
						throw new ServletException("Cannot expose session attribute '" + attribute +
								"' because of an existing model object of the same name");
					}
					Object attributeValue = session.getAttribute(attribute);
					// 如果开启了调试模式，记录暴露的会话属性
					if (logger.isDebugEnabled()) {
						exposed = exposed != null ? exposed : new LinkedHashMap<>();
						exposed.put(attribute, attributeValue);
					}
					// 将会话属性添加到模型中
					model.put(attribute, attributeValue);
				}
				// 如果开启了跟踪模式，记录暴露的会话属性
				if (logger.isTraceEnabled() && exposed != null) {
					logger.trace("Exposed session attributes to model: " + exposed);
				}
			}
		}

		// 判断是否需要暴露 Spring 宏助手
		if (this.exposeSpringMacroHelpers) {
			// 如果模型中已存在同名对象，则抛出异常
			if (model.containsKey(SPRING_MACRO_REQUEST_CONTEXT_ATTRIBUTE)) {
				throw new ServletException(
						"Cannot expose bind macro helper '" + SPRING_MACRO_REQUEST_CONTEXT_ATTRIBUTE +
								"' because of an existing model object of the same name");
			}
			// 公开 RequestContext 实例以供 Spring 宏使用。
			model.put(SPRING_MACRO_REQUEST_CONTEXT_ATTRIBUTE,
					new RequestContext(request, response, getServletContext(), model));
		}
		// 设置HTTP响应的内容类型
		applyContentType(response);

		// 如果开启了调试模式，记录渲染的 URL
		if (logger.isDebugEnabled()) {
			logger.debug("Rendering [" + getUrl() + "]");
		}

		// 渲染合并后的模板模型
		renderMergedTemplateModel(model, request, response);
	}

	/**
	 * 应用此视图的内容类型（如在 "contentType"  bean 属性中指定）到给定的响应。
	 * <p>仅在响应之前未设置内容类型的情况下应用视图的内容类型。
	 * 这允许处理程序在之前覆盖默认的内容类型。
	 *
	 * @param response 当前 HTTP 响应
	 * @see #setContentType
	 */
	protected void applyContentType(HttpServletResponse response) {
		if (response.getContentType() == null) {
			response.setContentType(getContentType());
		}
	}

	/**
	 * 子类必须实现此方法以实际渲染视图。
	 *
	 * @param model    合并后的输出 Map，其中如果需要，将请求属性和会话属性与之合并
	 * @param request  当前 HTTP 请求
	 * @param response 当前 HTTP 响应
	 * @throws Exception 如果渲染失败
	 */
	protected abstract void renderMergedTemplateModel(
			Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws Exception;

}
