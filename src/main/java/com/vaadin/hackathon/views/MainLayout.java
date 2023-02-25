package com.vaadin.hackathon.views;

import java.util.Objects;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;

/**
 * The main view is a top-level placeholder for other views.
 */
public class MainLayout extends VerticalLayout implements RouterLayout {

    private Component component;
    private final H2 viewTitle;

    public MainLayout() {
        this.setHeightFull();
        this.setWidthFull();

        this.viewTitle = new H2();
        this.viewTitle.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);
        this.add(this.viewTitle, new Hr());
    }

    @Override
    public void showRouterLayoutContent(final HasElement content) {
        this.getElement().appendChild(Objects.requireNonNull(content.getElement()));
        this.component = (Component) content;
        this.afterNavigation();
    }

    protected void afterNavigation() {
        this.viewTitle.setText(this.getCurrentPageTitle());
    }

    private String getCurrentPageTitle() {
        final PageTitle title = this.component.getClass().getAnnotation(PageTitle.class);
        return title == null ? "" : title.value();
    }
}
