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

package org.springframework.beans.factory.xml;

import java.util.Collection;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.ConstructorArgumentValues.ValueHolder;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.core.Conventions;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * Simple {@code NamespaceHandler} implementation that maps custom
 * attributes directly through to bean properties. An important point to note is
 * that this {@code NamespaceHandler} does not have a corresponding schema
 * since there is no way to know in advance all possible attribute names.
 *
 * <p>An example of the usage of this {@code NamespaceHandler} is shown below:
 *
 * <pre class="code">
 * &lt;bean id=&quot;author&quot; class=&quot;..TestBean&quot; c:name=&quot;Enescu&quot; c:work-ref=&quot;compositions&quot;/&gt;
 * </pre>
 * <p>
 * Here the '{@code c:name}' corresponds directly to the '{@code name}
 * ' argument declared on the constructor of class '{@code TestBean}'. The
 * '{@code c:work-ref}' attributes corresponds to the '{@code work}'
 * argument and, rather than being the concrete value, it contains the name of
 * the bean that will be considered as a parameter.
 *
 * <b>Note</b>: This implementation supports only named parameters - there is no
 * support for indexes or types. Further more, the names are used as hints by
 * the container which, by default, does type introspection.
 *
 * @author Costin Leau
 * @see SimplePropertyNamespaceHandler
 * @since 3.1
 */
public class SimpleConstructorNamespaceHandler implements NamespaceHandler {

	private static final String REF_SUFFIX = "-ref";

	private static final String DELIMITER_PREFIX = "_";


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
			//获取本地名称后，去除所有的空格
			String argName = StringUtils.trimWhitespace(parserContext.getDelegate().getLocalName(attr));
			//获取去除掉空格后的值
			String argValue = StringUtils.trimWhitespace(attr.getValue());
			//获取构造参数值对
			ConstructorArgumentValues cvs = definition.getBeanDefinition().getConstructorArgumentValues();
			boolean ref = false;

			// 处理 -ref 参数
			if (argName.endsWith(REF_SUFFIX)) {
				//如果参数名称以 -ref 结尾，将参数名设置为-ref的字符串
				ref = true;
				argName = argName.substring(0, argName.length() - REF_SUFFIX.length());
			}
			//构建值存储器
			ValueHolder valueHolder = new ValueHolder(ref ? new RuntimeBeanReference(argValue) : argValue);
			//设置源
			valueHolder.setSource(parserContext.getReaderContext().extractSource(attr));
			// 处理 "escaped"/"_" 参数
			if (argName.startsWith(DELIMITER_PREFIX)) {
				//如果参数名称以 "_" 开头，将参数名设置为"_"后的字符串
				String arg = argName.substring(1).trim();

				// 快速默认检查
				if (!StringUtils.hasText(arg)) {
					//如果参数名没有值，添加到同样的参数
					cvs.addGenericArgumentValue(valueHolder);
				} else {
					//否则假设它是一个索引
					int index = -1;
					try {
						index = Integer.parseInt(arg);
					} catch (NumberFormatException ex) {
						parserContext.getReaderContext().error(
								"Constructor argument '" + argName + "' specifies an invalid integer", attr);
					}
					if (index < 0) {
						parserContext.getReaderContext().error(
								"Constructor argument '" + argName + "' specifies a negative index", attr);
					}
					if (cvs.hasIndexedArgumentValue(index)) {
						//如果构造参数值对已经含有该索引了，报错
						parserContext.getReaderContext().error(
								"Constructor argument '" + argName + "' with index " + index + " already defined using <constructor-arg>." +
										" Only one approach may be used per argument.", attr);
					}
					//添加到索引参数中
					cvs.addIndexedArgumentValue(index, valueHolder);
				}
			} else {
				// no escaping -> ctr name
				//将参数名转为驼峰模式的参数
				String name = Conventions.attributeNameToPropertyName(argName);
				if (containsArgWithName(name, cvs)) {
					//如果通用参数或者索引参数已经有了这个参数名，报错
					parserContext.getReaderContext().error(
							"Constructor argument '" + argName + "' already defined using <constructor-arg>." +
									" Only one approach may be used per argument.", attr);
				}
				//添加到通用参数中
				valueHolder.setName(Conventions.attributeNameToPropertyName(argName));
				cvs.addGenericArgumentValue(valueHolder);
			}
		}
		return definition;
	}

	private boolean containsArgWithName(String name, ConstructorArgumentValues cvs) {
		return (checkName(name, cvs.getGenericArgumentValues()) ||
				checkName(name, cvs.getIndexedArgumentValues().values()));
	}

	private boolean checkName(String name, Collection<ValueHolder> values) {
		for (ValueHolder holder : values) {
			if (name.equals(holder.getName())) {
				return true;
			}
		}
		return false;
	}

}
