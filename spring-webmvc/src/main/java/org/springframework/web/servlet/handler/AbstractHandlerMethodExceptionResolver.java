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

package org.springframework.web.servlet.handler;

import org.springframework.lang.Nullable;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 支持处理类型为 {@link HandlerMethod} 的处理器引发的异常的
 * {@link org.springframework.web.servlet.HandlerExceptionResolver HandlerExceptionResolver} 实现的抽象基类。
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public abstract class AbstractHandlerMethodExceptionResolver extends AbstractHandlerExceptionResolver {

	/**
	 * 检查处理器是否为 {@link HandlerMethod}，然后委托给 {@code #shouldApplyTo(HttpServletRequest, Object)} 的基类实现，
	 * 传递 {@code HandlerMethod} 的 bean。否则返回 {@code false}。
	 */
	@Override
	protected boolean shouldApplyTo(HttpServletRequest request, @Nullable Object handler) {
		if (handler == null) {
			// 如果处理程序为空，则调用父类方法
			return super.shouldApplyTo(request, null);
		} else if (handler instanceof HandlerMethod) {
			// 如果处理程序是 HandlerMethod 类型
			HandlerMethod handlerMethod = (HandlerMethod) handler;
			// 获取处理程序所在的 bean
			handler = handlerMethod.getBean();
			// 调用父类方法
			return super.shouldApplyTo(request, handler);
		} else if (hasGlobalExceptionHandlers() && hasHandlerMappings()) {
			// 如果存在全局异常处理程序并且有处理程序映射
			// 调用父类方法
			return super.shouldApplyTo(request, handler);
		} else {
			// 否则返回 false
			return false;
		}
	}

	/**
	 * 此解析器是否具有全局异常处理程序，例如不在引发异常的 {@code HandlerMethod} 相同类中声明的异常处理程序，
	 * 因此可以应用于任何处理器。
	 *
	 * @since 5.3
	 */
	protected boolean hasGlobalExceptionHandlers() {
		return false;
	}

	@Override
	@Nullable
	protected final ModelAndView doResolveException(
			HttpServletRequest request, HttpServletResponse response, @Nullable Object handler, Exception ex) {

		// 如果处理程序是 HandlerMethod 类型，则转换为 HandlerMethod 对象，否则为 null
		HandlerMethod handlerMethod = (handler instanceof HandlerMethod ? (HandlerMethod) handler : null);
		// 调用 doResolveHandlerMethodException 方法处理异常
		return doResolveHandlerMethodException(request, response, handlerMethod, ex);
	}

	/**
	 * 实际解决在处理器执行期间引发的给定异常，如果适用，则返回表示特定错误页面的 ModelAndView。
	 * <p>可以在子类中重写，以应用特定的异常检查。
	 * 请注意，此模板方法将在检查此解析器是否适用（"mappedHandlers" 等）之后调用，
	 * 因此实现可能只需继续其实际异常处理。
	 *
	 * @param request       当前 HTTP 请求
	 * @param response      当前 HTTP 响应
	 * @param handlerMethod 已执行的处理器方法，如果在异常引发时未选择任何处理器，则为 {@code null}
	 *                      （例如，如果多部分解析失败）
	 * @param ex            在处理器执行期间引发的异常
	 * @return 要转发到的相应 ModelAndView，或者 {@code null} 进行默认处理
	 */
	@Nullable
	protected abstract ModelAndView doResolveHandlerMethodException(
			HttpServletRequest request, HttpServletResponse response, @Nullable HandlerMethod handlerMethod, Exception ex);

}
