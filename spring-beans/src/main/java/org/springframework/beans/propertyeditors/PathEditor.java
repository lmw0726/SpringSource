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

package org.springframework.beans.propertyeditors;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceEditor;
import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;

import java.beans.PropertyEditorSupport;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * {@link java.nio.file.Path} 的属性编辑器，用于直接填充 Path 属性，
 * 而不通过 String 属性作为桥梁。
 *
 * <p>基于 {@link Paths#get(URI)} 的解析算法，检查已注册的 NIO 文件系统提供器，
 * 包括 "file:..." 路径的默认文件系统。也支持 Spring 风格的 URL 表示法：
 * 任意完全限定的标准 URL 以及 Spring 特殊的 "classpath:" 伪 URL，
 * 还支持 Spring 上下文特定的相对文件路径。作为回退，如果未找到现有的
 * 上下文相关资源，将通过 {@code Paths#get(String)} 在文件系统中解析路径。
 *
 * @author Juergen Hoeller
 * @since 4.3.2
 * @see java.nio.file.Path
 * @see Paths#get(URI)
 * @see ResourceEditor
 * @see org.springframework.core.io.ResourceLoader
 * @see FileEditor
 * @see URLEditor
 */
public class PathEditor extends PropertyEditorSupport {

	private final ResourceEditor resourceEditor;


	/**
	 * 创建一个使用默认 ResourceEditor 的 PathEditor。
	 */
	public PathEditor() {
		this.resourceEditor = new ResourceEditor();
	}

	/**
	 * 创建一个使用指定 ResourceEditor 的 PathEditor。
	 * @param resourceEditor 要使用的 ResourceEditor
	 */
	public PathEditor(ResourceEditor resourceEditor) {
		Assert.notNull(resourceEditor, "ResourceEditor must not be null");
		this.resourceEditor = resourceEditor;
	}


	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		boolean nioPathCandidate = !text.startsWith(ResourceUtils.CLASSPATH_URL_PREFIX);
		if (nioPathCandidate && !text.startsWith("/")) {
			try {
				URI uri = new URI(text);
				if (uri.getScheme() != null) {
					nioPathCandidate = false;
					// 尝试通过 Paths.get(URI) 使用 NIO 文件系统提供器
					setValue(Paths.get(uri).normalize());
					return;
				}
			}
			catch (URISyntaxException ex) {
				// 不是有效的 URI；可能是 Windows 风格的路径，带有 file 前缀
				// 将尝试作为 Spring 资源位置处理
				nioPathCandidate = !text.startsWith(ResourceUtils.FILE_URL_PREFIX);
			}
			catch (FileSystemNotFoundException ex) {
				// URI 的 scheme 未在 NIO 中注册
				// 将尝试通过 Spring 的资源机制使用 URL 协议处理器
			}
		}

		this.resourceEditor.setAsText(text);
		Resource resource = (Resource) this.resourceEditor.getValue();
		if (resource == null) {
			setValue(null);
		}
		else if (nioPathCandidate && !resource.exists()) {
			setValue(Paths.get(text).normalize());
		}
		else {
			try {
				setValue(resource.getFile().toPath());
			}
			catch (IOException ex) {
				throw new IllegalArgumentException("Failed to retrieve file for " + resource, ex);
			}
		}
	}

	@Override
	public String getAsText() {
		Path value = (Path) getValue();
		return (value != null ? value.toString() : "");
	}

}
