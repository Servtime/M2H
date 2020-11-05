package marray.top.mhtml.Enum;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.net.QuotedPrintableCodec;
import org.apache.commons.lang3.StringUtils;

/**
 * @author ：wangsiliang
 * @date ：Created in 2020/11/4 20:58
 * @description：IMhtml的加密枚举类
 * @modified By：
 * @version: 1.1$
 */
public enum IMhtmlScretEnum {

    QUOTED_PRINTABLE("quoted-printable", QuotedPrintableCodec.class,
            "decodeQuotedPrintable"),
    BASE_64("base64", Base64.class, "decode");

    /**
     * 文档加密类型
     */
    public String contentTransferEncoding;

    /**
     * 对应的加解密工具
     */
    public Class scretInstance;

    public String methodName;

    IMhtmlScretEnum(String contentTransferEncoding, Class scretInstance, String methodName) {
        this.contentTransferEncoding = contentTransferEncoding;
        this.scretInstance = scretInstance;
        this.methodName = methodName;
    }

    /**
     * 获取加密
     *
     * @param contentTransferEncoding
     * @return
     */
    public static Class getScretInstance(String contentTransferEncoding) {
        IMhtmlScretEnum[] iMhtmlScretEnums = values();
        for (IMhtmlScretEnum iMhtmlScretEnum : iMhtmlScretEnums) {
            if (iMhtmlScretEnum.contentTransferEncoding.equalsIgnoreCase(StringUtils.deleteWhitespace(contentTransferEncoding))) {
                return iMhtmlScretEnum.scretInstance;
            }
        }
        throw new RuntimeException(String.format("非规范的mhtml文件,未符合%s的加密类", contentTransferEncoding));
    }

    /**
     * 获取加密
     *
     * @param contentTransferEncoding
     * @return
     */
    public static String getMetodName(String contentTransferEncoding) {
        IMhtmlScretEnum[] iMhtmlScretEnums = values();
        for (IMhtmlScretEnum iMhtmlScretEnum : iMhtmlScretEnums) {
            if (iMhtmlScretEnum.contentTransferEncoding.equalsIgnoreCase(StringUtils.deleteWhitespace(contentTransferEncoding))) {
                return iMhtmlScretEnum.methodName;
            }
        }
        throw new RuntimeException(String.format("非规范的mhtml文件,未符合%s的加密类", contentTransferEncoding));
    }

}
