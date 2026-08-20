/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.scripting.groovy;

import groovy.lang.GroovyObject;

/**
 * {@link GroovyScriptFactory} 使用的策略，用于对创建的 {@link GroovyObject} 进行自定义。
 *
 * <p>这对于 DSL 的编写、缺失方法的替换等场景非常有用。
 * 例如，可以指定一个自定义的 {@link groovy.lang.MetaClass}。
 *
 * @author Rod Johnson
 * @since 2.0.2
 * @see GroovyScriptFactory
 */
@FunctionalInterface
public interface GroovyObjectCustomizer {

	/**
	 * 对提供的 {@link GroovyObject} 进行自定义。
	 * <p>例如，可以用于设置自定义的元类（metaclass）来处理缺失的方法。
	 * @param goo 要自定义的 {@code GroovyObject}
	 */
	void customize(GroovyObject goo);

}
