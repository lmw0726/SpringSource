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

package org.springframework.core.annotation;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * 提供对合并注解集合的访问，通常从 {@link Class} 或 {@link Method} 等源获取。
 *
 * <p>每个合并注解表示一个视图，其中属性值可能已从不同的源值“合并”而来，通常包括：
 *
 * <ul>
 * <li>注解中一个或多个属性上的显式和隐式 {@link AliasFor @AliasFor} 声明</li>
 * <li>元注解的显式 {@link AliasFor @AliasFor} 声明</li>
 * <li>元注解的基于约定的属性别名</li>
 * <li>来自元注解声明</li>
 * </ul>
 *
 * <p>例如，{@code @PostMapping} 注解可能定义如下：
 *
 * <pre class="code">
 * &#064;Retention(RetentionPolicy.RUNTIME)
 * &#064;RequestMapping(method = RequestMethod.POST)
 * public &#064;interface PostMapping {
 *
 *     &#064;AliasFor(attribute = "path")
 *     String[] value() default {};
 *
 *     &#064;AliasFor(attribute = "value")
 *     String[] path() default {};
 * }
 * </pre>
 *
 * <p>如果一个方法用 {@code @PostMapping("/home")} 注解，它将包含 {@code @PostMapping}
 * 和元注解 {@code @RequestMapping} 的合并注解。
 * {@code @RequestMapping} 注解的合并视图将包含以下属性：
 *
 * <p><table border="1">
 * <tr>
 * <th>名称</th>
 * <th>值</th>
 * <th>来源</th>
 * </tr>
 * <tr>
 * <td>value</td>
 * <td>"/home"</td>
 * <td>在 {@code @PostMapping} 中声明</td>
 * </tr>
 * <tr>
 * <td>path</td>
 * <td>"/home"</td>
 * <td>显式 {@code @AliasFor}</td>
 * </tr>
 * <tr>
 * <td>method</td>
 * <td>RequestMethod.POST</td>
 * <td>在元注解中声明</td>
 * </tr>
 * </table>
 *
 * <p>{@code MergedAnnotations} 可以从任何 Java {@link AnnotatedElement}
 * {@linkplain #from(AnnotatedElement) 获取}。它们也可以用于不使用反射的源
 * （例如直接解析字节码的源）。
 *
 * <p>可以使用不同的 {@linkplain SearchStrategy 搜索策略} 来查找包含要聚合的注解的相关源元素。
 * 例如，{@link SearchStrategy#TYPE_HIERARCHY} 将搜索超类和已实现的接口。
 *
 * <p>从 {@code MergedAnnotations} 实例中，您可以
 * {@linkplain #get(String) 获取} 单个注解，或者
 * {@linkplain #stream() 流式传输所有注解}，或者只传输匹配
 * {@linkplain #stream(String) 特定类型} 的注解。您还可以快速判断注解
 * {@linkplain #isPresent(String) 是否存在}。
 *
 * <p>以下是一些典型示例：
 *
 * <pre class="code">
 * // 注解是否存在或元存在？
 * mergedAnnotations.isPresent(ExampleAnnotation.class);
 *
 * // 获取 ExampleAnnotation 的合并“value”属性（直接或元存在）
 * mergedAnnotations.get(ExampleAnnotation.class).getString("value");
 *
 * // 获取所有元注解，但不包括直接存在的注解
 * mergedAnnotations.stream().filter(MergedAnnotation::isMetaPresent);
 *
 * // 获取所有 ExampleAnnotation 声明（包括任何元注解）并打印合并的“value”属性
 * mergedAnnotations.stream(ExampleAnnotation.class)
 *     .map(mergedAnnotation -&gt; mergedAnnotation.getString("value"))
 *     .forEach(System.out::println);
 * </pre>
 *
 * <p><b>注意：{@code MergedAnnotations} API 及其底层模型是为 Spring
 * 常见组件模型中的可组合注解而设计的，重点关注属性别名和元注解关系。</b>
 * 此 API 不支持检索纯 Java 注解；请使用标准 Java 反射或 Spring 的
 * {@link AnnotationUtils} 进行简单的注解检索。
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 5.2
 * @see MergedAnnotation
 * @see MergedAnnotationCollectors
 * @see MergedAnnotationPredicates
 * @see MergedAnnotationSelectors
 */
public interface MergedAnnotations extends Iterable<MergedAnnotation<Annotation>> {

	/**
	 * 确定指定的注解类型是直接存在还是元存在。
	 * <p>等同于调用 {@code get(annotationType).isPresent()}。
	 * @param annotationType 要检查的注解类型
	 * @return 如果注解存在，则为 {@code true}
	 */
	<A extends Annotation> boolean isPresent(Class<A> annotationType);

	/**
	 * 确定指定的注解类型是直接存在还是元存在。
	 * <p>等同于调用 {@code get(annotationType).isPresent()}。
	 * @param annotationType 要检查的注解类型的完全限定类名
	 * @return 如果注解存在，则为 {@code true}
	 */
	boolean isPresent(String annotationType);

	/**
	 * 确定指定的注解类型是否直接存在。
	 * <p>等同于调用 {@code get(annotationType).isDirectlyPresent()}。
	 * @param annotationType 要检查的注解类型
	 * @return 如果注解直接存在，则为 {@code true}
	 */
	<A extends Annotation> boolean isDirectlyPresent(Class<A> annotationType);

	/**
	 * 确定指定的注解类型是否直接存在。
	 * <p>等同于调用 {@code get(annotationType).isDirectlyPresent()}。
	 * @param annotationType 要检查的注解类型的完全限定类名
	 * @return 如果注解直接存在，则为 {@code true}
	 */
	boolean isDirectlyPresent(String annotationType);

	/**
	 * 获取指定类型的 {@linkplain MergedAnnotationSelectors#nearest() 最近} 匹配注解或元注解，
	 * 如果不存在则为 {@link MergedAnnotation#missing()}。
	 * @param annotationType 要获取的注解类型
	 * @return {@link MergedAnnotation} 实例
	 */
	<A extends Annotation> MergedAnnotation<A> get(Class<A> annotationType);

	/**
	 * 获取指定类型的 {@linkplain MergedAnnotationSelectors#nearest() 最近} 匹配注解或元注解，
	 * 如果不存在则为 {@link MergedAnnotation#missing()}。
	 * @param annotationType 要获取的注解类型
	 * @param predicate 必须匹配的谓词，如果只需要类型匹配则为 {@code null}
	 * @return {@link MergedAnnotation} 实例
	 * @see MergedAnnotationPredicates
	 */
	<A extends Annotation> MergedAnnotation<A> get(Class<A> annotationType,
			@Nullable Predicate<? super MergedAnnotation<A>> predicate);

	/**
	 * 获取指定类型的匹配注解或元注解，如果不存在则为 {@link MergedAnnotation#missing()}。
	 * @param annotationType 要获取的注解类型
	 * @param predicate 必须匹配的谓词，如果只需要类型匹配则为 {@code null}
	 * @param selector 用于在聚合中选择最合适注解的选择器，如果选择
	 * {@linkplain MergedAnnotationSelectors#nearest() 最近} 则为 {@code null}
	 * @return {@link MergedAnnotation} 实例
	 * @see MergedAnnotationPredicates
	 * @see MergedAnnotationSelectors
	 */
	<A extends Annotation> MergedAnnotation<A> get(Class<A> annotationType,
			@Nullable Predicate<? super MergedAnnotation<A>> predicate,
			@Nullable MergedAnnotationSelector<A> selector);

	/**
	 * 获取指定类型的 {@linkplain MergedAnnotationSelectors#nearest() 最近} 匹配注解或元注解，
	 * 如果不存在则为 {@link MergedAnnotation#missing()}。
	 * @param annotationType 要获取的注解类型的完全限定类名
	 * @return {@link MergedAnnotation} 实例
	 */
	<A extends Annotation> MergedAnnotation<A> get(String annotationType);

	/**
	 * 获取指定类型的 {@linkplain MergedAnnotationSelectors#nearest() 最近} 匹配注解或元注解，
	 * 如果不存在则为 {@link MergedAnnotation#missing()}。
	 * @param annotationType 要获取的注解类型的完全限定类名
	 * @param predicate 必须匹配的谓词，如果只需要类型匹配则为 {@code null}
	 * @return {@link MergedAnnotation} 实例
	 * @see MergedAnnotationPredicates
	 */
	<A extends Annotation> MergedAnnotation<A> get(String annotationType,
			@Nullable Predicate<? super MergedAnnotation<A>> predicate);

	/**
	 * 获取指定类型的匹配注解或元注解，如果不存在则为 {@link MergedAnnotation#missing()}。
	 * @param annotationType 要获取的注解类型的完全限定类名
	 * @param predicate 必须匹配的谓词，如果只需要类型匹配则为 {@code null}
	 * @param selector 用于在聚合中选择最合适注解的选择器，如果选择
	 * {@linkplain MergedAnnotationSelectors#nearest() 最近} 则为 {@code null}
	 * @return {@link MergedAnnotation} 实例
	 * @see MergedAnnotationPredicates
	 * @see MergedAnnotationSelectors
	 */
	<A extends Annotation> MergedAnnotation<A> get(String annotationType,
			@Nullable Predicate<? super MergedAnnotation<A>> predicate,
			@Nullable MergedAnnotationSelector<A> selector);

	/**
	 * 流式传输所有匹配指定类型的注解和元注解。
	 * <p>结果流遵循与 {@link #stream()} 相同的排序规则。
	 * @param annotationType 要匹配的注解类型
	 * @return 匹配注解的流
	 */
	<A extends Annotation> Stream<MergedAnnotation<A>> stream(Class<A> annotationType);

	/**
	 * 流式传输所有匹配指定类型的注解和元注解。
	 * <p>结果流遵循与 {@link #stream()} 相同的排序规则。
	 * @param annotationType 要匹配的注解类型的完全限定类名
	 * @return 匹配注解的流
	 */
	<A extends Annotation> Stream<MergedAnnotation<A>> stream(String annotationType);

	/**
	 * 流式传输此集合中包含的所有注解和元注解。
	 * <p>结果流首先按 {@linkplain MergedAnnotation#getAggregateIndex() 聚合索引} 排序，
	 * 然后按注解距离排序（最近的注解优先）。此排序意味着，对于大多数用例，
	 * 最合适的注解会最先出现在流中。
	 * @return 注解流
	 */
	Stream<MergedAnnotation<Annotation>> stream();


	/**
	 * 创建一个新的 {@link MergedAnnotations} 实例，包含来自指定元素的所有注解和元注解。
	 * <p>结果实例将不包括任何继承的注解。
	 * 如果您也想包含这些，应使用带适当 {@link SearchStrategy} 的
	 * {@link #from(AnnotatedElement, SearchStrategy)}。
	 * @param element 源元素
	 * @return 包含元素注解的 {@code MergedAnnotations} 实例
	 */
	static MergedAnnotations from(AnnotatedElement element) {
		return from(element, SearchStrategy.DIRECT);
	}

	/**
	 * 创建一个新的 {@link MergedAnnotations} 实例，包含来自指定元素的所有注解和元注解，
	 * 并根据 {@link SearchStrategy} 包含相关的继承元素。
	 * @param element 源元素
	 * @param searchStrategy 要使用的搜索策略
	 * @return 包含合并元素注解的 {@code MergedAnnotations} 实例
	 */
	static MergedAnnotations from(AnnotatedElement element, SearchStrategy searchStrategy) {
		return from(element, searchStrategy, RepeatableContainers.standardRepeatables());
	}

	/**
	 * 创建一个新的 {@link MergedAnnotations} 实例，包含来自指定元素的所有注解和元注解，
	 * 并根据 {@link SearchStrategy} 包含相关的继承元素。
	 * @param element 源元素
	 * @param searchStrategy 要使用的搜索策略
	 * @param repeatableContainers 元素注解或元注解可能使用的可重复容器
	 * @return 包含合并元素注解的 {@code MergedAnnotations} 实例
	 */
	static MergedAnnotations from(AnnotatedElement element, SearchStrategy searchStrategy,
			RepeatableContainers repeatableContainers) {
		//从注解元素、搜索策略、可重复容器以及注解过滤器合并注解
		return from(element, searchStrategy, repeatableContainers, AnnotationFilter.PLAIN);
	}

	/**
	 * 创建一个新的 {@link MergedAnnotations} 实例，包含来自指定元素的所有注解和元注解，
	 * 并根据 {@link SearchStrategy} 包含相关的继承元素。
	 * @param element 源元素
	 * @param searchStrategy 要使用的搜索策略
	 * @param repeatableContainers 元素注解或元注解可能使用的可重复容器
	 * @param annotationFilter 用于限制考虑的注解的注解过滤器
	 * @return 包含所提供元素的合并注解的 {@code MergedAnnotations} 实例
	 */
	static MergedAnnotations from(AnnotatedElement element, SearchStrategy searchStrategy,
			RepeatableContainers repeatableContainers, AnnotationFilter annotationFilter) {

		Assert.notNull(repeatableContainers, "RepeatableContainers must not be null");
		Assert.notNull(annotationFilter, "AnnotationFilter must not be null");
		return TypeMappedAnnotations.from(element, searchStrategy, repeatableContainers, annotationFilter);
	}

	/**
	 * 从指定的注解创建新的 {@link MergedAnnotations} 实例。
	 * @param annotations 要包含的注解
	 * @return 包含注解的 {@code MergedAnnotations} 实例
	 * @see #from(Object, Annotation...)
	 */
	static MergedAnnotations from(Annotation... annotations) {
		return from(annotations, annotations);
	}

	/**
	 * 从指定的注解创建新的 {@link MergedAnnotations} 实例。
	 * @param source 注解的源。此源仅用于信息和日志记录。它不需要
	 * <em>实际</em>包含指定的注解，也不会被搜索。
	 * @param annotations 要包含的注解
	 * @return 包含注解的 {@code MergedAnnotations} 实例
	 * @see #from(Annotation...)
	 * @see #from(AnnotatedElement)
	 */
	static MergedAnnotations from(Object source, Annotation... annotations) {
		return from(source, annotations, RepeatableContainers.standardRepeatables());
	}

	/**
	 * 从指定的注解创建新的 {@link MergedAnnotations} 实例。
	 * @param source 注解的源。此源仅用于信息和日志记录。它不需要
	 * <em>实际</em>包含指定的注解，也不会被搜索。
	 * @param annotations 要包含的注解
	 * @param repeatableContainers 元注解可能使用的可重复容器
	 * @return 包含注解的 {@code MergedAnnotations} 实例
	 */
	static MergedAnnotations from(Object source, Annotation[] annotations, RepeatableContainers repeatableContainers) {
		return from(source, annotations, repeatableContainers, AnnotationFilter.PLAIN);
	}

	/**
	 * 从指定的注解创建新的 {@link MergedAnnotations} 实例。
	 * @param source 注解的源。此源仅用于信息和日志记录。它不需要
	 * <em>实际</em>包含指定的注解，也不会被搜索。
	 * @param annotations 要包含的注解
	 * @param repeatableContainers 元注解可能使用的可重复容器
	 * @param annotationFilter 用于限制考虑的注解的注解过滤器
	 * @return 包含注解的 {@code MergedAnnotations} 实例
	 */
	static MergedAnnotations from(Object source, Annotation[] annotations,
			RepeatableContainers repeatableContainers, AnnotationFilter annotationFilter) {

		Assert.notNull(repeatableContainers, "RepeatableContainers must not be null");
		Assert.notNull(annotationFilter, "AnnotationFilter must not be null");
		return TypeMappedAnnotations.from(source, annotations, repeatableContainers, annotationFilter);
	}

	/**
	 * 从直接存在的注解的指定集合创建新的 {@link MergedAnnotations} 实例。
	 * 此方法允许从不一定使用反射加载的注解创建 {@code MergedAnnotations} 实例。
	 * 所提供的注解必须全部 {@link MergedAnnotation#isDirectlyPresent() 直接存在}，
	 * 并且必须具有 {@link MergedAnnotation#getAggregateIndex() 聚合索引} 为 {@code 0}。
	 * <p>结果 {@code MergedAnnotations} 实例将包含指定的注解以及可以使用反射读取的任何元注解。
	 * @param annotations 要包含的注解
	 * @return 包含注解的 {@code MergedAnnotations} 实例
	 * @see MergedAnnotation#of(ClassLoader, Object, Class, java.util.Map)
	 */
	static MergedAnnotations of(Collection<MergedAnnotation<?>> annotations) {
		return MergedAnnotationsCollection.of(annotations);
	}


	/**
	 * {@link MergedAnnotations#from(AnnotatedElement, SearchStrategy)}
	 * 和该方法的变体支持的搜索策略。
	 *
	 * <p>每种策略都会创建一组不同的聚合，这些聚合将组合起来创建最终的 {@link MergedAnnotations}。
	 */
	enum SearchStrategy {

		/**
		 * 仅查找直接声明的注解，不考虑 {@link Inherited @Inherited} 注解，
		 * 也不搜索超类或已实现的接口。
		 */
		DIRECT,

		/**
		 * 查找所有直接声明的注解以及任何 {@link Inherited @Inherited} 超类注解。
		 * <p>此策略仅在与 {@link Class} 类型一起使用时才真正有用，因为
		 * {@link Inherited @Inherited} 注解对所有其他
		 * {@linkplain AnnotatedElement 带注解的元素} 都被忽略。
		 * <p>此策略不搜索已实现的接口。
		 */
		INHERITED_ANNOTATIONS,

		/**
		 * 查找所有直接声明和超类注解。
		 * <p>此策略类似于 {@link #INHERITED_ANNOTATIONS}，
		 * 区别在于注解不需要用 {@link Inherited @Inherited} 进行元注解。
		 * <p>此策略不搜索已实现的接口。
		 */
		SUPERCLASS,

		/**
		 * 对整个类型层次结构执行完整搜索，包括超类和已实现的接口。
		 * <p>超类注解不需要用 {@link Inherited @Inherited} 进行元注解。
		 */
		TYPE_HIERARCHY,

		/**
		 * 对源<em>和</em>任何封闭类上的整个类型层次结构执行完整搜索。
		 * <p>此策略类似于 {@link #TYPE_HIERARCHY}，
		 * 区别在于还会搜索 {@linkplain Class#getEnclosingClass() 封闭类}。
		 * <p>超类和封闭类注解不需要用 {@link Inherited @Inherited} 进行元注解。
		 * <p>当搜索 {@link Method} 源时，此策略与 {@link #TYPE_HIERARCHY} 相同。
		 * <p><strong>警告：</strong>此策略会递归搜索任何源类型（无论源类型是
		 * <em>内部类</em>、{@code static} 嵌套类还是嵌套接口）的封闭类上的注解。
		 * 因此，它可能会找到比您预期更多的注解。
		 */
		TYPE_HIERARCHY_AND_ENCLOSING_CLASSES

	}

}
