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

package org.springframework.web.method.support;

import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 处理方法返回值，委托给一组已注册的 {@link HandlerMethodReturnValueHandler HandlerMethodReturnValueHandlers}。
 * 为了更快的查找，先前已解析的返回类型被缓存起来。
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class HandlerMethodReturnValueHandlerComposite implements HandlerMethodReturnValueHandler {
	/**
	 * 处理方法返回值处理器列表
	 */
	private final List<HandlerMethodReturnValueHandler> returnValueHandlers = new ArrayList<>();


	/**
	 * 返回已注册处理程序的只读列表，如果没有则返回空列表。
	 */
	public List<HandlerMethodReturnValueHandler> getHandlers() {
		return Collections.unmodifiableList(this.returnValueHandlers);
	}

	/**
	 * 检查给定的 {@linkplain MethodParameter 方法返回类型} 是否被任何已注册的 {@link HandlerMethodReturnValueHandler} 支持。
	 */
	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		return getReturnValueHandler(returnType) != null;
	}

	@Nullable
	private HandlerMethodReturnValueHandler getReturnValueHandler(MethodParameter returnType) {
		// 遍历所有的 返回值处理器
		for (HandlerMethodReturnValueHandler handler : this.returnValueHandlers) {
			// 如果当前处理器支持返回类型，则返回该处理器
			if (handler.supportsReturnType(returnType)) {
				return handler;
			}
		}
		// 如果没有找到支持的处理器，则返回 null
		return null;
	}

	/**
	 * 迭代已注册的 {@link HandlerMethodReturnValueHandler HandlerMethodReturnValueHandlers} 并调用支持它的处理程序。
	 *
	 * @throws IllegalStateException 如果找不到合适的 {@link HandlerMethodReturnValueHandler}。
	 */
	@Override
	public void handleReturnValue(@Nullable Object returnValue, MethodParameter returnType,
								  ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws Exception {

		// 选择处理返回值的处理器
		HandlerMethodReturnValueHandler handler = selectHandler(returnValue, returnType);
		if (handler == null) {
			// 如果未找到处理器，则抛出异常
			throw new IllegalArgumentException("Unknown return value type: " + returnType.getParameterType().getName());
		}
		// 使用选择的处理器处理返回值
		handler.handleReturnValue(returnValue, returnType, mavContainer, webRequest);
	}

	@Nullable
	private HandlerMethodReturnValueHandler selectHandler(@Nullable Object value, MethodParameter returnType) {
		// 检查返回值是否为异步
		boolean isAsyncValue = isAsyncReturnValue(value, returnType);
		// 遍历处理返回值的处理器列表
		for (HandlerMethodReturnValueHandler handler : this.returnValueHandlers) {
			// 如果返回值为异步，但处理器不支持异步处理，则跳过
			if (isAsyncValue && !(handler instanceof AsyncHandlerMethodReturnValueHandler)) {
				continue;
			}
			if (handler.supportsReturnType(returnType)) {
				// 如果处理器支持当前返回值类型，则返回该处理器
				return handler;
			}
		}
		// 如果未找到匹配的处理器，则返回 null
		return null;
	}

	private boolean isAsyncReturnValue(@Nullable Object value, MethodParameter returnType) {
		// 遍历处理返回值的处理器列表
		for (HandlerMethodReturnValueHandler handler : this.returnValueHandlers) {
			if (handler instanceof AsyncHandlerMethodReturnValueHandler &&
					((AsyncHandlerMethodReturnValueHandler) handler).isAsyncReturnValue(value, returnType)) {
				// 如果处理器是异步处理器，并且返回值与返回类型匹配，则返回 true
				return true;
			}
		}
		// 如果未找到匹配的异步处理器，则返回 false
		return false;
	}

	/**
	 * 添加给定的 {@link HandlerMethodReturnValueHandler}。
	 */
	public HandlerMethodReturnValueHandlerComposite addHandler(HandlerMethodReturnValueHandler handler) {
		this.returnValueHandlers.add(handler);
		return this;
	}

	/**
	 * 添加给定的 {@link HandlerMethodReturnValueHandler HandlerMethodReturnValueHandlers}。
	 */
	public HandlerMethodReturnValueHandlerComposite addHandlers(
			@Nullable List<? extends HandlerMethodReturnValueHandler> handlers) {

		if (handlers != null) {
			this.returnValueHandlers.addAll(handlers);
		}
		return this;
	}

}
