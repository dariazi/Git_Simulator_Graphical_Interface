package common;

import java.net.URL;

public class MagitResourcesConstants {


        private static final String BASE_PACKAGE = "/examples/basic/tasks";
        private static final  String SINGLE_HISTOGRAM_FXML_RESOURCE_IDENTIFIER = BASE_PACKAGE + "/components/singlehistogram/singleWord.fxml";

        public static final String MAIN_FXML_RESOURCE_IDENTIFIER = BASE_PACKAGE + "/components/main/histogram.fxml";
        public static final URL MAIN_FXML_RESOURCE = common.MagitResourcesConstants.class.getResource(common.MagitResourcesConstants.SINGLE_HISTOGRAM_FXML_RESOURCE_IDENTIFIER);
        public static final String HEAD_RELATIVE_PATH= "\\.magit\\branches\\HEAD";
        public static final String BRANCHES= "\\.magit\\branches\\";
        public static final String OBJECTS="\\.magit\\objects\\";
    }


