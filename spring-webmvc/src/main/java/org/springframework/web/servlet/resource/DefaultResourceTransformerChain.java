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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

/**
 * {@link ResourceTransformerChain}的默认不可变实现。
 *
 * @author Rossen Stoyanchev
 * @since 4.1
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
	 * 资源转换器链
	 */
	@Nullable
	private final ResourceTransformerChain nextChain;


	public DefaultResourceTransformerChain(
			ResourceResolverChain resolverChain, @Nullable List<ResourceTransformer> transformers) {

		// 确保 资源解析器链 不为 null
		Assert.notNull(resolverChain, "ResourceResolverChain is required");
		// 将传入的 资源解析器链 设置为当前对象的 资源解析器链
		this.resolverChain = resolverChain;
		// 如果 资源转换器列表 不为 null，则使用 资源转换器列表；否则使用空列表
		transformers = (transformers != null ? transformers : Collections.emptyList());
		// 初始化转换器链
		DefaultResourceTransformerChain chain = initTransformerChain(resolverChain, new ArrayList<>(transformers));
		// 将初始化后的转换器链中的转换器设置为当前对象的转换器
		this.transformer = chain.transformer;
		// 将初始化后的转换器链中的下一个链设置为当前对象的下一个链
		this.nextChain = chain.nextChain;
	}

	private DefaultResourceTransformerChain initTransformerChain(ResourceResolverChain resolverChain,
																 ArrayList<ResourceTransformer> transformers) {

		// 创建默认资源转换器链
		DefaultResourceTransformerChain chain = new DefaultResourceTransformerChain(resolverChain, null, null);
		// 创建转换器列表的迭代器，从列表的末尾开始向前遍历
		ListIterator<? extends ResourceTransformer> it = transformers.listIterator(transformers.size());
		while (it.hasPrevious()) {
			// 从转换器列表中获取上一个转换器，并与当前链结合创建新的转换器链
			chain = new DefaultResourceTransformerChain(resolverChain, it.previous(), chain);
		}
		// 返回创建的资源转换器链
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
	public Resource transform(HttpServletRequest request, Resource resource) throws IOException {
		// 如果当前转换器和下一个链都不为null，则调用当前转换器对资源进行转换，并将结果传递给下一个链，否则直接返回资源
		return (this.transformer != null && this.nextChain != null ?
				this.transformer.transform(request, resource, this.nextChain) : resource);
	}

}
