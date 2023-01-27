package gov.nist.asbestos.client.channel;

public enum IgNameConstants {
    MHDV_3_X ( "MHDv3.x"),
    MHDV_4 ( "MHDv4"),
    MHDV_410 ( "MHDv410");

    private String igName;

    IgNameConstants(String igName) {
        this.igName = igName;
    }

    public String getIgName() {
        return igName;
    }

    @Override
    public String toString() {
        return igName;
    }

    public static IgNameConstants find(String value) {
        for (IgNameConstants igNameConstant : IgNameConstants.values()) {
            if (igNameConstant.igName.equals(value))
                return igNameConstant;
        }
        return null;
    }
}
