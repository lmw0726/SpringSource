package com.lmw.springmvc.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.servlet.http.HttpServletResponse;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.io.IOException;
import java.util.List;

/**
 * 全局异常处理器，这里处理所有的异常
 *
 * @author 林佛权
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
	@Autowired
	private MessageSource messageSource;

	/**
	 * 处理Spring 绑定参数失败的异常
	 *
	 * @param e Spring 绑定参数失败的异常
	 * @return 返回400状态码和默认的异常消息
	 */
	@ExceptionHandler(BindException.class)
	public ResponseEntity<Object> handleValidException(BindException e) {
		log.error("发生Spring 绑定参数异常，异常消息为：{}", e.getMessage());

		BindingResult bindingResult = e.getBindingResult();
		List<FieldError> fieldErrors = bindingResult.getFieldErrors();
		StringBuilder messageBuilder = new StringBuilder();
		for (FieldError error : fieldErrors) {
			messageBuilder.append(error.getField()).append(error.getDefaultMessage()).append(",");
		}
		messageBuilder = new StringBuilder(messageBuilder.substring(0, messageBuilder.length() - 1));
		String string = messageBuilder.toString();
		if (string != null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST.value()).contentType(MediaType.APPLICATION_JSON).body(string);
		}
		FieldError fieldError = bindingResult.getFieldError();

		if (fieldError != null) {
			String message = messageSource.getMessage(fieldError, LocaleContextHolder.getLocale());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST.value()).body(message);
		}
		List<ObjectError> allErrors = bindingResult.getAllErrors();
		String message = allErrors
				.stream()
				.map(ObjectError::getDefaultMessage)
				.findFirst()
				.orElse("");
		message = messageSource.getMessage(message, null, LocaleContextHolder.getLocale());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST.value()).body(message);
	}


	/**
	 * 处理Hibernate Validator方法参数校验失败产生的异常
	 *
	 * @param e 方法参数校验失败产生的异常
	 * @return 返回400的状态码，并取第一个参数的错误信息作为响应消息
	 */
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Object> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
		log.error("Hibernate Validator校验失败，发生方法参数异常，异常消息为：{}", e.getMessage());
		String message = e.getBindingResult()
				.getAllErrors()
				.stream()
				.map(ObjectError::getDefaultMessage)
				.findFirst()
				.orElse("");

		return ResponseEntity.status(HttpStatus.BAD_REQUEST.value()).body(message);
	}


	/**
	 * 异常体系
	 *
	 * @param httpServletResponse http响应
	 * @param e                   异常
	 * @throws IOException IO异常
	 */
	@ExceptionHandler(value = Throwable.class)
	public ResponseEntity<Object> handleCommonException(HttpServletResponse httpServletResponse, Throwable e) throws IOException {
		log.error("发生通用的异常，异常消息为：{}", e.getMessage());
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR.value()).body("服务异常，请联系管理员");
	}

	/**
	 * 处理通用异常，返回500状态码，并提示服务异常，请联系管理员
	 *
	 * @param httpServletResponse HttpServlet响应体
	 * @param e                   通用异常
	 * @throws IOException IO异常
	 */
	@ExceptionHandler(value = Exception.class)
	public ResponseEntity<Object> handleCommonException(HttpServletResponse httpServletResponse, Exception e) throws IOException {
		log.error("发生通用的异常，异常消息为：{}", e.getMessage());
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR.value()).body("服务异常，请联系管理员");
	}


	/**
	 * 处理不支持请求类型的异常，返回405状态码，并提示不支持此类型的请求方式
	 *
	 * @param httpServletResponse HttpServlet响应体
	 * @param e                   不支持请求类型的异常
	 * @throws IOException IO异常
	 */
	@ExceptionHandler(value = HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<Object> handleHttpRequestMethodNotSupported(HttpServletResponse httpServletResponse,
																	  HttpRequestMethodNotSupportedException e) throws IOException {
		log.info("发生请求方式错误的异常，异常消息为：{}", e.getMessage());
		return ResponseEntity.status(405).body("不支持此类型的请求方式");
	}


	/**
	 * 处理运行时异常
	 *
	 * @param httpServletResponse HttpServlet响应体
	 * @param e                   运行时异常
	 * @throws IOException 异常
	 */
	@ExceptionHandler(value = RuntimeException.class)
	public ResponseEntity<Object> handleRuntimeException(HttpServletResponse httpServletResponse, RuntimeException e) throws IOException {
		e.printStackTrace();
		log.error("发生运行时异常，异常消息为：{}", e);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR.value()).body("服务器异常，请联系管理员");
	}


	/**
	 * 处理JSON格式错误异常，直接返回400状态码，并提示JSON格式不正确,请检查参数是否正常
	 *
	 * @param httpServletResponse HttpServlet响应体
	 * @param e                   JSON格式错误异常
	 * @throws IOException IO异常
	 */
	@ExceptionHandler(value = HttpMessageNotReadableException.class)
	public ResponseEntity<Object> handleJsonErrorException(HttpServletResponse httpServletResponse,
														   HttpMessageNotReadableException e) throws IOException {
		log.error("发生JSON请求参数格式错误异常，异常消息为：{}", e.getMessage());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST.value()).body("JSON格式不正确,请检查参数是否正常");
	}


	/**
	 * 无效请求协议/类型
	 * 如使用Post请求Get的方法，就会抛出该异常。
	 *
	 * @param e 协议不匹配异常
	 * @return 返回415状态码，并提示不支持该请求类型，请核对接口
	 */
	@ExceptionHandler(value = HttpMediaTypeNotSupportedException.class)
	public ResponseEntity<Object> handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException e) {
		log.error("发生请求协议错误异常，异常消息为：{}", e.getMessage());
		return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value()).body("不支持该请求类型，请核对接口");
	}


	/**
	 * 处理参数校验失败异常,直接返回400，并根据校验失败的第一个参数提示消息
	 *
	 * @param e Hibernate Validator参数校验失败，抛出改异常
	 * @return 直接返回400，并根据校验失败的第一个参数提示消息
	 */
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<Object> handleConstraintViolationException(ConstraintViolationException e) {
		log.info("参数校验失败：{}", e.getMessage());
		String message = e.getConstraintViolations()
				.stream()
				.map(ConstraintViolation::getMessage)
				.findFirst()
				.orElse("");
		return ResponseEntity.status(HttpStatus.BAD_REQUEST.value()).body(message);
	}


	/**
	 * 访问不存在的路径时，由于找不到对应的Controller，抛出该异常。
	 * 直接返回404，并提示路径不存在，请检查路径是否正确
	 *
	 * @param e 没有处理者异常
	 * @throws IOException IO异常
	 */
	@ExceptionHandler(NoHandlerFoundException.class)
	public ResponseEntity<Object> handleNoFoundException(HttpServletResponse httpServletResponse, Exception e) throws IOException {
		log.error("未找到Controller异常，{}", e.getMessage(), e);
		return ResponseEntity.status(HttpStatus.NOT_FOUND.value()).body("路径不存在，请检查路径是否正确");
	}
}
