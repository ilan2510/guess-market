package guessmarket.engine;

import java.util.List;

public interface GuessMarketEngine {

    List<Event> loadFile(String filePath) throws InvalidEventFileException;

    List<Event> getAllEvents() throws EngineException;

    List<Event> getActiveEvents() throws EngineException;

    PurchaseResult buyShares(int eventId, int optionIndex, int quantity) throws EngineException;

    void closeEvent(int eventId, int winningOptionIndex) throws EngineException;
}
