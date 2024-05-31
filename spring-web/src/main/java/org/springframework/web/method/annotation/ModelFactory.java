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

package org.springframework.web.method.annotation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.core.Conventions;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.HttpSessionRequiredException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.support.InvocableHandlerMethod;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.lang.reflect.Method;
import java.util.*;

/**
 * 在控制器方法调用前协助初始化{@link Model}并在调用后对其进行更新。
 *
 * <p>初始化时，模型会通过会话中临时存储的属性和调用{@code @ModelAttribute}方法来填充。
 *
 * <p>更新时，模型属性会与会话同步，如果缺少{@link BindingResult}属性，也会将其添加到模型中。
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public final class ModelFactory {
	/**
	 * 日志记录器
	 */
	private static final Log logger = LogFactory.getLog(ModelFactory.class);

	/**
	 * 模型方法列表
	 */
	private final List<ModelMethod> modelMethods = new ArrayList<>();

	/**
	 * 数据绑定工厂
	 */
	private final WebDataBinderFactory dataBinderFactory;

	/**
	 * 会话属性处理器
	 */
	private final SessionAttributesHandler sessionAttributesHandler;


	/**
	 * 使用给定的{@code @ModelAttribute}方法创建一个新实例。
	 *
	 * @param handlerMethods   要调用的{@code @ModelAttribute}方法
	 * @param binderFactory    用于准备{@link BindingResult}属性
	 * @param attributeHandler 用于访问会话属性
	 */
	public ModelFactory(@Nullable List<InvocableHandlerMethod> handlerMethods,
						WebDataBinderFactory binderFactory, SessionAttributesHandler attributeHandler) {

		if (handlerMethods != null) {
			// 如果处理器方法列表不为空，遍历处理器方法
			for (InvocableHandlerMethod handlerMethod : handlerMethods) {
				// 将处理器方法撰文模型方法，并添加到模型方法列表中
				this.modelMethods.add(new ModelMethod(handlerMethod));
			}
		}
		this.dataBinderFactory = binderFactory;
		this.sessionAttributesHandler = attributeHandler;
	}


	/**
	 * 按以下顺序填充模型：
	 * <ol>
	 * <li>检索列为{@code @SessionAttributes}的“已知”会话属性。
	 * <li>调用{@code @ModelAttribute}方法
	 * <li>查找列为{@code @SessionAttributes}的{@code @ModelAttribute}方法参数，
	 * 确保它们存在于模型中，如果有必要则引发异常。
	 * </ol>
	 *
	 * @param request       当前请求
	 * @param container     包含要初始化的模型的容器
	 * @param handlerMethod 要初始化模型的方法
	 * @throws Exception 可能由{@code @ModelAttribute}方法引发
	 */
	public void initModel(NativeWebRequest request, ModelAndViewContainer container, HandlerMethod handlerMethod)
			throws Exception {

		// 从会话属性处理器中检索会话属性
		Map<String, ?> sessionAttributes = this.sessionAttributesHandler.retrieveAttributes(request);
		// 将会话属性合并到容器中
		container.mergeAttributes(sessionAttributes);
		// 调用模型属性方法
		invokeModelAttributeMethods(request, container);

		// 遍历会话属性参数名称
		for (String name : findSessionAttributeArguments(handlerMethod)) {
			// 如果容器中不包含该属性
			if (!container.containsAttribute(name)) {
				// 从会话属性处理器中检索该属性
				Object value = this.sessionAttributesHandler.retrieveAttribute(request, name);
				// 如果该属性值为空，抛出HttpSessionRequiredException异常
				if (value == null) {
					throw new HttpSessionRequiredException("Expected session attribute '" + name + "'", name);
				}
				// 将该属性添加到容器中
				container.addAttribute(name, value);
			}
		}
	}

	/**
	 * 调用模型属性方法来填充模型。
	 * 仅当模型中尚不存在属性时才添加属性。
	 */
	private void invokeModelAttributeMethods(NativeWebRequest request, ModelAndViewContainer container)
			throws Exception {

		// 当模型方法列表不为空时循环
		while (!this.modelMethods.isEmpty()) {
			// 获取下一个模型方法
			InvocableHandlerMethod modelMethod = getNextModelMethod(container).getHandlerMethod();
			// 获取模型方法上的 @ModelAttribute 注解
			ModelAttribute ann = modelMethod.getMethodAnnotation(ModelAttribute.class);
			// 确保模型方法上有 @ModelAttribute 注解
			Assert.state(ann != null, "No ModelAttribute annotation");
			// 如果容器中包含该注解的属性名
			if (container.containsAttribute(ann.name())) {
				// 如果注解的 binding 属性为 false，禁用绑定
				if (!ann.binding()) {
					container.setBindingDisabled(ann.name());
				}
				// 跳过当前循环
				continue;
			}

			// 为请求调用模型方法
			Object returnValue = modelMethod.invokeForRequest(request, container);
			// 如果模型方法返回类型为 void
			if (modelMethod.isVoid()) {
				// 如果注解的值不为空字符串
				if (StringUtils.hasText(ann.value())) {
					// 记录日志信息，表明 @ModelAttribute 注解的名称被忽略，因为方法返回 void
					if (logger.isDebugEnabled()) {
						logger.debug("Name in @ModelAttribute is ignored because method returns void: " +
								modelMethod.getShortLogMessage());
					}
				}
				// 跳过当前循环
				continue;
			}

			// 获取返回值的名称
			String returnValueName = getNameForReturnValue(returnValue, modelMethod.getReturnType());
			// 如果注解的 binding 属性为 false，禁用绑定
			if (!ann.binding()) {
				container.setBindingDisabled(returnValueName);
			}
			// 如果容器中不包含返回值的名称，将返回值添加到容器中
			if (!container.containsAttribute(returnValueName)) {
				container.addAttribute(returnValueName, returnValue);
			}
		}
	}

	private ModelMethod getNextModelMethod(ModelAndViewContainer container) {
		// 遍历模型方法列表
		for (ModelMethod modelMethod : this.modelMethods) {
			// 如果模型方法的依赖检查通过
			if (modelMethod.checkDependencies(container)) {
				// 从模型方法列表中移除该模型方法
				this.modelMethods.remove(modelMethod);
				// 返回该模型方法
				return modelMethod;
			}
		}
		// 如果没有模型方法通过依赖检查，从列表中获取第一个模型方法
		ModelMethod modelMethod = this.modelMethods.get(0);
		// 从模型方法列表中移除该模型方法
		this.modelMethods.remove(modelMethod);
		// 返回该模型方法
		return modelMethod;
	}

	/**
	 * 查找{@code @ModelAttribute}的参数并监听{@code @SessionAttributes}。
	 */
	private List<String> findSessionAttributeArguments(HandlerMethod handlerMethod) {
		// 创建一个存储结果的列表
		List<String> result = new ArrayList<>();
		// 遍历处理器方法的所有参数
		for (MethodParameter parameter : handlerMethod.getMethodParameters()) {
			// 如果参数上有 @ModelAttribute 注解
			if (parameter.hasParameterAnnotation(ModelAttribute.class)) {
				// 获取参数的名称
				String name = getNameForParameter(parameter);
				// 获取参数的类型
				Class<?> paramType = parameter.getParameterType();
				// 如果该参数是会话属性
				if (this.sessionAttributesHandler.isHandlerSessionAttribute(name, paramType)) {
					// 将参数名称添加到结果列表中
					result.add(name);
				}
			}
		}
		// 返回结果列表
		return result;
	}

	/**
	 * 将 {@code @SessionAttributes} 列出的模型属性提升到会话。
	 * 在必要时添加{@link BindingResult}属性。
	 *
	 * @param request   当前请求
	 * @param container 包含要更新的模型
	 * @throws Exception 如果创建BindingResult属性失败
	 */
	public void updateModel(NativeWebRequest request, ModelAndViewContainer container) throws Exception {
		// 获取容器的默认模型
		ModelMap defaultModel = container.getDefaultModel();
		// 如果容器的会话状态已完成
		if (container.getSessionStatus().isComplete()) {
			// 清理会话属性
			this.sessionAttributesHandler.cleanupAttributes(request);
		} else {
			// 存储会话属性
			this.sessionAttributesHandler.storeAttributes(request, defaultModel);
		}
		// 如果请求未处理且模型等于默认模型
		if (!container.isRequestHandled() && container.getModel() == defaultModel) {
			// 更新绑定结果
			updateBindingResult(request, defaultModel);
		}
	}

	/**
	 * 为需要的属性添加{@link BindingResult}属性到模型中。
	 */
	private void updateBindingResult(NativeWebRequest request, ModelMap model) throws Exception {
		// 获取模型中的所有键的列表
		List<String> keyNames = new ArrayList<>(model.keySet());
		// 遍历所有键名
		for (String name : keyNames) {
			// 获取键对应的值
			Object value = model.get(name);
			// 如果值不为空并且是绑定候选对象
			if (value != null && isBindingCandidate(name, value)) {
				// 生成绑定结果的键名
				String bindingResultKey = BindingResult.MODEL_KEY_PREFIX + name;
				// 如果模型中不包含绑定结果的键
				if (!model.containsAttribute(bindingResultKey)) {
					// 创建数据绑定器
					WebDataBinder dataBinder = this.dataBinderFactory.createBinder(request, value, name);
					// 将绑定结果放入模型中
					model.put(bindingResultKey, dataBinder.getBindingResult());
				}
			}
		}

	}

	/**
	 * 检查给定的属性是否需要在模型中添加{@link BindingResult}。
	 */
	private boolean isBindingCandidate(String attributeName, Object value) {
		if (attributeName.startsWith(BindingResult.MODEL_KEY_PREFIX)) {
			// 如果属性名称是以 “BindingResult.” 开头，返回false
			return false;
		}

		if (this.sessionAttributesHandler.isHandlerSessionAttribute(attributeName, value.getClass())) {
			// 如果属性是处理器会话属性，返回true
			return true;
		}
		// 判断值是否不是数组、集合、映射或简单值类型
		return (!value.getClass().isArray() && !(value instanceof Collection) &&
				!(value instanceof Map) && !BeanUtils.isSimpleValueType(value.getClass()));
	}


	/**
	 * 根据{@code @ModelAttribute}参数注解（如果存在）或基于参数类型的约定，为给定的方法参数派生模型属性名称。
	 *
	 * @param parameter 方法参数的描述符
	 * @return 派生的名称
	 * @see Conventions#getVariableNameForParameter(MethodParameter)
	 */
	public static String getNameForParameter(MethodParameter parameter) {
		// 获取方法参数上的 @ModelAttribute 注解
		ModelAttribute ann = parameter.getParameterAnnotation(ModelAttribute.class);
		// 如果注解不为空，则获取注解上的名称属性
		String name = (ann != null ? ann.value() : null);
		// 如果名称属性有值，则返回该属性值。否则，使用获取方法参数的的参数名称
		return (StringUtils.hasText(name) ? name : Conventions.getVariableNameForParameter(parameter));
	}

	/**
	 * 为给定的返回值派生模型属性名称。结果将基于以下几点：
	 * <ol>
	 * <li>方法的{@code ModelAttribute}注解值
	 * <li>声明的返回类型，如果它比{@code Object}更具体
	 * <li>实际的返回值类型
	 * </ol>
	 *
	 * @param returnValue 方法调用返回的值
	 * @param returnType  方法返回类型的描述符
	 * @return 派生的名称（从不为{@code null}或空字符串）
	 */
	public static String getNameForReturnValue(@Nullable Object returnValue, MethodParameter returnType) {
		// 获取返回类型上的 ModelAttribute 注解
		ModelAttribute ann = returnType.getMethodAnnotation(ModelAttribute.class);
		// 如果注解不为空并且 value 属性有文本内容
		if (ann != null && StringUtils.hasText(ann.value())) {
			// 返回注解的 value 属性值
			return ann.value();
		} else {
			// 获取方法
			Method method = returnType.getMethod();
			// 断言方法不为空
			Assert.state(method != null, "No handler method");
			// 获取包含类
			Class<?> containingClass = returnType.getContainingClass();
			// 解析返回类型
			Class<?> resolvedType = GenericTypeResolver.resolveReturnType(method, containingClass);
			// 返回适合返回类型的变量名
			return Conventions.getVariableNameForReturnType(method, resolvedType, returnValue);
		}
	}


	private static class ModelMethod {
		/**
		 * 处理器方法
		 */
		private final InvocableHandlerMethod handlerMethod;

		/**
		 * 依赖的参数名称集合
		 */
		private final Set<String> dependencies = new HashSet<>();

		public ModelMethod(InvocableHandlerMethod handlerMethod) {
			this.handlerMethod = handlerMethod;
			// 获取并比那里处理器方法上的方法参数
			for (MethodParameter parameter : handlerMethod.getMethodParameters()) {
				// 如果该方法参数上有 @ModelAttribute 注解
				if (parameter.hasParameterAnnotation(ModelAttribute.class)) {
					// 添加该参数的名称到 依赖的参数名称集合 中
					this.dependencies.add(getNameForParameter(parameter));
				}
			}
		}

		public InvocableHandlerMethod getHandlerMethod() {
			return this.handlerMethod;
		}

		public boolean checkDependencies(ModelAndViewContainer mavContainer) {
			// 遍历依赖项列表
			for (String name : this.dependencies) {
				// 如果模型和视图容器中不包含当前依赖项的属性
				if (!mavContainer.containsAttribute(name)) {
					// 返回 false
					return false;
				}
			}
			// 如果所有依赖项都在模型和视图容器中，返回 true
			return true;

		}

		@Override
		public String toString() {
			return this.handlerMethod.getMethod().toGenericString();
		}
	}

}
