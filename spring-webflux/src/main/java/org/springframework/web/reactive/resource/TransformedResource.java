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

package org.springframework.web.reactive.resource;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;

import java.io.IOException;

/**
 * {@link ByteArrayResource}的扩展，一个{@link ResourceTransformer}可以使用它来表示保留所有其他信息的原始资源，除了内容。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class TransformedResource extends ByteArrayResource {

	/**
	 * 文件名称
	 */
	@Nullable
	private final String filename;

	/**
	 * 最后修改的时间
	 */
	private final long lastModified;


	public TransformedResource(Resource original, byte[] transformedContent) {
		super(transformedContent);
		this.filename = original.getFilename();
		try {
			this.lastModified = original.lastModified();
		} catch (IOException ex) {
			// 不应该发生
			throw new IllegalArgumentException(ex);
		}
	}


	@Override
	@Nullable
	public String getFilename() {
		return this.filename;
	}

	@Override
	public long lastModified() {
		return this.lastModified;
	}

}
