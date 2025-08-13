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

import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.annotation.AnnotationTypeMapping.MirrorSets.MirrorSet;
import org.springframework.core.annotation.MergedAnnotation.Adapt;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;

/**
 * 用于处理注解、元注解、桥接方法（编译器为泛型声明生成的）以及
 * 超类方法（用于可选的 <em>注解继承</em>）的通用工具方法。
 *
 * <p>请注意，此类的大多数功能不是由 JDK 的自省机制本身提供的。
 *
 * <p>对于运行时保留的应用程序注解（例如用于事务控制、授权或服务暴露），
 * 一般规则是始终使用此类的查找方法（例如 {@link #findAnnotation(Method, Class)} 或
 * {@link #getAnnotation(Method, Class)}），而不是 JDK 中普通的注解查找方法。
 * 您仍然可以显式选择仅在给定类级别进行 <em>获取</em> 查找（{@link #getAnnotation(Method, Class)}）
 * 和在给定方法的整个继承层次结构中进行 <em>查找</em> 查找（{@link #findAnnotation(Method, Class)}）。
 *
 * <h3>术语</h3>
 * 术语 <em>直接存在</em>、<em>间接存在</em> 和 <em>存在</em> 的含义与
 * {@link AnnotatedElement}（在 Java 8 中）类级别 javadoc 中定义的含义相同。
 *
 * <p>如果注解在作为元素上 <em>存在</em> 的其他注解上被声明为元注解，则该注解在元素上是 <em>元存在</em> 的。
 * 如果注解 {@code A} 在另一个注解上 <em>直接存在</em> 或 <em>元存在</em>，则注解 {@code A} 在另一个注解上是 <em>元存在</em> 的。
 *
 * <h3>元注解支持</h3>
 * <p>此类中的大多数 {@code find*()} 方法和一些 {@code get*()} 方法都支持查找用作元注解的注解。
 * 有关详细信息，请查阅此类中每个方法的 javadoc。对于在 <em>组合注解</em> 中具有 <em>属性覆盖</em> 的元注解的精细支持，
 * 请考虑改用 {@link AnnotatedElementUtils} 中更具体的方法。
 *
 * <h3>属性别名</h3>
 * <p>此类中返回注解、注解数组或 {@link AnnotationAttributes} 的所有公共方法都透明地支持通过
 * {@link AliasFor @AliasFor} 配置的属性别名。有关详细信息，请查阅各种
 * {@code synthesizeAnnotation*(..)} 方法。
 *
 * <h3>搜索范围</h3>
 * <p>此类中方法使用的搜索算法一旦找到指定类型的第一个注解，就会停止搜索。
 * 因此，指定类型的其他注解将被静默忽略。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Mark Fisher
 * @author Chris Beams
 * @author Phillip Webb
 * @author Oleg Zhurakousky
 * @since 2.0
 * @see AliasFor
 * @see AnnotationAttributes
 * @see AnnotatedElementUtils
 * @see BridgeMethodResolver
 * @see java.lang.reflect.AnnotatedElement#getAnnotations()
 * @see java.lang.reflect.AnnotatedElement#getAnnotation(Class)
 * @see java.lang.reflect.AnnotatedElement#getDeclaredAnnotations()
 */
public abstract class AnnotationUtils {

	/**
	 * 具有单个元素的批注的属性名称。
	 */
	public static final String VALUE = MergedAnnotation.VALUE;

	private static final AnnotationFilter JAVA_LANG_ANNOTATION_FILTER =
			AnnotationFilter.packages("java.lang.annotation");

	private static final Map<Class<? extends Annotation>, Map<String, DefaultValueHolder>> defaultValuesCache =
			new ConcurrentReferenceHashMap<>();


	/**
	 * 确定给定类是否是承载指定注解之一（在类型、方法或字段级别）的候选。
	 * @param clazz 要自省的类
	 * @param annotationTypes 可搜索的注解类型
	 * @return 如果已知该类在任何级别都没有此类注解，则返回 {@code false}；
	 * 否则返回 {@code true}。如果此处返回 {@code true}，调用者通常会执行完整的方法/字段自省。
	 * @since 5.2
	 * @see #isCandidateClass(Class, Class)
	 * @see #isCandidateClass(Class, String)
	 */
	public static boolean isCandidateClass(Class<?> clazz, Collection<Class<? extends Annotation>> annotationTypes) {
		for (Class<? extends Annotation> annotationType : annotationTypes) {
			if (isCandidateClass(clazz, annotationType)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 确定给定类是否是承载指定注解（在类型、方法或字段级别）的候选。
	 * @param clazz 要自省的类
	 * @param annotationType 可搜索的注解类型
	 * @return 如果已知该类在任何级别都没有此类注解，则返回 {@code false}；
	 * 否则返回 {@code true}。如果此处返回 {@code true}，调用者通常会执行完整的方法/字段自省。
	 * @since 5.2
	 * @see #isCandidateClass(Class, String)
	 */
	public static boolean isCandidateClass(Class<?> clazz, Class<? extends Annotation> annotationType) {
		return isCandidateClass(clazz, annotationType.getName());
	}

	/**
	 * 确定给定类是否是承载指定注解（在类型、方法或字段级别）的候选。
	 * @param clazz 要自省的类
	 * @param annotationName 可搜索注解类型的完全限定名
	 * @return 如果已知该类在任何级别都没有此类注解，则返回 {@code false}；
	 * 否则返回 {@code true}。如果此处返回 {@code true}，调用者通常会执行完整的方法/字段自省。
	 * @since 5.2
	 * @see #isCandidateClass(Class, Class)
	 */
	public static boolean isCandidateClass(Class<?> clazz, String annotationName) {
		if (annotationName.startsWith("java.")) {
			return true;
		}
		if (AnnotationsScanner.hasPlainJavaAnnotationsOnly(clazz)) {
			return false;
		}
		return true;
	}

	/**
	 * 从提供的注解中获取一个 {@link Annotation} 类型的 {@code annotationType} 注解：
	 * 它可以是给定注解本身，也可以是其直接的元注解。
	 * <p>请注意，此方法仅支持单层元注解。
	 * 若要支持任意层级的元注解，请改用 {@code find*()} 方法之一。
	 * @param annotation 要检查的注解
	 * @param annotationType 要查找的注解类型，包括本地和作为元注解的类型
	 * @return 第一个匹配的注解，如果未找到则返回 {@code null}
	 * @since 4.0
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	public static <A extends Annotation> A getAnnotation(Annotation annotation, Class<A> annotationType) {
		// 快捷方式：直接存在于元素上，无需合并？
		if (annotationType.isInstance(annotation)) {
			return synthesizeAnnotation((A) annotation, annotationType);
		}
		// 快捷方式：在普通 Java 类和核心 Spring 类型上找不到可搜索的注解...
		if (AnnotationsScanner.hasPlainJavaAnnotationsOnly(annotation)) {
			return null;
		}
		// 详尽检索合并的注解...
		return MergedAnnotations.from(annotation, new Annotation[] {annotation}, RepeatableContainers.none())
				.get(annotationType).withNonMergedAttributes()
				.synthesize(AnnotationUtils::isSingleLevelPresent).orElse(null);
	}

	/**
	 * 从提供的 {@link AnnotatedElement} 中获取一个 {@link Annotation} 类型的 {@code annotationType} 注解，
	 * 其中该注解在 {@code AnnotatedElement} 上是 <em>存在</em> 或 <em>元存在</em> 的。
	 * <p>请注意，此方法仅支持单层元注解。
	 * 若要支持任意层级的元注解，请改用 {@link #findAnnotation(AnnotatedElement, Class)}。
	 * @param annotatedElement 要从中获取注解的 {@code AnnotatedElement}
	 * @param annotationType 要查找的注解类型，包括本地和作为元注解的类型
	 * @return 第一个匹配的注解，如果未找到则返回 {@code null}
	 * @since 3.1
	 */
	@Nullable
	public static <A extends Annotation> A getAnnotation(AnnotatedElement annotatedElement, Class<A> annotationType) {
		// 快捷方式：直接存在于元素上，无需合并？
		if (AnnotationFilter.PLAIN.matches(annotationType) ||
				AnnotationsScanner.hasPlainJavaAnnotationsOnly(annotatedElement)) {
			return annotatedElement.getAnnotation(annotationType);
		}
		// 详尽检索合并的注解...
		return MergedAnnotations.from(annotatedElement, SearchStrategy.INHERITED_ANNOTATIONS, RepeatableContainers.none())
				.get(annotationType).withNonMergedAttributes()
				.synthesize(AnnotationUtils::isSingleLevelPresent).orElse(null);
	}

	private static <A extends Annotation> boolean isSingleLevelPresent(MergedAnnotation<A> mergedAnnotation) {
		int distance = mergedAnnotation.getDistance();
		return (distance == 0 || distance == 1);
	}

	/**
	 * 从提供的 {@link Method} 中获取一个 {@link Annotation} 类型的 {@code annotationType} 注解，
	 * 其中该注解在方法上是 <em>存在</em> 或 <em>元存在</em> 的。
	 * <p>正确处理编译器生成的桥接 {@link Method 方法}。
	 * <p>请注意，此方法仅支持单层元注解。
	 * 若要支持任意层级的元注解，请改用 {@link #findAnnotation(Method, Class)}。
	 * @param method 要查找注解的方法
	 * @param annotationType 要查找的注解类型
	 * @return 第一个匹配的注解，如果未找到则返回 {@code null}
	 * @see org.springframework.core.BridgeMethodResolver#findBridgedMethod(Method)
	 * @see #getAnnotation(AnnotatedElement, Class)
	 */
	@Nullable
	public static <A extends Annotation> A getAnnotation(Method method, Class<A> annotationType) {
		Method resolvedMethod = BridgeMethodResolver.findBridgedMethod(method);
		return getAnnotation((AnnotatedElement) resolvedMethod, annotationType);
	}

	/**
	 * 获取在提供的 {@link AnnotatedElement} 上 <em>存在</em> 的所有 {@link Annotation 注解}。
	 * <p>不会搜索元注解。
	 * @param annotatedElement 要从中检索注解的方法、构造函数或字段
	 * @return 找到的注解，一个空数组，如果不可解析则返回 {@code null}
	 * （例如，因为注解属性中的嵌套 Class 值在运行时无法解析）
	 * @since 4.0.8
	 * @see AnnotatedElement#getAnnotations()
	 * @deprecated 自 5.2 版本起已弃用，因为它已被 {@link MergedAnnotations} API 取代
	 */
	@Deprecated
	@Nullable
	public static Annotation[] getAnnotations(AnnotatedElement annotatedElement) {
		try {
			return synthesizeAnnotationArray(annotatedElement.getAnnotations(), annotatedElement);
		}
		catch (Throwable ex) {
			handleIntrospectionFailure(annotatedElement, ex);
			return null;
		}
	}

	/**
	 * 获取在提供的 {@link Method} 上 <em>存在</em> 的所有 {@link Annotation 注解}。
	 * <p>正确处理编译器生成的桥接 {@link Method 方法}。
	 * <p>不会搜索元注解。
	 * @param method 要从中检索注解的方法
	 * @return 找到的注解，一个空数组，如果不可解析则返回 {@code null}
	 * （例如，因为注解属性中的嵌套 Class 值在运行时无法解析）
	 * @see org.springframework.core.BridgeMethodResolver#findBridgedMethod(Method)
	 * @see AnnotatedElement#getAnnotations()
	 * @deprecated 自 5.2 版本起已弃用，因为它已被 {@link MergedAnnotations} API 取代
	 */
	@Deprecated
	@Nullable
	public static Annotation[] getAnnotations(Method method) {
		try {
			return synthesizeAnnotationArray(BridgeMethodResolver.findBridgedMethod(method).getAnnotations(), method);
		}
		catch (Throwable ex) {
			handleIntrospectionFailure(method, ex);
			return null;
		}
	}

	/**
	 * 从提供的 {@link AnnotatedElement} 中获取 {@code annotationType} 的
	 * <em>可重复</em> {@linkplain Annotation 注解}，其中此类注解在元素上是
	 * <em>存在</em>、<em>间接存在</em> 或 <em>元存在</em> 的。
	 * <p>此方法模拟 Java 8 的
	 * {@link java.lang.reflect.AnnotatedElement#getAnnotationsByType(Class)} 功能，
	 * 支持自动检测通过 @{@link java.lang.annotation.Repeatable} 声明的
	 * <em>容器注解</em>（在 Java 8 或更高版本上运行时），并附加元注解支持。
	 * <p>处理单个注解和嵌套在 <em>容器注解</em> 中的注解。
	 * <p>如果提供的元素是 {@link Method}，则正确处理编译器生成的
	 * <em>桥接方法</em>。
	 * <p>如果注解在提供的元素上不 <em>存在</em>，则会搜索元注解。
	 * @param annotatedElement 要查找注解的元素
	 * @param annotationType 要查找的注解类型
	 * @return 找到的注解或一个空集（从不为 {@code null}）
	 * @since 4.2
	 * @see #getRepeatableAnnotations(AnnotatedElement, Class, Class)
	 * @see #getDeclaredRepeatableAnnotations(AnnotatedElement, Class, Class)
	 * @see AnnotatedElementUtils#getMergedRepeatableAnnotations(AnnotatedElement, Class)
	 * @see org.springframework.core.BridgeMethodResolver#findBridgedMethod
	 * @see java.lang.annotation.Repeatable
	 * @see java.lang.reflect.AnnotatedElement#getAnnotationsByType
	 * @deprecated 自 5.2 版本起已弃用，因为它已被 {@link MergedAnnotations} API 取代
	 */
	@Deprecated
	public static <A extends Annotation> Set<A> getRepeatableAnnotations(AnnotatedElement annotatedElement,
			Class<A> annotationType) {

		return getRepeatableAnnotations(annotatedElement, annotationType, null);
	}

	/**
	 * 从提供的 {@link AnnotatedElement} 中获取 {@code annotationType} 的
	 * <em>可重复</em> {@linkplain Annotation 注解}，其中此类注解在元素上是
	 * <em>存在</em>、<em>间接存在</em> 或 <em>元存在</em> 的。
	 * <p>此方法模拟 Java 8 的
	 * {@link java.lang.reflect.AnnotatedElement#getAnnotationsByType(Class)} 功能，
	 * 并附加元注解支持。
	 * <p>处理单个注解和嵌套在 <em>容器注解</em> 中的注解。
	 * <p>如果提供的元素是 {@link Method}，则正确处理编译器生成的
	 * <em>桥接方法</em>。
	 * <p>如果注解在提供的元素上不 <em>存在</em>，则会搜索元注解。
	 * @param annotatedElement 要查找注解的元素
	 * @param annotationType 要查找的注解类型
	 * @param containerAnnotationType 包含注解的容器类型；如果不支持容器，
	 * 或在 Java 8 或更高版本上运行时应通过 @{@link java.lang.annotation.Repeatable} 查找，则可能为 {@code null}
	 * @return 找到的注解或一个空集（从不为 {@code null}）
	 * @since 4.2
	 * @see #getRepeatableAnnotations(AnnotatedElement, Class)
	 * @see #getDeclaredRepeatableAnnotations(AnnotatedElement, Class)
	 * @see #getDeclaredRepeatableAnnotations(AnnotatedElement, Class, Class)
	 * @see AnnotatedElementUtils#getMergedRepeatableAnnotations(AnnotatedElement, Class, Class)
	 * @see org.springframework.core.BridgeMethodResolver#findBridgedMethod
	 * @see java.lang.annotation.Repeatable
	 * @see java.lang.reflect.AnnotatedElement#getAnnotationsByType
	 * @deprecated 自 5.2 版本起已弃用，因为它已被 {@link MergedAnnotations} API 取代
	 */
	@Deprecated
	public static <A extends Annotation> Set<A> getRepeatableAnnotations(AnnotatedElement annotatedElement,
			Class<A> annotationType, @Nullable Class<? extends Annotation> containerAnnotationType) {

		RepeatableContainers repeatableContainers = (containerAnnotationType != null ?
				RepeatableContainers.of(annotationType, containerAnnotationType) :
				RepeatableContainers.standardRepeatables());

		return MergedAnnotations.from(annotatedElement, SearchStrategy.SUPERCLASS, repeatableContainers)
				.stream(annotationType)
				.filter(MergedAnnotationPredicates.firstRunOf(MergedAnnotation::getAggregateIndex))
				.map(MergedAnnotation::withNonMergedAttributes)
				.collect(MergedAnnotationCollectors.toAnnotationSet());
	}

	/**
	 * 从提供的 {@link AnnotatedElement} 中获取 {@code annotationType} 的已声明的
	 * <em>可重复</em> {@linkplain Annotation 注解}，其中此类注解在元素上是
	 * <em>直接存在</em>、<em>间接存在</em> 或 <em>元存在</em> 的。
	 * <p>此方法模拟 Java 8 的
	 * {@link java.lang.reflect.AnnotatedElement#getDeclaredAnnotationsByType(Class)} 功能，
	 * 支持自动检测通过 @{@link java.lang.annotation.Repeatable} 声明的
	 * <em>容器注解</em>（在 Java 8 或更高版本上运行时），并附加元注解支持。
	 * <p>处理单个注解和嵌套在 <em>容器注解</em> 中的注解。
	 * <p>如果提供的元素是 {@link Method}，则正确处理编译器生成的
	 * <em>桥接方法</em>。
	 * <p>如果注解在提供的元素上不 <em>存在</em>，则会搜索元注解。
	 * @param annotatedElement 要查找注解的元素
	 * @param annotationType 要查找的注解类型
	 * @return 找到的注解或一个空集（从不为 {@code null}）
	 * @since 4.2
	 * @see #getRepeatableAnnotations(AnnotatedElement, Class)
	 * @see #getRepeatableAnnotations(AnnotatedElement, Class, Class)
	 * @see #getDeclaredRepeatableAnnotations(AnnotatedElement, Class, Class)
	 * @see AnnotatedElementUtils#getMergedRepeatableAnnotations(AnnotatedElement, Class)
	 * @see org.springframework.core.BridgeMethodResolver#findBridgedMethod
	 * @see java.lang.annotation.Repeatable
	 * @see java.lang.reflect.AnnotatedElement#getDeclaredAnnotationsByType
	 * @deprecated 自 5.2 版本起已弃用，因为它已被 {@link MergedAnnotations} API 取代
	 */
	@Deprecated
	public static <A extends Annotation> Set<A> getDeclaredRepeatableAnnotations(AnnotatedElement annotatedElement,
			Class<A> annotationType) {

		return getDeclaredRepeatableAnnotations(annotatedElement, annotationType, null);
	}

	/**
	 * 从提供的 {@link AnnotatedElement} 中获取 {@code annotationType} 的已声明的
	 * <em>可重复</em> {@linkplain Annotation 注解}，其中此类注解在元素上是
	 * <em>直接存在</em>、<em>间接存在</em> 或 <em>元存在</em> 的。
	 * <p>此方法模拟 Java 8 的
	 * {@link java.lang.reflect.AnnotatedElement#getDeclaredAnnotationsByType(Class)} 功能，
	 * 并附加元注解支持。
	 * <p>处理单个注解和嵌套在 <em>容器注解</em> 中的注解。
	 * <p>如果提供的元素是 {@link Method}，则正确处理编译器生成的
	 * <em>桥接方法</em>。
	 * <p>如果注解在提供的元素上不 <em>存在</em>，则会搜索元注解。
	 * @param annotatedElement 要查找注解的元素
	 * @param annotationType 要查找的注解类型
	 * @param containerAnnotationType 包含注解的容器类型；如果不支持容器，
	 * 或在 Java 8 或更高版本上运行时应通过 @{@link java.lang.annotation.Repeatable} 查找，则可能为 {@code null}
	 * @return 找到的注解或一个空集（从不为 {@code null}）
	 * @since 4.2
	 * @see #getRepeatableAnnotations(AnnotatedElement, Class)
	 * @see #getRepeatableAnnotations(AnnotatedElement, Class, Class)
	 * @see #getDeclaredRepeatableAnnotations(AnnotatedElement, Class)
	 * @see AnnotatedElementUtils#getMergedRepeatableAnnotations(AnnotatedElement, Class, Class)
	 * @see org.springframework.core.BridgeMethodResolver#findBridgedMethod
	 * @see java.lang.annotation.Repeatable
	 * @see java.lang.reflect.AnnotatedElement#getDeclaredAnnotationsByType
	 * @deprecated 自 5.2 版本起已弃用，因为它已被 {@link MergedAnnotations} API 取代
	 */
	@Deprecated
	public static <A extends Annotation> Set<A> getDeclaredRepeatableAnnotations(AnnotatedElement annotatedElement,
			Class<A> annotationType, @Nullable Class<? extends Annotation> containerAnnotationType) {

		RepeatableContainers repeatableContainers = containerAnnotationType != null ?
				RepeatableContainers.of(annotationType, containerAnnotationType) :
				RepeatableContainers.standardRepeatables();

		return MergedAnnotations.from(annotatedElement, SearchStrategy.DIRECT, repeatableContainers)
				.stream(annotationType)
				.map(MergedAnnotation::withNonMergedAttributes)
				.collect(MergedAnnotationCollectors.toAnnotationSet());
	}

	/**
	 * 在提供的 {@link AnnotatedElement} 上查找单个 {@code annotationType} 类型的
	 * {@link Annotation 注解}。
	 * <p>如果注解未 <em>直接存在</em> 于提供的元素上，则会搜索元注解。
	 * <p><strong>警告</strong>：此方法对带注解的元素进行泛型操作。
	 * 换句话说，此方法不执行针对类或方法的专门搜索算法。如果您需要
	 * {@link #findAnnotation(Class, Class)} 或 {@link #findAnnotation(Method, Class)}
	 * 更具体的语义，请改用这些方法之一。
	 * @param annotatedElement 要在其上查找注解的 {@code AnnotatedElement}
	 * @param annotationType 要查找的注解类型，包括本地和作为元注解的类型
	 * @return 第一个匹配的注解，如果未找到则返回 {@code null}
	 * @since 4.2
	 */
	@Nullable
	public static <A extends Annotation> A findAnnotation(
			AnnotatedElement annotatedElement, @Nullable Class<A> annotationType) {

		if (annotationType == null) {
			return null;
		}

		// 快捷方式：直接存在于元素上，无需合并？
		if (AnnotationFilter.PLAIN.matches(annotationType) ||
				AnnotationsScanner.hasPlainJavaAnnotationsOnly(annotatedElement)) {
			return annotatedElement.getDeclaredAnnotation(annotationType);
		}

		// 详尽检索合并的注解...
		return MergedAnnotations.from(annotatedElement, SearchStrategy.INHERITED_ANNOTATIONS, RepeatableContainers.none())
				.get(annotationType).withNonMergedAttributes()
				.synthesize(MergedAnnotation::isPresent).orElse(null);
	}

	/**
	 * 在提供的 {@link Method} 上查找单个 {@code annotationType} 类型的
	 * {@link Annotation 注解}，如果注解未 <em>直接存在</em> 于给定方法本身，
	 * 则遍历其超类方法（即来自超类和接口）。
	 * <p>正确处理编译器生成的桥接 {@link Method 方法}。
	 * <p>如果注解未 <em>直接存在</em> 于方法上，则会搜索元注解。
	 * <p>方法上的注解默认不继承，因此我们需要显式处理。
	 * @param method 要查找注解的方法
	 * @param annotationType 要查找的注解类型
	 * @return 第一个匹配的注解，如果未找到则返回 {@code null}
	 * @see #getAnnotation(Method, Class)
	 */
	@Nullable
	public static <A extends Annotation> A findAnnotation(Method method, @Nullable Class<A> annotationType) {
		if (annotationType == null) {
			return null;
		}

		// 快捷方式：直接存在于元素上，无需合并？
		if (AnnotationFilter.PLAIN.matches(annotationType) ||
				AnnotationsScanner.hasPlainJavaAnnotationsOnly(method)) {
			return method.getDeclaredAnnotation(annotationType);
		}

		// 详尽检索合并的注解...
		return MergedAnnotations.from(method, SearchStrategy.TYPE_HIERARCHY, RepeatableContainers.none())
				.get(annotationType).withNonMergedAttributes()
				.synthesize(MergedAnnotation::isPresent).orElse(null);
	}

	/**
	 * 在提供的 {@link Class} 上查找单个 {@code annotationType} 类型的
	 * {@link Annotation 注解}，如果注解未 <em>直接存在</em> 于给定类本身，
	 * 则遍历其接口、注解和超类。
	 * <p>此方法显式处理未声明为 {@link java.lang.annotation.Inherited 继承} 的类级别注解，
	 * <em>以及元注解和接口上的注解</em>。
	 * <p>算法操作如下：
	 * <ol>
	 * <li>在给定类上搜索注解，如果找到则返回。
	 * <li>递归搜索给定类声明的所有注解。
	 * <li>递归搜索给定类声明的所有接口。
	 * <li>递归搜索给定类的超类层次结构。
	 * </ol>
	 * <p>注意：在此上下文中，术语 <em>递归</em> 意味着搜索过程通过返回到步骤 #1 继续，
	 * 将当前接口、注解或超类作为要查找注解的类。
	 * @param clazz 要查找注解的类
	 * @param annotationType 要查找的注解类型
	 * @return 第一个匹配的注解，如果未找到则返回 {@code null}
	 */
	@Nullable
	public static <A extends Annotation> A findAnnotation(Class<?> clazz, @Nullable Class<A> annotationType) {
		if (annotationType == null) {
			return null;
		}

		// 快捷方式：直接存在于元素上，无需合并？
		if (AnnotationFilter.PLAIN.matches(annotationType) ||
				AnnotationsScanner.hasPlainJavaAnnotationsOnly(clazz)) {
			A annotation = clazz.getDeclaredAnnotation(annotationType);
			if (annotation != null) {
				return annotation;
			}
			// 为了向后兼容，即使未标记为 @Inherited，也要对普通注解执行超类搜索：例如对 @Deprecated 的 findAnnotation 搜索
			Class<?> superclass = clazz.getSuperclass();
			if (superclass == null || superclass == Object.class) {
				return null;
			}
			return findAnnotation(superclass, annotationType);
		}

		// 详尽检索合并的注解...
		return MergedAnnotations.from(clazz, SearchStrategy.TYPE_HIERARCHY, RepeatableContainers.none())
				.get(annotationType).withNonMergedAttributes()
				.synthesize(MergedAnnotation::isPresent).orElse(null);
	}

	/**
	 * 在指定 {@code clazz} 的继承层次结构中（包括指定的 {@code clazz} 本身）
	 * 查找第一个 {@link Class}，该类上 <em>直接存在</em> 指定 {@code annotationType} 的注解。
	 * <p>如果提供的 {@code clazz} 是一个接口，则只会检查接口本身；
	 * 不会遍历接口的继承层次结构。
	 * <p>不会搜索元注解。
	 * <p>标准 {@link Class} API 不提供确定继承层次结构中哪个类实际声明了
	 * {@link Annotation} 的机制，因此我们需要显式处理。
	 * @param annotationType 要查找的注解类型
	 * @param clazz 要检查注解的类（可能为 {@code null}）
	 * @return 继承层次结构中声明指定 {@code annotationType} 注解的第一个 {@link Class}，
	 * 如果未找到则返回 {@code null}
	 * @see Class#isAnnotationPresent(Class)
	 * @see Class#getDeclaredAnnotations()
	 * @deprecated 自 5.2 版本起已弃用，因为它已被 {@link MergedAnnotations} API 取代
	 */
	@Deprecated
	@Nullable
	public static Class<?> findAnnotationDeclaringClass(
			Class<? extends Annotation> annotationType, @Nullable Class<?> clazz) {

		if (clazz == null) {
			return null;
		}

		return (Class<?>) MergedAnnotations.from(clazz, SearchStrategy.SUPERCLASS)
				.get(annotationType, MergedAnnotation::isDirectlyPresent)
				.getSource();
	}

	/**
	 * 在指定 {@code clazz} 的继承层次结构中（包括指定的 {@code clazz} 本身）
	 * 查找第一个 {@link Class}，该类上 <em>直接存在</em> 至少一个指定 {@code annotationTypes} 中的注解。
	 * <p>如果提供的 {@code clazz} 是一个接口，则只会检查接口本身；
	 * 不会遍历接口的继承层次结构。
	 * <p>不会搜索元注解。
	 * <p>标准 {@link Class} API 不提供确定继承层次结构中哪个类实际声明了
	 * 多个候选 {@linkplain Annotation 注解} 中的一个的机制，因此我们
	 * 需要显式处理。
	 * @param annotationTypes 要查找的注解类型
	 * @param clazz 要检查注解的类（可能为 {@code null}）
	 * @return 继承层次结构中声明至少一个指定 {@code annotationTypes} 中注解的第一个 {@link Class}，
	 * 如果未找到则返回 {@code null}
	 * @since 3.2.2
	 * @see Class#isAnnotationPresent(Class)
	 * @see Class#getDeclaredAnnotations()
	 * @deprecated 自 5.2 版本起已弃用，因为它已被 {@link MergedAnnotations} API 取代
	 */
	@Deprecated
	@Nullable
	public static Class<?> findAnnotationDeclaringClassForTypes(
			List<Class<? extends Annotation>> annotationTypes, @Nullable Class<?> clazz) {

		if (clazz == null) {
			return null;
		}

		return (Class<?>) MergedAnnotations.from(clazz, SearchStrategy.SUPERCLASS)
				.stream()
				.filter(MergedAnnotationPredicates.typeIn(annotationTypes).and(MergedAnnotation::isDirectlyPresent))
				.map(MergedAnnotation::getSource)
				.findFirst().orElse(null);
	}

	/**
	 * 确定在提供的 {@code clazz} 上是否本地（即 <em>直接存在</em>）声明了
	 * 指定 {@code annotationType} 的注解。
	 * <p>提供的 {@link Class} 可以表示任何类型。
	 * <p>不会搜索元注解。
	 * <p>注意：此方法不会确定注解是否为 {@linkplain java.lang.annotation.Inherited 继承}。
	 * @param annotationType 要查找的注解类型
	 * @param clazz 要检查注解的类
	 * @return 如果指定 {@code annotationType} 的注解 <em>直接存在</em>，则返回 {@code true}
	 * @see java.lang.Class#getDeclaredAnnotations()
	 * @see java.lang.Class#getDeclaredAnnotation(Class)
	 */
	public static boolean isAnnotationDeclaredLocally(Class<? extends Annotation> annotationType, Class<?> clazz) {
		return MergedAnnotations.from(clazz).get(annotationType).isDirectlyPresent();
	}

	/**
	 * 确定指定 {@code annotationType} 的注解是否 <em>存在</em> 于提供的 {@code clazz} 上，
	 * 并且是 {@linkplain java.lang.annotation.Inherited 继承的}
	 * （即不是 <em>直接存在</em> 的）。
	 * <p>不会搜索元注解。
	 * <p>如果提供的 {@code clazz} 是一个接口，则只会检查接口本身。
	 * 根据 Java 中的标准元注解语义，不会遍历接口的继承层次结构。
	 * 有关注解继承的更多详细信息，请参阅 {@code @Inherited} 元注解的
	 * {@linkplain java.lang.annotation.Inherited Javadoc}。
	 * @param annotationType 要查找的注解类型
	 * @param clazz 要检查注解的类
	 * @return 如果指定 {@code annotationType} 的注解 <em>存在</em> 且 <em>继承</em>，则返回 {@code true}
	 * @see Class#isAnnotationPresent(Class)
	 * @see #isAnnotationDeclaredLocally(Class, Class)
	 * @deprecated 自 5.2 版本起已弃用，因为它已被 {@link MergedAnnotations} API 取代
	 */
	@Deprecated
	public static boolean isAnnotationInherited(Class<? extends Annotation> annotationType, Class<?> clazz) {
		return MergedAnnotations.from(clazz, SearchStrategy.INHERITED_ANNOTATIONS)
				.stream(annotationType)
				.filter(MergedAnnotation::isDirectlyPresent)
				.findFirst().orElseGet(MergedAnnotation::missing)
				.getAggregateIndex() > 0;
	}

	/**
	 * 确定类型为 {@code metaAnnotationType} 的注解是否 <em>元存在</em> 于提供的 {@code annotationType} 上。
	 * @param annotationType 要搜索的注解类型
	 * @param metaAnnotationType 要搜索的元注解类型
	 * @return 如果此类注解元存在，则返回 {@code true}
	 * @since 4.2.1
	 * @deprecated 自 5.2 版本起已弃用，因为它已被 {@link MergedAnnotations} API 取代
	 */
	@Deprecated
	public static boolean isAnnotationMetaPresent(Class<? extends Annotation> annotationType,
			@Nullable Class<? extends Annotation> metaAnnotationType) {

		if (metaAnnotationType == null) {
			return false;
		}
		// 快捷方式：直接存在于元素上，无需合并？
		if (AnnotationFilter.PLAIN.matches(metaAnnotationType) ||
				AnnotationsScanner.hasPlainJavaAnnotationsOnly(annotationType)) {
			return annotationType.isAnnotationPresent(metaAnnotationType);
		}
		// 详尽检索合并的注解...
		return MergedAnnotations.from(annotationType, SearchStrategy.INHERITED_ANNOTATIONS,
				RepeatableContainers.none()).isPresent(metaAnnotationType);
	}

	/**
	 * 确定提供的 {@link Annotation} 是否在核心 JDK 的 {@code java.lang.annotation} 包中定义。
	 * @param annotation 要检查的注解
	 * @return 如果注解在 {@code java.lang.annotation} 包中，则返回 {@code true}
	 */
	public static boolean isInJavaLangAnnotationPackage(@Nullable Annotation annotation) {
		return (annotation != null && JAVA_LANG_ANNOTATION_FILTER.matches(annotation));
	}

	/**
	 * 确定具有指定名称的 {@link Annotation 注解} 是否在核心 JDK 的
	 * {@code java.lang.annotation} 包中定义。
	 * @param annotationType 要检查的注解类型名称
	 * @return 如果注解在 {@code java.lang.annotation} 包中，则返回 {@code true}
	 * @since 4.2
	 */
	public static boolean isInJavaLangAnnotationPackage(@Nullable String annotationType) {
		return (annotationType != null && JAVA_LANG_ANNOTATION_FILTER.matches(annotationType));
	}

	/**
	 * 检查给定注解的声明属性，特别是覆盖
	 * Google App Engine 中 {@code Class} 值的 {@code TypeNotPresentExceptionProxy} 延迟出现的情况
	 * （而不是早期 {@code Class.getAnnotations()} 失败）。
	 * <p>此方法不失败表示 {@link #getAnnotationAttributes(Annotation)}
	 * 在稍后尝试时也不会失败。
	 * @param annotation 要验证的注解
	 * @throws IllegalStateException 如果声明的 {@code Class} 属性无法读取
	 * @since 4.3.15
	 * @see Class#getAnnotations()
	 * @see #getAnnotationAttributes(Annotation)
	 */
	public static void validateAnnotation(Annotation annotation) {
		AttributeMethods.forAnnotationType(annotation.annotationType()).validate(annotation);
	}

	/**
	 * 将给定注解的属性作为 {@link Map} 检索，保留所有
	 * 属性类型。
	 * <p>等同于调用 {@link #getAnnotationAttributes(Annotation, boolean, boolean)}，
	 * 其中 {@code classValuesAsString} 和 {@code nestedAnnotationsAsMap} 参数
	 * 设置为 {@code false}。
	 * <p>注意：此方法实际上返回一个 {@link AnnotationAttributes} 实例。
	 * 但是，为了二进制兼容性，保留了 {@code Map} 签名。
	 * @param annotation 要检索属性的注解
	 * @return 注解属性的 Map，以属性名称作为键，
	 * 对应的属性值作为值（从不为 {@code null}）
	 * @see #getAnnotationAttributes(AnnotatedElement, Annotation)
	 * @see #getAnnotationAttributes(Annotation, boolean, boolean)
	 * @see #getAnnotationAttributes(AnnotatedElement, Annotation, boolean, boolean)
	 */
	public static Map<String, Object> getAnnotationAttributes(Annotation annotation) {
		return getAnnotationAttributes(null, annotation);
	}

	/**
	 * 将给定注解的属性作为 {@link Map} 检索。
	 * <p>等同于调用 {@link #getAnnotationAttributes(Annotation, boolean, boolean)}，
	 * 其中 {@code nestedAnnotationsAsMap} 参数设置为 {@code false}。
	 * <p>注意：此方法实际上返回一个 {@link AnnotationAttributes} 实例。
	 * 但是，为了二进制兼容性，保留了 {@code Map} 签名。
	 * @param annotation 要检索属性的注解
	 * @param classValuesAsString 是否将 Class 引用转换为 String（为了与
	 * {@link org.springframework.core.type.AnnotationMetadata} 兼容）
	 * 或将其保留为 Class 引用
	 * @return 注解属性的 Map，以属性名称作为键，
	 * 对应的属性值作为值（从不为 {@code null}）
	 * @see #getAnnotationAttributes(Annotation, boolean, boolean)
	 */
	public static Map<String, Object> getAnnotationAttributes(
			Annotation annotation, boolean classValuesAsString) {

		return getAnnotationAttributes(annotation, classValuesAsString, false);
	}

	/**
	 * 将给定注解的属性作为 {@link AnnotationAttributes} map 检索。
	 * <p>此方法提供了完全递归的注解读取功能，与基于反射的
	 * {@link org.springframework.core.type.StandardAnnotationMetadata} 相当。
	 * @param annotation 要检索属性的注解
	 * @param classValuesAsString 是否将 Class 引用转换为 String（为了与
	 * {@link org.springframework.core.type.AnnotationMetadata} 兼容）
	 * 或将其保留为 Class 引用
	 * @param nestedAnnotationsAsMap 是否将嵌套注解转换为
	 * {@link AnnotationAttributes} map（为了与
	 * {@link org.springframework.core.type.AnnotationMetadata} 兼容）或将其保留为
	 * {@code Annotation} 实例
	 * @return 注解属性（一个专门的 Map），以属性名称作为键，
	 * 对应的属性值作为值（从不为 {@code null}）
	 * @since 3.1.1
	 */
	public static AnnotationAttributes getAnnotationAttributes(
			Annotation annotation, boolean classValuesAsString, boolean nestedAnnotationsAsMap) {

		return getAnnotationAttributes(null, annotation, classValuesAsString, nestedAnnotationsAsMap);
	}

	/**
	 * 将给定注解的属性作为 {@link AnnotationAttributes} map 检索。
	 * <p>等同于调用 {@link #getAnnotationAttributes(AnnotatedElement, Annotation, boolean, boolean)}，
	 * 其中 {@code classValuesAsString} 和 {@code nestedAnnotationsAsMap} 参数
	 * 设置为 {@code false}。
	 * @param annotatedElement 带有提供注解的元素；
	 * 如果未知，则可能为 {@code null}
	 * @param annotation 要检索属性的注解
	 * @return 注解属性（一个专门的 Map），以属性名称作为键，
	 * 对应的属性值作为值（从不为 {@code null}）
	 * @since 4.2
	 * @see #getAnnotationAttributes(AnnotatedElement, Annotation, boolean, boolean)
	 */
	public static AnnotationAttributes getAnnotationAttributes(
			@Nullable AnnotatedElement annotatedElement, Annotation annotation) {

		return getAnnotationAttributes(annotatedElement, annotation, false, false);
	}

	/**
	 * 将给定注解的属性作为 {@link AnnotationAttributes} map 检索。
	 * <p>此方法提供了完全递归的注解读取功能，与基于反射的
	 * {@link org.springframework.core.type.StandardAnnotationMetadata} 相当。
	 * @param annotatedElement 带有提供注解的元素；
	 * 如果未知，则可能为 {@code null}
	 * @param annotation 要检索属性的注解
	 * @param classValuesAsString 是否将 Class 引用转换为 String（为了与
	 * {@link org.springframework.core.type.AnnotationMetadata} 兼容）
	 * 或将其保留为 Class 引用
	 * @param nestedAnnotationsAsMap 是否将嵌套注解转换为
	 * {@link AnnotationAttributes} map（为了与
	 * {@link org.springframework.core.type.AnnotationMetadata} 兼容）或将其保留为
	 * {@code Annotation} 实例
	 * @return 注解属性（一个专门的 Map），以属性名称作为键，
	 * 对应的属性值作为值（从不为 {@code null}）
	 * @since 4.2
	 */
	public static AnnotationAttributes getAnnotationAttributes(
			@Nullable AnnotatedElement annotatedElement, Annotation annotation,
			boolean classValuesAsString, boolean nestedAnnotationsAsMap) {

		Adapt[] adaptations = Adapt.values(classValuesAsString, nestedAnnotationsAsMap);
		return MergedAnnotation.from(annotatedElement, annotation)
				.withNonMergedAttributes()
				.asMap(mergedAnnotation ->
						new AnnotationAttributes(mergedAnnotation.getType(), true), adaptations);
	}

	/**
	 * 如果可用，注册给定属性的注解声明的默认值。
	 * @param attributes 要处理的注解属性
	 * @since 4.3.2
	 */
	public static void registerDefaultValues(AnnotationAttributes attributes) {
		Class<? extends Annotation> annotationType = attributes.annotationType();
		if (annotationType != null && Modifier.isPublic(annotationType.getModifiers()) &&
				!AnnotationFilter.PLAIN.matches(annotationType)) {
			Map<String, DefaultValueHolder> defaultValues = getDefaultValues(annotationType);
			defaultValues.forEach(attributes::putIfAbsent);
		}
	}

	private static Map<String, DefaultValueHolder> getDefaultValues(
			Class<? extends Annotation> annotationType) {

		return defaultValuesCache.computeIfAbsent(annotationType,
				AnnotationUtils::computeDefaultValues);
	}

	private static Map<String, DefaultValueHolder> computeDefaultValues(
			Class<? extends Annotation> annotationType) {

		AttributeMethods methods = AttributeMethods.forAnnotationType(annotationType);
		if (!methods.hasDefaultValueMethod()) {
			return Collections.emptyMap();
		}
		Map<String, DefaultValueHolder> result = CollectionUtils.newLinkedHashMap(methods.size());
		if (!methods.hasNestedAnnotation()) {
			// 使用更简单的方法，如果没有嵌套注解
			for (int i = 0; i < methods.size(); i++) {
				Method method = methods.get(i);
				Object defaultValue = method.getDefaultValue();
				if (defaultValue != null) {
					result.put(method.getName(), new DefaultValueHolder(defaultValue));
				}
			}
		}
		else {
			// 如果有嵌套注解，我们需要将它们作为嵌套映射
			AnnotationAttributes attributes = MergedAnnotation.of(annotationType)
					.asMap(annotation ->
							new AnnotationAttributes(annotation.getType(), true), Adapt.ANNOTATION_TO_MAP);
			for (Map.Entry<String, Object> element : attributes.entrySet()) {
				result.put(element.getKey(), new DefaultValueHolder(element.getValue()));
			}
		}
		return result;
	}

	/**
	 * 后处理提供的 {@link AnnotationAttributes}，将嵌套注解保留为 {@code Annotation} 实例。
	 * <p>具体来说，此方法会为使用 {@link AliasFor @AliasFor} 注解的注解属性强制执行
	 * <em>属性别名</em>语义，并将默认值占位符替换为它们的原始默认值。
	 * @param annotatedElement 带有注解或注解层次结构的元素，从中创建了提供的属性；
	 * 如果未知，可能为 {@code null}
	 * @param attributes 要后处理的注解属性
	 * @param classValuesAsString 是否将 Class 引用转换为 String（为了与
	 * {@link org.springframework.core.type.AnnotationMetadata} 兼容），
	 * 还是将其保留为 Class 引用
	 * @since 4.3.2
	 * @see #getDefaultValue(Class, String)
	 */
	public static void postProcessAnnotationAttributes(@Nullable Object annotatedElement,
			@Nullable AnnotationAttributes attributes, boolean classValuesAsString) {

		if (attributes == null) {
			return;
		}
		if (!attributes.validated) {
			Class<? extends Annotation> annotationType = attributes.annotationType();
			if (annotationType == null) {
				return;
			}
			AnnotationTypeMapping mapping = AnnotationTypeMappings.forAnnotationType(annotationType).get(0);
			for (int i = 0; i < mapping.getMirrorSets().size(); i++) {
				MirrorSet mirrorSet = mapping.getMirrorSets().get(i);
				int resolved = mirrorSet.resolve(attributes.displayName, attributes,
						AnnotationUtils::getAttributeValueForMirrorResolution);
				if (resolved != -1) {
					Method attribute = mapping.getAttributes().get(resolved);
					Object value = attributes.get(attribute.getName());
					for (int j = 0; j < mirrorSet.size(); j++) {
						Method mirror = mirrorSet.get(j);
						if (mirror != attribute) {
							attributes.put(mirror.getName(),
									adaptValue(annotatedElement, value, classValuesAsString));
						}
					}
				}
			}
		}
		for (Map.Entry<String, Object> attributeEntry : attributes.entrySet()) {
			String attributeName = attributeEntry.getKey();
			Object value = attributeEntry.getValue();
			if (value instanceof DefaultValueHolder) {
				value = ((DefaultValueHolder) value).defaultValue;
				attributes.put(attributeName,
						adaptValue(annotatedElement, value, classValuesAsString));
			}
		}
	}

	private static Object getAttributeValueForMirrorResolution(Method attribute, Object attributes) {
		Object result = ((AnnotationAttributes) attributes).get(attribute.getName());
		return (result instanceof DefaultValueHolder ? ((DefaultValueHolder) result).defaultValue : result);
	}

	@Nullable
	private static Object adaptValue(
			@Nullable Object annotatedElement, @Nullable Object value, boolean classValuesAsString) {

		if (classValuesAsString) {
			if (value instanceof Class) {
				return ((Class<?>) value).getName();
			}
			if (value instanceof Class[]) {
				Class<?>[] classes = (Class<?>[]) value;
				String[] names = new String[classes.length];
				for (int i = 0; i < classes.length; i++) {
					names[i] = classes[i].getName();
				}
				return names;
			}
		}
		if (value instanceof Annotation) {
			Annotation annotation = (Annotation) value;
			return MergedAnnotation.from(annotatedElement, annotation).synthesize();
		}
		if (value instanceof Annotation[]) {
			Annotation[] annotations = (Annotation[]) value;
			Annotation[] synthesized = (Annotation[]) Array.newInstance(
					annotations.getClass().getComponentType(), annotations.length);
			for (int i = 0; i < annotations.length; i++) {
				synthesized[i] = MergedAnnotation.from(annotatedElement, annotations[i]).synthesize();
			}
			return synthesized;
		}
		return value;
	}

	/**
	 * 给定一个注解实例，检索单元素注解的 {@code value} 属性的 *值*。
	 * @param annotation 要从中检索值的注解实例
	 * @return 属性值，如果未找到则为 {@code null}，除非由于 {@link AnnotationConfigurationException}
	 * 无法检索属性值，在这种情况下将重新抛出该异常
	 * @see #getValue(Annotation, String)
	 */
	@Nullable
	public static Object getValue(Annotation annotation) {
		return getValue(annotation, VALUE);
	}

	/**
	 * 给定一个注解实例，检索命名属性的 *值*。
	 * @param annotation 要从中检索值的注解实例
	 * @param attributeName 要检索的属性值的名称
	 * @return 属性值，如果未找到则为 {@code null}，除非由于 {@link AnnotationConfigurationException}
	 * 无法检索属性值，在这种情况下将重新抛出该异常
	 * @see #getValue(Annotation)
	 */
	@Nullable
	public static Object getValue(@Nullable Annotation annotation, @Nullable String attributeName) {
		if (annotation == null || !StringUtils.hasText(attributeName)) {
			return null;
		}
		try {
			Method method = annotation.annotationType().getDeclaredMethod(attributeName);
			ReflectionUtils.makeAccessible(method);
			return method.invoke(annotation);
		}
		catch (NoSuchMethodException ex) {
			return null;
		}
		catch (InvocationTargetException ex) {
			rethrowAnnotationConfigurationException(ex.getTargetException());
			throw new IllegalStateException("Could not obtain value for annotation attribute '" +
					attributeName + "' in " + annotation, ex);
		}
		catch (Throwable ex) {
			handleIntrospectionFailure(annotation.getClass(), ex);
			return null;
		}
	}

	/**
	 * 如果提供的可抛出对象是 {@link AnnotationConfigurationException}，
	 * 它将被转换为 {@code AnnotationConfigurationException} 并抛出，
	 * 允许它传播到调用者。
	 * <p>否则，此方法不执行任何操作。
	 * @param ex 要检查的可抛出对象
	 */
	static void rethrowAnnotationConfigurationException(Throwable ex) {
		if (ex instanceof AnnotationConfigurationException) {
			throw (AnnotationConfigurationException) ex;
		}
	}

	/**
	 * 处理提供的注解自省异常。
	 * <p>如果提供的异常是 {@link AnnotationConfigurationException}，
	 * 它将被简单地抛出，允许它传播到调用者，并且不会记录任何内容。
	 * <p>否则，此方法在继续之前会记录自省失败（特别是针对 {@link TypeNotPresentException}），
	 * 假设嵌套的 {@code Class} 值在注解属性中无法解析，从而有效地假装指定元素上没有注解。
	 * @param element 我们尝试自省注解的元素
	 * @param ex 我们遇到的异常
	 * @see #rethrowAnnotationConfigurationException
	 * @see IntrospectionFailureLogger
	 */
	static void handleIntrospectionFailure(@Nullable AnnotatedElement element, Throwable ex) {
		rethrowAnnotationConfigurationException(ex);
		IntrospectionFailureLogger logger = IntrospectionFailureLogger.INFO;
		boolean meta = false;
		if (element instanceof Class && Annotation.class.isAssignableFrom((Class<?>) element)) {
			// 元注解或注解类型上的（默认）值查找
			logger = IntrospectionFailureLogger.DEBUG;
			meta = true;
		}
		if (logger.isEnabled()) {
			String message = meta ?
					"Failed to meta-introspect annotation " :
					"Failed to introspect annotations on ";
			logger.log(message + element + ": " + ex);
		}
	}

	/**
	 * 给定注解实例，检索单元素注解 {@code value} 属性的 *默认值*。
	 * @param annotation 要从中检索默认值的注解实例
	 * @return 默认值，如果未找到则为 {@code null}
	 * @see #getDefaultValue(Annotation, String)
	 */
	@Nullable
	public static Object getDefaultValue(Annotation annotation) {
		return getDefaultValue(annotation, VALUE);
	}

	/**
	 * 给定注解实例，检索命名属性的 *默认值*。
	 * @param annotation 要从中检索默认值的注解实例
	 * @param attributeName 要检索的属性值的名称
	 * @return 命名属性的默认值，如果未找到则为 {@code null}
	 * @see #getDefaultValue(Class, String)
	 */
	@Nullable
	public static Object getDefaultValue(@Nullable Annotation annotation, @Nullable String attributeName) {
		return (annotation != null ? getDefaultValue(annotation.annotationType(), attributeName) : null);
	}

	/**
	 * 给定 {@link Class 注解类型}，检索单元素注解 {@code value} 属性的 *默认值*。
	 * @param annotationType 要检索默认值的 *注解类型*
	 * @return 默认值，如果未找到则为 {@code null}
	 * @see #getDefaultValue(Class, String)
	 */
	@Nullable
	public static Object getDefaultValue(Class<? extends Annotation> annotationType) {
		return getDefaultValue(annotationType, VALUE);
	}

	/**
	 * 给定 {@link Class 注解类型}，检索命名属性的 *默认值*。
	 * @param annotationType 要检索默认值的 *注解类型*
	 * @param attributeName 要检索的属性值的名称。
	 * @return 命名属性的默认值，如果未找到则为 {@code null}
	 * @see #getDefaultValue(Annotation, String)
	 */
	@Nullable
	public static Object getDefaultValue(
			@Nullable Class<? extends Annotation> annotationType, @Nullable String attributeName) {

		if (annotationType == null || !StringUtils.hasText(attributeName)) {
			return null;
		}
		return MergedAnnotation.of(annotationType).getDefaultValue(attributeName).orElse(null);
	}

	/**
	 * 通过将提供的注解包装在动态代理中来<em>合成</em>注解，该代理透明地强制执行
	 * 使用 {@link AliasFor @AliasFor} 注解的注解属性的<em>属性别名</em>语义。
	 *
	 * @param annotation 要合成的注解
	 * @param annotatedElement 使用提供的注解进行注解的元素；如果未知则可以为 {@code null}
	 * @return 如果提供的注解是<em>可合成的</em>，则返回合成的注解；
	 *         如果提供的注解为 {@code null}，则返回 {@code null}；
	 *         否则返回未修改的提供的注解
	 * @throws AnnotationConfigurationException 如果检测到 {@code @AliasFor} 的无效配置
	 * @since 4.2
	 * @see #synthesizeAnnotation(Map, Class, AnnotatedElement)
	 * @see #synthesizeAnnotation(Class)
	 */
	public static <A extends Annotation> A synthesizeAnnotation(
			A annotation, @Nullable AnnotatedElement annotatedElement) {

		if (annotation instanceof SynthesizedAnnotation || AnnotationFilter.PLAIN.matches(annotation)) {
			return annotation;
		}
		return MergedAnnotation.from(annotatedElement, annotation).synthesize();
	}

	/**
	 * 从注解的默认属性值<em>合成</em>注解。
	 *
	 * <p>此方法简单地委托给 {@link #synthesizeAnnotation(Map, Class, AnnotatedElement)}，
	 * 为源属性值提供空映射，为 {@link AnnotatedElement} 提供 {@code null}。
	 *
	 * @param annotationType 要合成的注解类型
	 * @return 合成的注解
	 * @throws IllegalArgumentException 如果缺少必需的属性
	 * @throws AnnotationConfigurationException 如果检测到 {@code @AliasFor} 的无效配置
	 * @since 4.2
	 * @see #synthesizeAnnotation(Map, Class, AnnotatedElement)
	 * @see #synthesizeAnnotation(Annotation, AnnotatedElement)
	 */
	public static <A extends Annotation> A synthesizeAnnotation(Class<A> annotationType) {
		return synthesizeAnnotation(Collections.emptyMap(), annotationType, null);
	}

	/**
	 * 通过将注解属性映射包装在动态代理中来从提供的注解属性映射<em>合成</em>注解，
	 * 该代理实现指定 {@code annotationType} 的注解，并透明地强制执行
	 * 使用 {@link AliasFor @AliasFor} 注解的注解属性的<em>属性别名</em>语义。
	 *
	 * <p>提供的映射必须包含在提供的 {@code annotationType} 中定义的每个属性的键值对，
	 * 这些属性不是别名或没有默认值。嵌套映射和嵌套映射数组将分别递归合成为
	 * 嵌套注解或嵌套注解数组。
	 *
	 * <p>请注意，{@link AnnotationAttributes} 是 {@link Map} 的专用类型，
	 * 是此方法的 {@code attributes} 参数的理想候选者。
	 *
	 * @param attributes 要合成的注解属性映射
	 * @param annotationType 要合成的注解类型
	 * @param annotatedElement 使用与提供属性对应的注解进行注解的元素；如果未知则可以为 {@code null}
	 * @return 合成的注解
	 * @throws IllegalArgumentException 如果缺少必需的属性或属性类型不正确
	 * @throws AnnotationConfigurationException 如果检测到 {@code @AliasFor} 的无效配置
	 * @since 4.2
	 * @see #synthesizeAnnotation(Annotation, AnnotatedElement)
	 * @see #synthesizeAnnotation(Class)
	 * @see #getAnnotationAttributes(AnnotatedElement, Annotation)
	 * @see #getAnnotationAttributes(AnnotatedElement, Annotation, boolean, boolean)
	 */
	public static <A extends Annotation> A synthesizeAnnotation(Map<String, Object> attributes,
			Class<A> annotationType, @Nullable AnnotatedElement annotatedElement) {

		try {
			return MergedAnnotation.of(annotatedElement, annotationType, attributes).synthesize();
		}
		catch (NoSuchElementException | IllegalStateException ex) {
			throw new IllegalArgumentException(ex);
		}
	}

	/**
	 * 通过创建相同大小和类型的新数组并用输入数组中注解的
	 * {@linkplain #synthesizeAnnotation(Annotation, AnnotatedElement) 合成}版本
	 * 填充它，从提供的 {@code annotations} 数组<em>合成</em>注解数组。
	 *
	 * @param annotations 要合成的注解数组
	 * @param annotatedElement 使用提供的注解数组进行注解的元素；如果未知则可以为 {@code null}
	 * @return 新的合成注解数组，如果提供的数组为 {@code null} 则返回 {@code null}
	 * @throws AnnotationConfigurationException 如果检测到 {@code @AliasFor} 的无效配置
	 * @since 4.2
	 * @see #synthesizeAnnotation(Annotation, AnnotatedElement)
	 * @see #synthesizeAnnotation(Map, Class, AnnotatedElement)
	 */
	static Annotation[] synthesizeAnnotationArray(Annotation[] annotations, AnnotatedElement annotatedElement) {
		if (AnnotationsScanner.hasPlainJavaAnnotationsOnly(annotatedElement)) {
			return annotations;
		}
		Annotation[] synthesized = (Annotation[]) Array.newInstance(
				annotations.getClass().getComponentType(), annotations.length);
		for (int i = 0; i < annotations.length; i++) {
			synthesized[i] = synthesizeAnnotation(annotations[i], annotatedElement);
		}
		return synthesized;
	}

	/**
	 * 清除内部注解元数据缓存。
	 *
	 * @since 4.3.15
	 */
	public static void clearCache() {
		AnnotationTypeMappings.clearCache();
		AnnotationsScanner.clearCache();
	}


	/**
	 * 用于包装默认值的内部持有者。
	 */
	private static class DefaultValueHolder {

		final Object defaultValue;

		public DefaultValueHolder(Object defaultValue) {
			this.defaultValue = defaultValue;
		}

		@Override
		public String toString() {
			return "*" + this.defaultValue;
		}
	}

}
