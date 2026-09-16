package guessmarket.ui;

import guessmarket.engine.CommissionType;
import guessmarket.engine.EventInfo;
import guessmarket.engine.EventStatus;
import guessmarket.engine.GuessMarketEngine;
import guessmarket.engine.OrderInfo;
import guessmarket.engine.ParticipantInfo;
import guessmarket.engine.PriceStatsInfo;
import guessmarket.engine.TradeInfo;
import guessmarket.engine.TradingMethod;
import guessmarket.engine.exception.EngineException;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

public class EventsTab
{

    private final GuessMarketEngine engine;
    private final Main main;

    private final ListView<EventInfo> eventListView;
    private final VBox detailBox;
    private final ScrollPane content;

    private final ToggleGroup typeFilterGroup;
    private final ToggleGroup statusFilterGroup;
    private final ToggleGroup commissionFilterGroup;

    private List<EventInfo> allEvents;

    public EventsTab(GuessMarketEngine engine, Main main)
    {
        this.engine = engine;
        this.main = main;
        this.allEvents = new ArrayList<>();

        eventListView = new ListView<>();
        eventListView.setCellFactory(listView -> new EventCell());
        eventListView.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> showDetail(newValue));

        typeFilterGroup = new ToggleGroup();
        statusFilterGroup = new ToggleGroup();
        commissionFilterGroup = new ToggleGroup();

        HBox typeFilterRow = buildTypeFilterRow();
        HBox statusFilterRow = buildStatusFilterRow();
        HBox commissionFilterRow = buildCommissionFilterRow();

        VBox leftBox = new VBox(8,
                new Label("Filter by type:"), typeFilterRow,
                new Label("Filter by status:"), statusFilterRow,
                new Label("Filter by commission method:"), commissionFilterRow,
                new Separator(), eventListView);
        leftBox.setPadding(new Insets(10));
        leftBox.setPrefWidth(360);
        VBox.setVgrow(eventListView, Priority.ALWAYS);

        detailBox = new VBox(6);
        detailBox.setPadding(new Insets(10));
        detailBox.getChildren().add(new Label("Select an event to see its details."));

        ScrollPane detailScroll = new ScrollPane(detailBox);
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
            allEvents = engine.getAllEvents();
        }
        catch (EngineException e)
        {
            allEvents = new ArrayList<>();
        }
        applyFilters();
    }

    private HBox buildTypeFilterRow()
    {
        ToggleButton all = makeFilterButton("All", null, typeFilterGroup, true);
        ToggleButton lmsr = makeFilterButton("LMSR", TradingMethod.LMSR, typeFilterGroup, false);
        ToggleButton orderBook = makeFilterButton("Order Book", TradingMethod.ORDER_BOOK, typeFilterGroup, false);
        return new HBox(5, all, lmsr, orderBook);
    }

    private HBox buildStatusFilterRow()
    {
        ToggleButton all = makeFilterButton("All", null, statusFilterGroup, true);
        ToggleButton notStarted = makeFilterButton("Not started", EventStatus.NOT_STARTED, statusFilterGroup, false);
        ToggleButton active = makeFilterButton("Active", EventStatus.ACTIVE, statusFilterGroup, false);
        ToggleButton closed = makeFilterButton("Closed", EventStatus.CLOSED, statusFilterGroup, false);
        return new HBox(5, all, notStarted, active, closed);
    }

    private HBox buildCommissionFilterRow()
    {
        ToggleButton all = makeFilterButton("All", null, commissionFilterGroup, true);
        ToggleButton onPurchase = makeFilterButton("On purchase", CommissionType.ON_PURCHASE, commissionFilterGroup, false);
        ToggleButton onClose = makeFilterButton("On close", CommissionType.ON_CLOSE, commissionFilterGroup, false);
        return new HBox(5, all, onPurchase, onClose);
    }

    private ToggleButton makeFilterButton(String text, Object value, ToggleGroup group, boolean selected)
    {
        ToggleButton button = new ToggleButton(text);
        button.setUserData(value);
        button.setToggleGroup(group);
        button.setSelected(selected);
        button.setOnAction(event -> applyFilters());
        Main.preventFullDeselection(group);

        return button;
    }

    private void applyFilters()
    {
        Object typeFilter = getSelectedValue(typeFilterGroup);
        Object statusFilter = getSelectedValue(statusFilterGroup);
        Object commissionFilter = getSelectedValue(commissionFilterGroup);

        List<EventInfo> filtered = new ArrayList<>();
        for (EventInfo event : allEvents)
        {
            if (typeFilter != null && event.getMethod() != typeFilter)
            {
                continue;
            }
            if (statusFilter != null && event.getStatus() != statusFilter)
            {
                continue;
            }
            if (commissionFilter != null && event.getCommissionType() != commissionFilter)
            {
                continue;
            }
            filtered.add(event);
        }

        EventInfo previouslySelected = eventListView.getSelectionModel().getSelectedItem();
        eventListView.getItems().setAll(filtered);

        if (previouslySelected != null)
        {
            for (EventInfo event : filtered)
            {
                if (event.getId() == previouslySelected.getId())
                {
                    eventListView.getSelectionModel().select(event);
                    showDetail(event);
                    return;
                }
            }
        }
        showDetail(null);
    }

    private Object getSelectedValue(ToggleGroup group)
    {
        if (group.getSelectedToggle() == null)
        {
            return null;
        }
        return group.getSelectedToggle().getUserData();
    }

    private void showDetail(EventInfo event)
    {
        detailBox.getChildren().clear();
        if (event == null)
        {
            detailBox.getChildren().add(new Label("Select an event to see its details."));
            return;
        }

        detailBox.getChildren().add(new Label("Event " + event.getId() + ": " + event.getName()));
        detailBox.getChildren().add(new Label("Description: " + event.getDescription()));
        detailBox.getChildren().add(new Label("Status: " + event.getStatus()));
        detailBox.getChildren().add(new Label("Type: " + event.getMethod()));
        detailBox.getChildren().add(new Label("Commission: " + event.getCommissionPercent() + "% (" + event.getCommissionType() + ")"));
        detailBox.getChildren().add(new Label("Market maker: " + event.getMmUserName()));
        detailBox.getChildren().add(new Label(String.format("Event account balance: %.2f", event.getAccountBalance())));
        detailBox.getChildren().add(new Label(String.format("Total fees collected: %.2f", event.getTotalFeesCollected())));
        detailBox.getChildren().add(new Separator());

        if (event.getMethod() == TradingMethod.LMSR)
        {
            addLmsrDetail(event);
        }
        else
        {
            addOrderBookDetail(event);
        }

        main.fadeIn(detailBox);
    }

    private void addLmsrDetail(EventInfo event)
    {
        for (int i = 0; i < 2; i++)
        {
            detailBox.getChildren().add(new Label(String.format("%s: price %.2f, shares bought %d",
                    event.getOptionName(i), event.getCurrentPrice(i), event.getQuantity(i))));
        }

        if (event.isClosed())
        {
            detailBox.getChildren().add(new Label("Winning option: " + event.getOptionName(event.getWinningOptionIndex())));
        }

        detailBox.getChildren().add(new Separator());
        detailBox.getChildren().add(new Label("Trade history (most recent first):"));
        List<TradeInfo> history = event.getTradeHistory();
        if (history.isEmpty())
        {
            detailBox.getChildren().add(new Label("No trades yet."));
        }
        else
        {
            for (int i = history.size() - 1; i >= 0; i--)
            {
                TradeInfo trade = history.get(i);
                detailBox.getChildren().add(new Label(String.format("  %s bought %d of %s, paid %.2f",
                        trade.getUserName(), trade.getQuantity(), trade.getOptionName(), trade.getPricePaid())));
            }
        }
    }

    private void addOrderBookDetail(EventInfo event)
    {
        detailBox.getChildren().add(new Label("Base value d: " + event.getBaseValueD() + ", mint allowed: " + event.isAllowMint()));

        if (event.isClosed())
        {
            detailBox.getChildren().add(new Label("Winning option: " + event.getOptionName(event.getWinningOptionIndex())));
        }

        for (int i = 0; i < 2; i++)
        {
            detailBox.getChildren().add(new Separator());
            detailBox.getChildren().add(new Label(event.getOptionName(i) + " order book:"));

            PriceStatsInfo stats = event.getPriceStats(i);
            detailBox.getChildren().add(new Label(String.format("  LAST %s   BID %s   ASK %s   MID %s   SPREAD %s",
                    Main.formatStat(stats.getLast()), Main.formatStat(stats.getBid()), Main.formatStat(stats.getAsk()),
                    Main.formatStat(stats.getMid()), Main.formatStat(stats.getSpread()))));

            List<OrderInfo> orders = event.getOrderBook(i);
            if (orders.isEmpty())
            {
                detailBox.getChildren().add(new Label("  No pending orders."));
            }
            else
            {
                for (OrderInfo order : orders)
                {
                    detailBox.getChildren().add(new Label(String.format("  %s: %s %d @ %.2f",
                            order.getUserName(), order.getSide(), order.getQuantity(), order.getPrice())));
                }
            }
        }

        detailBox.getChildren().add(new Separator());
        detailBox.getChildren().add(new Label("Participants:"));
        List<ParticipantInfo> participants = event.getParticipants();
        if (participants.isEmpty())
        {
            detailBox.getChildren().add(new Label("  No participants yet."));
        }
        else
        {
            for (ParticipantInfo participant : participants)
            {
                detailBox.getChildren().add(new Label(String.format("  %s: %s %d (value %.2f), %s %d (value %.2f)",
                        participant.getUserName(),
                        event.getOptionName(0), participant.getQuantity(0), participant.getValue(0),
                        event.getOptionName(1), participant.getQuantity(1), participant.getValue(1))));
            }
        }
    }

    private static class EventCell extends ListCell<EventInfo>
    {

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
                setText(event.getId() + ". " + event.getName() + "  [" + event.getMethod() + ", " + event.getStatus() + "]");
            }
        }
    }
}
