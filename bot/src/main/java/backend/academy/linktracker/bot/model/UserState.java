package backend.academy.linktracker.bot.model;

public enum UserState {
    IDLE, // wait for /track
    WAITING_LINK, // got /track, wait for link
    WAITING_TAGS,
    WAITING_FILTERS, // got tags (or /skip), wait for filters,
    WAITING_UNTRACK_LINK // /untrack: waiting for URL to remove
}
