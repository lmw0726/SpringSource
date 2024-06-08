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

package org.springframework.web.context.request;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.Scope;
import org.springframework.lang.Nullable;

/**
 * 抽象的 {@link Scope} 实现，从当前线程绑定的 {@link RequestAttributes} 对象的特定范围中读取。
 *
 * <p>子类只需要实现 {@link #getScope()} 方法，以指示此类从哪个 {@link RequestAttributes} 范围中读取属性。
 *
 * <p>子类可能希望重写 {@link #get} 和 {@link #remove} 方法，在调用此超类时添加同步。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @since 2.0
 */
public abstract class AbstractRequestAttributesScope implements Scope {

	@Override
	public Object get(String name, ObjectFactory<?> objectFactory) {
		// 获取当前请求的属性
		RequestAttributes attributes = RequestContextHolder.currentRequestAttributes();
		// 根据名称和作用域获取作用域对象
		Object scopedObject = attributes.getAttribute(name, getScope());
		// 如果作用域对象为空
		if (scopedObject == null) {
			// 从对象工厂获取对象
			scopedObject = objectFactory.getObject();
			// 将对象设置为属性
			attributes.setAttribute(name, scopedObject, getScope());
			// 再次尝试获取属性，以确保在注册为隐式会话属性更新时对象仍然存在
			// 重新获取对象，以注册为隐式会话属性更新。
			// 此外，我们还允许在 getAttribute 级别进行潜在的装饰。
			Object retrievedObject = attributes.getAttribute(name, getScope());
			if (retrievedObject != null) {
				// 只有在仍然存在的情况下才继续获取对象（预期情况）。
				// 如果并发地消失了，则返回我们在本地创建的实例。
				// 如果对象仍然存在，则使用已检索到的对象
				scopedObject = retrievedObject;
			}
		}
		// 返回作用域对象
		return scopedObject;
	}

	@Override
	@Nullable
	public Object remove(String name) {
		// 获取当前请求的属性
		RequestAttributes attributes = RequestContextHolder.currentRequestAttributes();
		// 获取指定作用域下的属性对象
		Object scopedObject = attributes.getAttribute(name, getScope());
		// 如果属性对象不为空
		if (scopedObject != null) {
			// 从请求属性中移除指定作用域下的属性
			attributes.removeAttribute(name, getScope());
			// 返回移除的属性对象
			return scopedObject;
		} else {
			// 如果属性对象为空，则返回 null
			return null;
		}
	}

	@Override
	public void registerDestructionCallback(String name, Runnable callback) {
		// 获取当前请求的属性
		RequestAttributes attributes = RequestContextHolder.currentRequestAttributes();
		// 注册销毁回调函数，以便在作用域销毁时执行
		attributes.registerDestructionCallback(name, callback, getScope());
	}

	@Override
	@Nullable
	public Object resolveContextualObject(String key) {
		// 获取当前请求的属性
		RequestAttributes attributes = RequestContextHolder.currentRequestAttributes();
		// 解析引用并返回结果
		return attributes.resolveReference(key);
	}


	/**
	 * 模板方法，确定实际的目标范围。
	 *
	 * @return 目标范围，以适当的 {@link RequestAttributes} 常量形式返回
	 * @see RequestAttributes#SCOPE_REQUEST
	 * @see RequestAttributes#SCOPE_SESSION
	 */
	protected abstract int getScope();

}
