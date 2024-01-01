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

package org.springframework.web.reactive.config;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 将此注解添加到 {@code @Configuration} 类中，可以从 {@link WebFluxConfigurationSupport} 导入 Spring WebFlux 配置，以启用带注解的控制器和函数式端点的使用。
 *
 * 例如：
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableWebFlux
 * &#064;ComponentScan
 * public class MyConfiguration {
 * }
 * </pre>
 *
 * 要自定义导入的配置，请实现 {@link WebFluxConfigurer} 及其一个或多个方法：
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableWebFlux
 * &#064;ComponentScan
 * public class MyConfiguration implements WebFluxConfigurer {
 *
 *     &#064;Autowired
 *     private ObjectMapper objectMapper;
 *
 *     &#064;Override
 *     public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
 *         configurer.defaultCodecs().jackson2JsonEncoder(
 *             new Jackson2JsonEncoder(objectMapper)
 *         );
 *         configurer.defaultCodecs().jackson2JsonDecoder(
 *             new Jackson2JsonDecoder(objectMapper)
 *         );
 *     }
 *
 *     // ...
 * }
 * </pre>
 *
 * 只有一个 {@code @Configuration} 类应该具有 {@code @EnableWebFlux} 注解，以导入 Spring WebFlux 配置。然而，可以有多个实现 {@code WebFluxConfigurer} 的 {@code @Configuration} 类来定制提供的配置。
 *
 * 如果 {@code WebFluxConfigurer} 没有公开需要配置的某些设置，则考虑通过删除 {@code @EnableWebFlux} 注解并直接扩展 {@link WebFluxConfigurationSupport} 或 {@link DelegatingWebFluxConfiguration} 来切换到高级模式——后者允许检测和委派给一个或多个 {@code WebFluxConfigurer} 配置类。
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see WebFluxConfigurer
 * @see WebFluxConfigurationSupport
 * @see DelegatingWebFluxConfiguration
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(DelegatingWebFluxConfiguration.class)
public @interface EnableWebFlux {
}
