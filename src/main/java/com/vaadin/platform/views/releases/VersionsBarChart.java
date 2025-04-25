package com.vaadin.platform.views.releases;

import java.util.List;

import com.vaadin.flow.component.charts.Chart;
import com.vaadin.flow.component.charts.model.AxisType;
import com.vaadin.flow.component.charts.model.ChartType;
import com.vaadin.flow.component.charts.model.Configuration;
import com.vaadin.flow.component.charts.model.DataProviderSeries;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.platform.git.MajorVersionInfo;

public class VersionsBarChart extends Chart {

    private final List<MajorVersionInfo> consolidatedVersionsInfo;

    private String release;
    private SerializableFunction<MajorVersionInfo, Object> callBack;

    public VersionsBarChart(final List<MajorVersionInfo> consolidatedVersionsInfo, boolean isPre) {
        this.consolidatedVersionsInfo = consolidatedVersionsInfo;
        release = isPre ? "Pre-releases" : "Releases";
        callBack = isPre ? MajorVersionInfo::getNumberOfPreReleases : MajorVersionInfo::getNumberOfReleases;

        final Configuration configuration = this.getConfiguration();
        configuration.getChart().setType(ChartType.COLUMN);
        configuration.setTitle(release + " per version");
        configuration.getTooltip().setEnabled(true);
        configuration.addSeries(this.prepareChartData());
        configuration.getxAxis().setType(AxisType.CATEGORY);
        configuration.getyAxis().setTitle("Number of " + release);

        this.setHeightFull();
        this.setWidthFull();
    }

    private DataProviderSeries<MajorVersionInfo> prepareChartData() {
        final var dataprovider = new ListDataProvider<>(this.consolidatedVersionsInfo);
        final var series = new DataProviderSeries<>(dataprovider, callBack);
        series.setName(release + " counts");
        series.setX(MajorVersionInfo::getMajorVersion);
        return series;
    }
}
