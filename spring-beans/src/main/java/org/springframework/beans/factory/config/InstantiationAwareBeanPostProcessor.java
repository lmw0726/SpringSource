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

import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.lang.Nullable;

import java.beans.PropertyDescriptor;

/**
 * Subinterface of {@link BeanPostProcessor} that adds a before-instantiation callback,
 * and a callback after instantiation but before explicit properties are set or
 * autowiring occurs.
 *
 * <p>Typically used to suppress default instantiation for specific target beans,
 * for example to create proxies with special TargetSources (pooling targets,
 * lazily initializing targets, etc), or to implement additional injection strategies
 * such as field injection.
 *
 * <p><b>NOTE:</b> This interface is a special purpose interface, mainly for
 * internal use within the framework. It is recommended to implement the plain
 * {@link BeanPostProcessor} interface as far as possible.
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @see org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator#setCustomTargetSourceCreators
 * @see org.springframework.aop.framework.autoproxy.target.LazyInitTargetSourceCreator
 * @since 1.2
 */
public interface InstantiationAwareBeanPostProcessor extends BeanPostProcessor {

	/**
	 * 在目标bean被实例化 <i> 之前 </i>应用此BeanPostProcessor。
	 * 返回的bean对象可能是替代目标bean使用的代理，有效地抑制了目标bean的默认实例化。
	 * <p> 如果通过此方法返回非null对象，则bean创建过程将短路。
	 * 唯一应用的进一步处理是来自配置的 {@link BeanPostProcessor BeanPostProcessor BeanPostProcessors} 的 {@link #postProcessAfterInitialization} 回调。
	 * <p> 此回调将应用于具有bean类的bean定义以及工厂方法定义，在这种情况下，返回的bean类型将在此处传递。
	 * <p> 后处理器可以实现扩展的 {@link SmartInstantiationAwareBeanPostProcessor} 接口，以预测它们将在此处返回的bean对象的类型。
	 * <p> 默认实现返回 {@code null}。
	 *
	 * @param beanClass 要实例化的bean类
	 * @param beanName  bean名称
	 * @return 要公开的bean对象而不是目标bean的默认实例，返回 {@code null} 将继续进行默认实例化
	 * @throws org.springframework.beans.BeansException 在错误的情况下
	 * @see #postProcessAfterInstantiation
	 * @see org.springframework.beans.factory.support.AbstractBeanDefinition#getBeanClass()
	 * @see org.springframework.beans.factory.support.AbstractBeanDefinition#getFactoryMethodName()
	 */
	@Nullable
	default Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException {
		return null;
	}

	/**
	 * 在通过构造函数或工厂方法实例化bean之后，但在Spring属性填充 (从显式属性或自动执行) 发生之前，执行操作。
	 * <p> 这是在Spring的自动执行之前在给定的bean实例上执行自定义字段注入的理想回调。
	 * <p> 默认实现返回 {@code true}。
	 *
	 * @param bean     创建的bean实例，尚未设置属性
	 * @param beanName bean名称
	 * @return {@code true} 表示应该在bean上设置属性; {@code false}表示应该跳过属性填充。正常的实现应该返回 {@code true}。
	 * 返回 {@code false} 还将防止在此bean实例上调用任何后续的InstantiationAwareBeanPostProcessor实例。
	 * @throws org.springframework.beans.BeansException 在错误的情况下
	 * @see #postProcessBeforeInstantiation
	 */
	default boolean postProcessAfterInstantiation(Object bean, String beanName) throws BeansException {
		return true;
	}

	/**
	 * 在工厂将给定的属性值应用于给定的bean之前，对其进行后处理，而不需要任何属性描述符。
	 * <p> 如果实现提供自定义 {@link #postProcessPropertyValues} 实现，则应返回 {@code null} (默认值)，否则返回 {@code pvs}。
	 * 在此接口的未来版本中 (删除了 {@link #postProcessPropertyValues})，默认实现将直接返回给定的 {@code pvs}。
	 *
	 * @param pvs      工厂即将应用的属性值 (从不为 {@code null})
	 * @param bean     创建的bean实例，但其属性尚未设置
	 * @param beanName bean名称
	 * @return 要应用于给定bean的实际属性值 (可以是传入的PropertyValues实例)，或 {@code null}，其将继续使用现有属性，
	 * 但继续专门调用 {@link #postProcessPropertyValues} (需要为当前bean类初始化 {@code PropertyDescriptor})
	 * @throws org.springframework.beans.BeansException 在错误的情况下
	 * @see #postProcessPropertyValues
	 * @since 5.1
	 */
	@Nullable
	default PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName)
			throws BeansException {

		return null;
	}

	/**
	 * 在工厂将给定属性值应用于给定bean之前，对其进行后处理。
	 * 允许检查是否满足所有依赖关系，例如基于bean属性设置器上的 “必需” 注释。
	 * <p> 还允许替换要应用的属性值，通常是通过基于原始属性值创建新的MutablePropertyValues实例，添加或删除特定值。
	 * <p> 默认实现按状态返回给定的 {@code pvs}。
	 *
	 * @param pvs      工厂即将应用的属性值 (从不为 {@code null})
	 * @param pds      目标bean的相关属性描述符 (具有工厂专门处理-已经过滤掉了的被忽略的依赖类型)
	 * @param bean     创建的bean实例，但其属性尚未设置
	 * @param beanName bean名称
	 * @return 要应用于给定bean的实际属性值 (可以是传入的PropertyValues实例)，或 {@code null} 以跳过属性填充
	 * @throws org.springframework.beans.BeansException 在错误的情况下
	 * @see #postProcessProperties
	 * @see org.springframework.beans.MutablePropertyValues
	 * @deprecated as of 5.1, in favor of {@link #postProcessProperties(PropertyValues, Object, String)}
	 */
	@Deprecated
	@Nullable
	default PropertyValues postProcessPropertyValues(
			PropertyValues pvs, PropertyDescriptor[] pds, Object bean, String beanName) throws BeansException {

		return pvs;
	}

}
