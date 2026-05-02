/**
 * SPI 包，允许 Spring AOP 框架处理任意通知类型。
 *
 * <p>只想<i>使用</i> Spring AOP 框架而不扩展其功能的用户，
 * 不需要关心此包。
 *
 * <p>您可能希望使用这些适配器将 Spring 特定的通知（如 MethodBeforeAdvice）
 * 包装在 MethodInterceptor 中，以便在支持 AOP Alliance 接口的
 * 其他 AOP 框架中使用它们。
 *
 * <p>这些适配器不依赖于任何其他 Spring 框架类，以允许此类使用。
 */
@NonNullApi
@NonNullFields
package org.springframework.aop.framework.adapter;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;
