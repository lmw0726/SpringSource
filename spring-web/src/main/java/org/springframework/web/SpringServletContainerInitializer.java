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

package org.springframework.web;

import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.lang.Nullable;
import org.springframework.util.ReflectionUtils;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.HandlesTypes;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Servlet 3.0 {@link ServletContainerInitializer}，旨在通过Spring的{@link WebApplicationInitializer} SPI
 * 支持对Servlet容器进行基于代码的配置，而不是（或可能与之组合使用）传统的基于{@code web.xml}的方法。
 *
 * <h2>操作机制</h2>
 * 该类将在任何Servlet 3.0兼容的容器在启动期间加载和实例化，并调用其{@link #onStartup}方法，
 * 假设{@code spring-web}模块JAR存在于类路径上。这是通过JAR服务API {@link ServiceLoader#load(Class)}
 * 方法检测到{@code spring-web}模块的{@code META-INF/services/javax.servlet.ServletContainerInitializer}
 * 服务提供者配置文件实现的。有关详细信息，请参见
 * <a href="https://download.oracle.com/javase/6/docs/technotes/guides/jar/jar.html#Service%20Provider">
 * JAR服务API文档</a>，以及Servlet 3.0最终草案规范的第8.2.4节。
 *
 * <h3>与{@code web.xml}组合使用</h3>
 * Web应用程序可以选择通过{@code web.xml}中的{@code metadata-complete}属性限制Servlet容器在启动时的类路径扫描，
 * 该属性控制Servlet注解的扫描，或者通过{@code web.xml}中的{@code <absolute-ordering>}元素，该元素控制允许哪些Web片段（即JAR）执行
 * {@code ServletContainerInitializer}扫描。当使用此功能时，可以通过在{@code web.xml}中将“spring_web”添加到命名Web片段的列表来启用
 * {@link SpringServletContainerInitializer}，如下所示：
 *
 * <pre class="code">
 * &lt;absolute-ordering&gt;
 *   &lt;name&gt;some_web_fragment&lt;/name&gt;
 *   &lt;name&gt;spring_web&lt;/name&gt;
 * &lt;/absolute-ordering&gt;
 * </pre>
 *
 * <h2>与Spring的{@code WebApplicationInitializer}的关系</h2>
 * Spring的{@code WebApplicationInitializer} SPI只包含一个方法：
 * {@link WebApplicationInitializer#onStartup(ServletContext)}。签名故意非常类似于
 * {@link ServletContainerInitializer#onStartup(Set, ServletContext)}：简单来说，
 * {@code SpringServletContainerInitializer}负责实例化并委托{@code ServletContext}给任何用户定义的
 * {@code WebApplicationInitializer}实现。然后，每个{@code WebApplicationInitializer}的责任是
 * 执行实际的初始化{@code ServletContext}的工作。委托过程的详细过程在下面的{@link #onStartup onStartup}文档中有详细描述。
 *
 * <h2>一般说明</h2>
 * 通常，这个类应该被视为支持基础设施，用于更重要和面向用户的{@code WebApplicationInitializer} SPI。
 * 利用此容器初始化程序也完全是<em>可选的</em>：虽然确实会加载并在所有Servlet 3.0+运行时调用此初始化程序，
 * 但用户选择是否在类路径上提供任何{@code WebApplicationInitializer}实现。如果没有检测到
 * {@code WebApplicationInitializer}类型，则此容器初始化程序将不会起作用。
 *
 * <p>请注意，使用此容器初始化程序和{@code WebApplicationInitializer}与Spring MVC无关，除了这些类型被
 * 打包在{@code spring-web}模块JAR中这一事实。相反，它们可以被认为是通用的，在方便地基于代码的配置
 * {@code ServletContext}。换句话说，可以在{@code WebApplicationInitializer}中注册任何Servlet、监听器或过滤器，
 * 而不仅仅是Spring MVC特定的组件。
 *
 * <p>这个类既不是为了扩展，也不是打算扩展的。它应该被认为是一个内部类型，而{@code WebApplicationInitializer}
 * 是公共面向的SPI。
 *
 * <h2>另请参阅</h2>
 * 有关示例和详细用法建议，请参见{@link WebApplicationInitializer} Javadoc。<p>
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @see #onStartup(Set, ServletContext)
 * @see WebApplicationInitializer
 * @since 3.1
 */
@HandlesTypes(WebApplicationInitializer.class)
public class SpringServletContainerInitializer implements ServletContainerInitializer {

	/**
	 * 委托{@code ServletContext}给任何存在于应用程序类路径上的{@link WebApplicationInitializer}实现。
	 * <p>因为这个类声明了@{@code HandlesTypes(WebApplicationInitializer.class)}，
	 * Servlet 3.0+容器将自动扫描类路径以查找Spring的{@code WebApplicationInitializer}接口的实现，
	 * 并将所有这些类型提供给此方法的{@code webAppInitializerClasses}参数。
	 * <p>如果在类路径上没有找到{@code WebApplicationInitializer}实现，则此方法实际上是一个空操作。
	 * 将发出一个INFO级别的日志消息，通知用户确实调用了{@code ServletContainerInitializer}，但没有找到
	 * {@code WebApplicationInitializer}实现。
	 * <p>假设检测到一个或多个{@code WebApplicationInitializer}类型，它们将被实例化（如果存在@{@link
	 * org.springframework.core.annotation.Order @Order}注解或{@link org.springframework.core.Ordered
	 * Ordered}接口已实现，则会进行<em>排序</em>）。然后在每个实例上调用{@link WebApplicationInitializer#onStartup(ServletContext)}
	 * 方法，委托{@code ServletContext}，以便每个实例可以注册和配置诸如Spring的{@code DispatcherServlet}、
	 * Spring的{@code ContextLoaderListener}或任何其他Servlet API组件，如过滤器。
	 *
	 * @param webAppInitializerClasses 应用程序类路径上找到的所有{@link WebApplicationInitializer}实现
	 * @param servletContext           要初始化的servlet上下文
	 * @see WebApplicationInitializer#onStartup(ServletContext)
	 * @see AnnotationAwareOrderComparator
	 */
	@Override
	public void onStartup(@Nullable Set<Class<?>> webAppInitializerClasses, ServletContext servletContext)
			throws ServletException {

		List<WebApplicationInitializer> initializers = Collections.emptyList();

		if (webAppInitializerClasses != null) {
			initializers = new ArrayList<>(webAppInitializerClasses.size());
			for (Class<?> waiClass : webAppInitializerClasses) {
				// 要谨慎：一些Servlet容器提供了无效的类，无论@HandlesTypes说了什么...
				// 如果不是接口、不是抽象类，并且实现了WebApplicationInitializer接口，则添加到initializers列表中
				if (!waiClass.isInterface() && !Modifier.isAbstract(waiClass.getModifiers()) &&
						WebApplicationInitializer.class.isAssignableFrom(waiClass)) {
					try {
						// 实例化WebApplicationInitializer类，并添加到initializers列表中
						initializers.add((WebApplicationInitializer)
								ReflectionUtils.accessibleConstructor(waiClass).newInstance());
					} catch (Throwable ex) {
						// 如果实例化失败，则抛出ServletException异常
						throw new ServletException("Failed to instantiate WebApplicationInitializer class", ex);
					}
				}
			}
		}

		if (initializers.isEmpty()) {
			// 如果initializers列表为空，则记录日志
			servletContext.log("No Spring WebApplicationInitializer types detected on classpath");
			return;
		}

		// 记录检测到的Spring WebApplicationInitializer数量
		servletContext.log(initializers.size() + " Spring WebApplicationInitializers detected on classpath");
		// 对initializers列表进行排序
		AnnotationAwareOrderComparator.sort(initializers);
		// 调用每个WebApplicationInitializer的onStartup方法
		for (WebApplicationInitializer initializer : initializers) {
			initializer.onStartup(servletContext);
		}
	}

}
