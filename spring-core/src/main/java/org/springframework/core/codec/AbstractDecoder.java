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

package org.springframework.core.codec;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Publisher;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.lang.Nullable;
import org.springframework.util.MimeType;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * {@link Decoder} 实现的抽象基类。
 *
 * @author Sebastien Deleuze
 * @author Arjen Poutsma
 * @since 5.0
 * @param <T> 元素类型
 */
public abstract class AbstractDecoder<T> implements Decoder<T> {

	private final List<MimeType> decodableMimeTypes;

	protected Log logger = LogFactory.getLog(getClass());


	protected AbstractDecoder(MimeType... supportedMimeTypes) {
		this.decodableMimeTypes = Arrays.asList(supportedMimeTypes);
	}


	/**
	 * 设置一个替代的日志记录器，而不是基于类名称的日志记录器。
	 * @param logger 要使用的日志记录器
	 * @since 5.1
	 */
	public void setLogger(Log logger) {
		this.logger = logger;
	}

	/**
	 * 返回当前配置的日志记录器。
	 * @since 5.1
	 */
	public Log getLogger() {
		return logger;
	}


	@Override
	public List<MimeType> getDecodableMimeTypes() {
		return this.decodableMimeTypes;
	}

	@Override
	public boolean canDecode(ResolvableType elementType, @Nullable MimeType mimeType) {
		if (mimeType == null) {
			return true;
		}
		for (MimeType candidate : this.decodableMimeTypes) {
			if (candidate.isCompatibleWith(mimeType)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Mono<T> decodeToMono(Publisher<DataBuffer> inputStream, ResolvableType elementType,
			@Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		throw new UnsupportedOperationException();
	}

}
