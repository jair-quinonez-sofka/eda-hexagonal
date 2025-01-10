package ec.com.sofka.handlers;


import ec.com.sofka.data.UserReqDTO;
import ec.com.sofka.usecases.commands.CreateUserCommand;
import ec.com.sofka.usecases.commands.usecases.CreateUserUseCase;
import ec.com.sofka.usecases.gateway.UserDTO;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class UserHandler {
    private final CreateUserUseCase createUserUseCase;

    public UserHandler(CreateUserUseCase createUserUseCase) {
        this.createUserUseCase = createUserUseCase;
    }

    public Mono<UserReqDTO> createUser(UserReqDTO userReqDTO) {
        return createUserUseCase.execute(
                new CreateUserCommand(
                        userReqDTO.getUsername(),
                        userReqDTO.getPassword(),
                        userReqDTO.getRoles()
                )
        ).map(res ->new UserReqDTO(res.getUsername(),res.getPassword(),res.getRoles()));
    }
}
