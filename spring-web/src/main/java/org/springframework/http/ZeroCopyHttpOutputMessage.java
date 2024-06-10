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

package org.springframework.http;

import reactor.core.publisher.Mono;

import java.io.File;
import java.nio.file.Path;

/**
 * {@code ReactiveOutputMessage} 的子接口，支持 "zero-copy" 文件传输。
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @see <a href="https://en.wikipedia.org/wiki/Zero-copy">Zero-copy</a>
 * @since 5.0
 */
public interface ZeroCopyHttpOutputMessage extends ReactiveHttpOutputMessage {

	/**
	 * 使用给定的 {@link File} 将消息的主体写入底层的 HTTP 层。
	 *
	 * @param file     要传输的文件
	 * @param position 传输开始的文件中的位置
	 * @param count    要传输的字节数
	 * @return 表示完成或错误的发布者。
	 */
	default Mono<Void> writeWith(File file, long position, long count) {
		return writeWith(file.toPath(), position, count);
	}

	/**
	 * 使用给定的 {@link Path} 将消息的主体写入底层的 HTTP 层。
	 *
	 * @param file     要传输的文件
	 * @param position 传输开始的文件中的位置
	 * @param count    要传输的字节数
	 * @return 表示完成或错误的发布者。
	 * @since 5.1
	 */
	Mono<Void> writeWith(Path file, long position, long count);

}
