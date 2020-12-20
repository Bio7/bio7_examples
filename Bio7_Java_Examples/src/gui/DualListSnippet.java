package gui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.mihalis.opal.itemSelector.DLItem;
import org.mihalis.opal.itemSelector.DLItem.LAST_ACTION;
import org.mihalis.opal.itemSelector.DualList;
import org.mihalis.opal.itemSelector.SelectionChangeEvent;
import org.mihalis.opal.itemSelector.SelectionChangeListener;

import com.eco.bio7.collection.CustomView;

/**
 * A simple snipper for the ItemSelector Widget
 *
 */
public class DualListSnippet extends com.eco.bio7.compile.Model {

	public Composite parent;
	final List<DLItem> list = new ArrayList<DLItem>();
   
	public void setup() {

		CustomView view = new CustomView();

		parent = view.getComposite("DualListSnippet");
		Display dis = CustomView.getDisplay();

		dis.syncExec(new Runnable() {
			public void run() {

				final DualList dl = new DualList(parent, SWT.NONE);
				list.add(new DLItem("x"));
				list.add(new DLItem("y"));
				list.add(new DLItem("z"));
				dl.setItems(list);

				dl.addSelectionChangeListener(new SelectionChangeListener() {

					@Override
					public void widgetSelected(final SelectionChangeEvent e) {
						System.out.println("Selection Change Listener called");
						for (final DLItem item : e.getItems()) {
							final StringBuilder sb = new StringBuilder();
							if (item.getLastAction() == LAST_ACTION.SELECTION) {
								sb.append("[SELECTION] ");
							} else {
								sb.append("[DE-SELECTION] ");
							}
							sb.append(item.getText());
							System.out.println(sb.toString());
						}
					}
				});

				dl.setLayoutData(new GridData(GridData.FILL, GridData.FILL,
						true, true));
				parent.layout();
			}
		}); 
		
		list.clear();

	}
   
}
