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

package org.springframework.core.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * 用于为 {@link InputStream} 提供源的简单接口。
 *
 * <p>这是 Spring 更广泛的 {@link Resource} 接口的基本接口。
 *
 * <p>对于单次使用的流，可以使用 {@link InputStreamResource} 来表示任何给定的 {@code InputStream}。
 * Spring 的 {@link ByteArrayResource} 或任何基于文件的 {@code Resource} 实现可以用作具体的实例，
 * 允许多次读取底层内容流。这使得此接口在作为邮件附件的抽象内容源时非常有用，例如。
 *
 * @author Juergen Hoeller
 * @since 20.01.2004
 * @see java.io.InputStream
 * @see Resource
 * @see InputStreamResource
 * @see ByteArrayResource
 */
public interface InputStreamSource {

	/**
	 * 返回底层资源内容的 {@link InputStream}。
	 * <p>预期每次调用都会创建一个 <i>新的</i> 流。
	 * <p>当考虑到需要能够多次读取流的 API 时，例如 JavaMail 时，
	 * 此要求尤为重要。对于这种用例，<i>需要</i> 每个 {@code getInputStream()} 调用都返回一个新的流。
	 * @return 底层资源的输入流（不得为 {@code null}）
	 * @throws java.io.FileNotFoundException 如果底层资源不存在
	 * @throws IOException 如果无法打开内容流
	 * @see Resource#isReadable()
	 */
	InputStream getInputStream() throws IOException;

}
