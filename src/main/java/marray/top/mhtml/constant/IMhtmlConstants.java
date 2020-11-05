package marray.top.mhtml.constant;

/**
 * @author ：feiyu
 * @date ：Created in 2020/11/4 15:28
 * @description：mhtml或者.mht的文件解析常量类，请参考
 * {@link marray.top.mhtml.parase.IMhtmlParas}
 * @modified By：
 * @version: 1.1$
 */
public interface IMhtmlConstants {

    /**
     * 表明mhtml、mht文件里，不通文件内容的分隔标识符是什么
     */
    String BOUNDARY_REGEX = "\\bb(\\s)*o(\\s)*u(\\s)*n(\\s)*d(\\s)*a(\\s)*r(\\s)*y(\\s)*\\=(\\s)*\"(\\S)*\"";

    /**
     * html的Content-Type正则
     */
    String HTML_REGEX = "(\\s)*Content-Type(\\s)*:(\\s)*text/html";
    /**
     * css的Content-Type正则
     */
    String CSS_REGEX = "\\bContent-Type(\\s)*:(\\s)*text/css\\b";
    /**
     * PNG的Content-Type正则
     */
    String IMAGE_REGEX = "\\bContent-Type(\\s)*:(\\s)*(image/png|image/jpeg|image/gif|image/\\*){1}\\b";

    String CONTENT_TYPE="\\bContent-Type(\\s)*:(\\s)*(\\w|/|\\*)*\\b";
    /**
     * 编码转换格式 Content-Transfer-Encoding
     */
    String CONTENT_TRANSFER_ENCODING_REGEX = "\\bContent-Transfer-Encoding(\\s)*:(\\s)*(\\w|\\-)+\\b";
    /**
     * 原文件所在地址 Content-Location
     */
    String CONTENT_LOCATION_REGEX = "\\bContent-Location(\\s)*:(\\s)*(\\w|@|\\.|-|/|\\?|\\&|\\=|\\:|%|\\s)+\\b";


    /**
     * 原文件的部分数据标识Content-Location、
     */
    String PART_SIGN="\\b(Content-Location|Content-ID){1}(\\s)*:(\\s)*(\\w|@|\\.|-|/|\\?|\\&|\\=|\\:|%|\\s|\\>|\\<)+\\b";

    /**
     * 获取Content-ID正则
     */
    String CONTENT_ID="\\bContent-ID(\\s)*:(\\s)*(\\w|@|\\.|-|/|\\?|\\&|\\=|\\:|%|\\s|\\>|\\<)+\\b";





}
