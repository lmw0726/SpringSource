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

import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;

/**
 * {@link EntityResolver} implementation for the Spring beans DTD,
 * to load the DTD from the Spring class path (or JAR file).
 *
 * <p>Fetches "spring-beans.dtd" from the class path resource
 * "/org/springframework/beans/factory/xml/spring-beans.dtd",
 * no matter whether specified as some local URL that includes "spring-beans"
 * in the DTD name or as "https://www.springframework.org/dtd/spring-beans-2.0.dtd".
 *
 * @author Juergen Hoeller
 * @author Colin Sampaleanu
 * @see ResourceEntityResolver
 * @since 04.06.2003
 */
public class BeansDtdResolver implements EntityResolver {

	private static final String DTD_EXTENSION = ".dtd";

	private static final String DTD_NAME = "spring-beans";

	private static final Log logger = LogFactory.getLog(BeansDtdResolver.class);


	@Override
	@Nullable
	public InputSource resolveEntity(@Nullable String publicId, @Nullable String systemId) throws IOException {
		if (logger.isTraceEnabled()) {
			logger.trace("Trying to resolve XML entity with public ID [" + publicId +
					"] and system ID [" + systemId + "]");
		}

		if (systemId == null || !systemId.endsWith(DTD_EXTENSION)) {
			//如果系统ID为空，或者不以DTD结尾，返回空
			return null;
		}
		//最后的路径分割符位置
		int lastPathSeparator = systemId.lastIndexOf('/');
		int dtdNameStart = systemId.indexOf(DTD_NAME, lastPathSeparator);
		if (dtdNameStart == -1) {
			//如果从最后的路径分割符位置找字符串"spring-beans"，没有找到，则返回空
			return null;
		}
		//spring-beans.dtd
		String dtdFile = DTD_NAME + DTD_EXTENSION;
		if (logger.isTraceEnabled()) {
			logger.trace("Trying to locate [" + dtdFile + "] in Spring jar on classpath");
		}
		try {
			//加载org/springframework/beans/factory/xml/spring-beans.dtd
			Resource resource = new ClassPathResource(dtdFile, getClass());
			//获取InputSource
			InputSource source = new InputSource(resource.getInputStream());
			//设置公共ID
			source.setPublicId(publicId);
			//设置系统ID
			source.setSystemId(systemId);
			if (logger.isTraceEnabled()) {
				logger.trace("Found beans DTD [" + systemId + "] in classpath: " + dtdFile);
			}
			return source;
		} catch (FileNotFoundException ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("Could not resolve beans DTD [" + systemId + "]: not found in classpath", ex);
			}
		}

		//回退到解析器的默认行为
		return null;
	}


	@Override
	public String toString() {
		return "EntityResolver for spring-beans DTD";
	}

}
