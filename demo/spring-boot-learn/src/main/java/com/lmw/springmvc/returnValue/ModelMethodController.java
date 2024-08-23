package com.lmw.springmvc.returnValue;

import com.lmw.springmvc.entity.UserVo;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.method.annotation.ModelMethodProcessor;

/**
 * Model返回值示例
 * 这个控制器是 {@link ModelMethodProcessor} 返回值解析器的一个示例。
 *
 * @author LMW
 * @version 1.0
 * @since 2024-08-23 23:06
 */
@RequestMapping("/returnValue")
@Controller("modelMethodReturnValueController")
public class ModelMethodController {

	/**
	 * Model返回值示例
	 * 由于没有配置JSP视图，因此，访问该URL会出现404的情况。
	 *
	 * @param model 模型参数
	 * @return 解析结果
	 */
	@GetMapping("/model")
	public Model model(Model model) {
		UserVo vo = new UserVo().setName("lmw").setAge(18);
		model.addAttribute(vo);
		return model;
	}
}
