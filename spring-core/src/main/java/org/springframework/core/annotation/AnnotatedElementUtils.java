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

package org.springframework.core.annotation;

import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.annotation.MergedAnnotation.Adapt;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用于在 {@link AnnotatedElement AnnotatedElements} 上查找注解、元注解和可重复注解的通用工具方法。
 *
 * <p>{@code AnnotatedElementUtils} 定义了 Spring 元注解编程模型的公共 API，支持
 * <em>注解属性覆盖</em>。如果您不需要注解属性覆盖的支持，可以考虑使用 {@link AnnotationUtils}。
 *
 * <p>请注意，此类的功能并非由 JDK 自身的内省机制提供。
 *
 * <h3>注解属性覆盖</h3>
 * <p>对带有 <em>注解属性覆盖</em> 的 <em>复合注解</em> 中的元注解的支持，由所有
 * {@code getMergedAnnotationAttributes()}、{@code getMergedAnnotation()}、
 * {@code getAllMergedAnnotations()}、{@code getMergedRepeatableAnnotations()}、
 * {@code findMergedAnnotationAttributes()}、{@code findMergedAnnotation()}、
 * {@code findAllMergedAnnotations()} 和 {@code findMergedRepeatableAnnotations()}
 * 方法的变体提供。
 *
 * <h3>查找 (Find) 与 获取 (Get) 语义</h3>
 * <p>此类中方法使用的搜索算法遵循 <em>查找</em> 或 <em>获取</em> 语义。有关使用哪种搜索算法的详细信息，
 * 请查阅每个单独方法的 Javadoc。
 *
 * <p><strong>获取语义</strong> 仅限于搜索存在于 {@code AnnotatedElement} 上（即本地声明或
 * {@linkplain java.lang.annotation.Inherited 继承的}）或在 {@code AnnotatedElement}
 * <em>之上</em> 的注解层次结构中声明的注解。
 *
 * <p><strong>查找语义</strong> 更为详尽，提供 <em>获取语义</em> 并支持以下功能：
 *
 * <ul>
 * <li>如果被注解的元素是一个类，则搜索接口
 * <li>如果被注解的元素是一个类，则搜索父类
 * <li>如果被注解的元素是一个方法，则解析桥接方法
 * <li>如果被注解的元素是一个方法，则搜索接口中的方法
 * <li>如果被注解的元素是一个方法，则搜索父类中的方法
 * </ul>
 *
 * <h3>对 {@code @Inherited} 的支持</h3>
 * <p>遵循 <em>获取语义</em> 的方法将遵循 Java {@link java.lang.annotation.Inherited @Inherited}
 * 注解的约定，但本地声明的注解（包括自定义复合注解）将优先于继承的注解。
 * 相反，遵循 <em>查找语义</em> 的方法将完全忽略 {@code @Inherited} 的存在，
 * 因为 <em>查找</em> 搜索算法会手动遍历类型和方法层次结构，从而隐式支持注解继承，
 * 而无需 {@code @Inherited}。
 *
 * @author Phillip Webb
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 4.0
 * @see AliasFor
 * @see AnnotationAttributes
 * @see AnnotationUtils
 * @see BridgeMethodResolver
 */
public abstract class AnnotatedElementUtils {

	/**
	 * 为给定的注解构建一个适配的 {@link AnnotatedElement}，
	 * 通常用于 {@link AnnotatedElementUtils} 中的其他方法。
	 * @param annotations 要通过 {@code AnnotatedElement} 暴露的注解
	 * @since 4.3
	 */
	public static AnnotatedElement forAnnotations(Annotation... annotations) {
		return new AnnotatedElementForAnnotations(annotations);
	}

	/**
	 * 获取所提供的 {@link AnnotatedElement} 上（指定 {@code annotationType} 的）注解中
	 * <em>存在</em> 的所有元注解类型的完全限定类名。
	 * <p>此方法遵循 {@linkplain AnnotatedElementUtils 类级别 Javadoc} 中描述的
	 * <em>获取语义</em>。
	 * @param element 被注解的元素
	 * @param annotationType 要查找元注解的注解类型
	 * @return 注解中存在的所有元注解的名称，如果未找到则返回空集
	 * @since 4.2
	 * @see #getMetaAnnotationTypes(AnnotatedElement, String)
	 * @see #hasMetaAnnotationTypes
	 */
	public static Set<String> getMetaAnnotationTypes(AnnotatedElement element,
			Class<? extends Annotation> annotationType) {

		return getMetaAnnotationTypes(element, element.getAnnotation(annotationType));
	}

	/**
	 * 获取所提供的 {@link AnnotatedElement} 上（指定 {@code annotationName} 的）注解中
	 * <em>存在</em> 的所有元注解类型的完全限定类名。
	 * <p>此方法遵循 {@linkplain AnnotatedElementUtils 类级别 Javadoc} 中描述的
	 * <em>获取语义</em>。
	 * @param element 被注解的元素
	 * @param annotationName 要查找元注解的注解类型的完全限定类名
	 * @return 注解中存在的所有元注解的名称，如果未找到则返回空集
	 * @see #getMetaAnnotationTypes(AnnotatedElement, Class)
	 * @see #hasMetaAnnotationTypes
	 */
	public static Set<String> getMetaAnnotationTypes(AnnotatedElement element, String annotationName) {
		for (Annotation annotation : element.getAnnotations()) {
			if (annotation.annotationType().getName().equals(annotationName)) {
				return getMetaAnnotationTypes(element, annotation);
			}
		}
		return Collections.emptySet();
	}

	private static Set<String> getMetaAnnotationTypes(AnnotatedElement element, @Nullable Annotation annotation) {
		if (annotation == null) {
			return Collections.emptySet();
		}
		return getAnnotations(annotation.annotationType()).stream()
				.map(mergedAnnotation -> mergedAnnotation.getType().getName())
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	/**
	 * 判断所提供的 {@link AnnotatedElement} 是否带有被指定 {@code annotationType} 元注解标记的
	 * <em>复合注解</em>。
	 * <p>此方法遵循 {@linkplain AnnotatedElementUtils 类级别 Javadoc} 中描述的
	 * <em>获取语义</em>。
	 * @param element 被注解的元素
	 * @param annotationType 要查找的元注解类型
	 * @return 如果存在匹配的元注解，则返回 {@code true}
	 * @since 4.2.3
	 * @see #getMetaAnnotationTypes
	 */
	public static boolean hasMetaAnnotationTypes(AnnotatedElement element, Class<? extends Annotation> annotationType) {
		return getAnnotations(element).stream(annotationType).anyMatch(MergedAnnotation::isMetaPresent);
	}

	/**
	 * 判断所提供的 {@link AnnotatedElement} 是否带有被指定 {@code annotationName} 元注解标记的
	 * <em>复合注解</em>。
	 * <p>此方法遵循 {@linkplain AnnotatedElementUtils 类级别 Javadoc} 中描述的
	 * <em>获取语义</em>。
	 * @param element 被注解的元素
	 * @param annotationName 要查找的元注解类型的完全限定类名
	 * @return 如果存在匹配的元注解，则返回 {@code true}
	 * @see #getMetaAnnotationTypes
	 */
	public static boolean hasMetaAnnotationTypes(AnnotatedElement element, String annotationName) {
		return getAnnotations(element).stream(annotationName).anyMatch(MergedAnnotation::isMetaPresent);
	}

	/**
	 * 判断指定 {@code annotationType} 的注解是否在提供的 {@link AnnotatedElement} 上
	 * 或在指定元素 *之上* 的注解层级中 *存在*。
	 * <p>如果此方法返回 {@code true}，那么 {@link #getMergedAnnotationAttributes}
	 * 将返回一个非空值。
	 * <p>此方法遵循 {@linkplain AnnotatedElementUtils 类级别 Javadoc} 中描述的 *获取语义*。
	 * @param element 被注解的元素
	 * @param annotationType 要查找的注解类型
	 * @return 如果存在匹配的注解，则为 {@code true}
	 * @since 4.2.3
	 * @see #hasAnnotation(AnnotatedElement, Class)
	 */
	public static boolean isAnnotated(AnnotatedElement element, Class<? extends Annotation> annotationType) {
		// 快捷方式：直接存在于元素上，无需合并？
		if (AnnotationFilter.PLAIN.matches(annotationType) ||
				AnnotationsScanner.hasPlainJavaAnnotationsOnly(element)) {
			return element.isAnnotationPresent(annotationType);
		}
		// 穷尽式检索合并注解...
		return getAnnotations(element).isPresent(annotationType);
	}

	/**
	 * 判断指定 {@code annotationName} 的注解是否在提供的 {@link AnnotatedElement} 上
	 * 或在指定元素 *之上* 的注解层级中 *存在*。
	 * <p>如果此方法返回 {@code true}，那么 {@link #getMergedAnnotationAttributes}
	 * 将返回一个非空值。
	 * <p>此方法遵循 {@linkplain AnnotatedElementUtils 类级别 Javadoc} 中描述的 *获取语义*。
	 * @param element 被注解的元素
	 * @param annotationName 要查找的注解类型的完全限定类名
	 * @return 如果存在匹配的注解，则为 {@code true}
	 */
	public static boolean isAnnotated(AnnotatedElement element, String annotationName) {
		return getAnnotations(element).isPresent(annotationName);
	}

	/**
	 * 在提供的 {@code element} *之上* 的注解层级中获取指定 {@code annotationType} 的第一个注解，
	 * 并将该注解的属性与注解层级中较低层级的注解的 *匹配* 属性合并。
	 * <p>{@link AliasFor @AliasFor} 语义得到全面支持，无论是在单个注解内部还是在注解层级内部。
	 * <p>此方法委托给 {@link #getMergedAnnotationAttributes(AnnotatedElement, String)}。
	 * @param element 被注解的元素
	 * @param annotationType 要查找的注解类型
	 * @return 合并后的 {@code AnnotationAttributes}，如果未找到则返回 {@code null}
	 * @since 4.2
	 * @see #getMergedAnnotationAttributes(AnnotatedElement, String, boolean, boolean)
	 * @see #findMergedAnnotationAttributes(AnnotatedElement, String, boolean, boolean)
	 * @see #getMergedAnnotation(AnnotatedElement, Class)
	 * @see #findMergedAnnotation(AnnotatedElement, Class)
	 */
	@Nullable
	public static AnnotationAttributes getMergedAnnotationAttributes(
			AnnotatedElement element, Class<? extends Annotation> annotationType) {

		MergedAnnotation<?> mergedAnnotation = getAnnotations(element)
				.get(annotationType, null, MergedAnnotationSelectors.firstDirectlyDeclared());
		return getAnnotationAttributes(mergedAnnotation, false, false);
	}

	/**
	 * 在提供的 {@code element} *之上* 的注解层级中获取指定 {@code annotationName} 的第一个注解，
	 * 并将该注解的属性与注解层级中较低层级的注解的 *匹配* 属性合并。
	 * <p>{@link AliasFor @AliasFor} 语义得到全面支持，无论是在单个注解内部还是在注解层级内部。
	 * <p>此方法委托给 {@link #getMergedAnnotationAttributes(AnnotatedElement, String, boolean, boolean)}，
	 * 将 {@code classValuesAsString} 和 {@code nestedAnnotationsAsMap} 都设置为 {@code false}。
	 * @param element 被注解的元素
	 * @param annotationName 要查找的注解类型的完全限定类名
	 * @return 合并后的 {@code AnnotationAttributes}，如果未找到则返回 {@code null}
	 * @since 4.2
	 * @see #getMergedAnnotationAttributes(AnnotatedElement, String, boolean, boolean)
	 * @see #findMergedAnnotationAttributes(AnnotatedElement, String, boolean, boolean)
	 * @see #findMergedAnnotation(AnnotatedElement, Class)
	 * @see #getAllAnnotationAttributes(AnnotatedElement, String)
	 */
	@Nullable
	public static AnnotationAttributes getMergedAnnotationAttributes(AnnotatedElement element,
			String annotationName) {

		return getMergedAnnotationAttributes(element, annotationName, false, false);
	}

	/**
	 * 在提供的 {@code element} *之上* 的注解层级中获取指定 {@code annotationName} 的第一个注解，
	 * 并将该注解的属性与注解层级中较低层级的注解的 *匹配* 属性合并。
	 * <p>注解层级中较低层级的属性会覆盖较高层级中同名属性，并且 {@link AliasFor @AliasFor} 语义得到
	 * 全面支持，无论是在单个注解内部还是在注解层级内部。
	 * <p>与 {@link #getAllAnnotationAttributes} 不同，此方法使用的搜索算法
	 * 一旦找到指定 {@code annotationName} 的第一个注解，就会停止搜索注解层级。
	 * 因此，指定 {@code annotationName} 的其他注解将被忽略。
	 * <p>此方法遵循 {@linkplain AnnotatedElementUtils 类级别 Javadoc} 中描述的 *获取语义*。
	 * @param element 被注解的元素
	 * @param annotationName 要查找的注解类型的完全限定类名
	 * @param classValuesAsString 是否将 Class 引用转换为字符串，还是保留为 Class 引用
	 * @param nestedAnnotationsAsMap 是否将嵌套的 Annotation 实例转换为
	 * {@code AnnotationAttributes} map，还是保留为 Annotation 实例
	 * @return 合并后的 {@code AnnotationAttributes}，如果未找到则返回 {@code null}
	 * @since 4.2
	 * @see #findMergedAnnotation(AnnotatedElement, Class)
	 * @see #findMergedAnnotationAttributes(AnnotatedElement, String, boolean, boolean)
	 * @see #getAllAnnotationAttributes(AnnotatedElement, String, boolean, boolean)
	 */
	@Nullable
	public static AnnotationAttributes getMergedAnnotationAttributes(AnnotatedElement element,
			String annotationName, boolean classValuesAsString, boolean nestedAnnotationsAsMap) {

		MergedAnnotation<?> mergedAnnotation = getAnnotations(element)
				.get(annotationName, null, MergedAnnotationSelectors.firstDirectlyDeclared());
		return getAnnotationAttributes(mergedAnnotation, classValuesAsString, nestedAnnotationsAsMap);
	}

	/**
	 * 在提供的 {@code element} *之上* 的注解层级中获取指定 {@code annotationType} 的第一个注解，
	 * 将该注解的属性与注解层级中较低层级的注解的 *匹配* 属性合并，并将结果合成回指定 {@code annotationType} 的注解。
	 * <p>{@link AliasFor @AliasFor} 语义得到全面支持，无论是在单个注解内部还是在注解层级内部。
	 * @param element 被注解的元素
	 * @param annotationType 要查找的注解类型
	 * @return 合并后合成的 {@code Annotation}，如果未找到则返回 {@code null}
	 * @since 4.2
	 * @see #findMergedAnnotation(AnnotatedElement, Class)
	 */
	@Nullable
	public static <A extends Annotation> A getMergedAnnotation(AnnotatedElement element, Class<A> annotationType) {
		// 快捷方式：直接存在于元素上，无需合并？
		if (AnnotationFilter.PLAIN.matches(annotationType) ||
				AnnotationsScanner.hasPlainJavaAnnotationsOnly(element)) {
			return element.getDeclaredAnnotation(annotationType);
		}
		// 穷尽式检索合并注解...
		return getAnnotations(element)
				.get(annotationType, null, MergedAnnotationSelectors.firstDirectlyDeclared())
				.synthesize(MergedAnnotation::isPresent).orElse(null);
	}

	/**
	 * 在提供的 {@code element} *之上* 的注解层级中获取指定 {@code annotationType} 的 **所有** 注解；
	 * 对于找到的每个注解，将其属性与注解层级中较低层级的注解的 *匹配* 属性合并，并将结果合成回指定 {@code annotationType} 的注解。
	 * <p>{@link AliasFor @AliasFor} 语义得到全面支持，无论是在单个注解内部还是在注解层级内部。
	 * <p>此方法遵循 {@linkplain AnnotatedElementUtils 类级别 Javadoc} 中描述的 *获取语义*。
	 * @param element 被注解的元素（永不为 {@code null}）
	 * @param annotationType 要查找的注解类型（永不为 {@code null}）
	 * @return 找到的所有合并后合成的 {@code Annotations} 的集合，如果未找到则返回空集合
	 * @since 4.3
	 * @see #getMergedAnnotation(AnnotatedElement, Class)
	 * @see #getAllAnnotationAttributes(AnnotatedElement, String)
	 * @see #findAllMergedAnnotations(AnnotatedElement, Class)
	 */
	public static <A extends Annotation> Set<A> getAllMergedAnnotations(
			AnnotatedElement element, Class<A> annotationType) {

		return getAnnotations(element).stream(annotationType)
				.collect(MergedAnnotationCollectors.toAnnotationSet());
	}

	/**
	 * 在提供的 {@code element} *之上* 的注解层级中获取指定 {@code annotationTypes} 的 **所有** 注解；
	 * 对于找到的每个注解，将其属性与注解层级中较低层级的注解的 *匹配* 属性合并，并将结果合成回
	 * 相应 {@code annotationType} 的注解。
	 * <p>{@link AliasFor @AliasFor} 语义得到全面支持，无论是在单个注解内部还是在注解层级内部。
	 * <p>此方法遵循 {@linkplain AnnotatedElementUtils 类级别 Javadoc} 中描述的 *获取语义*。
	 * @param element 被注解的元素（永不为 {@code null}）
	 * @param annotationTypes 要查找的注解类型
	 * @return 找到的所有合并后合成的 {@code Annotations} 的集合，如果未找到则返回空集合
	 * @since 5.1
	 * @see #getAllMergedAnnotations(AnnotatedElement, Class)
	 */
	public static Set<Annotation> getAllMergedAnnotations(AnnotatedElement element,
			Set<Class<? extends Annotation>> annotationTypes) {

		return getAnnotations(element).stream()
				.filter(MergedAnnotationPredicates.typeIn(annotationTypes))
				.collect(MergedAnnotationCollectors.toAnnotationSet());
	}

	/**
	 * 在提供的 {@code element} *之上* 的注解层级中获取指定 {@code annotationType} 的所有
	 * *可重复注解*；对于找到的每个注解，将其属性与注解层级中较低层级的注解的 *匹配* 属性合并，
	 * 并将结果合成回指定 {@code annotationType} 的注解。
	 * <p>持有可重复注解的容器类型将通过 {@link java.lang.annotation.Repeatable} 查找。
	 * <p>{@link AliasFor @AliasFor} 语义得到全面支持，无论是在单个注解内部还是在注解层级内部。
	 * <p>此方法遵循 {@linkplain AnnotatedElementUtils 类级别 Javadoc} 中描述的 *获取语义*。
	 * @param element 被注解的元素（永不为 {@code null}）
	 * @param annotationType 要查找的注解类型（永不为 {@code null}）
	 * @return 找到的所有合并后的可重复 {@code Annotations} 的集合，如果未找到则返回空集合
	 * @throws IllegalArgumentException 如果 {@code element} 或 {@code annotationType}
	 * 是 {@code null}，或者如果容器类型无法解析
	 * @throws AnnotationConfigurationException 如果提供的 {@code containerType}
	 * 不是提供的 {@code annotationType} 的有效容器注解
	 * @since 4.3
	 * @see #getMergedAnnotation(AnnotatedElement, Class)
	 * @see #getAllMergedAnnotations(AnnotatedElement, Class)
	 * @see #getMergedRepeatableAnnotations(AnnotatedElement, Class, Class)
	 */
	public static <A extends Annotation> Set<A> getMergedRepeatableAnnotations(
			AnnotatedElement element, Class<A> annotationType) {

		return getMergedRepeatableAnnotations(element, annotationType, null);
	}

	/**
	 * 在提供的 {@code element} *之上* 的注解层级中获取指定 {@code annotationType} 的所有
	 * *可重复注解*；对于找到的每个注解，将其属性与注解层级中较低层级的注解的 *匹配* 属性合并，
	 * 并将结果合成回指定 {@code annotationType} 的注解。
	 * <p>{@link AliasFor @AliasFor} 语义得到全面支持，无论是在单个注解内部还是在注解层级内部。
	 * <p>此方法遵循 {@linkplain AnnotatedElementUtils 类级别 Javadoc} 中描述的 *获取语义*。
	 * @param element 被注解的元素（永不为 {@code null}）
	 * @param annotationType 要查找的注解类型（永不为 {@code null}）
	 * @param containerType 包含注解的容器类型；
	 * 如果容器类型应通过 {@link java.lang.annotation.Repeatable} 查找，则可能为 {@code null}
	 * @return 找到的所有合并后的可重复 {@code Annotations} 的集合，如果未找到则返回空集合
	 * @throws IllegalArgumentException 如果 {@code element} 或 {@code annotationType}
	 * 是 {@code null}，或者如果容器类型无法解析
	 * @throws AnnotationConfigurationException 如果提供的 {@code containerType}
	 * 不是提供的 {@code annotationType} 的有效容器注解
	 * @since 4.3
	 * @see #getMergedAnnotation(AnnotatedElement, Class)
	 * @see #getAllMergedAnnotations(AnnotatedElement, Class)
	 */
	public static <A extends Annotation> Set<A> getMergedRepeatableAnnotations(
			AnnotatedElement element, Class<A> annotationType,
			@Nullable Class<? extends Annotation> containerType) {

		return getRepeatableAnnotations(element, containerType, annotationType)
				.stream(annotationType)
				.collect(MergedAnnotationCollectors.toAnnotationSet());
	}

	/**
	 * 获取提供的 {@link AnnotatedElement} 之上注解层级中指定 {@code annotationName} 的 **所有** 注解的属性，
	 * 并将结果存储在 {@link MultiValueMap} 中。
	 * <p>注意：与 {@link #getMergedAnnotationAttributes(AnnotatedElement, String)} 不同，
	 * 此方法 *不* 支持属性覆盖。
	 * <p>此方法遵循 {@linkplain AnnotatedElementUtils 类级别 Javadoc} 中描述的 *获取语义*。
	 * @param element 被注解的元素
	 * @param annotationName 要查找的注解类型的完全限定类名
	 * @return 以属性名为键的 {@link MultiValueMap}，包含找到的所有注解的属性，如果未找到则返回 {@code null}
	 * @see #getAllAnnotationAttributes(AnnotatedElement, String, boolean, boolean)
	 */
	@Nullable
	public static MultiValueMap<String, Object> getAllAnnotationAttributes(
			AnnotatedElement element, String annotationName) {

		return getAllAnnotationAttributes(element, annotationName, false, false);
	}

	/**
	 * 获取提供的 {@link AnnotatedElement} 之上注解层级中指定 {@code annotationName} 的 **所有** 注解的属性，
	 * 并将结果存储在 {@link MultiValueMap} 中。
	 * <p>注意：与 {@link #getMergedAnnotationAttributes(AnnotatedElement, String)} 不同，
	 * 此方法 *不* 支持属性覆盖。
	 * <p>此方法遵循 {@linkplain AnnotatedElementUtils 类级别 Javadoc} 中描述的 *获取语义*。
	 * @param element 被注解的元素
	 * @param annotationName 要查找的注解类型的完全限定类名
	 * @param classValuesAsString 是否将 Class 引用转换为字符串，还是保留为 Class 引用
	 * @param nestedAnnotationsAsMap 是否将嵌套的 Annotation 实例转换为
	 * {@code AnnotationAttributes} map，还是保留为 Annotation 实例
	 * @return 以属性名为键的 {@link MultiValueMap}，包含找到的所有注解的属性，如果未找到则返回 {@code null}
	 */
	@Nullable
	public static MultiValueMap<String, Object> getAllAnnotationAttributes(AnnotatedElement element,
			String annotationName, final boolean classValuesAsString, final boolean nestedAnnotationsAsMap) {

		Adapt[] adaptations = Adapt.values(classValuesAsString, nestedAnnotationsAsMap);
		return getAnnotations(element).stream(annotationName)
				.filter(MergedAnnotationPredicates.unique(MergedAnnotation::getMetaTypes))
				.map(MergedAnnotation::withNonMergedAttributes)
				.collect(MergedAnnotationCollectors.toMultiValueMap(AnnotatedElementUtils::nullIfEmpty, adaptations));
	}

	/**
	 * 判断指定 {@code annotationType} 的注解是否在提供的 {@link AnnotatedElement} 上
	 * 或在指定元素 *之上* 的注解层级中 *可用*。
	 * <p>如果此方法返回 {@code true}，那么 {@link #findMergedAnnotationAttributes}
	 * 将返回一个非空值。
	 * <p>此方法遵循 {@linkplain AnnotatedElementUtils 类级别 Javadoc} 中描述的 *查找语义*。
	 * @param element 被注解的元素
	 * @param annotationType 要查找的注解类型
	 * @return 如果存在匹配的注解，则为 {@code true}
	 * @since 4.3
	 * @see #isAnnotated(AnnotatedElement, Class)
	 */
	public static boolean hasAnnotation(AnnotatedElement element, Class<? extends Annotation> annotationType) {
		// 快捷方式：直接存在于元素上，无需合并？
		if (AnnotationFilter.PLAIN.matches(annotationType) ||
				AnnotationsScanner.hasPlainJavaAnnotationsOnly(element)) {
			return element.isAnnotationPresent(annotationType);
		}
		// 穷尽式检索合并注解...
		return findAnnotations(element).isPresent(annotationType);
	}

	/**
	 * 在提供的 {@code element} *之上* 的注解层级中查找指定 {@code annotationType} 的第一个注解，
	 * 并将该注解的属性与注解层级中较低层级的注解的 *匹配* 属性合并。
	 * <p>注解层级中较低层级的属性会覆盖较高层级中同名属性，并且
	 * {@link AliasFor @AliasFor} 语义得到全面支持，无论是在单个注解内部还是在注解层级内部。
	 * <p>与 {@link #getAllAnnotationAttributes} 不同，此方法使用的搜索算法
	 * 一旦找到指定 {@code annotationType} 的第一个注解，就会停止搜索注解层级。
	 * 因此，指定 {@code annotationType} 的其他注解将被忽略。
	 * <p>此方法遵循 {@linkplain AnnotatedElementUtils 类级别 Javadoc} 中描述的 *查找语义*。
	 * @param element 被注解的元素
	 * @param annotationType 要查找的注解类型
	 * @param classValuesAsString 是否将 Class 引用转换为字符串，还是保留为 Class 引用
	 * @param nestedAnnotationsAsMap 是否将嵌套的 Annotation 实例转换为
	 * {@code AnnotationAttributes} map，还是保留为 Annotation 实例
	 * @return 合并后的 {@code AnnotationAttributes}，如果未找到则返回 {@code null}
	 * @since 4.2
	 * @see #findMergedAnnotation(AnnotatedElement, Class)
	 * @see #getMergedAnnotationAttributes(AnnotatedElement, String, boolean, boolean)
	 */
	@Nullable
	public static AnnotationAttributes findMergedAnnotationAttributes(AnnotatedElement element,
			Class<? extends Annotation> annotationType, boolean classValuesAsString, boolean nestedAnnotationsAsMap) {

		MergedAnnotation<?> mergedAnnotation = findAnnotations(element)
				.get(annotationType, null, MergedAnnotationSelectors.firstDirectlyDeclared());
		return getAnnotationAttributes(mergedAnnotation, classValuesAsString, nestedAnnotationsAsMap);
	}

	/**
	 * 在提供的 {@code element} *之上* 的注解层级中查找指定 {@code annotationName} 的第一个注解，
	 * 并将该注解的属性与注解层级中较低层级的注解的 *匹配* 属性合并。
	 * <p>注解层级中较低层级的属性会覆盖较高层级中同名属性，并且
	 * {@link AliasFor @AliasFor} 语义得到全面支持，无论是在单个注解内部还是在注解层级内部。
	 * <p>与 {@link #getAllAnnotationAttributes} 不同，此方法使用的搜索
	 * 算法一旦找到指定 {@code annotationName} 的第一个注解，就会停止搜索注解层级。
	 * 因此，指定 {@code annotationName} 的其他注解将被忽略。
	 * <p>此方法遵循 {@linkplain AnnotatedElementUtils 类级别 Javadoc} 中描述的 *查找语义*。
	 * @param element 被注解的元素
	 * @param annotationName 要查找的注解类型的完全限定类名
	 * @param classValuesAsString 是否将 Class 引用转换为字符串，还是保留为 Class 引用
	 * @param nestedAnnotationsAsMap 是否将嵌套的 Annotation 实例转换为
	 * {@code AnnotationAttributes} map，还是保留为 Annotation 实例
	 * @return 合并后的 {@code AnnotationAttributes}，如果未找到则返回 {@code null}
	 * @since 4.2
	 * @see #findMergedAnnotation(AnnotatedElement, Class)
	 * @see #getMergedAnnotationAttributes(AnnotatedElement, String, boolean, boolean)
	 */
	@Nullable
	public static AnnotationAttributes findMergedAnnotationAttributes(AnnotatedElement element,
			String annotationName, boolean classValuesAsString, boolean nestedAnnotationsAsMap) {

		MergedAnnotation<?> mergedAnnotation = findAnnotations(element)
				.get(annotationName, null, MergedAnnotationSelectors.firstDirectlyDeclared());
		return getAnnotationAttributes(mergedAnnotation, classValuesAsString, nestedAnnotationsAsMap);
	}

	/**
	 * 在提供的 {@code element} *之上* 的注解层级中查找指定 {@code annotationType} 的第一个注解，
	 * 将该注解的属性与注解层级中较低层级的注解的 *匹配* 属性合并，并将结果合成回指定 {@code annotationType} 的注解。
	 * <p>{@link AliasFor @AliasFor} 语义得到全面支持，无论是在单个注解内部还是在注解层级内部。
	 * <p>此方法遵循 {@linkplain AnnotatedElementUtils 类级别 Javadoc} 中描述的 *查找语义*。
	 * @param element 被注解的元素
	 * @param annotationType 要查找的注解类型
	 * @return 合并后合成的 {@code Annotation}，如果未找到则返回 {@code null}
	 * @since 4.2
	 * @see #findAllMergedAnnotations(AnnotatedElement, Class)
	 * @see #findMergedAnnotationAttributes(AnnotatedElement, String, boolean, boolean)
	 * @see #getMergedAnnotationAttributes(AnnotatedElement, Class)
	 */
	@Nullable
	public static <A extends Annotation> A findMergedAnnotation(AnnotatedElement element, Class<A> annotationType) {
		// 快捷方式：直接存在于元素上，无需合并？
		if (AnnotationFilter.PLAIN.matches(annotationType) ||
				AnnotationsScanner.hasPlainJavaAnnotationsOnly(element)) {
			return element.getDeclaredAnnotation(annotationType);
		}
		// 穷尽式检索合并注解...
		return findAnnotations(element)
				.get(annotationType, null, MergedAnnotationSelectors.firstDirectlyDeclared())
				.synthesize(MergedAnnotation::isPresent).orElse(null);
	}

	/**
	 * 在提供的 {@code element} *之上* 的注解层级中查找指定 {@code annotationType} 的 **所有** 注解；
	 * 对于找到的每个注解，将其属性与注解层级中较低层级的注解的 *匹配* 属性合并，并将结果合成回指定 {@code annotationType} 的注解。
	 * <p>{@link AliasFor @AliasFor} 语义得到全面支持，无论是在单个注解内部还是在注解层级内部。
	 * <p>此方法遵循 {@linkplain AnnotatedElementUtils 类级别 Javadoc} 中描述的 *查找语义*。
	 * @param element 被注解的元素（永不为 {@code null}）
	 * @param annotationType 要查找的注解类型（永不为 {@code null}）
	 * @return 找到的所有合并后合成的 {@code Annotations} 的集合，如果未找到则返回空集合
	 * @since 4.3
	 * @see #findMergedAnnotation(AnnotatedElement, Class)
	 * @see #getAllMergedAnnotations(AnnotatedElement, Class)
	 */
	public static <A extends Annotation> Set<A> findAllMergedAnnotations(AnnotatedElement element, Class<A> annotationType) {
		return findAnnotations(element).stream(annotationType)
				.sorted(highAggregateIndexesFirst())
				.collect(MergedAnnotationCollectors.toAnnotationSet());
	}

	/**
	 * 在提供的 {@code element} *之上* 的注解层级中查找指定 {@code annotationTypes} 的 **所有** 注解；
	 * 对于找到的每个注解，将其属性与注解层级中较低层级的注解的 *匹配* 属性合并，并将结果合成回
	 * 相应 {@code annotationType} 的注解。
	 * <p>{@link AliasFor @AliasFor} 语义得到全面支持，无论是在单个注解内部还是在注解层级内部。
	 * <p>此方法遵循 {@linkplain AnnotatedElementUtils 类级别 Javadoc} 中描述的 *查找语义*。
	 * @param element 被注解的元素（永不为 {@code null}）
	 * @param annotationTypes 要查找的注解类型
	 * @return 找到的所有合并后合成的 {@code Annotations} 的集合，如果未找到则返回空集合
	 * @since 5.1
	 * @see #findAllMergedAnnotations(AnnotatedElement, Class)
	 */
	public static Set<Annotation> findAllMergedAnnotations(AnnotatedElement element, Set<Class<? extends Annotation>> annotationTypes) {
		return findAnnotations(element).stream()
				.filter(MergedAnnotationPredicates.typeIn(annotationTypes))
				.sorted(highAggregateIndexesFirst())
				.collect(MergedAnnotationCollectors.toAnnotationSet());
	}

	/**
	 * 在提供的 {@code element} *之上* 的注解层级中查找指定 {@code annotationType} 的所有
	 * *可重复注解*；对于找到的每个注解，将其属性与注解层级中较低层级的注解的 *匹配* 属性进行合并，
	 * 并将结果合成回指定 {@code annotationType} 的注解。
	 * <p>持有可重复注解的容器类型将通过 {@link java.lang.annotation.Repeatable} 查找。
	 * <p>{@link AliasFor @AliasFor} 语义得到全面支持，无论是在单个注解内部还是在注解层级内部。
	 * <p>此方法遵循 {@linkplain AnnotatedElementUtils 类级别 Javadoc} 中描述的 *查找语义*。
	 * @param element 被注解的元素（永不为 {@code null}）
	 * @param annotationType 要查找的注解类型（永不为 {@code null}）
	 * @return 找到的所有合并后的可重复 {@code Annotations} 的集合，如果未找到则返回空集合
	 * @throws IllegalArgumentException 如果 {@code element} 或 {@code annotationType}
	 * 是 {@code null}，或者如果容器类型无法解析
	 * @since 4.3
	 * @see #findMergedAnnotation(AnnotatedElement, Class)
	 * @see #findAllMergedAnnotations(AnnotatedElement, Class)
	 * @see #findMergedRepeatableAnnotations(AnnotatedElement, Class, Class)
	 */
	public static <A extends Annotation> Set<A> findMergedRepeatableAnnotations(AnnotatedElement element,
			Class<A> annotationType) {

		return findMergedRepeatableAnnotations(element, annotationType, null);
	}

	/**
	 * 在提供的 {@code element} *之上* 的注解层级中查找指定 {@code annotationType} 的所有
	 * *可重复注解*；对于找到的每个注解，将其属性与注解层级中较低层级的注解的 *匹配* 属性进行合并，
	 * 并将结果合成回指定 {@code annotationType} 的注解。
	 * <p>{@link AliasFor @AliasFor} 语义得到全面支持，无论是在单个注解内部还是在注解层级内部。
	 * <p>此方法遵循 {@linkplain AnnotatedElementUtils 类级别 Javadoc} 中描述的 *查找语义*。
	 * @param element 被注解的元素（永不为 {@code null}）
	 * @param annotationType 要查找的注解类型（永不为 {@code null}）
	 * @param containerType 包含注解的容器类型；
	 * 如果容器类型应通过 {@link java.lang.annotation.Repeatable} 查找，则可能为 {@code null}
	 * @return 找到的所有合并后的可重复 {@code Annotations} 的集合，如果未找到则返回空集合
	 * @throws IllegalArgumentException 如果 {@code element} 或 {@code annotationType}
	 * 是 {@code null}，或者如果容器类型无法解析
	 * @throws AnnotationConfigurationException 如果提供的 {@code containerType}
	 * 不是提供的 {@code annotationType} 的有效容器注解
	 * @since 4.3
	 * @see #findMergedAnnotation(AnnotatedElement, Class)
	 * @see #findAllMergedAnnotations(AnnotatedElement, Class)
	 */
	public static <A extends Annotation> Set<A> findMergedRepeatableAnnotations(AnnotatedElement element,
			Class<A> annotationType, @Nullable Class<? extends Annotation> containerType) {

		return findRepeatableAnnotations(element, containerType, annotationType)
				.stream(annotationType)
				.sorted(highAggregateIndexesFirst())
				.collect(MergedAnnotationCollectors.toAnnotationSet());
	}

	private static MergedAnnotations getAnnotations(AnnotatedElement element) {
		return MergedAnnotations.from(element, SearchStrategy.INHERITED_ANNOTATIONS, RepeatableContainers.none());
	}

	private static MergedAnnotations getRepeatableAnnotations(AnnotatedElement element,
			@Nullable Class<? extends Annotation> containerType, Class<? extends Annotation> annotationType) {

		RepeatableContainers repeatableContainers = RepeatableContainers.of(annotationType, containerType);
		return MergedAnnotations.from(element, SearchStrategy.INHERITED_ANNOTATIONS, repeatableContainers);
	}

	private static MergedAnnotations findAnnotations(AnnotatedElement element) {
		return MergedAnnotations.from(element, SearchStrategy.TYPE_HIERARCHY, RepeatableContainers.none());
	}

	private static MergedAnnotations findRepeatableAnnotations(AnnotatedElement element,
			@Nullable Class<? extends Annotation> containerType, Class<? extends Annotation> annotationType) {

		RepeatableContainers repeatableContainers = RepeatableContainers.of(annotationType, containerType);
		return MergedAnnotations.from(element, SearchStrategy.TYPE_HIERARCHY, repeatableContainers);
	}

	@Nullable
	private static MultiValueMap<String, Object> nullIfEmpty(MultiValueMap<String, Object> map) {
		return (map.isEmpty() ? null : map);
	}

	private static <A extends Annotation> Comparator<MergedAnnotation<A>> highAggregateIndexesFirst() {
		return Comparator.<MergedAnnotation<A>> comparingInt(
				MergedAnnotation::getAggregateIndex).reversed();
	}

	@Nullable
	private static AnnotationAttributes getAnnotationAttributes(MergedAnnotation<?> annotation,
			boolean classValuesAsString, boolean nestedAnnotationsAsMap) {

		if (!annotation.isPresent()) {
			return null;
		}
		return annotation.asAnnotationAttributes(
				Adapt.values(classValuesAsString, nestedAnnotationsAsMap));
	}


	/**
	 * 适配后的 {@link AnnotatedElement}，用于保存特定注解。
	 */
	private static class AnnotatedElementForAnnotations implements AnnotatedElement {

		private final Annotation[] annotations;

		AnnotatedElementForAnnotations(Annotation... annotations) {
			this.annotations = annotations;
		}

		@Override
		@SuppressWarnings("unchecked")
		@Nullable
		public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
			for (Annotation annotation : this.annotations) {
				if (annotation.annotationType() == annotationClass) {
					return (T) annotation;
				}
			}
			return null;
		}

		@Override
		public Annotation[] getAnnotations() {
			return this.annotations.clone();
		}

		@Override
		public Annotation[] getDeclaredAnnotations() {
			return this.annotations.clone();
		}

	}

}
