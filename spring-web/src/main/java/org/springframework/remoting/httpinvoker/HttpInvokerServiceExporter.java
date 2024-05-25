/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.remoting.httpinvoker;

import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationResult;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.util.NestedServletException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;

/**
 * 基于 Servlet-API 的 HTTP 请求处理器，将指定的服务 bean 作为 HTTP 调用器服务端点导出，
 * 可通过 HTTP 调用器代理进行访问。
 *
 * <p>反序列化远程调用对象并序列化远程调用结果对象。使用 Java 序列化，就像 RMI 一样，
 * 但提供了与 Caucho 基于 HTTP 的 Hessian 协议相同的简单设置。
 *
 * <p><b>HTTP 调用器是 Java 到 Java 远程调用的推荐协议。</b>它比 Hessian 更强大、更可扩展，
 * 但代价是依赖于 Java。然而，它的设置和 Hessian 一样简单，这是它相对于 RMI 的主要优势。
 *
 * <p><b>警告：请注意由于不安全的 Java 反序列化而导致的漏洞：
 * 被操纵的输入流可能会在反序列化步骤中导致服务器上的不必要代码执行。因此，
 * 不要将 HTTP 调用器端点暴露给不受信任的客户端，而只是用于您自己的服务之间。</b>
 * 一般来说，我们强烈推荐使用任何其他消息格式（例如 JSON）来代替。
 *
 * @author Juergen Hoeller
 * @see HttpInvokerClientInterceptor
 * @see HttpInvokerProxyFactoryBean
 * @see org.springframework.remoting.rmi.RmiServiceExporter
 * @see org.springframework.remoting.caucho.HessianServiceExporter
 * @since 1.1
 * @deprecated 自 5.3 起（逐步淘汰基于序列化的远程调用）
 */
@Deprecated
public class HttpInvokerServiceExporter extends org.springframework.remoting.rmi.RemoteInvocationSerializingExporter implements HttpRequestHandler {

	/**
	 * 从请求中读取远程调用，执行它，并将远程调用结果写入响应。
	 *
	 * @see #readRemoteInvocation(HttpServletRequest)
	 * @see #invokeAndCreateResult(org.springframework.remoting.support.RemoteInvocation, Object)
	 * @see #writeRemoteInvocationResult(HttpServletRequest, HttpServletResponse, RemoteInvocationResult)
	 */
	@Override
	public void handleRequest(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		try {
			// 读取远程调用请求
			RemoteInvocation invocation = readRemoteInvocation(request);
			// 调用远程方法并创建结果
			RemoteInvocationResult result = invokeAndCreateResult(invocation, getProxy());
			// 写入远程调用结果
			writeRemoteInvocationResult(request, response, result);
		} catch (ClassNotFoundException ex) {
			// 处理反序列化过程中类未找到的异常
			throw new NestedServletException("Class not found during deserialization", ex);
		}
	}

	/**
	 * 从给定的 HTTP 请求中读取远程调用。
	 * <p>委托给 {@link #readRemoteInvocation(HttpServletRequest, InputStream)}，
	 * 使用 {@link HttpServletRequest#getInputStream() servlet 请求的输入流}。
	 *
	 * @param request 当前的 HTTP 请求
	 * @return 远程调用对象
	 * @throws IOException            在 I/O 失败的情况下
	 * @throws ClassNotFoundException 如果反序列化期间抛出此异常
	 */
	protected RemoteInvocation readRemoteInvocation(HttpServletRequest request)
			throws IOException, ClassNotFoundException {

		return readRemoteInvocation(request, request.getInputStream());
	}

	/**
	 * 从给定的输入流反序列化远程调用对象。
	 * <p>首先给 {@link #decorateInputStream} 一个装饰流的机会
	 * （例如用于自定义加密或压缩）。创建一个
	 * {@link org.springframework.remoting.rmi.CodebaseAwareObjectInputStream}，
	 * 并调用 {@link #doReadRemoteInvocation} 实际读取对象。
	 * <p>可以重写以实现调用的自定义序列化。
	 *
	 * @param request 当前的 HTTP 请求
	 * @param is      输入流
	 * @return 远程调用对象
	 * @throws IOException            在 I/O 失败的情况下
	 * @throws ClassNotFoundException 如果反序列化期间抛出此异常
	 */
	protected RemoteInvocation readRemoteInvocation(HttpServletRequest request, InputStream is)
			throws IOException, ClassNotFoundException {

		try (ObjectInputStream ois = createObjectInputStream(decorateInputStream(request, is))) {
			// 读取并返回远程调用
			return doReadRemoteInvocation(ois);
		}
	}

	/**
	 * 返回用于读取远程调用的输入流，可能装饰给定的原始输入流。
	 * <p>默认实现返回给定的流。可以重写，例如用于自定义加密或压缩。
	 *
	 * @param request 当前的 HTTP 请求
	 * @param is      原始输入流
	 * @return 可能装饰的输入流
	 * @throws IOException 在 I/O 失败的情况下
	 */
	protected InputStream decorateInputStream(HttpServletRequest request, InputStream is) throws IOException {
		return is;
	}

	/**
	 * 将给定的远程调用结果写入给定的 HTTP 响应。
	 *
	 * @param request  当前的 HTTP 请求
	 * @param response 当前的 HTTP 响应
	 * @param result   远程调用结果对象
	 * @throws IOException 在 I/O 失败的情况下
	 */
	protected void writeRemoteInvocationResult(
			HttpServletRequest request, HttpServletResponse response, RemoteInvocationResult result)
			throws IOException {

		// 设置响应的内容类型
		response.setContentType(getContentType());
		// 将远程调用结果写入响应输出流
		writeRemoteInvocationResult(request, response, result, response.getOutputStream());
	}

	/**
	 * 将给定的远程调用结果序列化到给定的输出流。
	 * <p>默认实现首先给 {@link #decorateOutputStream} 一个装饰流的机会
	 * （例如用于自定义加密或压缩）。为最终流创建一个
	 * {@link java.io.ObjectOutputStream}，并调用
	 * {@link #doWriteRemoteInvocationResult} 实际写入对象。
	 * <p>可以重写以实现调用结果的自定义序列化。
	 *
	 * @param request  当前的 HTTP 请求
	 * @param response 当前的 HTTP 响应
	 * @param result   远程调用结果对象
	 * @param os       输出流
	 * @throws IOException 在 I/O 失败的情况下
	 * @see #decorateOutputStream
	 * @see #doWriteRemoteInvocationResult
	 */
	protected void writeRemoteInvocationResult(
			HttpServletRequest request, HttpServletResponse response, RemoteInvocationResult result, OutputStream os)
			throws IOException {

		// 创建对象输出流，该流用完后会自动关闭
		try (ObjectOutputStream oos =
					 createObjectOutputStream(new FlushGuardedOutputStream(decorateOutputStream(request, response, os)))) {
			// 写入远程调用结果到输出流
			doWriteRemoteInvocationResult(result, oos);
		}
	}

	/**
	 * 返回用于写入远程调用结果的输出流，可能装饰给定的原始输出流。
	 * <p>默认实现返回给定的流。可以重写，例如用于自定义加密或压缩。
	 *
	 * @param request  当前的 HTTP 请求
	 * @param response 当前的 HTTP 响应
	 * @param os       原始输出流
	 * @return 可能装饰的输出流
	 * @throws IOException 在 I/O 失败的情况下
	 */
	protected OutputStream decorateOutputStream(
			HttpServletRequest request, HttpServletResponse response, OutputStream os) throws IOException {

		return os;
	}


	/**
	 * 装饰 {@code OutputStream} 以防止 {@code flush()} 调用，将其变成无操作。
	 * <p>因为 {@link ObjectOutputStream#close()} 实际上会两次刷新/排空底层流，
	 * 这个 {@link FilterOutputStream} 将防止单独的 flush 调用。
	 * 多次 flush 调用可能会导致性能问题，因为写操作没有像应有的那样聚集。
	 *
	 * @see <a href="https://jira.spring.io/browse/SPR-14040">SPR-14040</a>
	 */
	private static class FlushGuardedOutputStream extends FilterOutputStream {

		public FlushGuardedOutputStream(OutputStream out) {
			super(out);
		}

		@Override
		public void flush() throws IOException {
			// 在 flush 时不执行任何操作
		}
	}

}
