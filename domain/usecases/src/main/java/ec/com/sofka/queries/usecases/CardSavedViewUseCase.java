package ec.com.sofka.queries.usecases;

import ec.com.sofka.gateway.IAccountRepository;
import ec.com.sofka.gateway.ICardRepository;
import ec.com.sofka.gateway.dto.AccountDTO;
import ec.com.sofka.gateway.dto.CardDTO;
import ec.com.sofka.generics.interfaces.IUseCaseAccept;
import reactor.core.publisher.Mono;


public class CardSavedViewUseCase implements IUseCaseAccept<CardDTO, Void> {
    private final ICardRepository cardRepository;

    public CardSavedViewUseCase(ICardRepository cardRepository) {
        this.cardRepository = cardRepository;
    }
    @Override
    public void accept(CardDTO dto) {

        cardRepository.save(dto).subscribe();
    }
}