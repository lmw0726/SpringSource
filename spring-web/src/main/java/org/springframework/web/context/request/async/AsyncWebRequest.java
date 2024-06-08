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

package org.springframework.web.context.request.async;

import org.springframework.lang.Nullable;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.function.Consumer;

/**
 * 通过为异步请求处理添加方法扩展了 {@link NativeWebRequest}。
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public interface AsyncWebRequest extends NativeWebRequest {

	/**
	 * 设置并发处理完成所需的时间。
	 * 当并发处理正在进行时，即 {@link #isAsyncStarted()} 为 {@code true} 时，不应设置此属性。
	 *
	 * @param timeout 时间量（以毫秒为单位）；{@code null} 表示没有超时，即依赖容器的默认超时。
	 */
	void setTimeout(@Nullable Long timeout);

	/**
	 * 添加一个处理程序，在并发处理超时时调用。
	 */
	void addTimeoutHandler(Runnable runnable);

	/**
	 * 添加一个处理程序，在请求的并发处理过程中发生错误时调用。
	 *
	 * @since 5.0
	 */
	void addErrorHandler(Consumer<Throwable> exceptionHandler);

	/**
	 * 添加一个处理程序，在请求处理完成时调用。
	 */
	void addCompletionHandler(Runnable runnable);

	/**
	 * 标记异步请求处理的开始，以便当主处理线程退出时，响应保持打开以便在另一个线程中进一步处理。
	 *
	 * @throws IllegalStateException 如果异步处理已完成或不受支持
	 */
	void startAsync();

	/**
	 * 在调用 {@link #startAsync()} 后，请求是否处于异步模式。
	 * 如果异步处理从未启动、已完成或请求已调度以进行进一步处理，则返回“false”。
	 */
	boolean isAsyncStarted();

	/**
	 * 将请求分派到容器，以便在应用程序线程中的并发执行后恢复处理。
	 */
	void dispatch();

	/**
	 * 异步处理是否已完成。
	 */
	boolean isAsyncComplete();

}
