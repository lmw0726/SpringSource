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

package org.springframework.beans.factory.xml;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.lang.Nullable;

/**
 * Support class for implementing custom {@link NamespaceHandler NamespaceHandlers}.
 * Parsing and decorating of individual {@link Node Nodes} is done via {@link BeanDefinitionParser}
 * and {@link BeanDefinitionDecorator} strategy interfaces, respectively.
 *
 * <p>Provides the {@link #registerBeanDefinitionParser} and {@link #registerBeanDefinitionDecorator}
 * methods for registering a {@link BeanDefinitionParser} or {@link BeanDefinitionDecorator}
 * to handle a specific element.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @see #registerBeanDefinitionParser(String, BeanDefinitionParser)
 * @see #registerBeanDefinitionDecorator(String, BeanDefinitionDecorator)
 * @since 2.0
 */
public abstract class NamespaceHandlerSupport implements NamespaceHandler {

	/**
	 * key为{@link Element Elements} 的本地名称，value为{@link BeanDefinitionParser} 实现类。
	 */
	private final Map<String, BeanDefinitionParser> parsers = new HashMap<>();

	/**
	 * key为{@link Element Elements} 的本地名称，value为{@link BeanDefinitionDecorator} 实现类，处理{@link Element Elements}
	 */
	private final Map<String, BeanDefinitionDecorator> decorators = new HashMap<>();

	/**
	 * key为{@link Attr Attrs} 的本地名称，value为{@link BeanDefinitionDecorator} 实现类，处理 {@link Attr Attrs}
	 */
	private final Map<String, BeanDefinitionDecorator> attributeDecorators = new HashMap<>();


	/**
	 * Parses the supplied {@link Element} by delegating to the {@link BeanDefinitionParser} that is
	 * registered for that {@link Element}.
	 */
	@Override
	@Nullable
	public BeanDefinition parse(Element element, ParserContext parserContext) {
		//根据解析上下文解析元素，找到bean定义解析器
		BeanDefinitionParser parser = findParserForElement(element, parserContext);
		//如果解析器不为空，则进行解析，否则返回null
		return (parser != null ? parser.parse(element, parserContext) : null);
	}

	/**
	 * Locates the {@link BeanDefinitionParser} from the register implementations using
	 * the local name of the supplied {@link Element}.
	 */
	@Nullable
	private BeanDefinitionParser findParserForElement(Element element, ParserContext parserContext) {
		//获取元素的本地名称
		String localName = parserContext.getDelegate().getLocalName(element);
		BeanDefinitionParser parser = this.parsers.get(localName);
		if (parser == null) {
			parserContext.getReaderContext().fatal(
					"Cannot locate BeanDefinitionParser for element [" + localName + "]", element);
		}
		return parser;
	}

	/**
	 * 通过委派给注册为处理该 {@link Node} 的 {@link BeanDefinitionDecorator} 来修饰提供的 {@link Node}。
	 */
	@Override
	@Nullable
	public BeanDefinitionHolder decorate(
			Node node, BeanDefinitionHolder definition, ParserContext parserContext) {
		//根据解析上下文，找到节点的bean定义装饰器
		BeanDefinitionDecorator decorator = findDecoratorForNode(node, parserContext);
		//如果装饰器不为空，则进行装饰，否则返回null
		return (decorator != null ? decorator.decorate(node, definition, parserContext) : null);
	}

	/**
	 * 使用提供的 {@link Node} 的本地名称从注册器实现中找到 {@link BeanDefinitionParser}。
	 * 同时支持 {@link Element Elements} 和 {@link Attr Attrs}。
	 */
	@Nullable
	private BeanDefinitionDecorator findDecoratorForNode(Node node, ParserContext parserContext) {
		BeanDefinitionDecorator decorator = null;
		//获取节点的本地名称
		String localName = parserContext.getDelegate().getLocalName(node);
		if (node instanceof Element) {
			//如果节点为Element类型，则使用decorators获取bean定义装饰器
			decorator = this.decorators.get(localName);
		} else if (node instanceof Attr) {
			//如果是节点为 Attr类型，则使用attributeDecorators获取bean定义装饰器
			decorator = this.attributeDecorators.get(localName);
		} else {
			//提示错误
			parserContext.getReaderContext().fatal(
					"Cannot decorate based on Nodes of type [" + node.getClass().getName() + "]", node);
		}
		if (decorator == null) {
			//装饰器为空，提示错误
			parserContext.getReaderContext().fatal("Cannot locate BeanDefinitionDecorator for " +
					(node instanceof Element ? "element" : "attribute") + " [" + localName + "]", node);
		}
		return decorator;
	}


	/**
	 * 子类可以调用它来注册提供的 {@link BeanDefinitionParser} 来处理指定的元素。元素名称是本地 (非命名空间限定) 名称。
	 */
	protected final void registerBeanDefinitionParser(String elementName, BeanDefinitionParser parser) {
		this.parsers.put(elementName, parser);
	}

	/**
	 * Subclasses can call this to register the supplied {@link BeanDefinitionDecorator} to
	 * handle the specified element. The element name is the local (non-namespace qualified)
	 * name.
	 */
	protected final void registerBeanDefinitionDecorator(String elementName, BeanDefinitionDecorator dec) {
		this.decorators.put(elementName, dec);
	}

	/**
	 * Subclasses can call this to register the supplied {@link BeanDefinitionDecorator} to
	 * handle the specified attribute. The attribute name is the local (non-namespace qualified)
	 * name.
	 */
	protected final void registerBeanDefinitionDecoratorForAttribute(String attrName, BeanDefinitionDecorator dec) {
		this.attributeDecorators.put(attrName, dec);
	}

}
