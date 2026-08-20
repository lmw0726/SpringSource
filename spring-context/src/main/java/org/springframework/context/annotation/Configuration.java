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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;

/**
 * 表示一个类声明了一个或多个 {@link Bean @Bean} 方法，并且可以被 Spring 容器处理，
 * 在运行时为这些 bean 生成 bean 定义和服务请求，例如：
 *
 * <pre class="code">
 * &#064;Configuration
 * public class AppConfig {
 *
 *     &#064;Bean
 *     public MyBean myBean() {
 *         // 实例化、配置并返回 bean ...
 *     }
 * }</pre>
 *
 * <h2>引导 {@code @Configuration} 类</h2>
 *
 * <h3>通过 {@code AnnotationConfigApplicationContext}</h3>
 *
 * <p>{@code @Configuration} 类通常使用 {@link AnnotationConfigApplicationContext}
 * 或其支持 Web 的变体
 * {@link org.springframework.web.context.support.AnnotationConfigWebApplicationContext
 * AnnotationConfigWebApplicationContext} 进行引导。以下是使用前者的简单示例：
 *
 * <pre class="code">
 * AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
 * ctx.register(AppConfig.class);
 * ctx.refresh();
 * MyBean myBean = ctx.getBean(MyBean.class);
 * // 使用 myBean ...
 * </pre>
 *
 * <p>有关更多详细信息，请参阅 {@link AnnotationConfigApplicationContext} 的 javadoc，
 * 有关在 {@code Servlet} 容器中进行 Web 配置的说明，请参阅
 * {@link org.springframework.web.context.support.AnnotationConfigWebApplicationContext
 * AnnotationConfigWebApplicationContext}。
 *
 * <h3>通过 Spring {@code <beans>} XML</h3>
 *
 * <p>作为直接向 {@code AnnotationConfigApplicationContext} 注册 {@code @Configuration}
 * 类的替代方案，{@code @Configuration} 类可以在 Spring XML 文件中声明为普通的
 * {@code <bean>} 定义：
 *
 * <pre class="code">
 * &lt;beans&gt;
 *    &lt;context:annotation-config/&gt;
 *    &lt;bean class="com.acme.AppConfig"/&gt;
 * &lt;/beans&gt;
 * </pre>
 *
 * <p>在上面的示例中，需要 {@code <context:annotation-config/>} 来启用
 * {@link ConfigurationClassPostProcessor} 和其他与注解相关的后处理器，
 * 以便处理 {@code @Configuration} 类。
 *
 * <h3>通过组件扫描</h3>
 *
 * <p>{@code @Configuration} 使用 {@link Component @Component} 进行元注解，
 * 因此 {@code @Configuration} 类是组件扫描的候选者（通常使用 Spring XML 的
 * {@code <context:component-scan/>} 元素），因此也可以像任何普通的
 * {@code @Component} 一样使用 {@link Autowired @Autowired}/{@link javax.inject.Inject @Inject}。
 * 特别是，如果存在单个构造函数，将透明地应用该构造函数的自动装配语义：
 *
 * <pre class="code">
 * &#064;Configuration
 * public class AppConfig {
 *
 *     private final SomeBean someBean;
 *
 *     public AppConfig(SomeBean someBean) {
 *         this.someBean = someBean;
 *     }
 *
 *     // 使用 "SomeBean" 的 &#064;Bean 定义
 *
 * }</pre>
 *
 * <p>{@code @Configuration} 类不仅可以使用组件扫描进行引导，
 * 还可以使用 {@link ComponentScan @ComponentScan} 注解本身来
 * <em>配置</em> 组件扫描：
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;ComponentScan("com.acme.app.services")
 * public class AppConfig {
 *     // 各种 &#064;Bean 定义 ...
 * }</pre>
 *
 * <p>详情请参阅 {@link ComponentScan @ComponentScan} 的 javadoc。
 *
 * <h2>使用外部化值</h2>
 *
 * <h3>使用 {@code Environment} API</h3>
 *
 * <p>可以通过将 Spring {@link org.springframework.core.env.Environment}
 * 注入到 {@code @Configuration} 类中来查找外部化值 &mdash; 例如，使用
 * {@code @Autowired} 注解：
 *
 * <pre class="code">
 * &#064;Configuration
 * public class AppConfig {
 *
 *     &#064;Autowired Environment env;
 *
 *     &#064;Bean
 *     public MyBean myBean() {
 *         MyBean myBean = new MyBean();
 *         myBean.setName(env.getProperty("bean.name"));
 *         return myBean;
 *     }
 * }</pre>
 *
 * <p>通过 {@code Environment} 解析的属性驻留在一个或多个"属性源"对象中，
 * {@code @Configuration} 类可以使用 {@link PropertySource @PropertySource}
 * 注解向 {@code Environment} 对象贡献属性源：
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;PropertySource("classpath:/com/acme/app.properties")
 * public class AppConfig {
 *
 *     &#064;Inject Environment env;
 *
 *     &#064;Bean
 *     public MyBean myBean() {
 *         return new MyBean(env.getProperty("bean.name"));
 *     }
 * }</pre>
 *
 * <p>详情请参阅 {@link org.springframework.core.env.Environment Environment}
 * 和 {@link PropertySource @PropertySource} 的 javadoc。
 *
 * <h3>使用 {@code @Value} 注解</h3>
 *
 * <p>可以使用 {@link Value @Value} 注解将外部化值注入到
 * {@code @Configuration} 类中：
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;PropertySource("classpath:/com/acme/app.properties")
 * public class AppConfig {
 *
 *     &#064;Value("${bean.name}") String beanName;
 *
 *     &#064;Bean
 *     public MyBean myBean() {
 *         return new MyBean(beanName);
 *     }
 * }</pre>
 *
 * <p>此方法通常与 Spring 的
 * {@link org.springframework.context.support.PropertySourcesPlaceholderConfigurer
 * PropertySourcesPlaceholderConfigurer} 结合使用，可以通过
 * {@code <context:property-placeholder/>} 在 XML 配置中
 * <em>自动</em> 启用，或者通过专用的 {@code static} {@code @Bean} 方法在
 * {@code @Configuration} 类中 <em>显式</em> 启用（详情请参阅
 * {@link Bean @Bean} javadoc 中的"关于返回 {@code @Bean} 方法的
 * BeanFactoryPostProcessor 的说明"）。但是请注意，通过 {@code static}
 * {@code @Bean} 方法显式注册 {@code PropertySourcesPlaceholderConfigurer}
 * 通常仅在需要自定义配置（如占位符语法等）时才需要。具体来说，
 * 如果没有 bean 后处理器（如 {@code PropertySourcesPlaceholderConfigurer}）
 * 为 {@code ApplicationContext} 注册 <em>嵌入式值解析器</em>，
 * Spring 将注册一个默认的 <em>嵌入式值解析器</em>，
 * 该解析器根据 {@code Environment} 中注册的属性源解析占位符。
 * 有关使用 {@code @ImportResource} 将 {@code @Configuration} 类与 Spring XML
 * 组合的部分，请参阅下文；请参阅 {@link Value @Value} 的 javadoc；
 * 有关处理 {@code BeanFactoryPostProcessor} 类型（如
 * {@code PropertySourcesPlaceholderConfigurer}）的详细信息，
 * 请参阅 {@link Bean @Bean} 的 javadoc。
 *
 * <h2>组合 {@code @Configuration} 类</h2>
 *
 * <h3>使用 {@code @Import} 注解</h3>
 *
 * <p>{@code @Configuration} 类可以使用 {@link Import @Import} 注解进行组合，
 * 类似于 Spring XML 中 {@code <import>} 的工作方式。由于 {@code @Configuration}
 * 对象在容器中作为 Spring bean 进行管理，导入的配置可以通过
 * &mdash; 例如，通过构造函数注入：
 *
 * <pre class="code">
 * &#064;Configuration
 * public class DatabaseConfig {
 *
 *     &#064;Bean
 *     public DataSource dataSource() {
 *         // 实例化、配置并返回 DataSource
 *     }
 * }
 *
 * &#064;Configuration
 * &#064;Import(DatabaseConfig.class)
 * public class AppConfig {
 *
 *     private final DatabaseConfig dataConfig;
 *
 *     public AppConfig(DatabaseConfig dataConfig) {
 *         this.dataConfig = dataConfig;
 *     }
 *
 *     &#064;Bean
 *     public MyBean myBean() {
 *         // 引用 dataSource() bean 方法
 *         return new MyBean(dataConfig.dataSource());
 *     }
 * }</pre>
 *
 * <p>现在，只需向 Spring 上下文注册 {@code AppConfig}，
 * 即可引导 {@code AppConfig} 和导入的 {@code DatabaseConfig}：
 *
 * <pre class="code">
 * new AnnotationConfigApplicationContext(AppConfig.class);</pre>
 *
 * <h3>使用 {@code @Profile} 注解</h3>
 *
 * <p>{@code @Configuration} 类可以使用 {@link Profile @Profile} 注解进行标记，
 * 以指示仅当给定的 profile 或 profiles <em>处于活动状态</em> 时才应处理它们：
 *
 * <pre class="code">
 * &#064;Profile("development")
 * &#064;Configuration
 * public class EmbeddedDatabaseConfig {
 *
 *     &#064;Bean
 *     public DataSource dataSource() {
 *         // 实例化、配置并返回嵌入式 DataSource
 *     }
 * }
 *
 * &#064;Profile("production")
 * &#064;Configuration
 * public class ProductionDatabaseConfig {
 *
 *     &#064;Bean
 *     public DataSource dataSource() {
 *         // 实例化、配置并返回生产环境 DataSource
 *     }
 * }</pre>
 *
 * <p>或者，也可以在 {@code @Bean} 方法级别声明 profile 条件
 * &mdash; 例如，用于同一配置类中的替代 bean 变体：
 *
 * <pre class="code">
 * &#064;Configuration
 * public class ProfileDatabaseConfig {
 *
 *     &#064;Bean("dataSource")
 *     &#064;Profile("development")
 *     public DataSource embeddedDatabase() { ... }
 *
 *     &#064;Bean("dataSource")
 *     &#064;Profile("production")
 *     public DataSource productionDatabase() { ... }
 * }</pre>
 *
 * <p>详情请参阅 {@link Profile @Profile} 和
 * {@link org.springframework.core.env.Environment} 的 javadoc。
 *
 * <h3>使用 {@code @ImportResource} 注解与 Spring XML 结合</h3>
 *
 * <p>如上所述，{@code @Configuration} 类可以在 Spring XML 文件中声明为普通的
 * Spring {@code <bean>} 定义。也可以使用 {@link ImportResource @ImportResource}
 * 注解将 Spring XML 配置文件导入到 {@code @Configuration} 类中。
 * 从 XML 导入的 bean 定义可以被注入 &mdash; 例如，使用 {@code @Inject} 注解：
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;ImportResource("classpath:/com/acme/database-config.xml")
 * public class AppConfig {
 *
 *     &#064;Inject DataSource dataSource; // 来自 XML
 *
 *     &#064;Bean
 *     public MyBean myBean() {
 *         // 注入 XML 定义的 dataSource bean
 *         return new MyBean(this.dataSource);
 *     }
 * }</pre>
 *
 * <h3>嵌套 {@code @Configuration} 类</h3>
 *
 * <p>{@code @Configuration} 类可以相互嵌套，如下所示：
 *
 * <pre class="code">
 * &#064;Configuration
 * public class AppConfig {
 *
 *     &#064;Inject DataSource dataSource;
 *
 *     &#064;Bean
 *     public MyBean myBean() {
 *         return new MyBean(dataSource);
 *     }
 *
 *     &#064;Configuration
 *     static class DatabaseConfig {
 *         &#064;Bean
 *         DataSource dataSource() {
 *             return new EmbeddedDatabaseBuilder().build();
 *         }
 *     }
 * }</pre>
 *
 * <p>引导此类安排时，只需向应用程序上下文注册 {@code AppConfig}。
 * 由于它是嵌套的 {@code @Configuration} 类，{@code DatabaseConfig}
 * <em>将自动注册</em>。当 {@code AppConfig} 和 {@code DatabaseConfig}
 * 之间的关系已经隐式明确时，这避免了使用 {@code @Import} 注解的需要。
 *
 * <p>另请注意，嵌套 {@code @Configuration} 类可以与 {@code @Profile}
 * 注解配合使用，为外部 {@code @Configuration} 类提供同一 bean 的两个选项。
 *
 * <h2>配置延迟初始化</h2>
 *
 * <p>默认情况下，{@code @Bean} 方法将在容器引导时 <em>立即实例化</em>。
 * 为避免这种情况，{@code @Configuration} 可与 {@link Lazy @Lazy} 注解
 * 结合使用，以指示类中声明的所有 {@code @Bean} 方法默认延迟初始化。
 * 请注意，{@code @Lazy} 也可以用于单个 {@code @Bean} 方法。
 *
 * <h2>{@code @Configuration} 类的测试支持</h2>
 *
 * <p>{@code spring-test} 模块中提供的 Spring <em>TestContext 框架</em>
 * 提供了 {@code @ContextConfiguration} 注解，该注解可以接受
 * <em>组件类</em> 引用数组 &mdash; 通常是 {@code @Configuration} 或
 * {@code @Component} 类。
 *
 * <pre class="code">
 * &#064;RunWith(SpringRunner.class)
 * &#064;ContextConfiguration(classes = {AppConfig.class, DatabaseConfig.class})
 * public class MyTests {
 *
 *     &#064;Autowired MyBean myBean;
 *
 *     &#064;Autowired DataSource dataSource;
 *
 *     &#064;Test
 *     public void test() {
 *         // 针对 myBean 的断言 ...
 *     }
 * }</pre>
 *
 * <p>详情请参阅
 * <a href="https://docs.spring.io/spring/docs/current/spring-framework-reference/testing.html#testcontext-framework">TestContext 框架</a>
 * 参考文档。
 *
 * <h2>使用 {@code @Enable} 注解启用内置 Spring 功能</h2>
 *
 * <p>Spring 功能（如异步方法执行、定时任务执行、注解驱动的事务管理，
 * 甚至 Spring MVC）可以从 {@code @Configuration} 类使用各自的
 * "{@code @Enable}" 注解进行启用和配置。详情请参阅
 * {@link org.springframework.scheduling.annotation.EnableAsync @EnableAsync}、
 * {@link org.springframework.scheduling.annotation.EnableScheduling @EnableScheduling}、
 * {@link org.springframework.transaction.annotation.EnableTransactionManagement @EnableTransactionManagement}、
 * {@link org.springframework.context.annotation.EnableAspectJAutoProxy @EnableAspectJAutoProxy}
 * 和 {@link org.springframework.web.servlet.config.annotation.EnableWebMvc @EnableWebMvc}。
 *
 * <h2>编写 {@code @Configuration} 类时的约束</h2>
 *
 * <ul>
 * <li>配置类必须以类的形式提供（即不是从工厂方法返回的实例），
 * 允许通过生成的子类在运行时进行增强。
 * <li>配置类必须是非 final 的（允许在运行时创建子类），
 * 除非 {@link #proxyBeanMethods() proxyBeanMethods} 标志设置为 {@code false}，
 * 在这种情况下不需要运行时生成的子类。
 * <li>配置类必须是非局部的（即不能在方法内声明）。
 * <li>任何嵌套的配置类都必须声明为 {@code static}。
 * <li>{@code @Bean} 方法本身不能创建进一步的配置类
 * （任何此类实例将被视为普通 bean，其配置注解将不会被检测到）。
 * </ul>
 *
 * @author Rod Johnson
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.0
 * @see Bean
 * @see Profile
 * @see Import
 * @see ImportResource
 * @see ComponentScan
 * @see Lazy
 * @see PropertySource
 * @see AnnotationConfigApplicationContext
 * @see ConfigurationClassPostProcessor
 * @see org.springframework.core.env.Environment
 * @see org.springframework.test.context.ContextConfiguration
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface Configuration {

	/**
	 * 显式指定与 {@code @Configuration} 类关联的 Spring bean 定义的名称。
	 * 如果未指定（常见情况），将自动生成 bean 名称。
	 * <p>自定义名称仅在通过组件扫描获取 {@code @Configuration} 类
	 * 或直接提供给 {@link AnnotationConfigApplicationContext} 时适用。
	 * 如果 {@code @Configuration} 类注册为传统的 XML bean 定义，
	 * bean 元素的 name/id 将优先使用。
	 * @return 显式的组件名称（如果有），否则为空字符串
	 * @see AnnotationBeanNameGenerator
	 */
	@AliasFor(annotation = Component.class)
	String value() default "";

	/**
	 * 指定是否应对 {@code @Bean} 方法进行代理以强制执行 bean 生命周期行为，
	 * 例如，即使在用户代码中直接调用 {@code @Bean} 方法也能返回共享的单例 bean 实例。
	 * 此功能需要方法拦截，通过运行时生成的 CGLIB 子类实现，
	 * 这带来了诸如配置类及其方法不允许声明 {@code final} 等限制。
	 * <p>默认值为 {@code true}，允许通过配置类内的直接方法调用进行
	 * "bean 间引用"，以及从此配置的 {@code @Bean} 方法进行外部调用，
	 * 例如从另一个配置类调用。如果不需要此功能，因为此特定配置的
	 * 每个 {@code @Bean} 方法都是自包含的，并被设计为容器使用的普通工厂方法，
	 * 请将此标志切换为 {@code false} 以避免 CGLIB 子类处理。
	 * <p>关闭 bean 方法拦截会有效地像在非 {@code @Configuration} 类上声明时那样
	 * 单独处理 {@code @Bean} 方法，即"@Bean Lite 模式"
	 * （参见 {@link Bean @Bean} 的 javadoc）。因此，
	 * 在行为上等同于移除 {@code @Configuration} 刻板印象。
	 * @since 5.2
	 */
	boolean proxyBeanMethods() default true;

}
