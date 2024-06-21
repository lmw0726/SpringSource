/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.http.codec;

import org.springframework.beans.BeanUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.util.ClassUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * 内部委托类，用于加载默认编解码器配置器类名。
 * 与支持包中的默认实现形成松散的关系，仅需知道要使用的默认类名。
 *
 * @author Juergen Hoeller
 * @see ClientCodecConfigurer#create()
 * @see ServerCodecConfigurer#create()
 * @since 5.0.1
 */
final class CodecConfigurerFactory {

	/**
	 * 默认配置程序路径
	 */
	private static final String DEFAULT_CONFIGURERS_PATH = "CodecConfigurer.properties";

	/**
	 * 默认编解码器配置程序映射
	 */
	private static final Map<Class<?>, Class<?>> defaultCodecConfigurers = new HashMap<>(4);

	static {
		try {
			// 加载位于默认配置路径下的属性文件
			Properties props = PropertiesLoaderUtils.loadProperties(
					new ClassPathResource(DEFAULT_CONFIGURERS_PATH, CodecConfigurerFactory.class));

			// 遍历属性文件中的所有属性名
			for (String ifcName : props.stringPropertyNames()) {
				// 获取接口名对应的实现类名
				String implName = props.getProperty(ifcName);

				// 根据类名加载对应的接口和实现类
				Class<?> ifc = ClassUtils.forName(ifcName, CodecConfigurerFactory.class.getClassLoader());
				Class<?> impl = ClassUtils.forName(implName, CodecConfigurerFactory.class.getClassLoader());

				// 将接口和实现类的映射关系放入 默认编解码器配置程序映射 中
				defaultCodecConfigurers.put(ifc, impl);
			}
		} catch (IOException | ClassNotFoundException ex) {
			// 捕获 IO异常 或 未找到类异常 异常，并抛出 非法状态异常
			throw new IllegalStateException(ex);
		}
	}


	private CodecConfigurerFactory() {
	}


	@SuppressWarnings("unchecked")
	public static <T extends CodecConfigurer> T create(Class<T> ifc) {
		// 获取 指定类型 对应的实现类
		Class<?> impl = defaultCodecConfigurers.get(ifc);

		// 如果实现类为 null，抛出 非法状态异常
		if (impl == null) {
			throw new IllegalStateException("No default codec configurer found for " + ifc);
		}

		// 实例化实现类并返回
		return (T) BeanUtils.instantiateClass(impl);
	}

}
