/**
 * 本包包含用于便捷访问 EJB 的类。
 * 其基础是在 EJB 调用前后运行的 AOP 拦截器。
 * 特别地，本包中的类提供了对带有本地接口的无状态会话 Bean（SLSB）的透明访问，
 * 避免了使用这些 Bean 的应用代码需要依赖 EJB 特有的 API 和 JNDI 查找，
 * 并且支持使用不依赖 EJB 即可实现的业务接口。
 * 这为客户（如 Web 组件）和业务对象（无论是否是 EJB）提供了有价值的解耦。
 * 这使我们能够在不使用业务对象的代码受到影响的情况下，
 * 将 EJB 引入应用（或从应用中移除 EJB）。
 *
 * <p>本包中类的设计动机在 Rod Johnson 所著的
 * <a href="https://www.amazon.com/exec/obidos/tg/detail/-/0764543857/">《Expert One-On-One J2EE Design and Development》</a>
 * 第 11 章中进行了讨论（Wrox, 2002）。
 *
 * <p>不过，本包中类的实现和命名已经发生了变化。
 * 现在使用的是 FactoryBean 和 AOP，而非
 * <i>《Expert One-on-One J2EE》</i>中描述的自定义 Bean 定义。
 */
@NonNullApi
@NonNullFields
package org.springframework.ejb.access;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;
