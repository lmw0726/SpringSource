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

package org.springframework.beans.factory.aspectj;

import java.io.Serializable;

import org.aspectj.lang.annotation.control.CodeGenerationHint;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.AnnotationBeanWiringInfoResolver;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.wiring.BeanConfigurerSupport;

/**
 * 使用 {@link Configurable} 注解来识别哪些类需要自动装配（autowiring）的具体切面（aspect）。
 *
 * <p>如果指定了 {@code @Configurable} 注解，则将从该注解中获取要查找的 bean 名称；
 * 否则，默认要查找的 bean 名称为被配置类的全限定名。
 *
 * @author Rod Johnson
 * @author Ramnivas Laddad
 * @author Juergen Hoeller
 * @author Adrian Colyer
 * @since 2.0
 * @see org.springframework.beans.factory.annotation.Configurable
 * @see org.springframework.beans.factory.annotation.AnnotationBeanWiringInfoResolver
 */
public aspect AnnotationBeanConfigurerAspect extends AbstractInterfaceDrivenDependencyInjectionAspect
		implements BeanFactoryAware, InitializingBean, DisposableBean {

	private final BeanConfigurerSupport beanConfigurerSupport = new BeanConfigurerSupport();


	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanConfigurerSupport.setBeanWiringInfoResolver(new AnnotationBeanWiringInfoResolver());
		this.beanConfigurerSupport.setBeanFactory(beanFactory);
	}

	@Override
	public void afterPropertiesSet() {
		this.beanConfigurerSupport.afterPropertiesSet();
	}

	@Override
	public void configureBean(Object bean) {
		this.beanConfigurerSupport.configureBean(bean);
	}

	@Override
	public void destroy() {
		this.beanConfigurerSupport.destroy();
	}


	public pointcut inConfigurableBean() : @this(Configurable);

	public pointcut preConstructionConfiguration() : preConstructionConfigurationSupport(*);

	/*
	 * 用于匹配 preConstructionConfiguration 签名的中间层（该签名不暴露注解对象）
	 */
	@CodeGenerationHint(ifNameSuffix="bb0")
	private pointcut preConstructionConfigurationSupport(Configurable c) : @this(c) && if (c.preConstruction());


	declare parents: @Configurable * implements ConfigurableObject;

	/*
	 * 此声明本不应需要，除非遇到 AspectJ 的一个 bug（https://bugs.eclipse.org/bugs/show_bug.cgi?id=214559）
	 */
	declare parents: @Configurable Serializable+ implements ConfigurableDeserializationSupport;

}
