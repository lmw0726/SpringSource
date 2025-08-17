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

package org.springframework.beans.factory.config;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link FactoryBean} 在给定的目标对象上计算属性路径。
 *
 * <p>目标对象可以直接指定或通过bean名称指定。
 *
 * <p>使用示例：
 *
 * <pre class="code">&lt;!-- 通过名称引用的目标bean --&gt;
 * &lt;bean id="tb" class="org.springframework.beans.TestBean" singleton="false"&gt;
 *   &lt;property name="age" value="10"/&gt;
 *   &lt;property name="spouse"&gt;
 *     &lt;bean class="org.springframework.beans.TestBean"&gt;
 *       &lt;property name="age" value="11"/&gt;
 *     &lt;/bean&gt;
 *   &lt;/property&gt;
 * &lt;/bean&gt;
 *
 * &lt;!-- 将返回12，这是内部bean的属性'age'的值 --&gt;
 * &lt;bean id="propertyPath1" class="org.springframework.beans.factory.config.PropertyPathFactoryBean"&gt;
 *   &lt;property name="targetObject"&gt;
 *     &lt;bean class="org.springframework.beans.TestBean"&gt;
 *       &lt;property name="age" value="12"/&gt;
 *     &lt;/bean&gt;
 *   &lt;/property&gt;
 *   &lt;property name="propertyPath" value="age"/&gt;
 * &lt;/bean&gt;
 *
 * &lt;!-- 将返回11，这是bean 'tb'的属性'spouse.age'的值 --&gt;
 * &lt;bean id="propertyPath2" class="org.springframework.beans.factory.config.PropertyPathFactoryBean"&gt;
 *   &lt;property name="targetBeanName" value="tb"/&gt;
 *   &lt;property name="propertyPath" value="spouse.age"/&gt;
 * &lt;/bean&gt;
 *
 * &lt;!-- 将返回10，这是bean 'tb'的属性'age'的值 --&gt;
 * &lt;bean id="tb.age" class="org.springframework.beans.factory.config.PropertyPathFactoryBean"/&gt;</pre>
 *
 * <p>如果您在配置文件中使用Spring 2.0和XML Schema支持，
 * 您还可以使用以下配置风格来访问属性路径。
 * （更多示例请参阅Spring参考手册中标题为"基于XML Schema的配置"的附录。）
 *
 * <pre class="code"> &lt;!-- 将返回10，这是bean 'tb'的属性'age'的值 --&gt;
 * &lt;util:property-path id="name" path="testBean.age"/&gt;</pre>
 *
 * 感谢Matthias Ernst的建议和初始原型！
 *
 * @author Juergen Hoeller
 * @since 1.1.2
 * @see #setTargetObject
 * @see #setTargetBeanName
 * @see #setPropertyPath
 */
public class PropertyPathFactoryBean implements FactoryBean<Object>, BeanNameAware, BeanFactoryAware {

	private static final Log logger = LogFactory.getLog(PropertyPathFactoryBean.class);

	@Nullable
	private BeanWrapper targetBeanWrapper;

	@Nullable
	private String targetBeanName;

	@Nullable
	private String propertyPath;

	@Nullable
	private Class<?> resultType;

	@Nullable
	private String beanName;

	@Nullable
	private BeanFactory beanFactory;


	/**
	 * 指定要应用属性路径的目标对象。
	 * 或者，指定目标bean名称。
	 * @param targetObject 目标对象，例如bean引用
	 * 或内部bean
	 * @see #setTargetBeanName
	 */
	public void setTargetObject(Object targetObject) {
		this.targetBeanWrapper = PropertyAccessorFactory.forBeanPropertyAccess(targetObject);
	}

	/**
	 * 指定要应用属性路径的目标bean名称。
	 * 或者，直接指定目标对象。
	 * @param targetBeanName 要在包含的bean工厂中查找的bean名称
	 * （例如 "testBean"）
	 * @see #setTargetObject
	 */
	public void setTargetBeanName(String targetBeanName) {
		this.targetBeanName = StringUtils.trimAllWhitespace(targetBeanName);
	}

	/**
	 * 指定要应用到目标的属性路径。
	 * @param propertyPath 属性路径，可能是嵌套的
	 * （例如 "age" 或 "spouse.age"）
	 */
	public void setPropertyPath(String propertyPath) {
		this.propertyPath = StringUtils.trimAllWhitespace(propertyPath);
	}

	/**
	 * 指定计算属性路径的结果类型。
	 * <p>注意：对于直接指定的目标对象或单例目标bean，这不是必需的，
	 * 因为类型可以通过内省确定。只有在原型目标的情况下才指定此项，
	 * 前提是您需要按类型匹配（例如，用于自动装配）。
	 * @param resultType 结果类型，例如 "java.lang.Integer"
	 */
	public void setResultType(Class<?> resultType) {
		this.resultType = resultType;
	}

	/**
	 * 如果既没有指定 "targetObject" 也没有指定 "targetBeanName"
	 * 或 "propertyPath"，则此 PropertyPathFactoryBean 的bean名称
	 * 将被解释为 "beanName.property" 模式。
	 * 这允许使用仅带有id/name的简洁bean定义。
	 */
	@Override
	public void setBeanName(String beanName) {
		this.beanName = StringUtils.trimAllWhitespace(BeanFactoryUtils.originalBeanName(beanName));
	}


	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;

		if (this.targetBeanWrapper != null && this.targetBeanName != null) {
			throw new IllegalArgumentException("Specify either 'targetObject' or 'targetBeanName', not both");
		}

		if (this.targetBeanWrapper == null && this.targetBeanName == null) {
			if (this.propertyPath != null) {
				throw new IllegalArgumentException(
						"Specify 'targetObject' or 'targetBeanName' in combination with 'propertyPath'");
			}

			// 未指定其他属性: 检查bean名称。
			int dotIndex = (this.beanName != null ? this.beanName.indexOf('.') : -1);
			if (dotIndex == -1) {
				throw new IllegalArgumentException(
						"Neither 'targetObject' nor 'targetBeanName' specified, and PropertyPathFactoryBean " +
						"bean name '" + this.beanName + "' does not follow 'beanName.property' syntax");
			}
			this.targetBeanName = this.beanName.substring(0, dotIndex);
			this.propertyPath = this.beanName.substring(dotIndex + 1);
		}

		else if (this.propertyPath == null) {
			// 指定targetObject或targetBeanName
			throw new IllegalArgumentException("'propertyPath' is required");
		}

		if (this.targetBeanWrapper == null && this.beanFactory.isSingleton(this.targetBeanName)) {
			// 急切地获取单例目标bean，并确定结果类型。
			Object bean = this.beanFactory.getBean(this.targetBeanName);
			this.targetBeanWrapper = PropertyAccessorFactory.forBeanPropertyAccess(bean);
			this.resultType = this.targetBeanWrapper.getPropertyType(this.propertyPath);
		}
	}


	@Override
	@Nullable
	public Object getObject() throws BeansException {
		BeanWrapper target = this.targetBeanWrapper;
		if (target != null) {
			if (logger.isWarnEnabled() && this.targetBeanName != null &&
					this.beanFactory instanceof ConfigurableBeanFactory &&
					((ConfigurableBeanFactory) this.beanFactory).isCurrentlyInCreation(this.targetBeanName)) {
				logger.warn("Target bean '" + this.targetBeanName + "' is still in creation due to a circular " +
						"reference - obtained value for property '" + this.propertyPath + "' may be outdated!");
			}
		}
		else {
			// 获取原型目标bean...
			Assert.state(this.beanFactory != null, "No BeanFactory available");
			Assert.state(this.targetBeanName != null, "No target bean name specified");
			Object bean = this.beanFactory.getBean(this.targetBeanName);
			target = PropertyAccessorFactory.forBeanPropertyAccess(bean);
		}
		Assert.state(this.propertyPath != null, "No property path specified");
		return target.getPropertyValue(this.propertyPath);
	}

	@Override
	public Class<?> getObjectType() {
		return this.resultType;
	}

	/**
	 * 虽然此 FactoryBean 通常用于单例目标，
	 * 但属性路径的调用getter可能会为每次调用返回新对象，
	 * 所以我们必须假设每次 {@link #getObject()} 调用
	 * 都不会返回相同的对象。
	 */
	@Override
	public boolean isSingleton() {
		return false;
	}

}
