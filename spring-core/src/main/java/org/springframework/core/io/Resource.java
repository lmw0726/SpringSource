/*
 * Copyright 2002-2022 the original author or authors.
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

import org.springframework.lang.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

/**
 * 资源描述符的接口，抽象出底层资源的实际类型，例如文件或类路径资源。
 *
 * <p>如果资源以物理形式存在，则可以为每个资源打开 InputStream，
 * 但对于某些资源，只能返回 URL 或 File 句柄。实际行为由具体实现决定。
 *
 * @author Juergen Hoeller
 * @see #getInputStream()
 * @see #getURL()
 * @see #getURI()
 * @see #getFile()
 * @see WritableResource
 * @see ContextResource
 * @see UrlResource
 * @see FileUrlResource
 * @see FileSystemResource
 * @see ClassPathResource
 * @see ByteArrayResource
 * @see InputStreamResource
 * @since 28.12.2003
 */
public interface Resource extends InputStreamSource {

	/**
	 * 确定此资源是否实际存在于物理形式中。
	 * <p>此方法执行明确的存在检查，而 Resource 句柄的存在仅保证有效的描述符句柄。
	 */
	boolean exists();

	/**
	 * 指示是否可以通过 {@link #getInputStream()} 读取此资源的非空内容。
	 * <p>对于典型的资源描述符，此方法将返回 {@code true}，
	 * 因为它严格意味着 {@link #exists()} 语义，从 5.1 版开始。
	 * 请注意，尝试实际读取内容时仍可能失败。
	 * 但是，{@code false} 的值明确指示资源内容无法读取。
	 * @see #getInputStream()
	 * @see #exists()
	 */
	default boolean isReadable() {
		return exists();
	}

	/**
	 * 指示此资源是否表示具有打开流的句柄。
	 * 如果 {@code true}，则 InputStream 不能多次读取，
	 * 必须读取和关闭以避免资源泄漏。
	 * 对于典型的资源描述符，将为 {@code false}。
	 */
	default boolean isOpen() {
		return false;
	}

	/**
	 * 确定此资源是否表示文件系统中的文件。
	 * <p> {@code true} 的值强烈暗示（但不保证）
	 * {@link #getFile()} 调用将成功。
	 * 默认情况下，这是保守的 {@code false}。
	 *
	 * @see #getFile()
	 * @since 5.0
	 */
	default boolean isFile() {
		return false;
	}

	/**
	 * 返回此资源的 URL 句柄。
	 *
	 * @throws IOException 如果无法将资源解析为 URL，
	 *                     即如果资源不可用作描述符
	 */
	URL getURL() throws IOException;

	/**
	 * 返回此资源的 URI 句柄。
	 *
	 * @throws IOException 如果无法将资源解析为 URI，
	 *                     即如果资源不可用作描述符
	 * @since 2.5
	 */
	URI getURI() throws IOException;

	/**
	 * 返回此资源的 File 句柄。
	 *
	 * @throws java.io.FileNotFoundException 如果无法将资源解析为绝对文件路径，
	 *                                       即如果资源不可用于文件系统
	 * @throws IOException                   如果出现一般解析/读取失败
	 * @see #getInputStream()
	 */
	File getFile() throws IOException;

	/**
	 * 返回 ReadableByteChannel。
	 * <p>预期每次调用都会创建一个新的通道。
	 * <p>默认实现使用 {@link #getInputStream()} 的结果返回 {@link Channels#newChannel(InputStream)}。
	 *
	 * @return 底层资源的字节通道（不得为 {@code null}）
	 * @throws java.io.FileNotFoundException 如果底层资源不存在
	 * @throws IOException                   如果无法打开内容通道
	 * @see #getInputStream()
	 * @since 5.0
	 */
	default ReadableByteChannel readableChannel() throws IOException {
		return Channels.newChannel(getInputStream());
	}

	/**
	 * 确定此资源的内容长度。
	 *
	 * @throws IOException 如果无法解析资源
	 *                     （在文件系统中或作为其他已知物理资源类型）
	 */
	long contentLength() throws IOException;

	/**
	 * 确定此资源的最后修改时间戳。
	 *
	 * @throws IOException 如果无法解析资源
	 *                     （在文件系统中或作为其他已知物理资源类型）
	 */
	long lastModified() throws IOException;


	/**
	 * 根据资源的相对路径创建相关资源。
	 *
	 * @param relativePath 相对路径
	 * @return 新的相关资源
	 * @throws IOException IO异常
	 */
	Resource createRelative(String relativePath) throws IOException;

	/**
	 * 确定此资源的文件名，即通常路径的最后部分：例如 "myfile.txt"。
	 * <p>如果此类型的资源没有文件名，则返回 {@code null}。
	 */
	@Nullable
	String getFilename();

	/**
	 * 返回此资源的描述，用于在处理资源时的错误输出。
	 * <p>还鼓励实现从其 {@code toString} 方法返回此值。
	 *
	 * @see Object#toString()
	 */
	String getDescription();

}
