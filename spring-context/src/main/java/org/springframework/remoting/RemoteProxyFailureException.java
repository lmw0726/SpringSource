/*
 * Copyright 2002-2012 the original author or authors.
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

/**
 * RemoteAccessException 子类，在客户端代理用于远程服务失败时抛出，
 * 例如当底层 RMI stub 上找不到方法时。
 *
 * @author Juergen Hoeller
 * @since 1.2.8
 * @see RemoteInvocationFailureException
 */
@SuppressWarnings("serial")
public class RemoteProxyFailureException extends RemoteAccessException {

	/**
	 * RemoteProxyFailureException 的构造函数。
	 * @param msg 详细信息
	 * @param cause 来自所使用远程 API 的根本原因
	 */
	public RemoteProxyFailureException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
