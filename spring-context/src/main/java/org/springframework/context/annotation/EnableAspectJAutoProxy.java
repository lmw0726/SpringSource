/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 启用对使用AspectJ的{@code @Aspect}注解标记的组件的支持，类似于Spring的{@code <aop:aspectj-autoproxy>} XML元素的功能。
 * 应用于{@link Configuration}类，用法如下：
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableAspectJAutoProxy
 * public class AppConfig {
 *
 *     &#064;Bean
 *     public FooService fooService() {
 *         return new FooService();
 *     }
 *
 *     &#064;Bean
 *     public MyAspect myAspect() {
 *         return new MyAspect();
 *     }
 * }</pre>
 *
 * 其中，{@code FooService}是一个典型的POJO组件，而{@code MyAspect}是一个{@code @Aspect}风格的切面：
 *
 * <pre class="code">
 * public class FooService {
 *
 *     // 各种方法
 * }</pre>
 *
 * <pre class="code">
 * &#064;Aspect
 * public class MyAspect {
 *
 *     &#064;Before("execution(* FooService+.*(..))")
 *     public void advice() {
 *         // 适当地指导FooService方法
 *     }
 * }</pre>
 *
 * 在上述情况下，{@code @EnableAspectJAutoProxy}确保{@code MyAspect}将被正确处理，并且{@code FooService}将被代理，混合在其贡献的建议中。
 *
 * <p>用户可以使用{@link #proxyTargetClass()}属性控制为{@code FooService}创建的代理类型。
 * 以下内容启用CGLIB风格的'子类'代理，与默认的基于接口的JDK代理方法相反。
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableAspectJAutoProxy(proxyTargetClass=true)
 * public class AppConfig {
 *     // ...
 * }</pre>
 *
 * <p>注意，{@code @Aspect} Bean可以像其他任何Bean一样进行组件扫描。
 * 只需在切面上同时标记{@code @Aspect}和{@code @Component}：
 *
 * <pre class="code">
 * package com.foo;
 *
 * &#064;Component
 * public class FooService { ... }
 *
 * &#064;Aspect
 * &#064;Component
 * public class MyAspect { ... }</pre>
 *
 * 然后使用@{@link ComponentScan}注解来选择它们：
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;ComponentScan("com.foo")
 * &#064;EnableAspectJAutoProxy
 * public class AppConfig {
 *
 *     // 不需要明确的&#064;Bean定义
 * }</pre>
 *
 * <b>注意：{@code @EnableAspectJAutoProxy}仅适用于其本地应用上下文，允许在不同层次上选择性地代理Bean。</b>
 * 如果需要在多个层次上应用其行为，请在每个独立的上下文中重新声明{@code @EnableAspectJAutoProxy}，例如通用的根Web应用上下文和任何单独的{@code DispatcherServlet}应用上下文。
 *
 * <p>此功能需要类路径上有{@code aspectjweaver}的存在。
 * 虽然该依赖项对{@code spring-aop}一般而言是可选的，但对于{@code @EnableAspectJAutoProxy}及其底层设施是必需的。
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 * @see org.aspectj.lang.annotation.Aspect
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(AspectJAutoProxyRegistrar.class)
public @interface EnableAspectJAutoProxy {

    /**
     * 指示是否应创建基于子类（CGLIB）的代理，而不是标准的Java接口代理。
     * 默认为{@code false}。
     */
    boolean proxyTargetClass() default false;

    /**
     * 指示代理是否应由AOP框架作为{@code ThreadLocal}公开，以便通过{@link org.springframework.aop.framework.AopContext}类进行检索。
     * 默认情况下关闭，即不保证{@code AopContext}访问会起作用。
     * @since 4.3.1
     */
    boolean exposeProxy() default false;

}
