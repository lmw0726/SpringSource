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

package org.springframework.http.converter;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.StreamingHttpOutputMessage;
import org.springframework.lang.Nullable;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;

/**
 * {@link GenericHttpMessageConverter} 实现的抽象基类。
 *
 * @param <T> 转换后的对象类型
 * @author Sebastien Deleuze
 * @author Juergen Hoeller
 * @since 4.2
 */
public abstract class AbstractGenericHttpMessageConverter<T> extends AbstractHttpMessageConverter<T>
		implements GenericHttpMessageConverter<T> {

	/**
	 * 创建一个不支持任何媒体类型的 {@code AbstractGenericHttpMessageConverter}。
	 *
	 * @see #setSupportedMediaTypes
	 */
	protected AbstractGenericHttpMessageConverter() {
	}

	/**
	 * 创建一个支持单一媒体类型的 {@code AbstractGenericHttpMessageConverter}。
	 *
	 * @param supportedMediaType 支持的媒体类型
	 */
	protected AbstractGenericHttpMessageConverter(MediaType supportedMediaType) {
		super(supportedMediaType);
	}

	/**
	 * 创建一个支持多个媒体类型的 {@code AbstractGenericHttpMessageConverter}。
	 *
	 * @param supportedMediaTypes 支持的媒体类型数组
	 */
	protected AbstractGenericHttpMessageConverter(MediaType... supportedMediaTypes) {
		super(supportedMediaTypes);
	}


	@Override
	protected boolean supports(Class<?> clazz) {
		return true;
	}

	@Override
	public boolean canRead(Type type, @Nullable Class<?> contextClass, @Nullable MediaType mediaType) {
		return (type instanceof Class ? canRead((Class<?>) type, mediaType) : canRead(mediaType));
	}

	@Override
	public boolean canWrite(@Nullable Type type, Class<?> clazz, @Nullable MediaType mediaType) {
		return canWrite(clazz, mediaType);
	}

	/**
	 * 此实现通过调用 {@link #addDefaultHeaders} 设置默认头信息，
	 * 然后调用 {@link #writeInternal} 写入内容。
	 */
	@Override
	public final void write(final T t, @Nullable final Type type, @Nullable MediaType contentType,
							HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {

		// 获取输出消息的头部信息
		final HttpHeaders headers = outputMessage.getHeaders();

		// 添加默认的头部信息，包括内容类型等
		addDefaultHeaders(headers, t, contentType);

		// 如果输出消息是StreamingHttpOutputMessage的实例
		if (outputMessage instanceof StreamingHttpOutputMessage) {
			// 将outputMessage强制转换为StreamingHttpOutputMessage
			StreamingHttpOutputMessage streamingOutputMessage = (StreamingHttpOutputMessage) outputMessage;

			// 设置消息体的写入操作
			streamingOutputMessage.setBody(outputStream -> writeInternal(t, type, new HttpOutputMessage() {
				// 调用writeInternal方法写入内容到输出流
				@Override
				public OutputStream getBody() {
					// 返回传入的输出流
					return outputStream;
				}

				@Override
				public HttpHeaders getHeaders() {
					// 返回之前获取的头部信息
					return headers;
				}
			}));
		} else {
			// 如果不是StreamingHttpOutputMessage的实例，则直接调用writeInternal写入内容
			writeInternal(t, type, outputMessage);

			// 刷新消息体的输出流
			outputMessage.getBody().flush();
		}
	}

	@Override
	protected void writeInternal(T t, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {

		writeInternal(t, null, outputMessage);
	}

	/**
	 * 抽象模板方法，用于实际写入消息体内容。从 {@link #write} 方法调用。
	 *
	 * @param t             要写入到输出消息的对象
	 * @param type          要写入的对象类型（可能为 {@code null}）
	 * @param outputMessage 要写入的 HTTP 输出消息
	 * @throws IOException                     发生 I/O 错误时
	 * @throws HttpMessageNotWritableException 转换错误时
	 */
	protected abstract void writeInternal(T t, @Nullable Type type, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException;

}
