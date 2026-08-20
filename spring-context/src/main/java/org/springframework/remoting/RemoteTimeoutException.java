/*
 * Copyright 2002-2015 the original author or authors.
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
 * 当目标方法的执行在可配置的超时时间前未完成时抛出的RemoteAccessException子类，
 * 例如当未收到回复消息时。
 * @author Stephane Nicoll
 * @since 4.2
 */
@SuppressWarnings("serial")
public class RemoteTimeoutException extends RemoteAccessException {

	/**
	 * RemoteTimeoutException的构造函数。
	 * @param msg 详细信息
	 */
	public RemoteTimeoutException(String msg) {
		super(msg);
	}

	/**
	 * RemoteTimeoutException的构造函数。
	 * @param msg 详细信息
	 * @param cause 来自所使用远程API的根本原因
	 */
	public RemoteTimeoutException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
