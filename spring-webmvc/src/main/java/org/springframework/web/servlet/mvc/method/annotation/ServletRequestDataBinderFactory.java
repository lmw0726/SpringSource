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

import org.springframework.lang.Nullable;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.bind.support.WebBindingInitializer;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.annotation.InitBinderDataBinderFactory;
import org.springframework.web.method.support.InvocableHandlerMethod;

import java.util.List;

/**
 * 创建一个 {@code ServletRequestDataBinder}。
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class ServletRequestDataBinderFactory extends InitBinderDataBinderFactory {

	/**
	 * 创建一个新实例。
	 *
	 * @param binderMethods 一个或多个 {@code @InitBinder} 方法
	 * @param initializer   提供全局数据绑定器初始化
	 */
	public ServletRequestDataBinderFactory(@Nullable List<InvocableHandlerMethod> binderMethods,
										   @Nullable WebBindingInitializer initializer) {

		super(binderMethods, initializer);
	}

	/**
	 * 返回一个 {@link ExtendedServletRequestDataBinder} 实例。
	 */
	@Override
	protected ServletRequestDataBinder createBinderInstance(
			@Nullable Object target, String objectName, NativeWebRequest request) throws Exception {

		return new ExtendedServletRequestDataBinder(target, objectName);
	}

}
