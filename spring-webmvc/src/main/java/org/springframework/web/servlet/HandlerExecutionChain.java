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

package org.springframework.web.servlet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 处理程序执行链，包含处理程序对象和任何处理程序拦截器。
 * HandlerMapping的{@link HandlerMapping#getHandler}方法返回。
 *
 * @author Juergen Hoeller
 * @see HandlerInterceptor
 * @since 20.06.2003
 */
public class HandlerExecutionChain {
	/**
	 * 日志记录器
	 */
	private static final Log logger = LogFactory.getLog(HandlerExecutionChain.class);

	/**
	 * 要执行的处理器对象
	 */
	private final Object handler;

	/**
	 * 拦截器列表
	 */
	private final List<HandlerInterceptor> interceptorList = new ArrayList<>();

	/**
	 * 拦截器索引
	 */
	private int interceptorIndex = -1;


	/**
	 * 创建一个新的HandlerExecutionChain。
	 *
	 * @param handler 要执行的处理程序对象
	 */
	public HandlerExecutionChain(Object handler) {
		this(handler, (HandlerInterceptor[]) null);
	}

	/**
	 * 创建一个新的HandlerExecutionChain。
	 *
	 * @param handler      要执行的处理程序对象
	 * @param interceptors 要应用的拦截器数组
	 *                     （按给定顺序）在处理程序自身执行之前
	 */
	public HandlerExecutionChain(Object handler, @Nullable HandlerInterceptor... interceptors) {
		this(handler, (interceptors != null ? Arrays.asList(interceptors) : Collections.emptyList()));
	}

	/**
	 * 创建一个新的HandlerExecutionChain。
	 *
	 * @param handler         要执行的处理程序对象
	 * @param interceptorList 要应用的拦截器列表
	 *                        （按给定顺序）在处理程序自身执行之前
	 * @since 5.3
	 */
	public HandlerExecutionChain(Object handler, List<HandlerInterceptor> interceptorList) {
		if (handler instanceof HandlerExecutionChain) {
			// 如果是处理程序执行链器
			HandlerExecutionChain originalChain = (HandlerExecutionChain) handler;
			// 获取要执行的处理器对象
			this.handler = originalChain.getHandler();
			// 拷贝拦截器列表
			this.interceptorList.addAll(originalChain.interceptorList);
		} else {
			this.handler = handler;
		}
		this.interceptorList.addAll(interceptorList);
	}


	/**
	 * 返回要执行的处理器对象。
	 */
	public Object getHandler() {
		return this.handler;
	}

	/**
	 * 将给定的拦截器添加到此链的末尾。
	 */
	public void addInterceptor(HandlerInterceptor interceptor) {
		this.interceptorList.add(interceptor);
	}

	/**
	 * 将给定的拦截器添加到此链的指定索引处。
	 *
	 * @since 5.2
	 */
	public void addInterceptor(int index, HandlerInterceptor interceptor) {
		this.interceptorList.add(index, interceptor);
	}

	/**
	 * 将给定的拦截器添加到此链的末尾。
	 */
	public void addInterceptors(HandlerInterceptor... interceptors) {
		// 将给定的拦截器添加到此链的末尾
		CollectionUtils.mergeArrayIntoCollection(interceptors, this.interceptorList);
	}

	/**
	 * 返回要应用的拦截器数组（按照给定的顺序）。
	 *
	 * @return HandlerInterceptors实例的数组（可能为{@code null}）
	 */
	@Nullable
	public HandlerInterceptor[] getInterceptors() {
		return (!this.interceptorList.isEmpty() ? this.interceptorList.toArray(new HandlerInterceptor[0]) : null);
	}

	/**
	 * 返回要应用的拦截器列表（按给定顺序）。
	 *
	 * @return 拦截器列表（可能为空）的HandlerInterceptors实例列表
	 * @since 5.3
	 */
	public List<HandlerInterceptor> getInterceptorList() {
		// 拦截器列表不是空的，则返回不可更改的集合对象，否则返回空集合
		return (!this.interceptorList.isEmpty() ? Collections.unmodifiableList(this.interceptorList) :
				Collections.emptyList());
	}


	/**
	 * 应用注册的拦截器的preHandle方法。
	 *
	 * @return 如果执行链应继续下一个拦截器或处理程序本身，则为{@code true}。否则，DispatcherServlet假定此拦截器已经处理了响应本身。
	 */
	boolean applyPreHandle(HttpServletRequest request, HttpServletResponse response) throws Exception {
		for (int i = 0; i < this.interceptorList.size(); i++) {
			// 遍历拦截器列表，并依次获取拦截器对象。
			HandlerInterceptor interceptor = this.interceptorList.get(i);
			// 如果拦截器的预处理方法返回false，则触发完成后方法并返回false。
			if (!interceptor.preHandle(request, response, this.handler)) {
				// 触发拦截器完成后的执行方法
				triggerAfterCompletion(request, response, null);
				return false;
			}
			// 更新当前拦截器索引。
			this.interceptorIndex = i;
		}
		// 如果所有拦截器的预处理方法都返回true，则返回true。
		return true;
	}

	/**
	 * 应用注册拦截器的 postHandle 方法。
	 */
	void applyPostHandle(HttpServletRequest request, HttpServletResponse response, @Nullable ModelAndView mv)
			throws Exception {

		for (int i = this.interceptorList.size() - 1; i >= 0; i--) {
			// 从后向前遍历拦截器列表
			HandlerInterceptor interceptor = this.interceptorList.get(i);
			// 执行后置处理函数
			interceptor.postHandle(request, response, this.handler, mv);
		}
	}

	/**
	 * 触发映射的HandlerInterceptors的afterCompletion回调。
	 * 仅对成功完成并返回true的所有拦截器的preHandle调用触发afterCompletion。
	 */
	void triggerAfterCompletion(HttpServletRequest request, HttpServletResponse response, @Nullable Exception ex) {
		for (int i = this.interceptorIndex; i >= 0; i--) {
			// 从后向前遍历拦截器
			HandlerInterceptor interceptor = this.interceptorList.get(i);
			try {
				// 执行完成时处理函数
				interceptor.afterCompletion(request, response, this.handler, ex);
			} catch (Throwable ex2) {
				logger.error("HandlerInterceptor.afterCompletion threw exception", ex2);
			}
		}
	}

	/**
	 * 对映射的 AsyncHandlerInterceptor 应用 afterConcurrentHandlerStarted 回调。
	 */
	void applyAfterConcurrentHandlingStarted(HttpServletRequest request, HttpServletResponse response) {
		for (int i = this.interceptorList.size() - 1; i >= 0; i--) {
			// 从后向前遍历拦截器
			HandlerInterceptor interceptor = this.interceptorList.get(i);
			if (interceptor instanceof AsyncHandlerInterceptor) {
				// 如果是异步处理拦截器
				try {
					AsyncHandlerInterceptor asyncInterceptor = (AsyncHandlerInterceptor) interceptor;
					// 调用异步拦截器的 并发处理开始后 方法
					asyncInterceptor.afterConcurrentHandlingStarted(request, response, this.handler);
				} catch (Throwable ex) {
					if (logger.isErrorEnabled()) {
						logger.error("Interceptor [" + interceptor + "] failed in afterConcurrentHandlingStarted", ex);
					}
				}
			}
		}
	}


	/**
	 * 委托给处理程序的 {@code toString()} 实现。
	 */
	@Override
	public String toString() {
		return "HandlerExecutionChain with [" + getHandler() + "] and " + this.interceptorList.size() + " interceptors";
	}

}
