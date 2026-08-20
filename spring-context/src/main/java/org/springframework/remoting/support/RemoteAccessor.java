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

package org.springframework.remoting.support;

import org.springframework.util.Assert;

/**
 * 访问远程服务的类的抽象基类。
 * 提供 "serviceInterface" bean 属性。
 *
 * <p>请注意，使用的服务接口会表现出一些远程调用的特征，
 * 例如它所提供的方法调用粒度。此外，它的参数必须是可序列化的。
 *
 * <p>在远程调用失败时，访问器应当抛出 Spring 的通用
 * {@link org.springframework.remoting.RemoteAccessException}，
 * 前提是服务接口没有声明 {@code java.rmi.RemoteException}。
 *
 * @author Juergen Hoeller
 * @since 13.05.2003
 * @see org.springframework.remoting.RemoteAccessException
 * @see java.rmi.RemoteException
 */
public abstract class RemoteAccessor extends RemotingSupport {


	private Class<?> serviceInterface;



	/**
	 * 设置要访问的服务接口。
	 * 该接口必须适合特定的服务和远程调用策略。
	 * <p>通常需要能够创建合适的服务代理，
	 * 但如果查找返回的是已类型化的代理，则也可以省略。
	 */
	public void setServiceInterface(Class<?> serviceInterface) {
		Assert.notNull(serviceInterface, "'serviceInterface' must not be null");
		Assert.isTrue(serviceInterface.isInterface(), "'serviceInterface' must be an interface");
		this.serviceInterface = serviceInterface;
	}

	/**
	 * 返回要访问的服务接口。
	 */
	public Class<?> getServiceInterface() {
		return this.serviceInterface;
	}

}
