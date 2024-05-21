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

package org.springframework.web.servlet.config.annotation;

import org.springframework.util.CollectionUtils;
import org.springframework.web.cors.CorsConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 辅助注册全局的基于URL模式的 {@link CorsConfiguration} 映射。
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @see CorsRegistration
 * @since 4.2
 */
public class CorsRegistry {
	/**
	 * 跨域注册列表
	 */
	private final List<CorsRegistration> registrations = new ArrayList<>();


	/**
	 * 为指定的路径模式启用跨源请求处理。
	 * <p>支持精确路径映射URI（例如 {@code "/admin"}）以及Ant风格的路径模式（例如 {@code "/admin/**"}）。
	 * <p>默认情况下，此映射的 {@code CorsConfiguration} 使用默认值初始化，如
	 * {@link CorsConfiguration#applyPermitDefaultValues()} 中所述。
	 */
	public CorsRegistration addMapping(String pathPattern) {
		// 创建跨域注册
		CorsRegistration registration = new CorsRegistration(pathPattern);
		// 将注册添加到注册列表中
		this.registrations.add(registration);
		// 返回注册
		return registration;
	}

	/**
	 * 返回按路径模式键入的注册的 {@link CorsConfiguration} 对象。
	 */
	protected Map<String, CorsConfiguration> getCorsConfigurations() {
		// 创建一个 LinkedHashMap，用于存储跨域配置
		Map<String, CorsConfiguration> configs = CollectionUtils.newLinkedHashMap(this.registrations.size());
		// 遍历所有的注册
		for (CorsRegistration registration : this.registrations) {
			// 将路径模式和对应的跨域配置添加到 configs 中
			configs.put(registration.getPathPattern(), registration.getCorsConfiguration());
		}
		// 返回跨域配置映射
		return configs;
	}

}
