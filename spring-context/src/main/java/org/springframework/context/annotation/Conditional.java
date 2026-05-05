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

package org.springframework.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 表示一个组件只有在所有{@linkplain #value 指定的条件}都匹配时才有资格进行注册。
 *
 * <p><em>条件</em>是指任何可以在bean定义即将注册之前以编程方式确定的状态
 * （详见{@link Condition}）。
 *
 * <p>{@code @Conditional} 注解可以通过以下任意方式使用：
 * <ul>
 * <li>作为类型级别的注解，用于任何直接或间接使用{@code @Component}注解的类，
 * 包括{@link Configuration @Configuration}类</li>
 * <li>作为元注解，用于组合自定义的构造型注解</li>
 * <li>作为方法级别的注解，用于任何{@link Bean @Bean}方法</li>
 * </ul>
 *
 * <p>如果一个{@code @Configuration}类被标记为{@code @Conditional}，
 * 那么与该类关联的所有{@code @Bean}方法、{@link Import @Import}注解和
 * {@link ComponentScan @ComponentScan}注解都将受到这些条件的约束。
 *
 * <p><strong>注意</strong>：不支持{@code @Conditional}注解的继承；
 * 来自父类或被重写方法的任何条件都不会被考虑。为了强制执行这些语义，
 * {@code @Conditional}本身没有声明为
 * {@link java.lang.annotation.Inherited @Inherited}；此外，任何
 * 使用{@code @Conditional}进行元注解的自定义<em>组合注解</em>
 * 都不能声明为{@code @Inherited}。
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 4.0
 * @see Condition
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Conditional {

	/**
	 * 所有必须{@linkplain Condition#matches 匹配}的{@link Condition}类，
	 * 只有当这些条件都满足时，组件才会被注册。
	 */
	Class<? extends Condition>[] value();

}
