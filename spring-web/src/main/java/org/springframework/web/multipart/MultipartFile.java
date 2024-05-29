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

package org.springframework.web.multipart;

import org.springframework.core.io.InputStreamSource;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.util.FileCopyUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 表示在多部分请求中接收到的上传文件的表示形式。
 *
 * <p>文件内容可能存储在内存中，也可能暂时存储在磁盘上。
 * 在任何情况下，用户都负责将文件内容复制到会话级或持久存储中（如果需要的话）。
 * 临时存储将在请求处理结束时被清除。
 *
 * @author Juergen Hoeller
 * @author Trevor D. Cook
 * @see org.springframework.web.multipart.MultipartHttpServletRequest
 * @see org.springframework.web.multipart.MultipartResolver
 * @since 29.09.2003
 */
public interface MultipartFile extends InputStreamSource {

	/**
	 * 返回多部分表单中参数的名称。
	 *
	 * @return 参数的名称（永不为 {@code null} 或空）
	 */
	String getName();

	/**
	 * 返回客户端文件系统中的原始文件名。
	 * <p>这可能包含路径信息，这取决于所使用的浏览器，
	 * 但除了 Opera 之外，通常不会包含路径信息。
	 * <p><strong>注意：</strong> 请记住此文件名是由客户端提供的，不应盲目使用。
	 * 除了不使用目录部分之外，文件名还可能包含诸如 ".." 等可被恶意使用的字符。
	 * 建议不直接使用此文件名。最好生成一个唯一的文件名，并将此文件名保存在某处以供参考（如果需要）。
	 *
	 * @return 原始文件名；如果在多部分表单中未选择文件，则为空字符串；如果未定义或不可用，则为 {@code null}
	 * @see org.apache.commons.fileupload.FileItem#getName()
	 * @see org.springframework.web.multipart.commons.CommonsMultipartFile#setPreserveFilename
	 * @see <a href="https://tools.ietf.org/html/rfc7578#section-4.2">RFC 7578, Section 4.2</a>
	 * @see <a href="https://owasp.org/www-community/vulnerabilities/Unrestricted_File_Upload">Unrestricted File Upload</a>
	 */
	@Nullable
	String getOriginalFilename();

	/**
	 * 返回文件的内容类型。
	 *
	 * @return 内容类型；如果未定义（或在多部分表单中未选择文件），则为 {@code null}
	 */
	@Nullable
	String getContentType();

	/**
	 * 返回上传的文件是否为空，即，要么在多部分表单中未选择文件，要么所选择的文件没有内容。
	 */
	boolean isEmpty();

	/**
	 * 返回文件的大小（以字节为单位）。
	 *
	 * @return 文件的大小；如果为空，则为 0
	 */
	long getSize();

	/**
	 * 返回文件的内容作为字节数组。
	 *
	 * @return 文件的内容作为字节数组；如果为空，则为空字节数组
	 * @throws IOException 如果出现访问错误（如果临时存储失败）
	 */
	byte[] getBytes() throws IOException;

	/**
	 * 返回用于从文件中读取内容的 InputStream。
	 * <p>用户负责关闭返回的流。
	 *
	 * @return 文件的内容作为流；如果为空，则为空流
	 * @throws IOException 如果出现访问错误（如果临时存储失败）
	 */
	@Override
	InputStream getInputStream() throws IOException;

	/**
	 * 返回此 MultipartFile 的 Resource 表示形式。
	 * 这可用于将内容长度和文件名暴露给 {@code RestTemplate} 或 {@code WebClient}。
	 *
	 * @return 适用于 Resource 合约的此 MultipartFile
	 * @since 5.1
	 */
	default Resource getResource() {
		return new MultipartFileResource(this);
	}

	/**
	 * 将接收到的文件传输到给定的目标文件。
	 * <p>这可以将文件移动到文件系统中，复制文件到文件系统中，或将内存中的内容保存到目标文件中。
	 * 如果目标文件已经存在，将首先删除它。
	 * <p>如果目标文件已在文件系统中移动，则无法再次调用此操作。
	 * 因此，只能调用此方法一次以使用任何存储机制。
	 * <p><b>注意：</b> 依赖于底层提供者，临时存储可能与容器相关，包括此处指定的相对目标的基目录（例如，使用 Servlet 3.0 多部分处理）。
	 * 对于绝对目标，即使临时副本已存在，目标文件也可能被重命名/移动或新复制。
	 *
	 * @param dest 目标文件（通常是绝对的）
	 * @throws IOException           如果读取或写入出现错误
	 * @throws IllegalStateException 如果文件已在文件系统中移动，并且不再可用于另一个传输
	 * @see org.apache.commons.fileupload.FileItem#write(File)
	 * @see javax.servlet.http.Part#write(String)
	 */
	void transferTo(File dest) throws IOException, IllegalStateException;

	/**
	 * 将接收到的文件传输到给定的目标文件。
	 * <p>默认实现简单地复制文件输入流。
	 *
	 * @see #getInputStream()
	 * @see #transferTo(File)
	 * @since 5.1
	 */
	default void transferTo(Path dest) throws IOException, IllegalStateException {
		FileCopyUtils.copy(getInputStream(), Files.newOutputStream(dest));
	}

}
