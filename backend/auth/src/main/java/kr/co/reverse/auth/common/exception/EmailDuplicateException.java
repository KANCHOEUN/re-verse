package kr.co.reverse.auth.common.exception;

import kr.co.reverse.auth.common.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class EmailDuplicateException extends RuntimeException {

//    private final ErrorCode errorCode;

}

