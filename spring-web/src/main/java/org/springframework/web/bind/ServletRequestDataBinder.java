/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.web.bind;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.web.multipart.MultipartRequest;
import org.springframework.web.multipart.support.StandardServletPartUtils;
import org.springframework.web.util.WebUtils;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

/**
 * 专用于从Servlet请求参数将数据绑定到JavaBeans的{@link org.springframework.validation.DataBinder}的特殊实现，包括对多部分文件的支持。
 *
 * <p><strong>警告</strong>：数据绑定可能会导致安全问题，因为它会暴露对象图中本不应由外部客户端访问或修改的部分。因此，设计和使用数据绑定时应该仔细考虑安全性。有关详细信息，请参阅参考手册中关于
 * <a href="https://docs.spring.io/spring-framework/docs/current/reference/html/web.html#mvc-ann-initbinder-model-design">Spring Web MVC</a> 和
 * <a href="https://docs.spring.io/spring-framework/docs/current/reference/html/web-reactive.html#webflux-ann-initbinder-model-design">Spring WebFlux</a>
 * 上数据绑定的专用章节。
 *
 * <p>请参阅DataBinder/WebDataBinder超类以获取自定义选项，其中包括指定允许/必需字段和注册自定义属性编辑器。
 *
 * <p>还可以在自定义Web控制器中手动使用数据绑定：
 * 例如，在普通的Controller实现或MultiActionController处理程序方法中。只需为每个绑定过程实例化一个ServletRequestDataBinder，并使用当前的ServletRequest作为参数调用{@code bind}：
 *
 * <pre class="code">
 * MyBean myBean = new MyBean();
 * // 将绑定器应用于自定义目标对象
 * ServletRequestDataBinder binder = new ServletRequestDataBinder(myBean);
 * // 注册自定义编辑器，如果需要的话
 * binder.registerCustomEditor(...);
 * // 触发请求参数的实际绑定
 * binder.bind(request);
 * // 可选择评估绑定错误
 * Errors errors = binder.getErrors();
 * ...</pre>
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #bind(javax.servlet.ServletRequest)
 * @see #registerCustomEditor
 * @see #setAllowedFields
 * @see #setRequiredFields
 * @see #setFieldMarkerPrefix
 */
public class ServletRequestDataBinder extends WebDataBinder {

	/**
	 * 创建一个新的ServletRequestDataBinder实例，使用默认的对象名称。
	 *
	 * @param target 要绑定到的目标对象（或{@code null}，如果绑定器仅用于转换普通参数值）
	 * @see #DEFAULT_OBJECT_NAME
	 */
	public ServletRequestDataBinder(@Nullable Object target) {
		super(target);
	}

	/**
	 * 创建一个新的ServletRequestDataBinder实例。
	 *
	 * @param target     要绑定到的目标对象（或{@code null}，如果绑定器仅用于转换普通参数值）
	 * @param objectName 目标对象的名称
	 */
	public ServletRequestDataBinder(@Nullable Object target, String objectName) {
		super(target, objectName);
	}


	/**
	 * 将给定请求的参数绑定到此绑定器的目标，同时在多部分请求的情况下绑定多部分文件。
	 * <p>此调用可以创建字段错误，表示基本绑定错误，例如必填字段（代码“required”）或值与bean属性之间的类型不匹配（代码“typeMismatch”）。
	 * <p>多部分文件通过其参数名称绑定，就像普通的HTTP参数一样：例如，“uploadedFile”到“uploadedFile” bean属性，调用“setUploadedFile” setter方法。
	 * <p>多部分文件的目标属性类型可以是MultipartFile、byte[]或String。当请求未通过MultipartResolver解析为MultipartRequest时，也支持Servlet部分绑定。
	 *
	 * @param request 带有要绑定的参数的请求（可以是多部分的）
	 * @see org.springframework.web.multipart.MultipartHttpServletRequest
	 * @see org.springframework.web.multipart.MultipartRequest
	 * @see org.springframework.web.multipart.MultipartFile
	 * @see jakarta.servlet.http.Part
	 * @see #bind(org.springframework.beans.PropertyValues)
	 */
	public void bind(ServletRequest request) {
		// 使用请求创建可变属性值
		MutablePropertyValues mpvs = new ServletRequestParameterPropertyValues(request);
		// 获取原生的多部分请求
		MultipartRequest multipartRequest = WebUtils.getNativeRequest(request, MultipartRequest.class);
		// 如果存在多部分请求
		if (multipartRequest != null) {
			// 绑定多部分请求的文件映射
			bindMultipart(multipartRequest.getMultiFileMap(), mpvs);
		} else if (StringUtils.startsWithIgnoreCase(request.getContentType(), MediaType.MULTIPART_FORM_DATA_VALUE)) {
			HttpServletRequest httpServletRequest = WebUtils.getNativeRequest(request, HttpServletRequest.class);
			// 如果请求内容类型以multipart/form-data开头，且是POST方法
			if (httpServletRequest != null && HttpMethod.POST.matches(httpServletRequest.getMethod())) {
				// 绑定请求的标准部分
				StandardServletPartUtils.bindParts(httpServletRequest, mpvs, isBindEmptyMultipartFiles());
			}
		}
		// 添加绑定值
		addBindValues(mpvs, request);
		// 执行绑定
		doBind(mpvs);
	}

	/**
	 * 子类可以使用的扩展点，用于为请求添加额外的绑定值。在{@link #doBind(MutablePropertyValues)}之前调用。默认实现为空。
	 *
	 * @param mpvs    将用于数据绑定的属性值
	 * @param request 当前请求
	 */
	protected void addBindValues(MutablePropertyValues mpvs, ServletRequest request) {
	}

	/**
	 * 将错误视为致命的。
	 * <p>只有在输入无效时才使用此方法。如果所有输入都来自下拉框，那么这可能是合适的。
	 *
	 * @throws ServletRequestBindingException 任何绑定问题的ServletException子类
	 */
	public void closeNoCatch() throws ServletRequestBindingException {
		// 如果绑定结果中存在错误
		if (getBindingResult().hasErrors()) {
			// 抛出 ServletRequestBindingException 异常，指示绑定错误
			throw new ServletRequestBindingException(
					"Errors binding onto object '" + getBindingResult().getObjectName() + "'",
					new BindException(getBindingResult()));
		}
	}

}
