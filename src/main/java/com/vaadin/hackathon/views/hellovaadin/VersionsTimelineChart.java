package com.vaadin.hackathon.views.hellovaadin;

import java.io.IOException;
import java.time.format.DateTimeFormatter;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.charts.Chart;
import com.vaadin.flow.component.charts.model.AxisType;
import com.vaadin.flow.component.charts.model.ChartType;
import com.vaadin.flow.component.charts.model.Configuration;
import com.vaadin.flow.component.charts.model.DataLabels;
import com.vaadin.flow.component.charts.model.DataSeries;
import com.vaadin.flow.component.charts.model.DataSeriesItem;
import com.vaadin.flow.component.charts.model.DataSeriesItemTimeline;
import com.vaadin.flow.component.charts.model.MarkerSymbolEnum;
import com.vaadin.flow.component.charts.model.PlotOptionsTimeline;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.hackathon.git.GitHubService;
import com.vaadin.hackathon.git.MajorVersionInfo;

public class VersionsTimelineChart extends Chart {

    private final GitHubService gitHubService;

    public VersionsTimelineChart(final GitHubService gitHubService, final MajorVersionInfo majorVersionInfo) throws IOException {
        this.gitHubService = gitHubService;

        final Configuration configuration = this.getConfiguration();
        configuration.getChart().setType(ChartType.TIMELINE);
        configuration.setTitle("Timeline of releases in this version");
        configuration.getTooltip().setEnabled(true);
        configuration.getxAxis().setVisible(false);
        configuration.getxAxis().setType(AxisType.DATETIME);
        configuration.getyAxis().setVisible(false);

        configuration.addSeries(this.prepareVersionsTimelineChartData(majorVersionInfo));

        this.addPointClickListener(event -> {
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

    public void updateChart(final MajorVersionInfo majorVersionInfo) {
        final DataSeries series = this.prepareVersionsTimelineChartData(majorVersionInfo);
        this.getConfiguration().setSeries(series);
        this.drawChart(true);
    }
}
