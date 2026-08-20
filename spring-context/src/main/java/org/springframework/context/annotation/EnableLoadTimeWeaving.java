/*
 * Copyright 2002-2018 the original author or authors.
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

import org.springframework.context.weaving.DefaultContextLoadTimeWeaver;
import org.springframework.instrument.classloading.LoadTimeWeaver;

/**
 * 为当前应用上下文激活 Spring {@link LoadTimeWeaver}，该 Weaver 以名为 "loadTimeWeaver" 的 Bean 形式提供，
 * 类似于 Spring XML 中的 {@code <context:load-time-weaver>} 元素。
 *
 * <p>用于 @{@link org.springframework.context.annotation.Configuration Configuration} 类；
 * 以下是最简单的示例：
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableLoadTimeWeaving
 * public class AppConfig {
 *
 *     // 特定于应用的 &#064;Bean 定义 ...
 * }</pre>
 *
 * 上面的示例等价于以下 Spring XML 配置：
 *
 * <pre class="code">
 * &lt;beans&gt;
 *
 *     &lt;context:load-time-weaver/&gt;
 *
 *     &lt;!-- 特定于应用的 &lt;bean&gt; 定义 --&gt;
 *
 * &lt;/beans&gt;
 * </pre>
 *
 * <h2>{@code LoadTimeWeaverAware} 接口</h2>
 * 任何实现了 {@link
 * org.springframework.context.weaving.LoadTimeWeaverAware LoadTimeWeaverAware} 接口的 Bean
 * 都将自动接收 {@code LoadTimeWeaver} 引用；例如 Spring 的 JPA 引导支持。
 *
 * <h2>自定义 {@code LoadTimeWeaver}</h2>
 * 默认的 Weaver 由系统自动确定：参见 {@link DefaultContextLoadTimeWeaver}。
 *
 * <p>要自定义使用的 Weaver，标注了 {@code @EnableLoadTimeWeaving} 的 {@code @Configuration}
 * 类还可以实现 {@link LoadTimeWeavingConfigurer} 接口，并通过 {@code #getLoadTimeWeaver}
 * 方法返回自定义的 {@code LoadTimeWeaver} 实例：
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableLoadTimeWeaving
 * public class AppConfig implements LoadTimeWeavingConfigurer {
 *
 *     &#064;Override
 *     public LoadTimeWeaver getLoadTimeWeaver() {
 *         MyLoadTimeWeaver ltw = new MyLoadTimeWeaver();
 *         ltw.addClassTransformer(myClassFileTransformer);
 *         // ...
 *         return ltw;
 *     }
 * }</pre>
 *
 * <p>上面的示例可以与以下 Spring XML 配置进行比较：
 *
 * <pre class="code">
 * &lt;beans&gt;
 *
 *     &lt;context:load-time-weaver weaverClass="com.acme.MyLoadTimeWeaver"/&gt;
 *
 * &lt;/beans&gt;
 * </pre>
 *
 * <p>代码示例与 XML 示例的不同之处在于，它实际上实例化了 {@code MyLoadTimeWeaver} 类型，
 * 这意味着它还可以配置该实例，例如调用 {@code #addClassTransformer} 方法。
 * 这展示了基于代码的配置方式如何通过直接的编程访问提供更大的灵活性。
 *
 * <h2>启用基于 AspectJ 的织入</h2>
 * 可以通过 {@link #aspectjWeaving()} 属性启用 AspectJ 加载时织入，
 * 这将导致 {@linkplain
 * org.aspectj.weaver.loadtime.ClassPreProcessorAgentAdapter AspectJ 类转换器}
 * 通过 {@link LoadTimeWeaver#addTransformer} 进行注册。如果类路径上存在
 * "META-INF/aop.xml" 资源，AspectJ 织入将默认激活。示例：
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableLoadTimeWeaving(aspectjWeaving=ENABLED)
 * public class AppConfig {
 * }</pre>
 *
 * <p>上面的示例可以与以下 Spring XML 配置进行比较：
 *
 * <pre class="code">
 * &lt;beans&gt;
 *
 *     &lt;context:load-time-weaver aspectj-weaving="on"/&gt;
 *
 * &lt;/beans&gt;
 * </pre>
 *
 * <p>这两个示例是等价的，但有一个重要的例外：在 XML 方式中，
 * 当 {@code aspectj-weaving} 为 "on" 时，{@code <context:spring-configured>} 的功能会被隐式启用。
 * 而在使用 {@code @EnableLoadTimeWeaving(aspectjWeaving=ENABLED)} 时不会发生这种情况。
 * 你必须显式添加 {@code @EnableSpringConfigured}
 * （包含在 {@code spring-aspects} 模块中）
 *
 * @author Chris Beams
 * @since 3.1
 * @see LoadTimeWeaver
 * @see DefaultContextLoadTimeWeaver
 * @see org.aspectj.weaver.loadtime.ClassPreProcessorAgentAdapter
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(LoadTimeWeavingConfiguration.class)
public @interface EnableLoadTimeWeaving {


	/**
	 * 是否应启用 AspectJ 织入。
	 */
	AspectJWeaving aspectjWeaving() default AspectJWeaving.AUTODETECT;


	/**
	 * AspectJ 织入选项。
	 */
	enum AspectJWeaving {

		/**
		 * 开启基于 Spring 的 AspectJ 加载时织入。
		 */
		ENABLED,

		/**
		 * 关闭基于 Spring 的 AspectJ 加载时织入
		 * （即使类路径上存在 "META-INF/aop.xml" 资源）。
		 */
		DISABLED,

		/**
		 * 如果类路径中存在 "META-INF/aop.xml" 资源，则开启 AspectJ 加载时织入。
		 * 如果不存在该资源，则关闭 AspectJ 加载时织入。
		 */
		AUTODETECT;
	}

}
