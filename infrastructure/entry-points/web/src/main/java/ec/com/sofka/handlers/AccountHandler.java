package ec.com.sofka.handlers;

import ec.com.sofka.account.CreateAccountUseCase;
import ec.com.sofka.data.AccountReqDTO;
import ec.com.sofka.request.CreateAccountRequest;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class AccountHandler {
    private final CreateAccountUseCase createAccountUseCase;

    public AccountHandler(CreateAccountUseCase createAccountUseCase) {
        this.createAccountUseCase = createAccountUseCase;
    }

    public Mono<AccountReqDTO> createAccount(AccountReqDTO request){

        return createAccountUseCase.execute(
                new CreateAccountRequest(
                        request.getAccountBalance(),
                        request.getAccountNumber(),
                        request.getAccountOwner(),
                        request.getAccountType()
                )
        ).map(res ->new AccountReqDTO(res.getAccountNumber(),
                res.getAccountBalance(), res.getAccountType(),res.getOwnerName()));
    }
}
