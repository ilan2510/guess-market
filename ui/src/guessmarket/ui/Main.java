package guessmarket.ui;

import java.util.List;
import java.util.Scanner;

import guessmarket.engine.CommissionType;
import guessmarket.engine.EventInfo;
import guessmarket.engine.GuessMarketEngine;
import guessmarket.engine.GuessMarketEngineImpl;
import guessmarket.engine.PurchaseResult;
import guessmarket.engine.TradeInfo;
import guessmarket.engine.exception.EngineException;
import guessmarket.engine.exception.InvalidEventFileException;

public class Main
{

    private static final Scanner scanner = new Scanner(System.in);
    private static final GuessMarketEngine engine = new GuessMarketEngineImpl();

    public static void main(String[] args)
    {
        boolean running = true;
        while (running)
        {
            printMenu();
            String choice = readLine().trim();
            switch (choice)
            {
                case "1":
                    handleLoadFile();
                    break;
                case "2":
                    handleDisplayEvents();
                    break;
                case "3":
                    handleEventStatus();
                    break;
                case "4":
                    handleParticipate();
                    break;
                case "5":
                    handleCloseEvent();
                    break;
                case "6":
                    running = false;
                    System.out.println("Goodbye!");
                    break;
                default:
                    System.out.println("Invalid choice. Please enter a number between 1 and 6.");
            }
        }
        scanner.close();
    }

    private static String readLine()
    {
        if (!scanner.hasNextLine())
        {
            System.out.println();
            System.out.println("No more input. Exiting.");
            System.exit(0);
        }
        return scanner.nextLine();
    }

    private static void printMenu()
    {
        System.out.println();
        System.out.println("===== Guess Market =====");
        System.out.println("1. Load events file");
        System.out.println("2. Display events");
        System.out.println("3. Event trading status");
        System.out.println("4. Participate in an event (buy shares)");
        System.out.println("5. Close event");
        System.out.println("6. Exit");
        System.out.print("Choose a command: ");
    }

    private static void handleLoadFile()
    {
        System.out.print("Enter the full path to the XML events file: ");
        String path = readLine();
        try
        {
            List<EventInfo> loaded = engine.loadFile(path);
            System.out.println("File loaded successfully. " + loaded.size() + " event(s) loaded.");
        }
        catch (InvalidEventFileException e)
        {
            System.out.println("Could not load file: " + e.getMessage());
        }
    }

    private static void handleDisplayEvents()
    {
        try
        {
            List<EventInfo> allEvents = engine.getAllEvents();
            printEventList(allEvents);
        }
        catch (EngineException e)
        {
            System.out.println(e.getMessage());
        }
    }

    private static void handleEventStatus()
    {
        try
        {
            List<EventInfo> allEvents = engine.getAllEvents();
            EventInfo chosen = chooseEventFromList(allEvents);
            if (chosen == null)
            {
                return;
            }
            printEventStatus(chosen);
        }
        catch (EngineException e)
        {
            System.out.println(e.getMessage());
        }
    }

    private static void handleParticipate()
    {
        try
        {
            List<EventInfo> activeEvents = engine.getActiveEvents();
            EventInfo chosen = chooseEventFromList(activeEvents);
            if (chosen == null)
            {
                return;
            }
            printEventStatus(chosen);

            System.out.println();
            System.out.println("1. " + chosen.getOptionName(0));
            System.out.println("2. " + chosen.getOptionName(1));
            System.out.print("Which option do you believe in? ");
            int optionChoice = readOptionChoice();
            if (optionChoice == -1)
            {
                return;
            }

            System.out.print("How many shares do you want to buy? ");
            int quantity = readPositiveInt();
            if (quantity == -1)
            {
                return;
            }

            PurchaseResult result = engine.buyShares(chosen.getId(), optionChoice - 1, quantity);
            System.out.println();
            System.out.printf("Shares cost: %.2f%n", result.getSharesCost());
            if (result.getFeeCost() > 0)
            {
                System.out.printf("Fee: %.2f%n", result.getFeeCost());
            }
            System.out.printf("Total paid: %.2f%n", result.getTotalPaid());

            EventInfo updated = engine.getEventInfo(chosen.getId());
            printEventStatus(updated);
        }
        catch (EngineException e)
        {
            System.out.println(e.getMessage());
        }
    }

    private static void handleCloseEvent()
    {
        try
        {
            List<EventInfo> activeEvents = engine.getActiveEvents();
            EventInfo chosen = chooseEventFromList(activeEvents);
            if (chosen == null)
            {
                return;
            }
            printEventStatus(chosen);

            System.out.println();
            System.out.println("Which option actually happened?");
            System.out.println("1. " + chosen.getOptionName(0));
            System.out.println("2. " + chosen.getOptionName(1));
            System.out.print("Choose the winning option: ");
            int optionChoice = readOptionChoice();
            if (optionChoice == -1)
            {
                return;
            }

            engine.closeEvent(chosen.getId(), optionChoice - 1);
            System.out.println();
            System.out.println("Event closed.");
            EventInfo updated = engine.getEventInfo(chosen.getId());
            printEventStatus(updated);
        }
        catch (EngineException e)
        {
            System.out.println(e.getMessage());
        }
    }

    private static EventInfo chooseEventFromList(List<EventInfo> list)
    {
        if (list.isEmpty())
        {
            System.out.println("There are no events to choose from.");
            return null;
        }
        System.out.println();
        for (int i = 0; i < list.size(); i++)
        {
            EventInfo event = list.get(i);
            System.out.println((i + 1) + ". " + event.getName() + " (id " + event.getId() + ")");
        }
        System.out.print("Choose an event by number: ");
        String input = readLine().trim();
        int choice;
        try
        {
            choice = Integer.parseInt(input);
        }
        catch (NumberFormatException e)
        {
            System.out.println("Please enter a number.");
            return null;
        }
        if (choice < 1 || choice > list.size())
        {
            System.out.println("Please enter a number between 1 and " + list.size() + ".");
            return null;
        }
        return list.get(choice - 1);
    }

    private static int readOptionChoice()
    {
        String input = readLine().trim();
        try
        {
            int choice = Integer.parseInt(input);
            if (choice != 1 && choice != 2)
            {
                System.out.println("Please enter 1 or 2.");
                return -1;
            }
            return choice;
        }
        catch (NumberFormatException e)
        {
            System.out.println("Please enter a number (1 or 2).");
            return -1;
        }
    }

    private static int readPositiveInt()
    {
        String input = readLine().trim();
        try
        {
            int value = Integer.parseInt(input);
            if (value <= 0)
            {
                System.out.println("Please enter a positive whole number.");
                return -1;
            }
            return value;
        }
        catch (NumberFormatException e)
        {
            System.out.println("Please enter a whole number.");
            return -1;
        }
    }

    private static void printEventList(List<EventInfo> eventsToPrint)
    {
        for (EventInfo event : eventsToPrint)
        {
            String status;
            if (event.isClosed())
            {
                status = "Closed";
            }
            else
            {
                status = "Active";
            }
            System.out.println();
            System.out.println("Event number: " + event.getId());
            System.out.println("Name: " + event.getName());
            System.out.println("Description: " + event.getDescription());
            System.out.println("Commission: " + event.getCommissionPercent() + "%");
            System.out.println("Commission collected: " + formatCommissionType(event.getCommissionType()));
            System.out.println("Options: " + event.getOptionName(0) + ", " + event.getOptionName(1));
            System.out.println("Status: " + status);
        }
    }

    private static void printEventStatus(EventInfo event)
    {
        System.out.println();
        System.out.println("Status for event: " + event.getName());
        for (int i = 0; i < 2; i++)
        {
            System.out.printf("  %s: price %.2f, shares bought %d%n",
                    event.getOptionName(i), event.getCurrentPrice(i), event.getQuantity(i));
        }
        System.out.printf("Event account balance: %.2f%n", event.getAccountBalance());
        System.out.printf("Total fees collected: %.2f%n", event.getTotalFeesCollected());

        System.out.println("Trade history (most recent first):");
        List<TradeInfo> history = event.getTradeHistory();
        if (history.isEmpty())
        {
            System.out.println("  No trades yet.");
        }
        else
        {
            for (int i = history.size() - 1; i >= 0; i--)
            {
                TradeInfo trade = history.get(i);
                System.out.printf("  %s: %d shares, paid %.2f%n",
                        trade.getOptionName(), trade.getQuantity(), trade.getPricePaid());
            }
        }

        if (event.isClosed())
        {
            System.out.println("This event is closed.");
            System.out.println("Winning option: " + event.getOptionName(event.getWinningOptionIndex()));
            for (int i = 0; i < 2; i++)
            {
                System.out.println("  Total shares of " + event.getOptionName(i) + ": " + event.getQuantity(i));
            }
        }
    }

    private static String formatCommissionType(CommissionType type)
    {
        if (type == CommissionType.ON_PURCHASE)
        {
            return "On purchase";
        }
        else
        {
            return "On close";
        }
    }
}
