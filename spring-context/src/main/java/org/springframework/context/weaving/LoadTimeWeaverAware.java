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

package org.springframework.context.weaving;

import org.springframework.beans.factory.Aware;
import org.springframework.instrument.classloading.LoadTimeWeaver;

/**
 * 需要被通知应用上下文默认 {@link LoadTimeWeaver} 的任何对象都应实现此接口。
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 2.5
 * @see org.springframework.context.ConfigurableApplicationContext#LOAD_TIME_WEAVER_BEAN_NAME
 */
public interface LoadTimeWeaverAware extends Aware {

	/**
	 * 设置此对象所在 {@link org.springframework.context.ApplicationContext ApplicationContext} 的 {@link LoadTimeWeaver}。
	 * <p>在普通 bean 属性填充之后、初始化回调（如
	 * {@link org.springframework.beans.factory.InitializingBean InitializingBean 的}
	 * {@link org.springframework.beans.factory.InitializingBean#afterPropertiesSet() afterPropertiesSet()}
	 * 或自定义 init-method）之前被调用。在
	 * {@link org.springframework.context.ApplicationContextAware ApplicationContextAware 的}
	 * {@link org.springframework.context.ApplicationContextAware#setApplicationContext setApplicationContext(..)} 之后被调用。
	 * <p><b>注意：</b>此方法仅在应用上下文中确实存在
	 * {@code LoadTimeWeaver} 时才会被调用。如果不存在，该方法将不会被调用，
	 * 假设实现对象能够自行激活其织入依赖。
	 * @param loadTimeWeaver {@code LoadTimeWeaver} 实例（不为 {@code null}）
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet
	 * @see org.springframework.context.ApplicationContextAware#setApplicationContext
	 */
	void setLoadTimeWeaver(LoadTimeWeaver loadTimeWeaver);

}
