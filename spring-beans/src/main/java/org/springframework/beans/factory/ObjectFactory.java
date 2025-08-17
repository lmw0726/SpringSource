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

package org.springframework.beans.factory;

import org.springframework.beans.BeansException;

/**
 * 定义一个工厂，当被调用时可以返回一个对象实例
 * （可能是共享的或独立的）。
 *
 * <p>此接口通常用于封装一个通用工厂，该工厂在每次调用时
 * 返回某个目标对象的新实例（原型）。
 *
 * <p>此接口类似于{@link FactoryBean}，但后者的实现通常
 * 意味着被定义为{@link BeanFactory}中的SPI实例，而此类的实现通常
 * 意味着作为API提供给其他bean（通过注入）。因此，
 * {@code getObject()}方法具有不同的异常处理行为。
 *
 * @param <T> 对象类型
 * @author Colin Sampaleanu
 * @see FactoryBean
 * @since 1.0.2
 */
@FunctionalInterface
public interface ObjectFactory<T> {

	/**
	 * 返回此工厂管理的对象的实例 (可能是共享的或独立的)。
	 *
	 * @return 结果实例
	 * @throws BeansException 在创建错误的情况下
	 */
	T getObject() throws BeansException;

}
