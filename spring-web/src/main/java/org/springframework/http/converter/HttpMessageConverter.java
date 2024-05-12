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

package org.springframework.http.converter;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * 用于 HTTP 请求和响应之间的转换的策略接口。
 *
 * @param <T> 转换的对象类型
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @since 3.0
 */
public interface HttpMessageConverter<T> {

	/**
	 * 指示此转换器是否可以读取给定类。
	 *
	 * @param clazz     要测试可读性的类
	 * @param mediaType 要读取的媒体类型（如果未指定，则可以为 {@code null}）；
	 *                  通常是 {@code Content-Type} 标头的值
	 * @return 如果可读，则为 {@code true}；否则为 {@code false}
	 */
	boolean canRead(Class<?> clazz, @Nullable MediaType mediaType);

	/**
	 * 指示此转换器是否可以写入给定类。
	 *
	 * @param clazz     要测试可写性的类
	 * @param mediaType 要写入的媒体类型（如果未指定，则可以为 {@code null}）；
	 *                  通常是 {@code Accept} 标头的值
	 * @return 如果可写，则为 {@code true}；否则为 {@code false}
	 */
	boolean canWrite(Class<?> clazz, @Nullable MediaType mediaType);

	/**
	 * 返回此转换器支持的媒体类型列表。该列表可能不适用于每个可能的目标元素类型，并且对此方法的调用通常应通过 {@link #canWrite(Class, MediaType) canWrite(clazz, null)} 进行保护。
	 * 该列表还可能不包括仅支持特定类的 MIME 类型。或者，使用 {@link #getSupportedMediaTypes(Class)} 获取更精确的列表。
	 *
	 * @return 支持的媒体类型列表
	 */
	List<MediaType> getSupportedMediaTypes();

	/**
	 * 返回此转换器为给定类支持的媒体类型列表。如果转换器不支持给定类，或者仅支持其中的一部分媒体类型，则该列表可能与 {@link #getSupportedMediaTypes()} 不同。
	 *
	 * @param clazz 要检查的类的类型
	 * @return 为给定类支持的媒体类型列表
	 * @since 5.3.4
	 */
	default List<MediaType> getSupportedMediaTypes(Class<?> clazz) {
		return (canRead(clazz, null) || canWrite(clazz, null) ?
				// 如果可以读取或写入，则返回支持的媒体类型列表
				getSupportedMediaTypes() : Collections.emptyList());
	}

	/**
	 * 从给定的输入消息中读取给定类型的对象，并返回它。
	 *
	 * @param clazz        要返回的对象的类型。此类型必须先前已传递给此接口的 {@link #canRead canRead} 方法，该方法必须返回 {@code true}。
	 * @param inputMessage 要从中读取的 HTTP 输入消息
	 * @return 转换后的对象
	 * @throws IOException                     如果发生 I/O 错误
	 * @throws HttpMessageNotReadableException 如果发生转换错误
	 */
	T read(Class<? extends T> clazz, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException;

	/**
	 * 将给定对象写入给定的输出消息。
	 *
	 * @param t             要写入输出消息的对象。此对象的类型必须先前已传递给此接口的 {@link #canWrite canWrite} 方法，该方法必须返回 {@code true}。
	 * @param contentType   写入时要使用的内容类型。可以为 {@code null}，表示必须使用转换器的默认内容类型。如果不为 {@code null}，则此媒体类型必须先前已传递给此接口的 {@link #canWrite canWrite} 方法，该方法必须返回 {@code true}。
	 * @param outputMessage 要写入的消息
	 * @throws IOException                     如果发生 I/O 错误
	 * @throws HttpMessageNotWritableException 如果发生转换错误
	 */
	void write(T t, @Nullable MediaType contentType, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException;

}
