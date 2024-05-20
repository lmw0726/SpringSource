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

package org.springframework.web.servlet.config;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.parsing.CompositeComponentDefinition;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.core.Ordered;
import org.springframework.util.xml.DomUtils;
import org.springframework.web.servlet.view.BeanNameViewResolver;
import org.springframework.web.servlet.view.ContentNegotiatingViewResolver;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.ViewResolverComposite;
import org.springframework.web.servlet.view.freemarker.FreeMarkerViewResolver;
import org.springframework.web.servlet.view.groovy.GroovyMarkupViewResolver;
import org.springframework.web.servlet.view.script.ScriptTemplateViewResolver;
import org.springframework.web.servlet.view.tiles3.TilesViewResolver;
import org.w3c.dom.Element;

import java.util.List;

/**
 * 解析 {@code view-resolvers} MVC 命名空间元素并注册
 * {@link org.springframework.web.servlet.ViewResolver} bean 定义。
 *
 * <p>所有注册的解析器都被包装在一个单一的（复合的）ViewResolver 中，
 * 其 order 属性设置为 0，以便其他外部解析器可以在其之前或之后进行排序。
 *
 * <p>当启用内容协商时，order 属性设置为最高优先级，
 * ContentNegotiatingViewResolver 将封装所有其他已注册的视图解析器实例。
 * 这样，通过 MVC 命名空间注册的解析器形成一个自我封装的解析器链。
 *
 * @author Sivaprasad Valluru
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @see TilesConfigurerBeanDefinitionParser
 * @see FreeMarkerConfigurerBeanDefinitionParser
 * @see GroovyMarkupConfigurerBeanDefinitionParser
 * @see ScriptTemplateConfigurerBeanDefinitionParser
 * @since 4.1
 */
public class ViewResolversBeanDefinitionParser implements BeanDefinitionParser {

	/**
	 * 用于 {@code ViewResolverComposite} 的 bean 名称。
	 */
	public static final String VIEW_RESOLVER_BEAN_NAME = "mvcViewResolver";


	@Override
	public BeanDefinition parse(Element element, ParserContext context) {
		// 从元素中提取源对象
		Object source = context.extractSource(element);
		// 将包含的组件推送到上下文中
		context.pushContainingComponent(new CompositeComponentDefinition(element.getTagName(), source));

		// 创建一个 ManagedList 对象用于存储视图解析器
		ManagedList<Object> resolvers = new ManagedList<>(4);
		// 设置视图解析器列表的源
		resolvers.setSource(context.extractSource(element));
		// 定义解析器元素的名称数组
		String[] names = new String[]{
				"jsp", "tiles", "bean-name", "freemarker", "groovy", "script-template", "bean", "ref"};

		// 遍历解析器元素
		for (Element resolverElement : DomUtils.getChildElementsByTagName(element, names)) {
			// 获取标签名称
			String name = resolverElement.getLocalName();
			if ("bean".equals(name) || "ref".equals(name)) {
				// 如果是 "bean" 或 "ref" 子元素
				// 解析并添加 bean 或 ref 子元素
				resolvers.add(context.getDelegate().parsePropertySubElement(resolverElement, null));
				continue;
			}
			RootBeanDefinition resolverBeanDef;
			if ("jsp".equals(name)) {
				// 如果标签名称是jsp
				// 创建 内部资源视图解析器的 bean 定义
				resolverBeanDef = new RootBeanDefinition(InternalResourceViewResolver.class);
				// 设置内部资源视图解析器的前缀和后缀
				resolverBeanDef.getPropertyValues().add("prefix", "/WEB-INF/");
				resolverBeanDef.getPropertyValues().add("suffix", ".jsp");
				// 将 内部资源视图解析器 的Bean定义添加到 基于Url的视图解析程序属性 中
				addUrlBasedViewResolverProperties(resolverElement, resolverBeanDef);
			} else if ("tiles".equals(name)) {
				// 如果标签名称是 tiles
				// 创建 Tiles 视图解析器的 bean 定义
				resolverBeanDef = new RootBeanDefinition(TilesViewResolver.class);
				// 将 Tiles 视图解析器 的Bean定义添加到 基于Url的视图解析程序属性 中
				addUrlBasedViewResolverProperties(resolverElement, resolverBeanDef);
			} else if ("freemarker".equals(name)) {
				// 如果标签名称是 freemarker
				// 创建 FreeMarker 视图解析器的 bean 定义
				resolverBeanDef = new RootBeanDefinition(FreeMarkerViewResolver.class);
				// 设置 FreeMarker 视图解析器的后缀
				resolverBeanDef.getPropertyValues().add("suffix", ".ftl");
				// 将 FreeMarker 视图解析器 的Bean定义添加到 基于Url的视图解析程序属性 中
				addUrlBasedViewResolverProperties(resolverElement, resolverBeanDef);
			} else if ("groovy".equals(name)) {
				// 如果标签名称是 groovy
				// 创建 Groovy 视图解析器的 bean 定义
				resolverBeanDef = new RootBeanDefinition(GroovyMarkupViewResolver.class);
				// 设置 Groovy 视图解析器的后缀
				resolverBeanDef.getPropertyValues().add("suffix", ".tpl");
				// 将 Groovy 视图解析器 的Bean定义添加到 基于Url的视图解析程序属性 中
				addUrlBasedViewResolverProperties(resolverElement, resolverBeanDef);
			} else if ("script-template".equals(name)) {
				// 如果标签名称是 script-template
				// 创建 ScriptTemplate 视图解析器的 bean 定义
				resolverBeanDef = new RootBeanDefinition(ScriptTemplateViewResolver.class);
				// 将 ScriptTemplate 视图解析器 的Bean定义添加到 基于Url的视图解析程序属性 中
				addUrlBasedViewResolverProperties(resolverElement, resolverBeanDef);
			} else if ("bean-name".equals(name)) {
				// 如果标签名称是 bean-name
				// 创建 BeanName 视图解析器的 bean 定义
				resolverBeanDef = new RootBeanDefinition(BeanNameViewResolver.class);
			} else {
				// 不应该发生的情况
				throw new IllegalStateException("Unexpected element name: " + name);
			}
			// 设置解析器 bean 定义的源和角色
			resolverBeanDef.setSource(source);
			resolverBeanDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			// 将解析器 bean 定义添加到解析器列表中
			resolvers.add(resolverBeanDef);
		}

		// 定义视图解析器 bean 的名称 —— mvcViewResolver
		String beanName = VIEW_RESOLVER_BEAN_NAME;
		// 创建 ViewResolverComposite 类的 根Bean定义
		RootBeanDefinition compositeResolverBeanDef = new RootBeanDefinition(ViewResolverComposite.class);
		// 设置 根Bean定义 的源和角色
		compositeResolverBeanDef.setSource(source);
		compositeResolverBeanDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);

		// 查找内容协商元素
		names = new String[]{"content-negotiation"};
		List<Element> contentNegotiationElements = DomUtils.getChildElementsByTagName(element, names);
		if (contentNegotiationElements.isEmpty()) {
			// 如果没有内容协商元素，直接添加视图解析器列表
			compositeResolverBeanDef.getPropertyValues().add("viewResolvers", resolvers);
		} else if (contentNegotiationElements.size() == 1) {
			// 如果有一个内容协商元素，创建并配置内容协商视图解析器
			BeanDefinition beanDef = createContentNegotiatingViewResolver(contentNegotiationElements.get(0), context);
			// 设置视图解析器
			beanDef.getPropertyValues().add("viewResolvers", resolvers);
			ManagedList<Object> list = new ManagedList<>(1);
			list.add(beanDef);
			// 将排序设置为最高级别
			compositeResolverBeanDef.getPropertyValues().add("order", Ordered.HIGHEST_PRECEDENCE);
			// 设置视图解析器列表
			compositeResolverBeanDef.getPropertyValues().add("viewResolvers", list);
		} else {
			// 如果有多个内容协商元素，抛出异常
			throw new IllegalArgumentException("Only one <content-negotiation> element is allowed.");
		}

		// 如果元素具有 "order" 属性，添加到复合解析器 bean 定义中
		if (element.hasAttribute("order")) {
			compositeResolverBeanDef.getPropertyValues().add("order", element.getAttribute("order"));
		}

		// 在注册表中注册视图解析器 bean 定义
		context.getReaderContext().getRegistry().registerBeanDefinition(beanName, compositeResolverBeanDef);
		// 在上下文中注册 当前Bean定义的Bean组件定义
		context.registerComponent(new BeanComponentDefinition(compositeResolverBeanDef, beanName));
		// 弹出并注册包含组件
		context.popAndRegisterContainingComponent();
		return null;
	}

	private void addUrlBasedViewResolverProperties(Element element, RootBeanDefinition beanDefinition) {
		// 如果元素具有 "prefix" 属性
		if (element.hasAttribute("prefix")) {
			// 将 "prefix" 属性添加到 bean 定义的属性值中
			beanDefinition.getPropertyValues().add("prefix", element.getAttribute("prefix"));
		}

		// 如果元素具有 "suffix" 属性
		if (element.hasAttribute("suffix")) {
			// 将 "suffix" 属性添加到 bean 定义的属性值中
			beanDefinition.getPropertyValues().add("suffix", element.getAttribute("suffix"));
		}

		// 如果元素具有 "cache-views" 属性
		if (element.hasAttribute("cache-views")) {
			// 将 "cache-views" 属性添加到 bean 定义的属性值中，并命名为 "cache"
			beanDefinition.getPropertyValues().add("cache", element.getAttribute("cache-views"));
		}

		// 如果元素具有 "view-class" 属性
		if (element.hasAttribute("view-class")) {
			// 将 "view-class" 属性添加到 bean 定义的属性值中
			beanDefinition.getPropertyValues().add("viewClass", element.getAttribute("view-class"));
		}

		// 如果元素具有 "view-names" 属性
		if (element.hasAttribute("view-names")) {
			// 将 "view-names" 属性添加到 bean 定义的属性值中
			beanDefinition.getPropertyValues().add("viewNames", element.getAttribute("view-names"));
		}
	}

	private BeanDefinition createContentNegotiatingViewResolver(Element resolverElement, ParserContext context) {
		// 创建 ContentNegotiatingViewResolver 类的 根Bean定义
		RootBeanDefinition beanDef = new RootBeanDefinition(ContentNegotiatingViewResolver.class);
		// 设置来源
		beanDef.setSource(context.extractSource(resolverElement));
		// 将角色设置为基础设施角色
		beanDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		// 获取 bean 定义的属性值对象
		MutablePropertyValues values = beanDef.getPropertyValues();

		// 获取解析器元素中名为 "default-views" 的子元素列表
		List<Element> elements = DomUtils.getChildElementsByTagName(resolverElement, "default-views");
		// 如果 "default-views" 元素列表不为空
		if (!elements.isEmpty()) {
			// 创建一个 ManagedList 对象，用于保存默认视图
			ManagedList<Object> list = new ManagedList<>();
			// 获取 "default-views" 元素的第一个子元素中的所有 "bean" 和 "ref" 子元素
			for (Element element : DomUtils.getChildElementsByTagName(elements.get(0), "bean", "ref")) {
				// 解析子元素并添加到列表中
				list.add(context.getDelegate().parsePropertySubElement(element, null));
			}
			// 将默认视图列表添加到 bean 定义的属性值中
			values.add("defaultViews", list);
		}

		// 如果解析器元素具有 "use-not-acceptable" 属性
		if (resolverElement.hasAttribute("use-not-acceptable")) {
			// 将 "use-not-acceptable" 属性添加到 bean 定义的属性值中
			values.add("useNotAcceptableStatusCode", resolverElement.getAttribute("use-not-acceptable"));
		}

		// 获取内容协商管理器
		Object manager = MvcNamespaceUtils.getContentNegotiationManager(context);
		// 如果内容协商管理器不为空
		if (manager != null) {
			// 将内容协商管理器添加到 bean 定义的属性值中
			values.add("contentNegotiationManager", manager);
		}

		// 返回 bean 定义
		return beanDef;
	}

}
