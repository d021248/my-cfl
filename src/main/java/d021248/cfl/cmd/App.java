package d021248.cfl.cmd;

class App {

	public final String name;
	public final String state;
	public final String instances;
	public final String memory;
	public final String disk;
	public final String urls;

	public App(String name, String state, String instances, String memory, String disk, String urls) {
		this.name = name;
		this.state = state;
		this.instances = instances;
		this.memory = memory;
		this.disk = disk;
		this.urls = urls;
	}
}
