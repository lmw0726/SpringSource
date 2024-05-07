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

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.springframework.core.io.Resource;

/**
 * 用于调用一系列{@link ResourceTransformer ResourceTransformers}的契约，其中每个解析器都被给予链的引用，以便在必要时委托。
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public interface ResourceTransformerChain {

	/**
	 * 返回用于解析正在转换的{@code Resource}的{@code ResourceResolverChain}。
	 * 这可能需要用于解析相关资源，例如指向其他资源的链接。
	 */
	ResourceResolverChain getResolverChain();

	/**
	 * 转换给定的资源。
	 * @param request 当前请求
	 * @param resource 要转换的候选资源
	 * @return 转换后的或相同的资源，永远不会为{@code null}
	 * @throws IOException 如果转换失败
	 */
	Resource transform(HttpServletRequest request, Resource resource) throws IOException;

}
