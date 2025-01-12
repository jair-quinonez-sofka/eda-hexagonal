package ec.com.sofka.queries.usecases.card;




import ec.com.sofka.aggregate.account.Customer;
import ec.com.sofka.gateway.IEventStore;
import ec.com.sofka.generics.domain.DomainEvent;
import ec.com.sofka.generics.interfaces.IUseCaseGet;
import ec.com.sofka.generics.utils.QueryResponse;
import ec.com.sofka.queries.query.GetCardByNumQuery;
import ec.com.sofka.queries.responses.card.CreateCardResponse;
import reactor.core.publisher.Flux;


public class GetAllCardsUseCase implements IUseCaseGet<GetCardByNumQuery, CreateCardResponse> {
    private final IEventStore eventStore;


    public GetAllCardsUseCase(IEventStore eventStore) {
        this.eventStore = eventStore;
    }

    @Override
    public Flux<QueryResponse<CreateCardResponse>> get(GetCardByNumQuery request) {
        Flux<DomainEvent> events = eventStore.findAggregate(request.getAggregateId());
        return Customer.from(request.getAggregateId(), events)
                .flatMapMany(customer ->
                        Flux.fromIterable(customer.getCards())
                                .map(card -> new CreateCardResponse(
                                        null,
                                        customer.getId().getValue(),
                                        card.getCardName().getValue(),
                                        card.getCardNumber().getValue(),
                                        card.getCardType().getValue(),
                                        card.getCardStatus().getValue(),
                                        card.getCardExpiryDate().getValue(),
                                        card.getCardCVV().getValue(),
                                        card.getCardLimit().getValue(),
                                        card.getCardHolderName().getValue(),
                                        null

                                ))
                )
                .collectList()
                .flatMapMany(cards ->
                        Flux.just(QueryResponse.ofMultiple(cards)));
    }
}
