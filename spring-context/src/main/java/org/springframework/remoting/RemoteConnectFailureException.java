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
 * 当无法与远程服务建立连接时抛出的 RemoteAccessException 子类。
 *
 * @author Juergen Hoeller
 * @since 1.1
 */
@SuppressWarnings("serial")
public class RemoteConnectFailureException extends RemoteAccessException {

	/**
	 * RemoteConnectFailureException 的构造方法。
	 * @param msg 详细消息
	 * @param cause 来自所使用远程 API 的根本原因
	 */
	public RemoteConnectFailureException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
