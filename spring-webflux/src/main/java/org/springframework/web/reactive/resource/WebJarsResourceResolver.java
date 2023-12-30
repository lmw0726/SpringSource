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
import org.springframework.lang.Nullable;
import org.springframework.web.server.ServerWebExchange;
import org.webjars.WebJarAssetLocator;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 一个 {@code ResourceResolver}，它委托给链来定位资源，然后尝试找到包含在 WebJar JAR 文件中的匹配版本的资源。
 *
 * <p>这允许 WebJars.org 的用户在模板中编写版本无关的路径，例如 {@code <script src="/jquery/jquery.min.js"/>}。
 * 这个路径将被解析为唯一版本 {@code <script src="/jquery/1.2.0/jquery.min.js"/>}，
 * 这对于 HTTP 缓存和应用程序的版本管理更为合适。
 *
 * <p>这也解析版本无关的 HTTP 请求的资源，例如 {@code "GET /jquery/jquery.min.js"}。
 *
 * <p>此解析器需要在类路径上有 {@code org.webjars:webjars-locator-core} 库，
 * 如果存在该库，则会自动注册。
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @see <a href="https://www.webjars.org">webjars.org</a>
 * @since 5.0
 */
public class WebJarsResourceResolver extends AbstractResourceResolver {

	/**
	 * WebJars 的资源位置，指向 META-INF/resources/webjars/
	 */
	private static final String WEBJARS_LOCATION = "META-INF/resources/webjars/";

	/**
	 * WebJars 资源位置的长度
	 */
	private static final int WEBJARS_LOCATION_LENGTH = WEBJARS_LOCATION.length();

	/**
	 * WebJar 资源定位器
	 */
	private final WebJarAssetLocator webJarAssetLocator;


	/**
	 * 使用默认的 {@code WebJarAssetLocator} 实例创建 {@code WebJarsResourceResolver}。
	 */
	public WebJarsResourceResolver() {
		this(new WebJarAssetLocator());
	}

	/**
	 * 使用自定义的 {@code WebJarAssetLocator} 实例创建 {@code WebJarsResourceResolver}，
	 * 例如带有自定义索引的实例。
	 */
	public WebJarsResourceResolver(WebJarAssetLocator webJarAssetLocator) {
		this.webJarAssetLocator = webJarAssetLocator;
	}


	/**
	 * 解析实际资源的内部方法。
	 *
	 * @param exchange    当前的服务器 Web 交换，可能为空
	 * @param requestPath 要解析的请求路径部分，通常是编码后的路径
	 * @param locations   要在其中查找资源的位置列表
	 * @param chain       要委托给的资源解析器链
	 * @return 已解析的资源或空的 {@code Mono}
	 */
	@Override
	protected Mono<Resource> resolveResourceInternal(@Nullable ServerWebExchange exchange,
													 String requestPath, List<? extends Resource> locations,
													 ResourceResolverChain chain) {

		return chain.resolveResource(exchange, requestPath, locations)
				.switchIfEmpty(Mono.defer(() -> {
					// 尝试在 WebJars 中查找资源路径
					String webJarsResourcePath = findWebJarResourcePath(requestPath);
					if (webJarsResourcePath != null) {
						// 如果找到匹配的资源路径，则通过链再次尝试解析资源
						return chain.resolveResource(exchange, webJarsResourcePath, locations);
					} else {
						// 未找到匹配的资源路径，返回空的 Mono
						return Mono.empty();
					}
				}));
	}


	/**
	 * 解析资源公共 URL 路径的内部方法。
	 *
	 * @param resourceUrlPath 要解析用于公共使用的 "内部" 资源路径，通常是编码后的路径
	 * @param locations       要在其中查找资源的位置列表
	 * @param chain           要委托给的资源解析器链
	 * @return 解析的公共 URL 路径或空的 {@code Mono}
	 */
	@Override
	protected Mono<String> resolveUrlPathInternal(String resourceUrlPath,
												  List<? extends Resource> locations, ResourceResolverChain chain) {

		return chain.resolveUrlPath(resourceUrlPath, locations)
				.switchIfEmpty(Mono.defer(() -> {
					// 尝试在 WebJars 中查找资源路径
					String webJarResourcePath = findWebJarResourcePath(resourceUrlPath);
					if (webJarResourcePath != null) {
						// 如果找到匹配的资源路径，则通过链再次尝试解析资源的 URL 路径
						return chain.resolveUrlPath(webJarResourcePath, locations);
					} else {
						// 未找到匹配的资源路径，返回空的 Mono
						return Mono.empty();
					}
				}));
	}


	/**
	 * 在 WebJars 中查找资源路径的方法。
	 *
	 * @param path 要查找的资源路径
	 * @return 匹配的 WebJar 资源路径，如果未找到，则为 {@code null}
	 */
	@Nullable
	protected String findWebJarResourcePath(String path) {
		int startOffset = (path.startsWith("/") ? 1 : 0);
		int endOffset = path.indexOf('/', 1);
		if (endOffset != -1) {
			String webjar = path.substring(startOffset, endOffset);
			String partialPath = path.substring(endOffset + 1);
			// 获取指定 WebJar 资源的完整路径
			String webJarPath = this.webJarAssetLocator.getFullPathExact(webjar, partialPath);
			if (webJarPath != null) {
				// 返回匹配的 WebJar 资源路径（去除 WEBJARS_LOCATION 部分）
				return webJarPath.substring(WEBJARS_LOCATION_LENGTH);
			}
		}
		// 未找到匹配的 WebJar 资源路径
		return null;
	}

}
