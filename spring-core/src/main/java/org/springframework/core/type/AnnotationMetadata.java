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

package org.springframework.core.type;

import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 定义对特定类注解的抽象访问接口，
 * 以一种不要求加载该类的形式进行访问。
 *
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 2.5
 * @see StandardAnnotationMetadata
 * @see org.springframework.core.type.classreading.MetadataReader#getAnnotationMetadata()
 * @see AnnotatedTypeMetadata
 */
public interface AnnotationMetadata extends ClassMetadata, AnnotatedTypeMetadata {

	/**
	 * 获取底层类上所有<em>存在的</em>注解类型的全限定类名集合。
	 * @return 注解类型名称集合
	 */
	default Set<String> getAnnotationTypes() {
		return getAnnotations().stream()
				.filter(MergedAnnotation::isDirectlyPresent)
				.map(annotation -> annotation.getType().getName())
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	/**
	 * 获取底层类上指定注解类型的所有<em>存在的</em>元注解类型的全限定类名集合。
	 * @param annotationName 要查找的元注解类型的全限定类名
	 * @return 元注解类型名称集合，若未找到则返回空集合
	 */
	default Set<String> getMetaAnnotationTypes(String annotationName) {
		MergedAnnotation<?> annotation = getAnnotations().get(annotationName, MergedAnnotation::isDirectlyPresent);
		if (!annotation.isPresent()) {
			return Collections.emptySet();
		}
		return MergedAnnotations.from(annotation.getType(), SearchStrategy.INHERITED_ANNOTATIONS).stream()
				.map(mergedAnnotation -> mergedAnnotation.getType().getName())
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	/**
	 * 判断底层类上是否<em>存在</em>指定类型的注解。
	 * @param annotationName 要查找的注解类型的全限定类名
	 * @return 如果存在匹配的注解，则返回 {@code true}
	 */
	default boolean hasAnnotation(String annotationName) {
		return getAnnotations().isDirectlyPresent(annotationName);
	}

	/**
	 * 判断底层类上是否存在带有指定元注解类型的注解。
	 * @param metaAnnotationName 要查找的元注解类型的全限定类名
	 * @return 如果存在匹配的元注解，则返回 {@code true}
	 */
	default boolean hasMetaAnnotation(String metaAnnotationName) {
		return getAnnotations().get(metaAnnotationName,
				MergedAnnotation::isMetaPresent).isPresent();
	}

	/**
	 * 判断底层类是否有任意方法被指定注解类型注解（或元注解）。
	 * @param annotationName 要查找的注解类型的全限定类名
	 */
	default boolean hasAnnotatedMethods(String annotationName) {
		return !getAnnotatedMethods(annotationName).isEmpty();
	}

	/**
	 * 获取所有被指定注解类型注解（或元注解）的方法的元数据集合。
	 * <p>对于返回的任意方法，{@link MethodMetadata#isAnnotated} 对给定注解类型都会返回 {@code true}。
	 * @param annotationName 要查找的注解类型的全限定类名
	 * @return 符合注解条件的方法元数据集合，如果无匹配方法则返回空集合。
	 */
	Set<MethodMetadata> getAnnotatedMethods(String annotationName);


	/**
	 * 使用标准反射为给定类创建一个新的 {@link AnnotationMetadata} 实例的工厂方法。
	 * @param type 要分析的类
	 * @return 新的 {@link AnnotationMetadata} 实例
	 * @since 5.2
	 */
	static AnnotationMetadata introspect(Class<?> type) {
		return StandardAnnotationMetadata.from(type);
	}

}
