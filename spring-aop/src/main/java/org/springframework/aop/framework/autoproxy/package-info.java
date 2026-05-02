/**
 * 用于在 ApplicationContext 中使用的 Bean 后置处理器，
 * 通过自动创建 AOP 代理（无需使用 ProxyFactoryBean）来简化 AOP 使用。
 *
 * <p>此包中的各种后置处理器只需添加到 ApplicationContext 中
 *（通常在 XML bean 定义文档中），即可自动代理选定的 bean。
 *
 * <p><b>注意</b>：BeanFactory 实现不支持自动自动代理，
 * 因为后置处理器 bean 仅在应用程序上下文中自动检测。
 * 后置处理器可以在 ConfigurableBeanFactory 上显式注册。
 */
@NonNullApi
@NonNullFields
package org.springframework.aop.framework.autoproxy;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;
