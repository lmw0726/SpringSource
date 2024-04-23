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

package org.springframework.web.servlet;

import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.ui.ModelMap;
import org.springframework.util.CollectionUtils;

import java.util.Map;

/**
 * Web MVC 框架中包含模型和视图的持有者。
 * 请注意，这两者是完全不同的。此类仅持有两者，以使控制器能够在单个返回值中返回模型和视图。
 *
 * <p>表示由处理程序返回的模型和视图，由 DispatcherServlet 解析。
 * 视图可以采用视图名称的形式，需要由 ViewResolver 对象解析；
 * 或者可以直接指定 View 对象。模型是一个 Map，允许使用多个以名称为键的对象。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Rossen Stoyanchev
 * @see DispatcherServlet
 * @see ViewResolver
 * @see HandlerAdapter#handle
 * @see org.springframework.web.servlet.mvc.Controller#handleRequest
 */
public class ModelAndView {

	/**
	 * 视图实例或视图名称字符串。
	 */
	@Nullable
	private Object view;

	/**
	 * 模型 Map。
	 */
	@Nullable
	private ModelMap model;

	/**
	 * 响应的可选 HTTP 状态。
	 */
	@Nullable
	private HttpStatus status;

	/**
	 * 表示此实例是否已使用调用 {@link #clear()} 清除。
	 */
	private boolean cleared = false;


	/**
	 * Bean 样式用法的默认构造函数：填充 bean 属性而不是传递构造函数参数。
	 *
	 * @see #setView(View)
	 * @see #setViewName(String)
	 */
	public ModelAndView() {
	}

	/**
	 * 当没有要公开的模型数据时，方便的构造函数。
	 * 也可以与 {@code addObject} 结合使用。
	 *
	 * @param viewName 要渲染的 View 的名称，由 DispatcherServlet 的 ViewResolver 解析
	 * @see #addObject
	 */
	public ModelAndView(String viewName) {
		this.view = viewName;
	}

	/**
	 * 当没有要公开的模型数据时，方便的构造函数。
	 * 也可以与 {@code addObject} 结合使用。
	 *
	 * @param view 要渲染的 View 对象
	 * @see #addObject
	 */
	public ModelAndView(View view) {
		this.view = view;
	}

	/**
	 * 创建一个给定视图名称和模型的新 ModelAndView。
	 *
	 * @param viewName 要渲染的 View 的名称，由 DispatcherServlet 的 ViewResolver 解析
	 * @param model    模型名称（String）到模型对象（Object）的映射。模型条目可能不为 {@code null}，但如果没有模型数据，则模型 Map 可能为 {@code null}。
	 */
	public ModelAndView(String viewName, @Nullable Map<String, ?> model) {
		this.view = viewName;
		if (model != null) {
			getModelMap().addAllAttributes(model);
		}
	}

	/**
	 * 创建一个给定 View 对象和模型的新 ModelAndView。
	 * <em>注意：提供的模型数据会复制到此类的内部存储中。在提供给此类之后，请不要考虑修改提供的 Map</em>
	 *
	 * @param view  要渲染的 View 对象
	 * @param model 模型名称（String）到模型对象（Object）的映射。模型条目可能不为 {@code null}，但如果没有模型数据，则模型 Map 可能为 {@code null}。
	 */
	public ModelAndView(View view, @Nullable Map<String, ?> model) {
		this.view = view;
		if (model != null) {
			getModelMap().addAllAttributes(model);
		}
	}

	/**
	 * 给定视图名称和 HTTP 状态的新 ModelAndView。
	 *
	 * @param viewName 要渲染的 View 的名称，由 DispatcherServlet 的 ViewResolver 解析
	 * @param status   用于响应的 HTTP 状态代码（在视图渲染之前设置）
	 * @since 4.3.8
	 */
	public ModelAndView(String viewName, HttpStatus status) {
		this.view = viewName;
		this.status = status;
	}

	/**
	 * 给定视图名称、模型和 HTTP 状态的新 ModelAndView。
	 *
	 * @param viewName 要渲染的 View 的名称，由 DispatcherServlet 的 ViewResolver 解析
	 * @param model    模型名称（String）到模型对象（Object）的映射。模型条目可能不为 {@code null}，但如果没有模型数据，则模型 Map 可能为 {@code null}。
	 * @param status   用于响应的 HTTP 状态代码（在视图渲染之前设置）
	 * @since 4.3
	 */
	public ModelAndView(@Nullable String viewName, @Nullable Map<String, ?> model, @Nullable HttpStatus status) {
		this.view = viewName;
		if (model != null) {
			getModelMap().addAllAttributes(model);
		}
		this.status = status;
	}

	/**
	 * 方便的构造函数以使用单个模型对象。
	 *
	 * @param viewName    要渲染的 View 的名称，由 DispatcherServlet 的 ViewResolver 解析
	 * @param modelName   模型中的单个条目的名称
	 * @param modelObject 单个模型对象
	 */
	public ModelAndView(String viewName, String modelName, Object modelObject) {
		this.view = viewName;
		addObject(modelName, modelObject);
	}

	/**
	 * 方便的构造函数以使用单个模型对象。
	 *
	 * @param view        要渲染的 View 对象
	 * @param modelName   模型中的单个条目的名称
	 * @param modelObject 单个模型对象
	 */
	public ModelAndView(View view, String modelName, Object modelObject) {
		this.view = view;
		addObject(modelName, modelObject);
	}


	/**
	 * 为此 ModelAndView 设置视图名称，由 ViewResolver 通过视图名称解析 DispatcherServlet。
	 * 将覆盖任何先前存在的视图名称或 View。
	 */
	public void setViewName(@Nullable String viewName) {
		this.view = viewName;
	}

	/**
	 * 返回要由 ViewResolver 通过视图名称解析 DispatcherServlet 的视图名称，如果我们使用 View 对象，则返回 {@code null}。
	 */
	@Nullable
	public String getViewName() {
		return (this.view instanceof String ? (String) this.view : null);
	}

	/**
	 * 为此 ModelAndView 设置 View 对象。将覆盖任何先前存在的视图名称或 View。
	 */
	public void setView(@Nullable View view) {
		this.view = view;
	}

	/**
	 * 返回 View 对象，如果我们使用要由 ViewResolver 通过视图名称解析 DispatcherServlet 的视图名称，则返回 {@code null}。
	 */
	@Nullable
	public View getView() {
		return (this.view instanceof View ? (View) this.view : null);
	}

	/**
	 * 指示此 {@code ModelAndView} 是否具有视图，作为视图名称或直接 {@link View} 实例。
	 */
	public boolean hasView() {
		return (this.view != null);
	}

	/**
	 * 返回是否使用视图引用，即如果视图是通过视图名称由 DispatcherServlet 通过 ViewResolver 解析，则返回 {@code true}。
	 */
	public boolean isReference() {
		return (this.view instanceof String);
	}

	/**
	 * 返回模型映射。可能返回 {@code null}。
	 * DispatcherServlet 通过评估模型调用。
	 */
	@Nullable
	protected Map<String, Object> getModelInternal() {
		return this.model;
	}

	/**
	 * 返回底层 {@code ModelMap} 实例（永远不为 {@code null}）。
	 */
	public ModelMap getModelMap() {
		if (this.model == null) {
			this.model = new ModelMap();
		}
		return this.model;
	}

	/**
	 * 返回模型映射。永远不会返回 {@code null}。
	 * 应用程序代码调用以修改模型。
	 */
	public Map<String, Object> getModel() {
		return getModelMap();
	}

	/**
	 * 设置要用于响应的 HTTP 状态。
	 * <p>在视图渲染之前设置响应状态。
	 *
	 * @since 4.3
	 */
	public void setStatus(@Nullable HttpStatus status) {
		this.status = status;
	}

	/**
	 * 返回响应的配置的 HTTP 状态，如果有的话。
	 *
	 * @since 4.3
	 */
	@Nullable
	public HttpStatus getStatus() {
		return this.status;
	}


	/**
	 * 添加属性到模型。
	 *
	 * @param attributeName  要添加到模型的对象的名称（永远不会为 {@code null}）
	 * @param attributeValue 要添加到模型的对象（可以为 {@code null}）
	 * @see ModelMap#addAttribute(String, Object)
	 * @see #getModelMap()
	 */
	public ModelAndView addObject(String attributeName, @Nullable Object attributeValue) {
		getModelMap().addAttribute(attributeName, attributeValue);
		return this;
	}

	/**
	 * 使用参数名称生成添加属性到模型。
	 *
	 * @param attributeValue 要添加到模型的对象（永远不会为 {@code null}）
	 * @see ModelMap#addAttribute(Object)
	 * @see #getModelMap()
	 */
	public ModelAndView addObject(Object attributeValue) {
		getModelMap().addAttribute(attributeValue);
		return this;
	}

	/**
	 * 添加包含在提供的 Map 中的所有属性到模型。
	 *
	 * @param modelMap 包含 attributeName &rarr; attributeValue 对的 Map
	 * @see ModelMap#addAllAttributes(Map)
	 * @see #getModelMap()
	 */
	public ModelAndView addAllObjects(@Nullable Map<String, ?> modelMap) {
		getModelMap().addAllAttributes(modelMap);
		return this;
	}


	/**
	 * 清除此 ModelAndView 对象的状态。
	 * 之后，对象将为空。
	 * <p>可以用于在 HandlerInterceptor 的 {@code postHandle} 方法中抑制给定 ModelAndView 对象的渲染。
	 *
	 * @see #isEmpty()
	 * @see HandlerInterceptor#postHandle
	 */
	public void clear() {
		this.view = null;
		this.model = null;
		this.cleared = true;
	}

	/**
	 * 返回此 ModelAndView 对象是否为空，即它既不持有任何视图，也不包含模型。
	 */
	public boolean isEmpty() {
		return (this.view == null && CollectionUtils.isEmpty(this.model));
	}

	/**
	 * 返回此 ModelAndView 对象是否为空，作为调用 {@link #clear} 后的结果，即它既不持有任何视图，也不包含模型。
	 * <p>如果在调用 {@link #clear} 后添加了任何其他状态到实例中，则返回 {@code false}。
	 *
	 * @see #clear()
	 */
	public boolean wasCleared() {
		return (this.cleared && isEmpty());
	}


	/**
	 * 返回有关此模型和视图的诊断信息。
	 */
	@Override
	public String toString() {
		return "ModelAndView [view=" + formatView() + "; model=" + this.model + "]";
	}

	private String formatView() {
		return isReference() ? "\"" + this.view + "\"" : "[" + this.view + "]";
	}

}
