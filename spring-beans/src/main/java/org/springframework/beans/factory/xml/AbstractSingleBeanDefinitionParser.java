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

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.lang.Nullable;

/**
 * Base class for those {@link BeanDefinitionParser} implementations that
 * need to parse and define just a <i>single</i> {@code BeanDefinition}.
 *
 * <p>Extend this parser class when you want to create a single bean definition
 * from an arbitrarily complex XML element. You may wish to consider extending
 * the {@link AbstractSimpleBeanDefinitionParser} when you want to create a
 * single bean definition from a relatively simple custom XML element.
 *
 * <p>The resulting {@code BeanDefinition} will be automatically registered
 * with the {@link org.springframework.beans.factory.support.BeanDefinitionRegistry}.
 * Your job simply is to {@link #doParse parse} the custom XML {@link Element}
 * into a single {@code BeanDefinition}.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Rick Evans
 * @see #getBeanClass
 * @see #getBeanClassName
 * @see #doParse
 * @since 2.0
 */
public abstract class AbstractSingleBeanDefinitionParser extends AbstractBeanDefinitionParser {

	/**
	 * 为 {@link #getBeanClass bean类} 创建一个 {@link BeanDefinitionBuilder} 实例，
	 * 并将其传递给 {@link #doParse} 策略方法。
	 *
	 * @param element       要解析为单个BeanDefinition的元素
	 * @param parserContext 封装解析过程当前状态的对象
	 * @return 由提供的 {@link Element} 的解析产生的BeanDefinition
	 * @throws IllegalStateException 如果从 {@link #getBeanClass(org.w3c.dom.Element)}
	 *                               返回的bean {@link Class} 为 {@code null}
	 * @see #doParse
	 */
	@Override
	protected final AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
		//创建BeanDefinitionBuilder对象
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition();
		//获取父元素名称
		String parentName = getParentName(element);
		if (parentName != null) {
			//如果父元素名称，在原始的Bean定义中设置父元素名称
			builder.getRawBeanDefinition().setParentName(parentName);
		}
		//获取bean类
		Class<?> beanClass = getBeanClass(element);
		if (beanClass != null) {
			//如果bean类不为空，在原始的Bean定义中设置bean类
			builder.getRawBeanDefinition().setBeanClass(beanClass);
		} else {
			//bean类为null，意味着子类并没有重写getBeanClass()方法，则尝试去判断是否重写了getBeanClassName()
			String beanClassName = getBeanClassName(element);
			if (beanClassName != null) {
				//如果bean类名称不为空，在原始的Bean定义中设置bean类名称
				builder.getRawBeanDefinition().setBeanClassName(beanClassName);
			}
		}
		//bean定义设置源
		builder.getRawBeanDefinition().setSource(parserContext.extractSource(element));
		//获取包含的bean定义
		BeanDefinition containingBd = parserContext.getContainingBeanDefinition();
		if (containingBd != null) {
			// 如果包含的bean定义不为空，设置与包含bean定义相同的scope属性。
			// 内部bean定义必须接收与包含bean相同的作用域。
			builder.setScope(containingBd.getScope());
		}
		if (parserContext.isDefaultLazyInit()) {
			// 如果默认是懒加载的，将lazyInit设置为true
			// Default-lazy-init也适用于自定义bean定义。
			builder.setLazyInit(true);
		}
		//调用子类的doParse()进行解析
		doParse(element, parserContext, builder);
		//返回解析后的BeanDefinition
		return builder.getBeanDefinition();
	}

	/**
	 * 如果当前bean被定义为子bean，则确定当前解析的bean的父级名称。
	 * <p> 默认实现返回 {@code null}，表示根bean定义。
	 *
	 * @param element 正在解析的 {@code Element}
	 * @return 当前解析的bean的父bean名称，如果没有，则为 {@code null}
	 */
	@Nullable
	protected String getParentName(Element element) {
		return null;
	}

	/**
	 * 确定与提供的 {@link Element} 相对应的bean类。
	 * <p> 请注意，对于应用程序类，通常最好重写 {@link #getBeanClassName}，
	 * 以避免直接依赖bean实现类。
	 * BeanDefinitionParser及其命名空间处理器可以在IDE插件中使用，即使插件的类路径上没有应用程序类。
	 *
	 * @param element 正在解析的 {@code Element}
	 * @return 通过解析提供的 {@code Element} 定义的bean的 {@link Class}，如果没有，则为 {@code null}
	 * @see #getBeanClassName
	 */
	@Nullable
	protected Class<?> getBeanClass(Element element) {
		return null;
	}

	/**
	 * 确定与提供的 {@link Element} 相对应的bean类名称。
	 *
	 * @param element 正在解析的 {@code Element}
	 * @return 通过解析提供的 {@code Element} 定义的bean的类名，如果没有，则为 {@code null}
	 * @see #getBeanClass
	 */
	@Nullable
	protected String getBeanClassName(Element element) {
		return null;
	}

	/**
	 * 解析提供的 {@link Element} ，并根据需要填充提供的 {@link BeanDefinitionBuilder}。
	 * <p> 默认实现将委派给不带ParserContext参数的 {@code doParse} 版本。
	 *
	 * @param element       正在解析的XML元素
	 * @param parserContext 封装解析过程当前状态的对象
	 * @param builder       用于定义的 {@code BeanDefinition}
	 * @see #doParse(Element, BeanDefinitionBuilder)
	 */
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		doParse(element, builder);
	}

	/**
	 * 解析提供的 {@link Element}，并根据需要填充提供的 {@link BeanDefinitionBuilder}。
	 * <p>默认实现不执行任何操作。
	 *
	 * @param element 正在解析的XML元素
	 * @param builder 用于定义的 {@code BeanDefinition}
	 */
	protected void doParse(Element element, BeanDefinitionBuilder builder) {
	}

}
