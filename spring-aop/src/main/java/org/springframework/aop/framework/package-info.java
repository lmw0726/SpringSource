/**
 * 包含 Spring 基本 AOP 基础设施的包，符合
 * <a href="http://aopalliance.sourceforge.net">AOP Alliance</a> 接口。
 *
 * <p>Spring AOP 支持代理接口或类、引介，并提供
 * 静态和动态切点。
 *
 * <p>任何 Spring AOP 代理都可以强制转换为此包中的 ProxyConfig AOP 配置接口，
 * 以添加或删除拦截器。
 *
 * <p>ProxyFactoryBean 是在 BeanFactory 或 ApplicationContext 中
 * 创建 AOP 代理的便捷方式。但是，也可以使用 ProxyFactory 类
 * 以编程方式创建代理。
 */
@NonNullApi
@NonNullFields
package org.springframework.aop.framework;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;
