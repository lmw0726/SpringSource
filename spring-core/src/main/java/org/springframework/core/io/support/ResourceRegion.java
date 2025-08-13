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

package org.springframework.core.io.support;

import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * {@link Resource} 实现的一个区域，由 {@code position}（位置）
 * 和该区域长度的字节数 {@code count} 组成。
 *
 * @author Arjen Poutsma
 * @since 4.3
 */
public class ResourceRegion {

	private final Resource resource;

	private final long position;

	private final long count;


	/**
	 * 根据给定的 {@link Resource} 创建一个新的 {@code ResourceRegion}。
	 * 该资源区域由起始 {@code position} 和字节数 {@code count} 表示。
	 * @param resource 资源对象
	 * @param position 区域在该资源中的起始位置
	 * @param count 区域的字节长度
	 */
	public ResourceRegion(Resource resource, long position, long count) {
		Assert.notNull(resource, "Resource must not be null");
		Assert.isTrue(position >= 0, "'position' must be larger than or equal to 0");
		Assert.isTrue(count >= 0, "'count' must be larger than or equal to 0");
		this.resource = resource;
		this.position = position;
		this.count = count;
	}


	/**
	 * 返回此 {@code ResourceRegion} 所基于的底层 {@link Resource}。
	 */
	public Resource getResource() {
		return this.resource;
	}

	/**
	 * 返回此区域在底层 {@link Resource} 中的起始位置。
	 */
	public long getPosition() {
		return this.position;
	}

	/**
	 * 返回此区域在底层 {@link Resource} 中的字节数。
	 */
	public long getCount() {
		return this.count;
	}

}
