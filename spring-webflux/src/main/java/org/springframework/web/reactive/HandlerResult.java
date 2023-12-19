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

package org.springframework.web.reactive;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;

import java.util.function.Function;

/**
 * 表示调用处理程序或处理程序方法的结果。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class HandlerResult {

	/**
	 * 处理程序对象
	 */
	private final Object handler;

	/**
	 * 返回值对象，可为空
	 */
	@Nullable
	private final Object returnValue;

	/**
	 * 可解析类型的返回类型
	 */
	private final ResolvableType returnType;

	/**
	 * 绑定上下文对象
	 */
	private final BindingContext bindingContext;

	/**
	 * 异常处理器函数，可为空
	 */
	@Nullable
	private Function<Throwable, Mono<HandlerResult>> exceptionHandler;


	/**
	 * 创建新的{@code HandlerResult}。
	 *
	 * @param handler     处理请求的处理程序
	 * @param returnValue 处理程序的返回值，可能为{@code null}
	 * @param returnType  返回值类型
	 */
	public HandlerResult(Object handler, @Nullable Object returnValue, MethodParameter returnType) {
		this(handler, returnValue, returnType, null);
	}

	/**
	 * 创建新的{@code HandlerResult}。
	 *
	 * @param handler     处理请求的处理程序
	 * @param returnValue 处理程序的返回值，可能为{@code null}
	 * @param returnType  返回值类型
	 * @param context     用于请求处理的绑定上下文
	 */
	public HandlerResult(Object handler, @Nullable Object returnValue, MethodParameter returnType,
						 @Nullable BindingContext context) {

		Assert.notNull(handler, "'handler' is required");
		Assert.notNull(returnType, "'returnType' is required");
		this.handler = handler;
		this.returnValue = returnValue;
		this.returnType = ResolvableType.forMethodParameter(returnType);
		this.bindingContext = (context != null ? context : new BindingContext());
	}


	/**
	 * 返回处理请求的处理程序。
	 *
	 * @return 处理请求的处理程序
	 */
	public Object getHandler() {
		return this.handler;
	}

	/**
	 * 返回从处理程序返回的值，如果有的话。
	 *
	 * @return 处理程序返回的值
	 */
	@Nullable
	public Object getReturnValue() {
		return this.returnValue;
	}

	/**
	 * 返回从处理程序返回的值的类型，例如控制器方法签名上声明的返回类型。
	 * 还请参见{@link #getReturnTypeSource()}以获取返回类型的底层{@link MethodParameter}。
	 *
	 * @return 处理程序返回值的类型
	 */
	public ResolvableType getReturnType() {
		return this.returnType;
	}

	/**
	 * 返回创建{@link #getReturnType() returnType}的{@link MethodParameter}。
	 *
	 * @return 方法参数
	 */
	public MethodParameter getReturnTypeSource() {
		return (MethodParameter) this.returnType.getSource();
	}

	/**
	 * 返回用于请求处理的BindingContext。
	 *
	 * @return 绑定上下文
	 */
	public BindingContext getBindingContext() {
		return this.bindingContext;
	}

	/**
	 * 返回用于请求处理的模型。这是{@code getBindingContext().getModel()}的快捷方式。
	 *
	 * @return 用于请求处理的模型
	 */
	public Model getModel() {
		return this.bindingContext.getModel();
	}

	/**
	 * 配置可能用于在处理结果失败时生成替代结果的异常处理程序。
	 * 特别是对于异步返回值，在处理程序调用之后可能发生错误。
	 *
	 * @param function 错误处理程序
	 * @return 当前实例
	 */
	public HandlerResult setExceptionHandler(Function<Throwable, Mono<HandlerResult>> function) {
		this.exceptionHandler = function;
		return this;
	}

	/**
	 * 是否存在异常处理程序。
	 *
	 * @return 异常处理程序
	 */
	public boolean hasExceptionHandler() {
		return (this.exceptionHandler != null);
	}

	/**
	 * 应用异常处理程序并返回替代结果。
	 *
	 * @param failure 异常
	 * @return 新结果或如果没有异常处理程序则返回相同的错误
	 */
	public Mono<HandlerResult> applyExceptionHandler(Throwable failure) {
		return (this.exceptionHandler != null ? this.exceptionHandler.apply(failure) : Mono.error(failure));
	}

}
