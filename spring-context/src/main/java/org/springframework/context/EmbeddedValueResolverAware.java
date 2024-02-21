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

package org.springframework.context;

import org.springframework.beans.factory.Aware;
import org.springframework.util.StringValueResolver;

/**
 * 任何希望被通知用于解析嵌入式定义值的StringValueResolver的对象都必须实现的接口。
 * <p>
 * 这是通过ApplicationContextAware/BeanFactoryAware接口的一种替代方式，而不是通过完整的ConfigurableBeanFactory依赖。
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @see org.springframework.beans.factory.config.ConfigurableBeanFactory#resolveEmbeddedValue(String)
 * @see org.springframework.beans.factory.config.ConfigurableBeanFactory#getBeanExpressionResolver()
 * @see org.springframework.beans.factory.config.EmbeddedValueResolver
 * @since 3.0.3
 */
public interface EmbeddedValueResolverAware extends Aware {

	/**
	 * 设置StringValueResolver用于解析嵌入式定义值。
	 *
	 * @param resolver 字符串值解析器
	 */
	void setEmbeddedValueResolver(StringValueResolver resolver);

}
