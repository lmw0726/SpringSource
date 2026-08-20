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

package org.springframework.mail.javamail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.activation.FileTypeMap;
import javax.activation.MimetypesFileTypeMap;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;

/**
 * 基于 Spring 配置的 {@code FileTypeMap} 实现，它将从标准 JavaMail MIME 类型
 * 映射文件中读取 MIME 类型到文件扩展名的映射关系，底层使用标准的
 * {@code MimetypesFileTypeMap}。
 *
 * <p>映射文件应遵循以下格式（由 Java Activation Framework 定义）：
 *
 * <pre class="code">
 * # 将 text/html 映射到 .htm 和 .html 文件
 * text/html  html htm HTML HTM</pre>
 *
 * 以 {@code #} 开头的行被视为注释并被忽略。所有其他行被视为映射条目。
 * 每个映射行应包含 MIME 类型作为第一个条目，然后是映射到该 MIME 类型的
 * 各个文件扩展名作为后续条目。各条目之间用空格或制表符分隔。
 *
 * <p>默认情况下，使用位于本类所在包中的 {@code mime.types} 文件中的映射，
 * 该文件涵盖了许多常见的文件扩展名（与 {@code activation.jar} 中开箱即用
 * 的映射不同）。
 * 可以使用 {@code mappingLocation} 属性覆盖此默认设置。
 *
 * <p>可以通过 {@code mappings} Bean 属性添加额外的映射，
 * 其格式应遵循 {@code mime.types} 文件格式。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 1.2
 * @see #setMappingLocation
 * @see #setMappings
 * @see javax.activation.MimetypesFileTypeMap
 */
public class ConfigurableMimeFileTypeMap extends FileTypeMap implements InitializingBean {

	/**
	 * 用于加载映射文件的 {@code Resource}。
	 */
	private Resource mappingLocation = new ClassPathResource("mime.types", getClass());

	/**
	 * 用于配置额外的映射。
	 */
	@Nullable
	private String[] mappings;

	/**
	 * 委托的 FileTypeMap，由映射文件中的映射和 {@code mappings} 属性中的条目编译而成。
	 */
	@Nullable
	private FileTypeMap fileTypeMap;


	/**
	 * 指定加载映射所用的 {@code Resource}。
	 * <p>需要遵循 Java Activation Framework 定义的 {@code mime.types} 文件格式，
	 * 包含如下格式的行：<br>
	 * {@code text/html  html htm HTML HTM}
	 */
	public void setMappingLocation(Resource mappingLocation) {
		this.mappingLocation = mappingLocation;
	}

	/**
	 * 以遵循 Java Activation Framework 定义的 {@code mime.types} 文件格式的行，
	 * 指定额外的 MIME 类型映射。例如：<br>
	 * {@code text/html  html htm HTML HTM}
	 */
	public void setMappings(String... mappings) {
		this.mappings = mappings;
	}


	/**
	 * 创建最终的合并映射集合。
	 */
	@Override
	public void afterPropertiesSet() {
		getFileTypeMap();
	}

	/**
	 * 返回委托的 FileTypeMap，由映射文件中的映射和 {@code mappings} 属性中的条目编译而成。
	 * @see #setMappingLocation
	 * @see #setMappings
	 * @see #createFileTypeMap
	 */
	protected final FileTypeMap getFileTypeMap() {
		if (this.fileTypeMap == null) {
			try {
				this.fileTypeMap = createFileTypeMap(this.mappingLocation, this.mappings);
			}
			catch (IOException ex) {
				throw new IllegalStateException(
						"Could not load specified MIME type mapping file: " + this.mappingLocation, ex);
			}
		}
		return this.fileTypeMap;
	}

	/**
	 * 从给定映射文件中的映射和给定的映射条目编译 {@link FileTypeMap}。
	 * <p>默认实现创建一个 Activation Framework {@link MimetypesFileTypeMap}，
	 * 传入映射资源的 InputStream（如果有的话），并通过编程方式注册映射行。
	 * @param mappingLocation {@code mime.types} 映射资源（可为 {@code null}）
	 * @param mappings MIME 类型映射行数组（可为 {@code null}）
	 * @return 编译后的 FileTypeMap
	 * @throws IOException 如果资源访问失败
	 * @see javax.activation.MimetypesFileTypeMap#MimetypesFileTypeMap(java.io.InputStream)
	 * @see javax.activation.MimetypesFileTypeMap#addMimeTypes(String)
	 */
	protected FileTypeMap createFileTypeMap(@Nullable Resource mappingLocation, @Nullable String[] mappings) throws IOException {
		MimetypesFileTypeMap fileTypeMap = null;
		if (mappingLocation != null) {
			try (InputStream is = mappingLocation.getInputStream()) {
				fileTypeMap = new MimetypesFileTypeMap(is);
			}
		}
		else {
			fileTypeMap = new MimetypesFileTypeMap();
		}
		if (mappings != null) {
			for (String mapping : mappings) {
				fileTypeMap.addMimeTypes(mapping);
			}
		}
		return fileTypeMap;
	}


	/**
	 * 委托给底层的 FileTypeMap。
	 * @see #getFileTypeMap()
	 */
	@Override
	public String getContentType(File file) {
		return getFileTypeMap().getContentType(file);
	}

	/**
	 * 委托给底层的 FileTypeMap。
	 * @see #getFileTypeMap()
	 */
	@Override
	public String getContentType(String fileName) {
		return getFileTypeMap().getContentType(fileName);
	}

}
