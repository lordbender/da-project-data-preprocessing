package unf;

import unf.services.CsvService;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        try {
            CsvService csvService = new CsvService();

            csvService.preProcessDrgData();
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println( "Hello World!" );
    }
}
