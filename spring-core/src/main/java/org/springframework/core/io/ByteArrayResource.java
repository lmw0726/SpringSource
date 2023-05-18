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

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * {@link Resource} implementation for a given byte array.
 * <p>Creates a {@link ByteArrayInputStream} for the given byte array.
 *
 * <p>Useful for loading content from any given byte array,
 * without having to resort to a single-use {@link InputStreamResource}.
 * Particularly useful for creating mail attachments from local content,
 * where JavaMail needs to be able to read the stream multiple times.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @see java.io.ByteArrayInputStream
 * @see InputStreamResource
 * @see org.springframework.mail.javamail.MimeMessageHelper#addAttachment(String, InputStreamSource)
 * @since 1.2.3
 */
public class ByteArrayResource extends AbstractResource {

	private final byte[] byteArray;

	private final String description;


	public ByteArrayResource(byte[] byteArray) {
		this(byteArray, "resource loaded from byte array");
	}


	public ByteArrayResource(byte[] byteArray, @Nullable String description) {
		Assert.notNull(byteArray, "Byte array must not be null");
		this.byteArray = byteArray;
		this.description = (description != null ? description : "");
	}


	/**
	 * 返回字节数组
	 *
	 * @return 字节数组
	 */
	public final byte[] getByteArray() {
		return this.byteArray;
	}


	/**
	 * 资源是否存在，默认返回true
	 *
	 * @return true表示资源存在
	 */
	@Override
	public boolean exists() {
		return true;
	}


	/**
	 * 获取资源的内容长度
	 *
	 * @return 资源的内容长度
	 */
	@Override
	public long contentLength() {
		return this.byteArray.length;
	}

	/**
	 * 获取输入流
	 *
	 * @return 实际返回的是ByteArrayInputStream
	 * @throws IOException IO异常
	 */
	@Override
	public InputStream getInputStream() throws IOException {
		return new ByteArrayInputStream(this.byteArray);
	}


	/**
	 * 获取资源描述
	 *
	 * @return 资源描述
	 */
	@Override
	public String getDescription() {
		return "Byte array resource [" + this.description + "]";
	}


	/**
	 * 两个文件资源是否相同
	 *
	 * @param other 另外的资源
	 * @return true表示该资源相同
	 */
	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof ByteArrayResource &&
				Arrays.equals(((ByteArrayResource) other).byteArray, this.byteArray)));
	}

	/**
	 * 获取HashCode
	 *
	 * @return 哈希值
	 */
	@Override
	public int hashCode() {
		//计算的是数组的哈希值
		return Arrays.hashCode(this.byteArray);
	}

}
