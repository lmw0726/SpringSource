/*
 * Copyright 2002-2016 the original author or authors.
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
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.mvc.HttpRequestHandlerAdapter;
import org.springframework.web.servlet.resource.DefaultServletHttpRequestHandler;
import org.w3c.dom.Element;

import java.util.Map;

/**
 * 解析 {@code default-servlet-handler} 元素的 {@link BeanDefinitionParser}，
 * 用于注册 {@link DefaultServletHttpRequestHandler}。
 * 还将注册一个 {@link SimpleUrlHandlerMapping} 以映射资源请求，
 * 以及一个 {@link HttpRequestHandlerAdapter}。
 *
 * @author Jeremy Grelle
 * @author Rossen Stoyanchev
 * @since 3.0.4
 */
class DefaultServletHandlerBeanDefinitionParser implements BeanDefinitionParser {

	@Override
	@Nullable
	public BeanDefinition parse(Element element, ParserContext parserContext) {
		// 从解析上下文中提取元素的源对象
		Object source = parserContext.extractSource(element);

		// 获取元素的 "default-servlet-name" 属性
		String defaultServletName = element.getAttribute("default-servlet-name");
		// 创建 RootBeanDefinition 对象，表示 DefaultServletHttpRequestHandler 类的定义
		RootBeanDefinition defaultServletHandlerDef = new RootBeanDefinition(DefaultServletHttpRequestHandler.class);
		// 设置源对象
		defaultServletHandlerDef.setSource(source);
		// 设置角色为基础设施角色
		defaultServletHandlerDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		if (StringUtils.hasText(defaultServletName)) {
			// 如果存在 defaultServletName 属性，则添加到属性值中
			defaultServletHandlerDef.getPropertyValues().add("defaultServletName", defaultServletName);
		}
		// 生成 DefaultServletHttpRequestHandler 的 bean 名称
		String defaultServletHandlerName = parserContext.getReaderContext().generateBeanName(defaultServletHandlerDef);
		// 将 DefaultServletHttpRequestHandler 注册到 BeanDefinitionRegistry 中
		parserContext.getRegistry().registerBeanDefinition(defaultServletHandlerName, defaultServletHandlerDef);
		// 将 DefaultServletHttpRequestHandler 注册为组件
		parserContext.registerComponent(new BeanComponentDefinition(defaultServletHandlerDef, defaultServletHandlerName));

		// 创建一个 ManagedMap 对象，用于存放 URL 和处理器映射
		Map<String, String> urlMap = new ManagedMap<>();
		// 将默认 servlet 处理器名称映射到所有 URL
		urlMap.put("/**", defaultServletHandlerName);

		// 创建 RootBeanDefinition 对象，表示 SimpleUrlHandlerMapping 类的定义
		RootBeanDefinition handlerMappingDef = new RootBeanDefinition(SimpleUrlHandlerMapping.class);
		// 设置源对象
		handlerMappingDef.setSource(source);
		// 设置角色为基础设施角色
		handlerMappingDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		// 添加 urlMap 属性值
		handlerMappingDef.getPropertyValues().add("urlMap", urlMap);

		// 生成 SimpleUrlHandlerMapping 的 bean 名称
		String handlerMappingBeanName = parserContext.getReaderContext().generateBeanName(handlerMappingDef);
		// 将 SimpleUrlHandlerMapping 注册到 BeanDefinitionRegistry 中
		parserContext.getRegistry().registerBeanDefinition(handlerMappingBeanName, handlerMappingDef);
		// 将 SimpleUrlHandlerMapping 注册为组件
		parserContext.registerComponent(new BeanComponentDefinition(handlerMappingDef, handlerMappingBeanName));

		// 确保 BeanNameUrlHandlerMapping 和默认的 HandlerAdapters 不被关闭
		// 注册默认组件
		MvcNamespaceUtils.registerDefaultComponents(parserContext, source);

		return null;
	}

}
