package ec.com.sofka.commands.usecases;


import ec.com.sofka.aggregate.account.Customer;
import ec.com.sofka.commands.CreateAccountCommand;
import ec.com.sofka.gateway.BusEvent;
import ec.com.sofka.gateway.IAccountRepository;
import ec.com.sofka.gateway.IEventStore;
import ec.com.sofka.gateway.dto.AccountDTO;
import ec.com.sofka.generics.interfaces.IUseCaseExecute;
import ec.com.sofka.queries.responses.CreateAccountResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


public class CreateAccountUseCase implements IUseCaseExecute<CreateAccountCommand, CreateAccountResponse> {
    private final IEventStore eventRepository;
    private final IAccountRepository accountRepository;
    private final BusEvent busEvent;

    public CreateAccountUseCase(IEventStore repository, IAccountRepository accountRepository, BusEvent busEvent) {
        this.eventRepository = repository;
        this.accountRepository = accountRepository;
        this.busEvent = busEvent;
    }


    @Override
    public Mono<CreateAccountResponse> execute(CreateAccountCommand cmd) {

        return accountRepository.findByAccountNumber(cmd.getAccountNumber())
                .flatMap(existingAccount-> Mono.error(new RuntimeException("Account already exist")))
                .switchIfEmpty(Mono.defer(() ->{
                    Customer customer = new Customer();

                    customer.createAccount(cmd.getBalance(), cmd.getAccountNumber(), cmd.getOwnerName(), cmd.getAccountType());

                    customer.getUncommittedEvents()
                            .stream()
                            .map(eventRepository::save)
                            .forEach(busEvent::sendEventAccountCreated);

                    customer.markEventsAsCommitted();

                    return Mono.just(new CreateAccountResponse(
                                    customer.getId().getValue(),
                                    customer.getAccount().getAccountNumber().getValue(),
                                    customer.getAccount().getOwnerName().getValue(),
                                    customer.getAccount().getType().getValue(),
                                    customer.getAccount().getBalance().getValue()));
                }))
                .cast(CreateAccountResponse.class);

    }

}
