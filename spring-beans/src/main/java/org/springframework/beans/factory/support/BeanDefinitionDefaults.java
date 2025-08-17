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

import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * 用于 {@code BeanDefinition} 属性默认值的简单持有者。
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @since 2.5
 * @see AbstractBeanDefinition#applyDefaults
 */
public class BeanDefinitionDefaults {

	@Nullable
	private Boolean lazyInit;

	private int autowireMode = AbstractBeanDefinition.AUTOWIRE_NO;

	private int dependencyCheck = AbstractBeanDefinition.DEPENDENCY_CHECK_NONE;

	@Nullable
	private String initMethodName;

	@Nullable
	private String destroyMethodName;


	/**
	 * 设置是否默认延迟初始化 bean。
	 * <p>如果为 {@code false}，则执行单例预初始化的 bean 工厂会在启动时实例化该 bean。
	 * @see AbstractBeanDefinition#setLazyInit
	 */
	public void setLazyInit(boolean lazyInit) {
		this.lazyInit = lazyInit;
	}

	/**
	 * 返回是否默认延迟初始化 bean，即在启动时不立即实例化。仅适用于单例 bean。
	 * @return 是否应用延迟初始化语义（默认为 {@code false}）
	 */
	public boolean isLazyInit() {
		return (this.lazyInit != null && this.lazyInit.booleanValue());
	}

	/**
	 * 返回是否默认延迟初始化 bean，即在启动时不立即实例化。仅适用于单例 bean。
	 * @return 如果显式设置了延迟初始化标志，则返回该标志，否则返回 {@code null}
	 * @since 5.2
	 */
	@Nullable
	public Boolean getLazyInit() {
		return this.lazyInit;
	}

	/**
	 * 设置自动装配模式。此模式决定是否进行自动的依赖检测和注入。
	 * 默认值为 AUTOWIRE_NO，表示不会基于名称或类型进行约定式自动装配
	 *（但仍然可能有显式的注解驱动的自动装配）。
	 * @param autowireMode 要设置的自动装配模式，必须是 {@link AbstractBeanDefinition} 中定义的常量之一。
	 * @see AbstractBeanDefinition#setAutowireMode
	 */
	public void setAutowireMode(int autowireMode) {
		this.autowireMode = autowireMode;
	}

	/**
	 * 返回默认的自动装配模式。
	 */
	public int getAutowireMode() {
		return this.autowireMode;
	}

	/**
	 * 设置依赖检查代码。
	 * @param dependencyCheck 要设置的代码，必须是 {@link AbstractBeanDefinition} 中定义的常量之一。
	 * @see AbstractBeanDefinition#setDependencyCheck
	 */
	public void setDependencyCheck(int dependencyCheck) {
		this.dependencyCheck = dependencyCheck;
	}

	/**
	 * 返回默认的依赖检查代码。
	 */
	public int getDependencyCheck() {
		return this.dependencyCheck;
	}

	/**
	 * 设置默认初始化方法的名称。
	 * <p>注意：该方法不会强制应用于所有相关的 bean 定义，而是作为可选的回调方法，仅在实际存在时被调用。
	 * @see AbstractBeanDefinition#setInitMethodName
	 * @see AbstractBeanDefinition#setEnforceInitMethod
	 */
	public void setInitMethodName(@Nullable String initMethodName) {
		this.initMethodName = (StringUtils.hasText(initMethodName) ? initMethodName : null);
	}

	/**
	 * 返回默认初始化方法的名称。
	 */
	@Nullable
	public String getInitMethodName() {
		return this.initMethodName;
	}

	/**
	 * 设置默认销毁方法的名称。
	 * <p>注意：该方法不会强制应用于所有相关的 bean 定义，而是作为可选的回调方法，仅在实际存在时被调用。
	 * @see AbstractBeanDefinition#setDestroyMethodName
	 * @see AbstractBeanDefinition#setEnforceDestroyMethod
	 */
	public void setDestroyMethodName(@Nullable String destroyMethodName) {
		this.destroyMethodName = (StringUtils.hasText(destroyMethodName) ? destroyMethodName : null);
	}

	/**
	 * 返回默认销毁方法的名称。
	 */
	@Nullable
	public String getDestroyMethodName() {
		return this.destroyMethodName;
	}

}
