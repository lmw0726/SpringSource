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

package org.springframework.remoting.soap;

import javax.xml.namespace.QName;

import org.springframework.remoting.RemoteInvocationFailureException;

/**
 * RemoteInvocationFailureException 的子类，提供 SOAP 故障的详细信息。
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see javax.xml.rpc.soap.SOAPFaultException
 * @see javax.xml.ws.soap.SOAPFaultException
 */
@SuppressWarnings("serial")
public abstract class SoapFaultException extends RemoteInvocationFailureException {

	/**
	 * SoapFaultException 的构造方法。
	 * @param msg 详细消息
	 * @param cause 来自所使用的 SOAP API 的根本原因
	 */
	protected SoapFaultException(String msg, Throwable cause) {
		super(msg, cause);
	}


	/**
	 * 返回 SOAP 故障代码。
	 */
	public abstract String getFaultCode();

	/**
	 * 以 {@code QName} 对象的形式返回 SOAP 故障代码。
	 */
	public abstract QName getFaultCodeAsQName();

	/**
	 * 返回描述性的 SOAP 故障字符串。
	 */
	public abstract String getFaultString();

	/**
	 * 返回导致此故障的参与者。
	 */
	public abstract String getFaultActor();

}
