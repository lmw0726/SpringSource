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

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;

import java.io.IOException;

/**
 * {@link ByteArrayResource}的扩展，用于表示原始资源，保留除内容之外的所有其他信息，以便由{@link ResourceTransformer}使用。
 *
 * @author Jeremy Grelle
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public class TransformedResource extends ByteArrayResource {

	/**
	 * 文件名
	 */
	@Nullable
	private final String filename;

	/**
	 * 最后更新的时间戳
	 */
	private final long lastModified;


	public TransformedResource(Resource original, byte[] transformedContent) {
		super(transformedContent);
		this.filename = original.getFilename();
		try {
			this.lastModified = original.lastModified();
		} catch (IOException ex) {
			// 永远不应该发生
			throw new IllegalArgumentException(ex);
		}
	}


	@Override
	@Nullable
	public String getFilename() {
		return this.filename;
	}

	@Override
	public long lastModified() throws IOException {
		return this.lastModified;
	}

}
