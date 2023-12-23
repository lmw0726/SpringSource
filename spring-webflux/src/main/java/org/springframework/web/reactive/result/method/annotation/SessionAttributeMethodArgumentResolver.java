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

package org.springframework.web.reactive.result.method.annotation;

import reactor.core.publisher.Mono;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.bind.annotation.ValueConstants;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;

/**
 * 解析使用 @{@link SessionAttribute} 注解的方法参数。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see RequestAttributeMethodArgumentResolver
 */
public class SessionAttributeMethodArgumentResolver extends AbstractNamedValueArgumentResolver {

	public SessionAttributeMethodArgumentResolver(ConfigurableBeanFactory factory, ReactiveAdapterRegistry registry) {
		super(factory, registry);
	}

	/**
	 * 检查方法参数是否支持。
	 * @param parameter 方法参数
	 * @return 如果方法参数带有 SessionAttribute 注解，则返回 true，否则返回 false
	 */
	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		// 检查方法参数是否带有 SessionAttribute 注解
		return parameter.hasParameterAnnotation(SessionAttribute.class);
	}

	/**
	 * 创建命名值信息。
	 * @param parameter 方法参数
	 * @return 命名值信息对象
	 * @throws IllegalStateException 如果不存在 SessionAttribute 注解，则抛出 IllegalStateException 异常
	 */
	@Override
	protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
		SessionAttribute ann = parameter.getParameterAnnotation(SessionAttribute.class);
		Assert.state(ann != null, "No SessionAttribute annotation");
		return new NamedValueInfo(ann.name(), ann.required(), ValueConstants.DEFAULT_NONE);
	}

	/**
	 * 解析名称。
	 * @param name 名称
	 * @param parameter 方法参数
	 * @param exchange 服务器网络交换对象
	 * @return 返回包含对象的 Mono（单个元素的响应式序列），如果会话中存在具有给定名称的属性，则返回该属性，否则返回空
	 */
	@Override
	protected Mono<Object> resolveName(String name, MethodParameter parameter, ServerWebExchange exchange) {
		return exchange.getSession()
				.filter(session -> session.getAttribute(name) != null)
				.map(session -> session.getAttribute(name));
	}

	/**
	 * 处理缺失值情况。
	 * @param name 名称
	 * @param parameter 方法参数
	 * @throws ServerWebInputException 抛出输入异常，指明缺失的会话属性名称及其类型
	 */
	@Override
	protected void handleMissingValue(String name, MethodParameter parameter) {
		String type = parameter.getNestedParameterType().getSimpleName();
		String reason = "Missing session attribute '" + name + "' of type " + type;
		throw new ServerWebInputException(reason, parameter);
	}

}
