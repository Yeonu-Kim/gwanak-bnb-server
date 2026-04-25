package boostcampsnu.gwanakbnbserver.exception;

import boostcampsnu.gwanakbnbserver.dto.common.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;


@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleAppException(AppException e) {
        ErrorCode code = e.getErrorCode();
        log.error("[AppException] code={}, message={}", code.getCode(), code.getMessage());
        return ResponseEntity
                .status(code.getStatus())
                .body(new ErrorResponse(code.getCode(), code.getMessage(), Instant.now()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse(ErrorCode.VALIDATION_ERROR.getMessage());
        log.error("[ValidationError] message={}", message);
        return ResponseEntity
                .badRequest()
                .body(new ErrorResponse(ErrorCode.VALIDATION_ERROR.getCode(), message, Instant.now()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        log.error("[Unexpected] {}", e.getMessage(), e);
        return ResponseEntity.internalServerError()
                .body(new ErrorResponse(
                        "INTERNAL_SERVER_ERROR",
                        "서버 내부 오류가 발생했습니다.",
                        Instant.now()
                ));
    }
}
