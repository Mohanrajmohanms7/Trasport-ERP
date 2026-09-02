package com.transport.erp.exception;

import lombok.Getter;
import java.util.ArrayList;
import java.util.List;

@Getter
public class BusinessValidationException extends RuntimeException {

    private final String title;
    private final String errorCode;
    private final String userAction;
    private final List<String> errors;

    public BusinessValidationException(String title, String errorCode, String message, String userAction) {
        super(message);
        this.title = title;
        this.errorCode = errorCode;
        this.userAction = userAction;
        this.errors = new ArrayList<>();
        this.errors.add(message);
        if (userAction != null && !userAction.trim().isEmpty()) {
            this.errors.add("Action: " + userAction);
        }
    }

    public BusinessValidationException(String title, String errorCode, String message, String userAction, List<String> detailedErrors) {
        super(message);
        this.title = title;
        this.errorCode = errorCode;
        this.userAction = userAction;
        this.errors = detailedErrors != null ? new ArrayList<>(detailedErrors) : new ArrayList<>();
        if (this.errors.isEmpty() && message != null) {
            this.errors.add(message);
        }
    }
}
