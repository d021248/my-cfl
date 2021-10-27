package d021248.cfl.cmd;

import java.io.IOException;

public class TestCfCommand {

    public static void main(String[] args) throws InterruptedException, IOException {
        Cf.cmd("cf restage mkv-srv").start();

        var apps = Cf.apps();
        apps.stream().forEach(a -> System.out.println(a.name + " : " + a.urls));

        Cf.logs();
        Thread.sleep(5_000);

        var env = Cf.env("mkv-srv");
        System.out.println(env);

        Thread.sleep(60_000);

        var target = Cf.target();
        System.out.println(target.endpoint);

        Command.stopAll();
        System.out.println("done.");
    }
}
