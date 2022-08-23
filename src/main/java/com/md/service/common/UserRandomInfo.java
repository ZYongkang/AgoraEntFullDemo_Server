package com.md.service.common;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class UserRandomInfo {

    private static String nikeName = "James,William,Lucas,Henry,Jack,Daniel,Michael,Logan,Owen,Ashley,Aaron,Cooper,Alex,Wesley,Adam,Bryson,Jasper,Jason,Cole,Ace,Ivan,Leon,Brandon,Joe,Jenny,Simon,Kylie,Kobe,Jay,Travis,Jared,Jefferey,Hassan,Dash,Mia,Isabella,Emily,Layla,Nora,Lily,Zoe,Stella,Elena,Claire,Alice,Bella,Cora,Eva,Iris,Maria,Lucia,Jasmine,Olive,Blake,Aspen,Myla,Hanna,Julie,Eve";
    private static String headImage = "https://beidou-ktv-user.oss-cn-beijing.aliyuncs.com/1658044795022NaEWFm.png,https://beidou-ktv-user.oss-cn-beijing.aliyuncs.com/1658044822084UKTLom.png,https://beidou-ktv-user.oss-cn-beijing.aliyuncs.com/1658044831351WHndZm.png,https://beidou-ktv-user.oss-cn-beijing.aliyuncs.com/1658044840620DanQEm.png,https://beidou-ktv-user.oss-cn-beijing.aliyuncs.com/1658044851151ZWnWZm.png,https://beidou-ktv-user.oss-cn-beijing.aliyuncs.com/1658044862643rOEdSm.png,https://beidou-ktv-user.oss-cn-beijing.aliyuncs.com/1658044874226raVdCm.png,https://beidou-ktv-user.oss-cn-beijing.aliyuncs.com/1658044883640EMNPom.png,https://beidou-ktv-user.oss-cn-beijing.aliyuncs.com/1658044893363EanTom.png,https://beidou-ktv-user.oss-cn-beijing.aliyuncs.com/1658044904844TBnAGm.png,https://beidou-ktv-user.oss-cn-beijing.aliyuncs.com/1658044916443ranOCm.png,https://beidou-ktv-user.oss-cn-beijing.aliyuncs.com/1658044929427rGNETm.png,https://beidou-ktv-user.oss-cn-beijing.aliyuncs.com/1658044950568OGnRom.png,https://beidou-ktv-user.oss-cn-beijing.aliyuncs.com/1658044960051raODNm.png,https://beidou-ktv-user.oss-cn-beijing.aliyuncs.com/1658044979855SaXdFm.png,https://beidou-ktv-user.oss-cn-beijing.aliyuncs.com/1658044992223OSndNm.png,https://beidou-ktv-user.oss-cn-beijing.aliyuncs.com/1658045022196rBICom.png,https://beidou-ktv-user.oss-cn-beijing.aliyuncs.com/1658045035358PaXYom.png";

    public static String getName(){
        List<String> snList = Arrays.asList(nikeName.split(","));
        Random random = new Random();
        return snList.get(random.nextInt(snList.size() - 1));
    }

    public static String headImage() {
        List<String> snList = Arrays.asList(headImage.split(","));
        Random random = new Random();
        return snList.get(random.nextInt(snList.size() - 1));
    }
}
