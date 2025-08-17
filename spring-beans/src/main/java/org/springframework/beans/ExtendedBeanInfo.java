/*
 * Copyright 2002-2019 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;

import java.awt.*;
import java.beans.*;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.*;

/**
 * 对标准 {@link BeanInfo} 对象的装饰，例如通过 {@link Introspector#getBeanInfo(Class)} 创建的 BeanInfo，
 * 用于发现和注册静态或返回非 void 的 setter 方法。例如：
 *
 * <pre class="code">
 * public class Bean {
 *
 *     private Foo foo;
 *
 *     public Foo getFoo() {
 *         return this.foo;
 *     }
 *
 *     public Bean setFoo(Foo foo) {
 *         this.foo = foo;
 *         return this;
 *     }
 * }</pre>
 * <p>
 * 标准的 JavaBeans {@code Introspector} 会发现 {@code getFoo} 读方法，
 * 但会忽略 {@code #setFoo(Foo)} 写方法，因为其返回类型非 void，不符合 JavaBeans 规范。
 * {@code ExtendedBeanInfo} 则会识别并包含该方法。
 * 这使得带有“builder”或方法链式 setter 签名的 API 可以在 Spring {@code <beans>} XML 中使用。
 * {@link #getPropertyDescriptors()} 会返回封装的 {@code BeanInfo} 中所有现有的属性描述符，
 * 以及为返回非 void 的 setter 添加的属性描述符。标准（“非索引”）属性和
 * <a href="https://docs.oracle.com/javase/tutorial/javabeans/writing/properties.html">
 * 索引属性</a> 都得到完全支持。
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @see #ExtendedBeanInfo(BeanInfo)
 * @see ExtendedBeanInfoFactory
 * @see CachedIntrospectionResults
 * @since 3.1
 */
class ExtendedBeanInfo implements BeanInfo {

	private static final Log logger = LogFactory.getLog(ExtendedBeanInfo.class);

	private final BeanInfo delegate;

	private final Set<PropertyDescriptor> propertyDescriptors = new TreeSet<>(new PropertyDescriptorComparator());


	/**
	 * 包装给定的 {@link BeanInfo} 实例；将其所有现有的属性描述符本地复制一份，
	 * 并将每个描述符包装为自定义的 {@link SimpleIndexedPropertyDescriptor 索引型}
	 * 或 {@link SimplePropertyDescriptor 非索引型} {@code PropertyDescriptor} 变体，
	 * 以绕过 JDK 默认的弱/软引用管理；然后遍历其方法描述符，查找所有返回非 void 的写方法，
	 * 并更新或创建每个找到的 {@link PropertyDescriptor}。
	 *
	 * @param delegate 被包装的 {@code BeanInfo}，不会被修改
	 * @see #getPropertyDescriptors()
	 */
	public ExtendedBeanInfo(BeanInfo delegate) {
		this.delegate = delegate;
		for (PropertyDescriptor pd : delegate.getPropertyDescriptors()) {
			try {
				this.propertyDescriptors.add(pd instanceof IndexedPropertyDescriptor ?
						new SimpleIndexedPropertyDescriptor((IndexedPropertyDescriptor) pd) :
						new SimplePropertyDescriptor(pd));
			} catch (IntrospectionException ex) {
				// 可能只是一个不符合 JavaBeans 规范的方法……
				if (logger.isDebugEnabled()) {
					logger.debug("Ignoring invalid bean property '" + pd.getName() + "': " + ex.getMessage());
				}
			}
		}
		MethodDescriptor[] methodDescriptors = delegate.getMethodDescriptors();
		if (methodDescriptors != null) {
			for (Method method : findCandidateWriteMethods(methodDescriptors)) {
				try {
					handleCandidateWriteMethod(method);
				} catch (IntrospectionException ex) {
					// 我们这里只是尝试寻找候选方法，可以轻松忽略多余的方法……
					if (logger.isDebugEnabled()) {
						logger.debug("Ignoring candidate write method [" + method + "]: " + ex.getMessage());
					}
				}
			}
		}
	}


	private List<Method> findCandidateWriteMethods(MethodDescriptor[] methodDescriptors) {
		List<Method> matches = new ArrayList<>();
		for (MethodDescriptor methodDescriptor : methodDescriptors) {
			Method method = methodDescriptor.getMethod();
			if (isCandidateWriteMethod(method)) {
				matches.add(method);
			}
		}
		// 对返回非 void 的写方法进行排序，以防止
		// JDK 7 中 Class#getDeclaredMethods 返回方法顺序不确定导致的问题。
		// 参见：https://bugs.java.com/view_bug.do?bug_id=7023180
		matches.sort((m1, m2) -> m2.toString().compareTo(m1.toString()));
		return matches;
	}

	/**
	 * 是否是候选的写方法
	 *
	 * @param method 方法
	 * @return true 表示是候选的写方法
	 */
	public static boolean isCandidateWriteMethod(Method method) {
		// 获取方法名
		String methodName = method.getName();
		// 获取方法参数个数
		int nParams = method.getParameterCount();
		// 检查方法名长度是否大于 3 并且以 "set" 开头，
		// 方法修饰符是否为 public，
		// 方法的返回类型不是 void 或者是静态方法，
		// 方法参数个数为 1 或者为 2 且第一个参数类型为 int
		return (methodName.length() > 3 && methodName.startsWith("set") &&
				Modifier.isPublic(method.getModifiers()) &&
				(!void.class.isAssignableFrom(method.getReturnType()) || Modifier.isStatic(method.getModifiers())) &&
				(nParams == 1 || (nParams == 2 && int.class == method.getParameterTypes()[0])));
	}

	private void handleCandidateWriteMethod(Method method) throws IntrospectionException {
		int nParams = method.getParameterCount();
		String propertyName = propertyNameFor(method);
		Class<?> propertyType = method.getParameterTypes()[nParams - 1];
		PropertyDescriptor existingPd = findExistingPropertyDescriptor(propertyName, propertyType);
		if (nParams == 1) {
			if (existingPd == null) {
				this.propertyDescriptors.add(new SimplePropertyDescriptor(propertyName, null, method));
			} else {
				existingPd.setWriteMethod(method);
			}
		} else if (nParams == 2) {
			if (existingPd == null) {
				this.propertyDescriptors.add(
						new SimpleIndexedPropertyDescriptor(propertyName, null, null, null, method));
			} else if (existingPd instanceof IndexedPropertyDescriptor) {
				((IndexedPropertyDescriptor) existingPd).setIndexedWriteMethod(method);
			} else {
				this.propertyDescriptors.remove(existingPd);
				this.propertyDescriptors.add(new SimpleIndexedPropertyDescriptor(
						propertyName, existingPd.getReadMethod(), existingPd.getWriteMethod(), null, method));
			}
		} else {
			throw new IllegalArgumentException("Write method must have exactly 1 or 2 parameters: " + method);
		}
	}

	@Nullable
	private PropertyDescriptor findExistingPropertyDescriptor(String propertyName, Class<?> propertyType) {
		for (PropertyDescriptor pd : this.propertyDescriptors) {
			final Class<?> candidateType;
			final String candidateName = pd.getName();
			if (pd instanceof IndexedPropertyDescriptor) {
				IndexedPropertyDescriptor ipd = (IndexedPropertyDescriptor) pd;
				candidateType = ipd.getIndexedPropertyType();
				if (candidateName.equals(propertyName) &&
						(candidateType.equals(propertyType) || candidateType.equals(propertyType.getComponentType()))) {
					return pd;
				}
			} else {
				candidateType = pd.getPropertyType();
				if (candidateName.equals(propertyName) &&
						(candidateType.equals(propertyType) || propertyType.equals(candidateType.getComponentType()))) {
					return pd;
				}
			}
		}
		return null;
	}

	private String propertyNameFor(Method method) {
		return Introspector.decapitalize(method.getName().substring(3));
	}


	/**
	 * 返回封装的 {@link BeanInfo} 对象中的 {@link PropertyDescriptor PropertyDescriptors} 集合，
	 * 以及在构造过程中找到的每个非 void 返回类型的 setter 方法对应的 {@code PropertyDescriptors}。
	 *
	 * @see #ExtendedBeanInfo(BeanInfo)
	 */
	@Override
	public PropertyDescriptor[] getPropertyDescriptors() {
		return this.propertyDescriptors.toArray(new PropertyDescriptor[0]);
	}

	@Override
	public BeanInfo[] getAdditionalBeanInfo() {
		return this.delegate.getAdditionalBeanInfo();
	}

	@Override
	public BeanDescriptor getBeanDescriptor() {
		return this.delegate.getBeanDescriptor();
	}

	@Override
	public int getDefaultEventIndex() {
		return this.delegate.getDefaultEventIndex();
	}

	@Override
	public int getDefaultPropertyIndex() {
		return this.delegate.getDefaultPropertyIndex();
	}

	@Override
	public EventSetDescriptor[] getEventSetDescriptors() {
		return this.delegate.getEventSetDescriptors();
	}

	@Override
	public Image getIcon(int iconKind) {
		return this.delegate.getIcon(iconKind);
	}

	@Override
	public MethodDescriptor[] getMethodDescriptors() {
		return this.delegate.getMethodDescriptors();
	}


	/**
	 * 一个简单的 {@link PropertyDescriptor}.
	 */
	static class SimplePropertyDescriptor extends PropertyDescriptor {

		@Nullable
		private Method readMethod;

		@Nullable
		private Method writeMethod;

		@Nullable
		private Class<?> propertyType;

		@Nullable
		private Class<?> propertyEditorClass;

		public SimplePropertyDescriptor(PropertyDescriptor original) throws IntrospectionException {
			this(original.getName(), original.getReadMethod(), original.getWriteMethod());
			PropertyDescriptorUtils.copyNonMethodProperties(original, this);
		}

		public SimplePropertyDescriptor(String propertyName, @Nullable Method readMethod, Method writeMethod)
				throws IntrospectionException {

			super(propertyName, null, null);
			this.readMethod = readMethod;
			this.writeMethod = writeMethod;
			this.propertyType = PropertyDescriptorUtils.findPropertyType(readMethod, writeMethod);
		}

		@Override
		@Nullable
		public Method getReadMethod() {
			return this.readMethod;
		}

		@Override
		public void setReadMethod(@Nullable Method readMethod) {
			this.readMethod = readMethod;
		}

		@Override
		@Nullable
		public Method getWriteMethod() {
			return this.writeMethod;
		}

		@Override
		public void setWriteMethod(@Nullable Method writeMethod) {
			this.writeMethod = writeMethod;
		}

		@Override
		@Nullable
		public Class<?> getPropertyType() {
			if (this.propertyType == null) {
				try {
					this.propertyType = PropertyDescriptorUtils.findPropertyType(this.readMethod, this.writeMethod);
				} catch (IntrospectionException ex) {
					// 忽略异常，行为与 PropertyDescriptor#getPropertyType 一致
				}
			}
			return this.propertyType;
		}

		@Override
		@Nullable
		public Class<?> getPropertyEditorClass() {
			return this.propertyEditorClass;
		}

		@Override
		public void setPropertyEditorClass(@Nullable Class<?> propertyEditorClass) {
			this.propertyEditorClass = propertyEditorClass;
		}

		@Override
		public boolean equals(@Nullable Object other) {
			return (this == other || (other instanceof PropertyDescriptor &&
					PropertyDescriptorUtils.equals(this, (PropertyDescriptor) other)));
		}

		@Override
		public int hashCode() {
			return (ObjectUtils.nullSafeHashCode(getReadMethod()) * 29 + ObjectUtils.nullSafeHashCode(getWriteMethod()));
		}

		@Override
		public String toString() {
			return String.format("%s[name=%s, propertyType=%s, readMethod=%s, writeMethod=%s]",
					getClass().getSimpleName(), getName(), getPropertyType(), this.readMethod, this.writeMethod);
		}
	}


	/**
	 * 一个简单的 {@link IndexedPropertyDescriptor}.
	 */
	static class SimpleIndexedPropertyDescriptor extends IndexedPropertyDescriptor {

		@Nullable
		private Method readMethod;

		@Nullable
		private Method writeMethod;

		@Nullable
		private Class<?> propertyType;

		@Nullable
		private Method indexedReadMethod;

		@Nullable
		private Method indexedWriteMethod;

		@Nullable
		private Class<?> indexedPropertyType;

		@Nullable
		private Class<?> propertyEditorClass;

		public SimpleIndexedPropertyDescriptor(IndexedPropertyDescriptor original) throws IntrospectionException {
			this(original.getName(), original.getReadMethod(), original.getWriteMethod(),
					original.getIndexedReadMethod(), original.getIndexedWriteMethod());
			PropertyDescriptorUtils.copyNonMethodProperties(original, this);
		}

		public SimpleIndexedPropertyDescriptor(String propertyName, @Nullable Method readMethod,
											   @Nullable Method writeMethod, @Nullable Method indexedReadMethod, Method indexedWriteMethod)
				throws IntrospectionException {

			super(propertyName, null, null, null, null);
			this.readMethod = readMethod;
			this.writeMethod = writeMethod;
			this.propertyType = PropertyDescriptorUtils.findPropertyType(readMethod, writeMethod);
			this.indexedReadMethod = indexedReadMethod;
			this.indexedWriteMethod = indexedWriteMethod;
			this.indexedPropertyType = PropertyDescriptorUtils.findIndexedPropertyType(
					propertyName, this.propertyType, indexedReadMethod, indexedWriteMethod);
		}

		@Override
		@Nullable
		public Method getReadMethod() {
			return this.readMethod;
		}

		@Override
		public void setReadMethod(@Nullable Method readMethod) {
			this.readMethod = readMethod;
		}

		@Override
		@Nullable
		public Method getWriteMethod() {
			return this.writeMethod;
		}

		@Override
		public void setWriteMethod(@Nullable Method writeMethod) {
			this.writeMethod = writeMethod;
		}

		@Override
		@Nullable
		public Class<?> getPropertyType() {
			if (this.propertyType == null) {
				try {
					this.propertyType = PropertyDescriptorUtils.findPropertyType(this.readMethod, this.writeMethod);
				} catch (IntrospectionException ex) {
					// 忽略异常，行为与 IndexedPropertyDescriptor#getPropertyType 一致
				}
			}
			return this.propertyType;
		}

		@Override
		@Nullable
		public Method getIndexedReadMethod() {
			return this.indexedReadMethod;
		}

		@Override
		public void setIndexedReadMethod(@Nullable Method indexedReadMethod) throws IntrospectionException {
			this.indexedReadMethod = indexedReadMethod;
		}

		@Override
		@Nullable
		public Method getIndexedWriteMethod() {
			return this.indexedWriteMethod;
		}

		@Override
		public void setIndexedWriteMethod(@Nullable Method indexedWriteMethod) throws IntrospectionException {
			this.indexedWriteMethod = indexedWriteMethod;
		}

		@Override
		@Nullable
		public Class<?> getIndexedPropertyType() {
			if (this.indexedPropertyType == null) {
				try {
					this.indexedPropertyType = PropertyDescriptorUtils.findIndexedPropertyType(
							getName(), getPropertyType(), this.indexedReadMethod, this.indexedWriteMethod);
				} catch (IntrospectionException ex) {
					// 忽略异常，行为与 IndexedPropertyDescriptor#getIndexedPropertyType 一致
				}
			}
			return this.indexedPropertyType;
		}

		@Override
		@Nullable
		public Class<?> getPropertyEditorClass() {
			return this.propertyEditorClass;
		}

		@Override
		public void setPropertyEditorClass(@Nullable Class<?> propertyEditorClass) {
			this.propertyEditorClass = propertyEditorClass;
		}

		/*
		 * 参考 java.beans.IndexedPropertyDescriptor#equals 方法
		 */
		@Override
		public boolean equals(@Nullable Object other) {
			if (this == other) {
				return true;
			}
			if (!(other instanceof IndexedPropertyDescriptor)) {
				return false;
			}
			IndexedPropertyDescriptor otherPd = (IndexedPropertyDescriptor) other;
			return (ObjectUtils.nullSafeEquals(getIndexedReadMethod(), otherPd.getIndexedReadMethod()) &&
					ObjectUtils.nullSafeEquals(getIndexedWriteMethod(), otherPd.getIndexedWriteMethod()) &&
					ObjectUtils.nullSafeEquals(getIndexedPropertyType(), otherPd.getIndexedPropertyType()) &&
					PropertyDescriptorUtils.equals(this, otherPd));
		}

		@Override
		public int hashCode() {
			int hashCode = ObjectUtils.nullSafeHashCode(getReadMethod());
			hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(getWriteMethod());
			hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(getIndexedReadMethod());
			hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(getIndexedWriteMethod());
			return hashCode;
		}

		@Override
		public String toString() {
			return String.format("%s[name=%s, propertyType=%s, indexedPropertyType=%s, " +
							"readMethod=%s, writeMethod=%s, indexedReadMethod=%s, indexedWriteMethod=%s]",
					getClass().getSimpleName(), getName(), getPropertyType(), getIndexedPropertyType(),
					this.readMethod, this.writeMethod, this.indexedReadMethod, this.indexedWriteMethod);
		}
	}


	/**
	 * 对 PropertyDescriptor 实例按字母数字顺序进行排序，
	 * 以模拟 {@link java.beans.BeanInfo#getPropertyDescriptors()} 的行为。
	 *
	 * @see ExtendedBeanInfo#propertyDescriptors
	 */
	static class PropertyDescriptorComparator implements Comparator<PropertyDescriptor> {

		@Override
		public int compare(PropertyDescriptor desc1, PropertyDescriptor desc2) {
			String left = desc1.getName();
			String right = desc2.getName();
			byte[] leftBytes = left.getBytes();
			byte[] rightBytes = right.getBytes();
			for (int i = 0; i < left.length(); i++) {
				if (right.length() == i) {
					return 1;
				}
				int result = leftBytes[i] - rightBytes[i];
				if (result != 0) {
					return result;
				}
			}
			return left.length() - right.length();
		}
	}

}
