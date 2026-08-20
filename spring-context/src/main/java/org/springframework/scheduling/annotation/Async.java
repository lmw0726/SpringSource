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

package org.springframework.scheduling.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记方法为<i>异步</i>执行候选的注解。
 * 也可以用在类级别，此时该类的所有方法都将被视为异步方法。但请注意，{@code @Async} 不支持
 * 在
 * {@link org.springframework.context.annotation.Configuration @Configuration} 类中声明的方法上使用。
 *
 * <p>关于目标方法签名，支持任何参数类型。
 * 但返回类型限定为 {@code void} 或
 * {@link java.util.concurrent.Future}。在后一种情况下，可以声明更具体的
 * {@link org.springframework.util.concurrent.ListableFuture} 或
 * {@link java.util.concurrent.CompletableFuture} 类型，以实现与异步任务的更丰富交互，
 * 并支持与后续处理步骤的即时组合。
 *
 * <p>从代理返回的 {@code Future} 句柄将是一个真正的异步 {@code Future}，
 * 可用于跟踪异步方法执行的结果。但是，由于目标方法需要实现相同的签名，
 * 它将不得不返回一个仅传递值的临时 {@code Future} 句柄：例如 Spring 的
 * {@link AsyncResult}、EJB 3.1 的 {@link javax.ejb.AsyncResult}，
 * 或 {@link java.util.concurrent.CompletableFuture#completedFuture(Object)}。
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 3.0
 * @see AnnotationAsyncExecutionInterceptor
 * @see AsyncAnnotationAdvisor
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Async {

	/**
	 * 指定异步操作的限定符值。
	 * <p>可用于确定执行异步操作时使用的目标执行器，匹配特定
	 * {@link java.util.concurrent.Executor Executor} 或
	 * {@link org.springframework.core.task.TaskExecutor TaskExecutor}
	 * bean 定义的限定符值（或 bean 名称）。
	 * <p>当在类级别的 {@code @Async} 注解上指定时，表示该类中的所有方法都应使用指定的执行器。
	 * 方法级别的 {@code Async#value} 始终会覆盖类级别设置的任何值。
	 * @since 3.1.2
	 */
	String value() default "";

}
