package com.bibleapp.services;

/** Thrown when a bible-api.com call fails (network, HTTP error, or parse error). */
public class BibleApiException extends Exception {

    public BibleApiException(String message) {
        super(message);
    }

    public BibleApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
