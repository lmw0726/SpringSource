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
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Collections;

/**
 * {@code ResourceTransformerSupport} 是一个 {@code ResourceTransformer} 的基类，包含一个可选的辅助方法，
 * 用于在转换的资源中解析公共链接。
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 5.0
 */
public abstract class ResourceTransformerSupport implements ResourceTransformer {
	/**
	 * 资源 URL 提供者
	 */
	@Nullable
	private ResourceUrlProvider resourceUrlProvider;


	/**
	 * 配置一个 {@link ResourceUrlProvider}，用于解析转换后资源中公共链接的公共 URL（例如 CSS 文件中的导入链接）。
	 * 仅当链接以完整路径表示时需要配置此项，而对于相对链接则不需要。
	 *
	 * @param resourceUrlProvider 要使用的 URL 提供者
	 */
	public void setResourceUrlProvider(@Nullable ResourceUrlProvider resourceUrlProvider) {
		this.resourceUrlProvider = resourceUrlProvider;
	}

	/**
	 * 返回配置的 {@code ResourceUrlProvider}。
	 */
	@Nullable
	public ResourceUrlProvider getResourceUrlProvider() {
		return this.resourceUrlProvider;
	}

	/**
	 * 当转换的资源包含指向其他资源的链接时，转换器可以使用此方法。
	 * 此类链接需要根据资源解析器链（例如公共 URL 可能包含版本信息）替换为公共面向的链接。
	 *
	 * @param resourcePath     需要重写的资源路径
	 * @param exchange         当前的交换对象
	 * @param resource         正在转换的资源
	 * @param transformerChain 转换器链
	 * @return 解析后的 URL 或空的 {@link Mono}
	 */
	protected Mono<String> resolveUrlPath(String resourcePath, ServerWebExchange exchange,
										  Resource resource, ResourceTransformerChain transformerChain) {

		if (resourcePath.startsWith("/")) {
			// 完整资源路径
			ResourceUrlProvider urlProvider = getResourceUrlProvider();
			return (urlProvider != null ? urlProvider.getForUriString(resourcePath, exchange) : Mono.empty());
		} else {
			// 尝试解析为相对路径
			return transformerChain.getResolverChain()
					.resolveUrlPath(resourcePath, Collections.singletonList(resource));
		}
	}

	/**
	 * 将给定的相对请求路径转换为绝对路径，以给定请求路径为参考点。
	 * 结果路径也会清理类似于 "path/.." 的序列。
	 *
	 * @param path     要转换的相对路径
	 * @param exchange 当前的交换对象
	 * @return 给定资源路径的绝对请求路径
	 */
	protected String toAbsolutePath(String path, ServerWebExchange exchange) {
		// 获取请求的路径
		String requestPath = exchange.getRequest().getURI().getPath();

		// 如果给定的路径以斜杠开头，则使用该路径；否则将相对路径应用于请求路径，得到绝对路径
		String absolutePath = (path.startsWith("/") ? path : StringUtils.applyRelativePath(requestPath, path));

		// 对绝对路径进行清理，返回规范化后的路径
		return StringUtils.cleanPath(absolutePath);
	}

}
