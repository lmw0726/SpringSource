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
import org.springframework.core.codec.Decoder;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;

import java.util.Map;

/**
 * 扩展{@code Decoder}接口，暴露在HTTP请求或响应体解码上下文中相关的额外方法。
 *
 * @param <T> 输出流中元素的类型
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface HttpMessageDecoder<T> extends Decoder<T> {

	/**
	 * 基于服务器请求或目标控制器方法参数上的注解获取解码提示。
	 *
	 * @param actualType  要解码的实际目标类型，可能是一个反应性包装器，并且源自
	 *                    {@link org.springframework.core.MethodParameter}，即提供对方法参数注解的访问
	 * @param elementType 我们尝试解码的{@code Flux/Mono}中的元素类型
	 * @param request     当前请求
	 * @param response    当前响应
	 * @return 一个包含提示的Map，可能为空
	 */
	Map<String, Object> getDecodeHints(ResolvableType actualType, ResolvableType elementType,
									   ServerHttpRequest request, ServerHttpResponse response);

}
