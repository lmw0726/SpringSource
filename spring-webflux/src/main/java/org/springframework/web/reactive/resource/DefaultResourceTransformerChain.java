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
 * {@link ResourceTransformerChain} 的默认不可变实现。
 *
 * <p>该类实现了资源转换器链的基本功能，用于按照给定的资源转换器列表执行资源转换过程。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
class DefaultResourceTransformerChain implements ResourceTransformerChain {

	/**
	 * 资源解析器链
	 */
	private final ResourceResolverChain resolverChain;

	/**
	 * 资源转换器
	 */
	@Nullable
	private final ResourceTransformer transformer;

	/**
	 * 下一个资源转换器链
	 */
	@Nullable
	private final ResourceTransformerChain nextChain;


	public DefaultResourceTransformerChain(
			ResourceResolverChain resolverChain, @Nullable List<ResourceTransformer> transformers) {

		Assert.notNull(resolverChain, "ResourceResolverChain is required");
		this.resolverChain = resolverChain;
		transformers = (transformers != null ? transformers : Collections.emptyList());
		DefaultResourceTransformerChain chain = initTransformerChain(resolverChain, new ArrayList<>(transformers));
		this.transformer = chain.transformer;
		this.nextChain = chain.nextChain;
	}

	/**
	 * 初始化资源转换器链。该方法将给定的资源转换器列表转化为一个资源转换器链。
	 *
	 * <p>资源转换器链的创建是通过反向迭代给定的资源转换器列表实现的，确保了列表中的每个转换器按照其在列表中的顺序正确链接。
	 *
	 * @param resolverChain 资源解析器链
	 * @param transformers 资源转换器列表
	 * @return 创建好的资源转换器链
	 */
	private DefaultResourceTransformerChain initTransformerChain(ResourceResolverChain resolverChain,
																 ArrayList<ResourceTransformer> transformers) {

		// 创建初始的资源转换器链，没有转换器和下一个链
		DefaultResourceTransformerChain chain = new DefaultResourceTransformerChain(resolverChain, null, null);

		// 使用反向迭代遍历资源转换器列表
		ListIterator<? extends ResourceTransformer> it = transformers.listIterator(transformers.size());
		while (it.hasPrevious()) {
			// 在每次迭代中，将当前转换器添加到链中并更新链的下一个链
			chain = new DefaultResourceTransformerChain(resolverChain, it.previous(), chain);
		}

		// 返回创建好的资源转换器链
		return chain;
	}

	public DefaultResourceTransformerChain(ResourceResolverChain resolverChain,
										   @Nullable ResourceTransformer transformer, @Nullable ResourceTransformerChain chain) {

		Assert.isTrue((transformer == null && chain == null) || (transformer != null && chain != null),
				"Both transformer and transformer chain must be null, or neither is");
		this.resolverChain = resolverChain;
		this.transformer = transformer;
		this.nextChain = chain;
	}


	@Override
	public ResourceResolverChain getResolverChain() {
		return this.resolverChain;
	}

	@Override
	public Mono<Resource> transform(ServerWebExchange exchange, Resource resource) {
		return (this.transformer != null && this.nextChain != null ?
				this.transformer.transform(exchange, resource, this.nextChain) :
				Mono.just(resource));
	}

}
