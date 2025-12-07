package arduino;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import com.eco.bio7.collection.CustomView;

public class My_Model extends com.eco.bio7.compile.Model {

	public void setup(){

		CustomView view=new CustomView();

		Composite parent = view.getComposite("guiId");

		parent.setLayout(new FillLayout());

		new ModelGui(parent,this,SWT.NONE);

		parent.layout(true);

	}

	public void run(){


	}
}