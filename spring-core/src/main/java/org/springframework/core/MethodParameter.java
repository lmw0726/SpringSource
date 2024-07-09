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

package org.springframework.core;

import kotlin.Unit;
import kotlin.reflect.KFunction;
import kotlin.reflect.KParameter;
import kotlin.reflect.jvm.ReflectJvmMapping;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * 辅助类，封装方法参数的规范，即 {@link Method} 或 {@link Constructor} 加上参数索引和声明的泛型类型的嵌套类型索引。
 * 作为传递的规范对象非常有用。
 *
 * <p>从 4.2 版本开始，有一个 {@link org.springframework.core.annotation.SynthesizingMethodParameter}
 * 的子类可用，它合成带有属性别名的注解。该子类主要用于Web和消息端点处理。
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Andy Clement
 * @author Sam Brannen
 * @author Sebastien Deleuze
 * @author Phillip Webb
 * @see org.springframework.core.annotation.SynthesizingMethodParameter
 * @since 2.0
 */
public class MethodParameter {
	/**
	 * 空的注解数组
	 */
	private static final Annotation[] EMPTY_ANNOTATION_ARRAY = new Annotation[0];

	/**
	 * 执行对象
	 */
	private final Executable executable;

	/**
	 * 参数索引
	 */
	private final int parameterIndex;

	/**
	 * 参数对象
	 */
	@Nullable
	private volatile Parameter parameter;

	/**
	 * 嵌套级别
	 */
	private int nestingLevel;

	/**
	 * Integer级别到Integer类型索引的映射。
	 */
	@Nullable
	Map<Integer, Integer> typeIndexesPerLevel;

	/**
	 * 包含的类。也可以通过覆盖 {@link #getContainingClass()} 方法来提供。
	 */
	@Nullable
	private volatile Class<?> containingClass;

	/**
	 * 参数类型
	 */
	@Nullable
	private volatile Class<?> parameterType;

	/**
	 * 泛型参数类型
	 */
	@Nullable
	private volatile Type genericParameterType;

	/**
	 * 参数上的注解数组
	 */
	@Nullable
	private volatile Annotation[] parameterAnnotations;

	/**
	 * 参数名称发现者
	 */
	@Nullable
	private volatile ParameterNameDiscoverer parameterNameDiscoverer;

	/**
	 * 参数名
	 */
	@Nullable
	private volatile String parameterName;

	/**
	 * 嵌套的方法参数
	 */
	@Nullable
	private volatile MethodParameter nestedMethodParameter;


	/**
	 * 为给定的方法创建一个新的 {@code MethodParameter}，嵌套级别为 1。
	 *
	 * @param method         要指定参数的方法
	 * @param parameterIndex 参数的索引：-1 表示方法返回类型；0 表示第一个方法参数；1 表示第二个方法参数，依此类推
	 */
	public MethodParameter(Method method, int parameterIndex) {
		this(method, parameterIndex, 1);
	}

	/**
	 * 为给定的方法创建一个新的 {@code MethodParameter}。
	 *
	 * @param method         要指定参数的方法
	 * @param parameterIndex 参数的索引：-1 表示方法返回类型；0 表示第一个方法参数；1 表示第二个方法参数，依此类推
	 * @param nestingLevel   目标类型的嵌套级别
	 *                       （通常为 1；例如，对于列表的列表，1 表示嵌套列表，而 2 表示嵌套列表的元素）
	 */
	public MethodParameter(Method method, int parameterIndex, int nestingLevel) {
		Assert.notNull(method, "Method must not be null");
		this.executable = method;
		this.parameterIndex = validateIndex(method, parameterIndex);
		this.nestingLevel = nestingLevel;
	}

	/**
	 * 为给定的构造函数创建一个新的 MethodParameter，嵌套级别为 1。
	 *
	 * @param constructor    要指定参数的构造函数
	 * @param parameterIndex 参数的索引
	 */
	public MethodParameter(Constructor<?> constructor, int parameterIndex) {
		this(constructor, parameterIndex, 1);
	}

	/**
	 * 为给定的构造函数创建一个新的 MethodParameter。
	 *
	 * @param constructor    要指定参数的构造函数
	 * @param parameterIndex 参数的索引
	 * @param nestingLevel   目标类型的嵌套级别
	 *                       （通常为 1；例如，对于列表的列表，1 表示嵌套列表，而 2 表示嵌套列表的元素）
	 */
	public MethodParameter(Constructor<?> constructor, int parameterIndex, int nestingLevel) {
		Assert.notNull(constructor, "Constructor must not be null");
		this.executable = constructor;
		this.parameterIndex = validateIndex(constructor, parameterIndex);
		this.nestingLevel = nestingLevel;
	}

	/**
	 * 用于创建 {@link MethodParameter} 的内部构造函数，已设置包含类。
	 *
	 * @param executable      要指定参数的可执行体
	 * @param parameterIndex  参数的索引
	 * @param containingClass 包含类
	 * @since 5.2
	 */
	MethodParameter(Executable executable, int parameterIndex, @Nullable Class<?> containingClass) {
		Assert.notNull(executable, "Executable must not be null");
		this.executable = executable;
		this.parameterIndex = validateIndex(executable, parameterIndex);
		this.nestingLevel = 1;
		this.containingClass = containingClass;
	}

	/**
	 * 复制构造函数，生成一个独立的 MethodParameter 对象，
	 * 基于与原始对象相同的元数据和缓存状态。
	 *
	 * @param original 要复制的原始 MethodParameter 对象
	 */
	public MethodParameter(MethodParameter original) {
		Assert.notNull(original, "Original must not be null");
		this.executable = original.executable;
		this.parameterIndex = original.parameterIndex;
		this.parameter = original.parameter;
		this.nestingLevel = original.nestingLevel;
		this.typeIndexesPerLevel = original.typeIndexesPerLevel;
		this.containingClass = original.containingClass;
		this.parameterType = original.parameterType;
		this.genericParameterType = original.genericParameterType;
		this.parameterAnnotations = original.parameterAnnotations;
		this.parameterNameDiscoverer = original.parameterNameDiscoverer;
		this.parameterName = original.parameterName;
	}


	/**
	 * 返回封装的 Method，如果有的话。
	 * <p>注意：Method 或 Constructor 中只有一个可用。
	 *
	 * @return Method，如果没有则返回 {@code null}
	 */
	@Nullable
	public Method getMethod() {
		return (this.executable instanceof Method ? (Method) this.executable : null);
	}

	/**
	 * 返回封装的构造函数（如果有）。
	 * <p>注意：Method 或 Constructor 中只有一个可用。
	 *
	 * @return 构造函数，如果没有则返回 {@code null}
	 */
	@Nullable
	public Constructor<?> getConstructor() {
		return (this.executable instanceof Constructor ? (Constructor<?>) this.executable : null);
	}

	/**
	 * 返回声明底层 Method 或 Constructor 的类。
	 */
	public Class<?> getDeclaringClass() {
		return this.executable.getDeclaringClass();
	}

	/**
	 * 返回封装的成员。
	 *
	 * @return Method 或 Constructor 作为 Member
	 */
	public Member getMember() {
		return this.executable;
	}

	/**
	 * 返回封装的注解元素。
	 * <p>注意：该方法暴露了在方法/构造函数本身上声明的注解（即方法/构造函数级别，而不是参数级别）。
	 *
	 * @return Method 或 Constructor 作为 AnnotatedElement
	 */
	public AnnotatedElement getAnnotatedElement() {
		return this.executable;
	}

	/**
	 * 返回封装的可执行体。
	 *
	 * @return Method 或 Constructor 作为 Executable
	 * @since 5.0
	 */
	public Executable getExecutable() {
		return this.executable;
	}

	/**
	 * 返回方法/构造函数参数的 {@link Parameter} 描述符。
	 *
	 * @since 5.0
	 */
	public Parameter getParameter() {
		// 如果参数索引小于0
		if (this.parameterIndex < 0) {
			// 抛出IllegalStateException异常，表示无法检索方法返回类型的参数描述符
			throw new IllegalStateException("Cannot retrieve Parameter descriptor for method return type");
		}

		// 获取参数
		Parameter parameter = this.parameter;

		// 如果参数为null
		if (parameter == null) {
			// 从可执行对象的参数列表中获取参数
			parameter = getExecutable().getParameters()[this.parameterIndex];
			// 并赋值给当前参数
			this.parameter = parameter;
		}

		// 返回参数
		return parameter;
	}

	/**
	 * 返回方法/构造函数参数的索引。
	 *
	 * @return 参数索引（如果是返回类型则返回 -1）
	 */
	public int getParameterIndex() {
		return this.parameterIndex;
	}

	/**
	 * 增加此参数的嵌套级别。
	 *
	 * @see #getNestingLevel()
	 * @deprecated 自 5.2 起弃用，建议使用 {@link #nested(Integer)}
	 */
	@Deprecated
	public void increaseNestingLevel() {
		this.nestingLevel++;
	}

	/**
	 * 减少此参数的嵌套级别。
	 *
	 * @see #getNestingLevel()
	 * @deprecated 自 5.2 起弃用，建议保留原始的 MethodParameter，并在需要时使用 {@link #nested(Integer)}
	 */
	@Deprecated
	public void decreaseNestingLevel() {
		// 移除嵌套级别
		getTypeIndexesPerLevel().remove(this.nestingLevel);
		// 嵌套级别减一
		this.nestingLevel--;
	}

	/**
	 * 返回目标类型的嵌套级别
	 * （通常为 1；例如，对于 List<List>，1 表示嵌套的 List，而 2 表示嵌套 List 的元素）。
	 */
	public int getNestingLevel() {
		return this.nestingLevel;
	}

	/**
	 * 返回具有当前级别类型设置为指定值的 {@code MethodParameter} 变体。
	 *
	 * @param typeIndex 新的类型索引
	 * @since 5.2
	 */
	public MethodParameter withTypeIndex(int typeIndex) {
		return nested(this.nestingLevel, typeIndex);
	}

	/**
	 * 设置当前嵌套级别的类型索引。
	 *
	 * @param typeIndex 对应的类型索引（如果要使用默认类型索引，则为 {@code null}）
	 * @see #getNestingLevel()
	 * @deprecated 自 5.2 起弃用，建议使用 {@link #withTypeIndex}
	 */
	@Deprecated
	public void setTypeIndexForCurrentLevel(int typeIndex) {
		getTypeIndexesPerLevel().put(this.nestingLevel, typeIndex);
	}

	/**
	 * 返回当前嵌套级别的类型索引。
	 *
	 * @return 对应的类型索引，如果未指定则返回 {@code null}（表示默认类型索引）
	 * @see #getNestingLevel()
	 */
	@Nullable
	public Integer getTypeIndexForCurrentLevel() {
		return getTypeIndexForLevel(this.nestingLevel);
	}

	/**
	 * 返回指定嵌套级别的类型索引。
	 *
	 * @param nestingLevel 要检查的嵌套级别
	 * @return 对应的类型索引，如果未指定则返回 {@code null}（表示默认类型索引）
	 */
	@Nullable
	public Integer getTypeIndexForLevel(int nestingLevel) {
		return getTypeIndexesPerLevel().get(nestingLevel);
	}

	/**
	 * 获取（延迟构建的）每个级别类型索引的 Map。
	 */
	private Map<Integer, Integer> getTypeIndexesPerLevel() {
		if (this.typeIndexesPerLevel == null) {
			// 如果Integer级别到Integer类型索引的映射 为空，则创建一个空的Map
			this.typeIndexesPerLevel = new HashMap<>(4);
		}
		return this.typeIndexesPerLevel;
	}

	/**
	 * 返回指向相同参数但嵌套级别更深的 {@code MethodParameter} 变体。
	 *
	 * @since 4.3
	 */
	public MethodParameter nested() {
		return nested(null);
	}

	/**
	 * 返回指向相同参数但嵌套级别更深的 {@code MethodParameter} 变体。
	 *
	 * @param typeIndex 新嵌套级别的类型索引
	 * @since 5.2
	 */
	public MethodParameter nested(@Nullable Integer typeIndex) {
		// 获取嵌套的方法参数对象
		MethodParameter nestedParam = this.nestedMethodParameter;

		// 如果嵌套参数对象存在且类型索引为null
		if (nestedParam != null && typeIndex == null) {
			// 返回嵌套参数对象
			return nestedParam;
		}

		// 创建新的嵌套参数对象，增加嵌套级别并使用类型索引
		nestedParam = nested(this.nestingLevel + 1, typeIndex);

		// 如果类型索引为null
		if (typeIndex == null) {
			// 将新的嵌套参数对象赋值给this.nestedMethodParameter
			this.nestedMethodParameter = nestedParam;
		}

		// 返回新的嵌套参数对象
		return nestedParam;
	}

	private MethodParameter nested(int nestingLevel, @Nullable Integer typeIndex) {
		// 克隆当前对象，创建一个副本
		MethodParameter copy = clone();

		// 设置副本的嵌套级别
		copy.nestingLevel = nestingLevel;

		// 如果当前对象的类型索引映射不为空
		if (this.typeIndexesPerLevel != null) {
			// 创建一个新的类型索引映射，并将当前对象的映射内容复制到副本中
			copy.typeIndexesPerLevel = new HashMap<>(this.typeIndexesPerLevel);
		}

		// 如果类型索引不为空
		if (typeIndex != null) {
			// 将类型索引存入副本的类型索引映射中，键为嵌套级别
			copy.getTypeIndexesPerLevel().put(copy.nestingLevel, typeIndex);
		}

		// 将副本的参数类型设为null
		copy.parameterType = null;

		// 将副本的通用参数类型设为null
		copy.genericParameterType = null;

		// 返回副本对象
		return copy;
	}

	/**
	 * 返回此方法是否指示参数为非必需项：
	 * 可能是 Java 8 的 {@link java.util.Optional} 形式，任何参数级别的 {@code Nullable} 注解的变体
	 * （例如来自 JSR-305 或 FindBugs 注解集合），或者 Kotlin 中的语言级可空类型声明或 {@code Continuation} 参数。
	 *
	 * @since 4.3
	 */
	public boolean isOptional() {
		// 返回一个布尔值，表示参数是否为Optional类型或具有可空注解，
		// 或者如果参数属于Kotlin类型且为可选参数
		return (getParameterType() == Optional.class || hasNullableAnnotation() ||
				(KotlinDetector.isKotlinReflectPresent() &&
						KotlinDetector.isKotlinType(getContainingClass()) &&
						KotlinDelegate.isOptional(this)));
	}

	/**
	 * 检查此方法参数是否用任何 {@code Nullable} 注解的变体进行了注解，
	 * 例如 {@code javax.annotation.Nullable} 或 {@code edu.umd.cs.findbugs.annotations.Nullable}。
	 */
	private boolean hasNullableAnnotation() {
		// 遍历参数的所有注解
		for (Annotation ann : getParameterAnnotations()) {
			// 如果注解的简单名称为"Nullable"
			if ("Nullable".equals(ann.annotationType().getSimpleName())) {
				// 返回true，表示参数具有Nullable注解
				return true;
			}
		}

		// 如果没有找到Nullable注解，返回false
		return false;
	}

	/**
	 * 如果是 {@link java.util.Optional} 声明，则返回指向相同参数但更深一级嵌套的 {@code MethodParameter} 变体。
	 *
	 * @see #isOptional()
	 * @see #nested()
	 * @since 4.3
	 */
	public MethodParameter nestedIfOptional() {
		return (getParameterType() == Optional.class ? nested() : this);
	}

	/**
	 * 返回指向相同参数但引用给定包含类的 {@code MethodParameter} 变体。
	 *
	 * @param containingClass 具体的包含类（可能是声明类的子类，例如替换类型变量）
	 * @see #getParameterType()
	 * @since 5.2
	 */
	public MethodParameter withContainingClass(@Nullable Class<?> containingClass) {
		MethodParameter result = clone();
		result.containingClass = containingClass;
		result.parameterType = null;
		return result;
	}

	/**
	 * 设置一个包含类来解析参数类型。
	 */
	@Deprecated
	void setContainingClass(Class<?> containingClass) {
		this.containingClass = containingClass;
		this.parameterType = null;
	}

	/**
	 * 返回此方法参数的包含类。
	 *
	 * @return 特定的包含类（可能是声明类的子类），或者简单地返回声明类本身
	 * @see #getDeclaringClass()
	 */
	public Class<?> getContainingClass() {
		Class<?> containingClass = this.containingClass;
		return (containingClass != null ? containingClass : getDeclaringClass());
	}

	/**
	 * 设置已解析（泛型）参数类型。
	 */
	@Deprecated
	void setParameterType(@Nullable Class<?> parameterType) {
		this.parameterType = parameterType;
	}

	/**
	 * 返回方法/构造函数参数的类型。
	 *
	 * @return 参数类型（永远不会为 {@code null}）
	 */
	public Class<?> getParameterType() {
		// 获取参数类型
		Class<?> paramType = this.parameterType;

		// 如果参数类型不为null，直接返回
		if (paramType != null) {
			return paramType;
		}

		// 如果包含参数的类与声明参数的类不一致
		if (getContainingClass() != getDeclaringClass()) {
			// 解析方法参数的可解析类型，并尝试获取其实际类型
			paramType = ResolvableType.forMethodParameter(this, null, 1).resolve();
		}

		// 如果解析后参数类型仍为null
		if (paramType == null) {
			// 计算参数类型
			paramType = computeParameterType();
		}

		// 缓存参数类型
		this.parameterType = paramType;

		// 返回参数类型
		return paramType;
	}

	/**
	 * 返回方法/构造函数参数的泛型类型。
	 *
	 * @return 参数类型（永远不会为 {@code null}）
	 * @since 3.0
	 */
	public Type getGenericParameterType() {
		// 获取参数的通用类型
		Type paramType = this.genericParameterType;

		// 如果通用类型为null
		if (paramType == null) {
			// 如果参数索引小于0，表示是方法的返回类型
			if (this.parameterIndex < 0) {
				// 获取方法对象
				Method method = getMethod();
				paramType = (method != null ?
						// 如果是Kotlin类型，获取Kotlin的通用返回类型，否则获取Java的通用返回类型
						(KotlinDetector.isKotlinReflectPresent() && KotlinDetector.isKotlinType(getContainingClass()) ?
								KotlinDelegate.getGenericReturnType(method) : method.getGenericReturnType()) : void.class);
			} else {
				// 获取可执行对象的通用参数类型数组
				Type[] genericParameterTypes = this.executable.getGenericParameterTypes();
				int index = this.parameterIndex;

				// 如果可执行对象是构造函数且声明的类是内部类，且通用参数类型长度比参数个数少1
				if (this.executable instanceof Constructor &&
						ClassUtils.isInnerClass(this.executable.getDeclaringClass()) &&
						genericParameterTypes.length == this.executable.getParameterCount() - 1) {
					// javac中的一个bug：对于至少有一个泛型构造函数参数的内部类，类型数组排除了包含实例参数，因此通过将实际参数索引降低1访问它
					index = this.parameterIndex - 1;
				}

				// 获取对应索引的通用参数类型，如果索引无效则计算参数类型
				paramType = (index >= 0 && index < genericParameterTypes.length ?
						genericParameterTypes[index] : computeParameterType());
			}

			// 缓存通用参数类型
			this.genericParameterType = paramType;
		}

		// 返回通用参数类型
		return paramType;
	}

	private Class<?> computeParameterType() {
		// 如果参数索引小于0，表示需要获取方法的返回类型
		if (this.parameterIndex < 0) {
			// 获取方法对象
			Method method = getMethod();

			// 如果方法对象为null，返回void.class
			if (method == null) {
				return void.class;
			}

			// 如果检测到Kotlin反射存在且包含类是Kotlin类型，返回Kotlin的返回类型
			if (KotlinDetector.isKotlinReflectPresent() && KotlinDetector.isKotlinType(getContainingClass())) {
				return KotlinDelegate.getReturnType(method);
			}

			// 否则返回Java的返回类型
			return method.getReturnType();
		}

		// 如果参数索引大于等于0，返回该索引位置的参数类型
		return this.executable.getParameterTypes()[this.parameterIndex];
	}

	/**
	 * 返回方法/构造函数参数的嵌套类型。
	 *
	 * @return 参数类型（永远不会为 {@code null}）
	 * @see #getNestingLevel()
	 * @since 3.1
	 */
	public Class<?> getNestedParameterType() {
		// 如果嵌套级别大于1
		if (this.nestingLevel > 1) {
			// 获取通用参数类型
			Type type = getGenericParameterType();

			// 遍历嵌套级别，从2到当前嵌套级别
			for (int i = 2; i <= this.nestingLevel; i++) {
				// 如果类型是参数化类型
				if (type instanceof ParameterizedType) {
					// 获取实际类型参数
					Type[] args = ((ParameterizedType) type).getActualTypeArguments();
					// 获取当前嵌套级别的类型索引
					Integer index = getTypeIndexForLevel(i);
					// 根据索引获取类型参数，若索引为null，则取最后一个参数
					type = args[index != null ? index : args.length - 1];
				}
				// TODO: 如果无法解析类型，则返回 Object.class
			}

			// 如果类型是Class对象，返回该类型
			if (type instanceof Class) {
				return (Class<?>) type;
			} else if (type instanceof ParameterizedType) {
				// 如果类型是参数化类型，获取其原始类型
				Type arg = ((ParameterizedType) type).getRawType();
				// 如果原始类型是Class对象，返回该类型
				if (arg instanceof Class) {
					return (Class<?>) arg;
				}
			}
			// 默认返回Object.class
			return Object.class;
		} else {
			// 如果嵌套级别小于等于1，返回参数类型
			return getParameterType();
		}
	}

	/**
	 * 返回方法/构造函数参数的嵌套泛型类型。
	 *
	 * @return 参数类型（永远不会为 {@code null}）
	 * @see #getNestingLevel()
	 * @since 4.2
	 */
	public Type getNestedGenericParameterType() {
		// 如果嵌套级别大于1
		if (this.nestingLevel > 1) {
			// 获取通用参数类型
			Type type = getGenericParameterType();

			// 遍历嵌套级别，从2到当前嵌套级别
			for (int i = 2; i <= this.nestingLevel; i++) {
				// 如果类型是参数化类型
				if (type instanceof ParameterizedType) {
					// 获取实际类型参数
					Type[] args = ((ParameterizedType) type).getActualTypeArguments();
					// 获取当前嵌套级别的类型索引
					Integer index = getTypeIndexForLevel(i);
					// 根据索引获取类型参数，若索引为null，则取最后一个参数
					type = args[index != null ? index : args.length - 1];
				}
			}

			// 返回最终的类型
			return type;
		} else {
			// 如果嵌套级别小于等于1，返回通用参数类型
			return getGenericParameterType();
		}
	}

	/**
	 * 返回与目标方法/构造函数本身关联的注解。
	 */
	public Annotation[] getMethodAnnotations() {
		return adaptAnnotationArray(getAnnotatedElement().getAnnotations());
	}

	/**
	 * 返回给定类型的方法/构造函数注解，如果可用的话。
	 *
	 * @param annotationType 要查找的注解类型
	 * @return 注解对象，如果未找到则返回 {@code null}
	 */
	@Nullable
	public <A extends Annotation> A getMethodAnnotation(Class<A> annotationType) {
		// 获取注解元素上的指定类型的注解
		A annotation = getAnnotatedElement().getAnnotation(annotationType);

		// 如果找到了注解，适配并返回它；否则返回null
		return (annotation != null ? adaptAnnotation(annotation) : null);
	}

	/**
	 * 返回方法/构造函数是否用指定类型注解。
	 *
	 * @param annotationType 要查找的注解类型
	 * @see #getMethodAnnotation(Class)
	 * @since 4.3
	 */
	public <A extends Annotation> boolean hasMethodAnnotation(Class<A> annotationType) {
		return getAnnotatedElement().isAnnotationPresent(annotationType);
	}

	/**
	 * 返回与特定方法/构造函数参数关联的注解。
	 *
	 * @return 参数注解数组
	 */
	public Annotation[] getParameterAnnotations() {
		// 获取参数的注解数组
		Annotation[] paramAnns = this.parameterAnnotations;

		// 如果注解数组为null
		if (paramAnns == null) {
			// 获取可执行对象的参数注解二维数组
			Annotation[][] annotationArray = this.executable.getParameterAnnotations();
			int index = this.parameterIndex;

			// 如果可执行对象是构造函数且声明的类是内部类，且注解数组长度比参数个数少1
			if (this.executable instanceof Constructor &&
					ClassUtils.isInnerClass(this.executable.getDeclaringClass()) &&
					annotationArray.length == this.executable.getParameterCount() - 1) {
				// JDK < 9 的 javac 中存在的问题：对于内部类，注解数组会排除包含实例参数，
				// 因此通过降低实际参数索引来访问它
				index = this.parameterIndex - 1;
			}

			// 根据索引获取适配后的注解数组，若索引无效则返回空注解数组
			paramAnns = (index >= 0 && index < annotationArray.length ?
					adaptAnnotationArray(annotationArray[index]) : EMPTY_ANNOTATION_ARRAY);

			// 缓存参数的注解数组
			this.parameterAnnotations = paramAnns;
		}

		// 返回参数的注解数组
		return paramAnns;
	}

	/**
	 * 如果参数至少有一个注解，则返回 {@code true}，否则返回 {@code false}。
	 *
	 * @return 参数是否有注解
	 * @see #getParameterAnnotations()
	 */
	public boolean hasParameterAnnotations() {
		return (getParameterAnnotations().length != 0);
	}

	/**
	 * 返回给定类型的参数注解，如果存在的话。
	 *
	 * @param annotationType 要查找的注解类型
	 * @return 注解对象，如果未找到则返回 {@code null}
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	public <A extends Annotation> A getParameterAnnotation(Class<A> annotationType) {
		// 获取参数的注解数组
		Annotation[] anns = getParameterAnnotations();

		// 遍历注解数组，查找指定类型的注解
		for (Annotation ann : anns) {
			// 如果找到了符合条件的注解，将其转换为指定类型并返回
			if (annotationType.isInstance(ann)) {
				return (A) ann;
			}
		}

		// 如果未找到符合条件的注解，返回null
		return null;
	}

	/**
	 * 返回参数是否声明了给定类型的注解。
	 *
	 * @param annotationType 要查找的注解类型
	 * @see #getParameterAnnotation(Class)
	 */
	public <A extends Annotation> boolean hasParameterAnnotation(Class<A> annotationType) {
		return (getParameterAnnotation(annotationType) != null);
	}

	/**
	 * 初始化此方法参数的参数名称发现。
	 * <p>此方法实际上并不会在此时尝试检索参数名称；它只是在应用程序调用
	 * {@link #getParameterName()} 时（如果有的话）允许发现发生。
	 */
	public void initParameterNameDiscovery(@Nullable ParameterNameDiscoverer parameterNameDiscoverer) {
		this.parameterNameDiscoverer = parameterNameDiscoverer;
	}

	/**
	 * 返回方法/构造函数参数的名称。
	 *
	 * @return 参数名称（如果类文件中不包含参数名称元数据或未设置
	 * {@link #initParameterNameDiscovery ParameterNameDiscoverer}，则可能为 {@code null}）
	 */
	@Nullable
	public String getParameterName() {
		// 如果参数索引小于0，返回null
		if (this.parameterIndex < 0) {
			return null;
		}

		// 获取参数名发现器
		ParameterNameDiscoverer discoverer = this.parameterNameDiscoverer;

		// 如果发现器不为null
		if (discoverer != null) {
			String[] parameterNames = null;

			// 如果可执行对象是方法
			if (this.executable instanceof Method) {
				parameterNames = discoverer.getParameterNames((Method) this.executable);
			}
			// 如果可执行对象是构造函数
			else if (this.executable instanceof Constructor) {
				parameterNames = discoverer.getParameterNames((Constructor<?>) this.executable);
			}

			// 如果成功获取到参数名数组
			if (parameterNames != null) {
				// 获取当前参数的名称
				this.parameterName = parameterNames[this.parameterIndex];
			}

			// 清空参数名发现器，避免重复使用
			this.parameterNameDiscoverer = null;
		}

		// 返回参数名称
		return this.parameterName;
	}


	/**
	 * 一个模板方法，用于在将给定的注解实例返回给调用者之前对其进行后处理。
	 * <p>默认实现只是按原样返回给定的注解。
	 *
	 * @param annotation 即将返回的注解
	 * @return 后处理的注解（或只是原始注解）
	 * @since 4.2
	 */
	protected <A extends Annotation> A adaptAnnotation(A annotation) {
		return annotation;
	}

	/**
	 * 一个模板方法，用于在将给定的注解数组返回给调用者之前对其进行后处理。
	 * <p>默认实现只是按原样返回给定的注解数组。
	 *
	 * @param annotations 即将返回的注解数组
	 * @return 后处理的注解数组（或只是原始注解数组）
	 * @since 4.2
	 */
	protected Annotation[] adaptAnnotationArray(Annotation[] annotations) {
		return annotations;
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof MethodParameter)) {
			return false;
		}
		MethodParameter otherParam = (MethodParameter) other;
		return (getContainingClass() == otherParam.getContainingClass() &&
				ObjectUtils.nullSafeEquals(this.typeIndexesPerLevel, otherParam.typeIndexesPerLevel) &&
				this.nestingLevel == otherParam.nestingLevel &&
				this.parameterIndex == otherParam.parameterIndex &&
				this.executable.equals(otherParam.executable));
	}

	@Override
	public int hashCode() {
		return (31 * this.executable.hashCode() + this.parameterIndex);
	}

	@Override
	public String toString() {
		Method method = getMethod();
		return (method != null ? "method '" + method.getName() + "'" : "constructor") +
				" parameter " + this.parameterIndex;
	}

	@Override
	public MethodParameter clone() {
		return new MethodParameter(this);
	}

	/**
	 * 为给定的方法或构造函数创建一个新的 MethodParameter。
	 * <p>这是一个便捷的工厂方法，适用于在通用情况下处理方法或构造函数引用的场景。
	 *
	 * @param methodOrConstructor 指定参数的方法或构造函数
	 * @param parameterIndex      参数的索引
	 * @return 对应的 MethodParameter 实例
	 * @deprecated 自 5.0 起，建议使用 {@link #forExecutable}
	 */
	@Deprecated
	public static MethodParameter forMethodOrConstructor(Object methodOrConstructor, int parameterIndex) {
		// 如果传入的对象不是一个可执行对象（既不是方法也不是构造函数）
		if (!(methodOrConstructor instanceof Executable)) {
			// 抛出IllegalArgumentException异常
			throw new IllegalArgumentException(
					"Given object [" + methodOrConstructor + "] is neither a Method nor a Constructor");
		}

		// 将对象转换为可执行对象，并调用forExecutable方法，返回其结果
		return forExecutable((Executable) methodOrConstructor, parameterIndex);
	}

	/**
	 * 为给定的方法或构造函数创建一个新的 MethodParameter。
	 * <p>这是一个便捷的工厂方法，适用于在通用情况下处理方法或构造函数引用的场景。
	 *
	 * @param executable     指定参数的方法或构造函数
	 * @param parameterIndex 参数的索引
	 * @return 对应的 MethodParameter 实例
	 * @since 5.0
	 */
	public static MethodParameter forExecutable(Executable executable, int parameterIndex) {
		// 如果可执行对象是方法
		if (executable instanceof Method) {
			// 返回方法参数对象
			return new MethodParameter((Method) executable, parameterIndex);
		} else if (executable instanceof Constructor) {
			// 如果可执行对象是构造函数，返回构造函数参数对象
			return new MethodParameter((Constructor<?>) executable, parameterIndex);
		} else {
			// 如果可执行对象既不是方法也不是构造函数，抛出IllegalArgumentException异常
			throw new IllegalArgumentException("Not a Method/Constructor: " + executable);
		}
	}

	/**
	 * 为给定的参数描述符创建一个新的 MethodParameter。
	 * <p>这是一个便捷的工厂方法，适用于已存在 Java 8 {@link Parameter} 描述符的场景。
	 *
	 * @param parameter 参数描述符
	 * @return 对应的 MethodParameter 实例
	 * @since 5.0
	 */
	public static MethodParameter forParameter(Parameter parameter) {
		return forExecutable(parameter.getDeclaringExecutable(), findParameterIndex(parameter));
	}

	protected static int findParameterIndex(Parameter parameter) {
		// 获取参数所属的可执行对象（方法或构造函数）
		Executable executable = parameter.getDeclaringExecutable();

		// 获取可执行对象的所有参数
		Parameter[] allParams = executable.getParameters();

		// 首先尝试使用身份检查来提高性能
		for (int i = 0; i < allParams.length; i++) {
			if (parameter == allParams[i]) {
				// 如果找到匹配的参数，返回其索引
				return i;
			}
		}

		// 如果身份检查未找到匹配的参数，可能需要再次尝试使用对象相等性检查，
		// 以避免在调用java.lang.reflect.Executable.getParameters()时的竞态条件
		for (int i = 0; i < allParams.length; i++) {
			if (parameter.equals(allParams[i])) {
				// 如果找到匹配的参数，返回其索引
				return i;
			}
		}

		// 如果给定的参数未匹配到任何声明的参数，则抛出IllegalArgumentException异常
		throw new IllegalArgumentException("Given parameter [" + parameter +
				"] does not match any parameter in the declaring executable");
	}

	private static int validateIndex(Executable executable, int parameterIndex) {
		// 获取可执行对象的参数个数
		int count = executable.getParameterCount();

		// 断言参数索引在有效范围内
		Assert.isTrue(parameterIndex >= -1 && parameterIndex < count,
				() -> "Parameter index needs to be between -1 and " + (count - 1));

		// 返回参数索引
		return parameterIndex;
	}


	/**
	 * 内部类，用于在运行时避免对 Kotlin 的硬依赖。
	 */
	private static class KotlinDelegate {

		/**
		 * 检查指定的 {@link MethodParameter} 是否表示一个可为空的 Kotlin 类型、
		 * 一个可选参数（在 Kotlin 声明中具有默认值）或用于挂起函数的 {@code Continuation} 参数。
		 *
		 * @param param 方法参数
		 * @return 如果是可选的或挂起函数的 {@code Continuation} 参数，则返回 true
		 */
		public static boolean isOptional(MethodParameter param) {
			// 获取参数对应的方法
			Method method = param.getMethod();
			int index = param.getParameterIndex();

			// 如果方法存在且参数索引为-1
			if (method != null && index == -1) {
				// 获取与方法对应的Kotlin函数
				KFunction<?> function = ReflectJvmMapping.getKotlinFunction(method);
				// 返回Kotlin函数的返回类型是否被标记为可空
				return (function != null && function.getReturnType().isMarkedNullable());
			}

			KFunction<?> function;
			Predicate<KParameter> predicate;

			// 如果方法存在
			if (method != null) {
				// 如果参数类型为Kotlin的Continuation
				if (param.getParameterType().getName().equals("kotlin.coroutines.Continuation")) {
					return true;
				}
				// 获取与方法对应的Kotlin函数
				function = ReflectJvmMapping.getKotlinFunction(method);
				// 设置谓词为KParameter.Kind.VALUE
				predicate = p -> KParameter.Kind.VALUE.equals(p.getKind());
			} else {
				// 否则获取参数对应的构造函数
				Constructor<?> ctor = param.getConstructor();
				Assert.state(ctor != null, "Neither method nor constructor found");
				// 获取与构造函数对应的Kotlin函数
				function = ReflectJvmMapping.getKotlinFunction(ctor);
				// 设置谓词为KParameter.Kind.VALUE或KParameter.Kind.INSTANCE
				predicate = p -> (KParameter.Kind.VALUE.equals(p.getKind()) ||
						KParameter.Kind.INSTANCE.equals(p.getKind()));
			}

			// 如果Kotlin函数存在
			if (function != null) {
				int i = 0;
				// 遍历Kotlin函数的参数
				for (KParameter kParameter : function.getParameters()) {
					// 如果参数满足谓词条件
					if (predicate.test(kParameter)) {
						// 如果参数索引与当前索引匹配
						if (index == i++) {
							// 返回参数类型是否被标记为可空或参数是否为可选
							return (kParameter.getType().isMarkedNullable() || kParameter.isOptional());
						}
					}
				}
			}

			// 如果以上条件均不满足，返回false
			return false;
		}

		/**
		 * 返回方法的泛型返回类型，支持通过 Kotlin 反射挂起函数。
		 *
		 * @param method 方法
		 * @return 泛型返回类型
		 */
		private static Type getGenericReturnType(Method method) {
			try {
				// 获取与方法对应的Kotlin函数
				KFunction<?> function = ReflectJvmMapping.getKotlinFunction(method);

				// 如果Kotlin函数存在且是挂起函数
				if (function != null && function.isSuspend()) {
					// 返回Kotlin函数返回类型对应的Java类型
					return ReflectJvmMapping.getJavaType(function.getReturnType());
				}
			} catch (UnsupportedOperationException ex) {
				// 可能是一个合成类 - 让我们使用 Java 反射代替
			}

			// 返回方法的通用返回类型
			return method.getGenericReturnType();
		}

		/**
		 * 返回方法的返回类型，支持通过 Kotlin 反射挂起函数。
		 *
		 * @param method 方法
		 * @return 返回类型
		 */
		private static Class<?> getReturnType(Method method) {
			try {
				// 获取与方法对应的Kotlin函数
				KFunction<?> function = ReflectJvmMapping.getKotlinFunction(method);
				if (function != null && function.isSuspend()) {
					// 获取Kotlin函数返回类型对应的Java类型
					Type paramType = ReflectJvmMapping.getJavaType(function.getReturnType());
					if (paramType == Unit.class) {
						// 如果返回类型是Kotlin的Unit，转换为Java的void类型
						paramType = void.class;
					}
					// 返回可解析类型的实际返回类型
					return ResolvableType.forType(paramType).resolve(method.getReturnType());
				}
			} catch (UnsupportedOperationException ex) {
				// 可能是一个合成类 - 让我们使用 Java 反射代替
			}
			// 返回方法的原始返回类型
			return method.getReturnType();
		}
	}

}
