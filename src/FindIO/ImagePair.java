package FindIO;

/**
 * Created by Beyond on 10/12/2014 0012.
 */
public class ImagePair {
    String imageID;
    float value;

    ImagePair(String imageID, float value){
        this.imageID = imageID;
        this.value = value;
    }

    String getImageID(){
        return this.imageID;
    }

    float getValue(){
        return this.value;
    }
}
