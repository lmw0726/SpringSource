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

package org.springframework.web.reactive.resource;

import reactor.core.publisher.Mono;

import org.springframework.core.io.Resource;
import org.springframework.web.server.ServerWebExchange;

/**
 * 一个用于调用一系列 {@link ResourceTransformer 资源转换器} 的契约，其中每个转换器都可以访问到链中的下一个转换器，
 * 从而在必要时进行委托处理。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface ResourceTransformerChain {

	/**
	 * 返回用于解析正在被转换的 {@code Resource} 的 {@code ResourceResolverChain}。
	 * 这可能在解析相关资源（例如链接到其他资源）时需要使用。
	 *
	 * @return 使用过的 {@code ResourceResolverChain}
	 */
	ResourceResolverChain getResolverChain();

	/**
	 * 对给定的资源进行转换。
	 *
	 * @param exchange 当前的交换对象
	 * @param resource 待转换的资源
	 * @return 转换后的资源，不会为空
	 */
	Mono<Resource> transform(ServerWebExchange exchange, Resource resource);

}
