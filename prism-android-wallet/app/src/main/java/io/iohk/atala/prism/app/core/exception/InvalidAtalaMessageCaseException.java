package io.iohk.atala.prism.app.core.exception;

import lombok.Getter;

public class InvalidAtalaMessageCaseException extends Exception {

    @Getter
    private final ErrorCode code;

    public InvalidAtalaMessageCaseException(ErrorCode code) {
        super(code.getDescription());
        this.code = code;
    }
}
