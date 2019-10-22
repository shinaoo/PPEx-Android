package ppex.utils;

import java.io.File;
import java.util.Arrays;

public class FileUtil {
    public static File findTargetFileByName(File[] files,String filename){
        return Arrays.stream(files).filter( file -> file.getName().equals(filename)).findFirst().get();
    }
}
