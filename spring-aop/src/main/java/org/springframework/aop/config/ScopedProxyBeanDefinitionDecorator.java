/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.aop.config;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionDecorator;
import org.springframework.beans.factory.xml.ParserContext;

/**
 * {@link BeanDefinitionDecorator} responsible for parsing the
 * {@code <aop:scoped-proxy/>} tag.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @since 2.0
 */
class ScopedProxyBeanDefinitionDecorator implements BeanDefinitionDecorator {

	private static final String PROXY_TARGET_CLASS = "proxy-target-class";


	@Override
	public BeanDefinitionHolder decorate(Node node, BeanDefinitionHolder definition, ParserContext parserContext) {
		boolean proxyTargetClass = true;
		if (node instanceof Element) {
			//如果节点是Element类型
			Element ele = (Element) node;
			if (ele.hasAttribute(PROXY_TARGET_CLASS)) {
				//并且该类型含有proxy-target-class属性，解析该属性值，转为boolean类型后，赋值给proxyTargetClass
				proxyTargetClass = Boolean.parseBoolean(ele.getAttribute(PROXY_TARGET_CLASS));
			}
		}


		//注册原始bean定义，因为它将由作用域代理引用，并且与工具 (验证，导航) 相关。
		BeanDefinitionHolder holder =
				ScopedProxyUtils.createScopedProxy(definition, parserContext.getRegistry(), proxyTargetClass);
		//根据bean定义获取目标bean名称
		String targetBeanName = ScopedProxyUtils.getTargetBeanName(definition.getBeanName());
		//触发bean定义注册事件
		parserContext.getReaderContext().fireComponentRegistered(
				new BeanComponentDefinition(definition.getBeanDefinition(), targetBeanName));
		return holder;
	}

}
