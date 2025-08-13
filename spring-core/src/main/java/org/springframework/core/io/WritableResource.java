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

package org.springframework.core.io;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;

/**
 * 支持写入操作的资源扩展接口。
 * 提供 {@link #getOutputStream() OutputStream 访问器}。
 *
 * @author Juergen Hoeller
 * @since 3.1
 * @see java.io.OutputStream
 */
public interface WritableResource extends Resource {

	/**
	 * 指示是否可以通过 {@link #getOutputStream()} 写入该资源的内容。
	 * <p>对于典型的资源描述符，此方法返回 {@code true}；
	 * 但实际写入内容时仍可能失败。
	 * {@code false} 则明确表示资源内容不可修改。
	 * @see #getOutputStream()
	 * @see #isReadable()
	 */
	default boolean isWritable() {
		return true;
	}

	/**
	 * 返回该资源对应的 {@link OutputStream}，允许写入或覆盖其内容。
	 * @throws IOException 如果无法打开流
	 * @see #getInputStream()
	 */
	OutputStream getOutputStream() throws IOException;

	/**
	 * 返回一个 {@link WritableByteChannel}。
	 * <p>预期每次调用都会创建一个 <i>新的</i> 通道。
	 * <p>默认实现调用 {@link Channels#newChannel(OutputStream)}，参数为 {@link #getOutputStream()} 的结果。
	 * @return 底层资源的字节通道（不得为 {@code null}）
	 * @throws java.io.FileNotFoundException 如果底层资源不存在
	 * @throws IOException 如果无法打开内容通道
	 * @since 5.0
	 * @see #getOutputStream()
	 */
	default WritableByteChannel writableChannel() throws IOException {
		return Channels.newChannel(getOutputStream());
	}

}
