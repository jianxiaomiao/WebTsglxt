package com.example.entity;

public class DeptType {
    // 主键ID
    private Integer id;
    // 系别名称
    private String deptType;

    public DeptType() {
    }

    public DeptType(Integer id, String deptType) {
        this.id = id;
        this.deptType = deptType;
    }

    // Getter & Setter
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getDeptType() {
        return deptType;
    }

    public void setDeptType(String deptType) {
        this.deptType = deptType;
    }

    @Override
    public String toString() {
        return "DeptType{" +
                "id=" + id +
                ", deptType='" + deptType + '\'' +
                '}';
    }
}