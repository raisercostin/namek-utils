package namek.util.spring;

import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
//import org.springframework.http.HttpHeaders;
//import org.springframework.web.context.request.WebRequest;
//import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.context.request.WebRequest;

//Still to investigate
@ControllerAdvice
public class NamekExceptionHandler {
  //extends ResponseEntityExceptionHandler {
  @ExceptionHandler(Exception.class)
  public ResponseEntity<String> handleException(Exception ex, HttpServletRequest req, WebRequest request)
      throws Exception {
    Map<String, Object> all = NamekErrorController.describeError(req, request);
    return ResponseEntity
      .status(HttpStatus.INTERNAL_SERVER_ERROR)
      .body("Exception: " + ex.getLocalizedMessage());
  }
  //
  //  @Override
  //  protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
  //      HttpStatus status, WebRequest request) {
  //    throw new RuntimeException("Not implemented yet!!!");
  //  }
  //  @ExceptionHandler(value = { Throwable.class })
  //  protected ResponseEntity<Object> handleConflict(
  //      RuntimeException ex, WebRequest request) {
  //    String bodyOfResponse = "Namek error";
  //    return handleExceptionInternal(ex, bodyOfResponse,
  //      new HttpHeaders(), HttpStatus.CONFLICT, request);
  //  }
}
