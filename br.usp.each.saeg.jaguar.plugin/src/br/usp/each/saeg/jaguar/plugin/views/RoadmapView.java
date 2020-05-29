package br.usp.each.saeg.jaguar.plugin.views;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Slider;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.part.ViewPart;

import br.usp.each.saeg.jaguar.codeforest.model.Requirement.Type;
import br.usp.each.saeg.jaguar.plugin.Configuration;
import br.usp.each.saeg.jaguar.plugin.JaguarPlugin;
import br.usp.each.saeg.jaguar.plugin.ProjectUtils;
import br.usp.each.saeg.jaguar.plugin.actions.IdAction;
import br.usp.each.saeg.jaguar.plugin.actions.StartJaguarAction;
import br.usp.each.saeg.jaguar.plugin.actions.StopJaguarAction;
import br.usp.each.saeg.jaguar.plugin.data.ClassData;
import br.usp.each.saeg.jaguar.plugin.data.DuaRequirementData;
import br.usp.each.saeg.jaguar.plugin.data.MethodData;
import br.usp.each.saeg.jaguar.plugin.data.PackageData;
import br.usp.each.saeg.jaguar.plugin.data.RequirementData;
import br.usp.each.saeg.jaguar.plugin.editor.OpenEditor;
import br.usp.each.saeg.jaguar.plugin.project.ProjectPersistence;
import br.usp.each.saeg.jaguar.plugin.project.ProjectState;
import br.usp.each.saeg.jaguar.plugin.views.content.RequirementContentProvider;
import br.usp.each.saeg.jaguar.plugin.views.content.RequirementLabelProvider;
import br.usp.each.saeg.jaguar.plugin.views.content.RequirementSorter;
import br.usp.each.saeg.jaguar.plugin.views.content.RoadmapContentProvider;
import br.usp.each.saeg.jaguar.plugin.views.content.RoadmapLabelProvider;
import br.usp.each.saeg.jaguar.plugin.views.content.RoadmapSorter;

public class RoadmapView extends ViewPart {
	
	public static final String ID = "br.usp.each.saeg.jaguar.plugin.views.RoadmapView";
	private final double SLIDER_PRECISION_SCALE = 1000;
	private final int LEVEL_SCORE = 2;
	private TableViewer viewer;
	private Table roadmapTable;
	private TableColumnLayout roadmapTableColumnLayout;
	private TableViewer requirementTableViewer;
	private Table requirementTable;
	private TableColumnLayout requirementTableColumnLayout;
	private List<MethodData> originalRoadmap = new ArrayList<MethodData>();
	private Text textSearch;
	private Slider slider;
	private IProject project;
    private ProjectState state;
	
	@Override
	public void createPartControl(Composite parent) {
		project = ProjectUtils.getCurrentSelectedProject();
        if (project == null) {
            return;
        }
        state = ProjectPersistence.getStateOf(project);
        if (state == null || !state.containsAnalysis()) {
            return;
        }
		
        GridData parentData = new GridData(SWT.FILL,SWT.FILL,true,true);
		parent.setLayout(new GridLayout(1,true));
		parent.setLayoutData(parentData);
		
		
		//sorting the widgets
		Composite textComposite = new Composite(parent,SWT.BORDER);
		Composite roadmapComposite = new Composite(parent,SWT.BORDER);
		Composite sliderComposite = new Composite(parent,SWT.NONE);
		Composite requirementsComposite = new Composite(parent,SWT.BORDER);
				
		
		//Generating the roadmap 
		
		viewer = new TableViewer(roadmapComposite,SWT.SINGLE | SWT.FULL_SELECTION);
		
		roadmapTable = viewer.getTable();
		roadmapTable.setHeaderVisible(true);
		roadmapTable.setLinesVisible(false);
		roadmapTableColumnLayout = new TableColumnLayout();
		roadmapComposite.setLayout(roadmapTableColumnLayout);
		
		TableViewerColumn methodViewerColumn = new TableViewerColumn(viewer,SWT.LEFT);
		ColumnViewerToolTipSupport.enableFor(viewer, ToolTip.RECREATE);
		methodViewerColumn.setLabelProvider(new RoadmapLabelProvider("method"));
		methodViewerColumn.getColumn().setText((Configuration.LANGUAGE_EN)?"Class.Method":"Classe.Metodo");
		
		TableViewerColumn scoreViewerColumn = new TableViewerColumn(viewer,SWT.RIGHT);
		ColumnViewerToolTipSupport.enableFor(viewer, ToolTip.RECREATE);
		scoreViewerColumn.setLabelProvider(new RoadmapLabelProvider("score"));
		scoreViewerColumn.getColumn().setText((Configuration.LANGUAGE_EN)?"Score":"Valor");
		
		roadmapTableColumnLayout.setColumnData(methodViewerColumn.getColumn(), new ColumnWeightData(7,0));
		roadmapTableColumnLayout.setColumnData(scoreViewerColumn.getColumn(), new ColumnWeightData(1,0));
				
		GridDataFactory.fillDefaults().grab(true, true).hint(400, 250).applyTo(roadmapComposite);
		
		viewer.setContentProvider(new RoadmapContentProvider(state));
		//viewer.setLabelProvider(new RoadmapLabelProvider());//it avoids label providers for columns
		viewer.setSorter(new RoadmapSorter());
		viewer.setInput(getViewSite());
		
		createStructure();//to use in the experiment. the data is loaded only when the start button is clicked
		//copyInitialList();// i use this in jaguarview to keep the original packages when using rangeslider or testsearch
		
		viewer.addSelectionChangedListener(new ISelectionChangedListener(){
			@Override
			public void selectionChanged(SelectionChangedEvent event){
				IStructuredSelection selection = (IStructuredSelection)event.getSelection();
				MethodData methodData = (MethodData)selection.getFirstElement();
				OpenEditor.at(methodData.getOpenMarker());
				System.out.println("[Roadmap] click @ "+methodData.toString());
				JaguarPlugin.ui(project,viewer, "[Roadmap] click @ "+methodData.toString());
				requirementTableViewer.getTable().removeAll();
				//or reduce the amount of requirements by levels to be included here : verify the level number and cut
				//for(RequirementData req : getRequirementsByLevelScore(methodData)){
				//all requirements
				for(RequirementData req : methodData.getChildren()){
					if(req.isEnabled()){ 
						requirementTableViewer.add(req);
					}
				}
			}

		});
		
						
		//Generating the tableviewer for requirements
		
		requirementTableViewer = new TableViewer(requirementsComposite,SWT.SINGLE | SWT.FULL_SELECTION);
		requirementTable = requirementTableViewer.getTable();
		requirementTable.setHeaderVisible(true);
		requirementTable.setLinesVisible(false);
		requirementTableColumnLayout = new TableColumnLayout();
		requirementsComposite.setLayout(requirementTableColumnLayout);
		
		ColumnViewerToolTipSupport.enableFor(requirementTableViewer, ToolTip.RECREATE);
				
		if(state.getRequirementType() == Type.LINE){
			TableViewerColumn lineViewerColumn = new TableViewerColumn(requirementTableViewer,SWT.LEFT);
			lineViewerColumn.setLabelProvider(new RequirementLabelProvider("line"));
			lineViewerColumn.getColumn().setText((Configuration.LANGUAGE_EN)?"Statement":"Comando");
			TableViewerColumn scoreLineViewerColumn = new TableViewerColumn(requirementTableViewer,SWT.RIGHT);
			scoreLineViewerColumn.setLabelProvider(new RequirementLabelProvider("score"));
			scoreLineViewerColumn.getColumn().setText((Configuration.LANGUAGE_EN)?"Score":"Valor");
			requirementTableColumnLayout.setColumnData(lineViewerColumn.getColumn(), new ColumnWeightData(7,0));
			requirementTableColumnLayout.setColumnData(scoreLineViewerColumn.getColumn(), new ColumnWeightData(1,0));
		}else{
			TableViewerColumn varViewerColumn = new TableViewerColumn(requirementTableViewer,SWT.LEFT);
			varViewerColumn.setLabelProvider(new RequirementLabelProvider("var"));
			varViewerColumn.getColumn().setText("Var");
			TableViewerColumn defViewerColumn = new TableViewerColumn(requirementTableViewer,SWT.RIGHT);
			defViewerColumn.setLabelProvider(new RequirementLabelProvider("def"));
			defViewerColumn.getColumn().setText("Def");
			TableViewerColumn useViewerColumn = new TableViewerColumn(requirementTableViewer,SWT.RIGHT);
			useViewerColumn.setLabelProvider(new RequirementLabelProvider("use"));
			useViewerColumn.getColumn().setText((Configuration.LANGUAGE_EN)?"Use":"Uso");
			TableViewerColumn scoreDuaViewerColumn = new TableViewerColumn(requirementTableViewer,SWT.RIGHT);
			scoreDuaViewerColumn.setLabelProvider(new RequirementLabelProvider("score"));
			scoreDuaViewerColumn.getColumn().setText((Configuration.LANGUAGE_EN)?"Score":"Valor");
			requirementTableColumnLayout.setColumnData(varViewerColumn.getColumn(), new ColumnWeightData(4,0));
			requirementTableColumnLayout.setColumnData(defViewerColumn.getColumn(), new ColumnWeightData(1,0));
			requirementTableColumnLayout.setColumnData(useViewerColumn.getColumn(), new ColumnWeightData(1,0));
			requirementTableColumnLayout.setColumnData(scoreDuaViewerColumn.getColumn(), new ColumnWeightData(1,0));
		}
		
		GridDataFactory.fillDefaults().grab(true, true).hint(400, 250).applyTo(requirementsComposite);
		
		requirementTableViewer.setContentProvider(new RequirementContentProvider());
		//requirementTableViewer.setLabelProvider(new RequirementLabelProvider());
		requirementTableViewer.setSorter(new RequirementSorter());
		requirementTableViewer.setInput(getViewSite());
		
		requirementTableViewer.addSelectionChangedListener(new ISelectionChangedListener(){
			@Override
			public void selectionChanged(SelectionChangedEvent se){
				IStructuredSelection selection = (IStructuredSelection)requirementTableViewer.getSelection();
				RequirementData reqData = (RequirementData)selection.getFirstElement();
				OpenEditor.at(reqData.getMarker());
				System.out.println("[Line/Dua] click @ "+reqData.toString());
				if(state.getRequirementType() == Type.LINE){
					JaguarPlugin.ui(project, requirementTableViewer, "[Line] click @ "+reqData.toString());
				}else{
					JaguarPlugin.ui(project, requirementTableViewer, "[Dua] click @ "+reqData.toString());
				}
			}
		});
		
		//keep the color of the selected requirement's item and change the font's color
		/*requirementTable.addListener(SWT.EraseItem, new Listener() {
			public void handleEvent(Event event) {
				event.detail &= ~SWT.HOT;
				if ((event.detail & SWT.SELECTED) == 0) return;  item not selected 
				//GC gc = event.gc;
				//Color oldBackground = gc.getBackground();
				//gc.setBackground(new Color(Display.getCurrent(),0,0,0));
				//gc.setBackground(oldBackground);
				event.detail &= ~SWT.SELECTED;
			}
		});*/
		
		//Generating the slider
		sliderComposite.setLayoutData(new GridData(SWT.FILL,SWT.FILL,true,true));
		sliderComposite.setLayout(new GridLayout(3,false));
		
		final Label labelLower = new Label(sliderComposite,SWT.LEFT);
		labelLower.setLayoutData(new GridData(GridData.FILL,GridData.BEGINNING,true,false,1,1));
		
		final Label labelScore = new Label(sliderComposite,SWT.CENTER);
		labelScore.setLayoutData(new GridData(GridData.FILL,GridData.CENTER,true,false,1,1));
				
		final Label labelUpper = new Label(sliderComposite,SWT.RIGHT);
		labelUpper.setLayoutData(new GridData(GridData.FILL,GridData.END,true,false,1,1));
		
		slider = new Slider(sliderComposite,SWT.HORIZONTAL);
		slider.setLayoutData(new GridData(GridData.FILL,GridData.CENTER,true,true,3,1));
		
		slider.setMinimum(0);
		slider.setMaximum((int)SLIDER_PRECISION_SCALE);
		slider.setSelection(0);
		slider.setIncrement(1);
		slider.setPageIncrement(5);
		//slider.setBackground(new Color(Display.getCurrent(),255, 160, 160));
		
		labelLower.setText("Min: " + slider.getMinimum()/SLIDER_PRECISION_SCALE);
		labelUpper.setText("Max: " + slider.getMaximum()/SLIDER_PRECISION_SCALE);
		labelScore.setText(((Configuration.LANGUAGE_EN)?"Current min score: ":"Valor minimo atual: ")+slider.getSelection()/SLIDER_PRECISION_SCALE);
				
		slider.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(final SelectionEvent se){
					
				labelScore.setText(((Configuration.LANGUAGE_EN)?"Current min score: ":"Valor minimo atual: ")+slider.getSelection()/SLIDER_PRECISION_SCALE);
				
				System.out.println("changed min to:"+slider.getSelection()/SLIDER_PRECISION_SCALE);
				JaguarPlugin.ui(project, slider, "changed min to:"+slider.getSelection()/SLIDER_PRECISION_SCALE);
				
				viewer.getTable().removeAll();
				checkScoreBounds(slider.getSelection()/SLIDER_PRECISION_SCALE,slider.getMaximum()/SLIDER_PRECISION_SCALE);
				for(MethodData method : originalRoadmap){
					if(method.isEnabled())
						viewer.add(method);	
				}
				requirementTableViewer.getTable().removeAll();
			}
		});
						
		GridDataFactory.fillDefaults().grab(true, false).hint(400, 50).applyTo(sliderComposite);
		

		//Generating the range slider
		
		/*sliderComposite.setLayoutData(new GridData(SWT.FILL,SWT.FILL,true,true));
		sliderComposite.setLayout(new GridLayout(3,false));
		
		final Label labelLower = new Label(sliderComposite,SWT.LEFT);
		labelLower.setLayoutData(new GridData(GridData.FILL,GridData.BEGINNING,true,false,1,1));
		
		Label labelScore = new Label(sliderComposite,SWT.CENTER);
		labelScore.setLayoutData(new GridData(GridData.FILL,GridData.CENTER,true,false,1,1));
		labelScore.setText("Score");
		
		final Label labelUpper = new Label(sliderComposite,SWT.RIGHT);
		labelUpper.setLayoutData(new GridData(GridData.FILL,GridData.END,true,false,1,1));
		
		final RangeSlider slider = new RangeSlider(sliderComposite,SWT.HORIZONTAL);
		slider.setLayoutData(new GridData(GridData.FILL,GridData.CENTER,true,true,3,1));
		slider.setMinimum(0);
		slider.setMaximum((int)SLIDER_PRECISION_SCALE);
		slider.setLowerValue(0);
		slider.setUpperValue(1000);
		slider.setIncrement(1);
		slider.setPageIncrement(5);
		//slider.setBackground(new Color(Display.getCurrent(),255, 160, 160));
		
		labelLower.setText("Min: " + slider.getLowerValue()/SLIDER_PRECISION_SCALE);
		labelUpper.setText("Max: " + slider.getUpperValue()/SLIDER_PRECISION_SCALE);
				
		slider.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(final SelectionEvent se){
			//there's a bug in the RangeSlider when min and max values are the same
				if(((double)slider.getLowerValue() == (double)slider.getUpperValue()) && (double)slider.getLowerValue() > 0d)
					slider.setLowerValue(slider.getUpperValue()-1);
				
				labelLower.setText("Min: " + slider.getLowerValue()/SLIDER_PRECISION_SCALE);
				labelUpper.setText("Max: " + slider.getUpperValue()/SLIDER_PRECISION_SCALE);
				
				System.out.println("changed to min:"+slider.getLowerValue()/SLIDER_PRECISION_SCALE + ", max:"+slider.getUpperValue()/SLIDER_PRECISION_SCALE);
				JaguarPlugin.ui(project, slider, "changed to min:"+slider.getLowerValue()/SLIDER_PRECISION_SCALE + ", max:"+slider.getUpperValue()/SLIDER_PRECISION_SCALE);
				
				viewer.getTable().removeAll();
				checkScoreBounds(slider.getLowerValue()/SLIDER_PRECISION_SCALE,slider.getUpperValue()/SLIDER_PRECISION_SCALE);
				for(MethodData method : originalRoadmap){
					if(method.isEnabled())
						viewer.add(method);	
				}
				requirementTableViewer.getTable().removeAll();
			}
		});
						
		GridDataFactory.fillDefaults().grab(true, false).hint(400, 50).applyTo(sliderComposite);
		*/
		
		//Generating the text field
		
		textComposite.setLayoutData(new GridData(SWT.FILL,SWT.FILL,true,true));
		textComposite.setLayout(new GridLayout(3,false));
		
		Label labelSearch = new Label(textComposite,SWT.LEFT);
		labelSearch.setLayoutData(new GridData(GridData.FILL,GridData.CENTER,false,false,1,1));
		labelSearch.setText((Configuration.LANGUAGE_EN)?"Search:":"Busca:");
		
		textSearch = new Text(textComposite,SWT.BORDER | SWT.LEFT);
		textSearch.setLayoutData(new GridData(SWT.FILL,SWT.FILL,true,false,2,1));
		
		textSearch.addModifyListener(new ModifyListener(){
			public void modifyText(ModifyEvent me){ //refactor to avoid repeated code
				System.out.println("change to \""+textSearch.getText()+"\"");
				JaguarPlugin.ui(project, textSearch, "change to \""+textSearch.getText()+"\"");
				viewer.getTable().removeAll();
				//checkScoreBounds(slider.getLowerValue()/SLIDER_PRECISION_SCALE,slider.getUpperValue()/SLIDER_PRECISION_SCALE);//for RangeSlider
				checkScoreBounds(slider.getSelection()/SLIDER_PRECISION_SCALE,slider.getMaximum()/SLIDER_PRECISION_SCALE);//for RangeSlider
				for(MethodData method : originalRoadmap){
					if(method.isEnabled())
						viewer.add(method);	
				}
				requirementTableViewer.getTable().removeAll();
			}
			
		});
						
		GridDataFactory.fillDefaults().grab(true, false).hint(400, 35).applyTo(textComposite);	
		
		if(Configuration.EXPERIMENT_VERSION){
		//adding the toolbar buttons
			StopJaguarAction stopAction = new StopJaguarAction(project,this);
			stopAction.setText((Configuration.LANGUAGE_EN)?"Stop debugging session":"Finalizar depuracao");
			ImageDescriptor stopImage = JaguarPlugin.imageDescriptorFromPlugin(JaguarPlugin.PLUGIN_ID, "icon/stop.png");
			stopAction.setImageDescriptor(stopImage);
			
			StartJaguarAction startAction = new StartJaguarAction(project,stopAction,this);
			startAction.setText((Configuration.LANGUAGE_EN)?"Start debugging session":"Iniciar depuracao");
			ImageDescriptor startImage = JaguarPlugin.imageDescriptorFromPlugin(JaguarPlugin.PLUGIN_ID, "icon/bug.png");
			startAction.setImageDescriptor(startImage);//ImageDescriptor.createFromFile(getClass(), "icon/jaguar.png"));
			
			if(!Configuration.EXTERNAL_ID_GENERATION){
				if(Configuration.EXPERIMENT_JAGUAR_FIRST){
					IdAction idAction = new IdAction(project,startAction);
					idAction.setText((Configuration.LANGUAGE_EN)?"Create ID number":"Criar numero ID");
					ImageDescriptor idImage = JaguarPlugin.imageDescriptorFromPlugin(JaguarPlugin.PLUGIN_ID, "icon/key.png");
					idAction.setImageDescriptor(idImage);
									
					getViewSite().getActionBars().getToolBarManager().add(idAction);
				}
			}
			getViewSite().getActionBars().getToolBarManager().add(startAction);
			getViewSite().getActionBars().getToolBarManager().add(stopAction);
		}
		
		if(!Configuration.EXPERIMENT_VERSION){
			loadView();
		}
		
	}

	@Override
	public void setFocus() {
		viewer.getControl().setFocus();
	}
	
	
	/**
	 * This method generates a list from the TreeViewer structure
	 */
	private void copyInitialList() {
		for(TableItem tableItem : viewer.getTable().getItems()){
			MethodData methodItem = (MethodData)tableItem.getData();
			originalRoadmap.add(methodItem);
		}
	}
	
	/**
	 * Update the TableViewer with the RangeSlider entry. The bounds are applied to the requirement level.
	 * Methods which contains requirements between the bound limits will be kept in the table
	 * @param lower
	 * @param upper
	 */
	private void checkScoreBounds(double lower, double upper) {
		boolean keepMethod = false;
		
		for(MethodData method : originalRoadmap){
			keepMethod = false;
			for(RequirementData req : method.getRequirementData()){ //getRequirementsByLevelScore(method)){
				if(req.getScore() >= lower && req.getScore() <= upper && containsTerm(req)){
					req.enable();
					keepMethod = true;
				}
				else{
					req.disable();
				}
			}
			if(keepMethod){
				method.enable();
			}
			else{
				method.disable();
				if(method.getScore() >= lower && method.getScore() <= upper && containsTerm(method)){
					method.enable();
				}
			}
		}
	}
	
	private boolean containsTerm(Object element){ //pass this to SuspiciousElement, and checkStatus and updateStatus too
		if(StringUtils.isBlank(textSearch.getText())){
			return true;
		}
		if(element instanceof MethodData){
			if(StringUtils.isNotBlank(textSearch.getText()) && StringUtils.containsIgnoreCase(((MethodData)element).getName(), StringUtils.trim(textSearch.getText()))){
				return true;
			}
		}
		if(element instanceof RequirementData){
			if(state.getRequirementType() == Type.LINE){
				if(StringUtils.isNotBlank(textSearch.getText()) && StringUtils.containsIgnoreCase(((RequirementData)element).getValue().trim(), StringUtils.trim(textSearch.getText()))){
					return true;
				}
			}else{
				if(StringUtils.isNotBlank(textSearch.getText()) && StringUtils.containsIgnoreCase(((DuaRequirementData)element).getVar(), StringUtils.trim(textSearch.getText()))){
					return true;
				}
			}
		}
		return false;
	}
	
	//return only requirements within the level score
	private List<RequirementData> getRequirementsByLevelScore(MethodData methodData) {
		if(methodData.getRequirementData().isEmpty()){
			return new ArrayList<RequirementData>();
		}
		List<RequirementData> requirementLevelList = new ArrayList<RequirementData>();
		double maxScore = methodData.getRequirementData().get(0).getScore();
		int maxLevel = 0;
		for(RequirementData req : methodData.getRequirementData()){
			if(req.getScore() > 0){
				if(maxScore > req.getScore()){
					maxScore = req.getScore();
					maxLevel++;
				}
				if(maxLevel < LEVEL_SCORE){
					requirementLevelList.add(req);
				}
			}else{
				break;
			}
		}
		return requirementLevelList;
	}
	
	/*
	 * Load the data when the start button is clicked - to be used in the experiments
	 * */
	public void loadView() {
		checkScoreBounds(slider.getSelection()/SLIDER_PRECISION_SCALE,slider.getMaximum()/SLIDER_PRECISION_SCALE);
		for(MethodData method : originalRoadmap){
			if(method.isEnabled())
				viewer.add(method);	
		}
		requirementTableViewer.getTable().removeAll();
	}
	
	private void createStructure(){
		disableNonExecutedElements();
		originalRoadmap = getMethodList();
	}
	
	private List<MethodData> getMethodList(){
		List<MethodData> methodList = new ArrayList<MethodData> ();
		List<PackageData> listPackageData = state.getPackageDataResult();
		for(PackageData pack : listPackageData){
			for(ClassData clazz : pack.getClassData()){
				for(MethodData method : clazz.getMethodData()){
					if(method.getScore() > 0){
						methodList.add(method);
					}
				}
			}
		}
		Collections.sort(methodList);
		return methodList;
	}
	
	public void disableNonExecutedElements(){
		List<PackageData> listPackageData = state.getPackageDataResult();
		for(PackageData packData : listPackageData){
			for(ClassData classData : packData.getClassData()){
				for(MethodData methodData : classData.getMethodData()){
					for(RequirementData reqData : methodData.getRequirementData()){
						if(reqData.getScore() < 0)
							reqData.disable();
					}
					if(methodData.getScore() < 0)
						methodData.disable();
				}
				if(classData.getScore() < 0)
					classData.disable();
			}
			if(packData.getScore() < 0)
				packData.disable();
		}
	}
	
}
