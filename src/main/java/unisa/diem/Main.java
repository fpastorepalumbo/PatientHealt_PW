package unisa.diem;

import lombok.SneakyThrows;
import unisa.diem.parser.DatasetUtility;

/*
    * Use the load Dataset function to load the dataset
 */
public class Main {
    @SneakyThrows
    public static void main(String[] args) {
        DatasetUtility datasetUtility = new DatasetUtility();
        datasetUtility.loadDataset();
    }
}
