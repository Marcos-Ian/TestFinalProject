package controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class ReportsController {

    // ─────────── NAV BUTTONS ───────────
    @FXML private Button revenueReportBtn;
    @FXML private Button occupancyReportBtn;
    @FXML private Button feedbackReportBtn;
    @FXML private Button activityLogsBtn;

    // ─────────── MAIN VIEWS (CENTER CONTENT) ───────────
    @FXML private StackPane reportContentPane;

    @FXML private VBox revenueReportView;
    @FXML private VBox occupancyReportView;
    @FXML private VBox feedbackReportView;
    @FXML private VBox activityLogsView;

    // ─────────── REVENUE REPORT CONTROLS ───────────
    @FXML private ComboBox<String> revenuePeriodCombo;
    @FXML private DatePicker revenueFromDate;
    @FXML private DatePicker revenueToDate;
    @FXML private ComboBox<String> revenueRoomTypeCombo;

    @FXML private Button generateRevenueButton;
    @FXML private Button exportRevenueCSVButton;
    @FXML private Button exportRevenuePDFButton;

    @FXML private Label totalRevenueLabel;
    @FXML private Label totalReservationsLabel;
    @FXML private Label avgRevenueLabel;

    @FXML private TableView<?> revenueTable;
    @FXML private TableColumn<?,?> revPeriodColumn;
    @FXML private TableColumn<?,?> revCountColumn;
    @FXML private TableColumn<?,?> revSubtotalColumn;
    @FXML private TableColumn<?,?> revTaxColumn;
    @FXML private TableColumn<?,?> revDiscountsColumn;
    @FXML private TableColumn<?,?> revTotalColumn;

    // ─────────── OCCUPANCY REPORT CONTROLS ───────────
    @FXML private ComboBox<String> occupancyPeriodCombo;
    @FXML private DatePicker occupancyFromDate;
    @FXML private DatePicker occupancyToDate;
    @FXML private ComboBox<String> occupancyRoomTypeCombo;

    @FXML private Button generateOccupancyButton;
    @FXML private Button exportOccupancyCSVButton;
    @FXML private Button exportOccupancyPDFButton;

    @FXML private Label avgOccupancyLabel;
    @FXML private Label totalRoomsLabel;
    @FXML private Label peakOccupancyLabel;

    @FXML private TableView<?> occupancyTable;
    @FXML private TableColumn<?,?> occDateColumn;
    @FXML private TableColumn<?,?> occAvailableColumn;
    @FXML private TableColumn<?,?> occOccupiedColumn;
    @FXML private TableColumn<?,?> occPercentageColumn;

    // ─────────── FEEDBACK REPORT CONTROLS ───────────
    @FXML private ComboBox<String> feedbackPeriodCombo;
    @FXML private DatePicker feedbackFromDate;
    @FXML private DatePicker feedbackToDate;

    @FXML private Button generateFeedbackButton;
    @FXML private Button exportFeedbackButton;

    @FXML private Label avgRatingLabel;
    @FXML private Label totalFeedbackLabel;

    @FXML private TableView<?> feedbackTable;
    @FXML private TableColumn<?,?> fbReservationColumn;
    @FXML private TableColumn<?,?> fbGuestColumn;
    @FXML private TableColumn<?,?> fbRatingColumn;
    @FXML private TableColumn<?,?> fbCommentColumn;
    @FXML private TableColumn<?,?> fbDateColumn;
    @FXML private TableColumn<?,?> fbTagsColumn;

    // ─────────── ACTIVITY LOGS CONTROLS ───────────
    @FXML private DatePicker logsFromDate;
    @FXML private DatePicker logsToDate;
    @FXML private ComboBox<String> actorFilter;
    @FXML private ComboBox<String> actionFilter;

    @FXML private Button generateLogsButton;
    @FXML private Button exportLogsCSVButton;
    @FXML private Button exportLogsTXTButton;

    @FXML private Label logCountLabel;

    @FXML private TableView<?> logsTable;
    @FXML private TableColumn<?,?> logTimestampColumn;
    @FXML private TableColumn<?,?> logActorColumn;
    @FXML private TableColumn<?,?> logActionColumn;
    @FXML private TableColumn<?,?> logEntityTypeColumn;
    @FXML private TableColumn<?,?> logEntityIdColumn;
    @FXML private TableColumn<?,?> logMessageColumn;

    // ─────────── INTERNAL STATE ───────────
    private enum ReportSection {
        REVENUE, OCCUPANCY, FEEDBACK, ACTIVITY_LOGS
    }

    @FXML
    public void initialize() {
        // Default view when the screen loads
        showSection(ReportSection.REVENUE);
    }

    // ─────────── NAVIGATION HANDLERS (OPTIONAL: wire in FXML) ───────────
    @FXML
    private void showRevenueReport() {
        showSection(ReportSection.REVENUE);
    }

    @FXML
    private void showOccupancyReport() {
        showSection(ReportSection.OCCUPANCY);
    }

    @FXML
    private void showFeedbackReport() {
        showSection(ReportSection.FEEDBACK);
    }

    @FXML
    private void showActivityLogs() {
        showSection(ReportSection.ACTIVITY_LOGS);
    }

    // ─────────── HELPER TO TOGGLE VIEWS ───────────
    private void showSection(ReportSection section) {
        setSectionVisible(revenueReportView, section == ReportSection.REVENUE);
        setSectionVisible(occupancyReportView, section == ReportSection.OCCUPANCY);
        setSectionVisible(feedbackReportView, section == ReportSection.FEEDBACK);
        setSectionVisible(activityLogsView, section == ReportSection.ACTIVITY_LOGS);
    }

    private void setSectionVisible(VBox view, boolean visible) {
        if (view != null) {
            view.setVisible(visible);
            view.setManaged(visible);
        }
    }
}
