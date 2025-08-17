/*
 * Copyright 2002-2020 the original author or authors.
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

import org.springframework.core.io.ClassPathResource;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

import java.beans.PropertyEditorSupport;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * {@code java.net.URI} 的编辑器，用于直接填充 URI 属性，而不是通过 String 属性作为桥梁。
 *
 * <p>支持 Spring 风格的 URI 表示法：任何完全限定的标准 URI（如 "file:"、"http:" 等），
 * 以及 Spring 的特殊 "classpath:" 伪 URL，会被解析为对应的 URI。
 *
 * <p>默认情况下，该编辑器会对字符串进行编码，例如，将空格编码为 {@code %20}。
 * 可通过调用 {@link #URIEditor(boolean)} 构造函数修改此行为。
 *
 * <p>注意：URI 比 URL 更宽松，它不要求必须指定有效协议。
 * 在有效的 URI 语法内，允许任何 scheme，即使没有注册相应的协议处理器。
 *
 * @author Juergen Hoeller
 * @since 2.0.2
 * @see java.net.URI
 * @see URLEditor
 */
public class URIEditor extends PropertyEditorSupport {

	@Nullable
	private final ClassLoader classLoader;

	private final boolean encode;



	/**
	 * 创建一个新的 URIEditor 并启用编码，将 "classpath:" 路径转换为标准 URI（不尝试解析为物理资源）。
	 */
	public URIEditor() {
		this(true);
	}

	/**
	 * 创建一个新的 URIEditor，将 "classpath:" 路径转换为标准 URI（不尝试解析为物理资源）。
	 * @param encode 指示字符串是否进行编码
	 * @since 3.0
	 */
	public URIEditor(boolean encode) {
		this.classLoader = null;
		this.encode = encode;
	}

	/**
	 * 创建一个新的 URIEditor，使用指定的 ClassLoader 将 "classpath:" 路径解析为物理资源 URL。
	 * @param classLoader 用于解析 "classpath:" 路径的 ClassLoader（可为 {@code null} 表示默认 ClassLoader）
	 */
	public URIEditor(@Nullable ClassLoader classLoader) {
		this(classLoader, true);
	}

	/**
	 * 创建一个新的 URIEditor，使用指定的 ClassLoader 将 "classpath:" 路径解析为物理资源 URL。
	 * @param classLoader 用于解析 "classpath:" 路径的 ClassLoader（可为 {@code null} 表示默认 ClassLoader）
	 * @param encode 指示字符串是否进行编码
	 * @since 3.0
	 */
	public URIEditor(@Nullable ClassLoader classLoader, boolean encode) {
		this.classLoader = (classLoader != null ? classLoader : ClassUtils.getDefaultClassLoader());
		this.encode = encode;
	}


	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		if (StringUtils.hasText(text)) {
			String uri = text.trim();
			if (this.classLoader != null && uri.startsWith(ResourceUtils.CLASSPATH_URL_PREFIX)) {
				ClassPathResource resource = new ClassPathResource(
						uri.substring(ResourceUtils.CLASSPATH_URL_PREFIX.length()), this.classLoader);
				try {
					setValue(resource.getURI());
				}
				catch (IOException ex) {
					throw new IllegalArgumentException("Could not retrieve URI for " + resource + ": " + ex.getMessage());
				}
			}
			else {
				try {
					setValue(createURI(uri));
				}
				catch (URISyntaxException ex) {
					throw new IllegalArgumentException("Invalid URI syntax: " + ex.getMessage());
				}
			}
		}
		else {
			setValue(null);
		}
	}

	/**
	 * 根据用户提供的字符串创建 URI 实例。
	 * <p>默认实现会将字符串编码为符合 RFC-2396 的 URI。
	 * @param value 要转换为 URI 的字符串
	 * @return URI 实例
	 * @throws java.net.URISyntaxException 如果 URI 转换失败
	 */
	protected URI createURI(String value) throws URISyntaxException {
		int colonIndex = value.indexOf(':');
		if (this.encode && colonIndex != -1) {
			int fragmentIndex = value.indexOf('#', colonIndex + 1);
			String scheme = value.substring(0, colonIndex);
			String ssp = value.substring(colonIndex + 1, (fragmentIndex > 0 ? fragmentIndex : value.length()));
			String fragment = (fragmentIndex > 0 ? value.substring(fragmentIndex + 1) : null);
			return new URI(scheme, ssp, fragment);
		}
		else {
			// 不编码或值不包含 scheme - 使用默认构造
			return new URI(value);
		}
	}


	@Override
	public String getAsText() {
		URI value = (URI) getValue();
		return (value != null ? value.toString() : "");
	}

}
