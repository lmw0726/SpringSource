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

package org.springframework.validation;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.ConfigurablePropertyAccessor;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.lang.Nullable;

import java.io.Serializable;

/**
 * {@link Errors} 和 {@link BindingResult} 接口的默认实现，
 * 用于在 JavaBean 对象上注册和评估绑定错误。
 *
 * <p>执行标准的 JavaBean 属性访问，还支持嵌套属性。
 * 通常，应用程序代码将使用 {@code Errors} 接口或 {@code BindingResult} 接口。
 * {@link DataBinder} 通过 {@link DataBinder#getBindingResult()} 返回其 {@code BindingResult}。
 *
 * @author Juergen Hoeller
 * @see DataBinder#getBindingResult()
 * @see DataBinder#initBeanPropertyAccess()
 * @see DirectFieldBindingResult
 * @since 2.0
 */
@SuppressWarnings("serial")
public class BeanPropertyBindingResult extends AbstractPropertyBindingResult implements Serializable {
	/**
	 * 目标对象
	 */
	@Nullable
	private final Object target;

	/**
	 * 是否是自增长嵌套路径
	 */
	private final boolean autoGrowNestedPaths;

	/**
	 * 自动增加集合的限额
	 */
	private final int autoGrowCollectionLimit;

	/**
	 * Bean包装器
	 */
	@Nullable
	private transient BeanWrapper beanWrapper;


	/**
	 * 创建一个新的 {@link BeanPropertyBindingResult} 实例。
	 *
	 * @param target     目标 bean
	 * @param objectName 目标对象的名称
	 */
	public BeanPropertyBindingResult(@Nullable Object target, String objectName) {
		this(target, objectName, true, Integer.MAX_VALUE);
	}

	/**
	 * 创建一个新的 {@link BeanPropertyBindingResult} 实例。
	 *
	 * @param target                  目标 bean
	 * @param objectName              目标对象的名称
	 * @param autoGrowNestedPaths     是否自动增长包含 null 值的嵌套路径
	 * @param autoGrowCollectionLimit 数组和集合自动增长的限制
	 */
	public BeanPropertyBindingResult(@Nullable Object target, String objectName,
									 boolean autoGrowNestedPaths, int autoGrowCollectionLimit) {

		super(objectName);
		this.target = target;
		this.autoGrowNestedPaths = autoGrowNestedPaths;
		this.autoGrowCollectionLimit = autoGrowCollectionLimit;
	}


	@Override
	@Nullable
	public final Object getTarget() {
		return this.target;
	}

	/**
	 * 返回此实例使用的 {@link BeanWrapper}。
	 * 如果之前不存在，则创建一个新的 {@link BeanWrapper}。
	 *
	 * @see #createBeanWrapper()
	 */
	@Override
	public final ConfigurablePropertyAccessor getPropertyAccessor() {
		// 如果 bean包装器 尚未初始化
		if (this.beanWrapper == null) {
			// 创建一个新的 BeanWrapper 实例
			this.beanWrapper = createBeanWrapper();

			// 设置提取旧值以供编辑器使用
			this.beanWrapper.setExtractOldValueForEditor(true);

			// 设置自动增长嵌套路径的选项
			this.beanWrapper.setAutoGrowNestedPaths(this.autoGrowNestedPaths);

			// 设置自动增长集合的限制
			this.beanWrapper.setAutoGrowCollectionLimit(this.autoGrowCollectionLimit);
		}

		// 返回初始化后的 bean包装器 实例
		return this.beanWrapper;
	}

	/**
	 * 为底层目标对象创建一个新的 {@link BeanWrapper}。
	 *
	 * @see #getTarget()
	 */
	protected BeanWrapper createBeanWrapper() {
		// 如果目标对象为 null
		if (this.target == null) {
			// 抛出 IllegalStateException 异常，提示不能在 null bean 实例上访问属性
			throw new IllegalStateException("Cannot access properties on null bean instance '" + getObjectName() + "'");
		}

		// 返回用于访问目标对象属性的 PropertyAccessor
		return PropertyAccessorFactory.forBeanPropertyAccess(this.target);
	}

}
