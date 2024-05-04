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

package org.springframework.web.servlet.resource;

import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.webjars.WebJarAssetLocator;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 一个 {@code ResourceResolver}，委托给链来定位资源，然后尝试查找包含在 WebJar JAR 文件中的匹配版本化资源。
 *
 * <p>这允许 WebJars.org 用户在模板中编写版本不可知的路径，例如 {@code <script src="/jquery/jquery.min.js"/>}。
 * 此路径将解析为唯一版本 {@code <script src="/jquery/1.2.0/jquery.min.js"/>}，
 * 这更适合于应用程序中的 HTTP 缓存和版本管理。
 *
 * <p>这还解析版本不可知的 HTTP 请求的资源 {@code "GET /jquery/jquery.min.js"}。
 *
 * <p>此解析器需要类路径上的 {@code org.webjars:webjars-locator-core} 库，并且如果存在该库，则会自动注册。
 *
 * @author Brian Clozel
 * @see org.springframework.web.servlet.config.annotation.ResourceChainRegistration
 * @see <a href="https://www.webjars.org">webjars.org</a>
 * @since 4.2
 */
public class WebJarsResourceResolver extends AbstractResourceResolver {

	private static final String WEBJARS_LOCATION = "META-INF/resources/webjars/";

	private static final int WEBJARS_LOCATION_LENGTH = WEBJARS_LOCATION.length();

	/**
	 * Web Jar资产定位器
	 */
	private final WebJarAssetLocator webJarAssetLocator;


	/**
	 * 使用默认的 {@code WebJarAssetLocator} 实例创建一个 {@code WebJarsResourceResolver}。
	 */
	public WebJarsResourceResolver() {
		this(new WebJarAssetLocator());
	}

	/**
	 * 使用自定义的 {@code WebJarAssetLocator} 实例创建一个 {@code WebJarsResourceResolver}，
	 * 例如具有自定义索引。
	 *
	 * @since 4.3
	 */
	public WebJarsResourceResolver(WebJarAssetLocator webJarAssetLocator) {
		this.webJarAssetLocator = webJarAssetLocator;
	}


	@Override
	protected Resource resolveResourceInternal(@Nullable HttpServletRequest request, String requestPath,
											   List<? extends Resource> locations, ResourceResolverChain chain) {

		// 尝试解析资源
		Resource resolved = chain.resolveResource(request, requestPath, locations);
		// 如果解析不到资源
		if (resolved == null) {
			// 查找WebJar资源路径
			String webJarResourcePath = findWebJarResourcePath(requestPath);
			// 如果找到了WebJar资源路径
			if (webJarResourcePath != null) {
				// 再次尝试解析资源
				return chain.resolveResource(request, webJarResourcePath, locations);
			}
		}
		// 返回解析的资源
		return resolved;
	}

	@Override
	protected String resolveUrlPathInternal(String resourceUrlPath,
											List<? extends Resource> locations, ResourceResolverChain chain) {

		// 解析资源路径
		String path = chain.resolveUrlPath(resourceUrlPath, locations);
		// 如果解析不到的路径
		if (path == null) {
			// 查找WebJar资源路径
			String webJarResourcePath = findWebJarResourcePath(resourceUrlPath);
			// 如果找到了WebJar资源路径
			if (webJarResourcePath != null) {
				// 再次尝试解析资源路径
				return chain.resolveUrlPath(webJarResourcePath, locations);
			}
		}
		// 返回解析的路径
		return path;
	}

	@Nullable
	protected String findWebJarResourcePath(String path) {
		// 计算起始偏移量
		int startOffset = (path.startsWith("/") ? 1 : 0);
		// 计算结束偏移量
		int endOffset = path.indexOf('/', 1);
		// 如果存在结束偏移量
		if (endOffset != -1) {
			// 获取WebJar名称
			String webjar = path.substring(startOffset, endOffset);
			// 获取部分路径
			String partialPath = path.substring(endOffset + 1);
			// 获取WebJar资源完整路径
			String webJarPath = this.webJarAssetLocator.getFullPathExact(webjar, partialPath);
			// 如果找到了WebJar资源路径
			if (webJarPath != null) {
				// 返回相对路径
				return webJarPath.substring(WEBJARS_LOCATION_LENGTH);
			}
		}
		// 如果未找到WebJar资源路径，返回null
		return null;
	}

}
