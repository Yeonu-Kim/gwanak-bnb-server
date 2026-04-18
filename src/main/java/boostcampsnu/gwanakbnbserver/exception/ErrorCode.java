package boostcampsnu.gwanakbnbserver.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "필수 필드 누락 또는 형식 오류"),
    DUPLICATE_LOGIN_ID(HttpStatus.CONFLICT, "DUPLICATE_LOGIN_ID", "이미 사용 중인 로그인 아이디"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "아이디 또는 비밀번호 불일치"),
    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "INVALID_DATE_RANGE", "체크아웃이 체크인보다 이전이거나 동일"),
    ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "ROOM_NOT_FOUND", "해당 ID의 숙소가 존재하지 않음"),
    HOST_ONLY(HttpStatus.FORBIDDEN, "HOST_ONLY", "호스트 계정만 숙소 등록 가능"),
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "CATEGORY_NOT_FOUND", "카테고리 ID 없음"),
    REGION_NOT_FOUND(HttpStatus.NOT_FOUND, "REGION_NOT_FOUND", "지역 ID 없음"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN", "본인 숙소가 아님"),
    HAS_ACTIVE_RESERVATION(HttpStatus.CONFLICT, "HAS_ACTIVE_RESERVATION", "활성 예약이 있어 삭제 불가"),
    GUEST_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "GUEST_LIMIT_EXCEEDED", "게스트 수 초과"),
    ROOM_ALREADY_BOOKED(HttpStatus.CONFLICT, "ROOM_ALREADY_BOOKED", "해당 기간 이미 예약됨"),
    RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND, "RESERVATION_NOT_FOUND", "예약 없음"),
    ALREADY_CANCELLED(HttpStatus.CONFLICT, "ALREADY_CANCELLED", "이미 취소된 예약"),
    CANCEL_NOT_ALLOWED(HttpStatus.CONFLICT, "CANCEL_NOT_ALLOWED", "체크인 당일 취소 불가"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "인증 토큰 없음 또는 만료"),
    INVALID_SCORE(HttpStatus.BAD_REQUEST, "INVALID_SCORE", "점수 범위 오류 (1~5)"),
    CHECKOUT_REQUIRED(HttpStatus.FORBIDDEN, "CHECKOUT_REQUIRED", "아직 체크아웃 전"),
    REVIEW_ALREADY_EXISTS(HttpStatus.CONFLICT, "REVIEW_ALREADY_EXISTS", "이미 리뷰 작성됨"),
    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "REVIEW_NOT_FOUND", "리뷰 없음"),
    HOST_NOT_FOUND(HttpStatus.NOT_FOUND, "HOST_NOT_FOUND", "호스트 없음"),
    ALREADY_HOST(HttpStatus.CONFLICT, "ALREADY_HOST", "이미 호스트로 등록된 계정");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
