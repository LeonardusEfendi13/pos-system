package com.pos.posApps.ControllerMVC;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<String> handleValidationExceptions(MethodArgumentNotValidException ex) {
        BindingResult result = ex.getBindingResult();
        StringBuilder errorMessage = new StringBuilder();

        // Collect all validation error messages
        for (ObjectError error : result.getAllErrors()) {
            errorMessage.append(error.getDefaultMessage()).append("\n");
        }

        // Return custom validation error message as 400 Bad Request
        return new ResponseEntity<>(errorMessage.toString(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ModelAndView handleNotFoundException(Exception ex) {
        ModelAndView mav = new ModelAndView();

        // Mengarah ke file src/main/resources/templates/error/404.html
        mav.setViewName("error/404");

        // Tetap set status HTTP response menjadi 404
        mav.setStatus(HttpStatus.NOT_FOUND);

        return mav;
    }
}

