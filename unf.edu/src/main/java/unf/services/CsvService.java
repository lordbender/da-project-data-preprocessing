package unf.services;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.Future;

public class CsvService extends BaseService {
    private static final Gson gson = new GsonBuilder().disableHtmlEscaping().create();
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

                for (CSVRecord csvRecord : parser) {
                    String drgDescription = csvRecord.get(0);
                    String labelOne = drgDescription.split(" ")[0];

                    String providerId = csvRecord.get(1);
                    String providerRegionDescription = csvRecord.get(7).replaceAll(" ", "").replaceAll("-", "");
                    System.out.println(labelOne);
                    System.out.println(providerId);
                    System.out.println(providerRegionDescription);

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
