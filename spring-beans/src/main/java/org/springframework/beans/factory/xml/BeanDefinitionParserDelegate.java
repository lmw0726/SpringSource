/*
 * Copyright 2002-2020 the original author or authors.
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
import org.springframework.beans.BeanMetadataAttribute;
import org.springframework.beans.BeanMetadataAttributeAccessor;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.*;
import org.springframework.beans.factory.parsing.*;
import org.springframework.beans.factory.support.*;
import org.springframework.lang.Nullable;
import org.springframework.util.*;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.*;

/**
 * Stateful delegate class used to parse XML bean definitions.
 * Intended for use by both the main parser and any extension
 * {@link BeanDefinitionParser BeanDefinitionParsers} or
 * {@link BeanDefinitionDecorator BeanDefinitionDecorators}.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @author Mark Fisher
 * @author Gary Russell
 * @see ParserContext
 * @see DefaultBeanDefinitionDocumentReader
 * @since 2.0
 */
public class BeanDefinitionParserDelegate {

	public static final String BEANS_NAMESPACE_URI = "http://www.springframework.org/schema/beans";

	public static final String MULTI_VALUE_ATTRIBUTE_DELIMITERS = ",; ";

	/**
	 * 表示true的T/F属性的值。其他任何东西都代表错误。
	 */
	public static final String TRUE_VALUE = "true";

	public static final String FALSE_VALUE = "false";

	public static final String DEFAULT_VALUE = "default";

	public static final String DESCRIPTION_ELEMENT = "description";

	public static final String AUTOWIRE_NO_VALUE = "no";

	public static final String AUTOWIRE_BY_NAME_VALUE = "byName";

	public static final String AUTOWIRE_BY_TYPE_VALUE = "byType";

	public static final String AUTOWIRE_CONSTRUCTOR_VALUE = "constructor";

	public static final String AUTOWIRE_AUTODETECT_VALUE = "autodetect";

	public static final String NAME_ATTRIBUTE = "name";

	public static final String BEAN_ELEMENT = "bean";

	public static final String META_ELEMENT = "meta";

	public static final String ID_ATTRIBUTE = "id";

	public static final String PARENT_ATTRIBUTE = "parent";

	public static final String CLASS_ATTRIBUTE = "class";

	public static final String ABSTRACT_ATTRIBUTE = "abstract";

	public static final String SCOPE_ATTRIBUTE = "scope";

	private static final String SINGLETON_ATTRIBUTE = "singleton";

	public static final String LAZY_INIT_ATTRIBUTE = "lazy-init";

	public static final String AUTOWIRE_ATTRIBUTE = "autowire";

	public static final String AUTOWIRE_CANDIDATE_ATTRIBUTE = "autowire-candidate";

	public static final String PRIMARY_ATTRIBUTE = "primary";

	public static final String DEPENDS_ON_ATTRIBUTE = "depends-on";

	public static final String INIT_METHOD_ATTRIBUTE = "init-method";

	public static final String DESTROY_METHOD_ATTRIBUTE = "destroy-method";

	public static final String FACTORY_METHOD_ATTRIBUTE = "factory-method";

	public static final String FACTORY_BEAN_ATTRIBUTE = "factory-bean";

	public static final String CONSTRUCTOR_ARG_ELEMENT = "constructor-arg";

	public static final String INDEX_ATTRIBUTE = "index";

	public static final String TYPE_ATTRIBUTE = "type";

	public static final String VALUE_TYPE_ATTRIBUTE = "value-type";

	public static final String KEY_TYPE_ATTRIBUTE = "key-type";

	public static final String PROPERTY_ELEMENT = "property";

	public static final String REF_ATTRIBUTE = "ref";

	public static final String VALUE_ATTRIBUTE = "value";

	public static final String LOOKUP_METHOD_ELEMENT = "lookup-method";

	public static final String REPLACED_METHOD_ELEMENT = "replaced-method";

	public static final String REPLACER_ATTRIBUTE = "replacer";

	public static final String ARG_TYPE_ELEMENT = "arg-type";

	public static final String ARG_TYPE_MATCH_ATTRIBUTE = "match";

	public static final String REF_ELEMENT = "ref";

	public static final String IDREF_ELEMENT = "idref";

	public static final String BEAN_REF_ATTRIBUTE = "bean";

	public static final String PARENT_REF_ATTRIBUTE = "parent";

	public static final String VALUE_ELEMENT = "value";

	public static final String NULL_ELEMENT = "null";

	public static final String ARRAY_ELEMENT = "array";

	public static final String LIST_ELEMENT = "list";

	public static final String SET_ELEMENT = "set";

	public static final String MAP_ELEMENT = "map";

	public static final String ENTRY_ELEMENT = "entry";

	public static final String KEY_ELEMENT = "key";

	public static final String KEY_ATTRIBUTE = "key";

	public static final String KEY_REF_ATTRIBUTE = "key-ref";

	public static final String VALUE_REF_ATTRIBUTE = "value-ref";

	public static final String PROPS_ELEMENT = "props";

	public static final String PROP_ELEMENT = "prop";

	public static final String MERGE_ATTRIBUTE = "merge";

	public static final String QUALIFIER_ELEMENT = "qualifier";

	public static final String QUALIFIER_ATTRIBUTE_ELEMENT = "attribute";
	/**
	 * 默认的懒加载初始化配置
	 */
	public static final String DEFAULT_LAZY_INIT_ATTRIBUTE = "default-lazy-init";
	/**
	 * 默认合并属性
	 */
	public static final String DEFAULT_MERGE_ATTRIBUTE = "default-merge";
	/**
	 * 是否默认自动装配
	 */
	public static final String DEFAULT_AUTOWIRE_ATTRIBUTE = "default-autowire";
	/**
	 * 默认自动获取候选者
	 */
	public static final String DEFAULT_AUTOWIRE_CANDIDATES_ATTRIBUTE = "default-autowire-candidates";
	/**
	 * 默认初始化方法
	 */
	public static final String DEFAULT_INIT_METHOD_ATTRIBUTE = "default-init-method";
	/**
	 * 默认销毁方法
	 */
	public static final String DEFAULT_DESTROY_METHOD_ATTRIBUTE = "default-destroy-method";


	protected final Log logger = LogFactory.getLog(getClass());

	private final XmlReaderContext readerContext;
	/**
	 * 文档默认定义
	 */
	private final DocumentDefaultsDefinition defaults = new DocumentDefaultsDefinition();
	/**
	 * 记录解析状态，这里是用ArrayDeque存储
	 */
	private final ParseState parseState = new ParseState();

	/**
	 * 存储所有使用的bean名称，因此我们可以在每个bean元素的基础上强制执行唯一性。
	 * 重复的bean ids/names可能不存在于同一级别的bean元素嵌套中，但可能会跨级别重复。
	 */
	private final Set<String> usedNames = new HashSet<>();


	/**
	 * 创建新的BeanDefinitionParserDelegate实例，并关联所提供的
	 * {@link XmlReaderContext}.
	 */
	public BeanDefinitionParserDelegate(XmlReaderContext readerContext) {
		Assert.notNull(readerContext, "XmlReaderContext must not be null");
		this.readerContext = readerContext;
	}


	/**
	 * Get the {@link XmlReaderContext} associated with this helper instance.
	 */
	public final XmlReaderContext getReaderContext() {
		return this.readerContext;
	}

	/**
	 * 调用 {@link org.springframework.beans.factory.parsing.SourceExtractor} 从提供的 {@link Element} 中提取源元数据。
	 */
	@Nullable
	protected Object extractSource(Element ele) {
		return this.readerContext.extractSource(ele);
	}

	/**
	 * Report an error with the given message for the given source element.
	 */
	protected void error(String message, Node source) {
		this.readerContext.error(message, source, this.parseState.snapshot());
	}

	/**
	 * Report an error with the given message for the given source element.
	 */
	protected void error(String message, Element source) {
		this.readerContext.error(message, source, this.parseState.snapshot());
	}

	/**
	 * Report an error with the given message for the given source element.
	 */
	protected void error(String message, Element source, Throwable cause) {
		this.readerContext.error(message, source, this.parseState.snapshot(), cause);
	}


	/**
	 * Initialize the default settings assuming a {@code null} parent delegate.
	 */
	public void initDefaults(Element root) {
		initDefaults(root, null);
	}

	/**
	 * 初始化默认的lazy-init，autowire，依赖项检查设置，init方法，destroy方法和合并设置。
	 * 如果未在本地显式设置默认值，则通过回退到给定的父级来支持嵌套的 “beans” 元素用例。
	 *
	 * @see #populateDefaults(DocumentDefaultsDefinition, DocumentDefaultsDefinition, org.w3c.dom.Element)
	 * @see #getDefaults()
	 */
	public void initDefaults(Element root, @Nullable BeanDefinitionParserDelegate parent) {
		//填充默认属性
		populateDefaults(this.defaults, (parent == null ? null : parent.defaults), root);
		this.readerContext.fireDefaultsRegistered(this.defaults);
	}

	/**
	 * 使用默认的lazy-init，autowire，依赖项检查设置，init-方法，destroy-方法和合并设置填充给定的DocumentDefaultsDefinition实例。
	 * 如果未在本地显式设置默认值，则通过回退 {@code parentDefaults} 来支持嵌套的 “beans” 元素用例。
	 *
	 * @param defaults       填充的默认值
	 * @param parentDefaults (如果有) 默认回退到的父BeanDefinitionParserDelegate
	 * @param root           当前bean定义文档的根元素 (或嵌套beans元素)
	 */
	protected void populateDefaults(DocumentDefaultsDefinition defaults, @Nullable DocumentDefaultsDefinition parentDefaults, Element root) {
		String lazyInit = root.getAttribute(DEFAULT_LAZY_INIT_ATTRIBUTE);
		if (isDefaultValue(lazyInit)) {
			//如果是默认的懒加载初始化配置，查看父级文档默认定义是否存在，不存在将lazyInit设置为false。
			//否则按照父级文档默认定义的lazyInit进行设置。
			lazyInit = (parentDefaults == null ? FALSE_VALUE : parentDefaults.getLazyInit());
		}
		//设置默认的lazyInit
		defaults.setLazyInit(lazyInit);

		String merge = root.getAttribute(DEFAULT_MERGE_ATTRIBUTE);
		if (isDefaultValue(merge)) {
			//如果default-merge的值是空值或者default，则从父级文档默认定义中获取。
			//父级文档默认定义不存在，则将default-merge设置为false。存在则取父级文档默认定义的default-merge值。
			merge = (parentDefaults == null ? FALSE_VALUE : parentDefaults.getMerge());
		}
		defaults.setMerge(merge);

		String autowire = root.getAttribute(DEFAULT_AUTOWIRE_ATTRIBUTE);
		if (isDefaultValue(autowire)) {
			//如果default-autowire的值是空值或者default，则从父级文档默认定义中获取。
			//父级文档默认定义不存在，则将default-autowire设置为false。存在则取父级文档默认定义的default-autowire值。
			autowire = (parentDefaults == null ? AUTOWIRE_NO_VALUE : parentDefaults.getAutowire());
		}
		defaults.setAutowire(autowire);

		if (root.hasAttribute(DEFAULT_AUTOWIRE_CANDIDATES_ATTRIBUTE)) {
			//如果根元素有default-autowire-candidates，设置default-autowire-candidates值
			defaults.setAutowireCandidates(root.getAttribute(DEFAULT_AUTOWIRE_CANDIDATES_ATTRIBUTE));
		} else if (parentDefaults != null) {
			//如果父级文档默认定义存在，则从父级文档默认定义中获取。
			defaults.setAutowireCandidates(parentDefaults.getAutowireCandidates());
		}

		if (root.hasAttribute(DEFAULT_INIT_METHOD_ATTRIBUTE)) {
			//如果根元素有default-init-method，设置default-init-method值
			defaults.setInitMethod(root.getAttribute(DEFAULT_INIT_METHOD_ATTRIBUTE));
		} else if (parentDefaults != null) {
			//如果父级文档默认定义存在，则从父级文档默认定义中获取。
			defaults.setInitMethod(parentDefaults.getInitMethod());
		}

		if (root.hasAttribute(DEFAULT_DESTROY_METHOD_ATTRIBUTE)) {
			//如果根元素有default-destroy-method，设置default-destroy-method值
			defaults.setDestroyMethod(root.getAttribute(DEFAULT_DESTROY_METHOD_ATTRIBUTE));
		} else if (parentDefaults != null) {
			//如果父级文档默认定义存在，则从父级文档默认定义中获取。
			defaults.setDestroyMethod(parentDefaults.getDestroyMethod());
		}

		defaults.setSource(this.readerContext.extractSource(root));
	}

	/**
	 * 返回默认的定义对象。
	 */
	public DocumentDefaultsDefinition getDefaults() {
		return this.defaults;
	}

	/**
	 * Return the default settings for bean definitions as indicated within
	 * the attributes of the top-level {@code <beans/>} element.
	 */
	public BeanDefinitionDefaults getBeanDefinitionDefaults() {
		BeanDefinitionDefaults bdd = new BeanDefinitionDefaults();
		bdd.setLazyInit(TRUE_VALUE.equalsIgnoreCase(this.defaults.getLazyInit()));
		bdd.setAutowireMode(getAutowireMode(DEFAULT_VALUE));
		bdd.setInitMethodName(this.defaults.getInitMethod());
		bdd.setDestroyMethodName(this.defaults.getDestroyMethod());
		return bdd;
	}

	/**
	 * Return any patterns provided in the 'default-autowire-candidates'
	 * attribute of the top-level {@code <beans/>} element.
	 */
	@Nullable
	public String[] getAutowireCandidatePatterns() {
		String candidatePattern = this.defaults.getAutowireCandidates();
		return (candidatePattern != null ? StringUtils.commaDelimitedListToStringArray(candidatePattern) : null);
	}


	/**
	 * 解析提供的 {@code <bean>} 元素。如果解析过程中出现错误，可能会返回 {@code null}。
	 * 错误将报告给 {@link org.springframework.beans.factory.parsing.ProblemReporter}.
	 */
	@Nullable
	public BeanDefinitionHolder parseBeanDefinitionElement(Element ele) {
		return parseBeanDefinitionElement(ele, null);
	}

	/**
	 * 解析提供的 {@code <bean>} 元素。如果解析过程中出现错误，可能会返回 {@code null}。
	 * 错误将报告给 {@link org.springframework.beans.factory.parsing.ProblemReporter}.
	 */
	@Nullable
	public BeanDefinitionHolder parseBeanDefinitionElement(Element ele, @Nullable BeanDefinition containingBean) {
		//获取id值
		String id = ele.getAttribute(ID_ATTRIBUTE);
		//获取bean名称
		String nameAttr = ele.getAttribute(NAME_ATTRIBUTE);

		List<String> aliases = new ArrayList<>();
		if (StringUtils.hasLength(nameAttr)) {
			//将他们按照;或者,分割成字符串数组
			String[] nameArr = StringUtils.tokenizeToStringArray(nameAttr, MULTI_VALUE_ATTRIBUTE_DELIMITERS);
			aliases.addAll(Arrays.asList(nameArr));
		}
		//bean名称=id值
		String beanName = id;
		if (!StringUtils.hasText(beanName) && !aliases.isEmpty()) {
			//如果bean名称为空，且别名不为空，bean名称为第一个别名
			beanName = aliases.remove(0);
			if (logger.isTraceEnabled()) {
				logger.trace("No XML 'id' specified - using '" + beanName +
						"' as bean name and " + aliases + " as aliases");
			}
		}

		if (containingBean == null) {
			//如果该bean标签内不包含bean定义，检查名称是否唯一
			checkNameUniqueness(beanName, aliases, ele);
		}
		//解析bean标签内的bean名称解析成bean定义
		AbstractBeanDefinition beanDefinition = parseBeanDefinitionElement(ele, beanName, containingBean);
		if (beanDefinition == null) {
			//如果bean定义为空，返回null
			return null;
		}
		if (!StringUtils.hasText(beanName)) {
			//如果bean名称为空
			try {
				if (containingBean != null) {
					//如果该bean标签内包含bean定义，根据bean定义和bean定义注册器生成bean名称
					beanName = BeanDefinitionReaderUtils.generateBeanName(
							beanDefinition, this.readerContext.getRegistry(), true);
				} else {
					//否则根据bean定义生成bean名称
					//调用的代码相当于BeanDefinitionReaderUtils.generateBeanName(beanDefinition, registry, false)
					beanName = this.readerContext.generateBeanName(beanDefinition);
					//如果生成器返回了类名加上后缀，则为普通bean类名注册别名 (如果仍然可能)。
					// 这预计适用于Spring 1.2/2.0向后兼容性。
					//获取bean的类名
					String beanClassName = beanDefinition.getBeanClassName();
					if (beanClassName != null &&
							beanName.startsWith(beanClassName) &&
							beanName.length() > beanClassName.length() &&
							!this.readerContext.getRegistry().isBeanNameInUse(beanClassName)) {
						//如果bean类名不为空，且bean名称以bean类名开头，
						// 且bean名称长度大于bean类名长度，且该bean类名没有被使用，则将bean类名添加为别名
						aliases.add(beanClassName);
					}
				}
				if (logger.isTraceEnabled()) {
					logger.trace("Neither XML 'id' nor 'name' specified - " +
							"using generated bean name [" + beanName + "]");
				}
			} catch (Exception ex) {
				error(ex.getMessage(), ele);
				return null;
			}
		}
		//将别名列表转为字符串数组，并构建BeanDefinitionHolder
		String[] aliasesArray = StringUtils.toStringArray(aliases);
		return new BeanDefinitionHolder(beanDefinition, beanName, aliasesArray);

	}

	/**
	 * 验证指定的bean名称和别名尚未在当前bean元素嵌套级别中使用。
	 */
	protected void checkNameUniqueness(String beanName, List<String> aliases, Element beanElement) {
		String foundName = null;

		if (StringUtils.hasText(beanName) && this.usedNames.contains(beanName)) {
			//如果bean名称不为空，且该bean名称已经存在于已使用的Bean名称集合中，找到的名称则为该bean名称
			foundName = beanName;
		}
		if (foundName == null) {
			//如果找到的名称为空，在已使用的bean名称集合中，找到第一个名称为别名的bean名称。
			foundName = CollectionUtils.findFirstMatch(this.usedNames, aliases);
		}
		if (foundName != null) {
			//如果找到的名称仍为空，提示错误。
			error("Bean name '" + foundName + "' is already used in this <beans> element", beanElement);
		}
		//将bean名称和别名列表添加到已使用的bean名称集合中。
		this.usedNames.add(beanName);
		this.usedNames.addAll(aliases);
	}

	/**
	 * 解析bean定义本身，不考虑名称或别名。如果在解析bean定义期间出现问题，可能会返回 {@code null}。
	 */
	@Nullable
	public AbstractBeanDefinition parseBeanDefinitionElement(
			Element ele, String beanName, @Nullable BeanDefinition containingBean) {
		//将bean名称设置为解析状态
		this.parseState.push(new BeanEntry(beanName));
		//解析class属性
		String className = null;
		if (ele.hasAttribute(CLASS_ATTRIBUTE)) {
			//获取class属性值，并去除前后的空格
			className = ele.getAttribute(CLASS_ATTRIBUTE).trim();
		}
		//解析parent属性
		String parent = null;
		if (ele.hasAttribute(PARENT_ATTRIBUTE)) {
			//获取parent属性值
			parent = ele.getAttribute(PARENT_ATTRIBUTE);
		}

		try {
			//创建用于承载属性的AbstractBeanDefinition实例
			AbstractBeanDefinition bd = createBeanDefinition(className, parent);
			//解析默认bean的各种属性
			parseBeanDefinitionAttributes(ele, beanName, containingBean, bd);
			//提取description
			bd.setDescription(DomUtils.getChildElementValueByTagName(ele, DESCRIPTION_ELEMENT));
			//解析元数据<meta/>
			parseMetaElements(ele, bd);
			//解析lookup-method属性<look-method/>
			parseLookupOverrideSubElements(ele, bd.getMethodOverrides());
			//解析replaced-method属性<replaced-method/>
			parseReplacedMethodSubElements(ele, bd.getMethodOverrides());

			//解析构造函数参数<constructor-arg/>
			parseConstructorArgElements(ele, bd);
			//解析property子元素<property/>
			parsePropertyElements(ele, bd);
			//解析qualifier子元素<qualifier/>
			parseQualifierElements(ele, bd);

			//设置资源
			bd.setResource(this.readerContext.getResource());
			//设置源
			bd.setSource(extractSource(ele));

			return bd;
		} catch (ClassNotFoundException ex) {
			error("Bean class [" + className + "] not found", ele, ex);
		} catch (NoClassDefFoundError err) {
			error("Class that bean class [" + className + "] depends on not found", ele, err);
		} catch (Throwable ex) {
			error("Unexpected failure during bean definition parsing", ele, ex);
		} finally {
			//将该bean名称解析状态消除
			this.parseState.pop();
		}

		return null;
	}

	/**
	 * 将给定bean元素的属性应用于给定bean定义。
	 *
	 * @param ele            bean声明元素
	 * @param beanName       bean名称
	 * @param containingBean 嵌套的bean定义
	 * @return 根据bean元素属性初始化的bean定义
	 */
	public AbstractBeanDefinition parseBeanDefinitionAttributes(Element ele, String beanName,
																@Nullable BeanDefinition containingBean, AbstractBeanDefinition bd) {

		if (ele.hasAttribute(SINGLETON_ATTRIBUTE)) {
			//如果<bean/>标签有singleton属性，提示错误。
			error("Old 1.x 'singleton' attribute in use - upgrade to 'scope' declaration", ele);
		} else if (ele.hasAttribute(SCOPE_ATTRIBUTE)) {
			//如果有scope属性，设置bean定义的作用域
			bd.setScope(ele.getAttribute(SCOPE_ATTRIBUTE));
		} else if (containingBean != null) {
			// 在内部bean定义的情况下，从嵌套的bean中取默认值。
			bd.setScope(containingBean.getScope());
		}

		if (ele.hasAttribute(ABSTRACT_ATTRIBUTE)) {
			//如果有abstract属性，设置bean定义的是否是抽象的
			bd.setAbstract(TRUE_VALUE.equals(ele.getAttribute(ABSTRACT_ATTRIBUTE)));
		}
		//设置是否懒加载
		String lazyInit = ele.getAttribute(LAZY_INIT_ATTRIBUTE);
		if (isDefaultValue(lazyInit)) {
			//如果是空值或者default，则设置为默认的懒加载属性值，默认是true
			lazyInit = this.defaults.getLazyInit();
		}
		bd.setLazyInit(TRUE_VALUE.equals(lazyInit));
		//设置是否自动装配
		String autowire = ele.getAttribute(AUTOWIRE_ATTRIBUTE);
		bd.setAutowireMode(getAutowireMode(autowire));

		//如果有依赖于其他bean
		if (ele.hasAttribute(DEPENDS_ON_ATTRIBUTE)) {
			//获取依赖于其他bean名称
			String dependsOn = ele.getAttribute(DEPENDS_ON_ATTRIBUTE);
			//根据,或者;分割成字符串数组
			bd.setDependsOn(StringUtils.tokenizeToStringArray(dependsOn, MULTI_VALUE_ATTRIBUTE_DELIMITERS));
		}

		//自动装配候选者
		String autowireCandidate = ele.getAttribute(AUTOWIRE_CANDIDATE_ATTRIBUTE);
		if (isDefaultValue(autowireCandidate)) {
			//如果是空值或者是default，则设置为默认的自动装配候选者
			String candidatePattern = this.defaults.getAutowireCandidates();
			if (candidatePattern != null) {
				//解析候选者，以,分割成字符串数组
				String[] patterns = StringUtils.commaDelimitedListToStringArray(candidatePattern);
				//如果bean名称匹配了pattern数组中的任何一个，则将该bean定义设置为自动装配候选者
				bd.setAutowireCandidate(PatternMatchUtils.simpleMatch(patterns, beanName));
			}
		} else {
			//否则根据该值是否为true，将该bean定义设置为自动装配候选者
			bd.setAutowireCandidate(TRUE_VALUE.equals(autowireCandidate));
		}

		if (ele.hasAttribute(PRIMARY_ATTRIBUTE)) {
			//如果有primary属性，设置bean定义的primary属性值
			bd.setPrimary(TRUE_VALUE.equals(ele.getAttribute(PRIMARY_ATTRIBUTE)));
		}

		if (ele.hasAttribute(INIT_METHOD_ATTRIBUTE)) {
			//如果有初始化方法属性，获取初始化方法名称，并设置bean定义的初始化方法名称
			String initMethodName = ele.getAttribute(INIT_METHOD_ATTRIBUTE);
			bd.setInitMethodName(initMethodName);
		} else if (this.defaults.getInitMethod() != null) {
			//如果有默认的初始化方法名称，并设置bean定义的初始化方法名称，强制初始化方法为false
			bd.setInitMethodName(this.defaults.getInitMethod());
			bd.setEnforceInitMethod(false);
		}

		if (ele.hasAttribute(DESTROY_METHOD_ATTRIBUTE)) {
			//如果有销毁方法属性，获取销毁方法名称，并设置bean定义的销毁方法名称
			String destroyMethodName = ele.getAttribute(DESTROY_METHOD_ATTRIBUTE);
			bd.setDestroyMethodName(destroyMethodName);
		} else if (this.defaults.getDestroyMethod() != null) {
			//如果有默认的销毁方法名称，并设置bean定义的销毁方法名称，强制销毁方法为false
			bd.setDestroyMethodName(this.defaults.getDestroyMethod());
			bd.setEnforceDestroyMethod(false);
		}

		if (ele.hasAttribute(FACTORY_METHOD_ATTRIBUTE)) {
			//如果有工厂方法属性，获取工厂方法名称，并设置bean定义的工厂方法名称
			bd.setFactoryMethodName(ele.getAttribute(FACTORY_METHOD_ATTRIBUTE));
		}
		if (ele.hasAttribute(FACTORY_BEAN_ATTRIBUTE)) {
			//如果有工厂bean属性，获取并设置工厂bean值
			bd.setFactoryBeanName(ele.getAttribute(FACTORY_BEAN_ATTRIBUTE));
		}

		return bd;
	}

	/**
	 * 为给定的类名和父名创建bean定义。
	 *
	 * @param className  bean类的名称
	 * @param parentName 父级bean的名称
	 * @return 新创建的bean定义
	 * @throws ClassNotFoundException 如果尝试了bean类解析，但失败了
	 */
	protected AbstractBeanDefinition createBeanDefinition(@Nullable String className, @Nullable String parentName)
			throws ClassNotFoundException {

		return BeanDefinitionReaderUtils.createBeanDefinition(
				parentName, className, this.readerContext.getBeanClassLoader());
	}

	/**
	 * 解析给定元素下面的元元素 (如果有的话)。
	 */
	public void parseMetaElements(Element ele, BeanMetadataAttributeAccessor attributeAccessor) {
		//获取子节点
		NodeList nl = ele.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++) {
			Node node = nl.item(i);
			if (isCandidateElement(node) && nodeNameEquals(node, META_ELEMENT)) {
				//如果当前子节点是候选元素且该元素节点名称为meta
				Element metaElement = (Element) node;
				//获取key值
				String key = metaElement.getAttribute(KEY_ATTRIBUTE);
				//获取value值
				String value = metaElement.getAttribute(VALUE_ATTRIBUTE);
				//将key-value构建成Bean元数据属性实例
				BeanMetadataAttribute attribute = new BeanMetadataAttribute(key, value);
				attribute.setSource(extractSource(metaElement));
				attributeAccessor.addMetadataAttribute(attribute);
			}
		}
	}

	/**
	 * 将给定的autowire属性值解析为 {@link AbstractBeanDefinition} autowire常量。
	 */
	@SuppressWarnings("deprecation")
	public int getAutowireMode(String attrValue) {
		String attr = attrValue;
		if (isDefaultValue(attr)) {
			//如果是空值或者是default，则设置为默认的自动装配值
			attr = this.defaults.getAutowire();
		}
		//默认为非自动装配
		int autowire = AbstractBeanDefinition.AUTOWIRE_NO;
		if (AUTOWIRE_BY_NAME_VALUE.equals(attr)) {
			//根据名称自动装配
			autowire = AbstractBeanDefinition.AUTOWIRE_BY_NAME;
		} else if (AUTOWIRE_BY_TYPE_VALUE.equals(attr)) {
			//根据类型自动装配
			autowire = AbstractBeanDefinition.AUTOWIRE_BY_TYPE;
		} else if (AUTOWIRE_CONSTRUCTOR_VALUE.equals(attr)) {
			//根据构造函数自动装配
			autowire = AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR;
		} else if (AUTOWIRE_AUTODETECT_VALUE.equals(attr)) {
			//自动检测模式
			autowire = AbstractBeanDefinition.AUTOWIRE_AUTODETECT;
		}
		// 否则保留默认值。
		return autowire;
	}

	/**
	 * 解析给定bean元素的 constructor-arg 子元素
	 */
	public void parseConstructorArgElements(Element beanEle, BeanDefinition bd) {
		//获取子节点
		NodeList nl = beanEle.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++) {
			Node node = nl.item(i);
			if (isCandidateElement(node) && nodeNameEquals(node, CONSTRUCTOR_ARG_ELEMENT)) {
				//如果当前子节点是候选元素，且该元素节点名称为constructor-arg，解析构造参数元素
				parseConstructorArgElement((Element) node, bd);
			}
		}
	}

	/**
	 * 解析给定bean元素的property子元素。
	 */
	public void parsePropertyElements(Element beanEle, BeanDefinition bd) {
		//获取子节点
		NodeList nl = beanEle.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++) {
			Node node = nl.item(i);
			if (isCandidateElement(node) && nodeNameEquals(node, PROPERTY_ELEMENT)) {
				//如果当前子节点是候选元素，且该元素节点名称为property
				parsePropertyElement((Element) node, bd);
			}
		}
	}

	/**
	 * 解析给定bean元素的 qualifier 子元素
	 */
	public void parseQualifierElements(Element beanEle, AbstractBeanDefinition bd) {
		//获取子节点
		NodeList nl = beanEle.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++) {
			Node node = nl.item(i);
			if (isCandidateElement(node) && nodeNameEquals(node, QUALIFIER_ELEMENT)) {
				//如果当前子节点是候选元素，且该元素节点名称为qualifier
				parseQualifierElement((Element) node, bd);
			}
		}
	}

	/**
	 * 解析 给定bean元素的子元素 lookup-override。
	 */
	public void parseLookupOverrideSubElements(Element beanEle, MethodOverrides overrides) {
		//获取子节点
		NodeList nl = beanEle.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++) {
			Node node = nl.item(i);
			if (isCandidateElement(node) && nodeNameEquals(node, LOOKUP_METHOD_ELEMENT)) {
				//如果是候选节点，且该节点名称为lookup-method
				Element ele = (Element) node;
				//获取name的属性值
				String methodName = ele.getAttribute(NAME_ATTRIBUTE);
				//获取bean的属性值
				String beanRef = ele.getAttribute(BEAN_ELEMENT);
				//根据方法名和bean名称创建LookupOverride实例
				LookupOverride override = new LookupOverride(methodName, beanRef);
				override.setSource(extractSource(ele));
				overrides.addOverride(override);
			}
		}
	}

	/**
	 * 解析 给定bean元素的子元素 replaced-method
	 */
	public void parseReplacedMethodSubElements(Element beanEle, MethodOverrides overrides) {
		//获取子节点
		NodeList nl = beanEle.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++) {
			Node node = nl.item(i);
			if (isCandidateElement(node) && nodeNameEquals(node, REPLACED_METHOD_ELEMENT)) {
				//如果该节点是候选节点，且该节点名称为replaced-method
				Element replacedMethodEle = (Element) node;
				//获取name的属性值
				String name = replacedMethodEle.getAttribute(NAME_ATTRIBUTE);
				//获取replacer的属性值
				String callback = replacedMethodEle.getAttribute(REPLACER_ATTRIBUTE);
				//用上面的名称和回调方法创建ReplaceOverride实例
				ReplaceOverride replaceOverride = new ReplaceOverride(name, callback);
				// 寻找arg-type的元素。
				List<Element> argTypeEles = DomUtils.getChildElementsByTagName(replacedMethodEle, ARG_TYPE_ELEMENT);
				for (Element argTypeEle : argTypeEles) {
					//获取match属性值，这里是要替换的方法参数类型。
					String match = argTypeEle.getAttribute(ARG_TYPE_MATCH_ATTRIBUTE);
					//如果match属性值不为空，获取match值，否则从arg-type元素中获取文本值
					match = (StringUtils.hasText(match) ? match : DomUtils.getTextValue(argTypeEle));
					if (StringUtils.hasText(match)) {
						//如果match值不为空，添加到类型标识中
						replaceOverride.addTypeIdentifier(match);
					}
				}
				replaceOverride.setSource(extractSource(replacedMethodEle));
				overrides.addOverride(replaceOverride);
			}
		}
	}

	/**
	 * 解析一个constructor-arg元素
	 */
	public void parseConstructorArgElement(Element ele, BeanDefinition bd) {
		//获取index属性值
		String indexAttr = ele.getAttribute(INDEX_ATTRIBUTE);
		//获取type属性值
		String typeAttr = ele.getAttribute(TYPE_ATTRIBUTE);
		//获取name属性值
		String nameAttr = ele.getAttribute(NAME_ATTRIBUTE);
		if (StringUtils.hasLength(indexAttr)) {
			//如果index属性值不为空
			try {
				//解析成整数
				int index = Integer.parseInt(indexAttr);
				if (index < 0) {
					//如果index值小于0，提示错误
					error("'index' cannot be lower than 0", ele);
				} else {
					try {
						//将解析构造参数设置为解析中的状态
						this.parseState.push(new ConstructorArgumentEntry(index));
						//解析构造参数的属性值
						Object value = parsePropertyValue(ele, bd, null);
						//构建值存储器
						ConstructorArgumentValues.ValueHolder valueHolder = new ConstructorArgumentValues.ValueHolder(value);
						if (StringUtils.hasLength(typeAttr)) {
							//如果type属性值有值，设置type值
							valueHolder.setType(typeAttr);
						}
						if (StringUtils.hasLength(nameAttr)) {
							//如果name属性值有值，设置name值
							valueHolder.setName(nameAttr);
						}
						valueHolder.setSource(extractSource(ele));
						if (bd.getConstructorArgumentValues().hasIndexedArgumentValue(index)) {
							//如果该索引已经注册了，提示错误
							error("Ambiguous constructor-arg entries for index " + index, ele);
						} else {
							//否则添加上该索引和值存储器
							bd.getConstructorArgumentValues().addIndexedArgumentValue(index, valueHolder);
						}
					} finally {
						//退出解析状态
						this.parseState.pop();
					}
				}
			} catch (NumberFormatException ex) {
				error("Attribute 'index' of tag 'constructor-arg' must be an integer", ele);
			}
		} else {
			try {
				//将解析构造参数设置为解析中的状态
				this.parseState.push(new ConstructorArgumentEntry());
				//解析构造参数的属性值
				Object value = parsePropertyValue(ele, bd, null);
				//构建值存储器
				ConstructorArgumentValues.ValueHolder valueHolder = new ConstructorArgumentValues.ValueHolder(value);
				if (StringUtils.hasLength(typeAttr)) {
					//如果type属性值有值，设置type值
					valueHolder.setType(typeAttr);
				}
				if (StringUtils.hasLength(nameAttr)) {
					//如果name属性值有值，设置name值
					valueHolder.setName(nameAttr);
				}
				valueHolder.setSource(extractSource(ele));
				//将值存储器设置为通用的参数解析
				bd.getConstructorArgumentValues().addGenericArgumentValue(valueHolder);
			} finally {
				//退出解析状态
				this.parseState.pop();
			}
		}
	}

	/**
	 * 解析一个 property 元素
	 */
	public void parsePropertyElement(Element ele, BeanDefinition bd) {
		//获取属性名
		String propertyName = ele.getAttribute(NAME_ATTRIBUTE);
		if (!StringUtils.hasLength(propertyName)) {
			error("Tag 'property' must have a 'name' attribute", ele);
			return;
		}
		//将该属性名设置为解析状态
		this.parseState.push(new PropertyEntry(propertyName));
		try {
			if (bd.getPropertyValues().contains(propertyName)) {
				//如果bean的属性值，已经包含了该属性名，提示异常，直接结束。
				error("Multiple 'property' definitions for property '" + propertyName + "'", ele);
				return;
			}
			//解析属性值
			Object val = parsePropertyValue(ele, bd, propertyName);
			//构建属性名和属性值键值对
			PropertyValue pv = new PropertyValue(propertyName, val);
			//解析元元素
			parseMetaElements(ele, pv);
			//提取property标签的属性值
			pv.setSource(extractSource(ele));
			//添加到已解析的属性值对中。
			bd.getPropertyValues().addPropertyValue(pv);
		} finally {
			//清除该属性名的解析状态
			this.parseState.pop();
		}
	}

	/**
	 * 解析一个 qualifier 元素
	 */
	public void parseQualifierElement(Element ele, AbstractBeanDefinition bd) {
		//获取type属性名
		String typeName = ele.getAttribute(TYPE_ATTRIBUTE);
		if (!StringUtils.hasLength(typeName)) {
			//如果type属性名为空，提示错误，直接结束
			error("Tag 'qualifier' must have a 'type' attribute", ele);
			return;
		}
		//将解析type属性名设置为解析状态
		this.parseState.push(new QualifierEntry(typeName));
		try {
			//构建AutowireCandidateQualifier实例
			AutowireCandidateQualifier qualifier = new AutowireCandidateQualifier(typeName);
			//设置源
			qualifier.setSource(extractSource(ele));
			//获取type属性值
			String value = ele.getAttribute(VALUE_ATTRIBUTE);
			if (StringUtils.hasLength(value)) {
				//如果有值，AutowireCandidateQualifier设置自动候选者注入该值
				qualifier.setAttribute(AutowireCandidateQualifier.VALUE_KEY, value);
			}
			//获取子节点
			NodeList nl = ele.getChildNodes();
			for (int i = 0; i < nl.getLength(); i++) {
				Node node = nl.item(i);
				if (isCandidateElement(node) && nodeNameEquals(node, QUALIFIER_ATTRIBUTE_ELEMENT)) {
					//如果子节点是候选节点，且子节点是attribute元素
					Element attributeEle = (Element) node;
					//获取key属性
					String attributeName = attributeEle.getAttribute(KEY_ATTRIBUTE);
					//获取value属性
					String attributeValue = attributeEle.getAttribute(VALUE_ATTRIBUTE);
					if (StringUtils.hasLength(attributeName) && StringUtils.hasLength(attributeValue)) {
						//key-value不为空，设置MetadataAttribute
						BeanMetadataAttribute attribute = new BeanMetadataAttribute(attributeName, attributeValue);
						attribute.setSource(extractSource(attributeEle));
						qualifier.addMetadataAttribute(attribute);
					} else {
						error("Qualifier 'attribute' tag must have a 'name' and 'value'", attributeEle);
						return;
					}
				}
			}
			bd.addQualifier(qualifier);
		} finally {
			//退出解析状态
			this.parseState.pop();
		}
	}

	/**
	 * 获取属性元素的值。可能是列表等。也用于构造函数参数，在这种情况下，“propertyName” 为null。
	 */
	@Nullable
	public Object parsePropertyValue(Element ele, BeanDefinition bd, @Nullable String propertyName) {
		//如果属性名为空，元素名称为“constructor-arg”，否则为“property”元素
		String elementName = (propertyName != null ?
				"<property> element for property '" + propertyName + "'" :
				"<constructor-arg> element");

		// 应该只有一个子元素: ref、value、list等。
		//获取子节点
		NodeList nl = ele.getChildNodes();
		Element subElement = null;
		for (int i = 0; i < nl.getLength(); i++) {
			Node node = nl.item(i);
			if (node instanceof Element && !nodeNameEquals(node, DESCRIPTION_ELEMENT) &&
					!nodeNameEquals(node, META_ELEMENT)) {
				//如果子节点是元素类型，且节点名称不是“description”、“meta”等，
				// 子元素是我们正在寻找的。
				if (subElement != null) {
					//子元素不为空，提示子元素有多个
					error(elementName + " must not contain more than one sub-element", ele);
				} else {
					//否则子元素等于当前子节点
					subElement = (Element) node;
				}
			}
		}
		//获取关联bean的名称
		boolean hasRefAttribute = ele.hasAttribute(REF_ATTRIBUTE);
		//获取属性值
		boolean hasValueAttribute = ele.hasAttribute(VALUE_ATTRIBUTE);
		if ((hasRefAttribute && hasValueAttribute) ||
				((hasRefAttribute || hasValueAttribute) && subElement != null)) {
			//如果同时有ref和value属性，
			// 或者有ref属性（或value属性），且子元素不为空，提示错误
			error(elementName +
					" is only allowed to contain either 'ref' attribute OR 'value' attribute OR sub-element", ele);
		}

		if (hasRefAttribute) {
			//如果有ref属性，获取ref属性值
			String refName = ele.getAttribute(REF_ATTRIBUTE);
			if (!StringUtils.hasText(refName)) {
				//如果ref属性值为空，则提示错误
				error(elementName + " contains empty 'ref' attribute", ele);
			}
			//bean定义设置运行时关联bean
			RuntimeBeanReference ref = new RuntimeBeanReference(refName);
			ref.setSource(extractSource(ele));
			return ref;
		} else if (hasValueAttribute) {
			//如果有value属性，获取value属性值，并构建成TypedStringValue实例
			TypedStringValue valueHolder = new TypedStringValue(ele.getAttribute(VALUE_ATTRIBUTE));
			valueHolder.setSource(extractSource(ele));
			return valueHolder;
		} else if (subElement != null) {
			//如果子元素不为空，解析子元素
			return parsePropertySubElement(subElement, bd);
		} else {
			// 既没有找到子元素，也没有找到 “ref” 或 “value” 属性，提示错误信息
			error(elementName + " must specify a ref or value", ele);
			return null;
		}
	}

	/**
	 * 解析 property 或 constructor-arg元素中，ref或集合子元素的值。
	 *
	 * @param ele 属性元素的子元素; 我们还不知道哪个
	 * @param bd  当前bean定义 (如果有)
	 */
	@Nullable
	public Object parsePropertySubElement(Element ele, @Nullable BeanDefinition bd) {
		return parsePropertySubElement(ele, bd, null);
	}

	/**
	 * 解析 property 或 constructor-arg元素中，ref或集合子元素的值。
	 *
	 * @param ele              属性元素的子元素; 我们还不知道哪个
	 * @param bd               当前bean定义 (如果有)
	 * @param defaultValueType 可能创建的任何 {@code <value>} 标记的默认类型 (类名)
	 */
	@Nullable
	public Object parsePropertySubElement(Element ele, @Nullable BeanDefinition bd, @Nullable String defaultValueType) {
		if (!isDefaultNamespace(ele)) {
			//如果不是默认的命名空间，解析嵌套的自定义元素
			return parseNestedCustomElement(ele, bd);
		} else if (nodeNameEquals(ele, BEAN_ELEMENT)) {
			//如果是bean标签，解析bean标签，得到嵌套的bean定义。
			BeanDefinitionHolder nestedBd = parseBeanDefinitionElement(ele, bd);
			if (nestedBd != null) {
				//如果嵌套的bean定义不为空，装配嵌套的bean定义
				nestedBd = decorateBeanDefinitionIfRequired(ele, nestedBd, bd);
			}
			return nestedBd;
		} else if (nodeNameEquals(ele, REF_ELEMENT)) {
			// 对任何bean的任何名称的通用引用。
			//如果节点是ref节点，获取关联bean名称
			String refName = ele.getAttribute(BEAN_REF_ATTRIBUTE);
			boolean toParent = false;
			if (!StringUtils.hasLength(refName)) {
				//如果关联bean名称为空，获取parent值，并作为关联bean名称
				// 对父上下文中另一个bean的id的引用。
				refName = ele.getAttribute(PARENT_REF_ATTRIBUTE);
				toParent = true;
				if (!StringUtils.hasLength(refName)) {
					//如果关联bean名称仍为空，提示错误，返回null
					error("'bean' or 'parent' is required for <ref> element", ele);
					return null;
				}
			}
			if (!StringUtils.hasText(refName)) {
				//如果是空字符串，提示错误，返回null
				error("<ref> element contains empty target attribute", ele);
				return null;
			}
			//构建RuntimeBeanReference实例，设置源后，返回该实例
			RuntimeBeanReference ref = new RuntimeBeanReference(refName, toParent);
			ref.setSource(extractSource(ele));
			return ref;
		} else if (nodeNameEquals(ele, IDREF_ELEMENT)) {
			//如果节点是idref节点，获取关联bean名称
			return parseIdRefElement(ele);
		} else if (nodeNameEquals(ele, VALUE_ELEMENT)) {
			//如果节点是value节点，解析value节点
			return parseValueElement(ele, defaultValueType);
		} else if (nodeNameEquals(ele, NULL_ELEMENT)) {
			//如果节点是null节点，解析null节点
			// 这是一个显著的空值。让我们将其包装在TypedStringValue对象中，以便保留源位置。
			TypedStringValue nullHolder = new TypedStringValue(null);
			nullHolder.setSource(extractSource(ele));
			return nullHolder;
		} else if (nodeNameEquals(ele, ARRAY_ELEMENT)) {
			//如果节点是array节点，解析array节点
			return parseArrayElement(ele, bd);
		} else if (nodeNameEquals(ele, LIST_ELEMENT)) {
			//如果节点是list节点，解析list节点
			return parseListElement(ele, bd);
		} else if (nodeNameEquals(ele, SET_ELEMENT)) {
			//如果节点是set节点，解析set节点
			return parseSetElement(ele, bd);
		} else if (nodeNameEquals(ele, MAP_ELEMENT)) {
			//如果节点是map节点，解析map节点
			return parseMapElement(ele, bd);
		} else if (nodeNameEquals(ele, PROPS_ELEMENT)) {
			//如果节点是props节点，解析props节点
			return parsePropsElement(ele);
		} else {
			//提示错误消息，返回null
			error("Unknown property sub-element: [" + ele.getNodeName() + "]", ele);
			return null;
		}
	}

	/**
	 * 返回给定 'idref' 元素的类型化字符串值对象。
	 */
	@Nullable
	public Object parseIdRefElement(Element ele) {
		// 对任何bean的任何名称的通用引用。
		// 获取bean属性值
		String refName = ele.getAttribute(BEAN_REF_ATTRIBUTE);
		if (!StringUtils.hasLength(refName)) {
			//如果引用名称为null，提示错误，返回null
			error("'bean' is required for <idref> element", ele);
			return null;
		}
		if (!StringUtils.hasText(refName)) {
			//如果引用名称为空，提示错误，返回null
			error("<idref> element contains empty target attribute", ele);
			return null;
		}
		//构建运行时相关bean名称实例
		RuntimeBeanNameReference ref = new RuntimeBeanNameReference(refName);
		ref.setSource(extractSource(ele));
		return ref;
	}

	/**
	 * 返回给定的Value元素的类型化字符串值对象。
	 */
	public Object parseValueElement(Element ele, @Nullable String defaultTypeName) {
		// 这是字面上的值
		String value = DomUtils.getTextValue(ele);
		//获取type属性值
		String specifiedTypeName = ele.getAttribute(TYPE_ATTRIBUTE);
		String typeName = specifiedTypeName;
		if (!StringUtils.hasText(typeName)) {
			//如果没有指定类型，则类名名称为默认值类型名
			typeName = defaultTypeName;
		}
		try {
			//根据属性值和类型名称构建类型字符串值实例
			TypedStringValue typedValue = buildTypedStringValue(value, typeName);
			//设置源
			typedValue.setSource(extractSource(ele));
			//设置特殊的类型名称
			typedValue.setSpecifiedTypeName(specifiedTypeName);
			return typedValue;
		} catch (ClassNotFoundException ex) {
			//提示错误，直接返回值
			error("Type class [" + typeName + "] not found for <value> element", ele, ex);
			return value;
		}
	}

	/**
	 * 为给定的原始值构建一个类型化的字符串值对象。
	 *
	 * @see org.springframework.beans.factory.config.TypedStringValue
	 */
	protected TypedStringValue buildTypedStringValue(String value, @Nullable String targetTypeName)
			throws ClassNotFoundException {
		//获取类加载器
		ClassLoader classLoader = this.readerContext.getBeanClassLoader();
		TypedStringValue typedValue;
		if (!StringUtils.hasText(targetTypeName)) {
			//如果类型名称没有值，使用value构建TypedStringValue实例
			typedValue = new TypedStringValue(value);
		} else if (classLoader != null) {
			//如果类加载器存在，通过反射创建该类的实例
			Class<?> targetType = ClassUtils.forName(targetTypeName, classLoader);
			//根据类的实例构建TypedStringValue实例
			typedValue = new TypedStringValue(value, targetType);
		} else {
			//根据类名和值构建TypedStringValue实例
			typedValue = new TypedStringValue(value, targetTypeName);
		}
		return typedValue;
	}

	/**
	 * 解析一个数组元素
	 */
	public Object parseArrayElement(Element arrayEle, @Nullable BeanDefinition bd) {
		//获取value-type属性值
		String elementType = arrayEle.getAttribute(VALUE_TYPE_ATTRIBUTE);
		//获取子元素
		NodeList nl = arrayEle.getChildNodes();
		//创建管理数组实例
		ManagedArray target = new ManagedArray(elementType, nl.getLength());
		target.setSource(extractSource(arrayEle));
		target.setElementTypeName(elementType);
		//解析是否可以合并
		target.setMergeEnabled(parseMergeAttribute(arrayEle));
		//解析集合元素
		parseCollectionElements(nl, target, bd, elementType);
		return target;
	}

	/**
	 * 解析一个list元素
	 */
	public List<Object> parseListElement(Element collectionEle, @Nullable BeanDefinition bd) {
		//获取value-type属性值
		String defaultElementType = collectionEle.getAttribute(VALUE_TYPE_ATTRIBUTE);
		//获取子节点
		NodeList nl = collectionEle.getChildNodes();
		//创建管理集合实例
		ManagedList<Object> target = new ManagedList<>(nl.getLength());
		target.setSource(extractSource(collectionEle));
		//设置默认元素类型
		target.setElementTypeName(defaultElementType);
		//设置是否可以合并元素
		target.setMergeEnabled(parseMergeAttribute(collectionEle));
		//解析集合类型元素
		parseCollectionElements(nl, target, bd, defaultElementType);
		return target;
	}

	/**
	 * 解析一个 set 元素
	 */
	public Set<Object> parseSetElement(Element collectionEle, @Nullable BeanDefinition bd) {
		//获取value-type属性值
		String defaultElementType = collectionEle.getAttribute(VALUE_TYPE_ATTRIBUTE);
		//获取子节点
		NodeList nl = collectionEle.getChildNodes();
		//创建管理Set实例
		ManagedSet<Object> target = new ManagedSet<>(nl.getLength());
		//设置源
		target.setSource(extractSource(collectionEle));
		//设置元素类型名称
		target.setElementTypeName(defaultElementType);
		//设置是否可以合并属性
		target.setMergeEnabled(parseMergeAttribute(collectionEle));
		//解析集合元素
		parseCollectionElements(nl, target, bd, defaultElementType);
		return target;
	}

	protected void parseCollectionElements(
			NodeList elementNodes, Collection<Object> target, @Nullable BeanDefinition bd, String defaultElementType) {

		for (int i = 0; i < elementNodes.getLength(); i++) {
			//遍历子元素节点
			Node node = elementNodes.item(i);
			if (node instanceof Element && !nodeNameEquals(node, DESCRIPTION_ELEMENT)) {
				//如果该节点是Element元素，并且该节点不是description元素，添加解析好的子元素的节点值
				target.add(parsePropertySubElement((Element) node, bd, defaultElementType));
			}
		}
	}

	/**
	 * 解析一个 map 元素
	 */
	public Map<Object, Object> parseMapElement(Element mapEle, @Nullable BeanDefinition bd) {
		//获取key-type属性值
		String defaultKeyType = mapEle.getAttribute(KEY_TYPE_ATTRIBUTE);
		//获取value-type属性值
		String defaultValueType = mapEle.getAttribute(VALUE_TYPE_ATTRIBUTE);
		//获取名称为entry的子节点
		List<Element> entryEles = DomUtils.getChildElementsByTagName(mapEle, ENTRY_ELEMENT);
		//创建管理Map实例
		ManagedMap<Object, Object> map = new ManagedMap<>(entryEles.size());
		//设置源
		map.setSource(extractSource(mapEle));
		//设置keyType值
		map.setKeyTypeName(defaultKeyType);
		//设置ValueType值
		map.setValueTypeName(defaultValueType);
		//设置是否可以合并属性
		map.setMergeEnabled(parseMergeAttribute(mapEle));

		for (Element entryEle : entryEles) {
			// 应该只有一个值子元素: ref、value、list等。可选地，可能有一个关键子元素。
			//获取entry的子字节点列表
			NodeList entrySubNodes = entryEle.getChildNodes();
			Element keyEle = null;
			Element valueEle = null;
			for (int j = 0; j < entrySubNodes.getLength(); j++) {
				//entry节点的子节点
				Node node = entrySubNodes.item(j);
				if (node instanceof Element) {
					//如果该子节点是Element节点
					Element candidateEle = (Element) node;
					if (nodeNameEquals(candidateEle, KEY_ELEMENT)) {
						//名称为key的子节点
						if (keyEle != null) {
							//key元素不为空，提示错误
							error("<entry> element is only allowed to contain one <key> sub-element", entryEle);
						} else {
							//否则key元素为当前节点
							keyEle = candidateEle;
						}
					} else {
						// 子元素是我们正在寻找的。
						if (nodeNameEquals(candidateEle, DESCRIPTION_ELEMENT)) {
							//如果当前节点名为description元素，忽略
						} else if (valueEle != null) {
							//value节点也只能有一个
							error("<entry> element must not contain more than one value sub-element", entryEle);
						} else {
							//否则value节点为当前节点
							valueEle = candidateEle;
						}
					}
				}
			}

			// 从属性或子元素中提取键。
			Object key = null;
			//entry是否有key属性
			boolean hasKeyAttribute = entryEle.hasAttribute(KEY_ATTRIBUTE);
			////entry是否有key-ref属性
			boolean hasKeyRefAttribute = entryEle.hasAttribute(KEY_REF_ATTRIBUTE);
			if ((hasKeyAttribute && hasKeyRefAttribute) ||
					(hasKeyAttribute || hasKeyRefAttribute) && keyEle != null) {
				//如果entry既有key也有key-ref，或者key子元素有，且key属性或者key-ref属性有值，则提示错误
				error("<entry> element is only allowed to contain either " +
						"a 'key' attribute OR a 'key-ref' attribute OR a <key> sub-element", entryEle);
			}
			if (hasKeyAttribute) {
				//如果entry有key属性，获取key值，构建TypedStringValue 对象
				key = buildTypedStringValueForMap(entryEle.getAttribute(KEY_ATTRIBUTE), defaultKeyType, entryEle);
			} else if (hasKeyRefAttribute) {
				//如果entry有key-ref属性，获取key-ref值
				String refName = entryEle.getAttribute(KEY_REF_ATTRIBUTE);
				if (!StringUtils.hasText(refName)) {
					//如果关联key名为空，则提示错误
					error("<entry> element contains empty 'key-ref' attribute", entryEle);
				}
				//构建RuntimeBeanReference实例
				RuntimeBeanReference ref = new RuntimeBeanReference(refName);
				//设置源
				ref.setSource(extractSource(entryEle));
				key = ref;
			} else if (keyEle != null) {
				//如果有key子元素，解析key元素
				key = parseKeyElement(keyEle, bd, defaultKeyType);
			} else {
				error("<entry> element must specify a key", entryEle);
			}

			// 从属性或子元素中提取值。
			Object value = null;
			//entry节点是否有value属性
			boolean hasValueAttribute = entryEle.hasAttribute(VALUE_ATTRIBUTE);
			//entry节点是否有value-ref属性
			boolean hasValueRefAttribute = entryEle.hasAttribute(VALUE_REF_ATTRIBUTE);
			//entry节点是否有value-type属性
			boolean hasValueTypeAttribute = entryEle.hasAttribute(VALUE_TYPE_ATTRIBUTE);
			if ((hasValueAttribute && hasValueRefAttribute) ||
					(hasValueAttribute || hasValueRefAttribute) && valueEle != null) {
				//如果entry既有value也有value-ref，或者value子元素有，且value属性或者value-ref属性有值，则提示错误
				error("<entry> element is only allowed to contain either " +
						"'value' attribute OR 'value-ref' attribute OR <value> sub-element", entryEle);
			}
			if ((hasValueTypeAttribute && hasValueRefAttribute) ||
					(hasValueTypeAttribute && !hasValueAttribute) ||
					(hasValueTypeAttribute && valueEle != null)) {
				//entry不能同时有value-type属性和value-ref属性
				// 或者有value-type属性且没有value 属性
				// 或者是有value-type属性且有value子元素
				error("<entry> element is only allowed to contain a 'value-type' " +
						"attribute when it has a 'value' attribute", entryEle);
			}
			if (hasValueAttribute) {
				//如果entry有value属性，获取value值
				String valueType = entryEle.getAttribute(VALUE_TYPE_ATTRIBUTE);
				if (!StringUtils.hasText(valueType)) {
					//如果valueType为哦那个，则设置为默认的valueType值
					valueType = defaultValueType;
				}
				//获取value值构建TypedStringValue 对象
				value = buildTypedStringValueForMap(entryEle.getAttribute(VALUE_ATTRIBUTE), valueType, entryEle);
			} else if (hasValueRefAttribute) {
				//如果entry有value-ref属性，获取value-ref值
				String refName = entryEle.getAttribute(VALUE_REF_ATTRIBUTE);
				if (!StringUtils.hasText(refName)) {
					//如果关联名称为空，报错
					error("<entry> element contains empty 'value-ref' attribute", entryEle);
				}
				//构建RuntimeBeanReference实例
				RuntimeBeanReference ref = new RuntimeBeanReference(refName);
				ref.setSource(extractSource(entryEle));
				value = ref;
			} else if (valueEle != null) {
				//如果entry有value子元素，递归解析value子元素
				value = parsePropertySubElement(valueEle, bd, defaultValueType);
			} else {
				error("<entry> element must specify a value", entryEle);
			}

			// 向map中添加最终的key和value
			map.put(key, value);
		}

		return map;
	}

	/**
	 * 为给定的原始值构建一个类型化的字符串值对象。
	 *
	 * @see org.springframework.beans.factory.config.TypedStringValue
	 */
	protected final Object buildTypedStringValueForMap(String value, String defaultTypeName, Element entryEle) {
		try {
			//构建TypedStringValue实例，并设置源
			TypedStringValue typedValue = buildTypedStringValue(value, defaultTypeName);
			typedValue.setSource(extractSource(entryEle));
			return typedValue;
		} catch (ClassNotFoundException ex) {
			//找不到类，提示错误，并返回原始值
			error("Type class [" + defaultTypeName + "] not found for Map key/value type", entryEle, ex);
			return value;
		}
	}

	/**
	 * 解析map元素的 key 子元素。
	 */
	@Nullable
	protected Object parseKeyElement(Element keyEle, @Nullable BeanDefinition bd, String defaultKeyTypeName) {
		//获取子节点
		NodeList nl = keyEle.getChildNodes();
		Element subElement = null;
		for (int i = 0; i < nl.getLength(); i++) {
			//遍历字节点
			Node node = nl.item(i);
			if (node instanceof Element) {
				//如果子节点是Element类型，则设置子节点
				if (subElement != null) {
					//仅寻找一次，找到了，还能找到则提示错误。
					error("<key> element must not contain more than one value sub-element", keyEle);
				} else {
					subElement = (Element) node;
				}
			}
		}
		if (subElement == null) {
			//子节点为空，则返回null
			return null;
		}
		//递归解析子节点
		return parsePropertySubElement(subElement, bd, defaultKeyTypeName);
	}

	/**
	 * 解析一个 props 元素
	 */
	public Properties parsePropsElement(Element propsEle) {
		//构建管理Properties实例
		ManagedProperties props = new ManagedProperties();
		//设置源
		props.setSource(extractSource(propsEle));
		//设置是否可以合并属性
		props.setMergeEnabled(parseMergeAttribute(propsEle));
		//或者prop子元素
		List<Element> propEles = DomUtils.getChildElementsByTagName(propsEle, PROP_ELEMENT);
		for (Element propEle : propEles) {
			//遍历Prop子元素，并获取key值
			String key = propEle.getAttribute(KEY_ATTRIBUTE);
			// 修剪文本值，以避免由典型的XML格式引起的不必要的空格。
			//获取节点的文本值
			String value = DomUtils.getTextValue(propEle).trim();
			//构建TypedStringValue 实例
			TypedStringValue keyHolder = new TypedStringValue(key);
			keyHolder.setSource(extractSource(propEle));
			TypedStringValue valueHolder = new TypedStringValue(value);
			valueHolder.setSource(extractSource(propEle));
			//添加到ManagedProperties 实例中
			props.put(keyHolder, valueHolder);
		}

		return props;
	}

	/**
	 * 解析集合元素的合并属性 (如果有)。
	 */
	public boolean parseMergeAttribute(Element collectionElement) {
		//获取merge属性值
		String value = collectionElement.getAttribute(MERGE_ATTRIBUTE);
		if (isDefaultValue(value)) {
			//如果该值为空值或者default，设置为默认的合并属性
			value = this.defaults.getMerge();
		}
		//测试该值是否为true字符串
		return TRUE_VALUE.equals(value);
	}

	/**
	 * 解析自定义元素 (在默认名称空间之外)。
	 *
	 * @param ele 要解析的元素
	 * @return 生成的bean定义
	 */
	@Nullable
	public BeanDefinition parseCustomElement(Element ele) {
		return parseCustomElement(ele, null);
	}

	/**
	 * 解析自定义元素 (在默认名称空间之外)。
	 *
	 * @param ele          要解析的元素
	 * @param containingBd 包含bean定义 (如果有)
	 * @return 生成的bean定义
	 */
	@Nullable
	public BeanDefinition parseCustomElement(Element ele, @Nullable BeanDefinition containingBd) {
		//获取命名空间URI
		String namespaceUri = getNamespaceURI(ele);
		if (namespaceUri == null) {
			//如果URI为空，则返回null
			return null;
		}
		//解析命名空间URI，获取命名空间处理器
		NamespaceHandler handler = this.readerContext.getNamespaceHandlerResolver().resolve(namespaceUri);
		if (handler == null) {
			error("Unable to locate Spring NamespaceHandler for XML schema namespace [" + namespaceUri + "]", ele);
			return null;
		}
		//将该元素解析成bean定义
		return handler.parse(ele, new ParserContext(this.readerContext, this, containingBd));
	}

	/**
	 * 通过命名空间处理程序 (如果适用) 来装饰给定的bean定义。
	 *
	 * @param ele         当前元素
	 * @param originalDef 当前bean定义
	 * @return 装饰 bean 定义
	 */
	public BeanDefinitionHolder decorateBeanDefinitionIfRequired(Element ele, BeanDefinitionHolder originalDef) {
		return decorateBeanDefinitionIfRequired(ele, originalDef, null);
	}

	/**
	 * 通过命名空间处理程序 (如果适用) 来装饰给定的bean定义。
	 *
	 * @param ele          当前元素
	 * @param originalDef  当前bean定义
	 * @param containingBd 包含bean定义 (如果有)
	 * @return 装饰bean定义
	 */
	public BeanDefinitionHolder decorateBeanDefinitionIfRequired(
			Element ele, BeanDefinitionHolder originalDef, @Nullable BeanDefinition containingBd) {

		BeanDefinitionHolder finalDefinition = originalDef;

		// 首先基于自定义属性进行装饰。
		//获取元素的属性
		NamedNodeMap attributes = ele.getAttributes();
		for (int i = 0; i < attributes.getLength(); i++) {
			Node node = attributes.item(i);
			//根据属性值如果需要则进行装饰
			finalDefinition = decorateIfRequired(node, finalDefinition, containingBd);
		}

		// 基于自定义嵌套元素进行装饰。
		//获取元素的子节点
		NodeList children = ele.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node node = children.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				//如果子节点的类型是元素节点，根据需要使用该子节点装饰bean定义
				finalDefinition = decorateIfRequired(node, finalDefinition, containingBd);
			}
		}
		return finalDefinition;
	}

	/**
	 * 通过命名空间处理程序 (如果适用) 来装饰给定的bean定义。
	 *
	 * @param node         当前子节点
	 * @param originalDef  当前 bean 定义
	 * @param containingBd 包含bean定义 (如果有)
	 * @return 装饰的bean定义
	 */
	public BeanDefinitionHolder decorateIfRequired(
			Node node, BeanDefinitionHolder originalDef, @Nullable BeanDefinition containingBd) {
		//获取命名空间URI
		String namespaceUri = getNamespaceURI(node);
		if (namespaceUri != null && !isDefaultNamespace(namespaceUri)) {
			//如果命名空间URI不为空且不是默认命名空间，则进行装饰
			//获取命名空间的处理器
			NamespaceHandler handler = this.readerContext.getNamespaceHandlerResolver().resolve(namespaceUri);
			if (handler != null) {
				BeanDefinitionHolder decorated =
						handler.decorate(node, originalDef, new ParserContext(this.readerContext, this, containingBd));
				if (decorated != null) {
					return decorated;
				}
			} else if (namespaceUri.startsWith("http://www.springframework.org/schema/")) {
				error("Unable to locate Spring NamespaceHandler for XML schema namespace [" + namespaceUri + "]", node);
			} else {
				// A custom namespace, not to be handled by Spring - maybe "xml:...".
				if (logger.isDebugEnabled()) {
					logger.debug("No Spring NamespaceHandler found for XML schema namespace [" + namespaceUri + "]");
				}
			}
		}
		return originalDef;
	}

	@Nullable
	private BeanDefinitionHolder parseNestedCustomElement(Element ele, @Nullable BeanDefinition containingBd) {
		//解析自定义元素标签
		BeanDefinition innerDefinition = parseCustomElement(ele, containingBd);
		if (innerDefinition == null) {
			//如果内部的bean定义为空,则报错
			error("Incorrect usage of element '" + ele.getNodeName() + "' in a nested manner. " +
					"This tag cannot be used nested inside <property>.", ele);
			return null;
		}
		//拼接bean定义id
		String id = ele.getNodeName() + BeanDefinitionReaderUtils.GENERATED_BEAN_NAME_SEPARATOR +
				ObjectUtils.getIdentityHexString(innerDefinition);
		if (logger.isTraceEnabled()) {
			logger.trace("Using generated bean name [" + id +
					"] for nested custom element '" + ele.getNodeName() + "'");
		}
		return new BeanDefinitionHolder(innerDefinition, id);
	}


	/**
	 * 获取提供的节点的名称空间URI。
	 * <p>
	 * 默认实现使用 {@link Node#getNamespaceURI}。子类可能会覆盖默认实现，以提供不同的名称空间标识机制。
	 *
	 * @param node 节点
	 */
	@Nullable
	public String getNamespaceURI(Node node) {
		return node.getNamespaceURI();
	}

	/**
	 * 获取提供的 {@link Node} 的本地名称。
	 * <p> 默认实现调用 {@link Node#getLocalName}。子类可能会覆盖默认实现，以提供不同的机制来获取本地名称。
	 *
	 * @param node {@code Node}节点
	 */
	public String getLocalName(Node node) {
		return node.getLocalName();
	}

	/**
	 * 确定所提供节点的名称是否等于所提供的名称。
	 * <p> 默认实现将针对 {@link Node#getNodeName()} 和 {@link Node#getLocalName()} 检查提供的所需名称。
	 * <p>子类可能会覆盖默认实现，以提供用于比较节点名称的不同机制。
	 *
	 * @param node        要比较的节点
	 * @param desiredName 要检查的名称
	 */
	public boolean nodeNameEquals(Node node, String desiredName) {
		//节点名称等于指定名称，或者是节点的本地名称等于指定的名称
		return desiredName.equals(node.getNodeName()) || desiredName.equals(getLocalName(node));
	}

	/**
	 * Determine whether the given URI indicates the default namespace.
	 */
	public boolean isDefaultNamespace(@Nullable String namespaceUri) {
		return !StringUtils.hasLength(namespaceUri) || BEANS_NAMESPACE_URI.equals(namespaceUri);
	}

	/**
	 * 确定给定节点是否是默认名称空间。
	 */
	public boolean isDefaultNamespace(Node node) {
		return isDefaultNamespace(getNamespaceURI(node));
	}

	private boolean isDefaultValue(String value) {
		//空值或者值是default，则为true，否则返回false
		return !StringUtils.hasLength(value) || DEFAULT_VALUE.equals(value);
	}

	private boolean isCandidateElement(Node node) {
		//该节点是元素节点，该节点是默认的命名空间的元素或者该节点的父节点不是默认空间的元素
		return (node instanceof Element && (isDefaultNamespace(node) || !isDefaultNamespace(node.getParentNode())));
	}

}
