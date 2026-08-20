/*
 * Copyright 2002-2014 the original author or authors.
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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 聚合多个 {@link PropertySource} 注解的容器注解。
 *
 * <p>可以直接使用，声明多个嵌套的 {@link PropertySource} 注解。
 * 也可以与 Java 8 的<em>可重复注解</em>支持结合使用，
 * 即在同一个 {@linkplain ElementType#TYPE 类型}上多次声明 {@link PropertySource}，
 * 从而隐式地生成此容器注解。
 *
 * @author Phillip Webb
 * @since 4.0
 * @see PropertySource
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PropertySources {

	PropertySource[] value();

}
