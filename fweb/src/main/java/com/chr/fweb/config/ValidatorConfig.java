package com.chr.fweb.config;

import org.hibernate.validator.HibernateValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

/**
 * @author RAY
 * @descriptions
 * @since 2021/7/15
 */
@Configuration
public class ValidatorConfig {

    private static final String FAIL_FAST = "hibernate.validator.fail_fast";
    private static final String FAIL_FAST_BOOLEAN = "true";

    /**
     * 1、使用 @Valid 注解，对 RequestParam 对应的参数进行注解是无效的，需要使用 @Validated 注解来使得验证生效
     * 2、对 MethodValidationPostProcessor 进行设置 Validator
     * 3、方法所在的Controller上加注解@Validated
     * @return
     */
    @Bean
    public MethodValidationPostProcessor methodValidationPostProcessor() {
        MethodValidationPostProcessor postProcessor = new MethodValidationPostProcessor();
        postProcessor.setValidator(validator());
        return postProcessor;
    }

    @Bean
    public Validator validator() {
        ValidatorFactory validatorFactory = Validation.byProvider(HibernateValidator.class)
                .configure()
                .addProperty(FAIL_FAST, FAIL_FAST_BOOLEAN)
                .buildValidatorFactory();
        return validatorFactory.getValidator();
    }
}
