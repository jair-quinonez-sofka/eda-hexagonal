package ec.com.sofka.queries.responses.card;

import ec.com.sofka.card.Card;
import ec.com.sofka.gateway.dto.card.CardDTO;

public class GetCardByNumResponse {
    private Card card;
    private CardDTO cardDTO;

    public GetCardByNumResponse(Card card, CardDTO cardDTO) {
        this.card = card;
        this.cardDTO = cardDTO;
    }

    public Card getCard() {
        return card;
    }

    public void setCard(Card card) {
        this.card = card;
    }

    public CardDTO getCardDTO() {
        return cardDTO;
    }

    public void setCardDTO(CardDTO cardDTO) {
        this.cardDTO = cardDTO;
    }
}
