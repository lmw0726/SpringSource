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

package org.springframework.web.reactive.resource;

import reactor.core.publisher.Mono;

import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;

/**
 * 用于确定静态资源版本并在 URL 路径中应用和/或提取版本的策略。
 *
 * @since 5.0
 * @see VersionResourceResolver
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 */
public interface VersionStrategy {

	/**
	 * 从请求路径中提取资源版本。
	 *
	 * @param requestPath 要检查的请求路径
	 * @return 版本字符串，如果未找到则为 {@code null}
	 */
	@Nullable
	String extractVersion(String requestPath);

	/**
	 * 从请求路径中删除版本。假设给定的版本是通过 {@link #extractVersion(String)} 提取的。
	 *
	 * @param requestPath 要解析的资源的请求路径
	 * @param version     {@link #extractVersion(String)} 获取的版本
	 * @return 删除版本后的请求路径
	 */
	String removeVersion(String requestPath, String version);

	/**
	 * 为给定的请求路径添加版本。
	 *
	 * @param requestPath 请求路径
	 * @param version     版本
	 * @return 带有版本字符串更新的请求路径
	 */
	String addVersion(String requestPath, String version);

	/**
	 * 确定给定资源的版本。
	 *
	 * @param resource 要检查的资源
	 * @return 资源版本
	 */
	Mono<String> getResourceVersion(Resource resource);

}
