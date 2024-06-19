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

package org.springframework.http.converter;

import org.apache.commons.logging.Log;
import org.springframework.http.*;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * {@link HttpMessageConverter} 实现大多数的抽象基类。
 *
 * <p>此基类通过 {@link #setSupportedMediaTypes(List) supportedMediaTypes} bean 属性添加了对设置支持的 {@code MediaTypes} 的支持。
 * 它还在写入输出消息时添加了对 {@code Content-Type} 和 {@code Content-Length} 的支持。
 *
 * @param <T> 转换的对象类型
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @since 3.0
 */
public abstract class AbstractHttpMessageConverter<T> implements HttpMessageConverter<T> {

	/**
	 * 子类可用的日志记录器。
	 */
	protected final Log logger = HttpLogging.forLogName(getClass());

	/**
	 * 支持的媒体类型列表
	 */
	private List<MediaType> supportedMediaTypes = Collections.emptyList();

	/**
	 * 默认字符集
	 */
	@Nullable
	private Charset defaultCharset;


	/**
	 * 构造一个没有支持的媒体类型的 {@code AbstractHttpMessageConverter}。
	 *
	 * @see #setSupportedMediaTypes
	 */
	protected AbstractHttpMessageConverter() {
	}

	/**
	 * 构造一个支持一个媒体类型的 {@code AbstractHttpMessageConverter}。
	 *
	 * @param supportedMediaType 支持的媒体类型
	 */
	protected AbstractHttpMessageConverter(MediaType supportedMediaType) {
		setSupportedMediaTypes(Collections.singletonList(supportedMediaType));
	}

	/**
	 * 构造一个支持多个媒体类型的 {@code AbstractHttpMessageConverter}。
	 *
	 * @param supportedMediaTypes 支持的媒体类型
	 */
	protected AbstractHttpMessageConverter(MediaType... supportedMediaTypes) {
		setSupportedMediaTypes(Arrays.asList(supportedMediaTypes));
	}

	/**
	 * 构造一个具有默认字符集和多个支持媒体类型的 {@code AbstractHttpMessageConverter}。
	 *
	 * @param defaultCharset      默认字符集
	 * @param supportedMediaTypes 支持的媒体类型
	 * @since 4.3
	 */
	protected AbstractHttpMessageConverter(Charset defaultCharset, MediaType... supportedMediaTypes) {
		// 设置默认字符集
		this.defaultCharset = defaultCharset;
		// 设置支持的媒体类型列表
		setSupportedMediaTypes(Arrays.asList(supportedMediaTypes));
	}


	/**
	 * 设置此转换器支持的 {@link MediaType} 对象列表。
	 */
	public void setSupportedMediaTypes(List<MediaType> supportedMediaTypes) {
		Assert.notEmpty(supportedMediaTypes, "MediaType List must not be empty");
		this.supportedMediaTypes = new ArrayList<>(supportedMediaTypes);
	}

	@Override
	public List<MediaType> getSupportedMediaTypes() {
		return Collections.unmodifiableList(this.supportedMediaTypes);
	}

	/**
	 * 设置默认的字符集，如果有的话。
	 *
	 * @since 4.3
	 */
	public void setDefaultCharset(@Nullable Charset defaultCharset) {
		this.defaultCharset = defaultCharset;
	}

	/**
	 * 返回默认的字符集，如果有的话。
	 *
	 * @since 4.3
	 */
	@Nullable
	public Charset getDefaultCharset() {
		return this.defaultCharset;
	}


	/**
	 * 此实现检查给定的类是否 {@linkplain #supports(Class) 可支持}，
	 * 并且 {@linkplain #getSupportedMediaTypes() 支持的媒体类型}
	 * 是否 {@linkplain MediaType#includes(MediaType) 包含} 给定的媒体类型。
	 */
	@Override
	public boolean canRead(Class<?> clazz, @Nullable MediaType mediaType) {
		return supports(clazz) && canRead(mediaType);
	}

	/**
	 * 如果任何 {@linkplain #setSupportedMediaTypes(List) 支持的} 媒体类型
	 * {@link MediaType#includes(MediaType) 包含} 给定的媒体类型，则返回 {@code true}。
	 *
	 * @param mediaType 要读取的媒体类型；如果未指定，则可以为 {@code null}。
	 *                  通常是 {@code Content-Type} 头的值。
	 * @return 如果支持的媒体类型包含该媒体类型，或者媒体类型为 {@code null}，则返回 {@code true}
	 */
	protected boolean canRead(@Nullable MediaType mediaType) {
		// 如果媒体类型为 null，则返回 true
		if (mediaType == null) {
			return true;
		}

		// 遍历支持的媒体类型列表
		for (MediaType supportedMediaType : getSupportedMediaTypes()) {
			// 如果支持的媒体类型包含给定的媒体类型，则返回 true
			if (supportedMediaType.includes(mediaType)) {
				return true;
			}
		}

		// 如果没有找到匹配的支持的媒体类型，则返回 false
		return false;
	}

	/**
	 * 此实现检查给定的类是否 {@linkplain #supports(Class) 可支持}，
	 * 并且 {@linkplain #getSupportedMediaTypes() 支持的} 媒体类型
	 * {@linkplain MediaType#includes(MediaType) 包含} 给定的媒体类型。
	 */
	@Override
	public boolean canWrite(Class<?> clazz, @Nullable MediaType mediaType) {
		return supports(clazz) && canWrite(mediaType);
	}

	/**
	 * 如果给定的媒体类型包含任何 {@linkplain #setSupportedMediaTypes(List) 支持的媒体类型}，
	 * 则返回 {@code true}。
	 *
	 * @param mediaType 要写入的媒体类型；如果未指定，则可以为 {@code null}。
	 *                  通常是 {@code Accept} 头的值。
	 * @return 如果支持的媒体类型与媒体类型兼容，或者媒体类型为 {@code null}，则返回 {@code true}
	 */
	protected boolean canWrite(@Nullable MediaType mediaType) {
		// 如果媒体类型为 null 或者是通配符类型 "*/*"，则返回 true
		if (mediaType == null || MediaType.ALL.equalsTypeAndSubtype(mediaType)) {
			return true;
		}

		// 遍历支持的媒体类型列表
		for (MediaType supportedMediaType : getSupportedMediaTypes()) {
			// 如果支持的媒体类型与给定的媒体类型兼容，则返回 true
			if (supportedMediaType.isCompatibleWith(mediaType)) {
				return true;
			}
		}

		// 如果没有找到兼容的支持的媒体类型，则返回 false
		return false;
	}

	/**
	 * 此实现简单委托给 {@link #readInternal(Class, HttpInputMessage)}。
	 * 未来的实现可能会添加一些默认行为。
	 */
	@Override
	public final T read(Class<? extends T> clazz, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {

		return readInternal(clazz, inputMessage);
	}

	/**
	 * 此实现通过调用 {@link #addDefaultHeaders} 设置默认的头信息，然后调用 {@link #writeInternal}。
	 */
	@Override
	public final void write(final T t, @Nullable MediaType contentType, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {
		// 获取Http头部
		final HttpHeaders headers = outputMessage.getHeaders();
		// 将内容类型添加到默认头部
		addDefaultHeaders(headers, t, contentType);

		if (outputMessage instanceof StreamingHttpOutputMessage) {
			// 如果输出消息是流式输出消息类型
			StreamingHttpOutputMessage streamingOutputMessage = (StreamingHttpOutputMessage) outputMessage;
			// 设置输出消息的主体部分
			streamingOutputMessage.setBody(outputStream -> writeInternal(t, new HttpOutputMessage() {
				@Override
				public OutputStream getBody() {
					return outputStream;
				}

				@Override
				public HttpHeaders getHeaders() {
					return headers;
				}
			}));
		} else {
			// 否则，直接写入消息体
			writeInternal(t, outputMessage);
			// 刷新消息体的输出流
			outputMessage.getBody().flush();
		}
	}

	/**
	 * 向输出消息添加默认的头信息。
	 * <p>此实现委托给 {@link #getDefaultContentType(Object)} 如果未提供内容类型，
	 * 设置必要的默认字符集，调用 {@link #getContentLength}，并设置相应的头信息。
	 *
	 * @since 4.2
	 */
	protected void addDefaultHeaders(HttpHeaders headers, T t, @Nullable MediaType contentType) throws IOException {
		// 如果响应头中的 内容类型 为空
		if (headers.getContentType() == null) {
			MediaType contentTypeToUse = contentType;
			// 如果 内容类型 为空，或者内容类型不是具体的
			if (contentType == null || !contentType.isConcrete()) {
				// 使用默认媒体类型
				contentTypeToUse = getDefaultContentType(t);
			} else if (MediaType.APPLICATION_OCTET_STREAM.equals(contentType)) {
				// 如果内容类型为应用程序/octet-stream，则尝试使用默认内容类型
				MediaType mediaType = getDefaultContentType(t);
				// 如果默认的内容类型不为空，则使用默认的内容类型。否则，使用 应用程序/octet-stream 媒体类型
				contentTypeToUse = (mediaType != null ? mediaType : contentTypeToUse);
			}
			// 如果确定了要使用的内容类型
			if (contentTypeToUse != null) {
				// 如果内容类型中未指定字符集
				if (contentTypeToUse.getCharset() == null) {
					// 使用默认字符集
					Charset defaultCharset = getDefaultCharset();
					if (defaultCharset != null) {
						// 如果有默认字符集，则指定字符集并创建媒体类型
						contentTypeToUse = new MediaType(contentTypeToUse, defaultCharset);
					}
				}
				// 设置响应头的内容类型
				headers.setContentType(contentTypeToUse);
			}
		}

		// 如果响应头中的 内容长度 小于 0，并且没有设置 传输编码
		if (headers.getContentLength() < 0 && !headers.containsKey(HttpHeaders.TRANSFER_ENCODING)) {
			// 获取内容长度
			Long contentLength = getContentLength(t, headers.getContentType());
			// 如果获取到了内容长度，则设置到响应头中
			if (contentLength != null) {
				headers.setContentLength(contentLength);
			}
		}
	}

	/**
	 * 返回给定类型的默认内容类型。在调用 {@link #write} 时没有指定内容类型参数时调用。
	 * <p>默认情况下，如果有的话，这将返回 {@link #setSupportedMediaTypes(List) supportedMediaTypes} 属性的第一个元素。
	 * 可以在子类中重写。
	 *
	 * @param t 要返回内容类型的类型
	 * @return 内容类型，如果不知道则为 {@code null}
	 */
	@Nullable
	protected MediaType getDefaultContentType(T t) throws IOException {
		// 获取支持的媒体类型列表
		List<MediaType> mediaTypes = getSupportedMediaTypes();
		// 如果支持的媒体类型列表不为空，则返回第一个媒体类型；否则返回 null
		return (!mediaTypes.isEmpty() ? mediaTypes.get(0) : null);
	}

	/**
	 * 返回给定类型的内容长度。
	 * <p>默认情况下，这将返回 {@code null}，表示内容长度未知。
	 * 可以在子类中重写。
	 *
	 * @param t           要返回内容长度的类型
	 * @param contentType 内容类型，可以为 {@code null}
	 * @return 内容长度，如果不知道则为 {@code null}
	 */
	@Nullable
	protected Long getContentLength(T t, @Nullable MediaType contentType) throws IOException {
		return null;
	}


	/**
	 * 指示此转换器是否支持给定的类。
	 *
	 * @param clazz 要测试支持的类
	 * @return 如果支持则为 {@code true}；否则为 {@code false}
	 */
	protected abstract boolean supports(Class<?> clazz);

	/**
	 * 抽象模板方法，读取实际对象。从 {@link #read} 调用。
	 *
	 * @param clazz        要返回对象的类型
	 * @param inputMessage 要从中读取的 HTTP 输入消息
	 * @return 转换后的对象
	 * @throws IOException                     发生 I/O 错误时
	 * @throws HttpMessageNotReadableException 发生转换错误时
	 */
	protected abstract T readInternal(Class<? extends T> clazz, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException;

	/**
	 * 抽象模板方法，写入实际内容。从 {@link #write} 调用。
	 *
	 * @param t             要写入到输出消息的对象
	 * @param outputMessage 要写入的 HTTP 输出消息
	 * @throws IOException                     发生 I/O 错误时
	 * @throws HttpMessageNotWritableException 发生转换错误时
	 */
	protected abstract void writeInternal(T t, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException;

}
