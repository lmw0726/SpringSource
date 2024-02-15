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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.PropertiesPersister;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Properties;

/**
 * JavaBean风格组件的基类，需要从一个或多个资源加载属性。还支持本地属性，可配置覆盖。
 *
 * @author Juergen Hoeller
 * @since 1.2.2
 */
public abstract class PropertiesLoaderSupport {

	/**
	 * 可用于子类的记录器。
	 */
	protected final Log logger = LogFactory.getLog(getClass());

	/**
	 * 本地属性
	 */
	@Nullable
	protected Properties[] localProperties;

	/**
	 * 本地属性是否覆盖文件中的属性
	 */
	protected boolean localOverride = false;

	/**
	 * 资源位置
	 */
	@Nullable
	private Resource[] locations;

	/**
	 * 是否忽略未发现的资源
	 */
	private boolean ignoreResourceNotFound = false;

	/**
	 * 文件编码
	 */
	@Nullable
	private String fileEncoding;

	/**
	 * 属性持久化器
	 */
	private PropertiesPersister propertiesPersister = ResourcePropertiesPersister.INSTANCE;


	/**
	 * 设置本地属性，例如通过XML bean定义中的 "props" 标签设置。
	 * 这些属性可以被认为是默认值，可以被从文件中加载的属性覆盖。
	 *
	 * @param properties 要设置的本地属性
	 */
	public void setProperties(Properties properties) {
		this.localProperties = new Properties[]{properties};
	}

	/**
	 * 设置本地属性，例如通过XML bean定义中的 "props" 标签设置，
	 * 允许将多个属性集合合并为一个。
	 *
	 * @param propertiesArray 要设置的本地属性数组
	 */
	public void setPropertiesArray(Properties... propertiesArray) {
		this.localProperties = propertiesArray;
	}

	/**
	 * 设置要加载的属性文件的位置。
	 * <p>可以指向经典的属性文件，也可以指向遵循JDK 1.5的属性XML格式的XML文件。
	 *
	 * @param location 要设置的位置资源
	 */
	public void setLocation(Resource location) {
		this.locations = new Resource[]{location};
	}

	/**
	 * 设置要加载的属性文件的位置。
	 * <p>可以指向经典的属性文件，也可以指向遵循JDK 1.5的属性XML格式的XML文件。
	 * <p>注意：后续文件中定义的属性将覆盖较早文件中定义的属性，如果存在重叠的键。
	 * 因此，请确保给定位置列表中的最具体文件位于最后。
	 *
	 * @param locations 要设置的属性文件的位置资源数组
	 */
	public void setLocations(Resource... locations) {
		this.locations = locations;
	}

	/**
	 * 设置本地属性是否覆盖文件中的属性。
	 * <p>默认为 "false"：文件中的属性覆盖本地默认值。
	 * 可以切换为 "true"，以便让本地属性覆盖文件中的默认值。
	 *
	 * @param localOverride 是否本地属性覆盖文件中的属性
	 */
	public void setLocalOverride(boolean localOverride) {
		this.localOverride = localOverride;
	}

	/**
	 * 设置是否忽略找不到属性资源的失败。
	 * <p>如果属性文件是完全可选的，则选择 "true"。
	 * 默认为 "false"。
	 *
	 * @param ignoreResourceNotFound 是否忽略找不到属性资源的失败
	 */
	public void setIgnoreResourceNotFound(boolean ignoreResourceNotFound) {
		this.ignoreResourceNotFound = ignoreResourceNotFound;
	}

	/**
	 * 设置用于解析属性文件的编码。
	 * <p>默认为 none，使用 {@code java.util.Properties} 的默认编码。
	 * <p>仅适用于经典的属性文件，不适用于 XML 文件。
	 *
	 * @param encoding 要使用的编码
	 * @see org.springframework.util.PropertiesPersister#load
	 */
	public void setFileEncoding(String encoding) {
		this.fileEncoding = encoding;
	}

	/**
	 * 设置用于解析属性文件的 属性持久化器。
	 * 默认为 ResourcePropertiesPersister。
	 *
	 * @param propertiesPersister 要使用的 PropertiesPersister
	 * @see ResourcePropertiesPersister#INSTANCE
	 */
	public void setPropertiesPersister(@Nullable PropertiesPersister propertiesPersister) {
		this.propertiesPersister =
				(propertiesPersister != null ? propertiesPersister : ResourcePropertiesPersister.INSTANCE);
	}


	/**
	 * 返回一个合并的 Properties 实例，其中包含加载的属性和在此 FactoryBean 上设置的属性。
	 *
	 * @return 合并后的 Properties 实例
	 */
	protected Properties mergeProperties() throws IOException {
		Properties result = new Properties();

		if (this.localOverride) {
			// 提前从文件加载属性，以便让本地属性覆盖。
			loadProperties(result);
		}

		if (this.localProperties != null) {
			for (Properties localProp : this.localProperties) {
				CollectionUtils.mergePropertiesIntoMap(localProp, result);
			}
		}

		if (!this.localOverride) {
			// 后续从文件加载属性，以便让这些属性覆盖。
			loadProperties(result);
		}

		return result;
	}

	/**
	 * 将属性加载到给定实例中。
	 *
	 * @param props 要加载到的 Properties 实例
	 * @throws IOException 如果出现 I/O 错误
	 * @see #setLocations
	 */
	protected void loadProperties(Properties props) throws IOException {
		if (this.locations == null) {
			return;
		}
		for (Resource location : this.locations) {
			if (logger.isTraceEnabled()) {
				logger.trace("Loading properties file from " + location);
			}
			try {
				// 使用指定的文件编码、资源位置和属性持久化器填充属性
				PropertiesLoaderUtils.fillProperties(
						props, new EncodedResource(location, this.fileEncoding), this.propertiesPersister);
			} catch (FileNotFoundException | UnknownHostException | SocketException ex) {
				if (this.ignoreResourceNotFound) {
					if (logger.isDebugEnabled()) {
						logger.debug("Properties resource not found: " + ex.getMessage());
					}
				} else {
					throw ex;
				}
			}
		}
	}

}
