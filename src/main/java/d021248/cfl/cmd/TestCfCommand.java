package d021248.cfl.cmd;

public class TestCfCommand {

    public static void main(String[] args) throws InterruptedException {

        var env = Cf.getEnv("mkv-srv");
        System.out.println();
        System.out.println(env);
        System.out.println();

        var target = Cf.target();
        System.out.println();
        System.out.println(target);
        System.out.println();

        new Thread(() -> Cf.runAndLog("cf", "restage", "mkv-srv")).start();
        System.out.println();

        Cf.getApps().stream().forEach(a -> System.out.println(a.name + " : " + a.urls));
        System.out.println();

        Cf.logs();
        System.out.println();

        Thread.sleep(60_000);

        Command.stopAll();
        System.out.println("done.");
    }
}
