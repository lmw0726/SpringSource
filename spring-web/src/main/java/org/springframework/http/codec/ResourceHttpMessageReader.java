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

package org.springframework.http.codec;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Hints;
import org.springframework.core.codec.ResourceDecoder;
import org.springframework.core.io.Resource;
import org.springframework.http.ReactiveHttpInputMessage;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * {@code HttpMessageReader}，包装并委托给 {@link ResourceDecoder}，
 * 从 {@code "Content-Disposition"} 头中提取文件名（如果可用），并将其作为 {@link ResourceDecoder#FILENAME_HINT} 传递。
 *
 * @author Rossen Stoyanchev
 * @since 5.2
 */
public class ResourceHttpMessageReader extends DecoderHttpMessageReader<Resource> {

	public ResourceHttpMessageReader() {
		super(new ResourceDecoder());
	}

	public ResourceHttpMessageReader(ResourceDecoder resourceDecoder) {
		super(resourceDecoder);
	}


	@Override
	protected Map<String, Object> getReadHints(ResolvableType elementType, ReactiveHttpInputMessage message) {
		// 获取文件名称
		String filename = message.getHeaders().getContentDisposition().getFilename();
		// 如果文件名称不为空，则这是文件名称提示，否则返回空的提示。
		return (StringUtils.hasText(filename) ?
				Hints.from(ResourceDecoder.FILENAME_HINT, filename) : Hints.none());
	}

	@Override
	protected Map<String, Object> getReadHints(ResolvableType actualType, ResolvableType elementType,
											   ServerHttpRequest request, ServerHttpResponse response) {

		return getReadHints(elementType, request);
	}

}
