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

package org.springframework.scripting.config;

import java.util.List;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionDefaults;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.beans.factory.xml.XmlReaderContext;
import org.springframework.lang.Nullable;
import org.springframework.scripting.support.ScriptFactoryPostProcessor;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;

/**
 * {@code <lang:groovy/>}、{@code <lang:std/>} 和 {@code <lang:bsh/>} 标签的
 * BeanDefinitionParser 实现。允许使用动态语言编写的对象能够方便地通过
 * {@link org.springframework.beans.factory.BeanFactory} 进行暴露。
 *
 * <p>每个对象的脚本可以通过引用包含脚本的资源来指定（使用 {@code script-source}
 * 属性），也可以直接内联在 XML 配置中（使用 {@code inline-script} 属性）。
 *
 * <p>默认情况下，使用这些标签创建的动态对象是<strong>不可刷新</strong>的。要启用刷新功能，
 * 请使用 {@code refresh-check-delay} 属性为每个对象指定刷新检查延迟时间（以毫秒为单位）。
 *
 * @author Rob Harrop
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @since 2.0
 */
class ScriptBeanDefinitionParser extends AbstractBeanDefinitionParser {

	private static final String ENGINE_ATTRIBUTE = "engine";

	private static final String SCRIPT_SOURCE_ATTRIBUTE = "script-source";

	private static final String INLINE_SCRIPT_ELEMENT = "inline-script";

	private static final String SCOPE_ATTRIBUTE = "scope";

	private static final String AUTOWIRE_ATTRIBUTE = "autowire";

	private static final String DEPENDS_ON_ATTRIBUTE = "depends-on";

	private static final String INIT_METHOD_ATTRIBUTE = "init-method";

	private static final String DESTROY_METHOD_ATTRIBUTE = "destroy-method";

	private static final String SCRIPT_INTERFACES_ATTRIBUTE = "script-interfaces";

	private static final String REFRESH_CHECK_DELAY_ATTRIBUTE = "refresh-check-delay";

	private static final String PROXY_TARGET_CLASS_ATTRIBUTE = "proxy-target-class";

	private static final String CUSTOMIZER_REF_ATTRIBUTE = "customizer-ref";


	/**
	 * 此解析器实例将为其创建 BeanDefinition 的
	 * {@link org.springframework.scripting.ScriptFactory} 类。
	 */
	private final String scriptFactoryClassName;


	/**
	 * 创建此解析器的新实例，为指定的 {@link org.springframework.scripting.ScriptFactory}
	 * 类创建 BeanDefinition。
	 * @param scriptFactoryClassName 要操作的 ScriptFactory 类
	 */
	public ScriptBeanDefinitionParser(String scriptFactoryClassName) {
		this.scriptFactoryClassName = scriptFactoryClassName;
	}


	/**
	 * 解析动态对象元素并返回生成的 BeanDefinition。
	 * 如果需要，注册 {@link ScriptFactoryPostProcessor}。
	 */
	@Override
	@SuppressWarnings("deprecation")
	@Nullable
	protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
		// Engine 属性仅对 <lang:std> 支持
		String engine = element.getAttribute(ENGINE_ATTRIBUTE);

		// 解析脚本源。
		String value = resolveScriptSource(element, parserContext.getReaderContext());
		if (value == null) {
			return null;
		}

		// 设置基础设施。
		LangNamespaceUtils.registerScriptFactoryPostProcessorIfNecessary(parserContext.getRegistry());

		// 创建脚本工厂 BeanDefinition。
		GenericBeanDefinition bd = new GenericBeanDefinition();
		bd.setBeanClassName(this.scriptFactoryClassName);
		bd.setSource(parserContext.extractSource(element));
		bd.setAttribute(ScriptFactoryPostProcessor.LANGUAGE_ATTRIBUTE, element.getLocalName());

		// 确定 Bean 作用域。
		String scope = element.getAttribute(SCOPE_ATTRIBUTE);
		if (StringUtils.hasLength(scope)) {
			bd.setScope(scope);
		}

		// 确定自动装配模式。
		String autowire = element.getAttribute(AUTOWIRE_ATTRIBUTE);
		int autowireMode = parserContext.getDelegate().getAutowireMode(autowire);
		// 仅支持 "byType" 和 "byName"，但可能有其他继承的默认值...
		if (autowireMode == AbstractBeanDefinition.AUTOWIRE_AUTODETECT) {
			autowireMode = AbstractBeanDefinition.AUTOWIRE_BY_TYPE;
		}
		else if (autowireMode == AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR) {
			autowireMode = AbstractBeanDefinition.AUTOWIRE_NO;
		}
		bd.setAutowireMode(autowireMode);

		// 解析 depends-on 的 Bean 名称列表。
		String dependsOn = element.getAttribute(DEPENDS_ON_ATTRIBUTE);
		if (StringUtils.hasLength(dependsOn)) {
			bd.setDependsOn(StringUtils.tokenizeToStringArray(
					dependsOn, BeanDefinitionParserDelegate.MULTI_VALUE_ATTRIBUTE_DELIMITERS));
		}

		// 获取此解析器上下文中 BeanDefinition 的默认值
		BeanDefinitionDefaults beanDefinitionDefaults = parserContext.getDelegate().getBeanDefinitionDefaults();

		// 确定初始化方法和销毁方法。
		String initMethod = element.getAttribute(INIT_METHOD_ATTRIBUTE);
		if (StringUtils.hasLength(initMethod)) {
			bd.setInitMethodName(initMethod);
		}
		else if (beanDefinitionDefaults.getInitMethodName() != null) {
			bd.setInitMethodName(beanDefinitionDefaults.getInitMethodName());
		}

		if (element.hasAttribute(DESTROY_METHOD_ATTRIBUTE)) {
			String destroyMethod = element.getAttribute(DESTROY_METHOD_ATTRIBUTE);
			bd.setDestroyMethodName(destroyMethod);
		}
		else if (beanDefinitionDefaults.getDestroyMethodName() != null) {
			bd.setDestroyMethodName(beanDefinitionDefaults.getDestroyMethodName());
		}

		// 附加任何刷新元数据。
		String refreshCheckDelay = element.getAttribute(REFRESH_CHECK_DELAY_ATTRIBUTE);
		if (StringUtils.hasText(refreshCheckDelay)) {
			bd.setAttribute(ScriptFactoryPostProcessor.REFRESH_CHECK_DELAY_ATTRIBUTE, Long.valueOf(refreshCheckDelay));
		}

		// 附加任何代理目标类元数据。
		String proxyTargetClass = element.getAttribute(PROXY_TARGET_CLASS_ATTRIBUTE);
		if (StringUtils.hasText(proxyTargetClass)) {
			bd.setAttribute(ScriptFactoryPostProcessor.PROXY_TARGET_CLASS_ATTRIBUTE, Boolean.valueOf(proxyTargetClass));
		}

		// 添加构造函数参数。
		ConstructorArgumentValues cav = bd.getConstructorArgumentValues();
		int constructorArgNum = 0;
		if (StringUtils.hasLength(engine)) {
			cav.addIndexedArgumentValue(constructorArgNum++, engine);
		}
		cav.addIndexedArgumentValue(constructorArgNum++, value);
		if (element.hasAttribute(SCRIPT_INTERFACES_ATTRIBUTE)) {
			cav.addIndexedArgumentValue(
					constructorArgNum++, element.getAttribute(SCRIPT_INTERFACES_ATTRIBUTE), "java.lang.Class[]");
		}

		// 这用于 Groovy。它是一个指向 customizer Bean 的 Bean 引用。
		if (element.hasAttribute(CUSTOMIZER_REF_ATTRIBUTE)) {
			String customizerBeanName = element.getAttribute(CUSTOMIZER_REF_ATTRIBUTE);
			if (!StringUtils.hasText(customizerBeanName)) {
				parserContext.getReaderContext().error("Attribute 'customizer-ref' has empty value", element);
			}
			else {
				cav.addIndexedArgumentValue(constructorArgNum++, new RuntimeBeanReference(customizerBeanName));
			}
		}

		// 添加需要添加的任何属性定义。
		parserContext.getDelegate().parsePropertyElements(element, bd);

		return bd;
	}

	/**
	 * 从 {@code script-source} 属性或 {@code inline-script} 元素解析脚本源。
	 * 如果两者都指定了或者都没有指定，则记录日志并调用 {@link XmlReaderContext#error}
	 * 然后返回 {@code null}。
	 */
	@Nullable
	private String resolveScriptSource(Element element, XmlReaderContext readerContext) {
		boolean hasScriptSource = element.hasAttribute(SCRIPT_SOURCE_ATTRIBUTE);
		List<Element> elements = DomUtils.getChildElementsByTagName(element, INLINE_SCRIPT_ELEMENT);
		if (hasScriptSource && !elements.isEmpty()) {
			readerContext.error("Only one of 'script-source' and 'inline-script' should be specified.", element);
			return null;
		}
		else if (hasScriptSource) {
			return element.getAttribute(SCRIPT_SOURCE_ATTRIBUTE);
		}
		else if (!elements.isEmpty()) {
			Element inlineElement = elements.get(0);
			return "inline:" + DomUtils.getTextValue(inlineElement);
		}
		else {
			readerContext.error("Must specify either 'script-source' or 'inline-script'.", element);
			return null;
		}
	}

	/**
	 * 脚本化的 Bean 也可以是匿名的。
	 */
	@Override
	protected boolean shouldGenerateIdAsFallback() {
		return true;
	}

}
