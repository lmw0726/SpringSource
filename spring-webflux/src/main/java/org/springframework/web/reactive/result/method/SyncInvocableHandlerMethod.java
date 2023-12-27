/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.web.reactive.result.method;

import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.lang.Nullable;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.server.ServerErrorException;
import org.springframework.web.server.ServerWebExchange;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * {@link HandlerMethod}的扩展，通过{@link InvocableHandlerMethod}调用基础方法，但仅使用同步参数解析器，
 * 因此可以直接返回带有{@link HandlerResult}的结果，而无需使用异步包装器。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class SyncInvocableHandlerMethod extends HandlerMethod {

	private final InvocableHandlerMethod delegate;


	public SyncInvocableHandlerMethod(HandlerMethod handlerMethod) {
		super(handlerMethod);
		this.delegate = new InvocableHandlerMethod(handlerMethod);
	}

	public SyncInvocableHandlerMethod(Object bean, Method method) {
		super(bean, method);
		this.delegate = new InvocableHandlerMethod(bean, method);
	}


	/**
	 * 配置用于根据ServerWebExchange解析方法参数值的参数解析器。
	 *
	 * @param resolvers 用于解析方法参数值的参数解析器列表
	 */
	public void setArgumentResolvers(List<SyncHandlerMethodArgumentResolver> resolvers) {
		this.delegate.setArgumentResolvers(resolvers);
	}

	/**
	 * 返回已配置的参数解析器。
	 *
	 * @return 已配置的参数解析器列表
	 */
	public List<SyncHandlerMethodArgumentResolver> getResolvers() {
		return this.delegate.getResolvers().stream()
				.map(resolver -> (SyncHandlerMethodArgumentResolver) resolver)
				.collect(Collectors.toList());
	}

	/**
	 * 设置ParameterNameDiscoverer，用于在需要时解析参数名称（例如默认请求属性名称）。
	 * <p>默认为DefaultParameterNameDiscoverer。
	 *
	 * @param nameDiscoverer 要设置的ParameterNameDiscoverer
	 */
	public void setParameterNameDiscoverer(ParameterNameDiscoverer nameDiscoverer) {
		this.delegate.setParameterNameDiscoverer(nameDiscoverer);
	}

	/**
	 * 返回配置的参数名称发现器。
	 *
	 * @return 参数名称发现器
	 */
	public ParameterNameDiscoverer getParameterNameDiscoverer() {
		return this.delegate.getParameterNameDiscoverer();
	}


	/**
	 * 调用给定的交换方法。
	 *
	 * @param exchange       当前的交换
	 * @param bindingContext 要使用的绑定上下文
	 * @param providedArgs   可选的参数值列表，按类型匹配
	 * @return 包含{@link HandlerResult}的Mono
	 * @throws ServerErrorException 如果方法参数解析或方法调用失败
	 */
	@Nullable
	public HandlerResult invokeForHandlerResult(ServerWebExchange exchange,
												BindingContext bindingContext, Object... providedArgs) {

		// 使用委托的 invoke 方法调用处理器方法并转换为 CompletableFuture
		CompletableFuture<HandlerResult> future = this.delegate.invoke(exchange, bindingContext, providedArgs).toFuture();

		// 如果 CompletableFuture 没有完成，则抛出异常，因为 SyncInvocableHandlerMethod 应该同步完成
		if (!future.isDone()) {
			throw new IllegalStateException(
					"SyncInvocableHandlerMethod should have completed synchronously.");
		}

		Throwable failure;
		try {
			// 尝试获取 CompletableFuture 的结果，捕获可能的异常
			return future.get();
		} catch (ExecutionException ex) {
			failure = ex.getCause();
		} catch (InterruptedException ex) {
			failure = ex;
		}
		// 抛出服务器错误异常，指示调用失败
		throw (new ServerErrorException(
				"Failed to invoke: " + getShortLogMessage(), getMethod(), failure));
	}

}
