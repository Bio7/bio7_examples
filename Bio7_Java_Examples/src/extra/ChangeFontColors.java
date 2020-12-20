/*
  An example to change the font color of the Bio7 editors progammatically!
*/
package extra;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import com.eco.bio7.collection.CustomView;
import com.eco.bio7.collection.Work;
import com.eco.bio7.image.Util;

public class ChangeFontColors extends com.eco.bio7.compile.Model {

	public ChangeFontColors() {
		Display dis = Util.getDisplay();
		dis.syncExec(new Runnable() {
			public void run() {
				IPreferenceStore storeR = Work.getEditorStore("r");
				PreferenceConverter.setValue(storeR, "colourkey", new RGB(250, 243, 243));
				PreferenceConverter.setValue(storeR, "colourkey1", new RGB(250, 243, 243));
				PreferenceConverter.setValue(storeR, "colourkey2", new RGB(250, 243, 243));
				PreferenceConverter.setValue(storeR, "colourkey3", new RGB(250, 243, 243));
				PreferenceConverter.setValue(storeR, "colourkey4", new RGB(250, 243, 243));
				PreferenceConverter.setValue(storeR, "colourkey5", new RGB(250, 243, 243));
				PreferenceConverter.setValue(storeR, "colourkey6", new RGB(250, 243, 243));
				PreferenceConverter.setValue(storeR, "colourkey7", new RGB(250, 243, 243));
				PreferenceConverter.setValue(storeR, "colourkey8", new RGB(250, 243, 243));

				IPreferenceStore store = Work.getEditorStore("ijmacro");
				PreferenceConverter.setValue(store, "colourkey", new RGB(167, 236, 33));
				PreferenceConverter.setValue(store, "colourkey1", new RGB(177, 102, 218));
				PreferenceConverter.setValue(store, "colourkey2", new RGB(23, 198, 163));
				PreferenceConverter.setValue(store, "colourkey3", new RGB(128, 128, 128));
				PreferenceConverter.setValue(store, "colourkey4", new RGB(204, 108, 29));
				PreferenceConverter.setValue(store, "colourkey5", new RGB(230, 230, 250));
				PreferenceConverter.setValue(store, "colourkey6", new RGB(250, 243, 243));
				PreferenceConverter.setValue(store, "colourkey7", new RGB(104, 151, 187));
				PreferenceConverter.setValue(store, "colourkey8", new RGB(250, 243, 243));

				IPreferenceStore storeBsh = Work.getEditorStore("groovy");
				PreferenceConverter.setValue(storeBsh, "colourkey", new RGB(167, 236, 33));
				PreferenceConverter.setValue(storeBsh, "colourkey1", new RGB(177, 102, 218));
				PreferenceConverter.setValue(storeBsh, "colourkey2", new RGB(23, 198, 163));
				PreferenceConverter.setValue(storeBsh, "colourkey3", new RGB(128, 128, 128));
				PreferenceConverter.setValue(storeBsh, "colourkey4", new RGB(204, 108, 29));
				PreferenceConverter.setValue(storeBsh, "colourkey5", new RGB(230, 230, 250));
				PreferenceConverter.setValue(storeBsh, "colourkey6", new RGB(250, 243, 243));
				PreferenceConverter.setValue(storeBsh, "colourkey7", new RGB(104, 151, 187));

				IPreferenceStore storeMarkdown = Work.getEditorStore("markdown");
				PreferenceConverter.setValue(storeMarkdown, "colourkey", new RGB(250, 243, 243));
				PreferenceConverter.setValue(storeMarkdown, "colourkey1", new RGB(127, 0, 85));

				IPreferenceStore storePython = Work.getEditorStore("python");
				PreferenceConverter.setValue(storePython, "colourkey", new RGB(167, 236, 33));
				PreferenceConverter.setValue(storePython, "colourkey1", new RGB(177, 102, 218));
				PreferenceConverter.setValue(storePython, "colourkey2", new RGB(23, 198, 163));
				PreferenceConverter.setValue(storePython, "colourkey3", new RGB(128, 128, 128));
				PreferenceConverter.setValue(storePython, "colourkey4", new RGB(204, 108, 29));
				PreferenceConverter.setValue(storePython, "colourkey5", new RGB(230, 230, 250));
				PreferenceConverter.setValue(storePython, "colourkey7", new RGB(250, 243, 243));
				PreferenceConverter.setValue(storePython, "colourkey8", new RGB(104, 151, 187));
				PreferenceConverter.setValue(storePython, "colourkey9", new RGB(250, 243, 243));
				PreferenceConverter.setValue(storePython, "colourkey10", new RGB(0, 0, 0));
				PreferenceConverter.setValue(storePython, "colourkey12", new RGB(0, 0, 0));
			}

		});
	}
}