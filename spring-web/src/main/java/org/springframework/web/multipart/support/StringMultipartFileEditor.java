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

import org.springframework.lang.Nullable;
import org.springframework.web.multipart.MultipartFile;

import java.beans.PropertyEditorSupport;
import java.io.IOException;

/**
 * 自定义 {@link java.beans.PropertyEditor}，用于将 {@link MultipartFile MultipartFiles} 转换为字符串。
 *
 * <p>允许指定要使用的字符集。
 *
 * @autor Juergen Hoeller
 * @since 13.10.2003
 */
public class StringMultipartFileEditor extends PropertyEditorSupport {
	/**
	 * 字符集名称
	 */
	@Nullable
	private final String charsetName;

	/**
	 * 创建一个使用默认字符集的 {@link StringMultipartFileEditor}。
	 */
	public StringMultipartFileEditor() {
		this.charsetName = null;
	}

	/**
	 * 创建一个使用指定字符集的 {@link StringMultipartFileEditor}。
	 *
	 * @param charsetName 有效的字符集名称
	 * @see java.lang.String#String(byte[], String)
	 */
	public StringMultipartFileEditor(String charsetName) {
		this.charsetName = charsetName;
	}

	@Override
	public void setAsText(String text) {
		setValue(text);
	}

	@Override
	public void setValue(Object value) {
		if (value instanceof MultipartFile) {
			// 如果值是MultipartFile类型
			MultipartFile multipartFile = (MultipartFile) value;
			try {
				// 尝试将MultipartFile的字节内容转换为字符串并设置为值
				// 如果charsetName不为空，则使用指定的字符集名称进行转换
				super.setValue(this.charsetName != null ?
						new String(multipartFile.getBytes(), this.charsetName) :
						new String(multipartFile.getBytes()));
			} catch (IOException ex) {
				// 捕获IO异常并抛出IllegalArgumentException异常
				throw new IllegalArgumentException("Cannot read contents of multipart file", ex);
			}
		} else {
			// 否则，直接设置值
			super.setValue(value);
		}
	}

}
