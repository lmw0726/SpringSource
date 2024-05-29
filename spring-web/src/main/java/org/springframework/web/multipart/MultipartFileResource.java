/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.web.multipart;

import org.springframework.core.io.AbstractResource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.io.IOException;
import java.io.InputStream;

/**
 * 将 {@link MultipartFile} 适配为 {@link org.springframework.core.io.Resource}，
 * 将内容暴露为 {@code InputStream}，同时覆盖 {@link #contentLength()} 和 {@link #getFilename()}。
 *
 * @author Rossen Stoyanchev
 * @see MultipartFile#getResource()
 * @since 5.1
 */
class MultipartFileResource extends AbstractResource {

	private final MultipartFile multipartFile;


	public MultipartFileResource(MultipartFile multipartFile) {
		Assert.notNull(multipartFile, "MultipartFile must not be null");
		this.multipartFile = multipartFile;
	}


	/**
	 * 此实现始终返回 {@code true}。
	 */
	@Override
	public boolean exists() {
		return true;
	}

	/**
	 * 此实现始终返回 {@code true}。
	 */
	@Override
	public boolean isOpen() {
		return true;
	}

	@Override
	public long contentLength() {
		return this.multipartFile.getSize();
	}

	@Override
	public String getFilename() {
		return this.multipartFile.getOriginalFilename();
	}

	/**
	 * 如果尝试多次读取底层流，则此实现将抛出 IllegalStateException。
	 */
	@Override
	public InputStream getInputStream() throws IOException, IllegalStateException {
		return this.multipartFile.getInputStream();
	}

	/**
	 * 此实现返回具有多部分名称的描述。
	 */
	@Override
	public String getDescription() {
		return "MultipartFile resource [" + this.multipartFile.getName() + "]";
	}

	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof MultipartFileResource &&
				((MultipartFileResource) other).multipartFile.equals(this.multipartFile)));
	}

	@Override
	public int hashCode() {
		return this.multipartFile.hashCode();
	}

}
