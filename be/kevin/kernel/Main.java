package be.kevin.kernel;

import java.io.File;

public class Main {
	public static void main(String[] args) {
		new DoisJeVersioner(new File(Config.mainFolder));
	}
}
