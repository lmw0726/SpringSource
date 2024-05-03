/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.servlet.tags.form;

import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.servlet.support.BindStatus;

import java.beans.PropertyEditor;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * SelectedValueComparator 是一个抽象类，用于测试候选值是否与绑定值匹配。
 * 它尝试通过多种途径来证明比较结果，以处理诸如实例不等式、基于字符串表示的逻辑相等和基于 PropertyEditor 的比较等问题。
 *
 * <p>该类提供了对数组、集合和映射的完整支持。
 *
 * <p><h1><a name="equality-contract">相等性契约</a></h1>
 * 对于单一值对象，首先使用标准的 Java 相等性进行比较。因此，用户代码应尽力实现 {@link Object#equals} 方法以加速比较过程。
 * 如果 {@link Object#equals} 返回 false，则尝试使用 {@link #exhaustiveCompare exhaustive comparison} 进行比较，
 * 目的是<strong>证明</strong>相等而不是否定它。
 *
 * <p>接下来，尝试比较候选值和绑定值的 {@link String} 表示形式。这可能会因为两个值都表示为 {@link String} 而返回 true。
 *
 * <p>接下来，如果候选值是 {@link String}，则尝试将绑定值与应用到候选值的相应 {@link PropertyEditor} 的结果进行比较。
 * 这种比较可能会执行两次，一次是对直接的 {@link String} 实例，然后是对 {@link String} 表示形式（如果第一次比较结果为 false）。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 */
abstract class SelectedValueComparator {

	/**
	 * 如果提供的候选值与绑定到提供的 {@link BindStatus} 的值相等，则返回 true。
	 * 在这种情况下，相等性与标准的 Java 相等性不同，并在此处详细描述：<a href="#equality-contract">here</a>。
	 *
	 * @param bindStatus     绑定状态
	 * @param candidateValue 候选值
	 * @return 如果提供的候选值与绑定到提供的 {@link BindStatus} 的值相等，则返回 true
	 */
	public static boolean isSelected(BindStatus bindStatus, @Nullable Object candidateValue) {
		// 检查与候选值明显相等的匹配，首先与渲染值，然后与原始值。
		Object boundValue = bindStatus.getValue();
		if (ObjectUtils.nullSafeEquals(boundValue, candidateValue)) {
			// 如果绑定值与候选值相等，则返回true
			return true;
		}
		// 获取实际的值
		Object actualValue = bindStatus.getActualValue();
		if (actualValue != null && actualValue != boundValue &&
				ObjectUtils.nullSafeEquals(actualValue, candidateValue)) {
			// 如果实际值存在，实际值与绑定至不同，并且实际值与候选值相等，返回true
			return true;
		}
		if (actualValue != null) {
			// 如果实际值存在，将绑定值设置为实际值
			boundValue = actualValue;
		} else if (boundValue == null) {
			// 如果绑定值不存在，返回false
			return false;
		}

		// 非空值但没有明显的与候选值相等：
		// 进入更详细的比较。
		boolean selected = false;
		if (candidateValue != null) {
			// 如果候选值存在
			if (boundValue.getClass().isArray()) {
				// 如果绑定值是第一个数组，转为列表，进行集合比较。
				selected = collectionCompare(CollectionUtils.arrayToList(boundValue), candidateValue, bindStatus);
			} else if (boundValue instanceof Collection) {
				// 如果绑定值是一个集合，直接进行集合比较
				selected = collectionCompare((Collection<?>) boundValue, candidateValue, bindStatus);
			} else if (boundValue instanceof Map) {
				// 如果绑定值是一个映射，进行映射比较
				selected = mapCompare((Map<?, ?>) boundValue, candidateValue, bindStatus);
			}
		}
		if (!selected) {
			// 如果没有被选中，进行穷举比较
			selected = exhaustiveCompare(boundValue, candidateValue, bindStatus.getEditor(), null);
		}
		return selected;
	}

	private static boolean collectionCompare(
			Collection<?> boundCollection, Object candidateValue, BindStatus bindStatus) {
		try {
			if (boundCollection.contains(candidateValue)) {
				// 如果绑定集合中包含候选元素，返回true
				return true;
			}
		} catch (ClassCastException ex) {
			// 可能是来自 TreeSet - 忽略。
		}
		// 进行穷举集合比较
		return exhaustiveCollectionCompare(boundCollection, candidateValue, bindStatus);
	}

	private static boolean mapCompare(Map<?, ?> boundMap, Object candidateValue, BindStatus bindStatus) {
		try {
			if (boundMap.containsKey(candidateValue)) {
				// 如果绑定映射中的键包含该候选值，返回true
				return true;
			}
		} catch (ClassCastException ex) {
			// 可能是来自 TreeMap - 忽略。
		}
		// 进行穷举集合比较
		return exhaustiveCollectionCompare(boundMap.keySet(), candidateValue, bindStatus);
	}

	private static boolean exhaustiveCollectionCompare(
			Collection<?> collection, Object candidateValue, BindStatus bindStatus) {

		Map<PropertyEditor, Object> convertedValueCache = new HashMap<>();
		PropertyEditor editor = null;
		// 判断候选值是否为字符串类型
		boolean candidateIsString = (candidateValue instanceof String);
		if (!candidateIsString) {
			// 如果候选值不是字符串类型，则尝试找到与其类型匹配的PropertyEditor
			editor = bindStatus.findEditor(candidateValue.getClass());
		}
		// 遍历集合中的每个元素
		for (Object element : collection) {
			// 如果editor为null，且当前元素不为null，且候选值为字符串类型
			if (editor == null && element != null && candidateIsString) {
				//  尝试找到与当前元素类型匹配的PropertyEditor
				editor = bindStatus.findEditor(element.getClass());
			}
			// 穷举对比当前元素和候选值，如果匹配则返回true
			if (exhaustiveCompare(element, candidateValue, editor, convertedValueCache)) {
				return true;
			}
		}
		// 遍历结束仍未找到匹配项，返回false
		return false;
	}

	private static boolean exhaustiveCompare(@Nullable Object boundValue, @Nullable Object candidate,
											 @Nullable PropertyEditor editor, @Nullable Map<PropertyEditor, Object> convertedValueCache) {

		// 获取候选值的显示字符串表示形式
		String candidateDisplayString = ValueFormatter.getDisplayString(candidate, editor, false);
		if (boundValue != null && boundValue.getClass().isEnum()) {
			// 如果绑定的值不为null，且是枚举类型
			Enum<?> boundEnum = (Enum<?>) boundValue;
			// 获取枚举常量的名称并转换为字符串
			String enumCodeAsString = ObjectUtils.getDisplayString(boundEnum.name());
			// 如果枚举常量的名称与候选值的显示字符串相等，则返回true
			if (enumCodeAsString.equals(candidateDisplayString)) {
				return true;
			}
			// 获取枚举常量的标签并转换为字符串
			String enumLabelAsString = ObjectUtils.getDisplayString(boundEnum.toString());
			// 如果枚举常量的标签与候选值的显示字符串相等，则返回true
			if (enumLabelAsString.equals(candidateDisplayString)) {
				return true;
			}
		} else if (ObjectUtils.getDisplayString(boundValue).equals(candidateDisplayString)) {
			// 如果绑定的值的显示字符串与候选值的显示字符串相等，则返回true
			return true;
		}

		if (editor != null && candidate instanceof String) {
			// 如果 属性编辑器 不为null，且候选值是字符串类型
			// 尝试使用 属性编辑器 进行比较（ 属性编辑器 不应该创建线程）
			String candidateAsString = (String) candidate;
			Object candidateAsValue;
			if (convertedValueCache != null && convertedValueCache.containsKey(editor)) {
				// 如果 转换值缓存 不为null，且包含当前属性编辑器，则直接从缓存中获取候选值
				candidateAsValue = convertedValueCache.get(editor);
			} else {
				// 否则，将候选值设置到 属性编辑器 中，并获取其值
				editor.setAsText(candidateAsString);
				candidateAsValue = editor.getValue();
				// 如果 转换值缓存 不为null，则将结果缓存起来
				if (convertedValueCache != null) {
					convertedValueCache.put(editor, candidateAsValue);
				}
			}
			// 如果绑定的值与候选值相等，则返回true
			if (ObjectUtils.nullSafeEquals(boundValue, candidateAsValue)) {
				return true;
			}
		}
		// 没有找到匹配项，返回false
		return false;
	}

}
