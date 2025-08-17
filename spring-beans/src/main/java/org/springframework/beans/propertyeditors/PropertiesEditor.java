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

package org.springframework.beans.propertyeditors;

import org.springframework.lang.Nullable;

import java.beans.PropertyEditorSupport;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;

/**
 * 自定义 {@link java.beans.PropertyEditor} 用于 {@link Properties} 对象。
 *
 * <p>处理从内容 {@link String} 到 {@code Properties} 对象的转换。
 * 同时也处理 {@link Map} 到 {@code Properties} 的转换，可通过 XML 的 "map" 条目填充 {@code Properties} 对象。
 *
 * <p>所需格式遵循标准 {@code Properties} 文档，每个属性必须单独占一行。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see java.util.Properties#load
 */
public class PropertiesEditor extends PropertyEditorSupport {

	/**
	 * 将 {@link String} 转换为 {@link Properties}，将其视为属性内容。
	 * @param text 待转换的文本
	 */
	@Override
	public void setAsText(@Nullable String text) throws IllegalArgumentException {
		Properties props = new Properties();
		if (text != null) {
			try {
				// 必须使用 ISO-8859-1 编码，因为 Properties.load(stream) 期望此编码。
				props.load(new ByteArrayInputStream(text.getBytes(StandardCharsets.ISO_8859_1)));
			}
			catch (IOException ex) {
				// 理论上不会发生
				throw new IllegalArgumentException(
						"Failed to parse [" + text + "] into Properties", ex);
			}
		}
		setValue(props);
	}

	/**
	 * 如果值是 {@link Properties} 则直接使用；如果是 {@link Map} 则转换为 {@code Properties}。
	 */
	@Override
	public void setValue(Object value) {
		if (!(value instanceof Properties) && value instanceof Map) {
			Properties props = new Properties();
			props.putAll((Map<?, ?>) value);
			super.setValue(props);
		}
		else {
			super.setValue(value);
		}
	}

}
