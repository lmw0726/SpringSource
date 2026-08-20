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

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.EnableLoadTimeWeaving.AspectJWeaving;
import org.springframework.context.weaving.AspectJWeavingEnabler;
import org.springframework.context.weaving.DefaultContextLoadTimeWeaver;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.instrument.classloading.LoadTimeWeaver;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 注册 {@link LoadTimeWeaver} bean 的 {@code @Configuration} 配置类。
 *
 * <p>当使用 {@link EnableLoadTimeWeaving} 注解时，此配置类会被自动导入。
 * 详细的使用说明请参阅 {@code @EnableLoadTimeWeaving} 的 javadoc。
 *
 * @author Chris Beams
 * @since 3.1
 * @see LoadTimeWeavingConfigurer
 * @see ConfigurableApplicationContext#LOAD_TIME_WEAVER_BEAN_NAME
 */
@Configuration(proxyBeanMethods = false)
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class LoadTimeWeavingConfiguration implements ImportAware, BeanClassLoaderAware {

	@Nullable
	private AnnotationAttributes enableLTW;

	@Nullable
	private LoadTimeWeavingConfigurer ltwConfigurer;

	@Nullable
	private ClassLoader beanClassLoader;


	@Override
	public void setImportMetadata(AnnotationMetadata importMetadata) {
		this.enableLTW = AnnotationConfigUtils.attributesFor(importMetadata, EnableLoadTimeWeaving.class);
		if (this.enableLTW == null) {
			throw new IllegalArgumentException(
					"@EnableLoadTimeWeaving is not present on importing class " + importMetadata.getClassName());
		}
	}

	@Autowired(required = false)
	public void setLoadTimeWeavingConfigurer(LoadTimeWeavingConfigurer ltwConfigurer) {
		this.ltwConfigurer = ltwConfigurer;
	}

	@Override
	public void setBeanClassLoader(ClassLoader beanClassLoader) {
		this.beanClassLoader = beanClassLoader;
	}


	@Bean(name = ConfigurableApplicationContext.LOAD_TIME_WEAVER_BEAN_NAME)
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	public LoadTimeWeaver loadTimeWeaver() {
		Assert.state(this.beanClassLoader != null, "No ClassLoader set");
		LoadTimeWeaver loadTimeWeaver = null;

		if (this.ltwConfigurer != null) {
			// 用户提供了自定义的 LoadTimeWeaver 实例
			loadTimeWeaver = this.ltwConfigurer.getLoadTimeWeaver();
		}

		if (loadTimeWeaver == null) {
			// 未提供自定义 LoadTimeWeaver -> 回退到默认实现
			loadTimeWeaver = new DefaultContextLoadTimeWeaver(this.beanClassLoader);
		}

		if (this.enableLTW != null) {
			AspectJWeaving aspectJWeaving = this.enableLTW.getEnum("aspectjWeaving");
			switch (aspectJWeaving) {
				case DISABLED:
					// AJ 织入已禁用 -> 不做任何操作
					break;
				case AUTODETECT:
					if (this.beanClassLoader.getResource(AspectJWeavingEnabler.ASPECTJ_AOP_XML_RESOURCE) == null) {
						// classpath 中不存在 aop.xml -> 视为"禁用"状态
						break;
					}
					// classpath 中存在 aop.xml -> 启用织入
					AspectJWeavingEnabler.enableAspectJWeaving(loadTimeWeaver, this.beanClassLoader);
					break;
				case ENABLED:
					AspectJWeavingEnabler.enableAspectJWeaving(loadTimeWeaver, this.beanClassLoader);
					break;
			}
		}

		return loadTimeWeaver;
	}

}
