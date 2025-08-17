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

package org.springframework.beans.factory.xml;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.core.Conventions;
import org.springframework.lang.Nullable;

/**
 * 简单的 {@code NamespaceHandler} 实现，将自定义属性直接映射到 bean 属性上。
 * 需要注意的一点是，该 {@code NamespaceHandler} 没有对应的模式文件，
 * 因为无法预先知道所有可能的属性名称。
 *
 * <p>下面展示了该 {@code NamespaceHandler} 的使用示例：
 *
 * <pre class="code">
 * &lt;bean id=&quot;rob&quot; class=&quot;..TestBean&quot; p:name=&quot;Rob Harrop&quot; p:spouse-ref=&quot;sally&quot;/&gt;</pre>
 * <p>
 * 这里 '{@code p:name}' 直接对应类 '{@code TestBean}' 的 '{@code name}' 属性。
 * '{@code p:spouse-ref}' 属性对应 '{@code spouse}' 属性，
 * 并不是具体的值，而是将要注入到该属性的 bean 名称。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 */
public class SimplePropertyNamespaceHandler implements NamespaceHandler {

	private static final String REF_SUFFIX = "-ref";


	@Override
	public void init() {
	}

	@Override
	@Nullable
	public BeanDefinition parse(Element element, ParserContext parserContext) {
		parserContext.getReaderContext().error(
				"Class [" + getClass().getName() + "] does not support custom elements.", element);
		return null;
	}

	@Override
	public BeanDefinitionHolder decorate(Node node, BeanDefinitionHolder definition, ParserContext parserContext) {
		if (node instanceof Attr) {
			//如果节点是Attr类型
			Attr attr = (Attr) node;
			//获取属性节点的本地名称
			String propertyName = parserContext.getDelegate().getLocalName(attr);
			//获取属性节点的值
			String propertyValue = attr.getValue();
			//获取bean定义的可变属性值对
			MutablePropertyValues pvs = definition.getBeanDefinition().getPropertyValues();
			if (pvs.contains(propertyName)) {
				//如果该属性已经在可变属性值对中了，提示该属性已被使用的错误
				parserContext.getReaderContext().error("Property '" + propertyName + "' is already defined using " +
						"both <property> and inline syntax. Only one approach may be used per property.", attr);
			}
			if (propertyName.endsWith(REF_SUFFIX)) {
				//如果属性名以-ref结尾，获取-ref属性之前的字符串作为属性名
				propertyName = propertyName.substring(0, propertyName.length() - REF_SUFFIX.length());
				//将名称转为驼峰模式后，将属性值构建成RuntimeBeanReference实例，添加到可变属性值对中。
				pvs.add(Conventions.attributeNameToPropertyName(propertyName), new RuntimeBeanReference(propertyValue));
			} else {
				//将名称转为驼峰模式，并添加到可变属性值对中
				pvs.add(Conventions.attributeNameToPropertyName(propertyName), propertyValue);
			}
		}
		return definition;
	}

}
