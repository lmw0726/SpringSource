/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.servlet.view.tiles3;

import org.apache.tiles.request.ApplicationResource;
import org.apache.tiles.request.locale.URLApplicationResource;
import org.apache.tiles.request.servlet.ServletApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.context.support.ServletContextResourcePatternResolver;

import javax.servlet.ServletContext;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;

/**
 * Tiles ServletApplicationContext的Spring特定子类。
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 3.2
 */
public class SpringWildcardServletTilesApplicationContext extends ServletApplicationContext {

	/**
	 * 资源路径解析器
	 */
	private final ResourcePatternResolver resolver;


	public SpringWildcardServletTilesApplicationContext(ServletContext servletContext) {
		super(servletContext);
		this.resolver = new ServletContextResourcePatternResolver(servletContext);
	}


	@Override
	@Nullable
	public ApplicationResource getResource(String localePath) {
		// 获取给定路径的资源集合
		Collection<ApplicationResource> urlSet = getResources(localePath);

		// 如果资源集合不为空
		if (!CollectionUtils.isEmpty(urlSet)) {
			// 返回资源集合中的第一个资源
			return urlSet.iterator().next();
		}

		// 如果资源集合为空，则返回 null
		return null;
	}

	@Override
	@Nullable
	public ApplicationResource getResource(ApplicationResource base, Locale locale) {
		// 获取给定语言环境对应的路径的资源集合
		Collection<ApplicationResource> urlSet = getResources(base.getLocalePath(locale));

		// 如果资源集合不为空
		if (!CollectionUtils.isEmpty(urlSet)) {
			// 返回资源集合中的第一个资源
			return urlSet.iterator().next();
		}

		// 如果资源集合为空，则返回 null
		return null;
	}

	@Override
	public Collection<ApplicationResource> getResources(String path) {
		Resource[] resources;
		try {
			// 尝试获取指定路径的资源
			resources = this.resolver.getResources(path);
		} catch (IOException ex) {
			// 如果资源检索失败，则记录日志并返回空列表
			((ServletContext) getContext()).log("Resource retrieval failed for path: " + path, ex);
			return Collections.emptyList();
		}

		if (ObjectUtils.isEmpty(resources)) {
			// 如果资源数组为空，则记录日志并返回空列表
			((ServletContext) getContext()).log("No resources found for path pattern: " + path);
			return Collections.emptyList();
		}

		// 创建资源列表
		Collection<ApplicationResource> resourceList = new ArrayList<>(resources.length);

		// 遍历资源数组
		for (Resource resource : resources) {
			try {
				URL url = resource.getURL();
				// 将每个资源封装为 ApplicationResource，并添加到资源列表中
				resourceList.add(new URLApplicationResource(url.toExternalForm(), url));
			} catch (IOException ex) {
				// 不应该发生，因为我们使用的是这种类型的资源
				throw new IllegalArgumentException("No URL for " + resource, ex);
			}
		}

		// 返回资源列表
		return resourceList;
	}

}
