package marray.top.mhtml.Enum;

/**
 * @author ：feiyu
 * @date ：Created in 2020/11/4 19:32
 * @description：mhtml格式的内容枚举类
 * @modified By：
 * @version: 1.1$
 */
public enum IMhtmlPartEnum {

    HTML("html的内容"),
    CSS("css的内容"),
    PNG("png的图片"),
    JPEG("jpeg的内容"),
    GIF("GIF的内容");

    /**
     * 标签中文描述
     */
    private String describe;

    IMhtmlPartEnum(String describe) {
        this.describe = describe;
    }
}
