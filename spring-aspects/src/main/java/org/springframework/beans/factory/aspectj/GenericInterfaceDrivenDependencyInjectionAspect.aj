/*
 * Copyright 2002-2017 the original author or authors.
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

/**
 * 基于泛型的依赖注入切面（aspect）。
 *
 * <p>该切面允许用户在不使用 {@code @Configurable} 注解的情况下，
 * 实现高效、类型安全的依赖注入。
 *
 * <p>该切面的子切面无需包含任何 AOP 构造。例如，下面是一个配置
 * {@code PricingStrategyClient} 对象的子切面示例。
 *
 * <pre class="code">
 * aspect PricingStrategyDependencyInjectionAspect
 *        extends GenericInterfaceDrivenDependencyInjectionAspect<PricingStrategyClient> {
 *     private PricingStrategy pricingStrategy;
 *
 *     public void configure(PricingStrategyClient bean) {
 *         bean.setPricingStrategy(pricingStrategy);
 *     }
 *
 *     public void setPricingStrategy(PricingStrategy pricingStrategy) {
 *         this.pricingStrategy = pricingStrategy;
 *     }
 * }</pre>
 *
 * @author Ramnivas Laddad
 * @since 3.0
 */
public abstract aspect GenericInterfaceDrivenDependencyInjectionAspect<I> extends AbstractInterfaceDrivenDependencyInjectionAspect {

	declare parents: I implements ConfigurableObject;

	public pointcut inConfigurableBean() : within(I+);

	@SuppressWarnings("unchecked")
	public final void configureBean(Object bean) {
		configure((I) bean);
	}

	// 遗憾的是，与泛型一起使用的类型擦除（erasure）机制不允许使用同名的方法
	protected abstract void configure(I bean);

}
