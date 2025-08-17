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
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.beans.PropertyEditorSupport;
import java.io.IOException;

/**
 * 单向 PropertyEditor，可将文本字符串转换为 {@code java.io.InputStream}，
 * 并将给定字符串解释为 Spring 资源位置（例如 URL 字符串）。
 *
 * <p>支持 Spring 风格的 URL 表示法：任何完全限定的标准 URL
 * （"file:"、"http:" 等）以及 Spring 的特殊 "classpath:" 伪 URL。
 *
 * <p>注意，此类流通常不会由 Spring 自动关闭！
 *
 * @author Juergen Hoeller
 * @since 1.0.1
 * @see java.io.InputStream
 * @see org.springframework.core.io.ResourceEditor
 * @see org.springframework.core.io.ResourceLoader
 * @see URLEditor
 * @see FileEditor
 */
public class InputStreamEditor extends PropertyEditorSupport {

	private final ResourceEditor resourceEditor;


	/**
	 * 使用默认 ResourceEditor 创建一个新的 InputStreamEditor。
	 */
	public InputStreamEditor() {
		this.resourceEditor = new ResourceEditor();
	}

	/**
	 * 使用指定的 ResourceEditor 创建一个新的 InputStreamEditor。
	 * @param resourceEditor 要使用的 ResourceEditor
	 */
	public InputStreamEditor(ResourceEditor resourceEditor) {
		Assert.notNull(resourceEditor, "ResourceEditor must not be null");
		this.resourceEditor = resourceEditor;
	}


	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		this.resourceEditor.setAsText(text);
		Resource resource = (Resource) this.resourceEditor.getValue();
		try {
			setValue(resource != null ? resource.getInputStream() : null);
		}
		catch (IOException ex) {
			throw new IllegalArgumentException("Failed to retrieve InputStream for " + resource, ex);
		}
	}

	/**
	 * 返回 {@code null}，表示没有适当的文本表示。
	 */
	@Override
	@Nullable
	public String getAsText() {
		return null;
	}

}
