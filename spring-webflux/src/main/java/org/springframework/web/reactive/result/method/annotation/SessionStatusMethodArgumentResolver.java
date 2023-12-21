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

package org.springframework.web.reactive.result.method.annotation;

import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.result.method.SyncHandlerMethodArgumentResolver;
import org.springframework.web.server.ServerWebExchange;

/**
 * 用于从 {@link BindingContext} 中获取 {@link SessionStatus} 参数的解析器。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class SessionStatusMethodArgumentResolver implements SyncHandlerMethodArgumentResolver {

	/**
	 * 检查是否支持给定的参数类型
	 *
	 * @param parameter 方法参数
	 * @return true表示该解析器支持方法参数
	 */
	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return SessionStatus.class == parameter.getParameterType();
	}

	/**
	 * 解析参数值
	 *
	 * @param parameter      方法参数
	 * @param bindingContext 要使用的绑定上下文
	 * @param exchange       当前交换
	 * @return 解析好的SessionStatus对象
	 */
	@Nullable
	@Override
	public Object resolveArgumentValue(
			MethodParameter parameter, BindingContext bindingContext, ServerWebExchange exchange) {

		Assert.isInstanceOf(InitBinderBindingContext.class, bindingContext);
		// 从绑定上下文中获取 SessionStatus
		return ((InitBinderBindingContext) bindingContext).getSessionStatus();
	}

}
