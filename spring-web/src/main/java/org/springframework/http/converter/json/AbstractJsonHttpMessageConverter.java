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

package org.springframework.http.converter.json;

import org.springframework.core.GenericTypeResolver;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractGenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.lang.Nullable;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 用于普通 JSON 转换器的通用基类，例如 Gson 和 JSON-B。
 *
 * <p>注意，Jackson 转换器由于支持多种格式，有专门的类层次结构。
 *
 * @author Juergen Hoeller
 * @see GsonHttpMessageConverter
 * @see JsonbHttpMessageConverter
 * @see #readInternal(Type, Reader)
 * @see #writeInternal(Object, Type, Writer)
 * @since 5.0
 */
public abstract class AbstractJsonHttpMessageConverter extends AbstractGenericHttpMessageConverter<Object> {

	/**
	 * 转换器使用的默认字符集。
	 */
	public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

	/**
	 * JSON前缀
	 */
	@Nullable
	private String jsonPrefix;


	public AbstractJsonHttpMessageConverter() {
		super(MediaType.APPLICATION_JSON, new MediaType("application", "*+json"));
		setDefaultCharset(DEFAULT_CHARSET);
	}


	/**
	 * 设置用于 JSON 输出的自定义前缀。默认为空。
	 *
	 * @see #setPrefixJson
	 */
	public void setJsonPrefix(String jsonPrefix) {
		this.jsonPrefix = jsonPrefix;
	}

	/**
	 * 指示是否应在此视图输出的 JSON 前缀中添加 ")]}', "。默认为 {@code false}。
	 * <p>以这种方式为 JSON 字符串添加前缀是为了帮助防止 JSON 劫持。
	 * 前缀使字符串在语法上无效，因此无法被劫持。
	 * 在解析 JSON 字符串之前应该去除此前缀。
	 *
	 * @see #setJsonPrefix
	 */
	public void setPrefixJson(boolean prefixJson) {
		this.jsonPrefix = (prefixJson ? ")]}', " : null);
	}


	@Override
	public final Object read(Type type, @Nullable Class<?> contextClass, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {

		return readResolved(GenericTypeResolver.resolveType(type, contextClass), inputMessage);
	}

	@Override
	protected final Object readInternal(Class<?> clazz, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {

		return readResolved(clazz, inputMessage);
	}

	private Object readResolved(Type resolvedType, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {

		// 从输入消息中获取一个读取器对象，用于读取输入数据
		Reader reader = getReader(inputMessage);
		try {
			// 调用readInternal方法读取数据，并返回结果
			return readInternal(resolvedType, reader);
		} catch (Exception ex) {
			// 捕获可能的异常，抛出HttpMessageNotReadableException异常
			throw new HttpMessageNotReadableException("Could not read JSON: " + ex.getMessage(), ex, inputMessage);
		}
	}

	@Override
	protected final void writeInternal(Object object, @Nullable Type type, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {

		// 获取输出消息的写入器对象，用于向输出消息中写入数据
		Writer writer = getWriter(outputMessage);

		// 如果设置了JSON前缀，将其追加到写入器中
		if (this.jsonPrefix != null) {
			writer.append(this.jsonPrefix);
		}

		try {
			// 调用writeInternal方法将对象写入输出消息
			writeInternal(object, type, writer);
		} catch (Exception ex) {
			// 捕获可能的异常，抛出HttpMessageNotWritableException异常
			throw new HttpMessageNotWritableException("Could not write JSON: " + ex.getMessage(), ex);
		}

		// 刷新写入器，确保所有数据都写入到输出消息中
		writer.flush();
	}


	/**
	 * 模板方法，从给定的 {@link Reader} 中读取 JSON 绑定对象。
	 *
	 * @param resolvedType 要解析的泛型类型
	 * @param reader       要使用的 {@code} Reader
	 * @return 绑定的 JSON 对象
	 * @throws Exception 读取/解析失败时
	 */
	protected abstract Object readInternal(Type resolvedType, Reader reader) throws Exception;

	/**
	 * 模板方法，将 JSON 绑定对象写入给定的 {@link Writer}。
	 *
	 * @param object 要写入输出消息的对象
	 * @param type   要写入的对象类型（可能为 {@code null}）
	 * @param writer 要使用的 {@code} Writer
	 * @throws Exception 写入失败时
	 */
	protected abstract void writeInternal(Object object, @Nullable Type type, Writer writer) throws Exception;


	private static Reader getReader(HttpInputMessage inputMessage) throws IOException {
		return new InputStreamReader(inputMessage.getBody(), getCharset(inputMessage.getHeaders()));
	}

	private static Writer getWriter(HttpOutputMessage outputMessage) throws IOException {
		return new OutputStreamWriter(outputMessage.getBody(), getCharset(outputMessage.getHeaders()));
	}

	private static Charset getCharset(HttpHeaders headers) {
		// 获取内容类型的字符集
		Charset charset = (headers.getContentType() != null ? headers.getContentType().getCharset() : null);

		// 如果获取到了字符集，则返回该字符集；否则返回默认字符集
		return (charset != null ? charset : DEFAULT_CHARSET);
	}

}
