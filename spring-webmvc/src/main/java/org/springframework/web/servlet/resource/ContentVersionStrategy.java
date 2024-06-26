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
import org.springframework.util.DigestUtils;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;

/**
 * 从资源内容计算出一个十六进制的MD5哈希，并将其附加到文件名中的{@code VersionStrategy}。
 * 例如{@code "styles/main-e36d2e05253c6c7085a91522ce43a0b4.css"}。
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @see VersionResourceResolver
 * @since 4.1
 */
public class ContentVersionStrategy extends AbstractVersionStrategy {

	public ContentVersionStrategy() {
		super(new FileNameVersionPathStrategy());
	}

	@Override
	public String getResourceVersion(Resource resource) {
		try {
			// 将资源内容复制到字节数组中
			byte[] content = FileCopyUtils.copyToByteArray(resource.getInputStream());
			// 使用MD5算法计算字节数组的哈希值，并以十六进制字符串形式返回
			return DigestUtils.md5DigestAsHex(content);
		} catch (IOException ex) {
			// 捕获IO异常并抛出状态异常，说明计算哈希值失败
			throw new IllegalStateException("Failed to calculate hash for " + resource, ex);
		}
	}

}
