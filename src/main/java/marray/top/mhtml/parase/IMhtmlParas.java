package marray.top.mhtml.parase;

import lombok.extern.slf4j.Slf4j;
import marray.top.mhtml.Enum.IMhtmlPartEnum;
import marray.top.mhtml.Enum.IMhtmlScretEnum;
import marray.top.mhtml.constant.IMhtmlConstants;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.MethodUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author ：wangsiliang
 * @date ：Created in 2020/11/4 15:34
 * @description：解析mhtml格式或者mht文件的工具类
 * @modified By：
 * @version: 1.1$
 */
@Slf4j
public class IMhtmlParas implements IMhtmlConstants {


    /**
     * 原文件 mhtml或者mht
     */
    private String mhtFile;
    /**
     * 正则表达式
     */
    private static final Pattern p = Pattern.compile("(\\w|_|-)+\\.\\w+");

    /**
     * boundary分隔符
     */
    private static final Pattern boundary = Pattern.compile(BOUNDARY_REGEX);

    /**
     * Content-Transfer-Encoding头部正则
     */
    private static final Pattern contentTransferEncodingPattern = Pattern.compile(CONTENT_TRANSFER_ENCODING_REGEX);

    /**
     * Content-Location正则
     */
    private static final Pattern contentLocation = Pattern.compile(CONTENT_LOCATION_REGEX);

    private static final Pattern partSign = Pattern.compile(PART_SIGN);


    public List<String> boundaryParts = new ArrayList<String>(0);

    public boolean isinit = false;

    /**
     * 构建解析
     *
     * @param mhtFile
     */
    public IMhtmlParas(String mhtFile) {
        this.mhtFile = mhtFile;
    }

    /**
     * 解析mhtml模板数据,获取各部分的数据集合
     *
     * @return
     * @throws Exception
     */
    public IMhtmlParas init() {
        //将整个流转换成字符窜
        String content = this.mhtFile;
        //获取文本分隔符
        String boundary = getBoundary(content);
        if (boundary == null) {
            throw new RuntimeException("非规范的mhtml文件,缺少分隔符内容");
        }
        log.debug("mhtml的分隔符为:{}", boundary);
        isinit = true;
        String[] parts = content.split(boundary);
        List<String> partsAll = new ArrayList<String>(0);
        for (int i = 0; i < parts.length; i++) {
            if (i == 0) {
                log.warn("  {}  是文件头部,将被忽略", parts[i]);
                continue;
            }
            String part = parts[i];
            if (isPart(part)) {
                partsAll.add(part);
            } else {
                log.warn(" {}  不符合mhtml的内容规范，将忽略", part);
            }

        }
        boundaryParts.addAll(partsAll);
        return this;
    }

    /**
     * Content-ID: <frame-14866260E32CADC5D5ECDBF1C89D8ECE@mhtml.blink>
     * 是否包含part必须的元素即：Content-Location、Content-ID
     *
     * @param part
     * @return
     */
    private boolean isPart(String part) {
        boolean b = false;
        try {
            Matcher matcher = partSign.matcher(part);
            //防止遗留
            while (matcher.find()) {

                b = true;
            }
        } catch (Throwable e) {
            log.error("{}进行匹配异常，异常为{}", part, e);
        }
        return b;

    }


    /**
     * @param iMhtmlPartEnum 获取元素的
     * @return
     */
    public List<String> getParts(IMhtmlPartEnum iMhtmlPartEnum) {

        if (!isinit) {
            throw new RuntimeException("获取异常，请先初始化数据");
        }
        List<String> list = new ArrayList<String>(0);
        switch (iMhtmlPartEnum) {
            case CSS:
                list.addAll(findType(CSS_REGEX));
                break;
            case GIF:
                list.addAll(findType(GIF_REGEX));
                break;
            case PNG:
                list.addAll(findType(PNG_REGEX));
                break;
            case HTML:
                list.addAll(findType(HTML_REGEX));
                break;
            case JPEG:
                list.addAll(findType(JPEG_REGEX));
                break;
            default:
                log.warn("无知类型");
                break;
        }

        return list;

    }

    private List<String> findType(String partsRegex) {

        if (ObjectUtils.isEmpty(boundaryParts)) {
            throw new RuntimeException("非规范的mhtml文件,未发现符合条件的内容项");
        }
        List<String> list = new ArrayList<String>(0);
        for (String boundaryPart : boundaryParts) {

            Pattern pattern = Pattern.compile(partsRegex);

            Matcher matcher = pattern.matcher(boundaryPart);

            if (matcher.find()) {

                list.add(reductionContent(boundaryPart));
            }

        }

        return list;

    }

    /**
     * 还原内容==》
     *
     * @param group 文本内容
     * @return
     */
    private String reductionContent(String group) {


        //获取编码格式
        String content = getType(group);

        String part = getPartContent(group);

        //加密类
        Class screat = IMhtmlScretEnum.getScretInstance(content);

        //加密方法
        String method = IMhtmlScretEnum.getMetodName(content);

        try {
            Object ob = MethodUtils.invokeStaticMethod(screat, method, part.getBytes());

            if (ob.getClass().isArray()) {
                return new String((byte[]) ob);
            } else if (ob instanceof String) {
                return ob.toString();
            } else {
                throw new RuntimeException("加密类型返回异常");
            }
        } catch (Exception e) {
            log.error("加密异常", e);
            throw new RuntimeException("执行加密返回异常");
        }


    }

    /**
     * 获取mhtml的片段对应的全内容，不包含头信息
     *
     * @param group
     * @return
     */
    private String getPartContent(String group) {
        //首先使用location分隔
        String[] result = group.split(CONTENT_LOCATION_REGEX);
        //根据Content-Transfer-Encoding
        if (result.length == 1) {
            result = group.split(CONTENT_TRANSFER_ENCODING_REGEX);
        }
        if (result.length == 1) {
            throw new RuntimeException(String.format("%s 为非规范的mhtml文件,请手动添加提取纯内容的条件", group));
        }

        //获取location下的内容
        return result[1];
    }

    /**
     * 获取local提on对应的值
     *
     * @param group
     * @return
     */
    private String getLocation(String group) {

        Matcher matcher = contentLocation.matcher(group);

        String content = null;
        while (matcher.find()) {
            if (StringUtils.isBlank(content)) {
                content = matcher.group();
            } else {
                throw new RuntimeException("非规范的mhtml文件,发现多个符合条件的Content-Location");
            }
        }
        if (StringUtils.isBlank(content)) {
            throw new RuntimeException("非规范的mhtml文件,未发现符合条件的Content-Location");
        }

        int i = content.indexOf(":");

        return content.substring(i + 1);


    }

    /**
     * 获取Content-Transfer-Encoding对应的编码内容
     *
     * @param line
     * @return
     */
    private String getType(String line) {

        Matcher matcher = contentTransferEncodingPattern.matcher(line);
        String content = null;
        while (matcher.find()) {
            if (StringUtils.isBlank(content)) {
                content = matcher.group();
            } else {
                throw new RuntimeException(String.format("%s 非规范的mhtml文件,发现多个符合条件的Content-Transfer-Encoding", line));
            }
        }
        if (StringUtils.isBlank(content)) {
            throw new RuntimeException(String.format("%s非规范的mhtml文件,未发现符合条件的Content-Transfer-Encoding", line));
        }

        return content.split("\\:")[1];
    }

    /**
     * 从文档里提取 【电子邮件标头】里的文本分隔符
     */
    private String getBoundary(String content) {

        Matcher matcher = boundary.matcher(content);
        String result = null;
        while (matcher.find()) {
            if (result == null) {
                result = matcher.group();
            } else {
                throw new RuntimeException("非规范的mhtml文件,发现多个分隔符内容");
            }
        }

        result = result.split("\"")[1];
        return result;

    }


}
