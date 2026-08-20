/*
 * Copyright 2002-2011 the original author or authors.
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

import org.springframework.beans.factory.Aware;

/**
 * 需要接收其运行所在的 ApplicationEventPublisher（通常是 ApplicationContext）
 * 通知的任何对象都应实现此接口。
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 1.1.1
 * @see ApplicationContextAware
 */
public interface ApplicationEventPublisherAware extends Aware {

	/**
	 * 设置此对象运行所在的 ApplicationEventPublisher。
	 * <p>在普通 bean 属性填充之后、初始化回调（如 InitializingBean 的 afterPropertiesSet
	 * 或自定义 init-method）之前调用。在 ApplicationContextAware 的
	 * setApplicationContext 之前调用。
	 * @param applicationEventPublisher 此对象将使用的事件发布器
	 */
	void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher);

}
