package com.mci.ticketpilot.data.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Entity
public class Ticket extends AbstractEntity {

    @NotEmpty
    private String ticketName;

    @Size(max = 300)
    private String ticketDescription;

    @Column(nullable = true, columnDefinition = "DATE default CURRENT_DATE")
    private LocalDate ticketCreationDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(255) default 'OPEN'")
    private TicketStatus ticketStatus = TicketStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(255) default 'DEFAULT'")
    private TicketPriority ticketPriority = TicketPriority.DEFAULT;

    // Each Ticket can be assigned to one Project
    @ManyToOne(optional = true, fetch = FetchType.EAGER, cascade = CascadeType.MERGE)
    @JoinColumn(name="project_id", referencedColumnName = "id", nullable=true, insertable=true, updatable=true)
    private Project ticketProject;

    // Each Ticket can be assigned to one User
    @ManyToOne(optional = true, fetch = FetchType.EAGER, cascade = CascadeType.MERGE)
    @JoinColumn(name="user_id", referencedColumnName = "id", nullable=true, insertable=true, updatable=true)
    private Users ticketUser;

    // One ticket can have multiple comments
    @OneToMany(mappedBy = "ticket", fetch = FetchType.EAGER, cascade = CascadeType.MERGE)
    private List<Comment> comments = new ArrayList<>();

    @ManyToOne(optional = true, fetch = FetchType.EAGER, cascade = CascadeType.MERGE)
    private Users assignee; // 'assignee' field of type 'Users'

    public LocalDate getTicketCreationDate() {
        // or any other default value
        return Objects.requireNonNullElseGet(ticketCreationDate, LocalDate::now);
    }

    public void setTicketCreationDate(LocalDate ticketCreationDate) {
        this.ticketCreationDate = ticketCreationDate;
    }

    public Project getTicketProject() {
        return ticketProject;
    }

    public void setTicketProject(Project ticketProject) {
        this.ticketProject = ticketProject;
    }

    public Users getTicketUser() {
        return ticketUser;
    }

    public void setTicketUser(Users ticketUser) {
        this.ticketUser = ticketUser;
    }

    public Users getAssignee() {
        return assignee;
    }

    public void setAssignee(Users assignee) {
        this.assignee = assignee;
    }

    // Getter and Setter methods
    public String getTicketName() {
        return ticketName;
    }

    public void setTicketName(String ticketName) {
        this.ticketName = ticketName;
    }

    public String getTicketDescription() {
        return ticketDescription;
    }

    public void setTicketDescription(String ticketDescription) {
        this.ticketDescription = ticketDescription;
    }

    public TicketStatus getTicketStatus() {
        return ticketStatus;
    }

    public void setTicketStatus(TicketStatus ticketStatus) {
        this.ticketStatus = ticketStatus;
    }

    public TicketPriority getTicketPriority() {
        return ticketPriority;
    }

    public void setTicketPriority(TicketPriority ticketPriority) {
        this.ticketPriority = ticketPriority;
    }

    public Project getProject() {
        return ticketProject;
    }

    public void setProject(Project project) {
        this.ticketProject = project;
    }

    public Users getUser() {
        return ticketUser;
    }

    public void setUser(Users user) {
        this.ticketUser = user;
    }

    public List<Comment> getComments() {
        return comments;
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments;
    }
}

