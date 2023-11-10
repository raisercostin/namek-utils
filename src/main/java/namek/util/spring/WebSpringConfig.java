package namek.util.spring;

import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

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
}
