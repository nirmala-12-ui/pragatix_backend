package jjcet.PragatiX.modules.attendance.dto.response;

public class FacultyLookupResponse {
    private Long id;
    private Long userId;
    private String name;
    private String username;
    private String departmentName;
    private Long departmentId;
    private String designation;
    private Boolean isClassCoordinator;
    private Boolean isMarkingFaculty;
    private String role;

    public FacultyLookupResponse() {
    }

    public FacultyLookupResponse(Long id, String name, String username, String departmentName, Long departmentId, String designation) {
        this(id, null, name, username, departmentName, departmentId, designation, false, false, null);
    }

    public FacultyLookupResponse(Long id, Long userId, String name, String username, String departmentName, Long departmentId, String designation, Boolean isClassCoordinator, Boolean isMarkingFaculty, String role) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.username = username;
        this.departmentName = departmentName;
        this.departmentId = departmentId;
        this.designation = designation;
        this.isClassCoordinator = isClassCoordinator != null ? isClassCoordinator : false;
        this.isMarkingFaculty = isMarkingFaculty != null ? isMarkingFaculty : false;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public Boolean getIsClassCoordinator() {
        return isClassCoordinator;
    }

    public void setIsClassCoordinator(Boolean isClassCoordinator) {
        this.isClassCoordinator = isClassCoordinator;
    }

    public Boolean getIsMarkingFaculty() {
        return isMarkingFaculty;
    }

    public void setIsMarkingFaculty(Boolean isMarkingFaculty) {
        this.isMarkingFaculty = isMarkingFaculty;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
