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

    public List<Event> load(String filePath) throws InvalidEventFileException
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

        Element commissionElement = (Element) eventElement.getElementsByTagName("comision").item(0);
        int commissionPercent = Integer.parseInt(commissionElement.getTextContent().trim());
        String commissionTypeText = commissionElement.getAttribute("type");
        CommissionType commissionType =
                commissionTypeText.equalsIgnoreCase("on-close") ? CommissionType.ON_CLOSE : CommissionType.ON_PURCHASE;

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
        Element lmsrElement = (Element) methodElement.getElementsByTagName("GM-LMSR").item(0);
        int liquidityB = Integer.parseInt(getChildText(lmsrElement, "b"));

        return new Event(id, name, description, commissionPercent, commissionType, optionA, optionB, liquidityB);
    }

    private String getChildText(Element parent, String tagName)
    {
        return parent.getElementsByTagName(tagName).item(0).getTextContent().trim();
    }
}
