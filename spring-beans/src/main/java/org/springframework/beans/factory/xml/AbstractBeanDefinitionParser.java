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

import org.w3c.dom.Element;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

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
	 * Constant for the "id" attribute.
	 */
	public static final String ID_ATTRIBUTE = "id";

	/**
	 * Constant for the "name" attribute.
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
	 * Resolve the ID for the supplied {@link BeanDefinition}.
	 * <p>When using {@link #shouldGenerateId generation}, a name is generated automatically.
	 * Otherwise, the ID is extracted from the "id" attribute, potentially with a
	 * {@link #shouldGenerateIdAsFallback() fallback} to a generated id.
	 *
	 * @param element       the element that the bean definition has been built from
	 * @param definition    the bean definition to be registered
	 * @param parserContext the object encapsulating the current state of the parsing process;
	 *                      provides access to a {@link org.springframework.beans.factory.support.BeanDefinitionRegistry}
	 * @return the resolved id
	 * @throws BeanDefinitionStoreException if no unique name could be generated
	 *                                      for the given bean definition
	 */
	protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext)
			throws BeanDefinitionStoreException {

		if (shouldGenerateId()) {
			return parserContext.getReaderContext().generateBeanName(definition);
		} else {
			String id = element.getAttribute(ID_ATTRIBUTE);
			if (!StringUtils.hasText(id) && shouldGenerateIdAsFallback()) {
				id = parserContext.getReaderContext().generateBeanName(definition);
			}
			return id;
		}
	}

	/**
	 * Register the supplied {@link BeanDefinitionHolder bean} with the supplied
	 * {@link BeanDefinitionRegistry registry}.
	 * <p>Subclasses can override this method to control whether or not the supplied
	 * {@link BeanDefinitionHolder bean} is actually even registered, or to
	 * register even more beans.
	 * <p>The default implementation registers the supplied {@link BeanDefinitionHolder bean}
	 * with the supplied {@link BeanDefinitionRegistry registry} only if the {@code isNested}
	 * parameter is {@code false}, because one typically does not want inner beans
	 * to be registered as top level beans.
	 *
	 * @param definition the bean definition to be registered
	 * @param registry   the registry that the bean is to be registered with
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
	 * Should an ID be generated instead of read from the passed in {@link Element}?
	 * <p>Disabled by default; subclasses can override this to enable ID generation.
	 * Note that this flag is about <i>always</i> generating an ID; the parser
	 * won't even check for an "id" attribute in this case.
	 *
	 * @return whether the parser should always generate an id
	 */
	protected boolean shouldGenerateId() {
		return false;
	}

	/**
	 * Should an ID be generated instead if the passed in {@link Element} does not
	 * specify an "id" attribute explicitly?
	 * <p>Disabled by default; subclasses can override this to enable ID generation
	 * as fallback: The parser will first check for an "id" attribute in this case,
	 * only falling back to a generated ID if no value was specified.
	 *
	 * @return whether the parser should generate an id if no id was specified
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
	 * Hook method called after the primary parsing of a
	 * {@link BeanComponentDefinition} but before the
	 * {@link BeanComponentDefinition} has been registered with a
	 * {@link org.springframework.beans.factory.support.BeanDefinitionRegistry}.
	 * <p>Derived classes can override this method to supply any custom logic that
	 * is to be executed after all the parsing is finished.
	 * <p>The default implementation is a no-op.
	 *
	 * @param componentDefinition the {@link BeanComponentDefinition} that is to be processed
	 */
	protected void postProcessComponentDefinition(BeanComponentDefinition componentDefinition) {
	}

}
