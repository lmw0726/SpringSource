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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.core.annotation.AliasFor;

/**
 * 标识一个方法，该方法生产一个由 Spring 容器管理的 bean。
 *
 * <h3>概述</h3>
 *
 * <p>此注解的属性名称和语义有意与 Spring XML 模式中的 {@code <bean/>} 元素保持一致。例如：
 *
 * <pre class="code">
 *     &#064;Bean
 *     public MyBean myBean() {
 *         // 实例化并配置 MyBean 对象
 *         return obj;
 *     }
 * </pre>
 *
 * <h3>Bean 名称</h3>
 *
 * <p>虽然提供了 {@link #name} 属性，但确定 bean 名称的默认策略是使用 {@code @Bean} 方法的名称。
 * 这种方式方便且直观，但如果需要显式命名，可以使用 {@code name} 属性（或其别名 {@code value}）。
 * 另请注意，{@code name} 接受一个字符串数组，允许为单个 bean 指定多个名称（即一个主 bean 名称加上一个或多个别名）。
 *
 * <pre class="code">
 *     &#064;Bean({"b1", "b2"}) // bean 可通过 'b1' 和 'b2' 访问，但不能通过 'myBean'
 *     public MyBean myBean() {
 *         // 实例化并配置 MyBean 对象
 *         return obj;
 *     }
 * </pre>
 *
 * <h3>Profile、Scope、Lazy、DependsOn、Primary、Order</h3>
 *
 * <p>请注意，{@code @Bean} 注解并未提供 profile、scope、lazy、depends-on 或 primary 属性。
 * 相反，它应与 {@link Scope @Scope}、{@link Lazy @Lazy}、{@link DependsOn @DependsOn} 和
 * {@link Primary @Primary} 注解配合使用来声明这些语义。例如：
 *
 * <pre class="code">
 *     &#064;Bean
 *     &#064;Profile("production")
 *     &#064;Scope("prototype")
 *     public MyBean myBean() {
 *         // 实例化并配置 MyBean 对象
 *         return obj;
 *     }
 * </pre>
 *
 * 上述注解的语义与在组件类级别上的使用相同：{@code @Profile} 允许有选择地包含某些 bean。
 * {@code @Scope} 将 bean 的作用域从 singleton 更改为指定的作用域。
 * {@code @Lazy} 仅在默认的 singleton 作用域下才实际生效。
 * {@code @DependsOn} 强制在此 bean 创建之前先创建特定的其他 bean，
 * 此外还包括 bean 通过直接引用表达的依赖关系，这对 singleton 的启动通常很有帮助。
 * {@code @Primary} 是一种在注入点级别解决歧义的机制，当需要注入单个目标组件但多个 bean 按类型匹配时使用。
 *
 * <p>此外，{@code @Bean} 方法还可以声明限定符注解和 {@link org.springframework.core.annotation.Order @Order} 值，
 * 在注入点解析时会加以考虑，就像在相应的组件类上使用相应的注解一样，
 * 但可以针对每个 bean 定义单独配置（当多个定义使用相同的 bean 类时）。
 * 限定符在初始类型匹配后缩小候选集；在集合注入点的情况下（多个目标 bean 按类型和限定符匹配），
 * order 值决定解析后的元素顺序。
 *
 * <p><b>注意：</b> {@code @Order} 值可能影响注入点的优先级，但请注意它们不会影响 singleton 的启动顺序，
 * 启动顺序是一个独立的关注点，由依赖关系和上述 {@code @DependsOn} 声明决定。
 * 此外，{@link javax.annotation.Priority} 在此级别不可用，因为它不能在方法上声明；
 * 其语义可以通过在每个类型的单个 bean 上使用 {@code @Order} 值配合 {@code @Primary} 来建模。
 *
 * <h3>{@code @Configuration} 类中的 {@code @Bean} 方法</h3>
 *
 * <p>通常，{@code @Bean} 方法在 {@code @Configuration} 类中声明。在这种情况下，
 * bean 方法可以通过 <i>直接</i> 调用同一类中的其他 {@code @Bean} 方法来引用它们。
 * 这确保了 bean 之间的引用是强类型且可导航的。这种所谓的 <em>'bean 间引用'</em>
 * 保证了作用域和 AOP 语义的正确性，就像 {@code getBean()} 查找一样。
 * 这些是原始 'Spring JavaConfig' 项目中已知的语义，它要求在运行时对每个此类配置类进行 CGLIB 子类化。
 * 因此，在此模式下，{@code @Configuration} 类及其工厂方法不得标记为 final 或 private。例如：
 *
 * <pre class="code">
 * &#064;Configuration
 * public class AppConfig {
 *
 *     &#064;Bean
 *     public FooService fooService() {
 *         return new FooService(fooRepository());
 *     }
 *
 *     &#064;Bean
 *     public FooRepository fooRepository() {
 *         return new JdbcFooRepository(dataSource());
 *     }
 *
 *     // ...
 * }</pre>
 *
 * <h3>{@code @Bean} <em>轻量</em> 模式</h3>
 *
 * <p>{@code @Bean} 方法也可以在 <em>未</em> 被 {@code @Configuration} 注解的类中声明。
 * 例如，bean 方法可以在 {@code @Component} 类中声明，甚至可以在 <em>普通类</em> 中声明。
 * 在这种情况下，{@code @Bean} 方法将以所谓的 <em>'轻量'</em> 模式进行处理。
 *
 * <p><em>轻量</em> 模式中的 bean 方法将被容器视为普通的 <em>工厂方法</em>
 * （类似于 XML 中的 {@code factory-method} 声明），作用域和生命周期回调会被正确应用。
 * 在这种情况下，包含类保持不变，对包含类或工厂方法没有特殊的限制。
 *
 * <p>与 {@code @Configuration} 类中 bean 方法的语义相比，
 * <em>轻量</em> 模式不支持 <em>'bean 间引用'</em>。
 * 相反，当一个 {@code @Bean} 方法在 <em>轻量</em> 模式下调用另一个 {@code @Bean} 方法时，
 * 该调用是一个标准的 Java 方法调用；Spring 不会通过 CGLIB 代理拦截该调用。
 * 这类似于 {@code @Transactional} 方法之间的调用，在代理模式下，Spring 不会拦截调用 &mdash;
 * Spring 只在 AspectJ 模式下才会这样做。
 *
 * <p>例如：
 *
 * <pre class="code">
 * &#064;Component
 * public class Calculator {
 *     public int sum(int a, int b) {
 *         return a+b;
 *     }
 *
 *     &#064;Bean
 *     public MyBean myBean() {
 *         return new MyBean();
 *     }
 * }</pre>
 *
 * <h3>引导启动</h3>
 *
 * <p>有关更多详细信息，包括如何使用 {@link AnnotationConfigApplicationContext} 等引导容器，
 * 请参阅 @{@link Configuration} 的 Javadoc。
 *
 * <h3>返回 {@code BeanFactoryPostProcessor} 的 {@code @Bean} 方法</h3>
 *
 * <p>对于返回 Spring {@link org.springframework.beans.factory.config.BeanFactoryPostProcessor BeanFactoryPostProcessor}
 * （{@code BFPP}）类型的 {@code @Bean} 方法必须特别注意。因为 {@code BFPP} 对象必须在容器生命周期的
 * 非常早期实例化，它们可能会干扰 {@code @Configuration} 类中 {@code @Autowired}、{@code @Value} 和
 * {@code @PostConstruct} 等注解的处理。为避免这些生命周期问题，请将返回 {@code BFPP} 的 {@code @Bean}
 * 方法标记为 {@code static}。例如：
 *
 * <pre class="code">
 *     &#064;Bean
 *     public static PropertySourcesPlaceholderConfigurer pspc() {
 *         // 实例化、配置并返回 pspc ...
 *     }
 * </pre>
 *
 * 通过将此方法标记为 {@code static}，可以在不实例化其声明的 {@code @Configuration} 类的情况下调用它，
 * 从而避免上述生命周期冲突。但请注意，{@code static} {@code @Bean} 方法不会像上述那样被增强以应用
 * 作用域和 AOP 语义。这在 {@code BFPP} 的情况下是可以接受的，因为它们通常不会被其他 {@code @Bean}
 * 方法引用。作为提醒，对于任何返回类型可分配给 {@code BeanFactoryPostProcessor} 的非静态 {@code @Bean}
 * 方法，都会发出 INFO 级别的日志消息。
 *
 * @author Rod Johnson
 * @author Costin Leau
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.0
 * @see Configuration
 * @see Scope
 * @see DependsOn
 * @see Lazy
 * @see Primary
 * @see org.springframework.stereotype.Component
 * @see org.springframework.beans.factory.annotation.Autowired
 * @see org.springframework.beans.factory.annotation.Value
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Bean {


	/**
	 * {@link #name} 的别名。
	 * <p>当不需要其他属性时使用，例如：
	 * {@code @Bean("customBeanName")}。
	 * @since 4.3.3
	 * @see #name
	 */
	@AliasFor("name")
	String[] value() default {};

	/**
	 * 此 bean 的名称，或者如果有多个名称，则为主 bean 名称加上别名。
	 * <p>如果未指定，则 bean 的名称为被注解方法的名称。
	 * 如果指定了，则方法名将被忽略。
	 * <p>如果未声明其他属性，bean 名称和别名也可以通过 {@link #value} 属性配置。
	 * @see #value
	 */
	@AliasFor("value")
	String[] name() default {};

	/**
	 * 是否通过基于约定的按名称或按类型自动装配来注入依赖？
	 * <p>请注意，此自动装配模式仅涉及基于 bean 属性 setter 方法的外部驱动自动装配，
	 * 类似于 XML bean 定义中的自动装配。
	 * <p>默认模式确实允许基于注解的自动装配。"no" 仅指外部驱动的自动装配，
	 * 不影响 bean 类本身通过注解表达的任何自动装配需求。
	 * @see Autowire#BY_NAME
	 * @see Autowire#BY_TYPE
	 * @deprecated 自 5.1 起，因为 {@code @Bean} 工厂方法参数解析和 {@code @Autowired}
	 * 处理已取代基于名称/类型的 bean 属性注入。
	 */
	@Deprecated
	Autowire autowire() default Autowire.NO;

	/**
	 * 此 bean 是否可以作为自动装配到其他 bean 的候选项？
	 * <p>默认为 {@code true}；对于不打算干扰其他位置相同类型的 bean 的内部委托，
	 * 请将此值设置为 {@code false}。
	 * @since 5.1
	 */
	boolean autowireCandidate() default true;

	/**
	 * 在初始化期间要在 bean 实例上调用的方法的可选名称。
	 * 由于可以在 Bean 注解方法的方法体中直接以编程方式调用该方法，因此通常不需要使用此属性。
	 * <p>默认值为 {@code ""}，表示不调用任何初始化方法。
	 * @see org.springframework.beans.factory.InitializingBean
	 * @see org.springframework.context.ConfigurableApplicationContext#refresh()
	 */
	String initMethod() default "";

	/**
	 * 在关闭应用上下文时要在 bean 实例上调用的方法的可选名称，
	 * 例如 JDBC {@code DataSource} 实现上的 {@code close()} 方法，
	 * 或 Hibernate {@code SessionFactory} 对象上的方法。
	 * 该方法必须没有参数，但可以抛出任何异常。
	 * <p>为了方便用户，容器将尝试对 {@code @Bean} 方法返回的对象推断销毁方法。
	 * 例如，给定一个返回 Apache Commons DBCP {@code BasicDataSource} 的 {@code @Bean} 方法，
	 * 容器会注意到该对象上可用的 {@code close()} 方法，并自动将其注册为 {@code destroyMethod}。
	 * 这种"销毁方法推断"目前仅限于检测名为 'close' 或 'shutdown' 的公共无参方法。
	 * 该方法可以在继承层次结构的任何层级上声明，并且无论 {@code @Bean} 方法的返回类型如何
	 * 都可以被检测到（即，在创建时对 bean 实例本身进行反射检测）。
	 * <p>要为特定的 {@code @Bean} 禁用销毁方法推断，请指定空字符串作为值，
	 * 例如 {@code @Bean(destroyMethod="")}。请注意，{@link org.springframework.beans.factory.DisposableBean}
	 * 回调接口仍将被检测到，并且相应的销毁方法将被调用：换句话说，
	 * {@code destroyMethod=""} 只影响自定义的 close/shutdown 方法和
	 * {@link java.io.Closeable}/{@link java.lang.AutoCloseable} 声明的 close 方法。
	 * <p>注意：仅在生命周期完全由工厂控制的 bean 上调用，对于 singleton 始终如此，
	 * 但对于其他作用域则不保证。
	 * @see org.springframework.beans.factory.DisposableBean
	 * @see org.springframework.context.ConfigurableApplicationContext#close()
	 */
	String destroyMethod() default AbstractBeanDefinition.INFER_METHOD;

}
