package namek.util.spring;

import java.io.IOException;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order
@Slf4j
public class NamekDebugFilter implements Filter {
  public static final String X_USERINFO = "X-USERINFO";
  private static final String DEBUG_TEST_ERROR_PARAM = "debugTestError";
  private static final String DEBUG_XUSER_INFO_PARAM = "debugXUserInfo";

  @PostConstruct
  public void init() {
    log.info(
      "RequestResponseDebugFilter pass [{}=<eror_code_number>,<error_message>] in url to get that error from server.",
      DEBUG_TEST_ERROR_PARAM);
    log.info("RequestResponseDebugFilter pass [{}=true] in url to log xuserinfo", DEBUG_XUSER_INFO_PARAM);
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    HttpServletRequest req = (HttpServletRequest) request;
    HttpServletResponse res = (HttpServletResponse) response;
    log.debug("Logging Request  {} : {}", req.getMethod(), req.getRequestURI());
    String debug = request.getParameter(DEBUG_TEST_ERROR_PARAM);
    if (StringUtils.isNotEmpty(debug)) {
      int error = Integer.parseInt(StringUtils.substringBefore(debug, ","));
      String errorMessage = StringUtils.substringAfter(debug, ",");
      res.sendError(error, errorMessage);
    } else {
      //DO NOT catch exception here as the programmers cannot see exceptions in ui and so they are not aware when they break something
      //      try {
      chain.doFilter(request, response);
      //      } catch (Exception exc) {
      // Temporary fix for: java.lang.IllegalStateException: Response is committed
      //TODO:  Change all responses to be Mono<ResponseEntity> to deffer flushing result to the Response object (avoid the exception above)
      //  Otherwise, it seems that KC Oidc adapter tries to getSession to write something to it after that was commited!
      //        log.error("Error in RequestResponseDebugFiler.doFilter", exc);
      //      }
    }
    if (StringUtils.isNotEmpty(request.getParameter(DEBUG_XUSER_INFO_PARAM))) {
      log.info("{}=[{}]", X_USERINFO, req.getHeader(X_USERINFO));
      // TODO: should not HTTP parameter to HTTP header
      res.addHeader("debug-" + X_USERINFO, req.getHeader(X_USERINFO));
    }
    log.debug("Logging Response :{}", res.getContentType());
  }
}
