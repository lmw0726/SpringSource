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

package org.springframework.web.servlet.handler;

import org.springframework.core.Ordered;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.List;

/**
 * 一个实现了{@link HandlerExceptionResolver}接口的类，它将异常委托给其他一组{@link HandlerExceptionResolver}。
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class HandlerExceptionResolverComposite implements HandlerExceptionResolver, Ordered {

	/**
	 * 存储异常解析器列表
	 */
	@Nullable
	private List<HandlerExceptionResolver> resolvers;

	/**
	 * 设置默认优先级
	 */
	private int order = Ordered.LOWEST_PRECEDENCE;


	/**
	 * 设置要委托的异常解析器列表。
	 */
	public void setExceptionResolvers(List<HandlerExceptionResolver> exceptionResolvers) {
		this.resolvers = exceptionResolvers;
	}

	/**
	 * 返回要委托的异常解析器列表。
	 */
	public List<HandlerExceptionResolver> getExceptionResolvers() {
		return (this.resolvers != null ? Collections.unmodifiableList(this.resolvers) : Collections.emptyList());
	}

	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}


	/**
	 * 通过迭代配置的异常解析器列表来解析异常。
	 * <p>第一个返回{@link ModelAndView}的解析器获胜。否则返回null。
	 */
	@Override
	@Nullable
	public ModelAndView resolveException(
			HttpServletRequest request, HttpServletResponse response, @Nullable Object handler, Exception ex) {

		if (this.resolvers != null) {
			// 如果异常解析器不为空，则遍历异常解析器列表
			for (HandlerExceptionResolver handlerExceptionResolver : this.resolvers) {
				// 调用异常解析器的resolveException方法处理异常
				ModelAndView mav = handlerExceptionResolver.resolveException(request, response, handler, ex);
				if (mav != null) {
					// 如果返回的ModelAndView对象不为空，则直接返回该对象
					return mav;
				}
			}
		}
		// 如果所有异常解析器都未处理该异常，则返回null
		return null;
	}

}
