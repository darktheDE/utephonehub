package com.example.util;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

public class ValidationUtil {
    private static Validator validator;

    public static void initialize() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    public static Validator getValidator() {
        if (validator == null) {
            throw new IllegalStateException("ValidationUtil not initialized. Call initialize() first.");
        }
        return validator;
    }
}