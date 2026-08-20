package guessmarket.engine;

import guessmarket.engine.exception.EngineException;
import guessmarket.engine.exception.InvalidEventFileException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

public class GuessMarketEngineImpl implements GuessMarketEngine
{

    private static final double WINNING_SHARE_PAYOUT = 1.0;
    private static final String SAVE_FILE_EXTENSION = ".gmstate";

    private final EventFileLoader fileLoader;
    private List<Event> events;

    public GuessMarketEngineImpl()
    {
        this.fileLoader = new EventFileLoader();
        this.events = new ArrayList<>();
    }

    @Override
    public List<EventInfo> loadFile(String filePath) throws InvalidEventFileException
    {
        List<Event> loadedEvents = fileLoader.load(filePath);
        this.events = loadedEvents;
        return toEventInfoList(loadedEvents);
    }

    @Override
    public List<EventInfo> getAllEvents() throws EngineException
    {
        requireEventsLoaded();
        return toEventInfoList(events);
    }

    @Override
    public List<EventInfo> getActiveEvents() throws EngineException
    {
        requireEventsLoaded();
        List<Event> activeEvents = new ArrayList<>();
        for (Event event : events)
        {
            if (!event.isClosed())
            {
                activeEvents.add(event);
            }
        }
        return toEventInfoList(activeEvents);
    }

    @Override
    public EventInfo getEventInfo(int eventId) throws EngineException
    {
        Event event = findEventById(eventId);
        return event.toEventInfo();
    }

    @Override
    public PurchaseResult buyShares(int eventId, int optionIndex, int quantity) throws EngineException
    {
        Event event = findEventById(eventId);

        if (event.isClosed())
        {
            throw new EngineException("This event is already closed and cannot accept new trades.");
        }
        validateOptionIndex(optionIndex);
        if (quantity <= 0)
        {
            throw new EngineException("Quantity must be a positive whole number.");
        }

        int otherOptionIndex = 1 - optionIndex;
        double currentQuantity = event.getQuantity(optionIndex);
        double otherQuantity = event.getQuantity(otherOptionIndex);
        double b = event.getLiquidityB();

        double costBefore = Lmsr.cost(currentQuantity, otherQuantity, b);
        double costAfter = Lmsr.cost(currentQuantity + quantity, otherQuantity, b);
        double sharesCost = costAfter - costBefore;

        double feeCost = 0;
        if (event.getCommissionType() == CommissionType.ON_PURCHASE)
        {
            feeCost = sharesCost * event.getCommissionPercent() / 100.0;
        }

        event.recordPurchase(optionIndex, quantity, sharesCost);
        event.addToAccountBalance(sharesCost);
        if (feeCost > 0)
        {
            event.addToAccountBalance(feeCost);
            event.addFeesCollected(feeCost);
        }

        return new PurchaseResult(sharesCost, feeCost);
    }

    @Override
    public void closeEvent(int eventId, int winningOptionIndex) throws EngineException
    {
        Event event = findEventById(eventId);

        if (event.isClosed())
        {
            throw new EngineException("This event is already closed.");
        }
        validateOptionIndex(winningOptionIndex);

        int winningShares = event.getQuantity(winningOptionIndex);
        double totalOwedToWinners = winningShares * WINNING_SHARE_PAYOUT;

        double feeAmount = 0;
        if (event.getCommissionType() == CommissionType.ON_CLOSE)
        {
            feeAmount = totalOwedToWinners * event.getCommissionPercent() / 100.0;
            event.addFeesCollected(feeAmount);
        }

        double actualPayout = totalOwedToWinners - feeAmount;
        event.addToAccountBalance(-actualPayout);
        event.close(winningOptionIndex);
    }

    @Override
    public void saveStateToFile(String filePath) throws EngineException
    {
        requireEventsLoaded();
        String fullPath = filePath.trim() + SAVE_FILE_EXTENSION;
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(fullPath)))
        {
            out.writeObject(events);
        }
        catch (IOException e)
        {
            throw new EngineException("Could not save the system state: " + e.getMessage());
        }
    }

    @Override
    public void loadStateFromFile(String filePath) throws EngineException
    {
        String fullPath = filePath.trim() + SAVE_FILE_EXTENSION;
        if (!new File(fullPath).exists())
        {
            throw new EngineException("No saved state file found at: " + fullPath);
        }
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(fullPath)))
        {
            List<Event> loadedEvents = (List<Event>) in.readObject();
            this.events = loadedEvents;
        }
        catch (IOException e)
        {
            throw new EngineException("Could not load the saved state: " + e.getMessage());
        }
        catch (ClassNotFoundException e)
        {
            throw new EngineException("Could not load the saved state: " + e.getMessage());
        }
    }

    private List<EventInfo> toEventInfoList(List<Event> eventList)
    {
        List<EventInfo> infoList = new ArrayList<>();
        for (Event event : eventList)
        {
            infoList.add(event.toEventInfo());
        }
        return infoList;
    }

    private void validateOptionIndex(int optionIndex) throws EngineException
    {
        if (optionIndex != 0 && optionIndex != 1)
        {
            throw new EngineException("Invalid option chosen.");
        }
    }

    private void requireEventsLoaded() throws EngineException
    {
        if (events.isEmpty())
        {
            throw new EngineException("No events are loaded. Use the load command to load an events file first.");
        }
    }

    private Event findEventById(int eventId) throws EngineException
    {
        requireEventsLoaded();
        for (Event event : events)
        {
            if (event.getId() == eventId)
            {
                return event;
            }
        }
        throw new EngineException("No event found with id " + eventId + ".");
    }
}
