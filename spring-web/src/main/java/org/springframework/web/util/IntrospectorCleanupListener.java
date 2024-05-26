/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.web.util;

import org.springframework.beans.CachedIntrospectionResults;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.beans.Introspector;

/**
 * 监听器，用于在Web应用关闭时刷新JDK的{@link java.beans.Introspector JavaBeans Introspector}缓存。
 * 在您的{@code web.xml}中注册此监听器，以确保正确释放Web应用程序类加载器及其加载的类。
 *
 * <p><b>如果JavaBeans Introspector已用于分析应用程序类，则系统级别的Introspector缓存将持有对这些类的硬引用。
 * 因此，在Web应用关闭时，这些类和Web应用程序类加载器将不会被垃圾回收！</b> 此监听器执行正确的清理操作，
 * 以允许垃圾回收生效。
 *
 * <p>不幸的是，清理Introspector的唯一方法是刷新整个缓存，因为没有办法特别确定引用了应用程序的类。
 * 这将同时删除服务器中所有其他应用程序的缓存的内省结果。
 *
 * <p>请注意，在应用程序中使用Spring的bean基础设施时，不需要此监听器，
 * 因为Spring自身的内省结果缓存将立即从JavaBeans Introspector缓存中删除分析的类，
 * 并且仅在应用程序自己的ClassLoader中保持缓存。
 *
 * <b>尽管Spring本身不会创建JDK Introspector泄漏，但请注意，在Spring框架类本身位于“共享”ClassLoader（例如系统ClassLoader）的场景中，
 * 仍然应使用此监听器。</b> 在这种情况下，此监听器将正确清理Spring的内省缓存。
 *
 * <p>应用程序类几乎不需要直接使用JavaBeans Introspector，因此通常不是Introspector资源泄漏的原因。
 * 相反，许多库和框架没有清理Introspector：例如Struts和Quartz。
 *
 * <p>请注意，单个此类Introspector泄漏将导致整个Web应用程序类加载器无法被垃圾回收！
 * 这意味着您将在Web应用程序关闭后看到所有应用程序的静态类资源（如单例），这不是这些类的错！
 *
 * <p><b>此监听器应该在{@code web.xml}中注册为第一个监听器，位于任何应用程序监听器之前，例如Spring的ContextLoaderListener。</b>
 * 这允许监听器在生命周期的正确时间完全生效。
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see java.beans.Introspector#flushCaches()
 * @see org.springframework.beans.CachedIntrospectionResults#acceptClassLoader
 * @see org.springframework.beans.CachedIntrospectionResults#clearClassLoader
 */
public class IntrospectorCleanupListener implements ServletContextListener {

	@Override
	public void contextInitialized(ServletContextEvent event) {
		// 接受当前线程的上下文类加载器
		CachedIntrospectionResults.acceptClassLoader(Thread.currentThread().getContextClassLoader());
	}

	@Override
	public void contextDestroyed(ServletContextEvent event) {
		// 清除当前线程的上下文类加载器
		CachedIntrospectionResults.clearClassLoader(Thread.currentThread().getContextClassLoader());
		// 刷新内省器的缓存
		Introspector.flushCaches();
	}

}
