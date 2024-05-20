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
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;

/**
 * 解析 <code>&lt;mvc:tiles-configurer&gt;</code> MVC 命名空间元素并注册
 * 一个相应的 {@code TilesConfigurer} bean。
 *
 * @作者 Rossen Stoyanchev
 * @作者 Juergen Hoeller
 * @自从 4.1
 */
public class TilesConfigurerBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

	/**
	 * 用于 {@code TilesConfigurer} 的 bean 名称。
	 */
	public static final String BEAN_NAME = "mvcTilesConfigurer";


	@Override
	protected String getBeanClassName(Element element) {
		return "org.springframework.web.servlet.view.tiles3.TilesConfigurer";
	}

	@Override
	protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext) {
		return BEAN_NAME;
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		// 获取元素中名为 "definitions" 的子元素列表
		List<Element> childElements = DomUtils.getChildElementsByTagName(element, "definitions");
		// 如果 "definitions" 子元素列表不为空
		if (!childElements.isEmpty()) {
			// 创建一个列表用于存储位置
			List<String> locations = new ArrayList<>(childElements.size());
			// 遍历子元素并添加每个子元素的 "location" 属性值到列表中
			for (Element childElement : childElements) {
				locations.add(childElement.getAttribute("location"));
			}
			// 将 "definitions" 属性添加到构建器的属性值中
			builder.addPropertyValue("definitions", StringUtils.toStringArray(locations));
		}

		// 如果元素具有 "check-refresh" 属性
		if (element.hasAttribute("check-refresh")) {
			// 将 "check-refresh" 属性添加到构建器的属性值中
			builder.addPropertyValue("checkRefresh", element.getAttribute("check-refresh"));
		}

		// 如果元素具有 "validate-definitions" 属性
		if (element.hasAttribute("validate-definitions")) {
			// 将 "validate-definitions" 属性添加到构建器的属性值中
			builder.addPropertyValue("validateDefinitions", element.getAttribute("validate-definitions"));
		}

		// 如果元素具有 "definitions-factory" 属性
		if (element.hasAttribute("definitions-factory")) {
			// 将 "definitions-factory" 属性添加到构建器的属性值中
			builder.addPropertyValue("definitionsFactoryClass", element.getAttribute("definitions-factory"));
		}

		// 如果元素具有 "preparer-factory" 属性
		if (element.hasAttribute("preparer-factory")) {
			// 将 "preparer-factory" 属性添加到构建器的属性值中
			builder.addPropertyValue("preparerFactoryClass", element.getAttribute("preparer-factory"));
		}
	}

}
