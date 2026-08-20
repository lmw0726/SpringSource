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

package org.springframework.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 当多个候选 Bean 都满足自动注入单一值依赖的条件时，表示应优先选择该 Bean。
 * 如果候选者中恰好存在一个"主"（primary）Bean，则该 Bean 将作为自动注入的值。
 *
 * <p>该注解在语义上等同于 Spring XML 配置中 {@code <bean>} 元素的
 * {@code primary} 属性。
 *
 * <p>可直接或间接用于任何标注了 {@code @Component} 的类上，
 * 也可用于标注了 @{@link Bean} 的方法上。
 *
 * <h2>示例</h2>
 * <pre class="code">
 * &#064;Component
 * public class FooService {
 *
 *     private FooRepository fooRepository;
 *
 *     &#064;Autowired
 *     public FooService(FooRepository fooRepository) {
 *         this.fooRepository = fooRepository;
 *     }
 * }
 *
 * &#064;Component
 * public class JdbcFooRepository extends FooRepository {
 *
 *     public JdbcFooRepository(DataSource dataSource) {
 *         // ...
 *     }
 * }
 *
 * &#064;Primary
 * &#064;Component
 * public class HibernateFooRepository extends FooRepository {
 *
 *     public HibernateFooRepository(SessionFactory sessionFactory) {
 *         // ...
 *     }
 * }
 * </pre>
 *
 * <p>由于 {@code HibernateFooRepository} 标注了 {@code @Primary}，
 * 在同一个 Spring 应用上下文中同时存在该 Bean 和基于 JDBC 的变体时，
 * 将优先注入 {@code HibernateFooRepository}。当大量使用组件扫描时，
 * 这种情况很常见。
 *
 * <p>请注意，在类级别使用 {@code @Primary} 仅在使用组件扫描时才生效。
 * 如果一个标注了 {@code @Primary} 的类是通过 XML 声明的，
 * 则 {@code @Primary} 注解的元数据将被忽略，取而代之的是
 * {@code <bean primary="true|false"/>} 的配置。
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.0
 * @see Lazy
 * @see Bean
 * @see ComponentScan
 * @see org.springframework.stereotype.Component
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Primary {

}
