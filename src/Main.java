public class Main {
    public static void main(String[] args) {
        // Uruchomienie interfejsu
        javax.swing.SwingUtilities.invokeLater(() -> {
            interfejs app = new interfejs();
            app.setVisible(true);
        });
    }
}