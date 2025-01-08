package ec.com.sofka.account;


import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import ec.com.sofka.account.values.AccountId;
import ec.com.sofka.account.values.objects.AccountNumber;
import ec.com.sofka.account.values.objects.AccountType;
import ec.com.sofka.account.values.objects.Balance;
import ec.com.sofka.account.values.objects.OwnerName;

import java.io.IOException;

public class AccountDeserializer extends JsonDeserializer<Account> {
    @Override
    public Account deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);

        AccountId accountId = AccountId.of(node.get("id").get("value").asText());
        Balance balance = Balance.of(node.get("balance").get("value").decimalValue());
        AccountNumber accountNumber = AccountNumber.of(node.get("accountNumber").get("value").asText());
        OwnerName ownerName = OwnerName.of(node.get("ownerName").get("value").asText());
        AccountType accountType = AccountType.of(node.get("type").get("value").asText());

        return new Account(accountId, balance, accountNumber, ownerName, accountType);
    }
}
