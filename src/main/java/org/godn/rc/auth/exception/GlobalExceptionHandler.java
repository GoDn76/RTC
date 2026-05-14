package org.godn.rc.auth.exception;

import org.godn.rc.auth.payload.ApiResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

@ControllerAdvice
public class GlobalExceptionHandler {

    // 1. Handle Specific Exceptions (400 Bad Request)
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponseDto> handleBadRequestException(BadRequestException ex, WebRequest request) {
        ApiResponseDto response = new ApiResponseDto(false, ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // 2. Handle Not Found Exceptions (404)
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponseDto> handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        ApiResponseDto response = new ApiResponseDto(false, ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    // 3. Handle Unauthorized / Bad Credentials (401)
    // This catches standard Spring Security errors AND your custom ones
    @ExceptionHandler({UnauthorizedException.class, BadCredentialsException.class})
    public ResponseEntity<ApiResponseDto> handleUnauthorizedException(Exception ex, WebRequest request) {
        String message = ex.getMessage();
        if (ex instanceof BadCredentialsException) {
            message = "Invalid email or password";
        }
        ApiResponseDto response = new ApiResponseDto(false, message);
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    // 4. Handle Global/Unexpected Exceptions (500 Internal Server Error)
    // This is a safety net for NullPointers or DB connection crashes
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponseDto> handleGlobalException(Exception ex, WebRequest request) {
        // Log the real error to your console for debugging
        ex.printStackTrace();

        ApiResponseDto response = new ApiResponseDto(false, "An unexpected error occurred: " + ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}