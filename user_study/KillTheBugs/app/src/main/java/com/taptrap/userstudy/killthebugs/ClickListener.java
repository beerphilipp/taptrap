package com.taptrap.userstudy.killthebugs;

/**
 * Interface for handling click events.
 */
public interface ClickListener {

    /**
     * Method to be called when a click event occurs.
     * @param fromCT true if the click is from a Custom Tab, false otherwise.
     */
    void clicked(boolean fromCT);
}
