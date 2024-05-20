/*
 * Copyright 2002-2021 the original author or authors.
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
 * 解析 <code>&lt;mvc:freemarker-configurer&gt;</code> MVC 命名空间元素并注册 {@code FreeMarkerConfigurer} bean。
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public class FreeMarkerConfigurerBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

	/**
	 * {@code FreeMarkerConfigurer} 使用的 bean 名称。
	 */
	public static final String BEAN_NAME = "mvcFreeMarkerConfigurer";

	@Override
	protected String getBeanClassName(Element element) {
		return "org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer";
	}

	@Override
	protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext) {
		return BEAN_NAME;
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		// 获取所有名为 "template-loader-path" 的子元素
		List<Element> childElements = DomUtils.getChildElementsByTagName(element, "template-loader-path");

		// 如果子元素列表不为空
		if (!childElements.isEmpty()) {
			// 创建一个列表来存储位置字符串
			List<String> locations = new ArrayList<>(childElements.size());

			// 遍历每个子元素
			for (Element childElement : childElements) {
				// 获取子元素的 "location" 属性并添加到位置列表中
				locations.add(childElement.getAttribute("location"));
			}

			// 将位置列表转换为数组并添加到构建器的属性值中
			builder.addPropertyValue("templateLoaderPaths", StringUtils.toStringArray(locations));
		}
	}

}
