/*
An example to access the text editors of Bio7!
*/
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.PlatformUI;
import org.eclipse.swt.widgets.Display;
import com.eco.bio7.util.Util;

Display display = Util.getDisplay();
display.asyncExec(new Runnable() {

	public void run() {
		IEditorPart editor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		if (editor instanceof ITextEditor) {

			IDocumentProvider dp = editor.getDocumentProvider();
			IDocument doc = dp.getDocument(editor.getEditorInput());
			int offset = doc.getLineOffset(doc.getNumberOfLines() - 4);
			doc.replace(offset, 0, "Hello World" + "\n");
		}
	}

});

