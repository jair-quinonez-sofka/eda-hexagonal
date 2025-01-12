package ec.com.sofka.queries.usecases.account;

import ec.com.sofka.aggregate.account.Customer;
import ec.com.sofka.gateway.IEventStore;
import ec.com.sofka.generics.domain.DomainEvent;
import ec.com.sofka.generics.interfaces.IUseCaseGetEmpty;
import ec.com.sofka.generics.utils.QueryResponse;
import ec.com.sofka.queries.responses.account.CreateAccountResponse;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.stream.Collectors;

public class GetAllAccountsUseCase implements IUseCaseGetEmpty<CreateAccountResponse> {

    private final IEventStore eventStore;

    public GetAllAccountsUseCase(IEventStore eventStore) {
        this.eventStore = eventStore;
    }

    @Override
    public Flux<QueryResponse<CreateAccountResponse>> get() {
        return eventStore.findAllAggregate("customer")
                .collectList()
                .flatMapMany(events -> {
                    Map<String, DomainEvent> latestEvents = events.stream()
                            .collect(Collectors.toMap(
                                    DomainEvent::getAggregateRootId,
                                    event -> event,
                                    (existing, replacement) -> existing.getVersion() >= replacement.getVersion() ? existing : replacement
                            ));

                    return Flux.fromIterable(latestEvents.values())
                            .flatMap(event -> Customer.from(event.getAggregateRootId(), Flux.fromIterable(events)));
                })
                .map(customer -> new CreateAccountResponse(
                        customer.getId().getValue(),
                        customer.getAccount().getAccountNumber().getValue(),
                        customer.getAccount().getOwnerName().getValue(),
                        customer.getAccount().getType().getValue(),
                        customer.getAccount().getBalance().getValue()
                        )
                )
                .collectList()
                .flatMapMany(createAccountResponse ->
                        Flux.just(QueryResponse.ofMultiple(createAccountResponse)));
    }
}
