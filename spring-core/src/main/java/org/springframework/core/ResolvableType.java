/*
 * Copyright 2002-2022 the original author or authors.
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

import org.springframework.core.SerializableTypeWrapper.FieldTypeProvider;
import org.springframework.core.SerializableTypeWrapper.MethodParameterTypeProvider;
import org.springframework.core.SerializableTypeWrapper.TypeProvider;
import org.springframework.lang.Nullable;
import org.springframework.util.*;

import java.io.Serializable;
import java.lang.reflect.*;
import java.util.*;

/**
 * 封装了一个 Java {@link java.lang.reflect.Type}，提供访问
 * {@link #getSuperType() 父类型}、{@link #getInterfaces() 接口}和
 * {@link #getGeneric(int...) 泛型参数} 的能力，并最终能
 * {@link #resolve() 解析}为一个 {@link java.lang.Class}。
 *
 * <p>可以通过 {@linkplain #forField(Field) 字段}、
 * {@linkplain #forMethodParameter(Method, int) 方法参数}、
 * {@linkplain #forMethodReturnType(Method) 方法返回类型} 或
 * {@linkplain #forClass(Class) 类} 获取 {@code ResolvableType} 实例。
 * 该类的大多数方法自身也会返回 {@code ResolvableType}，便于链式导航。例如：
 * <pre class="code">
 * private HashMap&lt;Integer, List&lt;String&gt;&gt; myMap;
 *
 * public void example() {
 *     ResolvableType t = ResolvableType.forField(getClass().getDeclaredField("myMap"));
 *     t.getSuperType(); // AbstractMap&lt;Integer, List&lt;String&gt;&gt;
 *     t.asMap(); // Map&lt;Integer, List&lt;String&gt;&gt;
 *     t.getGeneric(0).resolve(); // Integer
 *     t.getGeneric(1).resolve(); // List
 *     t.getGeneric(1); // List&lt;String&gt;
 *     t.resolveGeneric(1, 0); // String
 * }
 * </pre>
 *
 * @author Phillip Webb
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @see #forField(Field)
 * @see #forMethodParameter(Method, int)
 * @see #forMethodReturnType(Method)
 * @see #forConstructorParameter(Constructor, int)
 * @see #forClass(Class)
 * @see #forType(Type)
 * @see #forInstance(Object)
 * @see ResolvableTypeProvider
 * @since 4.0
 */
@SuppressWarnings("serial")
public class ResolvableType implements Serializable {

	/**
	 * 没有值时返回 {@code ResolvableType}。{@code NONE} 被优先使用，替换掉{@code null}，以便可以安全地链接多个方法调用。
	 */
	public static final ResolvableType NONE = new ResolvableType(EmptyType.INSTANCE, null, null, 0);

	private static final ResolvableType[] EMPTY_TYPES_ARRAY = new ResolvableType[0];

	private static final ConcurrentReferenceHashMap<ResolvableType, ResolvableType> cache =
			new ConcurrentReferenceHashMap<>(256);


	/**
	 * 被管理的底层 Java 类型。
	 */
	private final Type type;

	/**
	 * 类型的可选提供者。
	 */
	@Nullable
	private final TypeProvider typeProvider;

	/**
	 * 使用的 {@code VariableResolver}，如果没有可用解析器则为 {@code null}。
	 */
	@Nullable
	private final VariableResolver variableResolver;

	/**
	 * 数组的组件类型，若需推断类型则为 {@code null}。
	 */
	@Nullable
	private final ResolvableType componentType;

	@Nullable
	private final Integer hash;

	@Nullable
	private Class<?> resolved;

	@Nullable
	private volatile ResolvableType superType;

	@Nullable
	private volatile ResolvableType[] interfaces;

	@Nullable
	private volatile ResolvableType[] generics;


	/**
	 * 用于创建新的 {@link ResolvableType} 的私有构造方法，
	 * 用于缓存键，不进行预先解析。
	 */
	private ResolvableType(
			Type type, @Nullable TypeProvider typeProvider, @Nullable VariableResolver variableResolver) {

		this.type = type;
		this.typeProvider = typeProvider;
		this.variableResolver = variableResolver;
		this.componentType = null;
		this.hash = calculateHashCode();
		this.resolved = null;
	}

	/**
	 * 用于创建新的 {@link ResolvableType} 的私有构造方法，
	 * 用于缓存值，进行预先解析并预计算哈希值。
	 *
	 * @since 4.2
	 */
	private ResolvableType(Type type, @Nullable TypeProvider typeProvider,
						   @Nullable VariableResolver variableResolver, @Nullable Integer hash) {

		this.type = type;
		this.typeProvider = typeProvider;
		this.variableResolver = variableResolver;
		this.componentType = null;
		this.hash = hash;
		this.resolved = resolveClass();
	}

	/**
	 * 用于创建新的 {@link ResolvableType} 的私有构造方法，
	 * 用于非缓存场景，预先解析但延迟计算哈希值。
	 */
	private ResolvableType(Type type, @Nullable TypeProvider typeProvider,
						   @Nullable VariableResolver variableResolver, @Nullable ResolvableType componentType) {

		this.type = type;
		this.typeProvider = typeProvider;
		this.variableResolver = variableResolver;
		this.componentType = componentType;
		this.hash = null;
		this.resolved = resolveClass();
	}

	/**
	 * 私有构造函数用于在 {@link Class} 的基础上创建新的 {@link ResolvableType}。
	 * <p> 避免所有 {@code instanceof} 检查，以创建直接的 {@link Class} 包装器。
	 *
	 * @since 4.2
	 */
	private ResolvableType(@Nullable Class<?> clazz) {
		this.resolved = (clazz != null ? clazz : Object.class);
		this.type = this.resolved;
		this.typeProvider = null;
		this.variableResolver = null;
		this.componentType = null;
		this.hash = null;
	}


	/**
	 * 返回被管理的底层 Java {@link Type}。
	 */
	public Type getType() {
		return SerializableTypeWrapper.unwrap(this.type);
	}

	/**
	 * 返回被管理的底层 Java {@link Class}（如果可用），否则返回 {@code null}。
	 */
	@Nullable
	public Class<?> getRawClass() {
		if (this.type == this.resolved) {
			return this.resolved;
		}
		Type rawType = this.type;
		if (rawType instanceof ParameterizedType) {
			rawType = ((ParameterizedType) rawType).getRawType();
		}
		return (rawType instanceof Class ? (Class<?>) rawType : null);
	}

	/**
	 * 返回该可解析类型的底层来源。
	 * 根据 {@link ResolvableType} 的构造方式，可能返回 {@link Field}、
	 * {@link MethodParameter} 或 {@link Type}。
	 * 除了 {@link #NONE} 常量，该方法永不返回 {@code null}。
	 * 主要用于访问其他 JVM 语言可能提供的额外类型信息或元数据。
	 */
	public Object getSource() {
		Object source = (this.typeProvider != null ? this.typeProvider.getSource() : null);
		return (source != null ? source : this.type);
	}

	/**
	 * 将此类型作为已解析的 {@code Class} 返回，如果无法解析特定的类，则回退到 {@link java.lang.Object}。
	 *
	 * @return the resolved {@link Class} or the {@code Object} fallback
	 * @see #getRawClass()
	 * @see #resolve(Class)
	 * @since 5.1
	 */
	public Class<?> toClass() {
		return resolve(Object.class);
	}

	/**
	 * 判断给定对象是否是此 {@code ResolvableType} 的实例。
	 *
	 * @param obj 要检查的对象
	 * @see #isAssignableFrom(Class)
	 * @since 4.2
	 */
	public boolean isInstance(@Nullable Object obj) {
		return (obj != null && isAssignableFrom(obj.getClass()));
	}

	/**
	 * 判断此 {@code ResolvableType} 是否可以从指定的其他类型赋值。
	 *
	 * @param other 要检查的类型（以 {@code Class} 形式）
	 * @see #isAssignableFrom(ResolvableType)
	 * @since 4.2
	 */
	public boolean isAssignableFrom(Class<?> other) {
		return isAssignableFrom(forClass(other), null);
	}

	/**
	 * 确定此 {@code ResolvableType}  是否可从指定的其他类型中分配。
	 * <p> 尝试遵循与Java编译器相同的规则，同时考虑 {@link #resolve() resolved} {@code Class}
	 * 是否是从{@link Class#isAssignableFrom(Class) }中分配的，以及是否所有  {@link #getGenerics() generics} 都可以分配。
	 *
	 * @param other 要检查的类型 (作为 {@code ResolvableType})
	 * @return 如果指定的其他类型可以分配给这个{@code ResolvableType}，返回{@code true}；否则{@code false}
	 */
	public boolean isAssignableFrom(ResolvableType other) {
		return isAssignableFrom(other, null);
	}

	private boolean isAssignableFrom(ResolvableType other, @Nullable Map<Type, Type> matchedBefore) {
		Assert.notNull(other, "ResolvableType must not be null");

		// 如果无法解析类型，则不可赋值
		if (this == NONE || other == NONE) {
			return false;
		}

		// 处理数组，通过组件类型委托判断
		if (isArray()) {
			return (other.isArray() && getComponentType().isAssignableFrom(other.getComponentType()));
		}

		if (matchedBefore != null && matchedBefore.get(this.type) == other.type) {
			return true;
		}

		// 处理通配符边界
		WildcardBounds ourBounds = WildcardBounds.get(this);
		WildcardBounds typeBounds = WildcardBounds.get(other);

		// 形式如 X 可赋值给 <? extends Number>
		if (typeBounds != null) {
			return (ourBounds != null && ourBounds.isSameKind(typeBounds) &&
					ourBounds.isAssignableFrom(typeBounds.getBounds()));
		}

		// 形式如 <? extends Number> 可赋值给 X
		if (ourBounds != null) {
			return ourBounds.isAssignableFrom(other);
		}

		// 下面是主要的可赋值性检查
		boolean exactMatch = (matchedBefore != null);
		// 现在检查嵌套的泛型变量
		boolean checkGenerics = true;
		Class<?> ourResolved = null;
		if (this.type instanceof TypeVariable) {
			TypeVariable<?> variable = (TypeVariable<?>) this.type;
			// 尝试默认的变量解析
			if (this.variableResolver != null) {
				ResolvableType resolved = this.variableResolver.resolveVariable(variable);
				if (resolved != null) {
					ourResolved = resolved.resolve();
				}
			}
			if (ourResolved == null) {
				// 尝试针对目标类型的变量解析
				if (other.variableResolver != null) {
					ResolvableType resolved = other.variableResolver.resolveVariable(variable);
					if (resolved != null) {
						ourResolved = resolved.resolve();
						checkGenerics = false;
					}
				}
			}
			if (ourResolved == null) {
				// 未解析的类型变量，可能是嵌套的 -> 不强制精确匹配
				exactMatch = false;
			}
		}
		if (ourResolved == null) {
			ourResolved = resolve(Object.class);
		}
		Class<?> otherResolved = other.toClass();

		// 泛型需要精确类型匹配
		// List<CharSequence> 不能赋值 List<String>
		if (exactMatch ? !ourResolved.equals(otherResolved) : !ClassUtils.isAssignable(ourResolved, otherResolved)) {
			return false;
		}

		if (checkGenerics) {
			// 递归检查每个泛型
			ResolvableType[] ourGenerics = getGenerics();
			ResolvableType[] typeGenerics = other.as(ourResolved).getGenerics();
			if (ourGenerics.length != typeGenerics.length) {
				return false;
			}
			if (matchedBefore == null) {
				matchedBefore = new IdentityHashMap<>(1);
			}
			matchedBefore.put(this.type, other.type);
			for (int i = 0; i < ourGenerics.length; i++) {
				if (!ourGenerics[i].isAssignableFrom(typeGenerics[i], matchedBefore)) {
					return false;
				}
			}
		}

		return true;
	}

	/**
	 * 如果此类型解析为表示数组的 Class，则返回 {@code true}。
	 *
	 * @see #getComponentType()
	 */
	public boolean isArray() {
		if (this == NONE) {
			return false;
		}
		return ((this.type instanceof Class && ((Class<?>) this.type).isArray()) ||
				this.type instanceof GenericArrayType || resolveType().isArray());
	}

	/**
	 * 返回表示数组组件类型的 ResolvableType，
	 * 如果此类型不表示数组，则返回 {@link #NONE}。
	 *
	 * @see #isArray()
	 */
	public ResolvableType getComponentType() {
		if (this == NONE) {
			return NONE;
		}
		if (this.componentType != null) {
			return this.componentType;
		}
		if (this.type instanceof Class) {
			Class<?> componentType = ((Class<?>) this.type).getComponentType();
			return forType(componentType, this.variableResolver);
		}
		if (this.type instanceof GenericArrayType) {
			return forType(((GenericArrayType) this.type).getGenericComponentType(), this.variableResolver);
		}
		return resolveType().getComponentType();
	}

	/**
	 * 便捷方法，将此类型作为可解析的 {@link Collection} 类型返回。
	 * <p>如果此类型未实现或继承 {@link Collection}，则返回 {@link #NONE}。
	 *
	 * @see #as(Class)
	 * @see #asMap()
	 */
	public ResolvableType asCollection() {
		return as(Collection.class);
	}

	/**
	 * 便捷方法，将此类型作为可解析的 {@link Map} 类型返回。
	 * <p>如果此类型未实现或继承 {@link Map}，则返回 {@link #NONE}。
	 *
	 * @see #as(Class)
	 * @see #asCollection()
	 */
	public ResolvableType asMap() {
		return as(Map.class);
	}

	/**
	 * 将此类型作为指定类的 {@link ResolvableType} 返回。
	 * 在 {@link #getSuperType() 父类} 和 {@link #getInterfaces() 接口} 层级中搜索匹配项，
	 * 如果此类型不实现或不继承指定类，则返回 {@link #NONE}。
	 *
	 * @param type 目标类型（通常是被限定的类型）
	 * @return 表示此对象为指定类型的 {@link ResolvableType}，如果无法解析为该类型则返回 {@link #NONE}
	 * @see #asCollection()
	 * @see #asMap()
	 * @see #getSuperType()
	 * @see #getInterfaces()
	 */
	public ResolvableType as(Class<?> type) {
		if (this == NONE) {
			return NONE;
		}
		Class<?> resolved = resolve();
		if (resolved == null || resolved == type) {
			return this;
		}
		for (ResolvableType interfaceType : getInterfaces()) {
			ResolvableType interfaceAsType = interfaceType.as(type);
			if (interfaceAsType != NONE) {
				return interfaceAsType;
			}
		}
		return getSuperType().as(type);
	}

	/**
	 * 返回表示此类型直接父类的 {@link ResolvableType}。
	 * <p>如果没有父类，则返回 {@link #NONE}。
	 * <p>注意：返回的 {@link ResolvableType} 实例可能不可序列化。
	 *
	 * @see #getInterfaces()
	 */
	public ResolvableType getSuperType() {
		Class<?> resolved = resolve();
		if (resolved == null) {
			return NONE;
		}
		try {
			Type superclass = resolved.getGenericSuperclass();
			if (superclass == null) {
				return NONE;
			}
			ResolvableType superType = this.superType;
			if (superType == null) {
				superType = forType(superclass, this);
				this.superType = superType;
			}
			return superType;
		} catch (TypeNotPresentException ex) {
			// 忽略泛型签名中不存在的类型
			return NONE;
		}
	}

	/**
	 * 返回表示此类型直接实现的接口的 {@link ResolvableType} 数组。
	 * <p>如果此类型没有实现任何接口，则返回空数组。
	 * <p>注意：返回的 {@link ResolvableType} 实例可能不可序列化。
	 *
	 * @see #getSuperType()
	 */
	public ResolvableType[] getInterfaces() {
		Class<?> resolved = resolve();
		if (resolved == null) {
			return EMPTY_TYPES_ARRAY;
		}
		ResolvableType[] interfaces = this.interfaces;
		if (interfaces == null) {
			Type[] genericIfcs = resolved.getGenericInterfaces();
			interfaces = new ResolvableType[genericIfcs.length];
			for (int i = 0; i < genericIfcs.length; i++) {
				interfaces[i] = forType(genericIfcs[i], this);
			}
			this.interfaces = interfaces;
		}
		return interfaces;
	}

	/**
	 * 如果此类型包含泛型参数，则返回{@code true}。
	 *
	 * @see #getGeneric(int...)
	 * @see #getGenerics()
	 */
	public boolean hasGenerics() {
		return (getGenerics().length > 0);
	}

	/**
	 * 如果此类型仅包含不可解析的泛型（即，其声明的类型变量都没有对应的替代类型），则返回 {@code true}。
	 */
	boolean isEntirelyUnresolvable() {
		if (this == NONE) {
			return false;
		}
		ResolvableType[] generics = getGenerics();
		for (ResolvableType generic : generics) {
			if (!generic.isUnresolvableTypeVariable() && !generic.isWildcardWithoutBounds()) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 判断底层类型是否包含任何不可解析的泛型：
	 * 可能是类型自身的不可解析类型变量，或者以原生类型的方式实现了泛型接口，
	 * 即未替换该接口的类型变量。仅在这两种情况下返回 {@code true}。
	 */
	public boolean hasUnresolvableGenerics() {
		if (this == NONE) {
			return false;
		}
		ResolvableType[] generics = getGenerics();
		for (ResolvableType generic : generics) {
			if (generic.isUnresolvableTypeVariable() || generic.isWildcardWithoutBounds()) {
				return true;
			}
		}
		Class<?> resolved = resolve();
		if (resolved != null) {
			try {
				for (Type genericInterface : resolved.getGenericInterfaces()) {
					if (genericInterface instanceof Class) {
						if (forClass((Class<?>) genericInterface).hasGenerics()) {
							return true;
						}
					}
				}
			} catch (TypeNotPresentException ex) {
				// 忽略泛型签名中不存在的类型
			}
			return getSuperType().hasUnresolvableGenerics();
		}
		return false;
	}

	/**
	 * 判断底层类型是否为无法通过关联的变量解析器解析的类型变量。
	 */
	private boolean isUnresolvableTypeVariable() {
		if (this.type instanceof TypeVariable) {
			if (this.variableResolver == null) {
				return true;
			}
			TypeVariable<?> variable = (TypeVariable<?>) this.type;
			ResolvableType resolved = this.variableResolver.resolveVariable(variable);
			if (resolved == null || resolved.isUnresolvableTypeVariable()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 判断底层类型是否表示一个无特定边界的通配符
	 * （即，等同于 {@code ? extends Object}）。
	 */
	private boolean isWildcardWithoutBounds() {
		if (this.type instanceof WildcardType) {
			WildcardType wt = (WildcardType) this.type;
			if (wt.getLowerBounds().length == 0) {
				Type[] upperBounds = wt.getUpperBounds();
				if (upperBounds.length == 0 || (upperBounds.length == 1 && Object.class == upperBounds[0])) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * 返回指定嵌套层级的 {@link ResolvableType}。
	 * <p>详细信息见 {@link #getNested(int, Map)}。
	 *
	 * @param nestingLevel 嵌套层级
	 * @return 返回对应的 {@link ResolvableType}，如果不存在则返回 {@code NONE}
	 */
	public ResolvableType getNested(int nestingLevel) {
		return getNested(nestingLevel, null);
	}

	/**
	 * 返回指定嵌套层级的 {@link ResolvableType}。
	 * <p>嵌套层级指明应返回的具体泛型参数。层级 1 表示当前类型；
	 * 层级 2 表示第一个嵌套泛型；层级 3 表示第二个，依此类推。
	 * 例如，给定 {@code List<Set<Integer>>}，层级 1 是 {@code List}，
	 * 层级 2 是 {@code Set}，层级 3 是 {@code Integer}。
	 * <p>{@code typeIndexesPerLevel} 参数是一个映射，用于指定对应层级的泛型索引。
	 * 例如，索引 0 表示 {@code Map} 的键，索引 1 表示值。
	 * 如果映射中未包含该层级的索引，则默认使用最后一个泛型参数（例如 {@code Map} 的值）。
	 * <p>嵌套层级也适用于数组类型，比如 {@code String[]} 中，层级 2 是 {@code String}。
	 * <p>如果类型没有泛型，则会考虑其超类型层级。
	 *
	 * @param nestingLevel        目标嵌套层级，从 1 开始计数（当前类型为 1，依次递增）
	 * @param typeIndexesPerLevel 每个层级对应的泛型索引映射，允许为 {@code null}
	 * @return 指定嵌套层级对应的 {@link ResolvableType}，无效时返回 {@link #NONE}
	 */
	public ResolvableType getNested(int nestingLevel, @Nullable Map<Integer, Integer> typeIndexesPerLevel) {
		ResolvableType result = this;
		for (int i = 2; i <= nestingLevel; i++) {
			if (result.isArray()) {
				result = result.getComponentType();
			} else {
				// 处理派生类型
				while (result != ResolvableType.NONE && !result.hasGenerics()) {
					result = result.getSuperType();
				}
				Integer index = (typeIndexesPerLevel != null ? typeIndexesPerLevel.get(i) : null);
				index = (index == null ? result.getGenerics().length - 1 : index);
				result = result.getGeneric(index);
			}
		}
		return result;
	}

	/**
	 * 返回表示给定索引的泛型参数的{@link ResolvableType}。索引从零开始；例如，给定类型
	 * {@code Map<Integer, List<String>>}，{@code getGeneric(0)} 将访问 {@code Integer}。
	 * 可以通过指定多个索引来访问嵌套泛型；例如，{@code getGeneric(1, 0)} 将访问嵌套 {@code List} 中的 {@code String}。
	 * 为方便起见，如果未指定索引，则返回第一个泛型。
	 * <p>如果在指定的索引处没有可用的泛型，则返回 {@link #NONE}。
	 *
	 * @param indexes 引用泛型参数的索引（可以省略以返回第一个泛型）
	 * @return 指定泛型的{@link ResolvableType}，或 {@link #NONE}
	 * @see #hasGenerics()
	 * @see #getGenerics()
	 * @see #resolveGeneric(int...)
	 * @see #resolveGenerics()
	 */
	public ResolvableType getGeneric(@Nullable int... indexes) {
		// 获取当前 ResolvableType 对象的泛型数组
		ResolvableType[] generics = getGenerics();
		// 如果索引数组为空或长度为 0
		if (indexes == null || indexes.length == 0) {
			// 如果泛型数组为空，则返回 NONE，否则返回第一个泛型类型
			return (generics.length == 0 ? NONE : generics[0]);
		}
		// 初始化泛型为当前 ResolvableType 对象
		ResolvableType generic = this;
		// 遍历索引数组
		for (int index : indexes) {
			// 获取当前泛型对象的泛型数组
			generics = generic.getGenerics();
			// 如果索引小于 0 或大于等于泛型数组长度，则返回 NONE
			if (index < 0 || index >= generics.length) {
				return NONE;
			}
			// 更新泛型为指定索引处的泛型对象
			generic = generics[index];
		}
		// 返回最终的泛型对象
		return generic;
	}

	/**
	 * 返回一个 {@link ResolvableType} 数组，表示此类型的泛型参数。
	 * 如果没有泛型，则返回空数组。
	 * 如果需要访问特定的泛型参数，建议使用 {@link #getGeneric(int...)} 方法，
	 * 因为它支持访问嵌套泛型且防止 {@code IndexOutOfBoundsException}。
	 *
	 * @return 表示泛型参数的 {@link ResolvableType} 数组（不会为 {@code null}）
	 * @see #hasGenerics()
	 * @see #getGeneric(int...)
	 * @see #resolveGeneric(int...)
	 * @see #resolveGenerics()
	 */
	public ResolvableType[] getGenerics() {
		if (this == NONE) {
			return EMPTY_TYPES_ARRAY;
		}
		ResolvableType[] generics = this.generics;
		if (generics == null) {
			if (this.type instanceof Class) {
				Type[] typeParams = ((Class<?>) this.type).getTypeParameters();
				generics = new ResolvableType[typeParams.length];
				for (int i = 0; i < generics.length; i++) {
					generics[i] = ResolvableType.forType(typeParams[i], this);
				}
			} else if (this.type instanceof ParameterizedType) {
				Type[] actualTypeArguments = ((ParameterizedType) this.type).getActualTypeArguments();
				generics = new ResolvableType[actualTypeArguments.length];
				for (int i = 0; i < actualTypeArguments.length; i++) {
					generics[i] = forType(actualTypeArguments[i], this.variableResolver);
				}
			} else {
				generics = resolveType().getGenerics();
			}
			this.generics = generics;
		}
		return generics;
	}

	/**
	 * 便捷方法，获取泛型参数并解析成对应的 {@link Class} 类型。
	 *
	 * @return 已解析的泛型参数数组（数组本身不会为 {@code null}，但元素可能为 {@code null}）
	 * @see #getGenerics()
	 * @see #resolve()
	 */
	public Class<?>[] resolveGenerics() {
		ResolvableType[] generics = getGenerics();
		Class<?>[] resolvedGenerics = new Class<?>[generics.length];
		for (int i = 0; i < generics.length; i++) {
			resolvedGenerics[i] = generics[i].resolve();
		}
		return resolvedGenerics;
	}

	/**
	 * 便捷方法，获取泛型参数并解析成对应的 {@link Class} 类型。
	 * 若解析失败则使用指定的 {@code fallback} 类作为默认返回值。
	 *
	 * @param fallback 如果解析失败时的备用 {@link Class}
	 * @return 已解析的泛型参数数组
	 * @see #getGenerics()
	 * @see #resolve()
	 */
	public Class<?>[] resolveGenerics(Class<?> fallback) {
		ResolvableType[] generics = getGenerics();
		Class<?>[] resolvedGenerics = new Class<?>[generics.length];
		for (int i = 0; i < generics.length; i++) {
			resolvedGenerics[i] = generics[i].resolve(fallback);
		}
		return resolvedGenerics;
	}

	/**
	 * 便捷方法，获取并解析指定索引的泛型参数。
	 *
	 * @param indexes 泛型参数的索引（可省略，默认返回第一个泛型）
	 * @return 解析后的 {@link Class}，如果无法解析则返回 {@code null}
	 * @see #getGeneric(int...)
	 * @see #resolve()
	 */
	@Nullable
	public Class<?> resolveGeneric(int... indexes) {
		return getGeneric(indexes).resolve();
	}

	/**
	 * 将此类型解析为 {@link java.lang.Class}，如果无法解析该类型，则返回 {@code null}。
	 * 如果直接解析失败，此方法将考虑 {@link TypeVariable TypeVariables} 和 {@link WildcardType WildcardType} 的边界;
	 * 但是，{@code Object.class} 的边界将被忽略。
	 * <p> 如果此方法返回非空 {@code Class}，并且 {@link #hasGenerics()} 返回 {@code false}，
	 * 则给定类型有效地包装了一个普通的 {@code Class}，如果需要，可以进行简单的 {@code Class} 处理。
	 *
	 * @return 已解析好的 {@link Class}，如果无法解析，则为 {@code null}
	 * @see #resolve(Class)
	 * @see #resolveGeneric(int...)
	 * @see #resolveGenerics()
	 */
	@Nullable
	public Class<?> resolve() {
		return this.resolved;
	}

	/**
	 * 将此类型解析为 {@link java.lang.Class}，如果无法解析该类型，则返回指定的 {@code fallback}。
	 * 如果直接解析失败，此方法将考虑 {@link TypeVariable TypeVariables}  和 {@link WildcardType WildcardTypes}  的边界;
	 * 但是，{@code Object.class} 的边界将被忽略。
	 *
	 * @param fallback 如果解析失败，要使用的回退类
	 * @return 已解析的 {@link Class} 或 {@code fallback}
	 * @see #resolve()
	 * @see #resolveGeneric(int...)
	 * @see #resolveGenerics()
	 */
	public Class<?> resolve(Class<?> fallback) {
		return (this.resolved != null ? this.resolved : fallback);
	}

	@Nullable
	private Class<?> resolveClass() {
		if (this.type == EmptyType.INSTANCE) {
			return null;
		}
		if (this.type instanceof Class) {
			return (Class<?>) this.type;
		}
		if (this.type instanceof GenericArrayType) {
			Class<?> resolvedComponent = getComponentType().resolve();
			return (resolvedComponent != null ? Array.newInstance(resolvedComponent, 0).getClass() : null);
		}
		return resolveType().resolve();
	}

	/**
	 * 解析当前类型一级，返回解析后的类型或 {@link #NONE}。
	 * <p>注意：返回的 {@link ResolvableType} 应仅作为中间使用，因为它不可序列化。
	 */
	ResolvableType resolveType() {
		if (this.type instanceof ParameterizedType) {
			return forType(((ParameterizedType) this.type).getRawType(), this.variableResolver);
		}
		if (this.type instanceof WildcardType) {
			Type resolved = resolveBounds(((WildcardType) this.type).getUpperBounds());
			if (resolved == null) {
				resolved = resolveBounds(((WildcardType) this.type).getLowerBounds());
			}
			return forType(resolved, this.variableResolver);
		}
		if (this.type instanceof TypeVariable) {
			TypeVariable<?> variable = (TypeVariable<?>) this.type;
			// 尝试默认的变量解析
			if (this.variableResolver != null) {
				ResolvableType resolved = this.variableResolver.resolveVariable(variable);
				if (resolved != null) {
					return resolved;
				}
			}
			// 回退到边界解析
			return forType(resolveBounds(variable.getBounds()), this.variableResolver);
		}
		return NONE;
	}

	@Nullable
	private Type resolveBounds(Type[] bounds) {
		if (bounds.length == 0 || bounds[0] == Object.class) {
			return null;
		}
		return bounds[0];
	}

	@Nullable
	private ResolvableType resolveVariable(TypeVariable<?> variable) {
		if (this.type instanceof TypeVariable) {
			return resolveType().resolveVariable(variable);
		}
		if (this.type instanceof ParameterizedType) {
			ParameterizedType parameterizedType = (ParameterizedType) this.type;
			Class<?> resolved = resolve();
			if (resolved == null) {
				return null;
			}
			TypeVariable<?>[] variables = resolved.getTypeParameters();
			for (int i = 0; i < variables.length; i++) {
				if (ObjectUtils.nullSafeEquals(variables[i].getName(), variable.getName())) {
					Type actualType = parameterizedType.getActualTypeArguments()[i];
					return forType(actualType, this.variableResolver);
				}
			}
			Type ownerType = parameterizedType.getOwnerType();
			if (ownerType != null) {
				return forType(ownerType, this.variableResolver).resolveVariable(variable);
			}
		}
		if (this.type instanceof WildcardType) {
			ResolvableType resolved = resolveType().resolveVariable(variable);
			if (resolved != null) {
				return resolved;
			}
		}
		if (this.variableResolver != null) {
			return this.variableResolver.resolveVariable(variable);
		}
		return null;
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof ResolvableType)) {
			return false;
		}

		ResolvableType otherType = (ResolvableType) other;
		if (!ObjectUtils.nullSafeEquals(this.type, otherType.type)) {
			return false;
		}
		if (this.typeProvider != otherType.typeProvider &&
				(this.typeProvider == null || otherType.typeProvider == null ||
						!ObjectUtils.nullSafeEquals(this.typeProvider.getType(), otherType.typeProvider.getType()))) {
			return false;
		}
		if (this.variableResolver != otherType.variableResolver &&
				(this.variableResolver == null || otherType.variableResolver == null ||
						!ObjectUtils.nullSafeEquals(this.variableResolver.getSource(), otherType.variableResolver.getSource()))) {
			return false;
		}
		if (!ObjectUtils.nullSafeEquals(this.componentType, otherType.componentType)) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		return (this.hash != null ? this.hash : calculateHashCode());
	}

	private int calculateHashCode() {
		int hashCode = ObjectUtils.nullSafeHashCode(this.type);
		if (this.typeProvider != null) {
			hashCode = 31 * hashCode + ObjectUtils.nullSafeHashCode(this.typeProvider.getType());
		}
		if (this.variableResolver != null) {
			hashCode = 31 * hashCode + ObjectUtils.nullSafeHashCode(this.variableResolver.getSource());
		}
		if (this.componentType != null) {
			hashCode = 31 * hashCode + ObjectUtils.nullSafeHashCode(this.componentType);
		}
		return hashCode;
	}

	/**
	 * 将当前 {@link ResolvableType} 适配为一个 {@link VariableResolver}。
	 */
	@Nullable
	VariableResolver asVariableResolver() {
		if (this == NONE) {
			return null;
		}
		return new DefaultVariableResolver(this);
	}

	/**
	 * 为 {@link #NONE} 提供自定义的序列化支持。
	 */
	private Object readResolve() {
		return (this.type == EmptyType.INSTANCE ? NONE : this);
	}

	/**
	 * 返回当前类型的字符串表示，显示其完全解析的形式
	 * （包括任何泛型参数）。
	 */
	@Override
	public String toString() {
		if (isArray()) {
			return getComponentType() + "[]";
		}
		if (this.resolved == null) {
			return "?";
		}
		if (this.type instanceof TypeVariable) {
			TypeVariable<?> variable = (TypeVariable<?>) this.type;
			if (this.variableResolver == null || this.variableResolver.resolveVariable(variable) == null) {
				// 不显示变量的边界...
				// 避免自引用时导致无限递归
				return "?";
			}
		}
		if (hasGenerics()) {
			return this.resolved.getName() + '<' + StringUtils.arrayToDelimitedString(getGenerics(), ", ") + '>';
		}
		return this.resolved.getName();
	}


	// 工厂方法

	/**
	 * 使用完整的泛型类型信息进行可分配性检查，为指定的 {@link Class} 返回一个 {@link ResolvableType}。
	 * <p> 例如: {@code ResolvableType.forClass(MyArrayList.class)}。
	 *
	 * @param clazz 内省类 ({@code null} 在语义上等同于 {@code Object.class} 这里的典型用例)
	 * @return 指定类的 {@link ResolvableType}
	 * @see #forClass(Class, Class)
	 * @see #forClassWithGenerics(Class, Class...)
	 */
	public static ResolvableType forClass(@Nullable Class<?> clazz) {
		return new ResolvableType(clazz);
	}

	/**
	 * 为指定的 {@link Class} 返回一个 {@link ResolvableType}，
	 * 仅对原始类进行可分配性检查 (类似于 {@link  Class#isAssignableFrom}，它用作包装。
	 * <p> 例如: {@code ResolvableType.forRawClass(List.Class)}。
	 *
	 * @param clazz 要内省的类，({@code null} 在语义上是
	 *              相当于 {@code Object.class} 这里的典型用例)
	 * @return 指定类的 {@link ResolvableType}
	 * @see #forClass(Class)
	 * @see #getRawClass()
	 * @since 4.2
	 */
	public static ResolvableType forRawClass(@Nullable Class<?> clazz) {
		return new ResolvableType(clazz) {
			@Override
			public ResolvableType[] getGenerics() {
				return EMPTY_TYPES_ARRAY;
			}

			@Override
			public boolean isAssignableFrom(Class<?> other) {
				//clazz为空，返回true，clazz可以分配给other也返回true
				return (clazz == null || ClassUtils.isAssignable(clazz, other));
			}

			@Override
			public boolean isAssignableFrom(ResolvableType other) {
				Class<?> otherClass = other.resolve();
				return (otherClass != null && (clazz == null || ClassUtils.isAssignable(clazz, otherClass)));
			}
		};
	}

	/**
	 * 返回具有给定实现类的指定基类型 (接口或基类) 的 {@link ResolvableType}。
	 * <p> 例如: {@code ResolvableType.forClass(List.class，MyArrayList.class)}。
	 *
	 * @param baseType            基本类型 (不得为 {@code null})
	 * @param implementationClass 实现类
	 * @return 由给定实现类支持的指定基本类型的 {@link ResolvableType}
	 * @see #forClass(Class)
	 * @see #forClassWithGenerics(Class, Class...)
	 */
	public static ResolvableType forClass(Class<?> baseType, Class<?> implementationClass) {
		Assert.notNull(baseType, "Base type must not be null");
		//将实现类的转换为父类或父接口对应的ResolvableType
		ResolvableType asType = forType(implementationClass).as(baseType);
		//如果转换后的ResolvableType为NONE，则返回基本类型的ResolvableType，
		// 否则返回父类或父接口对应的ResolvableType
		return (asType == NONE ? forType(baseType) : asType);
	}

	/**
	 * 返回带预声明泛型的指定 {@link Class} 的 {@link ResolvableType}。
	 *
	 * @param clazz    要分析的类（或接口）
	 * @param generics 类的泛型参数
	 * @return 指定类及其泛型对应的 {@link ResolvableType}
	 * @see #forClassWithGenerics(Class, ResolvableType...)
	 */
	public static ResolvableType forClassWithGenerics(Class<?> clazz, Class<?>... generics) {
		Assert.notNull(clazz, "Class must not be null");
		Assert.notNull(generics, "Generics array must not be null");
		ResolvableType[] resolvableGenerics = new ResolvableType[generics.length];
		for (int i = 0; i < generics.length; i++) {
			resolvableGenerics[i] = forClass(generics[i]);
		}
		return forClassWithGenerics(clazz, resolvableGenerics);
	}

	/**
	 * 返回带预声明泛型的指定 {@link Class} 的 {@link ResolvableType}。
	 *
	 * @param clazz    要分析的类（或接口）
	 * @param generics 类的泛型参数（{@link ResolvableType} 形式）
	 * @return 指定类及其泛型对应的 {@link ResolvableType}
	 * @see #forClassWithGenerics(Class, Class...)
	 */
	public static ResolvableType forClassWithGenerics(Class<?> clazz, ResolvableType... generics) {
		Assert.notNull(clazz, "Class must not be null");
		Assert.notNull(generics, "Generics array must not be null");
		TypeVariable<?>[] variables = clazz.getTypeParameters();
		Assert.isTrue(variables.length == generics.length, () -> "Mismatched number of generics specified for " + clazz.toGenericString());

		Type[] arguments = new Type[generics.length];
		for (int i = 0; i < generics.length; i++) {
			ResolvableType generic = generics[i];
			Type argument = (generic != null ? generic.getType() : null);
			arguments[i] = (argument != null && !(argument instanceof TypeVariable) ? argument : variables[i]);
		}

		ParameterizedType syntheticType = new SyntheticParameterizedType(clazz, arguments);
		return forType(syntheticType, new TypeVariablesVariableResolver(variables, generics));
	}

	/**
	 * 返回指定实例对应的 {@link ResolvableType}。
	 * <p>实例本身不携带泛型信息，但如果实现了 {@link ResolvableTypeProvider}，
	 * 则可以提供比简单 {@link #forClass(Class)} 更精确的类型信息。
	 *
	 * @param instance 实例对象
	 * @return 指定实例对应的 {@link ResolvableType}
	 * @see ResolvableTypeProvider
	 * @since 4.2
	 */
	public static ResolvableType forInstance(Object instance) {
		Assert.notNull(instance, "Instance must not be null");
		if (instance instanceof ResolvableTypeProvider) {
			ResolvableType type = ((ResolvableTypeProvider) instance).getResolvableType();
			if (type != null) {
				return type;
			}
		}
		return ResolvableType.forClass(instance.getClass());
	}

	/**
	 * 返回指定 {@link Field} 的 {@link ResolvableType}。
	 *
	 * @param field 来源字段
	 * @return 指定字段对应的 {@link ResolvableType}
	 * @see #forField(Field, Class)
	 */
	public static ResolvableType forField(Field field) {
		Assert.notNull(field, "Field must not be null");
		return forType(null, new FieldTypeProvider(field), null);
	}

	/**
	 * 返回指定 {@link Field} 及给定实现类对应的 {@link ResolvableType}。
	 * <p>当声明字段的类包含由实现类满足的泛型参数变量时，使用此方法。
	 *
	 * @param field               来源字段
	 * @param implementationClass 实现类
	 * @return 指定字段对应的 {@link ResolvableType}
	 * @see #forField(Field)
	 */
	public static ResolvableType forField(Field field, Class<?> implementationClass) {
		Assert.notNull(field, "Field must not be null");
		ResolvableType owner = forType(implementationClass).as(field.getDeclaringClass());
		return forType(null, new FieldTypeProvider(field), owner.asVariableResolver());
	}

	/**
	 * 返回指定 {@link Field} 及给定实现类型对应的 {@link ResolvableType}。
	 * <p>当声明字段的类包含由实现类型满足的泛型参数变量时，使用此方法。
	 *
	 * @param field              来源字段
	 * @param implementationType 实现类型
	 * @return 指定字段对应的 {@link ResolvableType}
	 * @see #forField(Field)
	 */
	public static ResolvableType forField(Field field, @Nullable ResolvableType implementationType) {
		Assert.notNull(field, "Field must not be null");
		ResolvableType owner = (implementationType != null ? implementationType : NONE);
		owner = owner.as(field.getDeclaringClass());
		return forType(null, new FieldTypeProvider(field), owner.asVariableResolver());
	}

	/**
	 * 返回指定 {@link Field} 并指定嵌套层级的 {@link ResolvableType}。
	 *
	 * @param field        来源字段
	 * @param nestingLevel 嵌套层级（1 表示外层，2 表示嵌套泛型类型，依此类推）
	 * @see #forField(Field)
	 */
	public static ResolvableType forField(Field field, int nestingLevel) {
		Assert.notNull(field, "Field must not be null");
		return forType(null, new FieldTypeProvider(field), null).getNested(nestingLevel);
	}

	/**
	 * 返回指定 {@link Field}，给定实现类及嵌套层级的 {@link ResolvableType}。
	 * <p>当声明字段的类包含由实现类满足的泛型参数变量时，使用此方法。
	 *
	 * @param field               来源字段
	 * @param nestingLevel        嵌套层级（1 表示外层，2 表示嵌套泛型类型，依此类推）
	 * @param implementationClass 实现类
	 * @return 指定字段对应的 {@link ResolvableType}
	 * @see #forField(Field)
	 */
	public static ResolvableType forField(Field field, int nestingLevel, @Nullable Class<?> implementationClass) {
		Assert.notNull(field, "Field must not be null");
		ResolvableType owner = forType(implementationClass).as(field.getDeclaringClass());
		return forType(null, new FieldTypeProvider(field), owner.asVariableResolver()).getNested(nestingLevel);
	}

	/**
	 * 返回指定 {@link Constructor} 参数的 {@link ResolvableType}。
	 *
	 * @param constructor    来源构造器（不能为空）
	 * @param parameterIndex 参数索引
	 * @return 指定构造器参数对应的 {@link ResolvableType}
	 * @see #forConstructorParameter(Constructor, int, Class)
	 */
	public static ResolvableType forConstructorParameter(Constructor<?> constructor, int parameterIndex) {
		Assert.notNull(constructor, "Constructor must not be null");
		return forMethodParameter(new MethodParameter(constructor, parameterIndex));
	}

	/**
	 * 返回指定 {@link Constructor} 参数及给定实现类对应的 {@link ResolvableType}。
	 * <p>当声明构造器的类包含由实现类满足的泛型参数变量时，使用此方法。
	 *
	 * @param constructor         来源构造器（不能为空）
	 * @param parameterIndex      参数索引
	 * @param implementationClass 实现类
	 * @return 指定构造器参数对应的 {@link ResolvableType}
	 * @see #forConstructorParameter(Constructor, int)
	 */
	public static ResolvableType forConstructorParameter(Constructor<?> constructor, int parameterIndex,
														 Class<?> implementationClass) {

		Assert.notNull(constructor, "Constructor must not be null");
		MethodParameter methodParameter = new MethodParameter(constructor, parameterIndex, implementationClass);
		return forMethodParameter(methodParameter);
	}

	/**
	 * 为指定的 {@link Method} 返回类型返回一个 {@link ResolvableType}。
	 *
	 * @param method 方法返回类型的来源
	 * @return 返回指定方法的 {@link ResolvableType}
	 * @see #forMethodReturnType(Method, Class)
	 */
	public static ResolvableType forMethodReturnType(Method method) {
		Assert.notNull(method, "Method must not be null");
		return forMethodParameter(new MethodParameter(method, -1));
	}

	/**
	 * 返回指定 {@link Method} 的返回类型的 {@link ResolvableType}。
	 * <p>当声明方法的类包含由实现类满足的泛型参数变量时，使用此方法。
	 *
	 * @param method              方法来源
	 * @param implementationClass 实现类
	 * @return 指定方法返回类型对应的 {@link ResolvableType}
	 * @see #forMethodReturnType(Method)
	 */
	public static ResolvableType forMethodReturnType(Method method, Class<?> implementationClass) {
		Assert.notNull(method, "Method must not be null");
		MethodParameter methodParameter = new MethodParameter(method, -1, implementationClass);
		return forMethodParameter(methodParameter);
	}

	/**
	 * 返回指定 {@link Method} 的参数对应的 {@link ResolvableType}。
	 *
	 * @param method         方法来源（不能为空）
	 * @param parameterIndex 参数索引
	 * @return 指定方法参数对应的 {@link ResolvableType}
	 * @see #forMethodParameter(Method, int, Class)
	 * @see #forMethodParameter(MethodParameter)
	 */
	public static ResolvableType forMethodParameter(Method method, int parameterIndex) {
		Assert.notNull(method, "Method must not be null");
		return forMethodParameter(new MethodParameter(method, parameterIndex));
	}

	/**
	 * 返回指定 {@link Method} 的参数及给定实现类对应的 {@link ResolvableType}。
	 * <p>当声明方法的类包含由实现类满足的泛型参数变量时，使用此方法。
	 *
	 * @param method              方法来源（不能为空）
	 * @param parameterIndex      参数索引
	 * @param implementationClass 实现类
	 * @return 指定方法参数对应的 {@link ResolvableType}
	 * @see #forMethodParameter(Method, int)
	 * @see #forMethodParameter(MethodParameter)
	 */
	public static ResolvableType forMethodParameter(Method method, int parameterIndex, Class<?> implementationClass) {
		Assert.notNull(method, "Method must not be null");
		MethodParameter methodParameter = new MethodParameter(method, parameterIndex, implementationClass);
		return forMethodParameter(methodParameter);
	}

	/**
	 * 返回指定 {@link MethodParameter} 对应的 {@link ResolvableType}。
	 *
	 * @param methodParameter 方法参数来源（不能为空）
	 * @return 指定方法参数对应的 {@link ResolvableType}
	 * @see #forMethodParameter(Method, int)
	 */
	public static ResolvableType forMethodParameter(MethodParameter methodParameter) {
		return forMethodParameter(methodParameter, (Type) null);
	}

	/**
	 * 返回指定 {@link MethodParameter} 对应的 {@link ResolvableType}，并指定实现类型。
	 * <p>当声明方法的类包含由实现类型满足的泛型参数变量时，使用此方法。
	 *
	 * @param methodParameter    方法参数来源（不能为空）
	 * @param implementationType 实现类型
	 * @return 指定方法参数对应的 {@link ResolvableType}
	 * @see #forMethodParameter(MethodParameter)
	 */
	public static ResolvableType forMethodParameter(MethodParameter methodParameter,
													@Nullable ResolvableType implementationType) {

		Assert.notNull(methodParameter, "MethodParameter must not be null");
		implementationType = (implementationType != null ? implementationType :
				forType(methodParameter.getContainingClass()));
		ResolvableType owner = implementationType.as(methodParameter.getDeclaringClass());
		return forType(null, new MethodParameterTypeProvider(methodParameter), owner.asVariableResolver()).
				getNested(methodParameter.getNestingLevel(), methodParameter.typeIndexesPerLevel);
	}

	/**
	 * 返回指定 {@link MethodParameter} 对应的 {@link ResolvableType}，并使用指定的目标类型覆盖需要解析的类型。
	 *
	 * @param methodParameter 方法参数来源（不能为空）
	 * @param targetType      需要解析的类型（方法参数类型的一部分）
	 * @return 指定方法参数对应的 {@link ResolvableType}
	 * @see #forMethodParameter(Method, int)
	 */
	public static ResolvableType forMethodParameter(MethodParameter methodParameter, @Nullable Type targetType) {
		Assert.notNull(methodParameter, "MethodParameter must not be null");
		return forMethodParameter(methodParameter, targetType, methodParameter.getNestingLevel());
	}

	/**
	 * 返回指定 {@link MethodParameter} 在特定嵌套级别对应的 {@link ResolvableType}，
	 * 并使用指定的目标类型覆盖需要解析的类型。
	 *
	 * @param methodParameter 方法参数来源（不能为空）
	 * @param targetType      需要解析的类型（方法参数类型的一部分）
	 * @param nestingLevel    使用的嵌套级别
	 * @return 指定方法参数对应的 {@link ResolvableType}
	 * @see #forMethodParameter(Method, int)
	 * @since 5.2
	 */
	static ResolvableType forMethodParameter(
			MethodParameter methodParameter, @Nullable Type targetType, int nestingLevel) {

		ResolvableType owner = forType(methodParameter.getContainingClass()).as(methodParameter.getDeclaringClass());
		return forType(targetType, new MethodParameterTypeProvider(methodParameter), owner.asVariableResolver()).
				getNested(nestingLevel, methodParameter.typeIndexesPerLevel);
	}

	/**
	 * 返回指定 {@code componentType} 的数组形式的 {@link ResolvableType}。
	 *
	 * @param componentType 组件类型
	 * @return 指定组件类型数组形式的 {@link ResolvableType}
	 */
	public static ResolvableType forArrayComponent(ResolvableType componentType) {
		Assert.notNull(componentType, "Component type must not be null");
		Class<?> arrayClass = Array.newInstance(componentType.resolve(), 0).getClass();
		return new ResolvableType(arrayClass, null, null, componentType);
	}

	/**
	 * 返回指定 {@link Type} 对应的 {@link ResolvableType}。
	 * <p>注意：返回的 {@link ResolvableType} 实例可能不可序列化。
	 *
	 * @param type 源类型（可能为 {@code null}）
	 * @return 指定 {@link Type} 对应的 {@link ResolvableType}
	 * @see #forType(Type, ResolvableType)
	 */
	public static ResolvableType forType(@Nullable Type type) {
		return forType(type, null, null);
	}

	/**
	 * 返回指定 {@link Type} 对应的 {@link ResolvableType}，并指定所属的 owner 类型。
	 * <p>注意：返回的 {@link ResolvableType} 实例可能不可序列化。
	 *
	 * @param type  源类型或 {@code null}
	 * @param owner 用于解析变量的 owner 类型
	 * @return 指定 {@link Type} 和 owner 对应的 {@link ResolvableType}
	 * @see #forType(Type)
	 */
	public static ResolvableType forType(@Nullable Type type, @Nullable ResolvableType owner) {
		VariableResolver variableResolver = null;
		if (owner != null) {
			variableResolver = owner.asVariableResolver();
		}
		return forType(type, variableResolver);
	}


	/**
	 * 返回指定 {@link ParameterizedTypeReference} 对应的 {@link ResolvableType}。
	 * <p>注意：返回的 {@link ResolvableType} 实例可能不可序列化。
	 *
	 * @param typeReference 用于获取源类型的引用
	 * @return 指定 {@link ParameterizedTypeReference} 对应的 {@link ResolvableType}
	 * @see #forType(Type)
	 * @since 4.3.12
	 */
	public static ResolvableType forType(ParameterizedTypeReference<?> typeReference) {
		return forType(typeReference.getType(), null, null);
	}

	/**
	 * 返回一个由指定 {@link VariableResolver} 支持的 {@link Type} 对应的 {@link ResolvableType}。
	 *
	 * @param type             源类型或 {@code null}
	 * @param variableResolver 变量解析器或 {@code null}
	 * @return 指定 {@link Type} 和 {@link VariableResolver} 对应的 {@link ResolvableType}
	 */
	static ResolvableType forType(@Nullable Type type, @Nullable VariableResolver variableResolver) {
		return forType(type, null, variableResolver);
	}

	/**
	 * 返回一个由指定 {@link VariableResolver} 支持的 {@link Type} 对应的 {@link ResolvableType}。
	 *
	 * @param type             源类型或 {@code null}
	 * @param typeProvider     类型提供者或 {@code null}
	 * @param variableResolver 变量解析器或 {@code null}
	 * @return 指定 {@link Type} 和 {@link VariableResolver} 对应的 {@link ResolvableType}
	 */
	static ResolvableType forType(
			@Nullable Type type, @Nullable TypeProvider typeProvider, @Nullable VariableResolver variableResolver) {

		if (type == null && typeProvider != null) {
			type = SerializableTypeWrapper.forTypeProvider(typeProvider);
		}
		if (type == null) {
			return NONE;
		}

		// 对于简单的 Class 引用，直接构造包装器 ——
		// 无需复杂解析，因此不值得缓存...
		if (type instanceof Class) {
			return new ResolvableType(type, typeProvider, variableResolver, (ResolvableType) null);
		}

		// 访问时清理空闲条目，因为没有清理线程或类似机制。
		cache.purgeUnreferencedEntries();

		// 检查缓存 — 可能之前已解析过该 ResolvableType...
		ResolvableType resultType = new ResolvableType(type, typeProvider, variableResolver);
		ResolvableType cachedType = cache.get(resultType);
		if (cachedType == null) {
			cachedType = new ResolvableType(type, typeProvider, variableResolver, resultType.hash);
			cache.put(cachedType, cachedType);
		}
		resultType.resolved = cachedType.resolved;
		return resultType;
	}

	/**
	 * 清空内部 {@code ResolvableType}/{@code SerializableTypeWrapper} 缓存。
	 *
	 * @since 4.2
	 */
	public static void clearCache() {
		cache.clear();
		SerializableTypeWrapper.cache.clear();
	}


	/**
	 * 用于解析 {@link TypeVariable 类型变量} 的策略接口。
	 */
	interface VariableResolver extends Serializable {

		/**
		 * 返回解析器的源对象（用于 hashCode 和 equals）。
		 */
		Object getSource();

		/**
		 * 解析指定的变量。
		 *
		 * @param variable 要解析的变量
		 * @return 解析后的类型，找不到则返回 {@code null}
		 */
		@Nullable
		ResolvableType resolveVariable(TypeVariable<?> variable);
	}


	@SuppressWarnings("serial")
	private static class DefaultVariableResolver implements VariableResolver {

		private final ResolvableType source;

		DefaultVariableResolver(ResolvableType resolvableType) {
			this.source = resolvableType;
		}

		@Override
		@Nullable
		public ResolvableType resolveVariable(TypeVariable<?> variable) {
			return this.source.resolveVariable(variable);
		}

		@Override
		public Object getSource() {
			return this.source;
		}
	}


	@SuppressWarnings("serial")
	private static class TypeVariablesVariableResolver implements VariableResolver {

		private final TypeVariable<?>[] variables;

		private final ResolvableType[] generics;

		public TypeVariablesVariableResolver(TypeVariable<?>[] variables, ResolvableType[] generics) {
			this.variables = variables;
			this.generics = generics;
		}

		@Override
		@Nullable
		public ResolvableType resolveVariable(TypeVariable<?> variable) {
			TypeVariable<?> variableToCompare = SerializableTypeWrapper.unwrap(variable);
			for (int i = 0; i < this.variables.length; i++) {
				TypeVariable<?> resolvedVariable = SerializableTypeWrapper.unwrap(this.variables[i]);
				if (ObjectUtils.nullSafeEquals(resolvedVariable, variableToCompare)) {
					return this.generics[i];
				}
			}
			return null;
		}

		@Override
		public Object getSource() {
			return this.generics;
		}
	}


	private static final class SyntheticParameterizedType implements ParameterizedType, Serializable {

		private final Type rawType;

		private final Type[] typeArguments;

		public SyntheticParameterizedType(Type rawType, Type[] typeArguments) {
			this.rawType = rawType;
			this.typeArguments = typeArguments;
		}

		@Override
		public String getTypeName() {
			String typeName = this.rawType.getTypeName();
			if (this.typeArguments.length > 0) {
				StringJoiner stringJoiner = new StringJoiner(", ", "<", ">");
				for (Type argument : this.typeArguments) {
					stringJoiner.add(argument.getTypeName());
				}
				return typeName + stringJoiner;
			}
			return typeName;
		}

		@Override
		@Nullable
		public Type getOwnerType() {
			return null;
		}

		@Override
		public Type getRawType() {
			return this.rawType;
		}

		@Override
		public Type[] getActualTypeArguments() {
			return this.typeArguments;
		}

		@Override
		public boolean equals(@Nullable Object other) {
			if (this == other) {
				return true;
			}
			if (!(other instanceof ParameterizedType)) {
				return false;
			}
			ParameterizedType otherType = (ParameterizedType) other;
			return (otherType.getOwnerType() == null && this.rawType.equals(otherType.getRawType()) &&
					Arrays.equals(this.typeArguments, otherType.getActualTypeArguments()));
		}

		@Override
		public int hashCode() {
			return (this.rawType.hashCode() * 31 + Arrays.hashCode(this.typeArguments));
		}

		@Override
		public String toString() {
			return getTypeName();
		}
	}


	/**
	 * 用于处理 {@link WildcardType 通配符类型} 边界的内部辅助类。
	 */
	private static class WildcardBounds {

		private final Kind kind;

		private final ResolvableType[] bounds;

		/**
		 * 内部构造方法，用于创建新的 {@link WildcardBounds} 实例。
		 *
		 * @param kind   边界的种类
		 * @param bounds 边界数组
		 * @see #get(ResolvableType)
		 */
		public WildcardBounds(Kind kind, ResolvableType[] bounds) {
			this.kind = kind;
			this.bounds = bounds;
		}

		/**
		 * 如果此边界与指定边界种类相同则返回 {@code true}。
		 */
		public boolean isSameKind(WildcardBounds bounds) {
			return this.kind == bounds.kind;
		}

		/**
		 * 如果此边界可赋值给所有指定的类型，则返回 {@code true}。
		 *
		 * @param types 要测试的类型
		 * @return 如果此边界可赋值给所有类型则返回 {@code true}
		 */
		public boolean isAssignableFrom(ResolvableType... types) {
			for (ResolvableType bound : this.bounds) {
				for (ResolvableType type : types) {
					if (!isAssignable(bound, type)) {
						return false;
					}
				}
			}
			return true;
		}

		private boolean isAssignable(ResolvableType source, ResolvableType from) {
			return (this.kind == Kind.UPPER ? source.isAssignableFrom(from) : from.isAssignableFrom(source));
		}

		/**
		 * 返回底层的边界数组。
		 */
		public ResolvableType[] getBounds() {
			return this.bounds;
		}

		/**
		 * 获取指定类型的 {@link WildcardBounds} 实例，
		 * 如果指定类型无法解析为 {@link WildcardType} 则返回 {@code null}。
		 *
		 * @param type 源类型
		 * @return 一个 {@link WildcardBounds} 实例或 {@code null}
		 */
		@Nullable
		public static WildcardBounds get(ResolvableType type) {
			ResolvableType resolveToWildcard = type;
			while (!(resolveToWildcard.getType() instanceof WildcardType)) {
				if (resolveToWildcard == NONE) {
					return null;
				}
				resolveToWildcard = resolveToWildcard.resolveType();
			}
			WildcardType wildcardType = (WildcardType) resolveToWildcard.type;
			Kind boundsType = (wildcardType.getLowerBounds().length > 0 ? Kind.LOWER : Kind.UPPER);
			Type[] bounds = (boundsType == Kind.UPPER ? wildcardType.getUpperBounds() : wildcardType.getLowerBounds());
			ResolvableType[] resolvableBounds = new ResolvableType[bounds.length];
			for (int i = 0; i < bounds.length; i++) {
				resolvableBounds[i] = ResolvableType.forType(bounds[i], type.variableResolver);
			}
			return new WildcardBounds(boundsType, resolvableBounds);
		}

		/**
		 * 边界的各种类型。
		 */
		enum Kind {UPPER, LOWER}
	}


	/**
	 * 用于表示空值的内部 {@link Type}。
	 */
	@SuppressWarnings("serial")
	static class EmptyType implements Type, Serializable {

		static final Type INSTANCE = new EmptyType();

		Object readResolve() {
			return INSTANCE;
		}
	}

}
