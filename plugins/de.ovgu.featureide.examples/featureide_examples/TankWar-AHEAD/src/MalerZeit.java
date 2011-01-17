

public class MalerZeit extends Thread {

	GameManager gameManager;

	public MalerZeit(GameManager gameManager) {
		this.gameManager = gameManager;
	}

	public void run() {

		while (true) {
			gameManager.aktualisieren();

			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}