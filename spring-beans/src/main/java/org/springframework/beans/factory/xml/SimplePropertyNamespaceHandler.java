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
 * Simple {@code NamespaceHandler} implementation that maps custom attributes
 * directly through to bean properties. An important point to note is that this
 * {@code NamespaceHandler} does not have a corresponding schema since there
 * is no way to know in advance all possible attribute names.
 *
 * <p>An example of the usage of this {@code NamespaceHandler} is shown below:
 *
 * <pre class="code">
 * &lt;bean id=&quot;rob&quot; class=&quot;..TestBean&quot; p:name=&quot;Rob Harrop&quot; p:spouse-ref=&quot;sally&quot;/&gt;</pre>
 * <p>
 * Here the '{@code p:name}' corresponds directly to the '{@code name}'
 * property on class '{@code TestBean}'. The '{@code p:spouse-ref}'
 * attributes corresponds to the '{@code spouse}' property and, rather
 * than being the concrete value, it contains the name of the bean that will
 * be injected into that property.
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
