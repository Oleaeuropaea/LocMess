package pt.ulisboa.tecnico.cmu.locmess.utils;


import android.content.Context;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class LocMessFilesUtils {
    public static final String CENTRALIZED_SAVED_POSTS = "centralizedSavedPosts";
    public static final String DECENTRALIZED_SAVED_POSTS = "decentralizedSavedPosts";

    public static final String CENTRALIZED_POSTED_POSTS = "centralizedPostedPosts";
    public static final String DECENTRALIZED_POSTED_POSTS = "decentralizedPostedPosts";

    public static final String CENTRALIZED_NEARBY_POSTS = "centralizedNearbyPosts";
    public static final String DECENTRALIZED_NEARBY_POSTS = "decentralizedNearbyPosts";

    public static final String THIRD_PARTY_POSTS = "thirdPartyPosts";



    public static void writeObjectToFile(Context context, String fileName, int mode, Object objectToWrite) {
        try {
            FileOutputStream fos = context.openFileOutput(fileName, mode);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(objectToWrite);
            oos.close();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }


    public static Object readObjectFromFile(Context context, String fileName) {
        try {
            FileInputStream fis = context.openFileInput(fileName);
            ObjectInputStream ois = new ObjectInputStream(fis);
            Object result = ois.readObject();
            ois.close();
            fis.close();
            return result;
        } catch (FileNotFoundException e) {
            Log.d("-> readObject:", "FileNotFound!! -<");
            return null;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

}
