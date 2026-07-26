package dev.alone.core.client;

/** Client-side cache of the server's authoritative craft-timer state for the currently-worked recipe.
 *  Drives the result-slot progress bar and the take-gate, so both agree with the server exactly. */
public final class ClientCraftState {
    /** 0..1 progress of the current craft, straight from the server. */
    public static volatile float progress = 0f;
    /** Whether the server will let you take the current result. */
    public static volatile boolean ready = false;

    private ClientCraftState() {
    }
}
