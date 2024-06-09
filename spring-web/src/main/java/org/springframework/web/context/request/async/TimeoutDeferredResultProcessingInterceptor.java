/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.context.request.async;

import org.springframework.web.context.request.NativeWebRequest;

/**
 * 如果响应尚未提交，则在超时情况下发送503（SERVICE_UNAVAILABLE）。从4.2.8开始，
 * 通过返回 {@link AsyncRequestTimeoutException} 作为处理结果间接实现此目的，
 * 然后由 Spring MVC 的默认异常处理器将其处理为 503 错误。
 *
 * <p>在所有其他拦截器之后注册，因此仅在没有其他拦截器处理超时时才调用。
 *
 * <p>请注意，根据 RFC 7231，没有“Retry-After”标头的503将被解释为500错误，
 * 并且客户端不应重试。应用程序可以安装自己的拦截器来处理超时，并在必要时添加“Retry-After”标头。
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class TimeoutDeferredResultProcessingInterceptor implements DeferredResultProcessingInterceptor {

	@Override
	public <T> boolean handleTimeout(NativeWebRequest request, DeferredResult<T> result) throws Exception {
		// 设置错误结果为 异步请求超时异常
		result.setErrorResult(new AsyncRequestTimeoutException());
		return false;
	}

}
