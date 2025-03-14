package com.vaadin.hackathon.views.hellovaadin;

import java.time.LocalDate;
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

        configuration.getTooltip().setFormatter(
                """
                            function() {
                             const days = (Math.abs(this.x2-this.x)/(24 * 60 * 60 * 1000)).toFixed(0);
                             const date1 = new Date(this.x);
                             const date2 = new Date(this.x2);

                             const year1 = date1.getFullYear();
                             const month1 = String(date1.getMonth() + 1).padStart(2, '0');
                             const day1 = String(date1.getDate()).padStart(2, '0');

                             const year2 = date2.getFullYear();
                             const month2 = String(date2.getMonth() + 1).padStart(2, '0');
                             const day2 = String(date2.getDate()).padStart(2, '0');

                             const f1 = new Intl.DateTimeFormat("en-US", {
                                day: "2-digit",
                                month: "short",
                                year: "numeric"
                            }).format(date1);
                             const f2 = new Intl.DateTimeFormat("en-US", {
                                day: "2-digit",
                                month: "short",
                                year: "numeric"
                            }).format(date2);

                            function calculateDuration(startDate, endDate) {
                                // Ensure startDate is earlier than endDate
                                if (startDate > endDate) {
                                    [startDate, endDate] = [endDate, startDate];
                                }

                                // Extract the initial difference in years, months, and days
                                let years = endDate.getFullYear() - startDate.getFullYear();
                                let months = endDate.getMonth() - startDate.getMonth();
                                let days = endDate.getDate() - startDate.getDate();

                                // Adjust for negative days
                                if (days < 0) {
                                    months -= 1; // Borrow a month
                                    // Get the number of days in the previous month
                                    const previousMonth = new Date(
                                        endDate.getFullYear(),
                                        endDate.getMonth(),
                                        0
                                    ).getDate();
                                    days += previousMonth;
                                }

                                // Adjust for negative months
                                if (months < 0) {
                                    years -= 1; // Borrow a year
                                    months += 12;
                                }

                                let ret = "";
                                if (months) ret += months + " months, ";
                                if (years)  ret += years + " years, ";
                                return ret + days + " days";
                             }

                             const duration = calculateDuration(date1, date2);
                             const version = this.key;
                             return `<span><b>Version:${version}.0</b></span><br/><span>${f1} - ${f2}</span><br/><span>${duration}</span><br/>`; }
                        """);
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
                    OffsetDateTime last = isPre ? item.getLastPreRelease() : item.getLastRelease();
                    final Long endTime = last.toInstant().toEpochMilli();
                    final var seriesItem = new DataSeriesItemXrange(startTime, endTime,
                            this.consolidatedVersionsInfo.indexOf(item));

                    LocalDate startDate = item.getFirstRelease().toLocalDate();
                    LocalDate endDate = last.toLocalDate();

                    final Period period = Period.between(startDate, endDate);
                    long between = ChronoUnit.DAYS.between(startDate, endDate);

                    final var label = labelFormat.formatted(item.getMajorVersion(), between, period.getYears(),
                            period.getMonths(), period.getDays());

                    seriesItem.setName(label);
                    return seriesItem;

                })
                .map(DataSeriesItem.class::cast)
                .toList();

        final var series = new DataSeries(itemTimelines);
        series.setName("Vaadin version");

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
