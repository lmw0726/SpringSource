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

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * Abstract {@link BeanDefinitionParser} implementation providing
 * a number of convenience methods and a
 * {@link AbstractBeanDefinitionParser#parseInternal template method}
 * that subclasses must override to provide the actual parsing logic.
 *
 * <p>Use this {@link BeanDefinitionParser} implementation when you want
 * to parse some arbitrarily complex XML into one or more
 * {@link BeanDefinition BeanDefinitions}. If you just want to parse some
 * XML into a single {@code BeanDefinition}, you may wish to consider
 * the simpler convenience extensions of this class, namely
 * {@link AbstractSingleBeanDefinitionParser} and
 * {@link AbstractSimpleBeanDefinitionParser}.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Rick Evans
 * @author Dave Syer
 * @since 2.0
 */
public abstract class AbstractBeanDefinitionParser implements BeanDefinitionParser {

	/**
	 * “id” 属性的常量。
	 */
	public static final String ID_ATTRIBUTE = "id";

	/**
	 * “名称” 属性的常量。
	 */
	public static final String NAME_ATTRIBUTE = "name";


	@Override
	@Nullable
	public final BeanDefinition parse(Element element, ParserContext parserContext) {
		//将内部元素解析成抽象bean定义
		AbstractBeanDefinition definition = parseInternal(element, parserContext);
		if (definition == null || parserContext.isNested()) {
			//如果抽象bean定义为空，或者该bean是嵌套bean，则返回当前的抽象bean定义
			return definition;
		}
		try {
			//解析id属性
			String id = resolveId(element, definition, parserContext);
			if (!StringUtils.hasText(id)) {
				//如果id为空，则提示报错
				parserContext.getReaderContext().error(
						"Id is required for element '" + parserContext.getDelegate().getLocalName(element)
								+ "' when used as a top-level tag", element);
			}
			String[] aliases = null;
			if (shouldParseNameAsAliases()) {
				//如果需要解析名称为别名，获取name属性值。
				String name = element.getAttribute(NAME_ATTRIBUTE);
				if (StringUtils.hasLength(name)) {
					//按照,分割name属性值，转换成字符串数组，并去除所有空格。
					aliases = StringUtils.trimArrayElements(StringUtils.commaDelimitedListToStringArray(name));
				}
			}
			//根据id，别名列表，bean定义构建bean定义持有者
			BeanDefinitionHolder holder = new BeanDefinitionHolder(definition, id, aliases);
			//注册bean定义
			registerBeanDefinition(holder, parserContext.getRegistry());
			if (shouldFireEvents()) {
				//如果需要触发事件，构建bean组件定义
				BeanComponentDefinition componentDefinition = new BeanComponentDefinition(holder);
				//后置处理组件定义
				postProcessComponentDefinition(componentDefinition);
				//注册bean组件定义
				parserContext.registerComponent(componentDefinition);
			}
		} catch (BeanDefinitionStoreException ex) {
			//如果解析失败，抛出异常，返回Null
			String msg = ex.getMessage();
			parserContext.getReaderContext().error((msg != null ? msg : ex.toString()), element);
			return null;
		}
		return definition;
	}

	/**
	 * 解析提供的{@link BeanDefinition}的ID。
	 * <p> 使用 {@link #shouldGenerateId } 生成时，名称会自动生成。
	 * 否则，将从 “ID” 属性中提取id，并可能回退到使用 {@link #shouldGenerateIdAsFallback()} 生成id。
	 *
	 * @param element       bean定义已从中构建的元素
	 * @param definition    要注册的bean定义
	 * @param parserContext 封装解析进程当前状态的对象; 提供对 {@link org.springframework.beans.factory.support.BeanDefinitionRegistry}的访问
	 * @return 已解析的id
	 * @throws BeanDefinitionStoreException 如果无法为给定的bean定义生成唯一名称
	 */
	protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext)
			throws BeanDefinitionStoreException {

		if (shouldGenerateId()) {
			//如果需要生成id，根据bean定义生成bean名称。
			return parserContext.getReaderContext().generateBeanName(definition);
		} else {
			//如果不需要生成id，将从ID属性中提取id
			String id = element.getAttribute(ID_ATTRIBUTE);
			if (StringUtils.hasText(id) || !shouldGenerateIdAsFallback()) {
				//如果id值不为空，或者不回退到生成id，返回该id值
				return id;
			}
			//根据bean定义生成名称
			return parserContext.getReaderContext().generateBeanName(definition);

		}
	}

	/**
	 * 使用提供的 {@link BeanDefinitionRegistry 注册表} 注册提供的 {@link BeanDefinitionHolder bean}。
	 * <p> 子类可以覆盖此方法，以控制是否实际注册了所提供的 {@link BeanDefinitionHolder bean}，或者注册了更多的bean。
	 * <p> 仅当 {@code isNested} 参数为 {@code false} 时，默认实现才会将提供的 {@link BeanDefinitionHolder bean}
	 * 与提供的 {@link BeanDefinitionRegistry registry} 一起注册，因为通常不希望内部bean被注册为顶级bean。
	 *
	 * @param definition 要注册的bean定义
	 * @param registry   bean要注册的注册表
	 * @see BeanDefinitionReaderUtils#registerBeanDefinition(BeanDefinitionHolder, BeanDefinitionRegistry)
	 */
	protected void registerBeanDefinition(BeanDefinitionHolder definition, BeanDefinitionRegistry registry) {
		BeanDefinitionReaderUtils.registerBeanDefinition(definition, registry);
	}


	/**
	 * 中央模板方法，用于将提供的 {@link Element} 实际解析为一个或多个 {@link BeanDefinition BeanDefinitions}。
	 *
	 * @param element       要解析为一个或多个 {@link BeanDefinition BeanDefinitions}的元素
	 * @param parserContext 封装解析过程当前状态的对象; 提供对
	 *                      {@link org.springframework.beans.factory.support.BeanDefinitionRegistry} 的访问
	 * @return 由提供的 {@link Element} 的解析产生的主要 {@link BeanDefinition}
	 * @see #parse(org.w3c.dom.Element, ParserContext)
	 * @see #postProcessComponentDefinition(org.springframework.beans.factory.parsing.BeanComponentDefinition)
	 */
	@Nullable
	protected abstract AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext);

	/**
	 * 是否应该生成一个ID，而不是从 {@link Element} 中传递的读取？
	 * <p> 默认情况下禁用; 子类可以覆盖此功能以启用ID生成。
	 * 请注意，此标志是关于 <i> 始终 <i> 生成ID;
	 * 在这种情况下，解析器甚至不会检查 “id” 属性。
	 *
	 * @return 解析器是否应始终生成id
	 */
	protected boolean shouldGenerateId() {
		return false;
	}

	/**
	 * 如果 {@link Element} 中传递的没有显式指定 “ID” 属性，是否应该生成id？
	 * <p>默认情况下禁用; 子类可以覆盖此功能以启用ID生成作为后备:
	 * 在这种情况下，解析器将首先检查 “id” 属性，仅在未指定值的情况下才会回退到生成的ID。
	 *
	 * @return 如果未指定id，解析器是否应生成id
	 */
	protected boolean shouldGenerateIdAsFallback() {
		return false;
	}

	/**
	 * 确定是否应将元素的 “name” 属性解析为bean定义别名，即替代bean定义名称。
	 * <p> 默认实现返回 {@code true}。
	 *
	 * @return 解析器是否应将 “name” 属性评估为别名
	 * @since 4.1.5
	 */
	protected boolean shouldParseNameAsAliases() {
		return true;
	}

	/**
	 * 确定此解析器是否应该在解析bean定义后触发
	 * {@link org.springframework.beans.factory.parsing.BeanComponentDefinition} 事件。
	 * <p> 此实现默认情况下返回 {@code true};
	 * 也就是说，当bean定义完全解析后，将触发事件。重写此以返回 {@code false} 以抑制事件。
	 *
	 * @return {@code true}，以便在解析bean定义后触发组件注册事件; {@code false} 抑制该事件
	 * @see #postProcessComponentDefinition
	 * @see org.springframework.beans.factory.parsing.ReaderContext#fireComponentRegistered
	 */
	protected boolean shouldFireEvents() {
		return true;
	}

	/**
	 * 在 {@link BeanComponentDefinition} 的主要解析之后
	 * 但在 {@link BeanComponentDefinition} 已通过
	 * {@link org.springframework.beans.factory.support.BeanDefinitionRegistry} 注册之前调用的钩子方法。
	 * <p> 派生类可以覆盖此方法，以提供所有解析完成后要执行的任何自定义逻辑。
	 * <p> 默认实现是无操作。
	 *
	 * @param componentDefinition 要处理的 {@link BeanComponentDefinition}
	 */
	protected void postProcessComponentDefinition(BeanComponentDefinition componentDefinition) {
	}

}
