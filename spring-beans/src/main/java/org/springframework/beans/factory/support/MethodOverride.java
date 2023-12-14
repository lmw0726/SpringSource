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

package org.springframework.beans.factory.support;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import java.lang.reflect.Method;

/**
 * Object representing the override of a method on a managed object by the IoC
 * container.
 *
 * <p>Note that the override mechanism is <em>not</em> intended as a generic
 * means of inserting crosscutting code: use AOP for that.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 1.1
 */
public abstract class MethodOverride implements BeanMetadataElement {

	/**
	 * 方法名称
	 */
	private final String methodName;

	/**
	 * 是否为重载方法
	 */
	private boolean overloaded = true;

	/**
	 * 源对象
	 */
	@Nullable
	private Object source;


	/**
	 * 为给定的方法构造一个新的覆盖。
	 *
	 * @param methodName 要覆盖的方法的名称
	 */
	protected MethodOverride(String methodName) {
		Assert.notNull(methodName, "Method name must not be null");
		this.methodName = methodName;
	}


	/**
	 * 返回要重载的方法的名称。
	 */
	public String getMethodName() {
		return this.methodName;
	}

	/**
	 * 设置重载的方法是否为 <em>overloaded<em> (即是否需要发生参数类型匹配以消除同名方法的歧义)。
	 * <p> 默认值为 {@code true}; 可以切换到 {@code false} 以优化运行时性能。
	 */
	protected void setOverloaded(boolean overloaded) {
		this.overloaded = overloaded;
	}

	/**
	 * 返回重载的方法是否为 <em>overloaded<em> (即是否需要发生参数类型匹配以消歧义同名方法)。
	 */
	protected boolean isOverloaded() {
		return this.overloaded;
	}

	/**设置此元数据元素的配置源 {@code Object}。
	 * <p> 对象的确切类型将取决于所使用的配置机制。
	 */
	public void setSource(@Nullable Object source) {
		this.source = source;
	}

	@Override
	@Nullable
	public Object getSource() {
		return this.source;
	}

	/**
	 * 子类必须覆盖此以指示它们是否 <em> 匹配 <em> 给定方法。
	 * 这允许参数列表检查以及方法名称检查。
	 *
	 * @param method 要检查的方法
	 * @return 此重载方法是否与给定方法匹配
	 */
	public abstract boolean matches(Method method);


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof MethodOverride)) {
			return false;
		}
		MethodOverride that = (MethodOverride) other;
		return (ObjectUtils.nullSafeEquals(this.methodName, that.methodName) &&
				ObjectUtils.nullSafeEquals(this.source, that.source));
	}

	@Override
	public int hashCode() {
		int hashCode = ObjectUtils.nullSafeHashCode(this.methodName);
		hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(this.source);
		return hashCode;
	}

}
