package ru.deewend.ccjlbridge;

import ru.deewend.ccjlbridge.event.*;
import ru.deewend.ccjlbridge.gameapi.ScheduledTask;

import java.util.*;

@SuppressWarnings("unused")
public class EventManager {
    public static final int SPECIAL_EVENTS_OFFSET = 10000;
    public static final int SCHEDULED_TASKS_OFFSET = 20000;
    public static final int LOADER_TICK_EVENT_ID = 0;

    private static final EventManager INSTANCE = new EventManager();

    @SuppressWarnings("rawtypes")
    private static final Class[] EVENTS_TABLE = {
            EntityAddedEvent.class,
            EntityRemovedEvent.class,
            TabListAddedEvent.class,
            TabListChangedEvent.class,
            TabListRemovedEvent.class,
            TextureAtlasChangedEvent.class,
            TexturePackChangedEvent.class,
            TextureFileChangedEvent.class,
            GfxViewDistanceChangedEvent.class,
            GfxLowVRAMDetectedEvent.class,
            GfxProjectionChangedEvent.class,
            GfxContextLostEvent.class,
            GfxContextRecreatedEvent.class,
            UserBlockChangedEvent.class,
            UserHackPermsChangedEvent.class,
            UserHeldBlockChangedEvent.class,
            UserHacksStateChangedEvent.class,
            BlockPermissionsChangedEvent.class,
            BlockBlockDefChangedEvent.class,
            WorldNewMapEvent.class,
            WorldLoadingEvent.class,
            WorldMapLoadedEvent.class,
            WorldEnvVarChangedEvent.class,
            WorldLightingModeChangedEvent.class,
            ChatFontChangedEvent.class,
            ChatChatReceivedEvent.class,
            ChatChatSendingEvent.class,
            ChatColCodeChangedEvent.class,
            WindowRedrawNeededEvent.class,
            WindowResizedEvent.class,
            WindowClosingEvent.class,
            WindowFocusChangedEvent.class,
            WindowStateChangedEvent.class,
            WindowCreatedEvent.class,
            WindowInactiveChangedEvent.class,
            WindowRedrawingEvent.class,
            InputPressEvent.class,
            InputDownEvent.class,
            InputUpEvent.class,
            InputWheelEvent.class,
            InputTextChangedEvent.class,
            InputDown2Event.class,
            InputUp2Event.class,
            PointerMovedEvent.class,
            PointerDownEvent.class,
            PointerUpEvent.class,
            PointerRawMovedEvent.class,
            ControllerAxisUpdateEvent.class,
            NetConnectedEvent.class,
            NetDisconnectedEvent.class,
            NetPluginMessageReceivedEvent.class
    };

    private static final String[] SPECIAL_EVENTS_TABLE = {
            "free",
            "reset",
            "onNewMap",
            "onNewMapLoaded"
    };

    private final Map<Class<? extends Event>, List<EventHandler<?>>> eventHandlerMap;

    private EventManager() {
        this.eventHandlerMap = new HashMap<>();
    }

    public static EventManager getInstance() {
        return INSTANCE;
    }

    public synchronized <T extends Event> void registerEventHandler(
            Class<T> eventClass, EventHandler<T> eventHandler
    ) {
        Objects.requireNonNull(eventClass);
        Objects.requireNonNull(eventHandler);

        List<EventHandler<? extends Event>> eventHandlerList =
                eventHandlerMap.computeIfAbsent(eventClass, (key) -> new ArrayList<>());
        eventHandlerList.add(eventHandler);
    }

    public void fireRawEvent(int eventId) {
        Class<?> clazz;
        if (eventId == LOADER_TICK_EVENT_ID) {
            clazz = LoaderTickEvent.class;
        } else if (eventId >= SPECIAL_EVENTS_OFFSET && eventId < SCHEDULED_TASKS_OFFSET) {
            String method = SPECIAL_EVENTS_TABLE[eventId - SPECIAL_EVENTS_OFFSET];
            PluginManager.getInstance().call(method);

            return;
        } else if (eventId >= SCHEDULED_TASKS_OFFSET) {
            ScheduledTask.invoke(eventId - SCHEDULED_TASKS_OFFSET);

            return;
        } else {
            clazz = EVENTS_TABLE[eventId - LOADER_TICK_EVENT_ID - 1];
        }

        try {
            fireEvent((Event) clazz.newInstance());
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized <T extends Event> void fireEvent(T event) {
        Objects.requireNonNull(event);

        if (ClassiCubeJavaLoaderBridge.DEBUG) Log.i("Firing event " + event.getClass().getName());

        Class<? extends Event> eventClass = event.getClass();
        if (!eventHandlerMap.containsKey(eventClass)) return;

        List<EventHandler<? extends Event>> handlerList =
                eventHandlerMap.get(eventClass);
        for (EventHandler<? extends Event> eventHandler : handlerList) {
            //noinspection unchecked
            ((EventHandler<T>) eventHandler).handleEvent(event);
        }
    }
}
