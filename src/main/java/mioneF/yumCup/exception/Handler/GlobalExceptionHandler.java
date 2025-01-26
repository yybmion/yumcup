package mioneF.yumCup.exception.Handler;

import lombok.extern.slf4j.Slf4j;
import mioneF.yumCup.exception.ExternalApiException;
import mioneF.yumCup.exception.InsufficientRestaurantsException;
import mioneF.yumCup.exception.NoNearbyRestaurantsException;
import mioneF.yumCup.exception.RestaurantProcessingException;
import mioneF.yumCup.exception.RestaurantProcessingTimeoutException;
import mioneF.yumCup.exception.response.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InsufficientRestaurantsException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleInsufficientRestaurants(InsufficientRestaurantsException e) {
        log.warn(e.getMessage());
        return new ErrorResponse(e.getMessage());
    }

    @ExceptionHandler(NoNearbyRestaurantsException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleNotFoundRestaurants(NoNearbyRestaurantsException e) {
        log.warn(e.getMessage());
        return new ErrorResponse(e.getMessage());
    }

    @ExceptionHandler(RestaurantProcessingTimeoutException.class)
    @ResponseStatus(HttpStatus.REQUEST_TIMEOUT)
    public ErrorResponse handleTimeout(RestaurantProcessingTimeoutException e) {
        log.warn(e.getMessage());
        return new ErrorResponse(e.getMessage());
    }

    @ExceptionHandler(ExternalApiException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ErrorResponse handleExternalApiError(ExternalApiException e) {
        log.error("외부 API 오류: {}", e.getMessage());
        return new ErrorResponse(e.getMessage());
    }

    @ExceptionHandler(RestaurantProcessingException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleProcessingError(RestaurantProcessingException e) {
        log.error(e.getMessage());
        return new ErrorResponse(e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleIllegalArgument(IllegalArgumentException e) {
        log.error("잘못된 요청값: {}", e.getMessage());
        return new ErrorResponse(e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleUnexpected(Exception e) {
        log.error(e.getMessage());
        return new ErrorResponse(e.getMessage());
    }
}
