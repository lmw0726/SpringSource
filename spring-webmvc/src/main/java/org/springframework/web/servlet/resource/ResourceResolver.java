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

package org.springframework.web.servlet.resource;

import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 用于解析服务器端资源请求的策略。
 *
 * <p>提供了解析传入请求到实际 {@link org.springframework.core.io.Resource} 的机制，
 * 以及获取客户端在请求资源时应使用的公共 URL 路径的机制。
 *
 * @author Jeremy Grelle
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @see org.springframework.web.servlet.resource.ResourceResolverChain
 * @since 4.1
 */
public interface ResourceResolver {

	/**
	 * 将提供的请求和请求路径解析为存在于给定资源位置之一下的 {@link Resource}。
	 *
	 * @param request     当前请求（在某些调用中可能不存在）
	 * @param requestPath 请求路径的部分
	 * @param locations   在查找资源时要搜索的位置
	 * @param chain       剩余解析器链用于委托
	 * @return 解析的资源，如果未解析则为 {@code null}
	 */
	@Nullable
	Resource resolveResource(@Nullable HttpServletRequest request, String requestPath,
							 List<? extends Resource> locations, ResourceResolverChain chain);

	/**
	 * 解析外部公共 URL 路径，以供客户端访问位于给定的内部资源路径的资源时使用。
	 * <p>在向客户端渲染 URL 链接时，这很有用。
	 *
	 * @param resourcePath 内部资源路径
	 * @param locations    在查找资源时要搜索的位置
	 * @param chain        解析器链用于委托
	 * @return 已解析的公共 URL 路径，如果未解析则为 {@code null}
	 */
	@Nullable
	String resolveUrlPath(String resourcePath, List<? extends Resource> locations, ResourceResolverChain chain);

}
