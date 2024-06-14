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

package org.springframework.http;

import org.springframework.util.StringUtils;

import java.beans.PropertyEditorSupport;

/**
 * {@link java.beans.PropertyEditor PropertyEditor}，用于 {@link MediaType}
 * 描述符，自动将 {@code String} 规范（例如 {@code "text/html"}）转换为 {@code MediaType} 属性。
 *
 * @author Juergen Hoeller
 * @see MediaType
 * @since 3.0
 */
public class MediaTypeEditor extends PropertyEditorSupport {

	@Override
	public void setAsText(String text) {
		// 如果文本内容不为空，或不是空白字符串
		if (StringUtils.hasText(text)) {
			// 解析文本内容为MediaType，并设置为属性值
			setValue(MediaType.parseMediaType(text));
		} else {
			// 如果文本内容为空，或空白字符串，则设置属性值为null
			setValue(null);
		}
	}

	@Override
	public String getAsText() {
		// 获取存储在属性中的MediaType对象
		MediaType mediaType = (MediaType) getValue();

		// 如果MediaType对象不为null，则返回其字符串表示形式；
		// 否则返回空字符串
		return (mediaType != null ? mediaType.toString() : "");
	}

}
