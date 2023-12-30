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

package org.springframework.web.reactive.resource;

import org.springframework.core.io.Resource;
import org.springframework.http.server.RequestPath;
import org.springframework.lang.Nullable;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 用于解析服务器端资源请求的策略。
 *
 * <p>提供了解析传入请求到实际 {@link Resource} 的机制，
 * 以及获取客户端在请求资源时应使用的公共 URL 路径。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface ResourceResolver {

	/**
	 * 将提供的请求和请求路径解析为位于给定资源位置下存在的 {@link Resource}。
	 *
	 * @param exchange    当前的 exchange
	 * @param requestPath 要使用的请求路径部分。预计这是编码后的路径，即 {@link RequestPath#value()}。
	 * @param locations   查找资源时要搜索的位置
	 * @param chain       剩余解析器的链，用于委托
	 * @return 已解析的资源，如果未解析则为空的 {@code Mono}
	 */
	Mono<Resource> resolveResource(@Nullable ServerWebExchange exchange, String requestPath,
								   List<? extends Resource> locations, ResourceResolverChain chain);

	/**
	 * 解析外部面向客户端的 <em>公共</em> URL 路径，以便访问位于给定 <em>内部</em> 资源路径的资源。
	 * <p>在为客户端渲染 URL 链接时非常有用。
	 *
	 * @param resourcePath 要解析用于公共使用的 "内部" 资源路径。预计这是编码后的路径。
	 * @param locations    查找资源时要搜索的位置
	 * @param chain        要委托给的解析器链
	 * @return 已解析的公共 URL 路径，如果未解析则为空的 {@code Mono}
	 */
	Mono<String> resolveUrlPath(String resourcePath, List<? extends Resource> locations,
								ResourceResolverChain chain);
}
