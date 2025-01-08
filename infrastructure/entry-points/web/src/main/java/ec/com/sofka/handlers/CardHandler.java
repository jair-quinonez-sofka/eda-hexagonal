package ec.com.sofka.handlers;

import ec.com.sofka.card.CreateCardUseCase;
import ec.com.sofka.data.CardReqDTO;
import ec.com.sofka.mapper.CardDTOMapper;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class CardHandler {
    private final CreateCardUseCase createCardUseCase;


    public CardHandler(CreateCardUseCase createCardUseCase) {
        this.createCardUseCase = createCardUseCase;
    }

    public Mono<CardReqDTO> createCard(CardReqDTO request) {

        return createCardUseCase.execute(CardDTOMapper.toCardRequest(request)).map(CardDTOMapper::toCardReqDTO);
    }
}
