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

package org.springframework.beans.factory.parsing;

import org.springframework.beans.PropertyValue;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.BeanReference;
import org.springframework.lang.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * ComponentDefinition based on a standard BeanDefinition, exposing the given bean
 * definition as well as inner bean definitions and bean references for the given bean.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 */
public class BeanComponentDefinition extends BeanDefinitionHolder implements ComponentDefinition {

	/**
	 * 内部的bean定义
	 */
	private BeanDefinition[] innerBeanDefinitions;

	/**
	 * bean引用数组
	 */
	private BeanReference[] beanReferences;


	/**
	 * 为给定的bean创建一个新的BeanComponentDefinition
	 *
	 * @param beanDefinition bean定义
	 * @param beanName       bean名称
	 */
	public BeanComponentDefinition(BeanDefinition beanDefinition, String beanName) {
		this(new BeanDefinitionHolder(beanDefinition, beanName));
	}

	/**
	 * 为给定的bean创建一个新的BeanComponentDefinition
	 *
	 * @param beanDefinition bean定义
	 * @param beanName       bean名称
	 * @param aliases        bean的别名，如果没有，则为 {@code null}
	 */
	public BeanComponentDefinition(BeanDefinition beanDefinition, String beanName, @Nullable String[] aliases) {
		this(new BeanDefinitionHolder(beanDefinition, beanName, aliases));
	}

	/**
	 * 为给定的bean创建一个新的BeanComponentDefinition
	 *
	 * @param beanDefinitionHolder 封装bean定义以及bean名称的bean 定义持有者
	 */
	public BeanComponentDefinition(BeanDefinitionHolder beanDefinitionHolder) {
		super(beanDefinitionHolder);

		List<BeanDefinition> innerBeans = new ArrayList<>();
		List<BeanReference> references = new ArrayList<>();
		//获取bean定义的属性值
		PropertyValues propertyValues = beanDefinitionHolder.getBeanDefinition().getPropertyValues();
		for (PropertyValue propertyValue : propertyValues.getPropertyValues()) {
			Object value = propertyValue.getValue();
			if (value instanceof BeanDefinitionHolder) {
				//如果值是BeanDefinitionHolder对象，获取他的bean定义，再添加进内部bean列表中。
				innerBeans.add(((BeanDefinitionHolder) value).getBeanDefinition());
			} else if (value instanceof BeanDefinition) {
				//如果值是bean定义，将其添加进内部bean列表中。
				innerBeans.add((BeanDefinition) value);
			} else if (value instanceof BeanReference) {
				//如果值是bean引用，添加到bean引用列表中
				references.add((BeanReference) value);
			}
		}
		this.innerBeanDefinitions = innerBeans.toArray(new BeanDefinition[0]);
		this.beanReferences = references.toArray(new BeanReference[0]);
	}


	@Override
	public String getName() {
		return getBeanName();
	}

	@Override
	public String getDescription() {
		return getShortDescription();
	}

	@Override
	public BeanDefinition[] getBeanDefinitions() {
		return new BeanDefinition[]{getBeanDefinition()};
	}

	@Override
	public BeanDefinition[] getInnerBeanDefinitions() {
		return this.innerBeanDefinitions;
	}

	@Override
	public BeanReference[] getBeanReferences() {
		return this.beanReferences;
	}


	/**
	 * 此实现返回此ComponentDefinition的描述。
	 *
	 * @see #getDescription()
	 */
	@Override
	public String toString() {
		return getDescription();
	}

	/**
	 * 除了超类的相等要求外，此实现还希望另一个对象也具有BeanComponentDefinition类型。
	 */
	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof BeanComponentDefinition && super.equals(other)));
	}

}
