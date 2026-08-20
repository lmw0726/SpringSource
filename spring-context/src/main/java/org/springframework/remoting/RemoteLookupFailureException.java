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
 * RemoteAccessException 子类，在查找失败时抛出，
 * 通常在每次方法调用时按需进行查找的情况下发生。
 *
 * @author Juergen Hoeller
 * @since 1.1
 */
@SuppressWarnings("serial")
public class RemoteLookupFailureException extends RemoteAccessException {

	/**
	 * RemoteLookupFailureException 的构造函数。
	 * @param msg 详细信息
	 */
	public RemoteLookupFailureException(String msg) {
		super(msg);
	}

	/**
	 * RemoteLookupFailureException 的构造函数。
	 * @param msg 消息
	 * @param cause 来自使用的远程 API 的根本原因
	 */
	public RemoteLookupFailureException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
