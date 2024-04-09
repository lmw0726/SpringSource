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

package org.springframework.web.servlet.view;

/**
 * 模板视图解析器的抽象基类，特别用于 FreeMarker 视图。
 *
 * <p>提供了一种方便的方式来指定 {@link AbstractTemplateView} 的请求属性、会话属性和 Spring 宏助手的暴露标志。
 *
 * @author Juergen Hoeller
 * @see AbstractTemplateView
 * @see org.springframework.web.servlet.view.freemarker.FreeMarkerViewResolver
 * @since 1.1
 */
public abstract class AbstractTemplateViewResolver extends UrlBasedViewResolver {
	/**
	 * 是否暴露请求属性
	 */
	private boolean exposeRequestAttributes = false;

	/**
	 * 是否允许请求覆盖
	 */
	private boolean allowRequestOverride = false;

	/**
	 * 是否暴露会话属性
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


	@Override
	protected Class<?> requiredViewClass() {
		return AbstractTemplateView.class;
	}

	/**
	 * 设置是否在与模板合并之前添加所有请求属性到模型中。默认为 "false"。
	 *
	 * @see AbstractTemplateView#setExposeRequestAttributes
	 */
	public void setExposeRequestAttributes(boolean exposeRequestAttributes) {
		this.exposeRequestAttributes = exposeRequestAttributes;
	}

	/**
	 * 设置是否允许 HttpServletRequest 属性覆盖（隐藏）相同名称的控制器生成的模型属性。默认为 "false"，
	 * 如果找到相同名称的请求属性和模型属性，则会引发异常。
	 *
	 * @see AbstractTemplateView#setAllowRequestOverride
	 */
	public void setAllowRequestOverride(boolean allowRequestOverride) {
		this.allowRequestOverride = allowRequestOverride;
	}

	/**
	 * 设置是否在与模板合并之前添加所有 HttpSession 属性到模型中。默认为 "false"。
	 *
	 * @see AbstractTemplateView#setExposeSessionAttributes
	 */
	public void setExposeSessionAttributes(boolean exposeSessionAttributes) {
		this.exposeSessionAttributes = exposeSessionAttributes;
	}

	/**
	 * 设置是否允许 HttpSession 属性覆盖（隐藏）相同名称的控制器生成的模型属性。默认为 "false"，
	 * 如果找到相同名称的会话属性和模型属性，则会引发异常。
	 *
	 * @see AbstractTemplateView#setAllowSessionOverride
	 */
	public void setAllowSessionOverride(boolean allowSessionOverride) {
		this.allowSessionOverride = allowSessionOverride;
	}

	/**
	 * 设置是否暴露 RequestContext 供 Spring 的宏库使用，名称为 "springMacroRequestContext"。默认为 "true"。
	 *
	 * @see AbstractTemplateView#setExposeSpringMacroHelpers
	 */
	public void setExposeSpringMacroHelpers(boolean exposeSpringMacroHelpers) {
		this.exposeSpringMacroHelpers = exposeSpringMacroHelpers;
	}


	@Override
	protected AbstractUrlBasedView buildView(String viewName) throws Exception {
		// 构建抽象模板视图类
		AbstractTemplateView view = (AbstractTemplateView) super.buildView(viewName);
		// 设置各项属性
		view.setExposeRequestAttributes(this.exposeRequestAttributes);
		view.setAllowRequestOverride(this.allowRequestOverride);
		view.setExposeSessionAttributes(this.exposeSessionAttributes);
		view.setAllowSessionOverride(this.allowSessionOverride);
		view.setExposeSpringMacroHelpers(this.exposeSpringMacroHelpers);
		return view;
	}

}
