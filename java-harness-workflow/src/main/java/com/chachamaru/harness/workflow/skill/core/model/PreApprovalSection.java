package com.chachamaru.harness.workflow.skill.core.model;

import java.util.List;

/**
 * 交付前确认章节
 */
public class PreApprovalSection {
    private final List<ApprovalItem> items;

    private PreApprovalSection(Builder builder) {
        this.items = builder.items != null ? List.copyOf(builder.items) : List.of();
    }

    public List<ApprovalItem> getItems() {
        return items;
    }

    public int getItemCount() {
        return items.size();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private List<ApprovalItem> items;

        public Builder items(List<ApprovalItem> items) {
            this.items = items;
            return this;
        }

        public PreApprovalSection build() {
            return new PreApprovalSection(this);
        }
    }

    public static class ApprovalItem {
        private final String description;
        private final boolean checked;

        public ApprovalItem(String description, boolean checked) {
            this.description = description;
            this.checked = checked;
        }

        public String getDescription() {
            return description;
        }

        public boolean isChecked() {
            return checked;
        }
    }
}