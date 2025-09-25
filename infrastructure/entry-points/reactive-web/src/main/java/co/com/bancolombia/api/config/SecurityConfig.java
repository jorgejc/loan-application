/**
 * SecurityConfig.java
 */
package co.com.bancolombia.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.util.*;

/**
 * <b>Descripción:</b> Clase que determina la configuración principal de seguridad para la aplicación reactiva
 * que implementa autenticación y autorización basada en tokens JWT. Define las reglas
 * de seguridad, decodificación de tokens, extracción de roles y permisos, y manejo
 * personalizado de errores de seguridad.
 * <br>
 * <b>HU03:</b> Agregar autenticación al sistema
 *<br>
 * <b>Algoritmo JWT:</b> HMAC-SHA256 (HS256) con clave secreta compartida
 * <br>
 * <b>Estándares:</b> OAuth 2.0 Resource Server, JWT (RFC 7519), Problem Details (RFC 7807)
 * @author Jorge Armando Julio Cruz
 */
@Configuration
@EnableReactiveMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final ObjectMapper objectMapper;

    /**
     * Método encargado de configurar la cadena de filtros de seguridad para la aplicación reactiva,
     * definiendo las reglas de autorización, autenticación JWT y manejo de errores.
     * <br>
     * <b>HU03:</b> Agregar autenticación al sistema
     *
     * @param http Configurador de seguridad HTTP reactivo
     * @param jwtDecoder Decodificador de tokens JWT configurado
     * @param jwtAuthConverter Convertidor de autenticación JWT para extracción de roles
     * @return Cadena de filtros de seguridad configurada
     * @author Jorge Armando Julio Cruz
     */
    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http, ReactiveJwtDecoder jwtDecoder,
            ReactiveJwtAuthenticationConverterAdapter jwtAuthConverter) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .headers(h -> h.frameOptions(fr -> fr.disable()))
                .authorizeExchange(ex -> ex
                        .pathMatchers("/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**", "/h2/**").permitAll()
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        .pathMatchers(HttpMethod.POST, "/api/v1/applications").hasAnyRole("ADMIN", "CLIENT")
                        .pathMatchers(HttpMethod.GET, "/api/v1/users/profile").hasAnyRole("CLIENT", "ADMIN")
                        .pathMatchers("/api/v1/admin/**").hasRole("ADMIN")

                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth -> oauth
                        .authenticationEntryPoint((exchange, exAuth) -> {
                            String detail = "Authentication is required to access this resource";
                            if (exAuth instanceof InvalidBearerTokenException) {
                                detail = "Invalid or expired token";
                            } else if (exAuth instanceof OAuth2AuthenticationException oae) {
                                String code = oae.getError().getErrorCode();
                                if ("invalid_token".equals(code) || "invalid_request".equals(code)) {
                                    detail = "Invalid or expired token";
                                }
                            }
                            return writeProblemDetail(exchange, HttpStatus.UNAUTHORIZED, "Unauthorized", detail);
                        })
                        .accessDeniedHandler((exchange, denied) ->
                                writeProblemDetail(exchange, HttpStatus.FORBIDDEN, "Forbidden",
                                        "You do not have permission to access this resource"))
                        .jwt(jwt -> jwt
                                .jwtDecoder(jwtDecoder)
                                .jwtAuthenticationConverter(jwtAuthConverter)
                        )
                )
                .build();
    }

    /**
     * Método encargado de configurar el decodificador de tokens JWT utilizando una clave secreta HMAC-SHA256.
     * Soporta tanto Base64 estándar como Base64-URL encoding para flexibilidad.
     * <br>
     * <b>HU03:</b> Agregar autenticación al sistema
     *
     * @param secretProp Clave secreta codificada en Base64 desde configuración.
     * @return Decodificador configurado para validar tokens JWT
     * @author Jorge Armando Julio Cruz <jjulio@heinsohn.com.co>
     */
    @Bean
    public ReactiveJwtDecoder jwtDecoder(@Value("${security.jwt.secret}") String secretProp) {
        byte[] secretBytes;
        try {
            secretBytes = Base64.getDecoder().decode(secretProp);
        } catch (IllegalArgumentException e) {
            secretBytes = Base64.getUrlDecoder().decode(secretProp);
        }

        var key = new SecretKeySpec(secretBytes, "HmacSHA256");
        return NimbusReactiveJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    /**
     * Método encargado de configurar el convertidor de autenticación JWT que extrae roles y permisos
     * desde los claims del token para construir las autoridades de Spring Security.
     * <br>
     * <b>HU03:</b> Agregar autenticación al sistema
     *
     * @return ReactiveJwtAuthenticationConverterAdapter Convertidor configurado
     * @author Jorge Armando Julio Cruz
     *
     * @see JwtAuthenticationConverter
     * @see ReactiveJwtAuthenticationConverterAdapter
     */
    @Bean
    public ReactiveJwtAuthenticationConverterAdapter jwtAuthConverter() {
        var delegate = new JwtAuthenticationConverter();
        delegate.setJwtGrantedAuthoritiesConverter(SecurityConfig::extractAuthorities);
        return new ReactiveJwtAuthenticationConverterAdapter(delegate);
    }

    /**
     * Método encargado de extraer las autoridades (roles y permisos) desde los claims del JWT.
     * <br>
     * <b>Caso de Uso:</b>
     *
     * @param jwt Token JWT decodificado con los claims del usuario
     * @return Collection &lt;GrantedAuthority&gt; Lista de autoridades extraídas
     * @author Jorge Armando Julio Cruz <jjulio@heinsohn.com.co>
     */
    private static Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        String role = jwt.getClaimAsString("role");
        if (role != null && !role.isBlank()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        }

        List<String> permissions = jwt.getClaimAsStringList("permissions");
        if (permissions != null) {
            permissions.stream()
                    .filter(Objects::nonNull)
                    .filter(p -> !p.isBlank())
                    .map(SimpleGrantedAuthority::new)
                    .forEach(authorities::add);
        }

        return authorities;
    }

    /**
     * Método encargado de escribir una respuesta de error HTTP utilizando el formato Problem Details (RFC 7807)
     * para proporcionar información estructurada sobre errores de seguridad.
     * <br>
     * <b>HU03:</b> Agregar autenticación al sistema
     *
     * @param exchange Contexto del intercambio web reactivo
     * @param status Código de estado HTTP para la respuesta
     * @param title Título breve del error (ej: "Unauthorized", "Forbidden")
     * @param detail Descripción detallada del error para el cliente
     * @return Mono<Void> Operación reactiva de escritura de la respuesta
     * @author Jorge Armando Julio Cruz
     */
    private Mono<Void> writeProblemDetail(ServerWebExchange exchange,
                                          HttpStatus status,
                                          String title,
                                          String detail) {
        var request = exchange.getRequest();
        var response = exchange.getResponse();

        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);

        var problemDetail = ProblemDetail.forStatus(status);
        problemDetail.setTitle(title);
        problemDetail.setDetail(detail);
        problemDetail.setInstance(URI.create(request.getPath().value()));

        return Mono.fromCallable(() -> objectMapper.writeValueAsBytes(problemDetail))
                .flatMap(bytes -> {
                    DataBuffer buffer = response.bufferFactory().wrap(bytes);
                    return response.writeWith(Mono.just(buffer));
                });
    }
}
