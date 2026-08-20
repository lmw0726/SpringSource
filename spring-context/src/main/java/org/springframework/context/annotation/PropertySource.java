/*
 * Copyright 2002-2020 the original author or authors.
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
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.io.support.PropertySourceFactory;

/**
 * 注解提供了一种方便的声明式机制，用于将
 * {@link org.springframework.core.env.PropertySource PropertySource} 添加到 Spring 的
 * {@link org.springframework.core.env.Environment Environment} 中。应与
 * @{@link Configuration} 类结合使用。
 *
 * <h3>使用示例</h3>
 *
 * <p>假设有一个文件 {@code app.properties} 包含键/值对
 * {@code testbean.name=myTestBean}，下面的 {@code @Configuration} 类
 * 使用 {@code @PropertySource} 将 {@code app.properties} 添加到
 * {@code Environment} 的 {@code PropertySources} 集合中。
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;PropertySource("classpath:/com/myco/app.properties")
 * public class AppConfig {
 *
 *     &#064;Autowired
 *     Environment env;
 *
 *     &#064;Bean
 *     public TestBean testBean() {
 *         TestBean testBean = new TestBean();
 *         testBean.setName(env.getProperty("testbean.name"));
 *         return testBean;
 *     }
 * }</pre>
 *
 * <p>请注意，{@code Environment} 对象通过
 * {@link org.springframework.beans.factory.annotation.Autowired @Autowired} 注入到
 * 配置类中，然后在填充 {@code TestBean} 对象时使用。根据上述配置，
 * 调用 {@code testBean.getName()} 将返回 "myTestBean"。
 *
 * <h3>在 {@code <bean>} 和 {@code @Value} 注解中解析 <code>${...}</code> 占位符</h3>
 *
 * <p>要使用来自 {@code PropertySource} 的属性解析 {@code <bean>} 定义或 {@code @Value}
 * 注解中的 ${...} 占位符，必须确保在 {@code ApplicationContext} 使用的
 * {@code BeanFactory} 中注册了适当的<em>嵌入值解析器</em>。当在 XML 中使用
 * {@code <context:property-placeholder>} 时，这会自动发生。当使用 {@code @Configuration}
 * 类时，可以通过 {@code static} {@code @Bean} 方法显式注册
 * {@code PropertySourcesPlaceholderConfigurer} 来实现。但请注意，通常只有在需要
 * 自定义配置（如占位符语法等）时，才需要通过 {@code static} {@code @Bean} 方法
 * 显式注册 {@code PropertySourcesPlaceholderConfigurer}。有关详细信息和示例，请参阅
 * {@link Configuration @Configuration} 的 javadoc 中的"使用外部化值"部分
 * 和 {@link Bean @Bean} 的 javadoc 中的"关于返回 {@code @Bean} 方法的
 * BeanFactoryPostProcessor 的说明"。
 *
 * <h3>在 {@code @PropertySource} 资源位置中解析 ${...} 占位符</h3>
 *
 * <p>出现在 {@code @PropertySource} {@linkplain #value()
 * 资源位置} 中的任何 ${...} 占位符将针对环境中已注册的属性源集合进行解析。例如：
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;PropertySource("classpath:/com/${my.placeholder:default/path}/app.properties")
 * public class AppConfig {
 *
 *     &#064;Autowired
 *     Environment env;
 *
 *     &#064;Bean
 *     public TestBean testBean() {
 *         TestBean testBean = new TestBean();
 *         testBean.setName(env.getProperty("testbean.name"));
 *         return testBean;
 *     }
 * }</pre>
 *
 * <p>假设 "my.placeholder" 存在于已注册的属性源之一中——例如系统属性或环境变量——
 * 占位符将解析为相应的值。如果不存在，则使用 "default/path" 作为默认值。表达默认值
 * （用冒号 ":" 分隔）是可选的。如果未指定默认值且无法解析属性，则会抛出
 * {@code IllegalArgumentException}。
 *
 * <h3>关于使用 {@code @PropertySource} 进行属性覆盖的说明</h3>
 *
 * <p>当给定的属性键存在于多个 {@code .properties} 文件中时，
 * 最后处理的 {@code @PropertySource} 注解将"获胜"并覆盖同名的任何先前键。
 *
 * <p>例如，给定两个属性文件 {@code a.properties} 和 {@code b.properties}，
 * 考虑以下两个使用 {@code @PropertySource} 注解引用它们的配置类：
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;PropertySource("classpath:/com/myco/a.properties")
 * public class ConfigA { }
 *
 * &#064;Configuration
 * &#064;PropertySource("classpath:/com/myco/b.properties")
 * public class ConfigB { }
 * </pre>
 *
 * <p>覆盖顺序取决于这些类注册到应用程序上下文的顺序。
 *
 * <pre class="code">
 * AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
 * ctx.register(ConfigA.class);
 * ctx.register(ConfigB.class);
 * ctx.refresh();
 * </pre>
 *
 * <p>在上述场景中，{@code b.properties} 中的属性将覆盖 {@code a.properties}
 * 中的任何重复项，因为 {@code ConfigB} 是最后注册的。
 *
 * <p>在某些情况下，当使用 {@code @PropertySource} 注解时，可能无法或不实用
 * 严格控制属性源的顺序。例如，如果上述 {@code @Configuration} 类是通过
 * 组件扫描注册的，则顺序难以预测。在这种情况下——如果覆盖很重要——
 * 建议用户回退使用编程式 {@code PropertySource} API。有关详细信息，请参阅
 * {@link org.springframework.core.env.ConfigurableEnvironment ConfigurableEnvironment}
 * 和 {@link org.springframework.core.env.MutablePropertySources MutablePropertySources}
 * 的 javadoc。
 *
 * <p><b>注意：根据 Java 8 约定，此注解是可重复的。</b>
 * 但是，所有这些 {@code @PropertySource} 注解需要在同一级别声明：要么直接放在
 * 配置类上，要么作为元注解放在同一个自定义注解上。不建议混合使用直接注解和
 * 元注解，因为直接注解将有效地覆盖元注解。
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 3.1
 * @see PropertySources
 * @see Configuration
 * @see org.springframework.core.env.PropertySource
 * @see org.springframework.core.env.ConfigurableEnvironment#getPropertySources()
 * @see org.springframework.core.env.MutablePropertySources
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(PropertySources.class)
public @interface PropertySource {

	/**
	 * 指示此属性源的名称。如果省略，{@link #factory} 将根据底层资源生成名称
	 * （对于 {@link org.springframework.core.io.support.DefaultPropertySourceFactory}：
	 * 通过相应的无名称 {@link org.springframework.core.io.support.ResourcePropertySource}
	 * 构造函数从资源描述派生）。
	 * @see org.springframework.core.env.PropertySource#getName()
	 * @see org.springframework.core.io.Resource#getDescription()
	 */
	String name() default "";

	/**
	 * 指示要加载的属性文件的资源位置。
	 * <p>支持传统和基于 XML 的属性文件格式——例如
	 * {@code "classpath:/com/myco/app.properties"} 或 {@code "file:/path/to/file.xml"}。
	 * <p>不允许使用资源位置通配符（例如 *&#42;/*.properties）；
	 * 每个位置必须恰好解析为一个 {@code .properties} 或 {@code .xml} 资源。
	 * <p>${...} 占位符将针对已与 {@code Environment} 注册的任何/所有属性源进行解析。
	 * 有关示例，请参阅 {@linkplain PropertySource 上面的 PropertySource}。
	 * <p>每个位置将作为其自己的属性源添加到封闭的 {@code Environment} 中，并按照声明的顺序添加。
	 */
	String[] value();

	/**
	 * 指示是否应忽略未能找到 {@link #value 属性资源} 的情况。
	 * <p>如果属性文件是完全可选的，则 {@code true} 是合适的。
	 * <p>默认值为 {@code false}。
	 * @since 4.0
	 */
	boolean ignoreResourceNotFound() default false;

	/**
	 * 给定资源的特定字符编码，例如 "UTF-8"。
	 * @since 4.3
	 */
	String encoding() default "";

	/**
	 * 指定自定义 {@link PropertySourceFactory}（如果有）。
	 * <p>默认情况下，将使用标准资源文件的默认工厂。
	 * @since 4.3
	 * @see org.springframework.core.io.support.DefaultPropertySourceFactory
	 * @see org.springframework.core.io.support.ResourcePropertySource
	 */
	Class<? extends PropertySourceFactory> factory() default PropertySourceFactory.class;

}
