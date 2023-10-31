package namek.util.spring;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**Inspired from https://www.baeldung.com/spring-boot-favicon*/
@Controller
class FaviconController {
  @GetMapping("favicon.ico")
  @ResponseBody
  void returnNoFavicon() {
  }
}