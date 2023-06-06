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

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.lang.Nullable;

/**
 * Base interface used by the {@link DefaultBeanDefinitionDocumentReader}
 * for handling custom namespaces in a Spring XML configuration file.
 *
 * <p>Implementations are expected to return implementations of the
 * {@link BeanDefinitionParser} interface for custom top-level tags and
 * implementations of the {@link BeanDefinitionDecorator} interface for
 * custom nested tags.
 *
 * <p>The parser will call {@link #parse} when it encounters a custom tag
 * directly under the {@code <beans>} tags and {@link #decorate} when
 * it encounters a custom tag directly under a {@code <bean>} tag.
 *
 * <p>Developers writing their own custom element extensions typically will
 * not implement this interface directly, but rather make use of the provided
 * {@link NamespaceHandlerSupport} class.
 *
 * @author Rob Harrop
 * @author Erik Wiersma
 * @see DefaultBeanDefinitionDocumentReader
 * @see NamespaceHandlerResolver
 * @since 2.0
 */
public interface NamespaceHandler {

	/**
	 * 在构造之后但在解析任何自定义元素之前，由 {@link DefaultBeanDefinitionDocumentReader} 调用。
	 *
	 * @see NamespaceHandlerSupport#registerBeanDefinitionParser(String, BeanDefinitionParser)
	 */
	void init();

	/**
	 * 解析指定的 {@link Element} ，并使用嵌入在 {@link ParserContext} 中的
	 * {@link org.springframework.beans.factory.support.BeanDefinitionRegistry} 注册任意结果值。
	 * <p> 实现类应该返回主要的{@code BeanDefinition}，如果实现希望嵌套在 (例如) {@code <property>} 标记中使用，则应返回从解析阶段产生的{@code BeanDefinition}。
	 * <p> 如果没有被用在嵌套的场景中，实现类可能会返回 {@code null}
	 *
	 * @param element       要解析为一个或多个 {@code BeanDefinitions} 的元素
	 * @param parserContext 封装解析过程当前状态的对象
	 * @return 主要的{@code BeanDefinition} (可以是 {@code null}，如上所述)
	 */
	@Nullable
	BeanDefinition parse(Element element, ParserContext parserContext);

	/**
	 * 解析指定的 {@link Node} 并装饰提供的 {@link BeanDefinitionHolder}，返回装饰的定义。
	 * <p> {@link Node} 可以是 {@link org.w3c.dom.Attr} 或 {@link Element}，具体取决于是否正在解析自定义属性或元素。
	 *
	 * <p> 实现类可能会选择返回一个全新的定义，该定义将替换生成的 {@link org.springframework.beans.factory.BeanFactory} 中的原始定义。
	 * <p> 提供的 {@link ParserContext} 可用于注册支持主定义所需的任何其他bean。
	 *
	 * @param source        要解析的源元素或属性
	 * @param definition    当前bean定义
	 * @param parserContext 封装解析过程当前状态的对象
	 * @return 装饰的定义 (将在BeanFactory中注册)，或者如果不需要装饰，则仅使用原始bean定义。
	 * {@code null} 值严格来说是无效的，但将被宽大处理，就像返回原始bean定义的情况一样。
	 */
	@Nullable
	BeanDefinitionHolder decorate(Node source, BeanDefinitionHolder definition, ParserContext parserContext);

}
