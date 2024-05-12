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

package org.springframework.web.servlet.mvc.method.annotation;

import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.util.Assert;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.annotation.ModelAttributeMethodProcessor;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.annotation.ModelAndViewResolver;

import java.lang.reflect.Method;
import java.util.List;

/**
 * 此返回值处理程序旨在在所有其他处理程序之后排序，因为它尝试处理 _任何_ 返回值类型（即对所有返回类型返回 {@code true}）。
 *
 * <p>返回值使用 {@link ModelAndViewResolver} 处理，否则将其视为模型属性（如果是非简单类型）。如果这两者都失败（基本上是简单类型而不是 String），则引发 {@link UnsupportedOperationException}。
 *
 * <p><strong>注意：</strong>此类主要用于支持 {@link ModelAndViewResolver}，不幸的是，它无法正确适应 {@link HandlerMethodReturnValueHandler} 契约，因为 {@link HandlerMethodReturnValueHandler#supportsReturnType} 方法无法实现。
 * 因此，{@code ModelAndViewResolver} 仅限于在所有其他返回值处理程序都有机会之后始终被调用。建议将 {@code ModelAndViewResolver} 重新实现为 {@code HandlerMethodReturnValueHandler}，
 * 这样还可以更好地访问返回类型和方法信息。
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class ModelAndViewResolverMethodReturnValueHandler implements HandlerMethodReturnValueHandler {
	/**
	 * 模型与视图解析器列表
	 */
	@Nullable
	private final List<ModelAndViewResolver> mavResolvers;

	/**
	 * 模型属性方法处理器
	 */
	private final ModelAttributeMethodProcessor modelAttributeProcessor =
			new ServletModelAttributeMethodProcessor(true);


	/**
	 * 创建一个新实例。
	 */
	public ModelAndViewResolverMethodReturnValueHandler(@Nullable List<ModelAndViewResolver> mavResolvers) {
		this.mavResolvers = mavResolvers;
	}


	/**
	 * 始终返回 {@code true}。参见类级别注释。
	 */
	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		return true;
	}

	@Override
	public void handleReturnValue(@Nullable Object returnValue, MethodParameter returnType,
								  ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws Exception {

		if (this.mavResolvers != null) {
			// 如果存在模型与视图解析器，则遍历所有的解析器
			for (ModelAndViewResolver mavResolver : this.mavResolvers) {
				// 获取处理器类型和方法
				Class<?> handlerType = returnType.getContainingClass();
				Method method = returnType.getMethod();
				Assert.state(method != null, "No handler method");
				// 获取模型并转换为ExtendedModelMap
				ExtendedModelMap model = (ExtendedModelMap) mavContainer.getModel();
				// 调用解析器解析 模型与视图
				ModelAndView mav = mavResolver.resolveModelAndView(method, handlerType, returnValue, model, webRequest);
				if (mav != ModelAndViewResolver.UNRESOLVED) {
					// 如果解析成功，则设置视图和属性并返回
					mavContainer.addAllAttributes(mav.getModel());
					mavContainer.setViewName(mav.getViewName());
					if (!mav.isReference()) {
						// 如果不是引用，则设置视图
						mavContainer.setView(mav.getView());
					}
					return;
				}
			}
		}

		if (this.modelAttributeProcessor.supportsReturnType(returnType)) {
			// 没有合适的 模型与视图解析器 ，则尝试使用 模型属性方法处理器 处理返回值
			this.modelAttributeProcessor.handleReturnValue(returnValue, returnType, mavContainer, webRequest);
		} else {
			// 抛出不支持的操作异常
			throw new UnsupportedOperationException("Unexpected return type: " +
					returnType.getParameterType().getName() + " in method: " + returnType.getMethod());
		}
	}

}
