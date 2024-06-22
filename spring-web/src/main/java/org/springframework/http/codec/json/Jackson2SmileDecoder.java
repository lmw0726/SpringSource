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

package org.springframework.http.codec.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;

import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;

/**
 * 使用Jackson解码字节流为Smile格式，并转换为对象。
 * 利用非阻塞解析功能。
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @see Jackson2JsonEncoder
 * @since 5.0
 */
public class Jackson2SmileDecoder extends AbstractJackson2Decoder {
	/**
	 * 默认Smile媒体类型
	 */
	private static final MimeType[] DEFAULT_SMILE_MIME_TYPES = new MimeType[]{
			new MimeType("application", "x-jackson-smile"),
			new MimeType("application", "*+x-jackson-smile")};


	/**
	 * 默认构造函数，使用默认的ObjectMapper和Smile媒体类型。
	 */
	public Jackson2SmileDecoder() {
		this(Jackson2ObjectMapperBuilder.smile().build(), DEFAULT_SMILE_MIME_TYPES);
	}

	/**
	 * 使用指定的ObjectMapper和Mime类型构造解码器。
	 *
	 * @param mapper    用于解码的ObjectMapper
	 * @param mimeTypes 支持的Mime类型
	 */
	public Jackson2SmileDecoder(ObjectMapper mapper, MimeType... mimeTypes) {
		super(mapper, mimeTypes);
		Assert.isAssignable(SmileFactory.class, mapper.getFactory().getClass());
	}

}
