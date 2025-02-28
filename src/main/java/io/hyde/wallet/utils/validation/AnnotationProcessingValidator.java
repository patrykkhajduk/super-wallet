package io.hyde.wallet.utils.validation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.validation.DataBinder;
import org.springframework.validation.SmartValidator;

@Component
@RequiredArgsConstructor
public class AnnotationProcessingValidator {

    private final SmartValidator smartValidator;

    public BindingResult validate(Object data) {
        DataBinder binder = new DataBinder(data);
        binder.setValidator(smartValidator);
        binder.validate();
        return binder.getBindingResult();
    }
}
