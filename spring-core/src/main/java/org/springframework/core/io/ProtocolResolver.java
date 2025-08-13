/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.core.io;

import org.springframework.lang.Nullable;

/**
 * 针对特定协议资源句柄的解析策略。
 *
 * <p>作为 {@link DefaultResourceLoader} 的 SPI 使用，允许
 * 在不继承加载器实现（或应用上下文实现）的情况下处理自定义协议。
 *
 * @author Juergen Hoeller
 * @since 4.3
 * @see DefaultResourceLoader#addProtocolResolver
 */
@FunctionalInterface
public interface ProtocolResolver {

	/**
	 * 如果此实现支持的协议匹配，则针对给定资源加载器解析指定的位置。
	 * @param location 用户指定的资源位置
	 * @param resourceLoader 关联的资源加载器
	 * @return 如果给定位置匹配此解析器协议，则返回对应的 {@code Resource} 句柄，否则返回 {@code null}
	 */
	@Nullable
	Resource resolve(String location, ResourceLoader resourceLoader);

}
