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

package org.springframework.web.servlet.config.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;

/**
 * 将此注解添加到一个 {@code @Configuration} 类中，可以从 {@link WebMvcConfigurationSupport} 导入 Spring MVC 配置，例如：
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableWebMvc
 * &#064;ComponentScan(basePackageClasses = MyConfiguration.class)
 * public class MyConfiguration {
 * }
 * </pre>
 *
 * <p>要自定义导入的配置，请实现 {@link WebMvcConfigurer} 接口并覆盖各个方法，例如：
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableWebMvc
 * &#064;ComponentScan(basePackageClasses = MyConfiguration.class)
 * public class MyConfiguration implements WebMvcConfigurer {
 *
 *     &#064;Override
 *     public void addFormatters(FormatterRegistry formatterRegistry) {
 *         formatterRegistry.addConverter(new MyConverter());
 *     }
 *
 *     &#064;Override
 *     public void configureMessageConverters(List&lt;HttpMessageConverter&lt;?&gt;&gt; converters) {
 *         converters.add(new MyHttpMessageConverter());
 *     }
 *
 * }
 * </pre>
 *
 * <p><strong>注意：</strong> 只有一个 {@code @Configuration} 类可以具有 {@code @EnableWebMvc} 注解以导入 Spring Web MVC 配置。然而，可以有多个 {@code @Configuration} 类实现 {@code WebMvcConfigurer} 以自定义提供的配置。
 *
 * <p>如果 {@link WebMvcConfigurer} 没有暴露一些需要配置的更高级设置，请考虑移除 {@code @EnableWebMvc} 注解并直接继承自 {@link WebMvcConfigurationSupport} 或 {@link DelegatingWebMvcConfiguration}，例如：
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;ComponentScan(basePackageClasses = { MyConfiguration.class })
 * public class MyConfiguration extends WebMvcConfigurationSupport {
 *
 *     &#064;Override
 *     public void addFormatters(FormatterRegistry formatterRegistry) {
 *         formatterRegistry.addConverter(new MyConverter());
 *     }
 *
 *     &#064;Bean
 *     public RequestMappingHandlerAdapter requestMappingHandlerAdapter() {
 *         // 创建或委托给“super”以创建并自定义 RequestMappingHandlerAdapter 的属性
 *     }
 * }
 * </pre>
 *
 * @author Dave Syer
 * @author Rossen Stoyanchev
 * @since 3.1
 * @see org.springframework.web.servlet.config.annotation.WebMvcConfigurer
 * @see org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport
 * @see org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(DelegatingWebMvcConfiguration.class)
public @interface EnableWebMvc {
}
