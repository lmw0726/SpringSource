/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.web.method.support;

import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.validation.support.BindingAwareModelMap;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.bind.support.SimpleSessionStatus;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 记录 {@link HandlerMethodArgumentResolver} 和 {@link HandlerMethodReturnValueHandler}
 * 在调用控制器方法过程中做出的模型和视图相关决策。
 *
 * <p>{@link #setRequestHandled} 标志可用于指示请求已直接处理，不需要视图解析。
 *
 * <p>在实例化时，会自动创建一个默认的 {@link Model}。在重定向场景中，可以通过 {@link #setRedirectModel}
 * 提供替代的模型实例。当 {@link #setRedirectModelScenario} 设置为 {@code true}，表示重定向场景，
 * {@link #getModel()} 将返回重定向模型而不是默认模型。
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 3.1
 */
public class ModelAndViewContainer {
	/**
	 * 重定向时忽略默认模型
	 */
	private boolean ignoreDefaultModelOnRedirect = false;

	/**
	 * 视图名称或 视图
	 */
	@Nullable
	private Object view;

	/**
	 * 默认模型
	 */
	private final ModelMap defaultModel = new BindingAwareModelMap();
	/**
	 * 重定向模型
	 */
	@Nullable
	private ModelMap redirectModel;

	/**
	 * 控制器是否返回了重定向指令
	 */
	private boolean redirectModelScenario = false;

	/**
	 * 配置的 HTTP 状态
	 */
	@Nullable
	private HttpStatus status;
	/**
	 * 未绑定的属性名称集合
	 */
	private final Set<String> noBinding = new HashSet<>(4);

	/**
	 * 禁止进行数据绑定的属性名称集合
	 */
	private final Set<String> bindingDisabled = new HashSet<>(4);

	/**
	 * 会话状态
	 */
	private final SessionStatus sessionStatus = new SimpleSessionStatus();

	/**
	 * 是否请求已完全在处理程序内处理
	 */
	private boolean requestHandled = false;


	/**
	 * 默认情况下，在呈现和重定向场景中都使用“默认”模型的内容。或者，控制器方法可以声明类型为
	 * {@code RedirectAttributes} 的参数，并使用它来提供属性以准备重定向 URL。
	 * 将此标志设置为 {@code true} 可确保在重定向场景中永远不使用“默认”模型，即使未声明
	 * RedirectAttributes 参数也是如此。将其设置为 {@code false} 意味着如果控制器方法
	 * 没有声明 RedirectAttributes 参数，则“默认”模型可能在重定向中使用。
	 * 默认设置为 {@code false}。
	 */
	public void setIgnoreDefaultModelOnRedirect(boolean ignoreDefaultModelOnRedirect) {
		this.ignoreDefaultModelOnRedirect = ignoreDefaultModelOnRedirect;
	}

	/**
	 * 设置由 DispatcherServlet 通过 ViewResolver 解析的视图名称。
	 * 将覆盖任何现有的视图名称或 View。
	 */
	public void setViewName(@Nullable String viewName) {
		this.view = viewName;
	}

	/**
	 * 返回由 DispatcherServlet 通过 ViewResolver 解析的视图名称，如果设置了 View 对象，则返回 {@code null}。
	 */
	@Nullable
	public String getViewName() {
		return (this.view instanceof String ? (String) this.view : null);
	}

	/**
	 * 设置要由 DispatcherServlet 使用的 View 对象。
	 * 将覆盖任何现有的视图名称或 View。
	 */
	public void setView(@Nullable Object view) {
		this.view = view;
	}

	/**
	 * 返回 View 对象，如果使用视图名称由 DispatcherServlet 通过 ViewResolver 解析，则返回 {@code null}。
	 */
	@Nullable
	public Object getView() {
		return this.view;
	}

	/**
	 * 查看视图是否是通过名称指定的视图引用，由 DispatcherServlet 通过 ViewResolver 解析。
	 */
	public boolean isViewReference() {
		return (this.view instanceof String);
	}

	/**
	 * 返回要使用的模型，即“默认”模型或“重定向”模型。如果 {@code redirectModelScenario=false}，
	 * 或者没有重定向模型（即未将 RedirectAttributes 声明为方法参数）并且
	 * {@code ignoreDefaultModelOnRedirect=false}，则使用默认模型。
	 */
	public ModelMap getModel() {
		// 如果使用默认模型，则返回默认模型
		if (useDefaultModel()) {
			return this.defaultModel;
		} else {
			// 否则，如果重定向模型为空，则创建一个新的重定向模型
			if (this.redirectModel == null) {
				this.redirectModel = new ModelMap();
			}
			return this.redirectModel;
		}
	}

	/**
	 * 是否使用默认模型或重定向模型。
	 */
	private boolean useDefaultModel() {
		return (!this.redirectModelScenario || (this.redirectModel == null && !this.ignoreDefaultModelOnRedirect));
	}

	/**
	 * 返回实例化时创建的“默认”模型。
	 * <p>通常建议使用 {@link #getModel()}，它返回“默认”模型（模板呈现）或“重定向”模型（重定向 URL 准备）。
	 * 在需要访问“默认”模型的情况下可能需要使用此方法，例如，以保存通过 {@code @SessionAttributes} 指定的模型属性。
	 *
	 * @return 默认模型（永不为 {@code null}）
	 * @since 4.1.4
	 */
	public ModelMap getDefaultModel() {
		return this.defaultModel;
	}

	/**
	 * 提供一个单独的模型实例，在重定向场景中使用。
	 * <p>但是，除非将 {@link #setRedirectModelScenario} 设置为 {@code true} 以表示实际的重定向场景，
	 * 否则不会使用提供的附加模型。
	 */
	public void setRedirectModel(ModelMap redirectModel) {
		this.redirectModel = redirectModel;
	}

	/**
	 * 控制器是否返回了重定向指令，例如以“redirect:”前缀的视图名称、RedirectView 实例等。
	 */
	public void setRedirectModelScenario(boolean redirectModelScenario) {
		this.redirectModelScenario = redirectModelScenario;
	}

	/**
	 * 提供一个 HTTP 状态，将在用于视图呈现目的的 {@code ModelAndView} 中传递。
	 *
	 * @since 4.3
	 */
	public void setStatus(@Nullable HttpStatus status) {
		this.status = status;
	}

	/**
	 * 返回配置的 HTTP 状态，如果有的话。
	 *
	 * @since 4.3
	 */
	@Nullable
	public HttpStatus getStatus() {
		return this.status;
	}

	/**
	 * 以编程方式注册一个不应进行数据绑定的属性，即使对于后续的 {@code @ModelAttribute} 声明也是如此。
	 *
	 * @param attributeName 属性的名称
	 * @since 4.3
	 */
	public void setBindingDisabled(String attributeName) {
		this.bindingDisabled.add(attributeName);
	}

	/**
	 * 返回给定模型属性是否已禁用绑定。
	 *
	 * @since 4.3
	 */
	public boolean isBindingDisabled(String name) {
		return (this.bindingDisabled.contains(name) || this.noBinding.contains(name));
	}

	/**
	 * 注册对应模型属性的数据绑定是否应该发生，对应于 {@code @ModelAttribute(binding=true/false)} 声明。
	 * <p>注意：虽然此标志将被 {@link #isBindingDisabled} 考虑，但是硬性的 {@link #setBindingDisabled} 声明始终会覆盖它。
	 *
	 * @param attributeName 属性的名称
	 * @since 4.3.13
	 */
	public void setBinding(String attributeName, boolean enabled) {
		// 如果未启用，则将属性名称添加到不绑定集合中
		if (!enabled) {
			this.noBinding.add(attributeName);
		} else {
			// 否则，从不绑定集合中移除属性名称
			this.noBinding.remove(attributeName);
		}
	}

	/**
	 * 返回可用于发出会话处理已完成的信号的 {@link SessionStatus} 实例。
	 */
	public SessionStatus getSessionStatus() {
		return this.sessionStatus;
	}

	/**
	 * 是否请求已完全在处理程序内处理，例如{@code @ResponseBody} 方法，因此不需要视图解析。当控制器方法声明类型为
	 * {@code ServletResponse} 或 {@code OutputStream} 的参数时，也可以设置此标志）。
	 * <p>默认值为 {@code false}。
	 */
	public void setRequestHandled(boolean requestHandled) {
		this.requestHandled = requestHandled;
	}

	/**
	 * 是否请求已完全在处理程序内处理。
	 */
	public boolean isRequestHandled() {
		return this.requestHandled;
	}

	/**
	 * 将提供的属性添加到底层模型中。
	 * {@code getModel().addAttribute(String, Object)} 的快捷方式。
	 */
	public ModelAndViewContainer addAttribute(String name, @Nullable Object value) {
		getModel().addAttribute(name, value);
		return this;
	}

	/**
	 * 将提供的属性添加到底层模型中。
	 * {@code getModel().addAttribute(Object)} 的快捷方式。
	 */
	public ModelAndViewContainer addAttribute(Object value) {
		getModel().addAttribute(value);
		return this;
	}

	/**
	 * 将所有属性复制到底层模型中。
	 * {@code getModel().addAllAttributes(Map)} 的快捷方式。
	 */
	public ModelAndViewContainer addAllAttributes(@Nullable Map<String, ?> attributes) {
		getModel().addAllAttributes(attributes);
		return this;
	}

	/**
	 * 用给定的 {@code Map} 中的属性复制到底层模型中，同名的现有对象优先（即不会被替换）。
	 * {@code getModel().mergeAttributes(Map<String, ?>)} 的快捷方式。
	 */
	public ModelAndViewContainer mergeAttributes(@Nullable Map<String, ?> attributes) {
		getModel().mergeAttributes(attributes);
		return this;
	}

	/**
	 * 从模型中移除给定的属性。
	 */
	public ModelAndViewContainer removeAttributes(@Nullable Map<String, ?> attributes) {
		// 如果属性不为 null，则遍历属性键集合
		if (attributes != null) {
			for (String key : attributes.keySet()) {
				// 从模型中移除属性
				getModel().remove(key);
			}
		}
		// 返回当前实例
		return this;
	}

	/**
	 * 底层模型是否包含给定的属性名。
	 * {@code getModel().containsAttribute(String)} 的快捷方式。
	 */
	public boolean containsAttribute(String name) {
		return getModel().containsAttribute(name);
	}


	/**
	 * 返回诊断信息。
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("ModelAndViewContainer: ");
		if (!isRequestHandled()) {
			if (isViewReference()) {
				sb.append("reference to view with name '").append(this.view).append('\'');
			} else {
				sb.append("View is [").append(this.view).append(']');
			}
			if (useDefaultModel()) {
				sb.append("; default model ");
			} else {
				sb.append("; redirect model ");
			}
			sb.append(getModel());
		} else {
			sb.append("Request handled directly");
		}
		return sb.toString();
	}

}
