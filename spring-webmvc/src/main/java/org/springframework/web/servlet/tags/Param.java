/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.servlet.tags;

import org.springframework.lang.Nullable;

/**
 * 用于从 {@link ParamTag} 传递名称-值对参数到 {@link ParamAware} 标签的 Bean。
 *
 * <p>属性是传递给 spring:param 标签的原始值，尚未进行编码或转义。
 *
 * @author Scott Andrews
 * @since 3.0
 * @see ParamTag
 */
public class Param {

	/**
	 * 参数名称
	 */
	@Nullable
	private String name;

	/**
	 * 参数值
	 */
	@Nullable
	private String value;


	/**
	 * 设置参数的原始名称。
	 */
	public void setName(@Nullable String name) {
		this.name = name;
	}

	/**
	 * 返回原始参数名称。
	 */
	@Nullable
	public String getName() {
		return this.name;
	}

	/**
	 * 设置参数的原始值。
	 */
	public void setValue(@Nullable String value) {
		this.value = value;
	}

	/**
	 * 返回原始参数值。
	 */
	@Nullable
	public String getValue() {
		return this.value;
	}


	@Override
	public String toString() {
		return "JSP Tag Param: name '" + this.name + "', value '" + this.value + "'";
	}

}
