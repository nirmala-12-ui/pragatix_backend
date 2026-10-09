package jjcet.PragatiX.modules.admin.dto.report;

public class CapColumnDto {
    private int capNumber;
    private String label;

    public CapColumnDto() {
    }

    public CapColumnDto(int capNumber, String label) {
        this.capNumber = capNumber;
        this.label = label;
    }

    public int getCapNumber() {
        return capNumber;
    }

    public void setCapNumber(int capNumber) {
        this.capNumber = capNumber;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
