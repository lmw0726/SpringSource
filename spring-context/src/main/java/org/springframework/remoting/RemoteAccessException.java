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

package org.springframework.remoting;

import org.springframework.core.NestedRuntimeException;

/**
 * 通用远程访问异常。任意远程调用协议的服务代理都应抛出此异常或其子类，
 * 以便透明地暴露一个纯 Java 业务接口。
 *
 * <p>当使用规范代理时，切换实际的远程调用协议（例如从 Hessian 切换到其他协议）
 * 不会影响客户端代码。客户端使用服务暴露的纯自然 Java 业务接口。
 * 客户端对象只需通过 bean 引用即可获得所需接口的实现，
 * 与使用本地 bean 的方式相同。
 *
 * <p>客户端可以选择捕获 RemoteAccessException，但由于远程访问错误通常不可恢复，
 * 它可能会让此类异常传播到更高层级进行统一处理。
 * 在这种情况下，客户端代码不会显示任何参与远程访问的迹象，
 * 因为不存在远程调用相关的依赖。
 *
 * <p>即使将远程服务代理切换为同一接口的本地实现，这也只是配置层面的事情。
 * 显然，客户端代码应当意识到它 <i>可能在与远程服务交互</i>，
 * 例如重复方法调用会导致不必要的网络往返等。但是，它不需要知道
 * 自己 <i>实际在与远程服务还是本地实现交互</i>，
 * 也不需要关心底层使用的是哪种远程调用协议。
 *
 * @author Juergen Hoeller
 * @since 14.05.2003
 */
public class RemoteAccessException extends NestedRuntimeException {

	/** 使用 Spring 1.2 的 serialVersionUID 以保持互操作性。 */
	private static final long serialVersionUID = -4906825139312227864L;


	/**
	 * RemoteAccessException 的构造方法。
	 * @param msg 详细信息
	 */
	public RemoteAccessException(String msg) {
		super(msg);
	}

	/**
	 * RemoteAccessException 的构造方法。
	 * @param msg 详细信息
	 * @param cause 根本原因（通常来自底层远程调用 API，如 RMI）
	 */
	public RemoteAccessException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
