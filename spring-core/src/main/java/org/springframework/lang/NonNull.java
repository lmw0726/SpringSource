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
import java.lang.annotation.*;

/**
 * Spring通用注解，用于声明被注解的元素不能为{@code null}。
 *
 * <p>利用JSR-305元注解在Java中向支持JSR-305的通用工具指示空值约束，
 * 并被Kotlin用于推断Spring API的可空性。
 *
 * <p>应在参数、返回值和字段级别使用。方法重写应重复父类的{@code @NonNull}注解，
 * 除非它们的行为不同。
 *
 * <p>使用{@code @NonNullApi}（作用域=参数+返回值）和/或{@code @NonNullFields}
 * （作用域=字段）将默认行为设置为非空，以避免在整个代码库中使用{@code @NonNull}注解。
 *
 * @author Sebastien Deleuze
 * @author Juergen Hoeller
 * @since 5.0
 * @see NonNullApi
 * @see NonNullFields
 * @see Nullable
 */
@Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Nonnull
@TypeQualifierNickname
public @interface NonNull {
}
