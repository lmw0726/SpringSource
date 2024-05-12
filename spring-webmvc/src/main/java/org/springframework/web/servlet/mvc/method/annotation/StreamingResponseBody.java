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

package org.springframework.web.servlet.mvc.method.annotation;


import java.io.IOException;
import java.io.OutputStream;

/**
 * 用于异步请求处理的控制器方法返回值类型，在应用程序可以直接写入响应 {@code OutputStream} 而不阻塞 Servlet 容器线程时使用。
 *
 * <p><strong>注意：</strong> 当使用此选项时，强烈建议显式配置 Spring MVC 中用于执行异步请求的 TaskExecutor。
 * MVC Java 配置和 MVC 命名空间都提供了配置异步处理的选项。
 * 如果不使用这些选项，应用程序可以设置 {@link org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter
 * RequestMappingHandlerAdapter} 的 {@code taskExecutor} 属性。
 *
 * @author Rossen Stoyanchev
 * @since 4.2
 */
@FunctionalInterface
public interface StreamingResponseBody {

	/**
	 * 用于写入响应主体的回调。
	 *
	 * @param outputStream 响应主体的流
	 * @throws IOException 写入时发生的异常
	 */
	void writeTo(OutputStream outputStream) throws IOException;

}
