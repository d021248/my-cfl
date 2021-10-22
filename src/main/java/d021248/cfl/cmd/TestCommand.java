package d021248.cfl.cmd;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class TestCommand {

	public static void main(String[] args) throws IOException, InterruptedException {

		System.out.println();
		Command c = new Command("cf", "env", "mkv-srv");
		c.start(TestCommand::handlerOut);
		System.out.println();

		System.out.println();
		Command d = new Command("cmd");
		d.start(TestCommand::handlerIn, TestCommand::handlerOut, TestCommand::handlerErr);
		System.out.println();

		System.out.println();
		Command e = new Command("notepad");
		e.start();
		System.out.println();

		Thread.sleep(10_000);
		Command.stopAll();
	}

	private static void handlerOut(OutputStream os) {
		System.out.println("starting handlerOut()");

		int c;
		try {
			while ((c = System.in.read()) > -1) {
				os.write(c);
				os.flush();
			}
		} catch (IOException e) {
		}
	}

	private static void handlerErr(InputStream is) {
		System.out.println("starting handlerErr()");

		String line;
		try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
			while ((line = br.readLine()) != null) {
				System.err.println("err>>" + line);
			}
		} catch (IOException e) {
		}
	}

	private static void handlerIn(InputStream is) {
		System.out.println("starting handlerIn()");

		String line;
		try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
			while ((line = br.readLine()) != null) {
				System.out.println("out>>" + line);
			}
		} catch (IOException e) {
		}
	}

}
