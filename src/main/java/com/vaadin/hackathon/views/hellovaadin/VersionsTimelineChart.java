package com.vaadin.hackathon.views.hellovaadin;

import java.time.LocalDate;
import java.time.Period;
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
    private boolean isPre;
    private String interval;

    public VersionsTimelineChart(final GitHubService gitHubService, final MajorVersionInfo majorVersionInfo) {
        this.gitHubService = gitHubService;

        final Configuration configuration = this.getConfiguration();
        configuration.getChart().setType(ChartType.TIMELINE);
        configuration.getTooltip().setEnabled(true);
        configuration.getxAxis().setVisible(false);
        configuration.getxAxis().setType(AxisType.DATETIME);
        configuration.getyAxis().setVisible(false);

        this.addPointClickListener(event -> {
            final String versionName = event.getItem().getName();
            final String releaseNotes = this.gitHubService.fetchReleaseNotes(versionName);

            final Parser parser = Parser.builder().build();
            final Node document = parser.parse(releaseNotes);
            final HtmlRenderer renderer = HtmlRenderer.builder().build();
            final String htmlReleaseNotes = "<div>" + renderer.render(document) + "</div>";

            final Dialog dialog = new Dialog();
            dialog.setHeaderTitle("Release Notes for " + versionName + interval);
            dialog.add(new Html(htmlReleaseNotes));
            dialog.open();
        });
        this.updateChart(majorVersionInfo);

        this.setHeightFull();
        this.setWidthFull();
    }

    public void setPre(boolean isPre) {
        this.isPre = isPre;
    }

    private DataSeries prepareChartData(final MajorVersionInfo majorVersionInfo) {
        final var itemTimelines = majorVersionInfo.getAllVersions()
                                                  .stream()
                                                  .filter(item -> {
                    return isPre ? item.getVersion().matches(".*(\\.0|(SNAPSHOT|alpha|beta|rc)\\d*)") : true;
                                                  })
                                                  .map(item -> new DataSeriesItemTimeline(Long.valueOf(item.getReleasedOn().toInstant().toEpochMilli()), item.getVersion(), "",
                                                                                          item.getReleasedOn().format(DateTimeFormatter.RFC_1123_DATE_TIME)))
                                                  .map(DataSeriesItem.class::cast)
                                                  .toList();

        final var series = new DataSeries(itemTimelines);

        final PlotOptionsTimeline options = new PlotOptionsTimeline();
        options.getMarker().setSymbol(MarkerSymbolEnum.CIRCLE);
        final DataLabels labels = options.getDataLabels();
        labels.setAllowOverlap(false);
        labels.setFormat(
                "<span style=\"color:{point.color}\">● </span><span style=\"font-weight: bold;\" > {point.name}</span><br/>{point.x:%d %b %Y}");
        series.setPlotOptions(options);

        return series;
    }

    public void updateChart(final MajorVersionInfo majorVersionInfo) {
        LocalDate first = majorVersionInfo.getFirstRelease().toLocalDate();
        LocalDate last = (isPre? majorVersionInfo.getLastPreRelease():majorVersionInfo.getLastRelease()).toLocalDate();
        Period period = Period.between(first, last);
        
        int months = period.getYears() * 12 + period.getMonths(); // Convert years and months to total months
        int remainingDays = period.getDays(); // Remaining days beyond complete months
        interval = "";
        if (months > 0) {
            interval += " (" + months + " months " + (remainingDays > 0 ? "and " : ")");
        }
        if (remainingDays > 0) {
            interval += (months > 0 ? "" : " (") + remainingDays + " days)";
        }
        
        this.getConfiguration().setTitle("Timeline of releases in version " + majorVersionInfo.getMajorVersion() + interval);
        
        final DataSeries series = this.prepareChartData(majorVersionInfo);
        this.getConfiguration().setSeries(series);
        this.drawChart(true);
    }
}
