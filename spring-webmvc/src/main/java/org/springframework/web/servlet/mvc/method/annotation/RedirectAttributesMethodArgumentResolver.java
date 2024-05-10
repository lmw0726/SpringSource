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
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.util.Assert;
import org.springframework.validation.DataBinder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.Map;

/**
 * 用于解析类型为 {@link RedirectAttributes} 的方法参数。
 *
 * <p>此解析器必须在 {@link org.springframework.web.method.annotation.ModelMethodProcessor}
 * 和 {@link org.springframework.web.method.annotation.MapMethodProcessor} 之前列出，
 * 这两者都支持 {@link Map} 和 {@link Model} 参数，而这两者都是 {@code RedirectAttributes} 的“超”类型，
 * 并且还会尝试解析 {@code RedirectAttributes} 参数。
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class RedirectAttributesMethodArgumentResolver implements HandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		// 检查方法参数类型是否是 RedirectAttributes 及其实现类
		return RedirectAttributes.class.isAssignableFrom(parameter.getParameterType());
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer,
								  NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) throws Exception {

		Assert.state(mavContainer != null, "RedirectAttributes argument only supported on regular handler methods");

		// 创建重定向属性对象
		ModelMap redirectAttributes;
		if (binderFactory != null) {
			// 如果 Web数据绑定工厂 不为空，则创建数据绑定器
			DataBinder dataBinder = binderFactory.createBinder(webRequest, null, DataBinder.DEFAULT_OBJECT_NAME);
			// 使用其创建重定向属性模型映射
			redirectAttributes = new RedirectAttributesModelMap(dataBinder);
		} else {
			// 否则，直接创建重定向属性模型映射
			redirectAttributes = new RedirectAttributesModelMap();
		}
		// 将重定向属性设置到 模型和视图容器 中
		mavContainer.setRedirectModel(redirectAttributes);
		// 返回重定向属性对象
		return redirectAttributes;
	}

}
