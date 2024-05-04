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

package org.springframework.web.servlet.resource;

import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

/**
 * {@link ResourceResolverChain} 的默认不可变实现。
 *
 * @author Rossen Stoyanchev
 * @since 4.1
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
		// 如果 资源解析器列表 不为空，则使用它，否则使用空列表
		resolvers = (resolvers != null ? resolvers : Collections.emptyList());
		// 初始化资源解析器链
		DefaultResourceResolverChain chain = initChain(new ArrayList<>(resolvers));
		// 将资源解析器链的第一个解析器赋值给 resolver
		this.resolver = chain.resolver;
		// 将下一个解析器链赋值给 nextChain
		this.nextChain = chain.nextChain;
	}

	private static DefaultResourceResolverChain initChain(ArrayList<? extends ResourceResolver> resolvers) {
		// 创建一个空的资源解析器链
		DefaultResourceResolverChain chain = new DefaultResourceResolverChain(null, null);
		// 创建一个反向迭代器，从最后一个解析器开始
		ListIterator<? extends ResourceResolver> it = resolvers.listIterator(resolvers.size());
		// 遍历解析器列表，并构建解析器链
		while (it.hasPrevious()) {
			// 将前一个解析器和当前链传递给构造函数，以构建新的链
			chain = new DefaultResourceResolverChain(it.previous(), chain);
		}
		// 返回构建好的解析器链
		return chain;
	}

	private DefaultResourceResolverChain(@Nullable ResourceResolver resolver, @Nullable ResourceResolverChain chain) {
		Assert.isTrue((resolver == null && chain == null) || (resolver != null && chain != null),
				"Both resolver and resolver chain must be null, or neither is");
		this.resolver = resolver;
		this.nextChain = chain;
	}


	@Override
	@Nullable
	public Resource resolveResource(
			@Nullable HttpServletRequest request, String requestPath, List<? extends Resource> locations) {

		if (this.resolver != null && this.nextChain != null) {
			// 如果资源解析器存在，并且存在下一个资源解析器链，则调用解析器解析资源
			return this.resolver.resolveResource(request, requestPath, locations, this.nextChain);
		}
		// 否则返回null
		return null;
	}

	@Override
	@Nullable
	public String resolveUrlPath(String resourcePath, List<? extends Resource> locations) {
		if (this.resolver != null && this.nextChain != null) {
			// 如果资源解析器存在，并且存在下一个资源解析器链，则调用解析器解析URL路径
			return this.resolver.resolveUrlPath(resourcePath, locations, this.nextChain);
		}
		// 否则返回null
		return null;
	}

}
