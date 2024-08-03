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

import org.springframework.beans.ConfigurablePropertyAccessor;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.lang.Nullable;

/**
 * {@link Errors} 和 {@link BindingResult} 接口的特殊实现，
 * 支持在值对象上注册和评估绑定错误。
 * 执行直接字段访问，而不是通过 JavaBean getter 方法。
 *
 * <p>从 Spring 4.1 开始，此实现能够遍历嵌套字段。
 *
 * @author Juergen Hoeller
 * @see DataBinder#getBindingResult()
 * @see DataBinder#initDirectFieldAccess()
 * @see BeanPropertyBindingResult
 * @since 2.0
 */
@SuppressWarnings("serial")
public class DirectFieldBindingResult extends AbstractPropertyBindingResult {
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
	 * 直接字段访问器
	 */
	@Nullable
	private transient ConfigurablePropertyAccessor directFieldAccessor;


	/**
	 * 创建一个新的 DirectFieldBindingResult 实例。
	 *
	 * @param target     目标对象
	 * @param objectName 目标对象的名称
	 */
	public DirectFieldBindingResult(@Nullable Object target, String objectName) {
		this(target, objectName, true);
	}

	/**
	 * 创建一个新的 DirectFieldBindingResult 实例。
	 *
	 * @param target              目标对象
	 * @param objectName          目标对象的名称
	 * @param autoGrowNestedPaths 是否自动增长包含 null 值的嵌套路径
	 */
	public DirectFieldBindingResult(@Nullable Object target, String objectName, boolean autoGrowNestedPaths) {
		super(objectName);
		this.target = target;
		this.autoGrowNestedPaths = autoGrowNestedPaths;
	}


	@Override
	@Nullable
	public final Object getTarget() {
		return this.target;
	}

	/**
	 * 返回此实例使用的 DirectFieldAccessor。
	 * 如果之前不存在，则创建一个新的 DirectFieldAccessor。
	 *
	 * @see #createDirectFieldAccessor()
	 */
	@Override
	public final ConfigurablePropertyAccessor getPropertyAccessor() {
		// 如果 直接字段访问器 尚未初始化
		if (this.directFieldAccessor == null) {
			// 创建一个新的 直接字段访问器 实例
			this.directFieldAccessor = createDirectFieldAccessor();

			// 设置提取旧值以供编辑器使用
			this.directFieldAccessor.setExtractOldValueForEditor(true);

			// 设置自动增长嵌套路径的选项
			this.directFieldAccessor.setAutoGrowNestedPaths(this.autoGrowNestedPaths);
		}

		// 返回初始化后的 直接字段访问器 实例
		return this.directFieldAccessor;
	}

	/**
	 * 为底层目标对象创建一个新的 DirectFieldAccessor。
	 *
	 * @see #getTarget()
	 */
	protected ConfigurablePropertyAccessor createDirectFieldAccessor() {
		// 如果目标对象为 null
		if (this.target == null) {
			// 抛出 IllegalStateException 异常，提示不能在 null 目标实例上访问字段
			throw new IllegalStateException("Cannot access fields on null target instance '" + getObjectName() + "'");
		}

		// 返回用于直接访问目标对象字段的 PropertyAccessor
		return PropertyAccessorFactory.forDirectFieldAccess(this.target);
	}

}
