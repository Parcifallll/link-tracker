package backend.academy.linktracker.scrapper.exception;

import java.net.URI;

public class LinkAlreadyExistsException extends RuntimeException {
    public LinkAlreadyExistsException(URI url) {
        super("Link already exists: " + url);
    }
}
