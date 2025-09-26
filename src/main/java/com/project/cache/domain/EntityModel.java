package com.project.cache.domain;

import javax.validation.constraints.NotBlank;

/** Simple DTO used by REST requests/responses. */
public class EntityModel {
    @NotBlank(message = "id must not be blank")
    private String id;
    private String data;

    public EntityModel() {}
    public EntityModel(String id, String data) { this.id = id; this.data = data; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getData() { return data; }
    public void setData(String data) { this.data = data; }

    @Override public String toString() { return "EntityModel{id='" + id + "', data='" + data + "'}"; }
}
