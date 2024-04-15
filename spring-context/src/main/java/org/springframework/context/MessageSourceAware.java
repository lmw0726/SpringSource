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

package org.springframework.context;

import org.springframework.beans.factory.Aware;

/**
 * 任何希望被通知其运行环境中的 MessageSource（通常是 ApplicationContext）的对象必须实现的接口。
 *
 * <p>请注意，MessageSource 通常也可以作为 bean 引用传递（用于任意 bean 属性或构造函数参数），
 * 因为它在应用程序上下文中定义为名为 "messageSource" 的 bean。
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @see ApplicationContextAware
 * @since 1.1.1
 */
public interface MessageSourceAware extends Aware {

	/**
	 * 设置该对象所在的 MessageSource。
	 * <p>在普通 bean 属性填充之后但在初始化回调之前调用，比如 InitializingBean 的 afterPropertiesSet 或自定义的 init 方法。
	 * 在 ApplicationContextAware 的 setApplicationContext 之前调用。
	 *
	 * @param messageSource 该对象要使用的消息源
	 */
	void setMessageSource(MessageSource messageSource);

}
