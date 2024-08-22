package com.aus.framework.common.validator;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class PhoneNumberValidator implements ConstraintValidator<PhoneNumber, String> {


    @Override
    public void initialize(PhoneNumber constraintAnnotation) {
        // 进行一些初始化操作
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(String phoneNumber, ConstraintValidatorContext context) {
        // 校验逻辑：正则表达式判断手机号是否为11位数字
        return phoneNumber != null && phoneNumber.matches("\\d{11}");
    }
}
