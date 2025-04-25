package com.vaadin.platform.views.releases;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.charts.events.PointClickEvent;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.spreadsheet.Spreadsheet;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamRegistration;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vaadin.platform.git.GitHubService;
import com.vaadin.platform.git.GitService;
import com.vaadin.platform.git.MajorVersionInfo;
import com.vaadin.platform.views.MainLayout;

@PageTitle("Platform Releases")
@Route(value = "", layout = MainLayout.class)
public class ReleasesView extends VerticalLayout {
    private final GitService gitService;
    private final GitHubService gitHubService;

    private final List<MajorVersionInfo> consolidatedVersionsInfo;

    private boolean isPre = false;

    private VersionsTimelineChart versionsTimelineChart;
    private HorizontalLayout chartArea;

    public ReleasesView(final GitService gitService, final GitHubService gitHubService) throws IOException, InterruptedException {
        this.gitService = gitService;
        this.gitHubService = gitHubService;

        this.consolidatedVersionsInfo = this.gitService.consolidatedVersionsInfo();
        this.init();
        this.setHeightFull();
        this.setWidthFull();
    }

    private void init() {
        this.chartArea = new HorizontalLayout();
        this.chartArea.setHeightFull();
        this.chartArea.setWidthFull();

        final MajorVersionInfo majorVersionInfo = this.consolidatedVersionsInfo
                .get(consolidatedVersionsInfo.size() - 1);
        this.versionsTimelineChart = new VersionsTimelineChart(this.gitHubService, majorVersionInfo);

        final var radioGroup = new RadioButtonGroup<ChartChoice>();
        radioGroup.setLabel("Chart Type");
        radioGroup.setItems(ChartChoice.values());
        radioGroup.addValueChangeListener(event -> {
            this.chartArea.removeAll();
            final var chart = switch (event.getValue()) {
                case BY_PRE_RELEASE_COUNT -> this.chartByReleaseCount(true);
                case BY_RELEASE_TIME_SPAN -> this.chartByTimeSpan(false);
                case BY_RELEASE_COUNT -> this.chartByReleaseCount(false);
                case BY_PRE_RELEASE_TIME_SPAN -> this.chartByTimeSpan(true);
            };
            this.chartArea.add(chart);
        });
        radioGroup.setValue(ChartChoice.BY_PRE_RELEASE_COUNT);

        final var header = new HorizontalLayout();
        header.addAndExpand(radioGroup);
        header.add(this.viewButton(), this.exportButton());
        header.addClassName(LumoUtility.AlignItems.CENTER);

        this.add(header, this.chartArea);
        this.add(this.versionsTimelineChart);
    }

    private Button exportButton() {
        return new Button("Export Raw Data", event -> {
            final StreamResource resource = new StreamResource("vaadin_versions.xlsx",
                    () -> new ByteArrayInputStream(this.prepareSpreadsheet().toByteArray()));
            final StreamRegistration registration = VaadinSession.getCurrent().getResourceRegistry()
                    .registerResource(resource);
            UI.getCurrent().getPage().open(registration.getResourceUri().toString());
        });
    }

    private Button viewButton() {
        return new Button("View Raw Data", event -> {
            final Dialog dialog = new Dialog();
            dialog.setHeaderTitle("Vaadin all released version details");
            dialog.add(this.createSpreadsheet());
            dialog.setWidth("80%");
            dialog.setHeight("80%");
            dialog.open();
        });
    }

    private ByteArrayOutputStream prepareSpreadsheet() {

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final Spreadsheet spreadsheet = this.createSpreadsheet();
        try {
            spreadsheet.write(out);
        } catch (final IOException e) {
            Notification.show("Error exporting charts");
        }
        return out;
    }

    private Spreadsheet createSpreadsheet() {
        final Spreadsheet spreadsheet = new Spreadsheet();
        spreadsheet.setHeightFull();
        spreadsheet.setWidthFull();

        final int versionColumn = 0;
        final int releasedOnColumn = 1;

        final AtomicInteger rowCount = new AtomicInteger(0);
        spreadsheet.createCell(rowCount.get(), versionColumn, "Version");
        spreadsheet.createCell(rowCount.getAndIncrement(), releasedOnColumn, "Released On");

        this.consolidatedVersionsInfo.stream()
                .map(MajorVersionInfo::getAllVersions)
                .flatMap(List::stream)
                .forEach(item -> {
                    spreadsheet.createCell(rowCount.get(), versionColumn, item.getVersion());
                    spreadsheet.createCell(rowCount.getAndIncrement(), releasedOnColumn,
                            item.getReleasedOn().toLocalDateTime());
                });
        spreadsheet.setColumnWidth(0, 150);
        spreadsheet.setColumnWidth(1, 200);
        spreadsheet.setSheetName(spreadsheet.getActiveSheetIndex(), "Vaadin Versions");
        return spreadsheet;
    }

    private VersionsBarChart chartByReleaseCount(boolean isPre) {
        versionsTimelineChart.setPre(isPre);
        final var barChart = new VersionsBarChart(this.consolidatedVersionsInfo, isPre);
        barChart.addPointClickListener(this::updateTimelineChart);
        this.versionsTimelineChart.setVisible(true);
        return barChart;
    }

    private VersionsXRangeChart chartByTimeSpan(boolean isPre) {
        versionsTimelineChart.setPre(isPre);
        final var xRangeChart = new VersionsXRangeChart(this.consolidatedVersionsInfo, isPre);
        xRangeChart.addPointClickListener(this::updateTimelineChart);
        this.versionsTimelineChart.setVisible(false);
        return xRangeChart;
    }

    private void updateTimelineChart(final PointClickEvent event) {
        final var index = event.getItemIndex();
        final MajorVersionInfo selectedMajorVersionInfo = this.consolidatedVersionsInfo.get(index);
        this.versionsTimelineChart.updateChart(selectedMajorVersionInfo);
    }

    private enum ChartChoice {
        BY_PRE_RELEASE_COUNT("by pre-release count"),
        BY_PRE_RELEASE_TIME_SPAN("by pre-release time span"),
        BY_RELEASE_COUNT("by release count"),
        BY_RELEASE_TIME_SPAN("by release time span");

        private String description;

        private ChartChoice(final String description) {
            this.description = description;
        }

        @Override
        public String toString() {
            return this.description;
        }
    };

}
