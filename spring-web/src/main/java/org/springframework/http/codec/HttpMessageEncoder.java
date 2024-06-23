/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.http.codec;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Encoder;
import org.springframework.core.codec.Hints;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;

import java.util.List;
import java.util.Map;

/**
 * {@code Encoder}的扩展接口，暴露在HTTP请求或响应体编码上下文中相关的额外方法。
 *
 * @param <T> 输入流中元素的类型
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface HttpMessageEncoder<T> extends Encoder<T> {

	/**
	 * 返回应自动刷新（与在输入流结束时刷新不同）的“流媒体”类型。
	 *
	 * @return 需要自动刷新的流媒体类型列表
	 */
	List<MediaType> getStreamingMediaTypes();

	/**
	 * 根据服务器请求或目标控制器方法参数上的注释获取解码提示。
	 *
	 * @param actualType  实际的源类型进行编码，可能是一个反应式包装器，并且来源于
	 *                    {@link org.springframework.core.MethodParameter}，即提供对方法注释的访问。
	 * @param elementType 我们试图编码的{@code Flux/Mono}内的元素类型。
	 * @param request     当前的请求
	 * @param response    当前的响应
	 * @return 提示的Map，可能为空
	 */
	default Map<String, Object> getEncodeHints(ResolvableType actualType, ResolvableType elementType,
											   @Nullable MediaType mediaType, ServerHttpRequest request, ServerHttpResponse response) {

		return Hints.none();
	}

}
