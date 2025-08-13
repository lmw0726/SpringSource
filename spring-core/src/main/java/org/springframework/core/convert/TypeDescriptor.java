/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.core.convert;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * 关于要转换的源类型或目标类型的上下文描述符。
 * <p>能够表示数组和泛型集合类型。
 *
 * @author Keith Donald
 * @author Andy Clement
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @author Sam Brannen
 * @author Stephane Nicoll
 * @see ConversionService#canConvert(TypeDescriptor, TypeDescriptor)
 * @see ConversionService#convert(Object, TypeDescriptor, TypeDescriptor)
 * @since 3.0
 */
@SuppressWarnings("serial")
public class TypeDescriptor implements Serializable {

	private static final Annotation[] EMPTY_ANNOTATION_ARRAY = new Annotation[0];
	/**
	 * 通用类型缓存，Key为Class对象，Value为类型描述符
	 */
	private static final Map<Class<?>, TypeDescriptor> commonTypesCache = new HashMap<>(32);

	private static final Class<?>[] CACHED_COMMON_TYPES = {
			boolean.class, Boolean.class, byte.class, Byte.class, char.class, Character.class,
			double.class, Double.class, float.class, Float.class, int.class, Integer.class,
			long.class, Long.class, short.class, Short.class, String.class, Object.class};

	static {
		for (Class<?> preCachedClass : CACHED_COMMON_TYPES) {
			commonTypesCache.put(preCachedClass, valueOf(preCachedClass));
		}
	}


	private final Class<?> type;

	private final ResolvableType resolvableType;

	private final AnnotatedElementAdapter annotatedElement;


	/**
	 * 从 {@link MethodParameter} 创建一个新的类型描述符。
	 * <p>当源或目标转换点是构造函数参数、方法参数或方法返回值时，使用此构造函数。
	 *
	 * @param methodParameter 方法参数
	 */
	public TypeDescriptor(MethodParameter methodParameter) {
		this.resolvableType = ResolvableType.forMethodParameter(methodParameter);
		this.type = this.resolvableType.resolve(methodParameter.getNestedParameterType());
		this.annotatedElement = new AnnotatedElementAdapter(methodParameter.getParameterIndex() == -1 ?
				methodParameter.getMethodAnnotations() : methodParameter.getParameterAnnotations());
	}

	/**
	 * 从 {@link Field} 创建一个新的类型描述符。
	 * <p>当源或目标转换点是字段时，使用此构造函数。
	 *
	 * @param field 字段
	 */
	public TypeDescriptor(Field field) {
		this.resolvableType = ResolvableType.forField(field);
		this.type = this.resolvableType.resolve(field.getType());
		this.annotatedElement = new AnnotatedElementAdapter(field.getAnnotations());
	}

	/**
	 * 从 {@link Property} 创建一个新的类型描述符。
	 * <p>当源或目标转换点是 Java 类的属性时，使用此构造函数。
	 *
	 * @param property 属性
	 */
	public TypeDescriptor(Property property) {
		Assert.notNull(property, "Property must not be null");
		this.resolvableType = ResolvableType.forMethodParameter(property.getMethodParameter());
		this.type = this.resolvableType.resolve(property.getType());
		this.annotatedElement = new AnnotatedElementAdapter(property.getAnnotations());
	}

	/**
	 * 从 {@link ResolvableType} 创建新的类型描述符。
	 * <p> 此构造函数在内部使用，也可能由支持具有扩展类型系统的非Java语言的子类使用。
	 * 截至5.1.4，它是公开的，而它以前是受保护的。
	 *
	 * @param resolvableType 可解析类型
	 * @param type           备份类型 (或 {@code null}，如果它应该得到解决)
	 * @param annotations    类型注释
	 * @since 4.0
	 */
	public TypeDescriptor(ResolvableType resolvableType, @Nullable Class<?> type, @Nullable Annotation[] annotations) {
		this.resolvableType = resolvableType;
		//如果类型为空，则使用Object类，否则使用目标类型
		this.type = (type != null ? type : resolvableType.toClass());
		this.annotatedElement = new AnnotatedElementAdapter(annotations);
	}


	/**
	 * {@link #getType()} 的变体，它通过返回其对象包装器类型来说明原始类型。
	 * <p>这对于希望标准化为基于对象的类型并且不直接使用原始类型的转换服务实现很有用。
	 */
	public Class<?> getObjectType() {
		//如有必要，转换为原始的包装类型
		return ClassUtils.resolvePrimitiveIfNecessary(getType());
	}

	/**
	 * 此TypeDescriptor描述的备份类、方法参数、字段或属性的类型。
	 * <p>按原始对象返回原始类型。有关此操作的变体，请参阅 {@link #getObjectType()}，
	 * 该操作可在必要时将原始类型解析为其相应的对象类型。
	 *
	 * @see #getObjectType()
	 */
	public Class<?> getType() {
		return this.type;
	}

	/**
	 * 返回底层 {@link ResolvableType}。
	 *
	 * @since 4.0
	 */
	public ResolvableType getResolvableType() {
		return this.resolvableType;
	}

	/**
	 * 返回描述符的底层源。根据 {@link TypeDescriptor} 的构造方式，
	 * 将返回 {@link Field}、{@link MethodParameter} 或 {@link Type}。
	 * 此方法主要用于提供对其他 JVM 语言可能提供的额外类型信息或元数据的访问。
	 *
	 * @since 4.0
	 */
	public Object getSource() {
		return this.resolvableType.getSource();
	}

	/**
	 * 通过将其类型设置为提供的值的类来缩窄此 {@link TypeDescriptor}。
	 * <p>如果值为 {@code null}，则不执行缩窄，并返回此 TypeDescriptor 不变。
	 * <p>设计为在绑定框架读取属性、字段或方法返回值时调用。允许这些框架缩窄
	 * 从声明的属性、字段或方法返回值类型构建的 TypeDescriptor。例如，
	 * 声明为 {@code java.lang.Object} 的字段，如果被设置为 {@code java.util.HashMap} 值，
	 * 则会被缩窄为 {@code java.util.HashMap}。然后可以使用缩窄的 TypeDescriptor
	 * 将 HashMap 转换为其他类型。注解和嵌套类型上下文在缩窄的副本中得到保留。
	 *
	 * @param value 用于缩窄此类型描述符的值
	 * @return 缩窄后的 TypeDescriptor（返回一个其类型已更新为提供值的类的副本）
	 */
	public TypeDescriptor narrow(@Nullable Object value) {
		if (value == null) {
			return this;
		}
		ResolvableType narrowed = ResolvableType.forType(value.getClass(), getResolvableType());
		return new TypeDescriptor(narrowed, value.getClass(), getAnnotations());
	}

	/**
	 * 将此 {@link TypeDescriptor} 转换为超类或实现的接口，
	 * 保留注解和嵌套类型上下文。
	 *
	 * @param superType 要转换到的超类型（可以为 {@code null}）
	 * @return 向上转换类型的新 TypeDescriptor
	 * @throws IllegalArgumentException 如果此类型不能分配给超类型
	 * @since 3.2
	 */
	@Nullable
	public TypeDescriptor upcast(@Nullable Class<?> superType) {
		if (superType == null) {
			return null;
		}
		Assert.isAssignable(superType, getType());
		return new TypeDescriptor(getResolvableType().as(superType), superType, getAnnotations());
	}

	/**
	 * 返回此类型的名称：完全限定类名。
	 */
	public String getName() {
		return ClassUtils.getQualifiedName(getType());
	}

	/**
	 * 此类型是否为基本类型？
	 */
	public boolean isPrimitive() {
		return getType().isPrimitive();
	}

	/**
	 * 返回与此类型描述符关联的注解（如果有）。
	 *
	 * @return 注解数组，如果没有则返回空数组
	 */
	public Annotation[] getAnnotations() {
		return this.annotatedElement.getAnnotations();
	}

	/**
	 * 确定此类型描述符是否具有指定的注解。
	 * <p>从 Spring Framework 4.2 开始，此方法支持任意级别的元注解。
	 *
	 * @param annotationType 注解类型
	 * @return 如果注解存在则为 <tt>true</tt>
	 */
	public boolean hasAnnotation(Class<? extends Annotation> annotationType) {
		if (this.annotatedElement.isEmpty()) {
			// 快捷方式：AnnotatedElementUtils 需要期待 AnnotatedElement.getAnnotations()
			// 返回数组的副本，而我们可以在这里更高效地完成。
			return false;
		}
		return AnnotatedElementUtils.isAnnotated(this.annotatedElement, annotationType);
	}

	/**
	 * 获取此类型描述符上指定 {@code annotationType} 的注解。
	 * <p>从 Spring Framework 4.2 开始，此方法支持任意级别的元注解。
	 *
	 * @param annotationType 注解类型
	 * @return 注解，如果此类型描述符上不存在此类注解则返回 {@code null}
	 */
	@Nullable
	public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
		if (this.annotatedElement.isEmpty()) {
			// 快捷方式：AnnotatedElementUtils 需要期待 AnnotatedElement.getAnnotations()
			// 返回数组的副本，而我们可以在这里更高效地完成。
			return null;
		}
		return AnnotatedElementUtils.getMergedAnnotation(this.annotatedElement, annotationType);
	}

	/**
	 * 如果可以将此类型描述符的对象分配给给定类型描述符描述的位置，则返回true。
	 * <p> 例如，{@code valueOf(String.class).isAssignableTo (valueOf(CharSequence.class))}
	 * 返回 {@code true}，因为可以将字符串值分配给CharSequence变量。
	 * <p>
	 * 另一方面，{@code valueOf(Number.class).isAssignableTo (valueOf(Integer.class))}
	 * 返回 {@code false}，因为虽然所有的整数都是数字，但并不是所有的数字都是整数。
	 * <p> 对于数组，集合和映射，如果声明，则检查元素和键值类型。
	 * 例如，List&lt;String&gt;字段值可分配给Collection&lt;CharSequence&gt; 字段，但List&lt;Number&gt;  不可分配给List&lt;Integer&gt;。
	 *
	 * @return 如果此类型可分配给提供的，将返回{@code true}
	 * type descriptor
	 * @see #getObjectType()
	 */
	public boolean isAssignableTo(TypeDescriptor typeDescriptor) {
		boolean typesAssignable = typeDescriptor.getObjectType().isAssignableFrom(getObjectType());
		if (!typesAssignable) {
			//如果父类型无法分配给子类型，则返回false。
			return false;
		}
		if (isArray() && typeDescriptor.isArray()) {
			//如果两者的类型都是数组，比较嵌套在数组里的类型元素
			return isNestedAssignable(getElementTypeDescriptor(), typeDescriptor.getElementTypeDescriptor());
		} else if (isCollection() && typeDescriptor.isCollection()) {
			//如果两者的类型都是集合，比较泛型里的类型元素
			return isNestedAssignable(getElementTypeDescriptor(), typeDescriptor.getElementTypeDescriptor());
		} else if (isMap() && typeDescriptor.isMap()) {
			//如果两者都是Map，同时表Key和Value的类型是否都可以赋值
			return isNestedAssignable(getMapKeyTypeDescriptor(), typeDescriptor.getMapKeyTypeDescriptor()) &&
					isNestedAssignable(getMapValueTypeDescriptor(), typeDescriptor.getMapValueTypeDescriptor());
		} else {
			return true;
		}
	}

	private boolean isNestedAssignable(@Nullable TypeDescriptor nestedTypeDescriptor,
									   @Nullable TypeDescriptor otherNestedTypeDescriptor) {

		return (nestedTypeDescriptor == null || otherNestedTypeDescriptor == null ||
				nestedTypeDescriptor.isAssignableTo(otherNestedTypeDescriptor));
	}

	/**
	 * 此类型是否为 {@link Collection} 类型？
	 */
	public boolean isCollection() {
		return Collection.class.isAssignableFrom(getType());
	}

	/**
	 * 这种类型是数组类型吗？
	 */
	public boolean isArray() {
		return getType().isArray();
	}

	/**
	 * 如果此类型是数组，则返回数组的组件类型。
	 * 如果此类型是 {@code Stream}，则返回流的组件类型。
	 * 如果此类型是 {@link Collection} 且已参数化，则返回 Collection 的元素类型。
	 * 如果 Collection 未参数化，则返回 {@code null}，表示元素类型未声明。
	 *
	 * @return 数组组件类型或 Collection 元素类型，如果此类型不是数组类型或
	 * {@code java.util.Collection}，或其元素类型未参数化，则返回 {@code null}
	 * @see #elementTypeDescriptor(Object)
	 */
	@Nullable
	public TypeDescriptor getElementTypeDescriptor() {
		if (getResolvableType().isArray()) {
			return new TypeDescriptor(getResolvableType().getComponentType(), null, getAnnotations());
		}
		if (Stream.class.isAssignableFrom(getType())) {
			return getRelatedIfResolvable(this, getResolvableType().as(Stream.class).getGeneric(0));
		}
		return getRelatedIfResolvable(this, getResolvableType().asCollection().getGeneric(0));
	}

	/**
	 * 如果此类型是 {@link Collection} 或数组，则从提供的集合或数组元素创建元素 TypeDescriptor。
	 * <p>将 {@link #getElementTypeDescriptor() elementType} 属性缩窄到提供的
	 * 集合或数组元素的类。例如，如果此描述符描述 {@code java.util.List<java.lang.Number>}
	 * 且元素参数是 {@code java.lang.Integer}，则返回的 TypeDescriptor 将是
	 * {@code java.lang.Integer}。如果此描述符描述 {@code java.util.List<?>}
	 * 且元素参数是 {@code java.lang.Integer}，则返回的 TypeDescriptor 也将是
	 * {@code java.lang.Integer}。
	 * <p>注解和嵌套类型上下文将在返回的缩窄 TypeDescriptor 中得到保留。
	 *
	 * @param element 集合或数组元素
	 * @return 元素类型描述符，缩窄为提供的元素的类型
	 * @see #getElementTypeDescriptor()
	 * @see #narrow(Object)
	 */
	@Nullable
	public TypeDescriptor elementTypeDescriptor(Object element) {
		return narrow(element, getElementTypeDescriptor());
	}

	/**
	 * 此类型是否为 {@link Map} 类型？
	 */
	public boolean isMap() {
		return Map.class.isAssignableFrom(getType());
	}

	/**
	 * 如果此类型是 {@link Map} 且其键类型已参数化，则返回映射的键类型。
	 * 如果 Map 的键类型未参数化，则返回 {@code null}，表示键类型未声明。
	 *
	 * @return Map 键类型，如果此类型是 Map 但其键类型未参数化则返回 {@code null}
	 * @throws IllegalStateException 如果此类型不是 {@code java.util.Map}
	 */
	@Nullable
	public TypeDescriptor getMapKeyTypeDescriptor() {
		Assert.state(isMap(), "Not a [java.util.Map]");
		return getRelatedIfResolvable(this, getResolvableType().asMap().getGeneric(0));
	}

	/**
	 * 如果此类型是 {@link Map}，则从提供的映射键创建 mapKey {@link TypeDescriptor}。
	 * <p>将 {@link #getMapKeyTypeDescriptor() mapKeyType} 属性缩窄到提供的
	 * 映射键的类。例如，如果此描述符描述 {@code java.util.Map<java.lang.Number, java.lang.String>}
	 * 且键参数是 {@code java.lang.Integer}，则返回的 TypeDescriptor 将是
	 * {@code java.lang.Integer}。如果此描述符描述 {@code java.util.Map<?, ?>}
	 * 且键参数是 {@code java.lang.Integer}，则返回的 TypeDescriptor 也将是
	 * {@code java.lang.Integer}。
	 * <p>注解和嵌套类型上下文将在返回的缩窄 TypeDescriptor 中得到保留。
	 *
	 * @param mapKey 映射键
	 * @return 映射键类型描述符
	 * @throws IllegalStateException 如果此类型不是 {@code java.util.Map}
	 * @see #narrow(Object)
	 */
	@Nullable
	public TypeDescriptor getMapKeyTypeDescriptor(Object mapKey) {
		return narrow(mapKey, getMapKeyTypeDescriptor());
	}

	/**
	 * 如果此类型是 {@link Map} 且其值类型已参数化，则返回映射的值类型。
	 * <p>如果 Map 的值类型未参数化，则返回 {@code null}，表示值类型未声明。
	 *
	 * @return Map 值类型，如果此类型是 Map 但其值类型未参数化则返回 {@code null}
	 * @throws IllegalStateException 如果此类型不是 {@code java.util.Map}
	 */
	@Nullable
	public TypeDescriptor getMapValueTypeDescriptor() {
		Assert.state(isMap(), "Not a [java.util.Map]");
		return getRelatedIfResolvable(this, getResolvableType().asMap().getGeneric(1));
	}

	/**
	 * 如果此类型是 {@link Map}，则从提供的映射值创建 mapValue {@link TypeDescriptor}。
	 * <p>将 {@link #getMapValueTypeDescriptor() mapValueType} 属性缩窄到提供的
	 * 映射值的类。例如，如果此描述符描述 {@code java.util.Map<java.lang.String, java.lang.Number>}
	 * 且值参数是 {@code java.lang.Integer}，则返回的 TypeDescriptor 将是
	 * {@code java.lang.Integer}。如果此描述符描述 {@code java.util.Map<?, ?>}
	 * 且值参数是 {@code java.lang.Integer}，则返回的 TypeDescriptor 也将是
	 * {@code java.lang.Integer}。
	 * <p>注解和嵌套类型上下文将在返回的缩窄 TypeDescriptor 中得到保留。
	 *
	 * @param mapValue 映射值
	 * @return 映射值类型描述符
	 * @throws IllegalStateException 如果此类型不是 {@code java.util.Map}
	 * @see #narrow(Object)
	 */
	@Nullable
	public TypeDescriptor getMapValueTypeDescriptor(Object mapValue) {
		return narrow(mapValue, getMapValueTypeDescriptor());
	}

	@Nullable
	private TypeDescriptor narrow(@Nullable Object value, @Nullable TypeDescriptor typeDescriptor) {
		if (typeDescriptor != null) {
			return typeDescriptor.narrow(value);
		}
		if (value != null) {
			return narrow(value);
		}
		return null;
	}

	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof TypeDescriptor)) {
			return false;
		}
		TypeDescriptor otherDesc = (TypeDescriptor) other;
		if (getType() != otherDesc.getType()) {
			return false;
		}
		if (!annotationsMatch(otherDesc)) {
			return false;
		}
		if (isCollection() || isArray()) {
			return ObjectUtils.nullSafeEquals(getElementTypeDescriptor(), otherDesc.getElementTypeDescriptor());
		} else if (isMap()) {
			return (ObjectUtils.nullSafeEquals(getMapKeyTypeDescriptor(), otherDesc.getMapKeyTypeDescriptor()) &&
					ObjectUtils.nullSafeEquals(getMapValueTypeDescriptor(), otherDesc.getMapValueTypeDescriptor()));
		} else {
			return true;
		}
	}

	private boolean annotationsMatch(TypeDescriptor otherDesc) {
		Annotation[] anns = getAnnotations();
		Annotation[] otherAnns = otherDesc.getAnnotations();
		if (anns == otherAnns) {
			return true;
		}
		if (anns.length != otherAnns.length) {
			return false;
		}
		if (anns.length > 0) {
			for (int i = 0; i < anns.length; i++) {
				if (!annotationEquals(anns[i], otherAnns[i])) {
					return false;
				}
			}
		}
		return true;
	}

	private boolean annotationEquals(Annotation ann, Annotation otherAnn) {
		// Annotation.equals 是反射式的且相当慢，所以让我们先检查身份和代理类型。
		return (ann == otherAnn || (ann.getClass() == otherAnn.getClass() && ann.equals(otherAnn)));
	}

	@Override
	public int hashCode() {
		return getType().hashCode();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		for (Annotation ann : getAnnotations()) {
			builder.append('@').append(ann.annotationType().getName()).append(' ');
		}
		builder.append(getResolvableType());
		return builder.toString();
	}


	/**
	 * 为对象创建新的类型描述符。
	 * <p> 在要求转换系统将其转换为另一种类型之前，请使用此工厂方法对源对象进行内省。
	 * <p> 如果提供的对象为 {@code null}，则返回 {@code null}，否则调用 {@link #valueOf(Class)} 从对象的类构建TypeDescriptor。
	 *
	 * @param source 源对象
	 * @return 类型描述符
	 */
	@Nullable
	public static TypeDescriptor forObject(@Nullable Object source) {
		//如果源对象为null，返回null
		//否则调用valueOf(Class)从对象的类构建TypeDescriptor
		return (source == null ? null : valueOf(source.getClass()));
	}

	/**
	 * 从给定类型创建新的类型描述符。
	 * <p> 当没有类型位置 (例如方法参数或字段) 可用于提供其他转换上下文时，使用它指示转换系统将对象转换为特定的目标类型。
	 * <p> 通常更喜欢使用 {@link #forObject(Object)} 从源对象构造类型描述符，因为它处理 {@code null} 对象大小写。
	 *
	 * @param type 类 (可以是 {@code null} 表示 {@code Object.class})
	 * @return 对应类型描述符
	 */
	public static TypeDescriptor valueOf(@Nullable Class<?> type) {
		if (type == null) {
			//如果类型为空，将类型重置为Object类型
			type = Object.class;
		}
		//通过通用类型缓存获取类型描述符
		TypeDescriptor desc = commonTypesCache.get(type);
		//如果类型描述符为空，将会返回Object对象的类型描述符
		return (desc == null ? new TypeDescriptor(ResolvableType.forClass(type), null, null) : desc);
	}

	/**
	 * 从 {@link java.util.Collection} 类型创建新的类型描述符。
	 * <p>用于转换为类型化的集合。
	 * <p>例如，{@code List<String>} 可以通过转换为使用此方法构建的目标类型
	 * 来转换为 {@code List<EmailAddress>}。
	 * 构造这样一个 {@code TypeDescriptor} 的方法调用看起来像这样：
	 * {@code collection(List.class, TypeDescriptor.valueOf(EmailAddress.class));}
	 *
	 * @param collectionType        集合类型，必须实现 {@link Collection}。
	 * @param elementTypeDescriptor 集合元素类型的描述符，
	 *                              用于转换集合元素
	 * @return 集合类型描述符
	 */
	public static TypeDescriptor collection(Class<?> collectionType, @Nullable TypeDescriptor elementTypeDescriptor) {
		Assert.notNull(collectionType, "Collection type must not be null");
		if (!Collection.class.isAssignableFrom(collectionType)) {
			throw new IllegalArgumentException("Collection type must be a [java.util.Collection]");
		}
		ResolvableType element = (elementTypeDescriptor != null ? elementTypeDescriptor.resolvableType : null);
		return new TypeDescriptor(ResolvableType.forClassWithGenerics(collectionType, element), null, null);
	}

	/**
	 * 从 {@link java.util.Map} 类型创建新的类型描述符。
	 * <p>用于转换为类型化的映射。
	 * <p>例如，Map&lt;String, String&gt; 可以通过转换为使用此方法构建的目标类型
	 * 来转换为 Map&lt;Id, EmailAddress&gt;：
	 * 构造这样一个 TypeDescriptor 的方法调用看起来像这样：
	 * <pre class="code">
	 * map(Map.class, TypeDescriptor.valueOf(Id.class), TypeDescriptor.valueOf(EmailAddress.class));
	 * </pre>
	 *
	 * @param mapType             映射类型，必须实现 {@link Map}
	 * @param keyTypeDescriptor   映射键类型的描述符，用于转换映射键
	 * @param valueTypeDescriptor 映射值类型的描述符，用于转换映射值
	 * @return 映射类型描述符
	 */
	public static TypeDescriptor map(Class<?> mapType, @Nullable TypeDescriptor keyTypeDescriptor,
									 @Nullable TypeDescriptor valueTypeDescriptor) {

		Assert.notNull(mapType, "Map type must not be null");
		if (!Map.class.isAssignableFrom(mapType)) {
			throw new IllegalArgumentException("Map type must be a [java.util.Map]");
		}
		ResolvableType key = (keyTypeDescriptor != null ? keyTypeDescriptor.resolvableType : null);
		ResolvableType value = (valueTypeDescriptor != null ? valueTypeDescriptor.resolvableType : null);
		return new TypeDescriptor(ResolvableType.forClassWithGenerics(mapType, key, value), null, null);
	}

	/**
	 * 创建指定类型数组的新类型描述符。
	 * <p>例如，要创建 {@code Map<String,String>[]}，请使用：
	 * <pre class="code">
	 * TypeDescriptor.array(TypeDescriptor.map(Map.class, TypeDescriptor.value(String.class), TypeDescriptor.value(String.class)));
	 * </pre>
	 *
	 * @param elementTypeDescriptor 数组元素的 {@link TypeDescriptor} 或 {@code null}
	 * @return 数组 {@link TypeDescriptor}，如果 {@code elementTypeDescriptor} 为 {@code null} 则返回 {@code null}
	 * @since 3.2.1
	 */
	@Nullable
	public static TypeDescriptor array(@Nullable TypeDescriptor elementTypeDescriptor) {
		if (elementTypeDescriptor == null) {
			return null;
		}
		return new TypeDescriptor(ResolvableType.forArrayComponent(elementTypeDescriptor.resolvableType),
				null, elementTypeDescriptor.getAnnotations());
	}

	/**
	 * 为方法参数中声明的嵌套类型创建类型描述符。
	 * <p>例如，如果方法参数是 {@code List<String>} 且嵌套级别为 1，
	 * 则嵌套类型描述符将是 String.class。
	 * <p>如果方法参数是 {@code List<List<String>>} 且嵌套级别为 2，
	 * 则嵌套类型描述符也将是 String.class。
	 * <p>如果方法参数是 {@code Map<Integer, String>} 且嵌套级别为 1，
	 * 则嵌套类型描述符将是 String，来自映射的值。
	 * <p>如果方法参数是 {@code List<Map<Integer, String>>} 且嵌套级别为 2，
	 * 则嵌套类型描述符将是 String，来自映射的值。
	 * <p>如果由于未声明而无法获得嵌套类型，则返回 {@code null}。
	 * 例如，如果方法参数是 {@code List<?>}，则返回的嵌套类型描述符将为 {@code null}。
	 *
	 * @param methodParameter 嵌套级别为 1 的方法参数
	 * @param nestingLevel    方法参数内集合/数组元素或映射键/值声明的嵌套级别
	 * @return 指定嵌套级别的嵌套类型描述符，如果无法获得则返回 {@code null}
	 * @throws IllegalArgumentException 如果输入的 {@link MethodParameter} 参数的嵌套级别不是 1，
	 *                                  或者直到指定嵌套级别的类型不是集合、数组或映射类型
	 */
	@Nullable
	public static TypeDescriptor nested(MethodParameter methodParameter, int nestingLevel) {
		if (methodParameter.getNestingLevel() != 1) {
			throw new IllegalArgumentException("MethodParameter nesting level must be 1: " +
					"use the nestingLevel parameter to specify the desired nestingLevel for nested type traversal");
		}
		return nested(new TypeDescriptor(methodParameter), nestingLevel);
	}

	/**
	 * 为字段中声明的嵌套类型创建类型描述符。
	 * <p>例如，如果字段是 {@code List<String>} 且嵌套级别为 1，
	 * 则嵌套类型描述符将是 {@code String.class}。
	 * <p>如果字段是 {@code List<List<String>>} 且嵌套级别为 2，
	 * 则嵌套类型描述符也将是 {@code String.class}。
	 * <p>如果字段是 {@code Map<Integer, String>} 且嵌套级别为 1，
	 * 则嵌套类型描述符将是 String，来自映射的值。
	 * <p>如果字段是 {@code List<Map<Integer, String>>} 且嵌套级别为 2，
	 * 则嵌套类型描述符将是 String，来自映射的值。
	 * <p>如果由于未声明而无法获得嵌套类型，则返回 {@code null}。
	 * 例如，如果字段是 {@code List<?>}，则返回的嵌套类型描述符将为 {@code null}。
	 *
	 * @param field        字段
	 * @param nestingLevel 字段内集合/数组元素或映射键/值声明的嵌套级别
	 * @return 指定嵌套级别的嵌套类型描述符，如果无法获得则返回 {@code null}
	 * @throws IllegalArgumentException 如果直到指定嵌套级别的类型不是集合、数组或映射类型
	 */
	@Nullable
	public static TypeDescriptor nested(Field field, int nestingLevel) {
		return nested(new TypeDescriptor(field), nestingLevel);
	}

	/**
	 * 为属性中声明的嵌套类型创建类型描述符。
	 * <p>例如，如果属性是 {@code List<String>} 且嵌套级别为 1，
	 * 则嵌套类型描述符将是 {@code String.class}。
	 * <p>如果属性是 {@code List<List<String>>} 且嵌套级别为 2，
	 * 则嵌套类型描述符也将是 {@code String.class}。
	 * <p>如果属性是 {@code Map<Integer, String>} 且嵌套级别为 1，
	 * 则嵌套类型描述符将是 String，来自映射的值。
	 * <p>如果属性是 {@code List<Map<Integer, String>>} 且嵌套级别为 2，
	 * 则嵌套类型描述符将是 String，来自映射的值。
	 * <p>如果由于未声明而无法获得嵌套类型，则返回 {@code null}。
	 * 例如，如果属性是 {@code List<?>}，则返回的嵌套类型描述符将为 {@code null}。
	 *
	 * @param property     属性
	 * @param nestingLevel 属性内集合/数组元素或映射键/值声明的嵌套级别
	 * @return 指定嵌套级别的嵌套类型描述符，如果无法获得则返回 {@code null}
	 * @throws IllegalArgumentException 如果直到指定嵌套级别的类型不是集合、数组或映射类型
	 */
	@Nullable
	public static TypeDescriptor nested(Property property, int nestingLevel) {
		return nested(new TypeDescriptor(property), nestingLevel);
	}

	@Nullable
	private static TypeDescriptor nested(TypeDescriptor typeDescriptor, int nestingLevel) {
		ResolvableType nested = typeDescriptor.resolvableType;
		for (int i = 0; i < nestingLevel; i++) {
			if (Object.class == nested.getType()) {
				// 可能是集合类型，但我们不知道其元素类型，
				// 所以让我们假设有一个 Object 类型的元素类型...
			} else {
				nested = nested.getNested(2);
			}
		}
		if (nested == ResolvableType.NONE) {
			return null;
		}
		return getRelatedIfResolvable(typeDescriptor, nested);
	}

	@Nullable
	private static TypeDescriptor getRelatedIfResolvable(TypeDescriptor source, ResolvableType type) {
		if (type.resolve() == null) {
			return null;
		}
		return new TypeDescriptor(type, null, source.getAnnotations());
	}


	/**
	 * 适配器类，用于将 {@code TypeDescriptor} 的注解作为
	 * {@link AnnotatedElement} 暴露，特别是暴露给 {@link AnnotatedElementUtils}。
	 *
	 * @see AnnotatedElementUtils#isAnnotated(AnnotatedElement, Class)
	 * @see AnnotatedElementUtils#getMergedAnnotation(AnnotatedElement, Class)
	 */
	private class AnnotatedElementAdapter implements AnnotatedElement, Serializable {

		@Nullable
		private final Annotation[] annotations;

		public AnnotatedElementAdapter(@Nullable Annotation[] annotations) {
			this.annotations = annotations;
		}

		@Override
		public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
			for (Annotation annotation : getAnnotations()) {
				if (annotation.annotationType() == annotationClass) {
					return true;
				}
			}
			return false;
		}

		@Override
		@Nullable
		@SuppressWarnings("unchecked")
		public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
			for (Annotation annotation : getAnnotations()) {
				if (annotation.annotationType() == annotationClass) {
					return (T) annotation;
				}
			}
			return null;
		}

		@Override
		public Annotation[] getAnnotations() {
			return (this.annotations != null ? this.annotations.clone() : EMPTY_ANNOTATION_ARRAY);
		}

		@Override
		public Annotation[] getDeclaredAnnotations() {
			return getAnnotations();
		}

		public boolean isEmpty() {
			return ObjectUtils.isEmpty(this.annotations);
		}

		@Override
		public boolean equals(@Nullable Object other) {
			return (this == other || (other instanceof AnnotatedElementAdapter &&
					Arrays.equals(this.annotations, ((AnnotatedElementAdapter) other).annotations)));
		}

		@Override
		public int hashCode() {
			return Arrays.hashCode(this.annotations);
		}

		@Override
		public String toString() {
			return TypeDescriptor.this.toString();
		}
	}

}
