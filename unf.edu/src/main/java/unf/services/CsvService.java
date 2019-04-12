package unf.services;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

public class CsvService extends BaseService {
    private static final String TRAINING_SET_CSV_FILE = "./training_set.csv";
    private static final String TEST_SET_CSV_FILE = "./test_set.csv";
    private static final String TOTAL_CSV_FILE = "./total_set.csv";

    private File file;

    public CsvService() {
        ClassLoader classLoader = getClass().getClassLoader();
        this.file = new File(classLoader.getResource("CMS_DRG.csv").getFile());

    }


    @SuppressWarnings("unchecked")
    public void preProcessDrgData() throws InterruptedException {
        final long startTime = System.currentTimeMillis();

        Map<String, Integer> map = new HashMap<>();

        Future<Integer> future = executor.submit(() -> {
            try {

                CSVFormat format = CSVFormat.RFC4180.withHeader().withDelimiter(',');
                CSVParser parser = new CSVParser(new FileReader(this.file.getAbsoluteFile()), format);
                try (
                        BufferedWriter writer = Files.newBufferedWriter(Paths.get(TOTAL_CSV_FILE));
                        BufferedWriter trainingSetWriter = Files.newBufferedWriter(Paths.get(TRAINING_SET_CSV_FILE));
                        BufferedWriter testSetWriter = Files.newBufferedWriter(Paths.get(TEST_SET_CSV_FILE));

                        CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT
                                .withHeader("class", "providerId", "providerRegionDescription", "totalDischarges", "averageCoveredPayments", "averageTotalPayments", "averageMedicarePayments"));
                        CSVPrinter csvTrainingSetPrinter = new CSVPrinter(trainingSetWriter, CSVFormat.DEFAULT
                                .withHeader("class", "providerId", "providerRegionDescription", "totalDischarges", "averageCoveredPayments", "averageTotalPayments", "averageMedicarePayments"));
                        CSVPrinter csvTestSetPrinter = new CSVPrinter(testSetWriter, CSVFormat.DEFAULT
                                .withHeader("class", "providerId", "providerRegionDescription", "totalDischarges", "averageCoveredPayments", "averageTotalPayments", "averageMedicarePayments"));
                ) {
                    for (CSVRecord csvRecord : parser) {
                        String drgDescription = csvRecord.get(0);
                        String labelOne = "DRG" + drgDescription.split(" ")[0];

                        if (!map.containsKey(labelOne)) {
                            map.put(labelOne, 0);
                        }
                        map.replace(labelOne, map.get(labelOne) + 1);

                        String providerId = "PID" + csvRecord.get(1);
                        String providerRegionDescription = "RID" + csvRecord.get(7).replaceAll(" ", "").replaceAll("-", "");
                        String totalDischarges = csvRecord.get(8);
                        String averageCoveredPayments = csvRecord.get(9).replaceAll(",", "");
                        String averageTotalPayments = csvRecord.get(10);
                        String averageMedicarePayments = csvRecord.get(11);

                        if (map.get(labelOne) < 11) {
                            csvTrainingSetPrinter.printRecord(labelOne, providerId, providerRegionDescription, totalDischarges, averageCoveredPayments.replaceAll(",", ""), averageTotalPayments.replaceAll(",", ""), averageMedicarePayments.replaceAll(",", ""));
                        }

                        if (map.get(labelOne)  > 11 && map.get(labelOne) < 101) {
                            csvTestSetPrinter.printRecord(labelOne, providerId, providerRegionDescription, totalDischarges, averageCoveredPayments.replaceAll(",", ""), averageTotalPayments.replaceAll(",", ""), averageMedicarePayments.replaceAll(",", ""));
                        }

                        csvPrinter.printRecord(labelOne, providerId, providerRegionDescription, totalDischarges, averageCoveredPayments.replaceAll(",", ""), averageTotalPayments.replaceAll(",", ""), averageMedicarePayments.replaceAll(",", ""));

                    }
                    csvPrinter.flush();
                }

            } catch (Exception e1) {
                e1.printStackTrace();
            }

            return 0;
        });

        int hrm = 0;
        while (!future.isDone()) {
            System.out.print("\rCalculating..." + (hrm++ % 2 == 0 ? "\\" : "/"));
            Thread.sleep(200);
        }
        System.out.print("\nDone!");

    }

}
