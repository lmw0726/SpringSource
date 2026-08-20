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

package org.springframework.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 当前 Bean 所依赖的 Bean。指定的任何 Bean 都保证在当前 Bean 之前由容器创建。
 * 在 Bean 不通过属性或构造函数参数显式依赖另一个 Bean，而是依赖另一个 Bean
 * 初始化的副作用的情况下，偶尔会使用此注解。
 *
 * <p>depends-on 声明可以指定初始化时的依赖关系，并且（仅适用于单例 Bean）
 * 还可以指定相应的销毁时依赖关系。与给定 Bean 定义了 depends-on 关系的
 * 依赖 Bean 会在给定 Bean 本身被销毁之前先被销毁。因此，depends-on 声明
 * 还可以控制关闭顺序。
 *
 * <p>可以用于任何直接或间接使用 {@link org.springframework.stereotype.Component}
 * 注解的类，或用于使用 {@link Bean} 注解的方法。
 *
 * <p>在类级别使用 {@link DependsOn} 没有效果，除非正在使用组件扫描。如果通过
 * XML 声明了 {@link DependsOn} 注解的类，则忽略 {@link DependsOn} 注解元数据，
 * 改为使用 {@code <bean depends-on="..."/>}。
 *
 * @author Juergen Hoeller
 * @since 3.0
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DependsOn {


	String[] value() default {};

}
