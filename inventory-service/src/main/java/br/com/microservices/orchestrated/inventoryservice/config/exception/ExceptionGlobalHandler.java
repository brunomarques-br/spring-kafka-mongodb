package br.com.microservices.orchestrated.inventoryservice.config.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/*
    @ControllerAdvice
        recupera exceções lançadas por métodos anotados com
            @RequestMapping, @ExceptionHandler e @InitBinder.
 */
@ControllerAdvice
public class ExceptionGlobalHandler {

    /* @ExceptionHandler
     * é um método de tratamento de exceção que lida com exceções específicas
     *         e envia uma resposta de erro personalizada.
     */
    @ExceptionHandler({ValidationException.class})
    public ResponseEntity<?> handleValidationException(ValidationException exception) {
        var exceptionDetails = new ExceptionDetails(HttpStatus.BAD_REQUEST.value(), exception.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exceptionDetails);
    }

}
