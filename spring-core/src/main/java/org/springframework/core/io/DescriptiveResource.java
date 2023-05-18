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

package org.springframework.core.io;

import org.springframework.lang.Nullable;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Simple {@link Resource} implementation that holds a resource description
 * but does not point to an actually readable resource.
 *
 * <p>To be used as placeholder if a {@code Resource} argument is
 * expected by an API but not necessarily used for actual reading.
 *
 * @author Juergen Hoeller
 * @since 1.2.6
 */
public class DescriptiveResource extends AbstractResource {

	private final String description;


	public DescriptiveResource(@Nullable String description) {
		this.description = (description != null ? description : "");
	}

	/**
	 * 默认资源不存在
	 *
	 * @return true资源存在
	 */
	@Override
	public boolean exists() {
		return false;
	}

	/**
	 * 资源不可读
	 *
	 * @return true表示资源可读
	 */
	@Override
	public boolean isReadable() {
		return false;
	}

	/**
	 * 获取输入流
	 *
	 * @return 直接抛出文件未找到异常
	 * @throws IOException IO异常
	 */
	@Override
	public InputStream getInputStream() throws IOException {
		throw new FileNotFoundException(
				getDescription() + " cannot be opened because it does not point to a readable resource");
	}

	/**
	 * 获取资源描述
	 *
	 * @return 资源描述
	 */
	@Override
	public String getDescription() {
		return this.description;
	}


	/**
	 * 两个文件资源是否相同
	 *
	 * @param other 另外的资源
	 * @return true表示该资源相同
	 */
	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof DescriptiveResource &&
				((DescriptiveResource) other).description.equals(this.description)));
	}

	/**
	 * 获取HashCode
	 *
	 * @return 哈希值
	 */
	@Override
	public int hashCode() {
		return this.description.hashCode();
	}

}
