package io.iohk.atala.prism.app.views.interfaces;

/*
 * MainActivityEventHandler is a temporary solution that helps us to know when a new
 * Contact / Connection has been added and thus request a messages synchronization.
 * this will be discarded when there is a stream of events in the backend or we have
 * the appropriate data repositories in this application.
 */
public interface MainActivityEventHandler {

    void handleEvent(MainActivityEvent event);

    enum MainActivityEvent {
        NEW_CONTACT
    }
}