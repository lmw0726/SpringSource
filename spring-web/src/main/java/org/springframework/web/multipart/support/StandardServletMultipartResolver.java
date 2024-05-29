/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.web.multipart.support;

import org.apache.commons.logging.LogFactory;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.MultipartResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;

/**
 * 基于 Servlet 3.0 {@link javax.servlet.http.Part} API 的 {@link MultipartResolver} 接口的标准实现。
 * 要将其添加为 Spring DispatcherServlet 上下文中的 "multipartResolver" bean，无需在 bean 级别进行任何额外配置（见下文）。
 *
 * <p>此解析器变体直接使用您的 Servlet 容器的多部分解析器，可能会使应用程序暴露于容器实现差异。请参见
 * {@link org.springframework.web.multipart.commons.CommonsMultipartResolver}，该实现使用应用程序内部的本地 Commons
 * FileUpload 库，从而提供了对 Servlet 容器的最大可移植性。
 * 此外，参见此解析器的配置选项 {@link #setStrictServletCompliance strict Servlet 合规性}，将 Spring 的
 * {@link MultipartHttpServletRequest} 的适用范围缩小到仅适用于表单数据。
 *
 * <p><b>注意：</b>要使用基于 Servlet 3.0 的多部分解析，需要在 {@code web.xml} 中的受影响 servlet 中标记一个 "multipart-config" 部分，
 * 或者在程序化的 servlet 注册中使用 {@link javax.servlet.MultipartConfigElement}，或者（对于自定义 servlet 类）可能需要在您的 servlet 类上使用
 * {@link javax.servlet.annotation.MultipartConfig} 注解。诸如最大大小或存储位置之类的配置设置需要应用于该 servlet 注册级别；Servlet 3.0
 * 不允许在 MultipartResolver 级别设置这些设置。
 *
 * <pre class="code">
 * public class AppInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {
 * 	 // ...
 * 	 &#064;Override
 * 	 protected void customizeRegistration(ServletRegistration.Dynamic registration) {
 *     // Optionally also set maxFileSize, maxRequestSize, fileSizeThreshold
 *     registration.setMultipartConfig(new MultipartConfigElement("/tmp"));
 *   }
 * }
 * </pre>
 *
 * @author Juergen Hoeller
 * @see #setResolveLazily
 * @see #setStrictServletCompliance
 * @see HttpServletRequest#getParts()
 * @see org.springframework.web.multipart.commons.CommonsMultipartResolver
 * @since 3.1
 */
public class StandardServletMultipartResolver implements MultipartResolver {
	/**
	 * 是否延迟解析多部分请求
	 */
	private boolean resolveLazily = false;

	/**
	 * 此解析器是否应严格遵守 Servlet 规范
	 */
	private boolean strictServletCompliance = false;


	/**
	 * 设置是否在访问文件或参数时延迟解析多部分请求。
	 * <p>默认值为 "false"，即立即解析多部分元素，在调用 {@link #resolveMultipart} 时抛出相应的异常。
	 * 将此设置为 "true" 以进行延迟多部分解析，一旦应用程序尝试获取多部分文件或参数，就会抛出解析异常。
	 *
	 * @since 3.2.9
	 */
	public void setResolveLazily(boolean resolveLazily) {
		this.resolveLazily = resolveLazily;
	}

	/**
	 * 指定此解析器是否应严格遵守 Servlet 规范，仅适用于 "multipart/form-data" 请求。
	 * <p>默认值为 "false"，尝试处理任何具有 "multipart/" 内容类型的请求，只要底层 Servlet 容器支持它（例如，Tomcat 可以，但 Jetty 不行）。
	 * 为了保证一致的可移植性，特别是为了对 Spring 外的非表单多部分请求类型进行一致的自定义处理，将此标志切换为 "true"：
	 * 仅会将 "multipart/form-data" 请求包装在 {@link MultipartHttpServletRequest} 中；
	 * 其他类型的请求将保持不变，允许用户代码中的自定义处理。
	 * <p>请注意，Commons FileUpload 和因此 {@link org.springframework.web.multipart.commons.CommonsMultipartResolver}
	 * 支持任何 "multipart/" 请求类型。
	 * 但是，它将处理限制为 POST 请求，标准 Servlet 多部分解析器可能不会这样做。
	 *
	 * @since 5.3.9
	 */
	public void setStrictServletCompliance(boolean strictServletCompliance) {
		this.strictServletCompliance = strictServletCompliance;
	}


	@Override
	public boolean isMultipart(HttpServletRequest request) {
		return StringUtils.startsWithIgnoreCase(request.getContentType(),
				(this.strictServletCompliance ? MediaType.MULTIPART_FORM_DATA_VALUE : "multipart/"));
	}

	@Override
	public MultipartHttpServletRequest resolveMultipart(HttpServletRequest request) throws MultipartException {
		return new StandardMultipartHttpServletRequest(request, this.resolveLazily);
	}

	@Override
	public void cleanupMultipart(MultipartHttpServletRequest request) {
		if (!(request instanceof AbstractMultipartHttpServletRequest) ||
				((AbstractMultipartHttpServletRequest) request).isResolved()) {
			// 如果请求不是 AbstractMultipartHttpServletRequest 类型，并且该请求已经解析好了。
			// 为了安全起见：显式删除部分，但仅删除实际的文件部分（为了与 Resin 的兼容性）
			try {
				// 遍历每一个部分
				for (Part part : request.getParts()) {
					if (request.getFile(part.getName()) != null) {
						// 如果该多部分对应的文件存在，则将该文件删除
						part.delete();
					}
				}
			} catch (Throwable ex) {
				LogFactory.getLog(getClass()).warn("Failed to perform cleanup of multipart items", ex);
			}
		}
	}

}
