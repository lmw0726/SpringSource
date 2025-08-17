/*
 * Copyright 2002-2012 the original author or authors.
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
 * 简单的{@link SourceExtractor}实现，仅将
 * 候选源元数据对象直接传递以进行附加。
 *
 * <p>使用此实现意味着工具将获得对
 * 工具提供的底层配置源元数据的原始访问权限。
 *
 * <p>此实现<strong>不应</strong>在生产
 * 应用程序中使用，因为它可能会在内存中保留过多元数据
 * （不必要地）。
 *
 * @author Rob Harrop
 * @since 2.0
 */
public class PassThroughSourceExtractor implements SourceExtractor {

	/**
	 * 只需按状态返回提供的 {@code sourceCandidate}。
	 * @param sourceCandidate 源元数据
	 * @return 提供的 {@code sourceCandidate}
	 */
	@Override
	public Object extractSource(Object sourceCandidate, @Nullable Resource definingResource) {
		return sourceCandidate;
	}

}
