package guessmarket.engine;

import guessmarket.engine.exception.InvalidEventFileException;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class EventFileLoader
{

    private static final int MIN_COMMISSION_PERCENT = 0;
    private static final int MAX_COMMISSION_PERCENT = 90;
    private static final int REQUIRED_OPTION_COUNT = 2;

    public LoadResult load(String filePath) throws InvalidEventFileException
    {
        String trimmedPath = filePath.trim();
        File file = new File(trimmedPath);

        if (!file.exists())
        {
            throw new InvalidEventFileException("File not found: " + trimmedPath);
        }
        if (!file.getName().toLowerCase().endsWith(".xml"))
        {
            throw new InvalidEventFileException("File must have a .xml extension: " + trimmedPath);
        }

        Document document;
        try
        {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            document = builder.parse(file);
        }
        catch (Exception e)
        {
            throw new InvalidEventFileException("File could not be read as XML: " + e.getMessage());
        }

        List<Event> events = parseEvents(document);
        List<User> users = parseUsers(document);
        assignMarketMakers(document, events);

        return new LoadResult(events, users);
    }

    private List<Event> parseEvents(Document document) throws InvalidEventFileException
    {
        NodeList eventNodes = document.getElementsByTagName("GM-event");
        if (eventNodes.getLength() == 0)
        {
            throw new InvalidEventFileException("The file does not contain any events.");
        }

        List<Event> events = new ArrayList<>();
        Set<Integer> usedIds = new HashSet<>();

        for (int i = 0; i < eventNodes.getLength(); i++)
        {
            Element eventElement = (Element) eventNodes.item(i);
            Event event;
            try
            {
                event = parseEvent(eventElement);
            }
            catch (Exception e)
            {
                throw new InvalidEventFileException("Event #" + (i + 1) + " in the file is not valid: " + e.getMessage());
            }

            if (usedIds.contains(event.getId()))
            {
                throw new InvalidEventFileException(
                        "Event id " + event.getId() + " appears more than once in the file. Every event must have a unique id.");
            }
            usedIds.add(event.getId());

            if (event.getCommissionPercent() < MIN_COMMISSION_PERCENT || event.getCommissionPercent() > MAX_COMMISSION_PERCENT)
            {
                throw new InvalidEventFileException("Event '" + event.getName() + "' has commission "
                        + event.getCommissionPercent() + "%, which is not between "
                        + MIN_COMMISSION_PERCENT + " and " + MAX_COMMISSION_PERCENT + ".");
            }

            events.add(event);
        }

        return events;
    }

    private Event parseEvent(Element eventElement)
    {
        String name = eventElement.getAttribute("name");
        int id = Integer.parseInt(getChildText(eventElement, "id"));
        String description = getChildText(eventElement, "description");

        Element commissionElement = (Element) eventElement.getElementsByTagName("commission").item(0);
        if (commissionElement == null)
        {
            throw new IllegalStateException("event is missing a <commission> element");
        }
        int commissionPercent = Integer.parseInt(commissionElement.getTextContent().trim());
        String commissionTypeText = commissionElement.getAttribute("type");
        CommissionType commissionType;
        if (commissionTypeText.equalsIgnoreCase("on-close"))
        {
            commissionType = CommissionType.ON_CLOSE;
        }
        else
        {
            commissionType = CommissionType.ON_PURCHASE;
        }

        Element optionsElement = (Element) eventElement.getElementsByTagName("GM-options").item(0);
        NodeList optionNodes = optionsElement.getElementsByTagName("GM-option");
        if (optionNodes.getLength() != REQUIRED_OPTION_COUNT)
        {
            throw new IllegalStateException("event must have exactly " + REQUIRED_OPTION_COUNT
                    + " options, found " + optionNodes.getLength());
        }
        String optionA = optionNodes.item(0).getTextContent().trim();
        String optionB = optionNodes.item(1).getTextContent().trim();

        Element methodElement = (Element) eventElement.getElementsByTagName("GM-method").item(0);

        NodeList lmsrNodes = methodElement.getElementsByTagName("GM-LMSR");
        if (lmsrNodes.getLength() > 0)
        {
            Element lmsrElement = (Element) lmsrNodes.item(0);
            int liquidityB = Integer.parseInt(getChildText(lmsrElement, "b"));
            return Event.createLmsrEvent(id, name, description, commissionPercent, commissionType,
                    optionA, optionB, liquidityB);
        }

        NodeList orderBookNodes = methodElement.getElementsByTagName("GM-order-book");
        if (orderBookNodes.getLength() == 0)
        {
            throw new IllegalStateException("event has no trading method (expected GM-LMSR or GM-order-book)");
        }
        Element orderBookElement = (Element) orderBookNodes.item(0);
        boolean allowMint = Boolean.parseBoolean(orderBookElement.getAttribute("allow-mint"));
        int initial = Integer.parseInt(orderBookElement.getAttribute("initial"));
        int d = Integer.parseInt(orderBookElement.getAttribute("d"));
        if (d <= 0)
        {
            throw new IllegalStateException("Order Book base value d must be greater than 0, found " + d);
        }
        if (initial < 0)
        {
            throw new IllegalStateException("Order Book initial investment cannot be negative, found " + initial);
        }
        return Event.createOrderBookEvent(id, name, description, commissionPercent, commissionType,
                optionA, optionB, d, allowMint, initial);
    }

    private List<User> parseUsers(Document document) throws InvalidEventFileException
    {
        NodeList userNodes = document.getElementsByTagName("GM-user");
        if (userNodes.getLength() == 0)
        {
            throw new InvalidEventFileException("The file does not contain any users.");
        }

        List<User> users = new ArrayList<>();
        Set<String> usedNames = new HashSet<>();

        for (int i = 0; i < userNodes.getLength(); i++)
        {
            Element userElement = (Element) userNodes.item(i);
            String name = userElement.getAttribute("name");

            if (usedNames.contains(name))
            {
                throw new InvalidEventFileException(
                        "User name '" + name + "' appears more than once in the file. Every user must have a unique name.");
            }
            usedNames.add(name);

            int initialCash;
            try
            {
                initialCash = Integer.parseInt(getChildText(userElement, "initial-cash"));
            }
            catch (Exception e)
            {
                throw new InvalidEventFileException("User '" + name + "' does not have a valid initial-cash value.");
            }
            if (initialCash <= 0)
            {
                throw new InvalidEventFileException("User '" + name + "' has an initial cash amount of "
                        + initialCash + ", which must be greater than 0.");
            }

            users.add(new User(name, initialCash));
        }

        return users;
    }

    private void assignMarketMakers(Document document, List<Event> events) throws InvalidEventFileException
    {
        NodeList userNodes = document.getElementsByTagName("GM-user");
        for (int i = 0; i < userNodes.getLength(); i++)
        {
            Element userElement = (Element) userNodes.item(i);
            String userName = userElement.getAttribute("name");

            NodeList mmNodes = userElement.getElementsByTagName("GM-market-maker");
            if (mmNodes.getLength() == 0)
            {
                continue;
            }
            Element mmElement = (Element) mmNodes.item(0);

            NodeList eventRefs = mmElement.getElementsByTagName("event");
            for (int j = 0; j < eventRefs.getLength(); j++)
            {
                Element eventRef = (Element) eventRefs.item(j);
                int eventId;
                try
                {
                    eventId = Integer.parseInt(eventRef.getAttribute("id"));
                }
                catch (NumberFormatException e)
                {
                    throw new InvalidEventFileException("User '" + userName
                            + "' has a market-maker event reference with an invalid or missing id.");
                }
                Event event = findEventById(events, eventId);
                if (event == null)
                {
                    throw new InvalidEventFileException("User '" + userName + "' is listed as market maker for event id "
                            + eventId + ", but no event with that id exists in the file.");
                }
                if (event.getMmUserName() != null)
                {
                    throw new InvalidEventFileException("Event '" + event.getName() + "' (id " + eventId
                            + ") has more than one market maker assigned (" + event.getMmUserName() + " and " + userName + ").");
                }
                event.assignMarketMaker(userName);
            }
        }

        for (Event event : events)
        {
            if (event.getMmUserName() == null)
            {
                throw new InvalidEventFileException("Event '" + event.getName() + "' (id " + event.getId()
                        + ") has no market maker assigned to it. Every event must have exactly one.");
            }
        }
    }

    private Event findEventById(List<Event> events, int eventId)
    {
        for (Event event : events)
        {
            if (event.getId() == eventId)
            {
                return event;
            }
        }
        return null;
    }

    private String getChildText(Element parent, String tagName)
    {
        return parent.getElementsByTagName(tagName).item(0).getTextContent().trim();
    }
}
