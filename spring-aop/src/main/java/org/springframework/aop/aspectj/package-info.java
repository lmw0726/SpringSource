/**
 * AspectJ 集成包。包括用于 AspectJ 5 注解样式方法的 Spring AOP Advice 实现，
 * 以及 AspectJExpressionPointcut：一个 Spring AOP Pointcut 实现，
 * 允许在 Spring AOP 运行时框架中使用 AspectJ 切点表达式语言。
 *
 * <p>请注意，使用此包<i>不</i>需要使用 {@code ajc} 编译器
 * 或 AspectJ 加载时织入器。它旨在启用 AspectJ 功能的有价值子集，
 * 并具有一致的语义，与基于代理的 Spring AOP 框架一起使用。
 */
@NonNullApi
@NonNullFields
package org.springframework.aop.aspectj;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;
