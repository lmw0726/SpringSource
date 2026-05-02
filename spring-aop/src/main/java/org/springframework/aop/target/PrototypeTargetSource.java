/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.aop.target;

import org.springframework.beans.BeansException;

/**
 * {@link org.springframework.aop.TargetSource} 的实现，
 * 为每个请求创建目标 Bean 的新实例，
 * 并在释放（每次请求后）时销毁每个实例。
 *
 * <p>从其包含的 {@link org.springframework.beans.factory.BeanFactory} 中获取 Bean 实例。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #setBeanFactory
 * @see #setTargetBeanName
 */
@SuppressWarnings("serial")
public class PrototypeTargetSource extends AbstractPrototypeBasedTargetSource {

	/**
	 * 为每次调用获取一个新的原型实例。
	 * @see #newPrototypeInstance()
	 */
	@Override
	public Object getTarget() throws BeansException {
		return newPrototypeInstance();
	}

	/**
	 * 销毁给定的独立实例。
	 * @see #destroyPrototypeInstance
	 */
	@Override
	public void releaseTarget(Object target) {
		destroyPrototypeInstance(target);
	}

	@Override
	public String toString() {
		return "PrototypeTargetSource for target bean with name '" + getTargetBeanName() + "'";
	}

}
