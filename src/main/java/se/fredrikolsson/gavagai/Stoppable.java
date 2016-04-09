package se.fredrikolsson.gavagai;

/**
 * Interface intended for classes that can be stopped.
 */
interface Stoppable {

    void stop();

    boolean isStopped();
}
