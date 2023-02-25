package com.vaadin.hackathon.views.hellovaadin;

import java.io.IOException;
import java.util.List;

import com.vaadin.flow.component.charts.events.PointClickEvent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.hackathon.git.GitHubService;
import com.vaadin.hackathon.git.GitService;
import com.vaadin.hackathon.git.MajorVersionInfo;
import com.vaadin.hackathon.views.MainLayout;

@PageTitle("Hello Vaadin")
@Route(value = "hello", layout = MainLayout.class)
@RouteAlias(value = "", layout = MainLayout.class)
public class HelloVaadinView extends VerticalLayout {
    private final GitService gitService;
    private final GitHubService gitHubService;

    private final List<MajorVersionInfo> consolidatedVersionsInfo;

    private VersionsTimelineChart versionsTimelineChart;
    private HorizontalLayout chartArea;

    public HelloVaadinView(final GitService gitService, final GitHubService gitHubService) throws IOException {
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

        final var radioGroup = new RadioButtonGroup<ChartChoice>();
        radioGroup.setLabel("Chart Type");
        radioGroup.setItems(ChartChoice.values());
        radioGroup.addValueChangeListener(event -> {
            this.chartArea.removeAll();

            final var chart = switch (event.getValue()) {
                case BY_RELEASE_COUNT -> this.chartByReleaseCount();
                case BY_TIME_SPAN -> this.chartByTimeSpan();
            };
            this.chartArea.add(chart);
        });
        radioGroup.setValue(ChartChoice.BY_RELEASE_COUNT);

        this.add(radioGroup, this.chartArea);

        final MajorVersionInfo majorVersionInfo = this.consolidatedVersionsInfo.get(0);
        this.versionsTimelineChart = new VersionsTimelineChart(this.gitHubService, majorVersionInfo);
        this.add(this.versionsTimelineChart);
    }

    private VersionsBarChart chartByReleaseCount() {
        final var barChart = new VersionsBarChart(this.consolidatedVersionsInfo);
        barChart.addPointClickListener(this::updateTimelineChart);
        return barChart;
    }

    private VersionsXRangeChart chartByTimeSpan() {
        final var xRangeChart = new VersionsXRangeChart(this.consolidatedVersionsInfo);
        xRangeChart.addPointClickListener(this::updateTimelineChart);
        return xRangeChart;
    }

    private void updateTimelineChart(final PointClickEvent event) {
        final var index = event.getItemIndex();
        final MajorVersionInfo selectedMajorVersionInfo = this.consolidatedVersionsInfo.get(index);
        this.versionsTimelineChart.updateChart(selectedMajorVersionInfo);
    }

    private enum ChartChoice {
        BY_RELEASE_COUNT("by release count"),
        BY_TIME_SPAN("by time span");

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
