/**
 * Spring AOP 核心接口，构建在 AOP Alliance AOP 互操作接口之上。
 *
 * <p>任何 AOP Alliance MethodInterceptor 都可在 Spring 中使用。
 *
 * <br>Spring AOP 还提供：
 * <ul>
 * <li>引介支持
 * <li>切点抽象，支持“静态”切点
 * （基于类和方法）以及“动态”切点（还会考虑方法参数）。
 * 目前 AOP Alliance 没有用于切点的接口。
 * <li>完整范围的通知类型，包括环绕、前置、返回后和异常通知。
 * <li>可扩展性，允许插入任意自定义通知类型，
 * 而无需修改核心框架。
 * </ul>
 *
 * <p>Spring AOP 可以以编程方式使用，或者（更推荐）
 * 与 Spring IoC 容器集成。
 */
@NonNullApi
@NonNullFields
package org.springframework.aop;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;
