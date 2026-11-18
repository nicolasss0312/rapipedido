package com.rapipedido;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Traduce errores de concurrencia a respuestas HTTP claras. */
@RestControllerAdvice
public class ManejadorErrores {

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ProblemDetail conflictoConcurrente(ObjectOptimisticLockingFailureException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "El recurso fue modificado al mismo tiempo por otra operación; vuelva a intentarlo.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail violacionIntegridad(DataIntegrityViolationException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "La operación viola una restricción de datos.");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail argumentoInvalido(IllegalArgumentException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }
}
