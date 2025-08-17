/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.beans.factory.xml;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;

/**
 * {@link EntityResolver} 实现，分别委托给 {@link BeansDtdResolver} 和 {@link PluggableSchemaResolver}
 * 来处理 DTD 和 XML Schema。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Rick Evans
 * @see BeansDtdResolver
 * @see PluggableSchemaResolver
 * @since 2.0
 */
public class DelegatingEntityResolver implements EntityResolver {

	/**
	 * DTD文件的后缀。
	 */
	public static final String DTD_SUFFIX = ".dtd";

	/**
	 * 模式定义文件的后缀。
	 */
	public static final String XSD_SUFFIX = ".xsd";

	/**
	 * DTD解析器
	 */
	private final EntityResolver dtdResolver;
	/**
	 * 模式解析器
	 */
	private final EntityResolver schemaResolver;


	/**
	 * 创建一个新的 DelegatingEntityResolver，委托给默认的 {@link BeansDtdResolver} 和
	 * 默认的 {@link PluggableSchemaResolver}。
	 * <p>使用提供的 {@link ClassLoader} 配置 {@link PluggableSchemaResolver}。
	 *
	 * @param classLoader 用于加载的 ClassLoader
	 *                    （可以为 {@code null}，使用默认 ClassLoader）
	 */
	public DelegatingEntityResolver(@Nullable ClassLoader classLoader) {
		this.dtdResolver = new BeansDtdResolver();
		this.schemaResolver = new PluggableSchemaResolver(classLoader);
	}

	/**
	 * 创建一个新的 DelegatingEntityResolver，委托给给定的 {@link EntityResolver}。
	 *
	 * @param dtdResolver    用于解析 DTD 的 EntityResolver
	 * @param schemaResolver 用于解析 XML Schema 的 EntityResolver
	 */
	public DelegatingEntityResolver(EntityResolver dtdResolver, EntityResolver schemaResolver) {
		Assert.notNull(dtdResolver, "'dtdResolver' is required");
		Assert.notNull(schemaResolver, "'schemaResolver' is required");
		this.dtdResolver = dtdResolver;
		this.schemaResolver = schemaResolver;
	}


	@Override
	@Nullable
	public InputSource resolveEntity(@Nullable String publicId, @Nullable String systemId)
			throws SAXException, IOException {

		if (systemId == null) {
			//没有系统编号，返回null。
			return null;
		}
		if (systemId.endsWith(DTD_SUFFIX)) {
			//以DTO文件后缀结尾，使用DTD解析器解析
			return this.dtdResolver.resolveEntity(publicId, systemId);
		} else if (systemId.endsWith(XSD_SUFFIX)) {
			//以XSD文件后缀结尾，使用XSD解析器解析
			return this.schemaResolver.resolveEntity(publicId, systemId);
		}

		//回退到解析器的默认行为
		return null;
	}


	@Override
	public String toString() {
		return "EntityResolver delegating " + XSD_SUFFIX + " to " + this.schemaResolver +
				" and " + DTD_SUFFIX + " to " + this.dtdResolver;
	}

}
