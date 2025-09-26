package com.project.cache.persistence.postgres;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/** JPA entity for table 'entities'. */
@Entity
@Table(name = "entities")
public class JpaEntityRecord {
    @Id
    private Long id;     // BIGINT
    private String data; // VARCHAR(255)

    public JpaEntityRecord(){}
    public JpaEntityRecord(Long id, String data){ this.id=id; this.data=data; }

    public Long getId(){ return id; }
    public void setId(Long id){ this.id=id; }
    public String getData(){ return data; }
    public void setData(String data){ this.data=data; }
}
