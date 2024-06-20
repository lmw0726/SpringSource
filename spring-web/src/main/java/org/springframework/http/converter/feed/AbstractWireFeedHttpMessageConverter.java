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

package org.springframework.http.converter.feed;

import com.rometools.rome.feed.WireFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.WireFeedInput;
import com.rometools.rome.io.WireFeedOutput;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Atom 和 RSS Feed 消息转换器的抽象基类，使用 <a href="https://github.com/rometools/rome">ROME tools</a> 项目。
 *
 * <p><b>注意: 从 Spring 4.1 开始，基于 {@code com.rometools} 的 ROME 变体，版本为 1.5。请升级您的构建依赖。</b>
 *
 * @param <T> 转换的对象类型
 * @author Arjen Poutsma
 * @see AtomFeedHttpMessageConverter
 * @see RssChannelHttpMessageConverter
 * @since 3.0.2
 */
public abstract class AbstractWireFeedHttpMessageConverter<T extends WireFeed>
		extends AbstractHttpMessageConverter<T> {

	/**
	 * 转换器使用的默认字符集。
	 */
	public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;


	protected AbstractWireFeedHttpMessageConverter(MediaType supportedMediaType) {
		super(supportedMediaType);
	}


	@Override
	@SuppressWarnings("unchecked")
	protected T readInternal(Class<? extends T> clazz, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {

		WireFeedInput feedInput = new WireFeedInput();
		// 获取输入消息的内容类型
		MediaType contentType = inputMessage.getHeaders().getContentType();
		// 获取字符集，如果 内容类型 不为 null， 且含有字符集信息，则使用该字符集；
		// 否则使用默认字符集
		Charset charset = (contentType != null && contentType.getCharset() != null ?
				contentType.getCharset() : DEFAULT_CHARSET);
		try {
			// 获取输入消息的输入流，并确保在处理完后不关闭流
			InputStream inputStream = StreamUtils.nonClosing(inputMessage.getBody());
			// 使用指定的字符集创建 InputStreamReader
			Reader reader = new InputStreamReader(inputStream, charset);
			// 使用 feedInput 解析构建 WireFeed 对象并返回
			return (T) feedInput.build(reader);
		} catch (FeedException ex) {
			// 如果解析过程中出现 FeedException，抛出消息不可读异常
			throw new HttpMessageNotReadableException("Could not read WireFeed: " + ex.getMessage(), ex, inputMessage);
		}
	}

	@Override
	protected void writeInternal(T wireFeed, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {

		// 获取字符集，如果 wireFeed 的编码不为空，则使用该编码的字符集；否则使用默认字符集
		Charset charset = (StringUtils.hasLength(wireFeed.getEncoding()) ?
				Charset.forName(wireFeed.getEncoding()) : DEFAULT_CHARSET);
		// 获取输出消息的内容类型
		MediaType contentType = outputMessage.getHeaders().getContentType();
		// 如果内容类型不为 null
		if (contentType != null) {
			// 使用字符集创建新的内容类型
			contentType = new MediaType(contentType, charset);
			// 设置到输出消息的头部
			outputMessage.getHeaders().setContentType(contentType);
		}

		// 创建 WireFeedOutput 对象
		WireFeedOutput feedOutput = new WireFeedOutput();
		try {
			// 使用指定的字符集创建 OutputStreamWriter
			Writer writer = new OutputStreamWriter(outputMessage.getBody(), charset);
			// 将 wireFeed 输出到输出消息的主体中
			feedOutput.output(wireFeed, writer);
		} catch (FeedException ex) {
			// 如果输出过程中出现 FeedException，抛出消息不可写异常
			throw new HttpMessageNotWritableException("Could not write WireFeed: " + ex.getMessage(), ex);
		}
	}

}
