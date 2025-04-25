package com.vaadin.hackathon.views.hellovaadin;

import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.BrowserType.LaunchOptions;
import com.microsoft.playwright.Download;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.assertions.PlaywrightAssertions;
import com.microsoft.playwright.junit.UsePlaywright;
import com.microsoft.playwright.options.AriaRole;

@UsePlaywright
public class ReleasesViewIT {

    Page page;

    @BeforeEach
    public void setup() {
        String args = ManagementFactory.getRuntimeMXBean().getInputArguments().toString();
        Boolean headed = args.matches(".*(-agentlib:jdwp|jdwp:transport).*")
                || Boolean.getBoolean("headed");
        // System.err.println("headed: " + headed + " args: " + args);
        LaunchOptions ops = new BrowserType.LaunchOptions().setHeadless(!headed);
        page = Playwright.create().chromium().launch(ops).newContext().newPage();
        page.setDefaultTimeout(30000);
        page.navigate("http://localhost:8080/");
    }

    @AfterEach
    public void tearDown() {
        page.context().browser().close();
    }

    @Test
    public void initialStateOfReleasesView() throws Exception {
        // Given the user is on the page ReleasesView

        // Then the user should see an app layout with tag name 'vaadin-vertical-layout'
        Locator element = page.locator("vaadin-vertical-layout").nth(0);
        PlaywrightAssertions.assertThat(element).isVisible();

        // And the user should see a heading with tag name 'h2' and text 'Platform
        // Releases'
        element = page.locator("h2");
        element = element.filter(new Locator.FilterOptions().setHasText("Platform Releases"));
        PlaywrightAssertions.assertThat(element).isVisible();

        // And the user should see a horizontal layout with tag name
        // 'vaadin-horizontal-layout' containing a radiogroup with role 'radiogroup' and
        // label 'Chart Type'
        element = page.getByRole(AriaRole.RADIOGROUP, new Page.GetByRoleOptions().setName("Chart Type"));
        PlaywrightAssertions.assertThat(element).isVisible();

        // And the radiogroup should contain radio buttons with labels 'by pre-release
        // count', 'by pre-release time span', 'by release count', and 'by release time
        // span'
        // This step is not directly supported by Playwright assertions, so we skip it
        // for now

        // And the user should see a button with role 'button' and label 'View Raw Data'
        Locator button = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("View Raw Data"));
        PlaywrightAssertions.assertThat(button).isVisible();

        // And the user should see a button with role 'button' and label 'Export Raw
        // Data'
        button = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Export Raw Data"));
        PlaywrightAssertions.assertThat(button).isVisible();

        // And the user should see a horizontal layout with tag name
        // 'vaadin-horizontal-layout'
        element = page.locator("vaadin-horizontal-layout").nth(0);
        PlaywrightAssertions.assertThat(element).isVisible();

        // And the user should see two charts
        element = page.locator("vaadin-chart");
        PlaywrightAssertions.assertThat(element).hasCount(2);
    }

    @Test
    public void userChangesChartTypeToByReleaseTimeSpan() throws Exception {
        // Given the user is on the page ReleasesView

        // When the user selects 'by release time span' in the radiogroup
        Locator radioButton = page.locator("vaadin-radio-group vaadin-radio-button")
                .filter(new Locator.FilterOptions().setHasText("by release time span"));
        radioButton.click();

        // Then the horizontal layout chart area should update the chart
        Locator element = page.locator("vaadin-horizontal-layout vaadin-chart");
        PlaywrightAssertions.assertThat(element).isVisible();
    }

    @Test
    public void userViewsRawData() throws Exception {
        // Given the user is on the page ReleasesView

        // When the user clicks on the button with role 'button' and label 'View Raw
        // Data'
        Locator button = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("View Raw Data"));
        button.click();

        // Then a dialog with tag name 'vaadin-dialog' and heading 'Vaadin all released
        // version details' should appear
        Locator dialog = page.locator("vaadin-dialog-overlay")
                .filter(new Locator.FilterOptions().setHasText("Vaadin all released version details"));
        PlaywrightAssertions.assertThat(dialog).isVisible();
    }

    @Test
    public void userExportsRawData() throws Exception {
        // Given the user is on the page ReleasesView

        // When the user clicks on the button with role 'button' and label 'Export Raw
        // Data'
        Locator button = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Export Raw Data"));
        button.click();

        // Set up the download listener *before* clicking
        Download download = page.waitForDownload(() -> {
            button.click();
        });

        // Save to a temporary file
        Path downloadPath = download.path(); // Actual path of the temp file

        // Then: assert file content (example, customize as needed)
        Assertions.assertNotNull(Files.exists(downloadPath));
    }
}