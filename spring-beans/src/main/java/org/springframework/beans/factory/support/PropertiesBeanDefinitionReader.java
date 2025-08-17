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

package org.springframework.beans.factory.support;

import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyAccessor;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.CannotLoadBeanClassException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.ResourcePropertiesPersister;
import org.springframework.lang.Nullable;
import org.springframework.util.PropertiesPersister;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

/**
 * 用于简单属性格式的 Bean 定义读取器。
 *
 * <p>为 Map/Properties 和 ResourceBundle 提供 Bean 定义注册方法。
 * 通常应用于 DefaultListableBeanFactory。
 *
 * <p><b>示例：</b>
 *
 * <pre class="code">
 * employee.(class)=MyClass       // bean 的类为 MyClass
 * employee.(abstract)=true       // 此 bean 无法直接实例化
 * employee.group=Insurance       // 真正的属性
 * employee.usesDialUp=false      // 真正的属性（可能被覆盖）
 *
 * salesrep.(parent)=employee     // 派生自 "employee" bean 定义
 * salesrep.(lazy-init)=true      // 延迟初始化此单例 bean
 * salesrep.manager(ref)=tony     // 引用另一个 bean
 * salesrep.department=Sales      // 真正的属性
 *
 * techie.(parent)=employee       // 派生自 "employee" bean 定义
 * techie.(scope)=prototype       // bean 为原型（非共享实例）
 * techie.manager(ref)=jeff       // 引用另一个 bean
 * techie.department=Engineering  // 真正的属性
 * techie.usesDialUp=true         // 真正的属性（覆盖父值）
 *
 * ceo.$0(ref)=secretary          // 将 'secretary' bean 注入第 0 个构造参数
 * ceo.$1=1000000                 // 将值 '1000000' 注入第 1 个构造参数
 * </pre>
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @see DefaultListableBeanFactory
 * @since 26.11.2003
 * @deprecated 自 5.3 起，建议使用 Spring 通用 Bean 定义格式
 * 和/或自定义读取器实现
 */
@Deprecated
public class PropertiesBeanDefinitionReader extends AbstractBeanDefinitionReader {

	/**
	 * 表示 true 的 T/F 属性值。
	 * 其他值表示 false。大小写敏感。
	 */
	public static final String TRUE_VALUE = "true";

	/**
	 * bean 名称与属性名称之间的分隔符。
	 * 我们遵循常规 Java 命名约定。
	 */
	public static final String SEPARATOR = ".";

	/**
	 * 特殊键来区分 {@code owner.(class)=com.myapp.MyClass}。
	 */
	public static final String CLASS_KEY = "(class)";

	/**
	 * 特殊键来区分 {@code owner.(parent)=parentBeanName}。
	 */
	public static final String PARENT_KEY = "(parent)";

	/**
	 * 特殊键来区分 {@code owner.(scope)=prototype}.
	 * 默认值是 "true".
	 */
	public static final String SCOPE_KEY = "(scope)";

	/**
	 * 特殊键来区分 {@code owner.(singleton)=false}.
	 * 默认值是 "true".
	 */
	public static final String SINGLETON_KEY = "(singleton)";

	/**
	 * 特殊键来区分 {@code owner.(abstract)=true}
	 * 默认是false
	 */
	public static final String ABSTRACT_KEY = "(abstract)";

	/**
	 * 特殊键来区分  {@code owner.(lazy-init)=true}
	 * 默认是false
	 */
	public static final String LAZY_INIT_KEY = "(lazy-init)";

	/**
	 * 当前BeanFactory中引用其他bean的属性后缀: 例如 {@code owner.dog(ref)=fido}。
	 * 这是对单例还是原型的引用将取决于目标bean的定义。
	 */
	public static final String REF_SUFFIX = "(ref)";

	/**
	 * 引用其他bean的值之前的前缀。
	 */
	public static final String REF_PREFIX = "*";

	/**
	 * 用于表示构造函数参数定义的前缀。
	 */
	public static final String CONSTRUCTOR_ARG_PREFIX = "$";


	@Nullable
	private String defaultParentBean;

	private PropertiesPersister propertiesPersister = ResourcePropertiesPersister.INSTANCE;


	/**
	 * 为给定的 bean 工厂创建新的 PropertiesBeanDefinitionReader。
	 *
	 * @param registry 要加载 bean 定义的 BeanFactory，
	 *                 以 BeanDefinitionRegistry 的形式提供
	 */
	public PropertiesBeanDefinitionReader(BeanDefinitionRegistry registry) {
		super(registry);
	}


	/**
	 * 为此 bean 工厂设置默认父 bean。
	 * 如果由此工厂处理的子 bean 定义既没有提供 parent，也没有 class 属性，则使用此默认值。
	 * <p>例如可用于视图定义文件，为所有视图定义具有默认视图类和通用属性的父级。
	 * 自定义父级或自带 class 的视图定义仍可覆盖此设置。
	 * <p>严格来说，默认父设置不适用于带有 class 的 bean 定义，这一规则是出于向后兼容性考虑。
	 * 但它仍然符合典型使用场景。
	 */
	public void setDefaultParentBean(@Nullable String defaultParentBean) {
		this.defaultParentBean = defaultParentBean;
	}

	/**
	 * 返回此 bean 工厂的默认父 bean。
	 */
	@Nullable
	public String getDefaultParentBean() {
		return this.defaultParentBean;
	}

	/**
	 * 设置用于解析 properties 文件的 PropertiesPersister。
	 * 默认使用 ResourcePropertiesPersister。
	 *
	 * @see ResourcePropertiesPersister#INSTANCE
	 */
	public void setPropertiesPersister(@Nullable PropertiesPersister propertiesPersister) {
		this.propertiesPersister =
				(propertiesPersister != null ? propertiesPersister : ResourcePropertiesPersister.INSTANCE);
	}

	/**
	 * 返回用于解析 properties 文件的 PropertiesPersister。
	 */
	public PropertiesPersister getPropertiesPersister() {
		return this.propertiesPersister;
	}


	/**
	 * 从指定的属性文件加载bean定义，使用所有属性键 (即不按前缀过滤)。
	 *
	 * @param resource 属性文件的资源描述符
	 * @return 找到的bean定义的数量
	 * @throws BeanDefinitionStoreException 在加载或解析错误的情况下
	 * @see #loadBeanDefinitions(org.springframework.core.io.Resource, String)
	 */
	@Override
	public int loadBeanDefinitions(Resource resource) throws BeanDefinitionStoreException {
		return loadBeanDefinitions(new EncodedResource(resource), null);
	}

	/**
	 * 从指定的属性文件中加载 Bean 定义。
	 *
	 * @param resource  属性文件的资源描述符
	 * @param prefix    键名的过滤前缀，例如 'beans.'（可以为空或 {@code null}）
	 * @return          找到的 Bean 定义的数量
	 * @throws BeanDefinitionStoreException 如果发生加载或解析错误
	 */
	public int loadBeanDefinitions(Resource resource, @Nullable String prefix) throws BeanDefinitionStoreException {
		return loadBeanDefinitions(new EncodedResource(resource), prefix);
	}

	/**
	 * 从指定的属性文件中加载 Bean 定义。
	 *
	 * @param encodedResource 属性文件的资源描述符，允许指定用于解析文件的编码格式
	 * @return                找到的 Bean 定义的数量
	 * @throws BeanDefinitionStoreException 如果发生加载或解析错误
	 */
	public int loadBeanDefinitions(EncodedResource encodedResource) throws BeanDefinitionStoreException {
		return loadBeanDefinitions(encodedResource, null);
	}

	/**
	 * 从指定的属性文件加载bean定义。
	 *
	 * @param encodedResource 属性文件的资源描述符，允许指定用于解析文件的编码
	 * @param prefix          映射中的键中的过滤器: 例如 “beans”。(可以为空或 {@code null})
	 * @return 找到的bean定义的数量
	 * @throws BeanDefinitionStoreException 在加载或解析错误的情况下
	 */
	public int loadBeanDefinitions(EncodedResource encodedResource, @Nullable String prefix)
			throws BeanDefinitionStoreException {

		if (logger.isTraceEnabled()) {
			logger.trace("Loading properties bean definitions from " + encodedResource);
		}
		//新建一个Properties，类似与Map结构
		Properties props = new Properties();
		try {
			try (InputStream is = encodedResource.getResource().getInputStream()) {
				if (encodedResource.getEncoding() != null) {

					//根据资源构建输入资源读取器
					InputStreamReader reader = new InputStreamReader(is, encodedResource.getEncoding());
					getPropertiesPersister().load(props, reader);
				} else {
					getPropertiesPersister().load(props, is);
				}
			}
			//根据属性值，前缀，以及资源描述注册Bean定义
			int count = registerBeanDefinitions(props, prefix, encodedResource.getResource().getDescription());
			if (logger.isDebugEnabled()) {
				logger.debug("Loaded " + count + " bean definitions from " + encodedResource);
			}
			return count;
		} catch (IOException ex) {
			throw new BeanDefinitionStoreException("Could not parse properties from " + encodedResource.getResource(), ex);
		}
	}

	/**
	 * 注册资源包（ResourceBundle）中包含的 Bean 定义，
	 * 使用所有属性键（即不通过前缀进行过滤）。
	 *
	 * @param rb 要从中加载的 ResourceBundle
	 * @return 找到的 Bean 定义的数量
	 * @throws BeanDefinitionStoreException 如果发生加载或解析错误
	 * @see #registerBeanDefinitions(java.util.ResourceBundle, String)
	 */
	public int registerBeanDefinitions(ResourceBundle rb) throws BeanDefinitionStoreException {
		return registerBeanDefinitions(rb, null);
	}

	/**
	 * 注册 ResourceBundle 中包含的 Bean 定义。
	 * <p>语法与 Map 类似。该方法有助于启用标准的 Java 国际化支持。
	 *
	 * @param rb     要从中加载的 ResourceBundle
	 * @param prefix 键名的过滤前缀，例如 'beans.'（可以为空或 {@code null}）
	 * @return 找到的 Bean 定义的数量
	 * @throws BeanDefinitionStoreException 如果发生加载或解析错误
	 */
	public int registerBeanDefinitions(ResourceBundle rb, @Nullable String prefix) throws BeanDefinitionStoreException {
		// 简单创建一个 Map 并调用重载方法
		Map<String, Object> map = new HashMap<>();
		Enumeration<String> keys = rb.getKeys();
		while (keys.hasMoreElements()) {
			String key = keys.nextElement();
			map.put(key, rb.getObject(key));
		}
		return registerBeanDefinitions(map, prefix);
	}


	/**
	 * 注册 Map 中包含的 Bean 定义，使用所有属性键（即不通过前缀进行过滤）。
	 *
	 * @param map 一个从 {@code 名称} 到 {@code 属性}（String 或 Object）的映射。
	 *            如果属性值来自 Properties 文件等，则值为字符串形式。
	 *            属性名称（键）<b>必须</b>是字符串，类对应的键也必须是字符串。
	 * @return 找到的 Bean 定义的数量
	 * @throws BeansException 如果发生加载或解析错误
	 * @see #registerBeanDefinitions(java.util.Map, String, String)
	 */
	public int registerBeanDefinitions(Map<?, ?> map) throws BeansException {
		return registerBeanDefinitions(map, null);
	}

	/**
	 * 注册 Map 中包含的 Bean 定义。
	 * 忽略不符合条件的属性。
	 *
	 * @param map    一个从 {@code 名称} 到 {@code 属性}（String 或 Object）的映射。
	 *               如果属性值来自 Properties 文件等，则值为字符串形式。
	 *               属性名称（键）<b>必须</b>是字符串，类对应的键也必须是字符串。
	 * @param prefix 键名的过滤前缀，例如 'beans.'（可以为空或 {@code null}）
	 * @return 找到的 Bean 定义的数量
	 * @throws BeansException 如果发生加载或解析错误
	 */
	public int registerBeanDefinitions(Map<?, ?> map, @Nullable String prefix) throws BeansException {
		return registerBeanDefinitions(map, prefix, "Map " + map);
	}

	/**
	 * 注册包含在Map中的Bean定义，忽略不合格的属性。
	 *
	 * @param map                 {@code name} 到 {@code property} (字符串或对象) 的映射。
	 *                            属性值将是字符串，如果来自属性文件等。属性名称 (键) <b> 必须 <b> 是字符串。
	 *                            类键必须是字符串。
	 * @param prefix              Map中的键中的过滤器: 例如 “beans”。(可以为空或 {@code null})
	 * @param resourceDescription 来自Map(用于日志记录)中的资源描述
	 * @return 找到了bean定义的数量
	 * @throws BeansException 在加载或解析错误的情况下
	 * @see #registerBeanDefinitions(Map, String)
	 */
	public int registerBeanDefinitions(Map<?, ?> map, @Nullable String prefix, String resourceDescription)
			throws BeansException {

		if (prefix == null) {
			prefix = "";
		}
		int beanCount = 0;

		for (Object key : map.keySet()) {
			if (!(key instanceof String)) {
				//如果Key不是字符串类型，抛出IllegalArgumentException
				throw new IllegalArgumentException("Illegal key [" + key + "]: only Strings allowed");
			}
			String keyString = (String) key;
			if (keyString.startsWith(prefix)) {
				//如果key的前缀是指定字符串
				// Key的形式为: 前缀 <name>。属性
				//获取前缀后的字符串，作为属性名称
				String nameAndProperty = keyString.substring(prefix.length());
				//在属性名称前查找点，忽略属性键中的点。
				int sepIdx;
				//如果查找属性名称中 [ 符号
				int propKeyIdx = nameAndProperty.indexOf(PropertyAccessor.PROPERTY_KEY_PREFIX);
				if (propKeyIdx != -1) {
					//如果含有 [ 符号，获取该符号之前的最后一个 . 符号
					sepIdx = nameAndProperty.lastIndexOf(SEPARATOR, propKeyIdx);
				} else {
					//否则获取最后一个 . 符号
					sepIdx = nameAndProperty.lastIndexOf(SEPARATOR);
				}
				if (sepIdx != -1) {
					//如果含有 . 符号，获取 . 符号之前的字符串作为bean名称
					String beanName = nameAndProperty.substring(0, sepIdx);
					if (logger.isTraceEnabled()) {
						logger.trace("Found bean name '" + beanName + "'");
					}
					if (!getRegistry().containsBeanDefinition(beanName)) {
						//如果已注册的bean定义中，没有该bean名称，注册该bean名称和属性
						registerBeanDefinition(beanName, map, prefix + beanName, resourceDescription);
						++beanCount;
					}
				} else {
					//忽略它: 它不是一个有效的bean名称和属性，尽管它确实以所需的前缀开头。
					if (logger.isDebugEnabled()) {
						logger.debug("Invalid bean name and property [" + nameAndProperty + "]");
					}
				}
			}
		}

		return beanCount;
	}

	/**
	 * 获取所有属性值，给定一个前缀 (将被剥离)，并将它们定义的bean添加到具有给定名称的工厂中。
	 *
	 * @param beanName            要定义的bean名称
	 * @param map                 包含字符串对的Map
	 * @param prefix              每个条目的前缀，将被剥离
	 * @param resourceDescription 来自Map(用于日志记录)中的资源描述
	 * @throws BeansException 如果无法解析或注册bean定义
	 */
	protected void registerBeanDefinition(String beanName, Map<?, ?> map, String prefix, String resourceDescription)
			throws BeansException {
		//类名
		String className = null;
		String parent = null;
		//Bean的范围
		String scope = BeanDefinition.SCOPE_SINGLETON;
		//是否是抽象的
		boolean isAbstract = false;
		//bean是否懒加载
		boolean lazyInit = false;

		//创建一个空的构造器参数值实例
		ConstructorArgumentValues cas = new ConstructorArgumentValues();
		//创建一个空的可变的属性值实例
		MutablePropertyValues pvs = new MutablePropertyValues();
		//带有 . 符号的前缀
		String prefixWithSep = prefix + SEPARATOR;
		//开始索引
		int beginIndex = prefixWithSep.length();

		for (Map.Entry<?, ?> entry : map.entrySet()) {
			//去除所有的空白字符，作为key
			String key = StringUtils.trimWhitespace((String) entry.getKey());
			if (key.startsWith(prefixWithSep)) {
				//如果是以有 . 符号的前缀开头，获取 . 符号后的字符作为属性值
				String property = key.substring(beginIndex);
				if (CLASS_KEY.equals(property)) {
					//如果属性值是(class)，将对应的value值去除空白字符后，就是属性名了
					className = StringUtils.trimWhitespace((String) entry.getValue());
				} else if (PARENT_KEY.equals(property)) {
					//如果属性值是(parent)，将对应的value值去除空白字符后，就是父级了
					parent = StringUtils.trimWhitespace((String) entry.getValue());
				} else if (ABSTRACT_KEY.equals(property)) {
					//如果属性值是(abstract)，将对应的value值去除空白字符后，就是抽象属性值了
					String val = StringUtils.trimWhitespace((String) entry.getValue());
					//判断该属性为true
					isAbstract = TRUE_VALUE.equals(val);
				} else if (SCOPE_KEY.equals(property)) {
					// Spring 2.0 style
					//如果属性值是(scope)，将对应的value值去除空白字符后，就是作用域
					scope = StringUtils.trimWhitespace((String) entry.getValue());
				} else if (SINGLETON_KEY.equals(property)) {
					// Spring 1.2 style
					//如果属性值是(singleton)，将对应的value值去除空白字符后，就是作用域
					String val = StringUtils.trimWhitespace((String) entry.getValue());
					//如果作用域没有有值，或者该值为true，则是单例，否则为为原型
					scope = (!StringUtils.hasLength(val) || TRUE_VALUE.equals(val) ?
							BeanDefinition.SCOPE_SINGLETON : BeanDefinition.SCOPE_PROTOTYPE);
				} else if (LAZY_INIT_KEY.equals(property)) {
					//是否懒加载
					//如果属性值是(lazy-init)，将对应的value值去除空白字符后，就是懒加载值了
					String val = StringUtils.trimWhitespace((String) entry.getValue());
					//值为true启用懒加载
					lazyInit = TRUE_VALUE.equals(val);
				} else if (property.startsWith(CONSTRUCTOR_ARG_PREFIX)) {
					//如果以$开头
					if (property.endsWith(REF_SUFFIX)) {
						//如果该属性值以 (ref) 结尾
						//截取第一个字符到(ref)字符串之间的字符窜
						String substring = property.substring(1, property.length() - REF_SUFFIX.length());
						//将其转为数字类型
						int index = Integer.parseInt(substring);
						//将该索引和属性值构成的RuntimeBeanReference添加到参数实例中
						cas.addIndexedArgumentValue(index, new RuntimeBeanReference(entry.getValue().toString()));
					} else {
						//不是以$开头，将该属性值截取掉首个字符后，转成int类型
						int index = Integer.parseInt(property.substring(1));
						//将该索引和读到的值添加到参数实例中
						cas.addIndexedArgumentValue(index, readValue(entry));
					}
				} else if (property.endsWith(REF_SUFFIX)) {
					// 这不是真实的属性，而是对另一个原型提取属性名称的引用: 属性是狗的形式(ref)
					//获取0-(ref)之间的字符串
					property = property.substring(0, property.length() - REF_SUFFIX.length());
					//去除所有空格
					String ref = StringUtils.trimWhitespace((String) entry.getValue());

					// 引用的bean是否尚未注册并不重要: 这将确保引用在运行时被解析。
					Object val = new RuntimeBeanReference(ref);
					pvs.add(property, val);
				} else {
					//这是正常的bean属性。
					pvs.add(property, readValue(entry));
				}
			}
		}

		if (logger.isTraceEnabled()) {
			logger.trace("Registering bean definition for bean name '" + beanName + "' with " + pvs);
		}

		// 如果我们不处理父级本身，并且没有指定类名，则只需使用默认父级。由于向后兼容的原因，后者必须发生。
		if (parent == null && className == null && !beanName.equals(this.defaultParentBean)) {
			//如果父级为空且类名为空，且bean名称不是默认父级bean，则将父级设置为默认的父级Bean
			parent = this.defaultParentBean;
		}

		try {
			//根据父级、类名、类加载器创建Bean定义
			AbstractBeanDefinition bd = BeanDefinitionReaderUtils.createBeanDefinition(
					parent, className, getBeanClassLoader());
			//设置Bean定义的相关信息：范围、是否是抽象类、是否懒加载、构造参数值对、可变的属性值
			bd.setScope(scope);
			bd.setAbstract(isAbstract);
			bd.setLazyInit(lazyInit);
			bd.setConstructorArgumentValues(cas);
			bd.setPropertyValues(pvs);
			//注册Bean定义
			getRegistry().registerBeanDefinition(beanName, bd);
		} catch (ClassNotFoundException ex) {
			throw new CannotLoadBeanClassException(resourceDescription, beanName, className, ex);
		} catch (LinkageError err) {
			throw new CannotLoadBeanClassException(resourceDescription, beanName, className, err);
		}
	}

	/**
	 * 读取条目的值。正确解释以星号为前缀的值的bean引用。
	 */
	private Object readValue(Map.Entry<?, ?> entry) {
		Object val = entry.getValue();
		if (val instanceof String) {
			//如果是String类型，将value转为String类型
			String strVal = (String) val;
			// 如果它以引用前缀开头...
			if (strVal.startsWith(REF_PREFIX)) {
				// 展开引用。
				String targetName = strVal.substring(1);
				if (targetName.startsWith(REF_PREFIX)) {
					//如果是以*开头
					//转义前缀-> 使用纯值。
					val = targetName;
				} else {
					//构造成RuntimeBeanReference返回
					val = new RuntimeBeanReference(targetName);
				}
			}
		}
		return val;
	}

}
