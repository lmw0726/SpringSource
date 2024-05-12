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

package org.springframework.web.servlet.mvc.method.annotation;

import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.util.PatternMatchUtils;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.SmartView;
import org.springframework.web.servlet.View;

/**
 * 处理 {@link ModelAndView} 类型的返回值，将视图和模型信息复制到 {@link ModelAndViewContainer} 中。
 *
 * <p>如果返回值为 {@code null}，则将 {@link ModelAndViewContainer#setRequestHandled(boolean)} 标志设置为 {@code true}，
 * 表示请求已直接处理。
 *
 * <p>{@link ModelAndView} 返回类型有一定的目的。因此，应在支持任何带有 {@code @ModelAttribute} 或 {@code @ResponseBody} 注释的返回值类型的处理程序之前配置此处理程序，以确保它们不会接管。
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class ModelAndViewMethodReturnValueHandler implements HandlerMethodReturnValueHandler {
	/**
	 * 重定向路径模式列表
	 */
	@Nullable
	private String[] redirectPatterns;


	/**
	 * 配置一个或多个简单模式（如 {@link PatternMatchUtils#simpleMatch} 中所述），以便识别自定义重定向前缀，除了 "redirect:" 之外。
	 * <p>请注意，仅配置此属性不会使自定义重定向前缀生效。必须还有一个自定义的 {@link View} 来识别该前缀。
	 *
	 * @since 4.1
	 */
	public void setRedirectPatterns(@Nullable String... redirectPatterns) {
		this.redirectPatterns = redirectPatterns;
	}

	/**
	 * 如果有的话，返回配置的重定向模式。
	 *
	 * @since 4.1
	 */
	@Nullable
	public String[] getRedirectPatterns() {
		return this.redirectPatterns;
	}


	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		// 检查返回值类型是否是 ModelAndView 及其子类
		return ModelAndView.class.isAssignableFrom(returnType.getParameterType());
	}

	@Override
	public void handleReturnValue(@Nullable Object returnValue, MethodParameter returnType,
								  ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws Exception {

		if (returnValue == null) {
			// 如果返回值为null，表示请求处理已完成，设置标志位并返回
			mavContainer.setRequestHandled(true);
			return;
		}

		// 将返回值强制转换为ModelAndView
		ModelAndView mav = (ModelAndView) returnValue;

		if (mav.isReference()) {
			// 如果ModelAndView是引用，则获取视图名并设置到 模型与视图容器 中
			String viewName = mav.getViewName();
			mavContainer.setViewName(viewName);
			if (viewName != null && isRedirectViewName(viewName)) {
				// 如果视图名不为空并且是重定向视图名，则设置重定向模型场景
				mavContainer.setRedirectModelScenario(true);
			}
		} else {
			// 如果ModelAndView不是引用，则获取视图并设置到 模型与视图容器 中
			View view = mav.getView();
			mavContainer.setView(view);
			if (view instanceof SmartView && ((SmartView) view).isRedirectView()) {
				// 如果视图是SmartView并且是重定向视图，则设置重定向模型场景
				mavContainer.setRedirectModelScenario(true);
			}
		}

		// 设置响应状态码
		mavContainer.setStatus(mav.getStatus());
		// 添加所有的属性到 模型与视图容器 中
		mavContainer.addAllAttributes(mav.getModel());
	}

	/**
	 * 判断给定的视图名称是否是重定向视图引用。
	 * 默认实现检查配置的重定向模式，以及视图名称是否以 "redirect:" 前缀开头。
	 *
	 * @param viewName 要检查的视图名称，永远不会为 {@code null}
	 * @return 如果给定的视图名称被认为是重定向视图引用，则为 "true"；否则为 "false"
	 */
	protected boolean isRedirectViewName(String viewName) {
		// 检查视图名称是否与重定向模式匹配，如果匹配或者以 "redirect:" 开头，则返回 true，否则返回 false。
		return (PatternMatchUtils.simpleMatch(this.redirectPatterns, viewName) || viewName.startsWith("redirect:"));
	}

}
