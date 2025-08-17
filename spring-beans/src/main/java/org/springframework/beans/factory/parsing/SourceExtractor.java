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

package org.springframework.beans.factory.parsing;

import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;

/**
 * 简单的策略接口，允许工具控制源元数据如何附加到 bean 定义元数据上。
 *
 * <p>配置解析器<strong>可能</strong>在解析阶段提供附加源元数据的功能。
 * 它们将以通用格式提供这些元数据，在附加到 bean 定义元数据之前，
 * 可通过 {@link SourceExtractor} 对其进行进一步处理。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 * @see org.springframework.beans.BeanMetadataElement#getSource()
 * @see org.springframework.beans.factory.config.BeanDefinition
 */
@FunctionalInterface
public interface SourceExtractor {

	/**
	 * 从配置解析器提供的候选对象中提取源元数据。
	 * @param sourceCandidate 原始源元数据 (从不是 {@code null})
	 * @param definingResource 定义给定源对象的资源 (可能是 {@code null})
	 * @return 要存储的源元数据对象 (可能是 {@code null})
	 */
	@Nullable
	Object extractSource(Object sourceCandidate, @Nullable Resource definingResource);

}
