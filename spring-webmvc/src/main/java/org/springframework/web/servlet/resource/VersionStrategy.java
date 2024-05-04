/*
 * Copyright 2002-2015 the original author or authors.
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

/**
 * {@link VersionPathStrategy}的扩展，添加了一个方法来确定{@link Resource}的实际版本。
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @since 4.1
 * @see VersionResourceResolver
 */
public interface VersionStrategy extends VersionPathStrategy {

	/**
	 * 确定给定资源的版本。
	 *
	 * @param resource 要检查的资源
	 * @return 版本（永远不为{@code null}）
	 */
	String getResourceVersion(Resource resource);

}
