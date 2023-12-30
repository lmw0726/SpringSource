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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.server.RequestPath;
import org.springframework.lang.Nullable;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 提供了一致的日志记录的基础 {@link ResourceResolver}。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public abstract class AbstractResourceResolver implements ResourceResolver {

	protected final Log logger = LogFactory.getLog(getClass());

	/**
	 * 将提供的请求和请求路径解析为位于给定资源位置下存在的 {@link Resource}。
	 *
	 * @param exchange    当前的 exchange
	 * @param requestPath 要使用的请求路径部分。预计这是编码后的路径，即 {@link RequestPath#value()}。
	 * @param locations   查找资源时要搜索的位置
	 * @param chain       剩余解析器的链，用于委托
	 * @return 已解析的资源，如果未解析则为空的 {@code Mono}
	 */
	@Override
	public Mono<Resource> resolveResource(@Nullable ServerWebExchange exchange, String requestPath,
										  List<? extends Resource> locations, ResourceResolverChain chain) {

		return resolveResourceInternal(exchange, requestPath, locations, chain);
	}

	/**
	 * 解析资源的公共 URL 路径。
	 *
	 * @param resourceUrlPath 要解析用于公共使用的 "内部" 资源路径，通常是编码后的路径
	 * @param locations       要在其中查找资源的位置列表
	 * @param chain           要委托给的资源解析器链
	 * @return 解析的公共 URL 路径或空的 {@code Mono}
	 */
	@Override
	public Mono<String> resolveUrlPath(String resourceUrlPath, List<? extends Resource> locations,
									   ResourceResolverChain chain) {

		return resolveUrlPathInternal(resourceUrlPath, locations, chain);
	}

	/**
	 * 解析请求到实际资源的内部方法。
	 *
	 * @param exchange    当前的服务器Web交换，可能为空
	 * @param requestPath 要解析的请求路径部分，通常是编码后的路径
	 * @param locations   要在其中查找资源的位置列表
	 * @param chain       用于委托给其他资源解析器的链
	 * @return 已解析的资源或空的 {@code Mono}
	 */
	protected abstract Mono<Resource> resolveResourceInternal(@Nullable ServerWebExchange exchange,
															  String requestPath, List<? extends Resource> locations, ResourceResolverChain chain);


	/**
	 * 解析资源公共 URL 路径的内部方法。
	 *
	 * @param resourceUrlPath 要解析用于公共使用的 "内部" 资源路径，通常是编码后的路径
	 * @param locations       要在其中查找资源的位置列表
	 * @param chain           要委托给的资源解析器链
	 * @return 解析的公共 URL 路径或空的 {@code Mono}
	 */
	protected abstract Mono<String> resolveUrlPathInternal(String resourceUrlPath,
														   List<? extends Resource> locations, ResourceResolverChain chain);

}
