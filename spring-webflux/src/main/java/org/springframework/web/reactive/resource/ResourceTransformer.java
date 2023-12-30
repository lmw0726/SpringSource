/*
 * Copyright 2002-2016 the original author or authors.
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

import reactor.core.publisher.Mono;

import org.springframework.core.io.Resource;
import org.springframework.web.server.ServerWebExchange;

/**
 * {@code ResourceTransformer} 是用于转换资源内容的抽象。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
@FunctionalInterface
public interface ResourceTransformer {

	/**
	 * 转换给定的资源。
	 *
	 * @param exchange         当前的交换对象
	 * @param resource         要转换的资源
	 * @param transformerChain 剩余转换器链，用于委托转换
	 * @return 转换后的资源（不会为空）
	 */
	Mono<Resource> transform(ServerWebExchange exchange, Resource resource,
							 ResourceTransformerChain transformerChain);

}
