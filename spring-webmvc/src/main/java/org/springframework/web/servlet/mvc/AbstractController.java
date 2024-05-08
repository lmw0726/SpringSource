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

package org.springframework.web.servlet.mvc;

import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.WebContentGenerator;
import org.springframework.web.util.WebUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * 方便的控制器实现的抽象类，使用模板方法设计模式。
 *
 * <p><b>工作流
 * (<a href="Controller.html#workflow">以及由接口定义的</a>)：</b><br>
 * <ol>
 * <li>{@link #handleRequest(HttpServletRequest, HttpServletResponse) handleRequest()} 将由 DispatcherServlet 调用</li>
 * <li>支持的方法检查（如果请求方法不受支持则抛出 ServletException）</li>
 * <li>如果需要会话，则尝试获取它（如果未找到则抛出 ServletException）</li>
 * <li>根据 cacheSeconds 属性需要的话设置缓存头</li>
 * <li>调用抽象方法 {@link #handleRequestInternal(HttpServletRequest, HttpServletResponse) handleRequestInternal()}，
 *     可以选择在 HttpSession 上同步调用（如果需要的话）</li>
 * </ol>
 *
 * <p><b><a name="config">公开的配置属性</a>
 * (<a href="Controller.html#config">以及由接口定义的</a>)：</b><br>
 * <table border="1">
 * <tr>
 * <td><b>名称</b></td>
 * <td><b>默认值</b></td>
 * <td><b>描述</b></td>
 * </tr>
 * <tr>
 * <td>supportedMethods</td>
 * <td>GET,POST</td>
 * <td>由逗号分隔的方法列表，此控制器支持的方法，例如 GET、POST 和 PUT</td>
 * </tr>
 * <tr>
 * <td>requireSession</td>
 * <td>false</td>
 * <td>请求是否需要会话才能由此控制器处理。这样确保派生的控制器可以调用 request.getSession() 来检索会话而不用担心空指针。如果在处理请求时找不到会话，则抛出 ServletException</td>
 * </tr>
 * <tr>
 * <td>cacheSeconds</td>
 * <td>-1</td>
 * <td>指示在此请求后响应的缓存头中包含的秒数。0（零）将包括完全不缓存的头，-1（默认值）不生成 <i>任何头</i>，任何正数将生成指定的秒数来缓存内容的头</td>
 * </tr>
 * <tr>
 * <td>synchronizeOnSession</td>
 * <td>false</td>
 * <td>是否应在会话上同步控制器执行，以序列化来自同一客户端的并行调用。
 * <p>具体地说，如果此标志为 "true"，则会在执行 {@code handleRequestInternal} 方法时同步。将使用最佳可用的会话互斥锁进行同步；
 * 理想情况下，这将是 HttpSessionMutexListener 暴露的互斥锁。
 * <p>会话互斥锁在会话的整个生命周期内保证是相同的对象，在 {@code SESSION_MUTEX_ATTRIBUTE} 常量定义的键下可用。
 * 它作为一个安全的引用用于对当前会话上的锁定进行同步。
 * <p>在许多情况下，HttpSession 引用本身也是一个安全的互斥锁，因为对于同一个活动逻辑会话来说，它始终是相同的对象引用。
 * 但是，在不同的 servlet 容器中，这不能保证；唯一 100% 安全的方法是会话互斥锁。
 *
 * @see AbstractController#handleRequestInternal
 * @see org.springframework.web.util.HttpSessionMutexListener
 * @see org.springframework.web.util.WebUtils#getSessionMutex(javax.servlet.http.HttpSession)
 */
public abstract class AbstractController extends WebContentGenerator implements Controller {

	/**
	 * 是否在会话级别中开启同步锁
	 */
	private boolean synchronizeOnSession = false;


	/**
	 * 创建一个新的 AbstractController，默认情况下支持 HTTP 方法 GET、HEAD 和 POST。
	 */
	public AbstractController() {
		this(true);
	}

	/**
	 * 创建一个新的 AbstractController。
	 *
	 * @param restrictDefaultSupportedMethods {@code true} 表示此控制器默认支持 HTTP 方法 GET、HEAD 和 POST，
	 *                                        {@code false} 表示它是无限制的
	 * @since 4.3
	 */
	public AbstractController(boolean restrictDefaultSupportedMethods) {
		super(restrictDefaultSupportedMethods);
	}


	/**
	 * 设置控制器执行是否应在会话上同步，以序列化来自同一客户端的并行调用。
	 * <p>具体地说，如果此标志为 "true"，则 {@code handleRequestInternal} 方法的执行将在同步块中。
	 * 将使用最佳可用的会话互斥锁进行同步；理想情况下，这将是 HttpSessionMutexListener 暴露的互斥锁。
	 * <p>会话互斥锁在会话的整个生命周期内保证是相同的对象，在 {@code SESSION_MUTEX_ATTRIBUTE} 常量定义的键下可用。
	 * 它作为一个安全的引用用于对当前会话上的锁定进行同步。
	 * <p>在许多情况下，HttpSession 引用本身也是一个安全的互斥锁，因为对于同一个活动逻辑会话来说，它始终是相同的对象引用。
	 * 但是，在不同的 servlet 容器中，这不能保证；唯一 100% 安全的方法是会话互斥锁。
	 *
	 * @see AbstractController#handleRequestInternal
	 * @see org.springframework.web.util.HttpSessionMutexListener
	 * @see org.springframework.web.util.WebUtils#getSessionMutex(javax.servlet.http.HttpSession)
	 */
	public final void setSynchronizeOnSession(boolean synchronizeOnSession) {
		this.synchronizeOnSession = synchronizeOnSession;
	}

	/**
	 * 返回控制器执行是否应在会话上同步。
	 */
	public final boolean isSynchronizeOnSession() {
		return this.synchronizeOnSession;
	}


	@Override
	@Nullable
	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response)
			throws Exception {

		if (HttpMethod.OPTIONS.matches(request.getMethod())) {
			// 对于 OPTIONS 请求，设置 Allow 头并返回 null
			response.setHeader("Allow", getAllowHeader());
			return null;
		}

		// 检查请求
		checkRequest(request);
		// 准备响应
		prepareResponse(response);

		// 如果需要，以同步块方式执行 handleRequestInternal。
		if (this.synchronizeOnSession) {
			// 获取当前请求的会话，如果存在的话
			HttpSession session = request.getSession(false);
			if (session != null) {
				// 获取会话级别的互斥锁对象
				Object mutex = WebUtils.getSessionMutex(session);
				synchronized (mutex) {
					// 在会话级别同步执行 handleRequestInternal
					return handleRequestInternal(request, response);
				}
			}
		}

		// 不需要同步会话，直接执行 handleRequestInternal
		return handleRequestInternal(request, response);
	}

	/**
	 * 模板方法。子类必须实现此方法。
	 * 合同与 {@code handleRequest} 相同。
	 *
	 * @see #handleRequest
	 */
	@Nullable
	protected abstract ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response)
			throws Exception;

}
