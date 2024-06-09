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

/**
 * 异步请求超时时抛出的异常。
 * 或者，应用程序可以通过 MVC Java 配置、MVC XML 命名空间或直接通过
 * {@code RequestMappingHandlerAdapter} 的属性注册
 * {@link DeferredResultProcessingInterceptor} 或 {@link CallableProcessingInterceptor} 来处理超时。
 *
 * <p>默认情况下，该异常将被处理为 503 错误。
 *
 * @author Rossen Stoyanchev
 * @since 4.2.8
 */
@SuppressWarnings("serial")
public class AsyncRequestTimeoutException extends RuntimeException {

}
