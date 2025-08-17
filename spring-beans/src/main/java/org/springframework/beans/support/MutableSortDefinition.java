/*
 * Copyright 2002-2012 the original author or authors.
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

import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import java.io.Serializable;

/**
 * {@link SortDefinition} 接口的可变实现。
 * 支持在再次设置相同属性时切换升序标志。
 *
 * @author Juergen Hoeller
 * @author Jean-Pierre Pawlak
 * @since 26.05.2003
 * @see #setToggleAscendingOnProperty
 */
@SuppressWarnings("serial")
public class MutableSortDefinition implements SortDefinition, Serializable {

	private String property = "";

	private boolean ignoreCase = true;

	private boolean ascending = true;

	private boolean toggleAscendingOnProperty = false;


	/**
	 * 创建一个空的 MutableSortDefinition，
	 * 通过其 bean 属性进行填充。
	 * @see #setProperty
	 * @see #setIgnoreCase
	 * @see #setAscending
	 */
	public MutableSortDefinition() {
	}

	/**
	 * 拷贝构造函数：创建一个新的 MutableSortDefinition，
	 * 镜像给定的排序定义。
	 * @param source 原始的排序定义
	 */
	public MutableSortDefinition(SortDefinition source) {
		this.property = source.getProperty();
		this.ignoreCase = source.isIgnoreCase();
		this.ascending = source.isAscending();
	}

	/**
	 * 根据给定设置创建 MutableSortDefinition。
	 * @param property 要比较的属性
	 * @param ignoreCase 是否在 String 值比较时忽略大小写
	 * @param ascending 是否升序排序（true 表示升序，false 表示降序）
	 */
	public MutableSortDefinition(String property, boolean ignoreCase, boolean ascending) {
		this.property = property;
		this.ignoreCase = ignoreCase;
		this.ascending = ascending;
	}

	/**
	 * 创建一个新的 MutableSortDefinition。
	 * @param toggleAscendingOnSameProperty 当再次设置同一属性时是否切换升序标志
	 * （即 {@code setProperty} 被调用且属性名与当前已设置的相同时）
	 */
	public MutableSortDefinition(boolean toggleAscendingOnSameProperty) {
		this.toggleAscendingOnProperty = toggleAscendingOnSameProperty;
	}


	/**
	 * 设置要比较的属性。
	 * <p>如果属性与当前属性相同，并且 "toggleAscendingOnProperty" 被激活，则排序顺序会被反转，
	 * 否则仅忽略该设置。
	 * @see #setToggleAscendingOnProperty
	 */
	public void setProperty(String property) {
		if (!StringUtils.hasLength(property)) {
			this.property = "";
		}
		else {
			// 是否隐式切换升序标志？
			if (isToggleAscendingOnProperty()) {
				this.ascending = (!property.equals(this.property) || !this.ascending);
			}
			this.property = property;
		}
	}

	@Override
	public String getProperty() {
		return this.property;
	}

	/**
	 * 设置在 String 值比较时是否忽略大小写。
	 */
	public void setIgnoreCase(boolean ignoreCase) {
		this.ignoreCase = ignoreCase;
	}

	@Override
	public boolean isIgnoreCase() {
		return this.ignoreCase;
	}

	/**
	 * 设置排序顺序：升序为 true，降序为 false。
	 */
	public void setAscending(boolean ascending) {
		this.ascending = ascending;
	}

	@Override
	public boolean isAscending() {
		return this.ascending;
	}

	/**
	 * 设置当再次设置同一属性时是否切换升序标志
	 * （即 {@link #setProperty} 被调用，且属性名与当前已设置的相同时）。
	 * <p>这对于通过 Web 请求进行参数绑定特别有用，例如再次点击字段标题时
	 * 可能希望对同一字段以相反顺序重新排序。
	 */
	public void setToggleAscendingOnProperty(boolean toggleAscendingOnProperty) {
		this.toggleAscendingOnProperty = toggleAscendingOnProperty;
	}

	/**
	 * 返回当再次设置同一属性时是否切换升序标志
	 * （即 {@code setProperty} 被调用，且属性名与当前已设置的相同时）。
	 */
	public boolean isToggleAscendingOnProperty() {
		return this.toggleAscendingOnProperty;
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof SortDefinition)) {
			return false;
		}
		SortDefinition otherSd = (SortDefinition) other;
		return (getProperty().equals(otherSd.getProperty()) &&
				isAscending() == otherSd.isAscending() &&
				isIgnoreCase() == otherSd.isIgnoreCase());
	}

	@Override
	public int hashCode() {
		int hashCode = getProperty().hashCode();
		hashCode = 29 * hashCode + (isIgnoreCase() ? 1 : 0);
		hashCode = 29 * hashCode + (isAscending() ? 1 : 0);
		return hashCode;
	}

}
