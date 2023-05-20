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

package org.springframework.beans.factory.xml;

import org.springframework.beans.factory.parsing.DefaultsDefinition;
import org.springframework.lang.Nullable;

/**
 * Simple JavaBean that holds the defaults specified at the {@code <beans>}
 * level in a standard Spring XML bean definition document:
 * {@code default-lazy-init}, {@code default-autowire}, etc.
 *
 * @author Juergen Hoeller
 * @since 2.0.2
 */
public class DocumentDefaultsDefinition implements DefaultsDefinition {
	/**
	 * 是否懒加载
	 */
	@Nullable
	private String lazyInit;
	/**
	 * 是否合并设置
	 */
	@Nullable
	private String merge;
	/**
	 * 自动装配
	 */
	@Nullable
	private String autowire;
	/**
	 * 自动装配勾选者
	 */
	@Nullable
	private String autowireCandidates;
	/**
	 * 初始化方法名称
	 */
	@Nullable
	private String initMethod;
	/**
	 * 销毁方法名称
	 */
	@Nullable
	private String destroyMethod;
	/**
	 * 元数据元素的配置源
	 */
	@Nullable
	private Object source;


	/**
	 * 为当前解析的文档设置默认的lazy-init标志。
	 */
	public void setLazyInit(@Nullable String lazyInit) {
		this.lazyInit = lazyInit;
	}

	/**
	 * 返回当前解析的文档的默认lazy-init标志。
	 */
	@Nullable
	public String getLazyInit() {
		return this.lazyInit;
	}

	/**
	 * 为当前解析的文档设置默认合并设置。
	 */
	public void setMerge(@Nullable String merge) {
		this.merge = merge;
	}

	/**
	 * 返回当前解析的文档的默认合并设置。
	 */
	@Nullable
	public String getMerge() {
		return this.merge;
	}

	/**
	 * 为当前解析的文档设置默认的自动装配设置。
	 */
	public void setAutowire(@Nullable String autowire) {
		this.autowire = autowire;
	}

	/**
	 * 返回当前解析的文档的默认自动装配设置。
	 */
	@Nullable
	public String getAutowire() {
		return this.autowire;
	}

	/**
	 * 为当前解析的文档设置默认的自动装配候选模式。也接受以逗号分隔的模式列表。
	 */
	public void setAutowireCandidates(@Nullable String autowireCandidates) {
		this.autowireCandidates = autowireCandidates;
	}

	/**
	 * 返回当前解析的文档的默认自动装配候选模式。也可以返回逗号分隔的模式列表。
	 */
	@Nullable
	public String getAutowireCandidates() {
		return this.autowireCandidates;
	}

	/**
	 * 为当前解析的文档设置默认的init-method设置。
	 */
	public void setInitMethod(@Nullable String initMethod) {
		this.initMethod = initMethod;
	}

	/**
	 * 返回当前解析的文档的默认init-method设置。
	 */
	@Nullable
	public String getInitMethod() {
		return this.initMethod;
	}

	/**
	 * 为当前解析的文档设置默认的destroy-method设置。
	 */
	public void setDestroyMethod(@Nullable String destroyMethod) {
		this.destroyMethod = destroyMethod;
	}

	/**
	 * 返回当前解析的文档的默认destroy-method设置。
	 */
	@Nullable
	public String getDestroyMethod() {
		return this.destroyMethod;
	}

	/**
	 * 设置此元数据元素的配置源 {@code Object}。
	 * <p> 对象的确切类型将取决于所使用的配置机制。
	 */
	public void setSource(@Nullable Object source) {
		this.source = source;
	}

	@Override
	@Nullable
	public Object getSource() {
		return this.source;
	}

}
