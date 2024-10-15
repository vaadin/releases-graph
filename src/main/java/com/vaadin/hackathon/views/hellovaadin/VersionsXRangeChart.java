package com.vaadin.hackathon.views.hellovaadin;

import java.time.OffsetDateTime;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.vaadin.flow.component.charts.Chart;
import com.vaadin.flow.component.charts.model.AxisType;
import com.vaadin.flow.component.charts.model.ChartType;
import com.vaadin.flow.component.charts.model.Configuration;
import com.vaadin.flow.component.charts.model.DataLabels;
import com.vaadin.flow.component.charts.model.DataSeries;
import com.vaadin.flow.component.charts.model.DataSeriesItem;
import com.vaadin.flow.component.charts.model.DataSeriesItemXrange;
import com.vaadin.flow.component.charts.model.PlotOptionsXrange;
import com.vaadin.flow.component.charts.model.style.SolidColor;
import com.vaadin.hackathon.git.MajorVersionInfo;

public class VersionsXRangeChart extends Chart {

    private final List<MajorVersionInfo> consolidatedVersionsInfo;
    private boolean isPre;

    public VersionsXRangeChart(final List<MajorVersionInfo> consolidatedVersionsInfo, boolean isPre) {
        this.consolidatedVersionsInfo = consolidatedVersionsInfo;
        this.isPre = isPre;

        final Configuration configuration = this.getConfiguration();
        configuration.getChart().setType(ChartType.XRANGE);
        configuration.setTitle("Vaadin versions by time span " + (isPre ? "(Pre-releases)" : "(Releases)"));
        configuration.getTooltip().setEnabled(true);
        configuration.getTooltip().setXDateFormat("%e.%b.%Y ");

        configuration.getxAxis().setType(AxisType.DATETIME);
        configuration.getyAxis().setTitle("");
        configuration.getyAxis().setCategories(this.categories());
        configuration.getyAxis().setReversed(false);

        configuration.addSeries(this.prepareChartData());

        this.setHeightFull();
        this.setWidthFull();
    }

    private String[] categories() {
        return this.consolidatedVersionsInfo.stream().map(MajorVersionInfo::getMajorVersion).toArray(String[]::new);
    }

    private DataSeries prepareChartData() {
        final String labelFormat = "%s";
        final var itemTimelines = this.consolidatedVersionsInfo.stream()
                                                               .map(item -> {
                                                                   final Long startTime = item.getFirstRelease().toInstant().toEpochMilli();
                                                                   OffsetDateTime last = isPre? item.getLastPreRelease() : item.getLastRelease();
                                                                   final Long endTime = last.toInstant().toEpochMilli();
                                                                   final var seriesItem = new DataSeriesItemXrange(startTime, endTime, this.consolidatedVersionsInfo.indexOf(item));

                                                                   final Period period = Period.between(item.getFirstRelease().toLocalDate(), last.toLocalDate());
                                                                   final var label = labelFormat.formatted(item.getMajorVersion(), period.getYears(), period.getMonths(), period.getDays());
                                                                   seriesItem.setName(label);

                                                                   return seriesItem;
                                                               })
                                                               .map(DataSeriesItem.class::cast)
                                                               .toList();

        final var series = new DataSeries(itemTimelines);
        series.setName("Vaadin version's timespan");

        final PlotOptionsXrange options = new PlotOptionsXrange();
        options.setBorderColor(SolidColor.GRAY);
        options.setPointWidth(20);
        options.getDataLabels().setEnabled(true);

        final DataLabels labels = options.getDataLabels();
        labels.setAllowOverlap(false);
        labels.setFormat("{point.name}");

        series.setPlotOptions(options);
        return series;
    }
}
