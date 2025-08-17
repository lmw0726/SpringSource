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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyEditorRegistrar;
import org.springframework.core.Ordered;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

import java.beans.PropertyEditor;
import java.util.Map;

/**
 * {@link BeanFactoryPostProcessor} 实现，便于注册自定义的 {@link PropertyEditor 属性编辑器}。
 *
 * <p>如果需要注册 {@link PropertyEditor} 实例，Spring 2.0 推荐的做法是
 * 使用自定义的 {@link PropertyEditorRegistrar} 实现，这些实现会在给定的
 * {@link org.springframework.beans.PropertyEditorRegistry registry} 上注册所需的编辑器实例。
 * 每个 PropertyEditorRegistrar 可以注册任意数量的自定义编辑器。
 *
 * <pre class="code">
 * &lt;bean id="customEditorConfigurer" class="org.springframework.beans.factory.config.CustomEditorConfigurer"&gt;
 *   &lt;property name="propertyEditorRegistrars"&gt;
 *     &lt;list&gt;
 *       &lt;bean class="mypackage.MyCustomDateEditorRegistrar"/&gt;
 *       &lt;bean class="mypackage.MyObjectEditorRegistrar"/&gt;
 *     &lt;/list&gt;
 *   &lt;/property&gt;
 * &lt;/bean&gt;
 * </pre>
 *
 * <p>
 * 通过 {@code customEditors} 属性注册 {@link PropertyEditor} <em>类</em> 也是完全可行的。
 * Spring 会在每次编辑尝试时创建它们的新实例：
 *
 * <pre class="code">
 * &lt;bean id="customEditorConfigurer" class="org.springframework.beans.factory.config.CustomEditorConfigurer"&gt;
 *   &lt;property name="customEditors"&gt;
 *     &lt;map&gt;
 *       &lt;entry key="java.util.Date" value="mypackage.MyCustomDateEditor"/&gt;
 *       &lt;entry key="mypackage.MyObject" value="mypackage.MyObjectEditor"/&gt;
 *     &lt;/map&gt;
 *   &lt;/property&gt;
 * &lt;/bean&gt;
 * </pre>
 *
 * <p>
 * 注意，不应通过 {@code customEditors} 属性注册 {@link PropertyEditor} bean 实例，
 * 因为 {@link PropertyEditor PropertyEditors} 是有状态的，这样实例在每次编辑尝试时都必须同步。
 * 如果需要控制 {@link PropertyEditor PropertyEditors} 的实例化过程，请使用 {@link PropertyEditorRegistrar} 注册它们。
 *
 * <p>
 * 还支持 "java.lang.String[]"-风格的数组类名和基本类型类名（如 "boolean"）。
 * 实际的类名解析由 {@link ClassUtils} 委派完成。
 *
 * <p><b>注意：</b> 使用此配置器注册的自定义属性编辑器 <i>不适用于数据绑定</i>。
 * 数据绑定的自定义编辑器需要注册到 {@link org.springframework.validation.DataBinder}：
 * 可使用公共基类或委派给通用的 PropertyEditorRegistrar 实现以复用编辑器注册。
 *
 * @author Juergen Hoeller
 * @since 27.02.2004
 * @see java.beans.PropertyEditor
 * @see org.springframework.beans.PropertyEditorRegistrar
 * @see ConfigurableBeanFactory#addPropertyEditorRegistrar
 * @see ConfigurableBeanFactory#registerCustomEditor
 * @see org.springframework.validation.DataBinder#registerCustomEditor
 */
public class CustomEditorConfigurer implements BeanFactoryPostProcessor, Ordered {

	protected final Log logger = LogFactory.getLog(getClass());

	private int order = Ordered.LOWEST_PRECEDENCE;  // 默认值：与非 Ordered 相同

	@Nullable
	private PropertyEditorRegistrar[] propertyEditorRegistrars;

	@Nullable
	private Map<Class<?>, Class<? extends PropertyEditor>> customEditors;


	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	/**
	 * 指定要应用于当前应用上下文中定义的 bean 的 {@link PropertyEditorRegistrar PropertyEditorRegistrars}。
	 * <p>这允许与 {@link org.springframework.validation.DataBinder DataBinders} 等共享 {@code PropertyEditorRegistrars}。
	 * 此外，它避免了自定义编辑器的同步需求：
	 * {@code PropertyEditorRegistrar} 会为每次 bean 创建尝试生成新的编辑器实例。
	 * @see ConfigurableListableBeanFactory#addPropertyEditorRegistrar
	 */
	public void setPropertyEditorRegistrars(PropertyEditorRegistrar[] propertyEditorRegistrars) {
		this.propertyEditorRegistrars = propertyEditorRegistrars;
	}

	/**
	 * 指定通过 {@link Map} 注册的自定义编辑器，
	 * 使用所需类型的类名作为 key，对应 {@link PropertyEditor} 的类名作为 value。
	 * @see ConfigurableListableBeanFactory#registerCustomEditor
	 */
	public void setCustomEditors(Map<Class<?>, Class<? extends PropertyEditor>> customEditors) {
		this.customEditors = customEditors;
	}


	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		if (this.propertyEditorRegistrars != null) {
			for (PropertyEditorRegistrar propertyEditorRegistrar : this.propertyEditorRegistrars) {
				beanFactory.addPropertyEditorRegistrar(propertyEditorRegistrar);
			}
		}
		if (this.customEditors != null) {
			this.customEditors.forEach(beanFactory::registerCustomEditor);
		}
	}

}
