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

package org.springframework.web.servlet.handler;

import org.springframework.web.method.HandlerMethod;

/**
 * 用于为处理程序方法映射分配名称的策略。
 *
 * <p>该策略可以配置在
 * {@link org.springframework.web.servlet.handler.AbstractHandlerMethodMapping
 * AbstractHandlerMethodMapping} 上。它用于为每个注册的处理程序方法分配名称。
 * 然后可以通过
 * {@link org.springframework.web.servlet.handler.AbstractHandlerMethodMapping#getHandlerMethodsForMappingName(String)
 * AbstractHandlerMethodMapping#getHandlerMethodsForMappingName} 查询名称。
 *
 * <p>应用程序可以通过静态方法
 * {@link org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder#fromMappingName(String)
 * MvcUriComponentsBuilder#fromMappingName} 构建到控制器方法的 URL，或者在 JSP 中通过 Spring 标签库注册的 "mvcUrl" 函数中构建。
 *
 * @param <T> 映射类型
 * @author Rossen Stoyanchev
 * @since 4.1
 */
@FunctionalInterface
public interface HandlerMethodMappingNamingStrategy<T> {

	/**
	 * 确定给定的 处理方法 和映射的名称。
	 *
	 * @param handlerMethod 处理程序方法
	 * @param mapping       映射
	 * @return 名称
	 */
	String getName(HandlerMethod handlerMethod, T mapping);

}
