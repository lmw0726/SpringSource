/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.beans.factory.config;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.lang.Nullable;
import org.springframework.util.StringValueResolver;

/**
 * 用于解析 bean 定义属性值中的占位符的属性资源配置器的抽象基类。实现从属性文件或其他属性源中将值“拉入”到 bean 定义中。
 *
 * <p>默认的占位符语法遵循 Ant / Log4J / JSP EL 风格：
 *
 * <pre class="code">${...}</pre>
 * <p>
 * 示例 XML bean 定义：
 *
 * <pre class="code">
 * &lt;bean id="dataSource" class="org.springframework.jdbc.datasource.DriverManagerDataSource"&gt;
 *   &lt;property name="driverClassName" value="${driver}" /&gt;
 *   &lt;property name="url" value="jdbc:${dbname}" /&gt;
 * &lt;/bean&gt;
 * </pre>
 * <p>
 * 示例属性文件：
 *
 * <pre class="code">
 * driver=com.mysql.jdbc.Driver
 * dbname=mysql:mydb</pre>
 * <p>
 * 注解化的 bean 定义可以利用 {@link org.springframework.beans.factory.annotation.Value @Value} 注解进行属性替换：
 *
 * <pre class="code">@Value("${person.age}")</pre>
 * <p>
 * 实现检查简单属性值、列表、映射、props 和 bean 引用中的属性。此外，占位符值也可以相互引用其他占位符，例如：
 *
 * <pre class="code">
 * rootPath=myrootdir
 * subPath=${rootPath}/subdir</pre>
 * <p>
 * 与 {@link PropertyOverrideConfigurer} 相比，此类型的子类允许填充 bean 定义中的显式占位符。
 *
 * <p>如果配置器无法解析占位符，则会抛出 {@link BeanDefinitionStoreException}。如果要针对多个属性文件进行检查，请通过 {@link #setLocations locations} 属性指定多个资源。您还可以定义多个配置器，每个配置器都有自己的占位符语法。使用 {@link #ignoreUnresolvablePlaceholders} 可以有意地抑制在无法解析占位符时抛出异常。
 *
 * <p>默认属性值可以通过 {@link #setProperties properties} 属性在每个配置器实例中全局定义，也可以使用值分隔符进行逐个属性定义，默认为 {@code ":"}，可以通过 {@link #setValueSeparator(String)} 进行自定义。
 *
 * <p>带有默认值的示例 XML 属性：
 *
 * <pre class="code">
 *   &lt;property name="url" value="jdbc:${dbname:defaultdb}" /&gt;
 * </pre>
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @see PropertyPlaceholderConfigurer
 * @see org.springframework.context.support.PropertySourcesPlaceholderConfigurer
 * @since 3.1
 */
public abstract class PlaceholderConfigurerSupport extends PropertyResourceConfigurer
		implements BeanNameAware, BeanFactoryAware {

	/**
	 * 默认占位符前缀：{@value}。
	 */
	public static final String DEFAULT_PLACEHOLDER_PREFIX = "${";

	/**
	 * 默认占位符后缀：{@value}。
	 */
	public static final String DEFAULT_PLACEHOLDER_SUFFIX = "}";

	/**
	 * 默认值分隔符：{@value}。
	 */
	public static final String DEFAULT_VALUE_SEPARATOR = ":";


	/**
	 * 默认为{@value #DEFAULT_PLACEHOLDER_PREFIX}。
	 */
	protected String placeholderPrefix = DEFAULT_PLACEHOLDER_PREFIX;

	/**
	 * 默认为{@value #DEFAULT_PLACEHOLDER_SUFFIX}。
	 */
	protected String placeholderSuffix = DEFAULT_PLACEHOLDER_SUFFIX;

	/**
	 * 默认为{@value #DEFAULT_VALUE_SEPARATOR}。
	 */
	@Nullable
	protected String valueSeparator = DEFAULT_VALUE_SEPARATOR;

	/**
	 * 是否裁剪前后的空格
	 */
	protected boolean trimValues = false;

	/**
	 * 空值
	 */
	@Nullable
	protected String nullValue;

	/**
	 * 设置是否忽略无法解析的占位符
	 */
	protected boolean ignoreUnresolvablePlaceholders = false;

	/**
	 * bean名称
	 */
	@Nullable
	private String beanName;

	/**
	 * bean工厂
	 */
	@Nullable
	private BeanFactory beanFactory;


	/**
	 * 设置占位符字符串的前缀。
	 * 默认为 {@value #DEFAULT_PLACEHOLDER_PREFIX}。
	 */
	public void setPlaceholderPrefix(String placeholderPrefix) {
		this.placeholderPrefix = placeholderPrefix;
	}

	/**
	 * 设置占位符字符串的后缀。
	 * 默认为 {@value #DEFAULT_PLACEHOLDER_SUFFIX}。
	 */
	public void setPlaceholderSuffix(String placeholderSuffix) {
		this.placeholderSuffix = placeholderSuffix;
	}

	/**
	 * 指定占位符变量与关联的默认值之间的分隔字符，如果不需要将特殊字符作为值分隔符处理，则为 {@code null}。
	 * 默认为 {@value #DEFAULT_VALUE_SEPARATOR}。
	 */
	public void setValueSeparator(@Nullable String valueSeparator) {
		this.valueSeparator = valueSeparator;
	}

	/**
	 * 指定是否在应用解析后的值之前修剪它们，从而从开头和结尾删除多余的空格。
	 * <p>默认值为 {@code false}。
	 *
	 * @since 4.3
	 */
	public void setTrimValues(boolean trimValues) {
		this.trimValues = trimValues;
	}

	/**
	 * 设置一个值，当解析为占位符值时应被视为 {@code null}：例如 ""（空字符串）或 "null"。
	 * <p>请注意，这仅适用于完整的属性值，而不适用于连接值的部分。
	 * <p>默认情况下，未定义这样的 null 值。这意味着没有办法将 {@code null} 表示为属性值，除非您在此显式映射相应的值。
	 */
	public void setNullValue(String nullValue) {
		this.nullValue = nullValue;
	}

	/**
	 * 设置是否忽略无法解析的占位符。
	 * <p>默认为 "false"：如果占位符无法解析，则会抛出异常。在这种情况下，将此标志切换为 "true"，以便在这种情况下保留占位符字符串不变，将其交给其他占位符配置器来解析。
	 */
	public void setIgnoreUnresolvablePlaceholders(boolean ignoreUnresolvablePlaceholders) {
		this.ignoreUnresolvablePlaceholders = ignoreUnresolvablePlaceholders;
	}

	/**
	 * 仅需要检查我们是否在解析自己的 bean 定义，以避免在属性文件位置中出现无法解析的占位符时失败。
	 * 后一种情况可能发生在资源位置中的系统属性占位符中。
	 *
	 * @see #setLocations
	 * @see org.springframework.core.io.ResourceEditor
	 */
	@Override
	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	/**
	 * 仅需要检查我们是否在解析自己的 bean 定义，以避免在属性文件位置中出现无法解析的占位符时失败。
	 * 后一种情况可能发生在资源位置中的系统属性占位符中。
	 *
	 * @see #setLocations
	 * @see org.springframework.core.io.ResourceEditor
	 */
	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}


	/**
	 * 处理属性。
	 *
	 * @param beanFactoryToProcess 要处理的可配置的可列出的 bean 工厂
	 * @param valueResolver        字符串值解析器
	 */
	protected void doProcessProperties(ConfigurableListableBeanFactory beanFactoryToProcess,
									   StringValueResolver valueResolver) {

		// 创建 BeanDefinitionVisitor 实例，用于访问 BeanDefinition 并解析占位符。
		BeanDefinitionVisitor visitor = new BeanDefinitionVisitor(valueResolver);
		// 获取所有 BeanDefinition 的名称数组，并遍历解析其中的占位符。
		String[] beanNames = beanFactoryToProcess.getBeanDefinitionNames();
		for (String curName : beanNames) {
			// 检查当前是否解析自身的 BeanDefinition，以避免在属性文件位置中出现无法解析的占位符。
			if (!(curName.equals(this.beanName) && beanFactoryToProcess.equals(this.beanFactory))) {
				BeanDefinition bd = beanFactoryToProcess.getBeanDefinition(curName);
				try {
					// 访问当前 BeanDefinition 并解析其中的占位符。
					visitor.visitBeanDefinition(bd);
				} catch (Exception ex) {
					// 如果解析过程中出现异常，则抛出 BeanDefinitionStoreException。
					throw new BeanDefinitionStoreException(bd.getResourceDescription(), curName, ex.getMessage(), ex);
				}
			}
		}

		// Spring 2.5 新增功能：解析别名目标名称和别名中的占位符。
		beanFactoryToProcess.resolveAliases(valueResolver);

		// Spring 3.0 新增功能：解析嵌入值中的占位符，例如注解属性。
		beanFactoryToProcess.addEmbeddedValueResolver(valueResolver);
	}

}
