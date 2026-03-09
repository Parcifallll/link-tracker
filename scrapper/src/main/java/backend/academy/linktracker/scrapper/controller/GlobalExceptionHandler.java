package backend.academy.linktracker.scrapper.controller;

import backend.academy.linktracker.scrapper.dto.ApiErrorResponse;
import backend.academy.linktracker.scrapper.exception.ChatAlreadyExistsException;
import backend.academy.linktracker.scrapper.exception.ChatNotFoundException;
import backend.academy.linktracker.scrapper.exception.LinkAlreadyExistsException;
import backend.academy.linktracker.scrapper.exception.LinkNotFoundException;
import java.util.Arrays;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ChatNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handleChatNotFound(ChatNotFoundException ex) {
        log.atError().addKeyValue("error", ex.getMessage()).log("Chat not found");
        return buildResponse("Chat not found", "404", ex);
    }

    @ExceptionHandler(ChatAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiErrorResponse handleChatAlreadyExists(ChatAlreadyExistsException ex) {
        log.atError().addKeyValue("error", ex.getMessage()).log("Chat already exists");
        return buildResponse("Chat already exists", "409", ex);
    }

    @ExceptionHandler(LinkNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handleLinkNotFound(LinkNotFoundException ex) {
        log.atError().addKeyValue("error", ex.getMessage()).log("Link not found");
        return buildResponse("Link not found", "404", ex);
    }

    @ExceptionHandler(LinkAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiErrorResponse handleLinkAlreadyExists(LinkAlreadyExistsException ex) {
        log.atError().addKeyValue("error", ex.getMessage()).log("Link already exists");
        return buildResponse("Link already exists", "409", ex);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleValidation(MethodArgumentNotValidException ex) {
        log.atError().addKeyValue("error", ex.getMessage()).log("Validation error");
        return buildResponse("Invalid request parameters", "400", ex);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiErrorResponse handleException(Exception ex) {
        log.atError().addKeyValue("error", ex.getMessage()).log("Unexpected error");
        return buildResponse("Internal server error", "500", ex);
    }

    private ApiErrorResponse buildResponse(String description, String code, Exception ex) {
        return new ApiErrorResponse(
            description,
            code,
            ex.getClass().getSimpleName(),
            ex.getMessage(),
            Arrays.stream(ex.getStackTrace())
                .map(StackTraceElement::toString)
                .collect(Collectors.toList())
        );
    }
}
