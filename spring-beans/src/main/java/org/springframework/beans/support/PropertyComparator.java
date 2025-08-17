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

package org.springframework.beans.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.BeansException;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * PropertyComparator 对两个 bean 进行比较，
 * 通过 BeanWrapper 获取指定的 bean 属性值进行评估。
 *
 * @author Juergen Hoeller
 * @author Jean-Pierre Pawlak
 * @since 19.05.2003
 * @param <T> 此比较器可比较的对象类型
 * @see org.springframework.beans.BeanWrapper
 */
public class PropertyComparator<T> implements Comparator<T> {

	protected final Log logger = LogFactory.getLog(getClass());

	private final SortDefinition sortDefinition;


	/**
	 * 使用给定的 SortDefinition 创建一个新的 PropertyComparator。
	 * @see MutableSortDefinition
	 */
	public PropertyComparator(SortDefinition sortDefinition) {
		this.sortDefinition = sortDefinition;
	}

	/**
	 * 根据给定的设置创建 PropertyComparator。
	 * @param property 要比较的属性
	 * @param ignoreCase 是否在 String 值比较中忽略大小写
	 * @param ascending 是否按升序（true）或降序（false）排序
	 */
	public PropertyComparator(String property, boolean ignoreCase, boolean ascending) {
		this.sortDefinition = new MutableSortDefinition(property, ignoreCase, ascending);
	}

	/**
	 * 返回此比较器使用的 SortDefinition。
	 */
	public final SortDefinition getSortDefinition() {
		return this.sortDefinition;
	}


	@Override
	@SuppressWarnings("unchecked")
	public int compare(T o1, T o2) {
		Object v1 = getPropertyValue(o1);
		Object v2 = getPropertyValue(o2);
		if (this.sortDefinition.isIgnoreCase() && (v1 instanceof String) && (v2 instanceof String)) {
			v1 = ((String) v1).toLowerCase();
			v2 = ((String) v2).toLowerCase();
		}

		int result;

		// 将属性值为 null 的对象放在排序结果末尾
		try {
			if (v1 != null) {
				result = (v2 != null ? ((Comparable<Object>) v1).compareTo(v2) : -1);
			}
			else {
				result = (v2 != null ? 1 : 0);
			}
		}
		catch (RuntimeException ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("Could not sort objects [" + o1 + "] and [" + o2 + "]", ex);
			}
			return 0;
		}

		return (this.sortDefinition.isAscending() ? result : -result);
	}

	/**
	 * 获取给定对象的 SortDefinition 属性值。
	 * @param obj 要获取属性值的对象
	 * @return 属性值
	 */
	@Nullable
	private Object getPropertyValue(Object obj) {
		// 如果嵌套属性不可读，则直接返回 null
		// （类似 JSTL EL）。如果属性根本不存在，则抛出异常。
		try {
			BeanWrapperImpl beanWrapper = new BeanWrapperImpl(false);
			beanWrapper.setWrappedInstance(obj);
			return beanWrapper.getPropertyValue(this.sortDefinition.getProperty());
		}
		catch (BeansException ex) {
			logger.debug("PropertyComparator could not access property - treating as null for sorting", ex);
			return null;
		}
	}


	/**
	 * 根据给定的排序定义对 List 进行排序。
	 * <p>注意：列表中的对象必须提供指定属性的 getter 方法。
	 * @param source 输入列表
	 * @param sortDefinition 排序参数
	 * @throws java.lang.IllegalArgumentException 如果缺少 propertyName
	 */
	public static void sort(List<?> source, SortDefinition sortDefinition) throws BeansException {
		if (StringUtils.hasText(sortDefinition.getProperty())) {
			source.sort(new PropertyComparator<>(sortDefinition));
		}
	}

	/**
	 * 根据给定的排序定义对数组进行排序。
	 * <p>注意：数组中的对象必须提供指定属性的 getter 方法。
	 * @param source 输入数组
	 * @param sortDefinition 排序参数
	 * @throws java.lang.IllegalArgumentException 如果缺少 propertyName
	 */
	public static void sort(Object[] source, SortDefinition sortDefinition) throws BeansException {
		if (StringUtils.hasText(sortDefinition.getProperty())) {
			Arrays.sort(source, new PropertyComparator<>(sortDefinition));
		}
	}

}
