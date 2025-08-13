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

package org.springframework.core.annotation;

import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.lang.Nullable;

import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * 从 {@link MergedAnnotations} 集合返回的单个合并注解。
 * 提供一个注解视图，其中属性值可能已从不同的源值“合并”而来。
 *
 * <p>可以使用各种 {@code get} 方法访问属性值。
 * 例如，要访问一个 {@code int} 属性，将使用 {@link #getInt(String)} 方法。
 *
 * <p>请注意，访问属性值时<b>不会</b>进行转换。
 * 例如，如果底层属性是 {@code int} 类型，则无法调用 {@link #getString(String)}。
 * 此规则的唯一例外是 {@code Class} 和 {@code Class[]} 值，它们可以分别作为
 * {@code String} 和 {@code String[]} 访问，以防止潜在的早期类初始化。
 *
 * <p>如有必要，可以将 {@code MergedAnnotation} {@linkplain #synthesize() 合成} 回
 * 实际的 {@link java.lang.annotation.Annotation}。
 *
 * @author Phillip Webb
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 5.2
 * @param <A> 注解类型
 * @see MergedAnnotations
 * @see MergedAnnotationPredicates
 */
public interface MergedAnnotation<A extends Annotation> {

	/**
	 * 只有一个元素的注解的属性名。
	 */
	String VALUE = "value";


	/**
	 * 获取实际注解类型的 {@code Class} 引用。
	 * @return 注解类型
	 */
	Class<A> getType();

	/**
	 * 确定注解是否存在于源上。在所使用的 {@link SearchStrategy} 上下文中，
	 * 考虑了 {@linkplain #isDirectlyPresent() 直接存在} 和
	 * {@linkplain #isMetaPresent() 元存在} 的注解。
	 * @return 如果注解存在，则为 {@code true}
	 */
	boolean isPresent();

	/**
	 * 确定注解是否直接存在于源上。
	 * <p>直接存在的注解是用户显式声明的注解，而不是
	 * {@linkplain #isMetaPresent() 元存在} 或 {@link Inherited @Inherited} 的注解。
	 * @return 如果注解直接存在，则为 {@code true}
	 */
	boolean isDirectlyPresent();

	/**
	 * 确定注解是否元存在于源上。
	 * <p>元存在注解是用户没有显式声明，但已在注解层次结构中的某个地方用作元注解的注解。
	 * @return 如果注解元存在，则为 {@code true}
	 */
	boolean isMetaPresent();

	/**
	 * 获取此注解作为元注解使用的距离。
	 * <p>直接声明的注解距离为 {@code 0}，元注解距离为 {@code 1}，
	 * 元注解上的元注解距离为 {@code 2}，依此类推。
	 * {@linkplain #missing() 缺失} 的注解将始终返回 {@code -1} 的距离。
	 * @return 注解距离，如果注解缺失则为 {@code -1}
	 */
	int getDistance();

	/**
	 * 获取包含此注解的聚合集合的索引。
	 * <p>可用于重新排序注解流，例如，为在超类或接口上声明的注解赋予更高的优先级。
	 * {@linkplain #missing() 缺失} 的注解将始终返回 {@code -1} 的聚合索引。
	 * @return 聚合索引（从 {@code 0} 开始），如果注解缺失则为 {@code -1}
	 */
	int getAggregateIndex();

	/**
	 * 获取最终声明根注解的源，如果源未知则为 {@code null}。
	 * <p>如果此合并注解是 {@link MergedAnnotations#from(AnnotatedElement) 从}
	 * {@link AnnotatedElement} 创建的，则此源将是相同类型的元素。
	 * 如果注解在不使用反射的情况下加载，则源可以是任何类型，但应具有合理的 {@code toString()}。
	 * 元注解将始终返回与 {@link #getRoot() 根} 相同的源。
	 * @return 源，或 {@code null}
	 */
	@Nullable
	Object getSource();

	/**
	 * 获取元注解的源，如果注解不 {@linkplain #isMetaPresent() 元存在} 则为 {@code null}。
	 * <p>元源是被此注解元注解的注解。
	 * @return 元注解源，或 {@code null}
	 * @see #getRoot()
	 */
	@Nullable
	MergedAnnotation<?> getMetaSource();

	/**
	 * 获取根注解，即直接在源上声明的 {@link #getDistance() 距离} 为 {@code 0} 的注解。
	 * @return 根注解
	 * @see #getMetaSource()
	 */
	MergedAnnotation<?> getRoot();

	/**
	 * 获取注解层次结构中从当前注解到 {@link #getRoot() 根} 的注解类型的完整列表。
	 * <p>提供了一种唯一标识合并注解实例的有用方法。
	 * @return 注解的元类型
	 * @see MergedAnnotationPredicates#unique(Function)
	 * @see #getRoot()
	 * @see #getMetaSource()
	 */
	List<Class<? extends Annotation>> getMetaTypes();


	/**
	 * 确定指定属性名与注解声明相比是否具有非默认值。
	 * @param attributeName 属性名
	 * @return 如果属性值与默认值不同，则为 {@code true}
	 */
	boolean hasNonDefaultValue(String attributeName);

	/**
	 * 确定指定属性名与注解声明相比是否具有默认值。
	 * @param attributeName 属性名
	 * @return 如果属性值与默认值相同，则为 {@code true}
	 */
	boolean hasDefaultValue(String attributeName) throws NoSuchElementException;

	/**
	 * 从注解中获取必需的字节属性值。
	 * @param attributeName 属性名
	 * @return 作为字节的值
	 * @throws NoSuchElementException 如果没有匹配的属性
	 */
	byte getByte(String attributeName) throws NoSuchElementException;

	/**
	 * 从注解中获取必需的字节数组属性值。
	 * @param attributeName 属性名
	 * @return 作为字节数组的值
	 * @throws NoSuchElementException 如果没有匹配的属性
	 */
	byte[] getByteArray(String attributeName) throws NoSuchElementException;

	/**
	 * 从注解中获取必需的布尔属性值。
	 * @param attributeName 属性名
	 * @return 作为布尔值的值
	 * @throws NoSuchElementException 如果没有匹配的属性
	 */
	boolean getBoolean(String attributeName) throws NoSuchElementException;

	/**
	 * 从注解中获取必需的布尔数组属性值。
	 * @param attributeName 属性名
	 * @return 作为布尔数组的值
	 * @throws NoSuchElementException 如果没有匹配的属性
	 */
	boolean[] getBooleanArray(String attributeName) throws NoSuchElementException;

	/**
	 * 从注解中获取必需的字符属性值。
	 * @param attributeName 属性名
	 * @return 作为字符的值
	 * @throws NoSuchElementException 如果没有匹配的属性
	 */
	char getChar(String attributeName) throws NoSuchElementException;

	/**
	 * 从注解中获取必需的字符数组属性值。
	 * @param attributeName 属性名
	 * @return 作为字符数组的值
	 * @throws NoSuchElementException 如果没有匹配的属性
	 */
	char[] getCharArray(String attributeName) throws NoSuchElementException;

	/**
	 * 从注解中获取必需的短整型属性值。
	 * @param attributeName 属性名
	 * @return 作为短整型的值
	 * @throws NoSuchElementException 如果没有匹配的属性
	 */
	short getShort(String attributeName) throws NoSuchElementException;

	/**
	 * 从注解中获取必需的短整型数组属性值。
	 * @param attributeName 属性名
	 * @return 作为短整型数组的值
	 * @throws NoSuchElementException 如果没有匹配的属性
	 */
	short[] getShortArray(String attributeName) throws NoSuchElementException;

	/**
	 * 从注解中获取必需的整型属性值。
	 * @param attributeName 属性名
	 * @return 作为整型的值
	 * @throws NoSuchElementException 如果没有匹配的属性
	 */
	int getInt(String attributeName) throws NoSuchElementException;

	/**
	 * 从注解中获取必需的整型数组属性值。
	 * @param attributeName 属性名
	 * @return 作为整型数组的值
	 * @throws NoSuchElementException 如果没有匹配的属性
	 */
	int[] getIntArray(String attributeName) throws NoSuchElementException;

	/**
	 * 从注解中获取必需的长整型属性值。
	 * @param attributeName 属性名
	 * @return 作为长整型的值
	 * @throws NoSuchElementException 如果没有匹配的属性
	 */
	long getLong(String attributeName) throws NoSuchElementException;

	/**
	 * 从注解中获取必需的长整型数组属性值。
	 * @param attributeName 属性名
	 * @return 作为长整型数组的值
	 * @throws NoSuchElementException 如果没有匹配的属性
	 */
	long[] getLongArray(String attributeName) throws NoSuchElementException;

	/**
	 * 从注解中获取必需的双精度浮点型属性值。
	 * @param attributeName 属性名
	 * @return 作为双精度浮点型的值
	 * @throws NoSuchElementException 如果没有匹配的属性
	 */
	double getDouble(String attributeName) throws NoSuchElementException;

	/**
	 * 从注解中获取必需的双精度浮点型数组属性值。
	 * @param attributeName 属性名
	 * @return 作为双精度浮点型数组的值
	 * @throws NoSuchElementException 如果没有匹配的属性
	 */
	double[] getDoubleArray(String attributeName) throws NoSuchElementException;

	/**
	 * 从注解中获取必需的浮点型属性值。
	 * @param attributeName 属性名
	 * @return 作为浮点型的值
	 * @throws NoSuchElementException 如果没有匹配的属性
	 */
	float getFloat(String attributeName) throws NoSuchElementException;

	/**
	 * 从注解中获取必需的浮点型数组属性值。
	 * @param attributeName 属性名
	 * @return 作为浮点型数组的值
	 * @throws NoSuchElementException 如果没有匹配的属性
	 */
	float[] getFloatArray(String attributeName) throws NoSuchElementException;

	/**
	 * 从注解中获取必需的字符串属性值。
	 * @param attributeName 属性名
	 * @return 作为字符串的值
	 * @throws NoSuchElementException 如果没有匹配的属性
	 */
	String getString(String attributeName) throws NoSuchElementException;

	/**
	 * 从注解中获取必需的字符串数组属性值。
	 * @param attributeName 属性名
	 * @return 作为字符串数组的值
	 * @throws NoSuchElementException 如果没有匹配的属性
	 */
	String[] getStringArray(String attributeName) throws NoSuchElementException;

	/**
	 * 从注解中获取必需的类属性值。
	 * @param attributeName 属性名
	 * @return 作为类的值
	 * @throws NoSuchElementException 如果没有匹配的属性
	 */
	Class<?> getClass(String attributeName) throws NoSuchElementException;

	/**
	 * 从注解中获取必需的类数组属性值。
	 * @param attributeName 属性名
	 * @return 作为类数组的值
	 * @throws NoSuchElementException 如果没有匹配的属性
	 */
	Class<?>[] getClassArray(String attributeName) throws NoSuchElementException;

	/**
	 * 从注解中获取必需的枚举属性值。
	 * @param attributeName 属性名
	 * @param type 枚举类型
	 * @return 作为枚举的值
	 * @throws NoSuchElementException 如果没有匹配的属性
	 */
	<E extends Enum<E>> E getEnum(String attributeName, Class<E> type) throws NoSuchElementException;

	/**
	 * 从注解中获取必需的枚举数组属性值。
	 * @param attributeName 属性名
	 * @param type 枚举类型
	 * @return 作为枚举数组的值
	 * @throws NoSuchElementException 如果没有匹配的属性
	 */
	<E extends Enum<E>> E[] getEnumArray(String attributeName, Class<E> type) throws NoSuchElementException;

	/**
	 * 从注解中获取必需的注解属性值。
	 * @param attributeName 属性名
	 * @param type 注解类型
	 * @return 作为 {@link MergedAnnotation} 的值
	 * @throws NoSuchElementException 如果没有匹配的属性
	 */
	<T extends Annotation> MergedAnnotation<T> getAnnotation(String attributeName, Class<T> type)
			throws NoSuchElementException;

	/**
	 * 从注解中获取必需的注解数组属性值。
	 * @param attributeName 属性名
	 * @param type 注解类型
	 * @return 作为 {@link MergedAnnotation} 数组的值
	 * @throws NoSuchElementException 如果没有匹配的属性
	 */
	<T extends Annotation> MergedAnnotation<T>[] getAnnotationArray(String attributeName, Class<T> type)
			throws NoSuchElementException;

	/**
	 * 从注解中获取可选的属性值。
	 * @param attributeName 属性名
	 * @return 可选值，如果没有匹配的属性则为 {@link Optional#empty()}
	 */
	Optional<Object> getValue(String attributeName);

	/**
	 * 从注解中获取可选的属性值。
	 * @param attributeName 属性名
	 * @param type 属性类型。必须与底层属性类型或 {@code Object.class} 兼容。
	 * @return 可选值，如果没有匹配的属性则为 {@link Optional#empty()}
	 */
	<T> Optional<T> getValue(String attributeName, Class<T> type);

	/**
	 * 获取注解声明中指定的默认属性值。
	 * @param attributeName 属性名
	 * @return 默认值的可选值，如果没有匹配的属性或没有定义默认值则为 {@link Optional#empty()}
	 */
	Optional<Object> getDefaultValue(String attributeName);

	/**
	 * 获取注解声明中指定的默认属性值。
	 * @param attributeName 属性名
	 * @param type 属性类型。必须与底层属性类型或 {@code Object.class} 兼容。
	 * @return 默认值的可选值，如果没有匹配的属性或没有定义默认值则为 {@link Optional#empty()}
	 */
	<T> Optional<T> getDefaultValue(String attributeName, Class<T> type);

	/**
	 * 创建一个注解的新视图，其中所有具有默认值的属性都被删除。
	 * @return 注解的过滤视图，不包含任何具有默认值的属性
	 * @see #filterAttributes(Predicate)
	 */
	MergedAnnotation<A> filterDefaultValues();

	/**
	 * 创建一个注解的新视图，其中只包含与给定谓词匹配的属性。
	 * @param predicate 用于过滤属性名的谓词
	 * @return 注解的过滤视图
	 * @see #filterDefaultValues()
	 * @see MergedAnnotationPredicates
	 */
	MergedAnnotation<A> filterAttributes(Predicate<String> predicate);

	/**
	 * 创建一个注解的新视图，该视图暴露非合并的属性值。
	 * <p>此视图中的方法将返回仅应用了别名镜像规则的属性值。
	 * 不会应用到 {@link #getMetaSource() 元源} 属性的别名。
	 * @return 注解的非合并视图
	 */
	MergedAnnotation<A> withNonMergedAttributes();

	/**
	 * 从此合并注解创建新的可变 {@link AnnotationAttributes} 实例。
	 * <p>{@link Adapt adaptations} 可用于更改添加值的方式。
	 * @param adaptations 应应用于注解值的适配器
	 * @return 包含属性和值的不可变映射
	 */
	AnnotationAttributes asAnnotationAttributes(Adapt... adaptations);

	/**
	 * 获取包含所有注解属性的不可变 {@link Map}。
	 * <p>{@link Adapt adaptations} 可用于更改添加值的方式。
	 * @param adaptations 应应用于注解值的适配器
	 * @return 包含属性和值的不可变映射
	 */
	Map<String, Object> asMap(Adapt... adaptations);

	/**
	 * 创建给定类型的新 {@link Map} 实例，包含所有注解属性。
	 * <p>{@link Adapt adaptations} 可用于更改添加值的方式。
	 * @param factory 映射工厂
	 * @param adaptations 应应用于注解值的适配器
	 * @return 包含属性和值的映射
	 */
	<T extends Map<String, Object>> T asMap(Function<MergedAnnotation<?>, T> factory, Adapt... adaptations);

	/**
	 * 创建此合并注解的类型安全合成版本，可直接在代码中使用。
	 * <p>结果是使用 JDK {@link Proxy} 合成的，因此首次调用时可能会产生计算成本。
	 * <p>如果此合并注解是 {@linkplain #from(Annotation) 从} 注解实例创建的，
	 * 如果该注解不可<em>合成</em>，则返回原始注解而不进行修改。
	 * 如果以下任一条件为真，则注解被认为是可合成的。
	 * <ul>
	 * <li>注解声明了带有 {@link AliasFor @AliasFor} 注解的属性。</li>
	 * <li>注解是组合注解，它依赖于元注解中基于约定的注解属性覆盖。</li>
	 * <li>注解声明了本身可合成的注解或注解数组属性。</li>
	 * </ul>
	 * @return 注解的合成版本或未修改的原始注解
	 * @throws NoSuchElementException 如果注解缺失
	 */
	A synthesize() throws NoSuchElementException;

	/**
	 * 根据条件谓词选择性地创建此注解的类型安全合成版本。
	 * <p>结果是使用 JDK {@link Proxy} 合成的，因此首次调用时可能会产生计算成本。
	 * <p>有关被认为是可合成的解释，请查阅 {@link #synthesize()} 的文档。
	 * @param condition 用于确定注解是否可以合成的测试
	 * @return 包含注解合成版本的可选对象，如果条件不匹配则为空可选对象
	 * @throws NoSuchElementException 如果注解缺失
	 * @see MergedAnnotationPredicates
	 */
	Optional<A> synthesize(Predicate<? super MergedAnnotation<A>> condition) throws NoSuchElementException;


	/**
	 * 创建表示缺失注解（即不存在的注解）的 {@link MergedAnnotation}。
	 * @return 表示缺失注解的实例
	 */
	static <A extends Annotation> MergedAnnotation<A> missing() {
		return MissingMergedAnnotation.getInstance();
	}

	/**
	 * 从指定注解创建新的 {@link MergedAnnotation} 实例。
	 * @param annotation 要包含的注解
	 * @return 包含注解的 {@link MergedAnnotation} 实例
	 */
	static <A extends Annotation> MergedAnnotation<A> from(A annotation) {
		return from(null, annotation);
	}

	/**
	 * 从指定注解创建新的 {@link MergedAnnotation} 实例。
	 * @param source 注解的源。此源仅用于信息和日志记录。它不需要
	 * <em>实际</em>包含指定的注解，也不会被搜索。
	 * @param annotation 要包含的注解
	 * @return 注解的 {@link MergedAnnotation} 实例
	 */
	static <A extends Annotation> MergedAnnotation<A> from(@Nullable Object source, A annotation) {
		return TypeMappedAnnotation.from(source, annotation);
	}

	/**
	 * 创建指定注解类型的新 {@link MergedAnnotation} 实例。
	 * 结果注解将不包含任何属性值，但仍可用于查询默认值。
	 * @param annotationType 注解类型
	 * @return 注解的 {@link MergedAnnotation} 实例
	 */
	static <A extends Annotation> MergedAnnotation<A> of(Class<A> annotationType) {
		return of(null, annotationType, null);
	}

	/**
	 * 使用映射提供的属性值创建指定注解类型的新 {@link MergedAnnotation} 实例。
	 * @param annotationType 注解类型
	 * @param attributes 注解属性，如果只应使用默认值则为 {@code null}
	 * @return 注解和属性的 {@link MergedAnnotation} 实例
	 * @see #of(AnnotatedElement, Class, Map)
	 */
	static <A extends Annotation> MergedAnnotation<A> of(
			Class<A> annotationType, @Nullable Map<String, ?> attributes) {

		return of(null, annotationType, attributes);
	}

	/**
	 * 使用映射提供的属性值创建指定注解类型的新 {@link MergedAnnotation} 实例。
	 * @param source 注解的源。此源仅用于信息和日志记录。它不需要
	 * <em>实际</em>包含指定的注解，也不会被搜索。
	 * @param annotationType 注解类型
	 * @param attributes 注解属性，如果只应使用默认值则为 {@code null}
	 * @return 注解和属性的 {@link MergedAnnotation} 实例
	 */
	static <A extends Annotation> MergedAnnotation<A> of(
			@Nullable AnnotatedElement source, Class<A> annotationType, @Nullable Map<String, ?> attributes) {

		return of(null, source, annotationType, attributes);
	}

	/**
	 * 使用映射提供的属性值创建指定注解类型的新 {@link MergedAnnotation} 实例。
	 * @param classLoader 用于解析类属性的类加载器
	 * @param source 注解的源。此源仅用于信息和日志记录。它不需要
	 * <em>实际</em>包含指定的注解，也不会被搜索。
	 * @param annotationType 注解类型
	 * @param attributes 注解属性，如果只应使用默认值则为 {@code null}
	 * @return 注解和属性的 {@link MergedAnnotation} 实例
	 */
	static <A extends Annotation> MergedAnnotation<A> of(
			@Nullable ClassLoader classLoader, @Nullable Object source,
			Class<A> annotationType, @Nullable Map<String, ?> attributes) {

		return TypeMappedAnnotation.of(classLoader, source, annotationType, attributes);
	}


	/**
	 * 适配器，可以在创建 {@linkplain MergedAnnotation#asMap(Adapt...) Maps} 或
	 * {@link MergedAnnotation#asAnnotationAttributes(Adapt...) AnnotationAttributes} 时应用于属性值。
	 */
	enum Adapt {

		/**
		 * 将类或类数组属性适配为字符串。
		 */
		CLASS_TO_STRING,

		/**
		 * 将嵌套注解或注解数组适配为映射，而不是合成这些值。
		 */
		ANNOTATION_TO_MAP;

		protected final boolean isIn(Adapt... adaptations) {
			for (Adapt candidate : adaptations) {
				if (candidate == this) {
					return true;
				}
			}
			return false;
		}

		/**
		 * 用于从一组布尔标志创建 {@link Adapt} 数组的工厂方法。
		 * @param classToString 如果包含 {@link Adapt#CLASS_TO_STRING}
		 * @param annotationsToMap 如果包含 {@link Adapt#ANNOTATION_TO_MAP}
		 * @return 新的 {@link Adapt} 数组
		 */
		public static Adapt[] values(boolean classToString, boolean annotationsToMap) {
			EnumSet<Adapt> result = EnumSet.noneOf(Adapt.class);
			addIfTrue(result, Adapt.CLASS_TO_STRING, classToString);
			addIfTrue(result, Adapt.ANNOTATION_TO_MAP, annotationsToMap);
			return result.toArray(new Adapt[0]);
		}

		private static <T> void addIfTrue(Set<T> result, T value, boolean test) {
			if (test) {
				result.add(value);
			}
		}
	}

}
