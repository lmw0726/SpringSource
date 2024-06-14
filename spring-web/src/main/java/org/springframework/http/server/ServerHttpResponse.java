/*
 * Copyright 2002-2013 the original author or authors.
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

import org.springframework.http.HttpOutputMessage;
import org.springframework.http.HttpStatus;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;

/**
 * 表示服务器端的HTTP响应。
 *
 * @author Arjen Poutsma
 * @since 3.0
 */
public interface ServerHttpResponse extends HttpOutputMessage, Flushable, Closeable {

	/**
	 * 设置HTTP响应的状态码。
	 *
	 * @param status HTTP状态，作为HttpStatus枚举值
	 */
	void setStatusCode(HttpStatus status);

	/**
	 * 确保响应的头部和内容被写出。
	 * <p>在第一次刷新之后，头部将无法再被更改。
	 * 只有进一步的内容写入和内容刷新是可能的。
	 */
	@Override
	void flush() throws IOException;

	/**
	 * 关闭此响应，释放创建的任何资源。
	 */
	@Override
	void close();

}
