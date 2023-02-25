package com.vaadin.hackathon.views.hellovaadin;

import java.io.IOException;
import java.util.List;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
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

    private VersionsBarChart versionsBarChart;
    private VersionsTimelineChart versionsTimelineChart;

    public HelloVaadinView(final GitService gitService, final GitHubService gitHubService) throws IOException {
        this.gitService = gitService;
        this.gitHubService = gitHubService;

        this.consolidatedVersionsInfo = this.gitService.consolidatedVersionsInfo();
        this.init();
    }

    private void init() throws IOException {
        final MajorVersionInfo majorVersionInfo = this.consolidatedVersionsInfo.get(0);

        this.versionsBarChart = new VersionsBarChart(this.consolidatedVersionsInfo);
        this.versionsTimelineChart = new VersionsTimelineChart(this.gitHubService, majorVersionInfo);
        this.add(this.versionsBarChart);
        this.add(this.versionsTimelineChart);

        this.versionsBarChart.addSeriesClickListener(event -> {
            final var index = (int) event.getxAxisValue();
            final MajorVersionInfo selectedMajorVersionInfo = this.consolidatedVersionsInfo.get(index);
            this.versionsTimelineChart.updateChart(selectedMajorVersionInfo);
        });

    }

}
