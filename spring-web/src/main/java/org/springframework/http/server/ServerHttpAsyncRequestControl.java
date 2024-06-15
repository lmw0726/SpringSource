/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.http.server;

/**
 * 控制可以将HTTP请求的处理放入异步模式，期间响应保持打开状态，直到显式关闭。
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public interface ServerHttpAsyncRequestControl {

	/**
	 * 启用异步处理，此后响应保持打开状态，直到调用 {@link #complete()} 方法或服务器超时请求。一旦启用，额外调用此方法将被忽略。
	 */
	void start();

	/**
	 * 启用异步处理的变体，允许指定超时值用于异步处理。如果在指定值内未调用 {@link #complete()}，则请求超时。
	 *
	 * @param timeout 超时时间（毫秒）
	 */
	void start(long timeout);

	/**
	 * 返回是否已启动异步请求处理。
	 *
	 * @return 如果已启动异步请求处理，则返回 true；否则返回 false。
	 */
	boolean isStarted();

	/**
	 * 标记异步请求处理已完成。
	 */
	void complete();

	/**
	 * 返回是否已完成异步请求处理。
	 *
	 * @return 如果已完成异步请求处理，则返回 true；否则返回 false。
	 */
	boolean isCompleted();

}
