package com.lmw.springmvc.returnValue;

import com.lmw.springmvc.entity.UserVo;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.method.annotation.ServletModelAttributeMethodProcessor;

/**
 * 带有@ModelAttribute以及简单类型的返回值解析示例
 * 这个控制器是 {@link ServletModelAttributeMethodProcessor} 返回值解析器的一个示例。
 *
 * @author LMW
 * @version 1.0
 * @since 2024-08-23 22:10
 */
@RequestMapping("/returnValue")
@Controller("servletModelAttributeMethodReturnValueController")
public class ServletModelAttributeMethodController {

	/**
	 * 非简单类型的返回值示例
	 * 由于没有配置JSP视图，这里返回了404
	 *
	 * @return 解析结果
	 */
	@GetMapping("/vo")
	public UserVo vo() {
		return new UserVo().setName("lmw").setAge(18);
	}

	/**
	 * 设置模型属性
	 *
	 * @return 用户信息
	 */
	@ModelAttribute
	public UserVo userVo() {
		return new UserVo().setName("lmw").setAge(18);
	}

	/**
	 * 使用Model接收@ModelAttribute的属性
	 *
	 * @param model 模型参数
	 * @return 解析结果
	 */
	@GetMapping("/modelAttribute")
	public ResponseEntity<Model> modelAttribute(Model model) {
		return ResponseEntity.ok(model);
	}
}
