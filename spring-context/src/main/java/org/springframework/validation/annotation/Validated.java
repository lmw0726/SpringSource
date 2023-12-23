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

package org.springframework.validation.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * JSR-303的{@link javax.validation.Valid}的变体，支持指定验证组。
 * 设计用于与Spring的JSR-303支持方便使用，但并非JSR-303特定。
 *
 * <p>可以与Spring MVC处理程序方法的参数一起使用。
 * 通过{@link org.springframework.validation.SmartValidator}的验证提示概念支持，
 * 其中验证组类充当提示对象。
 *
 * <p>还可用于方法级验证，指示应在方法级别验证特定类（作为相应验证拦截器的切入点），
 * 但也可在注释类中选择性地指定方法级验证的验证组。
 * 在方法级别应用此注解允许覆盖特定方法的验证组，但不充当切入点；
 * 但是，触发特定bean的方法验证仍然需要类级别的注解。
 * 还可将其用作自定义构造型注解或自定义特定组验证注解的元注解。
 *
 * @author Juergen Hoeller
 * @since 3.1
 * @see javax.validation.Validator#validate(Object, Class[])
 * @see org.springframework.validation.SmartValidator#validate(Object, org.springframework.validation.Errors, Object...)
 * @see org.springframework.validation.beanvalidation.SpringValidatorAdapter
 * @see org.springframework.validation.beanvalidation.MethodValidationPostProcessor
 */
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Validated {

	/**
	 * 指定要应用于此注解引发的验证步骤的一个或多个验证组。
	 * <p>JSR-303将验证组定义为应用程序声明的自定义注解，仅用于将它们用作类型安全的组参数，
	 * 正如{@link org.springframework.validation.beanvalidation.SpringValidatorAdapter}中实现的那样。
	 * <p>其他{@link org.springframework.validation.SmartValidator}实现也可以以其他方式支持类参数。
	 */
	Class<?>[] value() default {};

}
