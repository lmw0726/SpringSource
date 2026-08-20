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

package org.springframework.context.annotation;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.util.Assert;

/**
 * 描述 Spring 管理的 Bean 的作用域（Scope）特征，包括作用域名称和作用域代理（scoped-proxy）行为。
 *
 * <p>默认作用域为 "singleton"，默认情况下 <i>不</i> 创建作用域代理。
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @since 2.5
 * @see ScopeMetadataResolver
 * @see ScopedProxyMode
 */
public class ScopeMetadata {
	//默认就是Singleton级别的Scope
	private String scopeName = BeanDefinition.SCOPE_SINGLETON;

	private ScopedProxyMode scopedProxyMode = ScopedProxyMode.NO;


	/**
	 * 设置作用域名称。
	 */
	public void setScopeName(String scopeName) {
		Assert.notNull(scopeName, "'scopeName' must not be null");
		this.scopeName = scopeName;
	}

	/**
	 * 获取作用域名称。
	 */
	public String getScopeName() {
		return this.scopeName;
	}

	/**
	 * 设置作用于作用域实例的代理模式。
	 */
	public void setScopedProxyMode(ScopedProxyMode scopedProxyMode) {
		Assert.notNull(scopedProxyMode, "'scopedProxyMode' must not be null");
		this.scopedProxyMode = scopedProxyMode;
	}

	/**
	 * 获取作用于作用域实例的代理模式。
	 */
	public ScopedProxyMode getScopedProxyMode() {
		return this.scopedProxyMode;
	}

}
