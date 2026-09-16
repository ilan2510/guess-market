package guessmarket.engine;

import guessmarket.engine.exception.EngineException;
import guessmarket.engine.exception.InvalidEventFileException;

import java.util.List;

public interface GuessMarketEngine
{

    List<EventInfo> loadFile(String filePath) throws InvalidEventFileException;

    List<UserInfo> getAllUsers() throws EngineException;

    UserInfo getUserInfo(String userName) throws EngineException;

    List<EventInfo> getEventsForUser(String userName) throws EngineException;

    List<EventInfo> getAllEvents() throws EngineException;

    EventInfo getEventInfo(int eventId) throws EngineException;

    void startEvent(int eventId, String actingUserName) throws EngineException;

    void closeEvent(int eventId, String actingUserName, int winningOptionIndex) throws EngineException;

    PurchaseResult buyShares(int eventId, String actingUserName, int optionIndex, int quantity) throws EngineException;

    OrderSubmitResult submitOrder(int eventId, String actingUserName, int optionIndex, OrderSide side,
            int quantity, double price) throws EngineException;
}
