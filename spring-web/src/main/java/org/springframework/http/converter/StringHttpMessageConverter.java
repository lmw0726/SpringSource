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

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 实现了 {@link HttpMessageConverter} 接口，用于读取和写入字符串。
 *
 * <p>默认情况下，此转换器支持所有媒体类型（<code>&#42;/&#42;</code>），
 * 并使用 {@code Content-Type} 为 {@code text/plain} 进行写入。可以通过设置 {@link #setSupportedMediaTypes supportedMediaTypes}
 * 属性进行覆盖。
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 3.0
 */
public class StringHttpMessageConverter extends AbstractHttpMessageConverter<String> {
	/**
	 * JSON媒体类型
	 */
	private static final MediaType APPLICATION_PLUS_JSON = new MediaType("application", "*+json");

	/**
	 * 转换器使用的默认字符集。
	 */
	public static final Charset DEFAULT_CHARSET = StandardCharsets.ISO_8859_1;

	/**
	 * 可用字符集列表
	 */
	@Nullable
	private volatile List<Charset> availableCharsets;

	/**
	 * 是否写入接受字符集
	 */
	private boolean writeAcceptCharset = false;


	/**
	 * 使用 {@code "ISO-8859-1"} 作为默认字符集的默认构造函数。
	 *
	 * @see #StringHttpMessageConverter(Charset)
	 */
	public StringHttpMessageConverter() {
		this(DEFAULT_CHARSET);
	}

	/**
	 * 接受一个默认字符集参数的构造函数，用于在请求的内容类型未指定时使用。
	 */
	public StringHttpMessageConverter(Charset defaultCharset) {
		super(defaultCharset, MediaType.TEXT_PLAIN, MediaType.ALL);
	}


	/**
	 * 设置是否应该向任何传出请求写入 {@code Accept-Charset} 头部，源自 {@link Charset#availableCharsets()} 的值。
	 * 如果头部已设置，则行为会被抑制。
	 * <p>从 5.2 开始，默认设置为 {@code false}。
	 */
	public void setWriteAcceptCharset(boolean writeAcceptCharset) {
		this.writeAcceptCharset = writeAcceptCharset;
	}


	@Override
	public boolean supports(Class<?> clazz) {
		return String.class == clazz;
	}

	@Override
	protected String readInternal(Class<? extends String> clazz, HttpInputMessage inputMessage) throws IOException {
		// 获取内容类型对应的字符集
		Charset charset = getContentTypeCharset(inputMessage.getHeaders().getContentType());
		// 将输入消息的主体内容复制为字符串，并使用指定的字符集解码
		return StreamUtils.copyToString(inputMessage.getBody(), charset);
	}

	@Override
	protected Long getContentLength(String str, @Nullable MediaType contentType) {
		// 获取内容类型对应的字符集
		Charset charset = getContentTypeCharset(contentType);
		// 计算字符串在指定字符集下的字节长度并返回
		return (long) str.getBytes(charset).length;
	}


	@Override
	protected void addDefaultHeaders(HttpHeaders headers, String s, @Nullable MediaType type) throws IOException {
		// 如果响应头中的 内容类型 为空
		if (headers.getContentType() == null) {
			// 如果内容类型不为空，并且内容类型是具体的
			// 并且内容类型是Json媒体类型或者是应用JSON类型
			if (type != null && type.isConcrete() &&
					(type.isCompatibleWith(MediaType.APPLICATION_JSON) ||
							type.isCompatibleWith(APPLICATION_PLUS_JSON))) {
				// 对于 JSON 类型，避免设置字符集参数
				headers.setContentType(type);
			}
		}
		// 调用父类方法添加默认头部
		super.addDefaultHeaders(headers, s, type);
	}

	@Override
	protected void writeInternal(String str, HttpOutputMessage outputMessage) throws IOException {
		// 获取输出消息的头部
		HttpHeaders headers = outputMessage.getHeaders();
		// 如果写入接受字符集标头，并且头部中未设置 Accept-Charset
		if (this.writeAcceptCharset && headers.get(HttpHeaders.ACCEPT_CHARSET) == null) {
			// 设置接受字符集
			headers.setAcceptCharset(getAcceptedCharsets());
		}
		// 获取内容类型对应的字符集
		Charset charset = getContentTypeCharset(headers.getContentType());
		// 写入到输出消息的主体中
		StreamUtils.copy(str, charset, outputMessage.getBody());
	}


	/**
	 * 返回支持的 {@link Charset 字符集} 列表。
	 * <p>默认情况下，返回 {@link Charset#availableCharsets()}。
	 * 可以在子类中进行重写。
	 *
	 * @return 接受的字符集列表
	 */
	protected List<Charset> getAcceptedCharsets() {
		// 获取可用的字符集列表
		List<Charset> charsets = this.availableCharsets;
		// 如果可用字符集列表为空
		if (charsets == null) {
			// 获取系统中所有可用的字符集并存储在一个新的列表中
			charsets = new ArrayList<>(Charset.availableCharsets().values());
			// 将新创建的字符集列表 设置为可用字符集列表
			this.availableCharsets = charsets;
		}
		// 返回字符集列表
		return charsets;
	}

	private Charset getContentTypeCharset(@Nullable MediaType contentType) {
		// 如果 内容类型 不为空
		if (contentType != null) {
			// 获取 内容类型 中的字符集
			Charset charset = contentType.getCharset();
			// 如果字符集不为空
			if (charset != null) {
				// 返回获取到的字符集
				return charset;
				// 如果 内容类型 是兼容 JSON 的类型
			} else if (contentType.isCompatibleWith(MediaType.APPLICATION_JSON) ||
					contentType.isCompatibleWith(APPLICATION_PLUS_JSON)) {
				// 返回 默认字符集 UTF-8
				return StandardCharsets.UTF_8;
			}
		}
		// 获取默认字符集
		Charset charset = getDefaultCharset();
		// 断言默认字符集不为空，如果为空则抛出异常
		Assert.state(charset != null, "No default charset");
		// 返回默认字符集
		return charset;
	}

}
