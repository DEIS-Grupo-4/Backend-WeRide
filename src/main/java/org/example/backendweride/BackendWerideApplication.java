package org.example.backendweride;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableJpaAuditing
@SpringBootApplication
@OpenAPIDefinition(
        info = @Info(
                title = "WeRide",
                description = "API backend de WeRide para la gestión de vehículos, reservas, viajes, "
                        + "ubicaciones y usuarios."
        )
)

@EnableJpaRepositories(basePackages = "org.example.backendweride.platform")

@EntityScan(basePackages = "org.example.backendweride.platform")
public class BackendWerideApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendWerideApplication.class, args);
    }

}
