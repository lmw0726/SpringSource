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

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 用于调用一系列 {@link ResourceResolver ResourceResolvers} 链的契约，其中每个解析器
 * 都被给予链的引用，以便在必要时委托。
 *
 * @author Jeremy Grelle
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 4.1
 */
public interface ResourceResolverChain {

	/**
	 * 将提供的请求和请求路径解析为存在于给定资源位置之一下的 {@link Resource}。
	 *
	 * @param request     当前请求
	 * @param requestPath 要使用的请求路径部分
	 * @param locations   查找资源时要搜索的位置
	 * @return 已解析的资源，如果未解析则返回 {@code null}
	 */
	@Nullable
	Resource resolveResource(
			@Nullable HttpServletRequest request, String requestPath, List<? extends Resource> locations);

	/**
	 * 解析外部公共 URL 路径，以便客户端访问位于给定内部资源路径的资源。
	 * <p>在向客户端呈现 URL 链接时很有用。
	 *
	 * @param resourcePath 内部资源路径
	 * @param locations    查找资源时要搜索的位置
	 * @return 已解析的公共 URL 路径，如果未解析则返回 {@code null}
	 */
	@Nullable
	String resolveUrlPath(String resourcePath, List<? extends Resource> locations);

}
