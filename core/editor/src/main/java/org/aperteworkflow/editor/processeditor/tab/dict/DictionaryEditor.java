package org.aperteworkflow.editor.processeditor.tab.dict;

import com.vaadin.Application;
import com.vaadin.data.Property;
import com.vaadin.data.util.BeanItem;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.ui.*;
import org.aperteworkflow.editor.processeditor.tab.dict.wrappers.*;
import org.aperteworkflow.editor.vaadin.DataHandler;
import org.aperteworkflow.util.dict.ui.DictionaryItemForm;
import org.aperteworkflow.util.dict.ui.DictionaryItemFormFieldFactory;
import org.aperteworkflow.util.dict.ui.DictionaryItemTableBuilder;
import org.aperteworkflow.util.dict.ui.fields.DictionaryItemExtensionField;
import org.aperteworkflow.util.dict.ui.fields.DictionaryItemValuesField;
import org.aperteworkflow.util.dict.wrappers.DictionaryItemExtensionWrapper;
import org.aperteworkflow.util.dict.wrappers.DictionaryItemValueWrapper;
import org.aperteworkflow.util.vaadin.ui.Dialog;
import pl.net.bluesoft.rnd.processtool.dict.DictionaryLoader;
import pl.net.bluesoft.rnd.processtool.dict.xml.DictionaryEntry;
import pl.net.bluesoft.rnd.processtool.dict.xml.ProcessDictionaries;
import pl.net.bluesoft.rnd.util.i18n.I18NSource;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Set;

import static org.aperteworkflow.editor.processeditor.tab.dict.wrappers.XmlDictionaryWrapper.*;
import static org.aperteworkflow.util.vaadin.VaadinUtility.*;
import static pl.net.bluesoft.util.lang.cquery.CQuery.from;

public class DictionaryEditor extends VerticalLayout implements TabSheet.CloseHandler, Button.ClickListener, DataHandler,
		DictionaryItemTableBuilder.DictionaryItemModificationHandler<XmlDictionaryItemWrapper> {
//	private Label languageDescriptionLabel;
	private XmlProcessDictionariesWrapper processDictionaries;

	private Panel dictionaryLayout;
	private Select dictionarySelect;
	private Select languageSelect;
	private Button addDictionaryButton;
	private Button addLanguageButton;
	private TextField dictionaryNameField;
	private TextField dictionaryDescriptionField;
	private TextField editPermissionField;
	private Select defaultLanguageField;
	private Button addEntryButton;

	private BeanItemContainer<XmlDictionaryItemWrapper> container;

	private DictionaryItemTableBuilder builder = new DictionaryItemTableBuilder<
			DictionaryEntry,
			XmlDictionaryItemValueWrapper,
			XmlDictionaryItemWrapper
		>(this) {
		@Override
		protected DictionaryItemForm createDictionaryItemForm(Application application, I18NSource source, BeanItem<XmlDictionaryItemWrapper> item) {
			return new DictionaryItemForm(application, source, item) {
				@Override
				protected DictionaryItemFormFieldFactory createItemFormFieldFactory(Application application, I18NSource source, Set<String> visiblePropertyIds, Set<String> editablePropertyIds, Set<String> requiredPropertyIds) {
					return new DictionaryItemFormFieldFactory(application, source, visiblePropertyIds, editablePropertyIds, requiredPropertyIds) {
						@Override
						protected DictionaryItemValuesField createItemValuesField(Application application, I18NSource source, String valueType) {
							return new DictionaryItemValuesField(application, source, valueType) {
								@Override
								protected DictionaryItemValueWrapper createItemValueWrapper() {
									return new XmlDictionaryItemValueWrapper();
								}

								@Override
								protected DictionaryItemExtensionField createItemExtensionField(Application application, I18NSource source) {
									return new DictionaryItemExtensionField(application, source) {
										@Override
										protected DictionaryItemExtensionWrapper createDictionaryItemExtensionWrapper() {
											return new XmlDictionaryItemExtensionWrapper();
										}
									};
								}
							};
						}
					};
				}
			};
		}

		@Override
		protected Application getApplication() {
			return DictionaryEditor.this.getApplication();
		}

		@Override
		protected I18NSource getI18NSource() {
			return I18NSource.ThreadUtil.getThreadI18nSource();
		}
	};

    public DictionaryEditor() {
        initComponent();
        initLayout();
    }

	@Override
    public void onTabClose(TabSheet tabsheet, final Component tabContent) {
    }

    private void initComponent() {
        I18NSource messages = I18NSource.ThreadUtil.getThreadI18nSource();
//        languageDescriptionLabel = new Label(messages.getMessage("messages.dictionary.description"));

		dictionarySelect = new Select(messages.getMessage("Słownik"));
		dictionarySelect.setWidth("250px");
		dictionarySelect.setImmediate(true);
		dictionarySelect.addListener(new Property.ValueChangeListener() {
			@Override
			public void valueChange(Property.ValueChangeEvent valueChangeEvent) {
				displaySelectedDictionary();
			}
		});

		languageSelect = new Select(messages.getMessage("Język"));
		languageSelect.setWidth("80px");
		languageSelect.setImmediate(true);
		languageSelect.addListener(new Property.ValueChangeListener() {
			@Override
			public void valueChange(Property.ValueChangeEvent valueChangeEvent) {
				displaySelectedDictionary();
			}
		});

		addDictionaryButton = new Button(messages.getMessage("Nowy słownik"));
		addDictionaryButton.addListener(this);

		addLanguageButton = new Button(messages.getMessage("Nowy język"));
		addLanguageButton.addListener(this);

		VerticalLayout newContent = new VerticalLayout();
		newContent.setSpacing(true);

		dictionaryLayout = new Panel(messages.getMessage("Definicja"));
		dictionaryLayout.setContent(newContent);
		dictionaryLayout.setWidth("100%");

		dictionaryNameField = new TextField(messages.getMessage("Nazwa"));
		dictionaryNameField.setWidth("400px");
		dictionaryNameField.setNullRepresentation("");
		dictionaryNameField.setImmediate(true);

		dictionaryDescriptionField = new TextField(messages.getMessage("Opis"));
		dictionaryDescriptionField.setWidth("400px");
		dictionaryDescriptionField.setNullRepresentation("");
		dictionaryDescriptionField.setImmediate(true);

		editPermissionField = new TextField(messages.getMessage("Rola uprawniająca do edycji"));
		editPermissionField.setWidth("400px");
		editPermissionField.setNullRepresentation("");
		editPermissionField.setImmediate(true);

		defaultLanguageField = new Select(messages.getMessage("Język domyślny"));
		defaultLanguageField.setWidth("80px");
		defaultLanguageField.setImmediate(true);
		defaultLanguageField.setContainerDataSource(languageSelect.getContainerDataSource());
    } 
    
    private void initLayout() {
        setSpacing(true);

		HorizontalLayout hl = new HorizontalLayout();
		hl.setSpacing(true);

		for (Component component : new Component[] {
				dictionarySelect, languageSelect, addDictionaryButton, addLanguageButton
		}) {
			hl.addComponent(component);
			hl.setComponentAlignment(component, Alignment.BOTTOM_LEFT);
		}

		addComponent(hl);
//        addComponent(languageDescriptionLabel);
		addComponent(dictionaryLayout);
    }

    @Override
    public void loadData() {
    }

    @Override
    public void saveData() {
    }

    @Override
    public Collection<String> validateData() {
        return null;
    }

	public String getDictionary() {
		return formatProcessDictionaries(processDictionaries.getWrappedObject());
	}

	public void setDictionary(String dictionary) {
		ProcessDictionaries parsedProcessDictionaries = parseProcessDictionaries(dictionary);

		if (parsedProcessDictionaries == null) {
			parsedProcessDictionaries = new ProcessDictionaries();
		}

		processDictionaries = new XmlProcessDictionariesWrapper(parsedProcessDictionaries);

		languageSelect.removeAllItems();
		for (String langCode : processDictionaries.getLanguageCodes()) {
			languageSelect.addItem(langCode);
		}

		dictionarySelect.removeAllItems();
		for (String dictId : processDictionaries.getDictionaryIds()) {
			dictionarySelect.addItem(dictId);
		}

		bindProperty(defaultLanguageField, processDictionaries, XmlProcessDictionariesWrapper._DEFAULT_LANGUAGE);
	}

	private ProcessDictionaries parseProcessDictionaries(String dictionary) {
		if (dictionary != null) {
			try {
				Object obj = DictionaryLoader.getInstance().unmarshall(new ByteArrayInputStream(dictionary.getBytes("UTF-8")));
				return (ProcessDictionaries)obj;
			}
			catch (UnsupportedEncodingException e) {
				e.printStackTrace(); // TODO
			}
		}
		return null;
	}

	private String formatProcessDictionaries(ProcessDictionaries processDictionaries) {
		if (processDictionaries != null) {
			return DictionaryLoader.getInstance().marshall(processDictionaries);
		}
		return null;
	}

	private void displaySelectedDictionary() {
		String dictionaryId = getCurrentDictionaryId();
		String languageCode = getCurrentLanguageCode();

		dictionaryLayout.removeAllComponents();

		if (dictionaryId != null && languageCode != null) {
			HorizontalLayout hl = new HorizontalLayout();
			hl.setSpacing(true);
			hl.addComponent(dictionaryNameField);
			hl.addComponent(dictionaryDescriptionField);

			HorizontalLayout hl2 = new HorizontalLayout();
			hl2.setSpacing(true);
			hl2.addComponent(editPermissionField);
			hl2.addComponent(defaultLanguageField);

			container = new BeanItemContainer<XmlDictionaryItemWrapper>(XmlDictionaryItemWrapper.class, processDictionaries.getItems(dictionaryId, languageCode));

			bindProperty(dictionaryNameField, getCurrentDictionary(), _DICTIONARY_NAME);
			bindProperty(dictionaryDescriptionField, getCurrentDictionary(), _DESCRIPTION);
			bindProperty(editPermissionField, getCurrentDictionary(), _EDIT_PERMISSION);

			dictionaryLayout.addComponent(hl);
			dictionaryLayout.addComponent(hl2);
			dictionaryLayout.addComponent(getAddEntryButton());
			dictionaryLayout.addComponent(builder.createTable(container));
		}
	}

	private String getCurrentLanguageCode() {
		return (String)languageSelect.getValue();
	}

	private String getCurrentDictionaryId() {
		return (String)dictionarySelect.getValue();
	}

	private Component getAddEntryButton() {
		if (addEntryButton == null) {
			I18NSource i18NSource = I18NSource.ThreadUtil.getThreadI18nSource();

			addEntryButton = addIcon(getApplication());
			addEntryButton.setCaption(i18NSource.getMessage("dict.addentry"));
			addEntryButton.addListener(this);
		}
		return addEntryButton;
	}

	@Override
	public void handleItemSave(XmlDictionaryItemWrapper item) {
		// TODO
	}

	@Override
	public void handleItemDelete(XmlDictionaryItemWrapper item) {
		// TODO
	}

	@Override
	public void buttonClick(Button.ClickEvent clickEvent) {
		final I18NSource i18NSource = I18NSource.ThreadUtil.getThreadI18nSource();

		if (clickEvent.getButton() == addDictionaryButton) {
			InputDialog dialog = new InputDialog(i18NSource.getMessage("Dodawanie nowego słownika")) {
				@Override
				protected void handleAdd(String dictionaryId) {
					dictionarySelect.addItem(dictionaryId);
					dictionarySelect.setValue(dictionaryId);
				}
			};
			dialog.setInputCaption(i18NSource.getMessage("Id słownika"));
			dialog.show(getApplication());
		}
		else if (clickEvent.getButton() == addLanguageButton) {
			InputDialog dialog = new InputDialog(i18NSource.getMessage("Dodawanie nowego języka")) {
				@Override
				protected void handleAdd(String langCode) {
					languageSelect.addItem(langCode);
					languageSelect.setValue(langCode);
					if (languageSelect.getContainerDataSource().getItemIds().size() == 1
						&& defaultLanguageField.getValue() == null) {
						defaultLanguageField.setValue(from(languageSelect.getContainerDataSource()).first());
					}
				}
			};
			dialog.setInputCaption(i18NSource.getMessage("Kod języka"));
			dialog.show(getApplication());
		}
		else if (clickEvent.getButton() == addEntryButton) {
			XmlDictionaryItemWrapper item = new XmlDictionaryItemWrapper();

			builder.showItemDetails(new BeanItem<XmlDictionaryItemWrapper>(item), new DictionaryItemTableBuilder.SaveCallback<XmlDictionaryItemWrapper>() {
				@Override
				public void onSave(BeanItem<XmlDictionaryItemWrapper> item) {
					XmlDictionaryItemWrapper lookedUpItem = getCurrentDictionary().lookup(item.getBean().getKey());

					if (lookedUpItem != null) {
						validationNotification(getApplication(), i18NSource, i18NSource.getMessage("validate.dictentry.exists"));
					}
					else {
						getCurrentDictionary().addItem(item.getBean());
						container.addItem(item.getBean());
						container.sort(new Object[]{ XmlDictionaryItemWrapper._KEY }, new boolean[]{ true });
						builder.closeDetailsWindow();
					}
				}
			});
		}
	}

	private XmlDictionaryWrapper getCurrentDictionary() {
		return processDictionaries.getDictionary(getCurrentDictionaryId(), getCurrentLanguageCode());
	}

	private abstract class InputDialog extends Dialog {
		private TextField inputField;

		public InputDialog(String title) {
			super(title);

			I18NSource i18NSource = I18NSource.ThreadUtil.getThreadI18nSource();

			inputField = new TextField();
			inputField.setNullRepresentation("");
			inputField.setWidth("300px");
			addDialogContent(inputField);

			addDialogAction(i18NSource.getMessage("Dodaj"), new Dialog.ActionListener() {
				@Override
				public void handleAction(String action) {
					inputField.commit();
					handleAdd((String)inputField.getValue());
				}
			});
			addDialogAction(i18NSource.getMessage("Anuluj"), null);
		}

		public void setInputCaption(String caption) {
			inputField.setCaption(caption);
		}

		protected abstract void handleAdd(String value);
	}
}
