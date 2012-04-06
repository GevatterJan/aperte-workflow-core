package pl.net.bluesoft.rnd.processtool.ui.process;

import com.vaadin.Application;
import com.vaadin.ui.*;
import org.apache.commons.lang3.StringUtils;
import org.vaadin.jonatan.contexthelp.ContextHelp;
import pl.net.bluesoft.rnd.processtool.ProcessToolContext;
import pl.net.bluesoft.rnd.processtool.bpm.ProcessToolBpmSession;
import pl.net.bluesoft.rnd.processtool.model.BpmTask;
import pl.net.bluesoft.rnd.processtool.model.config.ProcessStateAction;
import pl.net.bluesoft.rnd.processtool.model.config.ProcessStateConfiguration;
import pl.net.bluesoft.rnd.processtool.model.config.ProcessStateWidget;
import pl.net.bluesoft.rnd.processtool.plugins.ProcessToolRegistry;
import pl.net.bluesoft.rnd.processtool.ui.WidgetContextSupport;
import pl.net.bluesoft.rnd.processtool.ui.common.FailedProcessToolWidget;
import pl.net.bluesoft.rnd.processtool.ui.widgets.*;
import pl.net.bluesoft.rnd.util.i18n.I18NSource;
import org.aperteworkflow.util.vaadin.VaadinUtility;
import org.aperteworkflow.util.vaadin.help.HelpFactory;
import org.aperteworkflow.util.vaadin.ui.AligningHorizontalLayout;
import pl.net.bluesoft.util.lang.Strings;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.vaadin.ui.Label.CONTENT_XHTML;
import static org.aperteworkflow.util.vaadin.VaadinExceptionHandler.Util.withErrorHandling;
import static pl.net.bluesoft.util.lang.Formats.nvl;

/**
 * @author tlipski@bluesoft.net.pl
 */
public class ProcessDataPane extends VerticalLayout implements WidgetContextSupport {
	private Logger logger = Logger.getLogger(ProcessDataPane.class.getName());

	private ProcessToolBpmSession bpmSession;

	private I18NSource i18NSource;

	private Set<ProcessToolDataWidget> dataWidgets = new HashSet<ProcessToolDataWidget>();
	private boolean isOwner;

	private Application application;
	private ProcessDataDisplayContext displayProcessContext;

	private BpmTask task;
	private ContextHelp contextHelp;
	private HelpFactory helpFactory;

	private ProcessToolActionCallback actionCallback;
	private GuiAction guiAction = null;

	private static enum GuiAction {
		ACTION_PERFORMED, SAVE_PERFORMED, ACTION_FAILED;
	}

	public ProcessDataPane(Application application, ProcessToolBpmSession bpmSession, I18NSource i18NSource, BpmTask bpmTask,
			ProcessDataDisplayContext hideProcessHandler) {
		this.application = application;
		this.bpmSession = bpmSession;
		this.i18NSource = i18NSource;
		displayProcessContext = hideProcessHandler;
		task = bpmTask;

		refreshTask();
		prepare();

		setSpacing(true);
		setMargin(new MarginInfo(false, false, true, true));
		initLayout(false);
	}

	private void prepare() {
		if (helpFactory == null) {
			contextHelp = new ContextHelp();
			application.getMainWindow().getContent().addComponent(contextHelp);
			helpFactory = new HelpFactory(task.getProcessInstance().getDefinition(), getApplication(), i18NSource, "axa_step_help", contextHelp);
		}
		actionCallback = new ProcessToolActionCallback() {
			private void actionCompleted(GuiAction guiAction, ProcessStateAction action) {
				ProcessDataPane.this.guiAction = guiAction;
				refreshTask();
				initLayout(action.getAutohide());
			}

			@Override
			public void actionPerformed(ProcessStateAction action) {
				actionCompleted(GuiAction.ACTION_PERFORMED, action);
			}

			@Override
			public void actionFailed(ProcessStateAction action) {
				actionCompleted(GuiAction.ACTION_FAILED, action);
			}

			@Override
			public WidgetContextSupport getWidgetContextSupport() {
				return ProcessDataPane.this;
			}
		};
	}

	private void initLayout(boolean autoHide) {
		ProcessToolContext ctx = getCurrentContext();

		removeAllComponents();
		setWidth("100%");
		dataWidgets.clear();

		boolean processRunning = bpmSession.isProcessRunning(task.getInternalProcessId(), ctx);
		isOwner = processRunning && !task.isFinished();
		if (!isOwner) {
			//showProcessStateInformation(processRunning);
			if (autoHide) {
				guiAction = null;
				displayProcessContext.hide();
				return;
			}
		}
		guiAction = null;

		ProcessStateConfiguration stateConfiguration = ctx.getProcessDefinitionDAO()
                .getProcessStateConfiguration(task);

		Label stateDescription = new Label(getMessage(stateConfiguration.getDescription()));
		stateDescription.addStyleName("h1 color processtool-title");
        stateDescription.setWidth("100%");

        addComponent(stateDescription);
        setComponentAlignment(stateDescription, Alignment.MIDDLE_LEFT);

		if (Strings.hasText(stateConfiguration.getCommentary())) {
			addComponent(new Label(getMessage(stateConfiguration.getCommentary()), Label.CONTENT_XHTML));
		}
		addComponent(helpFactory.helpIcon(task.getTaskName(), "step.help"));

		displayProcessContext.setCaption(task.getExternalProcessId() != null ? task.getExternalProcessId() : task.getInternalProcessId());

		final VerticalLayout vl = new VerticalLayout();
		vl.setSpacing(true);

		for (ProcessStateWidget w : stateConfiguration.getWidgets()) {
			try {
				ProcessToolWidget realWidget = getWidget(w, stateConfiguration, ctx, null);
				if (realWidget instanceof ProcessToolVaadinRenderable && (!nvl(w.getOptional(), false) || realWidget.hasVisibleData())) {
					processWidgetChildren(w, realWidget, stateConfiguration, ctx, null);
					ProcessToolVaadinRenderable vaadinW = (ProcessToolVaadinRenderable) realWidget;
					vl.addComponent(vaadinW.render());
				}
			}
			catch (Exception e) {
				logger.log(Level.SEVERE, e.getMessage(), e);
				vl.addComponent(new Label(getMessage("process.data.widget.exception-occurred")));
				vl.addComponent(new Label(e.getMessage()));
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				e.printStackTrace(new PrintWriter(baos));
				vl.addComponent(new Label("<pre>" + baos.toString() + "</pre>", CONTENT_XHTML));
			}

		}

		addComponent(vl);
		setExpandRatio(vl, 1.0f);

		if (isOwner) {
			HorizontalLayout buttonLayout = getButtonsPanel(stateConfiguration);
			addComponentAsFirst(buttonLayout);
			setComponentAlignment(buttonLayout, Alignment.BOTTOM_LEFT);

			buttonLayout = getButtonsPanel(stateConfiguration);
			addComponent(buttonLayout);
			setComponentAlignment(buttonLayout, Alignment.TOP_LEFT);
		}
	}

	/*    private void showProcessStateInformation(boolean processRunning) {
        String message = null;
        if (guiAction != null) {
            message = getMessage(guiAction.equals(GuiAction.ACTION_PERFORMED) ? "process.action.performed" : "process.data.save-success");
        }
        if (guiAction == null && !processRunning) {
            message = getMessage("process.data.process-ended");
        }
        else if (task.isFinished()) {
            message = getMessage("process.data.task-finished");
        }
        if (message != null) {
            VaadinUtility.informationNotification(application, message, 1500);
        }
    }*/

	private HorizontalLayout getButtonsPanel(ProcessStateConfiguration stateConfiguration) {
		AligningHorizontalLayout buttonLayout = new AligningHorizontalLayout(Alignment.MIDDLE_RIGHT);
		buttonLayout.setMargin(new MarginInfo(false, true, false, true));
		buttonLayout.setWidth("100%");

		List<ProcessStateAction> actionList = new ArrayList<ProcessStateAction>(stateConfiguration.getActions());
		Collections.sort(actionList, new Comparator<ProcessStateAction>() {
			@Override
			public int compare(ProcessStateAction o1, ProcessStateAction o2) {
				if (o1.getPriority().equals(o2.getPriority())) {
					return new Long(o1.getId()).compareTo(o2.getId());
				}
				return o1.getPriority().compareTo(o2.getPriority());
			}
		});

		for (final ProcessStateAction a : actionList) {
			final ProcessToolActionButton actionButton = makeButton(a);
			actionButton.setEnabled(isOwner);
			actionButton.loadData(task);
			actionButton.setActionCallback(actionCallback);
			if (actionButton instanceof ProcessToolVaadinRenderable) {
				buttonLayout.addComponent(((ProcessToolVaadinRenderable) actionButton).render());
			}
		}

		buttonLayout.addComponentAsFirst(new Label() {{
			setWidth("100%");
		}});

		buttonLayout.recalculateExpandRatios();

		return buttonLayout;
	}

    public List<Component> getToolbarButtons() {
        List<Component> buttons = new ArrayList<Component>();

        Button saveButton = createSaveButton();
        buttons.add(saveButton);

        return buttons;
    }

    public boolean canSaveProcessData() {
        return isOwner;
    }

	private Button createSaveButton() {
		Button saveButton = VaadinUtility.link(i18NSource.getMessage("button.save.process.data"), new Button.ClickListener() {
            @Override
            public void buttonClick(Button.ClickEvent event) {
                withErrorHandling(application, new Runnable() {
                    @Override
                    public void run() {
                        if (validateWidgetsAndSaveData(task)) {
                            refreshTask();
                            guiAction = GuiAction.SAVE_PERFORMED;
                            initLayout(false);
                        }
                    }
                });
			}
		});
        saveButton.addStyleName("with_message");
        saveButton.setDescription(i18NSource.getMessage("button.save.process.desc"));
        saveButton.setIcon(VaadinUtility.imageResource(application, "save.png"));
		saveButton.setEnabled(isOwner);
		return saveButton;
	}

	private void refreshTask() {
		task = refreshTask(bpmSession, task);
	}

	@Override
	public void updateTask(BpmTask task) {
		this.task = task;
	}

	@Override
	public Set<ProcessToolDataWidget> getWidgets() {
		return Collections.unmodifiableSet(dataWidgets);
	}

	@Override
	public void displayValidationErrors(Map<ProcessToolDataWidget, Collection<String>> errorMap) {
		String errorMessage = VaadinUtility.widgetsErrorMessage(i18NSource, errorMap);
		VaadinUtility.validationNotification(application, i18NSource, errorMessage);
	}

	@Override
	public Map<ProcessToolDataWidget, Collection<String>> getWidgetsErrors(BpmTask bpmTask, boolean skipRequired) {
		Map<ProcessToolDataWidget, Collection<String>> errorMap = new HashMap();
		for (ProcessToolDataWidget w : dataWidgets) {
			Collection<String> errors = w.validateData(bpmTask, skipRequired);
			if (errors != null && !errors.isEmpty()) {
				errorMap.put(w, errors);
			}
		}
		return errorMap;
	}

	@Override
	public boolean validateWidgetsAndSaveData(BpmTask task) {
		task = refreshTask(bpmSession, task);
		Map<ProcessToolDataWidget, Collection<String>> errorMap = getWidgetsErrors(task, true);
		if (!errorMap.isEmpty()) {
			displayValidationErrors(errorMap);
			return false;
		}
		saveTaskData(task);
		return true;
	}

	@Override
	public void saveTaskData(BpmTask task, ProcessToolActionButton... actions) {
		for (ProcessToolDataWidget w : dataWidgets) {
			w.saveData(task);
		}
		for (ProcessToolActionButton action : actions) {
			action.saveData(task);
		}
		bpmSession.saveProcessInstance(task.getProcessInstance(), getCurrentContext());
	}

    @Override
	public void saveTaskWithoutData(BpmTask task, ProcessToolActionButton... actions) {
		for (ProcessToolActionButton action : actions) {
			action.saveData(task);
		}
	}

	@Override
	public ProcessToolContext getCurrentContext() {
		return ProcessToolContext.Util.getThreadProcessToolContext();
	}

	@Override
	public BpmTask refreshTask(ProcessToolBpmSession bpmSession, BpmTask bpmTask) {
		return bpmSession.refreshTaskData(bpmTask, getCurrentContext());
	}

	public String getMessage(String key) {
		return i18NSource.getMessage(key);
	}

	private ProcessToolActionButton makeButton(ProcessStateAction a) {
		try {
			ProcessToolContext ctx = getCurrentContext();
			ProcessToolActionButton actionButton = ctx.getRegistry().makeButton(a.getButtonName());
			actionButton.setContext(a, bpmSession, application, i18NSource);
			return actionButton;
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void processWidgetChildren(ProcessStateWidget parentWidgetConfiguration, ProcessToolWidget parentWidgetInstance,
			ProcessStateConfiguration stateConfiguration, ProcessToolContext ctx, String generatorKey) {
		Set<ProcessStateWidget> children = parentWidgetConfiguration.getChildren();
		List<ProcessStateWidget> sortedList = new ArrayList<ProcessStateWidget>(children);
		Collections.sort(sortedList, new Comparator<ProcessStateWidget>() {
			@Override
			public int compare(ProcessStateWidget o1, ProcessStateWidget o2) {
				if (o1.getPriority().equals(o2.getPriority())) {
					return new Long(o1.getId()).compareTo(o2.getId());
				}
				return o1.getPriority().compareTo(o2.getPriority());
			}
		});

		for (ProcessStateWidget subW : sortedList) {
			if(StringUtils.isNotEmpty(subW.getGenerateFromCollection())){
				generateChildren(parentWidgetInstance, stateConfiguration, ctx, subW);
			} else {
			subW.setParent(parentWidgetConfiguration);
				addWidgetChild(parentWidgetInstance, stateConfiguration, ctx, subW, generatorKey);
			}
		}
	}

	private void generateChildren(ProcessToolWidget parentWidgetInstance, ProcessStateConfiguration stateConfiguration, ProcessToolContext ctx,
			ProcessStateWidget subW) {
		String collection = task.getProcessInstance().getSimpleAttributeValue(subW.getGenerateFromCollection(), null);
		if(StringUtils.isEmpty(collection))
			return;
		String[] items = collection.split("[,; ]");

		for(String item : items){
			addWidgetChild(parentWidgetInstance, stateConfiguration, ctx, subW, item);
		}
	}

	private void addWidgetChild(ProcessToolWidget parentWidgetInstance, ProcessStateConfiguration stateConfiguration, ProcessToolContext ctx,
			ProcessStateWidget subW, String generatorKey) {
		ProcessToolWidget widgetInstance = getWidget(subW, stateConfiguration, ctx, generatorKey);
			if (!nvl(subW.getOptional(), false) || widgetInstance.hasVisibleData()) {
			processWidgetChildren(subW, widgetInstance, stateConfiguration, ctx, generatorKey);
				parentWidgetInstance.addChild(widgetInstance);
			}
		}

	private ProcessToolWidget getWidget(ProcessStateWidget w, ProcessStateConfiguration stateConfiguration, ProcessToolContext ctx, String generatorKey) {
		ProcessToolWidget processToolWidget;
		try {
			ProcessToolRegistry toolRegistry = VaadinUtility.getProcessToolContext(application.getContext()).getRegistry();
			processToolWidget = w.getClassName() == null ? toolRegistry.makeWidget(w.getName()) : toolRegistry.makeWidget(w.getClassName());
			processToolWidget.setContext(stateConfiguration, w, i18NSource, bpmSession, application,
			                             bpmSession.getPermissionsForWidget(w, ctx), isOwner);
			processToolWidget.setGeneratorKey(generatorKey);
			if (processToolWidget instanceof ProcessToolDataWidget) {
				((ProcessToolDataWidget) processToolWidget).loadData(task);
				dataWidgets.add((ProcessToolDataWidget) processToolWidget);
			}
		}
		catch (final Exception e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
			FailedProcessToolWidget failedProcessToolVaadinWidget = new FailedProcessToolWidget(e);
			failedProcessToolVaadinWidget.setContext(stateConfiguration, w, i18NSource, bpmSession, application,
			                                         bpmSession.getPermissionsForWidget(w, ctx),
			                                         isOwner);
			dataWidgets.add(failedProcessToolVaadinWidget);
			processToolWidget = failedProcessToolVaadinWidget;
		}
		return processToolWidget;
	}

}
