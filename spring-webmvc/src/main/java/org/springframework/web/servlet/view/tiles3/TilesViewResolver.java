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

package org.springframework.web.servlet.view.tiles3;

import org.apache.tiles.request.render.Renderer;

import org.springframework.lang.Nullable;
import org.springframework.web.servlet.view.AbstractUrlBasedView;
import org.springframework.web.servlet.view.UrlBasedViewResolver;

/**
 * 支持 {@link TilesView}（即Tiles定义）及其自定义子类的{@link UrlBasedViewResolver} 的便利子类。
 *
 * @author Nicolas Le Bas
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @since 3.2
 */
public class TilesViewResolver extends UrlBasedViewResolver {
	/**
	 * 渲染器
	 */
	@Nullable
	private Renderer renderer;

	/**
	 * 指定是否始终包含视图而不是转发到它。
	 */
	@Nullable
	private Boolean alwaysInclude;


	/**
	 * 此解析器需要 {@link TilesView}。
	 */
	public TilesViewResolver() {
		setViewClass(requiredViewClass());
	}

	/**
	 * 设置要使用的 {@link Renderer}。如果未指定，则将使用默认的 {@link org.apache.tiles.renderer.DefinitionRenderer}。
	 *
	 * @see TilesView#setRenderer(Renderer)
	 */
	public void setRenderer(Renderer renderer) {
		this.renderer = renderer;
	}

	/**
	 * 指定是否始终包含视图而不是转发到它。
	 * <p>默认为“false”。打开此标志以强制使用Servlet包含，即使转发也是可能的。
	 *
	 * @since 4.1.2
	 * @see TilesView#setAlwaysInclude
	 */
	public void setAlwaysInclude(Boolean alwaysInclude) {
		this.alwaysInclude = alwaysInclude;
	}


	@Override
	protected Class<?> requiredViewClass() {
		return TilesView.class;
	}

	@Override
	protected AbstractUrlBasedView instantiateView() {
		return (getViewClass() == TilesView.class ? new TilesView() : super.instantiateView());
	}

	@Override
	protected TilesView buildView(String viewName) throws Exception {
		// 调用父类的方法构建 TilesView 对象
		TilesView view = (TilesView) super.buildView(viewName);
		// 如果渲染器不为空，则设置到视图对象中
		if (this.renderer != null) {
			view.setRenderer(this.renderer);
		}
		// 如果 alwaysInclude 属性不为空，则设置到视图对象中
		if (this.alwaysInclude != null) {
			view.setAlwaysInclude(this.alwaysInclude);
		}
		// 返回构建的 TilesView 对象
		return view;
	}

}
