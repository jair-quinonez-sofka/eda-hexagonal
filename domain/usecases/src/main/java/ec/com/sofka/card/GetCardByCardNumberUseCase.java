package ec.com.sofka.card;




import ec.com.sofka.account.Account;
import ec.com.sofka.aggregate.account.Customer;
import ec.com.sofka.gateway.ICardRepository;
import ec.com.sofka.gateway.IEventStore;
import ec.com.sofka.gateway.dto.CardDTO;
import ec.com.sofka.generics.domain.DomainEvent;
import ec.com.sofka.generics.interfaces.IUseCase;
import ec.com.sofka.request.GetCardByNumRequest;
import ec.com.sofka.responses.GetCardByNumResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


public class GetCardByCardNumberUseCase implements IUseCase<GetCardByNumRequest, GetCardByNumResponse> {
    private final ICardRepository cardRepository;
    private final IEventStore eventStore;


    public GetCardByCardNumberUseCase(ICardRepository cardRepository, IEventStore eventStore) {
        this.cardRepository = cardRepository;
        this.eventStore = eventStore;
    }

    public Mono<CardDTO> apply(String cardNumber) {
        return cardRepository.findByCardNumber(cardNumber);

    }

    @Override
    public Mono<GetCardByNumResponse> execute(GetCardByNumRequest request) {
        Flux<DomainEvent> events = eventStore.findAggregate(request.getAggregateId());

        return Customer.from(request.getAggregateId(), events)
                .flatMap(customer -> {
                    Card card = customer.getCard();
                    if (card == null) {
                        return Mono.error(new RuntimeException("Card not found in event store"));
                    }
                    return cardRepository.findByCardNumber(card.getCardNumber().getValue())
                            .switchIfEmpty(Mono.error(new RuntimeException("Card not found in repository")))
                            .map(cardDTO -> new GetCardByNumResponse(card, cardDTO));
                });
    }
}
