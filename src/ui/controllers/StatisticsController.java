package ui.controllers;

import ui.charts.AgeChartFrame;
import utils.LoggerUtil;

import javax.swing.SwingUtilities;

import service.StatisticsService;

public class StatisticsController {

	private final StatisticsService statisticsService;

	public StatisticsController(StatisticsService statisticsService) {
		this.statisticsService = statisticsService;
	}

	public void showAgeChart() {
		statisticsService.loadAgeDistributionAsync().thenAccept(dto -> {
			SwingUtilities.invokeLater(() -> {
				try {
					AgeChartFrame f = new AgeChartFrame(dto);
					f.setVisible(true);
				} catch (Exception ex) {
					LoggerUtil.error("Failed to show age chart", ex);
				}
			});
		}).exceptionally(ex -> {
			LoggerUtil.error("Failed to load stats", ex);
			return null;
		});
	}
}
