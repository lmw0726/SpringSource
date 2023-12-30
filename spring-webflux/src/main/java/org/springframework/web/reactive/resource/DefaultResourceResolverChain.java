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

import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

/**
 * {@link ResourceResolverChain} 的默认不可变实现。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
class DefaultResourceResolverChain implements ResourceResolverChain {

	/**
	 * 资源解析器
	 */
	@Nullable
	private final ResourceResolver resolver;

	/**
	 * 下一个资源解析器链
	 */
	@Nullable
	private final ResourceResolverChain nextChain;


	public DefaultResourceResolverChain(@Nullable List<? extends ResourceResolver> resolvers) {
		resolvers = (resolvers != null ? resolvers : Collections.emptyList());
		DefaultResourceResolverChain chain = initChain(new ArrayList<>(resolvers));
		this.resolver = chain.resolver;
		this.nextChain = chain.nextChain;
	}

	/**
	 * 初始化资源解析器链。
	 *
	 * @param resolvers 资源解析器列表
	 * @return 默认资源解析器链
	 */
	private static DefaultResourceResolverChain initChain(ArrayList<? extends ResourceResolver> resolvers) {
		// 创建一个具有默认值的资源解析器链
		DefaultResourceResolverChain chain = new DefaultResourceResolverChain(null, null);

		// 逆向遍历解析器列表
		ListIterator<? extends ResourceResolver> it = resolvers.listIterator(resolvers.size());
		while (it.hasPrevious()) {
			// 以当前解析器和前一个链为参数，创建新的解析器链
			chain = new DefaultResourceResolverChain(it.previous(), chain);
		}

		// 返回组装好的资源解析器链
		return chain;
	}

	private DefaultResourceResolverChain(@Nullable ResourceResolver resolver, @Nullable ResourceResolverChain chain) {
		Assert.isTrue((resolver == null && chain == null) || (resolver != null && chain != null),
				"Both resolver and resolver chain must be null, or neither is");
		this.resolver = resolver;
		this.nextChain = chain;
	}


	/**
	 * 解析资源并返回结果。
	 *
	 * @param exchange   当前的服务器网络交换对象
	 * @param requestPath 请求的资源路径
	 * @param locations  资源位置列表
	 * @return 资源解析结果的 Mono 对象，如果无法解析则返回空 Mono
	 */
	@Override
	public Mono<Resource> resolveResource(@Nullable ServerWebExchange exchange, String requestPath,
										  List<? extends Resource> locations) {
		return (this.resolver != null && this.nextChain != null ?
				this.resolver.resolveResource(exchange, requestPath, locations, this.nextChain) :
				Mono.empty());
	}

	/**
	 * 解析资源的 URL 路径并返回结果。
	 *
	 * @param resourcePath 资源的内部路径
	 * @param locations    资源位置列表
	 * @return 解析后的公共 URL 路径的 Mono 对象，如果无法解析则返回空 Mono
	 */
	@Override
	public Mono<String> resolveUrlPath(String resourcePath, List<? extends Resource> locations) {
		return (this.resolver != null && this.nextChain != null ?
				this.resolver.resolveUrlPath(resourcePath, locations, this.nextChain) :
				Mono.empty());
	}

}
