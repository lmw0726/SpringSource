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

package org.springframework.web.context.support;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * {@link org.springframework.context.support.LiveBeansView} 的 Servlet 变体的 MBean 暴露。
 *
 * <p>为当前 Web 应用程序中所有 ApplicationContext 中的当前 bean 及其依赖项生成 JSON 快照。
 *
 * @author Juergen Hoeller
 * @see org.springframework.context.support.LiveBeansView#getSnapshotAsJson()
 * @since 3.2
 * @deprecated 自 5.3 起，建议使用 Spring Boot 执行器来满足此类需求
 */
@Deprecated
@SuppressWarnings("serial")
public class LiveBeansViewServlet extends HttpServlet {
	/**
	 * live Bean 视图
	 */
	@Nullable
	private org.springframework.context.support.LiveBeansView liveBeansView;


	@Override
	public void init() throws ServletException {
		this.liveBeansView = buildLiveBeansView();
	}

	protected org.springframework.context.support.LiveBeansView buildLiveBeansView() {
		return new ServletContextLiveBeansView(getServletContext());
	}


	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		Assert.state(this.liveBeansView != null, "No LiveBeansView available");

		// 获取 LiveBeansView 快照的 JSON 内容
		String content = this.liveBeansView.getSnapshotAsJson();

		// 设置响应内容类型为 JSON
		response.setContentType("application/json");

		// 设置响应内容长度
		response.setContentLength(content.length());

		// 将 JSON 内容写入响应
		response.getWriter().write(content);
	}

}
