package guessmarket.engine;

import guessmarket.engine.exception.EngineException;
import guessmarket.engine.exception.InvalidEventFileException;

import java.util.List;

public interface GuessMarketEngine
{

    List<EventInfo> loadFile(String filePath) throws InvalidEventFileException;

    List<EventInfo> getAllEvents() throws EngineException;

    List<EventInfo> getActiveEvents() throws EngineException;

    EventInfo getEventInfo(int eventId) throws EngineException;

    PurchaseResult buyShares(int eventId, int optionIndex, int quantity) throws EngineException;

    void closeEvent(int eventId, int winningOptionIndex) throws EngineException;

    void saveStateToFile(String filePath) throws EngineException;

    void loadStateFromFile(String filePath) throws EngineException;
}
