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

package org.springframework.web.client;

import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.Nullable;

import java.io.IOException;
import java.lang.reflect.Type;

/**
 * {@link RestTemplate}的检索方法使用的通用回调接口。
 * 此接口的实现执行从{@link ClientHttpResponse}中提取数据的实际工作，但无需担心异常处理或关闭资源。
 *
 * <p>由{@link RestTemplate}内部使用，但也对应用程序代码很有用。有一个可用的工厂方法，请参见
 * {@link RestTemplate#responseEntityExtractor(Type)}。
 *
 * @param <T> 数据类型
 * @author Arjen Poutsma
 * @see RestTemplate#execute
 * @since 3.0
 */
@FunctionalInterface
public interface ResponseExtractor<T> {

	/**
	 * 从给定的{@code ClientHttpResponse}中提取数据并返回它。
	 *
	 * @param response HTTP响应
	 * @return 提取的数据
	 * @throws IOException 在I/O错误的情况下
	 */
	@Nullable
	T extractData(ClientHttpResponse response) throws IOException;

}
