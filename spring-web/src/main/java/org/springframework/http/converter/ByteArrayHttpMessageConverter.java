/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.http.converter;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * 实现了 {@link HttpMessageConverter}，能够读取和写入字节数组。
 *
 * <p>默认情况下，此转换器支持所有媒体类型（<code>&#42;/&#42;</code>），并使用 {@code Content-Type} 为 {@code application/octet-stream} 进行写入。
 * 可以通过设置 {@link #setSupportedMediaTypes supportedMediaTypes} 属性进行覆盖。
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 3.0
 */
public class ByteArrayHttpMessageConverter extends AbstractHttpMessageConverter<byte[]> {

	/**
	 * 创建一个新的 {@code ByteArrayHttpMessageConverter} 实例。
	 */
	public ByteArrayHttpMessageConverter() {
		super(MediaType.APPLICATION_OCTET_STREAM, MediaType.ALL);
	}


	@Override
	public boolean supports(Class<?> clazz) {
		return byte[].class == clazz;
	}

	@Override
	protected byte[] readInternal(Class<? extends byte[]> clazz, HttpInputMessage inputMessage) throws IOException {
		// 获取输入消息的内容长度
		long contentLength = inputMessage.getHeaders().getContentLength();
		// 创建 字节数组输出流 对象，并指定初始容量
		ByteArrayOutputStream bos =
				new ByteArrayOutputStream(contentLength >= 0 ? (int) contentLength : StreamUtils.BUFFER_SIZE);
		// 将输入消息的主体内容复制到 字节数组输出流 中
		StreamUtils.copy(inputMessage.getBody(), bos);
		// 将 字节数组输出流 转换为字节数组并返回
		return bos.toByteArray();
	}

	@Override
	protected Long getContentLength(byte[] bytes, @Nullable MediaType contentType) {
		return (long) bytes.length;
	}

	@Override
	protected void writeInternal(byte[] bytes, HttpOutputMessage outputMessage) throws IOException {
		StreamUtils.copy(bytes, outputMessage.getBody());
	}

}
