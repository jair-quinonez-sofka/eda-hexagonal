package ec.com.sofka.routes;

import ec.com.sofka.ErrorDetails;
import ec.com.sofka.data.AccountReqDTO;
import ec.com.sofka.data.UserReqDTO;
import ec.com.sofka.exceptions.BodyRequestValidator;
import ec.com.sofka.exceptions.GlobalExceptionsHandler;
import ec.com.sofka.handlers.UserHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springdoc.core.annotations.RouterOperation;
import org.springdoc.core.annotations.RouterOperations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.function.server.*;
import reactor.core.publisher.Mono;

import static org.springframework.web.reactive.function.server.RequestPredicates.accept;

@Configuration
public class UserRouter {
    private final UserHandler userHandler;
    private final BodyRequestValidator bodyRequestValidator;
    private final GlobalExceptionsHandler globalExceptionsHandler;


    public UserRouter(UserHandler userHandler, BodyRequestValidator bodyRequestValidator, GlobalExceptionsHandler globalExceptionsHandler) {
        this.userHandler = userHandler;
        this.bodyRequestValidator = bodyRequestValidator;
        this.globalExceptionsHandler = globalExceptionsHandler;
    }


    @Bean
    @RouterOperations({
            @RouterOperation(
                    path = "/api/v1/user/create",
                    method = RequestMethod.POST,
                    operation = @Operation(
                            tags = {"Users"},
                            operationId = "create",
                            summary = "Create a new user",
                            description = "Creates a new user with username and password",
                            requestBody = @RequestBody(
                                    description = "User creation details",
                                    required = true,
                                    content = @Content(
                                            mediaType = "application/json",
                                            schema = @Schema(implementation = UserReqDTO.class)
                                    )
                            ),
                            responses = {
                                    @ApiResponse(
                                            responseCode = "201",
                                            description = "User successfully created",
                                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserReqDTO.class))
                                    ),
                                    @ApiResponse(
                                            responseCode = "400",
                                            description = "Bad request, validation error or missing required fields",
                                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class))
                                    ),
                                    @ApiResponse(
                                            responseCode = "500",
                                            description = "Username could already exist",
                                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDetails.class))
                                    )
                            }
                    )
            )
    })
    public RouterFunction<ServerResponse> userRoutes() {
        return RouterFunctions
                .route(RequestPredicates.POST("/api/v1/user/create")
                        .and(accept(MediaType.APPLICATION_JSON)), this::createUser);
    }


    public Mono<ServerResponse> createUser(ServerRequest request) {

        return request.bodyToMono(UserReqDTO.class)
                .doOnNext(bodyRequestValidator::validate)
                .flatMap(userHandler::createUser)
                .flatMap(userDTO -> ServerResponse.status(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON).bodyValue(userDTO))
                .onErrorResume(globalExceptionsHandler::handleException);

    }
}
