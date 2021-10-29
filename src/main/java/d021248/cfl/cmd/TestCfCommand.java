package d021248.cfl.cmd;

public class TestCfCommand {

    public static void main(String[] args) throws InterruptedException {
        Cf.logs();
        System.out.println("----------------------------------------------------------------------------------------");

        var env = Cf.getEnv("mkv-srv");
        System.out.println("----------------------------------------------------------------------------------------");
        System.out.println(env);
        System.out.println("----------------------------------------------------------------------------------------");

        //System.exit(0);

        var target = Cf.target();
        System.out.println("----------------------------------------------------------------------------------------");
        System.out.println(target);
        System.out.println("----------------------------------------------------------------------------------------");

        new Thread(() -> Cf.run("cf", "restage", "mkv-srv")).start();
        System.out.println("----------------------------------------------------------------------------------------");

        Cf.getApps().stream().forEach(a -> System.out.println(a.name + " : " + a.urls));
        System.out.println("----------------------------------------------------------------------------------------");

        Cf.logs();
        System.out.println("----------------------------------------------------------------------------------------");

        Thread.sleep(20_000);
        Command.stopAll();
        System.out.println("========================================================================================");
        System.out.println("done.");
        System.out.println("========================================================================================");
    }
}
