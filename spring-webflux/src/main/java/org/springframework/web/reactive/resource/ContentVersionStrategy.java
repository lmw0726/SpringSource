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

package org.springframework.web.reactive.resource;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.util.DigestUtils;
import org.springframework.util.StreamUtils;

/**
 * {@code ContentVersionStrategy} 是一个版本策略，它从资源内容计算出 Hex MD5 哈希，并将其附加到文件名中，例如
 * {@code "styles/main-e36d2e05253c6c7085a91522ce43a0b4.css"}。
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @see VersionResourceResolver
 * @since 5.0
 */
public class ContentVersionStrategy extends AbstractFileNameVersionStrategy {

	/**
	 * 从给定资源获取版本信息。
	 *
	 * @param resource 要获取版本信息的资源
	 * @return 代表资源版本的 Mono（异步结果）
	 */
	@Override
	public Mono<String> getResourceVersion(Resource resource) {
		// 从资源中读取数据并创建一个数据流 Flux
		Flux<DataBuffer> flux = DataBufferUtils.read(
				resource, DefaultDataBufferFactory.sharedInstance, StreamUtils.BUFFER_SIZE);

		// 将数据流中的数据缓冲区合并为一个单独的 DataBuffer
		return DataBufferUtils.join(flux)
				.map(buffer -> {
					// 创建一个字节数组，大小为可读字节数
					byte[] result = new byte[buffer.readableByteCount()];
					// 将数据从缓冲区读取到结果字节数组中
					buffer.read(result);
					// 释放当前缓冲区资源
					DataBufferUtils.release(buffer);
					// 返回结果字节数组的 MD5 散列值作为字符串
					return DigestUtils.md5DigestAsHex(result);
				});
	}

}
