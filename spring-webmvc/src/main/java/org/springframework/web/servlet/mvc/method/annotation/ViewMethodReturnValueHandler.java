/*
 * Copyright 2002-2019 the original author or authors.
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
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.RequestToViewNameTranslator;
import org.springframework.web.servlet.SmartView;
import org.springframework.web.servlet.View;

/**
 * 处理返回类型为 {@link View} 的返回值。
 *
 * <p>{@code null} 返回值保持不变，由配置的 {@link RequestToViewNameTranslator} 按约定选择视图名称。
 *
 * <p>{@link View} 返回类型具有固定的目的。因此，此处理器应该配置在支持任何带有 {@code @ModelAttribute} 或 {@code @ResponseBody} 注解的返回值类型的处理器之前，
 * 以确保它们不会接管。
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class ViewMethodReturnValueHandler implements HandlerMethodReturnValueHandler {

	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		// 检查返回值类型是否是View及其子类
		return View.class.isAssignableFrom(returnType.getParameterType());
	}

	@Override
	public void handleReturnValue(@Nullable Object returnValue, MethodParameter returnType,
								  ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws Exception {

		if (returnValue instanceof View) {
			// 如果返回值是 View 类型
			View view = (View) returnValue;
			// 向模型和视图容器设置视图
			mavContainer.setView(view);
			if (view instanceof SmartView && ((SmartView) view).isRedirectView()) {
				// 如果视图是 SmartView 类型且为重定向视图，则设置重定向模型场景为 true
				mavContainer.setRedirectModelScenario(true);
			}
		} else if (returnValue != null) {
			// 不应该发生的情况
			throw new UnsupportedOperationException("Unexpected return type: " +
					returnType.getParameterType().getName() + " in method: " + returnType.getMethod());
		}
	}

}
