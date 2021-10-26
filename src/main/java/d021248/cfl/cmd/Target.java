package d021248.cfl.cmd;

public class Target {

    public final String endpoint;
    public final String version;
    public final String user;
    public final String org;
    public final String space;

    public Target(String endpoint, String version, String user, String org, String space) {
        this.endpoint = endpoint;
        this.version = version;
        this.user = user;
        this.org = org;
        this.space = space;
    }
}
