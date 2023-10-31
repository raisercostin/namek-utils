package namek.util.spring;

import static io.vavr.API.Map;

import java.io.BufferedReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import io.vavr.API;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.Array;
import io.vavr.collection.HashMap;
import io.vavr.collection.HashSet;
import io.vavr.collection.LinkedHashMap;
import io.vavr.collection.List;
import io.vavr.collection.Set;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.error.ErrorAttributeOptions.Include;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

@RestController
// @RequestMapping("/error")
@Component
/**
 * - See https://www.baeldung.com/exception-handling-for-rest-with-spring
 *
 * - https://www.dev2qa.com/how-to-disable-or-customize-spring-boot-whitelabel-error-page/ -
 * https://www.mkyong.com/spring-boot/spring-rest-error-handling-example/
 */
@RequestMapping("${server.error.path:${error.path:/error}}")
@Slf4j
public class NamekErrorController implements ErrorController {
  private final ErrorAttributes errorAttributes;

  @Autowired
  public NamekErrorController(ErrorAttributes errorAttributes) {
    Assert.notNull(errorAttributes, "ErrorAttributes must not be null");
    this.errorAttributes = errorAttributes;
  }

  public String getErrorPath() {
    return "/error";
  }

  @Getter
  @Setter
  @RequiredArgsConstructor
  @ToString
  @EqualsAndHashCode
  public static class ErrorClass {
    public final String code;
  }

  private final static Set<String> knownErrorCodes = HashSet.of("timeLimitOnKeycloak");

  @RequestMapping
  public Map<String, Object> error(HttpServletRequest request, WebRequest webRequest) {
    return describeError(this.errorAttributes, request, webRequest, null, null);
  }

  public static Map<String, Object> describeError(HttpServletRequest req, WebRequest request) {
    return describeError(req, request, null, null);
  }

  public static Map<String, Object> describeError(ServletRequest request, ServletResponse response, FilterChain chain) {
    if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
      return describeError((HttpServletRequest) request, (HttpServletResponse) response, chain);
    } else {
      return Map.of("message",
        String.format("Don't know how to describe error from servletRequest of type %s and ServletResponse %s",
          request.getClass(), response.getClass()));
    }
  }

  public static Map<String, Object> describeError(HttpServletRequest request, HttpServletResponse response,
      FilterChain chain) {
    WebRequest webRequest = new ServletWebRequest(request, response);
    return describeError(request, webRequest, response, chain);
  }

  public static Map<String, Object> describeError(HttpServletRequest request, WebRequest webRequest,
      HttpServletResponse response, FilterChain chain) {
    DefaultErrorAttributes all = new DefaultErrorAttributes();
    return describeError(all, request, webRequest, response, chain);
  }

  public static java.util.Map<String, Object> describeError(ErrorAttributes errorAttributes, HttpServletRequest request,
      WebRequest webRequest, HttpServletResponse response, FilterChain chain) {
    try {
      Map<String, Object> body = getErrorAttributes(errorAttributes, webRequest, getTraceParameter(request));
      String trace = (String) body.get("trace");
      if (trace != null) {
        String[] lines = trace.split("\n\t");
        body.put("trace", lines);
      }
      Array<Tuple2<String, String>> requestAttributes = Array
        .<String>ofAll(Collections.list(request.getAttributeNames()))
        .filter(x -> x != null && !x.equals("org.springframework.core.convert.ConversionService"))
        .map(x -> Tuple.of(x, Objects.toString(request.getAttribute(x))));
      LinkedHashMap<String, List<String>> requestParameters = LinkedHashMap.ofAll(request.getParameterMap())
        .mapValues(x -> List.of(x));
      Map<String, Object> errorAttributesParams = errorAttributes.getErrorAttributes(webRequest,
        ErrorAttributeOptions.defaults().including(Include.STACK_TRACE));
      Throwable error = errorAttributes.getError(webRequest);
      if (error == null) {
        error = new RuntimeException(
          "Namek error. See errorAttributes above.");
      }
      ErrorClass errorType = computeErrorType(requestAttributes, requestParameters, errorAttributesParams, error);
      String requestBody = extractRequestBody(request);
      boolean knownException = knownErrorCodes.contains(errorType.code);
      if (knownException) {
        log.warn("namek: {} Enable debug for this logger to get full details.", error.getMessage());
        log.debug(
          "namek: NamekErrorController\nerrorType:{}\nrequestAttributes:{}\nrequestParameters:{}\nrequestBody:[{}]\nerrorAttributes:{}",
          errorType, requestAttributes.mkString("\n  - ", "\n  - ", "\n"),
          requestParameters.mkString("\n  - ", "\n  - ", "\n"),
          requestBody,
          LinkedHashMap.ofAll(errorAttributesParams).mkString("\n  - ", "\n  - ", "\n"));
      } else {
        log.warn(
          "namek: NamekErrorController\nerrorType:{}\nrequestAttributes:{}\nrequestParameters:{}\nrequestBody:[{}]\nerrorAttributes:{}",
          errorType, requestAttributes.mkString("\n  - ", "\n  - ", "\n"),
          requestParameters.mkString("\n  - ", "\n  - ", "\n"),
          requestBody,
          LinkedHashMap.ofAll(errorAttributesParams).mkString("\n  - ", "\n  - ", "\n"), error);
      }
      return body;
    } catch (Exception e) {
      log.warn("error when handling error", e);
      return new java.util.HashMap<>();
    }
  }

  private static String extractRequestBody(HttpServletRequest request) {
    String requestBody = "<unknwon:Couldn't read body after error. Maybe already consumed. Tried to read from inputStream then reader.>";
    try {
      try (ServletInputStream is = request.getInputStream()) {
        requestBody = StringUtils.abbreviate(IOUtils.toString(is), 2000);
      }
    } catch (Exception e) {
      log.info("tried to read body using input stream", e);
      try {
        try (BufferedReader reader = request.getReader()) {
          requestBody = StringUtils.abbreviate(IOUtils.toString(reader), 2000);
        }
      } catch (Exception e2) {
        log.info("tried to read body using reader", e2);
      }
    }
    return requestBody;
  }

  private static java.util.Map<String, Object> getErrorAttributes(ErrorAttributes errorAttributes,
      WebRequest webRequest, boolean includeStackTrace) {
    return errorAttributes.getErrorAttributes(webRequest,
      includeStackTrace ? ErrorAttributeOptions.defaults().including(Include.STACK_TRACE)
          : ErrorAttributeOptions.defaults());
  }

  private static ErrorClass computeErrorType(Array<Tuple2<String, String>> requestAttributes,
      LinkedHashMap<String, List<String>> requestParameters, Map<String, Object> errorAttributesParams,
      Throwable throwable) {
    // Throwable cause = Throwables.getRootCause(throwable);
    // TODO should add a getErrorType() on throwable
    if (throwable.getMessage() != null
        && throwable.getMessage().startsWith("TimeLimit on connecting to keycloak TimeLimiterConfig")) {
      return new ErrorClass("timeLimitOnKeycloak");
    }
    return new ErrorClass("unknwon");
  }

  private static boolean getTraceParameter(HttpServletRequest request) {
    String parameter = request.getParameter("trace");
    if (parameter == null) {
      return false;
    }
    return !"false".equals(parameter.toLowerCase());
  }
}
