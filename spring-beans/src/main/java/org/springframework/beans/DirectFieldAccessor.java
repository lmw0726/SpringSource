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

package org.springframework.beans;

import org.springframework.core.ResolvableType;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.lang.Nullable;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
/**
 * {@link ConfigurablePropertyAccessor} 的实现，直接访问实例字段。
 * 允许直接绑定到字段而不是通过 JavaBean 的 setter 方法。
 *
 * <p>从 Spring 4.2 开始，绝大多数 {@link BeanWrapper} 的特性已经合并到 {@link AbstractPropertyAccessor} 中，
 * 这意味着属性遍历以及集合和映射的访问现在在这里也得到了支持。
 *
 * <p>DirectFieldAccessor 的 "extractOldValueForEditor" 设置的默认值为 "true"，
 * 因为字段可以始终被读取而不产生副作用。
 *
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @see #setExtractOldValueForEditor
 * @see BeanWrapper
 * @see org.springframework.validation.DirectFieldBindingResult
 * @see org.springframework.validation.DataBinder#initDirectFieldAccess()
 * @since 2.0
 */
public class DirectFieldAccessor extends AbstractNestablePropertyAccessor {

	private final Map<String, FieldPropertyHandler> fieldMap = new HashMap<>();


	/**
	 * 为给定对象创建一个新的 DirectFieldAccessor。
	 *
	 * @param object 被此 DirectFieldAccessor 包装的对象
	 */
	public DirectFieldAccessor(Object object) {
		super(object);
	}

	/**
	 * 为给定对象创建一个新的 DirectFieldAccessor，
	 * 注册对象所在的嵌套路径。
	 *
	 * @param object     被此 DirectFieldAccessor 包装的对象
	 * @param nestedPath 对象的嵌套路径
	 * @param parent     包含的 DirectFieldAccessor（不能为空）
	 */
	protected DirectFieldAccessor(Object object, String nestedPath, DirectFieldAccessor parent) {
		super(object, nestedPath, parent);
	}


	@Override
	@Nullable
	protected FieldPropertyHandler getLocalPropertyHandler(String propertyName) {
		// 尝试从字段映射中获取属性处理程序
		FieldPropertyHandler propertyHandler = this.fieldMap.get(propertyName);
		// 如果属性处理程序为空，则尝试通过反射查找字段并创建新的属性处理程序
		if (propertyHandler == null) {
			// 查找给定名称的字段
			Field field = ReflectionUtils.findField(getWrappedClass(), propertyName);
			// 如果找到字段，则创建字段属性处理程序并将其添加到字段映射中
			if (field != null) {
				propertyHandler = new FieldPropertyHandler(field);
				this.fieldMap.put(propertyName, propertyHandler);
			}
		}
		// 返回找到的属性处理程序（可能为null）
		return propertyHandler;
	}

	@Override
	protected DirectFieldAccessor newNestedPropertyAccessor(Object object, String nestedPath) {
		return new DirectFieldAccessor(object, nestedPath, this);
	}

	@Override
	protected NotWritablePropertyException createNotWritablePropertyException(String propertyName) {
		// 创建用于字段的属性匹配器
		PropertyMatches matches = PropertyMatches.forField(propertyName, getRootClass());
		// 抛出不可写属性异常，其中包括根类、嵌套路径、错误消息和可能的匹配项
		throw new NotWritablePropertyException(getRootClass(), getNestedPath() + propertyName,
				matches.buildErrorMessage(), matches.getPossibleMatches());
	}


	private class FieldPropertyHandler extends PropertyHandler {
		/**
		 * 字段信息
		 */
		private final Field field;

		public FieldPropertyHandler(Field field) {
			super(field.getType(), true, true);
			this.field = field;
		}

		@Override
		public TypeDescriptor toTypeDescriptor() {
			return new TypeDescriptor(this.field);
		}

		@Override
		public ResolvableType getResolvableType() {
			return ResolvableType.forField(this.field);
		}

		@Override
		@Nullable
		public TypeDescriptor nested(int level) {
			return TypeDescriptor.nested(this.field, level);
		}

		@Override
		@Nullable
		public Object getValue() throws Exception {
			try {
				// 提高字段的可访问性
				ReflectionUtils.makeAccessible(this.field);
				// 返回字段的值
				return this.field.get(getWrappedInstance());
			} catch (IllegalAccessException ex) {
				// 如果访问字段时发生异常，则抛出无效属性异常
				throw new InvalidPropertyException(getWrappedClass(),
						this.field.getName(), "Field is not accessible", ex);
			}
		}

		@Override
		public void setValue(@Nullable Object value) throws Exception {
			try {
				// 提高字段的可访问性
				ReflectionUtils.makeAccessible(this.field);
				// 设置字段的值
				this.field.set(getWrappedInstance(), value);
			} catch (IllegalAccessException ex) {
				// 如果访问字段时发生异常，则抛出无效属性异常
				throw new InvalidPropertyException(getWrappedClass(), this.field.getName(),
						"Field is not accessible", ex);
			}
		}
	}

}
