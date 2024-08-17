package com.lmw.springmvc.returnValue;

import com.lmw.springmvc.entity.UserVo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.RequestResponseBodyMethodProcessor;

import java.util.ArrayList;
import java.util.List;

/**
 * 带有@ResponseBody的返回值解析示例
 * 这个控制器是 {@link RequestResponseBodyMethodProcessor} 返回值解析器的一个示例。
 *
 * @author LMW
 * @version 1.0
 * @since 2024-08-17 21:58
 */
@RequestMapping("/returnValue")
@RestController("responseBodyMethodController")
public class RequestResponseBodyMethodController {

	/**
	 * 实体类返回值解析示例
	 *
	 * @return 实体类
	 */
	@GetMapping("/responseBody")
	public List<UserVo> userList() {
		List<UserVo> users = new ArrayList<>();
		users.add(new UserVo().setName("lmw").setAge(18));
		users.add(new UserVo().setName("qqq").setAge(26));
		return users;
	}
}
