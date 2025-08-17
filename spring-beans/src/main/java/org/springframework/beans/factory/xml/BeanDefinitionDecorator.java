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

import org.w3c.dom.Node;

import org.springframework.beans.factory.config.BeanDefinitionHolder;

/**
 * {@link DefaultBeanDefinitionDocumentReader} 用于处理自定义的、嵌套的（直接在 {@code <bean>} 下）标签的接口。
 *
 * <p>也可以根据应用于 {@code <bean>} 标签的自定义属性进行装饰。
 * 实现类可以自由地将自定义标签中的元数据转换为任意数量的
 * {@link org.springframework.beans.factory.config.BeanDefinition BeanDefinitions}，
 * 并可以转换包含该自定义标签的 {@code <bean>} 标签的
 * {@link org.springframework.beans.factory.config.BeanDefinition}，
 * 甚至可能返回一个完全不同的 {@link org.springframework.beans.factory.config.BeanDefinition} 来替换原有定义。
 *
 * <p>{@link BeanDefinitionDecorator BeanDefinitionDecorators} 应注意，它们可能是链的一部分。
 * 特别是，一个 {@link BeanDefinitionDecorator} 应意识到先前的 {@link BeanDefinitionDecorator}
 * 可能已用 {@link org.springframework.aop.framework.ProxyFactoryBean} 定义替换了原始的
 * {@link org.springframework.beans.factory.config.BeanDefinition}，从而允许添加自定义
 * {@link org.aopalliance.intercept.MethodInterceptor 拦截器}。
 *
 * <p>希望为封闭 Bean 添加拦截器的 {@link BeanDefinitionDecorator BeanDefinitionDecorators}
 * 应扩展 {@link org.springframework.aop.config.AbstractInterceptorDrivenBeanDefinitionDecorator}，
 * 它处理链的管理，确保只创建一个代理，并包含链中的所有拦截器。
 *
 * <p>解析器会从自定义标签所在命名空间的 {@link NamespaceHandler} 中定位相应的
 * {@link BeanDefinitionDecorator}。
 *
 * @author Rob Harrop
 * @since 2.0
 * @see NamespaceHandler
 * @see BeanDefinitionParser
 */
public interface BeanDefinitionDecorator {

	/**
	 * 解析指定的 {@link Node}（元素或属性），并装饰提供的
	 * {@link org.springframework.beans.factory.config.BeanDefinition}，
	 * 返回装饰后的定义。
	 *
	 * <p>实现类可以选择返回一个全新的定义，这将替换结果
	 * {@link org.springframework.beans.factory.BeanFactory} 中的原始定义。
	 *
	 * <p>提供的 {@link ParserContext} 可用于注册支持主定义所需的其他 Bean。
	 */
	BeanDefinitionHolder decorate(Node node, BeanDefinitionHolder definition, ParserContext parserContext);

}
