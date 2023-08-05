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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Default implementation of the {@link BeanDefinitionDocumentReader} interface that
 * reads bean definitions according to the "spring-beans" DTD and XSD format
 * (Spring's default XML bean definition format).
 *
 * <p>The structure, elements, and attribute names of the required XML document
 * are hard-coded in this class. (Of course a transform could be run if necessary
 * to produce this format). {@code <beans>} does not need to be the root
 * element of the XML document: this class will parse all bean definition elements
 * in the XML file, regardless of the actual root element.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Erik Wiersma
 * @since 18.12.2003
 */
public class DefaultBeanDefinitionDocumentReader implements BeanDefinitionDocumentReader {
	/**
	 * Bean元素
	 */
	public static final String BEAN_ELEMENT = BeanDefinitionParserDelegate.BEAN_ELEMENT;
	/**
	 * 嵌套Beans元素
	 */
	public static final String NESTED_BEANS_ELEMENT = "beans";

	/**
	 * 别名标签元素
	 */
	public static final String ALIAS_ELEMENT = "alias";
	/**
	 * 名称属性
	 */
	public static final String NAME_ATTRIBUTE = "name";
	/**
	 * 别名属性
	 */
	public static final String ALIAS_ATTRIBUTE = "alias";
	/**
	 * 导入标签元素
	 */
	public static final String IMPORT_ELEMENT = "import";
	/**
	 * 资源属性
	 */
	public static final String RESOURCE_ATTRIBUTE = "resource";
	/**
	 * 定义环境属性
	 */
	public static final String PROFILE_ATTRIBUTE = "profile";


	protected final Log logger = LogFactory.getLog(getClass());

	/**
	 * XML读取器上下文
	 */
	@Nullable
	private XmlReaderContext readerContext;

	/**
	 * Bean定义解析器代理
	 */
	@Nullable
	private BeanDefinitionParserDelegate delegate;


	/**
	 * 此实现根据 “spring-beans” XSD (或DTD，历史上) 解析bean定义。
	 * <p>打开一个DOM文档;
	 * 然后在 {@code <beans/>} 级别初始化指定的默认设置;
	 * 然后解析包含的bean定义。
	 */
	@Override
	public void registerBeanDefinitions(Document doc, XmlReaderContext readerContext) {
		this.readerContext = readerContext;
		//解析文档元素执行注册bean定义
		doRegisterBeanDefinitions(doc.getDocumentElement());
	}

	/**
	 * 返回此解析器工作的XML资源的描述符。
	 */
	protected final XmlReaderContext getReaderContext() {
		Assert.state(this.readerContext != null, "No XmlReaderContext available");
		return this.readerContext;
	}

	/**
	 * 调用 {@link org.springframework.beans.factory.parsing.SourceExtractor} 从提供的 {@link Element} 中提取源元数据。
	 */
	@Nullable
	protected Object extractSource(Element ele) {
		return getReaderContext().extractSource(ele);
	}


	/**
	 * 在给定的根 {@code <beans/>} 元素中注册每个bean定义。
	 */
	@SuppressWarnings("deprecation")
	protected void doRegisterBeanDefinitions(Element root) {
		/*
		任何嵌套的 <beans> 元素都会导致此方法中的递归。
		为了正确传播和保留 <beans> 默认属性，请跟踪当前 (父) 委托，该委托可能为null。
		创建带有对父级的引用的新 (子级) 委托，以进行回退，然后最终将this.delegate重置回其原始 (父级) 引用。
		这种行为模拟了一堆委托，而实际上并不需要一个。
		 */
		BeanDefinitionParserDelegate parent = this.delegate;
		//创建Bean定义解析器代理
		this.delegate = createDelegate(getReaderContext(), root, parent);

		if (this.delegate.isDefaultNamespace(root)) {
			//如果是默认的命名空间，获取环境属性
			String profileSpec = root.getAttribute(PROFILE_ATTRIBUTE);
			if (StringUtils.hasText(profileSpec)) {
				//环境字符串按照,或;分割成一个字符串数组
				String[] specifiedProfiles = StringUtils.tokenizeToStringArray(profileSpec, BeanDefinitionParserDelegate.MULTI_VALUE_ATTRIBUTE_DELIMITERS);
				//我们不能使用Profiles.of(...)，因为XML config中不支持profile表达式。有关详细信息，请参阅SPR-12458。
				if (!getReaderContext().getEnvironment().acceptsProfiles(specifiedProfiles)) {
					//如果当前环境不是指定的环境，记录日志后直接结束
					if (logger.isDebugEnabled()) {
						logger.debug("Skipped XML bean definition file due to specified profiles [" + profileSpec + "] not matching: " + getReaderContext().getResource());
					}
					return;
				}
			}
		}
		//预处理文档根对象
		preProcessXml(root);
		//解析文档中根级别的元素: “import”，“alias”，“bean”。
		parseBeanDefinitions(root, this.delegate);
		//后置处理文档根对象
		postProcessXml(root);

		this.delegate = parent;
	}

	protected BeanDefinitionParserDelegate createDelegate(XmlReaderContext readerContext, Element root, @Nullable BeanDefinitionParserDelegate parentDelegate) {
		//创建Bean定义解析器代理实例
		BeanDefinitionParserDelegate delegate = new BeanDefinitionParserDelegate(readerContext);
		//初始化根元素和父级Bean定义解析器代理
		delegate.initDefaults(root, parentDelegate);
		return delegate;
	}

	/**
	 * 解析文档中根级别的元素: “import”，“alias”，“bean”。
	 *
	 * @param root 文档的DOM根元素
	 */
	protected void parseBeanDefinitions(Element root, BeanDefinitionParserDelegate delegate) {
		if (delegate.isDefaultNamespace(root)) {
			//如果是默认的命名空间，获取根节点的子节点
			NodeList nl = root.getChildNodes();
			for (int i = 0; i < nl.getLength(); i++) {
				Node node = nl.item(i);
				if (node instanceof Element) {
					//如果是元素，转为元素
					Element ele = (Element) node;
					if (delegate.isDefaultNamespace(ele)) {
						//如果是默认的命名空间，解析默认元素
						parseDefaultElement(ele, delegate);
					} else {
						//否则解析自定义元素
						delegate.parseCustomElement(ele);
					}
				}
			}
		} else {
			//非默认命名空间，解析自定义元素
			delegate.parseCustomElement(root);
		}
	}

	private void parseDefaultElement(Element ele, BeanDefinitionParserDelegate delegate) {
		if (delegate.nodeNameEquals(ele, IMPORT_ELEMENT)) {
			//如果是import元素，通过该元素导入Bean定义资源
			importBeanDefinitionResource(ele);
		} else if (delegate.nodeNameEquals(ele, ALIAS_ELEMENT)) {
			//如果是别名元素标签，通过该元素处理别名注册
			processAliasRegistration(ele);
		} else if (delegate.nodeNameEquals(ele, BEAN_ELEMENT)) {
			//如果是Bean元素标签，通过该元素处理Bean定义
			processBeanDefinition(ele, delegate);
		} else if (delegate.nodeNameEquals(ele, NESTED_BEANS_ELEMENT)) {
			// 如果是嵌套Bean元素标签，递归处理嵌套Bean定义
			doRegisterBeanDefinitions(ele);
		}
	}

	/**
	 * 解析 “import” 元素，并将bean定义从给定资源加载到bean工厂。
	 */
	protected void importBeanDefinitionResource(Element ele) {
		//获取resource属性值
		String location = ele.getAttribute(RESOURCE_ATTRIBUTE);
		if (!StringUtils.hasText(location)) {
			getReaderContext().error("Resource location must not be empty", ele);
			return;
		}

		//解析系统属性: 例如  "${user.dir}"
		location = getReaderContext().getEnvironment().resolveRequiredPlaceholders(location);

		Set<Resource> actualResources = new LinkedHashSet<>(4);

		// 发现位置是绝对URI还是相对URI
		boolean absoluteLocation = false;
		try {
			//通过ResourcePatternUtils或者它的URI判断位置是否绝对路径
			absoluteLocation = ResourcePatternUtils.isUrl(location) || ResourceUtils.toURI(location).isAbsolute();
		} catch (URISyntaxException ex) {
			// 考虑到位置相对，无法转换为URI
			// 除非是众所周知的Spring前缀 “classpath*:”
		}

		// 绝对还是相对？
		if (absoluteLocation) {
			try {
				//加载Bean定义
				int importCount = getReaderContext().getReader().loadBeanDefinitions(location, actualResources);
				if (logger.isTraceEnabled()) {
					logger.trace("Imported " + importCount + " bean definitions from URL location [" + location + "]");
				}
			} catch (BeanDefinitionStoreException ex) {
				getReaderContext().error("Failed to import bean definitions from URL location [" + location + "]", ele, ex);
			}
		} else {
			// 没有URL -> 考虑资源位置相对于当前文件。
			try {
				//加载Bean定义的数量
				int importCount;
				//根据位置创建新的相关资源
				Resource relativeResource = getReaderContext().getResource().createRelative(location);
				if (relativeResource.exists()) {
					//如果相关资源存在，从该资源中加载Bean定义
					importCount = getReaderContext().getReader().loadBeanDefinitions(relativeResource);
					//将相关资源加入到集合中
					actualResources.add(relativeResource);
				} else {
					//获取当前资源的URL字符串形式，作为基础位置
					String baseLocation = getReaderContext().getResource().getURL().toString();
					importCount = getReaderContext().getReader().loadBeanDefinitions(StringUtils.applyRelativePath(baseLocation, location), actualResources);
				}
				if (logger.isTraceEnabled()) {
					logger.trace("Imported " + importCount + " bean definitions from relative location [" + location + "]");
				}
			} catch (IOException ex) {
				getReaderContext().error("Failed to resolve current resource location", ele, ex);
			} catch (BeanDefinitionStoreException ex) {
				getReaderContext().error("Failed to import bean definitions from relative location [" + location + "]", ele, ex);
			}
		}
		//获取第一个资源，并转为资源数组的形式
		Resource[] actResArray = actualResources.toArray(new Resource[0]);
		//触发导入处理的事件。
		getReaderContext().fireImportProcessed(location, actResArray, extractSource(ele));
	}

	/**
	 * 处理给定的别名元素，向注册表注册别名。
	 */
	protected void processAliasRegistration(Element ele) {
		//获取名称的属性值
		String name = ele.getAttribute(NAME_ATTRIBUTE);
		//获取别名的属性值
		String alias = ele.getAttribute(ALIAS_ATTRIBUTE);
		boolean valid = true;
		if (!StringUtils.hasText(name)) {
			//如果名称为空，抛出错误信息
			getReaderContext().error("Name must not be empty", ele);
			valid = false;
		}
		if (!StringUtils.hasText(alias)) {
			//如果别名为空，也提示错误信息
			getReaderContext().error("Alias must not be empty", ele);
			valid = false;
		}
		if (valid) {
			try {
				//注册别名
				getReaderContext().getRegistry().registerAlias(name, alias);
			} catch (Exception ex) {
				getReaderContext().error("Failed to register alias '" + alias + "' for bean with name '" + name + "'", ele, ex);
			}
			//触发别名注册的事件。
			getReaderContext().fireAliasRegistered(name, alias, extractSource(ele));
		}
	}

	/**
	 * 处理给定的bean元素，解析bean定义并将其注册到注册表中。
	 */
	protected void processBeanDefinition(Element ele, BeanDefinitionParserDelegate delegate) {
		//将元素解析成bean定义持有者
		BeanDefinitionHolder bdHolder = delegate.parseBeanDefinitionElement(ele);
		if (bdHolder != null) {
			//如果该bean定义持有者不为空，装饰bean定义，并产生bean定义持有者
			bdHolder = delegate.decorateBeanDefinitionIfRequired(ele, bdHolder);
			try {
				// 注册最终装饰实例。
				BeanDefinitionReaderUtils.registerBeanDefinition(bdHolder, getReaderContext().getRegistry());
			} catch (BeanDefinitionStoreException ex) {
				getReaderContext().error("Failed to register bean definition with name '" + bdHolder.getBeanName() + "'", ele, ex);
			}
			// 发送注册事件。
			getReaderContext().fireComponentRegistered(new BeanComponentDefinition(bdHolder));
		}
	}


	/**
	 * 在开始处理bean定义之前，首先通过处理任何自定义元素类型来允许XML是可扩展的。
	 * 此方法是XML的任何其他自定义预处理的自然扩展点。
	 * <p> 默认实现为空。子类可以覆盖此方法以将自定义元素转换为标准Spring bean定义，
	 * 例如：实现者可以通过相应的访问器访问解析器的bean定义读取器和底层XML资源。
	 *
	 * @see #getReaderContext()
	 */
	protected void preProcessXml(Element root) {
	}

	/**
	 * 在完成bean定义的处理后，通过最后处理任何自定义元素类型来允许XML可扩展。
	 * 此方法是XML的任何其他自定义后处理的自然扩展点。
	 * <p> 默认实现为空。子类可以覆盖此方法以将自定义元素转换为标准Spring bean定义，
	 * 例如：实现者可以通过相应的访问器访问解析器的bean定义读取器和底层XML资源。
	 *
	 * @see #getReaderContext()
	 */
	protected void postProcessXml(Element root) {
	}

}
