/*
 * Copyright 2002-2016 the original author or authors.
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

import java.util.HashSet;
import java.util.Set;

/**
 * 用于处理远程调用的通用工具类。
 *
 * <p>主要供远程调用框架内部使用。
 *
 * @author Juergen Hoeller
 * @since 2.0
 */
public abstract class RemoteInvocationUtils {

	/**
	 * 将当前客户端堆栈跟踪信息填充到给定的异常中。
	 * <p>给定的异常通常在服务器上抛出并原样序列化，客户端希望它也包含客户端堆栈跟踪信息。
	 * 我们可以在此处更新 {@code StackTraceElement} 数组，添加当前客户端堆栈跟踪信息，
	 * 前提是我们在 JDK 1.4+ 环境下运行。
	 * @param ex 要更新的异常
	 * @see Throwable#getStackTrace()
	 * @see Throwable#setStackTrace(StackTraceElement[])
	 */
	public static void fillInClientStackTraceIfPossible(Throwable ex) {
		if (ex != null) {
			StackTraceElement[] clientStack = new Throwable().getStackTrace();
			Set<Throwable> visitedExceptions = new HashSet<>();
			Throwable exToUpdate = ex;
			while (exToUpdate != null && !visitedExceptions.contains(exToUpdate)) {
				StackTraceElement[] serverStack = exToUpdate.getStackTrace();
				StackTraceElement[] combinedStack = new StackTraceElement[serverStack.length + clientStack.length];
				System.arraycopy(serverStack, 0, combinedStack, 0, serverStack.length);
				System.arraycopy(clientStack, 0, combinedStack, serverStack.length, clientStack.length);
				exToUpdate.setStackTrace(combinedStack);
				visitedExceptions.add(exToUpdate);
				exToUpdate = exToUpdate.getCause();
			}
		}
	}

}
