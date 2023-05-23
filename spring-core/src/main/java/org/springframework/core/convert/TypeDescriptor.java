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

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * Contextual descriptor about a type to convert from or to.
 * <p>Capable of representing arrays and generic collection types.
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
	 * Create a new type descriptor from a {@link MethodParameter}.
	 * <p>Use this constructor when a source or target conversion point is a
	 * constructor parameter, method parameter, or method return value.
	 *
	 * @param methodParameter the method parameter
	 */
	public TypeDescriptor(MethodParameter methodParameter) {
		this.resolvableType = ResolvableType.forMethodParameter(methodParameter);
		this.type = this.resolvableType.resolve(methodParameter.getNestedParameterType());
		this.annotatedElement = new AnnotatedElementAdapter(methodParameter.getParameterIndex() == -1 ?
				methodParameter.getMethodAnnotations() : methodParameter.getParameterAnnotations());
	}

	/**
	 * Create a new type descriptor from a {@link Field}.
	 * <p>Use this constructor when a source or target conversion point is a field.
	 *
	 * @param field the field
	 */
	public TypeDescriptor(Field field) {
		this.resolvableType = ResolvableType.forField(field);
		this.type = this.resolvableType.resolve(field.getType());
		this.annotatedElement = new AnnotatedElementAdapter(field.getAnnotations());
	}

	/**
	 * Create a new type descriptor from a {@link Property}.
	 * <p>Use this constructor when a source or target conversion point is a
	 * property on a Java class.
	 *
	 * @param property the property
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
	 * Return the underlying source of the descriptor. Will return a {@link Field},
	 * {@link MethodParameter} or {@link Type} depending on how the {@link TypeDescriptor}
	 * was constructed. This method is primarily to provide access to additional
	 * type information or meta-data that alternative JVM languages may provide.
	 *
	 * @since 4.0
	 */
	public Object getSource() {
		return this.resolvableType.getSource();
	}

	/**
	 * Narrows this {@link TypeDescriptor} by setting its type to the class of the
	 * provided value.
	 * <p>If the value is {@code null}, no narrowing is performed and this TypeDescriptor
	 * is returned unchanged.
	 * <p>Designed to be called by binding frameworks when they read property, field,
	 * or method return values. Allows such frameworks to narrow a TypeDescriptor built
	 * from a declared property, field, or method return value type. For example, a field
	 * declared as {@code java.lang.Object} would be narrowed to {@code java.util.HashMap}
	 * if it was set to a {@code java.util.HashMap} value. The narrowed TypeDescriptor
	 * can then be used to convert the HashMap to some other type. Annotation and nested
	 * type context is preserved by the narrowed copy.
	 *
	 * @param value the value to use for narrowing this type descriptor
	 * @return this TypeDescriptor narrowed (returns a copy with its type updated to the
	 * class of the provided value)
	 */
	public TypeDescriptor narrow(@Nullable Object value) {
		if (value == null) {
			return this;
		}
		ResolvableType narrowed = ResolvableType.forType(value.getClass(), getResolvableType());
		return new TypeDescriptor(narrowed, value.getClass(), getAnnotations());
	}

	/**
	 * Cast this {@link TypeDescriptor} to a superclass or implemented interface
	 * preserving annotations and nested type context.
	 *
	 * @param superType the super type to cast to (can be {@code null})
	 * @return a new TypeDescriptor for the up-cast type
	 * @throws IllegalArgumentException if this type is not assignable to the super-type
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
	 * Return the name of this type: the fully qualified class name.
	 */
	public String getName() {
		return ClassUtils.getQualifiedName(getType());
	}

	/**
	 * Is this type a primitive type?
	 */
	public boolean isPrimitive() {
		return getType().isPrimitive();
	}

	/**
	 * Return the annotations associated with this type descriptor, if any.
	 *
	 * @return the annotations, or an empty array if none
	 */
	public Annotation[] getAnnotations() {
		return this.annotatedElement.getAnnotations();
	}

	/**
	 * Determine if this type descriptor has the specified annotation.
	 * <p>As of Spring Framework 4.2, this method supports arbitrary levels
	 * of meta-annotations.
	 *
	 * @param annotationType the annotation type
	 * @return <tt>true</tt> if the annotation is present
	 */
	public boolean hasAnnotation(Class<? extends Annotation> annotationType) {
		if (this.annotatedElement.isEmpty()) {
			// Shortcut: AnnotatedElementUtils would have to expect AnnotatedElement.getAnnotations()
			// to return a copy of the array, whereas we can do it more efficiently here.
			return false;
		}
		return AnnotatedElementUtils.isAnnotated(this.annotatedElement, annotationType);
	}

	/**
	 * Obtain the annotation of the specified {@code annotationType} that is on this type descriptor.
	 * <p>As of Spring Framework 4.2, this method supports arbitrary levels of meta-annotations.
	 *
	 * @param annotationType the annotation type
	 * @return the annotation, or {@code null} if no such annotation exists on this type descriptor
	 */
	@Nullable
	public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
		if (this.annotatedElement.isEmpty()) {
			// Shortcut: AnnotatedElementUtils would have to expect AnnotatedElement.getAnnotations()
			// to return a copy of the array, whereas we can do it more efficiently here.
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
	 * Is this type a {@link Collection} type?
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
	 * If this type is an array, returns the array's component type.
	 * If this type is a {@code Stream}, returns the stream's component type.
	 * If this type is a {@link Collection} and it is parameterized, returns the Collection's element type.
	 * If the Collection is not parameterized, returns {@code null} indicating the element type is not declared.
	 *
	 * @return the array component type or Collection element type, or {@code null} if this type is not
	 * an array type or a {@code java.util.Collection} or if its element type is not parameterized
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
	 * If this type is a {@link Collection} or an array, creates a element TypeDescriptor
	 * from the provided collection or array element.
	 * <p>Narrows the {@link #getElementTypeDescriptor() elementType} property to the class
	 * of the provided collection or array element. For example, if this describes a
	 * {@code java.util.List<java.lang.Number>} and the element argument is a
	 * {@code java.lang.Integer}, the returned TypeDescriptor will be {@code java.lang.Integer}.
	 * If this describes a {@code java.util.List<?>} and the element argument is a
	 * {@code java.lang.Integer}, the returned TypeDescriptor will be {@code java.lang.Integer}
	 * as well.
	 * <p>Annotation and nested type context will be preserved in the narrowed
	 * TypeDescriptor that is returned.
	 *
	 * @param element the collection or array element
	 * @return a element type descriptor, narrowed to the type of the provided element
	 * @see #getElementTypeDescriptor()
	 * @see #narrow(Object)
	 */
	@Nullable
	public TypeDescriptor elementTypeDescriptor(Object element) {
		return narrow(element, getElementTypeDescriptor());
	}

	/**
	 * Is this type a {@link Map} type?
	 */
	public boolean isMap() {
		return Map.class.isAssignableFrom(getType());
	}

	/**
	 * If this type is a {@link Map} and its key type is parameterized,
	 * returns the map's key type. If the Map's key type is not parameterized,
	 * returns {@code null} indicating the key type is not declared.
	 *
	 * @return the Map key type, or {@code null} if this type is a Map
	 * but its key type is not parameterized
	 * @throws IllegalStateException if this type is not a {@code java.util.Map}
	 */
	@Nullable
	public TypeDescriptor getMapKeyTypeDescriptor() {
		Assert.state(isMap(), "Not a [java.util.Map]");
		return getRelatedIfResolvable(this, getResolvableType().asMap().getGeneric(0));
	}

	/**
	 * If this type is a {@link Map}, creates a mapKey {@link TypeDescriptor}
	 * from the provided map key.
	 * <p>Narrows the {@link #getMapKeyTypeDescriptor() mapKeyType} property
	 * to the class of the provided map key. For example, if this describes a
	 * {@code java.util.Map<java.lang.Number, java.lang.String>} and the key
	 * argument is a {@code java.lang.Integer}, the returned TypeDescriptor will be
	 * {@code java.lang.Integer}. If this describes a {@code java.util.Map<?, ?>}
	 * and the key argument is a {@code java.lang.Integer}, the returned
	 * TypeDescriptor will be {@code java.lang.Integer} as well.
	 * <p>Annotation and nested type context will be preserved in the narrowed
	 * TypeDescriptor that is returned.
	 *
	 * @param mapKey the map key
	 * @return the map key type descriptor
	 * @throws IllegalStateException if this type is not a {@code java.util.Map}
	 * @see #narrow(Object)
	 */
	@Nullable
	public TypeDescriptor getMapKeyTypeDescriptor(Object mapKey) {
		return narrow(mapKey, getMapKeyTypeDescriptor());
	}

	/**
	 * If this type is a {@link Map} and its value type is parameterized,
	 * returns the map's value type.
	 * <p>If the Map's value type is not parameterized, returns {@code null}
	 * indicating the value type is not declared.
	 *
	 * @return the Map value type, or {@code null} if this type is a Map
	 * but its value type is not parameterized
	 * @throws IllegalStateException if this type is not a {@code java.util.Map}
	 */
	@Nullable
	public TypeDescriptor getMapValueTypeDescriptor() {
		Assert.state(isMap(), "Not a [java.util.Map]");
		return getRelatedIfResolvable(this, getResolvableType().asMap().getGeneric(1));
	}

	/**
	 * If this type is a {@link Map}, creates a mapValue {@link TypeDescriptor}
	 * from the provided map value.
	 * <p>Narrows the {@link #getMapValueTypeDescriptor() mapValueType} property
	 * to the class of the provided map value. For example, if this describes a
	 * {@code java.util.Map<java.lang.String, java.lang.Number>} and the value
	 * argument is a {@code java.lang.Integer}, the returned TypeDescriptor will be
	 * {@code java.lang.Integer}. If this describes a {@code java.util.Map<?, ?>}
	 * and the value argument is a {@code java.lang.Integer}, the returned
	 * TypeDescriptor will be {@code java.lang.Integer} as well.
	 * <p>Annotation and nested type context will be preserved in the narrowed
	 * TypeDescriptor that is returned.
	 *
	 * @param mapValue the map value
	 * @return the map value type descriptor
	 * @throws IllegalStateException if this type is not a {@code java.util.Map}
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
		// Annotation.equals is reflective and pretty slow, so let's check identity and proxy type first.
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
	 * Create a new type descriptor from a {@link java.util.Collection} type.
	 * <p>Useful for converting to typed Collections.
	 * <p>For example, a {@code List<String>} could be converted to a
	 * {@code List<EmailAddress>} by converting to a targetType built with this method.
	 * The method call to construct such a {@code TypeDescriptor} would look something
	 * like: {@code collection(List.class, TypeDescriptor.valueOf(EmailAddress.class));}
	 *
	 * @param collectionType        the collection type, which must implement {@link Collection}.
	 * @param elementTypeDescriptor a descriptor for the collection's element type,
	 *                              used to convert collection elements
	 * @return the collection type descriptor
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
	 * Create a new type descriptor from a {@link java.util.Map} type.
	 * <p>Useful for converting to typed Maps.
	 * <p>For example, a Map&lt;String, String&gt; could be converted to a Map&lt;Id, EmailAddress&gt;
	 * by converting to a targetType built with this method:
	 * The method call to construct such a TypeDescriptor would look something like:
	 * <pre class="code">
	 * map(Map.class, TypeDescriptor.valueOf(Id.class), TypeDescriptor.valueOf(EmailAddress.class));
	 * </pre>
	 *
	 * @param mapType             the map type, which must implement {@link Map}
	 * @param keyTypeDescriptor   a descriptor for the map's key type, used to convert map keys
	 * @param valueTypeDescriptor the map's value type, used to convert map values
	 * @return the map type descriptor
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
	 * Create a new type descriptor as an array of the specified type.
	 * <p>For example to create a {@code Map<String,String>[]} use:
	 * <pre class="code">
	 * TypeDescriptor.array(TypeDescriptor.map(Map.class, TypeDescriptor.value(String.class), TypeDescriptor.value(String.class)));
	 * </pre>
	 *
	 * @param elementTypeDescriptor the {@link TypeDescriptor} of the array element or {@code null}
	 * @return an array {@link TypeDescriptor} or {@code null} if {@code elementTypeDescriptor} is {@code null}
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
	 * Create a type descriptor for a nested type declared within the method parameter.
	 * <p>For example, if the methodParameter is a {@code List<String>} and the
	 * nesting level is 1, the nested type descriptor will be String.class.
	 * <p>If the methodParameter is a {@code List<List<String>>} and the nesting
	 * level is 2, the nested type descriptor will also be a String.class.
	 * <p>If the methodParameter is a {@code Map<Integer, String>} and the nesting
	 * level is 1, the nested type descriptor will be String, derived from the map value.
	 * <p>If the methodParameter is a {@code List<Map<Integer, String>>} and the
	 * nesting level is 2, the nested type descriptor will be String, derived from the map value.
	 * <p>Returns {@code null} if a nested type cannot be obtained because it was not declared.
	 * For example, if the method parameter is a {@code List<?>}, the nested type
	 * descriptor returned will be {@code null}.
	 *
	 * @param methodParameter the method parameter with a nestingLevel of 1
	 * @param nestingLevel    the nesting level of the collection/array element or
	 *                        map key/value declaration within the method parameter
	 * @return the nested type descriptor at the specified nesting level,
	 * or {@code null} if it could not be obtained
	 * @throws IllegalArgumentException if the nesting level of the input
	 *                                  {@link MethodParameter} argument is not 1, or if the types up to the
	 *                                  specified nesting level are not of collection, array, or map types
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
	 * Create a type descriptor for a nested type declared within the field.
	 * <p>For example, if the field is a {@code List<String>} and the nesting
	 * level is 1, the nested type descriptor will be {@code String.class}.
	 * <p>If the field is a {@code List<List<String>>} and the nesting level is
	 * 2, the nested type descriptor will also be a {@code String.class}.
	 * <p>If the field is a {@code Map<Integer, String>} and the nesting level
	 * is 1, the nested type descriptor will be String, derived from the map value.
	 * <p>If the field is a {@code List<Map<Integer, String>>} and the nesting
	 * level is 2, the nested type descriptor will be String, derived from the map value.
	 * <p>Returns {@code null} if a nested type cannot be obtained because it was not
	 * declared. For example, if the field is a {@code List<?>}, the nested type
	 * descriptor returned will be {@code null}.
	 *
	 * @param field        the field
	 * @param nestingLevel the nesting level of the collection/array element or
	 *                     map key/value declaration within the field
	 * @return the nested type descriptor at the specified nesting level,
	 * or {@code null} if it could not be obtained
	 * @throws IllegalArgumentException if the types up to the specified nesting
	 *                                  level are not of collection, array, or map types
	 */
	@Nullable
	public static TypeDescriptor nested(Field field, int nestingLevel) {
		return nested(new TypeDescriptor(field), nestingLevel);
	}

	/**
	 * Create a type descriptor for a nested type declared within the property.
	 * <p>For example, if the property is a {@code List<String>} and the nesting
	 * level is 1, the nested type descriptor will be {@code String.class}.
	 * <p>If the property is a {@code List<List<String>>} and the nesting level
	 * is 2, the nested type descriptor will also be a {@code String.class}.
	 * <p>If the property is a {@code Map<Integer, String>} and the nesting level
	 * is 1, the nested type descriptor will be String, derived from the map value.
	 * <p>If the property is a {@code List<Map<Integer, String>>} and the nesting
	 * level is 2, the nested type descriptor will be String, derived from the map value.
	 * <p>Returns {@code null} if a nested type cannot be obtained because it was not
	 * declared. For example, if the property is a {@code List<?>}, the nested type
	 * descriptor returned will be {@code null}.
	 *
	 * @param property     the property
	 * @param nestingLevel the nesting level of the collection/array element or
	 *                     map key/value declaration within the property
	 * @return the nested type descriptor at the specified nesting level, or
	 * {@code null} if it could not be obtained
	 * @throws IllegalArgumentException if the types up to the specified nesting
	 *                                  level are not of collection, array, or map types
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
				// Could be a collection type but we don't know about its element type,
				// so let's just assume there is an element type of type Object...
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
	 * Adapter class for exposing a {@code TypeDescriptor}'s annotations as an
	 * {@link AnnotatedElement}, in particular to {@link AnnotatedElementUtils}.
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
