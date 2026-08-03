package com.chachamaru.harness.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Task model representing a single task in Plans.md.
 */
public class Task {
    private String id;
    private String content;
    private String dod;
    private String depends;
    private String status;
    private String title;
    private List<Subtask> subtasks = new ArrayList<>();

    /**
     * Subtask model for checkbox items.
     */
    public static class Subtask {
        private String text;
        private boolean completed;

        public Subtask(String text, boolean completed) {
            this.text = text;
            this.completed = completed;
        }

        public String getText() {
            return text;
        }

        public boolean isCompleted() {
            return completed;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Subtask subtask = (Subtask) o;
            return completed == subtask.completed && Objects.equals(text, subtask.text);
        }

        @Override
        public int hashCode() {
            return Objects.hash(text, completed);
        }
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getDod() {
        return dod;
    }

    public void setDod(String dod) {
        this.dod = dod;
    }

    public String getDepends() {
        return depends;
    }

    public void setDepends(String depends) {
        this.depends = depends;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<Subtask> getSubtasks() {
        return subtasks;
    }

    public void setSubtasks(List<Subtask> subtasks) {
        this.subtasks = subtasks;
    }

    public void addSubtask(String text, boolean completed) {
        this.subtasks.add(new Subtask(text, completed));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Task task = (Task) o;
        return Objects.equals(id, task.id) &&
                Objects.equals(content, task.content) &&
                Objects.equals(dod, task.dod) &&
                Objects.equals(depends, task.depends) &&
                Objects.equals(status, task.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, content, dod, depends, status);
    }

    @Override
    public String toString() {
        return "Task{" +
                "id='" + id + '\'' +
                ", content='" + content + '\'' +
                ", dod='" + dod + '\'' +
                ", depends='" + depends + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
