/*
 * Copyright 2002-2014 the original author or authors.
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
import org.w3c.dom.NamedNodeMap;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.core.Conventions;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * 方便的基类，当 XML 元素的属性名与被配置的 {@link Class} 的属性名
 * 存在一一对应关系时使用。
 *
 * <p>当希望从相对简单的自定义 XML 元素创建单个 Bean 定义时，可继承此解析器类。
 * 创建的 {@code BeanDefinition} 将自动注册到相应的
 * {@link org.springframework.beans.factory.support.BeanDefinitionRegistry}。
 *
 * <p>一个示例可以让此解析器类的用途更加清晰。假设有如下类定义：
 *
 * <pre class="code">public class SimpleCache implements Cache {
 *
 *     public void setName(String name) {...}
 *     public void setTimeout(int timeout) {...}
 *     public void setEvictionPolicy(EvictionPolicy policy) {...}
 *
 *     // 为简洁起见省略类的其他定义...
 * }</pre>
 *
 * <p>然后假设定义了如下 XML 标签，用于方便配置上述类的实例：
 *
 * <pre class="code">&lt;caching:cache name="..." timeout="..." eviction-policy="..."/&gt;</pre>
 *
 * <p>Java 开发者只需做以下工作，将该 XML 标签解析成实际的
 * {@code SimpleCache} Bean 定义：
 *
 * <pre class="code">public class SimpleCacheBeanDefinitionParser extends AbstractSimpleBeanDefinitionParser {
 *
 *     protected Class getBeanClass(Element element) {
 *         return SimpleCache.class;
 *     }
 * }</pre>
 *
 * <p>请注意，{@code AbstractSimpleBeanDefinitionParser} 仅限于通过属性值填充创建的 Bean 定义。
 * 如果希望从提供的 XML 元素解析构造函数参数或嵌套元素，则需要自行实现
 * {@link #postProcess(org.springframework.beans.factory.support.BeanDefinitionBuilder, org.w3c.dom.Element)}
 * 方法进行解析，或者（更常见的做法）直接继承
 * {@link AbstractSingleBeanDefinitionParser} 或 {@link AbstractBeanDefinitionParser} 类。
 *
 * <p>实际将 {@code SimpleCacheBeanDefinitionParser} 注册到 Spring XML 解析
 * 基础设施的过程，请参考 Spring Framework 官方文档（附录部分）。
 *
 * <p>要查看此解析器的示例，请参考
 * {@link org.springframework.beans.factory.xml.UtilNamespaceHandler.PropertiesBeanDefinitionParser} 的源码；
 * 细心（或不太细心）的读者会立即注意到实现中几乎没有代码。
 * {@code PropertiesBeanDefinitionParser} 从如下 XML 元素填充
 * {@link org.springframework.beans.factory.config.PropertiesFactoryBean}：
 *
 * <pre class="code">&lt;util:properties location="jdbc.properties"/&gt;</pre>
 *
 * <p>读者会发现，{@code <util:properties/>} 元素的唯一属性名与
 * {@link org.springframework.beans.factory.config.PropertiesFactoryBean#setLocation(org.springframework.core.io.Resource)}
 * 方法名相匹配（该用法对于任意数量的属性都成立）。
 * {@code PropertiesBeanDefinitionParser} 实际上只需要实现
 * {@link #getBeanClass(org.w3c.dom.Element)} 方法以返回
 * {@code PropertiesFactoryBean} 类型即可。
 *
 * @author Rob Harrop
 * @author Rick Evans
 * @author Juergen Hoeller
 * @since 2.0
 * @see Conventions#attributeNameToPropertyName(String)
 */
public abstract class AbstractSimpleBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

	/**
	 * 解析提供的 {@link Element} 并根据需要填充提供的
	 * {@link BeanDefinitionBuilder}。
	 * <p>此实现将元素上存在的所有属性映射为
	 * {@link org.springframework.beans.PropertyValue} 实例，并通过
	 * {@link BeanDefinitionBuilder#addPropertyValue(String, Object)}
	 * 添加到 {@link org.springframework.beans.factory.config.BeanDefinition} 中。
	 * <p>{@link #extractPropertyName(String)} 方法用于将属性名与 JavaBean
	 * 属性名进行对应。
	 * @param element 被解析的 XML 元素
	 * @param builder 用于定义 {@code BeanDefinition} 的构建器
	 * @see #extractPropertyName(String)
	 */
	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		NamedNodeMap attributes = element.getAttributes();
		for (int x = 0; x < attributes.getLength(); x++) {
			Attr attribute = (Attr) attributes.item(x);
			if (isEligibleAttribute(attribute, parserContext)) {
				String propertyName = extractPropertyName(attribute.getLocalName());
				Assert.state(StringUtils.hasText(propertyName),
						"Illegal property name returned from 'extractPropertyName(String)': cannot be null or empty.");
				builder.addPropertyValue(propertyName, attribute.getValue());
			}
		}
		postProcess(builder, element);
	}

	/**
	 * 判断给定属性是否有资格被转化为相应的 Bean 属性值。
	 * <p>默认实现认为除了 "id" 属性和命名空间声明属性之外的任何属性都是合格的。
	 * @param attribute 要检查的 XML 属性
	 * @param parserContext {@code ParserContext} 上下文
	 * @see #isEligibleAttribute(String)
	 */
	protected boolean isEligibleAttribute(Attr attribute, ParserContext parserContext) {
		String fullName = attribute.getName();
		return (!fullName.equals("xmlns") && !fullName.startsWith("xmlns:") &&
				isEligibleAttribute(parserContext.getDelegate().getLocalName(attribute)));
	}

	/**
	 * 判断给定属性名是否有资格被转化为相应的 Bean 属性值。
	 * <p>默认实现认为除了 "id" 属性之外的任何属性都是合格的。
	 * @param attributeName 直接从被解析的 XML 元素中获取的属性名（永不为 {@code null}）
	 */
	protected boolean isEligibleAttribute(String attributeName) {
		return !ID_ATTRIBUTE.equals(attributeName);
	}

	/**
	 * 从提供的属性名中提取 JavaBean 属性名。
	 * <p>默认实现使用 {@link Conventions#attributeNameToPropertyName(String)}
	 * 方法进行提取。
	 * <p>返回的名称必须遵守标准 JavaBean 属性命名规范。例如，对于具有
	 * setter 方法 '{@code setBingoHallFavourite(String)}' 的类，返回的名称应为
	 * '{@code bingoHallFavourite}'（大小写必须完全一致）。
	 * @param attributeName 直接从被解析的 XML 元素中获取的属性名（永不为 {@code null}）
	 * @return 提取出的 JavaBean 属性名（永不为 {@code null}）
	 */
	protected String extractPropertyName(String attributeName) {
		return Conventions.attributeNameToPropertyName(attributeName);
	}

	/**
	 * 钩子方法，派生类可以实现此方法在解析完成后检查或修改 Bean 定义。
	 * <p>默认实现不执行任何操作。
	 * @param beanDefinition 被解析（并可能已完全定义）的 Bean 定义对象
	 * @param element 作为 Bean 定义元数据来源的 XML 元素
	 */
	protected void postProcess(BeanDefinitionBuilder beanDefinition, Element element) {
	}

}
