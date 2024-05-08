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

package org.springframework.web.servlet.mvc.method;

import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerMethodMappingNamingStrategy;

/**
 * {@code RequestMappingInfo} 基于的处理程序方法映射的
 * {@link org.springframework.web.servlet.handler.HandlerMethodMappingNamingStrategy HandlerMethodMappingNamingStrategy}。
 * <p>
 * 如果设置了 {@code RequestMappingInfo} 的名称属性，则使用其值。
 * 否则，名称基于类名的大写字母，后跟 "#" 作为分隔符，然后是方法名。例如，
 * 对于名为 TestController，具有 getFoo 方法的类，名称为 "TC#getFoo"。
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public class RequestMappingInfoHandlerMethodMappingNamingStrategy
		implements HandlerMethodMappingNamingStrategy<RequestMappingInfo> {

	/**
	 * 类型和方法级别部分之间的分隔符。
	 */
	public static final String SEPARATOR = "#";


	@Override
	public String getName(HandlerMethod handlerMethod, RequestMappingInfo mapping) {
		// 如果映射的名称不为 null，则直接返回该名称
		if (mapping.getName() != null) {
			return mapping.getName();
		}
		// 否则构建一个默认名称
		StringBuilder sb = new StringBuilder();
		// 获取简单类型名称
		String simpleTypeName = handlerMethod.getBeanType().getSimpleName();
		for (int i = 0; i < simpleTypeName.length(); i++) {
			if (Character.isUpperCase(simpleTypeName.charAt(i))) {
				// 如果简单类型名称的某个字符是大写字母
				// 则将其拼接到字符串构建器中
				sb.append(simpleTypeName.charAt(i));
			}
		}
		// 使用分隔符分隔 处理器方法 的方法名称，并拼接到字符串构建器中
		sb.append(SEPARATOR).append(handlerMethod.getMethod().getName());
		// 返回构建好的字符串作为默认名称
		return sb.toString();
	}

}
