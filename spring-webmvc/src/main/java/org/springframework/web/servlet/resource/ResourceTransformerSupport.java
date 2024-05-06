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
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;

/**
 * 用于{@code ResourceTransformer}的基类，具有一个可选的帮助方法，用于解析转换资源中的公共链接。
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 4.1
 */
public abstract class ResourceTransformerSupport implements ResourceTransformer {

	/**
	 * URL资源提供者
	 */
	@Nullable
	private ResourceUrlProvider resourceUrlProvider;


	/**
	 * 配置一个{@link ResourceUrlProvider}，用于在转换的资源中解析公共链接的公共URL（例如，CSS文件中的导入链接）。
	 * 仅当链接表示为完整路径而不是相对链接时才需要此配置。
	 */
	public void setResourceUrlProvider(@Nullable ResourceUrlProvider resourceUrlProvider) {
		this.resourceUrlProvider = resourceUrlProvider;
	}

	/**
	 * 返回配置的{@code ResourceUrlProvider}。
	 */
	@Nullable
	public ResourceUrlProvider getResourceUrlProvider() {
		return this.resourceUrlProvider;
	}


	/**
	 * 当转换的资源包含对其他资源的链接时，转换器可以使用此方法。
	 * 这些链接需要替换为资源解析器链（例如，公共URL可能已插入版本）确定的面向公众的链接。
	 *
	 * @param resourcePath     需要重新编写的资源路径
	 * @param request          当前请求
	 * @param resource         被转换的资源
	 * @param transformerChain 转换器链
	 * @return 已解析的URL，如果无法解析则为{@code null}
	 */
	@Nullable
	protected String resolveUrlPath(String resourcePath, HttpServletRequest request,
									Resource resource, ResourceTransformerChain transformerChain) {

		if (resourcePath.startsWith("/")) {
			// 完整资源路径
			// 查找URL资源提供者
			ResourceUrlProvider urlProvider = findResourceUrlProvider(request);
			// 如果找到了，则使用URL资源提供者解析请求URL。否则返回null。
			return (urlProvider != null ? urlProvider.getForRequestUrl(request, resourcePath) : null);
		} else {
			// 尝试解析为相对路径
			return transformerChain.getResolverChain().resolveUrlPath(
					resourcePath, Collections.singletonList(resource));
		}
	}

	/**
	 * 将给定的相对请求路径转换为绝对路径，以给定请求的路径为参考点。
	 * 结果路径还会清除类似于"path/.."的序列。
	 *
	 * @param path    要转换的相对路径
	 * @param request 参考请求
	 * @return 给定资源路径的绝对请求路径
	 */
	protected String toAbsolutePath(String path, HttpServletRequest request) {
		// 初始化绝对路径为原始路径
		String absolutePath = path;
		// 如果路径不是以斜杠开头，则表示为相对路径
		if (!path.startsWith("/")) {
			// 查找请求对象对应的URL资源提供者
			ResourceUrlProvider urlProvider = findResourceUrlProvider(request);
			// 确保找到了URL资源提供者
			Assert.state(urlProvider != null, "No ResourceUrlProvider");
			// 获取请求路径
			String requestPath = urlProvider.getUrlPathHelper().getRequestUri(request);
			// 使用请求路径和相对路径生成绝对路径
			absolutePath = StringUtils.applyRelativePath(requestPath, path);
		}
		// 获取规范化后的路径
		return StringUtils.cleanPath(absolutePath);
	}

	@Nullable
	private ResourceUrlProvider findResourceUrlProvider(HttpServletRequest request) {
		// 如果已经设置了URL资源提供者，则直接返回
		if (this.resourceUrlProvider != null) {
			return this.resourceUrlProvider;
		}
		// 否则，尝试从请求属性中获取URL资源提供者
		return (ResourceUrlProvider) request.getAttribute(
				ResourceUrlProviderExposingInterceptor.RESOURCE_URL_PROVIDER_ATTR);
	}

}
