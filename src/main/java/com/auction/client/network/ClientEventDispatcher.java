package com.auction.client.network;

import com.auction.shared.protocol.ActionType;
import com.auction.shared.protocol.Response;
import javafx.application.Platform;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class ClientEventDispatcher {
    private static final ClientEventDispatcher INSTANCE = new ClientEventDispatcher();

    private final Map<ActionType, List<Consumer<Response<?>>>> listeners = new ConcurrentHashMap<>();

    private ClientEventDispatcher() {}

    public static ClientEventDispatcher getInstance() {
        return INSTANCE;
    }

    public void addListener(ActionType actionType, Consumer<Response<?>> listener) {
        if (actionType == null || listener == null) {
            return;
        }

        listeners.computeIfAbsent(actionType, ignored -> new CopyOnWriteArrayList<>())
                .add(listener);
    }

    public void removeListener(ActionType actionType, Consumer<Response<?>> listener) {
        if (actionType == null || listener == null) {
            return;
        }

        List<Consumer<Response<?>>> actionListeners = listeners.get(actionType);
        if (actionListeners == null) {
            return;
        }

        actionListeners.remove(listener);
        if (actionListeners.isEmpty()) {
            listeners.remove(actionType, actionListeners);
        }
    }

    public void dispatch(Response<?> response) {
        if (response == null || response.getAction() == null) {
            return;
        }

        List<Consumer<Response<?>>> actionListeners = listeners.get(response.getAction());
        if (actionListeners == null || actionListeners.isEmpty()) {
            return;
        }

        Runnable dispatchTask = () -> {
            for (Consumer<Response<?>> listener : actionListeners) {
                try {
                    listener.accept(response);
                } catch (Exception e) {
                    System.err.println("[ClientEventDispatcher] Listener failed for "
                            + response.getAction() + ": " + e.getMessage());
                }
            }
        };

        if (Platform.isFxApplicationThread()) {
            dispatchTask.run();
        } else {
            Platform.runLater(dispatchTask);
        }
    }

    public int getListenerCount(ActionType actionType) {
        List<Consumer<Response<?>>> actionListeners = listeners.get(actionType);
        return actionListeners == null ? 0 : actionListeners.size();
    }
}
