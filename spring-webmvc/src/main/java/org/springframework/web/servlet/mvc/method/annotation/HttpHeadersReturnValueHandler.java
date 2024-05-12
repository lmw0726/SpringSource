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

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;

import javax.servlet.http.HttpServletResponse;

/**
 * 处理 {@link HttpHeaders} 返回值。
 *
 * @author Stephane Nicoll
 * @since 4.0.1
 */
public class HttpHeadersReturnValueHandler implements HandlerMethodReturnValueHandler {

	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		// 检查返回值类型是否是HttpHeaders类型及其子类
		return HttpHeaders.class.isAssignableFrom(returnType.getParameterType());
	}

	@Override
	@SuppressWarnings("resource")
	public void handleReturnValue(@Nullable Object returnValue, MethodParameter returnType,
								  ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws Exception {

		// 标记请求已处理
		mavContainer.setRequestHandled(true);

		// 确保返回值是 HttpHeaders 对象
		Assert.state(returnValue instanceof HttpHeaders, "HttpHeaders expected");
		HttpHeaders headers = (HttpHeaders) returnValue;

		// 如果 HttpHeaders 对象不为空，则设置响应头信息
		if (!headers.isEmpty()) {
			// 获取原生的 HttpServletResponse
			HttpServletResponse servletResponse = webRequest.getNativeResponse(HttpServletResponse.class);
			Assert.state(servletResponse != null, "No HttpServletResponse");

			// 创建 ServletServerHttpResponse 对象
			ServletServerHttpResponse outputMessage = new ServletServerHttpResponse(servletResponse);

			// 将 HttpHeaders 中的头信息添加到响应头
			outputMessage.getHeaders().putAll(headers);

			// 刷新响应体，以便将头信息发送到客户端
			outputMessage.getBody();
		}
	}

}
