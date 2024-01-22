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

package org.springframework.context;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.Aware;

/**
 * 由任何希望被通知其运行的{@link ApplicationContext}的对象实现的接口。
 *
 * 实现此接口在对象需要访问一组协作bean时是有意义的。请注意，出于bean查找的目的而仅仅实现此接口并不是最佳选择。
 *
 * 当对象需要访问文件资源（即要调用{@code getResource}、要发布应用程序事件或需要访问MessageSource）时，
 * 也可以实现此接口。但是，在这种特定情况下，最好实现更具体的{@link ResourceLoaderAware}、
 * {@link ApplicationEventPublisherAware}或{@link MessageSourceAware}接口。
 *
 * 请注意，文件资源依赖关系还可以公开为{@link org.springframework.core.io.Resource}类型的bean属性，
 * 通过由bean工厂进行的自动类型转换使用字符串进行填充。这消除了仅出于访问特定文件资源的目的而实现任何回调接口的需要。
 *
 * {@link org.springframework.context.support.ApplicationObjectSupport}是应用程序对象的便利基类，实现了此接口。
 *
 * 对于所有bean生命周期方法的列表，请参见
 * {@link org.springframework.beans.factory.BeanFactory BeanFactory javadocs}。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Chris Beams
 * @see ResourceLoaderAware
 * @see ApplicationEventPublisherAware
 * @see MessageSourceAware
 * @see org.springframework.context.support.ApplicationObjectSupport
 * @see org.springframework.beans.factory.BeanFactoryAware
 */
public interface ApplicationContextAware extends Aware {

	/**
	 * 设置此对象运行的ApplicationContext。
	 * 通常，此调用将用于初始化对象。
	 * <p>在正常bean属性的填充之后，但在init回调之前调用，例如
	 * {@link org.springframework.beans.factory.InitializingBean#afterPropertiesSet()}或自定义的init-method。
	 * 在适用的情况下，在调用{@link ResourceLoaderAware#setResourceLoader}、
	 * {@link ApplicationEventPublisherAware#setApplicationEventPublisher}和
	 * {@link MessageSourceAware}之后调用。
	 *
	 * @param applicationContext 要由此对象使用的ApplicationContext对象
	 * @throws ApplicationContextException 在上下文初始化错误的情况下
	 * @throws BeansException 如果由应用程序上下文方法抛出
	 * @see org.springframework.beans.factory.BeanInitializationException
	 */
	void setApplicationContext(ApplicationContext applicationContext) throws BeansException;

}
