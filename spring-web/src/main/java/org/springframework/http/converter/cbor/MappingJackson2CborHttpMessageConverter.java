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

package org.springframework.http.converter.cbor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;

import org.springframework.http.MediaType;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.util.Assert;

/**
 * 实现 {@link org.springframework.http.converter.HttpMessageConverter
 * HttpMessageConverter} 接口，可以使用 <a href="https://cbor.io/">CBOR</a>
 * 数据格式读写，使用 <a href="https://github.com/FasterXML/jackson-dataformats-binary/tree/master/cbor">
 * 专用的 Jackson 2.x 扩展</a>。
 *
 * <p>默认情况下，此转换器支持 {@link MediaType#APPLICATION_CBOR_VALUE}
 * 媒体类型。可以通过设置 {@link #setSupportedMediaTypes
 * supportedMediaTypes} 属性进行覆盖。
 *
 * <p>默认构造函数使用由 {@link Jackson2ObjectMapperBuilder}
 * 提供的默认配置。
 *
 * <p>从 Spring 5.3 开始兼容 Jackson 2.9 到 2.12。
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
public class MappingJackson2CborHttpMessageConverter extends AbstractJackson2HttpMessageConverter {

	/**
	 * 使用 {@code Jackson2ObjectMapperBuilder} 提供的默认配置构造新的
	 * {@code MappingJackson2CborHttpMessageConverter}。
	 */
	public MappingJackson2CborHttpMessageConverter() {
		this(Jackson2ObjectMapperBuilder.cbor().build());
	}

	/**
	 * 使用自定义 {@link ObjectMapper} 构造新的
	 * {@code MappingJackson2CborHttpMessageConverter}。
	 * <p>必须使用配置有 {@code CBORFactory} 实例的 {@link ObjectMapper}。
	 * 可以使用 {@link Jackson2ObjectMapperBuilder} 轻松构建。
	 *
	 * @see Jackson2ObjectMapperBuilder#cbor()
	 */
	public MappingJackson2CborHttpMessageConverter(ObjectMapper objectMapper) {
		super(objectMapper, MediaType.APPLICATION_CBOR);
		Assert.isInstanceOf(CBORFactory.class, objectMapper.getFactory(), "CBORFactory required");
	}


	/**
	 * {@inheritDoc}
	 * {@code ObjectMapper} 必须配置有 {@code CBORFactory} 实例。
	 */
	@Override
	public void setObjectMapper(ObjectMapper objectMapper) {
		Assert.isInstanceOf(CBORFactory.class, objectMapper.getFactory(), "CBORFactory required");
		super.setObjectMapper(objectMapper);
	}

}
