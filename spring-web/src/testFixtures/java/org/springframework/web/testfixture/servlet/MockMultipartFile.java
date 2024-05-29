/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.web.testfixture.servlet;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * {@link org.springframework.web.multipart.MultipartFile} 接口的模拟实现。
 *
 * <p>与 {@link MockMultipartHttpServletRequest} 结合使用，用于测试访问多部分上传的应用程序控制器。
 *
 * @author Juergen Hoeller
 * @author Eric Crampton
 * @see MockMultipartHttpServletRequest
 * @since 2.0
 */
public class MockMultipartFile implements MultipartFile {

	private final String name;

	private final String originalFilename;

	@Nullable
	private final String contentType;

	private final byte[] content;


	/**
	 * 使用给定内容创建一个新的 MockMultipartFile。
	 *
	 * @param name    文件的名称
	 * @param content 文件的内容
	 */
	public MockMultipartFile(String name, @Nullable byte[] content) {
		this(name, "", null, content);
	}

	/**
	 * 使用给定内容创建一个新的 MockMultipartFile。
	 *
	 * @param name          文件的名称
	 * @param contentStream 文件内容的流
	 * @throws IOException 如果从流中读取失败
	 */
	public MockMultipartFile(String name, InputStream contentStream) throws IOException {
		this(name, "", null, FileCopyUtils.copyToByteArray(contentStream));
	}

	/**
	 * 使用给定内容创建一个新的 MockMultipartFile。
	 *
	 * @param name             文件的名称
	 * @param originalFilename 原始文件名（客户端机器上的文件名）
	 * @param contentType      内容类型（如果已知）
	 * @param content          文件的内容
	 */
	public MockMultipartFile(
			String name, @Nullable String originalFilename, @Nullable String contentType, @Nullable byte[] content) {

		Assert.hasLength(name, "Name must not be empty");
		this.name = name;
		this.originalFilename = (originalFilename != null ? originalFilename : "");
		this.contentType = contentType;
		this.content = (content != null ? content : new byte[0]);
	}

	/**
	 * 使用给定内容创建一个新的 MockMultipartFile。
	 *
	 * @param name             文件的名称
	 * @param originalFilename 原始文件名（客户端机器上的文件名）
	 * @param contentType      内容类型（如果已知）
	 * @param contentStream    文件内容的流
	 * @throws IOException 如果从流中读取失败
	 */
	public MockMultipartFile(
			String name, @Nullable String originalFilename, @Nullable String contentType, InputStream contentStream)
			throws IOException {

		this(name, originalFilename, contentType, FileCopyUtils.copyToByteArray(contentStream));
	}


	@Override
	public String getName() {
		return this.name;
	}

	@Override
	@NonNull
	public String getOriginalFilename() {
		return this.originalFilename;
	}

	@Override
	@Nullable
	public String getContentType() {
		return this.contentType;
	}

	@Override
	public boolean isEmpty() {
		return (this.content.length == 0);
	}

	@Override
	public long getSize() {
		return this.content.length;
	}

	@Override
	public byte[] getBytes() throws IOException {
		return this.content;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return new ByteArrayInputStream(this.content);
	}

	@Override
	public void transferTo(File dest) throws IOException, IllegalStateException {
		FileCopyUtils.copy(this.content, dest);
	}

}
