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
import org.springframework.core.convert.Property;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.lang.Nullable;
import org.springframework.util.ReflectionUtils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.security.*;

/**
 * 默认的 {@link BeanWrapper} 实现，适用于所有典型的用例。为了提高效率，缓存内省结果。
 *
 * <p>注意：自动注册来自 {@code org.springframework.beans.propertyeditors} 包的默认属性编辑器，
 * 这些编辑器除了 JDK 的标准 PropertyEditors 外还适用。应用程序可以调用
 * {@link #registerCustomEditor(Class, java.beans.PropertyEditor)} 方法
 * 为特定实例注册编辑器（即它们不在应用程序中共享）。详情请参见基类
 * {@link PropertyEditorRegistrySupport}。
 *
 * <p><b>注意：从Spring 2.5开始，这几乎对于所有目的都是一个内部类。</b>
 * 它只是公共的，以允许从其他框架包中访问。对于标准的应用程序访问目的，请使用
 * {@link PropertyAccessorFactory#forBeanPropertyAccess} 工厂方法。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Stephane Nicoll
 * @see #registerCustomEditor
 * @see #setPropertyValues
 * @see #setPropertyValue
 * @see #getPropertyValue
 * @see #getPropertyType
 * @see BeanWrapper
 * @see PropertyEditorRegistrySupport
 * @since 2001年4月15日
 */
public class BeanWrapperImpl extends AbstractNestablePropertyAccessor implements BeanWrapper {

	/**
	 * 为该对象缓存的内省结果，以防止每次都遇到 JavaBeans 内省的成本。
	 */
	@Nullable
	private CachedIntrospectionResults cachedIntrospectionResults;

	/**
	 * 用于调用属性方法的安全上下文。
	 */
	@Nullable
	private AccessControlContext acc;


	/**
	 * 创建一个新的空的 BeanWrapperImpl。之后需要设置包装实例。
	 * 注册默认编辑器。
	 *
	 * @see #setWrappedInstance
	 */
	public BeanWrapperImpl() {
		this(true);
	}

	/**
	 * 创建一个新的空的 BeanWrapperImpl。之后需要设置包装实例。
	 *
	 * @param registerDefaultEditors 是否注册默认编辑器
	 *                               （如果 BeanWrapper 不需要任何类型转换，则可以取消注册）
	 * @see #setWrappedInstance
	 */
	public BeanWrapperImpl(boolean registerDefaultEditors) {
		super(registerDefaultEditors);
	}

	/**
	 * 为给定对象创建一个新的 BeanWrapperImpl。
	 *
	 * @param object 由此 BeanWrapper 包装的对象
	 */
	public BeanWrapperImpl(Object object) {
		super(object);
	}

	/**
	 * 创建一个新的 BeanWrapperImpl，包装指定类的新实例。
	 *
	 * @param clazz 要实例化和包装的类
	 */
	public BeanWrapperImpl(Class<?> clazz) {
		super(clazz);
	}

	/**
	 * 为给定对象创建一个新的 BeanWrapperImpl，注册对象所在的嵌套路径。
	 *
	 * @param object     由此 BeanWrapper 包装的对象
	 * @param nestedPath 对象所在的嵌套路径
	 * @param rootObject 路径顶部的根对象
	 */
	public BeanWrapperImpl(Object object, String nestedPath, Object rootObject) {
		super(object, nestedPath, rootObject);
	}

	/**
	 * 为给定对象创建一个新的 BeanWrapperImpl，注册对象所在的嵌套路径。
	 *
	 * @param object     由此 BeanWrapper 包装的对象
	 * @param nestedPath 对象所在的嵌套路径
	 * @param parent     包含 BeanWrapper（不能为 null）
	 */
	private BeanWrapperImpl(Object object, String nestedPath, BeanWrapperImpl parent) {
		super(object, nestedPath, parent);
		setSecurityContext(parent.acc);
	}


	/**
	 * 设置一个 Bean 实例，而不解包 {@link java.util.Optional}。
	 *
	 * @param object 实际的目标对象
	 * @see #setWrappedInstance(Object)
	 * @since 4.3
	 */
	public void setBeanInstance(Object object) {
		this.wrappedObject = object;
		this.rootObject = object;
		this.typeConverterDelegate = new TypeConverterDelegate(this, this.wrappedObject);
		setIntrospectionClass(object.getClass());
	}

	@Override
	public void setWrappedInstance(Object object, @Nullable String nestedPath, @Nullable Object rootObject) {
		super.setWrappedInstance(object, nestedPath, rootObject);
		setIntrospectionClass(getWrappedClass());
	}

	/**
	 * 设置要内省的类。
	 * 当目标对象更改时需要调用此方法。
	 *
	 * @param clazz 要内省的类
	 */
	protected void setIntrospectionClass(Class<?> clazz) {
		if (this.cachedIntrospectionResults != null && this.cachedIntrospectionResults.getBeanClass() != clazz) {
			// 如果缓存的内省结果不为null且其对应的类与给定的类不同，则将缓存的内省结果置为null
			this.cachedIntrospectionResults = null;
		}
	}

	/**
	 * 获取包装对象的延迟初始化的 CachedIntrospectionResults 实例。
	 */
	private CachedIntrospectionResults getCachedIntrospectionResults() {
		if (this.cachedIntrospectionResults == null) {
			// 如果缓存的内省结果为null，则根据包装类获取对应的缓存内省结果
			this.cachedIntrospectionResults = CachedIntrospectionResults.forClass(getWrappedClass());
		}
		// 返回缓存的内省结果
		return this.cachedIntrospectionResults;
	}

	/**
	 * 设置在调用包装实例方法时使用的安全上下文。
	 * 可以为 null。
	 */
	public void setSecurityContext(@Nullable AccessControlContext acc) {
		this.acc = acc;
	}

	/**
	 * 返回在调用包装实例方法时使用的安全上下文。
	 * 可以为 null。
	 */
	@Nullable
	public AccessControlContext getSecurityContext() {
		return this.acc;
	}


	/**
	 * 将给定值转换为指定属性的类型。
	 * <p>此方法仅用于 BeanFactory 中的优化。
	 * 对于编程转换，请使用 convertIfNecessary 方法。
	 *
	 * @param value        要转换的值
	 * @param propertyName 目标属性
	 *                     （注意，此处不支持嵌套或索引属性）
	 * @return 新值，可能是类型转换的结果
	 * @throws TypeMismatchException 如果类型转换失败
	 */
	@Nullable
	public Object convertForProperty(@Nullable Object value, String propertyName) throws TypeMismatchException {
		// 获取缓存的内省结果
		CachedIntrospectionResults cachedIntrospectionResults = getCachedIntrospectionResults();
		// 通过属性名称获取属性描述符
		PropertyDescriptor pd = cachedIntrospectionResults.getPropertyDescriptor(propertyName);
		if (pd == null) {
			// 如果属性描述符为null，则抛出无效属性异常
			throw new InvalidPropertyException(getRootClass(), getNestedPath() + propertyName,
					"No property '" + propertyName + "' found");
		}
		// 获取属性描述符对应的类型描述符
		TypeDescriptor td = cachedIntrospectionResults.getTypeDescriptor(pd);
		if (td == null) {
			// 如果类型描述符为null，则创建一个新的类型描述符，并将其添加到缓存的内省结果中
			td = cachedIntrospectionResults.addTypeDescriptor(pd, new TypeDescriptor(property(pd)));
		}
		// 返回转换后的属性值
		return convertForProperty(propertyName, null, value, td);
	}

	/**
	 * 根据属性描述符获取属性。
	 *
	 * @param pd 属性描述符
	 * @return 属性
	 */
	private Property property(PropertyDescriptor pd) {
		// 将属性描述符转换为泛型感知属性描述符
		GenericTypeAwarePropertyDescriptor gpd = (GenericTypeAwarePropertyDescriptor) pd;
		// 使用泛型感知属性描述符创建一个新的属性对象，并返回
		return new Property(gpd.getBeanClass(), gpd.getReadMethod(), gpd.getWriteMethod(), gpd.getName());
	}

	@Override
	@Nullable
	protected BeanPropertyHandler getLocalPropertyHandler(String propertyName) {
		// 从缓存的内省结果中获取属性描述符
		PropertyDescriptor pd = getCachedIntrospectionResults().getPropertyDescriptor(propertyName);
		// 如果属性描述符不为空，则创建一个新的 BeanPropertyHandler 对象并返回，否则返回 null
		return (pd != null ? new BeanPropertyHandler(pd) : null);
	}

	@Override
	protected BeanWrapperImpl newNestedPropertyAccessor(Object object, String nestedPath) {
		return new BeanWrapperImpl(object, nestedPath, this);
	}

	@Override
	protected NotWritablePropertyException createNotWritablePropertyException(String propertyName) {
		// 创建 PropertyMatches 对象，用于匹配属性名
		PropertyMatches matches = PropertyMatches.forProperty(propertyName, getRootClass());
		// 抛出 NotWritablePropertyException 异常，包含属性不可写的相关信息和可能的匹配项
		throw new NotWritablePropertyException(getRootClass(), getNestedPath() + propertyName,
				matches.buildErrorMessage(), matches.getPossibleMatches());
	}

	@Override
	public PropertyDescriptor[] getPropertyDescriptors() {
		return getCachedIntrospectionResults().getPropertyDescriptors();
	}

	@Override
	public PropertyDescriptor getPropertyDescriptor(String propertyName) throws InvalidPropertyException {
		// 获取嵌套属性的 BeanWrapperImpl 对象
		BeanWrapperImpl nestedBw = (BeanWrapperImpl) getPropertyAccessorForPropertyPath(propertyName);
		// 获取最终路径
		String finalPath = getFinalPath(nestedBw, propertyName);
		// 从缓存的内省结果中获取属性描述符
		PropertyDescriptor pd = nestedBw.getCachedIntrospectionResults().getPropertyDescriptor(finalPath);
		// 如果属性描述符为空，则抛出无效属性异常，说明找不到对应的属性
		if (pd == null) {
			throw new InvalidPropertyException(getRootClass(), getNestedPath() + propertyName,
					"No property '" + propertyName + "' found");
		}
		return pd;
	}


	private class BeanPropertyHandler extends PropertyHandler {
		/**
		 * 属性描述符
		 */
		private final PropertyDescriptor pd;

		public BeanPropertyHandler(PropertyDescriptor pd) {
			super(pd.getPropertyType(), pd.getReadMethod() != null, pd.getWriteMethod() != null);
			this.pd = pd;
		}

		@Override
		public ResolvableType getResolvableType() {
			return ResolvableType.forMethodReturnType(this.pd.getReadMethod());
		}

		@Override
		public TypeDescriptor toTypeDescriptor() {
			return new TypeDescriptor(property(this.pd));
		}

		@Override
		@Nullable
		public TypeDescriptor nested(int level) {
			return TypeDescriptor.nested(property(this.pd), level);
		}

		@Override
		@Nullable
		public Object getValue() throws Exception {
			Method readMethod = this.pd.getReadMethod();
			if (System.getSecurityManager() != null) {
				// 如果安全管理器不为空，则以特权方式执行访问控制和方法调用
				AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
					ReflectionUtils.makeAccessible(readMethod);
					return null;
				});
				try {
					// 以特权方式执行方法调用并返回结果
					return AccessController.doPrivileged((PrivilegedExceptionAction<Object>)
							() -> readMethod.invoke(getWrappedInstance(), (Object[]) null), acc);
				} catch (PrivilegedActionException pae) {
					// 捕获特权操作异常并抛出其中的异常
					throw pae.getException();
				}
			} else {
				// 如果没有安全管理器，则直接执行方法调用并返回结果
				ReflectionUtils.makeAccessible(readMethod);
				return readMethod.invoke(getWrappedInstance(), (Object[]) null);
			}
		}

		@Override
		public void setValue(@Nullable Object value) throws Exception {
			Method writeMethod = (this.pd instanceof GenericTypeAwarePropertyDescriptor ?
					((GenericTypeAwarePropertyDescriptor) this.pd).getWriteMethodForActualAccess() :
					this.pd.getWriteMethod());
			if (System.getSecurityManager() != null) {
				// 如果安全管理器不为空，则以特权方式执行访问控制和方法调用
				AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
					ReflectionUtils.makeAccessible(writeMethod);
					return null;
				});
				try {
					// 以特权方式执行方法调用
					AccessController.doPrivileged((PrivilegedExceptionAction<Object>)
							() -> writeMethod.invoke(getWrappedInstance(), value), acc);
				} catch (PrivilegedActionException ex) {
					// 捕获特权操作异常并抛出其中的异常
					throw ex.getException();
				}
			} else {
				// 如果没有安全管理器，则直接执行方法调用
				ReflectionUtils.makeAccessible(writeMethod);
				writeMethod.invoke(getWrappedInstance(), value);
			}
		}
	}

}
