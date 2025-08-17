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

package org.springframework.beans.factory.config;

import kotlin.reflect.KProperty;
import kotlin.reflect.jvm.ReflectJvmMapping;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.InjectionPoint;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.core.KotlinDetector;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Optional;

/**
 * 即将被注入的特定依赖项的描述符。
 * 包装构造函数参数、方法参数或字段，
 * 允许统一访问它们的元数据。
 *
 * @author Juergen Hoeller
 * @since 2.5
 */
@SuppressWarnings("serial")
public class DependencyDescriptor extends InjectionPoint implements Serializable {

	private final Class<?> declaringClass;

	@Nullable
	private String methodName;

	@Nullable
	private Class<?>[] parameterTypes;

	private int parameterIndex;

	@Nullable
	private String fieldName;

	private final boolean required;

	private final boolean eager;

	private int nestingLevel = 1;

	@Nullable
	private Class<?> containingClass;

	@Nullable
	private transient volatile ResolvableType resolvableType;

	@Nullable
	private transient volatile TypeDescriptor typeDescriptor;


	/**
	 * 为方法或构造函数参数创建一个新的描述符。
	 * 将依赖项视为“急切”依赖。
	 * @param methodParameter 要包装的 MethodParameter
	 * @param required 该依赖项是否为必需
	 */
	public DependencyDescriptor(MethodParameter methodParameter, boolean required) {
		this(methodParameter, required, true);
	}

	/**
	 * 为方法或构造函数参数创建一个新的描述符。
	 * @param methodParameter 要包装的 MethodParameter
	 * @param required 该依赖项是否为必需
	 * @param eager 是否为“急切”依赖，即尽早解析可能的目标 Bean 以进行类型匹配
	 */
	public DependencyDescriptor(MethodParameter methodParameter, boolean required, boolean eager) {
		super(methodParameter);

		this.declaringClass = methodParameter.getDeclaringClass();
		if (methodParameter.getMethod() != null) {
			this.methodName = methodParameter.getMethod().getName();
		}
		this.parameterTypes = methodParameter.getExecutable().getParameterTypes();
		this.parameterIndex = methodParameter.getParameterIndex();
		this.containingClass = methodParameter.getContainingClass();
		this.required = required;
		this.eager = eager;
	}

	/**
	 * 为字段创建一个新的描述符。
	 * 将依赖项视为“急切”依赖。
	 * @param field 要包装的字段
	 * @param required 该依赖项是否为必需
	 */
	public DependencyDescriptor(Field field, boolean required) {
		this(field, required, true);
	}

	/**
	 * 为字段创建一个新的描述符。
	 * @param field 要包装的字段
	 * @param required 该依赖项是否为必需
	 * @param eager 是否为“急切”依赖，即尽早解析可能的目标 Bean 以进行类型匹配
	 */
	public DependencyDescriptor(Field field, boolean required, boolean eager) {
		super(field);

		this.declaringClass = field.getDeclaringClass();
		this.fieldName = field.getName();
		this.required = required;
		this.eager = eager;
	}

	/**
	 * 拷贝构造函数。
	 * @param original 要复制的原始描述符
	 */
	public DependencyDescriptor(DependencyDescriptor original) {
		super(original);

		this.declaringClass = original.declaringClass;
		this.methodName = original.methodName;
		this.parameterTypes = original.parameterTypes;
		this.parameterIndex = original.parameterIndex;
		this.fieldName = original.fieldName;
		this.containingClass = original.containingClass;
		this.required = original.required;
		this.eager = original.eager;
		this.nestingLevel = original.nestingLevel;
	}


	/**
	 * 返回该依赖项是否为必需。
	 * <p>可选语义来源于 Java 8 的 {@link java.util.Optional}，
	 * 参数级别的 {@code Nullable} 注解（例如来自 JSR-305 或 FindBugs 注解集），
	 * 或 Kotlin 的语言级可空类型声明。
	 */
	public boolean isRequired() {
		if (!this.required) {
			return false;
		}

		if (this.field != null) {
			return !(this.field.getType() == Optional.class || hasNullableAnnotation() ||
					(KotlinDetector.isKotlinReflectPresent() &&
							KotlinDetector.isKotlinType(this.field.getDeclaringClass()) &&
							KotlinDelegate.isNullable(this.field)));
		}
		else {
			return !obtainMethodParameter().isOptional();
		}
	}

	/**
	 * 检查底层字段是否被任何变体的 {@code Nullable} 注解标注，
	 * 例如 {@code javax.annotation.Nullable} 或
	 * {@code edu.umd.cs.findbugs.annotations.Nullable}。
	 */
	private boolean hasNullableAnnotation() {
		for (Annotation ann : getAnnotations()) {
			if ("Nullable".equals(ann.annotationType().getSimpleName())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 返回该依赖项是否为“急切”依赖，
	 * 即尽早解析可能的目标 Bean 以进行类型匹配。
	 */
	public boolean isEager() {
		return this.eager;
	}

	/**
	 * 处理指定的非唯一场景：默认情况下抛出 {@link NoUniqueBeanDefinitionException}。
	 * <p>子类可以重写此方法，从实例中选择一个，或者通过返回 {@code null} 完全放弃。
	 * @param type 请求的 Bean 类型
	 * @param matchingBeans 已预选的指定类型的 Bean 名称与实例映射
	 * （包括限定符等已应用）
	 * @return 选定的 Bean 实例，或 {@code null} 表示无
	 * @throws BeansException 当非唯一场景导致错误时
	 * @since 5.1
	 */
	@Nullable
	public Object resolveNotUnique(ResolvableType type, Map<String, Object> matchingBeans) throws BeansException {
		throw new NoUniqueBeanDefinitionException(type, matchingBeans.keySet());
	}

	/**
	 * 处理指定的非唯一场景：默认情况下抛出 {@link NoUniqueBeanDefinitionException}。
	 * <p>子类可以重写此方法，从实例中选择一个，或者通过返回 {@code null} 完全放弃。
	 * @param type 请求的 Bean 类型
	 * @param matchingBeans 已预选的指定类型的 Bean 名称与实例映射
	 * （包括限定符等已应用）
	 * @return 选定的 Bean 实例，或 {@code null} 表示无
	 * @throws BeansException 当非唯一场景导致错误时
	 * @since 4.3
	 * @deprecated 自 5.1 起，推荐使用 {@link #resolveNotUnique(ResolvableType, Map)}
	 */
	@Deprecated
	@Nullable
	public Object resolveNotUnique(Class<?> type, Map<String, Object> matchingBeans) throws BeansException {
		throw new NoUniqueBeanDefinitionException(type, matchingBeans.keySet());
	}

	/**
	 * 针对给定工厂解析该依赖项的快捷方式，例如考虑一些预解析的信息。
	 * <p>解析算法会先尝试通过此方法解析快捷方式，然后再进入所有 Bean 的常规类型匹配算法。
	 * 子类可以重写此方法，基于预缓存信息提升解析性能，同时仍能获取 {@link InjectionPoint} 暴露等功能。
	 * @param beanFactory 关联的工厂
	 * @return 快捷方式解析结果，如无则返回 {@code null}
	 * @throws BeansException 如果无法获取快捷方式
	 * @since 4.3.1
	 */
	@Nullable
	public Object resolveShortcut(BeanFactory beanFactory) throws BeansException {
		return null;
	}

	/**
	 * 将指定的 Bean 名称解析为 Bean 实例，作为该依赖项匹配算法的候选结果。
	 * <p>默认实现调用 {@link BeanFactory#getBean(String)}。
	 * 子类可以提供额外参数或其他自定义实现。
	 * @param beanName 作为候选结果的 Bean 名称
	 * @param requiredType 期望的 Bean 类型（用于断言）
	 * @param beanFactory 关联的工厂
	 * @return Bean 实例（永不为 {@code null}）
	 * @throws BeansException 如果无法获取 Bean
	 * @since 4.3.2
	 * @see BeanFactory#getBean(String)
	 */
	public Object resolveCandidate(String beanName, Class<?> requiredType, BeanFactory beanFactory)
			throws BeansException {

		return beanFactory.getBean(beanName);
	}


	/**
	 * 增加该描述符的嵌套层级。
	 */
	public void increaseNestingLevel() {
		this.nestingLevel++;
		this.resolvableType = null;
		if (this.methodParameter != null) {
			this.methodParameter = this.methodParameter.nested();
		}
	}

	/**
	 * 可选地设置包含此依赖项的具体类。
	 * 这可能与声明参数/字段的类不同，
	 * 因为它可能是其子类，并可能替换类型变量。
	 * @since 4.0
	 */
	public void setContainingClass(Class<?> containingClass) {
		this.containingClass = containingClass;
		this.resolvableType = null;
		if (this.methodParameter != null) {
			this.methodParameter = this.methodParameter.withContainingClass(containingClass);
		}
	}

	/**
	 * 为封装的参数/字段构建一个 {@link ResolvableType} 对象。
	 * @since 4.0
	 */
	public ResolvableType getResolvableType() {
		ResolvableType resolvableType = this.resolvableType;
		if (resolvableType == null) {
			resolvableType = (this.field != null ?
					ResolvableType.forField(this.field, this.nestingLevel, this.containingClass) :
					ResolvableType.forMethodParameter(obtainMethodParameter()));
			this.resolvableType = resolvableType;
		}
		return resolvableType;
	}

	/**
	 * 为封装的参数/字段构建一个 {@link TypeDescriptor} 对象。
	 * @since 5.1.4
	 */
	public TypeDescriptor getTypeDescriptor() {
		TypeDescriptor typeDescriptor = this.typeDescriptor;
		if (typeDescriptor == null) {
			typeDescriptor = (this.field != null ?
					new TypeDescriptor(getResolvableType(), getDependencyType(), getAnnotations()) :
					new TypeDescriptor(obtainMethodParameter()));
			this.typeDescriptor = typeDescriptor;
		}
		return typeDescriptor;
	}

	/**
	 * 返回是否允许回退匹配。
	 * <p>默认情况下为 {@code false}，但可以重写为 {@code true}，
	 * 以向 {@link org.springframework.beans.factory.support.AutowireCandidateResolver}
	 * 表示也可以接受回退匹配。
	 * @since 4.0
	 */
	public boolean fallbackMatchAllowed() {
		return false;
	}

	/**
	 * 返回该描述符的一个变体，用于回退匹配。
	 * @since 4.0
	 * @see #fallbackMatchAllowed()
	 */
	public DependencyDescriptor forFallbackMatch() {
		return new DependencyDescriptor(this) {
			@Override
			public boolean fallbackMatchAllowed() {
				return true;
			}
		};
	}

	/**
	 * 为底层方法参数（如果有）初始化参数名发现。
	 * <p>此方法在此时不会实际尝试获取参数名；
	 * 它只是允许在应用调用 {@link #getDependencyName()} 时进行发现（如果调用的话）。
	 */
	public void initParameterNameDiscovery(@Nullable ParameterNameDiscoverer parameterNameDiscoverer) {
		if (this.methodParameter != null) {
			this.methodParameter.initParameterNameDiscovery(parameterNameDiscoverer);
		}
	}

	/**
	 * 获取封装参数/字段的名称。
	 * @return 声明的名称（如果无法解析可能为 {@code null}）
	 */
	@Nullable
	public String getDependencyName() {
		return (this.field != null ? this.field.getName() : obtainMethodParameter().getParameterName());
	}

	/**
	 * 获取封装参数/字段的声明类型（非泛型）。
	 * @return 声明类型（永不为 {@code null}）
	 */
	public Class<?> getDependencyType() {
		if (this.field != null) {
			if (this.nestingLevel > 1) {
				Type type = this.field.getGenericType();
				for (int i = 2; i <= this.nestingLevel; i++) {
					if (type instanceof ParameterizedType) {
						Type[] args = ((ParameterizedType) type).getActualTypeArguments();
						type = args[args.length - 1];
					}
				}
				if (type instanceof Class) {
					return (Class<?>) type;
				}
				else if (type instanceof ParameterizedType) {
					Type arg = ((ParameterizedType) type).getRawType();
					if (arg instanceof Class) {
						return (Class<?>) arg;
					}
				}
				return Object.class;
			}
			else {
				return this.field.getType();
			}
		}
		else {
			return obtainMethodParameter().getNestedParameterType();
		}
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!super.equals(other)) {
			return false;
		}
		DependencyDescriptor otherDesc = (DependencyDescriptor) other;
		return (this.required == otherDesc.required && this.eager == otherDesc.eager &&
				this.nestingLevel == otherDesc.nestingLevel && this.containingClass == otherDesc.containingClass);
	}

	@Override
	public int hashCode() {
		return (31 * super.hashCode() + ObjectUtils.nullSafeHashCode(this.containingClass));
	}


	//---------------------------------------------------------------------
	// 序列化支持
	//---------------------------------------------------------------------

	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		// 依赖默认序列化；在反序列化后初始化状态。
		ois.defaultReadObject();

		// 恢复反射句柄（遗憾的是这些不可序列化）
		try {
			if (this.fieldName != null) {
				this.field = this.declaringClass.getDeclaredField(this.fieldName);
			}
			else {
				if (this.methodName != null) {
					this.methodParameter = new MethodParameter(
							this.declaringClass.getDeclaredMethod(this.methodName, this.parameterTypes), this.parameterIndex);
				}
				else {
					this.methodParameter = new MethodParameter(
							this.declaringClass.getDeclaredConstructor(this.parameterTypes), this.parameterIndex);
				}
				for (int i = 1; i < this.nestingLevel; i++) {
					this.methodParameter = this.methodParameter.nested();
				}
			}
		}
		catch (Throwable ex) {
			throw new IllegalStateException("Could not find original class structure", ex);
		}
	}


	/**
	 * 内部类，用于避免在运行时对 Kotlin 的硬依赖。
	 */
	private static class KotlinDelegate {

		/**
		 * 检查指定的 {@link Field} 是否表示可为空的 Kotlin 类型。
		 */
		public static boolean isNullable(Field field) {
			KProperty<?> property = ReflectJvmMapping.getKotlinProperty(field);
			return (property != null && property.getReturnType().isMarkedNullable());
		}
	}

}
