package namek.util.spring;

import java.io.IOException;
import java.util.Map;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order
//@WebFilter(
//    filterName = "ExceptionLogFilter",
//    urlPatterns = "/*",
//    dispatcherTypes = { DispatcherType.REQUEST, DispatcherType.ASYNC, DispatcherType.ERROR })
@Slf4j
public class NamekExceptionLogFilter implements Filter {

  @PostConstruct
  public void init() {
    log.info("ExceptionLogFilter started");
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    try {
      chain.doFilter(request, response);
      Object a = request.getAttribute("javax.servlet.error.exception");
      if (a != null) {
        log.error("bad thing happened during doFilter2:", a);
      }
    } catch (Throwable e) {
      Map<String, Object> all = NamekErrorController.describeError(request, response, chain);
      log.error("bad thing happened during doFilter with {}", all, e);
      throw e;
    }
  }
}