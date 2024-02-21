package unisa.diem;

import lombok.SneakyThrows;
import unisa.diem.parser.DatasetService;

/*
    * Use the load Dataset function to load the dataset
 */
public class Main {
    @SneakyThrows
    public static void main(String[] args) {
        DatasetService datasetUtility = new DatasetService();
        datasetUtility.loadDataset();
    }
}
