package gui;

/********************************************************************************
 * Copyright (c) 2026 Marcel Austenfeld
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 ********************************************************************************/

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.draw2d.LightweightSystem;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.nebula.jface.gridviewer.GridTableViewer;
import org.eclipse.nebula.jface.gridviewer.GridViewerColumn;
import org.eclipse.nebula.visualization.xygraph.dataprovider.CircularBufferDataProvider;
import org.eclipse.nebula.visualization.xygraph.dataprovider.Sample;
import org.eclipse.nebula.visualization.xygraph.figures.Trace;
import org.eclipse.nebula.visualization.xygraph.figures.Trace.PointStyle;
import org.eclipse.nebula.visualization.xygraph.figures.Trace.TraceType;
import org.eclipse.nebula.visualization.xygraph.figures.ToolbarArmedXYGraph;
import org.eclipse.nebula.visualization.xygraph.figures.XYGraph;
import org.eclipse.nebula.visualization.xygraph.util.XYGraphMediaFactory;
import org.eclipse.nebula.widgets.grid.Grid;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

import com.eco.bio7.collection.CustomView;

/**
 * An editable, extendable Nebula {@link Grid} whose rows drive a Nebula
 * Visualization {@link XYGraph} chart.
 *
 * <pre>
 * +--------------------------------------------------------------+
 * | [ Add row ]                                                   |
 * +--------------------------------------------------------------+
 * | Time | Temperature | Humidity | Pressure                      |  &lt;- editable grid
 * |  0   |    18.0     |   55.0   |  1000.0                       |
 * |  1   |    20.8     |   66.4   |  1008.0                       |
 * | ...                                                            |
 * +--------------------------------------------------------------+
 * |                     line chart (1 trace per parameter column) |
 * +--------------------------------------------------------------+
 * </pre>
 *
 * Each data column (everything but "Time") becomes one {@link Trace} in the
 * chart below, named after its column header. New rows can be appended with
 * the "Add row" button - the grid is not limited to a fixed number of rows -
 * and every cell is editable in place (double click a cell). By default all
 * rows are plotted; selecting one or more rows in the grid (Ctrl/Shift
 * click) restricts the chart to only that selection - clear the selection to
 * go back to visualizing everything.
 */
public class NebulaGridChartExample extends com.eco.bio7.compile.Model {

	/** The data columns (in addition to "Time") - each is one chart trace. */
	private static final String[] PARAMETERS = { "Temperature", "Humidity", "Pressure" };

	/** One row of the grid: a time stamp plus one value per parameter column. */
	private static class DataRow {
		double time;
		double[] values = new double[PARAMETERS.length];
	}

	private interface DoubleGetter {
		double get(DataRow row);
	}

	private interface DoubleSetter {
		void set(DataRow row, double value);
	}

	private final List<DataRow> rows = new ArrayList<DataRow>();

	private GridTableViewer viewer;

	private final CircularBufferDataProvider[] providers = new CircularBufferDataProvider[PARAMETERS.length];

	public void setup() {

		CustomView view = new CustomView();
		Composite parent = view.getComposite("NebulaGridChartExample");
		parent.setLayout(new GridLayout(1, false));

		Composite toolbar = new Composite(parent, SWT.NONE);
		toolbar.setLayout(new RowLayout());
		Button addRowButton = new Button(toolbar, SWT.PUSH);
		addRowButton.setText("Add row");
		addRowButton.addListener(SWT.Selection, e -> addRow());

		SashForm sash = new SashForm(parent, SWT.VERTICAL);
		sash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		createGrid(sash);
		createChart(sash);

		/* Give the grid roughly 40% of the height, the chart the rest. */
		sash.setWeights(new int[] { 40, 60 });

		seedInitialData();

		parent.layout();

		refreshChart();
	}

	/** The editable, extendable grid - one row per data point. */
	private void createGrid(Composite sashParent) {

		viewer = new GridTableViewer(sashParent, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI | SWT.FULL_SELECTION);

		Grid grid = viewer.getGrid();
		grid.setHeaderVisible(true);
		grid.setLinesVisible(true);

		viewer.setContentProvider(ArrayContentProvider.getInstance());

		addColumn("Time", row -> row.time, (row, value) -> row.time = value);
		for (int i = 0; i < PARAMETERS.length; i++) {
			final int index = i;
			addColumn(PARAMETERS[i], row -> row.values[index], (row, value) -> row.values[index] = value);
		}

		viewer.setInput(rows);

		/* Restricts the chart to the selected rows - or all rows if none are
		 * selected. */
		grid.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				refreshChart();
			}
		});
	}

	/**
	 * Adds one editable column, bound to a field of {@link DataRow} through the
	 * given getter/setter pair.
	 */
	private void addColumn(String title, DoubleGetter getter, DoubleSetter setter) {

		GridViewerColumn column = new GridViewerColumn(viewer, SWT.NONE);
		column.getColumn().setText(title);
		column.getColumn().setWidth(100);
		column.getColumn().setResizeable(true);

		column.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				return String.format("%.2f", getter.get((DataRow) element));
			}
		});

		column.setEditingSupport(new EditingSupport(viewer) {

			protected CellEditor getCellEditor(Object element) {
				return new TextCellEditor(viewer.getGrid());
			}

			protected boolean canEdit(Object element) {
				return true;
			}

			protected Object getValue(Object element) {
				return String.format("%.2f", getter.get((DataRow) element));
			}

			protected void setValue(Object element, Object value) {
				try {
					setter.set((DataRow) element, Double.parseDouble(value.toString()));
				} catch (NumberFormatException ex) {
					return;
				}
				viewer.update(element, null);
				refreshChart();
			}
		});
	}

	/** The chart - 1 trace per parameter column, all sharing 1 X and 1 Y axis. */
	private void createChart(Composite sashParent) {

		XYGraph xyGraph = new XYGraph();
		xyGraph.setTitle("Parameter Visualization");
		xyGraph.primaryXAxis.setTitle("Time");
		xyGraph.primaryXAxis.setAutoScale(true);
		xyGraph.primaryYAxis.setTitle("Value");
		xyGraph.primaryYAxis.setAutoScale(true);

		Color[] colors = { XYGraphMediaFactory.getInstance().getColor(200, 0, 0),
				XYGraphMediaFactory.getInstance().getColor(0, 0, 200),
				XYGraphMediaFactory.getInstance().getColor(0, 150, 0) };

		for (int i = 0; i < PARAMETERS.length; i++) {
			providers[i] = new CircularBufferDataProvider(false);
			providers[i].setBufferSize(1000);

			Trace trace = new Trace(PARAMETERS[i], xyGraph.primaryXAxis, xyGraph.primaryYAxis, providers[i]);
			trace.setTraceColor(colors[i % colors.length]);
			trace.setTraceType(TraceType.SOLID_LINE);
			trace.setPointStyle(PointStyle.FILLED_CIRCLE);
			trace.setLineWidth(2);
			xyGraph.addTrace(trace);
		}

		ToolbarArmedXYGraph toolbarArmedXYGraph = new ToolbarArmedXYGraph(xyGraph);

		Canvas canvas = new Canvas(sashParent, SWT.NONE);
		LightweightSystem lws = new LightweightSystem(canvas);
		lws.setContents(toolbarArmedXYGraph);
	}

	/** Appends a new, initially empty row - the grid is not limited in size. */
	private void addRow() {
		DataRow row = new DataRow();
		row.time = rows.size();
		rows.add(row);
		viewer.refresh();
		refreshChart();
	}

	/** A few example rows so the chart is not empty from the start. */
	private void seedInitialData() {
		for (int i = 0; i < 6; i++) {
			DataRow row = new DataRow();
			row.time = i;
			row.values[0] = 18 + 5 * Math.sin(i * 0.6);
			row.values[1] = 55 + 15 * Math.cos(i * 0.4);
			row.values[2] = 1000 + 8 * i;
			rows.add(row);
		}
		viewer.refresh();
	}

	/**
	 * Rebuilds every trace from the currently visible rows - either the whole
	 * dataset, or only the rows selected in the grid.
	 */
	private void refreshChart() {

		List<DataRow> visible = new ArrayList<DataRow>(getVisibleRows());
		Collections.sort(visible, new Comparator<DataRow>() {
			public int compare(DataRow a, DataRow b) {
				return Double.compare(a.time, b.time);
			}
		});

		for (int i = 0; i < PARAMETERS.length; i++) {
			providers[i].clearTrace();
			for (DataRow row : visible) {
				providers[i].addSample(new Sample(row.time, row.values[i]));
			}
		}
	}

	/** All rows, or only the selected ones if the grid selection is not empty. */
	private List<DataRow> getVisibleRows() {

		int[] indices = viewer.getGrid().getSelectionIndices();
		if (indices == null || indices.length == 0) {
			return rows;
		}

		List<DataRow> selected = new ArrayList<DataRow>();
		for (int index : indices) {
			if (index >= 0 && index < rows.size()) {
				selected.add(rows.get(index));
			}
		}
		return selected;
	}

}
