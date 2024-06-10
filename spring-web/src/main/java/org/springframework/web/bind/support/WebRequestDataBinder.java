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

package org.springframework.web.bind.support;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MultipartRequest;
import org.springframework.web.multipart.support.StandardServletPartUtils;

import javax.servlet.http.HttpServletRequest;

/**
 * 专用于从web请求参数绑定到JavaBean对象的{@link org.springframework.validation.DataBinder}的特殊实现，包括对多部分文件的支持。
 *
 * <p><strong>警告</strong>：数据绑定可能会导致安全问题，因为它会暴露对象图中本不应由外部客户端访问或修改的部分。因此，设计和使用数据绑定时应该仔细考虑安全性。有关详细信息，请参阅参考手册中关于
 * <a href="https://docs.spring.io/spring-framework/docs/current/reference/html/web.html#mvc-ann-initbinder-model-design">Spring Web MVC</a> 和
 * <a href="https://docs.spring.io/spring-framework/docs/current/reference/html/web-reactive.html#webflux-ann-initbinder-model-design">Spring WebFlux</a>
 * 上数据绑定的专用章节。
 *
 * <p>请参阅DataBinder/WebDataBinder超类以获取自定义选项，其中包括指定允许/必需字段和注册自定义属性编辑器。
 *
 * <p>还可以用于自定义web控制器或构建在Spring的{@link org.springframework.web.context.request.WebRequest}抽象之上的拦截器中的手动数据绑定：
 * 例如，在{@link org.springframework.web.context.request.WebRequestInterceptor}实现中。
 * 只需为每个绑定过程实例化一个WebRequestDataBinder，并使用当前的WebRequest作为参数调用{@code bind}：
 *
 * <pre class="code">
 * MyBean myBean = new MyBean();
 * // 将绑定器应用于自定义目标对象
 * WebRequestDataBinder binder = new WebRequestDataBinder(myBean);
 * // 如果需要，注册自定义编辑器
 * binder.registerCustomEditor(...);
 * // 触发实际的请求参数绑定
 * binder.bind(request);
 * // 可选地评估绑定错误
 * Errors errors = binder.getErrors();
 * ...</pre>
 *
 * @author Juergen Hoeller
 * @author Brian Clozel
 * @see #bind(org.springframework.web.context.request.WebRequest)
 * @see #registerCustomEditor
 * @see #setAllowedFields
 * @see #setRequiredFields
 * @see #setFieldMarkerPrefix
 * @since 2.5.2
 */
public class WebRequestDataBinder extends WebDataBinder {

	/**
	 * 创建一个新的WebRequestDataBinder实例，使用默认的对象名称。
	 *
	 * @param target 要绑定到的目标对象（或{@code null}，如果绑定器仅用于转换普通参数值）
	 * @see #DEFAULT_OBJECT_NAME
	 */
	public WebRequestDataBinder(@Nullable Object target) {
		super(target);
	}

	/**
	 * 创建一个新的WebRequestDataBinder实例。
	 *
	 * @param target     要绑定到的目标对象（或{@code null}，如果绑定器仅用于转换普通参数值）
	 * @param objectName 目标对象的名称
	 */
	public WebRequestDataBinder(@Nullable Object target, String objectName) {
		super(target, objectName);
	}


	/**
	 * 将给定请求的参数绑定到此绑定器的目标，也会在多部分请求的情况下绑定多部分文件。
	 * <p>此调用可能会创建字段错误，代表基本绑定错误，例如必填字段（代码“required”）或值和bean属性之间的类型不匹配（代码“typeMismatch”）。
	 * <p>多部分文件通过它们的参数名称绑定，就像普通HTTP参数一样：即“uploadedFile”到“uploadedFile” bean属性，调用“setUploadedFile” setter方法。
	 * <p>多部分请求的目标属性的类型可以是MultipartFile、byte[]或String。当请求尚未通过MultipartResolver解析为MultipartRequest时，也支持Servlet Part绑定。
	 *
	 * @param request 要绑定参数的请求（可以是多部分的）
	 * @see org.springframework.web.multipart.MultipartRequest
	 * @see org.springframework.web.multipart.MultipartFile
	 * @see javax.servlet.http.Part
	 * @see #bind(org.springframework.beans.PropertyValues)
	 */
	public void bind(WebRequest request) {
		// 创建 可变属性值 对象，使用请求参数映射初始化
		MutablePropertyValues mpvs = new MutablePropertyValues(request.getParameterMap());

		// 如果请求是 原生Web请求 的实例
		if (request instanceof NativeWebRequest) {
			NativeWebRequest nativeRequest = (NativeWebRequest) request;

			// 获取类型为 多部分请求 的原生请求对象
			MultipartRequest multipartRequest = nativeRequest.getNativeRequest(MultipartRequest.class);

			// 如果 多部分请求 不为空
			if (multipartRequest != null) {
				// 将多文件映射绑定到 可变属性值
				bindMultipart(multipartRequest.getMultiFileMap(), mpvs);
			} else {
				// 如果请求头的 Content-Type 以 multipart/form-data 开头（忽略大小写）
				if (StringUtils.startsWithIgnoreCase(
						request.getHeader(HttpHeaders.CONTENT_TYPE), MediaType.MULTIPART_FORM_DATA_VALUE)) {
					// 获取原生 Servlet 请求对象
					HttpServletRequest servletRequest = nativeRequest.getNativeRequest(HttpServletRequest.class);

					// 如果 Servlet 请求对象不为空，且请求方法为 POST
					if (servletRequest != null && HttpMethod.POST.matches(servletRequest.getMethod())) {
						// 绑定请求的标准 Servlet 部分
						StandardServletPartUtils.bindParts(servletRequest, mpvs, isBindEmptyMultipartFiles());
					}
				}
			}
		}

		// 执行数据绑定
		doBind(mpvs);
	}

	/**
	 * 将错误视为致命。
	 * <p>只有在输入无效时才使用此方法。例如，如果所有输入都来自下拉菜单，则可能是适当的。
	 *
	 * @throws BindException 如果遇到绑定错误
	 */
	public void closeNoCatch() throws BindException {
		// 如果绑定结果中存在错误
		if (getBindingResult().hasErrors()) {
			// 抛出绑定异常
			throw new BindException(getBindingResult());
		}
	}

}
