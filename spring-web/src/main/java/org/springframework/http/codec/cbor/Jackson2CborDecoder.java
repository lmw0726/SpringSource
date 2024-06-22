/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.http.codec.cbor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import org.reactivestreams.Publisher;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.AbstractJackson2Decoder;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * 使用Jackson解码字节为CBOR并转换为对象。
 * 目前不支持流解码。
 *
 * @author Sebastien Deleuze
 * @since 5.2
 * @see Jackson2CborEncoder
 * @see <a href="https://github.com/spring-projects/spring-framework/issues/20513">将CBOR支持添加到WebFlux</a>
 */
public class Jackson2CborDecoder extends AbstractJackson2Decoder {

	/**
	 * 默认构造函数，使用默认的ObjectMapper和CBOR媒体类型。
	 */
	public Jackson2CborDecoder() {
		this(Jackson2ObjectMapperBuilder.cbor().build(), MediaType.APPLICATION_CBOR);
	}

	/**
	 * 使用指定的ObjectMapper和Mime类型构造解码器。
	 *
	 * @param mapper    用于解码的ObjectMapper
	 * @param mimeTypes 支持的Mime类型
	 */
	public Jackson2CborDecoder(ObjectMapper mapper, MimeType... mimeTypes) {
		super(mapper, mimeTypes);
		Assert.isAssignable(CBORFactory.class, mapper.getFactory().getClass());
	}


	@Override
	public Flux<Object> decode(Publisher<DataBuffer> input, ResolvableType elementType, MimeType mimeType, Map<String, Object> hints) {
		throw new UnsupportedOperationException("Does not support stream decoding yet");
	}

}
