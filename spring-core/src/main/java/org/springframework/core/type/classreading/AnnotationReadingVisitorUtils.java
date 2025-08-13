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

package org.springframework.core.type.classreading;

import org.springframework.asm.Type;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.ObjectUtils;

import java.util.*;

/**
 * 通过 ASM 读取注解时使用的内部工具类。
 *
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author Costin Leau
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 4.0
 * @deprecated 自 Spring Framework 5.2 起，此类及本包中相关类
 * 已被 {@link SimpleAnnotationMetadataReadingVisitor} 及其相关类替代，
 * 仅供框架内部使用。
 */
@Deprecated
abstract class AnnotationReadingVisitorUtils {

	public static AnnotationAttributes convertClassValues(Object annotatedElement,
														  @Nullable ClassLoader classLoader, AnnotationAttributes original, boolean classValuesAsString) {

		AnnotationAttributes result = new AnnotationAttributes(original);
		AnnotationUtils.postProcessAnnotationAttributes(annotatedElement, result, classValuesAsString);

		for (Map.Entry<String, Object> entry : result.entrySet()) {
			try {
				Object value = entry.getValue();
				if (value instanceof AnnotationAttributes) {
					value = convertClassValues(
							annotatedElement, classLoader, (AnnotationAttributes) value, classValuesAsString);
				}
				else if (value instanceof AnnotationAttributes[]) {
					AnnotationAttributes[] values = (AnnotationAttributes[]) value;
					for (int i = 0; i < values.length; i++) {
						values[i] = convertClassValues(annotatedElement, classLoader, values[i], classValuesAsString);
					}
					value = values;
				}
				else if (value instanceof Type) {
					value = (classValuesAsString ? ((Type) value).getClassName() :
							ClassUtils.forName(((Type) value).getClassName(), classLoader));
				}
				else if (value instanceof Type[]) {
					Type[] array = (Type[]) value;
					Object[] convArray =
							(classValuesAsString ? new String[array.length] : new Class<?>[array.length]);
					for (int i = 0; i < array.length; i++) {
						convArray[i] = (classValuesAsString ? array[i].getClassName() :
								ClassUtils.forName(array[i].getClassName(), classLoader));
					}
					value = convArray;
				}
				else if (classValuesAsString) {
					if (value instanceof Class) {
						value = ((Class<?>) value).getName();
					}
					else if (value instanceof Class[]) {
						Class<?>[] clazzArray = (Class<?>[]) value;
						String[] newValue = new String[clazzArray.length];
						for (int i = 0; i < clazzArray.length; i++) {
							newValue[i] = clazzArray[i].getName();
						}
						value = newValue;
					}
				}
				entry.setValue(value);
			}
			catch (Throwable ex) {
				// 类未找到 —— 无法解析注解属性中的类引用。
				result.put(entry.getKey(), ex);
			}
		}

		return result;
	}

	/**
	 * 从提供的 {@code attributesMap} 中获取指定类型注解的合并属性（如果有）。
	 * <p>在注解层级中位置 <em>较低</em>（即更接近声明类） 的注解属性值
	 * 会覆盖位置 <em>较高</em> 的注解属性值。
	 * @param attributesMap 注解属性列表的映射，键为注解类型名称
	 * @param metaAnnotationMap 元注解关系映射，键为注解类型名称
	 * @param annotationName 要查找的注解类型的全限定类名
	 * @return 合并后的注解属性；如果 {@code attributesMap} 中不存在匹配的注解则返回 {@code null}
	 * @since 4.0.3
	 */
	@Nullable
	public static AnnotationAttributes getMergedAnnotationAttributes(
			LinkedMultiValueMap<String, AnnotationAttributes> attributesMap,
			Map<String, Set<String>> metaAnnotationMap, String annotationName) {

		// 获取目标注解的未合并属性列表。
		List<AnnotationAttributes> attributesList = attributesMap.get(annotationName);
		if (CollectionUtils.isEmpty(attributesList)) {
			return null;
		}

		// 首先用目标注解的第一个属性列表拷贝填充结果，
		// 以避免意外修改传入的元数据状态。
		AnnotationAttributes result = new AnnotationAttributes(attributesList.get(0));

		Set<String> overridableAttributeNames = new HashSet<>(result.keySet());
		overridableAttributeNames.remove(AnnotationUtils.VALUE);

		// 由于使用的是 LinkedMultiValueMap，我们依赖映射中元素的顺序，
		// 并反转键的顺序以便“向下”遍历注解层级。
		List<String> annotationTypes = new ArrayList<>(attributesMap.keySet());
		Collections.reverse(annotationTypes);

		// 不必重复访问目标注解类型：
		annotationTypes.remove(annotationName);

		for (String currentAnnotationType : annotationTypes) {
			List<AnnotationAttributes> currentAttributesList = attributesMap.get(currentAnnotationType);
			if (!ObjectUtils.isEmpty(currentAttributesList)) {
				Set<String> metaAnns = metaAnnotationMap.get(currentAnnotationType);
				if (metaAnns != null && metaAnns.contains(annotationName)) {
					AnnotationAttributes currentAttributes = currentAttributesList.get(0);
					for (String overridableAttributeName : overridableAttributeNames) {
						Object value = currentAttributes.get(overridableAttributeName);
						if (value != null) {
							// 存储该值，可能覆盖注解层级中更高处相同名称的属性值。
							result.put(overridableAttributeName, value);
						}
					}
				}
			}
		}

		return result;
	}

}
