/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.jmx.export.metadata;

/**
 * 所有 JMX 元数据类的基类。
 *
 * @author Rob Harrop
 * @since 1.2
 */
public abstract class AbstractJmxAttribute {

	private String description = "";

	private int currencyTimeLimit = -1;


	/**
	 * 为该属性设置描述。
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * 返回该属性的描述。
	 */
	public String getDescription() {
		return this.description;
	}

	/**
	 * 为该属性设置时效性时间限制。
	 */
	public void setCurrencyTimeLimit(int currencyTimeLimit) {
		this.currencyTimeLimit = currencyTimeLimit;
	}

	/**
	 * 返回该属性的时效性时间限制。
	 */
	public int getCurrencyTimeLimit() {
		return this.currencyTimeLimit;
	}

}
