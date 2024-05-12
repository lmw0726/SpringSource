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

package org.springframework.web.servlet.mvc.method.annotation;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.filter.ShallowEtagHeaderFilter;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.util.concurrent.Callable;

/**
 * 支持类型为 {@link org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody}
 * 和 {@code ResponseEntity<StreamingResponseBody>} 的返回值。
 *
 * @author Rossen Stoyanchev
 * @since 4.2
 */
public class StreamingResponseBodyReturnValueHandler implements HandlerMethodReturnValueHandler {

	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		if (StreamingResponseBody.class.isAssignableFrom(returnType.getParameterType())) {
			// 如果返回类型是 StreamingResponseBody 类型，则返回 true
			return true;
		} else if (ResponseEntity.class.isAssignableFrom(returnType.getParameterType())) {
			// 如果返回类型是 ResponseEntity 类型
			// 获取 ResponseEntity 的泛型类型
			Class<?> bodyType = ResolvableType.forMethodParameter(returnType).getGeneric().resolve();
			// 检查泛型类型是否是 StreamingResponseBody 及其实现类
			return (bodyType != null && StreamingResponseBody.class.isAssignableFrom(bodyType));
		}
		// 否则，返回 false
		return false;
	}

	@Override
	@SuppressWarnings("resource")
	public void handleReturnValue(@Nullable Object returnValue, MethodParameter returnType,
								  ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws Exception {

		if (returnValue == null) {
			// 如果返回值为 null，则标记请求已处理并结束程序
			mavContainer.setRequestHandled(true);
			return;
		}

		// 获取 HttpServletResponse 实例
		HttpServletResponse response = webRequest.getNativeResponse(HttpServletResponse.class);
		Assert.state(response != null, "No HttpServletResponse");

		// 将 HttpServletResponse 包装为 ServerHttpResponse
		ServerHttpResponse outputMessage = new ServletServerHttpResponse(response);

		if (returnValue instanceof ResponseEntity) {
			// 如果返回值是 ResponseEntity 类型
			ResponseEntity<?> responseEntity = (ResponseEntity<?>) returnValue;
			// 设置响应状态码和头部信息
			response.setStatus(responseEntity.getStatusCodeValue());
			// 服务端Http响应设置响应头
			outputMessage.getHeaders().putAll(responseEntity.getHeaders());
			// 获取响应体
			returnValue = responseEntity.getBody();
			if (returnValue == null) {
				// 如果响应体为空，则标记请求已处理并刷新输出消息
				mavContainer.setRequestHandled(true);
				outputMessage.flush();
				return;
			}
		}

		// 获取 ServletRequest 实例
		ServletRequest request = webRequest.getNativeRequest(ServletRequest.class);
		Assert.state(request != null, "No ServletRequest");

		// 禁用内容缓存
		ShallowEtagHeaderFilter.disableContentCaching(request);

		// 断言返回值为 StreamingResponseBody 类型
		Assert.isInstanceOf(StreamingResponseBody.class, returnValue, "StreamingResponseBody expected");

		// 获取 StreamingResponseBody 实例
		StreamingResponseBody streamingBody = (StreamingResponseBody) returnValue;

		// 创建 Callable 对象
		Callable<Void> callable = new StreamingResponseBodyTask(outputMessage.getBody(), streamingBody);
		// 启动异步处理
		WebAsyncUtils.getAsyncManager(webRequest).startCallableProcessing(callable, mavContainer);
	}


	private static class StreamingResponseBodyTask implements Callable<Void> {
		/**
		 * 输出流
		 */
		private final OutputStream outputStream;

		/**
		 * 流式响应体
		 */
		private final StreamingResponseBody streamingBody;

		public StreamingResponseBodyTask(OutputStream outputStream, StreamingResponseBody streamingBody) {
			this.outputStream = outputStream;
			this.streamingBody = streamingBody;
		}

		@Override
		public Void call() throws Exception {
			// 将输入流写入流式响应体
			this.streamingBody.writeTo(this.outputStream);
			// 刷新输出流
			this.outputStream.flush();
			return null;
		}
	}

}
