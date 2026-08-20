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

package org.springframework.context;

import org.springframework.beans.factory.Aware;
import org.springframework.core.metrics.ApplicationStartup;

/**
 * 任何希望获知其运行时所使用的 {@link ApplicationStartup} 的对象，都应实现此接口。
 *
 * @author Brian Clozel
 * @since 5.3
 * @see ApplicationContextAware
 */
public interface ApplicationStartupAware extends Aware {

	/**
	 * 设置此对象运行时所使用的 ApplicationStartup。
	 * <p>在普通 bean 属性填充之后、初始化回调（如 InitializingBean 的 afterPropertiesSet 或自定义 init-method）之前调用。
	 * 在 ApplicationContextAware 的 setApplicationContext 之前调用。
	 * @param applicationStartup 此对象要使用的 applicationStartup
	 */
	void setApplicationStartup(ApplicationStartup applicationStartup);

}
