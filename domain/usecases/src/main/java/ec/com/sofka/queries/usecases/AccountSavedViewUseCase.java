package ec.com.sofka.queries.usecases;


import ec.com.sofka.gateway.IAccountRepository;
import ec.com.sofka.gateway.dto.AccountDTO;
import ec.com.sofka.generics.interfaces.IUseCaseAccept;

public class AccountSavedViewUseCase implements IUseCaseAccept<AccountDTO, Void> {
    private final IAccountRepository accountRepository;

    public AccountSavedViewUseCase(IAccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public void accept(AccountDTO accountDTO) {
        accountRepository.save(accountDTO).subscribe();
    }


}
