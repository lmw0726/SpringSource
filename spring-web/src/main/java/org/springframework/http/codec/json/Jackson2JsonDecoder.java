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

package org.springframework.http.codec.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.reactivestreams.Publisher;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.StringDecoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.lang.Nullable;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Flux;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;

/**
 * 使用Jackson 2.9解码字节流为JSON，并转换为对象，利用非阻塞解析。
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @see Jackson2JsonEncoder
 * @since 5.0
 */
public class Jackson2JsonDecoder extends AbstractJackson2Decoder {
	/**
	 * 字符串解码器
	 */
	private static final StringDecoder STRING_DECODER = StringDecoder.textPlainOnly(Arrays.asList(",", "\n"), false);

	/**
	 * 字符串类型
	 */
	private static final ResolvableType STRING_TYPE = ResolvableType.forClass(String.class);

	/**
	 * 默认构造函数，使用默认的ObjectMapper。
	 */
	public Jackson2JsonDecoder() {
		super(Jackson2ObjectMapperBuilder.json().build());
	}

	/**
	 * 使用指定的ObjectMapper和Mime类型构造解码器。
	 *
	 * @param mapper    用于解码的ObjectMapper
	 * @param mimeTypes 支持的Mime类型
	 */
	public Jackson2JsonDecoder(ObjectMapper mapper, MimeType... mimeTypes) {
		super(mapper, mimeTypes);
	}

	@Override
	protected Flux<DataBuffer> processInput(Publisher<DataBuffer> input, ResolvableType elementType,
											@Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		Flux<DataBuffer> flux = Flux.from(input);
		if (mimeType == null) {
			// 如果Mime类型为空，则返回 输入数据构成的Flux
			return flux;
		}

		// Jackson异步解析器仅支持UTF-8
		// 获取字符集
		Charset charset = mimeType.getCharset();
		if (charset == null || StandardCharsets.UTF_8.equals(charset) || StandardCharsets.US_ASCII.equals(charset)) {
			// 如果字符集为空，或者字符集是UTF_8，或者是US_ASCII，则返回 输入数据构成的Flux
			return flux;
		}

		// 潜在地，这种转换的内存消耗可以通过使用CharBuffers而不是分配字符串来改进，
		// 但这需要重构StringDecoder的缓冲区标记化代码

		// 根据字符集构建文本Mime类型
		MimeType textMimeType = new MimeType(MimeTypeUtils.TEXT_PLAIN, charset);
		// 解码字符串 并转为Flux
		Flux<String> decoded = STRING_DECODER.decode(input, STRING_TYPE, textMimeType, null);
		// 转换为 数据缓冲区
		return decoded.map(s -> DefaultDataBufferFactory.sharedInstance.wrap(s.getBytes(StandardCharsets.UTF_8)));
	}

}
