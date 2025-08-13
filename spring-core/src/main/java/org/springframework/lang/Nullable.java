/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.lang;

import javax.annotation.Nonnull;
import javax.annotation.meta.TypeQualifierNickname;
import javax.annotation.meta.When;
import java.lang.annotation.*;

/**
 * Spring通用注解，用于声明被注解的元素在某些情况下可能为{@code null}。
 *
 * <p>利用JSR-305元注解在Java中向支持JSR-305的通用工具指示空值约束，
 * 并被Kotlin用于推断Spring API的可空性。
 *
 * <p>应在参数、返回值和字段级别使用。方法重写应重复父类的{@code @Nullable}注解，
 * 除非它们的行为不同。
 *
 * <p>可以与{@code @NonNullApi}或{@code @NonNullFields}配合使用，
 * 以覆盖默认的非空语义为可空。
 *
 * @author Sebastien Deleuze
 * @author Juergen Hoeller
 * @since 5.0
 * @see NonNullApi
 * @see NonNullFields
 * @see NonNull
 */
@Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Nonnull(when = When.MAYBE)
@TypeQualifierNickname
public @interface Nullable {
}
