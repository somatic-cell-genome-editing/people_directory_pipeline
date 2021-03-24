package edu.mcw.scge.directory;


import edu.mcw.scge.directory.dao.PersonDao;
import edu.mcw.scge.directory.process.ProcessFile;

import java.io.IOException;


public class Manager {
    public static void main(String[] args) throws Exception {

        ProcessFile process=new ProcessFile();
        try {

            process.insertFromFile("data/directory.xlsx");
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Done!!");
    }

}
