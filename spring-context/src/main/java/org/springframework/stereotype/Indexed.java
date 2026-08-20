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

package org.springframework.stereotype;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 表示被注解的元素代表了索引的一个 stereotype（类型分类）。
 *
 * <p>{@code CandidateComponentsIndex} 是类路径扫描的一种替代方案，
 * 它使用在编译时生成的元数据文件。该索引允许根据 stereotype 检索候选组件
 * （即全限定类名）。此注解指示生成器对被注解元素所在的元素进行索引，
 * 或对实现了或继承了被注解元素的类进行索引。stereotype 就是被注解元素的
 * 全限定名。
 *
 * <p>考虑默认的 {@link Component} 注解，它被元注解了此注解。
 * 如果一个组件被 {@link Component} 注解，则该组件的条目将以
 * {@code org.springframework.stereotype.Component} 作为 stereotype
 * 添加到索引中。
 *
 * <p>此注解也会作用于元注解。考虑以下自定义注解：
 * <pre class="code">
 * package com.example;
 *
 * &#064;Target(ElementType.TYPE)
 * &#064;Retention(RetentionPolicy.RUNTIME)
 * &#064;Documented
 * &#064;Indexed
 * &#064;Service
 * public @interface PrivilegedService { ... }
 * </pre>
 *
 * 如果上述注解出现在某个类型上，它将以两个 stereotype 进行索引：
 * {@code org.springframework.stereotype.Component} 和
 * {@code com.example.PrivilegedService}。虽然 {@link Service} 没有直接
 * 被 {@code Indexed} 注解，但它被元注解了 {@link Component}。
 *
 * <p>也可以通过在某个接口或类上添加 {@code @Indexed} 来索引该接口的
 * 所有实现类或该类的所有子类。
 *
 * 考虑以下基础接口：
 * <pre class="code">
 * package com.example;
 *
 * &#064;Indexed
 * public interface AdminService { ... }
 * </pre>
 *
 * 现在，考虑某处对 {@code AdminService} 的一个实现：
 * <pre class="code">
 * package com.example.foo;
 *
 * import com.example.AdminService;
 *
 * public class ConfigurationAdminService implements AdminService { ... }
 * </pre>
 *
 * 因为该类实现了一个被索引的接口，它将自动以
 * {@code com.example.AdminService} 作为 stereotype 被包含在索引中。
 * 如果层次结构中还有更多 {@code @Indexed} 的接口和/或超类，
 * 该类将映射到它们所有的 stereotype。
 *
 * @author Stephane Nicoll
 * @since 5.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Indexed {
}
