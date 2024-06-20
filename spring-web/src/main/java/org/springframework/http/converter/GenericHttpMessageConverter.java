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

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;

import java.io.IOException;
import java.lang.reflect.Type;

/**
 * 一个 {@link HttpMessageConverter} 的特化接口，可以将 HTTP 请求转换为指定泛型类型的目标对象，并将源对象转换为 HTTP 响应。
 *
 * @param <T> 转换后的对象类型
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @see org.springframework.core.ParameterizedTypeReference
 * @since 3.2
 */
public interface GenericHttpMessageConverter<T> extends HttpMessageConverter<T> {

	/**
	 * 指示此转换器是否可以读取给定类型。
	 * 此方法应执行与 {@link HttpMessageConverter#canRead(Class, MediaType)} 相同的检查，
	 * 并添加与泛型类型相关的其他检查。
	 *
	 * @param type         泛型类型，用于测试可读性
	 * @param contextClass 目标类型的上下文类，例如目标类型出现在方法签名中的类（可以为 {@code null}）
	 * @param mediaType    要读取的媒体类型，如果未指定则可以为 {@code null}。
	 *                     通常是 {@code Content-Type} 标头的值。
	 * @return {@code true} 如果可读；{@code false} 否则
	 */
	boolean canRead(Type type, @Nullable Class<?> contextClass, @Nullable MediaType mediaType);

	/**
	 * 从给定的输入消息中读取指定类型的对象，并返回它。
	 *
	 * @param type         要返回的对象的泛型类型。此类型必须先前已传递给此接口的 {@link #canRead canRead} 方法，
	 *                     该方法必须返回 {@code true}。
	 * @param contextClass 目标类型的上下文类，例如目标类型出现在方法签名中的类（可以为 {@code null}）
	 * @param inputMessage 要读取的 HTTP 输入消息
	 * @return 转换后的对象
	 * @throws IOException                     在 I/O 错误时抛出
	 * @throws HttpMessageNotReadableException 在转换错误时抛出
	 */
	T read(Type type, @Nullable Class<?> contextClass, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException;

	/**
	 * 指示此转换器是否可以写入给定的类。
	 * <p>此方法应执行与 {@link HttpMessageConverter#canWrite(Class, MediaType)} 相同的检查，
	 * 并添加与泛型类型相关的其他检查。
	 *
	 * @param type      要测试可写性的泛型类型（如果未指定可以为 {@code null}）
	 * @param clazz     要测试可写性的源对象类
	 * @param mediaType 要写入的媒体类型（如果未指定可以为 {@code null}）；
	 *                  通常是 {@code Accept} 标头的值。
	 * @return {@code true} 如果可写；{@code false} 否则
	 * @since 4.2
	 */
	boolean canWrite(@Nullable Type type, Class<?> clazz, @Nullable MediaType mediaType);

	/**
	 * 将给定对象写入到给定的输出消息中。
	 *
	 * @param t             要写入到输出消息的对象。此对象的类型必须先前已传递给此接口的 {@link #canWrite canWrite} 方法，
	 *                      该方法必须返回 {@code true}。
	 * @param type          要写入的对象的泛型类型。此类型必须先前已传递给此接口的 {@link #canWrite canWrite} 方法，
	 *                      该方法必须返回 {@code true}。如果未指定可以为 {@code null}。
	 * @param contentType   写入时要使用的内容类型。可以为 {@code null}，表示必须使用转换器的默认内容类型。
	 *                      如果不为 {@code null}，则此媒体类型必须先前已传递给此接口的 {@link #canWrite canWrite} 方法，
	 *                      该方法必须返回 {@code true}。
	 * @param outputMessage 要写入的消息
	 * @throws IOException                     在 I/O 错误时抛出
	 * @throws HttpMessageNotWritableException 在转换错误时抛出
	 * @since 4.2
	 */
	void write(T t, @Nullable Type type, @Nullable MediaType contentType, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException;

}
