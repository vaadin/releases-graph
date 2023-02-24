package com.vaadin.hackathon.views.hellovaadin;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.charts.Chart;
import com.vaadin.flow.component.charts.model.AxisType;
import com.vaadin.flow.component.charts.model.ChartType;
import com.vaadin.flow.component.charts.model.Configuration;
import com.vaadin.flow.component.charts.model.DataLabels;
import com.vaadin.flow.component.charts.model.DataProviderSeries;
import com.vaadin.flow.component.charts.model.DataSeries;
import com.vaadin.flow.component.charts.model.DataSeriesItem;
import com.vaadin.flow.component.charts.model.DataSeriesItemTimeline;
import com.vaadin.flow.component.charts.model.MarkerSymbolEnum;
import com.vaadin.flow.component.charts.model.PlotOptionsSeries;
import com.vaadin.flow.component.charts.model.PlotOptionsTimeline;
import com.vaadin.flow.component.charts.model.SeriesTooltip;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.ListDataProvider;
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
    private final List<MajorVersionInfo> consolidatedVersionsInfo;

    private final Chart timelinesChart;
    private final GitHubService gitHubService;

    public HelloVaadinView(final GitService gitService, final GitHubService gitHubService) throws IOException {
        this.gitService = gitService;
        this.gitHubService = gitHubService;

        this.consolidatedVersionsInfo = this.gitService.consolidatedVersionsInfo();

        this.add(this.versionsBarChart());

        this.timelinesChart = this.versionsTimeline(this.consolidatedVersionsInfo.get(0));
        this.add(this.timelinesChart);
    }

    private Chart versionsTimeline(final MajorVersionInfo majorVersionInfo) throws IOException {
        final Chart chart = new Chart(ChartType.TIMELINE);

        // Modify the default configuration
        final Configuration conf = chart.getConfiguration();
        conf.setTitle("Timeline of releases in this version");
        conf.getTooltip().setEnabled(true);

        // Add data
        conf.addSeries(this.prepareVersionsTimelineChartData(majorVersionInfo));

        // Configure the axes
        conf.getxAxis().setVisible(false);
        conf.getxAxis().setType(AxisType.DATETIME);
        conf.getyAxis().setVisible(false);

        chart.addPointClickListener(event -> {
            final String versionName = event.getItem().getName();
            final String releaseNotes = this.gitHubService.fetchReleaseNotes(versionName);

            final Parser parser = Parser.builder().build();
            final Node document = parser.parse(releaseNotes);
            final HtmlRenderer renderer = HtmlRenderer.builder().build();
            final String htmlReleaseNotes = "<div>" + renderer.render(document) + "</div>";

            final Dialog dialog = new Dialog();
            dialog.setHeaderTitle("Release Notes for " + versionName);
            dialog.add(new Html(htmlReleaseNotes));
            dialog.open();
        });

        return chart;
    }

    private DataSeries prepareVersionsTimelineChartData(final MajorVersionInfo majorVersionInfo) {
        final var itemTimelines = majorVersionInfo.getAllVersions()
                                                  .stream()
                                                  .map(item -> new DataSeriesItemTimeline(Long.valueOf(item.getReleasedOn().toInstant().toEpochMilli()), item.getVersion(), "",
                                                                                          item.getReleasedOn().format(DateTimeFormatter.RFC_1123_DATE_TIME)))
                                                  .map(DataSeriesItem.class::cast)
                                                  .toList();

        final var series = new DataSeries(itemTimelines);

        final PlotOptionsTimeline options = new PlotOptionsTimeline();
        options.getMarker().setSymbol(MarkerSymbolEnum.CIRCLE);
        final DataLabels labels = options.getDataLabels();
        labels.setAllowOverlap(false);
        labels.setFormat("<span style=\"color:{point.color}\">● </span><span style=\"font-weight: bold;\" > {point.x:%d %b %Y}</span><br/>{point.label}");
        series.setPlotOptions(options);

        return series;
    }

    private Chart versionsBarChart() throws IOException {
        final Chart chart = new Chart(ChartType.COLUMN);
        final Configuration configuration = chart.getConfiguration();
        configuration.setTitle("Vaadin Major versions by number of releases");
        configuration.getTooltip().setEnabled(true);
        configuration.addSeries(this.prepareVersionsBarChartData());
        configuration.getxAxis().setType(AxisType.CATEGORY);
        configuration.getyAxis().setTitle("Number of Releases");
        chart.addSeriesClickListener(event -> {
            final var index = (int) event.getxAxisValue();
            final DataSeries series = this.prepareVersionsTimelineChartData(this.consolidatedVersionsInfo.get(index));
            this.timelinesChart.getConfiguration().setSeries(series);
            this.timelinesChart.drawChart(true);
        });
        return chart;
    }

    private DataProviderSeries<MajorVersionInfo> prepareVersionsBarChartData() throws IOException {
        final var dataprovider = new ListDataProvider<>(this.consolidatedVersionsInfo);

        final var series = new DataProviderSeries<>(dataprovider, MajorVersionInfo::getNumberOfReleases);
        series.setName("Version release counts");
        series.setX(MajorVersionInfo::getMajorVersion);

        final SeriesTooltip seriesTooltip = new SeriesTooltip();
        seriesTooltip.setPointFormatter("function() { return this.x + ' km²' }");

        final PlotOptionsSeries options = new PlotOptionsSeries();
        options.setTooltip(seriesTooltip);
        series.setPlotOptions(options);

        return series;
    }

}
