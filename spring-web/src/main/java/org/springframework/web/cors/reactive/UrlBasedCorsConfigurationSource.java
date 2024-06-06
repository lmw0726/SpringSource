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

package org.springframework.web.cors.reactive;

import org.springframework.http.server.PathContainer;
import org.springframework.lang.Nullable;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 使用URL模式选择请求的{@code CorsConfiguration}的{@code CorsConfigurationSource}。
 *
 * @author Sebastien Deleuze
 * @author Brian Clozel
 * @see PathPattern
 * @since 5.0
 */
public class UrlBasedCorsConfigurationSource implements CorsConfigurationSource {
	/**
	 * 路径模式解析器
	 */
	private final PathPatternParser patternParser;

	/**
	 * 路径模式 —— 跨域配置 映射
	 */
	private final Map<PathPattern, CorsConfiguration> corsConfigurations = new LinkedHashMap<>();


	/**
	 * 使用默认的{@code PathPatternParser}构造一个新的{@code UrlBasedCorsConfigurationSource}实例。
	 *
	 * @since 5.0.6
	 */
	public UrlBasedCorsConfigurationSource() {
		this(PathPatternParser.defaultInstance);
	}

	/**
	 * 从提供的{@code PathPatternParser}构造一个新的{@code UrlBasedCorsConfigurationSource}实例。
	 *
	 * @param patternParser 路径模式解析器
	 */
	public UrlBasedCorsConfigurationSource(PathPatternParser patternParser) {
		this.patternParser = patternParser;
	}


	/**
	 * 基于URL模式设置CORS配置。
	 *
	 * @param configMap URL模式与CORS配置的映射
	 */
	public void setCorsConfigurations(@Nullable Map<String, CorsConfiguration> configMap) {
		// 清除当前的 CORS 配置
		this.corsConfigurations.clear();
		if (configMap != null) {
			// 如果配置映射不为空，则注册每个配置
			configMap.forEach(this::registerCorsConfiguration);
		}
	}

	/**
	 * 为指定的路径模式注册{@link CorsConfiguration}。
	 *
	 * @param path   路径模式
	 * @param config CORS配置
	 */
	public void registerCorsConfiguration(String path, CorsConfiguration config) {
		this.corsConfigurations.put(this.patternParser.parse(path), config);
	}

	@Override
	@Nullable
	public CorsConfiguration getCorsConfiguration(ServerWebExchange exchange) {
		// 获取请求的路径
		PathContainer path = exchange.getRequest().getPath().pathWithinApplication();
		// 遍历 CORS 配置映射
		for (Map.Entry<PathPattern, CorsConfiguration> entry : this.corsConfigurations.entrySet()) {
			if (entry.getKey().matches(path)) {
				// 如果当前路径匹配到了 CORS 配置的路径模式，则返回相应的配置
				return entry.getValue();
			}
		}
		// 如果未匹配到任何配置，则返回空
		return null;
	}

}
