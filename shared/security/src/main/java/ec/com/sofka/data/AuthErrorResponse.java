package ec.com.sofka.data;

public class AuthErrorResponse {
    private final String error;
    private final String message;

    public AuthErrorResponse(String error, String message) {
        this.error = error;
        this.message = message;
    }

    public String getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }
}
