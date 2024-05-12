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

package org.springframework.web.servlet.mvc.method.annotation;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.async.WebAsyncTask;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * 处理 {@link WebAsyncTask} 类型的返回值。
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class AsyncTaskMethodReturnValueHandler implements HandlerMethodReturnValueHandler {
	/**
	 * Bean工厂
	 */
	@Nullable
	private final BeanFactory beanFactory;


	public AsyncTaskMethodReturnValueHandler(@Nullable BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}


	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		// 检查返回值类型是否是 WebAsyncTask 及其子类
		return WebAsyncTask.class.isAssignableFrom(returnType.getParameterType());
	}

	@Override
	public void handleReturnValue(@Nullable Object returnValue, MethodParameter returnType,
								  ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws Exception {

		if (returnValue == null) {
			// 如果返回值为空，设置请求已处理并返回
			mavContainer.setRequestHandled(true);
			return;
		}

		// 如果返回值是 WebAsyncTask 类型，开始异步处理
		WebAsyncTask<?> webAsyncTask = (WebAsyncTask<?>) returnValue;
		if (this.beanFactory != null) {
			// 如果存在 Bean工厂，设置到 WebAsyncTask 中
			webAsyncTask.setBeanFactory(this.beanFactory);
		}
		// 开始异步处理任务
		WebAsyncUtils.getAsyncManager(webRequest).startCallableProcessing(webAsyncTask, mavContainer);
	}

}
