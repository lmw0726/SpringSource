/*
 * Copyright 2002-2011 the original author or authors.
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

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.lang.Nullable;
import org.w3c.dom.Element;

/**
 * Interface used by the {@link DefaultBeanDefinitionDocumentReader} to handle custom,
 * top-level (directly under {@code <beans/>}) tags.
 *
 * <p>Implementations are free to turn the metadata in the custom tag into as many
 * {@link BeanDefinition BeanDefinitions} as required.
 *
 * <p>The parser locates a {@link BeanDefinitionParser} from the associated
 * {@link NamespaceHandler} for the namespace in which the custom tag resides.
 *
 * @author Rob Harrop
 * @see NamespaceHandler
 * @see AbstractBeanDefinitionParser
 * @since 2.0
 */
public interface BeanDefinitionParser {

	/**
	 * 解析指定的 {@link Element}，并使用嵌入在提供的 {@link ParserContext} 中的
	 * {@link org.springframework.beans.factory.xml.ParserContext#getRegistry() BeanDefinitionRegistry} 注册生成的
	 * {@link BeanDefinition BeanDefinition(s)}。
	 * <p> 如果实现将以嵌套方式使用 (例如，作为 {@code <property/>} tag) 标记中的内部标记，则必须返回从解析产生的主要 {@link BeanDefinition}。
	 * 如果实现以嵌套方式使用 <strong> 而不是 <strong>，则它们可能会返回 {@code null}。
	 *
	 * @param element       要解析为一个或多个  {@link BeanDefinition BeanDefinitions} 的元素
	 * @param parserContext 封装解析过程当前状态的对象; 提供对
	 *                      {@link org.springframework.beans.factory.support.BeanDefinitionRegistry} 的访问
	 * @return 主要的 {@link BeanDefinition}
	 */
	@Nullable
	BeanDefinition parse(Element element, ParserContext parserContext);

}
