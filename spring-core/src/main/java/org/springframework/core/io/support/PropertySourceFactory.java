/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.core.io.support;

import org.springframework.core.env.PropertySource;
import org.springframework.lang.Nullable;

import java.io.IOException;

/**
 * 用于创建基于资源的 {@link PropertySource} 包装器的策略接口。
 *
 * @author Juergen Hoeller
 * @since 4.3
 * @see DefaultPropertySourceFactory
 */
public interface PropertySourceFactory {

	/**
	 * 创建一个包装给定资源的 {@link PropertySource}。
	 * @param name 属性源的名称
	 *             （可以为 {@code null}，此时工厂实现需要基于给定资源生成名称）
	 * @param resource 要包装的资源（可能是编码过的）
	 * @return 新创建的 {@link PropertySource}（绝不为 {@code null}）
	 * @throws IOException 如果资源解析失败
	 */
	PropertySource<?> createPropertySource(@Nullable String name, EncodedResource resource) throws IOException;

}
