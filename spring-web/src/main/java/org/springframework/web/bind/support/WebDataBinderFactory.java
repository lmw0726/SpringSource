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
 * 用于为指定名称的目标对象创建 {@link WebDataBinder} 实例的工厂。
 *
 * @author Arjen Poutsma
 * @since 3.1
 */
public interface WebDataBinderFactory {

	/**
	 * 为给定对象创建一个 {@link WebDataBinder}。
	 *
	 * @param webRequest 当前请求
	 * @param target     要为其创建数据绑定器的对象，如果要为简单类型创建绑定器，则为 {@code null}
	 * @param objectName 目标对象的名称
	 * @return 创建的 {@link WebDataBinder} 实例，永远不会为 null
	 * @throws Exception 如果数据绑定器的创建和初始化失败时抛出
	 */
	WebDataBinder createBinder(NativeWebRequest webRequest, @Nullable Object target, String objectName)
			throws Exception;

}
