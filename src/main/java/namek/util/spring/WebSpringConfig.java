package namek.util.spring;

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.bind.support.WebBindingInitializer;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.annotation.InitBinderDataBinderFactory;
import org.springframework.web.method.support.InvocableHandlerMethod;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.ServletRequestDataBinderFactory;

public class WebSpringConfig {
  // @Bean
  // public FilterRegistrationBean requestDumperFilter() {
  // FilterRegistrationBean registration = new FilterRegistrationBean();
  // Filter requestDumperFilter = new RequestDumperFilter();
  // registration.setFilter(requestDumperFilter);
  // registration.addUrlPatterns("/*");
  // return registration;
  // }
  public static WebMvcConfigurer createWebMvcConfigurer() {
    return new WebMvcConfigurer()
      {
        @Override
        public void addCorsMappings(CorsRegistry registry) {
          registry.addMapping("/**").allowedOrigins("*").allowedMethods("*").allowedOriginPatterns("*");
        }

        @Override
        public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
          /*
           * Is needed since a call from the browser bar will send the following accept and this will select xhtml
           * accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,* /
           * *;q=0.8,application/signed-exchange;v=b3;q=0.9
           */
          configurer.ignoreAcceptHeader(true);
          //All is needed as a fallback
          configurer.defaultContentType(MediaType.APPLICATION_JSON, MediaType.ALL);
        }
      };
  }

  // https://www.baeldung.com/etags-for-rest-with-spring
  // @Bean
  // public ShallowEtagHeaderFilter shallowEtagHeaderFilter() {
  // return new ShallowEtagHeaderFilter();
  // }
  //
  // @Bean
  // public FilterRegistrationBean<ShallowEtagHeaderFilter> shallowEtagHeaderFilter() {
  // FilterRegistrationBean<ShallowEtagHeaderFilter> filterRegistrationBean = new FilterRegistrationBean<>(new
  // ShallowEtagHeaderFilter());
  // filterRegistrationBean.addUrlPatterns("/*");
  // filterRegistrationBean.setName("etagFilter");
  // return filterRegistrationBean;
  // }

  public static WebMvcConfigurationSupport createWebMvcConfigurationSupport() {
    return new WebMvcConfigurationSupport2();
  }

  /**
  Solution inspired from https://stackoverflow.com/questions/22520496/how-do-i-inject-a-custom-version-of-webdatabinder-into-spring-3-mvc
  Use this by doing
  ```
  @Configuration
  public class Foo{
    @Bean
    public WebMvcConfigurationSupport create(){
      //return new MyConfiguration();
      return WebSpringConfig.createWebMvcConfigurationSupport();
    }
  }
  */
  public static class WebMvcConfigurationSupport2 extends WebMvcConfigurationSupport {
    @Override
    protected RequestMappingHandlerAdapter createRequestMappingHandlerAdapter() {
      return new RequestMappingHandlerAdapter()
        {
          @Override
          protected InitBinderDataBinderFactory createDataBinderFactory(List<InvocableHandlerMethod> binderMethods)
              throws Exception {
            WebBindingInitializer webBindingInitializer = getWebBindingInitializer();
            return new ServletRequestDataBinderFactory(binderMethods, webBindingInitializer)
              {
                @Override
                protected ServletRequestDataBinder createBinderInstance(Object target, String objectName,
                    NativeWebRequest request) throws Exception {
                  ServletRequestDataBinder binder = super.createBinderInstance(target, objectName, request);
                  binder.setIgnoreUnknownFields(false);
                  binder.setIgnoreInvalidFields(false);
                  return binder;
                }
              };
          }
        };
    }
  }
}
