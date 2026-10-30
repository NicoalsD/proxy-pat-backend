package co.edu.proxypat.proxy;

/**
 * Punto de entrada genérico a cualquier microservicio: se le pide una operación
 * por nombre y devuelve el resultado ya convertido al tipo esperado.
 */
public interface MicroserviceProxy<T> {

    T execute(String operation, Object... params);
}
