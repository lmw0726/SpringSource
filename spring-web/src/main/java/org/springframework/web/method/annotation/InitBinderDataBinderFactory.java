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

package org.springframework.web.method.annotation;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.support.DefaultDataBinderFactory;
import org.springframework.web.bind.support.WebBindingInitializer;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.support.InvocableHandlerMethod;

import java.util.Collections;
import java.util.List;

/**
 * 通过 {@code @InitBinder} 方法向 WebDataBinder 添加初始化。
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class InitBinderDataBinderFactory extends DefaultDataBinderFactory {

	/**
	 * @InitBinder标记的绑定方法
	 */
	private final List<InvocableHandlerMethod> binderMethods;


	/**
	 * 创建一个新的 InitBinderDataBinderFactory 实例。
	 *
	 * @param binderMethods {@code @InitBinder} 方法
	 * @param initializer   用于全局数据绑定器初始化的初始化器
	 */
	public InitBinderDataBinderFactory(@Nullable List<InvocableHandlerMethod> binderMethods,
									   @Nullable WebBindingInitializer initializer) {

		super(initializer);
		this.binderMethods = (binderMethods != null ? binderMethods : Collections.emptyList());
	}


	/**
	 * 使用 {@code @InitBinder} 方法初始化 WebDataBinder。
	 * <p>如果 {@code @InitBinder} 注解指定了属性名称，则仅当名称包含目标对象名称时才调用它。
	 *
	 * @throws Exception 如果调用的任何 @{@link InitBinder} 方法失败
	 * @see #isBinderMethodApplicable
	 */
	@Override
	public void initBinder(WebDataBinder dataBinder, NativeWebRequest request) throws Exception {
		// 遍历所有 @InitBinder 绑定的方法
		for (InvocableHandlerMethod binderMethod : this.binderMethods) {
			// 如果当前 绑定方法 适用于给定的 WebDataBinder，则执行该方法
			if (isBinderMethodApplicable(binderMethod, dataBinder)) {
				// 调用 绑定方法，并传递请求和数据绑定器作为参数
				Object returnValue = binderMethod.invokeForRequest(request, null, dataBinder);
				// 如果 绑定方法 返回值不为空，则抛出异常，因为 @InitBinder 绑定的方法应该返回 void
				if (returnValue != null) {
					throw new IllegalStateException(
							"@InitBinder methods must not return a value (should be void): " + binderMethod);
				}
			}
		}
	}

	/**
	 * 确定给定的 {@code @InitBinder} 方法是否应用于初始化给定的 {@link WebDataBinder} 实例。
	 * 默认情况下，我们检查注解值中指定的属性名称（如果有）。
	 */
	protected boolean isBinderMethodApplicable(HandlerMethod initBinderMethod, WebDataBinder dataBinder) {
		// 获取 @InitBinder 注解
		InitBinder ann = initBinderMethod.getMethodAnnotation(InitBinder.class);
		// 确保 @InitBinder 注解不为空
		Assert.state(ann != null, "No InitBinder annotation");
		// 获取 @InitBinder 注解中的 value 属性值
		String[] names = ann.value();
		// 返回值为真如果 value 属性值为空，或者对象名包含在 value 属性值中
		return (ObjectUtils.isEmpty(names) || ObjectUtils.containsElement(names, dataBinder.getObjectName()));
	}

}
