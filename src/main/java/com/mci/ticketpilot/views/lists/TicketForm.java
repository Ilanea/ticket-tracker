package com.mci.ticketpilot.views.lists;

import com.mci.ticketpilot.data.entity.*;
import com.mci.ticketpilot.data.service.ApplicationContextProvider;
import com.mci.ticketpilot.data.service.PilotService;
import com.mci.ticketpilot.data.service.SendMailService;
import com.mci.ticketpilot.security.SecurityService;
import com.mci.ticketpilot.security.SecurityUtils;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.shared.Registration;

import java.io.IOException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.time.LocalDate;
import java.util.List;

import org.apache.commons.mail.EmailException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TicketForm extends VerticalLayout {

    private static final Logger logger = LoggerFactory.getLogger(SecurityService.class);
    private PilotService service;
    private Project selectedProject;
    private Users selectedUser;



    private TextField ticketName = new TextField("Title");
    private DatePicker dueDate = new DatePicker("Due date");
    private ComboBox<TicketPriority> ticketPriority = new ComboBox<>("Priority");
    private ComboBox<TicketStatus> ticketStatus = new ComboBox<>("Status");
    private ComboBox<Project> linkedProject = new ComboBox<>("Project");
    private ComboBox<Users> linkedUser = new ComboBox<>("Assigned User");
    private TextArea ticketDescription = new TextArea("Description");
    private Component buttonContainer;
    private Button save = new Button("Save");
    private Button delete = new Button("Delete");
    private Button close = new Button("Cancel");
    private List<Ticket> tickets = new ArrayList<>();
    private Registration saveListener;
    private TextArea asigneeField = new TextArea("Assignee");
    private TextArea authorField = new TextArea("Author");

    Binder<Ticket> binder = new BeanValidationBinder<>(Ticket.class);

    public TicketForm(List<Ticket> tickets, PilotService service) {
        this.service = service;
        this.tickets = tickets;

        // Bind Form Fields to Object
        addClassName("ticket-form");
        binder.forField(linkedProject).bind(Ticket::getProject, Ticket::setProject);
        binder.forField(linkedUser).bind(Ticket::getAssignee, Ticket::setAssignee);
        binder.bindInstanceFields(this);

        // Priority
        ticketPriority.setItems(TicketPriority.values());
        ticketStatus.setItems(TicketStatus.values());

        // Linkedproject & Linkeduser
        List<Project> projects = service.findAllProjects();
        List<Users> users = service.findAllUsers();

        linkedProject.setClearButtonVisible(true);
        linkedProject.setRequiredIndicatorVisible(true);
        linkedProject.setPrefixComponent(VaadinIcon.SEARCH.create());
        linkedUser.setClearButtonVisible(true);
        linkedUser.setReadOnly(true);
        linkedUser.setPrefixComponent(VaadinIcon.SEARCH.create());


        linkedProject.setItems(projects);
        linkedUser.setItems(users);

        linkedProject.setItemLabelGenerator(Project::getProjectName);
        linkedUser.setItemLabelGenerator(user -> user.getFirstName() + " " + user.getLastName());

        linkedProject.addValueChangeListener(event -> {
            selectedProject = event.getValue();
        });

        linkedUser.addValueChangeListener(event -> {
            selectedUser = event.getValue();
        });

        // Ticket description
        ticketDescription.setMaxLength(300);
        ticketDescription.setValueChangeMode(ValueChangeMode.EAGER);
        ticketDescription.addValueChangeListener(e -> {
            e.getSource()
                    .setHelperText(e.getValue().length() + "/" + 300);
        });

        LocalDate now = LocalDate.now(ZoneId.systemDefault());
        dueDate.setMin(now);

        asigneeField.setReadOnly(true);
        authorField.setReadOnly(true);


        FormLayout formLayout = new FormLayout();
        formLayout.add(ticketName, ticketStatus, ticketPriority, dueDate, ticketDescription, linkedProject, linkedUser,authorField , asigneeField);
        formLayout.setColspan(ticketName, 2);
        formLayout.setColspan(ticketDescription, 2);

        buttonContainer = createButtonsLayout();
        setHorizontalComponentAlignment(Alignment.CENTER, buttonContainer);

        add(formLayout, buttonContainer);

    }

    private Component createButtonsLayout() {
        save.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
        close.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        //save.addClickShortcut(Key.ENTER);
        close.addClickShortcut(Key.ESCAPE);

        save.addClickListener(event -> {
            try {
                validateAndSave();
            } catch (EmailException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        delete.addClickListener(event -> fireEvent(new TicketForm.DeleteEvent(this, binder.getBean())));
        close.addClickListener(event -> fireEvent(new TicketForm.CloseEvent(this)));


        binder.addStatusChangeListener(e -> save.setEnabled(binder.isValid()));

        Div buttonContainer = new Div(save, delete, close);

        save.getStyle().set("width", "150px").set("margin-right", "20px");
        delete.getStyle().set("width", "150px").set("margin-right", "20px");
        close.getStyle().set("width", "150px");

        return buttonContainer;
    }

    public void validateAndSave() throws EmailException, IOException {
        if (binder.isValid()) {
            Ticket ticket = binder.getBean();
            if(ticket.getTicketCreationDate() == null){
                ticket.setTicketCreationDate(LocalDate.now());
                ticket.setTicketLastUpdateDate(LocalDate.now());
                ticket.setAuthor(SecurityUtils.getLoggedInUser());
            } else {
                ticket.setTicketLastUpdateDate(LocalDate.now());
            }
            // Saving a ticket without a project gets a NullPointerException
            // This hack is to circumvent this issue
            if (ticket.getProject() == null) {
                Notification.show("Please select a project", 2000, Notification.Position.MIDDLE);
            } else {
                // If the ticket is new, set the current user as the creator
                if(ticket.getAssignee() == null){
                    fireEvent(new TicketForm.SaveEvent(this, ticket, tickets));
                } else {
                    SendMailService sendMail = ApplicationContextProvider.getApplicationContext().getBean(SendMailService.class);
                    try {
                        sendMail.send(ticket.getAssignee().getEmail(), ticket.getAssignee().getFirstName(), ticket.getAssignee().getLastName(), ticket.getTicketName(), ticket.getTicketDescription());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    fireEvent(new TicketForm.SaveEvent(this, ticket, tickets));
                }
            }
        }
    }

    public void setTicket(Ticket ticket, boolean isNew) {
        if(ticket != null){
            binder.setBean(ticket);

            if (!isNew) {
              
                asigneeField.setValue(ticket.getAssignee() != null ? "\uD83D\uDC64 " +  ticket.getAssignee().getFirstName() + " " + ticket.getAssignee().getLastName()+ "\n✉ " +  ticket.getAssignee().getEmail() + "\n\uD83D\uDD54 " + ticket.getTicketLastUpdateDate() +" (assigned)": "N/A");
                authorField.setValue(ticket.getAuthor() != null ?  "\uD83D\uDC64 " + ticket.getAuthor().getFirstName()+ " " + ticket.getAuthor().getLastName() + "\n✉ " + ticket.getAuthor().getEmail() + "\n\uD83D\uDD54 " + ticket.getTicketCreationDate() + " (created)": "N/A");
              
                if(selectedUser != null){
                    linkedUser.setTooltipText(selectedUser.getEmail());
                }
                if(selectedProject != null){
                    linkedProject.setTooltipText("Project Manager: " + selectedProject.getManager().toString() + "\n" + selectedProject.getManager().getEmail());
                }
                // Ticket Information can only be changed by Admins, Managers,the current assignee or the project manager of the project the ticket is assigned to
                if (!SecurityUtils.userHasAdminRole() && !SecurityUtils.userHasManagerRole() && !service.isCurrentUserAssignee(ticket) && !service.isCurrentUserManager(ticket.getProject())) {
                    logger.info("User is not allowed to edit this ticket");
                    buttonContainer.setVisible(false);
                    ticketName.setReadOnly(true);
                    ticketDescription.setReadOnly(true);
                    ticketPriority.setReadOnly(true);
                    ticketStatus.setReadOnly(true);
                    linkedProject.setReadOnly(true);
                    linkedUser.setReadOnly(true);
                    dueDate.setReadOnly(true);
                } else {
                    logger.info("User allowed to edit this ticket");
                    buttonContainer.setVisible(true);
                    ticketName.setReadOnly(false);
                    ticketDescription.setReadOnly(false);
                    ticketPriority.setReadOnly(false);
                    ticketStatus.setReadOnly(false);
                    linkedProject.setReadOnly(false);
                    dueDate.setReadOnly(false);
                    linkedUser.setReadOnly(false);
                }
            } else {
                logger.info("User allowed to edit this ticket");
                buttonContainer.setVisible(true);
                ticketName.setReadOnly(false);
                ticketDescription.setReadOnly(false);
                ticketPriority.setReadOnly(false);
                ticketStatus.setReadOnly(false);
                linkedProject.setReadOnly(false);
                dueDate.setReadOnly(false);
                if(!SecurityUtils.userHasAdminRole() && !SecurityUtils.userHasManagerRole()){
                    linkedUser.setReadOnly(true);
                } else {
                    linkedUser.setReadOnly(false);
                }
            }
            logger.info("Set selected Ticket to: " + ticket);
        }
    }

    // Events
    public static abstract class TicketFormEvent extends ComponentEvent<TicketForm> {
        private Ticket ticket;

        protected TicketFormEvent(TicketForm source, Ticket ticket) {
            super(source, false);
            this.ticket = ticket;
        }

        public Ticket getTicket() {
            return ticket;
        }
    }

    public List<Ticket> getTickets() {
        return tickets;
    }

    public void setSaveListener(Registration saveListener) {
        if (this.saveListener != null) {
            this.saveListener.remove();
        }
        this.saveListener = saveListener;
    }

    public static class SaveEvent extends TicketForm.TicketFormEvent {
        SaveEvent(TicketForm source, Ticket ticket, List<Ticket> tickets) {
            super(source, ticket);
        }
    }

    public static class DeleteEvent extends TicketForm.TicketFormEvent {
        DeleteEvent(TicketForm source, Ticket ticket) {
            super(source, ticket);
        }

    }

    public static class CloseEvent extends TicketForm.TicketFormEvent {
        CloseEvent(TicketForm source) {
            super(source, null);
        }
    }

    public Registration addDeleteListener(ComponentEventListener<TicketForm.DeleteEvent> listener) {
        return addListener(TicketForm.DeleteEvent.class, listener);
    }

    public Registration addSaveListener(ComponentEventListener<TicketForm.SaveEvent> listener) {
        return addListener(TicketForm.SaveEvent.class, listener);
    }
    public Registration addCloseListener(ComponentEventListener<TicketForm.CloseEvent> listener) {
        return addListener(TicketForm.CloseEvent.class, listener);
    }
}

