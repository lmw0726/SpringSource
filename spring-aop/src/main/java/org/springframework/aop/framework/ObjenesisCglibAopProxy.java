/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.aop.framework;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cglib.proxy.Callback;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.Factory;
import org.springframework.objenesis.SpringObjenesis;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Constructor;

/**
 * {@link CglibAopProxy} 基于 Objenesis 的扩展，用于创建代理实例
 * 而不调用类的构造函数。自 Spring 4 起默认使用。
 *
 * @author Oliver Gierke
 * @author Juergen Hoeller
 * @since 4.0
 */
@SuppressWarnings("serial")
class ObjenesisCglibAopProxy extends CglibAopProxy {

	private static final Log logger = LogFactory.getLog(ObjenesisCglibAopProxy.class);

	private static final SpringObjenesis objenesis = new SpringObjenesis();


	/**
	 * 为给定 AOP 配置创建新的 ObjenesisCglibAopProxy。
	 * @param config 作为 AdvisedSupport 对象的 AOP 配置
	 */
	public ObjenesisCglibAopProxy(AdvisedSupport config) {
		super(config);
	}


	@Override
	protected Object createProxyClassAndInstance(Enhancer enhancer, Callback[] callbacks) {

		// ===================== 1. 生成代理类（字节码） =====================
		// 通过 CGLIB Enhancer 动态生成代理类（子类）
		Class<?> proxyClass = enhancer.createClass();

		// 代理实例（稍后创建）
		Object proxyInstance = null;

		// ===================== 2. 优先使用 Objenesis 创建实例 =====================
		// 判断是否值得尝试使用 Objenesis（无构造器实例化）
		if (objenesis.isWorthTrying()) {
			try {
				// 使用 Objenesis 创建对象（不会调用构造函数！）
				proxyInstance = objenesis.newInstance(proxyClass, enhancer.getUseCache());
			}
			catch (Throwable ex) {
				// 如果失败，记录 debug 日志，后面会 fallback
				logger.debug("Unable to instantiate proxy using Objenesis, " +
						"falling back to regular proxy construction", ex);
			}
		}

		// ===================== 3. 如果 Objenesis 失败，使用反射创建 =====================
		if (proxyInstance == null) {
			// 通过默认构造函数进行常规实例化...
			try {
				// 如果存在构造参数
				Constructor<?> ctor = (this.constructorArgs != null ?
						// 获取带参数构造器
						proxyClass.getDeclaredConstructor(this.constructorArgTypes) :
						// 获取无参构造器
						proxyClass.getDeclaredConstructor());

				// 设置构造器可访问（private 也能调用）
				ReflectionUtils.makeAccessible(ctor);

				// 创建实例（有参 or 无参）
				proxyInstance = (this.constructorArgs != null ?
						ctor.newInstance(this.constructorArgs) :
						ctor.newInstance());
			}
			catch (Throwable ex) {
				// 如果连反射创建都失败 → 抛异常
				throw new AopConfigException("Unable to instantiate proxy using Objenesis, " +
						"and regular proxy instantiation via default constructor fails as well", ex);
			}
		}

		// ===================== 4. 设置回调（拦截器链） =====================
		// CGLIB 代理类实现了 Factory 接口
		// 这里把 callbacks（拦截器）注入进去
		((Factory) proxyInstance).setCallbacks(callbacks);

		// ===================== 5. 返回代理对象 =====================
		return proxyInstance;
	}

}
