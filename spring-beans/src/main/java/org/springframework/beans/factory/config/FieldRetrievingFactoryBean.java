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

package org.springframework.beans.factory.config;

import org.springframework.beans.factory.*;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;

/**
 * 一个 {@link FactoryBean}，用于获取静态或非静态字段的值。
 *
 * <p>通常用于获取公共的静态常量。使用示例：
 *
 * <pre class="code">
 * // 标准方式：通过指定 "staticField" 属性来暴露静态字段
 * &lt;bean id="myField" class="org.springframework.beans.factory.config.FieldRetrievingFactoryBean"&gt;
 *   &lt;property name="staticField" value="java.sql.Connection.TRANSACTION_SERIALIZABLE"/&gt;
 * &lt;/bean&gt;
 *
 * // 简便方式：直接将静态字段模式作为 bean 名称
 * &lt;bean id="java.sql.Connection.TRANSACTION_SERIALIZABLE"
 *       class="org.springframework.beans.factory.config.FieldRetrievingFactoryBean"/&gt;
 * </pre>
 *
 * <p>如果你使用的是 Spring 2.0，还可以采用以下配置方式来获取公共静态字段：
 *
 * <pre class="code">&lt;util:constant static-field="java.sql.Connection.TRANSACTION_SERIALIZABLE"/&gt;</pre>
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see #setStaticField
 */
public class FieldRetrievingFactoryBean
		implements FactoryBean<Object>, BeanNameAware, BeanClassLoaderAware, InitializingBean {

	@Nullable
	private Class<?> targetClass;

	@Nullable
	private Object targetObject;

	@Nullable
	private String targetField;

	@Nullable
	private String staticField;

	@Nullable
	private String beanName;

	@Nullable
	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	// 我们将要获取的字段
	@Nullable
	private Field fieldObject;


	/**
	 * 设置字段所在的目标类。
	 * 仅当目标字段为静态字段时才需要；否则，需要指定目标对象。
	 * @see #setTargetObject
	 * @see #setTargetField
	 */
	public void setTargetClass(@Nullable Class<?> targetClass) {
		this.targetClass = targetClass;
	}

	/**
	 * 返回字段所在的目标类。
	 */
	@Nullable
	public Class<?> getTargetClass() {
		return this.targetClass;
	}

	/**
	 * 设置字段所在的目标对象。
	 * 仅当目标字段为非静态字段时才需要；否则，指定目标类即可。
	 * @see #setTargetClass
	 * @see #setTargetField
	 */
	public void setTargetObject(@Nullable Object targetObject) {
		this.targetObject = targetObject;
	}

	/**
	 * 返回字段所在的目标对象。
	 */
	@Nullable
	public Object getTargetObject() {
		return this.targetObject;
	}

	/**
	 * 设置要获取的字段名。
	 * 可以是静态字段或非静态字段，具体取决于是否设置了目标对象。
	 * @see #setTargetClass
	 * @see #setTargetObject
	 */
	public void setTargetField(@Nullable String targetField) {
		this.targetField = (targetField != null ? StringUtils.trimAllWhitespace(targetField) : null);
	}

	/**
	 * 返回要获取的字段名。
	 */
	@Nullable
	public String getTargetField() {
		return this.targetField;
	}

	/**
	 * 设置要获取的完全限定静态字段名，例如：
	 * "example.MyExampleClass.MY_EXAMPLE_FIELD"。
	 * 这是指定 targetClass 和 targetField 的便捷替代方式。
	 * @see #setTargetClass
	 * @see #setTargetField
	 */
	public void setStaticField(String staticField) {
		this.staticField = StringUtils.trimAllWhitespace(staticField);
	}

	/**
	 * 如果没有指定 "targetClass"、"targetObject" 或 "targetField"，
	 * 则该 FieldRetrievingFactoryBean 的 bean 名称将被解释为 "staticField" 模式。
	 * 这允许仅通过 id/name 来简洁地定义 bean。
	 */
	@Override
	public void setBeanName(String beanName) {
		this.beanName = StringUtils.trimAllWhitespace(BeanFactoryUtils.originalBeanName(beanName));
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}


	@Override
	public void afterPropertiesSet() throws ClassNotFoundException, NoSuchFieldException {
		if (this.targetClass != null && this.targetObject != null) {
			throw new IllegalArgumentException("Specify either targetClass or targetObject, not both");
		}

		if (this.targetClass == null && this.targetObject == null) {
			if (this.targetField != null) {
				throw new IllegalArgumentException(
						"Specify targetClass or targetObject in combination with targetField");
			}

			// 如果没有指定其他属性，则将bean名称视为静态字段表达式。
			if (this.staticField == null) {
				this.staticField = this.beanName;
				Assert.state(this.staticField != null, "No target field specified");
			}

			// 尝试将静态字段解析为类和字段。
			int lastDotIndex = this.staticField.lastIndexOf('.');
			if (lastDotIndex == -1 || lastDotIndex == this.staticField.length()) {
				throw new IllegalArgumentException(
						"staticField must be a fully qualified class plus static field name: " +
						"e.g. 'example.MyExampleClass.MY_EXAMPLE_FIELD'");
			}
			String className = this.staticField.substring(0, lastDotIndex);
			String fieldName = this.staticField.substring(lastDotIndex + 1);
			this.targetClass = ClassUtils.forName(className, this.beanClassLoader);
			this.targetField = fieldName;
		}

		else if (this.targetField == null) {
			// 指定targetClass或targetObject。
			throw new IllegalArgumentException("targetField is required");
		}

		// 首先尝试获取确切的方法。
		Class<?> targetClass = (this.targetObject != null ? this.targetObject.getClass() : this.targetClass);
		this.fieldObject = targetClass.getField(this.targetField);
	}


	@Override
	@Nullable
	public Object getObject() throws IllegalAccessException {
		if (this.fieldObject == null) {
			throw new FactoryBeanNotInitializedException();
		}
		ReflectionUtils.makeAccessible(this.fieldObject);
		if (this.targetObject != null) {
			// 实例字段
			return this.fieldObject.get(this.targetObject);
		}
		else {
			// 类字段
			return this.fieldObject.get(null);
		}
	}

	@Override
	public Class<?> getObjectType() {
		return (this.fieldObject != null ? this.fieldObject.getType() : null);
	}

	@Override
	public boolean isSingleton() {
		return false;
	}

}
