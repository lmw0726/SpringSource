/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.Properties;

/**
 * {@link PropertiesPersister} 接口的默认实现。
 * 遵循 {@code java.util.Properties} 的原生解析方式。
 *
 * <p>允许从任意 {@code Reader} 读取并写入到任意 {@code Writer}，
 * 例如可以为属性文件指定字符集。这是标准的 {@code java.util.Properties}
 * 在 JDK 5 之前所不具备的功能：当时只能使用 ISO-8859-1 字符集加载文件。
 *
 * <p>从流中加载和写入流时，分别委托给 {@code Properties.load} 和 {@code Properties.store}，
 * 以完全兼容 JDK Properties 类实现的 Unicode 转换。从 JDK 6 开始，
 * {@code Properties.load/store} 也用于 Reader/Writer，
 * 实际上使该类成为一个纯粹的向后兼容适配器。
 *
 * <p>使用 Reader/Writer 的持久化代码遵循 JDK 的解析策略，
 * 但不实现 Unicode 转换，因为 Reader/Writer 应该已经正确进行字符的解码/编码。
 * 如果你希望在属性文件中使用 Unicode 转义字符，
 * 请不要为 Reader/Writer 指定编码（例如 ReloadableResourceBundleMessageSource 的
 * "defaultEncoding" 和 "fileEncodings" 属性）。
 *
 * @author Juergen Hoeller
 * @since 2004-03-10
 * @see java.util.Properties
 * @see java.util.Properties#load
 * @see java.util.Properties#store
 * @see org.springframework.core.io.support.ResourcePropertiesPersister
 */
public class DefaultPropertiesPersister implements PropertiesPersister {

	@Override
	public void load(Properties props, InputStream is) throws IOException {
		props.load(is);
	}

	@Override
	public void load(Properties props, Reader reader) throws IOException {
		props.load(reader);
	}

	@Override
	public void store(Properties props, OutputStream os, String header) throws IOException {
		props.store(os, header);
	}

	@Override
	public void store(Properties props, Writer writer, String header) throws IOException {
		props.store(writer, header);
	}

	@Override
	public void loadFromXml(Properties props, InputStream is) throws IOException {
		props.loadFromXML(is);
	}

	@Override
	public void storeToXml(Properties props, OutputStream os, String header) throws IOException {
		props.storeToXML(os, header);
	}

	@Override
	public void storeToXml(Properties props, OutputStream os, String header, String encoding) throws IOException {
		props.storeToXML(os, header, encoding);
	}

}
