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

package org.springframework.web.servlet.view.tiles3;

import org.apache.tiles.TilesContainer;
import org.apache.tiles.access.TilesAccess;
import org.apache.tiles.renderer.DefinitionRenderer;
import org.apache.tiles.request.AbstractRequest;
import org.apache.tiles.request.ApplicationContext;
import org.apache.tiles.request.Request;
import org.apache.tiles.request.render.Renderer;
import org.apache.tiles.request.servlet.ServletRequest;
import org.apache.tiles.request.servlet.ServletUtil;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.support.JstlUtils;
import org.springframework.web.servlet.support.RequestContext;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.springframework.web.servlet.view.AbstractUrlBasedView;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Locale;
import java.util.Map;

/**
 * 实现了{@link org.springframework.web.servlet.View} 接口，通过Tiles请求API进行渲染。
 * "url"属性被解释为Tiles定义的名称。
 *
 * @author Nicolas Le Bas
 * @author mick semb wever
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @since 3.2
 */
public class TilesView extends AbstractUrlBasedView {
	/**
	 * 渲染器
	 */
	@Nullable
	private Renderer renderer;

	/**
	 * 是否公开JSTL属性。
	 */
	private boolean exposeJstlAttributes = true;

	/**
	 * 是否始终包含视图而不是转发到它。
	 */
	private boolean alwaysInclude = false;

	/**
	 * 应用上下文
	 */
	@Nullable
	private ApplicationContext applicationContext;


	/**
	 * 设置要使用的{@link Renderer}。
	 * 如果未设置，默认情况下将使用{@link DefinitionRenderer}。
	 */
	public void setRenderer(Renderer renderer) {
		this.renderer = renderer;
	}

	/**
	 * 是否公开JSTL属性。默认为{@code true}。
	 *
	 * @see JstlUtils#exposeLocalizationContext(RequestContext)
	 */
	protected void setExposeJstlAttributes(boolean exposeJstlAttributes) {
		this.exposeJstlAttributes = exposeJstlAttributes;
	}

	/**
	 * 指定是否始终包含视图而不是转发到它。
	 * <p>默认为"false"。打开此标志以强制使用Servlet包含，即使可以进行转发。
	 *
	 * @see TilesViewResolver#setAlwaysInclude
	 * @since 4.1.2
	 */
	public void setAlwaysInclude(boolean alwaysInclude) {
		this.alwaysInclude = alwaysInclude;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		// 调用父类的方法，检查URL必须时，URL不为空
		super.afterPropertiesSet();

		// 获取 ServletContext 并确保不为 null
		ServletContext servletContext = getServletContext();
		Assert.state(servletContext != null, "No ServletContext");

		// 从 ServletContext 中获取 ApplicationContext
		this.applicationContext = ServletUtil.getApplicationContext(servletContext);

		// 如果没有设置渲染器，则尝试从 ApplicationContext 中获取 Tiles 容器，并创建 DefinitionRenderer
		if (this.renderer == null) {
			TilesContainer container = TilesAccess.getContainer(this.applicationContext);
			this.renderer = new DefinitionRenderer(container);
		}
	}


	@Override
	public boolean checkResource(final Locale locale) throws Exception {
		Assert.state(this.renderer != null, "No Renderer set");

		HttpServletRequest servletRequest = null;

		// 获取当前请求的 请求属性
		RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();

		// 如果 请求属性 是 ServletRequestAttributes 类型，则从中获取 HttpServletRequest
		if (requestAttributes instanceof ServletRequestAttributes) {
			servletRequest = ((ServletRequestAttributes) requestAttributes).getRequest();
		}

		// 创建 Request 对象，用于判断是否可以渲染指定的 URL
		Request request = new ServletRequest(this.applicationContext, servletRequest, null) {
			@Override
			public Locale getRequestLocale() {
				return locale;
			}
		};

		// 调用 渲染器 的 isRenderable 方法判断指定的 URL 是否可以渲染
		return this.renderer.isRenderable(getUrl(), request);
	}

	@Override
	protected void renderMergedOutputModel(Map<String, Object> model, HttpServletRequest request,
										   HttpServletResponse response) throws Exception {

		Assert.state(this.renderer != null, "No Renderer set");

		// 将模型数据暴露为请求属性
		exposeModelAsRequestAttributes(model, request);

		// 如果设置了暴露 JSTL 属性，则将本地化上下文暴露给 JSTL
		if (this.exposeJstlAttributes) {
			JstlUtils.exposeLocalizationContext(new RequestContext(request, getServletContext()));
		}

		// 如果始终包含，则设置强制包含属性
		if (this.alwaysInclude) {
			request.setAttribute(AbstractRequest.FORCE_INCLUDE_ATTRIBUTE_NAME, true);
		}

		// 创建 TilesRequest 对象
		Request tilesRequest = createTilesRequest(request, response);

		// 使用 DefinitionRenderer 渲染指定的 URL
		this.renderer.render(getUrl(), tilesRequest);
	}

	/**
	 * 创建一个Tiles {@link Request}。
	 * <p>此实现创建一个{@link ServletRequest}。
	 *
	 * @param request  当前请求
	 * @param response 当前响应
	 * @return Tiles请求
	 */
	protected Request createTilesRequest(final HttpServletRequest request, HttpServletResponse response) {
		return new ServletRequest(this.applicationContext, request, response) {
			@Override
			public Locale getRequestLocale() {
				return RequestContextUtils.getLocale(request);
			}
		};
	}

}
