package unf;

import unf.services.CsvService;

public class App {
    public static void main(String[] args) {
        try {
            CsvService csvService = new CsvService();

            csvService.preProcessDrgData();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
