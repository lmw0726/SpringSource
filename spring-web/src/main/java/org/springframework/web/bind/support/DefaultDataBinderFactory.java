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

package org.springframework.web.bind.support;

import org.springframework.lang.Nullable;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * 创建一个 {@link WebRequestDataBinder} 实例，并使用 {@link WebBindingInitializer} 进行初始化。
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class DefaultDataBinderFactory implements WebDataBinderFactory {
	/**
	 * Web绑定初始化器
	 */
	@Nullable
	private final WebBindingInitializer initializer;


	/**
	 * 创建一个新的 {@code DefaultDataBinderFactory} 实例。
	 *
	 * @param initializer 用于全局数据绑定器初始化的初始化器（如果没有则为 {@code null}）
	 */
	public DefaultDataBinderFactory(@Nullable WebBindingInitializer initializer) {
		this.initializer = initializer;
	}


	/**
	 * 为给定的目标对象创建一个新的 {@link WebDataBinder} 并通过 {@link WebBindingInitializer} 进行初始化。
	 *
	 * @throws Exception 如果状态或参数无效
	 */
	@Override
	@SuppressWarnings("deprecation")
	public final WebDataBinder createBinder(
			NativeWebRequest webRequest, @Nullable Object target, String objectName) throws Exception {

		// 创建 WebDataBinder 实例
		WebDataBinder dataBinder = createBinderInstance(target, objectName, webRequest);
		if (this.initializer != null) {
			// 如果存在初始化器，则调用初始化器的 initBinder 方法进行初始化
			this.initializer.initBinder(dataBinder, webRequest);
		}
		// 调用 初始化绑定器 方法进行初始化
		initBinder(dataBinder, webRequest);
		// 返回创建的 WebDataBinder 实例
		return dataBinder;
	}

	/**
	 * 创建 WebDataBinder 实例的扩展点。
	 * 默认情况下，这是 {@code WebRequestDataBinder}。
	 *
	 * @param target     绑定目标，如果仅进行类型转换则为 {@code null}
	 * @param objectName 绑定目标对象名称
	 * @param webRequest 当前请求
	 * @throws Exception 如果状态或参数无效
	 */
	protected WebDataBinder createBinderInstance(
			@Nullable Object target, String objectName, NativeWebRequest webRequest) throws Exception {

		return new WebRequestDataBinder(target, objectName);
	}

	/**
	 * 在通过 {@link WebBindingInitializer} 进行“全局”初始化之后，进一步初始化创建的数据绑定器实例的扩展点
	 * （例如，使用 {@code @InitBinder} 方法）。
	 *
	 * @param dataBinder 数据绑定器实例以自定义
	 * @param webRequest 当前请求
	 * @throws Exception 如果初始化失败
	 */
	protected void initBinder(WebDataBinder dataBinder, NativeWebRequest webRequest)
			throws Exception {

	}

}
