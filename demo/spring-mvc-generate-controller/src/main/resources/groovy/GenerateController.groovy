package groovy

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody

/**
 * 要加载的Groovy 控制器代码
 *
 * @author LMW
 * @version 1.0
 * @date 2024-06-28 22:30
 */
@Controller
class GenerateController {

	@ResponseBody
	@RequestMapping("/groovy/hello")
	ResponseEntity<String> hello() {
		return ResponseEntity.ok("hello groovy!");
	}

}
