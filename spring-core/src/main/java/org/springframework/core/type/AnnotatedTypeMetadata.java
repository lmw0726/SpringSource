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

import org.springframework.core.annotation.*;
import org.springframework.core.annotation.MergedAnnotation.Adapt;
import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;

import java.lang.annotation.Annotation;
import java.util.Map;

/**
 * 定义对特定类型（{@link AnnotationMetadata 类}或{@link MethodMetadata 方法}）注解的访问接口，
 * 以一种不一定需要类加载的形式进行访问。
 *
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author Mark Pollack
 * @author Chris Beams
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 4.0
 * @see AnnotationMetadata
 * @see MethodMetadata
 */
public interface AnnotatedTypeMetadata {

	/**
	 * 根据底层元素的直接注解，返回合并后的注解信息。
	 * @return 基于直接注解的合并注解
	 * @since 5.2
	 */
	MergedAnnotations getAnnotations();

	/**
	 * 判断底层元素是否定义了指定类型的注解或元注解。
	 * <p>如果此方法返回 {@code true}，则 {@link #getAnnotationAttributes} 会返回非空的 Map。
	 * @param annotationName 要查找的注解类型的全限定类名
	 * @return 是否定义了匹配的注解
	 */
	default boolean isAnnotated(String annotationName) {
		// 是否存在指定的注解名称
		return getAnnotations().isPresent(annotationName);
	}

	/**
	 * 获取指定类型注解的属性（如果存在，即直接注解或元注解定义在底层元素上），
	 * 也会考虑复合注解中的属性覆盖。
	 * @param annotationName 要查找的注解类型的全限定类名
	 * @return 属性Map，键为属性名（例如"value"），值为对应的属性值；
	 *         如果无匹配注解则返回 {@code null}
	 */
	@Nullable
	default Map<String, Object> getAnnotationAttributes(String annotationName) {
		return getAnnotationAttributes(annotationName, false);
	}

	/**
	 * 获取指定类型注解的属性（如果存在，即直接注解或元注解定义在底层元素上），
	 * 也会考虑复合注解中的属性覆盖。
	 * @param annotationName 要查找的注解类型的全限定类名
	 * @param classValuesAsString 是否将类引用转换为字符串类名，以避免先加载类
	 * @return 属性Map，键为属性名（例如"value"），值为对应的属性值；
	 *         如果无匹配注解则返回 {@code null}
	 */
	@Nullable
	default Map<String, Object> getAnnotationAttributes(String annotationName,
			boolean classValuesAsString) {
		// 从注解中获取指定名称的直接声明注解
		MergedAnnotation<Annotation> annotation = getAnnotations().get(annotationName,
				null, MergedAnnotationSelectors.firstDirectlyDeclared());
		if (!annotation.isPresent()) {
			return null;
		}
		return annotation.asAnnotationAttributes(Adapt.values(classValuesAsString, true));
	}

	/**
	 * 获取指定类型注解的所有属性集合（如果存在，即直接注解或元注解定义在底层元素上）。
	 * 注意：此方法不考虑属性覆盖。
	 * @param annotationName 要查找的注解类型的全限定类名
	 * @return 多值Map，键为属性名（例如"value"），值为对应的属性值列表；
	 *         如果无匹配注解则返回 {@code null}
	 * @see #getAllAnnotationAttributes(String, boolean)
	 */
	@Nullable
	default MultiValueMap<String, Object> getAllAnnotationAttributes(String annotationName) {
		return getAllAnnotationAttributes(annotationName, false);
	}

	/**
	 * 获取指定类型注解的所有属性集合（如果存在，即直接注解或元注解定义在底层元素上）。
	 * 注意：此方法不考虑属性覆盖。
	 * @param annotationName 要查找的注解类型的全限定类名
	 * @param classValuesAsString 是否将类引用转换为字符串类名
	 * @return 多值Map，键为属性名（例如"value"），值为对应的属性值列表；
	 *         如果无匹配注解则返回 {@code null}
	 * @see #getAllAnnotationAttributes(String)
	 */
	@Nullable
	default MultiValueMap<String, Object> getAllAnnotationAttributes(
			String annotationName, boolean classValuesAsString) {

		Adapt[] adaptations = Adapt.values(classValuesAsString, true);
		return getAnnotations().stream(annotationName)
				.filter(MergedAnnotationPredicates.unique(MergedAnnotation::getMetaTypes))
				.map(MergedAnnotation::withNonMergedAttributes)
				.collect(MergedAnnotationCollectors.toMultiValueMap(map ->
						map.isEmpty() ? null : map, adaptations));
	}

}
