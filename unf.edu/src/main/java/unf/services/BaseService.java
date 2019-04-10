package unf.services;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BaseService {

    protected ExecutorService executor = Executors.newSingleThreadExecutor();

}
