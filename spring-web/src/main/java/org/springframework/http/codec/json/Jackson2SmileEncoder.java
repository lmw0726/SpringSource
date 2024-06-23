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

package org.springframework.http.codec.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.List;

/**
 * 使用Jackson 2.9将{@code Object}流编码为Smile对象的字节流。
 * 对于非流式使用情况，{@link Flux}元素在序列化之前会被收集到一个{@link List}中，以提高性能。
 *
 * @author Sebastien Deleuze
 * @see Jackson2SmileDecoder
 * @since 5.0
 */
public class Jackson2SmileEncoder extends AbstractJackson2Encoder {
	/**
	 * 默认Smile MIME类型列表
	 */
	private static final MimeType[] DEFAULT_SMILE_MIME_TYPES = new MimeType[]{
			new MimeType("application", "x-jackson-smile"),
			new MimeType("application", "*+x-jackson-smile")};

	/**
	 * 流分隔符
	 */
	private static final byte[] STREAM_SEPARATOR = new byte[0];


	public Jackson2SmileEncoder() {
		this(Jackson2ObjectMapperBuilder.smile().build(), DEFAULT_SMILE_MIME_TYPES);
	}

	public Jackson2SmileEncoder(ObjectMapper mapper, MimeType... mimeTypes) {
		super(mapper, mimeTypes);
		Assert.isAssignable(SmileFactory.class, mapper.getFactory().getClass());
		// 设置流媒体类型为 application/x-jackson-smile
		setStreamingMediaTypes(Collections.singletonList(new MediaType("application", "stream+x-jackson-smile")));
	}


	/**
	 * 返回用于给定MIME类型的分隔符。
	 * <p>默认情况下，如果给定的MIME类型是配置的{@link #setStreamingMediaTypes(List)流式}媒体类型之一，
	 * 此方法返回一个字节0。
	 *
	 * @since 5.3
	 */
	@Nullable
	@Override
	protected byte[] getStreamingMediaTypeSeparator(@Nullable MimeType mimeType) {
		// 遍历流媒体类型列表
		for (MediaType streamingMediaType : getStreamingMediaTypes()) {
			// 检查当前流媒体类型是否与给定的 Mime类型 兼容
			if (streamingMediaType.isCompatibleWith(mimeType)) {
				// 如果兼容，返回流分隔符
				return STREAM_SEPARATOR;
			}
		}
		// 如果没有找到兼容的流媒体类型，返回 null
		return null;
	}
}
