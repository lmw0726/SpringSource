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

package org.springframework.http.converter;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.StreamUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * 实现了 {@link HttpMessageConverter}，能够读取和写入 {@link Resource} 对象，并支持字节范围请求。
 *
 * <p>默认情况下，此转换器可以读取所有媒体类型。{@link MediaTypeFactory} 用于确定写入资源的 {@code Content-Type}。
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Kazuki Shimizu
 * @since 3.0.2
 */
public class ResourceHttpMessageConverter extends AbstractHttpMessageConverter<Resource> {
	/**
	 * 是否支持读取流
	 */
	private final boolean supportsReadStreaming;


	/**
	 * 创建一个支持读取流的 {@code ResourceHttpMessageConverter} 实例，
	 * 即可以将 {@code HttpInputMessage} 转换为 {@code InputStreamResource}。
	 */
	public ResourceHttpMessageConverter() {
		super(MediaType.ALL);
		this.supportsReadStreaming = true;
	}

	/**
	 * 创建一个新的 {@code ResourceHttpMessageConverter} 实例。
	 *
	 * @param supportsReadStreaming 是否支持读取流，即是否转换为 {@code InputStreamResource}
	 * @since 5.0
	 */
	public ResourceHttpMessageConverter(boolean supportsReadStreaming) {
		super(MediaType.ALL);
		this.supportsReadStreaming = supportsReadStreaming;
	}


	@Override
	protected boolean supports(Class<?> clazz) {
		return Resource.class.isAssignableFrom(clazz);
	}

	@Override
	protected Resource readInternal(Class<? extends Resource> clazz, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {

		// 如果支持读取流并且 类 是 输入流资源 类型
		if (this.supportsReadStreaming && InputStreamResource.class == clazz) {
			// 返回一个含有 输入消息的主体 的 输入流资源 对象
			return new InputStreamResource(inputMessage.getBody()) {
				@Override
				public String getFilename() {
					// 获取文件名
					return inputMessage.getHeaders().getContentDisposition().getFilename();
				}

				@Override
				public long contentLength() throws IOException {
					// 获取内容长度
					long length = inputMessage.getHeaders().getContentLength();
					return (length != -1 ? length : super.contentLength());
				}
			};
		} else if (Resource.class == clazz || ByteArrayResource.class.isAssignableFrom(clazz)) {
			// 如果 类 是 Resource 类型或者是 ByteArrayResource 的子类
			// 将输入消息的主体内容复制到字节数组中
			byte[] body = StreamUtils.copyToByteArray(inputMessage.getBody());
			// 使用复制后的字节数组作为内容，返回一个 字节数组资源 对象
			return new ByteArrayResource(body) {
				@Override
				@Nullable
				public String getFilename() {
					// 获取文件名
					return inputMessage.getHeaders().getContentDisposition().getFilename();
				}
			};
		} else {
			// 如果 类 类型不支持，则抛出异常
			throw new HttpMessageNotReadableException("Unsupported resource class: " + clazz, inputMessage);
		}
	}

	@Override
	protected MediaType getDefaultContentType(Resource resource) {
		return MediaTypeFactory.getMediaType(resource).orElse(MediaType.APPLICATION_OCTET_STREAM);
	}

	@Override
	protected Long getContentLength(Resource resource, @Nullable MediaType contentType) throws IOException {
		// 不要尝试在 InputStreamResource 上确定 内容长度 - 之后无法读取...
		// 注意: 自定义的 InputStreamResource 子类可以提供预先计算的 内容长度!
		// 如果 资源类型 是 InputStreamResource 类型，则直接返回 null
		if (InputStreamResource.class == resource.getClass()) {
			return null;
		}
		// 获取 资源的内容长度
		long contentLength = resource.contentLength();
		// 如果 内容长度 小于 0，则返回 null；否则返回 内容长度
		return (contentLength < 0 ? null : contentLength);
	}

	@Override
	protected void writeInternal(Resource resource, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {

		writeContent(resource, outputMessage);
	}

	protected void writeContent(Resource resource, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {
		// 由于我们在 finally 块中对 close() 方法进行了自定义处理，
		// 因此不能在这里使用 try-with-resources 处理 InputStream。
		try {
			// 获取资源的输入流
			InputStream in = resource.getInputStream();
			try {
				// 将输入流复制到输出消息的主体中
				StreamUtils.copy(in, outputMessage.getBody());
			} catch (NullPointerException ex) {
				// 忽略空指针异常，参见 SPR-13620
			} finally {
				try {
					// 关闭输入流
					in.close();
				} catch (Throwable ex) {
					// 忽略关闭流时可能抛出的异常，参见 SPR-12999
				}
			}
		} catch (FileNotFoundException ex) {
			// 忽略文件未找到异常，参见 SPR-12999
		}
	}

}
