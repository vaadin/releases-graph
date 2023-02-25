package com.vaadin.hackathon.views.hellovaadin;

import java.io.IOException;
import java.util.List;

import com.vaadin.flow.component.charts.Chart;
import com.vaadin.flow.component.charts.model.AxisType;
import com.vaadin.flow.component.charts.model.ChartType;
import com.vaadin.flow.component.charts.model.Configuration;
import com.vaadin.flow.component.charts.model.DataProviderSeries;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.hackathon.git.MajorVersionInfo;

public class VersionsBarChart extends Chart {

    private final List<MajorVersionInfo> consolidatedVersionsInfo;

    public VersionsBarChart(final List<MajorVersionInfo> consolidatedVersionsInfo) throws IOException {
        this.consolidatedVersionsInfo = consolidatedVersionsInfo;

        final Configuration configuration = this.getConfiguration();
        configuration.getChart().setType(ChartType.COLUMN);
        configuration.setTitle("Vaadin Major versions by number of releases");
        configuration.getTooltip().setEnabled(true);
        configuration.addSeries(this.prepareVersionsBarChartData());
        configuration.getxAxis().setType(AxisType.CATEGORY);
        configuration.getyAxis().setTitle("Number of Releases");
    }

    private DataProviderSeries<MajorVersionInfo> prepareVersionsBarChartData() throws IOException {
        final var dataprovider = new ListDataProvider<>(this.consolidatedVersionsInfo);

        final var series = new DataProviderSeries<>(dataprovider, MajorVersionInfo::getNumberOfReleases);
        series.setName("Version release counts");
        series.setX(MajorVersionInfo::getMajorVersion);
        return series;
    }
}
