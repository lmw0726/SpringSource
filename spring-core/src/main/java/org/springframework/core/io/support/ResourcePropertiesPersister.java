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

package org.springframework.core.io.support;

import org.springframework.core.SpringProperties;
import org.springframework.util.DefaultPropertiesPersister;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

/**
 * 基于普通 {@link DefaultPropertiesPersister} 的 Spring 感知子类，
 * 通过共享的 "spring.xml.ignore" 属性增加了对禁用 XML 支持的条件检查。
 *
 * <p>这是 Spring 资源支持中使用的标准实现。
 *
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @since 5.3
 */
public class ResourcePropertiesPersister extends DefaultPropertiesPersister {

	/**
	 * Spring 通用资源支持中使用的默认 {@code ResourcePropertiesPersister} 实例的便捷常量。
	 * @since 5.3
	 */
	public static final ResourcePropertiesPersister INSTANCE = new ResourcePropertiesPersister();

	/**
	 * 由 {@code spring.xml.ignore} 系统属性控制的布尔标志，
	 * 指示 Spring 是否忽略 XML，即不初始化与 XML 相关的基础设施。
	 * <p>默认值为 "false"。
	 */
	private static final boolean shouldIgnoreXml = SpringProperties.getFlag("spring.xml.ignore");


	@Override
	public void loadFromXml(Properties props, InputStream is) throws IOException {
		if (shouldIgnoreXml) {
			throw new UnsupportedOperationException("XML support disabled");
		}
		super.loadFromXml(props, is);
	}

	@Override
	public void storeToXml(Properties props, OutputStream os, String header) throws IOException {
		if (shouldIgnoreXml) {
			throw new UnsupportedOperationException("XML support disabled");
		}
		super.storeToXml(props, os, header);
	}

	@Override
	public void storeToXml(Properties props, OutputStream os, String header, String encoding) throws IOException {
		if (shouldIgnoreXml) {
			throw new UnsupportedOperationException("XML support disabled");
		}
		super.storeToXml(props, os, header, encoding);
	}

}
