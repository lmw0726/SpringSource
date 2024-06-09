/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.web.client;

import org.springframework.http.client.ClientHttpRequest;

import java.io.IOException;
import java.lang.reflect.Type;

/**
 * 用于操作{@link ClientHttpRequest}的代码的回调接口。
 * 允许操作请求头，并向请求体中写入内容。
 *
 * <p>由{@link RestTemplate}在内部使用，但对应用程序代码也很有用。有几个可用的工厂方法：
 * <ul>
 * <li>{@link RestTemplate#acceptHeaderRequestCallback(Class)}
 * <li>{@link RestTemplate#httpEntityCallback(Object)}
 * <li>{@link RestTemplate#httpEntityCallback(Object, Type)}
 * </ul>
 *
 * @author Arjen Poutsma
 * @see RestTemplate#execute
 * @since 3.0
 */
@FunctionalInterface
public interface RequestCallback {

	/**
	 * 在打开的{@code ClientHttpRequest}上由{@link RestTemplate#execute}调用。
	 * 不需要关心关闭请求或处理错误：这将由{@code RestTemplate}处理。
	 *
	 * @param request 当前的HTTP请求
	 * @throws IOException 如果发生I/O错误
	 */
	void doWithRequest(ClientHttpRequest request) throws IOException;

}
