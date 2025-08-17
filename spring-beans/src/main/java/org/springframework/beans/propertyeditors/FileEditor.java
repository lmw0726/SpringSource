/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.beans.propertyeditors;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceEditor;
import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

import java.beans.PropertyEditorSupport;
import java.io.File;
import java.io.IOException;

/**
 * {@code java.io.File} 的 PropertyEditor，可直接从 Spring 资源位置
 * 填充 File 属性。
 *
 * <p>支持 Spring 风格的 URL 表示法：任何完全限定的标准 URL
 * （"file:"、"http:" 等）以及 Spring 的特殊 "classpath:" 伪 URL。
 *
 * <p><b>注意：</b>此编辑器的行为在 Spring 2.0 中发生了变化。
 * 以前，它会直接从文件名创建 File 实例。
 * 从 Spring 2.0 开始，它使用标准 Spring 资源位置作为输入，
 * 与 URLEditor 和 InputStreamEditor 的行为一致。
 *
 * <p><b>注意：</b>在 Spring 2.5 中进行了如下修改：
 * 如果指定的文件名没有 URL 前缀或不是绝对路径，
 * 则尝试使用标准 ResourceLoader 语义定位文件。
 * 如果找不到文件，则创建一个相对文件位置的 File 实例。
 *
 * @author Juergen Hoeller
 * @author Thomas Risberg
 * @since 09.12.2003
 * @see java.io.File
 * @see org.springframework.core.io.ResourceEditor
 * @see org.springframework.core.io.ResourceLoader
 * @see URLEditor
 * @see InputStreamEditor
 */
public class FileEditor extends PropertyEditorSupport {

	private final ResourceEditor resourceEditor;


	/**
	 * 使用默认 ResourceEditor 创建新的 FileEditor。
	 */
	public FileEditor() {
		this.resourceEditor = new ResourceEditor();
	}

	/**
	 * 使用指定的 ResourceEditor 创建新的 FileEditor。
	 * @param resourceEditor 要使用的 ResourceEditor
	 */
	public FileEditor(ResourceEditor resourceEditor) {
		Assert.notNull(resourceEditor, "ResourceEditor must not be null");
		this.resourceEditor = resourceEditor;
	}


	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		if (!StringUtils.hasText(text)) {
			setValue(null);
			return;
		}

		// 检查是否为没有 "file:" 前缀的绝对文件路径
		File file = null;
		if (!ResourceUtils.isUrl(text)) {
			file = new File(text);
			if (file.isAbsolute()) {
				setValue(file);
				return;
			}
		}

		// 使用标准资源位置解析
		this.resourceEditor.setAsText(text);
		Resource resource = (Resource) this.resourceEditor.getValue();

		// 如果是 URL 或指向现有资源的路径，则直接使用
		if (file == null || resource.exists()) {
			try {
				setValue(resource.getFile());
			}
			catch (IOException ex) {
				throw new IllegalArgumentException(
						"Could not retrieve file for " + resource + ": " + ex.getMessage());
			}
		}
		else {
			// 设置相对文件引用
			setValue(file);
		}
	}

	@Override
	public String getAsText() {
		File value = (File) getValue();
		return (value != null ? value.getPath() : "");
	}

}
