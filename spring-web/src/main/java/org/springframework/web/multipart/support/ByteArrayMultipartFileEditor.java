/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.multipart.support;

import org.springframework.beans.propertyeditors.ByteArrayPropertyEditor;
import org.springframework.lang.Nullable;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 自定义 {@link java.beans.PropertyEditor}，用于将 {@link MultipartFile MultipartFiles} 转换为字节数组。
 *
 * @author Juergen Hoeller
 * @since 13.10.2003
 */
public class ByteArrayMultipartFileEditor extends ByteArrayPropertyEditor {

	@Override
	public void setValue(@Nullable Object value) {
		if (value instanceof MultipartFile) {
			// 如果值是MultipartFile类型
			MultipartFile multipartFile = (MultipartFile) value;
			try {
				// 尝试将MultipartFile的字节内容设置为值
				super.setValue(multipartFile.getBytes());
			} catch (IOException ex) {
				// 捕获IO异常并抛出IllegalArgumentException异常
				throw new IllegalArgumentException("Cannot read contents of multipart file", ex);
			}
		} else if (value instanceof byte[]) {
			// 如果值是byte数组类型，则直接设置为值
			super.setValue(value);
		} else {
			// 否则，将值转换为字符串并获取其字节内容后设置为值
			super.setValue(value != null ? value.toString().getBytes() : null);
		}
	}

	@Override
	public String getAsText() {
		// 从getValue方法获取byte数组类型的值
		byte[] value = (byte[]) getValue();
		// 如果值不为空，则将其转换为字符串并返回；否则，返回空字符串
		return (value != null ? new String(value) : "");
	}

}
