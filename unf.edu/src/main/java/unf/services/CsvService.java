package unf.services;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Future;

public class CsvService extends BaseService {
    private static final String TRAINING_SET_CSV_FILE = "./csv/training_set.csv";
    private static final String TEST_SET_CSV_FILE = "./csv/test_set.csv";
    private static final String TOTAL_CSV_FILE = "./csv/total_set.csv";
    private static final String SINGLE_DRG_CSV_FILE = "./csv/by-drg/drg_{0}_set.csv";
    private static final String SINGLE_REGION_CSV_FILE = "./csv/by-region/region_{0}_set.csv";

    private File file;
    private File regionMajorOrderFile;

    public CsvService() {
        var classLoader = getClass().getClassLoader();
        this.file = new File(Objects.requireNonNull(classLoader.getResource("CMS_DRG.csv")).getFile());
        this.regionMajorOrderFile = new File(Objects.requireNonNull(classLoader.getResource("CMS_DRG_SORTED_BY_REGION.csv")).getFile());
    }

    public void preProcessDrgData() throws InterruptedException {

        Future<Integer> future1 = processDRGMajorOrder();
        Future<Integer> future2 = processRegionMajorOrder();

        var hrm = 0;
        while (!future1.isDone() || !future2.isDone()) {
            System.out.print("\rCalculating..." + (hrm++ % 2 == 0 ? "\\" : "/"));
            Thread.sleep(200);
        }
        System.out.print("\nDone!");

    }

    private Future<Integer> processRegionMajorOrder() {
        return executor.submit(() -> {

            Map<String, List<CSVRecord>> dataFrames = new HashMap<>();

            try {

                var format = CSVFormat.RFC4180.withHeader().withDelimiter(',');
                var parser = new CSVParser(new FileReader(this.regionMajorOrderFile.getAbsoluteFile()), format);

                for (var csvRecord : parser) {
                    var providerRegionDescription = "RID" + csvRecord.get(7).replaceAll(" ", "").replaceAll("-", "");
                    if (!dataFrames.containsKey(providerRegionDescription)) {
                        dataFrames.put(providerRegionDescription, new ArrayList<>());
                    }

                    dataFrames.get(providerRegionDescription).add(csvRecord);
                }

                for (var r : dataFrames.keySet()) {
                    var records = dataFrames.get(r);
                    var helper = "RID" + records.get(0).get(7).replaceAll(" ", "").replaceAll("-", "").replace("/", "");
                    try (var drgSetWriter = Files.newBufferedWriter(Paths.get(SINGLE_REGION_CSV_FILE.replace("{0}", helper)));

                         var csvPrinter = new CSVPrinter(drgSetWriter, CSVFormat.DEFAULT
                                 .withHeader("drg", "providerId", "providerRegionDescription", "totalDischarges", "averageCoveredPayments", "averageTotalPayments", "averageMedicarePayments"))
                    ) {
                        processRecords(records, csvPrinter);
                    }
                }
            } catch (Exception e1) {
                e1.printStackTrace();
            }

            return 0;
        });
    }

    private void processRecords(List<CSVRecord> records, CSVPrinter csvPrinter) throws IOException {
        for (var drgGroupRow : records) {
            var drgDescription = drgGroupRow.get(0);
            var labelOne = "DRG" + drgDescription.split(" ")[0];

            var providerId = "PID" + drgGroupRow.get(1);
            var providerRegionDescription = "RID" + drgGroupRow.get(7).replaceAll(" ", "").replaceAll("-", "");
            var totalDischarges = drgGroupRow.get(8);
            var averageCoveredPayments = drgGroupRow.get(9).replaceAll(",", "");
            var averageTotalPayments = drgGroupRow.get(10);
            var averageMedicarePayments = drgGroupRow.get(11);

            csvPrinter.printRecord(labelOne, providerId, providerRegionDescription, totalDischarges, averageCoveredPayments.replaceAll(",", ""), averageTotalPayments.replaceAll(",", ""), averageMedicarePayments.replaceAll(",", ""));
        }
    }

    private Future<Integer> processDRGMajorOrder() {
        return executor.submit(() -> {

            Map<String, Integer> map = new HashMap<>();
            Map<String, List<CSVRecord>> dataFrames = new HashMap<>();

            try {

                var format = CSVFormat.RFC4180.withHeader().withDelimiter(',');
                var parser = new CSVParser(new FileReader(this.file.getAbsoluteFile()), format);
                try (
                        var writer = Files.newBufferedWriter(Paths.get(TOTAL_CSV_FILE));
                        var trainingSetWriter = Files.newBufferedWriter(Paths.get(TRAINING_SET_CSV_FILE));
                        var testSetWriter = Files.newBufferedWriter(Paths.get(TEST_SET_CSV_FILE));

                        var csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT
                                .withHeader("drg", "providerId", "providerRegionDescription", "totalDischarges", "averageCoveredPayments", "averageTotalPayments", "averageMedicarePayments"));
                        var csvTrainingSetPrinter = new CSVPrinter(trainingSetWriter, CSVFormat.DEFAULT
                                .withHeader("drg", "providerId", "providerRegionDescription", "totalDischarges", "averageCoveredPayments", "averageTotalPayments", "averageMedicarePayments"));
                        var csvTestSetPrinter = new CSVPrinter(testSetWriter, CSVFormat.DEFAULT
                                .withHeader("drg", "providerId", "providerRegionDescription", "totalDischarges", "averageCoveredPayments", "averageTotalPayments", "averageMedicarePayments"))
                ) {
                    for (var csvRecord : parser) {
                        var drgDescription = csvRecord.get(0);
                        var labelOne = "DRG" + drgDescription.split(" ")[0];
                        if (!dataFrames.containsKey(labelOne)) {
                            dataFrames.put(labelOne, new ArrayList<>());
                        }
                        if (!map.containsKey(labelOne)) {
                            map.put(labelOne, 0);
                        }
                        map.replace(labelOne, map.get(labelOne) + 1);
                        dataFrames.get(labelOne).add(csvRecord);

                        var providerId = "PID" + csvRecord.get(1);
                        var providerRegionDescription = "RID" + csvRecord.get(7).replaceAll(" ", "").replaceAll("-", "").replace("/", "");
                        var totalDischarges = csvRecord.get(8);
                        var averageCoveredPayments = csvRecord.get(9).replaceAll(",", "");
                        var averageTotalPayments = csvRecord.get(10);
                        var averageMedicarePayments = csvRecord.get(11);


                        if (map.get(labelOne) < 11) {
                            csvTrainingSetPrinter.printRecord(labelOne, providerId, providerRegionDescription, totalDischarges, averageCoveredPayments.replaceAll(",", ""), averageTotalPayments.replaceAll(",", ""), averageMedicarePayments.replaceAll(",", ""));
                        }

                        if (map.get(labelOne) > 11 && map.get(labelOne) < 101) {
                            csvTestSetPrinter.printRecord(labelOne, providerId, providerRegionDescription, totalDischarges, averageCoveredPayments.replaceAll(",", ""), averageTotalPayments.replaceAll(",", ""), averageMedicarePayments.replaceAll(",", ""));
                        }

                        csvPrinter.printRecord(labelOne, providerId, providerRegionDescription, totalDischarges, averageCoveredPayments.replaceAll(",", ""), averageTotalPayments.replaceAll(",", ""), averageMedicarePayments.replaceAll(",", ""));

                    }
                    csvPrinter.flush();
                    csvTestSetPrinter.flush();
                    csvTrainingSetPrinter.flush();
                }

                for (var r : dataFrames.keySet()) {
                    var records = dataFrames.get(r);
                    var helper = "DRG" + records.get(0).get(0).split(" ")[0];
                    try (var drgSetWriter = Files.newBufferedWriter(Paths.get(SINGLE_DRG_CSV_FILE.replace("{0}", helper)));

                         var csvPrinter = new CSVPrinter(drgSetWriter, CSVFormat.DEFAULT
                                 .withHeader("drg", "providerId", "providerRegionDescription", "totalDischarges", "averageCoveredPayments", "averageTotalPayments", "averageMedicarePayments"))
                    ) {
                        processRecords(records, csvPrinter);
                    }
                }
            } catch (Exception e1) {
                e1.printStackTrace();
            }

            return 0;
        });
    }

}
