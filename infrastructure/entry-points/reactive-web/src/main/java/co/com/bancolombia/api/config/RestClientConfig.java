/**
 * RestClientConfig.java
 */
package co.com.bancolombia.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * <b>Descripción:</b> Clase que determina la configuración de clientes HTTP reactivos (WebClient) para comunicación
 * con servicios externos. Proporciona beans de WebClient pre-configurados con interceptores
 * para propagación automática de tokens JWT y otras configuraciones de seguridad.
 * <br>
 * <b>HU03:</b>Agregar autenticación al sistema
 * <br>
 * <b>Patrón:</b> Factory Pattern - Crea instancias configuradas de WebClient
 * @author Jorge Armando Julio Cruz
 */
@Configuration
public class RestClientConfig {

    /**
     * Método encargado de crear un WebClient configurado para comunicación con servicios de autenticación
     * y autorización. Incluye interceptores para propagación automática de tokens JWT.
     * <br>
     * <b>CU03:</b> Agregar autenticación al sistema
     *
     * @param baseUrl URL base del servicio de autenticación obtenida desde configuración.
     * @return Instancia configurada de WebClient con interceptores de seguridad y URL base establecida
     * @author Jorge Armando Julio Cruz <jjulio@heinsohn.com.co>
     */
    @Bean("authServiceClient")
    public WebClient authServiceClient(@Value("${auth.base-url}") String baseUrl) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .filter((request, next) ->
                        ReactiveSecurityContextHolder.getContext()
                                .map(ctx -> (JwtAuthenticationToken) ctx.getAuthentication())
                                .map(jwt -> ClientRequest.from(request)
                                        .headers(h -> h.set(HttpHeaders.AUTHORIZATION,
                                                "Bearer " + jwt.getToken().getTokenValue()))
                                        .build())
                                .defaultIfEmpty(request)
                                .flatMap(next::exchange)
                )
                .build();
    }

    /**
     * Método encargado de crear un WebClient básico sin interceptores de seguridad para llamadas públicas
     * o servicios que no requieren autenticación.
     * <br>
     * <b>HU03:</b> Agregar autenticación al sistema
     *
     * @return WebClient Instancia básica sin interceptores de seguridad
     * @author Jorge Armando Julio Cruz
     */
    @Bean("publicServiceClient")
    public WebClient publicServiceClient() {
        return WebClient.builder()
                .build();
    }

}
