package co.candyhouse.sesame.utils


internal class BleBaseType {

    companion object {
        /**
         *
        public static final int CONNECTION_PRIORITY_BALANCED = 0;
        public static final int CONNECTION_PRIORITY_HIGH = 1;
        public static final int CONNECTION_PRIORITY_LOW_POWER = 2;
        public static final int GATT_CONNECTION_CONGESTED = 143;
        public static final int GATT_FAILURE = 257;
        public static final int GATT_INSUFFICIENT_AUTHENTICATION = 5;
        public static final int GATT_INSUFFICIENT_ENCRYPTION = 15;
        public static final int GATT_INVALID_ATTRIBUTE_LENGTH = 13;
        public static final int GATT_INVALID_OFFSET = 7;
        public static final int GATT_READ_NOT_PERMITTED = 2;
        public static final int GATT_REQUEST_NOT_SUPPORTED = 6;
        public static final int GATT_SUCCESS = 0;
        public static final int GATT_WRITE_NOT_PERMITTED = 3;
         * */

        internal fun GattConnectStatusDec(status: Int): String {

            if (status == 0) {
                return "SUCCESS"
            }
            if (status == 128) {
                return "GATT_NO RESOURCES"
            }
            if (status == 257) {
                return "GATT_FAILURE"
            }
            if (status == 8) {
                return "GATT_CONN_TIMEOUT ,Issue with bond"
            }
            if (status == 22) {
                return "GATT_CONN_TERMINATE_LOCAL_HOST"
            }
            if (status == 133) {//https://android.googlesource.com/platform/external/bluetooth/bluedroid/+/5738f83aeb59361a0a2eda2460113f6dc9194271/stack/include/gatt_api.h#49
                return "GATT_CONN_TERMINATE_LOCAL_HOST"
            }

            if (status == 19) {
                return "Disconnected by device 被踢！！"
            }

            return "Unknow:" + status
        }

        /**
         *
        int STATE_CONNECTED = 2;
        int STATE_CONNECTING = 1;
        int STATE_DISCONNECTED = 0;
        int STATE_DISCONNECTING = 3;

         * */
        internal fun GattConnectStateDec(status: Int): String {

            if (status == 0) {
                return " 斷開:STATE_DISCONNECTED"
            }
            if (status == 1) {
                return " 連結中:STATE_CONNECTING"
            }
            if (status == 2) {
                return " 已連上:STATE_CONNECTED"
            }
            if (status == 3) {
                return " 斷開中:STATE_DISCONNECTING"
            }
            return "Unknow status:$status"
        }
    }
}




