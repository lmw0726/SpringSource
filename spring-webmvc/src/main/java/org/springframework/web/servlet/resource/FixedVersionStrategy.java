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

/**
 * 一个{@code VersionStrategy}，依赖于作为请求路径前缀的固定版本，例如缩减的SHA、版本名称、发布日期等。
 *
 * <p>例如，当无法使用{@link ContentVersionStrategy}时，这非常有用，比如使用JavaScript模块加载器时，它负责加载JavaScript资源并需要知道它们的相对路径。
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @since 4.1
 * @see VersionResourceResolver
 */
public class FixedVersionStrategy extends AbstractVersionStrategy {

	/**
	 * 版本号
	 */
	private final String version;


	/**
	 * 使用给定的版本字符串创建一个新的FixedVersionStrategy。
	 *
	 * @param version 要使用的固定版本字符串
	 */
	public FixedVersionStrategy(String version) {
		super(new PrefixVersionPathStrategy(version));
		this.version = version;
	}


	@Override
	public String getResourceVersion(Resource resource) {
		return this.version;
	}

}
