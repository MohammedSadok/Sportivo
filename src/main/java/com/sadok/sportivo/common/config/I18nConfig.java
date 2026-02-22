package com.sadok.sportivo.common.config;

import java.util.List;
import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

@Configuration
public class I18nConfig implements WebMvcConfigurer {

  @Bean
  public MessageSource messageSource() {
    ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
    source.setBasename("classpath:messages");
    source.setDefaultEncoding("UTF-8");
    source.setDefaultLocale(Locale.ENGLISH);
    source.setFallbackToSystemLocale(false);
    return source;
  }

  /**
   * Wires Spring's {@link MessageSource} into Bean Validation so that
   * annotation messages referencing message keys (e.g.
   * {@code {validation.email.invalid}}) are resolved from our property files.
   */
  @Bean
  public LocalValidatorFactoryBean validator(MessageSource messageSource) {
    LocalValidatorFactoryBean factory = new LocalValidatorFactoryBean();
    factory.setValidationMessageSource(messageSource);
    return factory;
  }

  /**
   * Resolves the locale from the {@code Accept-Language} request header.
   * Falls back to {@link Locale#ENGLISH} when the header is absent or
   * contains an unsupported locale.
   */
  @Bean
  public LocaleResolver localeResolver() {
    AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
    resolver.setSupportedLocales(List.of(Locale.ENGLISH, Locale.FRENCH));
    resolver.setDefaultLocale(Locale.ENGLISH);
    return resolver;
  }

  /**
   * Tells Spring MVC to use our {@link LocalValidatorFactoryBean} (which is
   * backed by the i18n {@link MessageSource}) as the default validator,
   * ensuring that constraint messages are translated on every request.
   */
  @Override
  public Validator getValidator() {
    return validator(messageSource());
  }
}
