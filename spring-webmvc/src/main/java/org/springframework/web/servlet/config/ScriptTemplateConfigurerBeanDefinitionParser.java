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

package org.springframework.web.servlet.config;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSimpleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * 解析 <code>&lt;mvc:script-template-configurer&gt;</code> MVC 命名空间元素并
 * 注册 {@code ScriptTemplateConfigurer} bean。
 *
 * @author Sebastien Deleuze
 * @since 4.2
 */
public class ScriptTemplateConfigurerBeanDefinitionParser extends AbstractSimpleBeanDefinitionParser {

	/**
	 * 用于 {@code ScriptTemplateConfigurer} 的 bean 名称。
	 */
	public static final String BEAN_NAME = "mvcScriptTemplateConfigurer";


	@Override
	protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext) {
		return BEAN_NAME;
	}

	@Override
	protected String getBeanClassName(Element element) {
		return "org.springframework.web.servlet.view.script.ScriptTemplateConfigurer";
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		// 获取元素中名为 "script" 的子元素列表
		List<Element> childElements = DomUtils.getChildElementsByTagName(element, "script");
		// 如果 "script" 子元素列表不为空
		if (!childElements.isEmpty()) {
			// 创建一个列表用于存储位置
			List<String> locations = new ArrayList<>(childElements.size());
			// 遍历子元素并添加每个子元素的 "location" 属性值到列表中
			for (Element childElement : childElements) {
				locations.add(childElement.getAttribute("location"));
			}
			// 将 "scripts" 属性添加到构建器的属性值中
			builder.addPropertyValue("scripts", StringUtils.toStringArray(locations));
		}

		// 将 "engineName" 属性添加到构建器的属性值中
		builder.addPropertyValue("engineName", element.getAttribute("engine-name"));

		// 如果元素具有 "render-object" 属性
		if (element.hasAttribute("render-object")) {
			// 将 "renderObject" 属性添加到构建器的属性值中
			builder.addPropertyValue("renderObject", element.getAttribute("render-object"));
		}

		// 如果元素具有 "render-function" 属性
		if (element.hasAttribute("render-function")) {
			// 将 "renderFunction" 属性添加到构建器的属性值中
			builder.addPropertyValue("renderFunction", element.getAttribute("render-function"));
		}

		// 如果元素具有 "content-type" 属性
		if (element.hasAttribute("content-type")) {
			// 将 "contentType" 属性添加到构建器的属性值中
			builder.addPropertyValue("contentType", element.getAttribute("content-type"));
		}

		// 如果元素具有 "charset" 属性
		if (element.hasAttribute("charset")) {
			// 将 "charset" 属性添加到构建器的属性值中
			builder.addPropertyValue("charset", Charset.forName(element.getAttribute("charset")));
		}

		// 如果元素具有 "resource-loader-path" 属性
		if (element.hasAttribute("resource-loader-path")) {
			// 将 "resourceLoaderPath" 属性添加到构建器的属性值中
			builder.addPropertyValue("resourceLoaderPath", element.getAttribute("resource-loader-path"));
		}

		// 如果元素具有 "shared-engine" 属性
		if (element.hasAttribute("shared-engine")) {
			// 将 "sharedEngine" 属性添加到构建器的属性值中
			builder.addPropertyValue("sharedEngine", element.getAttribute("shared-engine"));
		}
	}

	@Override
	protected boolean isEligibleAttribute(String name) {
		// 返回 true 如果 name 是以下任意一个值："engine-name", "scripts", "render-object",
		// "render-function", "content-type", "charset", 或 "resource-loader-path"
		return (name.equals("engine-name") || name.equals("scripts") || name.equals("render-object") ||
				name.equals("render-function") || name.equals("content-type") ||
				name.equals("charset") || name.equals("resource-loader-path"));
	}

}
