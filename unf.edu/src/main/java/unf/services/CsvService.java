package unf.services;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
import java.sql.SQLException;
import java.util.Arrays;
import java.util.concurrent.Future;

public class CsvService extends BaseService {
    private static final String SAMPLE_CSV_FILE = "./ready_to_test.csv";
    private File file;

    public CsvService() {
        ClassLoader classLoader = getClass().getClassLoader();
        this.file = new File(classLoader.getResource("CMS_DRG.csv").getFile());

    }


    @SuppressWarnings("unchecked")
    public void preProcessDrgData() throws InterruptedException {
        final long startTime = System.currentTimeMillis();

        Future<Integer> future = executor.submit(() -> {
            try {

                CSVFormat format = CSVFormat.RFC4180.withHeader().withDelimiter(',');
                CSVParser parser = new CSVParser(new FileReader(this.file.getAbsoluteFile()), format);
                try (
                        BufferedWriter writer = Files.newBufferedWriter(Paths.get(SAMPLE_CSV_FILE));

                        CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT
                                .withHeader("class", "providerId", "providerRegionDescription", "totalDischarges", "averageCoveredPayments", "averageTotalPayments", "averageMedicarePayments"));
                ) {
                    for (CSVRecord csvRecord : parser) {
                        String drgDescription = csvRecord.get(0);
                        String labelOne = drgDescription.split(" ")[0];

                        String providerId = csvRecord.get(1);
                        String providerRegionDescription = csvRecord.get(7).replaceAll(" ", "").replaceAll("-", "");
                        String totalDischarges = csvRecord.get(8);
                        String averageCoveredPayments = csvRecord.get(9);
                        String averageTotalPayments = csvRecord.get(10);
                        String averageMedicarePayments = csvRecord.get(11);

                        //Average Covered Charges , Average Total Payments ,Average Medicare Payments
                        // labels
//                        System.out.println(labelOne);
//                        System.out.println(providerId);
//                        System.out.println(providerRegionDescription);
//                        System.out.println(totalDischarges);
//                        System.out.println(averageCoveredPayments);
//                        System.out.println(averageTotalPayments);
//                        System.out.println(averageMedicarePayments);


                        csvPrinter.printRecord(labelOne, providerId, providerRegionDescription, totalDischarges, averageCoveredPayments, averageTotalPayments, averageMedicarePayments);


                    }
                    csvPrinter.flush();
                }

            } catch (IOException e1) {
                e1.printStackTrace();
            }

            return 0;
        });

        int hrm = 0;
        while (!future.isDone()) {
            System.out.print("\rCalculating..." + (hrm++ % 2 == 0 ? "\\" : "/"));
            Thread.sleep(200);
        }


    }

}
