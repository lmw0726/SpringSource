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

package org.springframework.web.servlet.function;

/**
 * 表示处理{@linkplain ServerRequest 请求}的函数。
 *
 * @param <T> 函数响应的类型
 * @author Arjen Poutsma
 * @see RouterFunction
 * @since 5.2
 */
@FunctionalInterface
public interface HandlerFunction<T extends ServerResponse> {

	/**
	 * 处理给定的请求。
	 *
	 * @param request 要处理的请求
	 * @return 响应
	 * @throws Exception 处理过程中发生的异常
	 */
	T handle(ServerRequest request) throws Exception;

}
