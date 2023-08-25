<details> 
<summary>SSM2OpCode</summary>

```svg

public enum SSM2OpCode {create((byte)1)read((byte)2)update((byte)3)delete((byte)4)sync((byte)5)async((byte)6)response((byte)7)publish((byte)8)undefine((byte)16)}
```

</details>
<details> 
<summary>CHadv</summary>

```svg

class CHadv{
    private Boolean isConnecable
    private final byte[] advBytes
    private boolean isRegistered
    private boolean adv_tag_b1
    private final int rssi
    private BluetoothDevice device


    private String deviceName


    private CHProductModel productModel

    public CHadv( ScanResult scanResult)


    public Boolean isConnecable()

    public void setConnecable( Boolean data)

    public boolean isRegistered()

    public void setRegistered(boolean data)

    public boolean getAdv_tag_b1()

    public void setAdv_tag_b1(boolean data)


    public Integer getRssi()

    public BluetoothDevice getDevice()

    public void setDevice(BluetoothDevice data)


    public String getDeviceName()

    public void setDeviceName( String data)

    public CHProductModel getProductModel()

    public void setProductModel( CHProductModel data)

    public UUID getDeviceID()

    }
```

</details>
<details> 
<summary>SesameResultCode</summary>

```svg
public enum SesameResultCode {success((byte)0)invalidFormat((byte)1)notSupported((byte)2)StorageFail((byte)3)invalidSig((byte)4)notFound((byte)5)UNKNOWN((byte)6)BUSY((byte)7)INVALID_PARAM((byte)8)private final byte value

    SesameResultCode(byte value)

    public final byte getValue-w2LRezQ()


```

</details>
<details> 
<summary>SesameNotifypayload</summary>

```svg
public final class SesameNotifypayload {

    private final byte[] data


    private final SSM2OpCode notifyOpCode


    private final byte[] payload

    public SesameNotifypayload( byte[] data)


    public final byte[] getData()

    public final SSM2OpCode getNotifyOpCode()


    public final byte[] getPayload()}

```

</details>
<details> 
<summary>SesameItemCode</summary>

```svg
public enum SesameItemCode {none((byte)0)registration((byte)1)login((byte)2)user((byte)3)history((byte)4)versionTag((byte)5)disconnectRebootNow((byte)6)enableDFU((byte)7)time((byte)8)bleConnectionParam((byte)9)bleAdvParam((byte)10)autolock((byte)11)serverAdvKick((byte)12)ssmtoken((byte)13)initial((byte)14)IRER((byte)15)timePhone((byte)16)magnet((byte)17)BLE_ADV_PARAM_GET((byte)18)SENSOR_INVERVAL((byte)19)SENSOR_INVERVAL_GET((byte)20)mechSetting((byte)80)mechStatus((byte)81)lock((byte)82)unlock((byte)83)moveTo((byte)84)driveDirection((byte)85)stop((byte)86)detectDir((byte)87)toggle((byte)88)click((byte)89)ADD_SESAME((byte)101)PUB_KEY_SESAME((byte)102)REMOVE_SESAME((byte)103)Reset((byte)104)NOTIFY_LOCK_DOWN((byte)106)SSM_OS3_CARD_CHANGE((byte)107)SSM_OS3_CARD_DELETE((byte)108)SSM_OS3_CARD_GET((byte)109)SSM_OS3_CARD_NOTIFY((byte)110)SSM_OS3_CARD_LAST((byte)111)SSM_OS3_CARD_FIRST((byte)112)SSM_OS3_CARD_MODE_GET((byte)113)SSM_OS3_CARD_MODE_SET((byte)114)SSM_OS3_FINGERPRINT_CHANGE((byte)115)SSM_OS3_FINGERPRINT_DELETE((byte)116)SSM_OS3_FINGERPRINT_GET((byte)117)SSM_OS3_FINGERPRINT_NOTIFY((byte)118)SSM_OS3_FINGERPRINT_LAST((byte)119)SSM_OS3_FINGERPRINT_FIRST((byte)120)SSM_OS3_FINGERPRINT_MODE_GET((byte)121)SSM_OS3_FINGERPRINT_MODE_SET((byte)122)SSM_OS3_PASSCODE_CHANGE((byte)123)SSM_OS3_PASSCODE_DELETE((byte)124)SSM_OS3_PASSCODE_GET((byte)125)SSM_OS3_PASSCODE_NOTIFY((byte)126)SSM_OS3_PASSCODE_LAST(127)SSM_OS3_PASSCODE_FIRST(-128)SSM_OS3_PASSCODE_MODE_GET((byte)-127)SSM_OS3_PASSCODE_MODE_SET((byte)-126)

    private final byte value

    SesameItemCode(byte value)

    public final byte getValue-w2LRezQ()}

```

</details>
<details> 
<summary>SesameBleTransmit</summary>

```svg
public final class SesameBleTransmit {

    private DeviceSegmentType type;


    private byte[] input;

    private int isStart;

    public SesameBleTransmit( DeviceSegmentType type,  byte[] input)

    public final DeviceSegmentType getType()

    public final void setType( DeviceSegmentType data)


    public final byte[] getInput()

    public final void setInput( byte[] data)

    public final int isStart()

    public final void setStart(int data)


    public final byte[] getChunk$sesame_sdk_debug() 
```

</details>
<details> 
<summary>SesameBleReceiver</summary>

```svg
public final class SesameBleReceiver {

    private byte[] buffer = new byte[0];


    public final byte[] getBuffer()

    public final void setBuffer( byte[] data)

    public final Pair
<DeviceSegmentType, byte[]> feed$sesame_sdk_debug( byte[] input)

```

</details>
<details> 
<summary>Sesame2HistoryTypeEnum</summary>

```svg

public enum Sesame2HistoryTypeEnum {NONE((byte)0),BLE_LOCK((byte)1),BLE_UNLOCK((byte)2),TIME_CHANGED((byte)3),AUTOLOCK_UPDATED((byte)4),MECH_SETTING_UPDATED((byte)5),AUTOLOCK((byte)6),MANUAL_LOCKED((byte)7),MANUAL_UNLOCKED((byte)8),MANUAL_ELSE((byte)9),DRIVE_LOCKED((byte)10),DRIVE_UNLOCKED((byte)11),DRIVE_FAILED((byte)12),BLE_ADV_PARAM_UPDATED((byte)13),WM2_LOCK((byte)14),WM2_UNLOCK((byte)15),WEB_LOCK((byte)16),WEB_UNLOCK((byte)17);


    public static final Companion Companion;

    private byte value;


    private static final Sesame2HistoryTypeEnum[] values;

    Sesame2HistoryTypeEnum(byte value)

    public final byte getValue()

    public final void setValue(byte data)


    private Companion()


    datapublic final Sesame2HistoryTypeEnum getByValue(byte value)

    }

```

</details>
<details> 
<summary>Sesame2Chracs</summary>

```svg
public final class Sesame2Chracs {

    public static final Sesame2Chracs INSTANCE = new Sesame2Chracs();


    private static final UUID uuidService01 = UUID.fromString("0000fd81-0000-1000-8000-00805f9b34fb");


    public final UUID getUuidService01()


    private static final UUID uuidChr02 = UUID.fromString("16860002-A5AE-9856-B6D3-DBB4C676993E");


    public final UUID getUuidChr02()


    private static final UUID uuidChr03 = UUID.fromString("16860003-A5AE-9856-B6D3-DBB4C676993E");


    public final UUID getUuidChr03()

    }


```

</details>
<details> 
<summary>SSM3ResponsePayload</summary>

```svg
public final class SSM3ResponsePayload {

    private final byte[] data;

    private final byte cmdItCode;

    private final byte cmdResultCode;


    private final byte[] payload;

    public SSM3ResponsePayload( byte[] data)


    public final byte[] getData()public final byte getCmdItCode-w2LRezQ()

    public final byte getCmdResultCode-w2LRezQ()


    public final byte[] getPayload()}

```

</details>
<details> 
<summary>SSM3PublishPayload</summary>

```svg
public final class SSM3PublishPayload {

    private final byte[] data;

    private final byte cmdItCode;


    private final byte[] payload;

    public SSM3PublishPayload( byte[] data)


    public final byte[] getData()

    public final byte getCmdItCode-w2LRezQ()

    public final byte[] getPayload()}

```

</details>
<details> 
<summary>SSM2ResponsePayload</summary>

```svg
public final class SSM2ResponsePayload {

    private final byte[] data;

    private final byte cmdItCode;

    private final byte cmdOPCode;

    private final byte cmdResultCode;


    private final byte[] payload;

    public SSM2ResponsePayload( byte[] data)


    public final byte[] getData()

    public final byte getCmdItCode-w2LRezQ()

    public final byte getCmdOPCode-w2LRezQ()

    public final byte getCmdResultCode-w2LRezQ()


    public final byte[] getPayload()}

```

</details>
<details> 
<summary>DeviceSegmentType</summary>

```svg
public enum DeviceSegmentType {plain(1),cipher(2);


    public static final Companion Companion;

    private int value;


    private static final DeviceSegmentType[] values;

    DeviceSegmentType(int value)

    public final int getValue()

    public final void setValue(int data)


    public static final class Companion {private Companion() {}


    public final DeviceSegmentType getByValue(int value)}}
```

</details>

<details> 
<summary>CHBaseDevice</summary>

```svg
public class CHBaseDevice {public CHProductModel productModel;


    public final CHProductModel getProductModel()


    public final void setProductModel( CHProductModel data)

    private final SesameBleReceiver gattRxBuffer = new SesameBleReceiver();

    private SesameBleTransmit gattTxBuffer;

    public byte[] mSesameToken;

    private BluetoothGattCharacteristic mCharacteristic;

    private CHDeviceStatusDelegate delegate;

    private Long deviceTimestamp;

    private Long loginTimestamp;

    private UUID deviceId;


    public final SesameBleReceiver getGattRxBuffer()public final SesameBleTransmit getGattTxBuffer()

    public final void setGattTxBuffer(data SesameBleTransmit data)

    public final byte[] getMSesameToken()

    public final void setMSesameToken( byte[] data)

    public final BluetoothGattCharacteristic getMCharacteristic()public final void setMCharacteristic(data BluetoothGattCharacteristic data)public final CHDeviceStatusDelegate getDelegate()

    public final void setDelegate(data CHDeviceStatusDelegate data)

    public final Long getDeviceTimestamp()

    public final void setDeviceTimestamp(data Long data)public final Long getLoginTimestamp()public final void setLoginTimestamp(data Long data)

    public final UUID getDeviceId()public final void setDeviceId(data UUID data)private boolean isRegistered = true;

    public final boolean isRegistered()public final void setRegistered(boolean data)private Integer rssi = Integer.valueOf(0);

    private BluetoothGatt mBluetoothGatt;

    public final Integer getRssi()public final void setRssi(data Integer data)public final BluetoothGatt getMBluetoothGatt()public final void setMBluetoothGatt(data BluetoothGatt data)

    private Boolean isNeedAuthFromServer = Boolean.valueOf(false);

    private CHSesameProtocolMechStatus mechStatus;

    private CHDeviceStatus deviceShadowStatus;

    public final Boolean isNeedAuthFromServer()public final void setNeedAuthFromServer(data Boolean data)public final CHSesameProtocolMechStatus getMechStatus()public final void setMechStatus(data CHSesameProtocolMechStatus value)

    public final CHDeviceStatus getDeviceShadowStatus()public final void setDeviceShadowStatus(data CHDeviceStatus value)}


    private CHDeviceStatus deviceStatus = CHDeviceStatus.NoBleSignal;

    private CHDevice sesame2KeyData;


    public final CHDeviceStatus getDeviceStatus()

    public final void setDeviceStatus( CHDeviceStatus value)

    public final CHDevice getSesame2KeyData()

    public final void setSesame2KeyData(data CHDevice value)


    public final void invoke( Object ss2Keydata)

    public final void disconnect( Function1 result)}
```

</details>
<details> 
<summary>CHBaseAdv</summary>

```svg
public interface CHBaseAdv {Integer getRssi();

    boolean isRegistered();

    boolean getAdv_tag_b1();

    UUID getDeviceID();


    BluetoothDevice getDevice();

    void setDevice( BluetoothDevice paramBluetoothDevice);

    String getDeviceName();

    void setDeviceName(data String paramString);

    CHProductModel getProductModel();

    void setProductModel(data CHProductModel paramCHProductModel);

    Boolean isConnecable();

    void setConnecable(data Boolean paramBoolean);}

```

</details>
<details> 
<summary>Wm2Chracs</summary>

```svg
public final class Wm2Chracs {

    public static final Wm2Chracs INSTANCE = new Wm2Chracs();

    private static final UUID uuidService01 = UUID.fromString("1b7e8251-2877-41c3-b46e-cf057c562524");

    public final UUID getUuidService01()private static final UUID writeChrac = UUID.fromString("aca0ef7c-eeaa-48ad-9508-19a6cef6b356");

    public final UUID getWriteChrac()private static final UUID receiveChr = UUID.fromString("8ac32d3f-5cb9-4d44-bec2-ee689169f626");

    public final UUID getReceiveChr()}
```

</details>
<details> 
<summary>WM2ActionCode</summary>

```svg
public enum WM2ActionCode {CODE_NON((byte)0),REGISTER_WM2((byte)1),LOGIN_WM2((byte)2),UPDATE_WIFI_SSID((byte)3),UPDATE_WIFI_PASSWORD((byte)4),CONNECT_WIFI((byte)5),NETWORK_STATUS((byte)6),DELETE_SESAME((byte)7),ADD_SESAME((byte)8),INITIAL((byte)13),CCCD((byte)14),SESAME_KEYS((byte)16),RESET_WM2((byte)18),SCAN_WIFI_SSID((byte)19),OPEN_OTA_SERVER((byte)126),VERSION_TAG(127);

    private final byte value;

    WM2ActionCode(byte value)

    public final byte getValue-w2LRezQ()}

```

</details>
<details> 
<summary>CHWifiModule2Device</summary>

```svg
public final class CHWifiModule2Device extends CHSesameOS3 implements CHWifiModule2, CHDeviceUtil {

    public CHDevice getKey()

    public void createGuestKey( String keyName,  Function1 result)public void getGuestKeys( Function1 result)

    public void removeGuestKey( String guestKeyId,  Function1 result)

    public void updateGuestKey( String guestKeyId,  String name,  Function1 result)public void setHistoryTag( byte[] tag,  Function1 result)public byte[] getHistoryTag() {return CHWifiModule2.DefaultImpls.getHistoryTag(this);}


    public String getTimeSignature() {return CHWifiModule2.DefaultImpls.getTimeSignature(this);}


    private Map
<String, String> ssm2KeysMap = new LinkedHashMap<>();


public Map<String, String> getSsm2KeysMap()

public void setSsm2KeysMap( Map<String, String> data)

private CHWifiModule2MechSettings mechSetting = new CHWifiModule2MechSettings(null, null);

private CHadv advertisement;

public CHWifiModule2MechSettings getMechSetting()

public void setMechSetting(data CHWifiModule2MechSettings value)

public void goIOT()

public CHadv getAdvertisement()
public void setAdvertisement(data CHadv value)
public void connect( Function1 result)


private final BluetoothGattCallback mBluetoothGattCallback = new
CHWifiModule2Device$mBluetoothGattCallback$1();


public final BluetoothGattCallback getMBluetoothGattCallback()

private final void parseNotifyPayload(byte[] palntext)

private final void onGattSesameResponse(SSM3ResponsePayload wm2RespPl)

public void onServicesDiscovered(data BluetoothGatt gatt, int status)

public void onConnectionStateChange( BluetoothGatt gatt, int status, int newState)

public void register( Function1<? super Result<? extends CHResultState
<CHEmpty>>, Unit> result)

    public void login(data String token)

    public void scanWifiSSID( Function1<? super Result<? extends CHResultState
    <CHEmpty>>, Unit> result)

        public void setWifiSSID( String ssid, Function1<? super Result<? extends CHResultState
        <CHEmpty>>, Unit> result)


            public void setWifiPassword( String password, Function1<? super Result<? extends
            CHResultState
            <CHEmpty>>, Unit> result)
                public void connectWifi( Function1<? super Result<? extends CHResultState
                <CHEmpty>>, Unit> result)


                    public void insertSesames( CHDevices sesame, Function1<? super Result<? extends
                    CHResultState
                    <CHEmpty>>, Unit> result)


                        public void removeSesame( String sesameKeyTag, Function1<? super Result<?
                        extends CHResultState
                        <CHEmpty>>, Unit> result)

                            public void getVersionTag( Function1<? super Result<? extends
                            CHResultState
                            <String>>, Unit> result)


                                public void reset( Function1<? super Result<? extends CHResultState
                                <CHEmpty>>, Unit> result)


                                    public void updateFirmware( Function1<? super Result<? extends
                                    CHResultState
                                    <BluetoothDevice>>, Unit> onResponse)

                                        public final void invoke( SSM3ResponsePayload res)


                                        private final void onGattWM2Publish(SSM3PublishPayload
                                        receivePayload)

                                        }
```

</details>
<details> 
<summary>CHSesameTouchProDevice</summary>

```svg
public final class CHSesameTouchProDevice extends CHSesameOS3 implements CHSesameTouchPro, CHDeviceUtil {public void createGuestKey( String keyName,  Function1 result)

    public void getGuestKeys( Function1 result)public byte[] getHistoryTag()

    public CHDevice getKey()

    public String getTimeSignature()

    public void removeGuestKey( String guestKeyId,  Function1 result)public void setHistoryTag( byte[] tag,  Function1 result)

    public void updateGuestKey( String guestKeyId,  String name,  Function1 result)public void goIOT()

    private Map
<String, byte[]> ssm2KeysMap = (Map)new LinkedHashMap<>();

private CHadv advertisement;


public Map<String, byte[]> getSsm2KeysMap()

public void setSsm2KeysMap( Map<String, byte[]> data)

public CHadv getAdvertisement()

public void setAdvertisement(data CHadv value)

public void keyBoardPassCodeModeGet( Function1<? super Result<? extends CHResultState
<Byte>>, Unit> result)

    public final void invoke( SSM3ResponsePayload res)

    public void keyBoardPassCodeModeSet(byte mode, Function1<? super Result<? extends CHResultState
    <CHEmpty>>, Unit> result)


        public void keyBoardPassCodeDelete( String ID, Function1<? super Result<? extends
        CHResultState
        <CHEmpty>>, Unit> result)

            public void keyBoardPassCode( Function1<? super Result<? extends CHResultState
            <CHEmpty>>, Unit> result)
                public void keyBoardPassCodeChange( String ID, String name, Function1<? super
                Result<? extends CHResultState
                <CHEmpty>>, Unit> result)


                    public void fingerPrintModeGet( Function1<? super Result<? extends CHResultState
                    <Byte>>, Unit> result)
                        public void fingerPrintModeSet(byte mode, Function1<? super Result<? extends
                        CHResultState
                        <CHEmpty>>, Unit> result)
                            public void fingerPrintDelete( String ID, Function1<? super Result<?
                            extends CHResultState
                            <CHEmpty>>, Unit> result)


                                public void fingerPrints( Function1<? super Result<? extends
                                CHResultState
                                <CHEmpty>>, Unit> result)

                                    public void fingerPrintsChange( String ID, String name,
                                    Function1<? super Result<? extends CHResultState
                                    <CHEmpty>>, Unit> result)

                                        public void cardModeGet( Function1<? super Result<? extends
                                        CHResultState
                                        <Byte>>, Unit> result)

                                            public void cardModeSet(byte mode, Function1<? super
                                            Result<? extends CHResultState
                                            <CHEmpty>>, Unit> result)

                                                public void cardDelete( String ID, Function1<? super
                                                Result<? extends CHResultState
                                                <CHEmpty>>, Unit> result)

                                                    public void cardChange( String ID, String name,
                                                    Function1<? super Result<? extends CHResultState
                                                    <CHEmpty>>, Unit> result)


                                                        public void cards( Function1<? super
                                                        Result<? extends CHResultState
                                                        <CHEmpty>>, Unit> result)

                                                            public void login(data String
                                                            token)

                                                            public void register( Function1<? super
                                                            Result<? extends CHResultState
                                                            <CHEmpty>>, Unit> result)
                                                                public void insertSesame( CHDevices
                                                                sesame, Function1<? super Result<?
                                                                extends CHResultState
                                                                <CHEmpty>>, Unit> result)


                                                                    static final class
                                                                    CHSesameTouchProDevice$insertSesame$2
                                                                    extends Lambda implements
                                                                    Function1<SSM3ResponsePayload,
                                                                    Unit>

                                                                    CHSesameTouchProDevice$insertSesame$2(Function1
                                                                    <Result
                                                                    <? extends CHResultState
                                                                    <CHEmpty>>, Unit> $result)


                                                                        public void removeSesame(
                                                                        String tag, Function1<?
                                                                        super Result<? extends
                                                                        CHResultState
                                                                        <CHEmpty>>, Unit> result) {


                                                                            public final void
                                                                            invoke(
                                                                            SSM3ResponsePayload
                                                                            ssm2ResponsePayload)

                                                                            public void
                                                                            onGattSesamePublish(
                                                                            SSM3PublishPayload
                                                                            receivePayload)
                                                                            }

```

</details>
<details> 
<summary>CHSesameTouchCard</summary>

```svg

public final class CHSesameTouchCard {private final byte cardType;

    private final byte idLength;


    private final String cardID;

    private final int nameIndex;

    private final byte nameLength;


    private final String cardName;

    public CHSesameTouchCard( byte[] data)

    public final byte getCardType()public final byte getIdLength()


    public final String getCardID()

    public final int getNameIndex()public final byte getNameLength()

    public final String getCardName()}
```

</details>
<details> 
<summary>CHSesameBike2Device</summary>

```svg
public final class CHSesameBike2Device extends CHSesameOS3 implements CHSesameBike2, CHDeviceUtil {private CHadv advertisement;

    public void createGuestKey( String keyName,  Function1 result)

    public void disableNotification( String fcmToken,  Function1 result)public void enableNotification( String fcmToken,  Function1 result)

    public void getGuestKeys( Function1 result)

    public byte[] getHistoryTag()

    public CHDevice getKey()

    public String getTimeSignature()

    public void isEnableNotification( String fcmToken,  Function1 result)

    public void removeGuestKey( String guestKeyId,  Function1 result)

    public void setHistoryTag( byte[] tag,  Function1 result)

    public void updateGuestKey( String guestKeyId,  String name,  Function1 result)

    public void goIOT()

    public CHadv getAdvertisement()

    public void setAdvertisement(data CHadv value)public void unlock(data byte[] tag,  Function1<? super Result<? extends CHResultState
<CHEmpty>>, Unit> result)


    public void register( Function1<? super Result<? extends CHResultState
    <CHEmpty>>, Unit> result)
        public final void invoke( CHSesameBike2Device $this$makeApiCall)

        public void login(data String token)
        public final void invoke( SSM3ResponsePayload it)

        public void onGattSesamePublish( SSM3PublishPayload receivePayload)
        }
        }

```

</details>
<details> 
<summary>CHSesame5Device</summary>

```svg
public final class CHSesame5Device extends CHSesameOS3 implements CHSesame5, CHDeviceUtil {private UUID currentDeviceUUID;

    private Function1<? super Result<? extends CHResultState
<Pair<List<CHSesame5History>, Long>>>, Unit> historyCallback;

private boolean isHistory;

private CHSesame5MechSettings mechSetting;

private CHadv advertisement;

private boolean isConnectedByWM2;

public void createGuestKey( String keyName, Function1 result)

public void disableNotification( String fcmToken, Function1 result)
public void enableNotification( String fcmToken, Function1 result)
public void getGuestKeys( Function1 result)

public byte[] getHistoryTag()


public CHDevice getKey()

public String getTimeSignature()

public void isEnableNotification( String fcmToken, Function1 result)
public void removeGuestKey( String guestKeyId, Function1 result)

public void setHistoryTag( byte[] tag, Function1 result)
public void updateGuestKey( String guestKeyId, String name, Function1 result)
public final boolean isHistory()

public final void setHistory(boolean value)

public CHSesame5MechSettings getMechSetting()
public void setMechSetting(data CHSesame5MechSettings data)
public CHadv getAdvertisement()
public void setAdvertisement(data CHadv value)
public final boolean isConnectedByWM2()

public final void setConnectedByWM2(boolean data)
public void goIOT()

public void configureLockPosition(short lockTarget, short unlockTarget, Function1<? super Result<?
extends CHResultState
<CHEmpty>>, Unit> result)


    public final void invoke( SSM3ResponsePayload res)


    public void autolock(int delay, Function1<? super Result<? extends CHResultState
    <Integer>>, Unit> result)
        public void magnet( Function1<? super Result<? extends CHResultState
        <CHEmpty>>, Unit> result)
            private final CHSesame5History eventToHistory(Sesame2HistoryTypeEnum historyType, long
            ts, int recordID, CHSesame5MechStatus mechStatus, byte[] histag)

            public void history(data Long cursor, UUID uuid, Function1<? super Result<? extends
            CHResultState
            <Pair
            <List
            <CHSesame5History>, Long>>>, Unit> result)


                public void toggle(data byte[] historytag, Function1<? super Result<? extends
                CHResultState
                <CHEmpty>>, Unit> result)


                    public void unlock(data byte[] historytag, Function1<? super Result<?
                    extends CHResultState
                    <CHEmpty>>, Unit> result)


                        public void lock(data byte[] historytag, Function1<? super Result<?
                        extends CHResultState
                        <CHEmpty>>, Unit> result)


                            public void register( Function1<? super Result<? extends CHResultState
                            <CHEmpty>>, Unit> result)

                                public void login(data String token)

                                private final void readHistoryCommand()

                                public void onGattSesamePublish( SSM3PublishPayload receivePayload)
                                }


```

</details>
<details> 
<summary>SesameOS3Payload</summary>

```svg
public final class SesameOS3Payload {private final byte itemCode;


    private final byte[] data;

    private SesameOS3Payload(byte itemCode, byte[] data)public final byte getItemCode-w2LRezQ()

    public final byte[] getData()


    public final byte[] toDataWithHeader()

    public final byte component1-w2LRezQ()


    public final byte[] component2()


    public final SesameOS3Payload copy-0ky7B_Q(byte itemCode,  byte[] data)

    public String toString()

    public int hashCode()

    public boolean equals(data Object other)}

```

</details>
<details> 
<summary>SesameOS3BleCipher</summary>

```svg
public final class SesameOS3BleCipher {

    private final String name;


    private byte[] sessionKey;


    private byte[] sault;

    private long encryptCounter;

    private long decryptCounter;

    public SesameOS3BleCipher( String name,  byte[] sessionKey,  byte[] sault)


    public final String getName()public final long getEncryptCounter()public final void setEncryptCounter(long data)

    public final long getDecryptCounter()

    public final void setDecryptCounter(long data)

    public final byte[] encrypt$sesame_sdk_debug( byte[] plaintext)


    public final byte[] decrypt$sesame_sdk_debug( byte[] ciphertext)}
```

</details>
<details> 
<summary>CHSesameOS3Publish</summary>

```svg

public interface CHSesameOS3Publish {void onGattSesamePublish( SSM3PublishPayload paramSSM3PublishPayload);}

```

</details>
<details> 
<summary>CHSesameOS3Publish</summary>

```svg

public class CHSesameOS3 extends CHBaseDevice implements CHSesameOS3Publish {
  private SesameOS3BleCipher cipher;
  
  public final SesameOS3BleCipher getCipher() 
  
  public final void setCipher(data SesameOS3BleCipher data) 
  
  private Map<UByte, Function1<SSM3ResponsePayload, Unit>> cmdCallBack = new LinkedHashMap<>();
  
  
  public final Map<UByte, Function1<SSM3ResponsePayload, Unit>> getCmdCallBack()
  public final void setCmdCallBack( Map<UByte, Function1<SSM3ResponsePayload, Unit>> data)
  
  
  private Semaphore semaphore = new Semaphore(1);
  
  
  public final Semaphore getSemaphore() 
  public final void setSemaphore( Semaphore data) 
  
  public void connect( Function1 result)
  
  
  private final BluetoothGattCallback mBluetoothGattCallback = new CHSesameOS3$mBluetoothGattCallback$1();
  

    private final void parseNotifyPayload(byte[] palntext)
    
    private final void onGattSesameResponse(SSM3ResponsePayload ssm2ResponsePayload)
    public void onCharacteristicWrite(data BluetoothGatt gatt, data BluetoothGattCharacteristic characteristic, int status) {
      
    
    public void onServicesDiscovered(data BluetoothGatt gatt, int status) 
    
    public void onConnectionStateChange( BluetoothGatt gatt, int status, int newState) 
  
  public final String byToString( byte[] bs) 
  public final void transmit() 
  public final void sendCommand( SesameOS3Payload payload,  DeviceSegmentType isEncryt,  Function1<SSM3ResponsePayload, Unit> onResponse) 

  
  public void getVersionTag( Function1<? super Result<? extends CHResultState<String>>, Unit> result) 
  
  public void reset( Function1<? super Result<? extends CHResultState<CHEmpty>>, Unit> result) 
  
  public void updateFirmware( Function1 onResponse) 
  public final void parceADV(data CHadv value) 
  
  public void onGattSesamePublish( SSM3PublishPayload receivePayload)
}

