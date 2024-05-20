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

package org.springframework.web.servlet.config;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.w3c.dom.Element;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link org.springframework.beans.factory.xml.BeanDefinitionParser}，用于解析
 * {@code cors} 元素，以在 {@link AnnotationDrivenBeanDefinitionParser}、
 * {@link ResourcesBeanDefinitionParser} 和 {@link ViewControllerBeanDefinitionParser}
 * 创建的各种 {AbstractHandlerMapping} beans 中设置 CORS 配置。
 *
 * @author Sebastien Deleuze
 * @since 4.2
 */
public class CorsBeanDefinitionParser implements BeanDefinitionParser {

	@Override
	@Nullable
	public BeanDefinition parse(Element element, ParserContext parserContext) {

		// 创建一个 Map 来存储 CORS 配置
		Map<String, CorsConfiguration> corsConfigurations = new LinkedHashMap<>();

		// 获取所有名为 "mapping" 的子元素
		List<Element> mappings = DomUtils.getChildElementsByTagName(element, "mapping");

		// 如果没有找到任何 "mapping" 子元素
		if (mappings.isEmpty()) {
			// 创建一个默认的 CORS 配置并应用默认值
			CorsConfiguration config = new CorsConfiguration().applyPermitDefaultValues();
			// 将此默认配置应用于所有路径
			corsConfigurations.put("/**", config);
		} else {
			// 遍历每个 "mapping" 子元素
			for (Element mapping : mappings) {
				// 创建一个新的 CORS 配置
				CorsConfiguration config = new CorsConfiguration();

				// 如果 "mapping" 子元素具有 "allowed-origins" 属性
				if (mapping.hasAttribute("allowed-origins")) {
					// 获取 "allowed-origins" 属性值并将其转换为列表
					String[] allowedOrigins = StringUtils.tokenizeToStringArray(mapping.getAttribute("allowed-origins"), ",");
					// 设置 跨域配置 的允许源列表
					config.setAllowedOrigins(Arrays.asList(allowedOrigins));
				}

				// 如果 "mapping" 子元素具有 "allowed-origin-patterns" 属性
				if (mapping.hasAttribute("allowed-origin-patterns")) {
					// 获取 "allowed-origin-patterns" 属性值并将其转换为列表
					String[] patterns = StringUtils.tokenizeToStringArray(mapping.getAttribute("allowed-origin-patterns"), ",");
					//设置 跨域配置 的允许原模式
					config.setAllowedOriginPatterns(Arrays.asList(patterns));
				}

				// 如果 "mapping" 子元素具有 "allowed-methods" 属性
				if (mapping.hasAttribute("allowed-methods")) {
					// 获取 "allowed-methods" 属性值并将其转换为列表
					String[] allowedMethods = StringUtils.tokenizeToStringArray(mapping.getAttribute("allowed-methods"), ",");
					// 设置 跨域配置 的允许方法列表
					config.setAllowedMethods(Arrays.asList(allowedMethods));
				}

				// 如果 "mapping" 子元素具有 "allowed-headers" 属性
				if (mapping.hasAttribute("allowed-headers")) {
					// 获取 "allowed-headers" 属性值并将其转换为列表
					String[] allowedHeaders = StringUtils.tokenizeToStringArray(mapping.getAttribute("allowed-headers"), ",");
					// 设置 跨域配置 的允许请求头列表
					config.setAllowedHeaders(Arrays.asList(allowedHeaders));
				}

				// 如果 "mapping" 子元素具有 "exposed-headers" 属性
				if (mapping.hasAttribute("exposed-headers")) {
					// 获取 "exposed-headers" 属性值并将其转换为列表
					String[] exposedHeaders = StringUtils.tokenizeToStringArray(mapping.getAttribute("exposed-headers"), ",");
					// 设置 跨域配置 的暴露列表
					config.setExposedHeaders(Arrays.asList(exposedHeaders));
				}

				// 如果 "mapping" 子元素具有 "allow-credentials" 属性
				if (mapping.hasAttribute("allow-credentials")) {
					// 设置跨域配置的 允许凭据 属性值
					config.setAllowCredentials(Boolean.parseBoolean(mapping.getAttribute("allow-credentials")));
				}

				// 如果 "mapping" 子元素具有 "max-age" 属性
				if (mapping.hasAttribute("max-age")) {
					// 设置 最大生存时间 属性值
					config.setMaxAge(Long.parseLong(mapping.getAttribute("max-age")));
				}

				// 应用默认的许可值到 CORS 配置
				config.applyPermitDefaultValues();

				// 验证允许的凭证设置
				config.validateAllowCredentials();

				// 将此 CORS 配置添加到配置映射中
				corsConfigurations.put(mapping.getAttribute("path"), config);
			}
		}

		// 注册 CORS 配置
		MvcNamespaceUtils.registerCorsConfigurations(
				corsConfigurations, parserContext, parserContext.extractSource(element));

		return null;
	}

}
