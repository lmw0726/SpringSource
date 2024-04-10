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

package org.springframework.web;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

/**
 * 在Servlet 3.0+环境中实现的接口，以便以编程方式配置{@link ServletContext}，而不是（或可能与）传统的基于{@code web.xml}的方法结合使用。
 *
 * <p>实现此SPI将由{@link SpringServletContainerInitializer}自动检测到，SpringServletContainerInitializer本身将由任何Servlet 3.0容器自动引导。
 * 有关此引导机制的详细信息，请参见{@linkplain SpringServletContainerInitializer Javadoc}。
 *
 * <h2>示例</h2>
 * <h3>传统的基于XML的方法</h3>
 * 大多数构建Web应用程序的Spring用户都需要注册Spring的{@code DispatcherServlet}。例如，在WEB-INF/web.xml中，通常会这样做：
 * <pre class="code">
 * &lt;servlet&gt;
 *   &lt;servlet-name&gt;dispatcher&lt;/servlet-name&gt;
 *   &lt;servlet-class&gt;
 *     org.springframework.web.servlet.DispatcherServlet
 *   &lt;/servlet-class&gt;
 *   &lt;init-param&gt;
 *     &lt;param-name&gt;contextConfigLocation&lt;/param-name&gt;
 *     &lt;param-value&gt;/WEB-INF/spring/dispatcher-config.xml&lt;/param-value&gt;
 *   &lt;/init-param&gt;
 *   &lt;load-on-startup&gt;1&lt;/load-on-startup&gt;
 * &lt;/servlet&gt;
 *
 * &lt;servlet-mapping&gt;
 *   &lt;servlet-name&gt;dispatcher&lt;/servlet-name&gt;
 *   &lt;url-pattern&gt;/&lt;/url-pattern&gt;
 * &lt;/servlet-mapping&gt;</pre>
 *
 * <h3>使用{@code WebApplicationInitializer}的基于代码的方法</h3>
 * 这是等效的{@code DispatcherServlet}注册逻辑，使用{@code WebApplicationInitializer}风格：
 * <pre class="code">
 * public class MyWebAppInitializer implements WebApplicationInitializer {
 *
 *    &#064;Override
 *    public void onStartup(ServletContext container) {
 *      XmlWebApplicationContext appContext = new XmlWebApplicationContext();
 *      appContext.setConfigLocation("/WEB-INF/spring/dispatcher-config.xml");
 *
 *      ServletRegistration.Dynamic dispatcher =
 *        container.addServlet("dispatcher", new DispatcherServlet(appContext));
 *      dispatcher.setLoadOnStartup(1);
 *      dispatcher.addMapping("/");
 *    }
 *
 * }</pre>
 *
 * 作为以上方法的替代方案，您还可以从{@link org.springframework.web.servlet.support.AbstractDispatcherServletInitializer}
 * 继承。
 *
 * 如您所见，由于Servlet 3.0的新{@link ServletContext#addServlet}方法，我们实际上注册了{@code DispatcherServlet}的一个<em>实例</em>，
 * 这意味着{@code DispatcherServlet}现在可以像任何其他对象一样处理--在这种情况下接收其应用程序上下文的构造函数注入。
 *
 * <p>这种风格既简单又简洁。不必担心处理init-params等，只需使用正常的JavaBean风格属性和构造函数参数。您可以在必要时自由创建和处理Spring应用程序上下文，然后将它们注入{@code DispatcherServlet}。
 *
 * <p>大多数主要的Spring Web组件都已更新以支持这种注册风格。您会发现{@code DispatcherServlet}、{@code FrameworkServlet}、
 * {@code ContextLoaderListener}和{@code DelegatingFilterProxy}现在都支持构造函数参数。即使某个组件（例如非Spring、其他第三方）没有专门针对{@code WebApplicationInitializers}的使用进行更新，它们仍然可以在任何情况下使用。
 * Servlet 3.0 {@code ServletContext} API允许以编程方式设置init-params、context-params等。
 *
 * <h2>完全基于代码的配置方法</h2>
 * 在上面的示例中，{@code WEB-INF/web.xml}成功地被{@code WebApplicationInitializer}形式的代码替代，
 * 但实际的{@code dispatcher-config.xml} Spring配置仍然是基于XML的。
 * {@code WebApplicationInitializer}非常适合与Spring的基于代码的{@code @Configuration}类一起使用。
 * 请参见@{@link org.springframework.context.annotation.Configuration Configuration} Javadoc获取完整详细信息，
 * 但以下示例演示了将{@link org.springframework.web.context.support.AnnotationConfigWebApplicationContext
 * AnnotationConfigWebApplicationContext}与{@code AppConfig}和{@code DispatcherConfig}这样的用户定义的{@code @Configuration}类一起重构，
 * 以取代Spring XML文件。此示例还超出了上面的示例，以演示“根”应用程序上下文的典型配置和{@code ContextLoaderListener}的注册：
 * <pre class="code">
 * public class MyWebAppInitializer implements WebApplicationInitializer {
 *
 *    &#064;Override
 *    public void onStartup(ServletContext container) {
 *      // 创建“根”Spring应用程序上下文
 *      AnnotationConfigWebApplicationContext rootContext =
 *        new AnnotationConfigWebApplicationContext();
 *      rootContext.register(AppConfig.class);
 *
 *      // 管理根应用程序上下文的生命周期
 *      container.addListener(new ContextLoaderListener(rootContext));
 *
 *      // 创建调度程序Servlet的Spring应用程序上下文
 *      AnnotationConfigWebApplicationContext dispatcherContext =
 *        new AnnotationConfigWebApplicationContext();
 *      dispatcherContext.register(DispatcherConfig.class);
 *
 *      // 注册并映射调度程序Servlet
 *      ServletRegistration.Dynamic dispatcher =
 *        container.addServlet("dispatcher", new DispatcherServlet(dispatcherContext));
 *      dispatcher.setLoadOnStartup(1);
 *      dispatcher.addMapping("/");
 *    }
 *
 * }</pre>
 *
 * 作为以上方法的替代方案，您还可以从{@link org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer}继承。
 *
 * 请记住，{@code WebApplicationInitializer}实现是<em>自动检测</em>的--因此，您可以自由将它们打包到应用程序中，如您所见。
 *
 * <h2>对{@code WebApplicationInitializer}执行排序</h2>
 * {@code WebApplicationInitializer}实现可以选择在类级别使用Spring的@{@link org.springframework.core.annotation.Order Order}注解进行注解，
 * 或者可以实现Spring的{@link org.springframework.core.Ordered Ordered}接口。如果是这样，初始化程序将在调用之前进行排序。这提供了一种机制，
 * 让用户确保Servlet容器初始化发生的顺序。预计很少使用此功能，因为典型的应用程序可能会将所有容器初始化集中在单个{@code WebApplicationInitializer}中。
 *
 * <h2>注意事项</h2>
 *
 * <h3>web.xml版本</h3>
 * <p>{@code WEB-INF/web.xml}和{@code WebApplicationInitializer}的使用不是互斥的；例如，web.xml可以注册一个servlet，
 * 而{@code WebApplicationInitializer}可以注册另一个。通过诸如{@link ServletContext#getServletRegistration(String)}等方法，
 * 初始化程序甚至可以<em>修改</em>在{@code web.xml}中执行的注册。<strong>但是，如果应用程序中存在{@code WEB-INF/web.xml}，
 * 其{@code version}属性必须设置为“3.0”或更高，否则Servlet容器将忽略{@code ServletContainerInitializer}引导。</strong>
 *
 * <h3>映射到'/'在Tomcat下</h3>
 * <p>Apache Tomcat将其内部{@code DefaultServlet}映射到“/”，在Tomcat版本&lt;= 7.0.14中，此servlet映射<em>不能以编程方式覆盖</em>。
 * 7.0.15修复了此问题。成功在GlassFish 3.1下测试了覆盖“/”servlet映射。<p>
 *
 * @author Chris Beams
 * @since 3.1
 * @see SpringServletContainerInitializer
 * @see org.springframework.web.context.AbstractContextLoaderInitializer
 * @see org.springframework.web.servlet.support.AbstractDispatcherServletInitializer
 * @see org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer
 */
public interface WebApplicationInitializer {

	/**
	 * 使用对于初始化此Web应用程序而言必要的任何servlet、过滤器、监听器context-params和属性配置给定的{@link ServletContext}。
	 * 请参见示例{@linkplain WebApplicationInitializer above}。
	 * @param servletContext 要初始化的{@code ServletContext}
	 * @throws ServletException 如果针对给定的{@code ServletContext}的任何调用抛出{@code ServletException}
	 */
	void onStartup(ServletContext servletContext) throws ServletException;

}
