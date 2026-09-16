package guessmarket.ui;

import guessmarket.engine.EventInfo;
import guessmarket.engine.EventStatus;
import guessmarket.engine.GuessMarketEngine;
import guessmarket.engine.OrderInfo;
import guessmarket.engine.OrderSide;
import guessmarket.engine.OrderSubmitResult;
import guessmarket.engine.ParticipantInfo;
import guessmarket.engine.PriceStatsInfo;
import guessmarket.engine.PurchaseResult;
import guessmarket.engine.TradeInfo;
import guessmarket.engine.TradingMethod;
import guessmarket.engine.UserInfo;
import guessmarket.engine.exception.EngineException;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

public class UsersTab
{

    private final GuessMarketEngine engine;
    private final Main main;

    private final ListView<UserInfo> userListView;
    private final VBox userDetailBox;
    private final ScrollPane content;

    private List<UserInfo> allUsers;
    private String selectedUserName;
    private Integer selectedEventId;

    public UsersTab(GuessMarketEngine engine, Main main)
    {
        this.engine = engine;
        this.main = main;
        this.allUsers = new ArrayList<>();

        userListView = new ListView<>();
        userListView.setCellFactory(listView -> new UserCell());
        userListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) ->
        {
            selectedUserName = (newValue == null) ? null : newValue.getName();
            selectedEventId = null;
            showUserDetail();
        });

        VBox leftBox = new VBox(8, new Label("Users:"), userListView);
        leftBox.setPadding(new Insets(10));
        leftBox.setPrefWidth(280);
        VBox.setVgrow(userListView, Priority.ALWAYS);

        userDetailBox = new VBox(6);
        userDetailBox.setPadding(new Insets(10));
        userDetailBox.getChildren().add(new Label("Select a user to see their details."));

        ScrollPane detailScroll = new ScrollPane(userDetailBox);
        detailScroll.setFitToWidth(true);

        HBox root = new HBox(10, leftBox, detailScroll);
        HBox.setHgrow(detailScroll, Priority.ALWAYS);

        content = new ScrollPane(root);
        content.setFitToWidth(true);
        content.setFitToHeight(true);
    }

    public Node getContent()
    {
        return content;
    }

    public void refresh()
    {
        try
        {
            allUsers = engine.getAllUsers();
        }
        catch (EngineException e)
        {
            allUsers = new ArrayList<>();
        }

        String savedUserName = selectedUserName;
        Integer savedEventId = selectedEventId;
        userListView.getItems().setAll(allUsers);

        if (savedUserName != null)
        {
            for (UserInfo user : allUsers)
            {
                if (user.getName().equals(savedUserName))
                {
                    userListView.getSelectionModel().select(user);
                    break;
                }
            }
        }
        selectedUserName = savedUserName;
        selectedEventId = savedEventId;
        showUserDetail();
    }

    private void showUserDetail()
    {
        userDetailBox.getChildren().clear();

        if (selectedUserName == null)
        {
            userDetailBox.getChildren().add(new Label("Select a user to see their details."));
            return;
        }

        UserInfo user = findUser(selectedUserName);
        if (user == null)
        {
            userDetailBox.getChildren().add(new Label("Select a user to see their details."));
            return;
        }

        userDetailBox.getChildren().add(new Label("Name: " + user.getName()));
        userDetailBox.getChildren().add(new Label(String.format("Balance: %.2f", user.getBalance())));
        if (user.isBlocked())
        {
            Label blockedLabel = new Label("This user is BLOCKED (a past action went below zero) and cannot do anything more.");
            userDetailBox.getChildren().add(blockedLabel);
        }
        userDetailBox.getChildren().add(new Separator());

        List<EventInfo> userEvents = fetchEventsForUser(selectedUserName);
        userDetailBox.getChildren().add(new Label("Your events (managed as MM, or ever participated in):"));

        ListView<EventInfo> eventListView = new ListView<>();
        eventListView.setCellFactory(listView -> new UserEventCell(selectedUserName));
        eventListView.getItems().setAll(userEvents);
        eventListView.setPrefHeight(160);
        eventListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) ->
        {
            selectedEventId = (newValue == null) ? null : newValue.getId();
            showEventActionArea();
        });

        if (selectedEventId != null)
        {
            for (EventInfo event : userEvents)
            {
                if (event.getId() == selectedEventId)
                {
                    eventListView.getSelectionModel().select(event);
                    break;
                }
            }
        }

        userDetailBox.getChildren().add(eventListView);
        userDetailBox.getChildren().add(new Separator());

        VBox eventActionArea = new VBox(6);
        eventActionArea.setId("eventActionArea");
        userDetailBox.getChildren().add(eventActionArea);

        showEventActionArea();
        main.fadeIn(userDetailBox);
    }

    private void showEventActionArea()
    {
        VBox area = findEventActionArea();
        if (area == null)
        {
            return;
        }
        area.getChildren().clear();

        if (selectedUserName == null || selectedEventId == null)
        {
            area.getChildren().add(new Label("Select one of your events above to see its details."));
            return;
        }

        EventInfo event = fetchEvent(selectedEventId);
        if (event == null)
        {
            area.getChildren().add(new Label("This event could not be found."));
            return;
        }

        boolean isMm = event.getMmUserName().equals(selectedUserName);

        area.getChildren().add(new Label("Event " + event.getId() + ": " + event.getName() + "  [" + event.getStatus() + "]"));
        area.getChildren().add(new Label("Market maker: " + event.getMmUserName()));
        area.getChildren().add(new Label(String.format("Event account balance: %.2f", event.getAccountBalance())));

        if (event.getMethod() == TradingMethod.LMSR)
        {
            addLmsrParticipation(area, event, selectedUserName);
        }
        else
        {
            addOrderBookParticipation(area, event, selectedUserName);
        }

        area.getChildren().add(new Separator());

        if (isMm && event.getStatus() == EventStatus.NOT_STARTED)
        {
            addStartEventControls(area, event);
        }
        else if (isMm && event.getStatus() == EventStatus.ACTIVE)
        {
            addCloseEventControls(area, event);
        }
    }

    private VBox findEventActionArea()
    {
        for (Node node : userDetailBox.getChildren())
        {
            if (node instanceof VBox && "eventActionArea".equals(node.getId()))
            {
                return (VBox) node;
            }
        }
        return null;
    }

    private void addLmsrParticipation(VBox area, EventInfo event, String userName)
    {
        addLmsrPriceAndHistory(area, event, userName);

        if (event.isClosed())
        {
            area.getChildren().add(new Label("Winning option: " + event.getOptionName(event.getWinningOptionIndex())));
            return;
        }
        if (event.getStatus() != EventStatus.ACTIVE)
        {
            return;
        }

        area.getChildren().add(new Separator());
        addLmsrBuyForm(area, event, userName);
    }

    private void addLmsrPriceAndHistory(VBox area, EventInfo event, String userName)
    {
        for (int i = 0; i < 2; i++)
        {
            area.getChildren().add(new Label(String.format("%s: price %.2f, shares bought %d",
                    event.getOptionName(i), event.getCurrentPrice(i), event.getQuantity(i))));
        }

        area.getChildren().add(new Label("Your trades in this event (most recent first):"));
        List<TradeInfo> history = event.getTradeHistory();
        boolean anyOwn = false;
        double totalFee = 0;
        for (int i = history.size() - 1; i >= 0; i--)
        {
            TradeInfo trade = history.get(i);
            if (trade.getUserName().equals(userName))
            {
                anyOwn = true;
                totalFee += trade.getFee();
                area.getChildren().add(new Label(String.format("  bought %d of %s, paid %.2f",
                        trade.getQuantity(), trade.getOptionName(), trade.getPricePaid())));
            }
        }
        if (!anyOwn)
        {
            area.getChildren().add(new Label("  You have no trades in this event yet."));
        }
        else
        {
            area.getChildren().add(new Label(String.format("Total fee paid: %.2f", totalFee)));
        }
    }

    private void addLmsrBuyForm(VBox area, EventInfo event, String userName)
    {
        area.getChildren().add(new Label("Buy shares:"));

        ToggleGroup optionGroup = new ToggleGroup();
        ToggleButton option0 = new ToggleButton(event.getOptionName(0));
        option0.setUserData(0);
        option0.setToggleGroup(optionGroup);
        option0.setSelected(true);
        ToggleButton option1 = new ToggleButton(event.getOptionName(1));
        option1.setUserData(1);
        option1.setToggleGroup(optionGroup);
        Main.preventFullDeselection(optionGroup);

        TextField quantityField = new TextField();
        quantityField.setPromptText("Quantity");
        quantityField.setPrefWidth(80);

        Button buyButton = new Button("Buy");
        buyButton.setOnAction(actionEvent ->
        {
            Integer optionIndex = (Integer) optionGroup.getSelectedToggle().getUserData();
            Integer quantity = parsePositiveInt(quantityField.getText());
            if (quantity == null)
            {
                main.showError("Invalid quantity", "Quantity must be a positive whole number.");
                return;
            }
            try
            {
                PurchaseResult result = engine.buyShares(event.getId(), userName, optionIndex, quantity);
                main.showInfo("Purchase complete", String.format(
                        "Shares cost: %.2f%nFee: %.2f%nTotal paid: %.2f",
                        result.getSharesCost(), result.getFeeCost(), result.getTotalPaid()));
                main.refreshAll();
            }
            catch (EngineException e)
            {
                main.showError("Could not buy shares", e.getMessage());
            }
        });

        area.getChildren().add(new HBox(6, option0, option1, quantityField, buyButton));
    }

    private void addOrderBookParticipation(VBox area, EventInfo event, String userName)
    {
        addOrderBookStats(area, event);
        addOrderBookHoldings(area, event, userName);
        addOrderBookOpenOrders(area, event, userName);

        if (event.isClosed())
        {
            area.getChildren().add(new Label("Winning option: " + event.getOptionName(event.getWinningOptionIndex())));
            return;
        }
        if (event.getStatus() != EventStatus.ACTIVE)
        {
            return;
        }

        area.getChildren().add(new Separator());
        addOrderBookOrderForm(area, event, userName);
    }

    private void addOrderBookStats(VBox area, EventInfo event)
    {
        for (int i = 0; i < 2; i++)
        {
            PriceStatsInfo stats = event.getPriceStats(i);
            area.getChildren().add(new Label(String.format("%s: LAST %s  BID %s  ASK %s  MID %s  SPREAD %s",
                    event.getOptionName(i), Main.formatStat(stats.getLast()), Main.formatStat(stats.getBid()),
                    Main.formatStat(stats.getAsk()), Main.formatStat(stats.getMid()), Main.formatStat(stats.getSpread()))));
        }
    }

    private void addOrderBookHoldings(VBox area, EventInfo event, String userName)
    {
        area.getChildren().add(new Label("Your holdings in this event:"));
        boolean anyHolding = false;
        for (ParticipantInfo participant : event.getParticipants())
        {
            if (!participant.getUserName().equals(userName))
            {
                continue;
            }
            anyHolding = true;
            for (int i = 0; i < 2; i++)
            {
                area.getChildren().add(new Label(String.format("  %s: %d shares, paid %.2f",
                        event.getOptionName(i), participant.getQuantity(i), participant.getAmountPaid(i))));
                if (event.isClosed())
                {
                    double payout = 0;
                    if (i == event.getWinningOptionIndex())
                    {
                        payout = participant.getQuantity(i) * event.getBaseValueD();
                    }
                    double profitLoss = payout - participant.getAmountPaid(i);
                    area.getChildren().add(new Label(String.format("    profit/loss: %.2f", profitLoss)));
                }
            }
            area.getChildren().add(new Label(String.format("Total fee paid: %.2f", participant.getFeePaid())));
        }
        if (!anyHolding)
        {
            area.getChildren().add(new Label("  You hold no shares in this event."));
        }
    }

    private void addOrderBookOpenOrders(VBox area, EventInfo event, String userName)
    {
        area.getChildren().add(new Label("Your open orders:"));
        boolean anyOrder = false;
        for (int i = 0; i < 2; i++)
        {
            for (OrderInfo order : event.getOrderBook(i))
            {
                if (order.getUserName().equals(userName))
                {
                    anyOrder = true;
                    area.getChildren().add(new Label(String.format("  %s: %s %d @ %.2f",
                            event.getOptionName(i), order.getSide(), order.getQuantity(), order.getPrice())));
                }
            }
        }
        if (!anyOrder)
        {
            area.getChildren().add(new Label("  You have no open orders in this event."));
        }
    }

    private void addOrderBookOrderForm(VBox area, EventInfo event, String userName)
    {
        area.getChildren().add(new Label("Submit an order:"));

        ToggleGroup optionGroup = new ToggleGroup();
        ToggleButton option0 = new ToggleButton(event.getOptionName(0));
        option0.setUserData(0);
        option0.setToggleGroup(optionGroup);
        option0.setSelected(true);
        ToggleButton option1 = new ToggleButton(event.getOptionName(1));
        option1.setUserData(1);
        option1.setToggleGroup(optionGroup);
        Main.preventFullDeselection(optionGroup);

        ToggleGroup sideGroup = new ToggleGroup();
        ToggleButton buySide = new ToggleButton("Buy");
        buySide.setUserData(OrderSide.BUY);
        buySide.setToggleGroup(sideGroup);
        buySide.setSelected(true);
        ToggleButton sellSide = new ToggleButton("Sell");
        sellSide.setUserData(OrderSide.SELL);
        sellSide.setToggleGroup(sideGroup);
        Main.preventFullDeselection(sideGroup);

        TextField quantityField = new TextField();
        quantityField.setPromptText("Quantity");
        quantityField.setPrefWidth(70);

        TextField priceField = new TextField();
        priceField.setPromptText("Price/share");
        priceField.setPrefWidth(80);

        Button submitButton = new Button("Submit order");
        submitButton.setOnAction(actionEvent ->
        {
            Integer optionIndex = (Integer) optionGroup.getSelectedToggle().getUserData();
            OrderSide side = (OrderSide) sideGroup.getSelectedToggle().getUserData();
            Integer quantity = parsePositiveInt(quantityField.getText());
            Double price = parsePositiveDouble(priceField.getText());
            if (quantity == null)
            {
                main.showError("Invalid quantity", "Quantity must be a positive whole number.");
                return;
            }
            if (price == null)
            {
                main.showError("Invalid price", "Price must be a positive number, for example 0.45.");
                return;
            }
            try
            {
                OrderSubmitResult result = engine.submitOrder(event.getId(), userName, optionIndex, side, quantity, price);
                main.showInfo("Order submitted", String.format(
                        "Filled now: %d%nStill resting in the order book: %d%nFee paid: %.2f",
                        result.getFilledQuantity(), result.getRestingQuantity(), result.getFeePaid()));
                main.refreshAll();
            }
            catch (EngineException e)
            {
                main.showError("Could not submit order", e.getMessage());
            }
        });

        area.getChildren().add(new HBox(6, option0, option1, buySide, sellSide, quantityField, priceField, submitButton));
    }

    private void addStartEventControls(VBox area, EventInfo event)
    {
        Button startButton = new Button("Start this event (pay the opening cost from your balance)");
        startButton.setOnAction(actionEvent ->
        {
            try
            {
                engine.startEvent(event.getId(), selectedUserName);
                main.showInfo("Event started", "The event is now active.");
                main.refreshAll();
            }
            catch (EngineException e)
            {
                main.showError("Could not start event", e.getMessage());
            }
        });
        area.getChildren().add(startButton);
    }

    private void addCloseEventControls(VBox area, EventInfo event)
    {
        area.getChildren().add(new Label("Close this event and declare the winner:"));

        ToggleGroup winnerGroup = new ToggleGroup();
        ToggleButton option0 = new ToggleButton(event.getOptionName(0));
        option0.setUserData(0);
        option0.setToggleGroup(winnerGroup);
        option0.setSelected(true);
        ToggleButton option1 = new ToggleButton(event.getOptionName(1));
        option1.setUserData(1);
        option1.setToggleGroup(winnerGroup);
        Main.preventFullDeselection(winnerGroup);

        Button closeButton = new Button("Close event");
        closeButton.setOnAction(actionEvent ->
        {
            Integer winningOptionIndex = (Integer) winnerGroup.getSelectedToggle().getUserData();
            try
            {
                engine.closeEvent(event.getId(), selectedUserName, winningOptionIndex);
                main.showInfo("Event closed", "The event has been closed and winners have been paid.");
                main.refreshAll();
            }
            catch (EngineException e)
            {
                main.showError("Could not close event", e.getMessage());
            }
        });

        area.getChildren().add(new HBox(6, option0, option1, closeButton));
    }

    private List<EventInfo> fetchEventsForUser(String userName)
    {
        try
        {
            return engine.getEventsForUser(userName);
        }
        catch (EngineException e)
        {
            return new ArrayList<>();
        }
    }

    private EventInfo fetchEvent(int eventId)
    {
        try
        {
            return engine.getEventInfo(eventId);
        }
        catch (EngineException e)
        {
            return null;
        }
    }

    private UserInfo findUser(String userName)
    {
        try
        {
            return engine.getUserInfo(userName);
        }
        catch (EngineException e)
        {
            return null;
        }
    }

    private Integer parsePositiveInt(String text)
    {
        try
        {
            int value = Integer.parseInt(text.trim());
            if (value <= 0)
            {
                return null;
            }
            return value;
        }
        catch (NumberFormatException e)
        {
            return null;
        }
    }

    private Double parsePositiveDouble(String text)
    {
        try
        {
            double value = Double.parseDouble(text.trim());
            if (value <= 0)
            {
                return null;
            }
            return value;
        }
        catch (NumberFormatException e)
        {
            return null;
        }
    }

    private static class UserCell extends ListCell<UserInfo>
    {

        @Override
        protected void updateItem(UserInfo user, boolean empty)
        {
            super.updateItem(user, empty);
            if (empty || user == null)
            {
                setText(null);
            }
            else
            {
                String blockedTag = user.isBlocked() ? "  [BLOCKED]" : "";
                setText(user.getName() + "  (" + String.format("%.2f", user.getBalance()) + ")" + blockedTag);
            }
        }
    }

    private static class UserEventCell extends ListCell<EventInfo>
    {

        private final String userName;

        UserEventCell(String userName)
        {
            this.userName = userName;
        }

        @Override
        protected void updateItem(EventInfo event, boolean empty)
        {
            super.updateItem(event, empty);
            if (empty || event == null)
            {
                setText(null);
            }
            else
            {
                String role = event.getMmUserName().equals(userName) ? "MM" : "participant";
                setText(event.getId() + ". " + event.getName() + "  [" + event.getStatus() + ", " + role + "]");
            }
        }
    }
}
