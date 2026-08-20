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

package org.springframework.scripting.config;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.scripting.support.ScriptFactoryPostProcessor;

/**
 * 用于 {@link LangNamespaceHandler} 的工具类。
 *
 * @author Rob Harrop
 * @author Mark Fisher
 * @since 2.5
 */
public abstract class LangNamespaceUtils {

	/**
	 * 内部管理的 {@link ScriptFactoryPostProcessor} 在 {@link BeanDefinitionRegistry} 中注册时使用的唯一名称。
	 */
	private static final String SCRIPT_FACTORY_POST_PROCESSOR_BEAN_NAME =
			"org.springframework.scripting.config.scriptFactoryPostProcessor";


	/**
	 * 如果 {@link ScriptFactoryPostProcessor} 尚未注册，则在提供的
	 * {@link BeanDefinitionRegistry} 中注册一个 {@link ScriptFactoryPostProcessor} Bean 定义。
	 * @param registry 用于注册脚本处理器的 {@link BeanDefinitionRegistry}
	 * @return {@link ScriptFactoryPostProcessor} Bean 定义（新注册的或已存在的）
	 */
	public static BeanDefinition registerScriptFactoryPostProcessorIfNecessary(BeanDefinitionRegistry registry) {
		BeanDefinition beanDefinition;
		if (registry.containsBeanDefinition(SCRIPT_FACTORY_POST_PROCESSOR_BEAN_NAME)) {
			beanDefinition = registry.getBeanDefinition(SCRIPT_FACTORY_POST_PROCESSOR_BEAN_NAME);
		}
		else {
			beanDefinition = new RootBeanDefinition(ScriptFactoryPostProcessor.class);
			registry.registerBeanDefinition(SCRIPT_FACTORY_POST_PROCESSOR_BEAN_NAME, beanDefinition);
		}
		return beanDefinition;
	}

}
