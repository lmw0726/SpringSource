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

package org.springframework.beans.factory.parsing;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Representation of an alias that has been registered during the parsing process.
 *
 * @author Juergen Hoeller
 * @see ReaderEventListener#aliasRegistered(AliasDefinition)
 * @since 2.0
 */
public class AliasDefinition implements BeanMetadataElement {

	/**
	 * bean名称
	 */
	private final String beanName;

	/**
	 * 别名
	 */
	private final String alias;

	/**
	 * 数据源
	 */
	@Nullable
	private final Object source;


	/**
	 * 创建新的AliasDefinition。
	 *
	 * @param beanName bean的规范名称
	 * @param alias    为bean注册的别名
	 */
	public AliasDefinition(String beanName, String alias) {
		this(beanName, alias, null);
	}

	/**
	 * 创建新的AliasDefinition。
	 *
	 * @param beanName bean的规范名称
	 * @param alias    为bean注册的别名
	 * @param source   源对象 (可能是 {@code null})
	 */
	public AliasDefinition(String beanName, String alias, @Nullable Object source) {
		Assert.notNull(beanName, "Bean name must not be null");
		Assert.notNull(alias, "Alias must not be null");
		this.beanName = beanName;
		this.alias = alias;
		this.source = source;
	}


	/**
	 * 返回bean的规范名称。
	 */
	public final String getBeanName() {
		return this.beanName;
	}

	/**
	 * 返回为bean注册的别名。
	 */
	public final String getAlias() {
		return this.alias;
	}

	@Override
	@Nullable
	public final Object getSource() {
		return this.source;
	}

}
